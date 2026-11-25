package com.polycoffee.DAO;

import com.polycoffee.Entity.Category;
import com.polycoffee.Util.JdbcUtil;
import com.polycoffee.Util.JdbcUtil.RowMapper;
import java.util.List;

/** Truy cập bảng {@code categories}. Lỗi SQL được ném ra dạng RuntimeException (NFR-03). */
public class CategoryDAO {

    private static final RowMapper<Category> MAPPER =
            rs -> new Category(rs.getInt("id"), rs.getString("name"));

    public List<Category> findAll() {
        return JdbcUtil.query("SELECT id, name FROM categories ORDER BY id", MAPPER);
    }

    public Category findById(int id) {
        return JdbcUtil.queryOne("SELECT id, name FROM categories WHERE id = ?", MAPPER, id);
    }

    /** Thêm loại mới, trả về id vừa sinh. */
    public int create(Category c) {
        return (int) JdbcUtil.insert("INSERT INTO categories(name) VALUES (?)", c.getName());
    }

    public int update(Category c) {
        return JdbcUtil.update("UPDATE categories SET name = ? WHERE id = ?", c.getName(), c.getId());
    }

    public int deleteById(int id) {
        return JdbcUtil.update("DELETE FROM categories WHERE id = ?", id);
    }

    /** Số đồ uống thuộc loại, dùng để chặn xóa loại đang được sử dụng (BR-10). */
    public int countDrinks(int categoryId) {
        return (int) JdbcUtil.count("SELECT COUNT(*) FROM drinks WHERE category_id = ?", categoryId);
    }

    /**
     * Tên loại đã tồn tại ở loại khác chưa (BR-09). Khi sửa, truyền id của chính loại đó
     * để không báo trùng với chính nó. Khi thêm mới truyền 0.
     */
    public boolean existsByName(String name, int excludeId) {
        return JdbcUtil.count("SELECT COUNT(*) FROM categories WHERE name = ? AND id <> ?",
                name, excludeId) > 0;
    }
}
