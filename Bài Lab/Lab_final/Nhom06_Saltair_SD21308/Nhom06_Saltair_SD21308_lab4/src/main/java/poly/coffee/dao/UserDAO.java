package poly.coffee.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import poly.coffee.entity.User;
import poly.coffee.util.JdbcUtil;

/** Ban day du (ghi de UserDAO cua Lab 3): them findByEmail. */
public class UserDAO {
    protected User map(ResultSet rs) throws SQLException {
        return new User(rs.getInt("id"), rs.getString("fullname"), rs.getString("email"),
                rs.getString("password"), rs.getBoolean("manager"), rs.getBoolean("enabled"));
    }

    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        try (ResultSet rs = JdbcUtil.executeQuery("SELECT * FROM users ORDER BY id")) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public User findById(int id) {
        try (ResultSet rs = JdbcUtil.executeQuery("SELECT * FROM users WHERE id = ?", id)) {
            return rs.next() ? map(rs) : null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** Lab 4 - Bai 3: lay user theo email (dung cho dang nhap). */
    public User findByEmail(String email) {
        try (ResultSet rs = JdbcUtil.executeQuery("SELECT * FROM users WHERE email = ?", email)) {
            return rs.next() ? map(rs) : null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void create(User u) {
        JdbcUtil.executeUpdate(
            "INSERT INTO users(fullname,email,password,manager,enabled) VALUES (?,?,?,?,?)",
            u.getFullname(), u.getEmail(), u.getPassword(), u.isManager(), u.isEnabled());
    }

    public void update(User u) {
        JdbcUtil.executeUpdate(
            "UPDATE users SET fullname=?, email=?, password=?, manager=?, enabled=? WHERE id=?",
            u.getFullname(), u.getEmail(), u.getPassword(), u.isManager(), u.isEnabled(), u.getId());
    }

    public void deleteById(int id) {
        JdbcUtil.executeUpdate("DELETE FROM users WHERE id = ?", id);
    }
}
