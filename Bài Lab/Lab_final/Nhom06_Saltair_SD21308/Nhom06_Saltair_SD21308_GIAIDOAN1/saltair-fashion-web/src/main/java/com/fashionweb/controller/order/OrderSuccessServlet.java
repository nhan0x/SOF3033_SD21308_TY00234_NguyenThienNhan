package com.fashionweb.controller.order;

import com.fashionweb.constant.AppConstant;
import com.fashionweb.constant.OrderStatus;
import com.fashionweb.constant.PaymentMethod;
import com.fashionweb.constant.PaymentStatus;
import com.fashionweb.model.Order;
import com.fashionweb.model.User;
import com.fashionweb.service.order.OrderService;
import com.fashionweb.service.order.PaymentService;
import com.fashionweb.util.PaymentConfig;
import com.fashionweb.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * GET /order-success?code=... : trang đặt hàng thành công. Khách đã đăng nhập xem được đơn của mình; khách chưa đăng nhập
 * chỉ xem được đơn vừa đặt trong phiên này (mã lưu ở session, AppConstant.SESSION_GUEST_ORDER).
 * JSP: /WEB-INF/views/order/order-success.jsp. Thuộc tính request: order (Order), payable (true khi đơn chuyển khoản còn
 * chờ thanh toán: trang hiện nút "Thanh toán" sang payOS và tự cập nhật khi tiền về).
 * GET /order-success?code=...&amp;cancel=true : đơn chuyển khoản chưa trả mà khách đã hủy thanh toán: trang
 * order-pay-cancelled.jsp (thuộc tính order). Địa chỉ payOS chuyển về (code=00&amp;orderCode=...) cũng được hiểu.
 * GET /order-success?code=...&amp;status=1 : trả JSON {"paid":true|false}. Chưa thanh toán thì hỏi payOS luôn
 * (webhook có thể chưa tới được, ví dụ chạy trên máy cá nhân).
 * Mã sai hoặc đơn của người khác thì 404.
 */
@WebServlet("/order-success")
public class OrderSuccessServlet extends HttpServlet {

    private final OrderService orderService = new OrderService();
    private final PaymentService paymentService = new PaymentService();

    /**
     * Đơn mà người đang xem được phép xem theo tham số code: khách đăng nhập xem đơn của mình; khách chưa đăng nhập chỉ
     * xem đơn vừa đặt trong phiên. Không được phép hoặc không có thì null.
     */
    static Order findVisibleOrder(HttpServletRequest req, OrderService orderService) {
        String code = req.getParameter("code");
        // payOS thêm code=00 (ghi đè mã đơn của ta) và orderCode=<mã số payOS> vào địa chỉ khi chuyển khách về:
        // có orderCode thì đổi ngược về mã đơn Saltair
        String payOSCode = req.getParameter("orderCode");
        if (payOSCode != null && payOSCode.matches("\\d{1,18}")) {
            code = PaymentService.fromPayOSCode(Long.parseLong(payOSCode));
        }
        User user = WebUtil.currentUser(req);
        if (user != null) {
            return orderService.findByCodeAndUser(code, user.getId());
        }
        if (code != null && req.getSession(false) != null
                && code.trim().equals(req.getSession(false).getAttribute(AppConstant.SESSION_GUEST_ORDER))) {
            return orderService.findByCode(code);
        }
        return null;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Order order = findVisibleOrder(req, orderService);
        req.setAttribute("guest", WebUtil.currentUser(req) == null);
        if (order == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        // Đơn chuyển khoản chưa thanh toán: hỏi payOS xem khách đã trả chưa, rồi đọc lại đơn
        if (order.getPaymentMethod() == PaymentMethod.BANK_TRANSFER && order.getPaymentStatus() == PaymentStatus.UNPAID
                && PaymentConfig.get().isEnabled() && paymentService.syncPayment(order)) {
            order = orderService.findByCode(order.getOrderCode());
        }
        // ?status=1: trang tự hỏi định kỳ xem đơn đã được thanh toán chưa (JSON, không có dữ liệu nhạy cảm)
        if ("1".equals(req.getParameter("status"))) {
            resp.setContentType("application/json;charset=UTF-8");
            resp.setHeader("Cache-Control", "no-store");
            resp.getWriter().write("{\"paid\":" + (order.getPaymentStatus() == PaymentStatus.PAID) + "}");
            return;
        }
        req.setAttribute("order", order);
        // Khách hủy thanh toán (nút Hủy ở trang thanh toán, hoặc payOS chuyển về với cancel=true) mà đơn chưa trả: trang riêng
        boolean unpaidTransfer = order.getPaymentMethod() == PaymentMethod.BANK_TRANSFER
                && order.getPaymentStatus() == PaymentStatus.UNPAID && order.getStatus() != OrderStatus.CANCELLED;
        if (unpaidTransfer && "true".equals(req.getParameter("cancel"))) {
            req.getRequestDispatcher("/WEB-INF/views/order/order-pay-cancelled.jsp").forward(req, resp);
            return;
        }
        req.setAttribute("payable", order.getPaymentMethod() == PaymentMethod.BANK_TRANSFER
                && order.getPaymentStatus() == PaymentStatus.UNPAID && order.getStatus() != OrderStatus.CANCELLED
                && PaymentConfig.get().isEnabled());
        req.getRequestDispatcher("/WEB-INF/views/order/order-success.jsp").forward(req, resp);
    }
}
