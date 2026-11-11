package com.fashionweb.controller.order;

import com.fashionweb.model.Order;
import com.fashionweb.service.order.OrderService;
import com.fashionweb.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * GET /order-success?code=... : trang đặt hàng thành công (Customer, AuthFilter bắt buộc đăng nhập).
 * JSP: /WEB-INF/views/order/order-success.jsp. Thuộc tính request: order (Order).
 * Mã sai hoặc đơn của người khác thì 404.
 */
@WebServlet("/order-success")
public class OrderSuccessServlet extends HttpServlet {

    private final OrderService orderService = new OrderService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Order order = orderService.findByCodeAndUser(req.getParameter("code"), WebUtil.currentUser(req).getId());
        if (order == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        req.setAttribute("order", order);
        req.getRequestDispatcher("/WEB-INF/views/order/order-success.jsp").forward(req, resp);
    }
}
