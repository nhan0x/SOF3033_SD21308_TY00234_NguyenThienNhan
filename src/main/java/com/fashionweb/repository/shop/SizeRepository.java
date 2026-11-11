package com.fashionweb.repository.shop;

import com.fashionweb.exception.DataAccessException;
import com.fashionweb.model.Size;
import com.fashionweb.util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Truy vấn bảng sizes.
 *
 * <p>Chỉ chứa SQL (PreparedStatement) và chuyển ResultSet thành entity. Kết nối lấy từ DBContext.getConnection().
 */
public class SizeRepository {

    /**
     * SELECT is_deleted = 0 ORDER BY sort_order.
     */
    public List<Size> findAll() {
        List<Size> list = new ArrayList<>();
        String sql = "SELECT * FROM sizes WHERE is_deleted = 0 ORDER BY sort_order, id";
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc danh sách size", e);
        }
        return list;
    }

    /**
     * Size đã xóa mềm, mới xóa trước (Thùng rác).
     */
    public List<Size> findDeleted() {
        List<Size> list = new ArrayList<>();
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM sizes WHERE is_deleted = 1 ORDER BY deleted_at DESC, id DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc size đã xóa", e);
        }
        return list;
    }

    /**
     * INSERT, trả id.
     */
    public int insert(Size s) {
        String sql = "INSERT INTO sizes (name, sort_order) VALUES (?, ?)";
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getName());
            ps.setInt(2, s.getSortOrder());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi thêm size", e);
        }
    }

    /**
     * UPDATE.
     */
    public void update(Size s) {
        String sql = "UPDATE sizes SET name = ?, sort_order = ? WHERE id = ?";
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, s.getName());
            ps.setInt(2, s.getSortOrder());
            ps.setInt(3, s.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi sửa size", e);
        }
    }

    /**
     * Xóa mềm hoặc khôi phục.
     */
    public void setDeleted(int id, boolean deleted) {
        String sql = deleted
                ? "UPDATE sizes SET is_deleted = 1, deleted_at = NOW() WHERE id = ?"
                : "UPDATE sizes SET is_deleted = 0, deleted_at = NULL WHERE id = ?";
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi cập nhật trạng thái xóa của size", e);
        }
    }

    /**
     * SELECT theo id, kể cả đã xóa mềm. null nếu không có.
     */
    public Size findById(int id) {
        String sql = "SELECT * FROM sizes WHERE id = ?";
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc size theo id", e);
        }
    }

    /**
     * true nếu tên size đã có ở size khác (không phân biệt hoa thường, tính cả size đã xóa mềm). excludeId = 0 khi thêm.
     */
    public boolean existsName(String name, int excludeId) {
        String sql = "SELECT 1 FROM sizes WHERE name = ? AND id <> ?";
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi kiểm tra tên size", e);
        }
    }

    /**
     * Số sản phẩm chưa xóa mềm có biến thể dùng size này.
     */
    public int countProductsUsing(int sizeId) {
        String sql = "SELECT COUNT(DISTINCT v.product_id) FROM product_variants v "
                + "JOIN products p ON p.id = v.product_id WHERE v.size_id = ? AND p.is_deleted = 0";
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, sizeId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đếm sản phẩm dùng size", e);
        }
    }

    private Size map(ResultSet rs) throws SQLException {
        Size s = new Size();
        s.setId(rs.getInt("id"));
        s.setName(rs.getString("name"));
        s.setSortOrder(rs.getInt("sort_order"));
        s.setDeleted(rs.getBoolean("is_deleted"));
        s.setDeletedAt(rs.getTimestamp("deleted_at"));
        return s;
    }
}
