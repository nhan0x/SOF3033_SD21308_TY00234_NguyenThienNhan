package com.fashionweb.controller.account;

import com.fashionweb.model.User;
import com.fashionweb.service.account.AccountService;
import com.fashionweb.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * GET /wishlist : danh sách sản phẩm yêu thích (Customer, AuthFilter bắt buộc đăng nhập).
 * <p>JSP: /WEB-INF/views/account/wishlist.jsp. Request attribute: wishlistProducts (chưa xóa mềm, mới thêm trước).
 * <p>POST action=add | remove | toggle (productId). Nút tim ở trang chi tiết sản phẩm gửi action=toggle bằng fetch
 * (header X-Requested-With) và nhận JSON {"inWishlist": true|false}; form trên trang /wishlist gửi remove rồi redirect.
 */
@WebServlet("/wishlist")
public class WishlistServlet extends HttpServlet {

    private final AccountService accountService = new AccountService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("wishlistProducts", accountService.findWishlist(WebUtil.currentUser(req).getId()));
        req.getRequestDispatcher("/WEB-INF/views/account/wishlist.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = WebUtil.currentUser(req);
        int productId = WebUtil.parseInt(req.getParameter("productId"), 0);
        String action = req.getParameter("action");
        if (productId <= 0 || action == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        boolean in;
        switch (action) {
            case "add" -> {
                accountService.addWishlist(user.getId(), productId);
                in = true;
            }
            case "remove" -> {
                accountService.removeWishlist(user.getId(), productId);
                in = false;
            }
            case "toggle" -> {
                in = !accountService.isInWishlist(user.getId(), productId);
                if (in) {
                    accountService.addWishlist(user.getId(), productId);
                } else {
                    accountService.removeWishlist(user.getId(), productId);
                }
            }
            default -> {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
        }

        if ("XMLHttpRequest".equals(req.getHeader("X-Requested-With"))) {
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"inWishlist\":" + in + "}");
            return;
        }
        WebUtil.flash(req, in ? "Đã thêm vào yêu thích." : "Đã bỏ khỏi yêu thích.", "success");
        resp.sendRedirect(req.getContextPath() + "/wishlist");
    }
}
