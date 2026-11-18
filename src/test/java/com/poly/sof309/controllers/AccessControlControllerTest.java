package com.poly.sof309.controllers;

import com.fashionweb.constant.AppConstant;
import com.fashionweb.filter.AdminFilter;
import com.fashionweb.filter.AuthFilter;
import com.poly.sof309.controllers.support.ServletFixture;
import com.poly.sof309.controllers.support.Users;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.concurrent.TimeUnit;

import static com.poly.sof309.controllers.support.ServletFixture.CTX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** Cổng phân quyền của tầng Controller: AuthFilter và AdminFilter (TC_AUTH_14, TC_AUTH_15). */
@Timeout(value = 5, unit = TimeUnit.SECONDS)
class AccessControlControllerTest {

    private final FilterChain chain = mock(FilterChain.class);

    // ---------- AuthFilter: trang của khách hàng ----------

    @ParameterizedTest(name = "TC_AUTH_14 chưa đăng nhập mở {0}")
    @DisplayName("TC_AUTH_14: chưa đăng nhập thì chuyển sang /login?redirect=<trang đang mở>")
    @CsvSource({"/orders", "/order", "/profile", "/addresses", "/wishlist"})
    void anonymousIsSentToLogin(String path) throws Exception {
        ServletFixture f = ServletFixture.get(path).noSession();

        f.run(new AuthFilter(), chain);

        assertEquals(CTX + "/login?redirect=" + java.net.URLEncoder.encode(path, java.nio.charset.StandardCharsets.UTF_8),
                f.redirectedTo);
        verify(chain, never()).doFilter(f.req, f.resp);
    }

    @Test
    @DisplayName("TC_AUTH_14: giữ nguyên query string khi quay lại sau đăng nhập")
    void queryStringIsKept() throws Exception {
        ServletFixture f = ServletFixture.get("/orders").queryString("status=CANCELLED&page=2").noSession();

        f.run(new AuthFilter(), chain);

        assertEquals(CTX + "/login?redirect=%2Forders%3Fstatus%3DCANCELLED%26page%3D2", f.redirectedTo);
    }

    @Test
    @DisplayName("Session có nhưng chưa có người dùng cũng bị chuyển sang /login")
    void sessionWithoutUser() throws Exception {
        ServletFixture f = ServletFixture.get("/profile");

        f.run(new AuthFilter(), chain);

        assertEquals(CTX + "/login?redirect=%2Fprofile", f.redirectedTo);
    }

    @Test
    @DisplayName("Đã đăng nhập thì cho đi tiếp")
    void loggedInPasses() throws Exception {
        ServletFixture f = ServletFixture.get("/orders").loggedIn(Users.customer(2));

        f.run(new AuthFilter(), chain);

        verify(chain).doFilter(f.req, f.resp);
        assertNull(f.redirectedTo);
    }

    @ParameterizedTest(name = "POST với Referer [{0}] quay về [{1}]")
    @DisplayName("POST bị chặn thì quay về trang trước (Referer cùng máy chủ), Referer lạ bị bỏ qua")
    @CsvSource(delimiter = '|', value = {
            "http://localhost:8080/saltair/product?slug=ao-polo | /product?slug=ao-polo",
            "http://evil.com/saltair/product?slug=ao-polo | /wishlist",
            "http://localhost:8080/other/page | /wishlist",
            "ht tp://%%bad | /wishlist"
    })
    void postUsesRefererOnlyWhenSameHost(String referer, String expectedReturn) throws Exception {
        ServletFixture f = ServletFixture.post("/wishlist").noSession().header("Referer", referer);

        f.run(new AuthFilter(), chain);

        assertEquals(CTX + "/login?redirect=" + java.net.URLEncoder.encode(expectedReturn, java.nio.charset.StandardCharsets.UTF_8),
                f.redirectedTo);
    }

    // ---------- AdminFilter: khu vực quản trị ----------

    @Test
    @DisplayName("TC_AUTH_15: chưa đăng nhập vào /admin/orders thì chuyển sang /login, nhớ trang cần quay lại")
    void anonymousAdminIsSentToLogin() throws Exception {
        ServletFixture f = ServletFixture.get("/admin/orders").noSession();

        f.run(new AdminFilter(), chain);

        assertEquals(CTX + "/login?redirect=%2Fadmin%2Forders", f.redirectedTo);
        verify(chain, never()).doFilter(f.req, f.resp);
    }

    @ParameterizedTest(name = "TC_AUTH_15 Customer mở {0} bị 403")
    @DisplayName("TC_AUTH_15: Customer vào khu vực Admin bị 403, không chuyển hướng")
    @CsvSource({"/admin", "/admin/users", "/admin/orders", "/admin/products/form"})
    void customerGets403(String path) throws Exception {
        ServletFixture f = ServletFixture.post(path).loggedIn(Users.customer(2));

        f.run(new AdminFilter(), chain);

        assertEquals(403, f.errorStatus);
        assertNull(f.redirectedTo);
        verify(chain, never()).doFilter(f.req, f.resp);
    }

    @Test
    @DisplayName("TC_AUTH_15: Admin được đi tiếp")
    void adminPasses() throws Exception {
        ServletFixture f = ServletFixture.get("/admin/users").loggedIn(Users.admin(1));

        f.run(new AdminFilter(), chain);

        verify(chain).doFilter(f.req, f.resp);
        assertNull(f.errorStatus);
    }

    @ParameterizedTest(name = "AdminFilter với đường dẫn {0}: được cho qua = {1}")
    @DisplayName("Biên đường dẫn /admin/login: chỉ /admin/login và /admin/login/... được miễn kiểm tra")
    @CsvSource({
            "/admin/login, true",
            "/admin/login/x, true",
            "/admin/loginx, false",
            "/admin/logins/a, false",
            "/admin, false"
    })
    void loginPathBoundary(String path, boolean passes) throws Exception {
        ServletFixture f = ServletFixture.get(path).noSession();

        f.run(new AdminFilter(), chain);

        if (passes) {
            verify(chain).doFilter(f.req, f.resp);
        } else {
            verify(chain, never()).doFilter(f.req, f.resp);
            assertEquals(CTX + "/login?redirect=" + java.net.URLEncoder.encode(path, java.nio.charset.StandardCharsets.UTF_8),
                    f.redirectedTo);
        }
    }

    @Test
    @DisplayName("Giá trị lạ trong session không phải User thì coi như chưa đăng nhập")
    void nonUserSessionValueIsAnonymous() throws Exception {
        ServletFixture f = ServletFixture.get("/admin").sessionAttr(AppConstant.SESSION_USER, "admin");

        f.run(new AdminFilter(), chain);

        assertEquals(CTX + "/login?redirect=%2Fadmin", f.redirectedTo);
    }
}
