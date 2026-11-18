package com.poly.sof309.controllers.support;

import com.fashionweb.constant.Role;
import com.fashionweb.model.User;

/** Người dùng mẫu cho test (không đụng database). */
public final class Users {

    private Users() {
    }

    public static User customer(int id) {
        return user(id, "khach" + id + "@saltair.vn", Role.CUSTOMER);
    }

    public static User admin(int id) {
        return user(id, "admin" + id + "@saltair.vn", Role.ADMIN);
    }

    private static User user(int id, String email, Role role) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setFullName("Nguoi dung " + id);
        u.setRole(role);
        u.setActive(true);
        return u;
    }
}
