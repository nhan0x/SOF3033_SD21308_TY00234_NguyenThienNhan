package com.poly.sof309.controllers;

import com.fashionweb.constant.AppConstant;
import com.fashionweb.constant.DiscountType;
import com.fashionweb.constant.PaymentMethod;
import com.fashionweb.controller.cart.CheckoutServlet;
import com.fashionweb.exception.DataAccessException;
import com.fashionweb.exception.ValidationException;
import com.fashionweb.exception.VoucherException;
import com.fashionweb.model.Address;
import com.fashionweb.model.Cart;
import com.fashionweb.model.CartItem;
import com.fashionweb.model.Order;
import com.fashionweb.model.User;
import com.fashionweb.model.Voucher;
import com.fashionweb.service.account.AccountService;
import com.fashionweb.service.auth.AuthService;
import com.fashionweb.service.cart.CartService;
import com.fashionweb.service.cart.VoucherService;
import com.fashionweb.service.order.OrderService;
import com.fashionweb.util.PaymentConfig;
import com.poly.sof309.controllers.support.Harness;
import com.poly.sof309.controllers.support.ServletFixture;
import com.poly.sof309.controllers.support.Users;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.poly.sof309.controllers.support.ServletFixture.CTX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Controller thanh toán và đặt hàng (cụm rủi ro cao nhất): TC_CHECKOUT_01 đến 13 và phần voucher ở tầng Controller.
 * Service giả thay cho database; hàm thuần (chuẩn hóa email, tính giảm giá) vẫn chạy thật.
 */
@Timeout(value = 5, unit = TimeUnit.SECONDS)
class CheckoutControllerTest {

    private static final String CHECKOUT_VIEW = "/WEB-INF/views/cart/checkout.jsp";
    private static final String PHONE_MSG = "Số điện thoại gồm 10 chữ số, bắt đầu bằng 0.";
    private static final String ADDRESS_MSG = "Địa chỉ (số nhà, đường) từ 5 đến 200 ký tự.";
    private static final String NAME_MSG = "Tên người nhận từ 2 đến 100 ký tự.";
    private static final String ORDER_CODE = "SA20261118001";

    private Harness<CheckoutServlet> harness;
    private MockedStatic<PaymentConfig> paymentConfig;
    private CartService cartService;
    private AccountService accountService;
    private OrderService orderService;
    private VoucherService voucherService;
    private AuthService authService;
    private PaymentConfig payOsOn;

    @BeforeEach
    void setUp() {
        // Phải tạo PaymentConfig TRƯỚC khi mock static (mock static làm hàm static của chính lớp này trả null)
        PaymentConfig payOsOff = new PaymentConfig(new Properties());
        Properties keys = new Properties();
        keys.setProperty("payos.clientId", "id");
        keys.setProperty("payos.apiKey", "key");
        keys.setProperty("payos.checksumKey", "secret");
        payOsOn = new PaymentConfig(keys);
        // payOS tắt mặc định để kết quả không phụ thuộc file payment.properties trên máy chạy test
        paymentConfig = Mockito.mockStatic(PaymentConfig.class);
        paymentConfig.when(PaymentConfig::get).thenReturn(payOsOff);
        harness = Harness.partial(CheckoutServlet::new,
                new Class<?>[]{CartService.class, AccountService.class, OrderService.class},
                new Class<?>[]{VoucherService.class, AuthService.class});
        cartService = harness.service(CartService.class);
        accountService = harness.service(AccountService.class);
        orderService = harness.service(OrderService.class);
        voucherService = harness.service(VoucherService.class);
        authService = harness.service(AuthService.class);
        // Hai hàm của Service "partial" chạm database nên phải stub
        doReturn(List.of()).when(voucherService).findActive();
    }

    @AfterEach
    void tearDown() {
        if (harness != null) {
            harness.close();
        }
        if (paymentConfig != null) {
            paymentConfig.close();
        }
    }

    // ---------- dữ liệu mẫu ----------

    private static Cart cartWith(long subtotal) {
        CartItem item = new CartItem();
        item.setVariantId(5);
        item.setProductId(4);
        item.setProductName("Áo polo");
        item.setQuantity(1);
        item.setUnitPrice(subtotal);
        item.setLineTotal(subtotal);
        item.setStock(10);
        Cart cart = new Cart();
        cart.setItems(List.of(item));
        cart.setSubtotal(subtotal);
        cart.setItemCount(1);
        return cart;
    }

    private void guestHasCart(long subtotal) {
        when(cartService.getGuestCart(any())).thenReturn(cartWith(subtotal));
    }

    private static Order order(PaymentMethod method) {
        Order o = new Order();
        o.setOrderCode(ORDER_CODE);
        o.setPaymentMethod(method);
        return o;
    }

    /** Form khách vãng lai hợp lệ hoàn toàn. */
    private static ServletFixture guestForm() {
        return ServletFixture.post("/checkout")
                .param("action", "placeOrder")
                .param("email", "guest01@x.vn")
                .param("receiverName", "Nguyen A")
                .param("receiverPhone", "0912345678")
                .param("addressLine", "12 Le Loi")
                .param("ward", "Ben Nghe")
                .param("district", "Quan 1")
                .param("city", "TP HCM");
    }

    private void guestCanPlaceOrder() {
        guestHasCart(329_000);
        User owner = Users.customer(9);
        doReturn(owner).when(authService).findOrCreateCustomer(anyString(), anyString(), anyString());
        when(orderService.placeGuestOrder(eq(9), any(Cart.class), any(Address.class), any(), any(), any()))
                .thenReturn(order(PaymentMethod.COD));
    }

    // ---------- giỏ trống, action lạ ----------

    @Test
    @DisplayName("TC_CART_08: giỏ trống thì /checkout chuyển về /cart kèm thông báo")
    void emptyCartRedirects() throws Exception {
        when(cartService.getGuestCart(any())).thenReturn(emptyCart());
        ServletFixture get = ServletFixture.get("/checkout");
        ServletFixture post = guestForm();

        get.run(harness.servlet);
        post.run(harness.servlet);

        assertEquals(CTX + "/cart", get.redirectedTo);
        assertEquals("Giỏ hàng đang trống. Hãy chọn sản phẩm trước khi thanh toán.", get.flashMessage());
        assertEquals("info", get.flashType());
        assertEquals(CTX + "/cart", post.redirectedTo);
        assertEquals("Giỏ hàng đang trống, không thể đặt hàng.", post.flashMessage());
        assertEquals("error", post.flashType());
        verifyNoInteractions(orderService);
    }

    private static Cart emptyCart() {
        Cart cart = new Cart();
        cart.setItems(List.of());
        return cart;
    }

    @ParameterizedTest(name = "action [{0}] trả 400")
    @DisplayName("POST /checkout với action thiếu hoặc lạ trả HTTP 400")
    @CsvSource(value = {"xyz", "PLACEORDER", "ABSENT"})
    void unknownActionIs400(String action) throws Exception {
        guestHasCart(329_000);
        ServletFixture f = ServletFixture.post("/checkout");
        if (!"ABSENT".equals(action)) {
            f.param("action", action);
        }

        f.run(harness.servlet);

        assertEquals(400, f.errorStatus);
        verifyNoInteractions(orderService);
    }

    // ---------- TC_CHECKOUT_01: đặt hàng COD thành công (khách vãng lai) ----------

    @Test
    @DisplayName("TC_CHECKOUT_01: khách vãng lai đặt COD: tạo/tìm tài khoản theo email, xóa giỏ, nhớ mã đơn trong session")
    void guestPlacesCodOrder() throws Exception {
        guestCanPlaceOrder();
        Map<Integer, Integer> lines = new LinkedHashMap<>(Map.of(5, 1));
        ServletFixture f = guestForm().param("note", "Giao giờ hành chính")
                .sessionAttr(AppConstant.SESSION_GUEST_CART, lines);

        f.run(harness.servlet);

        assertEquals(CTX + "/order-success?code=" + ORDER_CODE, f.redirectedTo);
        assertEquals(ORDER_CODE, f.sessionAttrValue(AppConstant.SESSION_GUEST_ORDER));
        assertEquals(0, f.sessionAttrValue(AppConstant.SESSION_CART_COUNT));
        assertTrue(lines.isEmpty(), "Giỏ trong session phải được xóa");
        verify(authService).findOrCreateCustomer("guest01@x.vn", "Nguyen A", "0912345678");
        ArgumentCaptor<Address> address = ArgumentCaptor.forClass(Address.class);
        verify(orderService).placeGuestOrder(eq(9), any(Cart.class), address.capture(), eq(""), eq("Giao giờ hành chính"),
                eq(PaymentMethod.COD));
        assertEquals("12 Le Loi, Ben Nghe, Quan 1, TP HCM", address.getValue().getFullAddress());
    }

    @Test
    @DisplayName("TC_CHECKOUT_01: email được chuẩn hóa (trim, chữ thường) trước khi tìm tài khoản")
    void guestEmailIsNormalised() throws Exception {
        guestCanPlaceOrder();
        ServletFixture f = guestForm().param("email", "  Guest01@X.VN ");

        f.run(harness.servlet);

        verify(authService).findOrCreateCustomer(eq("guest01@x.vn"), anyString(), anyString());
    }

    // ---------- TC_CHECKOUT_02..08: kiểm tra từng trường (EP + BVA) ----------

    static Stream<Arguments> invalidFields() {
        return Stream.of(
                // email (TC_CHECKOUT_02)
                Arguments.of("email", "", "email", "Vui lòng nhập email."),
                Arguments.of("email", "invalid-email", "email", "Email không đúng định dạng."),
                Arguments.of("email", "a@b", "email", "Email không đúng định dạng."),
                Arguments.of("email", "@x.vn", "email", "Email không đúng định dạng."),
                Arguments.of("email", "a".repeat(146) + "@x.vn", "email", "Email không đúng định dạng."), // 151 ký tự
                // tên người nhận (TC_CHECKOUT_04): biên 1/101
                Arguments.of("receiverName", "a", "receiverName", NAME_MSG),
                Arguments.of("receiverName", " a ", "receiverName", NAME_MSG),
                Arguments.of("receiverName", "n".repeat(101), "receiverName", NAME_MSG),
                // số điện thoại (TC_CHECKOUT_05)
                Arguments.of("receiverPhone", "", "receiverPhone", PHONE_MSG),
                Arguments.of("receiverPhone", "0912345", "receiverPhone", PHONE_MSG),
                Arguments.of("receiverPhone", "091234567", "receiverPhone", PHONE_MSG),
                Arguments.of("receiverPhone", "09123456789", "receiverPhone", PHONE_MSG),
                Arguments.of("receiverPhone", "1234567890", "receiverPhone", PHONE_MSG),
                Arguments.of("receiverPhone", "09123abc78", "receiverPhone", PHONE_MSG),
                // địa chỉ (TC_CHECKOUT_06): biên 4/201
                Arguments.of("addressLine", "1234", "addressLine", ADDRESS_MSG),
                Arguments.of("addressLine", "", "addressLine", ADDRESS_MSG),
                Arguments.of("addressLine", "a".repeat(201), "addressLine", ADDRESS_MSG),
                // tỉnh/thành và ghi chú (TC_CHECKOUT_07)
                Arguments.of("city", "", "city", "Vui lòng nhập tỉnh/thành phố."),
                Arguments.of("city", "   ", "city", "Vui lòng nhập tỉnh/thành phố."),
                Arguments.of("city", "c".repeat(101), "city", "Vui lòng nhập tỉnh/thành phố."),
                Arguments.of("note", "n".repeat(501), "note", "Ghi chú tối đa 500 ký tự.")
        );
    }

    @ParameterizedTest(name = "TC_CHECKOUT_02-07 {0} không hợp lệ -> lỗi ở ô {2}")
    @MethodSource("invalidFields")
    @DisplayName("TC_CHECKOUT_02..07: trường không hợp lệ thì hiện lại form kèm lỗi, không tạo đơn, không tạo tài khoản")
    void invalidFieldIsRejected(String field, String value, String errorKey, String message) throws Exception {
        guestCanPlaceOrder();
        ServletFixture f = guestForm().param(field, value);

        f.run(harness.servlet);

        assertEquals(CHECKOUT_VIEW, f.lastForward());
        assertEquals(message, f.errors().get(errorKey));
        assertNull(f.redirectedTo);
        verifyNoInteractions(orderService);
        verify(authService, never()).findOrCreateCustomer(anyString(), anyString(), anyString());
    }

    static Stream<Arguments> validBoundaries() {
        return Stream.of(
                Arguments.of("receiverName", "ab"),
                Arguments.of("receiverName", "n".repeat(100)),
                Arguments.of("receiverPhone", "0912345678"),
                Arguments.of("receiverPhone", "0000000000"),
                Arguments.of("addressLine", "12345"),
                Arguments.of("addressLine", "a".repeat(200)),
                Arguments.of("city", "c".repeat(100)),
                Arguments.of("note", "n".repeat(500)),
                Arguments.of("email", "a".repeat(145) + "@x.vn") // đúng 150 ký tự
        );
    }

    @ParameterizedTest(name = "TC_CHECKOUT_04-07 {0} = [{1}] nằm trong biên nên đặt được")
    @MethodSource("validBoundaries")
    @DisplayName("TC_CHECKOUT_04..07: giá trị đúng biên hợp lệ thì đặt hàng được")
    void validBoundaryIsAccepted(String field, String value) throws Exception {
        guestCanPlaceOrder();
        ServletFixture f = guestForm().param(field, value);

        f.run(harness.servlet);

        assertEquals(CTX + "/order-success?code=" + ORDER_CODE, f.redirectedTo, "lỗi: " + f.errors());
    }

    @Test
    @DisplayName("TC_CHECKOUT_02..07: nhiều trường sai cùng lúc thì báo hết một lượt")
    void allErrorsAreReportedTogether() throws Exception {
        guestCanPlaceOrder();
        ServletFixture f = guestForm().param("email", "x").param("receiverName", "a").param("receiverPhone", "1")
                .param("addressLine", "abc").param("city", "");

        f.run(harness.servlet);

        assertEquals(List.of("email", "receiverName", "receiverPhone", "addressLine", "city"),
                List.copyOf(f.errors().keySet()));
    }

    // ---------- TC_CHECKOUT_03: email Admin hoặc tài khoản bị khóa ----------

    @ParameterizedTest(name = "TC_CHECKOUT_03 email [{0}]")
    @DisplayName("TC_CHECKOUT_03: email Admin hoặc tài khoản bị khóa không dùng để đặt hàng được")
    @CsvSource({"admin@saltair.vn", "mai.pham@example.com"})
    void adminOrLockedEmailIsRejected(String email) throws Exception {
        guestHasCart(329_000);
        doThrow(new ValidationException("email", "Email này không dùng để đặt hàng được. Vui lòng dùng email khác."))
                .when(authService).findOrCreateCustomer(eq(email), anyString(), anyString());
        ServletFixture f = guestForm().param("email", email);

        f.run(harness.servlet);

        assertEquals("Email này không dùng để đặt hàng được. Vui lòng dùng email khác.", f.errors().get("email"));
        assertEquals(CHECKOUT_VIEW, f.lastForward());
        verifyNoInteractions(orderService);
    }

    // ---------- TC_CHECKOUT_09, 10: giả thuyết lỗi R-02 (mô tả hành vi hiện tại) ----------

    @Test
    @DisplayName("TC_CHECKOUT_09 (R-02): Controller KHÔNG giới hạn độ dài 'ward' ở server; chuỗi địa chỉ vượt cột 300 ký tự vẫn được chuyển cho Service")
    void wardOver100IsNotRejectedByServer_gapR02() throws Exception {
        guestCanPlaceOrder();
        ServletFixture f = guestForm().param("ward", "w".repeat(400));

        f.run(harness.servlet);

        // Hành vi hiện tại: không báo lỗi. Cột orders.shipping_address là VARCHAR(300) nên DB sẽ từ chối (HTTP 500).
        // Khi đã sửa lỗi này (kiểm tra độ dài ở server), đổi assert thành: có lỗi ở ô "ward", orderService không được gọi.
        ArgumentCaptor<Address> address = ArgumentCaptor.forClass(Address.class);
        verify(orderService).placeGuestOrder(eq(9), any(Cart.class), address.capture(), any(), any(), any());
        assertTrue(address.getValue().getFullAddress().length() > 300,
                "địa chỉ gửi xuống Service dài " + address.getValue().getFullAddress().length() + " ký tự (> 300)");
    }

    @Test
    @DisplayName("TC_CHECKOUT_10 (R-02): từng ô đều hợp lệ nhưng tổng chuỗi địa chỉ 506 ký tự vẫn qua Controller")
    void totalAddressLengthIsNotChecked_gapR02() throws Exception {
        guestCanPlaceOrder();
        ServletFixture f = guestForm().param("addressLine", "a".repeat(200)).param("ward", "w".repeat(100))
                .param("district", "d".repeat(100)).param("city", "c".repeat(100));

        f.run(harness.servlet);

        ArgumentCaptor<Address> address = ArgumentCaptor.forClass(Address.class);
        verify(orderService).placeGuestOrder(eq(9), any(Cart.class), address.capture(), any(), any(), any());
        assertEquals(200 + 100 + 100 + 100 + 3 * 2, address.getValue().getFullAddress().length());
    }

    // ---------- TC_CHECKOUT_08: khách đã đăng nhập ----------

    private void userHasCart(int userId, long subtotal) {
        when(cartService.getCart(userId)).thenReturn(cartWith(subtotal));
    }

    @Test
    @DisplayName("TC_CHECKOUT_08: khách đăng nhập đặt hàng bằng địa chỉ trong sổ, không cần email")
    void loggedInUsesAddressBook() throws Exception {
        userHasCart(2, 329_000);
        Address saved = new Address();
        saved.setId(1);
        saved.setUserId(2);
        saved.setFullAddress("12 Nguyễn Huệ, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh");
        when(accountService.findAddress(2, 1)).thenReturn(saved);
        when(orderService.placeOrder(eq(2), any(Address.class), any(), any(), any())).thenReturn(order(PaymentMethod.COD));
        ServletFixture f = ServletFixture.post("/checkout").param("action", "placeOrder").param("addressId", "1")
                .loggedIn(Users.customer(2));

        f.run(harness.servlet);

        verify(orderService).placeOrder(2, saved, "", null, PaymentMethod.COD);
        assertEquals(CTX + "/order-success?code=" + ORDER_CODE, f.redirectedTo);
        assertEquals(0, f.sessionAttrValue(AppConstant.SESSION_CART_COUNT));
        assertNull(f.sessionAttrValue(AppConstant.SESSION_GUEST_ORDER), "Khách đăng nhập không dùng mã đơn khách vãng lai");
    }

    @Test
    @DisplayName("TC_CHECKOUT_08: địa chỉ không thuộc người dùng (IDOR) bị từ chối")
    void addressOfAnotherUserIsRejected() throws Exception {
        userHasCart(3, 329_000);
        when(accountService.findAddress(3, 1)).thenReturn(null); // id 1 thuộc người khác nên Service trả null
        ServletFixture f = ServletFixture.post("/checkout").param("action", "placeOrder").param("addressId", "1")
                .loggedIn(Users.customer(3));

        f.run(harness.servlet);

        assertEquals("Vui lòng chọn địa chỉ giao hàng.", f.errors().get("addressId"));
        verify(accountService).findAddress(3, 1);
        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("TC_CHECKOUT_12: tick 'Lưu địa chỉ' với địa chỉ mới thì lưu vào sổ sau khi đặt; lỗi lưu địa chỉ bị bỏ qua")
    void saveNewAddressFailureIsIgnored() throws Exception {
        userHasCart(2, 329_000);
        when(orderService.placeOrder(eq(2), any(Address.class), any(), any(), any())).thenReturn(order(PaymentMethod.COD));
        doThrow(new ValidationException("phone", "sai")).when(accountService).saveAddress(any(Address.class));
        ServletFixture f = ServletFixture.post("/checkout").param("action", "placeOrder").param("addressId", "new")
                .param("receiverName", "Nguyen A").param("receiverPhone", "0912345678").param("addressLine", "12 Le Loi")
                .param("city", "HCM").param("saveAddress", "1").loggedIn(Users.customer(2));

        f.run(harness.servlet);

        verify(accountService).saveAddress(any(Address.class));
        assertEquals(CTX + "/order-success?code=" + ORDER_CODE, f.redirectedTo, "Đơn đã đặt thành công dù lưu địa chỉ lỗi");
    }

    // ---------- voucher ở tầng Controller ----------

    private static Voucher percentVoucher() {
        Voucher v = new Voucher();
        v.setId(1);
        v.setCode("WELCOME10");
        v.setDiscountType(DiscountType.PERCENT);
        v.setDiscountValue(10);
        v.setMaxDiscount(50_000L);
        return v;
    }

    @ParameterizedTest(name = "TC_VOUCHER_01/02 subtotal {0}: giảm {1}, tổng {2}")
    @DisplayName("TC_VOUCHER_01, 02: tính giảm giá, miễn phí ship và tổng tiền khi áp voucher hợp lệ")
    @CsvSource({"948000, 50000, 898000", "300000, 30000, 270000", "329000, 32900, 296100"})
    void validVoucherChangesTotals(long subtotal, long discount, long total) throws Exception {
        guestHasCart(subtotal);
        doReturn(percentVoucher()).when(voucherService).validate(eq("WELCOME10"), eq(0), any(Cart.class));
        ServletFixture f = ServletFixture.post("/checkout").param("action", "applyVoucher").param("voucherInput", " welcome10 ");

        f.run(harness.servlet);

        assertEquals(CHECKOUT_VIEW, f.lastForward());
        assertEquals(discount, f.requestAttr("discountAmount"));
        assertEquals(0L, f.requestAttr("shippingFee"));
        assertEquals(total, f.requestAttr("total"));
        assertEquals("WELCOME10", f.requestAttr("voucherCode"));
        assertNull(f.requestAttr("voucherError"));
    }

    @ParameterizedTest(name = "TC_VOUCHER_04 mã [{0}] -> {1}")
    @DisplayName("TC_VOUCHER_04: mã rỗng hoặc không dùng được thì hiện lỗi, phí ship vẫn 30.000, không giảm giá")
    @CsvSource(value = {
            "'', 'Vui lòng nhập mã giảm giá.'",
            "'   ', 'Vui lòng nhập mã giảm giá.'",
            "khongco, 'Mã giảm giá không tồn tại.'",
            "hethan20, 'Mã giảm giá HETHAN20 đã hết hạn.'"
    })
    void invalidVoucherShowsError(String input, String message) throws Exception {
        guestHasCart(329_000);
        doThrow(new VoucherException(message)).when(voucherService).validate(anyString(), anyInt(), any(Cart.class));
        ServletFixture f = ServletFixture.post("/checkout").param("action", "applyVoucher").param("voucherInput", input);

        f.run(harness.servlet);

        assertEquals(message, f.requestAttr("voucherError"));
        assertEquals(30_000L, f.requestAttr("shippingFee"));
        assertEquals(0L, f.requestAttr("discountAmount"));
        assertEquals(359_000L, f.requestAttr("total"));
        assertNull(f.requestAttr("appliedVoucher"));
    }

    @Test
    @DisplayName("TC_CHECKOUT_13: gỡ mã thì phí ship về 30.000 và không còn mã áp dụng")
    void removeVoucher() throws Exception {
        guestHasCart(329_000);
        ServletFixture f = ServletFixture.post("/checkout").param("action", "removeVoucher").param("voucherCode", "WELCOME10");

        f.run(harness.servlet);

        assertEquals(CHECKOUT_VIEW, f.lastForward());
        assertNull(f.requestAttr("appliedVoucher"));
        assertEquals(30_000L, f.requestAttr("shippingFee"));
    }

    @Test
    @DisplayName("TC_CHECKOUT_12: mã đang áp dụng được gửi xuống Service lúc đặt hàng (Service kiểm tra lại theo tài khoản)")
    void voucherCodeIsPassedToPlaceOrder() throws Exception {
        guestCanPlaceOrder();
        ServletFixture f = guestForm().param("voucherCode", "welcome10");

        f.run(harness.servlet);

        verify(orderService).placeGuestOrder(eq(9), any(Cart.class), any(Address.class), eq("WELCOME10"), any(), any());
    }

    @Test
    @DisplayName("TC_CHECKOUT_12: voucher hết hiệu lực lúc đặt: lỗi hiện ở khung voucher, mã bị bỏ, không lỗi chung")
    void voucherRejectedAtPlaceOrder() throws Exception {
        guestCanPlaceOrder();
        when(orderService.placeGuestOrder(anyInt(), any(Cart.class), any(Address.class), any(), any(), any()))
                .thenThrow(new ValidationException("voucherCode", "Mã giảm giá WELCOME10 vừa hết lượt sử dụng."));
        ServletFixture f = guestForm().param("voucherCode", "WELCOME10");

        f.run(harness.servlet);

        assertEquals("Mã giảm giá WELCOME10 vừa hết lượt sử dụng.", f.requestAttr("voucherError"));
        assertFalse(f.errors().containsKey("voucherCode"));
        assertEquals("", f.requestAttr("voucherCode"));
        assertEquals(CHECKOUT_VIEW, f.lastForward());
    }

    @Test
    @DisplayName("Lỗi chung từ Service (không gắn ô nào) hiện thành thông báo, không phải lỗi ô")
    void generalErrorBecomesMessage() throws Exception {
        guestCanPlaceOrder();
        when(orderService.placeGuestOrder(anyInt(), any(Cart.class), any(Address.class), any(), any(), any()))
                .thenThrow(new ValidationException(null, "\"Áo polo\" (Đen, M) vừa hết hàng. Hãy chỉnh lại giỏ hàng."));
        ServletFixture f = guestForm();

        f.run(harness.servlet);

        assertEquals("\"Áo polo\" (Đen, M) vừa hết hàng. Hãy chỉnh lại giỏ hàng.", f.requestAttr("message"));
        assertEquals("error", f.requestAttr("messageType"));
        assertTrue(f.errors().isEmpty());
    }

    // ---------- TC_CHECKOUT_11: chuyển khoản ----------

    @Test
    @DisplayName("TC_CHECKOUT_11: payOS chưa cấu hình thì BANK_TRANSFER bị từ chối dù client tự gửi tham số")
    void bankTransferRejectedWhenDisabled() throws Exception {
        guestCanPlaceOrder();
        ServletFixture f = guestForm().param("paymentMethod", "BANK_TRANSFER");

        f.run(harness.servlet);

        assertEquals("Hiện chưa hỗ trợ chuyển khoản. Vui lòng chọn thanh toán khi nhận hàng.", f.errors().get("paymentMethod"));
        assertEquals(false, f.requestAttr("bankTransferEnabled"));
        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("Khi payOS bật, đặt BANK_TRANSFER chuyển sang /order-pay thay vì /order-success")
    void bankTransferGoesToPaymentPage() throws Exception {
        paymentConfig.when(PaymentConfig::get).thenReturn(payOsOn);
        guestHasCart(329_000);
        doReturn(Users.customer(9)).when(authService).findOrCreateCustomer(anyString(), anyString(), anyString());
        when(orderService.placeGuestOrder(eq(9), any(Cart.class), any(Address.class), any(), any(), eq(PaymentMethod.BANK_TRANSFER)))
                .thenReturn(order(PaymentMethod.BANK_TRANSFER));
        ServletFixture f = guestForm().param("paymentMethod", "BANK_TRANSFER");

        f.run(harness.servlet);

        assertEquals(CTX + "/order-pay?code=" + ORDER_CODE, f.redirectedTo);
    }

    @Test
    @DisplayName("Giá trị paymentMethod lạ được coi là COD")
    void unknownPaymentMethodIsCod() throws Exception {
        guestCanPlaceOrder();
        ServletFixture f = guestForm().param("paymentMethod", "BITCOIN");

        f.run(harness.servlet);

        verify(orderService).placeGuestOrder(eq(9), any(Cart.class), any(Address.class), any(), any(), eq(PaymentMethod.COD));
    }

    // ---------- lỗi hạ tầng ----------

    @Test
    @DisplayName("Lỗi database khi đặt hàng không bị nuốt: lan ra để container trả trang 500")
    void databaseErrorPropagates() {
        guestCanPlaceOrder();
        when(orderService.placeGuestOrder(anyInt(), any(Cart.class), any(Address.class), any(), any(), any()))
                .thenThrow(new DataAccessException("Lỗi đặt hàng", new java.sql.SQLException("Data too long")));
        ServletFixture f = guestForm();

        assertThrows(DataAccessException.class, () -> f.run(harness.servlet));
        assertNull(f.redirectedTo);
    }
}
