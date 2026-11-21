package poly.coffee.servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import poly.coffee.dao.UserDAO;
import poly.coffee.entity.User;
import poly.coffee.util.AuthUtil;
import poly.coffee.util.ParamUtil;
import poly.coffee.util.PasswordUtil;

/** /auth/login (GET form, POST xu ly) va /auth/logout */
@WebServlet("/auth/*")
public class AuthServlet extends HttpServlet {
    private final UserDAO userDAO = new UserDAO();

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
                req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, resp);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String email = ParamUtil.getString(req, "email", "");
        String password = ParamUtil.getString(req, "password", "");
        String redirect = ParamUtil.getString(req, "redirect", "");

        String error = null;
        User user = null;
        if (email.isEmpty() || password.isEmpty()) {
            error = "Vui lòng nhập email và mật khẩu";
        } else {
            user = userDAO.findByEmail(email);
            if (user == null || !user.getPassword().equals(PasswordUtil.hash(password))) {
                error = "Email hoặc mật khẩu không đúng";
            } else if (!user.isEnabled()) {
                error = "Tài khoản đã bị khóa";
            }
        }
        if (error != null) {
            req.setAttribute("error", error);
            req.setAttribute("email", email);
            req.setAttribute("redirect", redirect);
            req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, resp);
            return;
        }
        AuthUtil.setUser(req, user);
        // chi cho phep chuyen huong noi bo
        String target = redirect.startsWith("/") && !redirect.startsWith("//") ? redirect : "/home";
        resp.sendRedirect(req.getContextPath() + target);
    }
}
