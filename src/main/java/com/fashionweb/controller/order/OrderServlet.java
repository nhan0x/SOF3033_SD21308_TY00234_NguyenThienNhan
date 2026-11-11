package com.fashionweb.controller.order;

import com.fashionweb.constant.AppConstant;
import com.fashionweb.constant.OrderStatus;
import com.fashionweb.model.Order;
import com.fashionweb.model.User;
import com.fashionweb.service.order.OrderService;
import com.fashionweb.service.shop.ProductService;
import com.fashionweb.util.Page;
import com.fashionweb.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * GET /orders?status=...&page=... : danh sách đơn hàng của khách đang đăng nhập (Customer, khu vực tài khoản).
 * JSP: /WEB-INF/views/order/orders.jsp
 * Tham số: status = PENDING | SHIPPING | COMPLETED | CANCELLED (rỗng hoặc sai = tất cả, không gồm đơn đã hủy); page (mặc định 1).
 * Thuộc tính request: orders (List&lt;Order&gt; của trang hiện tại, mới nhất trước, kèm items), orderPage (Page&lt;Order&gt;),
 * status (String), statusCounts (Map&lt;String,Integer&gt;), baseUrl (cho phân trang); welcomeProducts (List&lt;Product&gt;)
 * chỉ có khi khách chưa có đơn nào, để hiện dải ảnh ở thẻ chào mừng.
 * Hủy đơn và "Đã nhận hàng" gửi POST sang /order (xem OrderDetailServlet).
 */
@WebServlet("/orders")
public class OrderServlet extends HttpServlet {

    /** Số ảnh sản phẩm trong dải ảnh của thẻ chào mừng (khi chưa có đơn nào). */
    private static final int WELCOME_PRODUCTS = 5;

    private final OrderService orderService = new OrderService();
    private final ProductService productService = new ProductService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = WebUtil.currentUser(req);
        OrderStatus status = parseStatus(req.getParameter("status"));
        int page = WebUtil.parseInt(req.getParameter("page"), 1);

        Page<Order> orderPage = orderService.findPageByUser(user.getId(), status, page, AppConstant.ORDER_PAGE_SIZE);

        req.setAttribute("orders", orderPage.getItems());
        req.setAttribute("orderPage", orderPage);
        req.setAttribute("status", status == null ? "" : status.name());
        var statusCounts = orderService.countByStatusForUser(user.getId());
        req.setAttribute("statusCounts", statusCounts);
        if (statusCounts.getOrDefault("TOTAL", 0) == 0) {
            req.setAttribute("welcomeProducts", productService.bestSellers(WELCOME_PRODUCTS));
        }
        req.setAttribute("baseUrl", status == null ? "/orders" : "/orders?status=" + status.name());
        req.getRequestDispatcher("/WEB-INF/views/order/orders.jsp").forward(req, resp);
    }

    private static OrderStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return OrderStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
