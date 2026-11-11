package com.fashionweb.controller.auth;

import com.fashionweb.constant.AppConstant;
import com.fashionweb.constant.Role;
import com.fashionweb.exception.ValidationException;
import com.fashionweb.model.User;
import com.fashionweb.service.auth.AuthService;
import com.fashionweb.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Đăng nhập trang quản trị bằng email + mã 6 số (#74, phương án 1). AdminFilter không chặn các đường dẫn này.
 * <p>GET /admin/login : trang nhập email. POST /admin/login (email, redirect): luôn báo cùng một câu dù email có phải
 * admin hay không; chỉ admin đang hoạt động mới nhận mã (hạn 5 phút, sai 3 lần thì hủy, tối đa 3 mã mỗi giờ).
 * <p>GET /admin/login/code : trang nhập mã. POST /admin/login/code (code) đúng thì đăng nhập, phiên 30 phút,
 * gửi mail báo đăng nhập (thời gian, IP, trình duyệt). POST action=resend: gửi lại mã.
 */
@WebServlet({"/admin/login", "/admin/login/code"})
public class AdminLoginServlet extends HttpServlet {

    private static final String SENT_MESSAGE = "Nếu email thuộc tài khoản quản trị, mã đăng nhập đã được gửi tới hộp thư.";

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User current = LoginSupport.currentUser(req);
        if (current != null && current.getRole() == Role.ADMIN) {
            resp.sendRedirect(req.getContextPath() + "/admin");
            return;
        }
        if (isCodePath(req)) {
            String email = pendingEmail(req);
            if (email == null) {
                resp.sendRedirect(req.getContextPath() + "/admin/login");
                return;
            }
            showCode(req, resp, email);
            return;
        }
        showEmail(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!isCodePath(req)) {
            requestCode(req, resp);
            return;
        }
        String email = pendingEmail(req);
        if (email == null) {
            resp.sendRedirect(req.getContextPath() + "/admin/login");
            return;
        }
        if ("resend".equals(req.getParameter("action"))) {
            try {
                LoginSupport.rememberDevCode(req, authService.requestCode(email, AuthService.Flow.ADMIN));
                WebUtil.flash(req, SENT_MESSAGE, "success");
            } catch (ValidationException e) {
                WebUtil.flash(req, e.getMessage(), "error");
            }
            resp.sendRedirect(req.getContextPath() + "/admin/login/code");
            return;
        }

        User user;
        try {
            user = authService.verifyCode(email, LoginSupport.codeParam(req), AuthService.Flow.ADMIN);
        } catch (ValidationException e) {
            if (LoginSupport.wantsJson(req)) {
                LoginSupport.json(resp, LoginSupport.error(req, e.getErrors(), e.getMessage()));
                return;
            }
            req.setAttribute("errors", e.getErrors());
            showCode(req, resp, email);
            return;
        }
        String target = (String) req.getSession().getAttribute(AppConstant.SESSION_ADMIN_LOGIN_REDIRECT);
        HttpSession session = LoginSupport.login(req, user);
        session.setMaxInactiveInterval(AppConstant.ADMIN_SESSION_SECONDS);
        authService.notifyAdminLogin(user, req.getRemoteAddr(), req.getHeader("User-Agent"));
        if (target == null || !target.startsWith("/admin")) {
            target = "/admin";
        }
        if (LoginSupport.wantsJson(req)) {
            LoginSupport.json(resp, LoginSupport.ok(req.getContextPath() + target));
            return;
        }
        resp.sendRedirect(req.getContextPath() + target);
    }

    private void requestCode(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String email;
        String devCode;
        try {
            email = authService.normalizeEmail(req.getParameter("email"));
            devCode = authService.requestCode(email, AuthService.Flow.ADMIN);
        } catch (ValidationException e) {
            req.setAttribute("errors", e.getErrors());
            showEmail(req, resp);
            return;
        }
        HttpSession session = req.getSession(true);
        session.setAttribute(AppConstant.SESSION_ADMIN_LOGIN_EMAIL, email);
        String redirect = LoginSupport.safeRedirect(req.getParameter("redirect"));
        if (redirect != null && redirect.startsWith("/admin")) {
            session.setAttribute(AppConstant.SESSION_ADMIN_LOGIN_REDIRECT, redirect);
        } else {
            session.removeAttribute(AppConstant.SESSION_ADMIN_LOGIN_REDIRECT);
        }
        LoginSupport.rememberDevCode(req, devCode);
        WebUtil.flash(req, SENT_MESSAGE, "success");
        resp.sendRedirect(req.getContextPath() + "/admin/login/code");
    }

    private static boolean isCodePath(HttpServletRequest req) {
        return "/admin/login/code".equals(req.getServletPath());
    }

    private static String pendingEmail(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session == null ? null : (String) session.getAttribute(AppConstant.SESSION_ADMIN_LOGIN_EMAIL);
    }

    private void showEmail(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("mode", "admin");
        LoginServlet.show(req, resp);
    }

    private void showCode(HttpServletRequest req, HttpServletResponse resp, String email) throws ServletException, IOException {
        req.setAttribute("mode", "admin");
        req.setAttribute("email", email);
        req.setAttribute("devCode", req.getSession().getAttribute(AppConstant.SESSION_DEV_CODE));
        req.setAttribute("resendWait", authService.resendWaitSeconds(email));
        req.setAttribute("authImage", LoginServlet.AUTH_IMAGE);
        req.getRequestDispatcher("/WEB-INF/views/auth/login-code.jsp").forward(req, resp);
    }
}
