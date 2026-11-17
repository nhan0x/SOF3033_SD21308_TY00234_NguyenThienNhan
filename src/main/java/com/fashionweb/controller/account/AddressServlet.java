package com.fashionweb.controller.account;

import com.fashionweb.exception.ValidationException;
import com.fashionweb.model.Address;
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
 * GET /addresses : sổ địa chỉ giao hàng (Customer, AuthFilter bắt buộc đăng nhập).
 * <p>JSP: /WEB-INF/views/account/addresses.jsp. Request attribute: addresses (mặc định đứng đầu), errors khi lưu lỗi.
 * <p>POST action=save (id, receiverName, phone, addressLine, ward, district, city, defaultAddress): id rỗng thêm, có id sửa;
 * action=setDefault (id); action=delete (id). Mọi thao tác chỉ trên địa chỉ của chính user. Xong redirect kèm flash.
 */
@WebServlet("/addresses")
public class AddressServlet extends HttpServlet {

    private final AccountService accountService = new AccountService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        show(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = WebUtil.currentUser(req);
        String action = req.getParameter("action");
        int id = WebUtil.parseInt(req.getParameter("id"), 0);
        try {
            switch (action == null ? "" : action) {
                case "save" -> {
                    Address a = new Address();
                    a.setId(id);
                    a.setUserId(user.getId());
                    a.setReceiverName(req.getParameter("receiverName"));
                    a.setPhone(req.getParameter("phone"));
                    a.setAddressLine(req.getParameter("addressLine"));
                    a.setWard(req.getParameter("ward"));
                    a.setDistrict(req.getParameter("district"));
                    a.setCity(req.getParameter("city"));
                    a.setDefaultAddress(req.getParameter("defaultAddress") != null);
                    accountService.saveAddress(a);
                    WebUtil.flash(req, id > 0 ? "Đã cập nhật địa chỉ." : "Đã thêm địa chỉ mới.", "success");
                }
                case "setDefault" -> {
                    accountService.setDefaultAddress(user.getId(), id);
                    WebUtil.flash(req, "Đã đặt làm địa chỉ mặc định.", "success");
                }
                case "delete" -> {
                    accountService.deleteAddress(user.getId(), id);
                    WebUtil.flash(req, "Đã xóa địa chỉ.", "success");
                }
                default -> {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
            }
        } catch (ValidationException e) {
            if ("save".equals(action) && !e.getErrors().isEmpty()) {
                req.setAttribute("errors", e.getErrors()); // JSP tự mở lại hộp thoại khi có errors
                show(req, resp);
                return;
            }
            WebUtil.flash(req, e.getMessage(), "error");
        }
        resp.sendRedirect(req.getContextPath() + "/addresses");
    }

    private void show(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("addresses", accountService.findAddresses(WebUtil.currentUser(req).getId()));
        req.getRequestDispatcher("/WEB-INF/views/account/addresses.jsp").forward(req, resp);
    }
}
