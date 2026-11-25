package com.polycoffee.Util;

import jakarta.servlet.http.HttpServletRequest;

/** Đọc tham số request an toàn: không ném NumberFormatException, tự cắt khoảng trắng đầu cuối. */
public final class ParamUtil {

    private ParamUtil() {
    }

    /** Chuỗi đã cắt khoảng trắng. Thiếu tham số hoặc toàn khoảng trắng thì trả {@code defaultValue}. */
    public static String getString(HttpServletRequest req, String name, String defaultValue) {
        String v = req.getParameter(name);
        return (v == null || v.trim().isEmpty()) ? defaultValue : v.trim();
    }

    public static int getInt(HttpServletRequest req, String name, int defaultValue) {
        try {
            return Integer.parseInt(req.getParameter(name).trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static long getLong(HttpServletRequest req, String name, long defaultValue) {
        try {
            return Long.parseLong(req.getParameter(name).trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /** Checkbox, "true", "on", "1" đều được xem là true. */
    public static boolean getBoolean(HttpServletRequest req, String name, boolean defaultValue) {
        String v = req.getParameter(name);
        if (v == null) {
            return defaultValue;
        }
        v = v.trim().toLowerCase();
        return v.equals("true") || v.equals("on") || v.equals("1");
    }
}
