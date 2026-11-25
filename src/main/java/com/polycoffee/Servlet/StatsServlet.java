package com.polycoffee.Servlet;

import com.polycoffee.DAO.BillDAO;
import com.polycoffee.Dto.RevenuePoint;
import com.polycoffee.Util.ParamUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;

/**
 * Thống kê (BR-29 đến BR-32).
 * <pre>
 *   GET /manager/stats/revenue?from=&amp;to=      doanh thu theo ngày, biểu đồ cột
 *   GET /manager/stats/top-drinks?from=&amp;to=   top 5 đồ uống bán chạy
 * </pre>
 * Ngày theo dạng yyyy-MM-dd. Mặc định là 30 ngày gần nhất.
 */
@WebServlet("/manager/stats/*")
public class StatsServlet extends HttpServlet {

    static final String REVENUE = "/WEB-INF/views/manager/stats/revenue.jsp";
    static final String TOP_DRINKS = "/WEB-INF/views/manager/stats/top-drinks.jsp";
    static final int TOP_LIMIT = 5;
    static final int DEFAULT_DAYS = 30;

    private final BillDAO dao;

    public StatsServlet() {
        this(new BillDAO());
    }

    StatsServlet(BillDAO dao) {
        this.dao = dao;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        boolean revenue = path.equals("/revenue");
        if (!revenue && !path.equals("/top-drinks")) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String fromText = ParamUtil.getString(req, "from", LocalDate.now().minusDays(DEFAULT_DAYS).toString());
        String toText = ParamUtil.getString(req, "to", LocalDate.now().toString());
        req.setAttribute("from", fromText);
        req.setAttribute("to", toText);

        String error = null;
        LocalDate from = null;
        LocalDate to = null;
        try {
            from = LocalDate.parse(fromText);
            to = LocalDate.parse(toText);
            if (from.isAfter(to)) {
                error = "Từ ngày phải nhỏ hơn hoặc bằng đến ngày";
            }
        } catch (DateTimeParseException e) {
            error = "Ngày không hợp lệ (định dạng yyyy-MM-dd)";
        }

        if (error != null) {
            req.setAttribute("error", error);
            req.setAttribute(revenue ? "points" : "top", Collections.emptyList());
            req.setAttribute("grandTotal", 0.0);
        } else if (revenue) {
            List<RevenuePoint> points = dao.revenueByDay(from, to);
            req.setAttribute("points", points);
            req.setAttribute("grandTotal", points.stream().mapToDouble(RevenuePoint::getRevenue).sum());
        } else {
            req.setAttribute("top", dao.topDrinks(from, to, TOP_LIMIT));
        }
        req.getRequestDispatcher(revenue ? REVENUE : TOP_DRINKS).forward(req, resp);
    }
}
