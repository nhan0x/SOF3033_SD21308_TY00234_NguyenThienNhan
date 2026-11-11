package com.fashionweb.service.admin;

import com.fashionweb.constant.OrderStatus;
import com.fashionweb.repository.admin.StatisticRepository;
import com.fashionweb.dto.DashboardStats;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Số liệu cho dashboard của Admin.
 *
 * <p>Luồng gọi: Servlet -> Service -> Repository -> MySQL.
 */
public class StatisticService {

    private static final int TOP_PRODUCTS = 5;

    private final StatisticRepository statisticRepository = new StatisticRepository();

    /**
     * Trong khoảng [from, to]: tổng đơn, doanh thu (chỉ đơn COMPLETED), doanh thu theo ngày, top sản phẩm bán chạy
     * (đơn không bị hủy), số đơn theo trạng thái (khóa là nhãn tiếng Việt). Số khách và đơn đang chờ xác nhận tính trên toàn bộ.
     */
    public DashboardStats getStats(LocalDate from, LocalDate to) {
        DashboardStats s = new DashboardStats();
        s.setTotalOrders(statisticRepository.countOrders(from, to, null));
        s.setTotalRevenue(statisticRepository.sumRevenue(from, to));
        s.setTotalCustomers(statisticRepository.countCustomers());
        s.setPendingOrders(statisticRepository.countPendingNow());
        s.setRevenueByDay(statisticRepository.revenueByDay(from, to));
        s.setTopProducts(statisticRepository.topProducts(from, to, TOP_PRODUCTS));
        Map<String, Integer> raw = statisticRepository.ordersByStatus(from, to);
        Map<String, Integer> byStatus = new LinkedHashMap<>();
        for (OrderStatus st : OrderStatus.values()) {
            byStatus.put(st.getLabel(), raw.getOrDefault(st.name(), 0));
        }
        s.setOrdersByStatus(byStatus);
        return s;
    }
}
