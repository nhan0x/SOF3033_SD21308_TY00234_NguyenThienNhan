package com.fashionweb.controller.admin.catalog;

import com.fashionweb.constant.AppConstant;
import com.fashionweb.exception.ValidationException;
import com.fashionweb.model.Category;
import com.fashionweb.model.Product;
import com.fashionweb.model.ProductVariant;
import com.fashionweb.model.Size;
import com.fashionweb.service.shop.AttributeService;
import com.fashionweb.service.shop.CategoryService;
import com.fashionweb.service.shop.ProductService;
import com.fashionweb.util.ImageUploadUtil;
import com.fashionweb.util.Page;
import com.fashionweb.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * GET /admin/products : danh sách sản phẩm của Admin (tìm theo tên, lọc theo danh mục, phân trang).
 * JSP: /WEB-INF/views/admin/catalog/products.jsp
 * Tham số: q (tên sản phẩm), category (slug danh mục), page (mặc định 1; vượt trang cuối thì lấy trang cuối).
 * Thuộc tính: productPage, categories, q, category, baseUrl.
 * <p>
 * POST /admin/products (multipart):
 * <ul>
 *   <li>action=save: id rỗng = thêm, có id = sửa. Trường: name, slug (trống = tự sinh), categoryId, price, description,
 *       featured, imageFile (JPG/PNG/WebP tối đa 2MB, không chọn thì giữ ảnh cũ; lưu qua ImageUploadUtil: Cloudinary
 *       khi có cloudinary.properties, ngược lại assets/images/products; ảnh thumb 600px cho lưới sản phẩm), colorIds, stock_{colorId}_{sizeId}. Sản phẩm và biến thể lưu trong một transaction.
 *       Lỗi thì hiện lại form kèm errors; thành công thì flash rồi redirect /admin/products.</li>
 *   <li>action=delete (id): xóa mềm, sản phẩm vào Thùng rác; đơn cũ giữ nguyên.</li>
 * </ul>
 * AdminFilter bảo vệ /admin/*.
 */
@WebServlet("/admin/products")
@MultipartConfig(maxFileSize = 5 * 1024 * 1024, maxRequestSize = 6 * 1024 * 1024)
public class AdminProductServlet extends HttpServlet {

    private static final String IMAGE_DIR = "assets/images/products";

    private final ProductService productService = new ProductService();
    private final CategoryService categoryService = new CategoryService();
    private final AttributeService attributeService = new AttributeService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String q = trim(req.getParameter("q"));
        String category = trim(req.getParameter("category"));
        List<Category> categories = categoryService.findAll();
        String selected = category;
        if (categories.stream().noneMatch(c -> c.getSlug().equals(selected))) {
            category = "";
        }
        int page = WebUtil.parseInt(req.getParameter("page"), 1);
        Page<Product> result = productService.searchForAdmin(q, category.isEmpty() ? null : category, page, AppConstant.ADMIN_PAGE_SIZE);

        StringBuilder base = new StringBuilder("/admin/products");
        char sep = '?';
        if (!q.isEmpty()) {
            base.append(sep).append("q=").append(URLEncoder.encode(q, StandardCharsets.UTF_8));
            sep = '&';
        }
        if (!category.isEmpty()) {
            base.append(sep).append("category=").append(URLEncoder.encode(category, StandardCharsets.UTF_8));
        }

        req.setAttribute("productPage", result);
        req.setAttribute("categories", categories);
        req.setAttribute("q", q);
        req.setAttribute("category", category);
        req.setAttribute("baseUrl", base.toString());
        req.getRequestDispatcher("/WEB-INF/views/admin/catalog/products.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action;
        try {
            action = req.getParameter("action");
        } catch (IllegalStateException tooLarge) {
            WebUtil.flash(req, "Ảnh quá lớn, chỉ nhận ảnh tối đa 2MB.", "error");
            redirect(req, resp);
            return;
        }
        if ("save".equals(action)) {
            save(req, resp);
        } else if ("delete".equals(action)) {
            try {
                productService.softDelete(WebUtil.parseInt(req.getParameter("id"), 0));
                WebUtil.flash(req, "Đã chuyển sản phẩm vào Thùng rác.", "success");
            } catch (ValidationException e) {
                WebUtil.flash(req, e.getMessage(), "error");
            }
            redirect(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void save(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int id = WebUtil.parseInt(req.getParameter("id"), 0);
        Product existing = id > 0 ? productService.findById(id) : null;
        if (id > 0 && existing == null) {
            WebUtil.flash(req, "Sản phẩm không tồn tại hoặc đã bị xóa.", "error");
            redirect(req, resp);
            return;
        }

        Product p = new Product();
        p.setId(id);
        p.setName(req.getParameter("name"));
        p.setSlug(req.getParameter("slug"));
        p.setCategoryId(WebUtil.parseInt(req.getParameter("categoryId"), 0));
        p.setPrice(parseLong(req.getParameter("price")));
        p.setDescription(req.getParameter("description"));
        p.setFeatured("true".equals(req.getParameter("featured")));
        if (existing != null) {
            p.setImage(existing.getImage());
            p.setImageThumb(existing.getImageThumb());
        }

        // Biến thể: mỗi màu được chọn x mọi size đang dùng, tồn kho lấy từ ô stock_{colorId}_{sizeId}
        Set<Integer> colorIds = new LinkedHashSet<>();
        String[] rawColors = req.getParameterValues("colorIds");
        if (rawColors != null) {
            for (String c : rawColors) {
                int cid = WebUtil.parseInt(c, 0);
                if (cid > 0) {
                    colorIds.add(cid);
                }
            }
        }
        List<ProductVariant> variants = new ArrayList<>();
        Map<String, Integer> stockMap = new HashMap<>();
        boolean badStock = false;
        for (Integer cid : colorIds) {
            for (Size sz : attributeService.findSizes()) {
                String key = cid + "_" + sz.getId();
                String raw = trim(req.getParameter("stock_" + key));
                int stock = raw.isEmpty() ? 0 : WebUtil.parseInt(raw, -1);
                badStock |= stock < 0;
                ProductVariant v = new ProductVariant();
                v.setColorId(cid);
                v.setSizeId(sz.getId());
                v.setStock(stock);
                variants.add(v);
                stockMap.put(key, Math.max(stock, 0));
            }
        }

        Part image = req.getPart("imageFile");
        String stored = null;
        try {
            if (badStock) {
                throw new ValidationException("stock", "Tồn kho phải là số nguyên từ 0 đến 99.999.");
            }
            if (ImageUploadUtil.hasFile(image)) {
                String base = p.getSlug() == null || p.getSlug().isBlank() ? p.getName() : p.getSlug();
                stored = ImageUploadUtil.store(getServletContext(), image, "imageFile", IMAGE_DIR, base);
                p.setImage(stored);
                p.setImageThumb(ImageUploadUtil.thumbOf(stored));
            }
            if (id == 0) {
                productService.create(p, variants);
            } else {
                productService.update(p, variants);
            }
            WebUtil.flash(req, (id == 0 ? "Đã thêm sản phẩm \"" : "Đã lưu sản phẩm \"") + p.getName() + "\".", "success");
            redirect(req, resp);
        } catch (ValidationException e) {
            ImageUploadUtil.deleteQuietly(getServletContext(), stored);
            req.setAttribute("errors", e.getErrors().isEmpty() ? Map.of("name", e.getMessage()) : e.getErrors());
            AdminProductFormServlet.prepareForm(req, existing, stockMap, colorIds);
            req.getRequestDispatcher(AdminProductFormServlet.VIEW).forward(req, resp);
        }
    }

    private static long parseLong(String s) {
        try {
            return s == null ? 0 : Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static void redirect(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.sendRedirect(req.getContextPath() + "/admin/products");
    }
}
