package com.fashionweb.controller.cart;

import com.fashionweb.constant.AppConstant;
import com.fashionweb.exception.ValidationException;
import com.fashionweb.model.User;
import com.fashionweb.service.cart.CartService;
import com.fashionweb.service.shop.ProductService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URI;

/**
 * GET /cart : giỏ hàng của khách đang đăng nhập (AuthFilter bắt buộc đăng nhập).
 * JSP: /WEB-INF/views/cart/cart.jsp. Thuộc tính: cart (Cart), shippingFee (long), suggestions (List&lt;Product&gt;).
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
        var cart = cartService.getCart(user.getId());
        req.getSession().setAttribute(AppConstant.SESSION_CART_COUNT, cart.getItemCount());
        req.setAttribute("cart", cart);
        req.setAttribute("shippingFee", AppConstant.SHIPPING_FEE);
        req.setAttribute("suggestions", productService.bestSellers(SUGGESTION_COUNT));
        req.getRequestDispatcher("/WEB-INF/views/cart/cart.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();
        User user = (User) session.getAttribute(AppConstant.SESSION_USER);
        String action = req.getParameter("action");
        String back = "/cart";
        try {
            switch (action == null ? "" : action) {
                case "add" -> {
                    back = refererPath(req);
                    int variantId = parseInt(req.getParameter("variantId"), 0);
                    if (variantId <= 0) {
                        throw new ValidationException(null, "Vui lòng chọn màu và size.");
                    }
                    cartService.addItem(user.getId(), variantId, parseInt(req.getParameter("quantity"), 1));
                    flash(session, "Đã thêm vào giỏ hàng.", "success");
                }
                case "update" -> cartService.updateQuantity(user.getId(),
                        parseInt(req.getParameter("itemId"), 0), parseInt(req.getParameter("quantity"), 1));
                case "remove" -> {
                    cartService.removeItem(user.getId(), parseInt(req.getParameter("itemId"), 0));
                    flash(session, "Đã xóa sản phẩm khỏi giỏ hàng.", "success");
                }
                default -> {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
            }
        } catch (ValidationException e) {
            flash(session, e.getMessage(), "error");
        }
        session.setAttribute(AppConstant.SESSION_CART_COUNT, cartService.countItems(user.getId()));
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
