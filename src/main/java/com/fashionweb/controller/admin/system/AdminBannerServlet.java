package com.fashionweb.controller.admin.system;

import com.fashionweb.exception.ValidationException;
import com.fashionweb.model.Banner;
import com.fashionweb.service.shop.BannerService;
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
 * GET /admin/banners : danh sách banner cho Admin (cả banner đang tắt, sắp theo sortOrder).
 * JSP: /WEB-INF/views/admin/system/banners.jsp
 * <p>
 * POST (multipart):
 * <ul>
 *   <li>action=save (id rỗng = thêm; title, linkUrl, sortOrder, imageFile, active): ảnh JPG/PNG/WebP tối đa 2MB lưu vào
 *       assets/images/banners; thêm mới bắt buộc có ảnh, sửa không chọn ảnh thì giữ ảnh cũ. Lỗi thì hộp thoại tự mở kèm lỗi.</li>
 *   <li>action=toggle (id, active): công tắc bật/tắt hiển thị (có tham số active = bật).</li>
 *   <li>action=delete (id): xóa hẳn.</li>
 * </ul>
 * Trang chủ đọc banner đang bật theo sortOrder nên thay đổi phản ánh ngay. AdminFilter bảo vệ /admin/*.
 */
@WebServlet("/admin/banners")
@MultipartConfig(maxFileSize = 5 * 1024 * 1024, maxRequestSize = 6 * 1024 * 1024)
public class AdminBannerServlet extends HttpServlet {

    private static final String IMAGE_DIR = "assets/images/banners";

    private final BannerService bannerService = new BannerService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("banners", bannerService.findAll());
        req.getRequestDispatcher("/WEB-INF/views/admin/system/banners.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action;
        try {
            action = req.getParameter("action");
        } catch (IllegalStateException tooLarge) {
            WebUtil.flash(req, "Ảnh quá lớn, chỉ nhận ảnh tối đa 2MB.", "error");
            resp.sendRedirect(req.getContextPath() + "/admin/banners");
            return;
        }
        int id = WebUtil.parseInt(req.getParameter("id"), 0);
        try {
            switch (action == null ? "" : action) {
                case "save" -> {
                    save(req, resp, id);
                    return;
                }
                case "toggle" -> {
                    boolean on = req.getParameter("active") != null;
                    bannerService.setActive(id, on);
                    WebUtil.flash(req, on ? "Đã bật banner trên trang chủ." : "Đã tắt banner khỏi trang chủ.", "success");
                }
                case "delete" -> {
                    bannerService.delete(id);
                    WebUtil.flash(req, "Đã xóa banner.", "success");
                }
                default -> {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
            }
        } catch (ValidationException e) {
            WebUtil.flash(req, e.getMessage(), "error");
        }
        resp.sendRedirect(req.getContextPath() + "/admin/banners");
    }

    private void save(HttpServletRequest req, HttpServletResponse resp, int id) throws ServletException, IOException {
        Banner existing = id > 0 ? bannerService.findById(id) : null;
        Banner b = new Banner();
        b.setId(id);
        b.setTitle(req.getParameter("title"));
        b.setLinkUrl(req.getParameter("linkUrl"));
        b.setSortOrder(WebUtil.parseInt(req.getParameter("sortOrder"), 0));
        b.setActive("true".equals(req.getParameter("active")));
        b.setImage(existing == null ? null : existing.getImage());
        Part image = req.getPart("imageFile");
        String stored = null;
        try {
            if (ImageUploadUtil.hasFile(image)) {
                stored = ImageUploadUtil.store(getServletContext(), image, "imageFile", IMAGE_DIR, b.getTitle());
                b.setImage(stored);
            }
            bannerService.save(b);
            WebUtil.flash(req, (id == 0 ? "Đã thêm banner \"" : "Đã lưu banner \"") + b.getTitle() + "\".", "success");
            resp.sendRedirect(req.getContextPath() + "/admin/banners");
        } catch (ValidationException e) {
            ImageUploadUtil.deleteQuietly(getServletContext(), stored);
            if (e.getErrors().isEmpty()) {
                WebUtil.flash(req, e.getMessage(), "error");
                resp.sendRedirect(req.getContextPath() + "/admin/banners");
                return;
            }
            req.setAttribute("errors", e.getErrors());
            doGet(req, resp);
        }
    }
}
