package com.polycoffee.DAO;

import com.polycoffee.Entity.User;
import com.polycoffee.Util.JdbcUtil;
import com.polycoffee.Util.JdbcUtil.RowMapper;
import java.util.List;

/** Truy cập bảng {@code users}. */
public class UserDAO {

    private static final RowMapper<User> MAPPER = rs -> new User(
            rs.getInt("id"), rs.getString("fullname"), rs.getString("email"),
            rs.getString("password"), rs.getBoolean("manager"), rs.getBoolean("enabled"));

    public List<User> findAll() {
        return JdbcUtil.query("SELECT * FROM users ORDER BY id", MAPPER);
    }

    public User findById(int id) {
        return JdbcUtil.queryOne("SELECT * FROM users WHERE id = ?", MAPPER, id);
    }

    /** Tìm theo email, dùng cho đăng nhập. */
    public User findByEmail(String email) {
        return JdbcUtil.queryOne("SELECT * FROM users WHERE email = ?", MAPPER, email);
    }

    /** Email đã được tài khoản khác dùng chưa (BR-15). Khi thêm mới truyền excludeId = 0. */
    public boolean existsByEmail(String email, int excludeId) {
        return JdbcUtil.count("SELECT COUNT(*) FROM users WHERE email = ? AND id <> ?", email, excludeId) > 0;
    }

    /** Số hóa đơn do người này tạo, dùng để chặn xóa nhân viên đã có dữ liệu (BR-18). */
    public int countBills(int userId) {
        return (int) JdbcUtil.count("SELECT COUNT(*) FROM bills WHERE user_id = ?", userId);
    }

    /** Thêm tài khoản, trả về id vừa sinh. {@code password} phải là bản băm. */
    public int create(User u) {
        return (int) JdbcUtil.insert(
                "INSERT INTO users(fullname, email, password, manager, enabled) VALUES (?,?,?,?,?)",
                u.getFullname(), u.getEmail(), u.getPassword(), u.isManager(), u.isEnabled());
    }

    /** Cập nhật thông tin. Không đổi mật khẩu, dùng {@link #updatePassword}. */
    public int update(User u) {
        return JdbcUtil.update(
                "UPDATE users SET fullname=?, email=?, manager=?, enabled=? WHERE id=?",
                u.getFullname(), u.getEmail(), u.isManager(), u.isEnabled(), u.getId());
    }

    /** Đặt bản băm mật khẩu mới. */
    public int updatePassword(int id, String passwordHash) {
        return JdbcUtil.update("UPDATE users SET password = ? WHERE id = ?", passwordHash, id);
    }

    public int deleteById(int id) {
        return JdbcUtil.update("DELETE FROM users WHERE id = ?", id);
    }
}
