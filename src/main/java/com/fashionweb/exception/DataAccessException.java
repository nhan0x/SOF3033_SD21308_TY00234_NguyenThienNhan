package com.fashionweb.exception;

/**
 * Lỗi khi làm việc với CSDL (bọc SQLException). Repository ném lỗi này để Service và Servlet
 * không phải khai báo throws SQLException. Tomcat sẽ hiện trang 500.
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
