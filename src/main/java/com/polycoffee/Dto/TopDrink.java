package com.polycoffee.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Đồ uống bán chạy: tổng số lượng và doanh thu trong khoảng thời gian. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopDrink {
    private String name;
    private long quantity;
    private double revenue;
}
