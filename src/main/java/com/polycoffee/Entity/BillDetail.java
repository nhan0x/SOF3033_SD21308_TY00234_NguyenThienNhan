package com.polycoffee.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor 
@NoArgsConstructor 
@Data 

public class BillDetail {
    private Integer id;
    private Integer billId;
    private Integer drinkId;
    private int quantity;
    private int price;
}
