package com.polycoffee.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor 
@NoArgsConstructor 
@Data 


public class Category {
    private Integer id;
    private String name;
    private boolean active;
};
