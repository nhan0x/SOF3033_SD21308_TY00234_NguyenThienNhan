package com.fashionweb.service.shop;

import com.fashionweb.repository.shop.CategoryRepository;
import com.fashionweb.repository.shop.ProductRepository;
import com.fashionweb.exception.DataAccessException;
import com.fashionweb.exception.ValidationException;
import com.fashionweb.model.Category;
import com.fashionweb.model.Color;
import com.fashionweb.model.Product;
import com.fashionweb.model.ProductVariant;
import com.fashionweb.model.Size;
import com.fashionweb.util.DBContext;
import com.fashionweb.util.Page;
import com.fashionweb.util.SlugUtil;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Sản phẩm phía khách và dùng lại ở trang Admin.
 *
 * <p>Đã làm: phía khách (#7, #8); Admin thêm, sửa, xóa mềm (#24). findAllForFilter (#21), lowestStock (#26).
 * <p>Luồng gọi: Servlet -> Service -> Repository -> MySQL.
 */
public class ProductService {

    private static final long MIN_PRICE = 1000;
    private static final long MAX_PRICE = 99_999_999;
    private static final int MAX_STOCK = 99_999;

    private final ProductRepository productRepository = new ProductRepository();
    private final CategoryRepository categoryRepository = new CategoryRepository();
    private final AttributeService attributeService = new AttributeService();

    /**
     * Danh sách sản phẩm cho trang /products. Chỉ lấy sản phẩm chưa xóa mềm. sort: newest | price_asc | price_desc | bestseller | rating.
     * Mỗi Product có avgRating, reviewCount, soldCount, stockTotal, colors (view-only).
     */
    public Page<Product> search(String q, String categorySlug, Long minPrice, Long maxPrice, String colorCode, String sizeName, String sort, int page, int pageSize) {
        return productRepository.findPage(q, categorySlug, minPrice, maxPrice, colorCode, sizeName, sort, page, pageSize);
    }

    /**
     * Tối đa {@code limit} tên sản phẩm đang bán có chứa {@code q} (cho khung tìm kiếm nhanh).
     */
    public List<String> suggestNames(String q, int limit) {
        if (q == null || q.trim().length() < 2 || limit <= 0) {
            return List.of();
        }
        return productRepository.suggestNames(q.trim(), limit);
    }

    /**
     * Chi tiết sản phẩm kèm variants, colors, sizes, avgRating, reviewCount. null nếu không có hoặc đã xóa mềm.
     */
    public Product findBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return null;
        }
        Product p = productRepository.findBySlug(slug.trim());
        if (p == null) {
            return null;
        }
        // Màu và size lấy từ các biến thể. Repository trả biến thể theo sort_order của size,
        // nên size giữ thứ tự xuất hiện; màu sắp theo id.
        List<ProductVariant> variants = productRepository.findVariants(p.getId());
        Map<Integer, Color> colors = new TreeMap<>();
        Map<Integer, Size> sizes = new LinkedHashMap<>();
        for (ProductVariant v : variants) {
            colors.computeIfAbsent(v.getColorId(), id -> {
                Color c = new Color();
                c.setId(id);
                c.setName(v.getColorName());
                c.setHexCode(v.getColorHex());
                return c;
            });
            sizes.computeIfAbsent(v.getSizeId(), id -> {
                Size s = new Size();
                s.setId(id);
                s.setName(v.getSizeName());
                return s;
            });
        }
        p.setVariants(variants);
        p.setColors(new ArrayList<>(colors.values()));
        p.setSizes(new ArrayList<>(sizes.values()));
        return p;
    }

    /** Số ô ảnh tối đa của khung ảnh trang chi tiết: 2 ảnh sản phẩm, 2 ảnh người mẫu mặc. */
    public static final int GALLERY_SIZE = 4;

    /**
     * Ảnh cho khung ảnh trang chi tiết, tối đa GALLERY_SIZE. Chưa có ảnh phụ thì trả ảnh chính (0 hoặc 1 phần tử).
     */
    public List<String> galleryImages(Product p) {
        List<String> images = productRepository.findImages(p.getId());
        if (images.isEmpty()) {
            if (p.getImage() != null && !p.getImage().isBlank()) {
                images.add(p.getImage());
            }
            return images;
        }
        return images.size() > GALLERY_SIZE ? new ArrayList<>(images.subList(0, GALLERY_SIZE)) : images;
    }

    /**
     * Sản phẩm chưa xóa mềm theo id kèm variants (form sửa của Admin). null nếu không có.
     */
    public Product findById(int id) {
        Product p = productRepository.findById(id);
        if (p != null) {
            p.setVariants(productRepository.findVariants(id));
        }
        return p;
    }

    /**
     * n sản phẩm mới nhất (ORDER BY created_at DESC).
     */
    public List<Product> newest(int n) {
        return productRepository.findTop("newest", n);
    }

    /**
     * n sản phẩm nổi bật (Admin bật "Hiển thị nổi bật"), mới nhất trước.
     */
    public List<Product> featured(int n) {
        return productRepository.findFeatured(n);
    }

    /**
     * n sản phẩm bán chạy nhất: tổng quantity trong order_details của đơn không bị hủy.
     */
    public List<Product> bestSellers(int n) {
        return productRepository.findTop("bestseller", n);
    }

    /**
     * n sản phẩm cùng danh mục (trừ chính nó).
     */
    public List<Product> related(Product p, int n) {
        return productRepository.findRelated(p.getCategoryId(), p.getId(), n);
    }

    /**
     * Danh sách cho /admin/products. Tính stockTotal từ product_variants.
     */
    public Page<Product> searchForAdmin(String q, String categorySlug, int page, int pageSize) {
        return productRepository.findPageForAdmin(q, categorySlug, page, pageSize);
    }

    /**
     * Mọi sản phẩm (id, name) cho ô chọn lọc ở /admin/reviews.
     */
    public List<Product> findAllForFilter() {
        return productRepository.findAllNames();
    }

    /**
     * n sản phẩm có tổng tồn kho thấp nhất (dashboard).
     */
    public List<Product> lowestStock(int n) {
        return productRepository.findLowestStock(n);
    }

    /**
     * Thêm sản phẩm và các biến thể trong một transaction. Slug để trống thì tự sinh từ tên; slug phải duy nhất.
     */
    public void create(Product p, List<ProductVariant> variants) {
        p.setId(0);
        validate(p, variants);
        save(p, variants);
    }

    /**
     * Sửa sản phẩm và upsert biến thể trong một transaction. Biến thể bị bỏ chọn: xóa nếu chưa nằm trong đơn
     * hay giỏ nào, ngược lại đặt tồn = 0.
     */
    public void update(Product p, List<ProductVariant> variants) {
        if (p.getId() <= 0 || productRepository.findById(p.getId()) == null) {
            throw new ValidationException(null, "Sản phẩm không tồn tại hoặc đã bị xóa.");
        }
        validate(p, variants);
        save(p, variants);
    }

    /**
     * is_deleted = 1, deleted_at = NOW(). Đơn cũ giữ nguyên (order_details lưu sẵn tên, giá, ảnh).
     */
    public void softDelete(int id) {
        if (!productRepository.setDeleted(id, true)) {
            throw new ValidationException(null, "Sản phẩm không tồn tại hoặc đã nằm trong Thùng rác.");
        }
    }

    /**
     * Sản phẩm đã xóa mềm (Thùng rác).
     */
    public List<Product> findDeleted() {
        return productRepository.findDeleted();
    }

    /**
     * Khôi phục sản phẩm. Danh mục của nó đang bị xóa mềm thì báo rõ, cần khôi phục danh mục trước.
     */
    public void restore(int id) {
        Boolean categoryDeleted = productRepository.isCategoryDeletedOfDeletedProduct(id);
        if (categoryDeleted == null) {
            throw new ValidationException(null, "Sản phẩm không có trong Thùng rác.");
        }
        if (categoryDeleted) {
            throw new ValidationException(null, "Danh mục của sản phẩm này đang nằm trong Thùng rác. Hãy khôi phục danh mục trước.");
        }
        productRepository.setDeleted(id, false);
    }

    // ----- Hàm phụ -----

    private void save(Product p, List<ProductVariant> variants) {
        try (Connection con = DBContext.getConnection()) {
            con.setAutoCommit(false);
            try {
                if (p.getId() == 0) {
                    p.setId(productRepository.insert(con, p));
                } else {
                    productRepository.update(con, p);
                }
                productRepository.upsertVariants(con, p.getId(), variants);
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi lưu sản phẩm", e);
        }
    }

    /** Kiểm tra dữ liệu form; gom mọi lỗi theo tên trường rồi ném ValidationException. Chuẩn hóa name, slug, description. */
    private void validate(Product p, List<ProductVariant> variants) {
        Map<String, String> errors = new LinkedHashMap<>();
        String name = p.getName() == null ? "" : p.getName().trim();
        p.setName(name);
        if (name.length() < 2 || name.length() > 150) {
            errors.put("name", "Tên sản phẩm cần từ 2 đến 150 ký tự.");
        }
        String slug = p.getSlug() == null || p.getSlug().isBlank() ? SlugUtil.toSlug(name) : p.getSlug().trim();
        p.setSlug(slug);
        if (!SlugUtil.isValid(slug) || slug.length() > 170) {
            errors.put("slug", "Slug chỉ gồm chữ thường không dấu, số và dấu gạch ngang.");
        } else if (productRepository.existsSlug(slug, p.getId())) {
            errors.put("slug", "Slug đã được dùng cho sản phẩm khác (kể cả trong Thùng rác).");
        }
        Category c = p.getCategoryId() > 0 ? categoryRepository.findById(p.getCategoryId()) : null;
        if (c == null || c.isDeleted()) {
            errors.put("categoryId", "Hãy chọn danh mục.");
        }
        if (p.getPrice() < MIN_PRICE || p.getPrice() > MAX_PRICE) {
            errors.put("price", "Giá bán từ 1.000đ đến 99.999.999đ.");
        }
        if (p.getDescription() != null) {
            String d = p.getDescription().trim();
            p.setDescription(d.isEmpty() ? null : d);
            if (d.length() > 2000) {
                errors.put("description", "Mô tả tối đa 2000 ký tự.");
            }
        }
        if (variants == null || variants.isEmpty()) {
            errors.put("colorIds", "Chọn ít nhất một màu.");
        } else {
            Set<Integer> colorIds = new HashSet<>();
            Set<Integer> sizeIds = new HashSet<>();
            attributeService.findColors().forEach(col -> colorIds.add(col.getId()));
            attributeService.findSizes().forEach(sz -> sizeIds.add(sz.getId()));
            Set<String> seen = new HashSet<>();
            for (ProductVariant v : variants) {
                if (!colorIds.contains(v.getColorId()) || !sizeIds.contains(v.getSizeId())) {
                    errors.put("colorIds", "Màu hoặc size không hợp lệ (có thể đã bị xóa).");
                } else if (!seen.add(v.getColorId() + "_" + v.getSizeId())) {
                    errors.put("stock", "Mỗi cặp màu x size chỉ được nhập một lần.");
                } else if (v.getStock() < 0 || v.getStock() > MAX_STOCK) {
                    errors.put("stock", "Tồn kho phải là số nguyên từ 0 đến 99.999.");
                }
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
