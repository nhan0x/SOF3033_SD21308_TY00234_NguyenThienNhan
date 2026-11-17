package com.fashionweb.controller.shop;

import com.fashionweb.model.Category;
import com.fashionweb.model.Product;
import com.fashionweb.service.shop.CategoryService;
import com.fashionweb.service.shop.ProductService;
import com.fashionweb.util.Page;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GET /search/quick?q=... : trả đoạn HTML (JSP fragment) gợi ý từ khóa, sản phẩm và danh mục cho khung tìm kiếm nhanh.
 *
 * <p>JSP: /WEB-INF/views/shop/_quick-search.jsp
 * <p>Từ khóa {@code q} dưới 2 ký tự (sau khi trim) thì trả fragment rỗng (HTTP 200) mà không truy vấn CSDL.
 */
@WebServlet("/search/quick")
public class QuickSearchServlet extends HttpServlet {

    private static final int MAX_QUERY_LENGTH = 100;
    private static final int SUGGEST_LIMIT = 4;
    private static final int PRODUCT_LIMIT = 8;

    private final ProductService productService = new ProductService();
    private final CategoryService categoryService = new CategoryService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Cache-Control", "no-store");

        String raw = req.getParameter("q");
        String q = raw == null ? "" : raw.trim();
        if (q.length() > MAX_QUERY_LENGTH) {
            q = q.substring(0, MAX_QUERY_LENGTH);
        }

        if (q.length() < 2) {
            req.setAttribute("q", "");
            req.getRequestDispatcher("/WEB-INF/views/shop/_quick-search.jsp").forward(req, resp);
            return;
        }

        List<String> rawNames = productService.suggestNames(q, SUGGEST_LIMIT);
        List<Map<String, String>> suggestions = new ArrayList<>(rawNames.size());
        String foldedQ = foldOneToOne(q);
        for (String name : rawNames) {
            suggestions.add(splitHighlight(name, foldedQ));
        }

        Page<Product> productPage = productService.search(q, null, null, null, null, null, "newest", 1, PRODUCT_LIMIT);
        List<Category> categories = categoryService.searchByName(q);

        req.setAttribute("q", q);
        req.setAttribute("suggestions", suggestions);
        req.setAttribute("productPage", productPage);
        req.setAttribute("categories", categories);
        req.getRequestDispatcher("/WEB-INF/views/shop/_quick-search.jsp").forward(req, resp);
    }

    /**
     * Tách tên sản phẩm thành 3 đoạn (prefix, match, suffix) để JSP bọc {@code <mark>} quanh đoạn khớp
     * mà vẫn dùng {@code <c:out>} cho cả 3 đoạn, chống XSS tuyệt đối.
     */
    static Map<String, String> splitHighlight(String name, String foldedQ) {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("name", name);
        String foldedName = foldOneToOne(name);
        int idx = foldedQ.isEmpty() ? -1 : foldedName.indexOf(foldedQ);
        if (idx >= 0 && idx + foldedQ.length() <= name.length()) {
            item.put("prefix", name.substring(0, idx));
            item.put("match", name.substring(idx, idx + foldedQ.length()));
            item.put("suffix", name.substring(idx + foldedQ.length()));
        } else {
            item.put("prefix", name);
            item.put("match", "");
            item.put("suffix", "");
        }
        return item;
    }

    /**
     * Chuẩn hóa từng ký tự về đúng 1 ký tự chữ thường không dấu (bảo toàn độ dài 1-1 với chuỗi gốc).
     */
    static String foldOneToOne(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == 'đ' || c == 'Đ') {
                sb.append('d');
            } else if (c < 128) {
                sb.append(Character.toLowerCase(c));
            } else {
                String nfd = Normalizer.normalize(String.valueOf(c), Normalizer.Form.NFD);
                sb.append(Character.toLowerCase(nfd.charAt(0)));
            }
        }
        return sb.toString();
    }
}
