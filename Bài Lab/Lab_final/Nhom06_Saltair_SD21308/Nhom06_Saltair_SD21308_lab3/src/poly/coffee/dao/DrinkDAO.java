package poly.coffee.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import poly.coffee.entity.Drink;
import poly.coffee.util.JdbcUtil;

public class DrinkDAO {
    protected Drink map(ResultSet rs) throws SQLException {
        return new Drink(rs.getInt("id"), rs.getString("name"), rs.getString("image"),
                rs.getDouble("unit_price"), rs.getDouble("discount"),
                rs.getBoolean("available"), rs.getInt("category_id"));
    }

    public List<Drink> findAll() {
        List<Drink> list = new ArrayList<>();
        try (ResultSet rs = JdbcUtil.executeQuery("SELECT * FROM drinks ORDER BY id")) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public Drink findById(int id) {
        try (ResultSet rs = JdbcUtil.executeQuery("SELECT * FROM drinks WHERE id = ?", id)) {
            return rs.next() ? map(rs) : null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void create(Drink d) {
        JdbcUtil.executeUpdate(
            "INSERT INTO drinks(name,image,unit_price,discount,available,category_id) VALUES (?,?,?,?,?,?)",
            d.getName(), d.getImage(), d.getUnitPrice(), d.getDiscount(), d.isAvailable(), d.getCategoryId());
    }

    public void update(Drink d) {
        JdbcUtil.executeUpdate(
            "UPDATE drinks SET name=?, image=?, unit_price=?, discount=?, available=?, category_id=? WHERE id=?",
            d.getName(), d.getImage(), d.getUnitPrice(), d.getDiscount(), d.isAvailable(), d.getCategoryId(), d.getId());
    }

    public void deleteById(int id) {
        JdbcUtil.executeUpdate("DELETE FROM drinks WHERE id = ?", id);
    }
}
