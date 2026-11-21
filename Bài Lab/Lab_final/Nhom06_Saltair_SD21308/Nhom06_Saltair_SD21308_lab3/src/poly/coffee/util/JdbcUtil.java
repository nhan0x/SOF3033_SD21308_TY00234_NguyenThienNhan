package poly.coffee.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Tien ich ket noi SQL Server va thuc thi cau lenh SQL co tham so. */
public class JdbcUtil {
    private static final String DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    private static final String URL =
            "jdbc:sqlserver://localhost:1433;databaseName=PolyCoffee;encrypt=true;trustServerCertificate=true";
    private static final String USER = "sa";
    private static final String PASSWORD = "123456";   // doi theo may cua ban

    static {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Khong tim thay JDBC driver SQL Server", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    private static PreparedStatement prepare(Connection con, String sql, Object... args) throws SQLException {
        PreparedStatement ps = con.prepareStatement(sql);
        for (int i = 0; i < args.length; i++) {
            ps.setObject(i + 1, args[i]);
        }
        return ps;
    }

    /**
     * Thuc thi SELECT. Statement va Connection duoc dong tu dong khi ResultSet dong
     * (closeOnCompletion), nen noi goi chi can dung try-with-resources voi ResultSet.
     */
    public static ResultSet executeQuery(String sql, Object... args) {
        try {
            Connection con = getConnection();
            PreparedStatement ps = prepare(con, sql, args);
            ps.closeOnCompletion();
            return ps.executeQuery();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** Thuc thi INSERT / UPDATE / DELETE, tra ve so dong bi anh huong. */
    public static int executeUpdate(String sql, Object... args) {
        try (Connection con = getConnection(); PreparedStatement ps = prepare(con, sql, args)) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** Thuc thi SELECT chi lay 1 gia tri (COUNT, SUM...). */
    public static Object executeScalar(String sql, Object... args) {
        try (ResultSet rs = executeQuery(sql, args)) {
            return rs.next() ? rs.getObject(1) : null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } 
    }
}
