package com.polycoffee.DAO;

import com.polycoffee.Dto.BillLine;
import com.polycoffee.Entity.BillDetail;
import com.polycoffee.Util.JdbcUtil;
import com.polycoffee.Util.JdbcUtil.RowMapper;
import java.util.List;

/** Truy cập bảng {@code bill_details}. */
public class BillDetailDAO {

    private static final RowMapper<BillDetail> MAPPER = rs -> new BillDetail(
            rs.getLong("id"), rs.getLong("bill_id"), rs.getInt("drink_id"),
            rs.getDouble("unit_price"), rs.getDouble("discount"), rs.getInt("quantity"));

    private static final RowMapper<BillLine> LINE_MAPPER = rs -> new BillLine(
            rs.getString("name"), rs.getInt("quantity"),
            rs.getDouble("unit_price"), rs.getDouble("discount"));

    public List<BillDetail> findByBillId(long billId) {
        return JdbcUtil.query("SELECT * FROM bill_details WHERE bill_id = ? ORDER BY id", MAPPER, billId);
    }

    /** Các dòng chi tiết kèm tên đồ uống, dùng để hiển thị (BR-23). */
    public List<BillLine> findLinesByBillId(long billId) {
        return JdbcUtil.query(
                """
                    SELECT d.name, bd.quantity, bd.unit_price, bd.discount
                    FROM bill_details bd JOIN drinks d ON d.id = bd.drink_id
                    WHERE bd.bill_id = ? ORDER BY bd.id
                    """,
                LINE_MAPPER, billId);
    }

    public BillDetail findById(long id) {
        return JdbcUtil.queryOne("SELECT * FROM bill_details WHERE id = ?", MAPPER, id);
    }

    public long create(BillDetail d) {
        return JdbcUtil.insert(
                "INSERT INTO bill_details(bill_id, drink_id, unit_price, discount, quantity) VALUES (?,?,?,?,?)",
                d.getBillId(), d.getDrinkId(), d.getUnitPrice(), d.getDiscount(), d.getQuantity());
    }

    public int update(BillDetail d) {
        return JdbcUtil.update("UPDATE bill_details SET quantity=?, unit_price=?, discount=? WHERE id=?",
                d.getQuantity(), d.getUnitPrice(), d.getDiscount(), d.getId());
    }

    public int deleteById(long id) {
        return JdbcUtil.update("DELETE FROM bill_details WHERE id = ?", id);
    }
}
