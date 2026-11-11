package com.fashionweb.repository.admin;

import com.fashionweb.dto.DashboardStats;
import com.fashionweb.exception.DataAccessException;
import com.fashionweb.model.Product;
import com.fashionweb.util.DBContext;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Truy vấn thống kê cho dashboard của Admin.
 *
 * <p>Khoảng ngày [from, to] tính trên created_at của đơn: created_at &gt;= from 00:00 và &lt; (to + 1 ngày) 00:00.
 * <p>Chỉ chứa SQL (PreparedStatement). Kết nối lấy từ DBContext.getConnection().
 */
public class StatisticRepository {

    private static final String RANGE = "o.created_at >= ? AND o.created_at < ? ";
    private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("dd/MM");

    /** Số đơn trong khoảng ngày; statusOrNull khác null thì chỉ đếm đơn có trạng thái đó. */
    public int countOrders(LocalDate from, LocalDate to, String statusOrNull) {
        String sql = "SELECT COUNT(*) FROM orders o WHERE " + RANGE + (statusOrNull == null ? "" : "AND o.status = ?");
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bindRange(ps, from, to);
            if (statusOrNull != null) {
                ps.setString(3, statusOrNull);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đếm đơn hàng", e);
        }
    }

    /** Số đơn đang chờ xác nhận (mọi thời điểm, việc cần xử lý ngay). */
    public int countPendingNow() {
        return scalarInt("SELECT COUNT(*) FROM orders WHERE status = 'PENDING'");
    }

    /** Tổng doanh thu của các đơn COMPLETED trong khoảng ngày (SUM total_amount). Đơn hủy không tính. */
    public long sumRevenue(LocalDate from, LocalDate to) {
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT COALESCE(SUM(o.total_amount), 0) FROM orders o WHERE o.status = 'COMPLETED' AND " + RANGE)) {
            bindRange(ps, from, to);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi tính doanh thu", e);
        }
    }

    /** Doanh thu đơn COMPLETED theo từng ngày trong khoảng; ngày không có đơn vẫn có mặt với giá trị 0. */
    public List<DashboardStats.DailyRevenue> revenueByDay(LocalDate from, LocalDate to) {
        Map<LocalDate, Long> byDay = new LinkedHashMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            byDay.put(d, 0L);
        }
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT DATE(o.created_at) AS d, SUM(o.total_amount) FROM orders o "
                     + "WHERE o.status = 'COMPLETED' AND " + RANGE + "GROUP BY DATE(o.created_at)")) {
            bindRange(ps, from, to);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    byDay.put(rs.getDate(1).toLocalDate(), rs.getLong(2));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi tính doanh thu theo ngày", e);
        }
        List<DashboardStats.DailyRevenue> list = new ArrayList<>();
        byDay.forEach((d, v) -> list.add(new DashboardStats.DailyRevenue(d.format(LABEL), v)));
        return list;
    }

    /** n sản phẩm bán chạy nhất trong khoảng ngày (SUM quantity trong order_details của đơn không bị hủy). */
    public List<Product> topProducts(LocalDate from, LocalDate to, int n) {
        String sql = "SELECT p.id, p.name, p.slug, p.price, p.image_thumb, c.name AS category_name, SUM(d.quantity) AS sold, "
                + "COALESCE((SELECT SUM(v.stock) FROM product_variants v WHERE v.product_id = p.id), 0) AS stock_total "
                + "FROM order_details d JOIN orders o ON o.id = d.order_id JOIN products p ON p.id = d.product_id "
                + "JOIN categories c ON c.id = p.category_id "
                + "WHERE o.status <> 'CANCELLED' AND " + RANGE
                + "GROUP BY p.id, p.name, p.slug, p.price, p.image_thumb, c.name ORDER BY sold DESC, p.id LIMIT ?";
        List<Product> list = new ArrayList<>();
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bindRange(ps, from, to);
            ps.setInt(3, n);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Product p = new Product();
                    p.setId(rs.getInt("id"));
                    p.setName(rs.getString("name"));
                    p.setSlug(rs.getString("slug"));
                    p.setPrice(rs.getLong("price"));
                    p.setImageThumb(rs.getString("image_thumb"));
                    p.setCategoryName(rs.getString("category_name"));
                    p.setSoldCount(rs.getInt("sold"));
                    p.setStockTotal(rs.getInt("stock_total"));
                    list.add(p);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc sản phẩm bán chạy", e);
        }
        return list;
    }

    /** Số đơn theo từng trạng thái trong khoảng ngày (khóa là tên trạng thái: PENDING, SHIPPING...). */
    public Map<String, Integer> ordersByStatus(LocalDate from, LocalDate to) {
        Map<String, Integer> map = new LinkedHashMap<>();
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT o.status, COUNT(*) FROM orders o WHERE " + RANGE + "GROUP BY o.status")) {
            bindRange(ps, from, to);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString(1), rs.getInt(2));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đếm đơn theo trạng thái", e);
        }
        return map;
    }

    /** Số khách hàng (role CUSTOMER). */
    public int countCustomers() {
        return scalarInt("SELECT COUNT(*) FROM users WHERE role = 'CUSTOMER'");
    }

    private static void bindRange(PreparedStatement ps, LocalDate from, LocalDate to) throws SQLException {
        ps.setDate(1, Date.valueOf(from));
        ps.setDate(2, Date.valueOf(to.plusDays(1)));
    }

    private static int scalarInt(String sql) {
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi thống kê", e);
        }
    }
}
