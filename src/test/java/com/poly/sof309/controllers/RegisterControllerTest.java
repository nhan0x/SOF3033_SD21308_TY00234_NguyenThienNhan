package com.poly.sof309.controllers;

import com.fashionweb.controller.auth.LogoutServlet;
import com.fashionweb.controller.auth.RegisterServlet;
import com.poly.sof309.controllers.support.ServletFixture;
import com.poly.sof309.controllers.support.Users;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.concurrent.TimeUnit;

import static com.poly.sof309.controllers.support.ServletFixture.CTX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Controller đăng ký và đăng xuất: TC_AUTH_10, 11 và phần đăng xuất của TC_AUTH_16.
 * Email luôn để sai định dạng để Service dừng trước khi chạm database; các lỗi ở ô khác vẫn được gom đủ.
 */
@Timeout(value = 5, unit = TimeUnit.SECONDS)
class RegisterControllerTest {

    private static final String REGISTER_VIEW = "/WEB-INF/views/auth/register.jsp";

    private ServletFixture form(String password, String confirm, String fullName) {
        return ServletFixture.post("/register")
                .param("email", "khong-hop-le")
                .param("fullName", fullName)
                .param("password", password)
                .param("confirm", confirm);
    }

    @ParameterizedTest(name = "TC_AUTH_10 mật khẩu {0} ký tự -> lỗi [{1}]")
    @DisplayName("TC_AUTH_10: biên độ dài mật khẩu 7/8/64/65")
    @CsvSource(value = {
            "0,   'Mật khẩu tối thiểu 8 ký tự.'",
            "7,   'Mật khẩu tối thiểu 8 ký tự.'",
            "8,   ",
            "64,  ",
            "65,  'Mật khẩu tối đa 64 ký tự.'"
    })
    void passwordLengthBoundary(int length, String expectedError) throws Exception {
        String password = "a".repeat(length);
        ServletFixture f = form(password, password, "Nguyen Van A");

        f.run(new RegisterServlet());

        assertEquals(REGISTER_VIEW, f.lastForward());
        assertEquals(expectedError, f.errors().get("password"));
        assertEquals("Email không đúng định dạng.", f.errors().get("email"), "Lỗi email luôn có mặt trong test này");
    }

    @Test
    @DisplayName("TC_AUTH_11: nhập lại mật khẩu lệch thì báo ở ô confirm, không báo ở ô password")
    void confirmMismatch() throws Exception {
        ServletFixture f = form("Abcd1234", "Abcd12345", "Nguyen Van A");

        f.run(new RegisterServlet());

        assertEquals("Mật khẩu nhập lại không khớp.", f.errors().get("confirm"));
        assertNull(f.errors().get("password"));
    }

    @Test
    @DisplayName("TC_AUTH_11: mật khẩu quá ngắn thì không kiểm tra tiếp ô nhập lại")
    void confirmNotCheckedWhenPasswordTooShort() throws Exception {
        ServletFixture f = form("abc", "khac", "Nguyen Van A");

        f.run(new RegisterServlet());

        assertEquals("Mật khẩu tối thiểu 8 ký tự.", f.errors().get("password"));
        assertNull(f.errors().get("confirm"));
    }

    @ParameterizedTest(name = "TC_AUTH_11 họ tên {0} ký tự -> lỗi [{1}]")
    @DisplayName("TC_AUTH_11: biên họ tên 0/1/100/101 (đăng ký không có tối thiểu 2, khác hồ sơ)")
    @CsvSource(value = {
            "0,   'Vui lòng nhập họ tên.'",
            "1,   ",
            "100, ",
            "101, 'Họ tên tối đa 100 ký tự.'"
    })
    void fullNameBoundary(int length, String expectedError) throws Exception {
        ServletFixture f = form("Abcd1234", "Abcd1234", "n".repeat(length));

        f.run(new RegisterServlet());

        assertEquals(expectedError, f.errors().get("fullName"));
    }

    @Test
    @DisplayName("Họ tên chỉ có khoảng trắng bị coi là rỗng")
    void blankFullNameIsEmpty() throws Exception {
        ServletFixture f = form("Abcd1234", "Abcd1234", "     ");

        f.run(new RegisterServlet());

        assertEquals("Vui lòng nhập họ tên.", f.errors().get("fullName"));
    }

    @Test
    @DisplayName("GET /register khi chưa đăng nhập hiện form; khi đã đăng nhập về /home")
    void getRegister() throws Exception {
        ServletFixture anonymous = ServletFixture.get("/register");
        anonymous.run(new RegisterServlet());
        assertEquals(REGISTER_VIEW, anonymous.lastForward());

        ServletFixture loggedIn = ServletFixture.get("/register").loggedIn(Users.customer(2));
        loggedIn.run(new RegisterServlet());
        assertEquals(CTX + "/home", loggedIn.redirectedTo);
    }

    // ---------- đăng xuất ----------

    @Test
    @DisplayName("TC_AUTH_16: đăng xuất hủy phiên rồi về trang chủ")
    void logoutInvalidatesSession() throws Exception {
        ServletFixture f = ServletFixture.get("/logout").loggedIn(Users.customer(2));

        f.run(new LogoutServlet());

        assertTrue(f.sessionInvalidated);
        assertEquals(CTX + "/home", f.redirectedTo);
    }

    @Test
    @DisplayName("Đăng xuất khi chưa có phiên vẫn không lỗi")
    void logoutWithoutSession() throws Exception {
        ServletFixture f = ServletFixture.get("/logout").noSession();

        f.run(new LogoutServlet());

        assertFalse(f.sessionInvalidated);
        assertEquals(CTX + "/home", f.redirectedTo);
    }
}
