package com.fashionweb.filter;

import com.fashionweb.constant.AppConstant;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Bắt buộc đăng nhập cho các trang của khách hàng: /cart, /checkout, /order, /orders, /order-success,
 * /profile, /addresses, /wishlist. Khai báo và thứ tự chạy nằm trong web.xml.
 *
 * <p>Chưa đăng nhập thì chuyển sang /login?redirect=&lt;trang cần quay lại&gt;. Với GET là chính trang đang mở;
 * với POST (ví dụ bấm "Thêm vào giỏ" ở trang sản phẩm) là trang trước đó (Referer) để khách quay lại đúng chỗ.
 */
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute(AppConstant.SESSION_USER) != null) {
            chain.doFilter(request, response);
            return;
        }
        resp.sendRedirect(req.getContextPath() + "/login?redirect="
                + URLEncoder.encode(returnPath(req), StandardCharsets.UTF_8));
    }

    /** Đường dẫn (không kèm context path) để quay lại sau khi đăng nhập. Dùng chung cho AdminFilter. */
    static String returnPath(HttpServletRequest req) {
        String ctx = req.getContextPath();
        if ("GET".equalsIgnoreCase(req.getMethod())) {
            String path = req.getRequestURI().substring(ctx.length());
            return req.getQueryString() == null ? path : path + "?" + req.getQueryString();
        }
        String referer = req.getHeader("Referer");
        if (referer != null) {
            try {
                URI uri = URI.create(referer);
                String host = req.getServerName();
                if ((uri.getHost() == null || uri.getHost().equalsIgnoreCase(host))
                        && uri.getRawPath() != null && uri.getRawPath().startsWith(ctx + "/")) {
                    String path = uri.getRawPath().substring(ctx.length());
                    return uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery();
                }
            } catch (IllegalArgumentException ignored) {
                // Referer không hợp lệ: dùng đường dẫn hiện tại
            }
        }
        return req.getRequestURI().substring(ctx.length());
    }
}
