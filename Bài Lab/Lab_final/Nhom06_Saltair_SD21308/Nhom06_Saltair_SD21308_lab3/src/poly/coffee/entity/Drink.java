package poly.coffee.entity;

/** Entity anh xa bang CSDL */
public class Drink {
    private int id;
    private String name;
    private String image;
    private double unitPrice;
    private double discount;
    private boolean available;
    private int categoryId;

    public Drink() {}

    public Drink(int id, String name, String image, double unitPrice, double discount, boolean available, int categoryId) {
        this.id = id;
        this.name = name;
        this.image = image;
        this.unitPrice = unitPrice;
        this.discount = discount;
        this.available = available;
        this.categoryId = categoryId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
}
