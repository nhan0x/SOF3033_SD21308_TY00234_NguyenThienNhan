package com.fashionweb.service.order;

import com.fashionweb.constant.CancelledBy;
import com.fashionweb.constant.OrderStatus;
import com.fashionweb.constant.PaymentMethod;
import com.fashionweb.constant.PaymentStatus;
import com.fashionweb.repository.cart.CartRepository;
import com.fashionweb.repository.cart.VoucherRepository;
import com.fashionweb.repository.order.OrderRepository;
import com.fashionweb.repository.order.OrderDetailRepository;
import com.fashionweb.repository.shop.ProductRepository;
import com.fashionweb.exception.DataAccessException;
import com.fashionweb.exception.ValidationException;
import com.fashionweb.exception.VoucherException;
import com.fashionweb.model.Address;
import com.fashionweb.model.Cart;
import com.fashionweb.model.CartItem;
import com.fashionweb.model.Order;
import com.fashionweb.model.OrderDetail;
import com.fashionweb.model.Voucher;
import com.fashionweb.service.cart.CartService;
import com.fashionweb.service.cart.VoucherService;
import com.fashionweb.util.DBContext;
import com.fashionweb.util.Page;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Đặt hàng, đơn của khách và quản lý đơn của Admin.
 *
 * <p>Luồng gọi: Servlet -> Service -> Repository -> MySQL. Thao tác nhiều bảng (đặt hàng, hủy đơn) chạy trong một
 * transaction do Service mở; Repository nhận Connection của transaction đó.
 * <p>Đã làm: placeOrder (#14); findByUser, findPageByUser, countByStatusForUser, findByCodeAndUser, countByUser (#16);
 * cancelByCustomer, confirmReceived, cancelByAdmin (#17, hàm hủy dùng chung). search, countByStatus, findByCode, latest (#19, #26). updateStatus, cancelByAdmin (#20).
 */
public class OrderService {

    private final OrderRepository orderRepository = new OrderRepository();
    private final OrderDetailRepository orderDetailRepository = new OrderDetailRepository();
    private final ProductRepository productRepository = new ProductRepository();
    private final VoucherRepository voucherRepository = new VoucherRepository();
    private final CartRepository cartRepository = new CartRepository();
    private final CartService cartService = new CartService();
    private final VoucherService voucherService = new VoucherService();

    /**
     * MỘT transaction: kiểm tra giỏ không rỗng, kiểm tra voucher, tạo order và order_details (lưu snapshot tên, giá, màu, size, ảnh), trừ tồn kho từng biến thể, tăng lượt dùng voucher, xóa giỏ.
     * shippingFee = AppConstant.SHIPPING_FEE, bằng 0 khi có voucher hợp lệ. total = subtotal - discount + shippingFee. Tạo orderCode dạng SAyyyyMMddNNN.
     */
    public Order placeOrder(int userId, Address shippingAddress, String voucherCode, String note) {
        return placeOrder(userId, cartService.getCart(userId), true, shippingAddress, voucherCode, note);
    }

    /**
     * Đặt hàng cho khách chưa đăng nhập: giỏ lấy từ session (CartService.getGuestCart), đơn gắn vào tài khoản userId
     * (tài khoản của email khách nhập). Không đụng tới giỏ trong DB của tài khoản đó.
     */
    public Order placeGuestOrder(int userId, Cart guestCart, Address shippingAddress, String voucherCode, String note) {
        return placeOrder(userId, guestCart, false, shippingAddress, voucherCode, note);
    }

    private Order placeOrder(int userId, Cart cart, boolean clearDbCart, Address shippingAddress, String voucherCode, String note) {
        if (cart.getItems().isEmpty()) {
            throw new ValidationException(null, "Giỏ hàng đang trống, không thể đặt hàng.");
        }
        for (CartItem it : cart.getItems()) {
            if (it.getQuantity() > it.getStock()) {
                throw new ValidationException(null, "\"" + it.getProductName() + "\" (" + it.getColorName() + ", "
                        + it.getSizeName() + ") chỉ còn " + it.getStock() + " sản phẩm. Hãy chỉnh lại giỏ hàng.");
            }
        }
        Voucher voucher = null;
        if (voucherCode != null && !voucherCode.isBlank()) {
            try {
                voucher = voucherService.validate(voucherCode, userId, cart);
            } catch (VoucherException e) {
                throw new ValidationException("voucherCode", e.getMessage());
            }
        }
        long subtotal = cart.getSubtotal();
        long discount = voucherService.computeDiscount(voucher, subtotal);
        long shipping = voucherService.shippingFee(voucher);

        Order o = new Order();
        o.setUserId(userId);
        o.setReceiverName(shippingAddress.getReceiverName());
        o.setReceiverPhone(shippingAddress.getPhone());
        o.setShippingAddress(shippingAddress.getFullAddress());
        o.setNote(note == null || note.isBlank() ? null : note.trim());
        o.setPaymentMethod(PaymentMethod.COD);
        o.setPaymentStatus(PaymentStatus.UNPAID);
        o.setStatus(OrderStatus.PENDING);
        o.setSubtotal(subtotal);
        o.setDiscountAmount(discount);
        o.setShippingFee(shipping);
        o.setTotalAmount(subtotal - discount + shipping);
        if (voucher != null) {
            o.setVoucherId(voucher.getId());
            o.setVoucherCode(voucher.getCode());
        }
        List<OrderDetail> details = new ArrayList<>();
        for (CartItem it : cart.getItems()) {
            OrderDetail d = new OrderDetail();
            d.setVariantId(it.getVariantId());
            d.setProductId(it.getProductId());
            d.setProductName(it.getProductName());
            d.setColorName(it.getColorName());
            d.setSizeName(it.getSizeName());
            d.setImage(it.getImage());
            d.setUnitPrice(it.getUnitPrice());
            d.setQuantity(it.getQuantity());
            d.setLineTotal(it.getUnitPrice() * it.getQuantity());
            details.add(d);
        }

        // Một transaction: tạo đơn, chi tiết, trừ tồn kho, tăng lượt voucher, xóa giỏ. Lỗi thì rollback toàn bộ.
        try (Connection con = DBContext.getConnection()) {
            con.setAutoCommit(false);
            try {
                String prefix = "SA" + new SimpleDateFormat("yyyyMMdd").format(new Date());
                o.setOrderCode(orderRepository.nextOrderCode(con, prefix));
                o.setId(orderRepository.insert(con, o));
                orderDetailRepository.insertAll(con, o.getId(), details);
                for (OrderDetail d : details) {
                    // Kiểm tra lại tồn kho ngay trong transaction (chống hai người cùng mua món cuối)
                    if (!productRepository.decreaseStock(con, d.getVariantId(), d.getQuantity())) {
                        throw new ValidationException(null, "\"" + d.getProductName() + "\" (" + d.getColorName() + ", "
                                + d.getSizeName() + ") vừa hết hàng. Hãy chỉnh lại giỏ hàng.");
                    }
                }
                if (voucher != null && !voucherRepository.increaseUsed(con, voucher.getId())) {
                    throw new ValidationException("voucherCode", "Mã giảm giá " + voucher.getCode() + " vừa hết lượt sử dụng.");
                }
                if (clearDbCart) {
                    cartRepository.deleteAllItemsOfUser(con, userId);
                }
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đặt hàng", e);
        }
        o.setItems(details);
        return o;
    }

    /**
     * Đơn của khách, lọc trạng thái (null = tất cả), mới nhất trước, kèm items.
     */
    public List<Order> findByUser(int userId, OrderStatus status) {
        List<Order> orders = orderRepository.findByUser(userId, status);
        orders.forEach(o -> o.setItems(orderDetailRepository.findByOrder(o.getId())));
        return orders;
    }

    /**
     * Một trang đơn của khách (trang "Đơn hàng của tôi"), mỗi đơn kèm items.
     * status null (tab "Tất cả") không gồm đơn đã hủy: đơn đã hủy chỉ xem ở tab "Đã hủy" (#4).
     */
    public Page<Order> findPageByUser(int userId, OrderStatus status, int page, int pageSize) {
        Page<Order> p = orderRepository.findPageByUser(userId, status, OrderStatus.CANCELLED, page, pageSize);
        p.getItems().forEach(o -> o.setItems(orderDetailRepository.findByOrder(o.getId())));
        return p;
    }

    /**
     * Số đơn của khách theo từng trạng thái. ALL = số đơn ở tab "Tất cả" (không tính đơn đã hủy, #4);
     * TOTAL = mọi đơn kể cả đã hủy (0 nghĩa là khách chưa từng đặt hàng).
     */
    public Map<String, Integer> countByStatusForUser(int userId) {
        Map<String, Integer> counts = orderRepository.countByStatus(userId, null, null);
        int total = counts.getOrDefault("ALL", 0);
        counts.put("TOTAL", total);
        counts.put("ALL", total - counts.getOrDefault(OrderStatus.CANCELLED.name(), 0));
        return counts;
    }

    /**
     * Đơn kèm items; null nếu không phải đơn của user.
     */
    public Order findByCodeAndUser(String code, int userId) {
        if (code == null || code.isBlank()) {
            return null;
        }
        Order o = orderRepository.findByCode(code.trim());
        if (o == null || o.getUserId() != userId) {
            return null;
        }
        o.setItems(orderDetailRepository.findByOrder(o.getId()));
        return o;
    }

    /**
     * Chỉ khi PENDING. Cộng trả tồn kho, trả lượt voucher, status = CANCELLED, cancelledBy = CUSTOMER.
     */
    public void cancelByCustomer(String code, int userId, String reason) {
        cancel(code, userId, CancelledBy.CUSTOMER, reason, EnumSet.of(OrderStatus.PENDING),
                "Chỉ hủy được đơn đang ở trạng thái Chờ xác nhận.");
    }

    /**
     * Chỉ khi SHIPPING. status = COMPLETED, paymentStatus = PAID, completedAt = NOW().
     */
    public void confirmReceived(String code, int userId) {
        try (Connection con = DBContext.getConnection()) {
            con.setAutoCommit(false);
            try {
                Order o = lockOrder(con, code, userId);
                if (o.getStatus() != OrderStatus.SHIPPING) {
                    throw new ValidationException(null, "Chỉ xác nhận đã nhận hàng khi đơn đang giao.");
                }
                orderRepository.updateStatus(con, o.getId(), OrderStatus.COMPLETED, PaymentStatus.PAID, new Date());
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi xác nhận đã nhận hàng", e);
        }
    }

    /**
     * Hủy đơn dùng chung cho khách (#17) và Admin (#20), chạy trong MỘT transaction:
     * khóa dòng đơn (SELECT ... FOR UPDATE) rồi kiểm tra trạng thái, nên gọi hủy hai lần thì lần sau bị từ chối
     * và tồn kho không bị cộng hai lần. Cộng trả tồn kho từng biến thể, trả một lượt voucher, ghi lý do và người hủy.
     *
     * @param ownerId id khách sở hữu đơn; null khi Admin hủy (không kiểm tra chủ đơn)
     */
    private void cancel(String code, Integer ownerId, CancelledBy by, String reason,
                        Set<OrderStatus> allowed, String notAllowedMessage) {
        String r = reason == null ? "" : reason.trim();
        if (r.length() < 5 || r.length() > 255) {
            throw new ValidationException("cancelReason", "Lý do hủy cần từ 5 đến 255 ký tự.");
        }
        try (Connection con = DBContext.getConnection()) {
            con.setAutoCommit(false);
            try {
                Order o = lockOrder(con, code, ownerId);
                if (!allowed.contains(o.getStatus())) {
                    throw new ValidationException(null, notAllowedMessage);
                }
                for (OrderDetail d : orderDetailRepository.findByOrder(o.getId())) {
                    productRepository.increaseStock(con, d.getVariantId(), d.getQuantity());
                }
                if (o.getVoucherId() != null) {
                    voucherRepository.decreaseUsed(con, o.getVoucherId());
                }
                orderRepository.cancel(con, o.getId(), r, by);
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi hủy đơn hàng", e);
        }
    }

    /**
     * Khóa đơn theo mã trong transaction. Không có, hoặc không phải đơn của ownerId (khi khác null), thì báo không tìm thấy.
     */
    private Order lockOrder(Connection con, String code, Integer ownerId) throws SQLException {
        Order o = code == null || code.isBlank() ? null : orderRepository.lockByCode(con, code.trim());
        if (o == null || (ownerId != null && o.getUserId() != ownerId)) {
            throw new ValidationException(null, "Không tìm thấy đơn hàng.");
        }
        return o;
    }

    /**
     * Tổng số đơn của khách (trang hồ sơ).
     */
    public int countByUser(int userId) {
        return orderRepository.countByUser(userId);
    }

    /**
     * Admin: tìm theo mã đơn hoặc tên khách, lọc trạng thái và thanh toán.
     */
    public Page<Order> search(String q, OrderStatus status, PaymentStatus payment, int page, int pageSize) {
        Page<Order> p = orderRepository.findPage(q, status, payment, page, pageSize);
        if (p.getPage() > p.getTotalPages() && p.getTotalPages() > 0) {
            p = orderRepository.findPage(q, status, payment, p.getTotalPages(), pageSize);
        }
        return p;
    }

    /**
     * Admin: số đơn theo trạng thái cho các chip lọc.
     */
    public Map<String, Integer> countByStatus(String q, PaymentStatus payment) {
        return orderRepository.countByStatus(null, q, payment);
    }

    /**
     * Admin: đơn kèm items, customerName, customerEmail.
     */
    public Order findByCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        Order o = orderRepository.findByCode(code.trim());
        if (o != null) {
            o.setItems(orderDetailRepository.findByOrder(o.getId()));
        }
        return o;
    }

    /**
     * Admin: PENDING -> SHIPPING -> COMPLETED. COMPLETED đặt paymentStatus = PAID và completedAt.
     */
    public void updateStatus(String code, OrderStatus next) {
        try (Connection con = DBContext.getConnection()) {
            con.setAutoCommit(false);
            try {
                Order o = lockOrder(con, code, null);
                OrderStatus allowedNext = switch (o.getStatus()) {
                    case PENDING -> OrderStatus.SHIPPING;
                    case SHIPPING -> OrderStatus.COMPLETED;
                    default -> null;
                };
                if (next == null || next != allowedNext) {
                    throw new ValidationException(null, allowedNext == null
                            ? "Đơn đã " + (o.getStatus() == OrderStatus.CANCELLED ? "hủy" : "hoàn thành") + ", không đổi trạng thái được nữa."
                            : "Chỉ chuyển được theo thứ tự Chờ xác nhận → Đang giao → Hoàn thành.");
                }
                boolean done = next == OrderStatus.COMPLETED;
                orderRepository.updateStatus(con, o.getId(), next, done ? PaymentStatus.PAID : o.getPaymentStatus(), done ? new Date() : null);
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi cập nhật trạng thái đơn", e);
        }
    }

    /**
     * Admin: hủy khi PENDING hoặc SHIPPING, cancelledBy = ADMIN, trả tồn kho và lượt voucher.
     */
    public void cancelByAdmin(String code, String reason) {
        cancel(code, null, CancelledBy.ADMIN, reason, EnumSet.of(OrderStatus.PENDING, OrderStatus.SHIPPING),
                "Chỉ hủy được đơn đang Chờ xác nhận hoặc Đang giao.");
    }

    /**
     * n đơn mới nhất (dashboard).
     */
    public List<Order> latest(int n) {
        return orderRepository.findLatest(n);
    }
}
