package com.fashionweb.controller.admin.system;

import com.fashionweb.controller.admin.AdminOpsParams;

import com.fashionweb.constant.AppConstant;
import com.fashionweb.constant.Role;
import com.fashionweb.exception.ValidationException;
import com.fashionweb.service.auth.UserService;
import com.fashionweb.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * GET /admin/users?q=&amp;role=&amp;page= : danh sách người dùng cho Admin.
 * JSP: /WEB-INF/views/admin/system/users.jsp
 * Tham số: q (email hoặc họ tên), role (CUSTOMER|ADMIN), page.
 * AdminFilter phải bảo vệ /admin/*.
 * POST (xong thì flash rồi quay về đúng trang danh sách đang xem):
 * <ul>
 *   <li>action=toggleActive (id): khóa hoặc mở khóa; không cho tự khóa chính mình. Tài khoản bị khóa không đăng nhập được.</li>
 *   <li>action=changeRole (id, role=CUSTOMER|ADMIN): không cho tự đổi quyền của chính mình.</li>
 * </ul>
 */
@WebServlet("/admin/users")
public class AdminUserServlet extends HttpServlet {

    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String q = AdminOpsParams.text(req, "q");
        Role role = AdminOpsParams.enumValue(Role.class, req.getParameter("role"));
        int page = AdminOpsParams.page(req);

        req.setAttribute("userPage", userService.search(q, role, page, AppConstant.ADMIN_PAGE_SIZE));
        req.setAttribute("q", q);
        req.setAttribute("role", role == null ? "" : role.name());
        req.setAttribute("currentAdminId", WebUtil.currentUser(req).getId());
        req.getRequestDispatcher("/WEB-INF/views/admin/system/users.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int adminId = WebUtil.currentUser(req).getId();
        int id = WebUtil.parseInt(req.getParameter("id"), 0);
        String action = req.getParameter("action");
        try {
            if ("toggleActive".equals(action)) {
                WebUtil.flash(req, userService.toggleActive(adminId, id) ? "Đã mở khóa tài khoản." : "Đã khóa tài khoản.", "success");
            } else if ("changeRole".equals(action)) {
                Role role = AdminOpsParams.enumValue(Role.class, req.getParameter("role"));
                userService.changeRole(adminId, id, role);
                WebUtil.flash(req, "Đã đổi quyền thành " + (role == Role.ADMIN ? "Quản trị viên." : "Khách hàng."), "success");
            } else {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
        } catch (ValidationException e) {
            WebUtil.flash(req, e.getMessage(), "error");
        }
        String back = req.getContextPath() + "/admin/users";
        String referer = req.getHeader("Referer");
        if (referer != null) {
            int i = referer.indexOf(back);
            if (i >= 0 && referer.startsWith("/admin/users", i + req.getContextPath().length())) {
                back = referer.substring(i);
            }
        }
        resp.sendRedirect(back);
    }
}
