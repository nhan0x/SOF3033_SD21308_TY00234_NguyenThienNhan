package com.polycoffee.DAO;

import com.polycoffee.Dto.BillSummary;
import com.polycoffee.Dto.RevenuePoint;
import com.polycoffee.Dto.TopDrink;
import com.polycoffee.Entity.Bill;
import com.polycoffee.Util.JdbcUtil;
import com.polycoffee.Util.JdbcUtil.RowMapper;
import com.polycoffee.Util.Page;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

/**
 * Truy cập bảng {@code bills} và các truy vấn tổng hợp (danh sách có tổng tiền, thống kê).
 * Tổng tiền không lưu trong bảng, luôn tính từ chi tiết (BR-22).
 */
public class BillDAO {

    private static final RowMapper<Bill> MAPPER = rs -> new Bill(
            rs.getLong("id"), rs.getInt("user_id"), toDate(rs.getTimestamp("created_at")),
            toDate(rs.getTimestamp("checkout_at")), rs.getInt("status"));

    private static final RowMapper<BillSummary> SUMMARY_MAPPER = rs -> new BillSummary(
            rs.getLong("id"), rs.getString("fullname"), toDate(rs.getTimestamp("created_at")),
            toDate(rs.getTimestamp("checkout_at")), rs.getInt("status"), rs.getDouble("total"));

    private static Date toDate(Timestamp t) {
        return t == null ? null : new Date(t.getTime());
    }

    public List<Bill> findAll() {
        return JdbcUtil.query("SELECT * FROM bills ORDER BY created_at DESC, id DESC", MAPPER);
    }

    public Bill findById(long id) {
        return JdbcUtil.queryOne("SELECT * FROM bills WHERE id = ?", MAPPER, id);
    }

    /** Tạo hóa đơn mới với thời điểm tạo là hiện tại, trả về id vừa sinh. */
    public long create(Bill b) {
        return JdbcUtil.insert("INSERT INTO bills(user_id, created_at, status) VALUES (?, NOW(), ?)",
                b.getUserId(), b.getStatus());
    }

    public int update(Bill b) {
        return JdbcUtil.update("UPDATE bills SET user_id=?, checkout_at=?, status=? WHERE id=?",
                b.getUserId(),
                b.getCheckoutAt() == null ? null : new Timestamp(b.getCheckoutAt().getTime()),
                b.getStatus(), b.getId());
    }

    /** Xóa hóa đơn, các dòng chi tiết bị xóa theo (ON DELETE CASCADE). */
    public int deleteById(long id) {
        return JdbcUtil.update("DELETE FROM bills WHERE id = ?", id);
    }

    public long count() {
        return JdbcUtil.count("SELECT COUNT(*) FROM bills");
    }

    /**
     * Danh sách hóa đơn mới nhất trước, có tên nhân viên và tổng tiền (BR-21).
     * Số trang vượt quá được đưa về trang cuối.
     */
    public Page<BillSummary> findSummaries(int page, int size) {
        long total = count();
        int current = Page.clamp(page, total, size);
        List<BillSummary> items = JdbcUtil.query(
                """
                    SELECT b.id, u.fullname, b.created_at, b.checkout_at, b.status,
                    IFNULL(SUM(bd.quantity * bd.unit_price * (1 - bd.discount)), 0) AS total
                    FROM bills b JOIN users u ON u.id = b.user_id
                    LEFT JOIN bill_details bd ON bd.bill_id = b.id
                    GROUP BY b.id, u.fullname, b.created_at, b.checkout_at, b.status
                    ORDER BY b.created_at DESC, b.id DESC LIMIT ? OFFSET ?
                    """,
                SUMMARY_MAPPER, size, Page.offset(current, size));
        return new Page<>(items, current, size, total);
    }

    /** Một hóa đơn kèm tên nhân viên và tổng tiền (BR-23). Trả null nếu không tồn tại. */
    public BillSummary findSummaryById(long id) {
        return JdbcUtil.queryOne(
                """
                    SELECT b.id, u.fullname, b.created_at, b.checkout_at, b.status,
                    IFNULL(SUM(bd.quantity * bd.unit_price * (1 - bd.discount)), 0) AS total
                    FROM bills b JOIN users u ON u.id = b.user_id
                    LEFT JOIN bill_details bd ON bd.bill_id = b.id
                    WHERE b.id = ?
                    GROUP BY b.id, u.fullname, b.created_at, b.checkout_at, b.status
                    """,
                SUMMARY_MAPPER, id);
    }

    /**
     * Hủy hóa đơn (BR-24): chỉ khi trạng thái khác "đã hủy".
     *
     * @return số dòng thay đổi: 1 nếu hủy được, 0 nếu không tồn tại hoặc đã hủy từ trước
     */
    public int cancel(long id) {
        return JdbcUtil.update("UPDATE bills SET status = 2 WHERE id = ? AND status <> 2", id);
    }

    /**
     * Doanh thu theo ngày thanh toán (BR-29). Chỉ tính hóa đơn đã thanh toán.
     * Ngày {@code to} được tính hết ngày (BR-30).
     */
    public List<RevenuePoint> revenueByDay(LocalDate from, LocalDate to) {
        return JdbcUtil.query(
                """
                    SELECT DATE_FORMAT(b.checkout_at, '%Y-%m-%d') AS bill_day,
                    SUM(bd.quantity * bd.unit_price * (1 - bd.discount)) AS revenue
                    FROM bills b JOIN bill_details bd ON bd.bill_id = b.id
                    WHERE b.status = 1 AND b.checkout_at >= ? AND b.checkout_at < ?
                    GROUP BY DATE_FORMAT(b.checkout_at, '%Y-%m-%d') ORDER BY bill_day
                    """,
                rs -> new RevenuePoint(rs.getString("bill_day"), rs.getDouble("revenue")),
                from, to.plusDays(1));
    }

    /**
     * Đồ uống bán chạy nhất (BR-31): số lượng giảm dần, hòa thì theo doanh thu giảm dần.
     * Chỉ tính hóa đơn đã thanh toán trong khoảng ngày (BR-29).
     */
    public List<TopDrink> topDrinks(LocalDate from, LocalDate to, int limit) {
        return JdbcUtil.query(
                """
                    SELECT d.name, SUM(bd.quantity) AS qty,
                    SUM(bd.quantity * bd.unit_price * (1 - bd.discount)) AS revenue
                    FROM bills b JOIN bill_details bd ON bd.bill_id = b.id
                    JOIN drinks d ON d.id = bd.drink_id
                    WHERE b.status = 1 AND b.checkout_at >= ? AND b.checkout_at < ?
                    GROUP BY d.id, d.name ORDER BY qty DESC, revenue DESC, d.id LIMIT ?
                    """,
                rs -> new TopDrink(rs.getString("name"), rs.getLong("qty"), rs.getDouble("revenue")),
                from, to.plusDays(1), limit);
    }
}
