# ĐẶC TẢ HỆ THỐNG POLYCOFFEE (SPECIFICATIONS)

> Tài liệu đầu vào cho [TEST_PLAN.md](TEST_PLAN.md) và [TEST_CASES.md](TEST_CASES.md). Mọi nội dung được **đối chiếu trực tiếp với mã nguồn** trong `src/main/java/com/polycoffee/`. Những phần chưa có trong code được ghi rõ là "chưa triển khai".

| Mục | Giá trị |
| :--- | :--- |
| Đề tài | PolyCoffee: ứng dụng Java quản lý quán cà phê (phần hiện có: danh mục đồ uống, kết nối CSDL) |
| Sinh viên | Nguyễn Thiện Nhân · TY00234 · SD21308 (`nhan0x`) |
| Phiên bản đặc tả | 3.1 (21/11/2026) |
| Nguồn sự thật | Mã nguồn tại nhánh `main`, commit `682604c`. `src/` và `pom.xml` giống hệt commit `e640f1d` (đã kiểm tra bằng `git diff`) |
| Tham chiếu ngoài repo | Đề `Assignment.pdf`, `Lab_Final/Lab_3/sql/Lab_3.sql` và slide Bài 2 (ERD) trong thư mục `JAV102 - Dự án Java 1` trên máy sinh viên. Không nằm trong repo |

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

> **Lệch với đề:** đề JAV102 và `Lab_3.sql` dùng **SQL Server 2008+**, còn code dùng **MySQL 8**. Xem mục 3 và mục 11.

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

**Kiến trúc đích theo đề** (chưa có trong repo): Trình duyệt → Servlet/JSP (có Filter phân quyền) → DAO (JDBC) → CSDL. Các lab JAV102 (`Lab_Final/Lab_4` đến `Lab_6`) triển khai theo cấu trúc này, nhưng không nằm trong repo.

---

## 3. CƠ SỞ DỮ LIỆU

Repo **không có file SQL** (không có `schema.sql` hay migration). Schema dưới đây là **suy ra** từ câu SQL trong DAO và trường của Entity. Ngoài repo còn hai nguồn tham chiếu (`Lab_3.sql` của JAV102 và slide ERD Bài 2), và ba nguồn này **không khớp nhau**. Cần chọn một nguồn trước khi viết ca kiểm thử ràng buộc.

**Kết nối** (`JdbcUtil`): MySQL 8, `jdbc:mysql://localhost:3306/polycoffee`, user `root`. Khác với đề (SQL Server), xem mục 11.

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

**So sánh ba nguồn schema** (`Lab_3.sql` nằm ngoài repo, trong thư mục JAV102):

| Bảng | Code hiện tại | `Lab_3.sql` (SQL Server) | Slide ERD Bài 2 |
| :--- | :--- | :--- | :--- |
| `categories` | `id`, `name`, `active` | `id` IDENTITY, `name` NVARCHAR(100) UNIQUE. **Không có `active`** | `id`, `name`, `active` (bit) |
| `drinks` | `id`, `categoryId`, `name`, `description`, `image`, `price`, `active` | `id`, `name`, `image`, `unit_price`, `discount`, `available`, `category_id`. **Không có `description`** | `id`, `name`, `price`, `image`, `description`, `active`, `category_id` |
| `users` | **Chưa có Entity** | `id`, `fullname`, `email` UNIQUE, `password`, `manager`, `enabled` | `id`, `email`, `password`, `full_name`, `phone`, `role`, `active` |
| `bills` | `id`, `userId`, `code`, `createdAt`, `total`, `status` (String) | `id` BIGINT, `user_id`, `created_at`, `checkout_at`, `status` INT (0/1/2). **Không có `code`, `total`** | `id`, `code`, `created_at`, `total`, `status`, `user_id`, `card_id` |
| `bill_details` | `id`, `billId`, `drinkId`, `quantity`, `price` | `id`, `bill_id`, `drink_id`, `unit_price`, `discount`, `quantity`, UNIQUE(`bill_id`, `drink_id`) | `id`, `quantity`, `price`, `bill_id`, `drink_id` |

> **Hệ quả (suy ra từ đọc mã và DDL, chưa chạy thực tế):** `CategoryDAO` đọc và ghi cột `active`, nhưng `Lab_3.sql` không có cột này. Nếu chạy code với CSDL theo `Lab_3.sql`, `insert` trả `0` và `findAll` trả `[]` (do R-01). Driver MySQL cũng không kết nối được SQL Server.

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
| **R-09** | Lệch schema và DBMS giữa code, đề và `Lab_3.sql`. `CategoryDAO` đọc và ghi cột `active` không có trong `Lab_3.sql`. Driver là MySQL, đề yêu cầu SQL Server | `JdbcUtil.java` (driver, URL), `CategoryDAO.java` (câu SQL có `active`) | Cao |
| **R-10** | Repo không có script CSDL (N2). Người khác clone về không có cách tạo bảng và dữ liệu mẫu | Thư mục gốc, không có file `.sql` | Cao |
| **R-11** | Không có Servlet hay màn hình nào. Mục tiêu tuần 1 (web chạy ở `localhost:8080`) và tuần 2 (Unit Test cho `controllers/`) chưa có đối tượng để kiểm thử | `src/main/java/com/polycoffee/` | Cao |

---

## 10. GIẢ ĐỊNH VÀ GIỚI HẠN

- Schema trong code (MySQL) chưa có DDL xác nhận. `Lab_3.sql` (SQL Server) và slide ERD không khớp code (mục 3). Các ca liên quan đến ràng buộc (`NOT NULL`, `UNIQUE`, độ dài cột) được đánh dấu **phụ thuộc DDL** cho đến khi chốt schema.
- Môi trường kiểm thử dùng MySQL 8 cục bộ với cơ sở dữ liệu riêng, không dùng dữ liệu thật.
- Không kiểm thử giao diện người dùng, vì ứng dụng hiện không có giao diện.
- Mục 11 chỉ để theo dõi khoảng cách với đề. Các mục đó không đưa vào phạm vi kiểm thử trong `TEST_PLAN.md`.

---

## 11. ĐỐI CHIẾU VỚI ĐỀ JAV102 (DỰ ÁN JAVA 1)

Đề yêu cầu website Java Servlet/JSP, JDBC, SQL Server, có đăng nhập, phân quyền, CRUD và validate. Bảng dưới ghi **trạng thái hiện tại của repo**. Đây là khoảng cách cần xử lý, không phải phạm vi đã triển khai.

| Mã | Yêu cầu của đề | Trạng thái trong repo | Ghi chú |
| :--- | :--- | :--- | :--- |
| Y1 | Đăng nhập / đăng xuất | Chưa | Chưa có `User` Entity, DAO, Servlet hay màn hình |
| Y2 | Phân quyền, ít nhất 2 role | Chưa | Schema tham chiếu có cờ `manager`, code chưa có |
| Y3 | CRUD tối thiểu 3 thực thể | Chưa đạt | Chỉ `Category` có Create (`insert`) và Read (`findAll`) ở DAO. Không có Update, Delete, và không có giao diện |
| Y4 | Validate phía client và server | Chưa | Không có kiểm tra đầu vào (R-03), không có form |
| N1 | Mã nguồn | Có một phần | Chỉ là ứng dụng console |
| N2 | CSDL dạng script có dữ liệu | Chưa | Không có script trong repo (R-10) |
| N3 | File zip thư mục Publish | Chưa | `pom.xml` đóng gói `war`, nhưng không có `src/main/webapp` |
| N4 | Tài liệu cài đặt và triển khai | Chưa | `README.md` hiện là template của môn SOF309 |

> **Theo thang đánh giá của đề:** hạng C cần N1 và N2, hạng B cần Y1–Y3, hạng A cần Y1–Y4. Repo hiện thiếu N2 nên chưa đạt cả hạng C.

**Kiến trúc đích theo đề:** trình duyệt → Servlet/JSP → DAO (JDBC) → SQL Server. Mã mẫu trong `Lab_Final/Lab_4` đến `Lab_6` có Filter đăng nhập và chặn nhân viên truy cập `/manager/*` (trả về 403). Các mã mẫu này nằm ngoài repo.
