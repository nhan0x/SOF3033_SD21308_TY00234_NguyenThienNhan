package com.fashionweb.controller.admin.system;

import com.fashionweb.constant.AppConstant;
import com.fashionweb.controller.admin.AdminOpsParams;
import com.fashionweb.exception.ValidationException;
import com.fashionweb.model.Blog;
import com.fashionweb.service.shop.BlogService;
import com.fashionweb.util.ImageUploadUtil;
import com.fashionweb.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;

/**
 * GET /admin/blogs?q=&amp;page= : danh sách bài viết cho Admin (cả bài đang ẩn), tìm theo tiêu đề, phân trang.
 * JSP: /WEB-INF/views/admin/system/blogs.jsp
 * <p>
 * POST /admin/blogs (multipart):
 * <ul>
 *   <li>action=save (id, title, slug, summary, content, coverImage, published): thêm hoặc sửa. Ảnh bìa JPG/PNG/WebP
 *       tối đa 2MB lưu vào assets/images/blogs, không chọn thì giữ ảnh cũ. Lỗi thì hiện lại form kèm errors.</li>
 *   <li>action=togglePublish (id): đảo đăng/ẩn. action=delete (id): xóa bài.</li>
 * </ul>
 * Xử lý xong flash rồi redirect /admin/blogs (PRG). AdminFilter bảo vệ /admin/*.
 */
@WebServlet("/admin/blogs")
@MultipartConfig(maxFileSize = 5 * 1024 * 1024, maxRequestSize = 6 * 1024 * 1024)
public class AdminBlogServlet extends HttpServlet {

    private static final String IMAGE_DIR = "assets/images/blogs";

    private final BlogService blogService = new BlogService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String q = AdminOpsParams.text(req, "q");
        req.setAttribute("blogPage", blogService.search(q, AdminOpsParams.page(req), AppConstant.ADMIN_PAGE_SIZE));
        req.setAttribute("q", q);
        req.getRequestDispatcher("/WEB-INF/views/admin/system/blogs.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action;
        try {
            action = req.getParameter("action");
        } catch (IllegalStateException tooLarge) {
            WebUtil.flash(req, "Ảnh quá lớn, chỉ nhận ảnh tối đa 2MB.", "error");
            redirect(req, resp);
            return;
        }
        int id = WebUtil.parseInt(req.getParameter("id"), 0);
        try {
            switch (action == null ? "" : action) {
                case "save" -> {
                    save(req, resp, id);
                    return;
                }
                case "togglePublish" -> WebUtil.flash(req, blogService.togglePublish(id)
                        ? "Đã đăng bài viết." : "Đã ẩn bài viết khỏi trang khách.", "success");
                case "delete" -> {
                    blogService.delete(id);
                    WebUtil.flash(req, "Đã xóa bài viết.", "success");
                }
                default -> {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
            }
        } catch (ValidationException e) {
            WebUtil.flash(req, e.getMessage(), "error");
        }
        redirect(req, resp);
    }

    private void save(HttpServletRequest req, HttpServletResponse resp, int id) throws ServletException, IOException {
        Blog existing = id > 0 ? blogService.findById(id) : null;
        if (id > 0 && existing == null) {
            WebUtil.flash(req, "Bài viết không tồn tại.", "error");
            redirect(req, resp);
            return;
        }
        Blog b = new Blog();
        b.setId(id);
        b.setAuthorId(existing != null ? existing.getAuthorId() : WebUtil.currentUser(req).getId());
        b.setTitle(req.getParameter("title"));
        b.setSlug(req.getParameter("slug"));
        b.setSummary(req.getParameter("summary"));
        b.setContent(req.getParameter("content"));
        b.setPublished("true".equals(req.getParameter("published")));
        b.setCoverImage(existing == null ? null : existing.getCoverImage());

        Part cover = req.getPart("coverImage");
        String stored = null;
        try {
            if (ImageUploadUtil.hasFile(cover)) {
                String base = b.getSlug() == null || b.getSlug().isBlank() ? b.getTitle() : b.getSlug();
                stored = ImageUploadUtil.store(getServletContext(), cover, "coverImage", IMAGE_DIR, base);
                b.setCoverImage(stored);
            }
            blogService.save(b);
            WebUtil.flash(req, (id == 0 ? "Đã thêm bài viết \"" : "Đã lưu bài viết \"") + b.getTitle() + "\".", "success");
            redirect(req, resp);
        } catch (ValidationException e) {
            ImageUploadUtil.deleteQuietly(getServletContext(), stored);
            req.setAttribute("errors", e.getErrors());
            req.setAttribute("blog", existing);
            req.setAttribute("mode", existing == null ? "create" : "edit");
            req.getRequestDispatcher(AdminBlogFormServlet.VIEW).forward(req, resp);
        }
    }

    private static void redirect(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.sendRedirect(req.getContextPath() + "/admin/blogs");
    }
}
