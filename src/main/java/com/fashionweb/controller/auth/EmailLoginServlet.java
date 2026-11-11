package com.fashionweb.controller.auth;

import com.fashionweb.constant.AppConstant;
import com.fashionweb.exception.ValidationException;
import com.fashionweb.service.auth.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * POST /login/email (email, redirect): bước 1 của đăng nhập bằng mã (#74).
 * <p>Email hợp lệ thì tạo mã, gửi mail, lưu email và trang cần quay lại vào session rồi chuyển sang /login/code.
 * Lỗi (sai định dạng, quá giới hạn, gửi mail lỗi, tài khoản khóa, email admin) thì hiện lại trang /login kèm lỗi dưới ô email.
 */
@WebServlet("/login/email")
public class EmailLoginServlet extends HttpServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.sendRedirect(req.getContextPath() + "/login");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String redirect = LoginSupport.safeRedirect(req.getParameter("redirect"));
        String email;
        String devCode;
        try {
            email = authService.normalizeEmail(req.getParameter("email"));
            devCode = authService.requestCode(email, AuthService.Flow.CUSTOMER);
        } catch (ValidationException e) {
            if (LoginSupport.wantsJson(req)) {
                LoginSupport.json(resp, LoginSupport.error(req, e.getErrors(), e.getMessage()));
                return;
            }
            req.setAttribute("errors", e.getErrors());
            LoginServlet.show(req, resp);
            return;
        }
        HttpSession session = req.getSession(true);
        session.setAttribute(AppConstant.SESSION_LOGIN_EMAIL, email);
        if (redirect != null) {
            session.setAttribute(AppConstant.SESSION_LOGIN_REDIRECT, redirect);
        } else {
            session.removeAttribute(AppConstant.SESSION_LOGIN_REDIRECT);
        }
        LoginSupport.rememberDevCode(req, devCode);
        if (LoginSupport.wantsJson(req)) {
            LoginSupport.json(resp, LoginSupport.ok(req.getContextPath() + "/login/code"));
            return;
        }
        resp.sendRedirect(req.getContextPath() + "/login/code");
    }
}
