# ĐẶC TẢ HỆ THỐNG POLYCOFFEE (SPECIFICATIONS)

> Tài liệu đầu vào cho [TEST_PLAN.md](TEST_PLAN.md) và [TEST_CASES.md](TEST_CASES.md). Mọi nội dung được **đối chiếu trực tiếp với mã nguồn** trong `src/main/java/com/polycoffee/`. Những phần chưa có trong code được ghi rõ là "chưa triển khai".

| Mục | Giá trị |
| :--- | :--- |
| Đề tài | PolyCoffee: ứng dụng Java quản lý quán cà phê (phần hiện có: danh mục đồ uống, kết nối CSDL) |
| Sinh viên | Nguyễn Thiện Nhân · TY00234 · SD21308 (`nhan0x`) |
| Phiên bản đặc tả | 3.0 (19/11/2026) |
| Nguồn sự thật | Mã nguồn tại nhánh `feat/Week-2`, commit `e640f1d` |

---

## 1. CÔNG NGHỆ

| Lớp | Công nghệ | Phiên bản | Ghi chú |
| :--- | :--- | :--- | :--- |
| Ngôn ngữ | Java | 17 (`maven.compiler.release` trong `pom.xml`) | Build phải dùng JDK 17. Lombok 1.18.44 không chạy trên JDK 27 |
| Build | Maven | 3.10+ | `mvn clean compile` |
| Sinh code | Lombok | 1.18.44 | `@Data`, `@AllArgsConstructor`, `@NoArgsConstructor` cho Entity. Cấu hình ở `dependencies` và `annotationProcessorPaths` |
| CSDL | MySQL | 8 | JDBC thuần (`java.sql`), driver `com.mysql.cj.jdbc.Driver` (`mysql-connector-j`) |
| Kiểm thử | JUnit 5, Mockito | khai báo trong `pom.xml` | Chưa có test nào trong `src/test/` |

> **Lưu ý `pom.xml`:** file còn mang cấu hình của dự án cũ (`artifactId` `saltair`, `packaging` `war`, các dependency Servlet/JSP/Tomcat/Flyway/Selenium). Ứng dụng hiện là **console app**, không chạy trên Tomcat. Chưa có `src/main/webapp` hay `web.xml`.

---

## 2. KIẾN TRÚC VÀ CẤU TRÚC MÃ

```
com.polycoffee
├── Main.java                  Điểm vào: đọc và in danh sách danh mục
├── Util/JdbcUtil.java         Tạo kết nối JDBC
├── DAO/CategoryDAO.java       Truy vấn bảng categories
└── Entity/
    ├── Category.java          Danh mục đồ uống (có DAO)
    ├── Drink.java             Đồ uống (chưa có DAO)
    ├── Bill.java              Hóa đơn (chưa có DAO)
    └── BillDetail.java        Chi tiết hóa đơn (chưa có DAO)
```

Luồng hiện tại: `Main` → `CategoryDAO.findAll()` → `JdbcUtil.getConnection()` → MySQL `polycoffee`.

---

## 3. CƠ SỞ DỮ LIỆU

Repo **không có file SQL** (không có `schema.sql` hay migration). Cấu trúc dưới đây suy ra từ câu SQL trong DAO và trường của Entity, cần đối chiếu với CSDL thật trước khi chạy test.

**Kết nối** (`JdbcUtil`): `jdbc:mysql://localhost:3306/polycoffee`, user `root`.

**Bảng `categories`** (có DAO, xác nhận từ code):

| Cột | Kiểu suy ra | Được dùng ở |
| :--- | :--- | :--- |
| `id` | INT, khóa chính, tự tăng (suy ra) | `CategoryDAO.findAll()` đọc `id` |
| `name` | VARCHAR (độ dài chưa rõ) | `insert` ghi, `findAll` đọc |
| `active` | BOOLEAN / TINYINT(1) | `insert` ghi, `findAll` đọc |

**Các bảng còn lại** (`drinks`, `bills`, `bill_details`): chưa có DAO và chưa có câu SQL nào trong code. Cột suy ra từ Entity, chỉ để tham khảo.

| Entity | Trường | Ghi chú |
| :--- | :--- | :--- |
| `Drink` | id, categoryId, name, description, image, price (int), active | Khóa ngoại `categoryId` tới `categories` (suy ra) |
| `Bill` | id, userId, code, createdAt (LocalDateTime), total (int), status | |
| `BillDetail` | id, billId, drinkId, quantity (int), price (int) | Khóa ngoại tới `bills` và `drinks` (suy ra) |

---

## 4. YÊU CẦU CHỨC NĂNG (THEO CODE)

| Mã | Chức năng | Mô tả theo code | Trạng thái |
| :--- | :--- | :--- | :--- |
| FR-01 | Xem danh sách danh mục | `Main.main` gọi `findAll()` và in mỗi danh mục gồm `id`, `name`, `active` (mỗi giá trị một dòng) | Có |
| FR-02 | Thêm danh mục | `CategoryDAO.insert(Category)` chèn `name`, `active` vào `categories`, trả về số dòng ảnh hưởng | Có ở tầng DAO. **Chưa có giao diện**: `Main` không gọi `insert` |
| FR-03 | Kết nối CSDL | `JdbcUtil.getConnection()` nạp driver trong static block và mở kết nối mới mỗi lần gọi | Có |
| FR-04 | Quản lý đồ uống | `Drink` đã khai báo nhưng chưa có DAO, chưa có chức năng | Chưa triển khai |
| FR-05 | Quản lý hóa đơn và chi tiết | `Bill`, `BillDetail` đã khai báo, chưa có DAO hay chức năng | Chưa triển khai |
| FR-06 | Sửa, xóa danh mục | Không có trong code | Chưa triển khai |

---

## 5. HÀNH VI CHI TIẾT CỦA DAO

### 5.1. `CategoryDAO.insert(Category category)`

| Khía cạnh | Hành vi theo code |
| :--- | :--- |
| Câu SQL | `INSERT INTO categories(name, active) VALUES (?, ?)` (`PreparedStatement`, có tham số hóa) |
| Thành công | Trả về số dòng ảnh hưởng (thường là `1`) |
| Lỗi `SQLException` | Gọi `printStackTrace()`, **trả về `0`**, không ném lỗi ra ngoài |
| Trường `id` | Bị bỏ qua. Không trả về `id` vừa sinh |

### 5.2. `CategoryDAO.findAll()`

| Khía cạnh | Hành vi theo code |
| :--- | :--- |
| Câu SQL | `SELECT * FROM categories` (không có `ORDER BY`, thứ tự không được đảm bảo) |
| Ánh xạ | Đọc `id` (int), `name` (String), `active` (boolean) và gán vào `Category` |
| Có dữ liệu | Trả về danh sách đủ dòng |
| Bảng rỗng | Trả về danh sách rỗng (không phải `null`) |
| Lỗi `SQLException` | Gọi `printStackTrace()`, **trả về danh sách rỗng**, không ném lỗi ra ngoài |

### 5.3. `JdbcUtil`

| Khía cạnh | Hành vi theo code |
| :--- | :--- |
| Nạp driver | Static block gọi `Class.forName`. Nếu lỗi thì chỉ in stack trace |
| `getConnection()` | Gọi `DriverManager.getConnection` với URL, user, password cố định. Lỗi thì ném `SQLException` |

---

## 6. HÀNH VI CỦA ENTITY

| Khía cạnh | Hành vi theo code |
| :--- | :--- |
| Getter/setter, `toString`, `equals`, `hashCode` | Sinh tự động bởi Lombok `@Data` |
| Constructor không tham số | Giá trị mặc định: `Integer` là `null`, `int` là `0`, `boolean` là `false`, `String` là `null` |
| Constructor đủ tham số | Thứ tự tham số theo thứ tự khai báo trường (`Category(id, name, active)`) |
| Getter của `boolean active` | `isActive()` (không phải `getActive()`) |

---

## 7. HÀNH VI CỦA `Main`

| Khía cạnh | Hành vi theo code |
| :--- | :--- |
| Dữ liệu đầu vào | Không có. Không đọc tham số dòng lệnh hay bàn phím |
| Đầu ra | Với mỗi danh mục, in lần lượt `id`, `name`, `active`, mỗi giá trị một dòng |
| Bảng rỗng | Không in gì |
| Lỗi CSDL | `CategoryDAO` tự nuốt lỗi, `Main` in danh sách rỗng và kết thúc bình thường (không có thông báo lỗi cho người dùng) |

---

## 8. YÊU CẦU PHI CHỨC NĂNG

| Mã | Nội dung | Trạng thái |
| :--- | :--- | :--- |
| NFR-01 | Truy vấn dùng `PreparedStatement` (chống SQL injection cho `insert`) | Đạt |
| NFR-02 | Tài nguyên JDBC được đóng bằng `try-with-resources` | Đạt |
| NFR-03 | Lỗi CSDL được báo cho người gọi | **Không đạt**: DAO nuốt lỗi và trả giá trị mặc định (xem R-01) |
| NFR-04 | Thông tin kết nối không nằm trong mã nguồn | **Không đạt**: mật khẩu được hardcode trong `JdbcUtil` (xem R-02) |

---

## 9. RỦI RO ĐÃ NHẬN DIỆN (GIẢ THUYẾT LỖI)

| Mã | Rủi ro | Căn cứ trong code | Mức |
| :--- | :--- | :--- | :---: |
| **R-01** | Lỗi CSDL bị nuốt. `insert` trả `0` và `findAll` trả `[]`, không phân biệt được với "không có dữ liệu" hay "thêm thất bại" | `CategoryDAO.java` (khối `catch` của cả hai hàm) | Cao |
| **R-02** | Mật khẩu MySQL (`root`) được hardcode trong mã nguồn và đã được đẩy lên repo public | `JdbcUtil.java`, dòng khai báo `password` | Cao |
| **R-03** | `insert` không có kiểm tra đầu vào: `name` null, rỗng, quá dài đều đi thẳng xuống CSDL | `CategoryDAO.insert` | Trung bình |
| **R-04** | `insert` không trả về `id` vừa tạo nên người gọi không biết khóa mới | `CategoryDAO.insert` | Thấp |
| **R-05** | Không có đường đi từ giao diện đến `insert`. Chức năng thêm danh mục chỉ dùng được khi gọi trực tiếp từ mã | `Main.java` | Thấp |
| **R-06** | `Drink`, `Bill`, `BillDetail` không có DAO và không có test nên lỗi ánh xạ trong tương lai không được phát hiện | Thư mục `Entity/` | Thấp |
| **R-07** | `findAll` không có `ORDER BY` nên thứ tự in ra có thể thay đổi giữa các lần chạy | `CategoryDAO.findAll` | Thấp |
| **R-08** | Build thất bại trên JDK 27 do Lombok, và `pom.xml` còn cấu hình của dự án Saltair (war, Tomcat) | `pom.xml` | Trung bình |

---

## 10. GIẢ ĐỊNH VÀ GIỚI HẠN

- Schema `categories` chưa được xác nhận. Các ca liên quan đến ràng buộc (`NOT NULL`, `UNIQUE`, độ dài cột) được đánh dấu **phụ thuộc DDL**.
- Môi trường kiểm thử dùng MySQL 8 cục bộ với cơ sở dữ liệu riêng, không dùng dữ liệu thật.
- Không kiểm thử giao diện người dùng, vì ứng dụng hiện không có giao diện.
