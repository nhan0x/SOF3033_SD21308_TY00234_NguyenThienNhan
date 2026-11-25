package com.polycoffee.Filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;

/**
 * Đặt UTF-8 cho request và response để tiếng Việt hiển thị đúng.
 * Phải chạy TRƯỚC mọi filter hoặc servlet đọc tham số, nên thứ tự được khai báo trong web.xml.
 */
public class Utf8Filter implements jakarta.servlet.Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        chain.doFilter(req, resp);
    }
}
