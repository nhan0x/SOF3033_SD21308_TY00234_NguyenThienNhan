package com.fashionweb.repository.shop;

import com.fashionweb.exception.DataAccessException;
import com.fashionweb.model.Category;
import com.fashionweb.util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Truy vấn bảng categories.
 *
 * <p>Chỉ chứa SQL (PreparedStatement) và chuyển ResultSet thành entity. Kết nối lấy từ DBContext.getConnection().
 */
public class CategoryRepository {

    /** Số sản phẩm đang bán (chưa xóa mềm) của danh mục. */
    private static final String PRODUCT_COUNT =
            "(SELECT COUNT(*) FROM products p WHERE p.category_id = c.id AND p.is_deleted = 0) AS product_count";

    /**
     * SELECT kèm COUNT sản phẩm, is_deleted = 0.
     */
    public List<Category> findAll() {
        String sql = "SELECT c.*, " + PRODUCT_COUNT + " FROM categories c WHERE c.is_deleted = 0 ORDER BY c.id";
        return queryList(sql);
    }

    /**
     * Danh mục chưa xóa mềm có tên chứa {@code q}, kèm số sản phẩm đang bán.
     */
    public List<Category> searchByName(String q) {
        if (q == null || q.isBlank()) {
            return List.of();
        }
        String sql = "SELECT c.*, " + PRODUCT_COUNT
                + " FROM categories c WHERE c.is_deleted = 0 AND c.name LIKE ? ESCAPE '!' ORDER BY c.id";
        String pattern = "%" + q.trim().replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        List<Category> list = new ArrayList<>();
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi tìm danh mục theo tên", e);
        }
        return list;
    }

    /**
     * SELECT theo slug (chỉ danh mục chưa xóa mềm). null nếu không có.
     */
    public Category findBySlug(String slug) {
        String sql = "SELECT c.*, " + PRODUCT_COUNT + " FROM categories c WHERE c.slug = ? AND c.is_deleted = 0";
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, slug);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc danh mục theo slug", e);
        }
    }

    /**
     * INSERT, trả id.
     */
    public int insert(Category c) {
        String sql = "INSERT INTO categories (name, slug, image) VALUES (?, ?, ?)";
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getName());
            ps.setString(2, c.getSlug());
            ps.setString(3, c.getImage());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi thêm danh mục", e);
        }
    }

    /**
     * UPDATE tên, slug, ảnh.
     */
    public void update(Category c) {
        String sql = "UPDATE categories SET name = ?, slug = ?, image = ? WHERE id = ?";
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getName());
            ps.setString(2, c.getSlug());
            ps.setString(3, c.getImage());
            ps.setInt(4, c.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi sửa danh mục", e);
        }
    }

    /**
     * Xóa mềm hoặc khôi phục.
     */
    public void setDeleted(int id, boolean deleted) {
        String sql = deleted
                ? "UPDATE categories SET is_deleted = 1, deleted_at = NOW() WHERE id = ?"
                : "UPDATE categories SET is_deleted = 0, deleted_at = NULL WHERE id = ?";
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi cập nhật trạng thái xóa của danh mục", e);
        }
    }

    /**
     * SELECT theo id, kể cả danh mục đã xóa mềm (Admin dùng khi sửa, khôi phục). null nếu không có.
     */
    public Category findById(int id) {
        String sql = "SELECT c.*, " + PRODUCT_COUNT + " FROM categories c WHERE c.id = ?";
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc danh mục theo id", e);
        }
    }

    /**
     * Danh mục đã xóa mềm, mới xóa trước (cho Thùng rác).
     */
    public List<Category> findDeleted() {
        String sql = "SELECT c.*, " + PRODUCT_COUNT + " FROM categories c WHERE c.is_deleted = 1 ORDER BY c.deleted_at DESC";
        return queryList(sql);
    }

    /**
     * true nếu slug đã có ở danh mục khác (tính cả danh mục đã xóa mềm vì cột slug là UNIQUE).
     * excludeId = 0 khi thêm mới.
     */
    public boolean existsSlug(String slug, int excludeId) {
        String sql = "SELECT 1 FROM categories WHERE slug = ? AND id <> ?";
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, slug);
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi kiểm tra slug danh mục", e);
        }
    }

    private List<Category> queryList(String sql) {
        List<Category> list = new ArrayList<>();
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc danh sách danh mục", e);
        }
        return list;
    }

    private Category map(ResultSet rs) throws SQLException {
        Category c = new Category();
        c.setId(rs.getInt("id"));
        c.setName(rs.getString("name"));
        c.setSlug(rs.getString("slug"));
        c.setImage(rs.getString("image"));
        c.setDeleted(rs.getBoolean("is_deleted"));
        c.setDeletedAt(rs.getTimestamp("deleted_at"));
        c.setCreatedAt(rs.getTimestamp("created_at"));
        c.setProductCount(rs.getInt("product_count"));
        return c;
    }
}
