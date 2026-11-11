package com.fashionweb.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Cho file tĩnh trong /assets/* (css, js, ảnh) luôn được trình duyệt hỏi lại server trước khi dùng.
 *
 * <p>Tomcat không gửi Cache-Control cho file tĩnh, nên Chrome tự đoán thời gian giữ cache và có thể
 * dùng file css cũ sau khi code đổi (trang hiện không có kiểu). "no-cache" không tắt cache:
 * file chưa đổi thì server trả 304, rất nhẹ; file đã đổi thì tải bản mới.
 */
public class StaticCacheFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (response instanceof HttpServletResponse http) {
            http.setHeader("Cache-Control", "no-cache");
        }
        chain.doFilter(request, response);
    }
}
