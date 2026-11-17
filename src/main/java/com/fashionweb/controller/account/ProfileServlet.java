package com.fashionweb.controller.account;

import com.fashionweb.constant.AppConstant;
import com.fashionweb.exception.ValidationException;
import com.fashionweb.model.User;
import com.fashionweb.service.account.AccountService;
import com.fashionweb.service.auth.AuthService;
import com.fashionweb.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * GET /profile : thông tin tài khoản (Customer, AuthFilter bắt buộc đăng nhập).
 * <p>JSP: /WEB-INF/views/account/profile.jsp. Request attributes: user, addresses, errors.
 * <p>POST action=updateProfile (fullName, phone; email và role không sửa được).
 * <p>POST action=changePassword (currentPassword, newPassword, confirm).
 * Xong redirect kèm flash, lỗi thì hiện lại trang với hộp thoại tương ứng mở sẵn.
 */
@WebServlet("/profile")
public class ProfileServlet extends HttpServlet {

    private final AccountService accountService = new AccountService();
    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        show(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = WebUtil.currentUser(req);
        String action = req.getParameter("action");
        try {
            if ("updateProfile".equals(action)) {
                accountService.updateProfile(user.getId(), req.getParameter("fullName"), req.getParameter("phone"));
                // Cập nhật bản User trong session để đầu trang hiện tên mới
                req.getSession().setAttribute(AppConstant.SESSION_USER, accountService.findUser(user.getId()));
                WebUtil.flash(req, "Đã lưu thông tin tài khoản.", "success");
            } else if ("changePassword".equals(action)) {
                authService.changePassword(user.getId(), req.getParameter("currentPassword"),
                        req.getParameter("newPassword"), req.getParameter("confirm"));
                WebUtil.flash(req, "Đã đổi mật khẩu.", "success");
            } else {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
        } catch (ValidationException e) {
            req.setAttribute("errors", e.getErrors());
            show(req, resp);
            return;
        }
        resp.sendRedirect(req.getContextPath() + "/profile");
    }

    private void show(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int userId = WebUtil.currentUser(req).getId();
        req.setAttribute("user", accountService.findUser(userId));
        req.setAttribute("addresses", accountService.findAddresses(userId));
        req.getRequestDispatcher("/WEB-INF/views/account/profile.jsp").forward(req, resp);
    }
}
