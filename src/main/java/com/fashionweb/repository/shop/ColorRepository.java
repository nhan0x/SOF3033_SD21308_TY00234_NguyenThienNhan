package com.fashionweb.repository.shop;

import com.fashionweb.exception.DataAccessException;
import com.fashionweb.model.Color;
import com.fashionweb.util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Truy vấn bảng colors.
 *
 * <p>Chỉ chứa SQL (PreparedStatement) và chuyển ResultSet thành entity. Kết nối lấy từ DBContext.getConnection().
 */
public class ColorRepository {

    /**
     * SELECT is_deleted = 0.
     */
    public List<Color> findAll() {
        List<Color> list = new ArrayList<>();
        String sql = "SELECT * FROM colors WHERE is_deleted = 0 ORDER BY id";
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc danh sách màu", e);
        }
        return list;
    }

    /**
     * Màu đã xóa mềm, mới xóa trước (Thùng rác).
     */
    public List<Color> findDeleted() {
        List<Color> list = new ArrayList<>();
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM colors WHERE is_deleted = 1 ORDER BY deleted_at DESC, id DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc màu đã xóa", e);
        }
        return list;
    }

    /**
     * INSERT, trả id.
     */
    public int insert(Color c) {
        String sql = "INSERT INTO colors (name, code, hex_code) VALUES (?, ?, ?)";
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getName());
            ps.setString(2, c.getCode());
            ps.setString(3, c.getHexCode());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi thêm màu", e);
        }
    }

    /**
     * UPDATE.
     */
    public void update(Color c) {
        String sql = "UPDATE colors SET name = ?, code = ?, hex_code = ? WHERE id = ?";
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getName());
            ps.setString(2, c.getCode());
            ps.setString(3, c.getHexCode());
            ps.setInt(4, c.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi sửa màu", e);
        }
    }

    /**
     * Xóa mềm hoặc khôi phục.
     */
    public void setDeleted(int id, boolean deleted) {
        String sql = deleted
                ? "UPDATE colors SET is_deleted = 1, deleted_at = NOW() WHERE id = ?"
                : "UPDATE colors SET is_deleted = 0, deleted_at = NULL WHERE id = ?";
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi cập nhật trạng thái xóa của màu", e);
        }
    }

    /**
     * SELECT theo id, kể cả đã xóa mềm. null nếu không có.
     */
    public Color findById(int id) {
        String sql = "SELECT * FROM colors WHERE id = ?";
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc màu theo id", e);
        }
    }

    /**
     * true nếu mã màu đã có ở màu khác (tính cả màu đã xóa mềm vì cột code là UNIQUE). excludeId = 0 khi thêm.
     */
    public boolean existsCode(String code, int excludeId) {
        String sql = "SELECT 1 FROM colors WHERE code = ? AND id <> ?";
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi kiểm tra mã màu", e);
        }
    }

    /**
     * Số sản phẩm chưa xóa mềm có biến thể dùng màu này.
     */
    public int countProductsUsing(int colorId) {
        String sql = "SELECT COUNT(DISTINCT v.product_id) FROM product_variants v "
                + "JOIN products p ON p.id = v.product_id WHERE v.color_id = ? AND p.is_deleted = 0";
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, colorId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đếm sản phẩm dùng màu", e);
        }
    }

    private Color map(ResultSet rs) throws SQLException {
        Color c = new Color();
        c.setId(rs.getInt("id"));
        c.setName(rs.getString("name"));
        c.setCode(rs.getString("code"));
        c.setHexCode(rs.getString("hex_code"));
        c.setDeleted(rs.getBoolean("is_deleted"));
        c.setDeletedAt(rs.getTimestamp("deleted_at"));
        return c;
    }
}
