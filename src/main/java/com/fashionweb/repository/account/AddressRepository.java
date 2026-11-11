package com.fashionweb.repository.account;

import com.fashionweb.exception.DataAccessException;
import com.fashionweb.model.Address;
import com.fashionweb.util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Truy vấn bảng addresses.
 *
 * <p>Chỉ chứa SQL (PreparedStatement) và chuyển ResultSet thành entity. Kết nối lấy từ DBContext.getConnection().
 */
public class AddressRepository {

    /**
     * SELECT ORDER BY is_default DESC, id.
     */
    public List<Address> findByUser(int userId) {
        List<Address> list = new ArrayList<>();
        String sql = "SELECT * FROM addresses WHERE user_id = ? ORDER BY is_default DESC, id";
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc sổ địa chỉ", e);
        }
        return list;
    }

    /**
     * SELECT theo id. null nếu không có.
     */
    public Address findById(int id) {
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM addresses WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc địa chỉ", e);
        }
    }

    /**
     * INSERT, trả id.
     */
    public int insert(Address a) {
        String sql = "INSERT INTO addresses (receiver_name, phone, address_line, ward, district, city, is_default, user_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, a);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi thêm địa chỉ", e);
        }
    }

    /**
     * UPDATE (chỉ địa chỉ của đúng user).
     */
    public void update(Address a) {
        String sql = "UPDATE addresses SET receiver_name = ?, phone = ?, address_line = ?, ward = ?, district = ?, city = ?, "
                + "is_default = ? WHERE user_id = ? AND id = ?";
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, a);
            ps.setInt(9, a.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi sửa địa chỉ", e);
        }
    }

    /**
     * DELETE.
     */
    public void delete(int id) {
        execute("DELETE FROM addresses WHERE id = ?", id, "Lỗi xóa địa chỉ");
    }

    /**
     * UPDATE is_default = 0 cho mọi địa chỉ của user.
     */
    public void clearDefault(int userId) {
        execute("UPDATE addresses SET is_default = 0 WHERE user_id = ?", userId, "Lỗi bỏ địa chỉ mặc định");
    }

    /**
     * UPDATE is_default = 1.
     */
    public void setDefault(int id) {
        execute("UPDATE addresses SET is_default = 1 WHERE id = ?", id, "Lỗi đặt địa chỉ mặc định");
    }

    private void execute(String sql, int id, String error) {
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException(error, e);
        }
    }

    private void bind(PreparedStatement ps, Address a) throws SQLException {
        ps.setString(1, a.getReceiverName());
        ps.setString(2, a.getPhone());
        ps.setString(3, a.getAddressLine());
        ps.setString(4, a.getWard());
        ps.setString(5, a.getDistrict());
        ps.setString(6, a.getCity());
        ps.setBoolean(7, a.isDefaultAddress());
        ps.setInt(8, a.getUserId());
    }

    private Address map(ResultSet rs) throws SQLException {
        Address a = new Address();
        a.setId(rs.getInt("id"));
        a.setUserId(rs.getInt("user_id"));
        a.setReceiverName(rs.getString("receiver_name"));
        a.setPhone(rs.getString("phone"));
        a.setAddressLine(rs.getString("address_line"));
        a.setWard(rs.getString("ward"));
        a.setDistrict(rs.getString("district"));
        a.setCity(rs.getString("city"));
        a.setDefaultAddress(rs.getBoolean("is_default"));
        a.setCreatedAt(rs.getTimestamp("created_at"));
        StringBuilder full = new StringBuilder(a.getAddressLine());
        for (String part : new String[]{a.getWard(), a.getDistrict(), a.getCity()}) {
            if (part != null && !part.isBlank()) {
                full.append(", ").append(part);
            }
        }
        a.setFullAddress(full.toString());
        return a;
    }
}
