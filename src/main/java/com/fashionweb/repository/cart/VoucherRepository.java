package com.fashionweb.repository.cart;

import com.fashionweb.constant.DiscountType;
import com.fashionweb.exception.DataAccessException;
import com.fashionweb.model.Voucher;
import com.fashionweb.util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Truy vấn bảng vouchers.
 *
 * <p>Chỉ chứa SQL (PreparedStatement) và chuyển ResultSet thành entity. Kết nối lấy từ DBContext.getConnection().
 * Các hàm increaseUsed / decreaseUsed nhận Connection để chạy chung transaction đặt hàng, hủy đơn.
 */
public class VoucherRepository {

    private static final String SELECT = "SELECT v.*, c.name AS category_name FROM vouchers v "
            + "LEFT JOIN categories c ON c.id = v.category_id ";

    /**
     * SELECT theo code (không phân biệt hoa thường nhờ collation _ci).
     */
    public Voucher findByCode(String code) {
        return findOne(SELECT + "WHERE v.code = ?", code);
    }

    /**
     * SELECT theo id.
     */
    public Voucher findById(int id) {
        return findOne(SELECT + "WHERE v.id = ?", id);
    }

    /**
     * SELECT kèm tên danh mục, mới tạo trước.
     */
    public List<Voucher> findAll() {
        List<Voucher> list = new ArrayList<>();
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT + "ORDER BY v.created_at DESC, v.id DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc danh sách voucher", e);
        }
        return list;
    }

    /**
     * Số đơn của user dùng voucher này và chưa bị hủy.
     */
    public int countUsedByUser(int voucherId, int userId) {
        String sql = "SELECT COUNT(*) FROM orders WHERE voucher_id = ? AND user_id = ? AND status <> 'CANCELLED'";
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, voucherId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đếm lượt dùng voucher", e);
        }
    }

    /**
     * Số đơn (mọi trạng thái) có dùng voucher này; còn đơn thì không xóa được vì khóa ngoại orders.voucher_id.
     */
    public int countOrders(int voucherId) {
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM orders WHERE voucher_id = ?")) {
            ps.setInt(1, voucherId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đếm đơn dùng voucher", e);
        }
    }

    /**
     * INSERT, trả id.
     */
    public int insert(Voucher v) {
        String sql = "INSERT INTO vouchers (code, description, discount_type, discount_value, max_discount, min_order_amount, "
                + "category_id, start_date, end_date, usage_limit, per_user_limit, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, v.getCode());
            int i = bindEditable(ps, v, 2);
            ps.setBoolean(i, v.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi thêm voucher", e);
        }
    }

    /**
     * UPDATE (không đổi code và số lượt đã dùng; có đổi is_active theo công tắc trong form).
     */
    public void update(Voucher v) {
        String sql = "UPDATE vouchers SET description = ?, discount_type = ?, discount_value = ?, max_discount = ?, "
                + "min_order_amount = ?, category_id = ?, start_date = ?, end_date = ?, usage_limit = ?, per_user_limit = ?, is_active = ? WHERE id = ?";
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int i = bindEditable(ps, v, 1);
            ps.setBoolean(i++, v.isActive());
            ps.setInt(i, v.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi sửa voucher", e);
        }
    }

    /**
     * UPDATE is_active.
     */
    public void setActive(int id, boolean active) {
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE vouchers SET is_active = ? WHERE id = ?")) {
            ps.setBoolean(1, active);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi bật/tắt voucher", e);
        }
    }

    /**
     * DELETE.
     */
    public void delete(int id) {
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM vouchers WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi xóa voucher", e);
        }
    }

    /**
     * UPDATE used_count = used_count + 1 WHERE used_count &lt; usage_limit. Trả false nếu hết lượt.
     * Chạy trên Connection của transaction đặt hàng.
     */
    public boolean increaseUsed(Connection con, int voucherId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE vouchers SET used_count = used_count + 1 WHERE id = ? AND used_count < usage_limit")) {
            ps.setInt(1, voucherId);
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Trả lượt khi hủy đơn. Chạy trên Connection của transaction hủy đơn.
     */
    public void decreaseUsed(Connection con, int voucherId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE vouchers SET used_count = used_count - 1 WHERE id = ? AND used_count > 0")) {
            ps.setInt(1, voucherId);
            ps.executeUpdate();
        }
    }

    private int bindEditable(PreparedStatement ps, Voucher v, int i) throws SQLException {
        ps.setString(i++, v.getDescription());
        ps.setString(i++, v.getDiscountType().name());
        ps.setLong(i++, v.getDiscountValue());
        if (v.getMaxDiscount() == null) {
            ps.setNull(i++, Types.DECIMAL);
        } else {
            ps.setLong(i++, v.getMaxDiscount());
        }
        ps.setLong(i++, v.getMinOrderAmount());
        if (v.getCategoryId() == null) {
            ps.setNull(i++, Types.INTEGER);
        } else {
            ps.setInt(i++, v.getCategoryId());
        }
        ps.setTimestamp(i++, new Timestamp(v.getStartDate().getTime()));
        ps.setTimestamp(i++, new Timestamp(v.getEndDate().getTime()));
        ps.setInt(i++, v.getUsageLimit());
        ps.setInt(i++, v.getPerUserLimit());
        return i;
    }

    private Voucher findOne(String sql, Object param) {
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (param instanceof Integer n) {
                ps.setInt(1, n);
            } else {
                ps.setString(1, (String) param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc voucher", e);
        }
    }

    private Voucher map(ResultSet rs) throws SQLException {
        Voucher v = new Voucher();
        v.setId(rs.getInt("id"));
        v.setCode(rs.getString("code"));
        v.setDescription(rs.getString("description"));
        v.setDiscountType(DiscountType.valueOf(rs.getString("discount_type")));
        v.setDiscountValue(rs.getLong("discount_value"));
        long max = rs.getLong("max_discount");
        v.setMaxDiscount(rs.wasNull() ? null : max);
        v.setMinOrderAmount(rs.getLong("min_order_amount"));
        int cat = rs.getInt("category_id");
        v.setCategoryId(rs.wasNull() ? null : cat);
        v.setStartDate(rs.getTimestamp("start_date"));
        v.setEndDate(rs.getTimestamp("end_date"));
        v.setUsageLimit(rs.getInt("usage_limit"));
        v.setUsedCount(rs.getInt("used_count"));
        v.setPerUserLimit(rs.getInt("per_user_limit"));
        v.setActive(rs.getBoolean("is_active"));
        v.setCreatedAt(rs.getTimestamp("created_at"));
        v.setCategoryName(rs.getString("category_name"));
        return v;
    }
}
