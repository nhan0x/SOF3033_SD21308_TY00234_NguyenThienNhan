package com.fashionweb.repository.account;

import com.fashionweb.exception.DataAccessException;
import com.fashionweb.model.Product;
import com.fashionweb.util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Truy vấn bảng wishlists.
 *
 * <p>Chỉ chứa SQL (PreparedStatement) và chuyển ResultSet thành entity. Kết nối lấy từ DBContext.getConnection().
 */
public class WishlistRepository {

    /**
     * JOIN products, chỉ sản phẩm chưa xóa mềm, mới thêm trước. Kèm điểm, số đánh giá và tổng tồn để hiện thẻ sản phẩm.
     */
    public List<Product> findProductsByUser(int userId) {
        String sql = "SELECT p.*, "
                + "COALESCE((SELECT AVG(r.rating) FROM reviews r WHERE r.product_id = p.id AND r.is_hidden = 0), 0) AS avg_rating, "
                + "(SELECT COUNT(*) FROM reviews r WHERE r.product_id = p.id AND r.is_hidden = 0) AS review_count, "
                + "COALESCE((SELECT SUM(v.stock) FROM product_variants v WHERE v.product_id = p.id), 0) AS stock_total "
                + "FROM wishlists w JOIN products p ON p.id = w.product_id "
                + "WHERE w.user_id = ? AND p.is_deleted = 0 ORDER BY w.created_at DESC";
        List<Product> list = new ArrayList<>();
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Product p = new Product();
                    p.setId(rs.getInt("id"));
                    p.setCategoryId(rs.getInt("category_id"));
                    p.setName(rs.getString("name"));
                    p.setSlug(rs.getString("slug"));
                    p.setPrice(rs.getLong("price"));
                    p.setImage(rs.getString("image"));
                    p.setImageThumb(rs.getString("image_thumb"));
                    p.setAvgRating(Math.round(rs.getDouble("avg_rating") * 10) / 10.0);
                    p.setReviewCount(rs.getInt("review_count"));
                    p.setStockTotal(rs.getInt("stock_total"));
                    list.add(p);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc danh sách yêu thích", e);
        }
        return list;
    }

    /**
     * Đã có chưa.
     */
    public boolean exists(int userId, int productId) {
        return queryInt("SELECT COUNT(*) FROM wishlists WHERE user_id = ? AND product_id = ?", userId, productId) > 0;
    }

    /**
     * INSERT IGNORE (thêm hai lần không lỗi, không trùng).
     */
    public void insert(int userId, int productId) {
        update("INSERT IGNORE INTO wishlists (user_id, product_id) VALUES (?, ?)", userId, productId);
    }

    /**
     * DELETE.
     */
    public void delete(int userId, int productId) {
        update("DELETE FROM wishlists WHERE user_id = ? AND product_id = ?", userId, productId);
    }

    /**
     * COUNT (chỉ sản phẩm chưa xóa mềm).
     */
    public int count(int userId) {
        return queryInt("SELECT COUNT(*) FROM wishlists w JOIN products p ON p.id = w.product_id "
                + "WHERE w.user_id = ? AND p.is_deleted = 0", userId);
    }

    private int queryInt(String sql, int... params) {
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setInt(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc danh sách yêu thích", e);
        }
    }

    private void update(String sql, int userId, int productId) {
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, productId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi cập nhật danh sách yêu thích", e);
        }
    }
}
