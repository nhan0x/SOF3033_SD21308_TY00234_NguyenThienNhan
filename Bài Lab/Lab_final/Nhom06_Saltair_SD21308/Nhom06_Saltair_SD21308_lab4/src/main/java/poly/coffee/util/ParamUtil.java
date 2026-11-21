package poly.coffee.util;

import javax.servlet.http.HttpServletRequest;

/** Doc tham so request an toan (khong nem NumberFormatException). */
public class ParamUtil {
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

    public static double getDouble(HttpServletRequest req, String name, double defaultValue) {
        try {
            return Double.parseDouble(req.getParameter(name).trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /** checkbox / "true" / "on" / "1" deu duoc xem la true */
    public static boolean getBoolean(HttpServletRequest req, String name, boolean defaultValue) {
        String v = req.getParameter(name);
        if (v == null) return defaultValue;
        v = v.trim().toLowerCase();
        return v.equals("true") || v.equals("on") || v.equals("1");
    }
}
