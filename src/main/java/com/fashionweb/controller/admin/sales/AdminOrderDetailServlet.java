package com.fashionweb.controller.admin.sales;

import com.fashionweb.controller.admin.AdminOpsParams;

import com.fashionweb.constant.OrderStatus;
import com.fashionweb.exception.ValidationException;
import com.fashionweb.model.Order;
import com.fashionweb.service.order.OrderService;
import com.fashionweb.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * GET /admin/order?code= : chi tiết một đơn hàng cho Admin.
 * JSP: /WEB-INF/views/admin/sales/order-detail.jsp
 * Tham số: code (mã đơn). Không tìm thấy thì trả 404.
 * AdminFilter phải bảo vệ /admin/*.
 * GET: OrderService.findByCode(code) (kèm items, tên và email khách).
 * POST (sau khi xử lý flash rồi redirect về GET /admin/order?code=):
 * <ul>
 *   <li>action=updateStatus (orderCode, status=SHIPPING|COMPLETED): chỉ theo luồng PENDING -> SHIPPING -> COMPLETED;
 *       COMPLETED đặt paymentStatus=PAID và completedAt.</li>
 *   <li>action=cancel (orderCode, cancelReason bắt buộc 5..255 ký tự): chỉ khi PENDING hoặc SHIPPING, cancelledBy=ADMIN,
 *       cộng trả tồn kho và lượt voucher (dùng chung hàm hủy của #17). Thiếu lý do thì hiện lại trang kèm lỗi.</li>
 * </ul>
 */
@WebServlet("/admin/order")
public class AdminOrderDetailServlet extends HttpServlet {

    private final OrderService orderService = new OrderService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String code = AdminOpsParams.text(req, "code");
        Order order = orderService.findByCode(code);
        if (order == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        req.setAttribute("order", order);
        req.getRequestDispatcher("/WEB-INF/views/admin/sales/order-detail.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String code = AdminOpsParams.text(req, "orderCode");
        String action = req.getParameter("action");
        try {
            if ("updateStatus".equals(action)) {
                OrderStatus next = AdminOpsParams.enumValue(OrderStatus.class, req.getParameter("status"));
                orderService.updateStatus(code, next);
                WebUtil.flash(req, "Đã chuyển đơn " + code + " sang \"" + next.getLabel() + "\".", "success");
            } else if ("cancel".equals(action)) {
                orderService.cancelByAdmin(code, req.getParameter("cancelReason"));
                WebUtil.flash(req, "Đã hủy đơn " + code + ". Tồn kho và lượt voucher đã được hoàn lại.", "success");
            } else {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
        } catch (ValidationException e) {
            if (e.getErrors().containsKey("cancelReason")) {
                Order order = orderService.findByCode(code);
                if (order == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                req.setAttribute("order", order);
                req.setAttribute("errors", e.getErrors());
                req.getRequestDispatcher("/WEB-INF/views/admin/sales/order-detail.jsp").forward(req, resp);
                return;
            }
            WebUtil.flash(req, e.getMessage(), "error");
        }
        resp.sendRedirect(req.getContextPath() + "/admin/order?code=" + URLEncoder.encode(code, StandardCharsets.UTF_8));
    }
}
