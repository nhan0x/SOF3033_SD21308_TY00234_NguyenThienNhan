package com.polycoffee.DAO;

import com.polycoffee.Entity.Drink;
import com.polycoffee.Util.JdbcUtil;
import com.polycoffee.Util.JdbcUtil.RowMapper;
import com.polycoffee.Util.Page;
import java.util.List;

/** Truy cập bảng {@code drinks}. */
public class DrinkDAO {

    private static final RowMapper<Drink> MAPPER = rs -> new Drink(
            rs.getInt("id"), rs.getString("name"), rs.getString("image"),
            rs.getDouble("unit_price"), rs.getDouble("discount"),
            rs.getBoolean("available"), rs.getInt("category_id"));

    public List<Drink> findAll() {
        return JdbcUtil.query("SELECT * FROM drinks ORDER BY id", MAPPER);
    }

    /** Đồ uống đang bán, sắp theo loại rồi theo id (trang chủ, BR-08). */
    public List<Drink> findAvailable() {
        return JdbcUtil.query("SELECT * FROM drinks WHERE available = 1 ORDER BY category_id, id", MAPPER);
    }

    public Drink findById(int id) {
        return JdbcUtil.queryOne("SELECT * FROM drinks WHERE id = ?", MAPPER, id);
    }

    /**
     * Tìm kiếm và phân trang (BR-13). Tham số null nghĩa là không lọc theo điều kiện đó.
     *
     * @param keyword    chuỗi con của tên, không phân biệt hoa thường
     * @param categoryId loại đồ uống
     * @param available  true: đang bán, false: ngừng bán
     */
    public Page<Drink> search(String keyword, Integer categoryId, Boolean available, int page, int size) {
        String like = keyword == null ? null : "%" + escapeLike(keyword) + "%";
        long total = JdbcUtil.count(
                """
                    SELECT COUNT(*) FROM drinks WHERE (? IS NULL OR name LIKE ?)
                    AND (? IS NULL OR category_id = ?) AND (? IS NULL OR available = ?)
                    """,
                like, like, categoryId, categoryId, available, available);
        int current = Page.clamp(page, total, size);
        List<Drink> items = JdbcUtil.query(
                """
                    SELECT * FROM drinks WHERE (? IS NULL OR name LIKE ?)
                    AND (? IS NULL OR category_id = ?) AND (? IS NULL OR available = ?)
                    ORDER BY id LIMIT ? OFFSET ?
                    """,
                MAPPER, like, like, categoryId, categoryId, available, available,
                size, Page.offset(current, size));
        return new Page<>(items, current, size, total);
    }

    /** Thêm đồ uống mới, trả về id vừa sinh. */
    public int create(Drink d) {
        return (int) JdbcUtil.insert(
                "INSERT INTO drinks(name, image, unit_price, discount, available, category_id) VALUES (?,?,?,?,?,?)",
                d.getName(), d.getImage(), d.getUnitPrice(), d.getDiscount(), d.isAvailable(), d.getCategoryId());
    }

    public int update(Drink d) {
        return JdbcUtil.update(
                "UPDATE drinks SET name=?, image=?, unit_price=?, discount=?, available=?, category_id=? WHERE id=?",
                d.getName(), d.getImage(), d.getUnitPrice(), d.getDiscount(), d.isAvailable(),
                d.getCategoryId(), d.getId());
    }

    public int deleteById(int id) {
        return JdbcUtil.update("DELETE FROM drinks WHERE id = ?", id);
    }

    /** Số dòng hóa đơn có đồ uống này, dùng để chặn xóa đồ uống đã bán (BR-12). */
    public int countBillDetails(int drinkId) {
        return (int) JdbcUtil.count("SELECT COUNT(*) FROM bill_details WHERE drink_id = ?", drinkId);
    }

    /** Thoát các ký tự đại diện của LIKE để "%" và "_" người dùng nhập được hiểu theo nghĩa đen. */
    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
