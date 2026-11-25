package com.polycoffee.Servlet;

import com.polycoffee.DAO.UserDAO;
import com.polycoffee.Entity.User;
import com.polycoffee.Util.AuthUtil;
import com.polycoffee.Util.MailUtil;
import com.polycoffee.Util.ParamUtil;
import com.polycoffee.Util.PasswordUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Quản lý nhân viên (BR-15 đến BR-20).
 * <pre>
 *   GET  /manager/staff               danh sách
 *   GET  /manager/staff/new | /edit?id=   form
 *   POST /manager/staff/save          thêm hoặc cập nhật
 *   POST /manager/staff/delete        xóa
 *   POST /manager/staff/reset-password  cấp lại mật khẩu và gửi email
 * </pre>
 */
@WebServlet("/manager/staff/*")
public class StaffServlet extends HttpServlet {

    static final String LIST = "/WEB-INF/views/manager/staff/list.jsp";
    static final String FORM = "/WEB-INF/views/manager/staff/form.jsp";
    static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    static final int PASSWORD_MIN = 6;
    static final int NAME_MAX = 100;
    static final int EMAIL_MAX = 100;
    static final int NEW_PASSWORD_LENGTH = 8;

    private final UserDAO dao;

    public StaffServlet() {
        this(new UserDAO());
    }

    StaffServlet(UserDAO dao) {
        this.dao = dao;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        switch (path) {
            case "":
            case "/":
                req.setAttribute("users", dao.findAll());
                req.getRequestDispatcher(LIST).forward(req, resp);
                break;
            case "/new":
                User fresh = new User();
                fresh.setEnabled(true);
                req.setAttribute("staff", fresh);
                req.getRequestDispatcher(FORM).forward(req, resp);
                break;
            case "/edit":
                User u = dao.findById(ParamUtil.getInt(req, "id", 0));
                if (u == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy nhân viên");
                    return;
                }
                u.setPassword(null);
                req.setAttribute("staff", u);
                req.getRequestDispatcher(FORM).forward(req, resp);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        switch (path) {
            case "/save":
                save(req, resp);
                break;
            case "/delete":
                delete(req, resp);
                break;
            case "/reset-password":
                resetPassword(req, resp);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void save(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User u = new User();
        u.setId(ParamUtil.getInt(req, "id", 0));
        u.setFullname(ParamUtil.getString(req, "fullname", ""));
        u.setEmail(ParamUtil.getString(req, "email", ""));
        u.setManager(ParamUtil.getBoolean(req, "manager", false));
        u.setEnabled(ParamUtil.getBoolean(req, "enabled", false));
        boolean isNew = u.getId() == 0;
        String password = req.getParameter("password") == null ? "" : req.getParameter("password");

        String error = null;
        if (u.getFullname().isEmpty()) {
            error = "Họ tên không được để trống";
        } else if (u.getFullname().length() > NAME_MAX) {
            error = "Họ tên tối đa " + NAME_MAX + " ký tự";
        } else if (u.getEmail().length() > EMAIL_MAX || !EMAIL.matcher(u.getEmail()).matches()) {
            error = "Email không hợp lệ";
        } else if (dao.existsByEmail(u.getEmail(), u.getId())) {
            error = "Email đã được sử dụng";
        } else if (isNew && password.length() < PASSWORD_MIN) {
            error = "Mật khẩu tối thiểu " + PASSWORD_MIN + " ký tự";
        } else if (!isNew && dao.findById(u.getId()) == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy nhân viên");
            return;
        }
        if (error != null) {
            req.setAttribute("error", error);
            req.setAttribute("staff", u);
            req.getRequestDispatcher(FORM).forward(req, resp);
            return;
        }
        if (isNew) {
            u.setPassword(PasswordUtil.hash(password));
            dao.create(u);
        } else {
            dao.update(u);
        }
        req.getSession().setAttribute("flashOk", "Đã lưu nhân viên");
        resp.sendRedirect(req.getContextPath() + "/manager/staff");
    }

    private void delete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int id = ParamUtil.getInt(req, "id", 0);
        User current = AuthUtil.getUser(req);
        if (current != null && current.getId() == id) {
            flashError(req, "Không thể tự xóa tài khoản đang đăng nhập");
        } else if (dao.findById(id) == null) {
            flashError(req, "Không tìm thấy nhân viên");
        } else if (dao.countBills(id) > 0) {
            flashError(req, "Không thể xóa: nhân viên đã có dữ liệu (hãy khóa tài khoản)");
        } else {
            dao.deleteById(id);
            req.getSession().setAttribute("flashOk", "Đã xóa nhân viên");
        }
        resp.sendRedirect(req.getContextPath() + "/manager/staff");
    }

    /**
     * Cấp lại mật khẩu (BR-19): đổi sang mật khẩu ngẫu nhiên rồi gửi email.
     * Nếu gửi lỗi thì khôi phục mật khẩu cũ. Không ghi mật khẩu hay cấu hình mail ra log (BR-20).
     */
    private void resetPassword(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User u = dao.findById(ParamUtil.getInt(req, "id", 0));
        if (u == null) {
            flashError(req, "Không tìm thấy nhân viên");
            resp.sendRedirect(req.getContextPath() + "/manager/staff");
            return;
        }
        String oldHash = u.getPassword();
        String newPassword = PasswordUtil.random(NEW_PASSWORD_LENGTH);
        dao.updatePassword(u.getId(), PasswordUtil.hash(newPassword));
        try {
            MailUtil.send(u.getEmail(), "PolyCoffee: mật khẩu mới của bạn",
                    "Xin chào " + u.getFullname() + ",\n\nMật khẩu mới của bạn là: " + newPassword
                            + "\nHãy đăng nhập và giữ kín mật khẩu này.\n\nPolyCoffee");
            req.getSession().setAttribute("flashOk", "Đã gửi mật khẩu mới tới " + u.getEmail());
        } catch (Exception e) {
            dao.updatePassword(u.getId(), oldHash);
            getServletContext().log("Gửi email cấp lại mật khẩu thất bại: " + e.getClass().getSimpleName());
            flashError(req, "Gửi email thất bại, mật khẩu chưa bị thay đổi");
        }
        resp.sendRedirect(req.getContextPath() + "/manager/staff");
    }

    private static void flashError(HttpServletRequest req, String message) {
        req.getSession().setAttribute("flashError", message);
    }
}
