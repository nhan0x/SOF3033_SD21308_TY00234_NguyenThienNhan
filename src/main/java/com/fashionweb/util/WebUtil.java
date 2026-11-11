package com.fashionweb.util;

import com.fashionweb.constant.AppConstant;
import com.fashionweb.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/** Hàm nhỏ dùng chung cho Servlet: người dùng đang đăng nhập, thông báo flash, đọc số từ tham số. */
public final class WebUtil {

    private WebUtil() {
    }

    /** User trong session, null nếu chưa đăng nhập. */
    public static User currentUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null && session.getAttribute(AppConstant.SESSION_USER) instanceof User u ? u : null;
    }

    /** Thông báo hiện ở trang kế tiếp sau redirect (flash.jspf). type: success | error | info. */
    public static void flash(HttpServletRequest req, String message, String type) {
        HttpSession session = req.getSession();
        session.setAttribute("flashMessage", message);
        session.setAttribute("flashType", type);
    }

    /** Số nguyên từ tham số; rỗng hoặc sai định dạng thì trả fallback. */
    public static int parseInt(String s, int fallback) {
        try {
            return s == null || s.isBlank() ? fallback : Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
