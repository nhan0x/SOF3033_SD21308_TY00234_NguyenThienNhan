package com.fashionweb.controller.shop;

import com.fashionweb.constant.AppConstant;
import com.fashionweb.model.Category;
import com.fashionweb.model.Color;
import com.fashionweb.model.Product;
import com.fashionweb.model.Size;
import com.fashionweb.service.shop.AttributeService;
import com.fashionweb.service.shop.CategoryService;
import com.fashionweb.service.shop.ProductService;
import com.fashionweb.util.Page;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * GET /products : danh sách sản phẩm có lọc, sắp xếp, tìm kiếm và phân trang (Guest + Customer).
 * <p>
 * JSP: /WEB-INF/views/shop/product-list.jsp
 * <p>
 * Tham số (đều tùy chọn, giá trị sai được bỏ qua chứ không báo lỗi):
 * q (tìm theo tên), category (slug), minPrice, maxPrice (VNĐ), color (mã màu), size (tên size),
 * sort (newest | price_asc | price_desc | bestseller | rating), page (từ 1).
 * <p>
 * Dữ liệu: ProductService.search (chỉ sản phẩm chưa xóa mềm, kèm avgRating/reviewCount/soldCount/stockTotal),
 * categories từ CategoryService, colors/sizes từ AttributeService.
 */
@WebServlet("/products")
public class ProductListServlet extends HttpServlet {

    private static final List<String> SORTS = List.of("newest", "price_asc", "price_desc", "bestseller", "rating");
    private static final int MAX_QUERY_LENGTH = 100;

    private final ProductService productService = new ProductService();
    private final CategoryService categoryService = new CategoryService();
    private final AttributeService attributeService = new AttributeService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 1. Đọc và làm sạch tham số
        String q = clean(req.getParameter("q"));
        if (q.length() > MAX_QUERY_LENGTH) {
            q = q.substring(0, MAX_QUERY_LENGTH);
        }

        String categorySlug = clean(req.getParameter("category"));
        Category currentCategory = categorySlug.isEmpty() ? null : categoryService.findBySlug(categorySlug);
        if (currentCategory == null) {
            categorySlug = "";
        }
        List<Color> colors = attributeService.findColors();
        List<Size> sizes = attributeService.findSizes();

        Long minPrice = parsePrice(req.getParameter("minPrice"));
        Long maxPrice = parsePrice(req.getParameter("maxPrice"));
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            Long tmp = minPrice;
            minPrice = maxPrice;
            maxPrice = tmp;
        }

        String colorCode = clean(req.getParameter("color"));
        Color selectedColorObj = null;
        for (Color c : colors) {
            if (c.getCode().equals(colorCode)) {
                selectedColorObj = c;
                break;
            }
        }
        if (selectedColorObj == null) {
            colorCode = "";
        }

        String sizeName = clean(req.getParameter("size")).toUpperCase(Locale.ROOT);
        String validSize = "";
        for (Size s : sizes) {
            if (s.getName().equals(sizeName)) {
                validSize = s.getName();
                break;
            }
        }
        sizeName = validSize;

        String sort = clean(req.getParameter("sort"));
        if (!SORTS.contains(sort)) {
            sort = "";
        }

        int page = parsePage(req.getParameter("page"));

        // 2. Truy vấn
        String effectiveSort = sort.isEmpty() ? "newest" : sort;
        Page<Product> productPage = search(q, categorySlug, minPrice, maxPrice, colorCode, sizeName, effectiveSort, page);
        if (page > productPage.getTotalPages()) {
            productPage = search(q, categorySlug, minPrice, maxPrice, colorCode, sizeName, effectiveSort, productPage.getTotalPages());
        }

        // 3. baseUrl (không có page) và các chip bộ lọc đang bật
        Map<String, String> params = new LinkedHashMap<>();
        if (!q.isEmpty()) {
            params.put("q", q);
        }
        if (!categorySlug.isEmpty()) {
            params.put("category", categorySlug);
        }
        if (minPrice != null) {
            params.put("minPrice", String.valueOf(minPrice));
        }
        if (maxPrice != null) {
            params.put("maxPrice", String.valueOf(maxPrice));
        }
        if (!colorCode.isEmpty()) {
            params.put("color", colorCode);
        }
        if (!sizeName.isEmpty()) {
            params.put("size", sizeName);
        }
        if (!sort.isEmpty()) {
            params.put("sort", sort);
        }

        List<Map<String, String>> activeFilters = new ArrayList<>();
        if (!q.isEmpty()) {
            activeFilters.add(chip("Từ khóa: “" + q + "”", params, "q"));
        }
        if (currentCategory != null) {
            activeFilters.add(chip(currentCategory.getName(), params, "category"));
        }
        if (minPrice != null || maxPrice != null) {
            String label;
            if (minPrice != null && maxPrice != null) {
                label = money(minPrice) + " - " + money(maxPrice);
            } else if (minPrice != null) {
                label = "Từ " + money(minPrice);
            } else {
                label = "Đến " + money(maxPrice);
            }
            activeFilters.add(chip(label, params, "minPrice", "maxPrice"));
        }
        if (selectedColorObj != null) {
            activeFilters.add(chip("Màu: " + selectedColorObj.getName(), params, "color"));
        }
        if (!sizeName.isEmpty()) {
            activeFilters.add(chip("Size " + sizeName, params, "size"));
        }
        String sortLabel = switch (sort) {
            case "price_asc" -> "Giá tăng dần";
            case "price_desc" -> "Giá giảm dần";
            case "rating" -> "Đánh giá cao";
            default -> "";
        };
        if (!sortLabel.isEmpty()) {
            activeFilters.add(chip("Sắp xếp: " + sortLabel, params, "sort"));
        }

        // 4. Gửi sang JSP
        req.setAttribute("productPage", productPage);
        req.setAttribute("categories", categoryService.findAll());
        req.setAttribute("colors", colors);
        req.setAttribute("sizes", sizes);
        req.setAttribute("currentCategory", currentCategory);
        req.setAttribute("q", q);
        req.setAttribute("sort", sort);
        req.setAttribute("selectedColor", colorCode);
        req.setAttribute("selectedSize", sizeName);
        req.setAttribute("minPrice", minPrice);
        req.setAttribute("maxPrice", maxPrice);
        req.setAttribute("baseUrl", buildUrl(params));
        req.setAttribute("activeFilters", activeFilters);
        req.getRequestDispatcher("/WEB-INF/views/shop/product-list.jsp").forward(req, resp);
    }

    private Page<Product> search(String q, String categorySlug, Long minPrice, Long maxPrice,
                                 String colorCode, String sizeName, String sort, int page) {
        return productService.search(
                q.isEmpty() ? null : q,
                categorySlug.isEmpty() ? null : categorySlug,
                minPrice, maxPrice,
                colorCode.isEmpty() ? null : colorCode,
                sizeName.isEmpty() ? null : sizeName,
                sort, page, AppConstant.PAGE_SIZE);
    }

    private static String clean(String s) {
        return s == null ? "" : s.trim();
    }

    /** Trả về null nếu rỗng, không phải số, hoặc âm. */
    private static Long parsePrice(String s) {
        String v = clean(s);
        if (v.isEmpty()) {
            return null;
        }
        try {
            long n = Long.parseLong(v);
            return n < 0 ? null : n;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int parsePage(String s) {
        try {
            return Math.max(1, Integer.parseInt(clean(s)));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /** 300000 -> "300.000đ" */
    private static String money(long n) {
        return String.format(Locale.ROOT, "%,d", n).replace(',', '.') + "đ";
    }

    /** Chip bộ lọc: nhãn và đường dẫn sau khi bỏ các tham số trong removeKeys (kèm context path chưa gắn, JSP tự thêm ${ctx}). */
    private static Map<String, String> chip(String label, Map<String, String> params, String... removeKeys) {
        Map<String, String> rest = new LinkedHashMap<>(params);
        for (String k : removeKeys) {
            rest.remove(k);
        }
        Map<String, String> chip = new LinkedHashMap<>();
        chip.put("label", label);
        chip.put("url", buildUrl(rest));
        return chip;
    }

    /** "/products" hoặc "/products?a=1&b=2" (giá trị được mã hóa URL). */
    private static String buildUrl(Map<String, String> params) {
        if (params.isEmpty()) {
            return "/products";
        }
        StringBuilder sb = new StringBuilder("/products?");
        boolean first = true;
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (!first) {
                sb.append('&');
            }
            sb.append(e.getKey()).append('=').append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
            first = false;
        }
        return sb.toString();
    }
}
