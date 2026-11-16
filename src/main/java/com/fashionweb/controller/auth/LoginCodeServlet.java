package com.fashionweb.controller.auth;

import com.fashionweb.constant.AppConstant;
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
 * Bước nhập mã của khách (#74).
 * <p>GET /login/code : trang nhập mã. Chưa có email chờ trong session thì về /login.
 * JSP: /WEB-INF/views/auth/login-code.jsp. Request attribute: mode, email, devCode, resendWait, errors.
 * <p>POST /login/code (code): đúng thì đăng nhập (khách mới tự tạo tài khoản), về trang đã lưu hoặc /home.
 * <p>POST /login/code/resend : gửi lại mã (chặn trong 60 giây, tối đa 5 mã mỗi giờ).
 */
@WebServlet({"/login/code", "/login/code/resend"})
public class LoginCodeServlet extends HttpServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if ("/login/code/resend".equals(req.getServletPath())) {
            resp.sendRedirect(req.getContextPath() + "/login/code");
            return;
        }
        String email = pendingEmail(req);
        if (email == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        show(req, resp, email);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String email = pendingEmail(req);
        if (email == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        if ("/login/code/resend".equals(req.getServletPath())) {
            try {
                LoginSupport.rememberDevCode(req, authService.requestCode(email, AuthService.Flow.CUSTOMER));
                WebUtil.flash(req, "Đã gửi mã mới tới " + email + ".", "success");
            } catch (ValidationException e) {
                WebUtil.flash(req, e.getMessage(), "error");
            }
            resp.sendRedirect(req.getContextPath() + "/login/code");
            return;
        }

        User user;
        try {
            user = authService.verifyCode(email, LoginSupport.codeParam(req), AuthService.Flow.CUSTOMER);
        } catch (ValidationException e) {
            if (LoginSupport.wantsJson(req)) {
                LoginSupport.json(resp, LoginSupport.error(req, e.getErrors(), e.getMessage()));
                return;
            }
            req.setAttribute("errors", e.getErrors());
            show(req, resp, email);
            return;
        }
        String target = (String) req.getSession().getAttribute(AppConstant.SESSION_LOGIN_REDIRECT);
        LoginSupport.login(req, user);
        if (target == null || target.startsWith("/admin")) {
            target = "/home";
        }
        if (LoginSupport.wantsJson(req)) {
            LoginSupport.json(resp, LoginSupport.ok(req.getContextPath() + target));
            return;
        }
        resp.sendRedirect(req.getContextPath() + target);
    }

    private static String pendingEmail(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session == null ? null : (String) session.getAttribute(AppConstant.SESSION_LOGIN_EMAIL);
    }

    private void show(HttpServletRequest req, HttpServletResponse resp, String email) throws ServletException, IOException {
        req.setAttribute("mode", "customer");
        req.setAttribute("email", email);
        req.setAttribute("devCode", req.getSession().getAttribute(AppConstant.SESSION_DEV_CODE));
        req.setAttribute("resendWait", authService.resendWaitSeconds(email));
        req.getRequestDispatcher("/WEB-INF/views/auth/login-code.jsp").forward(req, resp);
    }
}
