package poly.coffee.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import poly.coffee.entity.Bill;
import poly.coffee.util.JdbcUtil;

public class BillDAO {
    protected Bill map(ResultSet rs) throws SQLException {
        Timestamp co = rs.getTimestamp("checkout_at");
        return new Bill(rs.getLong("id"), rs.getInt("user_id"),
                new Date(rs.getTimestamp("created_at").getTime()),
                co == null ? null : new Date(co.getTime()), rs.getInt("status"));
    }

    public List<Bill> findAll() {
        List<Bill> list = new ArrayList<>();
        try (ResultSet rs = JdbcUtil.executeQuery("SELECT * FROM bills ORDER BY id DESC")) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public List<Bill> findByUserId(int userId) {
        List<Bill> list = new ArrayList<>();
        try (ResultSet rs = JdbcUtil.executeQuery(
                "SELECT * FROM bills WHERE user_id = ? ORDER BY id DESC", userId)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public Bill findById(long id) {
        try (ResultSet rs = JdbcUtil.executeQuery("SELECT * FROM bills WHERE id = ?", id)) {
            return rs.next() ? map(rs) : null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** Tao phieu moi va tra ve id vua sinh. */
    public long create(Bill b) {
        JdbcUtil.executeUpdate("INSERT INTO bills(user_id, created_at, status) VALUES (?, GETDATE(), ?)",
                b.getUserId(), b.getStatus());
        Object id = JdbcUtil.executeScalar("SELECT MAX(id) FROM bills WHERE user_id = ?", b.getUserId());
        return ((Number) id).longValue();
    }

    public void update(Bill b) {
        JdbcUtil.executeUpdate("UPDATE bills SET user_id=?, checkout_at=?, status=? WHERE id=?",
                b.getUserId(),
                b.getCheckoutAt() == null ? null : new Timestamp(b.getCheckoutAt().getTime()),
                b.getStatus(), b.getId());
    }

    public void deleteById(long id) {
        JdbcUtil.executeUpdate("DELETE FROM bills WHERE id = ?", id);   // bill_details xoa theo (CASCADE)
    }
}
