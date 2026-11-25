package com.polycoffee.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Dòng chi tiết hóa đơn để hiển thị (BR-23). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillLine {
    private String drinkName;
    private int quantity;
    private double unitPrice;
    private double discount;

    /** Thành tiền = số lượng × đơn giá × (1 − giảm giá). */
    public double getAmount() {
        return quantity * unitPrice * (1 - discount);
    }
}
