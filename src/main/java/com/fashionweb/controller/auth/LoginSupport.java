package com.fashionweb.controller.auth;

import com.fashionweb.constant.AppConstant;
import com.fashionweb.model.User;
import com.fashionweb.service.cart.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Phần dùng chung của luồng đăng nhập khách và admin (#74).
 */
final class LoginSupport {

    private static final CartService CART_SERVICE = new CartService();

    private LoginSupport() {
    }

    /** Chỉ nhận đường dẫn nội bộ dạng "/xxx" (chặn "//host", "/\host", URL tuyệt đối và các trang đăng nhập). */
    static String safeRedirect(String r) {
        if (r == null || r.isBlank()) {
            return null;
        }
        r = r.trim();
        if (!r.startsWith("/") || r.startsWith("//") || r.startsWith("/\\") || r.contains("\r") || r.contains("\n")
                || r.startsWith("/login") || r.startsWith("/logout") || r.startsWith("/register")
                || r.startsWith("/admin/login")) {
            return null;
        }
        return r;
    }

    static User currentUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null && session.getAttribute(AppConstant.SESSION_USER) instanceof User u ? u : null;
    }

    /**
     * Chống session fixation: đổi session ID, lưu User và số món trong giỏ. Xóa các thuộc tính chờ nhập mã.
     */
    static HttpSession login(HttpServletRequest req, User user) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            req.changeSessionId();
        }
        session = req.getSession(true);
        session.removeAttribute(AppConstant.SESSION_LOGIN_EMAIL);
        session.removeAttribute(AppConstant.SESSION_LOGIN_REDIRECT);
        session.removeAttribute(AppConstant.SESSION_ADMIN_LOGIN_EMAIL);
        session.removeAttribute(AppConstant.SESSION_ADMIN_LOGIN_REDIRECT);
        session.removeAttribute(AppConstant.SESSION_DEV_CODE);
        session.setAttribute(AppConstant.SESSION_USER, user);
        session.setAttribute(AppConstant.SESSION_CART_COUNT, CART_SERVICE.countItems(user.getId()));
        return session;
    }

    /** Mã từ ô "code" (fetch) hoặc ghép từ 6 ô "d" (gửi form khi tắt JS). */
    static String codeParam(HttpServletRequest req) {
        String code = req.getParameter("code");
        String[] digits = req.getParameterValues("d");
        if ((code == null || code.isBlank()) && digits != null) {
            code = String.join("", digits);
        }
        return code;
    }

    /** true khi gọi bằng fetch từ khung tài khoản hoặc trang nhập mã (Accept: application/json). */
    static boolean wantsJson(HttpServletRequest req) {
        String accept = req.getHeader("Accept");
        return accept != null && accept.contains("application/json");
    }

    /** Trả JSON phẳng {key: chuỗi | số | boolean}. */
    static void json(HttpServletResponse resp, Map<String, Object> data) throws IOException {
        StringBuilder sb = new StringBuilder("{");
        for (Map.Entry<String, Object> e : data.entrySet()) {
            if (sb.length() > 1) {
                sb.append(',');
            }
            sb.append('"').append(e.getKey()).append("\":");
            Object v = e.getValue();
            if (v instanceof Number || v instanceof Boolean) {
                sb.append(v);
            } else {
                sb.append('"').append(escape(String.valueOf(v))).append('"');
            }
        }
        resp.setContentType("application/json;charset=UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        resp.getWriter().write(sb.append('}').toString());
    }

    static Map<String, Object> ok(String redirect) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        m.put("redirect", redirect);
        return m;
    }

    /** Lỗi đầu tiên của ValidationException; field "admin" thì kèm adminUrl. */
    static Map<String, Object> error(HttpServletRequest req, Map<String, String> errors, String fallback) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", false);
        m.put("error", errors.isEmpty() ? fallback : errors.values().iterator().next());
        if (errors.containsKey("admin")) {
            m.put("adminUrl", req.getContextPath() + "/admin/login");
        }
        return m;
    }

    private static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '<' -> sb.append("\\u003c");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    /** Lưu mã vào session để hiện trên trang chỉ khi auth.devCode bật (devCode != null), ngược lại xóa. */
    static void rememberDevCode(HttpServletRequest req, String devCode) {
        if (devCode != null) {
            req.getSession().setAttribute(AppConstant.SESSION_DEV_CODE, devCode);
        } else {
            req.getSession().removeAttribute(AppConstant.SESSION_DEV_CODE);
        }
    }
}
