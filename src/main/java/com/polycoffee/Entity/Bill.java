package com.polycoffee.Entity;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Hóa đơn. Ánh xạ bảng {@code bills}.
 * {@code status}: 0 đang xử lý, 1 đã thanh toán, 2 đã hủy. Tổng tiền không lưu ở đây (BR-22).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Bill {
    public static final int PENDING = 0;
    public static final int PAID = 1;
    public static final int CANCELLED = 2;

    private long id;
    private int userId;
    private Date createdAt;
    private Date checkoutAt;
    private int status;
}
