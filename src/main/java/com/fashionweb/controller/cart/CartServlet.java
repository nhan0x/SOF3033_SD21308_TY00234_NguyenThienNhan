package com.fashionweb.controller.cart;

import com.fashionweb.constant.AppConstant;
import com.fashionweb.exception.ValidationException;
import com.fashionweb.model.User;
import com.fashionweb.service.cart.CartService;
import com.fashionweb.service.shop.ProductService;
import com.fashionweb.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URI;

/**
 * GET /cart : giỏ hàng. Khách đã đăng nhập dùng giỏ trong DB; khách chưa đăng nhập dùng giỏ trong session
 * (WebUtil.guestCart, itemId chính là variantId), đăng nhập thì được gộp vào giỏ trong DB.
 * JSP: /WEB-INF/views/cart/cart.jsp. Thuộc tính: cart (Cart), shippingFee (long), suggestions (List&lt;Product&gt;).
 * <p>GET /cart?fragment=drawer : chỉ trả phần ruột của khung giỏ hàng trượt (#84), JSP /WEB-INF/views/cart/_cart-drawer.jsp.
 * <p>POST có header Accept: application/json (gọi bằng fetch từ khung giỏ hàng): không redirect, trả
 * {"ok": true|false, "message": "...", "count": số dòng trong giỏ}.
 * <p>POST action=note (note): lưu ghi chú đơn hàng vào session (AppConstant.SESSION_CART_NOTE), trang thanh toán điền sẵn.
 * <p>POST (PRG, cập nhật session cartCount sau mỗi thay đổi):
 * <ul>
 *   <li>action=add (variantId, quantity): thêm vào giỏ, quay lại trang sản phẩm (Referer) kèm thông báo.</li>
 *   <li>action=update (itemId, quantity): đổi số lượng, không vượt tồn kho; 0 thì xóa dòng.</li>
 *   <li>action=remove (itemId): xóa dòng.</li>
 * </ul>
 */
@WebServlet("/cart")
public class CartServlet extends HttpServlet {

    private static final int SUGGESTION_COUNT = 4;

    private final CartService cartService = new CartService();
    private final ProductService productService = new ProductService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = (User) req.getSession().getAttribute(AppConstant.SESSION_USER);
        var cart = user != null ? cartService.getCart(user.getId()) : cartService.getGuestCart(WebUtil.guestCart(req));
        req.getSession().setAttribute(AppConstant.SESSION_CART_COUNT, cart.getItemCount());
        req.setAttribute("cart", cart);
        req.setAttribute("shippingFee", AppConstant.SHIPPING_FEE);
        if ("drawer".equals(req.getParameter("fragment"))) {
            resp.setHeader("Cache-Control", "no-store");
            req.getRequestDispatcher("/WEB-INF/views/cart/_cart-drawer.jsp").forward(req, resp);
            return;
        }
        req.setAttribute("suggestions", productService.bestSellers(SUGGESTION_COUNT));
        req.getRequestDispatcher("/WEB-INF/views/cart/cart.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();
        User user = (User) session.getAttribute(AppConstant.SESSION_USER);
        String action = req.getParameter("action");
        String back = "/cart";
        String accept = req.getHeader("Accept");
        boolean json = accept != null && accept.contains("application/json");
        String message = null;
        boolean ok = true;
        try {
            switch (action == null ? "" : action) {
                case "add" -> {
                    back = refererPath(req);
                    int variantId = parseInt(req.getParameter("variantId"), 0);
                    if (variantId <= 0) {
                        throw new ValidationException(null, "Vui lòng chọn màu và size.");
                    }
                    int quantity = parseInt(req.getParameter("quantity"), 1);
                    if (user != null) {
                        cartService.addItem(user.getId(), variantId, quantity);
                    } else {
                        cartService.addGuestItem(WebUtil.guestCart(req), variantId, quantity);
                    }
                    message = "Đã thêm vào giỏ hàng.";
                }
                case "update" -> {
                    int itemId = parseInt(req.getParameter("itemId"), 0);
                    int quantity = parseInt(req.getParameter("quantity"), 1);
                    if (user != null) {
                        cartService.updateQuantity(user.getId(), itemId, quantity);
                    } else {
                        cartService.updateGuestQuantity(WebUtil.guestCart(req), itemId, quantity);
                    }
                }
                case "remove" -> {
                    int itemId = parseInt(req.getParameter("itemId"), 0);
                    if (user != null) {
                        cartService.removeItem(user.getId(), itemId);
                    } else {
                        WebUtil.guestCart(req).remove(itemId);
                    }
                    message = "Đã xóa sản phẩm khỏi giỏ hàng.";
                }
                case "note" -> {
                    String note = req.getParameter("note");
                    note = note == null ? "" : note.trim();
                    session.setAttribute(AppConstant.SESSION_CART_NOTE, note.length() > 500 ? note.substring(0, 500) : note);
                    message = "Đã lưu ghi chú.";
                }
                default -> {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
            }
        } catch (ValidationException e) {
            ok = false;
            message = e.getMessage();
        }
        int count = user != null ? cartService.countItems(user.getId()) : WebUtil.guestCart(req).size();
        session.setAttribute(AppConstant.SESSION_CART_COUNT, count);
        if (json) {
            resp.setContentType("application/json;charset=UTF-8");
            resp.setHeader("Cache-Control", "no-store");
            resp.getWriter().write("{\"ok\":" + ok + ",\"count\":" + count + ",\"message\":\"" + jsonEscape(message) + "\"}");
            return;
        }
        if (message != null) {
            flash(session, message, ok ? "success" : "error");
        }
        resp.sendRedirect(req.getContextPath() + back);
    }

    /** Trang trước đó trong cùng ứng dụng (không kèm context path), mặc định /cart. */
    private static String refererPath(HttpServletRequest req) {
        String referer = req.getHeader("Referer");
        String ctx = req.getContextPath();
        if (referer != null) {
            try {
                URI uri = URI.create(referer);
                if ((uri.getHost() == null || uri.getHost().equalsIgnoreCase(req.getServerName()))
                        && uri.getRawPath() != null && uri.getRawPath().startsWith(ctx + "/")) {
                    String path = uri.getRawPath().substring(ctx.length());
                    return uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery();
                }
            } catch (IllegalArgumentException ignored) {
                // Referer không hợp lệ
            }
        }
        return "/cart";
    }

    private static String jsonEscape(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (char ch : s.toCharArray()) {
            switch (ch) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (ch < 0x20 || ch == '<' || ch == '>' || ch == '&') {
                        sb.append(String.format("\\u%04x", (int) ch));
                    } else {
                        sb.append(ch);
                    }
                }
            }
        }
        return sb.toString();
    }

    private static int parseInt(String s, int fallback) {
        try {
            return s == null || s.isBlank() ? fallback : Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static void flash(HttpSession session, String message, String type) {
        session.setAttribute("flashMessage", message);
        session.setAttribute("flashType", type);
    }
}
