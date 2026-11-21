package com.fashionweb.controller.shop;

import com.fashionweb.service.shop.BlogService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * GET /blog : danh sách bài viết đang hiển thị (is_published = 1), mới nhất trước (Guest + Customer).
 * JSP: /WEB-INF/views/shop/blog-list.jsp. Thuộc tính: blogs.
 */
@WebServlet("/blog")
public class BlogServlet extends HttpServlet {

    private final BlogService blogService = new BlogService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("blogs", blogService.listPublished());
        req.getRequestDispatcher("/WEB-INF/views/shop/blog-list.jsp").forward(req, resp);
    }
}
