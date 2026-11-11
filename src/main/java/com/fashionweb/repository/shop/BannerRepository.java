package com.fashionweb.repository.shop;

import com.fashionweb.exception.DataAccessException;
import com.fashionweb.model.Banner;
import com.fashionweb.util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Truy vấn bảng banners.
 *
 * <p>Chỉ chứa SQL (PreparedStatement) và chuyển ResultSet thành entity. Kết nối lấy từ DBContext.getConnection().
 */
public class BannerRepository {

    /**
     * SELECT ORDER BY sort_order. onlyActive = true chỉ lấy banner đang bật (trang chủ).
     */
    public List<Banner> findAll(boolean onlyActive) {
        String sql = "SELECT * FROM banners " + (onlyActive ? "WHERE is_active = 1 " : "") + "ORDER BY sort_order, id";
        List<Banner> list = new ArrayList<>();
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc banner", e);
        }
        return list;
    }

    /**
     * SELECT theo id, null nếu không có.
     */
    public Banner findById(int id) {
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM banners WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc banner", e);
        }
    }

    /**
     * INSERT, trả id mới.
     */
    public int insert(Banner b) {
        String sql = "INSERT INTO banners (title, image, link_url, sort_order, is_active) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, b.getTitle());
            ps.setString(2, b.getImage());
            ps.setString(3, b.getLinkUrl());
            ps.setInt(4, b.getSortOrder());
            ps.setBoolean(5, b.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi thêm banner", e);
        }
    }

    /**
     * UPDATE title, image, link_url, sort_order, is_active.
     */
    public void update(Banner b) {
        execute("UPDATE banners SET title = ?, image = ?, link_url = ?, sort_order = ?, is_active = ? WHERE id = ?",
                b.getTitle(), b.getImage(), b.getLinkUrl(), b.getSortOrder(), b.isActive(), b.getId());
    }

    /**
     * Bật hoặc tắt.
     */
    public void setActive(int id, boolean active) {
        execute("UPDATE banners SET is_active = ? WHERE id = ?", active, id);
    }

    /**
     * DELETE.
     */
    public void delete(int id) {
        execute("DELETE FROM banners WHERE id = ?", id);
    }

    private void execute(String sql, Object... params) {
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi cập nhật banner", e);
        }
    }

    private static Banner map(ResultSet rs) throws SQLException {
        Banner b = new Banner();
        b.setId(rs.getInt("id"));
        b.setTitle(rs.getString("title"));
        b.setImage(rs.getString("image"));
        b.setLinkUrl(rs.getString("link_url"));
        b.setSortOrder(rs.getInt("sort_order"));
        b.setActive(rs.getBoolean("is_active"));
        b.setCreatedAt(rs.getTimestamp("created_at"));
        return b;
    }
}
