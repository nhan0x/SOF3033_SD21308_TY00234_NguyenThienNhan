package com.fashionweb.dao.order;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Bảng payment_transactions: mọi giao dịch tiền vào ngân hàng báo về. Chỉ chứa SQL (PreparedStatement).
 * Các hàm chạy trong transaction do PaymentService mở.
 */
public class PaymentTransactionDAO {

    /** Kết quả đã xử lý của giao dịch này, null nếu chưa từng thấy. */
    public String findResult(Connection con, String provider, String txId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT result FROM payment_transactions WHERE provider = ? AND provider_tx_id = ? FOR UPDATE")) {
            ps.setString(1, provider);
            ps.setString(2, txId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    public void insert(Connection con, String provider, String txId, Integer orderId, String orderCode, long amount,
                       String content, String result) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO payment_transactions (provider, provider_tx_id, order_id, order_code, amount, content, result) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, provider);
            ps.setString(2, txId);
            ps.setObject(3, orderId);
            ps.setString(4, orderCode);
            ps.setLong(5, amount);
            ps.setString(6, content == null ? null : (content.length() > 500 ? content.substring(0, 500) : content));
            ps.setString(7, result);
            ps.executeUpdate();
        }
    }
}
