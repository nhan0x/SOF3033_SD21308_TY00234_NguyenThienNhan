package com.fashionweb.controller.admin.system;

import com.fashionweb.dto.DashboardStats;
import com.fashionweb.model.Order;
import com.fashionweb.model.Product;
import com.fashionweb.service.admin.StatisticService;
import com.fashionweb.service.order.OrderService;
import com.fashionweb.service.shop.ProductService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * GET /admin : trang tổng quan của Admin.
 * JSP: /WEB-INF/views/admin/system/dashboard.jsp
 * Tham số: dateFrom, dateTo (yyyy-MM-dd, mặc định 7 ngày gần nhất tính đến hôm nay; sai định dạng thì dùng mặc định).
 * Thuộc tính: stats, recentOrders, lowStockProducts, dateFrom, dateTo, revenueMax, rangeRevenue.
 * AdminFilter phải bảo vệ /admin/* (chỉ role ADMIN).
 * Dữ liệu: StatisticService.getStats(dateFrom, dateTo), OrderService.latest(5), ProductService.lowestStock(5).
 * Ngày kết thúc trước ngày bắt đầu thì đổi chỗ hai ngày; khoảng dài hơn 92 ngày thì lấy 92 ngày tính lùi từ ngày kết thúc
 * (biểu đồ theo ngày không quá dài).
 */
@WebServlet("/admin")
public class AdminDashboardServlet extends HttpServlet {

    private static final int RECENT_ORDERS = 5;
    private static final int LOW_STOCK = 5;
    private static final int DEFAULT_RANGE_DAYS = 7;
    private static final int MAX_RANGE_DAYS = 92;

    private final StatisticService statisticService = new StatisticService();
    private final OrderService orderService = new OrderService();
    private final ProductService productService = new ProductService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LocalDate to = parseDate(req.getParameter("dateTo"), LocalDate.now());
        LocalDate from = parseDate(req.getParameter("dateFrom"), to.minusDays(DEFAULT_RANGE_DAYS - 1));
        if (from.isAfter(to)) {
            LocalDate tmp = from;
            from = to;
            to = tmp;
        }
        if (from.isBefore(to.minusDays(MAX_RANGE_DAYS - 1))) {
            from = to.minusDays(MAX_RANGE_DAYS - 1);
        }

        DashboardStats stats = statisticService.getStats(from, to);
        List<Order> recentOrders = orderService.latest(RECENT_ORDERS);
        List<Product> lowStockProducts = productService.lowestStock(LOW_STOCK);

        long revenueMax = 0;
        long rangeRevenue = 0;
        if (stats.getRevenueByDay() != null) {
            for (DashboardStats.DailyRevenue day : stats.getRevenueByDay()) {
                revenueMax = Math.max(revenueMax, day.getValue());
                rangeRevenue += day.getValue();
            }
        }

        req.setAttribute("stats", stats);
        req.setAttribute("recentOrders", recentOrders);
        req.setAttribute("lowStockProducts", lowStockProducts);
        req.setAttribute("dateFrom", from.toString());
        req.setAttribute("dateTo", to.toString());
        req.setAttribute("revenueMax", revenueMax);
        req.setAttribute("rangeRevenue", rangeRevenue);
        req.getRequestDispatcher("/WEB-INF/views/admin/system/dashboard.jsp").forward(req, resp);
    }

    private static LocalDate parseDate(String value, LocalDate fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return fallback;
        }
    }
}
