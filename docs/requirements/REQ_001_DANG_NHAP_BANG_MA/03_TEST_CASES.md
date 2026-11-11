# BỘ KỊCH BẢN KIỂM THỬ (TEST CASES) REQ_001: Đăng nhập bằng email + mã 6 số, khung tài khoản dạng popover
> *Issue và PR của yêu cầu này nằm ở repo cũ `nhan0x/saltair-fashion-web`.*
> *Ví dụ đã điền đầy đủ theo mẫu `_TEMPLATE/03_TEST_CASES.md`. Môi trường: dữ liệu sạch từ `schema.sql` + `data.sql`, `auth.devCode=true`.*

* **Ngày chạy:** 11/11/2026 · **Người chạy:** Trưởng nhóm (kèm bộ kiểm thử toàn luồng bằng script) · **Phiên bản:** nhánh `feature/74-email-login` và `feature/75-account-popover`

| Test Case ID | Quy tắc | Mô tả ca kiểm thử | Loại (Pos/Neg) | Các bước thực hiện | Dữ liệu kiểm thử (Test Data) | Kết quả mong đợi | Trạng thái (Pass/Fail) |
| :---: | :---: | :--- | :---: | :--- | :--- | :--- | :---: |
| **TC_AUTH_01** | BR-8, BR-15 | Email mới nhập đúng mã thì tạo tài khoản và quay về trang trước | Positive | 1. Mở `/login?redirect=/wishlist`<br>2. Nhập email, gửi<br>3. Nhập mã hiện ở dòng "Chế độ thử" | `tester1@x.vn` | Về `/wishlist`; DB có user `CUSTOMER`, `full_name = tester1` | Pass |
| **TC_AUTH_02** | BR-15 | Khách có sẵn đăng nhập đúng mã | Positive | 1. Nhập email<br>2. Nhập đúng mã | `khach@saltair.vn` | Về `/home`, header hiện tài khoản | Pass |
| **TC_AUTH_03** | - | Email sai định dạng | Negative | 1. Nhập email, gửi | `abc` | Báo "Email không đúng định dạng." | Pass |
| **TC_AUTH_04** | BR-9 | Tài khoản bị khóa | Negative | 1. Nhập email, gửi | `mai.pham@example.com` | Báo "Tài khoản đã bị khóa..." , không tạo mã | Pass |
| **TC_AUTH_05** | BR-10 | Email admin ở luồng khách | Negative | 1. Nhập email, gửi | `admin@saltair.vn` | Báo câu chuyển sang trang quản trị, có link `/admin/login` | Pass |
| **TC_AUTH_06** | BR-6 | Mã sai báo số lần còn lại | Negative | 1. Xin mã<br>2. Nhập mã sai | `000000` | "Mã không đúng, bạn còn 4 lần thử." | Pass |
| **TC_AUTH_07** | BR-6 | Sai đủ 5 lần thì hủy mã (biên) | Negative | 1. Nhập sai 5 lần<br>2. Nhập mã đúng | `000001` x5 | Lần 5: "Bạn đã nhập sai quá số lần..."; mã đúng sau đó bị từ chối | Pass |
| **TC_AUTH_08** | BR-6 | Mã quá 15 phút | Negative | 1. Xin mã<br>2. Cho `expires_at` qua hạn<br>3. Nhập mã | Mã hợp lệ | "Mã đã hết hạn hoặc không còn hiệu lực..." | Pass |
| **TC_AUTH_09** | BR-4 | Dùng lại mã đã dùng | Negative | 1. Đăng nhập bằng mã<br>2. Đăng xuất, nhập lại mã cũ | Mã vừa dùng | Bị từ chối | Pass |
| **TC_AUTH_10** | BR-7 | Gửi lại trước 60 giây (biên) | Negative | 1. Xin mã<br>2. Xin lại ngay | `tester1@x.vn` | "Bạn thử lại sau ít phút." | Pass |
| **TC_AUTH_11** | BR-3, BR-7 | Gửi lại sau 60 giây thì mã cũ hết hiệu lực | Positive | 1. Xin mã A<br>2. Sau 60 giây xin mã B<br>3. Nhập mã A | | Mã A bị từ chối, mã B dùng được | Pass |
| **TC_AUTH_12** | BR-2 | DB không lưu mã gốc | Positive | 1. Xin mã<br>2. Tra `login_codes` | | `code_hash` 64 ký tự, không có dòng nào bằng mã gốc | Pass |
| **TC_AUTH_13** | BR-14 | Email không nằm trên URL | Positive | 1. Gửi email | | URL chỉ là `/login/code`, không có email | Pass |
| **TC_AUTH_14** | BR-12 | Admin flow với email thường | Negative | 1. Mở `/admin/login`<br>2. Nhập email khách | `lan.tran@example.com` | Báo câu chung chung, không có mã nào được tạo | Pass |
| **TC_AUTH_15** | BR-11, BR-13 | Admin đăng nhập đúng | Positive | 1. Mở `/admin/products` khi chưa đăng nhập<br>2. Nhập email admin, nhập mã | `admin@saltair.vn` | Chuyển `/admin/login?redirect=...`, sau đó về `/admin/products`; mã hạn 5 phút; log có dòng báo đăng nhập | Pass |
| **TC_AUTH_16** | BR-11 | Admin nhập sai mã | Negative | 1. Nhập mã sai | `111111` | "Mã không đúng, bạn còn 2 lần thử." | Pass |
| **TC_AUTH_17** | - | Khách không vào được trang quản trị | Negative | 1. Đăng nhập khách<br>2. Mở `/admin` | | Trang 403 | Pass |
| **TC_AUTH_18** | BR-16 | Tắt `auth.devCode`, không có SMTP | Negative | 1. Đặt `auth.devCode=false`<br>2. Nhập email | `khach@saltair.vn` | "Không gửi được mã, vui lòng thử lại."; log và trang không có mã | Pass |
| **TC_AUTH_19** | - | Link cũ vẫn chạy | Positive | 1. Mở `/register`, `/forgot-password` | | Chuyển về `/login` | Pass |
| **TC_AUTH_20** | BR-17 | Mở và đóng khung tài khoản | Positive | 1. Bấm icon người<br>2. Bấm X / bấm ra ngoài / nhấn Esc | | Khung trồi lên 20px, nền không mờ; đóng được bằng cả 3 cách, focus trả về icon | Pass |
| **TC_AUTH_21** | BR-17 | Khung tài khoản và khung tìm kiếm đóng lẫn nhau | Positive | 1. Mở khung tài khoản<br>2. Bấm icon tìm kiếm | | Khung tài khoản đóng, khung tìm kiếm mở (và ngược lại) | Pass |
| **TC_AUTH_22** | BR-17 | Bấm dòng menu khi chưa đăng nhập | Negative | 1. Mở khung<br>2. Bấm "Đơn hàng" | | Không chuyển trang, focus ô email, hiện "Đăng nhập để xem đơn hàng" | Pass |
| **TC_AUTH_23** | BR-17 | Khung khi đã đăng nhập (admin) | Positive | 1. Đăng nhập admin<br>2. Mở khung | `admin@saltair.vn` | Hiện "Tài khoản", email, có dòng "Trang quản trị" và "Đăng xuất" | Pass |
| **TC_AUTH_24** | BR-18 | Dán cả mã vào ô bất kỳ | Positive | 1. Copy mã<br>2. Dán vào ô thứ 4 | Mã đúng | Đủ 6 ô, tự gửi, về đúng trang trước | Pass |
| **TC_AUTH_25** | BR-18 | Gõ mã sai trên 6 ô | Negative | 1. Gõ 6 số sai | `000000` | Ô viền đỏ, rung, báo số lần còn lại, xóa ô và focus ô đầu | Pass |
| **TC_AUTH_26** | BR-18 | Tắt JavaScript vẫn đăng nhập được | Positive | 1. Tắt JS<br>2. Nhập 6 ô, bấm "Xác nhận" | Mã đúng | Đăng nhập thành công | Pass |
| **TC_AUTH_27** | BR-17 | Khổ điện thoại | Positive | 1. Mở ở 375px<br>2. Bấm icon người | | Khung rộng `100vw - 32px`, 6 ô vừa màn hình | Pass |

## UNIT TEST ĐI KÈM
| Class test | Phương thức | Kỹ thuật | Quy tắc |
| :--- | :--- | :--- | :---: |
| *(chưa có, cần bổ sung)* | `AuthServiceTest#normalizeEmail`: email hợp lệ, sai định dạng, đúng 150 / 151 ký tự | Tham số hóa | BR-8 |
| *(chưa có, cần bổ sung)* | `AuthServiceTest#verifyCode`: mã 5 chữ số, 7 chữ số, có chữ cái | Ngoại lệ | BR-1 |

## KẾT QUẢ
* **Tổng:** 27 ca · **Pass:** 27 · **Fail:** 0 · **Unit test:** chưa có
* **Lỗi phát hiện & cách sửa:** trang nhập mã dùng `${ctx}` trước khi `layout.tag` gán giá trị nên form gửi sai đường dẫn, đã đổi sang tự ghép đường dẫn; focus không vào ô email khi khung mở bằng chuyển động, đã thử lại focus sau khi khung hiện.
* **Hồi quy (luồng chính trong TEST_PLAN.md):** Đạt (bộ kiểm thử toàn luồng 101/101)
