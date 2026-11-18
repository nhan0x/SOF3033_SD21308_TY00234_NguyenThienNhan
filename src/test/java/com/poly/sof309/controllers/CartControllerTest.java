package com.poly.sof309.controllers;

import com.fashionweb.constant.AppConstant;
import com.fashionweb.controller.cart.CartServlet;
import com.fashionweb.exception.DataAccessException;
import com.fashionweb.exception.ValidationException;
import com.fashionweb.model.Cart;
import com.fashionweb.service.cart.CartService;
import com.fashionweb.service.shop.ProductService;
import com.poly.sof309.controllers.support.Harness;
import com.poly.sof309.controllers.support.ServletFixture;
import com.poly.sof309.controllers.support.Users;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.poly.sof309.controllers.support.ServletFixture.CTX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Controller giỏ hàng: TC_CART_02, 03, 04, 05, 07, 08. Khách vãng lai dùng Service thật (không chạm DB), khách đăng nhập dùng Service giả. */
@Timeout(value = 5, unit = TimeUnit.SECONDS)
class CartControllerTest {

    private static final String QUANTITY_RANGE = "Số lượng mỗi lần thêm từ 1 đến 10.";

    private Harness<CartServlet> harness;

    @AfterEach
    void closeHarness() {
        if (harness != null) {
            harness.close();
        }
    }

    private static ServletFixture addToCart(String variantId, String quantity) {
        ServletFixture f = ServletFixture.post("/cart").param("action", "add");
        if (variantId != null) {
            f.param("variantId", variantId);
        }
        if (quantity != null) {
            f.param("quantity", quantity);
        }
        return f;
    }

    // ---------- khách vãng lai: kiểm tra dữ liệu trước khi chạm DB ----------

    @ParameterizedTest(name = "TC_CART_02 số lượng {0} bị từ chối")
    @DisplayName("TC_CART_02: số lượng mỗi lần thêm ngoài khoảng 1..10 bị từ chối, giỏ không đổi")
    @ValueSource(strings = {"0", "-1", "11", "100", "2147483647"})
    void quantityOutOfRangeIsRejected(String quantity) throws Exception {
        ServletFixture f = addToCart("5", quantity);

        f.run(new CartServlet());

        assertEquals(QUANTITY_RANGE, f.flashMessage());
        assertEquals("error", f.flashType());
        assertEquals(CTX + "/cart", f.redirectedTo);
        assertEquals(0, f.sessionAttrValue(AppConstant.SESSION_CART_COUNT), "Giỏ vẫn rỗng");
    }

    @ParameterizedTest(name = "TC_CART_04 variantId [{0}] -> chưa chọn màu/size")
    @DisplayName("TC_CART_04: không chọn màu và size (variantId thiếu, 0, âm hoặc không phải số)")
    @CsvSource(value = {"0", "-3", "abc", "''", "ABSENT"})
    void missingVariantIsRejected(String variantId) throws Exception {
        ServletFixture f = addToCart("ABSENT".equals(variantId) ? null : variantId, "1");

        f.run(new CartServlet());

        assertEquals("Vui lòng chọn màu và size.", f.flashMessage());
        assertEquals("error", f.flashType());
    }

    @Test
    @DisplayName("TC_CART_04: thêm lỗi thì quay lại đúng trang sản phẩm theo Referer cùng máy chủ")
    void errorReturnsToProductPage() throws Exception {
        ServletFixture f = addToCart("0", "1").header("Referer", "http://localhost:8080/saltair/product?slug=ao-polo");

        f.run(new CartServlet());

        assertEquals(CTX + "/product?slug=ao-polo", f.redirectedTo);
    }

    @Test
    @DisplayName("TC_CART_04: Referer của trang web khác bị bỏ qua (không redirect ra ngoài)")
    void foreignRefererIsIgnored() throws Exception {
        ServletFixture f = addToCart("0", "1").header("Referer", "http://evil.com/saltair/product?slug=x");

        f.run(new CartServlet());

        assertEquals(CTX + "/cart", f.redirectedTo);
    }

    // ---------- khách vãng lai: sửa và xóa dòng giỏ trong session ----------

    @Test
    @DisplayName("TC_CART_05: cập nhật dòng không có trong giỏ thì báo lỗi")
    void updateMissingLine() throws Exception {
        ServletFixture f = ServletFixture.post("/cart").param("action", "update").param("itemId", "42").param("quantity", "2");

        f.run(new CartServlet());

        assertEquals("Sản phẩm không còn trong giỏ hàng.", f.flashMessage());
        assertEquals("error", f.flashType());
    }

    @Test
    @DisplayName("TC_CART_05: cập nhật số lượng về 0 thì xóa dòng và cập nhật số trên icon giỏ")
    void updateToZeroRemovesLine() throws Exception {
        Map<Integer, Integer> lines = new LinkedHashMap<>(Map.of(5, 2));
        ServletFixture f = ServletFixture.post("/cart").param("action", "update").param("itemId", "5").param("quantity", "0")
                .sessionAttr(AppConstant.SESSION_GUEST_CART, lines);

        f.run(new CartServlet());

        assertTrue(lines.isEmpty());
        assertEquals(0, f.sessionAttrValue(AppConstant.SESSION_CART_COUNT));
        assertNull(f.flashMessage(), "Cập nhật thành công không hiện thông báo");
    }

    @Test
    @DisplayName("Xóa một dòng trong giỏ khách vãng lai")
    void removeLine() throws Exception {
        Map<Integer, Integer> lines = new LinkedHashMap<>(Map.of(5, 2, 6, 1));
        ServletFixture f = ServletFixture.post("/cart").param("action", "remove").param("itemId", "5")
                .sessionAttr(AppConstant.SESSION_GUEST_CART, lines);

        f.run(new CartServlet());

        assertEquals(Map.of(6, 1), lines);
        assertEquals("Đã xóa sản phẩm khỏi giỏ hàng.", f.flashMessage());
        assertEquals(1, f.sessionAttrValue(AppConstant.SESSION_CART_COUNT));
    }

    // ---------- TC_CART_08: action lạ, ghi chú ----------

    @ParameterizedTest(name = "TC_CART_08 action [{0}] trả 400")
    @DisplayName("TC_CART_08: action thiếu hoặc không hợp lệ trả HTTP 400")
    @CsvSource(value = {"xyz", "ADD", "''", "ABSENT"})
    void unknownActionIs400(String action) throws Exception {
        ServletFixture f = ServletFixture.post("/cart");
        if (!"ABSENT".equals(action)) {
            f.param("action", action);
        }

        f.run(new CartServlet());

        assertEquals(400, f.errorStatus);
        assertNull(f.redirectedTo);
    }

    @ParameterizedTest(name = "TC_CART_08 ghi chú dài {0} ký tự được lưu {1}")
    @DisplayName("TC_CART_08: ghi chú bị cắt ở 500 ký tự, khoảng trắng hai đầu bị bỏ")
    @CsvSource({"499, 499", "500, 500", "501, 500", "1000, 500"})
    void noteIsTrimmedTo500(int length, int stored) throws Exception {
        ServletFixture f = ServletFixture.post("/cart").param("action", "note").param("note", "  " + "n".repeat(length) + "  ");

        f.run(new CartServlet());

        assertEquals(stored, ((String) f.sessionAttrValue(AppConstant.SESSION_CART_NOTE)).length());
        assertEquals("Đã lưu ghi chú.", f.flashMessage());
    }

    @Test
    @DisplayName("GET /cart của khách vãng lai rỗng: giỏ rỗng, phí ship 30.000, có gợi ý sản phẩm")
    void getEmptyGuestCart() throws Exception {
        harness = Harness.mocked(CartServlet::new, ProductService.class);
        ServletFixture f = ServletFixture.get("/cart");

        f.run(harness.servlet);

        Cart cart = (Cart) f.requestAttr("cart");
        assertEquals(0, cart.getItemCount());
        assertEquals(AppConstant.SHIPPING_FEE, f.requestAttr("shippingFee"));
        assertEquals("/WEB-INF/views/cart/cart.jsp", f.lastForward());
    }

    // ---------- khách đã đăng nhập: Controller phải dùng id trong session ----------

    private void loggedInHarness() {
        harness = Harness.mocked(CartServlet::new, CartService.class, ProductService.class);
    }

    @ParameterizedTest(name = "TC_CART_02 số lượng [{0}] -> gọi addItem với {1}")
    @DisplayName("TC_CART_02: số lượng hợp lệ và giá trị lạ được chuyển cho Service (không phải số thì coi là 1)")
    @CsvSource(value = {"1, 1", "10, 10", "abc, 1", "'', 1", "ABSENT, 1"})
    void loggedInAddDelegatesToService(String quantity, int expected) throws Exception {
        loggedInHarness();
        CartService cart = harness.service(CartService.class);
        when(cart.countItems(7)).thenReturn(2);
        ServletFixture f = addToCart("12", "ABSENT".equals(quantity) ? null : quantity).loggedIn(Users.customer(7));

        f.run(harness.servlet);

        verify(cart).addItem(7, 12, expected);
        assertEquals("Đã thêm vào giỏ hàng.", f.flashMessage());
        assertEquals("success", f.flashType());
        assertEquals(CTX + "/cart", f.redirectedTo);
        assertEquals(2, f.sessionAttrValue(AppConstant.SESSION_CART_COUNT));
    }

    @Test
    @DisplayName("TC_CART_03: Service báo vượt tồn kho thì hiện đúng thông báo và quay lại trang sản phẩm")
    void stockErrorIsShown() throws Exception {
        loggedInHarness();
        CartService cart = harness.service(CartService.class);
        doThrow(new ValidationException(null, "Chỉ còn 5 sản phẩm trong kho (giỏ của bạn đã có 3)."))
                .when(cart).addItem(7, 6, 3);
        ServletFixture f = addToCart("6", "3").loggedIn(Users.customer(7))
                .header("Referer", "http://localhost:8080/saltair/product?slug=ao-thun-co-tron");

        f.run(harness.servlet);

        assertEquals("Chỉ còn 5 sản phẩm trong kho (giỏ của bạn đã có 3).", f.flashMessage());
        assertEquals("error", f.flashType());
        assertEquals(CTX + "/product?slug=ao-thun-co-tron", f.redirectedTo);
    }

    @Test
    @DisplayName("TC_CART_07: Controller luôn dùng id người dùng trong session, bỏ qua userId do client gửi")
    void ownerIdComesFromSessionNotFromRequest() throws Exception {
        loggedInHarness();
        CartService cart = harness.service(CartService.class);
        ServletFixture update = ServletFixture.post("/cart").param("action", "update").param("itemId", "1")
                .param("quantity", "2").param("userId", "2").loggedIn(Users.customer(3));
        ServletFixture remove = ServletFixture.post("/cart").param("action", "remove").param("itemId", "1")
                .param("userId", "2").loggedIn(Users.customer(3));

        update.run(harness.servlet);
        remove.run(harness.servlet);

        verify(cart).updateQuantity(3, 1, 2);
        verify(cart).removeItem(3, 1);
        verify(cart, never()).updateQuantity(2, 1, 2);
        verify(cart, never()).removeItem(2, 1);
    }

    @Test
    @DisplayName("TC_CART_05: Service từ chối vượt tồn thì Controller hiện lỗi và giữ nguyên giỏ")
    void updateBeyondStockShowsError() throws Exception {
        loggedInHarness();
        CartService cart = harness.service(CartService.class);
        doThrow(new ValidationException(null, "\"Áo polo\" chỉ còn 5 sản phẩm trong kho."))
                .when(cart).updateQuantity(7, 4, 6);
        ServletFixture f = ServletFixture.post("/cart").param("action", "update").param("itemId", "4")
                .param("quantity", "6").loggedIn(Users.customer(7));

        f.run(harness.servlet);

        assertEquals("\"Áo polo\" chỉ còn 5 sản phẩm trong kho.", f.flashMessage());
        assertEquals("error", f.flashType());
        assertEquals(CTX + "/cart", f.redirectedTo);
    }

    @Test
    @DisplayName("Lỗi database không bị nuốt: lan ra ngoài để container trả trang 500")
    void databaseErrorPropagates() {
        loggedInHarness();
        doThrow(new DataAccessException("mất kết nối", new java.sql.SQLException("down")))
                .when(harness.service(CartService.class)).addItem(anyInt(), anyInt(), anyInt());
        ServletFixture f = addToCart("12", "1").loggedIn(Users.customer(7));

        assertThrows(DataAccessException.class, () -> f.run(harness.servlet));
    }
}
