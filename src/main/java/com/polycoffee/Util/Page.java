package com.polycoffee.Util;

import java.util.List;

/** Một trang kết quả phân trang. */
public class Page<T> {

    /** Số dòng mỗi trang (BR-13, BR-21). */
    public static final int SIZE = 10;

    private final List<T> items;
    private final int page;
    private final int size;
    private final long total;

    public Page(List<T> items, int page, int size, long total) {
        this.items = items;
        this.page = page;
        this.size = size;
        this.total = total;
    }

    /** Đưa số trang yêu cầu về khoảng hợp lệ [1, tổng số trang]. */
    public static int clamp(int requestedPage, long total, int size) {
        int pages = (int) Math.max(1, (total + size - 1) / size);
        return Math.min(Math.max(requestedPage, 1), pages);
    }

    public static int offset(int page, int size) {
        return (page - 1) * size;
    }

    public List<T> getItems() {
        return items;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotal() {
        return total;
    }

    public int getTotalPages() {
        return (int) Math.max(1, (total + size - 1) / size);
    }
}
