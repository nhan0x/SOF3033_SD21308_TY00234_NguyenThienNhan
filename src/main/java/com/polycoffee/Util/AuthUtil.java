package com.polycoffee.Util;

import com.polycoffee.Entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/** Quản lý thông tin đăng nhập lưu trong session. */
public final class AuthUtil {

    public static final String KEY = "authUser";

    private AuthUtil() {
    }

    /**
     * Lưu người dùng vào session. Đổi session id nếu session đã tồn tại (chống session fixation).
     * Không giữ bản băm mật khẩu trong session.
     */
    public static void setUser(HttpServletRequest request, User user) {
        HttpSession old = request.getSession(false);
        if (old != null) {
            request.changeSessionId();
        }
        User safe = new User(user.getId(), user.getFullname(), user.getEmail(), null,
                user.isManager(), user.isEnabled());
        request.getSession().setAttribute(KEY, safe);
    }

    public static User getUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute(KEY);
    }

    public static boolean isAuthenticated(HttpServletRequest request) {
        return getUser(request) != null;
    }

    public static boolean isManager(HttpServletRequest request) {
        User u = getUser(request);
        return u != null && u.isManager();
    }

    /** Đăng xuất: hủy session (BR-06). */
    public static void clear(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
