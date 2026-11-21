package poly.coffee.entity;

/** Entity anh xa bang CSDL */
public class User {
    private int id;
    private String fullname;
    private String email;
    private String password;
    private boolean manager;
    private boolean enabled;

    public User() {}

    public User(int id, String fullname, String email, String password, boolean manager, boolean enabled) {
        this.id = id;
        this.fullname = fullname;
        this.email = email;
        this.password = password;
        this.manager = manager;
        this.enabled = enabled;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFullname() { return fullname; }
    public void setFullname(String fullname) { this.fullname = fullname; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isManager() { return manager; }
    public void setManager(boolean manager) { this.manager = manager; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
