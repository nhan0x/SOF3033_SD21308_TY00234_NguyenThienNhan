package com.fashionweb.controller.auth;

import com.fashionweb.constant.Role;
import com.fashionweb.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * GET /login : trang nhập email (bước 1 của đăng nhập bằng mã, #74). Đã đăng nhập thì về trang chủ (Admin về /admin).
 * Trang này vẫn giữ để AuthFilter, link cũ và trình duyệt tắt JS dùng được; khung tài khoản ở header (#75) gọi cùng endpoint.
 * <p>JSP: /WEB-INF/views/auth/login.jsp. Request attribute: authImage, mode ("customer"), errors.
 * <p>Form gửi POST /login/email (EmailLoginServlet). /register, /forgot-password, /reset-password cũ chuyển về đây.
 */
@WebServlet({"/login", "/register", "/forgot-password", "/reset-password"})
public class LoginServlet extends HttpServlet {

    static final String AUTH_IMAGE = "assets/images/banners/hero-01.jpg";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User current = LoginSupport.currentUser(req);
        if (current != null) {
            resp.sendRedirect(req.getContextPath() + (current.getRole() == Role.ADMIN ? "/admin" : "/home"));
            return;
        }
        if (!"/login".equals(req.getServletPath())) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        show(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Form mật khẩu cũ không còn: quay về trang nhập email
        resp.sendRedirect(req.getContextPath() + "/login");
    }

    static void show(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("authImage", AUTH_IMAGE);
        if (req.getAttribute("mode") == null) {
            req.setAttribute("mode", "customer");
        }
        req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, resp);
    }
}
