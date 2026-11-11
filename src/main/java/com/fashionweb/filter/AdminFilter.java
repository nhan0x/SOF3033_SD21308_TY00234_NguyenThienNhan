package com.fashionweb.filter;

import com.fashionweb.constant.AppConstant;
import com.fashionweb.constant.Role;
import com.fashionweb.model.User;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Chỉ ADMIN được vào /admin và /admin/*. Khai báo và thứ tự chạy nằm trong web.xml.
 * Chưa đăng nhập: chuyển sang /admin/login?redirect=...; đã đăng nhập nhưng không phải ADMIN: trang 403.
 * /admin/login và /admin/login/* (trang đăng nhập admin, #74) được cho qua.
 */
public class AdminFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = req.getRequestURI().substring(req.getContextPath().length());
        if (path.equals("/admin/login") || path.startsWith("/admin/login/")) {
            chain.doFilter(request, response);
            return;
        }
        HttpSession session = req.getSession(false);
        Object user = session == null ? null : session.getAttribute(AppConstant.SESSION_USER);
        if (!(user instanceof User u)) {
            resp.sendRedirect(req.getContextPath() + "/admin/login?redirect="
                    + URLEncoder.encode(AuthFilter.returnPath(req), StandardCharsets.UTF_8));
            return;
        }
        if (u.getRole() != Role.ADMIN) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        chain.doFilter(request, response);
    }
}
