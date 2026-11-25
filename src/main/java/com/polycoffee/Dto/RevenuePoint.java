package com.polycoffee.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Doanh thu của một ngày. {@code day} có dạng yyyy-MM-dd. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevenuePoint {
    private String day;
    private double revenue;
}
