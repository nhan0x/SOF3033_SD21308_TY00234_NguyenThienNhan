# KẾ HOẠCH KIỂM THỬ (TEST PLAN)

> Đặc tả: [SPECIFICATIONS.md](SPECIFICATIONS.md) · Test case: [TEST_CASES.md](TEST_CASES.md)

| Mục | Giá trị |
| :--- | :--- |
| Dự án | PolyCoffee (Java 17, Maven, JDBC, MySQL 8, Lombok) |
| Người phụ trách | Nguyễn Thiện Nhân · TY00234 · SD21308 (`nhan0x`) |
| Phiên bản | 3.0 (19/11/2026). Thay thế bản 2.0 viết cho dự án Saltair |

---

## 1. MỤC TIÊU

1. Xác nhận **FR-01** (xem danh sách danh mục) và **FR-02** (thêm danh mục ở tầng DAO) hoạt động đúng với CSDL thật.
2. Xác nhận các Entity ánh xạ đúng dữ liệu và constructor, getter, setter do Lombok sinh.
3. Làm rõ các rủi ro đã nêu trong [SPECIFICATIONS.md](SPECIFICATIONS.md) mục 9 (R-01 đến R-08) bằng test có kết quả đo được, không chỉ bằng suy đoán.

## 2. PHẠM VI

| Trong phạm vi | Ngoài phạm vi |
| :--- | :--- |
| `CategoryDAO.insert`, `CategoryDAO.findAll` | `Drink`, `Bill`, `BillDetail` có DAO (chưa được triển khai) |
| `JdbcUtil.getConnection` | Giao diện người dùng (ứng dụng không có) |
| Entity `Category`, `Drink`, `Bill`, `BillDetail` (cấu trúc do Lombok sinh) | Kiểm thử hiệu năng, tải |
| `Main.main` (đầu ra in ra console) | Bảo mật CSDL ngoài việc kiểm tra mật khẩu hardcode (R-02) |
| Hành vi khi CSDL không khả dụng | Triển khai, deploy |

## 3. CHIẾN LƯỢC KIỂM THỬ

| Tầng | Công cụ | Đối tượng | Lý do |
| :--- | :--- | :--- | :--- |
| Đơn vị (Unit) | JUnit 5 | Entity | Không cần CSDL, chạy nhanh |
| Tích hợp (Integration) | JUnit 5 + MySQL thật | `CategoryDAO`, `JdbcUtil` | `JdbcUtil` gọi `DriverManager` trực tiếp, không có cách mock gọn gàng. Test chạy trên DB riêng `polycoffee_test` |
| Đầu ra (Output) | JUnit 5 + bắt `System.out` | `Main` | `Main` không có tham số, kiểm tra bằng cách so sánh đầu ra |
| Tĩnh (Static) | Tìm kiếm trong mã nguồn | `JdbcUtil` (R-02) | Kiểm tra không có mật khẩu hardcode |

> **Mock không được dùng cho DAO** vì `JdbcUtil` không có điểm tiêm phụ thuộc. Nếu muốn cô lập DAO khỏi CSDL, cần refactor `JdbcUtil` nhận `DataSource` hoặc `Connection`. Việc này nằm ngoài phạm vi kế hoạch này.

## 4. MÔI TRƯỜNG

| Thành phần | Giá trị |
| :--- | :--- |
| JDK | **17** (không dùng JDK 27: Lombok lỗi, xem R-08) |
| Maven | 3.10+, lệnh `JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn test` |
| MySQL | 8, chạy cục bộ, cổng **3306** |
| Cơ sở dữ liệu | `polycoffee` (dữ liệu thật của sinh viên) và `polycoffee_test` (chỉ dùng cho test) |
| Tài khoản | Chỉ dùng tài khoản test. Không dùng `root` cho test sau khi R-02 được xử lý |
| Schema test | Tạo bảng `categories` theo suy luận ở SPECIFICATIONS mục 3, có xác nhận trước khi chạy |

> **Lưu ý:** `JdbcUtil` hiện trỏ tới `polycoffee`, không phải `polycoffee_test`. Để chạy test an toàn, cần tách cấu hình test trước khi chạy tích hợp, nếu không test sẽ ghi vào dữ liệu thật.

---

## 5. KHÔNG GIAN ĐẦU VÀO VÀ KỸ THUẬT THIẾT KẾ CA

Nguyên lý ISTQB: không thể kiểm thử vét cạn, nên chọn mẫu bằng các kỹ thuật sau.

| Kỹ thuật | Áp dụng |
| :--- | :--- |
| Phân vùng tương đương (EP) | `name`: hợp lệ, rỗng, null, có ký tự đặc biệt. `active`: `true`, `false` |
| Giá trị biên (BVA) | `name` ở độ dài 1, và độ dài tối đa của cột (khi biết DDL) |
| Đoán lỗi (EG) | Lỗi CSDL bị nuốt (R-01), chuỗi SQL injection (kiểm chứng NFR-01) |
| Đơn lỗi (Single fault) | Mỗi ca chỉ thay đổi một yếu tố so với ca cơ sở |

Ca đánh dấu 🔶 là **giả thuyết lỗi**. Nếu kết quả thực tế khác kết quả mong đợi thì ghi nhận là lỗi thật.

---

## 6. PHÂN BỔ CA KIỂM THỬ

| Nhóm | Số ca | Tỷ lệ | Ghi chú |
| :--- | :---: | :---: | :--- |
| Entity | 4 | 21% | Rủi ro thấp, kiểm tra nhanh |
| Kết nối CSDL | 2 | 11% | Nền tảng cho mọi ca tích hợp |
| `CategoryDAO.insert` | 6 | 32% | Có tham số hóa, có lỗi bị nuốt, có phụ thuộc DDL |
| `CategoryDAO.findAll` | 4 | 21% | Có lỗi bị nuốt, có bảng rỗng |
| `Main` | 2 | 11% | Kiểm tra đầu ra |
| Tĩnh (mã nguồn) | 1 | 5% | Phát hiện R-02 |

Tổng: **19 ca** (xem [TEST_CASES.md](TEST_CASES.md)). Trong đó có **2 ca phụ thuộc DDL** (TC_CAT_04, TC_CAT_05) cần xác nhận schema trước khi chạy.

> Vì project nhỏ, không áp dụng nguyên lý *Defect Clustering* theo module như bản Saltair. Rủi ro tập trung ở `CategoryDAO` (R-01, R-03) và `JdbcUtil` (R-02), nên phần lớn ca tập trung ở hai nơi này.

---

## 7. TIÊU CHÍ

| Loại | Điều kiện |
| :--- | :--- |
| Bắt đầu kiểm thử | `mvn clean compile` thành công trên JDK 17. Schema `polycoffee_test` đã được tạo |
| Hoàn thành kiểm thử | Mọi ca P1 đạt. Mọi ca 🔶 đã có kết luận (đạt hoặc ghi nhận lỗi) |
| Đình chỉ | Lỗi làm hỏng schema test hoặc ghi nhầm vào `polycoffee` |

**Mức ưu tiên:** P1 là ca bắt buộc đạt. P2 là ca hồi quy. P3 là ca chấp nhận rủi ro.

---

## 8. RỦI RO KIỂM THỬ

| Rủi ro | Cách giảm |
| :--- | :--- |
| Test ghi vào dữ liệu thật vì `JdbcUtil` trỏ `polycoffee` | Tách cấu hình test. Trước mắt, chỉ chạy trên máy cá nhân với DB test |
| Schema suy ra không khớp CSDL thật | Xác nhận DDL trước khi viết ca phụ thuộc DDL |
| Mật khẩu `root` đã lộ trong repo public | Đổi mật khẩu MySQL và chuyển cấu hình ra ngoài mã nguồn (xem R-02) |
| Lombok lỗi trên JDK không tương thích | Ghi rõ JDK 17 trong môi trường (mục 4) |

---

## 9. LỊCH TRÌNH

| Giai đoạn | Nội dung |
| :--- | :--- |
| 1 | Xác nhận DDL bảng `categories`, dựng `polycoffee_test` |
| 2 | Viết và chạy test Entity và kết nối |
| 3 | Viết và chạy test `CategoryDAO` và `Main` |
| 4 | Kiểm tra tĩnh R-02, ghi nhận kết quả |
| 5 | Tổng hợp kết quả, cập nhật cột Trạng thái trong TEST_CASES |
