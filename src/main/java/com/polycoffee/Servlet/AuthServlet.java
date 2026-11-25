package com.polycoffee.Servlet;

import com.polycoffee.DAO.UserDAO;
import com.polycoffee.Entity.User;
import com.polycoffee.Util.AuthUtil;
import com.polycoffee.Util.ParamUtil;
import com.polycoffee.Util.PasswordUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Đăng nhập, đăng xuất: GET/POST /auth/login và GET /auth/logout (BR-01, BR-02, BR-05, BR-06). */
@WebServlet("/auth/*")
public class AuthServlet extends HttpServlet {

    static final String LOGIN_VIEW = "/WEB-INF/views/auth/login.jsp";

    private final UserDAO userDAO;

    public AuthServlet() {
        this(new UserDAO());
    }

    AuthServlet(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        switch (path) {
            case "/logout":
                AuthUtil.clear(req);
                resp.sendRedirect(req.getContextPath() + "/auth/login");
                break;
            case "/login":
                if (AuthUtil.isAuthenticated(req)) {
                    resp.sendRedirect(req.getContextPath() + "/home");
                    return;
                }
                req.setAttribute("redirect", ParamUtil.getString(req, "redirect", ""));
                req.getRequestDispatcher(LOGIN_VIEW).forward(req, resp);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!"/login".equals(req.getPathInfo())) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String email = ParamUtil.getString(req, "email", "");
        String password = req.getParameter("password");
        String redirect = ParamUtil.getString(req, "redirect", "");

        String error = null;
        User user = null;
        if (email.isEmpty() || password == null || password.isEmpty()) {
            error = "Vui lòng nhập email và mật khẩu";
        } else {
            user = userDAO.findByEmail(email);
            if (user == null || !PasswordUtil.matches(password, user.getPassword())) {
                // Không tiết lộ email hay mật khẩu sai
                error = "Email hoặc mật khẩu không đúng";
            } else if (!user.isEnabled()) {
                error = "Tài khoản đã bị khóa";
            }
        }
        if (error != null) {
            req.setAttribute("error", error);
            req.setAttribute("email", email);
            req.setAttribute("redirect", redirect);
            req.getRequestDispatcher(LOGIN_VIEW).forward(req, resp);
            return;
        }
        AuthUtil.setUser(req, user);
        resp.sendRedirect(req.getContextPath() + safeTarget(redirect));
    }

    /**
     * Chỉ chuyển hướng nội bộ (BR-05). Loại "//host" và "/\host" vì trình duyệt hiểu là URL tuyệt đối.
     */
    static String safeTarget(String redirect) {
        boolean internal = redirect.startsWith("/")
                && !redirect.startsWith("//") && !redirect.startsWith("/\\");
        return internal ? redirect : "/home";
    }
}
