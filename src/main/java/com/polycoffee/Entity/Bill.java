package com.polycoffee.Entity;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor 
@NoArgsConstructor 
@Data 
public class Bill {
    private Integer id;
    private Integer userId;
    private String code;
    private LocalDateTime createdAt;
    private int total;
    private String status;
}
