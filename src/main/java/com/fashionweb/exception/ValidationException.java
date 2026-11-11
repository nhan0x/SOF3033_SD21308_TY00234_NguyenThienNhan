package com.fashionweb.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dữ liệu không hợp lệ hoặc vi phạm quy tắc nghiệp vụ. Service ném lỗi này, Servlet bắt lại và
 * hiện thông báo tiếng Việt cho người dùng.
 *
 * <p>errors: lỗi theo tên ô trong form (ví dụ "email" -> "Email đã được sử dụng"), dùng cho
 * thuộc tính request "errors" của JSP. getMessage(): câu tóm tắt (lỗi đầu tiên) để hiện dạng thông báo chung.
 */
public class ValidationException extends RuntimeException {

    private final Map<String, String> errors;

    public ValidationException(Map<String, String> errors) {
        super(errors.isEmpty() ? "Dữ liệu không hợp lệ" : errors.values().iterator().next());
        this.errors = Collections.unmodifiableMap(new LinkedHashMap<>(errors));
    }

    /** Một lỗi gắn với một ô; field có thể null nếu là lỗi chung. */
    public ValidationException(String field, String message) {
        super(message);
        Map<String, String> m = new LinkedHashMap<>();
        if (field != null) {
            m.put(field, message);
        }
        this.errors = Collections.unmodifiableMap(m);
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
