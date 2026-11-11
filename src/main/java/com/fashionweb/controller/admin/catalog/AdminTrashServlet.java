    package com.fashionweb.controller.admin;

import com.fashionweb.exception.ValidationException;
import com.fashionweb.listener.AppStartupListener;
import com.fashionweb.service.shop.AttributeService;
import com.fashionweb.service.shop.CategoryService;
import com.fashionweb.service.shop.ProductService;
import com.fashionweb.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GET /admin/trash : thùng rác chung, khôi phục dữ liệu đã xóa mềm (không có xóa vĩnh viễn trong phạm vi đồ án).
 * JSP: /WEB-INF/views/admin/catalog/trash.jsp
 * Tham số: tab = products | categories | colors | sizes (mặc định products; giá trị lạ thì dùng products).
 * Thuộc tính: tab (String), items (List: Product | Category | Color | Size tùy tab), tabCounts (Map tab -> số mục).
 * Các nhóm khác có thể thêm tab mới (banners, blogs...) bằng cách mở rộng servlet và JSP này.
 * AdminFilter phải bảo vệ /admin/*.
 * Dữ liệu: ProductService/CategoryService/AttributeService.findDeleted... (is_deleted = 1, mới xóa trước).
 * POST action=restore, type (products|categories|colors|sizes), id: đặt is_deleted = 0, deleted_at = NULL, rồi quay về tab đó.
 * Khôi phục sản phẩm mà danh mục đang bị xóa mềm thì báo rõ; khôi phục danh mục thì làm mới menu.
 */
@WebServlet("/admin/trash")
public class AdminTrashServlet extends HttpServlet {

    private final ProductService productService = new ProductService();
    private final CategoryService categoryService = new CategoryService();
    private final AttributeService attributeService = new AttributeService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String tab = req.getParameter("tab");
        if (tab == null) {
            tab = "products";
        }
        tab = tab.trim();

        Map<String, List<?>> all = new LinkedHashMap<>();
        all.put("products", productService.findDeleted());
        all.put("categories", categoryService.findDeleted());
        all.put("colors", attributeService.findDeletedColors());
        all.put("sizes", attributeService.findDeletedSizes());
        if (!all.containsKey(tab)) {
            tab = "products";
        }

        Map<String, Integer> tabCounts = new LinkedHashMap<>();
        all.forEach((k, v) -> tabCounts.put(k, v.size()));

        req.setAttribute("tab", tab);
        req.setAttribute("items", all.get(tab));
        req.setAttribute("tabCounts", tabCounts);
        req.getRequestDispatcher("/WEB-INF/views/admin/catalog/trash.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String type = req.getParameter("type");
        int id = WebUtil.parseInt(req.getParameter("id"), 0);
        if (!"restore".equals(req.getParameter("action")) || type == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try {
            switch (type) {
                case "products" -> productService.restore(id);
                case "categories" -> {
                    categoryService.restore(id);
                    AppStartupListener.refreshNavCategories(getServletContext());
                }
                case "colors" -> attributeService.restoreColor(id);
                case "sizes" -> attributeService.restoreSize(id);
                default -> {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
            }
            WebUtil.flash(req, "Đã khôi phục.", "success");
        } catch (ValidationException e) {
            WebUtil.flash(req, e.getMessage(), "error");
        }
        resp.sendRedirect(req.getContextPath() + "/admin/trash?tab=" + type);
    }
}
