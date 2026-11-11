package com.fashionweb.controller.admin.sales;

import com.fashionweb.constant.DiscountType;
import com.fashionweb.controller.admin.AdminOpsParams;
import com.fashionweb.exception.ValidationException;
import com.fashionweb.listener.AppStartupListener;
import com.fashionweb.model.Voucher;
import com.fashionweb.service.cart.VoucherService;
import com.fashionweb.service.shop.CategoryService;
import com.fashionweb.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * GET /admin/vouchers : danh sách voucher cho Admin (không phân trang), kèm số lượt đã dùng và trạng thái.
 * JSP: /WEB-INF/views/admin/sales/vouchers.jsp
 * <p>
 * POST:
 * <ul>
 *   <li>action=save (id rỗng = thêm): kiểm tra ở VoucherService.save; lỗi thì hiện lại trang, hộp thoại tự mở kèm lỗi.
 *       startDate, endDate dạng yyyy-MM-ddTHH:mm (ô datetime-local).</li>
 *   <li>action=toggleActive (id): bật hoặc tạm dừng.</li>
 *   <li>action=delete (id): chỉ khi chưa có người dùng.</li>
 * </ul>
 * AdminFilter bảo vệ /admin/*.
 */
@WebServlet("/admin/vouchers")
public class AdminVoucherServlet extends HttpServlet {

    private final VoucherService voucherService = new VoucherService();
    private final CategoryService categoryService = new CategoryService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        show(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        int id = WebUtil.parseInt(req.getParameter("id"), 0);
        try {
            switch (action == null ? "" : action) {
                case "save" -> {
                    Voucher v = fromForm(req, id);
                    voucherService.save(v);
                    WebUtil.flash(req, (id == 0 ? "Đã thêm voucher " : "Đã lưu voucher ") + v.getCode() + ".", "success");
                }
                case "toggleActive" -> WebUtil.flash(req, voucherService.toggleActive(id)
                        ? "Đã bật lại voucher." : "Đã tạm dừng voucher.", "success");
                case "delete" -> {
                    voucherService.delete(id);
                    WebUtil.flash(req, "Đã xóa voucher.", "success");
                }
                default -> {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
            }
            AppStartupListener.refreshAnnouncements(getServletContext());
        } catch (ValidationException e) {
            if ("save".equals(action) && !e.getErrors().isEmpty()) {
                req.setAttribute("errors", e.getErrors());
                show(req, resp);
                return;
            }
            WebUtil.flash(req, e.getMessage(), "error");
        }
        resp.sendRedirect(req.getContextPath() + "/admin/vouchers");
    }

    private void show(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("vouchers", voucherService.findAll());
        req.setAttribute("categories", categoryService.findAll());
        req.getRequestDispatcher("/WEB-INF/views/admin/sales/vouchers.jsp").forward(req, resp);
    }

    private static Voucher fromForm(HttpServletRequest req, int id) {
        Voucher v = new Voucher();
        v.setId(id);
        v.setCode(req.getParameter("code"));
        v.setDescription(req.getParameter("description"));
        v.setDiscountType(AdminOpsParams.enumValue(DiscountType.class, req.getParameter("discountType")));
        v.setDiscountValue(parseLong(req.getParameter("discountValue"), 0));
        long max = parseLong(req.getParameter("maxDiscount"), -1);
        v.setMaxDiscount(isBlank(req.getParameter("maxDiscount")) ? null : max);
        v.setMinOrderAmount(parseLong(req.getParameter("minOrderAmount"), 0));
        int cat = WebUtil.parseInt(req.getParameter("categoryId"), 0);
        v.setCategoryId(cat > 0 ? cat : null);
        v.setStartDate(parseDate(req.getParameter("startDate")));
        v.setEndDate(parseDate(req.getParameter("endDate")));
        v.setUsageLimit(WebUtil.parseInt(req.getParameter("usageLimit"), 0));
        v.setPerUserLimit(WebUtil.parseInt(req.getParameter("perUserLimit"), 0));
        v.setActive("true".equals(req.getParameter("active")));
        return v;
    }

    private static Date parseDate(String s) {
        if (isBlank(s)) {
            return null;
        }
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
        f.setLenient(false);
        try {
            return f.parse(s.trim());
        } catch (ParseException e) {
            return null;
        }
    }

    private static long parseLong(String s, long fallback) {
        try {
            return isBlank(s) ? fallback : Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
