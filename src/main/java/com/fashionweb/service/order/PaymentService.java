package com.fashionweb.service.order;

import com.fashionweb.constant.OrderStatus;
import com.fashionweb.constant.PaymentMethod;
import com.fashionweb.constant.PaymentStatus;
import com.fashionweb.dao.order.OrderDAO;
import com.fashionweb.dao.order.PaymentTransactionDAO;
import com.fashionweb.exception.DataAccessException;
import com.fashionweb.exception.ValidationException;
import com.fashionweb.model.Order;
import com.fashionweb.util.DBContext;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Thanh toán chuyển khoản qua payOS: tạo link thanh toán cho đơn, và đối chiếu tiền về (webhook hoặc hỏi trạng thái)
 * với đơn hàng.
 *
 * <p>Luồng: Servlet -> PaymentService -> PayOSClient / DAO -> MySQL. Phần quyết định (decide) và đổi mã (toPayOSCode,
 * fromPayOSCode) là hàm thuần, không cần database nên test được trực tiếp.
 */
public class PaymentService {

    /** Kết quả xử lý một giao dịch tiền vào. Tên trùng với cột payment_transactions.result. */
    public enum Outcome {
        /** Khớp đơn, đủ tiền: đơn được đánh dấu đã thanh toán. */
        PAID,
        /** Nội dung chuyển khoản không có mã đơn, hoặc mã đơn không tồn tại. */
        NO_ORDER,
        /** Đơn tồn tại nhưng là đơn COD. */
        WRONG_METHOD,
        /** Đơn đã bị hủy: tiền đã vào, cần hoàn lại thủ công. */
        CANCELLED,
        /** Đơn đã thanh toán rồi: tiền chuyển dư, cần hoàn lại thủ công. */
        ALREADY_PAID,
        /** Số tiền ít hơn tổng đơn: không đánh dấu đã thanh toán. */
        AMOUNT_TOO_LOW
    }

    public static final String PROVIDER_PAYOS = "PAYOS";

    /** Khoảng cách giữa các lần tạo link của cùng một đơn (xem toPayOSCode). Lớn hơn mọi số trong mã đơn (11 chữ số). */
    static final long ATTEMPT_STEP = 100_000_000_000L;

    /** Mã đơn dạng SAyyyyMMddNNN (xem OrderDAO.nextOrderCode): "SA" + 11 chữ số. */
    private static final Pattern ORDER_CODE = Pattern.compile("SA\\d{11}");

    private final OrderDAO orderDAO = new OrderDAO();
    private final PaymentTransactionDAO txDAO = new PaymentTransactionDAO();
    private final PayOSClient payOS;

    public PaymentService() {
        this(new PayOSClient());
    }

    public PaymentService(PayOSClient payOS) {
        this.payOS = payOS;
    }

    /**
     * Mã đơn payOS (phải là số) cho lần tạo link thứ attempt của đơn: phần số của mã đơn cộng attempt * ATTEMPT_STEP.
     * Ví dụ SA20261117001, lần 1 -> 100000000000 + 20261117001. Mỗi link cần một mã riêng vì payOS không cho dùng lại
     * mã của link đã hủy hoặc hết hạn.
     */
    public static long toPayOSCode(String orderCode, int attempt) {
        if (orderCode == null || !ORDER_CODE.matcher(orderCode).matches()) {
            throw new IllegalArgumentException("Mã đơn không đúng định dạng: " + orderCode);
        }
        return Long.parseLong(orderCode.substring(2)) + (long) attempt * ATTEMPT_STEP;
    }

    /** Đổi ngược mã đơn payOS về mã đơn của Saltair (bỏ phần attempt). */
    public static String fromPayOSCode(long payOSCode) {
        return "SA" + String.format(Locale.ROOT, "%011d", payOSCode % ATTEMPT_STEP);
    }

    /** Quyết định kết quả cho một khoản tiền vào. order = null khi không tìm thấy đơn. Không đụng database. */
    public static Outcome decide(Order order, long amount) {
        if (order == null) {
            return Outcome.NO_ORDER;
        }
        if (order.getPaymentMethod() != PaymentMethod.BANK_TRANSFER) {
            return Outcome.WRONG_METHOD;
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return Outcome.CANCELLED;
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            return Outcome.ALREADY_PAID;
        }
        return amount >= order.getTotalAmount() ? Outcome.PAID : Outcome.AMOUNT_TOO_LOW;
    }

    /**
     * Ghi nhận một giao dịch tiền vào và (nếu khớp) đánh dấu đơn đã thanh toán, trong một transaction.
     * Cùng một giao dịch (provider + txId) gửi lại nhiều lần chỉ xử lý một lần: các lần sau trả lại kết quả đã lưu.
     *
     * @param txId mã giao dịch dùng để chống xử lý trùng (với payOS: paymentLinkId, webhook và hỏi trạng thái dùng chung)
     * @param orderCode mã đơn Saltair (null nếu không biết)
     * @param amount số tiền VND
     * @param content ghi chú lưu cùng giao dịch (mô tả từ payOS)
     */
    public Outcome recordTransfer(String provider, String txId, String orderCode, long amount, String content) {
        try (Connection con = DBContext.getConnection()) {
            con.setAutoCommit(false);
            try {
                String seen = txDAO.findResult(con, provider, txId);
                if (seen != null) {
                    con.rollback();
                    return Outcome.valueOf(seen);
                }
                String code = orderCode;
                Order order = code == null ? null : orderDAO.lockByCode(con, code);
                Outcome outcome = decide(order, amount);
                if (outcome == Outcome.PAID) {
                    orderDAO.markPaid(con, order.getId());
                }
                txDAO.insert(con, provider, txId, order == null ? null : order.getId(), code, amount, content, outcome.name());
                con.commit();
                return outcome;
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (java.sql.SQLIntegrityConstraintViolationException dup) {
            // Hai yêu cầu cùng giao dịch đến đúng lúc: yêu cầu kia đã ghi xong, đọc lại kết quả của nó
            try (Connection con = DBContext.getConnection()) {
                String seen = txDAO.findResult(con, provider, txId);
                if (seen != null) {
                    return Outcome.valueOf(seen);
                }
            } catch (SQLException e) {
                throw new DataAccessException("Lỗi đọc giao dịch thanh toán", e);
            }
            throw new DataAccessException("Lỗi ghi giao dịch thanh toán", dup);
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi ghi giao dịch thanh toán", e);
        }
    }

    /**
     * Tạo link thanh toán payOS cho đơn, trả về dữ liệu để trang Saltair tự hiện mã QR và thông tin tài khoản nhận tiền.
     * Mỗi lần gọi là một link mới.
     *
     * @throws ValidationException nếu đơn không phải chuyển khoản, đã hủy hoặc đã thanh toán
     * @throws PayOSClient.PayOSException nếu payOS lỗi hoặc không kết nối được
     */
    public PayOSClient.PaymentLink startPayment(Order order, String returnUrl, String cancelUrl) {
        if (order.getPaymentMethod() != PaymentMethod.BANK_TRANSFER || order.getStatus() == OrderStatus.CANCELLED
                || order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new ValidationException(null, "Đơn này không cần thanh toán chuyển khoản.");
        }
        int attempt;
        try (Connection con = DBContext.getConnection()) {
            attempt = orderDAO.nextPaymentAttempt(con, order.getId());
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi tạo link thanh toán", e);
        }
        return payOS.createPaymentLink(toPayOSCode(order.getOrderCode(), attempt), order.getTotalAmount(),
                order.getOrderCode(), returnUrl, cancelUrl);
    }

    /**
     * Hủy link thanh toán gần nhất của đơn ở payOS để mã QR cũ không còn dùng được. Đơn vẫn giữ nguyên (chưa thanh toán),
     * khách có thể tạo link mới. Lỗi mạng hoặc payOS (link đã hủy, đã hết hạn, đã trả tiền) thì bỏ qua.
     *
     * @return true nếu payOS xác nhận đã hủy
     */
    public boolean cancelPayment(Order order) {
        if (order.getPaymentAttempts() == 0) {
            return false;
        }
        try {
            payOS.cancelPaymentLink(toPayOSCode(order.getOrderCode(), order.getPaymentAttempts()), "Khách hủy thanh toán");
            return true;
        } catch (PayOSClient.PayOSException e) {
            return false;
        }
    }

    /**
     * Hỏi payOS xem đơn đã thanh toán chưa (dùng khi webhook không tới được, ví dụ chạy trên máy cá nhân). Kiểm tra tối đa
     * 3 link gần nhất; thấy link đã PAID thì ghi nhận như webhook. Lỗi mạng hoặc payOS thì bỏ qua, lần hỏi sau thử lại.
     *
     * @return true nếu đơn vừa được đánh dấu đã thanh toán
     */
    public boolean syncPayment(Order order) {
        if (order.getPaymentMethod() != PaymentMethod.BANK_TRANSFER || order.getPaymentStatus() == PaymentStatus.PAID
                || order.getStatus() == OrderStatus.CANCELLED || order.getPaymentAttempts() == 0) {
            return false;
        }
        for (int attempt = order.getPaymentAttempts(); attempt >= Math.max(1, order.getPaymentAttempts() - 2); attempt--) {
            try {
                PayOSClient.PaymentStatusInfo info = payOS.getPaymentStatus(toPayOSCode(order.getOrderCode(), attempt));
                if ("PAID".equals(info.status())) {
                    String txId = info.paymentLinkId().isEmpty() ? "order-" + order.getOrderCode() + "-" + attempt : info.paymentLinkId();
                    return recordTransfer(PROVIDER_PAYOS, txId, order.getOrderCode(), info.amountPaid(), "hỏi trạng thái payOS")
                            == Outcome.PAID;
                }
            } catch (PayOSClient.PayOSException e) {
                // payOS chưa biết link này hoặc đang lỗi: thử link khác hoặc lần sau
            }
        }
        return false;
    }
}
