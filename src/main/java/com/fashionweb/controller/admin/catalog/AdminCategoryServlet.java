package com.fashionweb.controller.admin.catalog;

import com.fashionweb.exception.ValidationException;
import com.fashionweb.listener.AppStartupListener;
import com.fashionweb.model.Category;
import com.fashionweb.service.shop.CategoryService;
import com.fashionweb.util.ImageUploadUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.util.Map;

/**
 * GET /admin/categories : danh sách danh mục (thêm và sửa bằng hộp thoại ngay trên trang).
 * JSP: /WEB-INF/views/admin/catalog/categories.jsp
 * Thuộc tính: categories (kèm productCount). Khi lưu lỗi: errors, openDialog ("category-dialog").
 * AdminFilter bảo vệ /admin/*.
 * <p>POST (multipart vì có imageFile):
 * <ul>
 *   <li>action=save: id rỗng = thêm, có id = sửa. Slug để trống thì tự sinh từ tên. imageFile JPG/PNG/WebP tối đa 2MB (Cloudinary khi có cloudinary.properties, xem ImageUploadUtil),
 *       không chọn thì giữ ảnh cũ. Ảnh lưu vào assets/images/categories/ của ứng dụng đang chạy.</li>
 *   <li>action=delete: xóa mềm. Danh mục còn sản phẩm đang bán thì bị chặn (xem CategoryService).</li>
 * </ul>
 */
@WebServlet("/admin/categories")
@MultipartConfig(maxFileSize = 5 * 1024 * 1024, maxRequestSize = 6 * 1024 * 1024)
public class AdminCategoryServlet extends HttpServlet {

    private static final String IMAGE_DIR = "assets/images/categories";

    private final CategoryService categoryService = new CategoryService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        show(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action;
        try {
            action = req.getParameter("action");
        } catch (IllegalStateException tooLarge) {
            flash(req, "Ảnh quá lớn, chỉ nhận ảnh tối đa 2MB.", "error");
            redirect(req, resp);
            return;
        }

        if ("save".equals(action)) {
            save(req, resp);
        } else if ("delete".equals(action)) {
            try {
                categoryService.softDelete(parseId(req.getParameter("id")));
                AppStartupListener.refreshNavCategories(getServletContext());
                flash(req, "Đã chuyển danh mục vào Thùng rác.", "success");
            } catch (ValidationException e) {
                flash(req, e.getMessage(), "error");
            }
            redirect(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void save(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Category c = new Category();
        c.setId(parseId(req.getParameter("id")));
        c.setName(req.getParameter("name"));
        c.setSlug(req.getParameter("slug"));

        Part image = req.getPart("imageFile");
        String stored = null;
        try {
            boolean isNew = c.getId() == 0;
            if (ImageUploadUtil.hasFile(image)) {
                String base = c.getSlug() == null || c.getSlug().isBlank() ? c.getName() : c.getSlug();
                stored = ImageUploadUtil.store(getServletContext(), image, "imageFile", IMAGE_DIR, base);
                c.setImage(stored);
            }
            categoryService.save(c);
            AppStartupListener.refreshNavCategories(getServletContext());
            flash(req, isNew ? "Đã thêm danh mục \"" + c.getName() + "\"." : "Đã lưu danh mục \"" + c.getName() + "\".", "success");
            redirect(req, resp);
        } catch (ValidationException e) {
            ImageUploadUtil.deleteQuietly(getServletContext(), stored);
            showWithErrors(req, resp, e.getErrors());
        }
    }

    private void show(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("categories", categoryService.findAll());
        req.getRequestDispatcher("/WEB-INF/views/admin/catalog/categories.jsp").forward(req, resp);
    }

    private void showWithErrors(HttpServletRequest req, HttpServletResponse resp, Map<String, String> errors)
            throws ServletException, IOException {
        req.setAttribute("errors", errors);
        req.setAttribute("openDialog", "category-dialog");
        show(req, resp);
    }

    private static int parseId(String s) {
        try {
            return s == null || s.isBlank() ? 0 : Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static void flash(HttpServletRequest req, String message, String type) {
        req.getSession().setAttribute("flashMessage", message);
        req.getSession().setAttribute("flashType", type);
    }

    private static void redirect(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.sendRedirect(req.getContextPath() + "/admin/categories");
    }
}
