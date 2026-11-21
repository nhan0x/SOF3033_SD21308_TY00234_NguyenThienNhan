package poly.coffee.entity;

import java.util.Date;

/** Entity anh xa bang CSDL */
public class Bill {
    private long id;
    private int userId;
    private Date createdAt;
    private Date checkoutAt;
    private int status;

    public Bill() {}

    public Bill(long id, int userId, Date createdAt, Date checkoutAt, int status) {
        this.id = id;
        this.userId = userId;
        this.createdAt = createdAt;
        this.checkoutAt = checkoutAt;
        this.status = status;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getCheckoutAt() { return checkoutAt; }
    public void setCheckoutAt(Date checkoutAt) { this.checkoutAt = checkoutAt; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
}
