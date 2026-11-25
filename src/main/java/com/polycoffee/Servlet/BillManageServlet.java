package com.polycoffee.Servlet;

import com.polycoffee.DAO.BillDAO;
import com.polycoffee.DAO.BillDetailDAO;
import com.polycoffee.Dto.BillSummary;
import com.polycoffee.Util.Page;
import com.polycoffee.Util.ParamUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Quản lý hóa đơn (BR-21 đến BR-24).
 * <pre>
 *   GET  /manager/bills?page=      danh sách, mới nhất trước, 10 dòng mỗi trang
 *   GET  /manager/bills/detail?id= chi tiết
 *   POST /manager/bills/cancel     hủy (chỉ POST, GET không đổi dữ liệu)
 * </pre>
 */
@WebServlet("/manager/bills/*")
public class BillManageServlet extends HttpServlet {

    static final String LIST = "/WEB-INF/views/manager/bill/list.jsp";
    static final String DETAIL = "/WEB-INF/views/manager/bill/detail.jsp";

    private final BillDAO billDAO;
    private final BillDetailDAO detailDAO;

    public BillManageServlet() {
        this(new BillDAO(), new BillDetailDAO());
    }

    BillManageServlet(BillDAO billDAO, BillDetailDAO detailDAO) {
        this.billDAO = billDAO;
        this.detailDAO = detailDAO;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        switch (path) {
            case "":
            case "/":
                Page<BillSummary> pg = billDAO.findSummaries(ParamUtil.getInt(req, "page", 1), Page.SIZE);
                req.setAttribute("pg", pg);
                req.setAttribute("pagerBase", req.getContextPath() + "/manager/bills?");
                req.getRequestDispatcher(LIST).forward(req, resp);
                break;
            case "/detail":
                long id = ParamUtil.getLong(req, "id", 0);
                BillSummary bill = billDAO.findSummaryById(id);
                if (bill == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy hóa đơn");
                    return;
                }
                req.setAttribute("bill", bill);
                req.setAttribute("lines", detailDAO.findLinesByBillId(id));
                req.getRequestDispatcher(DETAIL).forward(req, resp);
                break;
            case "/cancel":
                resp.setHeader("Allow", "POST");
                resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Hủy hóa đơn phải dùng POST");
                break;
            default:
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!"/cancel".equals(req.getPathInfo())) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        long id = ParamUtil.getLong(req, "id", 0);
        if (billDAO.cancel(id) > 0) {
            req.getSession().setAttribute("flashOk", "Đã hủy hóa đơn #" + id);
        } else {
            req.getSession().setAttribute("flashError", "Không thể hủy hóa đơn (không tồn tại hoặc đã hủy)");
        }
        resp.sendRedirect(req.getContextPath() + "/manager/bills");
    }
}
