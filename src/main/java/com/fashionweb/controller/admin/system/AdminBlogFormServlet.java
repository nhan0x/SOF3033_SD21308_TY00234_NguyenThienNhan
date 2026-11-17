package com.fashionweb.controller.admin.system;

import com.fashionweb.controller.admin.AdminOpsParams;
import com.fashionweb.model.Blog;
import com.fashionweb.service.shop.BlogService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * GET /admin/blogs/form?id= : form thêm (không có id) hoặc sửa (có id) bài viết.
 * JSP: /WEB-INF/views/admin/system/blog-form.jsp
 * Tham số: id (tùy chọn). id sai định dạng hoặc không tìm thấy thì trả 404.
 * Form gửi POST /admin/blogs (AdminBlogServlet) với action=save; khi lỗi, AdminBlogServlet forward lại trang này.
 */
@WebServlet("/admin/blogs/form")
public class AdminBlogFormServlet extends HttpServlet {

    static final String VIEW = "/WEB-INF/views/admin/system/blog-form.jsp";

    private final BlogService blogService = new BlogService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String rawId = AdminOpsParams.text(req, "id");
        Blog blog = null;
        if (!rawId.isEmpty()) {
            Integer id = AdminOpsParams.positiveInt(rawId);
            blog = id == null ? null : blogService.findById(id);
            if (blog == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        }
        req.setAttribute("blog", blog);
        req.setAttribute("mode", blog == null ? "create" : "edit");
        req.getRequestDispatcher(VIEW).forward(req, resp);
    }
}
