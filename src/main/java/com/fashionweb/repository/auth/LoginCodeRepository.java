package com.fashionweb.repository.auth;

import com.fashionweb.exception.DataAccessException;
import com.fashionweb.util.DBContext;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Truy vấn bảng login_codes (mã đăng nhập 6 số, #74).
 *
 * <p>Chỉ lưu băm SHA-256 của mã. Hạn dùng tính bằng NOW() của MySQL để cùng một đồng hồ khi tạo và khi kiểm tra.
 */
public class LoginCodeRepository {

    /** Kết quả kiểm tra mã. */
    public enum Status { OK, WRONG, LOCKED, NONE }

    /** status và số lần thử còn lại (chỉ có nghĩa khi WRONG). */
    public record VerifyResult(Status status, int remaining) {
    }

    /** Số mã đã tạo trong 1 giờ qua và số giây kể từ mã gần nhất (-1 nếu chưa có). */
    public record RecentStats(int countLastHour, int secondsSinceLast) {
    }

    public RecentStats recentStats(String email) {
        String sql = "SELECT COUNT(*), TIMESTAMPDIFF(SECOND, MAX(created_at), NOW()) FROM login_codes "
                + "WHERE email = ? AND created_at > NOW() - INTERVAL 1 HOUR";
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                int count = rs.getInt(1);
                int since = rs.getInt(2);
                return new RecentStats(count, rs.wasNull() ? -1 : since);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc mã đăng nhập", e);
        }
    }

    /**
     * Một transaction: hủy các mã chưa dùng của email (used = 1) rồi thêm mã mới hết hạn sau minutes phút.
     */
    public void replace(String email, String codeHash, int minutes) {
        try (Connection con = DBContext.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement("UPDATE login_codes SET used = 1 WHERE email = ? AND used = 0")) {
                    ps.setString(1, email);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO login_codes (email, code_hash, expires_at) VALUES (?, ?, NOW() + INTERVAL ? MINUTE)")) {
                    ps.setString(1, email);
                    ps.setString(2, codeHash);
                    ps.setInt(3, minutes);
                    ps.executeUpdate();
                }
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi lưu mã đăng nhập", e);
        }
    }

    /**
     * Một transaction: khóa mã còn hiệu lực mới nhất của email (FOR UPDATE) để mỗi mã chỉ dùng được một lần.
     * Đúng thì used = 1. Sai thì tăng attempts; đủ maxAttempts thì hủy mã (used = 1).
     */
    public VerifyResult verify(String email, String codeHash, int maxAttempts) {
        try (Connection con = DBContext.getConnection()) {
            con.setAutoCommit(false);
            try {
                long id;
                String stored;
                int attempts;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT id, code_hash, attempts FROM login_codes WHERE email = ? AND used = 0 AND expires_at > NOW() "
                                + "ORDER BY id DESC LIMIT 1 FOR UPDATE")) {
                    ps.setString(1, email);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            return new VerifyResult(Status.NONE, 0);
                        }
                        id = rs.getLong(1);
                        stored = rs.getString(2);
                        attempts = rs.getInt(3);
                    }
                }
                VerifyResult result;
                if (MessageDigest.isEqual(stored.getBytes(StandardCharsets.US_ASCII), codeHash.getBytes(StandardCharsets.US_ASCII))) {
                    update(con, "UPDATE login_codes SET used = 1 WHERE id = ?", id);
                    result = new VerifyResult(Status.OK, 0);
                } else if (attempts + 1 >= maxAttempts) {
                    update(con, "UPDATE login_codes SET attempts = attempts + 1, used = 1 WHERE id = ?", id);
                    result = new VerifyResult(Status.LOCKED, 0);
                } else {
                    update(con, "UPDATE login_codes SET attempts = attempts + 1 WHERE id = ?", id);
                    result = new VerifyResult(Status.WRONG, maxAttempts - attempts - 1);
                }
                con.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi kiểm tra mã đăng nhập", e);
        }
    }

    private static void update(Connection con, String sql, long id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }
}
