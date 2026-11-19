package com.polycoffee.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data

public class Drink {
    private Integer id;
    private Integer categoryId;
    private String name;
    private String description;
    private String image;
    private int price;
    private boolean active;
}
