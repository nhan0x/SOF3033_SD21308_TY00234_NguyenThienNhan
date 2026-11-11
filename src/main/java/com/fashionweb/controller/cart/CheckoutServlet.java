package com.fashionweb.controller.cart;

import com.fashionweb.constant.AppConstant;
import com.fashionweb.exception.ValidationException;
import com.fashionweb.exception.VoucherException;
import com.fashionweb.model.Address;
import com.fashionweb.model.Cart;
import com.fashionweb.model.Order;
import com.fashionweb.model.User;
import com.fashionweb.model.Voucher;
import com.fashionweb.service.account.AccountService;
import com.fashionweb.service.auth.AuthService;
import com.fashionweb.service.cart.CartService;
import com.fashionweb.service.cart.VoucherService;
import com.fashionweb.service.order.OrderService;
import com.fashionweb.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * GET /checkout : trang thanh toán, mở được cả khi chưa đăng nhập. Giỏ trống thì về /cart.
 * Khách chưa đăng nhập (thuộc tính guest = true): giỏ lấy từ session, bắt buộc nhập email và địa chỉ; đơn gắn vào tài khoản
 * của email đó (chưa có thì tạo), không tự đăng nhập, mã đơn lưu vào session để mở trang đặt hàng thành công.
 * JSP: /WEB-INF/views/cart/checkout.jsp. Tham số voucher: mã đang áp dụng (kiểm tra lại mỗi lần mở trang).
 * Thuộc tính: cart, addresses, defaultAddress, subtotal, shippingFee, discountAmount, total, appliedVoucher,
 * hintVouchers, voucherCode, voucherError, errors.
 * <p>POST action=applyVoucher (voucherInput) hoặc removeVoucher: hiện lại trang với mã mới (hoặc bỏ mã), giữ nguyên dữ liệu
 * khách đã nhập ở form; mã sai thì hiện voucherError.
 * <p>POST action=placeOrder (addressId hoặc "new" + receiverName, receiverPhone, addressLine, ward, district, city;
 * saveAddress, voucherCode, note): OrderService.placeOrder trong một transaction, xong redirect /order-success?code=...
 */
@WebServlet("/checkout")
public class CheckoutServlet extends HttpServlet {

    private static final Pattern PHONE = Pattern.compile("0[0-9]{9}");

    private final CartService cartService = new CartService();
    private final AccountService accountService = new AccountService();
    private final VoucherService voucherService = new VoucherService();
    private final OrderService orderService = new OrderService();
    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Cart cart = currentCart(req);
        if (cart.getItems().isEmpty()) {
            WebUtil.flash(req, "Giỏ hàng đang trống. Hãy chọn sản phẩm trước khi thanh toán.", "info");
            resp.sendRedirect(req.getContextPath() + "/cart");
            return;
        }
        show(req, resp, cart, req.getParameter("voucher"));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = WebUtil.currentUser(req);
        Cart cart = currentCart(req);
        if (cart.getItems().isEmpty()) {
            WebUtil.flash(req, "Giỏ hàng đang trống, không thể đặt hàng.", "error");
            resp.sendRedirect(req.getContextPath() + "/cart");
            return;
        }
        String action = req.getParameter("action");
        String code = normalize(req.getParameter("voucherCode"));

        // Áp mã hoặc gỡ mã: không redirect, hiện lại trang để giữ dữ liệu khách đã nhập (JSP đọc lại từ param)
        if ("applyVoucher".equals(action)) {
            String input = normalize(req.getParameter("voucherInput"));
            if (input.isEmpty()) {
                req.setAttribute("voucherError", "Vui lòng nhập mã giảm giá.");
            }
            show(req, resp, cart, input); // show() kiểm tra mã, sai thì đặt voucherError và giữ mã trong ô nhập
            return;
        }
        if ("removeVoucher".equals(action)) {
            show(req, resp, cart, "");
            return;
        }
        if (!"placeOrder".equals(action)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            Order order;
            if (user != null) {
                Address address = shippingAddress(req, user.getId());
                order = orderService.placeOrder(user.getId(), address, code, req.getParameter("note"));
                saveNewAddress(req, user.getId(), address);
            } else {
                order = placeGuestOrder(req, cart, code);
                WebUtil.guestCart(req).clear();
                req.getSession().setAttribute(AppConstant.SESSION_GUEST_ORDER, order.getOrderCode());
            }
            req.getSession().setAttribute(AppConstant.SESSION_CART_COUNT, 0);
            req.getSession().removeAttribute(AppConstant.SESSION_CART_NOTE);
            resp.sendRedirect(req.getContextPath() + "/order-success?code=" + URLEncoder.encode(order.getOrderCode(), StandardCharsets.UTF_8));
        } catch (ValidationException e) {
            Map<String, String> errors = new LinkedHashMap<>(e.getErrors());
            if (errors.isEmpty()) {
                req.setAttribute("message", e.getMessage());
                req.setAttribute("messageType", "error");
            }
            String voucherError = errors.remove("voucherCode");
            if (voucherError != null) {
                req.setAttribute("voucherError", voucherError);
                code = "";
            }
            req.setAttribute("errors", errors);
            show(req, resp, currentCart(req), code);
        }
    }

    /** Khách chọn "Lưu địa chỉ này vào sổ địa chỉ" khi nhập địa chỉ mới: lưu sau khi đặt hàng xong, lỗi thì bỏ qua. */
    private void saveNewAddress(HttpServletRequest req, int userId, Address address) {
        if (!"1".equals(req.getParameter("saveAddress")) || address.getId() > 0) {
            return;
        }
        try {
            address.setUserId(userId);
            accountService.saveAddress(address);
        } catch (ValidationException ignored) {
            // Đơn đã đặt xong; địa chỉ không hợp lệ với sổ địa chỉ thì chỉ không lưu
        }
    }

    /** Giỏ trong DB của khách đã đăng nhập, hoặc giỏ trong session của khách chưa đăng nhập. */
    private Cart currentCart(HttpServletRequest req) {
        User user = WebUtil.currentUser(req);
        return user != null ? cartService.getCart(user.getId()) : cartService.getGuestCart(WebUtil.guestCart(req));
    }

    /** Id dùng để kiểm tra voucher; khách chưa đăng nhập là 0 (kiểm tra lại theo tài khoản của email lúc đặt hàng). */
    private static int userId(User user) {
        return user == null ? 0 : user.getId();
    }

    /** Khách chưa đăng nhập: kiểm tra email cùng lúc với địa chỉ, tìm hoặc tạo tài khoản theo email rồi đặt hàng. */
    private Order placeGuestOrder(HttpServletRequest req, Cart cart, String code) {
        Map<String, String> errors = new LinkedHashMap<>();
        String email = null;
        try {
            email = authService.normalizeEmail(req.getParameter("email"));
        } catch (ValidationException e) {
            errors.putAll(e.getErrors());
        }
        Address address = null;
        try {
            address = shippingAddress(req, 0);
        } catch (ValidationException e) {
            errors.putAll(e.getErrors());
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        User owner = authService.findOrCreateCustomer(email, address.getReceiverName(), address.getPhone());
        return orderService.placeGuestOrder(owner.getId(), cart, address, code, req.getParameter("note"));
    }

    /** Địa chỉ giao: chọn từ sổ địa chỉ (addressId) hoặc nhập địa chỉ khác (addressId = "new"). */
    private Address shippingAddress(HttpServletRequest req, int userId) {
        String addressId = req.getParameter("addressId");
        if (userId > 0 && addressId != null && !"new".equals(addressId)) {
            Address a = accountService.findAddress(userId, WebUtil.parseInt(addressId, 0));
            if (a == null) {
                throw new ValidationException("addressId", "Vui lòng chọn địa chỉ giao hàng.");
            }
            return a;
        }
        Address a = new Address();
        a.setReceiverName(trim(req.getParameter("receiverName")));
        a.setPhone(trim(req.getParameter("receiverPhone")));
        a.setAddressLine(trim(req.getParameter("addressLine")));
        a.setWard(trim(req.getParameter("ward")));
        a.setDistrict(trim(req.getParameter("district")));
        a.setCity(trim(req.getParameter("city")));
        Map<String, String> errors = new LinkedHashMap<>();
        if (a.getReceiverName().length() < 2 || a.getReceiverName().length() > 100) {
            errors.put("receiverName", "Tên người nhận từ 2 đến 100 ký tự.");
        }
        if (!PHONE.matcher(a.getPhone()).matches()) {
            errors.put("receiverPhone", "Số điện thoại gồm 10 chữ số, bắt đầu bằng 0.");
        }
        if (a.getAddressLine().length() < 5 || a.getAddressLine().length() > 200) {
            errors.put("addressLine", "Địa chỉ (số nhà, đường) từ 5 đến 200 ký tự.");
        }
        if (a.getCity().isEmpty() || a.getCity().length() > 100) {
            errors.put("city", "Vui lòng nhập tỉnh/thành phố.");
        }
        String note = req.getParameter("note");
        if (note != null && note.length() > 500) {
            errors.put("note", "Ghi chú tối đa 500 ký tự.");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        StringBuilder full = new StringBuilder(a.getAddressLine());
        for (String part : new String[]{a.getWard(), a.getDistrict(), a.getCity()}) {
            if (!part.isEmpty()) {
                full.append(", ").append(part);
            }
        }
        a.setFullAddress(full.toString());
        return a;
    }

    private void show(HttpServletRequest req, HttpServletResponse resp, Cart cart, String voucherParam)
            throws ServletException, IOException {
        User user = WebUtil.currentUser(req);
        long subtotal = cart.getSubtotal();
        Voucher applied = null;
        String code = normalize(voucherParam);
        if (!code.isEmpty()) {
            try {
                applied = voucherService.validate(code, userId(user), cart);
            } catch (VoucherException e) {
                req.setAttribute("voucherError", e.getMessage());
            }
        }
        if (applied != null) {
            req.removeAttribute("voucherError");
        }
        long discount = voucherService.computeDiscount(applied, subtotal);
        long shippingFee = voucherService.shippingFee(applied);
        List<Address> addresses = user == null ? List.of() : accountService.findAddresses(user.getId());

        req.setAttribute("guest", user == null);
        req.setAttribute("cart", cart);
        req.setAttribute("addresses", addresses);
        req.setAttribute("defaultAddress", addresses.isEmpty() ? null : addresses.get(0)); // mặc định đứng đầu
        req.setAttribute("subtotal", subtotal);
        req.setAttribute("shippingFee", shippingFee);
        req.setAttribute("discountAmount", discount);
        req.setAttribute("total", subtotal - discount + shippingFee);
        req.setAttribute("appliedVoucher", applied);
        req.setAttribute("hintVouchers", voucherService.findActive());
        req.setAttribute("voucherCode", code);
        req.getRequestDispatcher("/WEB-INF/views/cart/checkout.jsp").forward(req, resp);
    }

    private static String normalize(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
