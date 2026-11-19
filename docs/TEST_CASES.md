# BỘ KỊCH BẢN KIỂM THỬ (TEST CASES)

> Sinh ra từ [TEST_PLAN.md](TEST_PLAN.md) và [SPECIFICATIONS.md](SPECIFICATIONS.md). Mọi **kết quả mong đợi** lấy từ hành vi trong code, không suy đoán.

* **Dự án:** PolyCoffee · **Môi trường:** JDK 17, MySQL 8 cục bộ, DB test `polycoffee_test` (không dùng `polycoffee`) · **Số ca:** 19
* **Ngày thiết kế:** 19/11/2026 · **Trạng thái ban đầu:** *Chưa chạy*

## CÁCH ĐỌC BẢNG

| Cột | Ý nghĩa |
| :--- | :--- |
| **P · Kỹ thuật · Tầng** | **P1** bắt buộc đạt; **P2** hồi quy; **P3** chấp nhận rủi ro. **Kỹ thuật:** `EP` phân vùng tương đương, `BVA` giá trị biên, `EG` đoán lỗi, `SEC` an toàn, `ST` tĩnh. **Tầng:** `Unit` (JUnit, không DB), `Integ` (JUnit + MySQL thật), `Out` (so sánh đầu ra console), `Tĩnh` (tìm trong mã nguồn) |
| **Loại** | Positive / Negative |
| **🔶** | *Giả thuyết lỗi*: ca nhằm xác nhận rủi ro R-xx trong SPECIFICATIONS mục 9. Kết quả khác mong đợi thì ghi nhận là **lỗi thật** |
| **Phụ thuộc DDL** | Kết quả phụ thuộc ràng buộc của bảng `categories` chưa được xác nhận. Phải kiểm tra DDL thật trước khi chạy |

---

## 1. ENTITY (4 CA)

| Test Case ID | Chức năng | Mô tả ca kiểm thử | Loại | Các bước thực hiện | Dữ liệu kiểm thử | Kết quả mong đợi | P · Kỹ thuật · Tầng | Trạng thái |
| :---: | :---: | :--- | :---: | :--- | :--- | :--- | :---: | :---: |
| **TC_ENT_01** | Entity Category | Constructor không tham số cho giá trị mặc định | Positive | 1. `new Category()`<br>2. Đọc từng trường | Không có | `getId()` là `null`; `getName()` là `null`; `isActive()` là `false` | P2 · EP · Unit | Chưa chạy |
| **TC_ENT_02** | Entity Category | Constructor đủ tham số và getter của `boolean` | Positive | 1. `new Category(1, "Cà phê", true)`<br>2. Gọi `getId()`, `getName()`, `isActive()` | `1`; `Cà phê`; `true` | Trả về đúng `1`, `Cà phê`, `true`. Getter boolean là `isActive()` | P2 · EP · Unit | Chưa chạy |
| **TC_ENT_03** | Entity Category | `equals` và `hashCode` theo giá trị trường | Positive | 1. Tạo hai `Category` cùng dữ liệu<br>2. So sánh `equals` và `hashCode`<br>3. Đổi `name` của một bên, so sánh lại | `(1, "Cà phê", true)` hai lần; sau đó đổi thành `"Trà"` | Bước 2: `equals` là `true`, `hashCode` bằng nhau. Bước 3: `equals` là `false` | P2 · EP · Unit | Chưa chạy |
| **TC_ENT_04** | Entity Drink, Bill, BillDetail | Constructor đủ tham số giữ đúng thứ tự trường | Positive | 1. `new Drink(1, 2, "Latte", "Ngọt", "a.jpg", 45000, true)`<br>2. `new BillDetail(1, 10, 3, 2, 45000)`<br>3. Đọc từng getter | Giá trị như cột bên | `getCategoryId()` của Drink là `2`; `getDrinkId()` của BillDetail là `3`; `getQuantity()` là `2`. Không lẫn thứ tự | P2 · EP · Unit | Chưa chạy |

---

## 2. KẾT NỐI CSDL (2 CA)

| Test Case ID | Chức năng | Mô tả ca kiểm thử | Loại | Các bước thực hiện | Dữ liệu kiểm thử | Kết quả mong đợi | P · Kỹ thuật · Tầng | Trạng thái |
| :---: | :---: | :--- | :---: | :--- | :--- | :--- | :---: | :---: |
| **TC_DB_01** | Kết nối | Mở kết nối khi MySQL đang chạy và cấu hình đúng | Positive | 1. Bảo đảm MySQL chạy ở cổng 3306<br>2. Gọi `JdbcUtil.getConnection()`<br>3. Kiểm tra `isValid(2)` rồi đóng | Cấu hình hiện tại trong `JdbcUtil` | Trả về `Connection` không `null`; `isValid(2)` là `true`; không ném `SQLException` | P1 · EP · Integ | Chưa chạy |
| **TC_DB_02** | Kết nối | Mở kết nối khi MySQL không chạy | Negative | 1. Tạm dừng MySQL (`brew services stop mysql`)<br>2. Gọi `JdbcUtil.getConnection()`<br>3. Khởi động lại MySQL | Không có | Ném `SQLException`. Không tạo kết nối rỗng. Ghi chú: `JdbcUtil` không có lớp xử lý lỗi | P2 · EG · Integ | Chưa chạy |

---

## 3. DAO: `CategoryDAO.insert` (6 CA)

| Test Case ID | Chức năng | Mô tả ca kiểm thử | Loại | Các bước thực hiện | Dữ liệu kiểm thử | Kết quả mong đợi | P · Kỹ thuật · Tầng | Trạng thái |
| :---: | :---: | :--- | :---: | :--- | :--- | :--- | :---: | :---: |
| **TC_CAT_01** | Thêm danh mục | Thêm danh mục hợp lệ, `active = true` | Positive | 1. Làm rỗng `categories` trong `polycoffee_test`<br>2. `insert(new Category(null, "Cà phê", true))`<br>3. Đếm dòng và đọc lại | `Cà phê`, `true` | Trả về `1`. Bảng có đúng 1 dòng với `name = "Cà phê"`, `active = 1` | P1 · EP · Integ | Chưa chạy |
| **TC_CAT_02** | Thêm danh mục | Thêm danh mục với `active = false` | Positive | 1. `insert(new Category(null, "Trà", false))`<br>2. Đọc dòng vừa thêm | `Trà`, `false` | Trả về `1`. Cột `active` lưu `0` | P2 · EP · Integ | Chưa chạy |
| **TC_CAT_03** | Thêm danh mục | Chuỗi chứa SQL injection được lưu như văn bản thường | Negative | 1. `insert` với `name = "x'); DROP TABLE categories; --"`<br>2. Đếm bảng `categories`<br>3. Đọc lại `name` | `x'); DROP TABLE categories; --` | Trả về `1`. Bảng `categories` vẫn tồn tại. `name` đọc lại đúng nguyên chuỗi đã nhập | P1 · SEC · Integ | Chưa chạy |
| **TC_CAT_04** | Thêm danh mục | Thêm danh mục với `name = null` | Negative | 1. `insert(new Category(null, null, true))` | `name = null` | Phụ thuộc DDL. Nếu cột `NOT NULL`: trả về `0`, in stack trace, không ném lỗi. Nếu cột cho phép null: trả về `1` | P2 · BVA · Integ | Chưa chạy. **Phụ thuộc DDL** |
| **TC_CAT_05** | Thêm danh mục | Thêm hai danh mục trùng tên | Negative | 1. `insert(new Category(null, "Cà phê", true))` hai lần<br>2. Đếm dòng | `Cà phê` × 2 | Phụ thuộc DDL. Nếu `name` là `UNIQUE`: lần 2 trả về `0`. Nếu không: cả hai trả về `1` và bảng có 2 dòng | P2 · EP · Integ | Chưa chạy. **Phụ thuộc DDL** |
| **TC_CAT_06** 🔶 | Thêm danh mục | **R-01** Lỗi CSDL khi thêm bị nuốt, trả về `0` | Negative | 1. Tạm dừng MySQL<br>2. `insert(new Category(null, "Trà", true))`<br>3. Khởi động lại MySQL | `Trà`, `true` | Trả về `0`, không ném lỗi. Người gọi không phân biệt được với "không thêm được dòng nào" | P1 · EG · Integ | Chưa chạy |

---

## 4. DAO: `CategoryDAO.findAll` (4 CA)

| Test Case ID | Chức năng | Mô tả ca kiểm thử | Loại | Các bước thực hiện | Dữ liệu kiểm thử | Kết quả mong đợi | P · Kỹ thuật · Tầng | Trạng thái |
| :---: | :---: | :--- | :---: | :--- | :--- | :--- | :---: | :---: |
| **TC_CAT_07** | Xem danh mục | Bảng rỗng trả về danh sách rỗng, không phải `null` | Positive | 1. Làm rỗng `categories`<br>2. Gọi `findAll()` | Không có | Trả về danh sách không `null`, `size() == 0` | P1 · BVA · Integ | Chưa chạy |
| **TC_CAT_08** | Xem danh mục | Đọc đủ dòng và ánh xạ đúng từng trường | Positive | 1. Chèn 3 dòng: `(Cà phê, 1)`, `(Trà, 1)`, `(Bánh, 0)`<br>2. Gọi `findAll()`<br>3. Kiểm tra từng phần tử | Ba danh mục như cột bên | `size() == 3`. Mỗi phần tử có `id` khác `null`, `name` đúng, `isActive()` đúng (`true`, `true`, `false`) | P1 · EP · Integ | Chưa chạy |
| **TC_CAT_09** 🔶 | Xem danh mục | **R-01** Lỗi CSDL khi đọc bị nuốt, trả về danh sách rỗng | Negative | 1. Chèn 2 dòng<br>2. Tạm dừng MySQL<br>3. Gọi `findAll()`<br>4. Khởi động lại MySQL | Không có | Trả về danh sách rỗng, không ném lỗi. **Không phân biệt được với bảng rỗng thật** | P1 · EG · Integ | Chưa chạy |
| **TC_CAT_10** 🔶 | Xem danh mục | **R-07** Thứ tự kết quả không được đảm bảo | Positive | 1. Chèn 5 dòng theo thứ tự<br>2. Gọi `findAll()` nhiều lần | 5 danh mục | Thứ tự phụ thuộc CSDL, không có `ORDER BY`. Ghi nhận nếu thứ tự thay đổi giữa các lần gọi | P3 · EG · Integ | Chưa chạy |

---

## 5. MAIN (2 CA)

| Test Case ID | Chức năng | Mô tả ca kiểm thử | Loại | Các bước thực hiện | Dữ liệu kiểm thử | Kết quả mong đợi | P · Kỹ thuật · Tầng | Trạng thái |
| :---: | :---: | :--- | :---: | :--- | :--- | :--- | :---: | :---: |
| **TC_MAIN_01** | Hiển thị danh mục | Chạy `Main` với 2 danh mục | Positive | 1. Chèn `(Cà phê, true)`, `(Trà, false)`<br>2. Bắt `System.out`<br>3. Gọi `Main.main(new String[0])` | 2 danh mục | Đầu ra có 6 dòng theo thứ tự: `id`, `Cà phê`, `true`, `id`, `Trà`, `false` (`id` là số tự tăng) | P1 · EP · Out | Chưa chạy |
| **TC_MAIN_02** | Hiển thị danh mục | Chạy `Main` khi bảng rỗng | Positive | 1. Làm rỗng `categories`<br>2. Bắt `System.out`<br>3. Gọi `Main.main` | Không có | Không in dòng nào. Chương trình kết thúc bình thường, không ném lỗi | P2 · BVA · Out | Chưa chạy |

---

## 6. TĨNH: MÃ NGUỒN (1 CA)

| Test Case ID | Chức năng | Mô tả ca kiểm thử | Loại | Các bước thực hiện | Dữ liệu kiểm thử | Kết quả mong đợi | P · Kỹ thuật · Tầng | Trạng thái |
| :---: | :---: | :--- | :---: | :--- | :--- | :--- | :---: | :---: |
| **TC_SEC_01** 🔶 | Bảo mật cấu hình | **R-02** Không có mật khẩu CSDL được hardcode trong mã nguồn | Negative | 1. Tìm chuỗi gán `password` trong `src/main/java/com/polycoffee/Util/JdbcUtil.java`<br>2. Kiểm tra `git log -p` có chứa mật khẩu đó | Mật khẩu hiện có trong `JdbcUtil` | **Dự kiến Fail hiện tại**: `JdbcUtil` có mật khẩu hardcode và đã nằm trong commit `e640f1d` trên `origin`. Kết quả đạt chỉ khi mật khẩu được chuyển ra ngoài mã nguồn và đã đổi | P1 · SEC · Tĩnh | Chưa chạy |

---

## TỔNG HỢP

| Nhóm | Số ca | Ca P1 | Ca 🔶 | Ca phụ thuộc DDL |
| :--- | :---: | :---: | :---: | :---: |
| Entity | 4 | 0 | 0 | 0 |
| Kết nối CSDL | 2 | 1 | 0 | 0 |
| `CategoryDAO.insert` | 6 | 3 | 1 | 2 |
| `CategoryDAO.findAll` | 4 | 3 | 2 | 0 |
| Main | 2 | 1 | 0 | 0 |
| Tĩnh | 1 | 1 | 1 | 0 |
| **Tổng** | **19** | **9** | **4** | **2** |

> Ca phụ thuộc DDL: TC_CAT_04 (`name` null) và TC_CAT_05 (trùng tên). Hai ca này phải được xác nhận bằng DDL thật trước khi chạy.
