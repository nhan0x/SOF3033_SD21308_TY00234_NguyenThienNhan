package poly.coffee.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import poly.coffee.entity.BillDetail;
import poly.coffee.util.JdbcUtil;

public class BillDetailDAO {
    protected BillDetail map(ResultSet rs) throws SQLException {
        return new BillDetail(rs.getLong("id"), rs.getLong("bill_id"), rs.getInt("drink_id"),
                rs.getDouble("unit_price"), rs.getDouble("discount"), rs.getInt("quantity"));
    }

    public List<BillDetail> findByBillId(long billId) {
        List<BillDetail> list = new ArrayList<>();
        try (ResultSet rs = JdbcUtil.executeQuery(
                "SELECT * FROM bill_details WHERE bill_id = ? ORDER BY id", billId)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public BillDetail findById(long id) {
        try (ResultSet rs = JdbcUtil.executeQuery("SELECT * FROM bill_details WHERE id = ?", id)) {
            return rs.next() ? map(rs) : null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void create(BillDetail d) {
        JdbcUtil.executeUpdate(
            "INSERT INTO bill_details(bill_id,drink_id,unit_price,discount,quantity) VALUES (?,?,?,?,?)",
            d.getBillId(), d.getDrinkId(), d.getUnitPrice(), d.getDiscount(), d.getQuantity());
    }

    public void update(BillDetail d) {
        JdbcUtil.executeUpdate("UPDATE bill_details SET quantity=?, unit_price=?, discount=? WHERE id=?",
                d.getQuantity(), d.getUnitPrice(), d.getDiscount(), d.getId());
    }

    public void deleteById(long id) {
        JdbcUtil.executeUpdate("DELETE FROM bill_details WHERE id = ?", id);
    }
}
