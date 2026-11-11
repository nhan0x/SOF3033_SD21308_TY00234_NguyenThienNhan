package com.fashionweb.controller.shop;


import com.fashionweb.constant.AppConstant;
import com.fashionweb.constant.Role;
import com.fashionweb.exception.ValidationException;
import com.fashionweb.model.Product;
import com.fashionweb.model.Review;
import com.fashionweb.model.User;
import com.fashionweb.service.account.AccountService;
import com.fashionweb.service.order.ReviewService;
import com.fashionweb.service.shop.ProductService;
import com.fashionweb.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * GET /product?slug=... : chi tiết sản phẩm (Guest + Customer).
 * <p>
 * JSP: /WEB-INF/views/shop/product-detail.jsp
 * <p>
 * Tham số: slug (bắt buộc). Thiếu hoặc không tìm thấy sản phẩm thì trả 404.
 * <p>
 * Thuộc tính thêm: canReview (khách có đơn Hoàn thành chứa sản phẩm), myReview (đánh giá của chính khách, null nếu chưa có).
 * <p>
 * POST /product (khách đã đăng nhập, AuthFilter không chặn /product nên Servlet tự kiểm tra):
 * <ul>
 *   <li>action=review (productId, slug, rating 1..5, comment 10..1000 ký tự): thêm, hoặc sửa nếu đã đánh giá.
 *       Sai dữ liệu thì hiện lại trang kèm errors; đúng thì flash rồi redirect về /product?slug=...#reviews.</li>
 *   <li>action=deleteReview (productId, slug): xóa đánh giá của chính mình.</li>
 * </ul>
 * Chưa đăng nhập thì chuyển sang /login.
 */
@WebServlet("/product")
public class ProductDetailServlet extends HttpServlet {

    private static final int RELATED_LIMIT = 8;

    private final ProductService productService = new ProductService();
    private final ReviewService reviewService = new ReviewService();
    private final AccountService accountService = new AccountService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String slug = req.getParameter("slug");
        Product product = productService.findBySlug(slug);
        if (product == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Đánh giá: mới nhất trước; đếm theo số sao (chỉ số 1..5, phần tử 0 không dùng)
        List<Review> reviews = reviewService.listVisibleByProduct(product.getId());
        int[] breakdown = new int[6];
        int total = 0;
        int sum = 0;
        for (Review r : reviews) {
            int star = Math.min(5, Math.max(1, r.getRating()));
            breakdown[star]++;
            total++;
            sum += star;
        }
        double avg = total == 0 ? 0 : Math.round(sum * 10.0 / total) / 10.0;

        // Ảnh: dự án mỗi sản phẩm 1 ảnh. JSP lặp qua danh sách này nên thêm nhiều ảnh sau này vẫn chạy.
        List<String> galleryImages = new ArrayList<>();
        if (product.getImage() != null && !product.getImage().isBlank()) {
            galleryImages.add(product.getImage());
        }

        // Được đánh giá khi có đơn Hoàn thành chứa sản phẩm; đã đánh giá thì form dùng để sửa
        User user = WebUtil.currentUser(req);
        boolean customer = user != null && user.getRole() == Role.CUSTOMER;
        boolean canReview = customer && reviewService.canReview(user.getId(), product.getId());
        Review myReview = customer ? reviewService.findMine(user.getId(), product.getId()) : null;
        boolean inWishlist = user != null && accountService.isInWishlist(user.getId(), product.getId());

        req.setAttribute("product", product);
        req.setAttribute("galleryImages", galleryImages);
        req.setAttribute("reviews", reviews);
        req.setAttribute("relatedProducts", productService.related(product, RELATED_LIMIT));
        req.setAttribute("canReview", canReview);
        req.setAttribute("myReview", myReview);
        req.setAttribute("inWishlist", inWishlist);
        req.setAttribute("ratingBreakdown", breakdown);
        req.setAttribute("reviewTotal", total);
        req.setAttribute("reviewAvg", avg);
        req.setAttribute("shippingFee", AppConstant.SHIPPING_FEE);
        req.getRequestDispatcher("/WEB-INF/views/shop/product-detail.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String slug = req.getParameter("slug");
        String back = req.getContextPath() + "/product?slug=" + URLEncoder.encode(slug == null ? "" : slug, StandardCharsets.UTF_8);
        User user = WebUtil.currentUser(req);
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login?redirect=" + URLEncoder.encode(
                    "/product?slug=" + (slug == null ? "" : slug), StandardCharsets.UTF_8));
            return;
        }
        int productId = WebUtil.parseInt(req.getParameter("productId"), 0);
        String action = req.getParameter("action");
        if ("deleteReview".equals(action)) {
            reviewService.delete(user.getId(), productId);
            WebUtil.flash(req, "Đã xóa đánh giá của bạn.", "success");
            resp.sendRedirect(back + "#reviews");
            return;
        }
        if (!"review".equals(action)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        Review r = new Review();
        r.setUserId(user.getId());
        r.setProductId(productId);
        r.setRating(WebUtil.parseInt(req.getParameter("rating"), 0));
        r.setComment(req.getParameter("comment"));
        try {
            boolean updated = reviewService.save(r);
            WebUtil.flash(req, updated ? "Đã cập nhật đánh giá của bạn." : "Cảm ơn bạn đã đánh giá sản phẩm!", "success");
        } catch (ValidationException e) {
            if (e.getErrors().isEmpty()) {
                WebUtil.flash(req, e.getMessage(), "error");
            } else {
                // Lỗi nhập liệu: hiện lại trang, giữ sao và nhận xét đã nhập (param.rating, param.comment)
                req.setAttribute("errors", e.getErrors());
                doGet(req, resp);
                return;
            }
        }
        resp.sendRedirect(back + "#reviews");
    }
}
