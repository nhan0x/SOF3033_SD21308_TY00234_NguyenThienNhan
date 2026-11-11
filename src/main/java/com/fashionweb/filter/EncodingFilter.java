package com.fashionweb.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import java.io.IOException;

/**
 * Đặt UTF-8 cho dữ liệu gửi lên (request) để đọc đúng tiếng Việt từ form.
 *
 * <p>KHÔNG gọi response.setCharacterEncoding ở đây: bộ lọc này chạy cho cả file tĩnh (css, js).
 * Tomcat sẽ chuyển mã lại nội dung file tĩnh theo bảng mã mặc định của máy (Windows-1252)
 * và làm vỡ chữ tiếng Việt trong file .js. Trang JSP đã tự khai báo UTF-8 bằng
 * contentType="text/html;charset=UTF-8" và cấu hình jsp-config trong web.xml.
 * Khai báo và thứ tự chạy nằm trong web.xml (chạy đầu tiên).
 */
public class EncodingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        request.setCharacterEncoding("UTF-8");
        chain.doFilter(request, response);
    }
}
