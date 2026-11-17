package com.fashionweb.controller.shop;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * GET /about : trang giới thiệu thương hiệu (tĩnh, Guest + Customer).
 * <p>
 * JSP: /WEB-INF/views/shop/about.jsp. Không có tham số.
 * <p>
 * Thuộc tính: aboutImage (ảnh câu chuyện thương hiệu có sẵn trong assets/images/banners).
 */
@WebServlet("/about")
public class AboutServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("aboutCover", "assets/images/banners/hero-02.jpg");
        req.setAttribute("aboutImage", "assets/images/banners/story-01.jpg");
        req.getRequestDispatcher("/WEB-INF/views/shop/about.jsp").forward(req, resp);
    }
}
