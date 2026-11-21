# ĐẶC TẢ HỆ THỐNG POLYCOFFEE (SPECIFICATIONS)

> Tài liệu đầu vào cho [TEST_PLAN.md](TEST_PLAN.md) và [TEST_CASES.md](TEST_CASES.md).
> Đặc tả mô tả **sản phẩm đích** theo các bài Lab 1–8 đã làm. Cột **Trạng thái** cho biết phần nào đã có trong mã nguồn `src/main/` của repo này. Phần đánh dấu "chưa có" là việc còn lại.

| Mục | Giá trị |
| :--- | :--- |
| Đề tài | PolyCoffee: website quản lý quán cà phê (đồ uống, loại đồ uống, nhân viên, hóa đơn, thống kê) |
| Sinh viên | Nguyễn Thiện Nhân · TY00234 · SD21308 (`nhan0x`) |
| Phiên bản đặc tả | 4.0 (21/11/2026) |
| Nguồn sự thật | Bài làm Lab 1–8 trong thư mục `JAV102 - Dự án Java 1/Lab_Final` (nằm ngoài repo). Quyết định của sinh viên ngày 21/11/2026: **CSDL dùng MySQL 8** |
| Trạng thái mã nguồn | Repo `main` (commit `682604c`) hiện chỉ là ứng dụng console, đang được sinh viên tự code hoàn thiện. Xem mục 10 |

---

## 1. CÔNG NGHỆ

| Lớp | Công nghệ | Phiên bản | Ghi chú |
| :--- | :--- | :--- | :--- |
| Ngôn ngữ | Java | 8+ (theo Lab 8) | `pom.xml` của repo đang đặt `release` 17 |
| Web | Java Servlet, JSP, JSTL | Servlet 4.0 (`javax.servlet`), JSP 2.3, JSTL 1.2 | Theo Lab. `pom.xml` của repo đang dùng `jakarta` 6.1 (Tomcat 11). Cần chọn một bộ |
| Web server | Apache Tomcat | 9.x | Tomcat 10 trở lên không tương thích với `javax.servlet` |
| CSDL | **MySQL** | 8 | Quyết định ngày 21/11/2026. Driver `mysql-connector-j` (đang có trong `pom.xml`). Lab dùng SQL Server, xem mục 3.3 |
| Truy cập dữ liệu | JDBC thuần (`java.sql`), `PreparedStatement` | | Không dùng ORM |
| Gửi email | JavaMail | 1.6.2 (Lab) | `angus-mail` đang có trong `pom.xml`. Dùng cho chức năng cấp lại mật khẩu |
| Biểu đồ | Chart.js | 4.4.1 (CDN) | Cần Internet khi xem thống kê |
| Build | Maven | 3.6+ | Đóng gói `target/PolyCoffee.war` |
| Kiểm thử | JUnit 5, Mockito | | Khai báo trong `pom.xml`. Chưa có test nào trong `src/test/` |

---

## 2. KIẾN TRÚC VÀ CẤU TRÚC MÃ

### 2.1 Kiến trúc

```
[ Trình duyệt ] --HTTP--> [ Tomcat: Filter -> Servlet (Controller) -> DAO -> JSP (View) ] --JDBC--> [ MySQL 8 ]
```

Luồng xử lý một request: `Utf8Filter` → `AuthFilter` (đăng nhập, phân quyền) → Servlet (đọc tham số, kiểm tra dữ liệu, gọi DAO) → forward sang JSP trong `WEB-INF/views` (hoặc redirect sau thao tác ghi).

### 2.2 Cấu trúc package (theo Lab 4–8)

```
poly.coffee
├── servlet/   HomeServlet, AuthServlet, CategoryServlet, DrinkServlet, StaffServlet,
│              BillManageServlet, StatsServlet
├── filter/    Utf8Filter, AuthFilter
├── dao/       CategoryDAO, DrinkDAO, UserDAO, BillDAO, BillDetailDAO
├── entity/    Category, Drink, User, Bill, BillDetail
├── dto/       BillSummary, BillLine, TopDrink, RevenuePoint
└── util/      JdbcUtil, AuthUtil, ParamUtil, PasswordUtil, FileUtil, MailUtil, Page
webapp/
├── css/, images/
└── WEB-INF/views/   _header.jspf, _footer.jspf, home.jsp, auth/login.jsp,
                     manager/{category, drink, staff, bill, stats}/
```

| Lớp | Vai trò | Ghi chú |
| :--- | :--- | :--- |
| Servlet | Nhận request, đọc và kiểm tra tham số, gọi DAO, chọn view hoặc redirect | Dự án không có tầng Service riêng. Nghiệp vụ đơn giản (kiểm tra xóa, cấp lại mật khẩu) nằm trong Servlet |
| DAO | Truy vấn JDBC, trả về Entity hoặc DTO | Lỗi SQL ném `RuntimeException` (xem NFR-03) |
| Entity | Ánh xạ một bảng, có getter và setter | |
| DTO | Dữ liệu cho màn hình hóa đơn và thống kê (có cột tính toán như tổng tiền) | |
| View (JSP) | Hiển thị bằng JSTL/EL. Không truy cập CSDL trực tiếp | |

### 2.3 Quy ước

- Package gốc `poly.coffee`. Lớp PascalCase, phương thức và biến camelCase, cột CSDL snake_case.
- Hậu tố: `*Servlet`, `*DAO`, `*Util`, `*Filter`.
- Tài nguyên quản lý nằm dưới `/manager/*`. Thao tác ghi dùng **POST**, thao tác đọc dùng **GET**.
- Sau thao tác ghi thành công: **Post-Redirect-Get**. Thông báo qua session (`flashOk`, `flashError`).
- Mọi câu SQL dùng `?`, không nối chuỗi từ dữ liệu người dùng.
- JSP hiển thị dữ liệu người dùng bằng `<c:out>` (chống XSS).
- UTF-8 toàn bộ: file, `Utf8Filter`, `pageEncoding` của JSP.

---

## 3. CƠ SỞ DỮ LIỆU (MYSQL 8)

### 3.1 Bảng và quan hệ

| Bảng | Khóa | Quan hệ | Ghi chú |
| :--- | :--- | :--- | :--- |
| `categories` | `id` | 1 — N `drinks` | Loại đồ uống. Tên duy nhất |
| `drinks` | `id` | N — 1 `categories`; N — N `bills` qua `bill_details` | Đồ uống. `available = 0` là ngừng bán |
| `users` | `id` | 1 — N `bills` | Nhân viên và quản lý. `manager = 1` là quản lý |
| `bills` | `id` | N — 1 `users`; 1 — N `bill_details` | Hóa đơn (phiếu bán hàng) |
| `bill_details` | `id` | N — 1 `bills`; N — 1 `drinks` | Dòng hàng trong hóa đơn. Cặp (`bill_id`, `drink_id`) là duy nhất |

### 3.2 DDL đích

> DDL này được **chuyển đổi từ `Lab_3.sql`** (SQL Server) sang MySQL 8. Chưa chạy trên MySQL thật. Cần chạy thử trước khi dùng cho test.

```sql
CREATE DATABASE IF NOT EXISTS polycoffee CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE polycoffee;

DROP TABLE IF EXISTS bill_details, bills, drinks, categories, users;

CREATE TABLE categories (
    id    INT AUTO_INCREMENT PRIMARY KEY,
    name  VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE users (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    fullname  VARCHAR(100) NOT NULL,
    email     VARCHAR(100) NOT NULL UNIQUE,
    password  VARCHAR(100) NOT NULL,              -- SHA-256 dạng hex, 64 ký tự
    manager   BOOLEAN NOT NULL DEFAULT FALSE,     -- 1: quản lý, 0: nhân viên
    enabled   BOOLEAN NOT NULL DEFAULT TRUE       -- 1: đang hoạt động, 0: bị khóa
);

CREATE TABLE drinks (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    image        VARCHAR(255) NULL,
    unit_price   DECIMAL(18,0) NOT NULL CHECK (unit_price >= 0),
    discount     DOUBLE NOT NULL DEFAULT 0 CHECK (discount BETWEEN 0 AND 1),
    available    BOOLEAN NOT NULL DEFAULT TRUE,   -- 1: đang bán, 0: ngừng bán
    category_id  INT NOT NULL,
    CONSTRAINT fk_drinks_categories FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE bills (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      INT NOT NULL,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    checkout_at  DATETIME NULL,
    status       INT NOT NULL DEFAULT 0 CHECK (status IN (0, 1, 2)),  -- 0: đang xử lý, 1: đã thanh toán, 2: đã hủy
    CONSTRAINT fk_bills_users FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE bill_details (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    bill_id     BIGINT NOT NULL,
    drink_id    INT NOT NULL,
    unit_price  DECIMAL(18,0) NOT NULL,           -- giá tại thời điểm bán (chụp lại)
    discount    DOUBLE NOT NULL DEFAULT 0,        -- giảm giá tại thời điểm bán
    quantity    INT NOT NULL CHECK (quantity > 0),
    CONSTRAINT fk_bd_bills  FOREIGN KEY (bill_id)  REFERENCES bills(id) ON DELETE CASCADE,
    CONSTRAINT fk_bd_drinks FOREIGN KEY (drink_id) REFERENCES drinks(id),
    CONSTRAINT uq_bd UNIQUE (bill_id, drink_id)
);
```

**Dữ liệu mẫu** (giữ như `Lab_3.sql`): 3 tài khoản (`manager@polycoffee.com` là quản lý; `staff1@polycoffee.com` và `staff2@polycoffee.com` là nhân viên), mật khẩu mẫu `123456`; 4 loại (Cà phê, Trà, Sinh tố, Nước ép); 9 đồ uống, trong đó "Nước ép dưa hấu" đang ngừng bán; 3 hóa đơn (2 đã thanh toán, 1 đang xử lý) với 6 dòng chi tiết.

### 3.3 Chuyển đổi từ SQL Server (Lab) sang MySQL

| SQL Server (`Lab_3.sql`, Lab 5–6) | MySQL 8 |
| :--- | :--- |
| `IDENTITY(1,1)` | `AUTO_INCREMENT` |
| `NVARCHAR(n)` | `VARCHAR(n)`, bảng và kết nối dùng `utf8mb4` |
| `BIT` | `BOOLEAN` (lưu dưới dạng `TINYINT(1)`) |
| `FLOAT` | `DOUBLE` |
| `GETDATE()` | `NOW()` (hoặc `CURRENT_TIMESTAMP` trong DDL) |
| `DATEADD(day, 1, ?)` | `DATE_ADD(?, INTERVAL 1 DAY)` |
| `ISNULL(a, b)` | `IFNULL(a, b)` |
| `SELECT TOP (n) ...` | `... LIMIT n` |
| Phân trang bằng `ROW_NUMBER() OVER (...)` | `ORDER BY ... LIMIT ? OFFSET ?` |
| `CONVERT(VARCHAR(10), x, 23)` | `DATE_FORMAT(x, '%Y-%m-%d')` |
| `SELECT MAX(id)` sau `INSERT` | `Statement.RETURN_GENERATED_KEYS` hoặc `LAST_INSERT_ID()` |
| `IF OBJECT_ID(...) ... DROP`, `GO` | `DROP TABLE IF EXISTS`, bỏ `GO` |
| `jdbc:sqlserver://localhost:1433`, user `sa` | `jdbc:mysql://localhost:3306/polycoffee`, user theo máy |

---

## 4. PHÂN QUYỀN VÀ ĐƯỜNG DẪN

| Đường dẫn | Phương thức | Vai trò được phép | Chức năng |
| :--- | :--- | :--- | :--- |
| `/`, `/home` | GET | Tất cả (kể cả khách) | Trang chủ, xem thực đơn |
| `/auth/login` | GET, POST | Tất cả | Hiển thị và xử lý đăng nhập |
| `/auth/logout` | GET | Đã đăng nhập | Đăng xuất |
| `/manager/categories`, `/new`, `/edit?id=`, `/save`, `/delete` | GET, POST | Quản lý | Quản lý loại đồ uống |
| `/manager/drinks`, `/new`, `/edit?id=`, `/save`, `/delete` | GET, POST | Quản lý | Quản lý đồ uống |
| `/manager/staff`, `/new`, `/edit?id=`, `/save`, `/delete`, `/reset-password` | GET, POST | Quản lý | Quản lý nhân viên |
| `/manager/bills`, `/detail?id=`, `/cancel` | GET, POST | Quản lý | Quản lý hóa đơn |
| `/manager/stats/revenue`, `/manager/stats/top-drinks` | GET | Quản lý | Thống kê |

Quy tắc chung (BR-03, BR-04): mọi đường dẫn ngoài danh sách công khai đều yêu cầu đăng nhập. Đường dẫn `/manager/*` chỉ dành cho quản lý. Danh sách công khai: `/`, `/home`, `/auth/*`, `/css/*`, `/js/*`, `/images/*`.

---

## 5. QUY TẮC NGHIỆP VỤ

Mã BR-xx để tham chiếu từ issue và test case. Số liệu lấy từ code Lab 4–6 và đặc tả Lab 7–8.

### 5.1 Đăng nhập và phân quyền

| Mã | Quy tắc |
| :--- | :--- |
| BR-01 | Đăng nhập bằng email và mật khẩu. Thiếu một trong hai: báo "Vui lòng nhập email và mật khẩu". Email không tồn tại hoặc mật khẩu sai: báo "Email hoặc mật khẩu không đúng" (không nói rõ cái nào sai). |
| BR-02 | Tài khoản có `enabled = 0` bị từ chối với "Tài khoản đã bị khóa", kể cả khi nhập đúng mật khẩu. |
| BR-03 | Chưa đăng nhập mà truy cập trang không công khai: chuyển đến `/auth/login?redirect=<đường dẫn>`. |
| BR-04 | Nhân viên truy cập `/manager/*`: trả về HTTP 403, "Bạn không có quyền truy cập chức năng này". |
| BR-05 | Đăng nhập thành công: chuyển đến `redirect` nếu bắt đầu bằng `/` và không bắt đầu bằng `//`. Ngược lại chuyển đến `/home`. |
| BR-06 | Đăng xuất: hủy session, chuyển đến `/auth/login`. |
| BR-07 | Mật khẩu lưu dưới dạng SHA-256 hex (không có muối). So sánh bằng cách băm mật khẩu nhập vào. |

### 5.2 Trang chủ khách

| Mã | Quy tắc |
| :--- | :--- |
| BR-08 | Trang chủ chỉ hiển thị đồ uống có `available = 1`, nhóm theo loại. Không cần đăng nhập. |

### 5.3 Loại đồ uống (Lab 4)

| Mã | Quy tắc |
| :--- | :--- |
| BR-09 | Tên loại không được rỗng, tối đa 100 ký tự. Trùng tên với loại khác: báo "Tên loại đã tồn tại". |
| BR-10 | Không xóa loại còn đồ uống. Báo "Không thể xóa: loại này đang có đồ uống". |

### 5.4 Đồ uống (Lab 5)

| Mã | Quy tắc |
| :--- | :--- |
| BR-11 | Tên đồ uống không được rỗng. Đơn giá là số nguyên không âm. Giảm giá nằm trong khoảng 0–100% (lưu 0 đến 1). Phải chọn loại đồ uống tồn tại. |
| BR-12 | Không xóa đồ uống đã có trong hóa đơn (`bill_details`). Báo "Không thể xóa: đồ uống đã được bán (hãy chuyển sang ngừng bán)". Muốn ngừng bán thì đặt `available = 0`. |
| BR-13 | Tìm kiếm theo tên (chứa chuỗi), loại và trạng thái. Phân trang 10 dòng mỗi trang. |
| BR-14 | Ảnh đồ uống tải lên được lưu vào `webapp/images`. Ảnh sẽ mất khi deploy lại file WAR (ghi chú của Lab 5). |

### 5.5 Nhân viên (Lab 5)

| Mã | Quy tắc |
| :--- | :--- |
| BR-15 | Họ tên không được rỗng. Email đúng định dạng và duy nhất trong bảng `users`. Trùng email với tài khoản khác: báo "Email đã được sử dụng" (khi sửa thì bỏ qua chính tài khoản đó). |
| BR-16 | Khi thêm mới, mật khẩu tối thiểu 6 ký tự. |
| BR-17 | Không tự xóa tài khoản đang đăng nhập. Báo "Không thể tự xóa tài khoản đang đăng nhập". |
| BR-18 | Không xóa nhân viên đã có hóa đơn. Báo "Không thể xóa: nhân viên đã có dữ liệu (hãy khóa tài khoản)". |
| BR-19 | Cấp lại mật khẩu: sinh mật khẩu ngẫu nhiên 8 ký tự, lưu bản băm mới, rồi gửi email. Nếu gửi email lỗi thì khôi phục mật khẩu cũ và báo "Gửi email thất bại, mật khẩu chưa bị thay đổi". |
| BR-20 | Thông tin gửi mail (`MAIL_USER`, `MAIL_PASSWORD`) đọc từ biến môi trường, không ghi trong mã nguồn. |

### 5.6 Hóa đơn (Lab 6, Lab 7)

| Mã | Quy tắc |
| :--- | :--- |
| BR-21 | Danh sách hóa đơn: mới nhất trước, 10 dòng mỗi trang, có cột tổng tiền. |
| BR-22 | Tổng tiền hóa đơn = Σ (`quantity` × `unit_price` × (1 − `discount`)) trên các dòng chi tiết. Không lưu cột tổng trong `bills`. |
| BR-23 | Chi tiết hóa đơn hiển thị: đồ uống, số lượng, đơn giá, giảm giá, thành tiền, tổng, trạng thái, nhân viên tạo, ngày tạo. |
| BR-24 | Hủy hóa đơn: chỉ khi `status` khác 2. Thành công thì `status = 2`. Nếu không tồn tại hoặc đã hủy: báo "Không thể hủy hóa đơn (không tồn tại hoặc đã hủy)". |
| BR-25 | *(Đặc tả Lab 7, chưa có code trong Lab 1–6)* Phiếu mới có `status = 0` và thuộc nhân viên đang đăng nhập. Thêm đồ uống: số lượng là số nguyên dương. Thêm đồ uống đã có trong phiếu thì cộng dồn số lượng, không tạo dòng trùng. |
| BR-26 | *(Đặc tả Lab 7)* Thanh toán: `status = 1`, ghi `checkout_at`. Phiếu đã thanh toán không sửa được (kể cả khi gửi yêu cầu trực tiếp). |
| BR-27 | *(Đặc tả Lab 7)* Không thêm đồ uống đang ngừng bán vào phiếu. |
| BR-28 | *(Đặc tả Lab 7)* Nhân viên chỉ xem được lịch sử phiếu do mình tạo. |

### 5.7 Thống kê (Lab 6)

| Mã | Quy tắc |
| :--- | :--- |
| BR-29 | Chỉ tính hóa đơn `status = 1`, theo ngày thanh toán (`checkout_at`). |
| BR-30 | Khoảng thời gian nhập theo `yyyy-MM-dd`. Ngày sai định dạng: "Ngày không hợp lệ (định dạng yyyy-MM-dd)". Từ ngày lớn hơn đến ngày: "Từ ngày phải nhỏ hơn hoặc bằng đến ngày". Ngày đến được tính đến hết ngày. Mặc định là 30 ngày gần nhất. |
| BR-31 | Top 5 đồ uống bán chạy: sắp theo số lượng giảm dần, hòa thì theo doanh thu giảm dần. |
| BR-32 | Doanh thu theo ngày hiển thị bằng biểu đồ cột (Chart.js). |

---

## 6. YÊU CẦU VALIDATE

Mỗi trường được kiểm tra ở hai nơi: **client** (thuộc tính HTML5 như `required`, `type="email"`, `min`, `max`) và **server** (trong Servlet). Server là nơi quyết định, client chỉ để phản hồi nhanh.

| Trường | Điều kiện | Thông báo (server) |
| :--- | :--- | :--- |
| Email đăng nhập | Không rỗng | Vui lòng nhập email và mật khẩu |
| Mật khẩu đăng nhập | Không rỗng | Vui lòng nhập email và mật khẩu |
| Tên loại | Không rỗng, ≤ 100 ký tự, duy nhất | Tên loại không được để trống / tối đa 100 ký tự / Tên loại đã tồn tại |
| Tên đồ uống | Không rỗng | Tên đồ uống không được để trống |
| Đơn giá | Số nguyên ≥ 0 | Đơn giá phải là số không âm |
| Giảm giá | 0 ≤ giá trị ≤ 100% | Giảm giá phải từ 0 đến 100% |
| Loại đồ uống | Phải tồn tại | Vui lòng chọn loại đồ uống |
| Họ tên nhân viên | Không rỗng | Họ tên không được để trống |
| Email nhân viên | Đúng định dạng `^[\w.+-]+@[\w-]+(\.[\w-]+)+$`, duy nhất | Email không hợp lệ / Email đã được sử dụng |
| Mật khẩu nhân viên (thêm mới) | ≥ 6 ký tự | Mật khẩu tối thiểu 6 ký tự |
| Ngày thống kê | Định dạng `yyyy-MM-dd`, từ ≤ đến | Ngày không hợp lệ / Từ ngày phải nhỏ hơn hoặc bằng đến ngày |
| Số lượng dòng hóa đơn *(Lab 7)* | Số nguyên > 0 | Số lượng phải là số nguyên dương |

---

## 7. YÊU CẦU PHI CHỨC NĂNG

| Mã | Nội dung | Ghi chú |
| :--- | :--- | :--- |
| NFR-01 | Mọi truy vấn dùng `PreparedStatement` | Chống SQL Injection |
| NFR-02 | Tài nguyên JDBC được đóng bằng `try-with-resources` | |
| NFR-03 | Lỗi CSDL ném `RuntimeException` về Servlet, không nuốt lỗi | Khác với code console hiện tại (R-01) |
| NFR-04 | Thông tin kết nối CSDL và mật khẩu mail không nằm trong mã nguồn | Lab 3 còn ghi cứng, cần chuyển sang cấu hình |
| NFR-05 | Giao diện dùng được trên máy tính (1440px) và điện thoại (375px) | Theo tiêu chí giao diện của dự án |
| NFR-06 | Post-Redirect-Get sau mọi thao tác ghi | Tránh gửi lại form khi tải lại trang |
| NFR-07 | Dữ liệu người dùng hiển thị bằng `<c:out>` | Chống XSS |

---

## 8. GIAO DIỆN (TÓM TẮT)

| Màn hình | Đường dẫn | Ai dùng |
| :--- | :--- | :--- |
| Đăng nhập | `/auth/login` | Tất cả |
| Trang chủ (thực đơn) | `/home` | Khách, nhân viên, quản lý |
| Quản lý loại đồ uống | `/manager/categories` | Quản lý |
| Quản lý đồ uống | `/manager/drinks` | Quản lý |
| Quản lý nhân viên | `/manager/staff` | Quản lý |
| Quản lý hóa đơn | `/manager/bills` | Quản lý |
| Thống kê doanh thu | `/manager/stats/revenue` | Quản lý |
| Top đồ uống bán chạy | `/manager/stats/top-drinks` | Quản lý |

Phần đầu và chân trang dùng chung nằm trong `_header.jspf` và `_footer.jspf`.

---

## 9. RỦI RO (GIẢ THUYẾT LỖI)

| Mã | Rủi ro | Căn cứ | Mức |
| :--- | :--- | :--- | :---: |
| **R-01** | Mã console hiện tại nuốt lỗi SQL: `insert` trả `0`, `findAll` trả `[]`, không phân biệt được với "không có dữ liệu" | `CategoryDAO.java` | Cao |
| **R-02** | Mật khẩu MySQL (`root`) được ghi cứng trong `JdbcUtil.java` và repo đang PUBLIC | `JdbcUtil.java`, repo `nhan0x/SOF3033_...` | Cao |
| **R-03** | Lab 3 ghi cứng mật khẩu SQL Server trong `JdbcUtil`. Khi chuyển sang MySQL, dễ chép lại cách làm này | `Lab_3/.../JdbcUtil.java` | Cao |
| **R-04** | Mật khẩu băm SHA-256 không có muối. Bảng băm tra cứu sẵn có thể đoán được mật khẩu yếu như `123456` | BR-07, DDL `users` | Trung bình |
| **R-05** | Hủy được hóa đơn đã thanh toán (BR-24 chỉ chặn `status = 2`). Điều này ảnh hưởng doanh thu đã báo cáo | `BillDAO.cancel` | Trung bình |
| **R-06** | Ảnh tải lên lưu trong `webapp/images` nên mất khi deploy lại WAR | Lab 5 README | Trung bình |
| **R-07** | Lệch phiên bản Servlet: `pom.xml` dùng `jakarta`, Lab dùng `javax` và Tomcat 9 | `pom.xml` | Cao |
| **R-08** | Entity hiện tại không khớp DDL MySQL (xem mục 10.2). Ví dụ `Category.active`, `Bill.code`, `Bill.total` không có trong DDL | `src/main/.../Entity/` | Cao |
| **R-09** | Schema và DDL MySQL chưa được chạy thử. DDL được chuyển đổi tay từ SQL Server | mục 3.2 | Trung bình |
| **R-10** | Repo chưa có script CSDL (N2 của đề) và chưa có `README` cài đặt (N4) | Thư mục gốc | Cao |

---

## 10. TRẠNG THÁI SO VỚI ĐỀ VÀ MÃ NGUỒN

### 10.1 Đối chiếu đề JAV102 (Dự án Java 1)

Đề yêu cầu Y1–Y4 (chức năng) và N1–N4 (sản phẩm nộp). Cột "Bài Lab" là trạng thái trong bài làm Lab 1–8 (ngoài repo). Cột "Repo" là trạng thái trong `src/main/` của repo này.

| Mã | Yêu cầu | Bài Lab | Repo |
| :--- | :--- | :--- | :--- |
| Y1 | Đăng nhập / đăng xuất | Có (Lab 4) | Chưa |
| Y2 | Phân quyền, ít nhất 2 role | Có (Lab 4: quản lý và nhân viên, `AuthFilter`) | Chưa |
| Y3 | CRUD tối thiểu 3 thực thể | Có: loại, đồ uống, nhân viên đủ CRUD. Hóa đơn chỉ xem và hủy (Lab 6) | Một phần: `Category` có thêm và đọc |
| Y4 | Validate client và server | Có (Lab 4–6) | Chưa |
| N1 | Mã nguồn | Có | Có một phần (console) |
| N2 | CSDL dạng script có dữ liệu | Có (`Lab_3.sql`, SQL Server) | Chưa (MySQL, xem R-10) |
| N3 | File zip thư mục Publish | Chưa xác nhận | Chưa |
| N4 | Tài liệu cài đặt và triển khai | Có (Lab 8, Bài 4) | Chưa |

> **Thang đánh giá của đề:** A cần Y1–Y4, B cần Y1–Y3, C cần N1–N2, D là không đạt.

### 10.2 Mã nguồn `src/main/` hiện tại

| Thành phần | Trạng thái | Chênh với đặc tả |
| :--- | :--- | :--- |
| `Main.java` | Có, console, in danh sách loại | Sẽ thay bằng Servlet |
| `Util/JdbcUtil.java` | Có, MySQL, ghi cứng mật khẩu | NFR-04, R-02 |
| `DAO/CategoryDAO.java` | `insert` và `findAll` | Thiếu `findById`, cập nhật, xóa, đếm đồ uống, kiểm tra trùng tên (BR-09, BR-10). Đọc và ghi cột `active` không có trong DDL (R-08) |
| `Entity/Category.java` | Có | Cột `active` không có trong DDL |
| `Entity/Drink.java` | Có | Dùng `active` thay cho `available`. Thiếu `discount`. Có `description` không có trong DDL |
| `Entity/Bill.java` | Có | Có `code`, `total` không có trong DDL. `status` kiểu `String`, DDL dùng `INT` |
| `Entity/BillDetail.java` | Có | Trường `price` đổi thành `unit_price`, thiếu `discount` |
| `Entity/User.java` | **Chưa có** | Cần cho Y1, Y2 |
| Servlet, Filter, JSP | **Chưa có** | Y1–Y4, mục 4 |
| Test | Chưa có | Mục 9 |

---

## 11. GIẢ ĐỊNH VÀ GIỚI HẠN

- DDL ở mục 3.2 là bản chuyển đổi từ Lab, chưa chạy thử trên MySQL. Các ca kiểm thử ràng buộc (`NOT NULL`, `UNIQUE`, `CHECK`) phải chờ DDL được xác nhận.
- Đặc tả BR-25 đến BR-28 (phiếu bán hàng) lấy từ Lab 7–8, chưa có code trong Lab 1–6. Cần thống nhất trước khi viết test.
- Môi trường kiểm thử dùng MySQL 8 cục bộ với cơ sở dữ liệu riêng (ví dụ `polycoffee_test`), không dùng dữ liệu thật.
- Không kiểm thử hiệu năng, bảo mật chuyên sâu, thanh toán điện tử, in hóa đơn, hay dịch vụ gửi mail của bên thứ ba.
