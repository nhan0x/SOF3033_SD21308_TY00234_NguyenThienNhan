package com.fashionweb.controller.auth;

import com.fashionweb.exception.ValidationException;
import com.fashionweb.model.User;
import com.fashionweb.service.auth.AuthService;
import com.fashionweb.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Đăng ký tài khoản khách.
 * <p>GET /register : form đăng ký. Đã đăng nhập thì về /home.
 * <p>POST /register (email, fullName, password, confirm, redirect): tạo tài khoản CUSTOMER, đăng nhập luôn,
 * về trang redirect hoặc /home. Lỗi thì hiện lại form kèm lỗi từng ô.
 * <p>JSP: /WEB-INF/views/auth/register.jsp. Request attribute: errors.
 */
@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (LoginSupport.currentUser(req) != null) {
            resp.sendRedirect(req.getContextPath() + "/home");
            return;
        }
        show(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user;
        try {
            user = authService.register(req.getParameter("email"), req.getParameter("fullName"),
                    req.getParameter("password"), req.getParameter("confirm"));
        } catch (ValidationException e) {
            req.setAttribute("errors", e.getErrors());
            show(req, resp);
            return;
        }
        LoginSupport.login(req, user);
        WebUtil.flash(req, "Đã tạo tài khoản. Chào mừng bạn đến với Saltair!", "success");
        String target = LoginSupport.safeRedirect(req.getParameter("redirect"));
        if (target == null || target.startsWith("/admin")) {
            target = "/home";
        }
        resp.sendRedirect(req.getContextPath() + target);
    }

    private void show(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/auth/register.jsp").forward(req, resp);
    }
}
