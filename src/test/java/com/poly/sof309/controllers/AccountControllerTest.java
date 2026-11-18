package com.poly.sof309.controllers;

import com.fashionweb.constant.AppConstant;
import com.fashionweb.controller.account.AddressServlet;
import com.fashionweb.controller.account.ProfileServlet;
import com.fashionweb.exception.ValidationException;
import com.fashionweb.model.Address;
import com.fashionweb.model.User;
import com.fashionweb.service.account.AccountService;
import com.fashionweb.service.auth.AuthService;
import com.poly.sof309.controllers.support.Harness;
import com.poly.sof309.controllers.support.ServletFixture;
import com.poly.sof309.controllers.support.Users;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.poly.sof309.controllers.support.ServletFixture.CTX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Controller hồ sơ và sổ địa chỉ: TC_PROF_01..03, TC_ADDR_01..05 ở tầng Controller. */
@Timeout(value = 5, unit = TimeUnit.SECONDS)
class AccountControllerTest {

    private static final String NOT_OWNER = "Địa chỉ không tồn tại hoặc không thuộc tài khoản của bạn.";

    private Harness<?> harness;

    @AfterEach
    void closeHarness() {
        if (harness != null) {
            harness.close();
        }
    }

    // =====================================================================
    // /addresses
    // =====================================================================

    private Harness<AddressServlet> addressHarness() {
        Harness<AddressServlet> h = Harness.mocked(AddressServlet::new, AccountService.class);
        harness = h;
        return h;
    }

    private static ServletFixture saveForm(int userId) {
        return ServletFixture.post("/addresses").param("action", "save")
                .param("receiverName", "Nguyen A").param("phone", "0912345678").param("addressLine", "12 Le Loi")
                .param("ward", "Ben Nghe").param("district", "Quan 1").param("city", "HCM")
                .loggedIn(Users.customer(userId));
    }

    @ParameterizedTest(name = "TC_ADDR_01 id=[{0}] tick mặc định={1} -> {2}")
    @DisplayName("TC_ADDR_01: thêm (không có id) hoặc sửa (có id); chỉ nhận mặc định khi form có tick")
    @CsvSource(value = {
            "ABSENT, false, 'Đã thêm địa chỉ mới.'",
            "'', true, 'Đã thêm địa chỉ mới.'",
            "5, false, 'Đã cập nhật địa chỉ.'",
            "abc, true, 'Đã thêm địa chỉ mới.'"
    })
    void saveAddress(String id, boolean makeDefault, String message) throws Exception {
        Harness<AddressServlet> h = addressHarness();
        ServletFixture f = saveForm(2);
        if (!"ABSENT".equals(id)) {
            f.param("id", id);
        }
        if (makeDefault) {
            f.param("defaultAddress", "on");
        }

        f.run(h.servlet);

        ArgumentCaptor<Address> saved = ArgumentCaptor.forClass(Address.class);
        verify(h.service(AccountService.class)).saveAddress(saved.capture());
        assertEquals(makeDefault, saved.getValue().isDefaultAddress());
        assertEquals(message, f.flashMessage());
        assertEquals("success", f.flashType());
        assertEquals(CTX + "/addresses", f.redirectedTo);
    }

    @Test
    @DisplayName("TC_ADDR_05: địa chỉ luôn gắn với người dùng trong session, bỏ qua userId do client gửi")
    void addressOwnerComesFromSession() throws Exception {
        Harness<AddressServlet> h = addressHarness();
        ServletFixture f = saveForm(3).param("userId", "2").param("id", "1");

        f.run(h.servlet);

        ArgumentCaptor<Address> saved = ArgumentCaptor.forClass(Address.class);
        verify(h.service(AccountService.class)).saveAddress(saved.capture());
        assertEquals(3, saved.getValue().getUserId());
        assertEquals(1, saved.getValue().getId());
    }

    @ParameterizedTest(name = "TC_ADDR_05 {0} địa chỉ của người khác")
    @DisplayName("TC_ADDR_05: Service từ chối địa chỉ không thuộc người dùng thì Controller hiện lỗi, không đổi gì")
    @CsvSource({"delete", "setDefault", "save"})
    void notOwnerShowsError(String action) throws Exception {
        Harness<AddressServlet> h = addressHarness();
        AccountService account = h.service(AccountService.class);
        ValidationException notOwner = new ValidationException(null, NOT_OWNER); // errors rỗng: lỗi chung
        doThrow(notOwner).when(account).deleteAddress(3, 1);
        doThrow(notOwner).when(account).setDefaultAddress(3, 1);
        doThrow(notOwner).when(account).saveAddress(any(Address.class));
        ServletFixture f = saveForm(3).param("action", action).param("id", "1");

        f.run(h.servlet);

        assertEquals(NOT_OWNER, f.flashMessage());
        assertEquals("error", f.flashType());
        assertEquals(CTX + "/addresses", f.redirectedTo);
    }

    @Test
    @DisplayName("TC_ADDR_04: lỗi nhập liệu thì hiện lại trang kèm lỗi từng ô (không redirect)")
    void fieldErrorsReopenForm() throws Exception {
        Harness<AddressServlet> h = addressHarness();
        Map<String, String> errors = new LinkedHashMap<>();
        errors.put("addressLine", "Địa chỉ (số nhà, đường) từ 5 đến 255 ký tự.");
        errors.put("city", "Vui lòng nhập tỉnh/thành phố (tối đa 100 ký tự).");
        doThrow(new ValidationException(errors)).when(h.service(AccountService.class)).saveAddress(any(Address.class));
        ServletFixture f = saveForm(2);

        f.run(h.servlet);

        assertEquals("/WEB-INF/views/account/addresses.jsp", f.lastForward());
        assertEquals(errors, f.errors());
        assertNull(f.redirectedTo);
    }

    @Test
    @DisplayName("TC_ADDR_02, 03: đặt mặc định và xóa chuyển đúng id")
    void setDefaultAndDelete() throws Exception {
        Harness<AddressServlet> h = addressHarness();
        ServletFixture setDefault = ServletFixture.post("/addresses").param("action", "setDefault").param("id", "2").loggedIn(Users.customer(2));
        ServletFixture delete = ServletFixture.post("/addresses").param("action", "delete").param("id", "2").loggedIn(Users.customer(2));

        setDefault.run(h.servlet);
        delete.run(h.servlet);

        verify(h.service(AccountService.class)).setDefaultAddress(2, 2);
        verify(h.service(AccountService.class)).deleteAddress(2, 2);
        assertEquals("Đã đặt làm địa chỉ mặc định.", setDefault.flashMessage());
        assertEquals("Đã xóa địa chỉ.", delete.flashMessage());
    }

    @ParameterizedTest(name = "action [{0}] trả 400")
    @DisplayName("POST /addresses với action thiếu hoặc lạ trả 400")
    @CsvSource(value = {"update", "ABSENT"})
    void addressUnknownAction(String action) throws Exception {
        Harness<AddressServlet> h = addressHarness();
        ServletFixture f = ServletFixture.post("/addresses").loggedIn(Users.customer(2));
        if (!"ABSENT".equals(action)) {
            f.param("action", action);
        }

        f.run(h.servlet);

        assertEquals(400, f.errorStatus);
        verifyNoInteractions(h.service(AccountService.class));
    }

    // =====================================================================
    // /profile
    // =====================================================================

    private Harness<ProfileServlet> profileHarness() {
        Harness<ProfileServlet> h = Harness.mocked(ProfileServlet::new, AccountService.class, AuthService.class);
        harness = h;
        return h;
    }

    @Test
    @DisplayName("TC_PROF_01: cập nhật hồ sơ thì làm mới User trong session (tên mới hiện ở header)")
    void updateProfileRefreshesSession() throws Exception {
        Harness<ProfileServlet> h = profileHarness();
        User refreshed = Users.customer(2);
        refreshed.setFullName("Nguyen Van B");
        when(h.service(AccountService.class).findUser(2)).thenReturn(refreshed);
        ServletFixture f = ServletFixture.post("/profile").param("action", "updateProfile")
                .param("fullName", "Nguyen Van B").param("phone", "0912345678").param("email", "hacker@x.vn")
                .loggedIn(Users.customer(2));

        f.run(h.servlet);

        verify(h.service(AccountService.class)).updateProfile(2, "Nguyen Van B", "0912345678");
        assertSame(refreshed, f.sessionAttrValue(AppConstant.SESSION_USER));
        assertEquals("Đã lưu thông tin tài khoản.", f.flashMessage());
        assertEquals(CTX + "/profile", f.redirectedTo);
    }

    @Test
    @DisplayName("TC_PROF_02: lỗi nhập liệu thì hiện lại trang kèm lỗi, không đổi session và không redirect")
    void profileErrorsReopenPage() throws Exception {
        Harness<ProfileServlet> h = profileHarness();
        Map<String, String> errors = new LinkedHashMap<>();
        errors.put("fullName", "Họ và tên từ 2 đến 100 ký tự.");
        errors.put("phone", "Số điện thoại gồm 10 chữ số, bắt đầu bằng 0.");
        doThrow(new ValidationException(errors)).when(h.service(AccountService.class)).updateProfile(2, "a", "123");
        User before = Users.customer(2);
        ServletFixture f = ServletFixture.post("/profile").param("action", "updateProfile")
                .param("fullName", "a").param("phone", "123").loggedIn(before);

        f.run(h.servlet);

        assertEquals("/WEB-INF/views/account/profile.jsp", f.lastForward());
        assertEquals(errors, f.errors());
        assertSame(before, f.sessionAttrValue(AppConstant.SESSION_USER));
        assertNull(f.redirectedTo);
    }

    @Test
    @DisplayName("TC_PROF_03: đổi mật khẩu đúng thì flash; sai thì hiện lỗi ở từng ô")
    void changePassword() throws Exception {
        Harness<ProfileServlet> h = profileHarness();
        AuthService auth = h.service(AuthService.class);
        Map<String, String> errors = new LinkedHashMap<>();
        errors.put("currentPassword", "Mật khẩu hiện tại không đúng.");
        doThrow(new ValidationException(errors)).when(auth).changePassword(2, "sai", "Khach@456", "Khach@456");
        ServletFixture ok = ServletFixture.post("/profile").param("action", "changePassword").param("currentPassword", "Khach@123")
                .param("newPassword", "Khach@456").param("confirm", "Khach@456").loggedIn(Users.customer(2));
        ServletFixture wrong = ServletFixture.post("/profile").param("action", "changePassword").param("currentPassword", "sai")
                .param("newPassword", "Khach@456").param("confirm", "Khach@456").loggedIn(Users.customer(2));

        ok.run(h.servlet);
        wrong.run(h.servlet);

        verify(auth).changePassword(2, "Khach@123", "Khach@456", "Khach@456");
        assertEquals("Đã đổi mật khẩu.", ok.flashMessage());
        assertEquals(CTX + "/profile", ok.redirectedTo);
        assertEquals(errors, wrong.errors());
        assertNull(wrong.flashMessage());
        assertFalse(wrong.forwards.isEmpty());
        assertTrue(wrong.lastForward().endsWith("profile.jsp"));
    }

    @ParameterizedTest(name = "action [{0}] trả 400")
    @DisplayName("POST /profile với action thiếu hoặc lạ trả 400")
    @CsvSource(value = {"deleteAccount", "ABSENT"})
    void profileUnknownAction(String action) throws Exception {
        Harness<ProfileServlet> h = profileHarness();
        ServletFixture f = ServletFixture.post("/profile").loggedIn(Users.customer(2));
        if (!"ABSENT".equals(action)) {
            f.param("action", action);
        }

        f.run(h.servlet);

        assertEquals(400, f.errorStatus);
    }
}
