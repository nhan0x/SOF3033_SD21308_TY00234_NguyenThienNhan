package poly.coffee.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import poly.coffee.entity.User;

/** Quan ly thong tin dang nhap luu trong session. */
public class AuthUtil {
    private static final String KEY = "authUser";

    public static void setUser(HttpServletRequest request, User user) {
        request.getSession().setAttribute(KEY, user);
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

    public static void clear(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
    }
}
