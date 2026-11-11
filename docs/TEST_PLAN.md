# KẾ HOẠCH KIỂM THỬ (TEST PLAN)
> *Đặc tả: [SPECIFICATIONS.md](SPECIFICATIONS.md) · Test case: [TEST_CASES.md](TEST_CASES.md) · Lộ trình: [HUONG_DAN_CHI_TIET_6_TUAN.md](HUONG_DAN_CHI_TIET_6_TUAN.md)*

* **Dự án:** Saltair - website bán quần áo basic (JAV102)
* **Sinh viên / Người phụ trách:** Nguyễn Thiện Nhân - TY00234 (`nhan0x`)
* **Môn:** SOF309 - Kiểm thử nâng cao cùng AI ([lộ trình 6 tuần](HUONG_DAN_CHI_TIET_6_TUAN.md))

---

## 1. MỤC TIÊU KIỂM THỬ
* Kiểm thử nghiệp vụ ở tầng Service và Controller bằng **JUnit 5** (`@ParameterizedTest`, `assertThrows`, `@Timeout`).
* Kiểm thử giao diện tự động bằng **Selenium WebDriver** theo mô hình **Page Object Model**, nâng cấp **TestNG** (groups, `@DataProvider`, chụp ảnh khi fail).
* Tích hợp kiểm thử tự động với **GitHub Actions** (Chrome headless), báo cáo lên tab Artifacts.
* Chạy các ca trong `TEST_CASES.md` liên quan tới thay đổi trước khi mở PR.
* Kiểm thử toàn luồng (khách xem hàng → giỏ → đặt hàng → admin xử lý đơn) trước mỗi lần merge lớn vào `develop` và trước khi đưa lên `main`.

## 2. PHẠM VI KIỂM THỬ
* **Trong phạm vi:**
  * Đăng nhập bằng email + mã 6 số (khách và admin), phân quyền `AuthFilter`/`AdminFilter`.
  * Danh sách, tìm kiếm, lọc, chi tiết sản phẩm.
  * Giỏ hàng (cả khách chưa đăng nhập), voucher, phí ship, đặt hàng, hủy đơn, tồn kho.
  * Tài khoản: hồ sơ, sổ địa chỉ, yêu thích, đơn hàng, đánh giá.
  * Trang quản trị: sản phẩm, danh mục, màu/size, đơn hàng, voucher, người dùng, banner, blog, dashboard.
  * Giao diện ở khổ máy tính (1440px) và điện thoại (375px).
* **Ngoài phạm vi (theo đề SOF309):** kiểm thử hiệu năng, bảo mật chuyên sâu, thanh toán thật, gửi mail thật trong máy chấm (dùng `auth.devCode=true`).

## 3. CẤP ĐỘ KIỂM THỬ

| Cấp độ | Công cụ | Thư mục / Tài liệu | Khi nào chạy |
| :--- | :--- | :--- | :--- |
| Unit test nghiệp vụ | JUnit 5 (`mvn test`) | `src/test/java/com/fashionweb/` | Mỗi lần commit có sửa Service |
| Unit test Controller (Y3) | JUnit 5 | `src/test/java/com/poly/sof309/controllers/` | Mỗi PR, GitHub Actions |
| UI tự động (Y4) | Selenium POM + TestNG (`testng.xml`) | `src/test/java/com/poly/sof309/ui/` | Mỗi PR, GitHub Actions (headless) |
| Test case tay | Làm theo bảng | `docs/TEST_CASES.md` | Trước khi mở PR |
| Toàn luồng | Làm tay (hoặc script curl) | Mục 5 bên dưới | Trước khi merge PR lớn, trước khi lên `main` |

## 4. MÔI TRƯỜNG & DỮ LIỆU
* Cách chạy, cấu hình và dữ liệu mẫu: [SPECIFICATIONS.md](SPECIFICATIONS.md) mục 2 và mục 6.
* Trước mỗi đợt test: chạy lại `database/schema.sql` rồi `database/data.sql` để có dữ liệu sạch.

## 5. CÁC LUỒNG CHÍNH PHẢI LUÔN CHẠY ĐƯỢC (SMOKE)
1. Khách chưa đăng nhập mở trang chủ, tìm kiếm, mở chi tiết sản phẩm, thêm vào giỏ.
2. Khách đăng nhập bằng mã, quay về đúng trang trước đó, giỏ hàng giữ nguyên.
3. Đặt hàng có voucher `WELCOME10`: phí ship bằng 0, tồn kho giảm.
4. Khách hủy đơn "Chờ xác nhận": tồn kho và lượt dùng voucher được trả lại.
5. Admin đăng nhập ở `/admin/login`, chuyển đơn sang "Đang giao" rồi "Hoàn thành".
6. Khách đánh giá đơn "Hoàn thành".
7. Tài khoản bị khóa không xin được mã đăng nhập.

## 6. TIÊU CHÍ HOÀN THÀNH
* `mvn test`: 100% Pass; GitHub Actions trên `main` báo **xanh**.
* Các ca trong `TEST_CASES.md` liên quan tới đợt thay đổi đều Pass.
* 7 luồng ở mục 5 đều chạy được, log Tomcat không có `SEVERE`/`Exception`.
