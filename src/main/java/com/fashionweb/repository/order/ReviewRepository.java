package com.fashionweb.repository.order;

import com.fashionweb.exception.DataAccessException;
import com.fashionweb.model.Review;
import com.fashionweb.util.DBContext;
import com.fashionweb.util.Page;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Truy vấn bảng reviews.
 *
 * <p>Đã làm: phần của khách (#8, #18), trang chủ (#9), Admin ẩn/hiện (#21).
 * <p>Chỉ chứa SQL (PreparedStatement) và chuyển ResultSet thành entity. Kết nối lấy từ DBContext.getConnection().
 */
public class ReviewRepository {

    /**
     * SELECT.
     */
    public Review findByUserAndProduct(int userId, int productId) {
        String sql = "SELECT r.*, u.full_name FROM reviews r JOIN users u ON u.id = r.user_id "
                + "WHERE r.user_id = ? AND r.product_id = ?";
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, productId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc đánh giá", e);
        }
    }

    /**
     * INSERT. Trả id vừa tạo.
     */
    public int insert(Review r) {
        String sql = "INSERT INTO reviews (user_id, product_id, rating, comment) VALUES (?, ?, ?, ?)";
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getUserId());
            ps.setInt(2, r.getProductId());
            ps.setInt(3, r.getRating());
            ps.setString(4, r.getComment());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi thêm đánh giá", e);
        }
    }

    /**
     * UPDATE rating, comment theo (user_id, product_id).
     */
    public void update(Review r) {
        executeUpdate("UPDATE reviews SET rating = ?, comment = ? WHERE user_id = ? AND product_id = ?",
                r.getRating(), r.getComment(), r.getUserId(), r.getProductId());
    }

    /**
     * DELETE đánh giá của user cho sản phẩm.
     */
    public void delete(int userId, int productId) {
        executeUpdate("DELETE FROM reviews WHERE user_id = ? AND product_id = ?", userId, productId);
    }

    /**
     * SELECT hidden = 0 JOIN users.
     */
    public List<Review> findVisibleByProduct(int productId) {
        String sql = "SELECT r.*, u.full_name FROM reviews r JOIN users u ON u.id = r.user_id "
                + "WHERE r.product_id = ? AND r.is_hidden = 0 ORDER BY r.created_at DESC, r.id DESC";
        List<Review> list = new ArrayList<>();
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc đánh giá của sản phẩm", e);
        }
        return list;
    }

    /**
     * n đánh giá chưa ẩn mới nhất kèm tên khách và sản phẩm (trang chủ). Bỏ đánh giá của sản phẩm đã xóa mềm.
     */
    public List<Review> findLatestVisible(int n) {
        String sql = "SELECT r.*, u.full_name, p.name AS product_name, p.slug AS product_slug, p.image AS product_image "
                + "FROM reviews r JOIN users u ON u.id = r.user_id JOIN products p ON p.id = r.product_id "
                + "WHERE r.is_hidden = 0 AND p.is_deleted = 0 ORDER BY r.created_at DESC, r.id DESC LIMIT ?";
        List<Review> list = new ArrayList<>();
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, n);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Review r = map(rs);
                    r.setProductName(rs.getString("product_name"));
                    r.setProductSlug(rs.getString("product_slug"));
                    r.setProductImage(rs.getString("product_image"));
                    list.add(r);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc đánh giá mới nhất", e);
        }
        return list;
    }

    /**
     * {số đánh giá chưa ẩn, điểm trung bình} trên toàn cửa hàng (trang chủ).
     */
    public double[] visibleStats() {
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*), COALESCE(AVG(rating), 0) FROM reviews WHERE is_hidden = 0");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return new double[]{rs.getLong(1), rs.getDouble(2)};
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi thống kê đánh giá", e);
        }
    }

    /**
     * SELECT phân trang cho Admin.
     */
    public Page<Review> findPage(Integer productId, Boolean hidden, int page, int pageSize) {
        StringBuilder where = new StringBuilder("WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();
        if (productId != null) {
            where.append("AND r.product_id = ? ");
            params.add(productId);
        }
        if (hidden != null) {
            where.append("AND r.is_hidden = ? ");
            params.add(hidden);
        }
        String from = "FROM reviews r JOIN users u ON u.id = r.user_id JOIN products p ON p.id = r.product_id ";
        try (Connection con = DBContext.getConnection()) {
            long total;
            try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) " + from + where)) {
                for (int i = 0; i < params.size(); i++) {
                    ps.setObject(i + 1, params.get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    total = rs.getLong(1);
                }
            }
            int totalPages = (int) Math.max(1, (total + pageSize - 1) / pageSize);
            int safePage = Math.min(Math.max(1, page), totalPages);
            List<Review> items = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement("SELECT r.*, u.full_name, p.name AS product_name, p.slug AS product_slug, "
                    + "p.image AS product_image " + from + where + "ORDER BY r.created_at DESC, r.id DESC LIMIT ? OFFSET ?")) {
                int i = 1;
                for (Object v : params) {
                    ps.setObject(i++, v);
                }
                ps.setInt(i++, pageSize);
                ps.setInt(i, (safePage - 1) * pageSize);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Review r = map(rs);
                        r.setProductName(rs.getString("product_name"));
                        r.setProductSlug(rs.getString("product_slug"));
                        r.setProductImage(rs.getString("product_image"));
                        items.add(r);
                    }
                }
            }
            return new Page<>(items, safePage, pageSize, total);
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc danh sách bình luận", e);
        }
    }

    /**
     * Trạng thái ẩn hiện tại của bình luận; null nếu không có.
     */
    public Boolean findHidden(int id) {
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT is_hidden FROM reviews WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBoolean(1) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc bình luận", e);
        }
    }

    /**
     * UPDATE hidden.
     */
    public void setHidden(int id, boolean hidden) {
        executeUpdate("UPDATE reviews SET is_hidden = ? WHERE id = ?", hidden, id);
    }

    // ----- Hàm phụ -----

    private void executeUpdate(String sql, Object... params) {
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi cập nhật đánh giá", e);
        }
    }

    private static Review map(ResultSet rs) throws SQLException {
        Review r = new Review();
        r.setId(rs.getInt("id"));
        r.setUserId(rs.getInt("user_id"));
        r.setProductId(rs.getInt("product_id"));
        r.setRating(rs.getInt("rating"));
        r.setComment(rs.getString("comment"));
        r.setHidden(rs.getBoolean("is_hidden"));
        r.setCreatedAt(rs.getTimestamp("created_at"));
        r.setUpdatedAt(rs.getTimestamp("updated_at"));
        r.setUserName(rs.getString("full_name"));
        return r;
    }
}
