package poly.coffee.filter;

import java.io.IOException;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import poly.coffee.util.AuthUtil;

/**
 * - Trang cong khai: trang chu, /auth/*, tai nguyen tinh.
 * - Chua dang nhap  -> chuyen ve /auth/login.
 * - /manager/*      -> chi quan ly; nhan vien nhan 403.
 */
@WebFilter("/*")
public class AuthFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = req.getRequestURI().substring(req.getContextPath().length());

        if (isPublic(path)) {
            chain.doFilter(request, response);
            return;
        }
        if (!AuthUtil.isAuthenticated(req)) {
            resp.sendRedirect(req.getContextPath() + "/auth/login?redirect="
                    + java.net.URLEncoder.encode(path, "UTF-8"));
            return;
        }
        if (path.startsWith("/manager") && !AuthUtil.isManager(req)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Ban khong co quyen truy cap chuc nang nay");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isPublic(String path) {
        return path.equals("") || path.equals("/") || path.equals("/home")
                || path.startsWith("/auth/")
                || path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/images/");
    }
}
