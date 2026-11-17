package com.fashionweb.model;

import java.util.Date;
import java.util.List;

public class Cart {

    // ----- Cột trong bảng CSDL -----
    private int id;
    private int userId;
    private Date updatedAt;

    // ----- Chỉ dùng để hiển thị trên giao diện, KHÔNG có cột trong CSDL -----
    private List<CartItem> items;
    private long subtotal;
    private int itemCount;

    public Cart() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
    }

    public long getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(long subtotal) {
        this.subtotal = subtotal;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }
}
