package com.polycoffee.Util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Tiện ích JDBC dùng chung cho các DAO.
 *
 * <ul>
 *   <li>Cấu hình kết nối KHÔNG nằm trong mã nguồn (NFR-04). Thứ tự ưu tiên:
 *       biến môi trường {@code DB_URL}, {@code DB_USER}, {@code DB_PASSWORD}
 *       &gt; file {@code db.properties} trên classpath &gt; giá trị mặc định.</li>
 *   <li>Mọi câu lệnh dùng {@link PreparedStatement} (NFR-01) và được đóng bằng
 *       try-with-resources (NFR-02).</li>
 *   <li>Lỗi SQL được ném ra dạng {@link RuntimeException}, không nuốt lỗi (NFR-03).</li>
 * </ul>
 */
public final class JdbcUtil {

    /** Chuyển một dòng ResultSet thành đối tượng. Không gọi {@code rs.next()} trong hàm này. */
    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/polycoffee?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String DEFAULT_USER = "root";

    private static final String URL;
    private static final String USER;
    private static final String PASSWORD;

    static {
        Properties file = new Properties();
        try (InputStream in = JdbcUtil.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                file.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Không đọc được db.properties", e);
        }
        URL = pick("DB_URL", file.getProperty("db.url"), DEFAULT_URL);
        USER = pick("DB_USER", file.getProperty("db.user"), DEFAULT_USER);
        PASSWORD = pick("DB_PASSWORD", file.getProperty("db.password"), "");
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Không tìm thấy JDBC driver MySQL", e);
        }
    }

    private JdbcUtil() {
    }

    private static String pick(String envName, String fileValue, String defaultValue) {
        String env = System.getenv(envName);
        if (env != null && !env.isEmpty()) {
            return env;
        }
        if (fileValue != null && !fileValue.trim().isEmpty()) {
            return fileValue.trim();
        }
        return defaultValue;
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    private static void bind(PreparedStatement ps, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) {
            ps.setObject(i + 1, args[i]);
        }
    }

    /** SELECT nhiều dòng. Trả danh sách rỗng nếu không có dòng nào. */
    public static <T> List<T> query(String sql, RowMapper<T> mapper, Object... args) {
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, args);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapper.map(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn CSDL", e);
        }
    }

    /** SELECT một dòng. Trả {@code null} nếu không có dòng nào. */
    public static <T> T queryOne(String sql, RowMapper<T> mapper, Object... args) {
        List<T> list = query(sql, mapper, args);
        return list.isEmpty() ? null : list.get(0);
    }

    /** SELECT một giá trị số (COUNT, SUM...). Trả 0 nếu không có dòng hoặc giá trị NULL. */
    public static long count(String sql, Object... args) {
        Long n = queryOne(sql, rs -> rs.getLong(1), args);
        return n == null ? 0 : n;
    }

    /** INSERT / UPDATE / DELETE. Trả về số dòng bị ảnh hưởng. */
    public static int update(String sql, Object... args) {
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, args);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi ghi CSDL", e);
        }
    }

    /** INSERT và trả về khóa tự sinh (AUTO_INCREMENT) của dòng vừa thêm. */
    public static long insert(String sql, Object... args) {
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, args);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi ghi CSDL", e);
        }
    }
}
