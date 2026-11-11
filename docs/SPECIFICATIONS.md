# ĐẶC TẢ NGHIỆP VỤ WEB CÁ NHÂN (SPECIFICATIONS)
> *Dùng làm ngữ cảnh (Context) gửi cho AI để sinh Test Plan & Test Case chính xác theo dự án riêng của bạn.*

---

## 1. THÔNG TIN DỰ ÁN
* **Tên đề tài:** Saltair - Website bán quần áo basic unisex (thương hiệu giả, chỉ dùng để học)
* **Sinh viên:** Nguyễn Thiện Nhân - TY00234 - SD21308
* **Công nghệ:** Java 17, Jakarta Servlet 6.1 / JSP 4 / JSTL 3, JDBC `PreparedStatement`, MySQL 8, Apache Tomcat 11, Maven (WAR), HTML/CSS/JS thuần
* **Kiến trúc:** MVC + Service + Repository. Luồng bắt buộc `Servlet -> Service -> Repository -> MySQL`; Servlet không viết SQL, JSP không gọi Service/Repository. Package chia theo tầng rồi theo chức năng (`auth`, `account`, `shop`, `cart`, `order`, `admin`).
* **URL chạy local:** `http://localhost:8080/saltair/home` · Trang quản trị: `http://localhost:8080/saltair/admin`
* **Chạy:** tạo DB bằng `database/schema.sql` rồi `database/data.sql`; chép `db.properties`, `mail.properties`, `cloudinary.properties` từ file `.example` trong `src/main/resources/` (không commit, repo Public); `mvn package` rồi deploy `target/saltair.war`.
* **Test:** `mvn test` (JUnit 5); GitHub Actions chạy `mvn clean test` khi push hoặc mở PR vào `main`.

## 2. QUY TẮC NGHIỆP VỤ (BUSINESS RULES)

### 2.1. Vai trò
| Vai trò | Lưu trong DB | Quyền |
| :--- | :--- | :--- |
| Guest | Không | Xem hàng, tìm kiếm, giỏ hàng và đặt hàng không cần đăng nhập |
| Customer | `users.role = CUSTOMER` | Như Guest + tài khoản, sổ địa chỉ, yêu thích, lịch sử đơn, đánh giá |
| Admin | `users.role = ADMIN` | Trang quản trị `/admin/*` (khách vào bị 403) |

### 2.2. Đăng nhập bằng email + mã 6 số (không có mật khẩu)
* **Khách** (`/login`, khung tài khoản ở header): nhập email → nhận mã 6 số → nhập mã.
  * Email sai định dạng hoặc dài quá 150 ký tự → "Email không đúng định dạng."
  * Mã hết hạn sau **15 phút**; sai **5 lần** thì mã bị hủy.
  * Gửi lại mã sau ít nhất **60 giây**, tối đa **5 mã/giờ** cho một email → vượt thì "Bạn thử lại sau ít phút."
  * Email chưa có tài khoản → nhập đúng mã thì tự tạo tài khoản CUSTOMER (họ tên = phần trước `@`).
  * Tài khoản bị khóa → "Tài khoản đã bị khóa..."; email Admin → báo chuyển sang `/admin/login`.
  * Đăng nhập xong quay về trang đang xem; giỏ hàng của khách vãng lai được gộp vào giỏ tài khoản.
* **Admin** (`/admin/login`): mã hết hạn sau **5 phút**, sai **3 lần** thì hủy, tối đa **3 mã/giờ**; luôn báo cùng một câu dù email có phải admin hay không; phiên hết hạn sau **30 phút** không thao tác.
* DB chỉ lưu băm SHA-256 của mã, email đang chờ nằm trong session (không lên URL).
* Thử nghiệm: bật `auth.devCode=true` trong `mail.properties` thì mã hiện trên trang nhập mã (tài khoản mẫu là email giả).

### 2.3. Sản phẩm & cửa hàng
* 4 danh mục (Áo thun và Polo, Áo nỉ và Hoodie, Áo khoác, Quần), 24 sản phẩm; mỗi sản phẩm có biến thể **màu + size** (S, M, L, XL) và tồn kho riêng.
* Danh sách: phân trang 12 sản phẩm/trang, lọc danh mục, khoảng giá, màu, size; sắp xếp mới nhất, bán chạy, đánh giá, giá tăng/giảm; tìm theo tên (tìm nhanh khi gõ từ 2 ký tự).
* Chi tiết: ảnh, mô tả, giá, màu, size, tồn kho, đánh giá; hết hàng thì không chọn được size đó.

### 2.4. Giỏ hàng & đặt hàng
* Thêm vào giỏ theo màu + size; số lượng ≥ 1 và không vượt tồn kho.
* Khách chưa đăng nhập: giỏ lưu trong session; khi đặt hàng **bắt buộc email**, đơn gắn vào tài khoản của email đó (chưa có thì tạo CUSTOMER), từ chối email Admin hoặc tài khoản bị khóa.
* Đặt hàng COD trong một transaction: trừ tồn kho, lưu mã voucher, số tiền giảm và phí ship tại thời điểm đặt.
* **Phí ship cố định 30.000đ**; dùng voucher hợp lệ thì phí ship = 0. `Tổng = tiền hàng − giảm giá + phí ship`.

### 2.5. Voucher (kiểm tra theo đúng thứ tự, gặp lỗi đầu tiên thì dừng)
1. Mã không tồn tại → 2. Bị vô hiệu hóa → 3. Chưa đến hạn hoặc quá hạn → 4. Hết tổng lượt dùng → 5. Khách đã dùng đủ số lần cho phép → 6. Chưa đạt đơn tối thiểu → 7. Đơn không có sản phẩm thuộc danh mục áp dụng.
* `PERCENT` (1 đến 100, có thể giới hạn "giảm tối đa") hoặc `AMOUNT` (> 0); giảm tính trên tổng tiền hàng, không vượt tiền hàng.
* Mỗi đơn tối đa 1 voucher; đặt hàng thành công thì tăng lượt dùng, đơn bị hủy thì trả lại lượt.
* Admin: mã viết hoa không dấu, không sửa mã sau khi tạo; ngày kết thúc không trước ngày bắt đầu; voucher đã có người dùng thì chỉ vô hiệu hóa, không xóa.

### 2.6. Đơn hàng & đánh giá
* Trạng thái: `Chờ xác nhận → Đang giao → Hoàn thành`, nhánh `Đã hủy`.
* Trang Đơn hàng của khách (`/orders`): tab **Tất cả** không hiện đơn đã hủy và số trên tab không tính đơn đã hủy; đơn đã hủy chỉ xem ở tab **Đã hủy**. Khách chỉ còn đơn đã hủy thì tab Tất cả báo "Không có đơn đang xử lý".
* Khách chỉ hủy được đơn "Chờ xác nhận"; hủy thì cộng trả tồn kho và lượt voucher. Admin hủy phải nhập lý do.
* Chỉ đơn "Hoàn thành" mới được đánh giá (1 đến 5 sao + bình luận); khách sửa, xóa đánh giá của mình; Admin ẩn/hiện bình luận.

### 2.7. Tài khoản khách
* Hồ sơ: họ tên 2 đến 100 ký tự, số điện thoại 10 chữ số bắt đầu bằng 0; email và vai trò không sửa được.
* Sổ địa chỉ: thêm, sửa, xóa, đặt mặc định. Yêu thích: thêm, bỏ, xem.

### 2.8. Quản trị (CRUD)
* **Sản phẩm:** tên 2 đến 150 ký tự; giá bán 1.000đ đến 99.999.999đ; tồn kho mỗi biến thể là số nguyên 0 đến 99.999, mỗi cặp màu × size chỉ nhập một lần; thêm, sửa, xóa mềm, khôi phục từ Thùng rác.
* **Danh mục** (tên 2 đến 100 ký tự, xóa mềm), **Màu** (tên 1 đến 50 ký tự), **Size** (tên 1 đến 20 ký tự, không trùng), **Banner** (bật/tắt, sắp thứ tự), **Blog** (ẩn/hiện): thêm, sửa, xóa.
* **Người dùng:** tìm theo email, khóa/mở khóa, đổi vai trò; Admin không tự khóa chính mình.
* **Thống kê:** tổng đơn, doanh thu (chỉ đơn Hoàn thành) theo khoảng ngày, top sản phẩm bán chạy.
* **Không làm:** thanh toán online, đăng nhập Google, nhập kho riêng, đổi trả, xuất Excel.

## 3. DỮ LIỆU MẪU ĐỂ KIỂM THỬ (`database/data.sql`)
| Loại | Dữ liệu |
| :--- | :--- |
| Admin | `admin@saltair.vn` (đăng nhập ở `/admin/login`) |
| Khách | `khach@saltair.vn` (có giỏ, đơn đủ trạng thái), `lan.tran@example.com`, `hoang.le@example.com`, `duc.vo@example.com` |
| Khách bị khóa | `mai.pham@example.com` |
| Voucher dùng được | `WELCOME10` (giảm 10%, tối đa 50.000đ, đơn từ 200.000đ), `GIAM50K` (giảm 50.000đ, đơn từ 500.000đ, một danh mục) |
| Voucher lỗi | `HETHAN20` (hết hạn), `HETLUOT` (hết lượt), `TAMDUNG` (vô hiệu hóa) |

## 4. QUY ƯỚC LÀM VIỆC TRÊN GITHUB
* **`main`**: bản ổn định, giảng viên chấm. Chỉ nhận PR từ `develop` khi xong một đợt (cuối tuần / nộp bài), không code trực tiếp.
* **`develop`**: nhánh phát triển. Nhận feature hoặc bug thì tách nhánh từ `develop`, xong mở PR **vào `develop`**:
  ```bash
  git checkout develop && git pull origin develop
  git checkout -b feature/<số-issue>-<tên>
  ```
* Tên nhánh: `feature/<số-issue>-<tên>` (tính năng), `fix/<số-issue>-<tên>` (sửa lỗi), `test/<số-issue>-<tên>` (bài kiểm thử), `docs/<tên>`.
* GitHub Actions chạy khi push hoặc mở PR vào `develop` và `main`; chỉ merge khi Actions **xanh**.
* Commit: `<loại>(<phạm vi>): <việc đã làm> (#<số-issue>)`, loại `feat`, `fix`, `test`, `refactor`, `docs`, `chore`.
* Nhãn issue: `logic`, `giao-dien`, `test`, `chore`, mảng (`auth`, `cart`...), `sof309-y1` → `sof309-y6`.
