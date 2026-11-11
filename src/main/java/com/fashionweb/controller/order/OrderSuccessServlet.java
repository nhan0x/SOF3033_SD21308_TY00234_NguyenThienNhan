package com.fashionweb.controller.order;

import com.fashionweb.constant.AppConstant;
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

/**
 * GET /order-success?code=... : trang đặt hàng thành công. Khách đã đăng nhập xem được đơn của mình; khách chưa đăng nhập
 * chỉ xem được đơn vừa đặt trong phiên này (mã lưu ở session, AppConstant.SESSION_GUEST_ORDER).
 * JSP: /WEB-INF/views/order/order-success.jsp. Thuộc tính request: order (Order).
 * Mã sai hoặc đơn của người khác thì 404.
 */
@WebServlet("/order-success")
public class OrderSuccessServlet extends HttpServlet {

    private final OrderService orderService = new OrderService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String code = req.getParameter("code");
        User user = WebUtil.currentUser(req);
        Order order = null;
        if (user != null) {
            order = orderService.findByCodeAndUser(code, user.getId());
        } else if (code != null && req.getSession(false) != null
                && code.trim().equals(req.getSession(false).getAttribute(AppConstant.SESSION_GUEST_ORDER))) {
            order = orderService.findByCode(code);
        }
        req.setAttribute("guest", user == null);
        if (order == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        req.setAttribute("order", order);
        req.getRequestDispatcher("/WEB-INF/views/order/order-success.jsp").forward(req, resp);
    }
}
