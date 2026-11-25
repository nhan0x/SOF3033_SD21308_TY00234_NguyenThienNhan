package com.polycoffee.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Nhân viên hoặc quản lý. Ánh xạ bảng {@code users}. {@code password} là SHA-256 hex. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private int id;
    private String fullname;
    private String email;
    private String password;
    private boolean manager;
    private boolean enabled;
}
