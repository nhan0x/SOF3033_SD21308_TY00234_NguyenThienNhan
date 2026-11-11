package com.fashionweb.listener;

import com.fashionweb.model.Category;
import com.fashionweb.model.Voucher;
import com.fashionweb.service.cart.VoucherService;
import com.fashionweb.service.shop.CategoryService;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Nạp dữ liệu dùng chung cho mọi trang (menu, thanh thông báo) vào application scope.
 * Danh mục trên menu lấy từ CategoryService; Admin thêm, sửa, xóa danh mục thì gọi
 * refreshNavCategories để menu cập nhật ngay.
 * Thanh thông báo: dòng phí ship cố định cộng một dòng cho mỗi voucher đang dùng được (VoucherService.findActive);
 * Admin thêm, sửa, bật tắt, xóa voucher thì gọi refreshAnnouncements.
 */
@WebListener
public class AppStartupListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();
        refreshNavCategories(ctx);
        refreshAnnouncements(ctx);
    }

    /** Dựng lại thanh thông báo từ voucher đang dùng được. Lỗi CSDL thì chỉ giữ dòng phí ship. */
    public static void refreshAnnouncements(ServletContext ctx) {
        List<String> lines = new ArrayList<>();
        lines.add("Phí ship cố định 30.000đ, miễn phí khi dùng mã giảm giá");
        try {
            for (Voucher v : new VoucherService().findActive()) {
                lines.add("Nhập " + v.getCode() + (v.getDescription() == null ? "" : ": " + v.getDescription()));
            }
        } catch (RuntimeException e) {
            ctx.log("Không đọc được voucher cho thanh thông báo: " + e.getMessage(), e);
        }
        ctx.setAttribute("announcements", lines);
    }

    /** Đọc lại danh mục cho menu. Lỗi CSDL (ví dụ chưa có db.properties) thì để menu trống thay vì làm sập ứng dụng. */
    public static void refreshNavCategories(ServletContext ctx) {
        List<Category> categories;
        try {
            categories = new CategoryService().findAll();
        } catch (RuntimeException e) {
            ctx.log("Không đọc được danh mục cho menu: " + e.getMessage(), e);
            categories = List.of();
        }
        ctx.setAttribute("navCategories", categories);
    }
}
