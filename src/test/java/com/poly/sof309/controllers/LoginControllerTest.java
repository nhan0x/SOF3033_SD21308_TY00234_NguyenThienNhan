package com.poly.sof309.controllers;

import com.fashionweb.controller.auth.LoginServlet;
import com.fashionweb.exception.DataAccessException;
import com.fashionweb.exception.ValidationException;
import com.fashionweb.service.auth.AuthService;
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

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.poly.sof309.controllers.support.ServletFixture.CTX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Controller đăng nhập: TC_AUTH_03, 04, 05, 08 (xem docs/TEST_CASES.md). Không cần database:
 * các đường kiểm tra dữ liệu chạy trước khi chạm DB, đường sai thông tin dùng AuthService giả.
 */
@Timeout(value = 5, unit = TimeUnit.SECONDS)
class LoginControllerTest {

    private static final String LOGIN_VIEW = "/WEB-INF/views/auth/login.jsp";

    private Harness<LoginServlet> harness;

    @AfterEach
    void closeHarness() {
        if (harness != null) {
            harness.close();
        }
    }

    // ---------- TC_AUTH_04: kiểm tra email (BVA + EP) ----------

    @ParameterizedTest(name = "TC_AUTH_04 email [{0}] bị từ chối: {1}")
    @DisplayName("TC_AUTH_04: email rỗng hoặc sai định dạng thì hiện lại form kèm lỗi, không đăng nhập")
    @CsvSource({
            "'', 'Vui lòng nhập email.'",
            "'   ', 'Vui lòng nhập email.'",
            "abc, 'Email không đúng định dạng.'",
            "a@b, 'Email không đúng định dạng.'",
            "@x.vn, 'Email không đúng định dạng.'",
            "'a b@x.vn', 'Email không đúng định dạng.'",
            "a@@x.vn, 'Email không đúng định dạng.'"
    })
    void invalidEmailIsRejected(String email, String message) throws Exception {
        ServletFixture f = ServletFixture.post("/login").param("email", email).param("password", "Khach@123");

        f.run(new LoginServlet());

        assertEquals(LOGIN_VIEW, f.lastForward());
        assertEquals(message, f.errors().get("email"));
        assertNull(f.redirectedTo, "Không được chuyển hướng khi email sai");
    }

    @Test
    @DisplayName("TC_AUTH_04: thiếu hẳn tham số email giống email rỗng")
    void missingEmailParameter() throws Exception {
        ServletFixture f = ServletFixture.post("/login").param("password", "Khach@123");

        f.run(new LoginServlet());

        assertEquals("Vui lòng nhập email.", f.errors().get("email"));
    }

    @ParameterizedTest(name = "TC_AUTH_04 biên 150/151: phần local dài {0} ký tự")
    @DisplayName("TC_AUTH_04: biên độ dài email 150 hợp lệ, 151 bị từ chối")
    @CsvSource({"145, false", "146, true"})
    void emailLengthBoundary(int localLength, boolean tooLong) throws Exception {
        String email = "a".repeat(localLength) + "@x.vn"; // 150 hoặc 151 ký tự
        ServletFixture f = ServletFixture.post("/login").param("email", email).param("password", "");

        f.run(new LoginServlet());

        if (tooLong) {
            assertEquals("Email không đúng định dạng.", f.errors().get("email"));
        } else {
            assertNull(f.errors().get("email"), "150 ký tự còn hợp lệ, lỗi phải chuyển sang ô mật khẩu");
            assertEquals("Vui lòng nhập mật khẩu.", f.errors().get("password"));
        }
    }

    // ---------- TC_AUTH_05: mật khẩu rỗng ----------

    @ParameterizedTest(name = "TC_AUTH_05 mật khẩu [{0}]")
    @DisplayName("TC_AUTH_05: để trống mật khẩu thì báo lỗi ở ô mật khẩu")
    @CsvSource({"''", "ABSENT"})
    void emptyPasswordIsRejected(String password) throws Exception {
        ServletFixture f = ServletFixture.post("/login").param("email", " Khach@Saltair.vn ");
        if (!"ABSENT".equals(password)) {
            f.param("password", password);
        }

        f.run(new LoginServlet());

        assertEquals(LOGIN_VIEW, f.lastForward());
        assertEquals("Vui lòng nhập mật khẩu.", f.errors().get("password"));
        assertNull(f.errors().get("email"), "Email có khoảng trắng và chữ hoa vẫn hợp lệ sau khi chuẩn hóa");
    }

    // ---------- TC_AUTH_03: sai thông tin dùng cùng một câu ----------

    @Test
    @DisplayName("TC_AUTH_03: sai mật khẩu và email không tồn tại cùng hiện một thông báo")
    void wrongCredentialsShowSameMessage() throws Exception {
        harness = Harness.mocked(LoginServlet::new, AuthService.class);
        AuthService auth = harness.service(AuthService.class);
        ValidationException wrong = new ValidationException("password", "Email hoặc mật khẩu không đúng.");
        when(auth.login(anyString(), anyString())).thenThrow(wrong);

        ServletFixture wrongPassword = ServletFixture.post("/login")
                .param("email", "khach@saltair.vn").param("password", "Sai@12345");
        ServletFixture unknownEmail = ServletFixture.post("/login")
                .param("email", "khong.co@x.vn").param("password", "Khach@123");
        wrongPassword.run(harness.servlet);
        unknownEmail.run(harness.servlet);

        assertEquals(LOGIN_VIEW, wrongPassword.lastForward());
        assertEquals("Email hoặc mật khẩu không đúng.", wrongPassword.errors().get("password"));
        assertEquals(wrongPassword.errors(), unknownEmail.errors(), "Không được lộ email nào có tài khoản");
    }

    @Test
    @DisplayName("Lỗi database lan ra ngoài Servlet (container hiện trang 500), không bị nuốt")
    void databaseErrorPropagates() {
        harness = Harness.mocked(LoginServlet::new, AuthService.class);
        when(harness.service(AuthService.class).login(anyString(), anyString()))
                .thenThrow(new DataAccessException("mất kết nối", new java.sql.SQLException("down")));

        ServletFixture f = ServletFixture.post("/login").param("email", "khach@saltair.vn").param("password", "x");

        assertThrows(DataAccessException.class, () -> f.run(harness.servlet));
        assertNull(f.redirectedTo);
    }

    // ---------- GET /login và /admin/login ----------

    @Test
    @DisplayName("GET /login khi chưa đăng nhập hiện form")
    void getLoginShowsForm() throws Exception {
        ServletFixture f = ServletFixture.get("/login");

        f.run(new LoginServlet());

        assertEquals(LOGIN_VIEW, f.lastForward());
    }

    @ParameterizedTest(name = "đã đăng nhập vai trò {0} mở /login thì về {1}")
    @DisplayName("GET /login khi đã đăng nhập chuyển về trang phù hợp vai trò")
    @CsvSource({"CUSTOMER, /home", "ADMIN, /admin"})
    void getLoginWhenLoggedInRedirects(String role, String target) throws Exception {
        ServletFixture f = ServletFixture.get("/login")
                .loggedIn("ADMIN".equals(role) ? Users.admin(1) : Users.customer(2));

        f.run(new LoginServlet());

        assertEquals(CTX + target, f.redirectedTo);
    }

    @ParameterizedTest(name = "/admin/login?redirect={0} chuyển về {1}")
    @DisplayName("Đường dẫn cũ /admin/login chuyển về /login và giữ tham số redirect (đã mã hóa)")
    @CsvSource(value = {
            "/admin/orders, /login?redirect=%2Fadmin%2Forders",
            "ABSENT, /login",
            "'  ', /login"
    })
    void legacyAdminLoginRedirects(String redirect, String expected) throws Exception {
        ServletFixture f = ServletFixture.get("/admin/login");
        if (!"ABSENT".equals(redirect)) {
            f.param("redirect", redirect);
        }

        f.run(new LoginServlet());

        assertEquals(CTX + expected, f.redirectedTo);
    }

    // ---------- TC_AUTH_08: chặn open redirect, chèn header ----------

    static Stream<Arguments> redirectCases() {
        return Stream.of(
                Arguments.of("/wishlist", "/wishlist"),
                Arguments.of("/products?category=quan&page=2", "/products?category=quan&page=2"),
                Arguments.of("  /home  ", "/home"),
                Arguments.of(null, null),
                Arguments.of("", null),
                Arguments.of("   ", null),
                Arguments.of("//evil.com", null),
                Arguments.of("/\\evil.com", null),
                Arguments.of("http://evil.com", null),
                Arguments.of("https://evil.com/saltair/home", null),
                Arguments.of("javascript:alert(1)", null),
                Arguments.of("home", null),
                Arguments.of("/login", null),
                Arguments.of("/login?redirect=/x", null),
                Arguments.of("/logout", null),
                Arguments.of("/register", null),
                Arguments.of("/admin/login", null),
                Arguments.of("/home\r\nSet-Cookie: x=1", null),
                Arguments.of("/home\nLocation: http://evil.com", null)
        );
    }

    @ParameterizedTest(name = "TC_AUTH_08 redirect [{0}] -> [{1}]")
    @MethodSource("redirectCases")
    @DisplayName("TC_AUTH_08: chỉ nhận đường dẫn nội bộ, chặn open redirect và chèn header")
    void safeRedirectOnlyAllowsInternalPaths(String input, String expected) throws Exception {
        // LoginSupport là lớp nội bộ của package controller.auth, nên gọi qua reflection
        Method m = Class.forName("com.fashionweb.controller.auth.LoginSupport")
                .getDeclaredMethod("safeRedirect", String.class);
        m.setAccessible(true);

        assertEquals(expected, m.invoke(null, input));
    }
}
