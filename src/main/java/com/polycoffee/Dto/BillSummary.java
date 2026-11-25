package com.polycoffee.Dto;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Một dòng trong danh sách hóa đơn, kèm tên nhân viên và tổng tiền tính từ chi tiết (BR-22). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillSummary {
    private long id;
    private String userFullname;
    private Date createdAt;
    private Date checkoutAt;
    private int status;
    private double total;

    public String getStatusText() {
        switch (status) {
            case 0:
                return "Đang xử lý";
            case 1:
                return "Đã thanh toán";
            case 2:
                return "Đã hủy";
            default:
                return "Không xác định";
        }
    }
}
