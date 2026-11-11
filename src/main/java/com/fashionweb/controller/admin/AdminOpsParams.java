package com.fashionweb.controller.admin;

import com.fashionweb.constant.AppConstant;
import com.fashionweb.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Hàm đọc tham số an toàn dùng chung cho nhóm Servlet Admin vận hành (orders, users, banners, blogs, vouchers, reviews).
 * Không bao giờ ném lỗi khi tham số sai: trả về giá trị mặc định.
 */
public final class AdminOpsParams {

    private static final int MAX_LEN = 100;

    private AdminOpsParams() {
    }

    /** Chuỗi đã trim, không null, tối đa 100 ký tự. */
    public static String text(HttpServletRequest req, String name) {
        String v = req.getParameter(name);
        if (v == null) {
            return "";
        }
        v = v.trim();
        return v.length() > MAX_LEN ? v.substring(0, MAX_LEN) : v;
    }

    /** Số trang từ tham số page, tối thiểu 1. */
    public static int page(HttpServletRequest req) {
        try {
            return Math.max(1, Integer.parseInt(text(req, "page")));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /** Số nguyên dương, hoặc null nếu rỗng hoặc sai định dạng. */
    public static Integer positiveInt(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            int n = Integer.parseInt(s.trim());
            return n > 0 ? n : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Giá trị enum theo tên (không phân biệt hoa thường), hoặc null nếu rỗng hoặc không hợp lệ. */
    public static <E extends Enum<E>> E enumValue(Class<E> type, String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
