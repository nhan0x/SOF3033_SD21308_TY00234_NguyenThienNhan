package poly.coffee.entity;

/** Entity anh xa bang CSDL */
public class BillDetail {
    private long id;
    private long billId;
    private int drinkId;
    private double unitPrice;
    private double discount;
    private int quantity;

    public BillDetail() {}

    public BillDetail(long id, long billId, int drinkId, double unitPrice, double discount, int quantity) {
        this.id = id;
        this.billId = billId;
        this.drinkId = drinkId;
        this.unitPrice = unitPrice;
        this.discount = discount;
        this.quantity = quantity;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getBillId() { return billId; }
    public void setBillId(long billId) { this.billId = billId; }

    public int getDrinkId() { return drinkId; }
    public void setDrinkId(int drinkId) { this.drinkId = drinkId; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
