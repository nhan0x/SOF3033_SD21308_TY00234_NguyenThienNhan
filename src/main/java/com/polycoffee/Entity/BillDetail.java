package com.polycoffee.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Dòng hàng trong hóa đơn. Ánh xạ bảng {@code bill_details}. Giá và giảm giá là giá lúc bán. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillDetail {
    private long id;
    private long billId;
    private int drinkId;
    private double unitPrice;
    private double discount;
    private int quantity;
}
