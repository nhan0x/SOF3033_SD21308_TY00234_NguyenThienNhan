package com.fashionweb.controller.auth;

import com.fashionweb.constant.AppConstant;
import com.fashionweb.constant.Role;
import com.fashionweb.exception.ValidationException;
import com.fashionweb.model.User;
import com.fashionweb.service.auth.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Đăng nhập bằng email + mật khẩu, một form cho cả khách và Admin.
 * <p>GET /login : trang đăng nhập. Đã đăng nhập thì về trang chủ (Admin về /admin).
 * <p>POST /login (email, password, redirect): đúng thì đăng nhập. Admin vào /admin (hoặc trang /admin/... đang xem dở,
 * phiên 30 phút, gửi mail báo đăng nhập); khách về trang redirect hoặc /home. Sai thì hiện lại form.
 * <p>/admin/login (đường dẫn cũ) chuyển về /login.
 * <p>JSP: /WEB-INF/views/auth/login.jsp. Request attribute: errors.
 */
@WebServlet({"/login", "/admin/login"})
public class LoginServlet extends HttpServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if ("/admin/login".equals(req.getServletPath())) {
            String r = req.getParameter("redirect");
            resp.sendRedirect(req.getContextPath() + "/login" + (r == null || r.isBlank() ? "" : "?redirect=" + URLEncoder.encode(r, StandardCharsets.UTF_8)));
            return;
        }
        User current = LoginSupport.currentUser(req);
        if (current != null) {
            resp.sendRedirect(req.getContextPath() + (current.getRole() == Role.ADMIN ? "/admin" : "/home"));
            return;
        }
        show(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user;
        try {
            user = authService.login(req.getParameter("email"), req.getParameter("password"));
        } catch (ValidationException e) {
            req.setAttribute("errors", e.getErrors());
            show(req, resp);
            return;
        }
        HttpSession session = LoginSupport.login(req, user);
        String target = LoginSupport.safeRedirect(req.getParameter("redirect"));
        if (user.getRole() == Role.ADMIN) {
            session.setMaxInactiveInterval(AppConstant.ADMIN_SESSION_SECONDS);
            authService.notifyAdminLogin(user, req.getRemoteAddr(), req.getHeader("User-Agent"));
            if (target == null || !target.startsWith("/admin")) {
                target = "/admin";
            }
        } else if (target == null || target.startsWith("/admin")) {
            target = "/home";
        }
        resp.sendRedirect(req.getContextPath() + target);
    }

    static void show(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, resp);
    }
}
