package com.fashionweb.repository.order;

import com.fashionweb.constant.CancelledBy;
import com.fashionweb.constant.OrderStatus;
import com.fashionweb.constant.PaymentMethod;
import com.fashionweb.constant.PaymentStatus;
import com.fashionweb.exception.DataAccessException;
import com.fashionweb.model.Order;
import com.fashionweb.util.DBContext;
import com.fashionweb.util.Page;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Truy vấn bảng orders.
 *
 * <p>Chỉ chứa SQL (PreparedStatement) và chuyển ResultSet thành entity. Các hàm nhận Connection chạy trong
 * transaction do OrderService mở (đặt hàng, hủy đơn).
 */
public class OrderRepository {

    private static final String SELECT = "SELECT o.*, u.full_name AS customer_name, u.email AS customer_email, "
            + "(SELECT COALESCE(SUM(d.quantity), 0) FROM order_details d WHERE d.order_id = o.id) AS item_count "
            + "FROM orders o JOIN users u ON u.id = o.user_id ";

    /**
     * INSERT, trả id (dùng chung Connection của transaction).
     */
    public int insert(Connection con, Order o) throws SQLException {
        String sql = "INSERT INTO orders (order_code, user_id, receiver_name, receiver_phone, shipping_address, note, "
                + "payment_method, payment_status, status, subtotal, discount_amount, shipping_fee, total_amount, "
                + "voucher_id, voucher_code) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, o.getOrderCode());
            ps.setInt(2, o.getUserId());
            ps.setString(3, o.getReceiverName());
            ps.setString(4, o.getReceiverPhone());
            ps.setString(5, o.getShippingAddress());
            ps.setString(6, o.getNote());
            ps.setString(7, o.getPaymentMethod().name());
            ps.setString(8, o.getPaymentStatus().name());
            ps.setString(9, o.getStatus().name());
            ps.setLong(10, o.getSubtotal());
            ps.setLong(11, o.getDiscountAmount());
            ps.setLong(12, o.getShippingFee());
            ps.setLong(13, o.getTotalAmount());
            if (o.getVoucherId() == null) {
                ps.setNull(14, Types.INTEGER);
            } else {
                ps.setInt(14, o.getVoucherId());
            }
            ps.setString(15, o.getVoucherCode());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    /**
     * Mã đơn kế tiếp trong ngày dạng prefix + NNN (prefix = "SAyyyyMMdd"). Gọi trong transaction đặt hàng.
     */
    public String nextOrderCode(Connection con, String prefix) throws SQLException {
        String sql = "SELECT MAX(order_code) FROM orders WHERE order_code LIKE ? FOR UPDATE";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                int next = 1;
                if (rs.next() && rs.getString(1) != null) {
                    next = Integer.parseInt(rs.getString(1).substring(prefix.length())) + 1;
                }
                return prefix + String.format("%03d", next);
            }
        }
    }

    /**
     * SELECT đơn của khách, lọc trạng thái (null = tất cả), mới nhất trước.
     */
    public List<Order> findByUser(int userId, OrderStatus status) {
        String sql = SELECT + "WHERE o.user_id = ?" + (status == null ? "" : " AND o.status = ?")
                + " ORDER BY o.created_at DESC, o.id DESC";
        List<Object> params = new ArrayList<>(List.of(userId));
        if (status != null) {
            params.add(status.name());
        }
        return queryList(sql, params);
    }

    /**
     * SELECT phân trang đơn của khách, lọc trạng thái (null = tất cả), mới nhất trước.
     */
    public Page<Order> findPageByUser(int userId, OrderStatus status, int page, int pageSize) {
        String where = "WHERE o.user_id = ?" + (status == null ? " " : " AND o.status = ? ");
        List<Object> params = new ArrayList<>(List.of(userId));
        if (status != null) {
            params.add(status.name());
        }
        long total = queryLong("SELECT COUNT(*) FROM orders o " + where, params);
        int totalPages = (int) Math.max(1, (total + pageSize - 1) / pageSize);
        int safePage = Math.min(Math.max(1, page), totalPages);
        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(pageSize);
        pageParams.add((safePage - 1) * pageSize);
        List<Order> items = queryList(SELECT + where + "ORDER BY o.created_at DESC, o.id DESC LIMIT ? OFFSET ?", pageParams);
        return new Page<>(items, safePage, pageSize, total);
    }

    /**
     * SELECT theo order_code. null nếu không có.
     */
    public Order findByCode(String code) {
        List<Order> list = queryList(SELECT + "WHERE o.order_code = ?", List.of(code));
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * Khóa dòng đơn trong transaction (SELECT ... FOR UPDATE) để đổi trạng thái an toàn. null nếu không có.
     */
    public Order lockByCode(Connection con, String code) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT o.*, NULL AS customer_name, NULL AS customer_email, 0 AS item_count FROM orders o WHERE o.order_code = ? FOR UPDATE")) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /**
     * SELECT phân trang JOIN users. q tìm theo mã đơn, tên khách, tên người nhận, email.
     */
    public Page<Order> findPage(String q, OrderStatus status, PaymentStatus payment, int page, int pageSize) {
        StringBuilder where = new StringBuilder("WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();
        appendFilters(where, params, null, q, payment);
        if (status != null) {
            where.append("AND o.status = ? ");
            params.add(status.name());
        }
        long total = queryLong("SELECT COUNT(*) FROM orders o JOIN users u ON u.id = o.user_id " + where, params);
        int safePage = Math.max(1, page);
        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(pageSize);
        pageParams.add((safePage - 1) * pageSize);
        List<Order> items = queryList(SELECT + where + "ORDER BY o.created_at DESC, o.id DESC LIMIT ? OFFSET ?", pageParams);
        return new Page<>(items, safePage, pageSize, total);
    }

    /**
     * Số đơn theo trạng thái, kèm khóa "ALL" là tổng. userId null = mọi khách (Admin).
     */
    public Map<String, Integer> countByStatus(Integer userId, String q, PaymentStatus payment) {
        StringBuilder where = new StringBuilder("WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();
        appendFilters(where, params, userId, q, payment);
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("ALL", 0);
        for (OrderStatus s : OrderStatus.values()) {
            counts.put(s.name(), 0);
        }
        String sql = "SELECT o.status, COUNT(*) FROM orders o JOIN users u ON u.id = o.user_id " + where + "GROUP BY o.status";
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                int all = 0;
                while (rs.next()) {
                    counts.put(rs.getString(1), rs.getInt(2));
                    all += rs.getInt(2);
                }
                counts.put("ALL", all);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đếm đơn theo trạng thái", e);
        }
        return counts;
    }

    /**
     * UPDATE trạng thái (chạy trong transaction).
     */
    public void updateStatus(Connection con, int id, OrderStatus status, PaymentStatus payment, java.util.Date completedAt) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE orders SET status = ?, payment_status = ?, completed_at = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setString(2, payment.name());
            ps.setTimestamp(3, completedAt == null ? null : new Timestamp(completedAt.getTime()));
            ps.setInt(4, id);
            ps.executeUpdate();
        }
    }

    /**
     * UPDATE status = CANCELLED, cancel_reason, cancelled_by (chạy trong transaction).
     */
    public void cancel(Connection con, int id, String reason, CancelledBy by) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE orders SET status = 'CANCELLED', cancel_reason = ?, cancelled_by = ? WHERE id = ?")) {
            ps.setString(1, reason);
            ps.setString(2, by.name());
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    /**
     * COUNT đơn của khách.
     */
    public int countByUser(int userId) {
        return (int) queryLong("SELECT COUNT(*) FROM orders WHERE user_id = ?", List.of(userId));
    }

    /**
     * n đơn mới nhất.
     */
    public List<Order> findLatest(int n) {
        return queryList(SELECT + "ORDER BY o.created_at DESC, o.id DESC LIMIT ?", List.of(n));
    }

    // ----- Hàm phụ -----

    private static void appendFilters(StringBuilder where, List<Object> params, Integer userId, String q, PaymentStatus payment) {
        if (userId != null) {
            where.append("AND o.user_id = ? ");
            params.add(userId);
        }
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            where.append("AND (o.order_code LIKE ? ESCAPE '!' OR u.full_name LIKE ? ESCAPE '!' "
                    + "OR o.receiver_name LIKE ? ESCAPE '!' OR u.email LIKE ? ESCAPE '!') ");
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (payment != null) {
            where.append("AND o.payment_status = ? ");
            params.add(payment.name());
        }
    }

    private static void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    private long queryLong(String sql, List<Object> params) {
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đếm đơn hàng", e);
        }
    }

    private List<Order> queryList(String sql, List<Object> params) {
        List<Order> list = new ArrayList<>();
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc đơn hàng", e);
        }
        return list;
    }

    private Order map(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setId(rs.getInt("id"));
        o.setOrderCode(rs.getString("order_code"));
        o.setUserId(rs.getInt("user_id"));
        o.setReceiverName(rs.getString("receiver_name"));
        o.setReceiverPhone(rs.getString("receiver_phone"));
        o.setShippingAddress(rs.getString("shipping_address"));
        o.setNote(rs.getString("note"));
        o.setPaymentMethod(PaymentMethod.valueOf(rs.getString("payment_method")));
        o.setPaymentStatus(PaymentStatus.valueOf(rs.getString("payment_status")));
        o.setStatus(OrderStatus.valueOf(rs.getString("status")));
        o.setSubtotal(rs.getLong("subtotal"));
        o.setDiscountAmount(rs.getLong("discount_amount"));
        o.setShippingFee(rs.getLong("shipping_fee"));
        o.setTotalAmount(rs.getLong("total_amount"));
        int voucherId = rs.getInt("voucher_id");
        o.setVoucherId(rs.wasNull() ? null : voucherId);
        o.setVoucherCode(rs.getString("voucher_code"));
        o.setCancelReason(rs.getString("cancel_reason"));
        String by = rs.getString("cancelled_by");
        o.setCancelledBy(by == null ? null : CancelledBy.valueOf(by));
        o.setCreatedAt(rs.getTimestamp("created_at"));
        o.setUpdatedAt(rs.getTimestamp("updated_at"));
        o.setCompletedAt(rs.getTimestamp("completed_at"));
        o.setCustomerName(rs.getString("customer_name"));
        o.setCustomerEmail(rs.getString("customer_email"));
        o.setItemCount(rs.getInt("item_count"));
        return o;
    }
}
