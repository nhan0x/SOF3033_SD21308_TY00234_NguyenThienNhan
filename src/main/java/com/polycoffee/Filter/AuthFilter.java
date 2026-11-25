package com.polycoffee.Filter;

import com.polycoffee.Util.AuthUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Đăng nhập và phân quyền (BR-03, BR-04).
 * <ul>
 *   <li>Trang công khai: /, /home, /auth/*, /css/*, /js/*, /images/*.</li>
 *   <li>Chưa đăng nhập: chuyển tới /auth/login?redirect=&lt;đường dẫn&gt;.</li>
 *   <li>/manager/*: chỉ quản lý, nhân viên nhận HTTP 403.</li>
 * </ul>
 */
public class AuthFilter implements jakarta.servlet.Filter {

    public static final String FORBIDDEN_MESSAGE = "Bạn không có quyền truy cập chức năng này";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = pathOf(req);

        if (isPublic(path)) {
            chain.doFilter(request, response);
            return;
        }
        if (!AuthUtil.isAuthenticated(req)) {
            resp.sendRedirect(req.getContextPath() + "/auth/login?redirect="
                    + URLEncoder.encode(path, StandardCharsets.UTF_8));
            return;
        }
        if (isManagerArea(path) && !AuthUtil.isManager(req)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, FORBIDDEN_MESSAGE);
            return;
        }
        chain.doFilter(request, response);
    }

    /**
     * Đường dẫn đã được container chuẩn hóa (bỏ "/..", bỏ ";jsessionid", giải mã %xx).
     * Không dùng getRequestURI vì nó giữ nguyên chuỗi thô: "/images/../manager/drinks"
     * sẽ lọt qua kiểm tra "công khai" trong khi container vẫn định tuyến tới /manager/drinks.
     */
    static String pathOf(HttpServletRequest req) {
        String servletPath = req.getServletPath() == null ? "" : req.getServletPath();
        String pathInfo = req.getPathInfo() == null ? "" : req.getPathInfo();
        return servletPath + pathInfo;
    }

    static boolean isPublic(String path) {
        return path.isEmpty() || path.equals("/") || path.equals("/home")
                || path.equals("/auth") || path.startsWith("/auth/")
                || path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/images/");
    }

    static boolean isManagerArea(String path) {
        return path.equals("/manager") || path.startsWith("/manager/");
    }
}
