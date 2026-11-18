package com.poly.sof309.controllers;

import com.fashionweb.constant.AppConstant;
import com.fashionweb.constant.OrderStatus;
import com.fashionweb.constant.PaymentMethod;
import com.fashionweb.constant.PaymentStatus;
import com.fashionweb.controller.order.OrderDetailServlet;
import com.fashionweb.controller.order.OrderServlet;
import com.fashionweb.controller.order.OrderSuccessServlet;
import com.fashionweb.exception.ValidationException;
import com.fashionweb.model.Order;
import com.fashionweb.service.order.OrderService;
import com.fashionweb.service.order.PaymentService;
import com.fashionweb.service.shop.ProductService;
import com.fashionweb.util.Page;
import com.fashionweb.util.PaymentConfig;
import com.poly.sof309.controllers.support.Harness;
import com.poly.sof309.controllers.support.ServletFixture;
import com.poly.sof309.controllers.support.Users;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static com.poly.sof309.controllers.support.ServletFixture.CTX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Controller đơn hàng của khách: TC_ORDER_06, 07, 08, 12, 13 ở tầng Controller.
 * Quy tắc hủy/chuyển trạng thái nằm ở OrderService (cần DB nên kiểm thử riêng); ở đây kiểm tra Controller
 * chuyển đúng dữ liệu, dùng đúng id người dùng trong session, và hiển thị đúng kết quả.
 */
@Timeout(value = 5, unit = TimeUnit.SECONDS)
class OrderControllerTest {

    private static final String CODE = "SA20261105001";

    private Harness<?> harness;
    private MockedStatic<PaymentConfig> paymentConfig;

    @AfterEach
    void tearDown() {
        if (harness != null) {
            harness.close();
        }
        if (paymentConfig != null) {
            paymentConfig.close();
        }
    }

    private OrderService orderServiceOf(Harness<?> h) {
        harness = h;
        return h.service(OrderService.class);
    }

    private static Order order(String code, PaymentMethod method, PaymentStatus payment, OrderStatus status) {
        Order o = new Order();
        o.setOrderCode(code);
        o.setPaymentMethod(method);
        o.setPaymentStatus(payment);
        o.setStatus(status);
        return o;
    }

    // ---------- POST /order: hủy đơn, đã nhận hàng ----------

    @ParameterizedTest(name = "TC_ORDER_06 returnTo [{0}] -> quay về {1}")
    @DisplayName("TC_ORDER_06: hủy đơn thành công, quay về đúng trang theo returnTo")
    @CsvSource(value = {
            "detail, /order?code=SA20261105001",
            "orders, /orders",
            "ABSENT, /orders",
            "evil.com, /orders"
    })
    void cancelSuccessRedirectsBack(String returnTo, String expected) throws Exception {
        Harness<OrderDetailServlet> h = Harness.mocked(OrderDetailServlet::new, OrderService.class);
        OrderService orders = orderServiceOf(h);
        ServletFixture f = ServletFixture.post("/order").param("action", "cancel").param("orderCode", CODE)
                .param("cancelReason", "Mình đặt nhầm size").loggedIn(Users.customer(2));
        if (!"ABSENT".equals(returnTo)) {
            f.param("returnTo", returnTo);
        }

        f.run(h.servlet);

        verify(orders).cancelByCustomer(CODE, 2, "Mình đặt nhầm size");
        assertEquals("Đã hủy đơn " + CODE + ". Tồn kho và mã giảm giá đã được hoàn lại.", f.flashMessage());
        assertEquals("success", f.flashType());
        assertEquals(CTX + expected, f.redirectedTo);
    }

    @Test
    @DisplayName("TC_ORDER_07, 08: Service từ chối (lý do ngắn, sai trạng thái, đã thanh toán) thì hiện đúng thông báo lỗi")
    void cancelRejectedByServiceShowsError() throws Exception {
        Harness<OrderDetailServlet> h = Harness.mocked(OrderDetailServlet::new, OrderService.class);
        OrderService orders = orderServiceOf(h);
        doThrow(new ValidationException("cancelReason", "Lý do hủy cần từ 5 đến 255 ký tự."))
                .when(orders).cancelByCustomer(CODE, 2, "abc");
        ServletFixture f = ServletFixture.post("/order").param("action", "cancel").param("orderCode", CODE)
                .param("cancelReason", "abc").loggedIn(Users.customer(2));

        f.run(h.servlet);

        assertEquals("Lý do hủy cần từ 5 đến 255 ký tự.", f.flashMessage());
        assertEquals("error", f.flashType());
        assertEquals(CTX + "/orders", f.redirectedTo);
    }

    @Test
    @DisplayName("TC_ORDER_08: Controller luôn dùng id trong session, bỏ qua userId do client gửi (không hủy đơn của người khác)")
    void cancelUsesSessionUser() throws Exception {
        Harness<OrderDetailServlet> h = Harness.mocked(OrderDetailServlet::new, OrderService.class);
        OrderService orders = orderServiceOf(h);
        ServletFixture f = ServletFixture.post("/order").param("action", "cancel").param("orderCode", "SA20261104006")
                .param("cancelReason", "Hủy giúp mình nhé").param("userId", "3").loggedIn(Users.customer(2));

        f.run(h.servlet);

        verify(orders).cancelByCustomer("SA20261104006", 2, "Hủy giúp mình nhé");
    }

    @Test
    @DisplayName("TC_ORDER_09: xác nhận đã nhận hàng")
    void confirmReceived() throws Exception {
        Harness<OrderDetailServlet> h = Harness.mocked(OrderDetailServlet::new, OrderService.class);
        OrderService orders = orderServiceOf(h);
        ServletFixture f = ServletFixture.post("/order").param("action", "confirmReceived").param("orderCode", CODE)
                .loggedIn(Users.customer(2));

        f.run(h.servlet);

        verify(orders).confirmReceived(CODE, 2);
        assertEquals("Cảm ơn bạn! Đơn " + CODE + " đã hoàn thành. Hãy đánh giá sản phẩm bạn đã mua.", f.flashMessage());
    }

    @Test
    @DisplayName("TC_ORDER_09: đơn chưa ở trạng thái Đang giao thì Service từ chối và Controller hiện lỗi")
    void confirmReceivedRejected() throws Exception {
        Harness<OrderDetailServlet> h = Harness.mocked(OrderDetailServlet::new, OrderService.class);
        OrderService orders = orderServiceOf(h);
        doThrow(new ValidationException(null, "Chỉ xác nhận đã nhận hàng khi đơn đang giao."))
                .when(orders).confirmReceived(CODE, 2);
        ServletFixture f = ServletFixture.post("/order").param("action", "confirmReceived").param("orderCode", CODE)
                .loggedIn(Users.customer(2));

        f.run(h.servlet);

        assertEquals("Chỉ xác nhận đã nhận hàng khi đơn đang giao.", f.flashMessage());
        assertEquals("error", f.flashType());
    }

    @ParameterizedTest(name = "action [{0}] trả 400")
    @DisplayName("POST /order với action thiếu hoặc lạ trả 400, không gọi Service")
    @CsvSource(value = {"delete", "CANCEL", "ABSENT"})
    void unknownActionIs400(String action) throws Exception {
        Harness<OrderDetailServlet> h = Harness.mocked(OrderDetailServlet::new, OrderService.class);
        orderServiceOf(h);
        ServletFixture f = ServletFixture.post("/order").param("orderCode", CODE).loggedIn(Users.customer(2));
        if (!"ABSENT".equals(action)) {
            f.param("action", action);
        }

        f.run(h.servlet);

        assertEquals(400, f.errorStatus);
        verifyNoInteractions(h.service(OrderService.class));
    }

    @Test
    @DisplayName("TC_ORDER_13: GET /order của đơn không thuộc người dùng trả 404; đơn của mình thì hiện chi tiết")
    void orderDetailOnlyForOwner() throws Exception {
        Harness<OrderDetailServlet> h = Harness.mocked(OrderDetailServlet::new, OrderService.class);
        OrderService orders = orderServiceOf(h);
        when(orders.findByCodeAndUser("SA20261104006", 2)).thenReturn(null); // đơn của lan.tran
        when(orders.findByCodeAndUser(CODE, 2)).thenReturn(order(CODE, PaymentMethod.COD, PaymentStatus.UNPAID, OrderStatus.PENDING));
        ServletFixture others = ServletFixture.get("/order").param("code", "SA20261104006").loggedIn(Users.customer(2));
        ServletFixture mine = ServletFixture.get("/order").param("code", CODE).loggedIn(Users.customer(2));

        others.run(h.servlet);
        mine.run(h.servlet);

        assertEquals(404, others.errorStatus);
        assertNull(others.lastForward());
        assertEquals("/WEB-INF/views/order/order-detail.jsp", mine.lastForward());
        assertEquals(CODE, ((Order) mine.requestAttr("order")).getOrderCode());
    }

    // ---------- GET /orders: tham số lọc ----------

    @ParameterizedTest(name = "TC_ORDER_12 status=[{0}] page=[{1}] -> lọc {2}, trang {3}")
    @DisplayName("TC_ORDER_12: status sai coi là Tất cả (không lọc), page không phải số coi là 1, status không phân biệt hoa thường")
    @CsvSource(value = {
            "ABSENT, ABSENT, ALL, 1",
            "abc, abc, ALL, 1",
            "'', '', ALL, 1",
            "CANCELLED, 2, CANCELLED, 2",
            "completed, 3, COMPLETED, 3",
            "'  pending ', 1, PENDING, 1"
    })
    void orderListParameters(String status, String page, String expectedStatus, int expectedPage) throws Exception {
        Harness<OrderServlet> h = Harness.mocked(OrderServlet::new, OrderService.class, ProductService.class);
        OrderService orders = orderServiceOf(h);
        OrderStatus filter = "ALL".equals(expectedStatus) ? null : OrderStatus.valueOf(expectedStatus);
        when(orders.findPageByUser(2, filter, expectedPage, AppConstant.ORDER_PAGE_SIZE))
                .thenReturn(new Page<>(List.of(), expectedPage, AppConstant.ORDER_PAGE_SIZE, 0));
        when(orders.countByStatusForUser(2)).thenReturn(Map.of("ALL", 4, "CANCELLED", 1));
        ServletFixture f = ServletFixture.get("/orders").loggedIn(Users.customer(2));
        if (!"ABSENT".equals(status)) {
            f.param("status", status);
        }
        if (!"ABSENT".equals(page)) {
            f.param("page", page);
        }

        f.run(h.servlet);

        verify(orders).findPageByUser(2, filter, expectedPage, AppConstant.ORDER_PAGE_SIZE);
        assertEquals("ALL".equals(expectedStatus) ? "" : expectedStatus, f.requestAttr("status"));
        assertEquals("/WEB-INF/views/order/orders.jsp", f.lastForward());
        assertNull(f.requestAttr("welcomeProducts"), "Khách đã có đơn thì không hiện thẻ chào mừng");
    }

    @Test
    @DisplayName("TC_ORDER_12: khách chưa có đơn nào thì có dải sản phẩm chào mừng; baseUrl theo bộ lọc")
    void welcomeProductsForNewCustomer() throws Exception {
        Harness<OrderServlet> h = Harness.mocked(OrderServlet::new, OrderService.class, ProductService.class);
        OrderService orders = orderServiceOf(h);
        when(orders.findPageByUser(anyInt(), Mockito.any(), anyInt(), anyInt()))
                .thenReturn(new Page<>(List.of(), 1, AppConstant.ORDER_PAGE_SIZE, 0));
        when(orders.countByStatusForUser(5)).thenReturn(Map.of("ALL", 0));
        ServletFixture f = ServletFixture.get("/orders").param("status", "SHIPPING").loggedIn(Users.customer(5));

        f.run(h.servlet);

        assertTrue(f.requestAttr("welcomeProducts") != null);
        assertEquals("/orders?status=SHIPPING", f.requestAttr("baseUrl"));
    }

    // ---------- GET /order-success: ai được xem đơn ----------

    private Harness<OrderSuccessServlet> successHarness() {
        // Phải tạo PaymentConfig trước khi mock static
        PaymentConfig off = new PaymentConfig(new Properties());
        paymentConfig = Mockito.mockStatic(PaymentConfig.class);
        paymentConfig.when(PaymentConfig::get).thenReturn(off);
        Harness<OrderSuccessServlet> h = Harness.mocked(OrderSuccessServlet::new, OrderService.class, PaymentService.class);
        harness = h;
        return h;
    }

    @Test
    @DisplayName("TC_ORDER_13: khách vãng lai chỉ xem được đơn vừa đặt trong phiên; đơn khác thì 404")
    void guestSeesOnlyOwnSessionOrder() throws Exception {
        Harness<OrderSuccessServlet> h = successHarness();
        OrderService orders = h.service(OrderService.class);
        when(orders.findByCode(CODE)).thenReturn(order(CODE, PaymentMethod.COD, PaymentStatus.UNPAID, OrderStatus.PENDING));
        ServletFixture mine = ServletFixture.get("/order-success").param("code", CODE)
                .sessionAttr(AppConstant.SESSION_GUEST_ORDER, CODE);
        ServletFixture other = ServletFixture.get("/order-success").param("code", "SA20261104006")
                .sessionAttr(AppConstant.SESSION_GUEST_ORDER, CODE);
        ServletFixture noSession = ServletFixture.get("/order-success").param("code", CODE).noSession();

        mine.run(h.servlet);
        other.run(h.servlet);
        noSession.run(h.servlet);

        assertEquals("/WEB-INF/views/order/order-success.jsp", mine.lastForward());
        assertEquals(false, mine.requestAttr("payable"), "Đơn COD không cần thanh toán");
        assertEquals(404, other.errorStatus);
        assertEquals(404, noSession.errorStatus);
    }

    @Test
    @DisplayName("TC_ORDER_13: khách đăng nhập xem đơn của người khác thì 404")
    void loggedInCannotSeeAnotherUsersOrder() throws Exception {
        Harness<OrderSuccessServlet> h = successHarness();
        when(h.service(OrderService.class).findByCodeAndUser(anyString(), anyInt())).thenReturn(null);
        ServletFixture f = ServletFixture.get("/order-success").param("code", "SA20261104006").loggedIn(Users.customer(2));

        f.run(h.servlet);

        assertEquals(404, f.errorStatus);
    }

    @ParameterizedTest(name = "payment={0}: JSON {1}")
    @DisplayName("GET /order-success?status=1 trả JSON {\"paid\":true|false} theo trạng thái thanh toán")
    @CsvSource({"PAID, '{\"paid\":true}'", "UNPAID, '{\"paid\":false}'"})
    void statusPolling(PaymentStatus payment, String json) throws Exception {
        Harness<OrderSuccessServlet> h = successHarness();
        when(h.service(OrderService.class).findByCodeAndUser(CODE, 2))
                .thenReturn(order(CODE, PaymentMethod.COD, payment, OrderStatus.PENDING));
        ServletFixture f = ServletFixture.get("/order-success").param("code", CODE).param("status", "1")
                .loggedIn(Users.customer(2));

        f.run(h.servlet);

        f.resp.getWriter().flush();
        assertEquals(json, f.responseBody());
        assertNull(f.lastForward(), "Trả JSON, không forward JSP");
    }
}
