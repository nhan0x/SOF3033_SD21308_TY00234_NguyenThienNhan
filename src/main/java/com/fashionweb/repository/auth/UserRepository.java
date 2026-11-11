package com.fashionweb.repository.auth;

import com.fashionweb.constant.Role;
import com.fashionweb.exception.DataAccessException;
import com.fashionweb.model.User;
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
 * Truy vấn bảng users.
 *
 * <p>Đã làm: tài khoản (#1, #4, #74), Admin người dùng (#6).
 * <p>Chỉ chứa SQL (PreparedStatement) và chuyển ResultSet thành entity. Kết nối lấy từ DBContext.getConnection().
 */
public class UserRepository {

    /**
     * SELECT theo email.
     */
    public User findByEmail(String email) {
        return findOne("SELECT * FROM users WHERE email = ?", email);
    }

    /**
     * SELECT theo id.
     */
    public User findById(int id) {
        return findOne("SELECT * FROM users WHERE id = ?", id);
    }

    /**
     * INSERT, trả id.
     */
    public int insert(User u) {
        String sql = "INSERT INTO users (email, full_name, phone, role, is_active) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getEmail());
            ps.setString(2, u.getFullName());
            ps.setString(3, u.getPhone());
            ps.setString(4, u.getRole().name());
            ps.setBoolean(5, u.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi thêm người dùng", e);
        }
    }

    /**
     * true nếu email đã có tài khoản (không phân biệt hoa thường).
     */
    public boolean existsByEmail(String email) {
        return findByEmail(email) != null;
    }

    private User findOne(String sql, Object param) {
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
            throw new DataAccessException("Lỗi đọc người dùng", e);
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setEmail(rs.getString("email"));
        u.setFullName(rs.getString("full_name"));
        u.setPhone(rs.getString("phone"));
        u.setRole(Role.valueOf(rs.getString("role")));
        u.setActive(rs.getBoolean("is_active"));
        u.setCreatedAt(rs.getTimestamp("created_at"));
        u.setUpdatedAt(rs.getTimestamp("updated_at"));
        return u;
    }

    /**
     * UPDATE.
     */
    public void updateProfile(int id, String fullName, String phone) {
        executeUpdate("UPDATE users SET full_name = ?, phone = ? WHERE id = ?", fullName, phone, id);
    }

    /**
     * UPDATE is_active.
     */
    public void setActive(int id, boolean active) {
        executeUpdate("UPDATE users SET is_active = ? WHERE id = ?", active, id);
    }

    /**
     * UPDATE role.
     */
    public void setRole(int id, Role role) {
        executeUpdate("UPDATE users SET role = ? WHERE id = ?", role.name(), id);
    }

    /**
     * Số đơn hàng của user (mọi trạng thái).
     */
    public int countOrders(int userId) {
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM orders WHERE user_id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đếm đơn hàng của người dùng", e);
        }
    }

    private void executeUpdate(String sql, Object... params) {
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi cập nhật người dùng", e);
        }
    }

    /**
     * SELECT phân trang kèm COUNT đơn của mỗi user.
     */
    public Page<User> findPage(String q, Role role, int page, int pageSize) {
        StringBuilder where = new StringBuilder("WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            where.append("AND (u.email LIKE ? ESCAPE '!' OR u.full_name LIKE ? ESCAPE '!') ");
            params.add(like);
            params.add(like);
        }
        if (role != null) {
            where.append("AND u.role = ? ");
            params.add(role.name());
        }
        try (Connection con = DBContext.getConnection()) {
            long total;
            try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM users u " + where)) {
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
            List<User> items = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement("SELECT u.*, (SELECT COUNT(*) FROM orders o WHERE o.user_id = u.id) AS order_count "
                    + "FROM users u " + where + "ORDER BY u.created_at DESC, u.id DESC LIMIT ? OFFSET ?")) {
                int i = 1;
                for (Object v : params) {
                    ps.setObject(i++, v);
                }
                ps.setInt(i++, pageSize);
                ps.setInt(i, (safePage - 1) * pageSize);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        User u = map(rs);
                        u.setOrderCount(rs.getInt("order_count"));
                        items.add(u);
                    }
                }
            }
            return new Page<>(items, safePage, pageSize, total);
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc danh sách người dùng", e);
        }
    }
}
