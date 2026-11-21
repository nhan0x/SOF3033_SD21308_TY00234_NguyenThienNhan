package poly.coffee.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import poly.coffee.entity.Category;
import poly.coffee.util.JdbcUtil;

public class CategoryDAO {
    private Category map(ResultSet rs) throws SQLException {
        return new Category(rs.getInt("id"), rs.getString("name"));
    }

    public List<Category> findAll() {
        List<Category> list = new ArrayList<>();
        try (ResultSet rs = JdbcUtil.executeQuery("SELECT * FROM categories ORDER BY id")) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public Category findById(int id) {
        try (ResultSet rs = JdbcUtil.executeQuery("SELECT * FROM categories WHERE id = ?", id)) {
            return rs.next() ? map(rs) : null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void create(Category c) {
        JdbcUtil.executeUpdate("INSERT INTO categories(name) VALUES (?)", c.getName());
    }

    public void update(Category c) {
        JdbcUtil.executeUpdate("UPDATE categories SET name = ? WHERE id = ?", c.getName(), c.getId());
    }

    public void deleteById(int id) {
        JdbcUtil.executeUpdate("DELETE FROM categories WHERE id = ?", id);
    }

    /** So do uong thuoc loai (dung de chan xoa loai dang duoc su dung). */
    public int countDrinks(int categoryId) {
        Object n = JdbcUtil.executeScalar("SELECT COUNT(*) FROM drinks WHERE category_id = ?", categoryId);
        return n == null ? 0 : ((Number) n).intValue();
    }
}
