package com.poly.sof309.controllers;

import com.fashionweb.constant.OrderStatus;
import com.fashionweb.constant.PaymentMethod;
import com.fashionweb.constant.PaymentStatus;
import com.fashionweb.constant.Role;
import com.fashionweb.controller.admin.AdminOpsParams;
import com.fashionweb.controller.admin.sales.AdminOrderDetailServlet;
import com.fashionweb.controller.admin.system.AdminDashboardServlet;
import com.fashionweb.controller.admin.system.AdminUserServlet;
import com.fashionweb.dto.DashboardStats;
import com.fashionweb.exception.ValidationException;
import com.fashionweb.model.Order;
import com.fashionweb.model.User;
import com.fashionweb.service.admin.StatisticService;
import com.fashionweb.service.auth.UserService;
import com.fashionweb.service.order.OrderService;
import com.fashionweb.service.shop.ProductService;
import com.fashionweb.util.Page;
import com.poly.sof309.controllers.support.Harness;
import com.poly.sof309.controllers.support.ServletFixture;
import com.poly.sof309.controllers.support.Users;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.poly.sof309.controllers.support.ServletFixture.CTX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Controller quản trị: TC_ADM_08, TC_ADM_12, TC_ADM_13, TC_ORDER_10, 11 ở tầng Controller, cùng hàm tiện ích AdminOpsParams. */
@Timeout(value = 5, unit = TimeUnit.SECONDS)
class AdminControllerTest {

    private static final String CODE = "SA20261103002";

    private Harness<?> harness;

    @AfterEach
    void closeHarness() {
        if (harness != null) {
            harness.close();
        }
    }

    // =====================================================================
    // /admin/users (TC_ADM_08)
    // =====================================================================

    private Harness<AdminUserServlet> userHarness() {
        Harness<AdminUserServlet> h = Harness.mocked(AdminUserServlet::new, UserService.class);
        harness = h;
        return h;
    }

    @ParameterizedTest(name = "TC_ADM_08 toggleActive trả {0} -> [{1}]")
    @DisplayName("TC_ADM_08: khóa / mở khóa tài khoản, admin id lấy từ session")
    @CsvSource({"true, 'Đã mở khóa tài khoản.'", "false, 'Đã khóa tài khoản.'"})
    void toggleActive(boolean nowActive, String message) throws Exception {
        Harness<AdminUserServlet> h = userHarness();
        when(h.service(UserService.class).toggleActive(1, 5)).thenReturn(nowActive);
        ServletFixture f = ServletFixture.post("/admin/users").param("action", "toggleActive").param("id", "5")
                .param("adminId", "99").loggedIn(Users.admin(1));

        f.run(h.servlet);

        verify(h.service(UserService.class)).toggleActive(1, 5);
        assertEquals(message, f.flashMessage());
        assertEquals("success", f.flashType());
        assertEquals(CTX + "/admin/users", f.redirectedTo);
    }

    @Test
    @DisplayName("TC_ADM_08: Admin tự khóa mình bị Service từ chối, Controller hiện lỗi")
    void adminCannotLockSelf() throws Exception {
        Harness<AdminUserServlet> h = userHarness();
        doThrow(new ValidationException(null, "Bạn không thể tự khóa tài khoản của chính mình."))
                .when(h.service(UserService.class)).toggleActive(1, 1);
        ServletFixture f = ServletFixture.post("/admin/users").param("action", "toggleActive").param("id", "1").loggedIn(Users.admin(1));

        f.run(h.servlet);

        assertEquals("Bạn không thể tự khóa tài khoản của chính mình.", f.flashMessage());
        assertEquals("error", f.flashType());
    }

    @ParameterizedTest(name = "TC_ADM_08 role=[{0}] -> {1}")
    @DisplayName("TC_ADM_08: role không phân biệt hoa thường; role lạ thành null để Service từ chối")
    @CsvSource(value = {"admin, ADMIN", "CUSTOMER, CUSTOMER", "xyz, NULL", "'', NULL", "ABSENT, NULL"})
    void changeRoleParsing(String role, String expected) throws Exception {
        Harness<AdminUserServlet> h = userHarness();
        ServletFixture f = ServletFixture.post("/admin/users").param("action", "changeRole").param("id", "5").loggedIn(Users.admin(1));
        if (!"ABSENT".equals(role)) {
            f.param("role", role);
        }

        f.run(h.servlet);

        verify(h.service(UserService.class)).changeRole(1, 5, "NULL".equals(expected) ? null : Role.valueOf(expected));
    }

    @ParameterizedTest(name = "Referer [{0}] -> quay về [{1}]")
    @DisplayName("Quay về đúng trang danh sách đang xem (giữ bộ lọc); Referer ngoài khu /admin/users bị bỏ")
    @CsvSource(delimiter = '|', value = {
            "http://localhost:8080/saltair/admin/users?q=lan&page=2 | /saltair/admin/users?q=lan&page=2",
            "http://localhost:8080/saltair/admin/orders | /saltair/admin/users",
            "http://evil.com/phishing | /saltair/admin/users"
    })
    void returnsToListWithFilters(String referer, String expected) throws Exception {
        Harness<AdminUserServlet> h = userHarness();
        ServletFixture f = ServletFixture.post("/admin/users").param("action", "toggleActive").param("id", "5")
                .header("Referer", referer).loggedIn(Users.admin(1));

        f.run(h.servlet);

        assertEquals(expected, f.redirectedTo);
    }

    @ParameterizedTest(name = "q=[{0}] role=[{1}] page=[{2}]")
    @DisplayName("GET /admin/users: bộ lọc sai bị bỏ, từ khóa cắt 100 ký tự, trang tối thiểu 1")
    @CsvSource(value = {"lan, admin, 3, lan, ADMIN, 3", "ABSENT, foo, abc, '', NULL, 1", "ABSENT, ABSENT, 0, '', NULL, 1"})
    void userListParameters(String q, String role, String page, String expQ, String expRole, int expPage) throws Exception {
        Harness<AdminUserServlet> h = userHarness();
        when(h.service(UserService.class).search(anyString(), any(), anyInt(), anyInt())).thenReturn(new Page<User>(List.of(), 1, 10, 0));
        ServletFixture f = ServletFixture.get("/admin/users").loggedIn(Users.admin(1));
        if (!"ABSENT".equals(q)) {
            f.param("q", q);
        }
        if (!"ABSENT".equals(role)) {
            f.param("role", role);
        }
        f.param("page", page);

        f.run(h.servlet);

        verify(h.service(UserService.class)).search(expQ, "NULL".equals(expRole) ? null : Role.valueOf(expRole), expPage, 10);
        assertEquals("NULL".equals(expRole) ? "" : expRole, f.requestAttr("role"));
        assertEquals(1, f.requestAttr("currentAdminId"));
    }

    @Test
    @DisplayName("POST /admin/users với action lạ trả 400")
    void usersUnknownAction() throws Exception {
        Harness<AdminUserServlet> h = userHarness();
        ServletFixture f = ServletFixture.post("/admin/users").param("action", "delete").loggedIn(Users.admin(1));

        f.run(h.servlet);

        assertEquals(400, f.errorStatus);
        verifyNoInteractions(h.service(UserService.class));
    }

    // =====================================================================
    // /admin/order (TC_ORDER_10, 11)
    // =====================================================================

    private Harness<AdminOrderDetailServlet> orderHarness() {
        Harness<AdminOrderDetailServlet> h = Harness.mocked(AdminOrderDetailServlet::new, OrderService.class);
        harness = h;
        return h;
    }

    @ParameterizedTest(name = "TC_ORDER_10 chuyển sang {0}")
    @DisplayName("TC_ORDER_10: chuyển trạng thái đơn, thông báo dùng nhãn tiếng Việt")
    @CsvSource({"SHIPPING, 'Đang giao'", "completed, 'Hoàn thành'"})
    void updateStatus(String status, String label) throws Exception {
        Harness<AdminOrderDetailServlet> h = orderHarness();
        ServletFixture f = ServletFixture.post("/admin/order").param("action", "updateStatus").param("orderCode", CODE)
                .param("status", status).loggedIn(Users.admin(1));

        f.run(h.servlet);

        verify(h.service(OrderService.class)).updateStatus(CODE, OrderStatus.valueOf(status.toUpperCase()));
        assertEquals("Đã chuyển đơn " + CODE + " sang \"" + label + "\".", f.flashMessage());
        assertEquals(CTX + "/admin/order?code=" + CODE, f.redirectedTo);
    }

    @Test
    @DisplayName("TC_ORDER_10: nhảy bước hoặc trạng thái lạ bị Service từ chối, Controller hiện lỗi (không NullPointerException)")
    void invalidTransitionShowsError() throws Exception {
        Harness<AdminOrderDetailServlet> h = orderHarness();
        String message = "Chỉ chuyển được theo thứ tự Chờ xác nhận → Đang giao → Hoàn thành.";
        // trạng thái lạ "abc" được đổi thành null; Service thật ném ValidationException cho next == null
        doThrow(new ValidationException(null, message)).when(h.service(OrderService.class)).updateStatus(eq(CODE), isNull());
        ServletFixture f = ServletFixture.post("/admin/order").param("action", "updateStatus").param("orderCode", CODE)
                .param("status", "abc").loggedIn(Users.admin(1));

        f.run(h.servlet);

        assertEquals(message, f.flashMessage());
        assertEquals("error", f.flashType());
        assertEquals(CTX + "/admin/order?code=" + CODE, f.redirectedTo);
    }

    @Test
    @DisplayName("TC_ORDER_11: hủy đơn thành công")
    void cancelSuccess() throws Exception {
        Harness<AdminOrderDetailServlet> h = orderHarness();
        ServletFixture f = ServletFixture.post("/admin/order").param("action", "cancel").param("orderCode", CODE)
                .param("cancelReason", "Hết hàng màu đã chọn").loggedIn(Users.admin(1));

        f.run(h.servlet);

        verify(h.service(OrderService.class)).cancelByAdmin(CODE, "Hết hàng màu đã chọn");
        assertEquals("Đã hủy đơn " + CODE + ". Tồn kho và lượt voucher đã được hoàn lại.", f.flashMessage());
    }

    @Test
    @DisplayName("TC_ORDER_11: thiếu lý do hủy thì hiện lại trang chi tiết kèm lỗi (không flash, không redirect)")
    void cancelWithoutReasonReopensPage() throws Exception {
        Harness<AdminOrderDetailServlet> h = orderHarness();
        OrderService orders = h.service(OrderService.class);
        doThrow(new ValidationException("cancelReason", "Lý do hủy cần từ 5 đến 255 ký tự.")).when(orders).cancelByAdmin(CODE, "");
        Order order = new Order();
        order.setOrderCode(CODE);
        order.setStatus(OrderStatus.SHIPPING);
        order.setPaymentMethod(PaymentMethod.COD);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        when(orders.findByCode(CODE)).thenReturn(order);
        ServletFixture f = ServletFixture.post("/admin/order").param("action", "cancel").param("orderCode", CODE)
                .param("cancelReason", "").loggedIn(Users.admin(1));

        f.run(h.servlet);

        assertEquals("/WEB-INF/views/admin/sales/order-detail.jsp", f.lastForward());
        assertEquals("Lý do hủy cần từ 5 đến 255 ký tự.", f.errors().get("cancelReason"));
        assertEquals(order, f.requestAttr("order"));
        assertNull(f.redirectedTo);
        assertNull(f.flashMessage());
    }

    @Test
    @DisplayName("TC_ORDER_11: lỗi trạng thái (đơn đã hoàn thành) hiện bằng flash và quay lại trang chi tiết")
    void cancelWrongStatusShowsFlash() throws Exception {
        Harness<AdminOrderDetailServlet> h = orderHarness();
        doThrow(new ValidationException(null, "Chỉ hủy được đơn đang Chờ xác nhận hoặc Đang giao."))
                .when(h.service(OrderService.class)).cancelByAdmin(CODE, "Khách yêu cầu");
        ServletFixture f = ServletFixture.post("/admin/order").param("action", "cancel").param("orderCode", CODE)
                .param("cancelReason", "Khách yêu cầu").loggedIn(Users.admin(1));

        f.run(h.servlet);

        assertEquals("Chỉ hủy được đơn đang Chờ xác nhận hoặc Đang giao.", f.flashMessage());
        assertEquals(CTX + "/admin/order?code=" + CODE, f.redirectedTo);
    }

    @Test
    @DisplayName("GET /admin/order: mã không tồn tại trả 404; action lạ trả 400")
    void adminOrderNotFoundAndUnknownAction() throws Exception {
        Harness<AdminOrderDetailServlet> h = orderHarness();
        ServletFixture missing = ServletFixture.get("/admin/order").param("code", "SA00000000000").loggedIn(Users.admin(1));
        ServletFixture unknown = ServletFixture.post("/admin/order").param("action", "delete").param("orderCode", CODE).loggedIn(Users.admin(1));

        missing.run(h.servlet);
        unknown.run(h.servlet);

        assertEquals(404, missing.errorStatus);
        assertEquals(400, unknown.errorStatus);
    }

    // =====================================================================
    // /admin: khoảng ngày của dashboard (TC_ADM_12)
    // =====================================================================

    private Harness<AdminDashboardServlet> dashboardHarness() {
        Harness<AdminDashboardServlet> h = Harness.mocked(AdminDashboardServlet::new,
                StatisticService.class, OrderService.class, ProductService.class);
        harness = h;
        DashboardStats stats = new DashboardStats();
        stats.setRevenueByDay(List.of(new DashboardStats.DailyRevenue("01/11", 100_000), new DashboardStats.DailyRevenue("02/11", 300_000)));
        when(h.service(StatisticService.class).getStats(any(LocalDate.class), any(LocalDate.class))).thenReturn(stats);
        return h;
    }

    private static final LocalDate TO = LocalDate.of(2026, 11, 18);

    static Stream<Arguments> dateRanges() {
        return Stream.of(
                Arguments.of("2026-11-01", "2026-11-10", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 10)),
                Arguments.of("2026-11-10", "2026-11-01", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 10)), // đảo chỗ
                Arguments.of("2026-11-18", "2026-11-18", TO, TO), // một ngày
                Arguments.of(TO.minusDays(91).toString(), TO.toString(), TO.minusDays(91), TO), // đúng 92 ngày: giữ nguyên
                Arguments.of(TO.minusDays(92).toString(), TO.toString(), TO.minusDays(91), TO), // 93 ngày: cắt về 92
                Arguments.of("2026-01-01", TO.toString(), TO.minusDays(91), TO), // quá dài
                Arguments.of("abc", "2026-11-10", LocalDate.of(2026, 11, 4), LocalDate.of(2026, 11, 10)), // from sai: to - 6
                Arguments.of("2026-13-45", "2026-11-10", LocalDate.of(2026, 11, 4), LocalDate.of(2026, 11, 10)),
                Arguments.of("", "2026-11-10", LocalDate.of(2026, 11, 4), LocalDate.of(2026, 11, 10))
        );
    }

    @ParameterizedTest(name = "TC_ADM_12 {0} .. {1} -> {2} .. {3}")
    @MethodSource("dateRanges")
    @DisplayName("TC_ADM_12: đảo ngày, giới hạn 92 ngày, ngày sai định dạng dùng mặc định")
    void dashboardDateRange(String from, String to, LocalDate expectedFrom, LocalDate expectedTo) throws Exception {
        Harness<AdminDashboardServlet> h = dashboardHarness();
        ServletFixture f = ServletFixture.get("/admin").param("dateFrom", from).param("dateTo", to).loggedIn(Users.admin(1));

        f.run(h.servlet);

        verify(h.service(StatisticService.class)).getStats(expectedFrom, expectedTo);
        assertEquals(expectedFrom.toString(), f.requestAttr("dateFrom"));
        assertEquals(expectedTo.toString(), f.requestAttr("dateTo"));
        assertEquals(300_000L, f.requestAttr("revenueMax"));
        assertEquals(400_000L, f.requestAttr("rangeRevenue"));
        assertEquals("/WEB-INF/views/admin/system/dashboard.jsp", f.lastForward());
    }

    @Test
    @DisplayName("TC_ADM_12: không có tham số thì lấy 7 ngày gần nhất tính đến hôm nay")
    void dashboardDefaultsToLastSevenDays() throws Exception {
        Harness<AdminDashboardServlet> h = dashboardHarness();
        ServletFixture f = ServletFixture.get("/admin").loggedIn(Users.admin(1));

        f.run(h.servlet);

        ArgumentCaptor<LocalDate> from = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> to = ArgumentCaptor.forClass(LocalDate.class);
        verify(h.service(StatisticService.class)).getStats(from.capture(), to.capture());
        assertEquals(LocalDate.now(), to.getValue());
        assertEquals(LocalDate.now().minusDays(6), from.getValue());
    }

    // =====================================================================
    // AdminOpsParams: đọc tham số an toàn, không bao giờ ném lỗi (TC_ADM_13)
    // =====================================================================

    @ParameterizedTest(name = "TC_ADM_13 text dài {0} ký tự -> còn {1} ký tự")
    @DisplayName("TC_ADM_13: AdminOpsParams.text trim và cắt ở 100 ký tự, thiếu tham số trả chuỗi rỗng")
    @CsvSource({"0, 0", "99, 99", "100, 100", "101, 100", "500, 100"})
    void textIsCutAt100(int length, int expected) {
        ServletFixture f = ServletFixture.get("/admin/orders").param("q", "  " + "q".repeat(length) + "  ");

        assertEquals(expected, AdminOpsParams.text(f.req, "q").length());
        assertEquals("", AdminOpsParams.text(f.req, "khong-co"));
    }

    @ParameterizedTest(name = "AdminOpsParams.page [{0}] -> {1}")
    @CsvSource(value = {"ABSENT, 1", "'', 1", "abc, 1", "0, 1", "-3, 1", "1, 1", "7, 7", "99999999999, 1"})
    void pageNeverBelowOne(String raw, int expected) {
        ServletFixture f = ServletFixture.get("/admin/orders");
        if (!"ABSENT".equals(raw)) {
            f.param("page", raw);
        }

        assertEquals(expected, AdminOpsParams.page(f.req));
    }

    @ParameterizedTest(name = "positiveInt [{0}] -> {1}")
    @CsvSource(value = {"5, 5", "' 5 ', 5", "0, ", "-1, ", "abc, ", "'', ", "2147483648, "})
    void positiveInt(String raw, Integer expected) {
        assertEquals(expected, AdminOpsParams.positiveInt(raw));
        assertNull(AdminOpsParams.positiveInt(null));
    }

    @ParameterizedTest(name = "enumValue [{0}] -> {1}")
    @CsvSource(value = {"admin, ADMIN", "' Customer ', CUSTOMER", "foo, NULL", "'', NULL"})
    void enumValue(String raw, String expected) {
        assertEquals("NULL".equals(expected) ? null : Role.valueOf(expected), AdminOpsParams.enumValue(Role.class, raw));
        assertNull(AdminOpsParams.enumValue(Role.class, null));
    }
}
