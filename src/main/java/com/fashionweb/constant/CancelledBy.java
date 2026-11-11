package com.fashionweb.constant;

public enum CancelledBy {
    CUSTOMER("Khách hàng"),
    ADMIN("Quản trị viên");

    private final String label;

    CancelledBy(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
