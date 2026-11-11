# BỘ KỊCH BẢN KIỂM THỬ (TEST CASES)

* **Dự án:** Saltair · **Đặc tả:** [SPECIFICATIONS.md](SPECIFICATIONS.md) · **Kế hoạch:** [TEST_PLAN.md](TEST_PLAN.md)
* **Môi trường:** dữ liệu sạch từ `schema.sql` + `data.sql`, `auth.devCode=true` · **Ngày chạy gần nhất:** 11/11/2026

## 1. ĐĂNG NHẬP BẰNG EMAIL + MÃ 6 SỐ (mục 2.2 trong SPECIFICATIONS)

| Test Case ID | Chức năng | Mô tả ca kiểm thử | Loại test (Pos/Neg) | Các bước thực hiện | Dữ liệu kiểm thử (Test Data) | Kết quả mong đợi | Trạng thái (Pass/Fail) |
| :---: | :---: | :--- | :---: | :--- | :--- | :--- | :---: |
| **TC_AUTH_01** | Đăng nhập | Email mới nhập đúng mã thì tạo tài khoản và quay về trang trước | Positive | 1. Mở `/login?redirect=/wishlist`<br>2. Nhập email, gửi<br>3. Nhập mã hiện ở dòng "Chế độ thử" | `tester1@x.vn` | Về `/wishlist`; DB có user `CUSTOMER`, `full_name = tester1` | Pass |
| **TC_AUTH_02** | Đăng nhập | Khách có sẵn đăng nhập đúng mã | Positive | 1. Nhập email<br>2. Nhập đúng mã | `khach@saltair.vn` | Về `/home`, header hiện tài khoản | Pass |
| **TC_AUTH_03** | Đăng nhập | Email sai định dạng | Negative | 1. Nhập email, gửi | `abc` | Báo "Email không đúng định dạng." | Pass |
| **TC_AUTH_04** | Đăng nhập | Tài khoản bị khóa | Negative | 1. Nhập email, gửi | `mai.pham@example.com` | Báo "Tài khoản đã bị khóa..." , không tạo mã | Pass |
| **TC_AUTH_05** | Đăng nhập | Email admin ở luồng khách | Negative | 1. Nhập email, gửi | `admin@saltair.vn` | Báo câu chuyển sang trang quản trị, có link `/admin/login` | Pass |
| **TC_AUTH_06** | Đăng nhập | Mã sai báo số lần còn lại | Negative | 1. Xin mã<br>2. Nhập mã sai | `000000` | "Mã không đúng, bạn còn 4 lần thử." | Pass |
| **TC_AUTH_07** | Đăng nhập | Sai đủ 5 lần thì hủy mã (biên) | Negative | 1. Nhập sai 5 lần<br>2. Nhập mã đúng | `000001` x5 | Lần 5: "Bạn đã nhập sai quá số lần..."; mã đúng sau đó bị từ chối | Pass |
| **TC_AUTH_08** | Đăng nhập | Mã quá 15 phút | Negative | 1. Xin mã<br>2. Cho `expires_at` qua hạn<br>3. Nhập mã | Mã hợp lệ | "Mã đã hết hạn hoặc không còn hiệu lực..." | Pass |
| **TC_AUTH_09** | Đăng nhập | Dùng lại mã đã dùng | Negative | 1. Đăng nhập bằng mã<br>2. Đăng xuất, nhập lại mã cũ | Mã vừa dùng | Bị từ chối | Pass |
| **TC_AUTH_10** | Đăng nhập | Gửi lại trước 60 giây (biên) | Negative | 1. Xin mã<br>2. Xin lại ngay | `tester1@x.vn` | "Bạn thử lại sau ít phút." | Pass |
| **TC_AUTH_11** | Đăng nhập | Gửi lại sau 60 giây thì mã cũ hết hiệu lực | Positive | 1. Xin mã A<br>2. Sau 60 giây xin mã B<br>3. Nhập mã A | | Mã A bị từ chối, mã B dùng được | Pass |
| **TC_AUTH_12** | Đăng nhập | DB không lưu mã gốc | Positive | 1. Xin mã<br>2. Tra `login_codes` | | `code_hash` 64 ký tự, không có dòng nào bằng mã gốc | Pass |
| **TC_AUTH_13** | Đăng nhập | Email không nằm trên URL | Positive | 1. Gửi email | | URL chỉ là `/login/code`, không có email | Pass |
| **TC_AUTH_14** | Đăng nhập Admin | Admin flow với email thường | Negative | 1. Mở `/admin/login`<br>2. Nhập email khách | `lan.tran@example.com` | Báo câu chung chung, không có mã nào được tạo | Pass |
| **TC_AUTH_15** | Đăng nhập Admin | Admin đăng nhập đúng | Positive | 1. Mở `/admin/products` khi chưa đăng nhập<br>2. Nhập email admin, nhập mã | `admin@saltair.vn` | Chuyển `/admin/login?redirect=...`, sau đó về `/admin/products`; mã hạn 5 phút; log có dòng báo đăng nhập | Pass |
| **TC_AUTH_16** | Đăng nhập Admin | Admin nhập sai mã | Negative | 1. Nhập mã sai | `111111` | "Mã không đúng, bạn còn 2 lần thử." | Pass |
| **TC_AUTH_17** | Phân quyền | Khách không vào được trang quản trị | Negative | 1. Đăng nhập khách<br>2. Mở `/admin` | | Trang 403 | Pass |
| **TC_AUTH_18** | Đăng nhập | Tắt `auth.devCode`, không có SMTP | Negative | 1. Đặt `auth.devCode=false`<br>2. Nhập email | `khach@saltair.vn` | "Không gửi được mã, vui lòng thử lại."; log và trang không có mã | Pass |
| **TC_AUTH_19** | Đăng nhập | Link cũ vẫn chạy | Positive | 1. Mở `/register`, `/forgot-password` | | Chuyển về `/login` | Pass |
| **TC_AUTH_20** | Giao diện đăng nhập | Mở và đóng khung tài khoản | Positive | 1. Bấm icon người<br>2. Bấm X / bấm ra ngoài / nhấn Esc | | Khung trồi lên 20px, nền không mờ; đóng được bằng cả 3 cách, focus trả về icon | Pass |
| **TC_AUTH_21** | Giao diện đăng nhập | Khung tài khoản và khung tìm kiếm đóng lẫn nhau | Positive | 1. Mở khung tài khoản<br>2. Bấm icon tìm kiếm | | Khung tài khoản đóng, khung tìm kiếm mở (và ngược lại) | Pass |
| **TC_AUTH_22** | Giao diện đăng nhập | Bấm dòng menu khi chưa đăng nhập | Negative | 1. Mở khung<br>2. Bấm "Đơn hàng" | | Không chuyển trang, focus ô email, hiện "Đăng nhập để xem đơn hàng" | Pass |
| **TC_AUTH_23** | Giao diện đăng nhập | Khung khi đã đăng nhập (admin) | Positive | 1. Đăng nhập admin<br>2. Mở khung | `admin@saltair.vn` | Hiện "Tài khoản", email, có dòng "Trang quản trị" và "Đăng xuất" | Pass |
| **TC_AUTH_24** | Giao diện đăng nhập | Dán cả mã vào ô bất kỳ | Positive | 1. Copy mã<br>2. Dán vào ô thứ 4 | Mã đúng | Đủ 6 ô, tự gửi, về đúng trang trước | Pass |
| **TC_AUTH_25** | Giao diện đăng nhập | Gõ mã sai trên 6 ô | Negative | 1. Gõ 6 số sai | `000000` | Ô viền đỏ, rung, báo số lần còn lại, xóa ô và focus ô đầu | Pass |
| **TC_AUTH_26** | Giao diện đăng nhập | Tắt JavaScript vẫn đăng nhập được | Positive | 1. Tắt JS<br>2. Nhập 6 ô, bấm "Xác nhận" | Mã đúng | Đăng nhập thành công | Pass |
| **TC_AUTH_27** | Giao diện đăng nhập | Khổ điện thoại | Positive | 1. Mở ở 375px<br>2. Bấm icon người | | Khung rộng `100vw - 32px`, 6 ô vừa màn hình | Pass |

> Các chức năng còn lại (giỏ hàng, voucher, đặt hàng, CRUD quản trị) sẽ bổ sung ở Y2 theo [HUONG_DAN_CHI_TIET_6_TUAN.md](HUONG_DAN_CHI_TIET_6_TUAN.md).
