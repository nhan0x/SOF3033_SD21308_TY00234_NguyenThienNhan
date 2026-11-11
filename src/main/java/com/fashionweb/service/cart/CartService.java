package com.fashionweb.service.cart;

import com.fashionweb.repository.cart.CartRepository;
import com.fashionweb.exception.ValidationException;
import com.fashionweb.model.Cart;
import com.fashionweb.model.CartItem;

import java.util.ArrayList;

/**
 * Giỏ hàng của khách (lưu trong DB, mỗi người một giỏ).
 *
 * <p>Luồng gọi: Servlet -> Service -> Repository -> MySQL.
 */
public class CartService {

    /** Số lượng tối đa của một dòng mỗi lần thêm (khớp ô số lượng ở trang sản phẩm). */
    public static final int MAX_ADD = 10;

    private final CartRepository cartRepository = new CartRepository();

    /**
     * Giỏ kèm items (tên, ảnh, màu, size, đơn giá, tồn kho, lineTotal), subtotal, itemCount. Tạo giỏ nếu chưa có.
     */
    public Cart getCart(int userId) {
        Cart cart = cartRepository.findByUser(userId);
        if (cart == null) {
            cartRepository.create(userId);
            cart = cartRepository.findByUser(userId);
        }
        if (cart.getItems() == null) {
            cart.setItems(new ArrayList<>());
        }
        long subtotal = 0;
        for (CartItem it : cart.getItems()) {
            subtotal += it.getLineTotal();
        }
        cart.setSubtotal(subtotal);
        cart.setItemCount(cart.getItems().size());
        return cart;
    }

    /**
     * Thêm hoặc cộng dồn số lượng, không vượt tồn kho. Sai thì ném ValidationException kèm thông báo.
     */
    public void addItem(int userId, int variantId, int quantity) {
        if (quantity < 1 || quantity > MAX_ADD) {
            throw new ValidationException(null, "Số lượng mỗi lần thêm từ 1 đến " + MAX_ADD + ".");
        }
        Integer stock = cartRepository.findSellableStock(variantId);
        if (stock == null) {
            throw new ValidationException(null, "Sản phẩm không tồn tại hoặc đã ngừng bán. Vui lòng chọn lại màu và size.");
        }
        Cart cart = getCart(userId);
        int inCart = cartRepository.quantityInCart(cart.getId(), variantId);
        if (inCart + quantity > stock) {
            String msg = stock == 0 ? "Phân loại này đã hết hàng."
                    : "Chỉ còn " + stock + " sản phẩm trong kho" + (inCart > 0 ? " (giỏ của bạn đã có " + inCart + ")" : "") + ".";
            throw new ValidationException(null, msg);
        }
        cartRepository.upsertItem(cart.getId(), variantId, quantity);
    }

    /**
     * Đổi số lượng của dòng thuộc giỏ của user, không vượt tồn kho. quantity &lt;= 0 thì xóa dòng.
     */
    public void updateQuantity(int userId, int itemId, int quantity) {
        CartItem item = cartRepository.findItemOfUser(itemId, userId);
        if (item == null) {
            throw new ValidationException(null, "Sản phẩm không còn trong giỏ hàng.");
        }
        if (quantity <= 0) {
            cartRepository.deleteItem(itemId);
            return;
        }
        if (quantity > item.getStock()) {
            throw new ValidationException(null, "\"" + item.getProductName() + "\" chỉ còn " + item.getStock() + " sản phẩm trong kho.");
        }
        cartRepository.setQuantity(itemId, quantity);
    }

    /**
     * Xóa dòng khỏi giỏ (chỉ dòng thuộc giỏ của user).
     */
    public void removeItem(int userId, int itemId) {
        if (cartRepository.findItemOfUser(itemId, userId) != null) {
            cartRepository.deleteItem(itemId);
        }
    }

    /**
     * Số dòng trong giỏ (hiện trên icon giỏ).
     */
    public int countItems(int userId) {
        return cartRepository.countItems(userId);
    }

    /**
     * Xóa toàn bộ giỏ (sau khi đặt hàng).
     */
    public void clear(int userId) {
        Cart cart = cartRepository.findByUser(userId);
        if (cart != null) {
            cartRepository.deleteAllItems(cart.getId());
        }
    }
}
