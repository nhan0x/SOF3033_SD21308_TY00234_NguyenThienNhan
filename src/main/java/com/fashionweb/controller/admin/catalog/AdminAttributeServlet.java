package com.fashionweb.controller.admin.catalog;

import com.fashionweb.exception.ValidationException;
import com.fashionweb.model.Color;
import com.fashionweb.model.Size;
import com.fashionweb.service.shop.AttributeService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * GET /admin/attributes : quản lý Màu và Size (hai thẻ cạnh nhau, thêm và sửa bằng hộp thoại).
 * JSP: /WEB-INF/views/admin/catalog/attributes.jsp
 * Thuộc tính: colors, sizes. Khi lưu lỗi: errors, openDialog ("color-dialog" hoặc "size-dialog").
 * AdminFilter bảo vệ /admin/*.
 * <p>POST action: saveColor (id, name, code, hexCode), deleteColor (id), saveSize (id, name, sortOrder), deleteSize (id).
 * Xóa là xóa mềm; màu hoặc size đang có trong biến thể của sản phẩm thì bị chặn (xem AttributeService).
 */
@WebServlet("/admin/attributes")
public class AdminAttributeServlet extends HttpServlet {

    private final AttributeService attributeService = new AttributeService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        show(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        int id = parseInt(req.getParameter("id"), 0);
        try {
            switch (action == null ? "" : action) {
                case "saveColor" -> {
                    Color c = new Color();
                    c.setId(id);
                    c.setName(req.getParameter("name"));
                    c.setCode(req.getParameter("code"));
                    c.setHexCode(req.getParameter("hexCode"));
                    attributeService.saveColor(c);
                    flash(req, "Đã lưu màu \"" + c.getName() + "\".", "success");
                }
                case "deleteColor" -> {
                    attributeService.deleteColor(id);
                    flash(req, "Đã chuyển màu vào Thùng rác.", "success");
                }
                case "saveSize" -> {
                    Size s = new Size();
                    s.setId(id);
                    s.setName(req.getParameter("name"));
                    s.setSortOrder(parseInt(req.getParameter("sortOrder"), -1));
                    attributeService.saveSize(s);
                    flash(req, "Đã lưu size \"" + s.getName() + "\".", "success");
                }
                case "deleteSize" -> {
                    attributeService.deleteSize(id);
                    flash(req, "Đã chuyển size vào Thùng rác.", "success");
                }
                default -> {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
            }
        } catch (ValidationException e) {
            if (action.startsWith("save")) {
                // Lỗi khi lưu: mở lại đúng hộp thoại kèm lỗi theo ô
                req.setAttribute("errors", e.getErrors());
                req.setAttribute("openDialog", "saveColor".equals(action) ? "color-dialog" : "size-dialog");
                show(req, resp);
                return;
            }
            flash(req, e.getMessage(), "error");
        }
        resp.sendRedirect(req.getContextPath() + "/admin/attributes");
    }

    private void show(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("colors", attributeService.findColors());
        req.setAttribute("sizes", attributeService.findSizes());
        req.getRequestDispatcher("/WEB-INF/views/admin/catalog/attributes.jsp").forward(req, resp);
    }

    private static int parseInt(String s, int fallback) {
        try {
            return s == null || s.isBlank() ? fallback : Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static void flash(HttpServletRequest req, String message, String type) {
        req.getSession().setAttribute("flashMessage", message);
        req.getSession().setAttribute("flashType", type);
    }
}
