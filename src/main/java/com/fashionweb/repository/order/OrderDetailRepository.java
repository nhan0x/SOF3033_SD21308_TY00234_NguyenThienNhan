package com.fashionweb.repository.order;

import com.fashionweb.exception.DataAccessException;
import com.fashionweb.model.OrderDetail;
import com.fashionweb.util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Truy vấn bảng order_details.
 *
 * <p>Chỉ chứa SQL (PreparedStatement) và chuyển ResultSet thành entity.
 */
public class OrderDetailRepository {

    /**
     * INSERT hàng loạt (batch) trong transaction. Lưu sẵn tên, màu, size, ảnh, đơn giá tại thời điểm đặt.
     */
    public void insertAll(Connection con, int orderId, List<OrderDetail> items) throws SQLException {
        String sql = "INSERT INTO order_details (order_id, variant_id, product_id, product_name, color_name, size_name, "
                + "image, unit_price, quantity, line_total) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (OrderDetail d : items) {
                ps.setInt(1, orderId);
                ps.setInt(2, d.getVariantId());
                ps.setInt(3, d.getProductId());
                ps.setString(4, d.getProductName());
                ps.setString(5, d.getColorName());
                ps.setString(6, d.getSizeName());
                ps.setString(7, d.getImage());
                ps.setLong(8, d.getUnitPrice());
                ps.setInt(9, d.getQuantity());
                ps.setLong(10, d.getLineTotal());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * SELECT các dòng của đơn, kèm slug sản phẩm và cờ "khách đã đánh giá sản phẩm này chưa".
     */
    public List<OrderDetail> findByOrder(int orderId) {
        String sql = "SELECT d.*, p.slug AS product_slug, "
                + "EXISTS (SELECT 1 FROM reviews r WHERE r.user_id = o.user_id AND r.product_id = d.product_id) AS reviewed "
                + "FROM order_details d JOIN orders o ON o.id = d.order_id JOIN products p ON p.id = d.product_id "
                + "WHERE d.order_id = ? ORDER BY d.id";
        List<OrderDetail> list = new ArrayList<>();
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderDetail d = new OrderDetail();
                    d.setId(rs.getInt("id"));
                    d.setOrderId(rs.getInt("order_id"));
                    d.setVariantId(rs.getInt("variant_id"));
                    d.setProductId(rs.getInt("product_id"));
                    d.setProductName(rs.getString("product_name"));
                    d.setColorName(rs.getString("color_name"));
                    d.setSizeName(rs.getString("size_name"));
                    d.setImage(rs.getString("image"));
                    d.setUnitPrice(rs.getLong("unit_price"));
                    d.setQuantity(rs.getInt("quantity"));
                    d.setLineTotal(rs.getLong("line_total"));
                    d.setProductSlug(rs.getString("product_slug"));
                    d.setReviewed(rs.getBoolean("reviewed"));
                    list.add(d);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc chi tiết đơn", e);
        }
        return list;
    }

    /**
     * Khách có đơn COMPLETED chứa sản phẩm này không (điều kiện được đánh giá).
     */
    public boolean hasPurchased(int userId, int productId) {
        String sql = "SELECT 1 FROM order_details d JOIN orders o ON o.id = d.order_id "
                + "WHERE o.user_id = ? AND d.product_id = ? AND o.status = 'COMPLETED' LIMIT 1";
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, productId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi kiểm tra lịch sử mua hàng", e);
        }
    }
}
