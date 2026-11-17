package com.fashionweb.controller.auth;

import com.fashionweb.constant.AppConstant;
import com.fashionweb.model.User;
import com.fashionweb.service.cart.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Phần dùng chung của đăng nhập khách, đăng ký và đăng nhập admin.
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
     * Chống session fixation: đổi session ID, lưu User và số món trong giỏ.
     */
    static HttpSession login(HttpServletRequest req, User user) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            req.changeSessionId();
        }
        session = req.getSession(true);
        session.setAttribute(AppConstant.SESSION_USER, user);
        // Giỏ khách đã chọn lúc chưa đăng nhập được gộp vào giỏ của tài khoản (Admin không có giỏ)
        if (user.getRole() == com.fashionweb.constant.Role.CUSTOMER
                && session.getAttribute(AppConstant.SESSION_GUEST_CART) instanceof java.util.Map<?, ?> guest) {
            @SuppressWarnings("unchecked")
            java.util.Map<Integer, Integer> lines = (java.util.Map<Integer, Integer>) guest;
            CART_SERVICE.mergeGuestCart(user.getId(), lines);
        }
        session.removeAttribute(AppConstant.SESSION_GUEST_CART);
        session.removeAttribute(AppConstant.SESSION_GUEST_ORDER);
        session.setAttribute(AppConstant.SESSION_CART_COUNT, CART_SERVICE.countItems(user.getId()));
        return session;
    }
}
