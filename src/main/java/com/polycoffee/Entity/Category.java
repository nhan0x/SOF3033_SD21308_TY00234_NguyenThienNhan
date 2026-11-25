package com.polycoffee.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Loại đồ uống. Ánh xạ bảng {@code categories}. {@code id == 0} nghĩa là chưa lưu. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    private int id;
    private String name;
}
