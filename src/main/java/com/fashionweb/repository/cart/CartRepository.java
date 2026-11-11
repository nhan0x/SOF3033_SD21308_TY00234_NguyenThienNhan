package com.fashionweb.repository.cart;

import com.fashionweb.exception.DataAccessException;
import com.fashionweb.model.Cart;
import com.fashionweb.model.CartItem;
import com.fashionweb.util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Truy vấn bảng carts và cart_items.
 *
 * <p>Chỉ chứa SQL (PreparedStatement) và chuyển ResultSet thành entity. Kết nối lấy từ DBContext.getConnection().
 */
public class CartRepository {

    private static final String SELECT_ITEMS = "SELECT ci.*, p.id AS product_id, p.category_id, p.name AS product_name, "
            + "p.slug AS product_slug, p.image_thumb, p.price, p.is_deleted, v.stock, co.name AS color_name, s.name AS size_name "
            + "FROM cart_items ci JOIN product_variants v ON v.id = ci.variant_id JOIN products p ON p.id = v.product_id "
            + "JOIN colors co ON co.id = v.color_id JOIN sizes s ON s.id = v.size_id ";

    /**
     * SELECT giỏ và các dòng (JOIN product_variants, products, colors, sizes). null nếu user chưa có giỏ.
     */
    public Cart findByUser(int userId) {
        try (Connection con = DBContext.getConnection()) {
            Cart cart = null;
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM carts WHERE user_id = ?")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        cart = new Cart();
                        cart.setId(rs.getInt("id"));
                        cart.setUserId(rs.getInt("user_id"));
                        cart.setUpdatedAt(rs.getTimestamp("updated_at"));
                    }
                }
            }
            if (cart == null) {
                return null;
            }
            List<CartItem> items = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(SELECT_ITEMS + "WHERE ci.cart_id = ? ORDER BY ci.id")) {
                ps.setInt(1, cart.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        items.add(mapItem(rs));
                    }
                }
            }
            cart.setItems(items);
            return cart;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc giỏ hàng", e);
        }
    }

    /**
     * INSERT giỏ, trả id.
     */
    public int create(int userId) {
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement("INSERT INTO carts (user_id) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi tạo giỏ hàng", e);
        }
    }

    /**
     * INSERT ... ON DUPLICATE KEY UPDATE quantity = quantity + ?.
     */
    public void upsertItem(int cartId, int variantId, int quantity) {
        String sql = "INSERT INTO cart_items (cart_id, variant_id, quantity) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE quantity = quantity + VALUES(quantity)";
        update(sql, "Lỗi thêm sản phẩm vào giỏ", cartId, variantId, quantity);
    }

    /**
     * UPDATE.
     */
    public void setQuantity(int itemId, int quantity) {
        update("UPDATE cart_items SET quantity = ? WHERE id = ?", "Lỗi đổi số lượng trong giỏ", quantity, itemId);
    }

    /**
     * DELETE.
     */
    public void deleteItem(int itemId) {
        update("DELETE FROM cart_items WHERE id = ?", "Lỗi xóa dòng trong giỏ", itemId);
    }

    /**
     * DELETE tất cả dòng.
     */
    public void deleteAllItems(int cartId) {
        update("DELETE FROM cart_items WHERE cart_id = ?", "Lỗi xóa giỏ hàng", cartId);
    }

    /**
     * Xóa toàn bộ dòng trong giỏ của user, chạy trong transaction đặt hàng.
     */
    public void deleteAllItemsOfUser(Connection con, int userId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "DELETE ci FROM cart_items ci JOIN carts c ON c.id = ci.cart_id WHERE c.user_id = ?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    /**
     * Một dòng trong giỏ của user (kèm tồn kho hiện tại). null nếu dòng không thuộc giỏ của user.
     */
    public CartItem findItemOfUser(int itemId, int userId) {
        String sql = SELECT_ITEMS + "JOIN carts c ON c.id = ci.cart_id WHERE ci.id = ? AND c.user_id = ?";
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapItem(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc dòng trong giỏ", e);
        }
    }

    /**
     * Số lượng biến thể này đang có trong giỏ (0 nếu chưa có).
     */
    public int quantityInCart(int cartId, int variantId) {
        String sql = "SELECT quantity FROM cart_items WHERE cart_id = ? AND variant_id = ?";
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, cartId);
            ps.setInt(2, variantId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc số lượng trong giỏ", e);
        }
    }

    /**
     * Tồn kho của biến thể đang bán. null nếu biến thể không có hoặc sản phẩm đã xóa mềm.
     */
    public Integer findSellableStock(int variantId) {
        String sql = "SELECT v.stock FROM product_variants v JOIN products p ON p.id = v.product_id "
                + "WHERE v.id = ? AND p.is_deleted = 0";
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, variantId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc tồn kho", e);
        }
    }

    /**
     * Số dòng trong giỏ của user (0 nếu chưa có giỏ).
     */
    public int countItems(int userId) {
        String sql = "SELECT COUNT(*) FROM cart_items ci JOIN carts c ON c.id = ci.cart_id WHERE c.user_id = ?";
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đếm giỏ hàng", e);
        }
    }

    private void update(String sql, String error, int... params) {
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setInt(i + 1, params[i]);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException(error, e);
        }
    }

    private CartItem mapItem(ResultSet rs) throws SQLException {
        CartItem it = new CartItem();
        it.setId(rs.getInt("id"));
        it.setCartId(rs.getInt("cart_id"));
        it.setVariantId(rs.getInt("variant_id"));
        it.setQuantity(rs.getInt("quantity"));
        it.setProductId(rs.getInt("product_id"));
        it.setCategoryId(rs.getInt("category_id"));
        it.setProductName(rs.getString("product_name"));
        it.setProductSlug(rs.getString("product_slug"));
        it.setImage(rs.getString("image_thumb"));
        it.setColorName(rs.getString("color_name"));
        it.setSizeName(rs.getString("size_name"));
        it.setUnitPrice(rs.getLong("price"));
        it.setLineTotal(it.getUnitPrice() * it.getQuantity());
        // Sản phẩm đã ngừng bán (xóa mềm) coi như hết hàng
        it.setStock(rs.getBoolean("is_deleted") ? 0 : rs.getInt("stock"));
        return it;
    }
}
