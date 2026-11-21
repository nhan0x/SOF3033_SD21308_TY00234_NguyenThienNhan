package com.fashionweb.constant;

/** Luồng: PENDING -> SHIPPING -> COMPLETED, hoặc CANCELLED. */
public enum OrderStatus {
    PENDING("Chờ xác nhận", "pending"),
    SHIPPING("Đang giao", "shipping"),
    COMPLETED("Hoàn thành", "completed"),
    CANCELLED("Đã hủy", "cancelled");

    private final String label;
    private final String cssClass;

    OrderStatus(String label, String cssClass) {
        this.label = label;
        this.cssClass = cssClass;
    }

    public String getLabel() {
        return label;
    }

    /** Dùng làm hậu tố cho class CSS: badge--pending, badge--shipping... */
    public String getCssClass() {
        return cssClass;
    }
}
