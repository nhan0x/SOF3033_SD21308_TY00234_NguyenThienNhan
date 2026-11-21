package com.fashionweb.controller.admin.catalog;

import com.fashionweb.model.Product;
import com.fashionweb.model.ProductVariant;
import com.fashionweb.service.shop.AttributeService;
import com.fashionweb.service.shop.CategoryService;
import com.fashionweb.service.shop.ProductService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * GET /admin/products/form : form thêm hoặc sửa sản phẩm (POST của form gửi về /admin/products, xem AdminProductServlet).
 * JSP: /WEB-INF/views/admin/catalog/product-form.jsp
 * Tham số: id (không có = thêm mới; id không tồn tại, đã xóa mềm hoặc không hợp lệ = 404).
 * Thuộc tính: product (null khi thêm), categories, colors, sizes (chỉ màu và size chưa xóa mềm), mode (create|edit),
 * stockMap (Map "colorId_sizeId" -> tồn kho), selectedColorIds (Set id màu đã gắn cho sản phẩm).
 * AdminFilter bảo vệ /admin/*.
 */
@WebServlet("/admin/products/form")
public class AdminProductFormServlet extends HttpServlet {

    static final String VIEW = "/WEB-INF/views/admin/catalog/product-form.jsp";

    private final ProductService productService = new ProductService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String idParam = req.getParameter("id");
        Product product = null;
        if (idParam != null && !idParam.isBlank()) {
            int id;
            try {
                id = Integer.parseInt(idParam.trim());
            } catch (NumberFormatException e) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            product = productService.findById(id);
            if (product == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        }

        Map<String, Integer> stockMap = new HashMap<>();
        Set<Integer> selectedColorIds = new HashSet<>();
        if (product != null && product.getVariants() != null) {
            for (ProductVariant v : product.getVariants()) {
                stockMap.put(v.getColorId() + "_" + v.getSizeId(), v.getStock());
                selectedColorIds.add(v.getColorId());
            }
        }
        prepareForm(req, product, stockMap, selectedColorIds);
        req.getRequestDispatcher(VIEW).forward(req, resp);
    }

    /** Đặt các thuộc tính form dùng chung cho GET và khi POST lưu bị lỗi (AdminProductServlet). */
    static void prepareForm(HttpServletRequest req, Product product, Map<String, Integer> stockMap, Set<Integer> selectedColorIds) {
        AttributeService attributeService = new AttributeService();
        req.setAttribute("product", product);
        req.setAttribute("mode", product == null ? "create" : "edit");
        req.setAttribute("categories", new CategoryService().findAll());
        req.setAttribute("colors", attributeService.findColors());
        req.setAttribute("sizes", attributeService.findSizes());
        req.setAttribute("stockMap", stockMap);
        req.setAttribute("selectedColorIds", selectedColorIds);
    }
}
