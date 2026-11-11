package com.fashionweb.controller.order;

import com.fashionweb.exception.ValidationException;
import com.fashionweb.model.Order;
import com.fashionweb.model.User;
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

/**
 * GET /order?code=... : chi tiết một đơn hàng của khách (Customer, khu vực tài khoản).
 * JSP: /WEB-INF/views/order/order-detail.jsp
 * Tham số: code (mã đơn). Không có hoặc không phải đơn của khách thì trả 404.
 * Thuộc tính request: order (Order, có items).
 * <p>
 * POST /order (form gửi từ /orders và /order, field returnTo = "orders" | "detail" để biết quay về đâu):
 * <ul>
 *   <li>action=cancel (orderCode, cancelReason): chỉ khi đơn PENDING; hoàn tồn kho, trả lượt voucher, cancelledBy = CUSTOMER.</li>
 *   <li>action=confirmReceived (orderCode): chỉ khi đơn SHIPPING; chuyển COMPLETED, đã thanh toán.</li>
 * </ul>
 * Xử lý xong đặt flash message rồi redirect (PRG).
 */
@WebServlet("/order")
public class OrderDetailServlet extends HttpServlet {

    private final OrderService orderService = new OrderService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = WebUtil.currentUser(req);
        Order order = orderService.findByCodeAndUser(req.getParameter("code"), user.getId());
        if (order == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        req.setAttribute("order", order);
        req.getRequestDispatcher("/WEB-INF/views/order/order-detail.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = WebUtil.currentUser(req);
        String code = req.getParameter("orderCode");
        String action = req.getParameter("action");
        try {
            if ("cancel".equals(action)) {
                orderService.cancelByCustomer(code, user.getId(), req.getParameter("cancelReason"));
                WebUtil.flash(req, "Đã hủy đơn " + code + ". Tồn kho và mã giảm giá đã được hoàn lại.", "success");
            } else if ("confirmReceived".equals(action)) {
                orderService.confirmReceived(code, user.getId());
                WebUtil.flash(req, "Cảm ơn bạn! Đơn " + code + " đã hoàn thành. Hãy đánh giá sản phẩm bạn đã mua.", "success");
            } else {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
        } catch (ValidationException e) {
            WebUtil.flash(req, e.getMessage(), "error");
        }
        String back = "detail".equals(req.getParameter("returnTo")) && code != null
                ? "/order?code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                : "/orders";
        resp.sendRedirect(req.getContextPath() + back);
    }
}
