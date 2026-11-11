package com.fashionweb.repository.shop;

import com.fashionweb.exception.DataAccessException;
import com.fashionweb.model.Color;
import com.fashionweb.model.Product;
import com.fashionweb.model.ProductVariant;
import com.fashionweb.model.Size;
import com.fashionweb.util.DBContext;
import com.fashionweb.util.Page;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Truy vấn bảng products và product_variants.
 *
 * <p>Chỉ chứa SQL (PreparedStatement) và chuyển ResultSet thành entity. Kết nối lấy từ DBContext.getConnection().
 * Câu lệnh có điều kiện động được ghép từ các đoạn SQL cố định trong code; giá trị người dùng nhập
 * luôn truyền qua dấu ? (không nối chuỗi từ tham số URL).
 */
public class ProductRepository {

    /** Cột sản phẩm kèm số liệu hiển thị: điểm và số đánh giá (bỏ đánh giá ẩn), số đã bán (đơn chưa hủy), tổng tồn. */
    private static final String SELECT_WITH_STATS = "SELECT p.*, c.name AS category_name, c.slug AS category_slug, "
            + "COALESCE((SELECT AVG(r.rating) FROM reviews r WHERE r.product_id = p.id AND r.is_hidden = 0), 0) AS avg_rating, "
            + "(SELECT COUNT(*) FROM reviews r WHERE r.product_id = p.id AND r.is_hidden = 0) AS review_count, "
            + "COALESCE((SELECT SUM(od.quantity) FROM order_details od JOIN orders o ON o.id = od.order_id "
            + "  WHERE od.product_id = p.id AND o.status <> 'CANCELLED'), 0) AS sold_count, "
            + "COALESCE((SELECT SUM(v.stock) FROM product_variants v WHERE v.product_id = p.id), 0) AS stock_total, "
            // Ảnh thứ hai trong khung ảnh (#82), thẻ sản phẩm hiện khi rê chuột (#89)
            + "(SELECT pi.image FROM product_images pi WHERE pi.product_id = p.id ORDER BY pi.sort_order, pi.id LIMIT 1 OFFSET 1) AS hover_image "
            + "FROM products p JOIN categories c ON c.id = p.category_id ";

    /** Chỉ sản phẩm đang bán: chưa xóa mềm và danh mục chưa xóa mềm. */
    private static final String VISIBLE = "WHERE p.is_deleted = 0 AND c.is_deleted = 0 ";

    /**
     * SELECT có lọc, sắp xếp, phân trang (LIMIT/OFFSET) và đếm tổng.
     * sort: newest | price_asc | price_desc | bestseller | rating (giá trị khác coi như newest).
     */
    public Page<Product> findPage(String q, String categorySlug, Long minPrice, Long maxPrice, String colorCode, String sizeName, String sort, int page, int pageSize) {
        StringBuilder where = new StringBuilder(VISIBLE);
        List<Object> params = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            // utf8mb4_unicode_ci: không phân biệt hoa thường và dấu ("ao" khớp "Áo")
            where.append("AND p.name LIKE ? ESCAPE '!' ");
            params.add("%" + q.trim().replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%");
        }
        if (categorySlug != null && !categorySlug.isBlank()) {
            where.append("AND c.slug = ? ");
            params.add(categorySlug);
        }
        if (minPrice != null) {
            where.append("AND p.price >= ? ");
            params.add(minPrice);
        }
        if (maxPrice != null) {
            where.append("AND p.price <= ? ");
            params.add(maxPrice);
        }
        if (colorCode != null && !colorCode.isBlank()) {
            where.append("AND EXISTS (SELECT 1 FROM product_variants v JOIN colors co ON co.id = v.color_id "
                    + "WHERE v.product_id = p.id AND co.code = ? AND co.is_deleted = 0) ");
            params.add(colorCode);
        }
        if (sizeName != null && !sizeName.isBlank()) {
            where.append("AND EXISTS (SELECT 1 FROM product_variants v JOIN sizes s ON s.id = v.size_id "
                    + "WHERE v.product_id = p.id AND s.name = ? AND s.is_deleted = 0) ");
            params.add(sizeName);
        }

        long total;
        List<Product> items;
        int safePage = Math.max(1, page);
        try (Connection con = DBContext.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT COUNT(*) FROM products p JOIN categories c ON c.id = p.category_id " + where)) {
                bind(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    total = rs.getLong(1);
                }
            }
            List<Object> pageParams = new ArrayList<>(params);
            pageParams.add(pageSize);
            pageParams.add((safePage - 1) * pageSize);
            items = queryList(con, SELECT_WITH_STATS + where + orderBy(sort) + "LIMIT ? OFFSET ?", pageParams);
            loadColors(con, items);
            loadSizes(con, items);
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi tìm sản phẩm", e);
        }
        return new Page<>(items, safePage, pageSize, total);
    }

    /**
     * n sản phẩm đang bán theo thứ tự sort (newest, bestseller, ...). Dùng cho trang chủ, gợi ý trong giỏ.
     */
    public List<Product> findTop(String sort, int n) {
        try (Connection con = DBContext.getConnection()) {
            List<Product> list = queryList(con, SELECT_WITH_STATS + VISIBLE + orderBy(sort) + "LIMIT ?", List.of(n));
            loadColors(con, list);
            loadSizes(con, list);
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc danh sách sản phẩm", e);
        }
    }

    /**
     * Mọi sản phẩm chưa xóa mềm (id, tên), sắp theo tên, cho ô lọc của Admin.
     */
    public List<Product> findAllNames() {
        List<Product> list = new ArrayList<>();
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT id, name, slug FROM products WHERE is_deleted = 0 ORDER BY name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Product p = new Product();
                p.setId(rs.getInt("id"));
                p.setName(rs.getString("name"));
                p.setSlug(rs.getString("slug"));
                list.add(p);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc tên sản phẩm", e);
        }
        return list;
    }

    /**
     * Gợi ý tối đa {@code limit} tên sản phẩm đang bán có chứa {@code q} (không phân biệt hoa thường và dấu).
     */
    public List<String> suggestNames(String q, int limit) {
        if (q == null || q.isBlank() || limit <= 0) {
            return List.of();
        }
        String sql = "SELECT p.name FROM products p JOIN categories c ON c.id = p.category_id "
                + VISIBLE + "AND p.name LIKE ? ESCAPE '!' ORDER BY p.created_at DESC, p.id DESC LIMIT ?";
        String pattern = "%" + q.trim().replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        List<String> names = new ArrayList<>();
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    names.add(rs.getString(1));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi gợi ý tên sản phẩm", e);
        }
        return names;
    }

    /**
     * Sản phẩm đã xóa mềm kèm tên danh mục, mới xóa trước (Thùng rác).
     */
    public List<Product> findDeleted() {
        try (Connection con = DBContext.getConnection()) {
            return queryList(con, SELECT_WITH_STATS + "WHERE p.is_deleted = 1 ORDER BY p.deleted_at DESC, p.id DESC", List.of());
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc sản phẩm đã xóa", e);
        }
    }

    /**
     * id danh mục và trạng thái xóa của danh mục chứa sản phẩm đã xóa mềm; null nếu sản phẩm không nằm trong Thùng rác.
     */
    public Boolean isCategoryDeletedOfDeletedProduct(int productId) {
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT c.is_deleted FROM products p JOIN categories c ON c.id = p.category_id "
                     + "WHERE p.id = ? AND p.is_deleted = 1")) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBoolean(1) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi kiểm tra sản phẩm đã xóa", e);
        }
    }

    /**
     * n sản phẩm chưa xóa mềm có tổng tồn kho thấp nhất (dashboard).
     */
    public List<Product> findLowestStock(int n) {
        try (Connection con = DBContext.getConnection()) {
            return queryList(con, SELECT_WITH_STATS + "WHERE p.is_deleted = 0 ORDER BY stock_total ASC, p.id LIMIT ?", List.of(n));
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc sản phẩm sắp hết hàng", e);
        }
    }

    /**
     * n sản phẩm nổi bật (is_featured = 1) đang bán, mới nhất trước.
     */
    public List<Product> findFeatured(int n) {
        try (Connection con = DBContext.getConnection()) {
            List<Product> list = queryList(con, SELECT_WITH_STATS + VISIBLE + "AND p.is_featured = 1 " + orderBy("newest") + "LIMIT ?", List.of(n));
            loadColors(con, list);
            loadSizes(con, list);
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc sản phẩm nổi bật", e);
        }
    }

    /**
     * SELECT một sản phẩm theo slug.
     */
    public Product findBySlug(String slug) {
        try (Connection con = DBContext.getConnection()) {
            List<Product> list = queryList(con, SELECT_WITH_STATS + VISIBLE + "AND p.slug = ?", List.of(slug));
            return list.isEmpty() ? null : list.get(0);
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc sản phẩm theo slug", e);
        }
    }

    /**
     * n sản phẩm đang bán cùng danh mục, trừ sản phẩm excludeId, mới nhất trước.
     */
    public List<Product> findRelated(int categoryId, int excludeId, int n) {
        try (Connection con = DBContext.getConnection()) {
            List<Product> list = queryList(con, SELECT_WITH_STATS + VISIBLE + "AND p.category_id = ? AND p.id <> ? "
                    + orderBy("newest") + "LIMIT ?", List.of(categoryId, excludeId, n));
            loadColors(con, list);
            loadSizes(con, list);
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc sản phẩm liên quan", e);
        }
    }

    /**
     * SELECT một sản phẩm chưa xóa mềm theo id (form sửa của Admin). null nếu không có.
     */
    public Product findById(int id) {
        try (Connection con = DBContext.getConnection()) {
            List<Product> list = queryList(con, SELECT_WITH_STATS + "WHERE p.is_deleted = 0 AND p.id = ?", List.of(id));
            return list.isEmpty() ? null : list.get(0);
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc sản phẩm theo id", e);
        }
    }

    /**
     * Ảnh của khung ảnh trang chi tiết (bảng product_images), theo sort_order. Rỗng nếu sản phẩm chưa có ảnh phụ.
     */
    public List<String> findImages(int productId) {
        List<String> list = new ArrayList<>();
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT image FROM product_images WHERE product_id = ? ORDER BY sort_order, id")) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("image"));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc ảnh sản phẩm", e);
        }
        return list;
    }

    /**
     * SELECT biến thể kèm tên màu, mã màu hex, tên size.
     */
    public List<ProductVariant> findVariants(int productId) {
        String sql = "SELECT v.*, co.name AS color_name, co.hex_code, s.name AS size_name "
                + "FROM product_variants v JOIN colors co ON co.id = v.color_id JOIN sizes s ON s.id = v.size_id "
                + "WHERE v.product_id = ? AND co.is_deleted = 0 AND s.is_deleted = 0 ORDER BY s.sort_order, s.id, co.id";
        List<ProductVariant> list = new ArrayList<>();
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ProductVariant v = new ProductVariant();
                    v.setId(rs.getInt("id"));
                    v.setProductId(rs.getInt("product_id"));
                    v.setColorId(rs.getInt("color_id"));
                    v.setSizeId(rs.getInt("size_id"));
                    v.setStock(rs.getInt("stock"));
                    v.setColorName(rs.getString("color_name"));
                    v.setColorHex(rs.getString("hex_code"));
                    v.setSizeName(rs.getString("size_name"));
                    list.add(v);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc biến thể sản phẩm", e);
        }
        return list;
    }

    /**
     * Admin: sản phẩm chưa xóa mềm, tìm theo tên, lọc danh mục, mới nhất trước, phân trang.
     * Trang vượt quá trang cuối thì lấy trang cuối.
     */
    public Page<Product> findPageForAdmin(String q, String categorySlug, int page, int pageSize) {
        StringBuilder where = new StringBuilder("WHERE p.is_deleted = 0 ");
        List<Object> params = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            where.append("AND p.name LIKE ? ESCAPE '!' ");
            params.add("%" + q.trim().replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%");
        }
        if (categorySlug != null && !categorySlug.isBlank()) {
            where.append("AND c.slug = ? ");
            params.add(categorySlug);
        }
        try (Connection con = DBContext.getConnection()) {
            long total;
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT COUNT(*) FROM products p JOIN categories c ON c.id = p.category_id " + where)) {
                bind(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    total = rs.getLong(1);
                }
            }
            int totalPages = (int) Math.max(1, (total + pageSize - 1) / pageSize);
            int safePage = Math.min(Math.max(1, page), totalPages);
            List<Object> pageParams = new ArrayList<>(params);
            pageParams.add(pageSize);
            pageParams.add((safePage - 1) * pageSize);
            List<Product> items = queryList(con, SELECT_WITH_STATS + where + orderBy("newest") + "LIMIT ? OFFSET ?", pageParams);
            return new Page<>(items, safePage, pageSize, total);
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi tìm sản phẩm (Admin)", e);
        }
    }

    /**
     * Slug đã có ở sản phẩm khác chưa (kể cả sản phẩm trong Thùng rác, vì cột slug là UNIQUE).
     */
    public boolean existsSlug(String slug, int excludeId) {
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT 1 FROM products WHERE slug = ? AND id <> ? LIMIT 1")) {
            ps.setString(1, slug);
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi kiểm tra slug sản phẩm", e);
        }
    }

    /**
     * INSERT (chạy trong transaction), trả id mới.
     */
    public int insert(Connection con, Product p) throws SQLException {
        String sql = "INSERT INTO products (category_id, name, slug, description, price, image, image_thumb, is_featured) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindProduct(ps, p);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    /**
     * UPDATE các cột của sản phẩm (chạy trong transaction).
     */
    public void update(Connection con, Product p) throws SQLException {
        String sql = "UPDATE products SET category_id = ?, name = ?, slug = ?, description = ?, price = ?, image = ?, "
                + "image_thumb = ?, is_featured = ? WHERE id = ? AND is_deleted = 0";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            bindProduct(ps, p);
            ps.setInt(9, p.getId());
            ps.executeUpdate();
        }
    }

    /**
     * INSERT ... ON DUPLICATE KEY UPDATE stock theo UNIQUE (product_id, color_id, size_id), nên mỗi cặp màu x size
     * chỉ có một biến thể. Biến thể không còn trong danh sách gửi lên: xóa nếu chưa nằm trong đơn hay giỏ nào,
     * ngược lại đặt tồn = 0 (giữ dòng để đơn cũ còn tham chiếu được).
     */
    public void upsertVariants(Connection con, int productId, List<ProductVariant> variants) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO product_variants (product_id, color_id, size_id, stock) VALUES (?, ?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE stock = VALUES(stock)")) {
            for (ProductVariant v : variants) {
                ps.setInt(1, productId);
                ps.setInt(2, v.getColorId());
                ps.setInt(3, v.getSizeId());
                ps.setInt(4, v.getStock());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        StringBuilder keep = new StringBuilder();
        for (ProductVariant v : variants) {
            keep.append(keep.length() == 0 ? "(?, ?)" : ", (?, ?)");
        }
        String notKept = variants.isEmpty() ? "" : " AND (color_id, size_id) NOT IN (" + keep + ")";
        String used = "(EXISTS (SELECT 1 FROM order_details d WHERE d.variant_id = product_variants.id) "
                + "OR EXISTS (SELECT 1 FROM cart_items ci WHERE ci.variant_id = product_variants.id))";
        try (PreparedStatement del = con.prepareStatement(
                     "DELETE FROM product_variants WHERE product_id = ?" + notKept + " AND NOT " + used);
             PreparedStatement zero = con.prepareStatement(
                     "UPDATE product_variants SET stock = 0 WHERE product_id = ?" + notKept)) {
            for (PreparedStatement ps : List.of(del, zero)) {
                int i = 1;
                ps.setInt(i++, productId);
                for (ProductVariant v : variants) {
                    ps.setInt(i++, v.getColorId());
                    ps.setInt(i++, v.getSizeId());
                }
                ps.executeUpdate();
            }
        }
    }

    /**
     * Cập nhật is_deleted và deleted_at (xóa mềm hoặc khôi phục). Trả false nếu không có dòng nào đổi.
     */
    public boolean setDeleted(int id, boolean deleted) {
        String sql = deleted
                ? "UPDATE products SET is_deleted = 1, deleted_at = NOW() WHERE id = ? AND is_deleted = 0"
                : "UPDATE products SET is_deleted = 0, deleted_at = NULL WHERE id = ? AND is_deleted = 1";
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi cập nhật trạng thái xóa sản phẩm", e);
        }
    }

    /**
     * UPDATE stock = stock - ? WHERE id = ? AND stock >= ?. Trả false nếu không đủ hàng (dùng trong transaction đặt đơn).
     */
    public boolean decreaseStock(Connection con, int variantId, int quantity) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE product_variants SET stock = stock - ? WHERE id = ? AND stock >= ?")) {
            ps.setInt(1, quantity);
            ps.setInt(2, variantId);
            ps.setInt(3, quantity);
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Cộng trả tồn kho khi hủy đơn (chạy trong transaction hủy đơn).
     */
    public void increaseStock(Connection con, int variantId, int quantity) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE product_variants SET stock = stock + ? WHERE id = ?")) {
            ps.setInt(1, quantity);
            ps.setInt(2, variantId);
            ps.executeUpdate();
        }
    }

    // ----- Hàm phụ -----

    private static String orderBy(String sort) {
        return switch (sort == null ? "" : sort) {
            case "price_asc" -> "ORDER BY p.price ASC, p.id DESC ";
            case "price_desc" -> "ORDER BY p.price DESC, p.id DESC ";
            case "bestseller" -> "ORDER BY sold_count DESC, p.created_at DESC, p.id DESC ";
            case "rating" -> "ORDER BY avg_rating DESC, review_count DESC, p.id DESC ";
            default -> "ORDER BY p.created_at DESC, p.id DESC ";
        };
    }

    private static void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object v = params.get(i);
            if (v instanceof Integer n) {
                ps.setInt(i + 1, n);
            } else if (v instanceof Long n) {
                ps.setLong(i + 1, n);
            } else {
                ps.setString(i + 1, (String) v);
            }
        }
    }

    private static void bindProduct(PreparedStatement ps, Product p) throws SQLException {
        ps.setInt(1, p.getCategoryId());
        ps.setString(2, p.getName());
        ps.setString(3, p.getSlug());
        ps.setString(4, p.getDescription());
        ps.setLong(5, p.getPrice());
        ps.setString(6, p.getImage());
        ps.setString(7, p.getImageThumb());
        ps.setBoolean(8, p.isFeatured());
    }

    private List<Product> queryList(Connection con, String sql, List<Object> params) throws SQLException {
        List<Product> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapWithStats(rs));
                }
            }
        }
        return list;
    }

    /** Gắn các size còn hàng (stock &gt; 0) cho từng sản phẩm, theo sort_order; thẻ sản phẩm hiện khi rê chuột. */
    private void loadSizes(Connection con, List<Product> products) throws SQLException {
        if (products.isEmpty()) {
            return;
        }
        Map<Integer, Product> byId = new LinkedHashMap<>();
        StringBuilder in = new StringBuilder();
        for (Product p : products) {
            p.setSizes(new ArrayList<>());
            byId.put(p.getId(), p);
            in.append(in.length() == 0 ? "?" : ", ?");
        }
        String sql = "SELECT v.product_id, s.id, s.name, s.sort_order FROM product_variants v JOIN sizes s ON s.id = v.size_id "
                + "WHERE s.is_deleted = 0 AND v.stock > 0 AND v.product_id IN (" + in + ") "
                + "GROUP BY v.product_id, s.id, s.name, s.sort_order ORDER BY v.product_id, s.sort_order, s.id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            int i = 1;
            for (Integer id : byId.keySet()) {
                ps.setInt(i++, id);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Size sz = new Size();
                    sz.setId(rs.getInt("id"));
                    sz.setName(rs.getString("name"));
                    sz.setSortOrder(rs.getInt("sort_order"));
                    byId.get(rs.getInt("product_id")).getSizes().add(sz);
                }
            }
        }
    }

    /** Gắn danh sách màu (swatch) cho từng sản phẩm bằng một câu truy vấn cho cả trang. */
    private void loadColors(Connection con, List<Product> products) throws SQLException {
        if (products.isEmpty()) {
            return;
        }
        Map<Integer, Product> byId = new LinkedHashMap<>();
        StringBuilder in = new StringBuilder();
        for (Product p : products) {
            p.setColors(new ArrayList<>());
            byId.put(p.getId(), p);
            in.append(in.length() == 0 ? "?" : ", ?");
        }
        String sql = "SELECT DISTINCT v.product_id, co.* FROM product_variants v JOIN colors co ON co.id = v.color_id "
                + "WHERE co.is_deleted = 0 AND v.product_id IN (" + in + ") ORDER BY v.product_id, co.id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            int i = 1;
            for (Integer id : byId.keySet()) {
                ps.setInt(i++, id);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Color c = new Color();
                    c.setId(rs.getInt("id"));
                    c.setName(rs.getString("name"));
                    c.setCode(rs.getString("code"));
                    c.setHexCode(rs.getString("hex_code"));
                    byId.get(rs.getInt("product_id")).getColors().add(c);
                }
            }
        }
    }

    private Product map(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getInt("id"));
        p.setCategoryId(rs.getInt("category_id"));
        p.setName(rs.getString("name"));
        p.setSlug(rs.getString("slug"));
        p.setDescription(rs.getString("description"));
        p.setPrice(rs.getLong("price"));
        p.setImage(rs.getString("image"));
        p.setImageThumb(rs.getString("image_thumb"));
        p.setFeatured(rs.getBoolean("is_featured"));
        p.setDeleted(rs.getBoolean("is_deleted"));
        p.setDeletedAt(rs.getTimestamp("deleted_at"));
        p.setCreatedAt(rs.getTimestamp("created_at"));
        p.setUpdatedAt(rs.getTimestamp("updated_at"));
        return p;
    }

    private Product mapWithStats(ResultSet rs) throws SQLException {
        Product p = map(rs);
        p.setCategoryName(rs.getString("category_name"));
        p.setCategorySlug(rs.getString("category_slug"));
        p.setAvgRating(Math.round(rs.getDouble("avg_rating") * 10) / 10.0);
        p.setReviewCount(rs.getInt("review_count"));
        p.setSoldCount(rs.getInt("sold_count"));
        p.setStockTotal(rs.getInt("stock_total"));
        p.setHoverImage(rs.getString("hover_image"));
        return p;
    }
}
