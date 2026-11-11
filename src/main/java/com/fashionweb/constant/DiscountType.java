package com.fashionweb.constant;

public enum DiscountType {
    PERCENT("Phần trăm"),
    AMOUNT("Số tiền");

    private final String label;

    DiscountType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
