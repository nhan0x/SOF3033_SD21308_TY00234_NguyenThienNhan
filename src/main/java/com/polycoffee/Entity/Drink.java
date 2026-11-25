package com.polycoffee.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Đồ uống. Ánh xạ bảng {@code drinks}. {@code discount} nằm trong khoảng 0 đến 1. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Drink {
    private int id;
    private String name;
    private String image;
    private double unitPrice;
    private double discount;
    private boolean available;
    private int categoryId;

    /** Giá sau giảm giá. */
    public double getFinalPrice() {
        return unitPrice * (1 - discount);
    }
}
