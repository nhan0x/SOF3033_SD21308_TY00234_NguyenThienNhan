package com.fashionweb.controller.shop;

import com.fashionweb.model.Blog;
import com.fashionweb.service.shop.BlogService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * GET /blog/detail?slug=... : chi tiết một bài viết (Guest + Customer).
 * JSP: /WEB-INF/views/shop/blog-detail.jsp
 * Tham số: slug (bắt buộc). Thiếu, không tìm thấy hoặc bài đang ẩn thì trả 404.
 * Nội dung hiển thị bằng c:out theo từng đoạn nên không chèn được HTML hay script.
 */
@WebServlet("/blog/detail")
public class BlogDetailServlet extends HttpServlet {

    private static final int OTHER_LIMIT = 3;

    private final BlogService blogService = new BlogService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Blog blog = blogService.findPublishedBySlug(req.getParameter("slug"));
        if (blog == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        req.setAttribute("blog", blog);
        req.setAttribute("otherBlogs", blogService.listOthers(blog.getId(), OTHER_LIMIT));
        // Tập ký tự phân tách đoạn cho fn:split (mỗi ký tự là một dấu phân tách, token rỗng bị bỏ)
        req.setAttribute("lineBreaks", "\r\n");
        req.getRequestDispatcher("/WEB-INF/views/shop/blog-detail.jsp").forward(req, resp);
    }
}
