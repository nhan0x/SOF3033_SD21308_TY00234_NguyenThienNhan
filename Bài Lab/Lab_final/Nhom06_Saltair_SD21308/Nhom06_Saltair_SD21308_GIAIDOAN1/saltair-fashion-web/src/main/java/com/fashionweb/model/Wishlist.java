package com.fashionweb.model;

import java.util.Date;

public class Wishlist {

    // ----- Cột trong bảng CSDL -----
    private int userId;
    private int productId;
    private Date createdAt;

    // ----- Chỉ dùng để hiển thị trên giao diện, KHÔNG có cột trong CSDL -----
    private Product product;

    public Wishlist() {
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }
}
