package com.fashionweb.dto;

import com.fashionweb.model.Product;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Số liệu cho trang Dashboard của Admin (không phải bảng trong CSDL). */
public class DashboardStats {

    /** Một cột trong biểu đồ doanh thu theo ngày. */
    public static class DailyRevenue {
        private final String label;
        private final long value;

        public DailyRevenue(String label, long value) {
            this.label = label;
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public long getValue() {
            return value;
        }
    }

    private int totalOrders;
    private long totalRevenue;
    private int totalCustomers;
    private int pendingOrders;
    private List<DailyRevenue> revenueByDay;
    private List<Product> topProducts;
    private Map<String, Integer> ordersByStatus = new LinkedHashMap<>();

    public int getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(int totalOrders) {
        this.totalOrders = totalOrders;
    }

    public long getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(long totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public int getTotalCustomers() {
        return totalCustomers;
    }

    public void setTotalCustomers(int totalCustomers) {
        this.totalCustomers = totalCustomers;
    }

    public int getPendingOrders() {
        return pendingOrders;
    }

    public void setPendingOrders(int pendingOrders) {
        this.pendingOrders = pendingOrders;
    }

    public List<DailyRevenue> getRevenueByDay() {
        return revenueByDay;
    }

    public void setRevenueByDay(List<DailyRevenue> revenueByDay) {
        this.revenueByDay = revenueByDay;
    }

    public List<Product> getTopProducts() {
        return topProducts;
    }

    public void setTopProducts(List<Product> topProducts) {
        this.topProducts = topProducts;
    }

    public Map<String, Integer> getOrdersByStatus() {
        return ordersByStatus;
    }

    public void setOrdersByStatus(Map<String, Integer> ordersByStatus) {
        this.ordersByStatus = ordersByStatus;
    }
}
