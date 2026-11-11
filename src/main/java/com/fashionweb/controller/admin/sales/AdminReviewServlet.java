package com.fashionweb.controller.admin.sales;

import com.fashionweb.controller.admin.AdminOpsParams;

import com.fashionweb.constant.AppConstant;
import com.fashionweb.exception.ValidationException;
import com.fashionweb.service.order.ReviewService;
import com.fashionweb.service.shop.ProductService;
import com.fashionweb.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * GET /admin/reviews?product=&amp;status=&amp;page= : danh sách bình luận cho Admin (cả bình luận đã ẩn).
 * JSP: /WEB-INF/views/admin/sales/reviews.jsp
 * Tham số: product (id sản phẩm), status (visible|hidden), page.
 * AdminFilter phải bảo vệ /admin/*.
 * POST action=toggleHidden (id): đảo ẩn/hiện, flash rồi quay về trang danh sách đang xem (giữ bộ lọc).
 */
@WebServlet("/admin/reviews")
public class AdminReviewServlet extends HttpServlet {

    private final ReviewService reviewService = new ReviewService();
    private final ProductService productService = new ProductService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Integer productId = AdminOpsParams.positiveInt(req.getParameter("product"));
        String rawStatus = AdminOpsParams.text(req, "status");
        String status = "visible".equals(rawStatus) || "hidden".equals(rawStatus) ? rawStatus : "";
        int page = AdminOpsParams.page(req);

        req.setAttribute("reviewPage", reviewService.search(productId, status, page, AppConstant.ADMIN_PAGE_SIZE));
        req.setAttribute("products", productService.findAllForFilter());
        req.setAttribute("product", productId == null ? "" : String.valueOf(productId));
        req.setAttribute("status", status);
        req.getRequestDispatcher("/WEB-INF/views/admin/sales/reviews.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!"toggleHidden".equals(req.getParameter("action"))) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try {
            boolean hidden = reviewService.toggleHidden(WebUtil.parseInt(req.getParameter("id"), 0));
            WebUtil.flash(req, hidden ? "Đã ẩn bình luận khỏi trang sản phẩm." : "Đã hiện lại bình luận.", "success");
        } catch (ValidationException e) {
            WebUtil.flash(req, e.getMessage(), "error");
        }
        // Quay về đúng trang đang xem nếu Referer là trang danh sách bình luận, tránh chuyển hướng ra ngoài
        String back = req.getContextPath() + "/admin/reviews";
        String referer = req.getHeader("Referer");
        if (referer != null) {
            int i = referer.indexOf(back);
            if (i >= 0 && referer.startsWith("/admin/reviews", i + req.getContextPath().length())) {
                back = referer.substring(i);
            }
        }
        resp.sendRedirect(back);
    }
}
