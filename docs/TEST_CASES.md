# BỘ KỊCH BẢN KIỂM THỬ (TEST CASES)

> Sinh từ [TEST_PLAN.md](TEST_PLAN.md) và [SPECIFICATIONS.md](SPECIFICATIONS.md) (bản 4.0). Mỗi ca truy vết về mã **BR-xx** hoặc **NFR-xx** trong đặc tả. Kết quả mong đợi lấy từ quy tắc đặc tả và code Lab 4–6.

* **Dự án:** PolyCoffee · **Môi trường:** JDK 17, Tomcat 9, MySQL 8, DB test `polycoffee_test` · **Số ca:** 91
* **Ngày thiết kế:** 21/11/2026 · **Trạng thái ban đầu:** *Chưa chạy*. Repo hiện chỉ có app console, nên các ca chưa chạy được cho đến khi code web hoàn thiện.

## CÁCH ĐỌC BẢNG

| Cột | Ý nghĩa |
| :--- | :--- |
| **BR / NFR** | Quy tắc đặc tả mà ca kiểm tra |
| **P · Kỹ thuật · Tầng** | **P1** bắt buộc đạt. **P2** hồi quy. **P3** chấp nhận rủi ro. **Kỹ thuật:** `EP` phân vùng tương đương, `BVA` giá trị biên, `EG` đoán lỗi, `SEC` an toàn, `ST` chuyển trạng thái. **Tầng:** `Unit` (Servlet/Filter với DAO mock), `Integ` (DAO với MySQL test), `Manual` (trình duyệt), `Tĩnh` (tìm trong mã nguồn) |
| **🔶** | Giả thuyết lỗi: ca nhằm xác nhận rủi ro R-xx hoặc điểm chưa chốt Q-x trong TEST_PLAN mục 9. Kết quả khác mong đợi thì ghi nhận là lỗi thật |
| **Dữ liệu mẫu** | Ký hiệu dữ liệu nền trong SPECIFICATIONS mục 3.2. Các ca viết theo dữ liệu này |

**Dữ liệu nền** (nạp lại trước mỗi ca):

| Tài khoản | id | Vai trò | Ghi chú |
| :--- | :---: | :--- | :--- |
| `manager@polycoffee.com` / `123456` | 1 | Quản lý | |
| `staff1@polycoffee.com` / `123456` | 2 | Nhân viên | Có hóa đơn 1, 2 |
| `staff2@polycoffee.com` / `123456` | 3 | Nhân viên | Có hóa đơn 3 (đang xử lý) |

| Đồ uống | id | Loại | Giá | Giảm giá | Trạng thái |
| :--- | :---: | :--- | ---: | :---: | :--- |
| Cà phê đen | 1 | Cà phê | 20.000 | 0% | Đang bán |
| Cà phê sữa | 2 | Cà phê | 25.000 | 0% | Đang bán |
| Bạc xỉu | 3 | Cà phê | 29.000 | 10% | Đang bán |
| Trà đào cam sả | 4 | Trà | 35.000 | 0% | Đang bán |
| Trà vải | 5 | Trà | 32.000 | 0% | Đang bán |
| Sinh tố bơ | 6 | Sinh tố | 40.000 | 0% | Đang bán |
| Sinh tố xoài | 7 | Sinh tố | 38.000 | 0% | Đang bán |
| Nước ép cam | 8 | Nước ép | 35.000 | 0% | Đang bán |
| Nước ép dưa hấu | 9 | Nước ép | 30.000 | 0% | **Ngừng bán** |

| Hóa đơn | id | Nhân viên | Trạng thái | Chi tiết | Tổng tiền | Ngày thanh toán |
| :--- | :---: | :---: | :---: | :--- | ---: | :--- |
| Hóa đơn 1 | 1 | 2 | 1 (đã thanh toán) | Cà phê đen × 2, Trà đào cam sả × 1 | **75.000** | D−2 |
| Hóa đơn 2 | 2 | 2 | 1 (đã thanh toán) | Cà phê sữa × 3, Sinh tố bơ × 1, Bạc xỉu × 2 (giảm 10%) | **167.200** | D−1 |
| Hóa đơn 3 | 3 | 3 | 0 (đang xử lý) | Trà vải × 2 | 64.000 (không tính doanh thu) | — |

> `D` là ngày nạp dữ liệu mẫu. Tổng tiền: 2 × 20.000 + 1 × 35.000 = 75.000. 3 × 25.000 + 1 × 40.000 + 2 × 29.000 × 0,9 = 167.200.

---

## 1. ĐĂNG NHẬP, PHÂN QUYỀN, CÔNG KHAI (17 CA)

| Test Case ID | Chức năng | Mô tả ca kiểm thử | Loại | Các bước thực hiện | Dữ liệu kiểm thử | Kết quả mong đợi | BR / NFR | P · Kỹ thuật · Tầng | Trạng thái |
| :---: | :---: | :--- | :---: | :--- | :--- | :--- | :---: | :---: | :---: |
| **TC_AUTH_01** | Đăng nhập | Quản lý đăng nhập đúng | Positive | 1. Mở `/auth/login`<br>2. Nhập email và mật khẩu quản lý<br>3. Gửi form | `manager@polycoffee.com` / `123456` | Chuyển đến `/home`. Session có `authUser` với `manager = true` | BR-01, BR-05 | P1 · EP · Unit | Chưa chạy |
| **TC_AUTH_02** | Đăng nhập | Nhân viên đăng nhập đúng | Positive | 1. Đăng nhập bằng `staff1`<br>2. Kiểm tra session | `staff1@polycoffee.com` / `123456` | Chuyển đến `/home`. `manager = false` | BR-01, BR-05 | P1 · EP · Unit | Chưa chạy |
| **TC_AUTH_03** | Đăng nhập | Email để trống | Negative | 1. Để trống email<br>2. Nhập mật khẩu<br>3. Gửi | Email `""`, mật khẩu `123456` | Ở lại trang đăng nhập. Thông báo "Vui lòng nhập email và mật khẩu" | BR-01 | P1 · EP · Unit | Chưa chạy |
| **TC_AUTH_04** | Đăng nhập | Mật khẩu để trống | Negative | 1. Nhập email<br>2. Để trống mật khẩu<br>3. Gửi | Email đúng, mật khẩu `""` | Thông báo "Vui lòng nhập email và mật khẩu" | BR-01 | P1 · EP · Unit | Chưa chạy |
| **TC_AUTH_05** | Đăng nhập | Email không tồn tại | Negative | 1. Nhập email lạ<br>2. Gửi | `khong@co.com` / `123456` | Thông báo "Email hoặc mật khẩu không đúng" | BR-01 | P1 · EP · Unit | Chưa chạy |
| **TC_AUTH_06** | Đăng nhập | Mật khẩu sai. Thông báo không tiết lộ email có tồn tại hay không | Negative | 1. Nhập email đúng<br>2. Mật khẩu sai<br>3. Gửi | `manager@...` / `sai123` | Cùng thông báo với TC_AUTH_05: "Email hoặc mật khẩu không đúng" | BR-01 | P1 · SEC · Unit | Chưa chạy |
| **TC_AUTH_07** | Đăng nhập | Tài khoản bị khóa, đúng mật khẩu | Negative | 1. Chạy `UPDATE users SET enabled = 0 WHERE id = 2`<br>2. Đăng nhập bằng `staff1`<br>3. Khôi phục `enabled = 1` | `staff1` / `123456` | Thông báo "Tài khoản đã bị khóa". Không tạo session | BR-02 | P1 · ST · Unit | Chưa chạy |
| **TC_AUTH_08** | Đăng nhập | Chuỗi SQL injection trong ô email | Negative | 1. Nhập email `' OR '1'='1`<br>2. Nhập mật khẩu bất kỳ<br>3. Gửi | Email `' OR '1'='1`, mật khẩu `x` | Không đăng nhập. Thông báo "Email hoặc mật khẩu không đúng". Bảng `users` không đổi | NFR-01, BR-01 | P1 · SEC · Integ | Chưa chạy |
| **TC_AUTH_09** | Đăng nhập | Chuyển hướng sau đăng nhập về đúng trang đã yêu cầu | Positive | 1. Chưa đăng nhập, mở `/manager/stats/revenue`<br>2. Được chuyển đến `/auth/login?redirect=/manager/stats/revenue`<br>3. Đăng nhập quản lý | Quản lý | Sau đăng nhập, chuyển đến `/manager/stats/revenue` | BR-03, BR-05 | P2 · EP · Unit | Chưa chạy |
| **TC_AUTH_10** | Đăng nhập | Tham số `redirect` trỏ ra ngoài site bị bỏ qua | Negative | 1. Mở `/auth/login?redirect=//evil.example`<br>2. Đăng nhập đúng | Quản lý | Chuyển đến `/home`, không chuyển ra ngoài | BR-05 | P1 · SEC · Unit | Chưa chạy |
| **TC_AUTH_11** | Phân quyền | Khách truy cập `/manager/bills` khi chưa đăng nhập | Negative | 1. Chưa đăng nhập<br>2. Mở `/manager/bills` | Không có | Chuyển đến `/auth/login?redirect=...`. Không hiển thị dữ liệu hóa đơn | BR-03 | P1 · SEC · Unit | Chưa chạy |
| **TC_AUTH_12** | Phân quyền | Nhân viên GET `/manager/bills` | Negative | 1. Đăng nhập `staff1`<br>2. Mở `/manager/bills` | `staff1` | HTTP 403, thông báo "Bạn không có quyền truy cập chức năng này" | BR-04 | P1 · SEC · Unit | Chưa chạy |
| **TC_AUTH_13** | Phân quyền | Nhân viên gửi POST tới `/manager/drinks/save` | Negative | 1. Đăng nhập `staff1`<br>2. Gửi POST form đồ uống tới `/manager/drinks/save` | `staff1`, form hợp lệ | HTTP 403. Không có đồ uống mới trong bảng `drinks` | BR-04 | P1 · SEC · Unit | Chưa chạy |
| **TC_AUTH_14** | Phân quyền | Quản lý truy cập được `/manager/bills` | Positive | 1. Đăng nhập quản lý<br>2. Mở `/manager/bills` | Quản lý | HTTP 200. Hiển thị danh sách hóa đơn | BR-04 | P1 · EP · Unit | Chưa chạy |
| **TC_AUTH_15** | Đăng xuất | Đăng xuất hủy session | Positive | 1. Đăng nhập quản lý<br>2. Mở `/auth/logout`<br>3. Mở `/manager/bills` | Quản lý | Bước 2 chuyển đến `/auth/login`. Bước 3 bị chuyển đến login, session đã bị hủy | BR-06 | P1 · ST · Unit | Chưa chạy |
| **TC_AUTH_16** | Công khai | Trang công khai không yêu cầu đăng nhập | Positive | 1. Chưa đăng nhập<br>2. Mở `/home`, `/auth/login`, `/css/style.css` | Không có | Cả ba trả về nội dung, không chuyển hướng | BR-03 | P2 · BVA · Unit | Chưa chạy |
| **TC_AUTH_17** | Mật khẩu | Mật khẩu được lưu dưới dạng băm SHA-256 | Positive | 1. Đọc `password` của `manager` trong `users`<br>2. So với SHA-256 của `123456`<br>3. Đăng nhập đúng | Dữ liệu mẫu | Giá trị lưu là chuỗi hex 64 ký tự, bằng `8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92`. Đăng nhập vẫn thành công | BR-07 | P1 · EP · Integ | Chưa chạy |

---

## 2. TRANG CHỦ KHÁCH (2 CA)

| Test Case ID | Chức năng | Mô tả ca kiểm thử | Loại | Các bước thực hiện | Dữ liệu kiểm thử | Kết quả mong đợi | BR / NFR | P · Kỹ thuật · Tầng | Trạng thái |
| :---: | :---: | :--- | :---: | :--- | :--- | :--- | :---: | :---: | :---: |
| **TC_HOME_01** | Trang chủ | Khách chỉ thấy đồ uống đang bán | Positive | 1. Không đăng nhập<br>2. Mở `/home`<br>3. Đếm đồ uống hiển thị | Dữ liệu mẫu | 8 đồ uống. "Nước ép dưa hấu" không hiển thị | BR-08 | P1 · EP · Manual | Chưa chạy |
| **TC_HOME_02** | Trang chủ | Đồ uống được nhóm theo loại | Positive | 1. Mở `/home`<br>2. Kiểm tra các nhóm | Dữ liệu mẫu | 4 nhóm: Cà phê (3), Trà (2), Sinh tố (2), Nước ép (1) | BR-08 | P2 · EP · Manual | Chưa chạy |

---

## 3. LOẠI ĐỒ UỐNG (9 CA)

| Test Case ID | Chức năng | Mô tả ca kiểm thử | Loại | Các bước thực hiện | Dữ liệu kiểm thử | Kết quả mong đợi | BR / NFR | P · Kỹ thuật · Tầng | Trạng thái |
| :---: | :---: | :--- | :---: | :--- | :--- | :--- | :---: | :---: | :---: |
| **TC_CAT_01** | Thêm loại | Thêm loại hợp lệ | Positive | 1. Đăng nhập quản lý<br>2. Mở `/manager/categories/new`<br>3. Nhập tên, gửi | `Bánh ngọt` | Chuyển về danh sách, thông báo "Đã lưu loại đồ uống". Danh sách có "Bánh ngọt" | BR-09 | P1 · EP · Unit | Chưa chạy |
| **TC_CAT_02** | Thêm loại | Tên để trống hoặc toàn khoảng trắng | Negative | 1. Nhập tên `"   "`<br>2. Gửi | `"   "` | Ở lại form. Thông báo "Tên loại không được để trống". 🔶 Q-4: cần xác nhận có cắt khoảng trắng | BR-09 | P1 · EP · Unit | Chưa chạy |
| **TC_CAT_03** | Thêm loại | Tên đúng 100 ký tự được chấp nhận | Positive | 1. Nhập tên 100 ký tự<br>2. Gửi | Chuỗi 100 ký tự | Lưu thành công | BR-09 | P1 · BVA · Unit | Chưa chạy |
| **TC_CAT_04** | Thêm loại | Tên 101 ký tự bị từ chối | Negative | 1. Nhập tên 101 ký tự<br>2. Gửi | Chuỗi 101 ký tự | Thông báo "Tên loại tối đa 100 ký tự". Không lưu | BR-09 | P1 · BVA · Unit | Chưa chạy |
| **TC_CAT_05** | Thêm loại | Tên trùng với loại đã có | Negative | 1. Nhập `Cà phê`<br>2. Gửi | `Cà phê` (đã có) | Thông báo "Tên loại đã tồn tại". Số dòng `categories` không đổi | BR-09 | P1 · EG · Integ | Chưa chạy |
| **TC_CAT_06** | Xóa loại | Không xóa loại còn đồ uống | Negative | 1. Mở danh sách loại<br>2. Xóa "Cà phê" | Loại id 1 (có 3 đồ uống) | Thông báo "Không thể xóa: loại này đang có đồ uống". Loại vẫn còn | BR-10 | P1 · ST · Integ | Chưa chạy |
| **TC_CAT_07** | Xóa loại | Xóa loại không còn đồ uống | Positive | 1. Thêm loại `Tạm`<br>2. Xóa `Tạm` | `Tạm` | Thông báo "Đã xóa loại đồ uống". Loại biến mất khỏi bảng | BR-10 | P1 · ST · Integ | Chưa chạy |
| **TC_CAT_08** 🔶 | Sửa loại | Sửa loại mà giữ nguyên tên hiện tại không báo trùng với chính nó | Positive | 1. Mở sửa loại "Trà"<br>2. Đổi nội dung khác, giữ tên "Trà"<br>3. Lưu | Loại id 2 | Lưu thành công, không báo "Tên loại đã tồn tại" | BR-09 | P2 · EG · Integ | Chưa chạy |
| **TC_CAT_09** | Sửa loại | Mở sửa loại không tồn tại | Negative | 1. Mở `/manager/categories/edit?id=999` | id 999 | HTTP 404 | BR-09 | P2 · BVA · Unit | Chưa chạy |

---

## 4. ĐỒ UỐNG (13 CA)

| Test Case ID | Chức năng | Mô tả ca kiểm thử | Loại | Các bước thực hiện | Dữ liệu kiểm thử | Kết quả mong đợi | BR / NFR | P · Kỹ thuật · Tầng | Trạng thái |
| :---: | :---: | :--- | :---: | :--- | :--- | :--- | :---: | :---: | :---: |
| **TC_DRK_01** | Thêm đồ uống | Thêm đồ uống hợp lệ | Positive | 1. Mở `/manager/drinks/new`<br>2. Nhập: tên `Latte`, giá 45000, giảm 0%, loại Cà phê<br>3. Gửi | Dữ liệu trong cột bên | Thông báo "Đã lưu đồ uống". Có dòng mới trong `drinks` | BR-11 | P1 · EP · Unit | Chưa chạy |
| **TC_DRK_02** | Thêm đồ uống | Tên để trống | Negative | 1. Để trống tên<br>2. Gửi | Tên `""` | Thông báo "Tên đồ uống không được để trống" | BR-11 | P1 · EP · Unit | Chưa chạy |
| **TC_DRK_03** | Thêm đồ uống | Đơn giá âm bị từ chối | Negative | 1. Nhập giá -1<br>2. Gửi | Giá `-1` | Thông báo "Đơn giá phải là số không âm". Không lưu | BR-11 | P1 · BVA · Unit | Chưa chạy |
| **TC_DRK_04** | Thêm đồ uống | Đơn giá 0 được chấp nhận | Positive | 1. Nhập giá 0<br>2. Gửi | Giá `0` | Lưu thành công | BR-11 | P1 · BVA · Unit | Chưa chạy |
| **TC_DRK_05** | Thêm đồ uống | Giảm giá ngoài khoảng 0–100% bị từ chối | Negative | 1. Nhập giảm giá -1% rồi 101%<br>2. Gửi mỗi lần | -1%, 101% | Thông báo "Giảm giá phải từ 0 đến 100%" cho cả hai | BR-11 | P1 · BVA · Unit | Chưa chạy |
| **TC_DRK_06** | Thêm đồ uống | Giảm giá 0% và 100% được chấp nhận | Positive | 1. Nhập giảm giá 0%, lưu<br>2. Nhập giảm giá 100%, lưu | 0%, 100% | Cả hai lưu thành công. Trong DB lưu 0 và 1 | BR-11 | P1 · BVA · Unit | Chưa chạy |
| **TC_DRK_07** | Thêm đồ uống | Loại không tồn tại | Negative | 1. Gửi form với `categoryId = 999` | Loại 999 | Thông báo "Vui lòng chọn loại đồ uống" | BR-11 | P1 · EP · Unit | Chưa chạy |
| **TC_DRK_08** | Xóa đồ uống | Không xóa đồ uống đã có trong hóa đơn | Negative | 1. Xóa "Cà phê đen" | Đồ uống id 1 (có trong hóa đơn 1) | Thông báo "Không thể xóa: đồ uống đã được bán (hãy chuyển sang ngừng bán)". Dòng còn trong `drinks` | BR-12 | P1 · ST · Integ | Chưa chạy |
| **TC_DRK_09** | Xóa đồ uống | Xóa đồ uống chưa bán | Positive | 1. Thêm `Tạm`<br>2. Xóa `Tạm` | `Tạm` | Thông báo "Đã xóa đồ uống". Dòng biến mất | BR-12 | P1 · ST · Integ | Chưa chạy |
| **TC_DRK_10** | Ngừng bán | Đồ uống ngừng bán ẩn ở trang chủ, vẫn hiện trong quản lý | Positive | 1. Đặt "Cà phê sữa" thành ngừng bán<br>2. Mở `/home`<br>3. Mở `/manager/drinks` | Đồ uống id 2 | Trang chủ không còn "Cà phê sữa" (7 đồ uống). Danh sách quản lý vẫn có, trạng thái "Ngừng bán" | BR-08, BR-12 | P1 · ST · Manual | Chưa chạy |
| **TC_DRK_11** | Tìm kiếm | Tìm theo tên chứa "cà phê" | Positive | 1. Tìm `cà phê`, không lọc loại, không lọc trạng thái | `cà phê` | 2 kết quả: "Cà phê đen", "Cà phê sữa". Không phân biệt hoa thường | BR-13 | P2 · EP · Integ | Chưa chạy |
| **TC_DRK_12** | Tìm kiếm | Lọc theo loại Trà | Positive | 1. Chọn loại Trà, không lọc tên | Loại id 2 | 2 kết quả: "Trà đào cam sả", "Trà vải" | BR-13 | P2 · EP · Integ | Chưa chạy |
| **TC_DRK_13** | Phân trang | Phân trang 10 dòng mỗi trang | Positive | 1. Dữ liệu mẫu có 9 đồ uống, mở danh sách<br>2. Thêm 2 đồ uống (tổng 11)<br>3. Mở trang 2 | 2 đồ uống thêm | Bước 1: 1 trang. Bước 3: trang 2 có 1 dòng. Trang 1 có 10 dòng | BR-13 | P2 · BVA · Integ | Chưa chạy |

---

## 5. NHÂN VIÊN (12 CA)

| Test Case ID | Chức năng | Mô tả ca kiểm thử | Loại | Các bước thực hiện | Dữ liệu kiểm thử | Kết quả mong đợi | BR / NFR | P · Kỹ thuật · Tầng | Trạng thái |
| :---: | :---: | :--- | :---: | :--- | :--- | :--- | :---: | :---: | :---: |
| **TC_STF_01** | Thêm nhân viên | Thêm nhân viên hợp lệ, mật khẩu được băm | Positive | 1. Mở `/manager/staff/new`<br>2. Nhập: "Phạm A", `a@poly.com`, mật khẩu `abc123`<br>3. Gửi | Dữ liệu trong cột bên | Thông báo "Đã lưu nhân viên". Cột `password` là SHA-256, không phải `abc123` | BR-15, BR-16 | P1 · EP · Integ | Chưa chạy |
| **TC_STF_02** | Thêm nhân viên | Họ tên để trống | Negative | 1. Để trống họ tên<br>2. Gửi | Họ tên `""` | Thông báo "Họ tên không được để trống" | BR-15 | P1 · EP · Unit | Chưa chạy |
| **TC_STF_03** | Thêm nhân viên | Email sai định dạng | Negative | 1. Lần lượt nhập `abc`, `a@b`, `a b@c.com`<br>2. Gửi mỗi lần | Ba email trên | Với mỗi email: "Email không hợp lệ". Không lưu | BR-15 | P1 · EP · Unit | Chưa chạy |
| **TC_STF_04** | Thêm nhân viên | Email trùng với tài khoản đã có | Negative | 1. Thêm nhân viên với `manager@polycoffee.com` | Email đã có | Thông báo "Email đã được sử dụng" | BR-15 | P1 · EG · Integ | Chưa chạy |
| **TC_STF_05** | Sửa nhân viên | Sửa nhân viên giữ nguyên email của chính mình | Positive | 1. Mở sửa `staff1`<br>2. Đổi họ tên, giữ email<br>3. Lưu | `staff1` | Lưu thành công, không báo trùng email | BR-15 | P1 · EG · Integ | Chưa chạy |
| **TC_STF_06** | Thêm nhân viên | Mật khẩu 5 ký tự bị từ chối, 6 ký tự được chấp nhận | Positive / Negative | 1. Thêm với mật khẩu `abcde`, gửi<br>2. Thêm với mật khẩu `abcdef`, gửi | 5 và 6 ký tự | Lần 1: "Mật khẩu tối thiểu 6 ký tự". Lần 2: lưu thành công | BR-16 | P1 · BVA · Unit | Chưa chạy |
| **TC_STF_07** | Xóa nhân viên | Quản lý không tự xóa được tài khoản của mình | Negative | 1. Đăng nhập quản lý<br>2. Xóa chính mình | `manager` (id 1) | Thông báo "Không thể tự xóa tài khoản đang đăng nhập". Tài khoản còn | BR-17 | P1 · ST · Unit | Chưa chạy |
| **TC_STF_08** | Xóa nhân viên | Không xóa nhân viên đã có hóa đơn | Negative | 1. Xóa `staff1` | `staff1` (id 2, có hóa đơn 1, 2) | Thông báo "Không thể xóa: nhân viên đã có dữ liệu (hãy khóa tài khoản)". Tài khoản còn | BR-18 | P1 · ST · Integ | Chưa chạy |
| **TC_STF_09** | Xóa nhân viên | Xóa nhân viên chưa có hóa đơn | Positive | 1. Thêm nhân viên `Tạm`<br>2. Xóa `Tạm` | `Tạm` | Thông báo "Đã xóa nhân viên". Dòng biến mất | BR-18 | P1 · ST · Integ | Chưa chạy |
| **TC_STF_10** | Cấp lại mật khẩu | Cấp lại mật khẩu thành công | Positive | 1. Ghi lại hash hiện tại của `staff2`<br>2. Bấm "Cấp lại mật khẩu"<br>3. Kiểm tra DB và hộp thư giả lập | `staff2` (id 3) | Thông báo "Đã gửi mật khẩu mới tới ...". Hash đã đổi. Mật khẩu mới dài 8 ký tự, chỉ gồm chữ và số. Mail đến hộp thư test | BR-19 | P1 · EP · Integ | Chưa chạy |
| **TC_STF_11** 🔶 | Cấp lại mật khẩu | Gửi mail thất bại thì khôi phục mật khẩu cũ | Negative | 1. Ghi lại hash của `staff2`<br>2. Đặt `MAIL_USER` trống hoặc sai<br>3. Bấm "Cấp lại mật khẩu" | `staff2` | Thông báo "Gửi email thất bại, mật khẩu chưa bị thay đổi: ...". Hash trong DB bằng hash cũ | BR-19 | P1 · EG · Integ | Chưa chạy |
| **TC_STF_12** | Cấu hình | Thiếu biến môi trường mail thì không lộ mật khẩu | Negative | 1. Chạy ứng dụng không đặt `MAIL_PASSWORD`<br>2. Cấp lại mật khẩu<br>3. Đọc log Tomcat | Không có | Lỗi được báo. Log không chứa giá trị mật khẩu mail | BR-20, NFR-04 | P2 · SEC · Manual | Chưa chạy |

---

## 6. HÓA ĐƠN (12 CA)

| Test Case ID | Chức năng | Mô tả ca kiểm thử | Loại | Các bước thực hiện | Dữ liệu kiểm thử | Kết quả mong đợi | BR / NFR | P · Kỹ thuật · Tầng | Trạng thái |
| :---: | :---: | :--- | :---: | :--- | :--- | :--- | :---: | :---: | :---: |
| **TC_BILL_01** | Danh sách | Hóa đơn mới nhất hiển thị trước | Positive | 1. Đăng nhập quản lý<br>2. Mở `/manager/bills` | Dữ liệu mẫu | 3 dòng theo thứ tự id 3, 2, 1. Mỗi dòng có tổng tiền và tên nhân viên | BR-21 | P1 · EP · Integ | Chưa chạy |
| **TC_BILL_02** | Danh sách | Phân trang 10 dòng mỗi trang | Positive | 1. Thêm 8 hóa đơn (tổng 11)<br>2. Mở trang 1, rồi trang 2 | 8 hóa đơn thêm | Trang 1: 10 dòng. Trang 2: 1 dòng | BR-21 | P1 · BVA · Integ | Chưa chạy |
| **TC_BILL_03** | Tổng tiền | Tổng tiền hóa đơn 1 | Positive | 1. Mở chi tiết hóa đơn 1 | Hóa đơn 1 | Tổng tiền **75.000** | BR-22 | P1 · EP · Integ | Chưa chạy |
| **TC_BILL_04** | Tổng tiền | Tổng tiền hóa đơn 2 có giảm giá | Positive | 1. Mở chi tiết hóa đơn 2 | Hóa đơn 2 | Tổng tiền **167.200**. Dòng Bạc xỉu có thành tiền 52.200 | BR-22 | P1 · EP · Integ | Chưa chạy |
| **TC_BILL_05** | Chi tiết | Chi tiết hóa đơn hiển thị đủ thông tin | Positive | 1. Mở chi tiết hóa đơn 2 | Hóa đơn 2 | 3 dòng. Có tên đồ uống, số lượng, đơn giá, giảm giá, thành tiền. Trạng thái "Đã thanh toán". Nhân viên "Trần Văn Nhân". Có ngày tạo | BR-23 | P1 · EP · Manual | Chưa chạy |
| **TC_BILL_06** | Hủy hóa đơn | Hủy hóa đơn đang xử lý | Positive | 1. Mở chi tiết hóa đơn 3<br>2. Bấm "Hủy hóa đơn" | Hóa đơn 3 (status 0) | Thông báo "Đã hủy hóa đơn #3". Trạng thái = 2 | BR-24 | P1 · ST · Integ | Chưa chạy |
| **TC_BILL_07** | Hủy hóa đơn | Hủy lại hóa đơn đã hủy | Negative | 1. Hủy hóa đơn 3 lần thứ hai | Hóa đơn 3 (status 2) | Thông báo "Không thể hủy hóa đơn (không tồn tại hoặc đã hủy)". Trạng thái vẫn 2 | BR-24 | P1 · ST · Integ | Chưa chạy |
| **TC_BILL_08** | Hủy hóa đơn | Hủy hóa đơn không tồn tại | Negative | 1. Gửi POST hủy với `id = 999` | id 999 | Thông báo "Không thể hủy hóa đơn...". Không đổi dữ liệu | BR-24 | P1 · BVA · Integ | Chưa chạy |
| **TC_BILL_09** 🔶 | Hủy hóa đơn | Hủy hóa đơn đã thanh toán. Hành vi theo Lab 6 là được phép (R-05, Q-2) | Positive | 1. Hủy hóa đơn 1 | Hóa đơn 1 (status 1) | Theo BR-24 hiện tại: hủy thành công, trạng thái = 2. **Ghi nhận R-05**: cần quyết định quy tắc này trước khi chốt | BR-24 | P2 · ST · Integ | Chưa chạy |
| **TC_BILL_10** | Hủy hóa đơn | Hóa đơn đã hủy không còn được tính doanh thu | Positive | 1. Hủy hóa đơn 2<br>2. Xem doanh thu ngày D−1 | Hóa đơn 2 | Doanh thu ngày D−1 bằng 0. Không còn dòng thống kê ngày này | BR-24, BR-29 | P1 · ST · Integ | Chưa chạy |
| **TC_BILL_11** | Hủy hóa đơn | Hủy phải dùng POST. GET không đổi trạng thái | Negative | 1. Mở `/manager/bills/cancel?id=3` bằng GET | Hóa đơn 3 | Không đổi trạng thái (vẫn 0). Không báo hủy thành công | BR-24, NFR-06 | P1 · SEC · Unit | Chưa chạy |
| **TC_BILL_12** | Chi tiết | Mở chi tiết hóa đơn không tồn tại | Negative | 1. Mở `/manager/bills/detail?id=999` | id 999 | HTTP 404 | BR-23 | P2 · BVA · Unit | Chưa chạy |

---

## 7. PHIẾU BÁN HÀNG (8 CA) · Đặc tả Lab 7, chưa có code

> Nhóm này chưa có trong code Lab 1–6. Giữ trong kế hoạch theo Q-3 (TEST_PLAN mục 9). Đưa ca vào vòng kiểm thử khi có code.

| Test Case ID | Chức năng | Mô tả ca kiểm thử | Loại | Các bước thực hiện | Dữ liệu kiểm thử | Kết quả mong đợi | BR / NFR | P · Kỹ thuật · Tầng | Trạng thái |
| :---: | :---: | :--- | :---: | :--- | :--- | :--- | :---: | :---: | :---: |
| **TC_SALE_01** | Tạo phiếu | Tạo phiếu mới | Positive | 1. Đăng nhập `staff1`<br>2. Tạo phiếu mới | `staff1` | Phiếu có `status = 0`, `user_id = 2`, không có dòng chi tiết | BR-25 | P2 · EP · Integ | Chưa chạy |
| **TC_SALE_02** | Thêm đồ uống | Thêm đồ uống vào phiếu | Positive | 1. Mở phiếu mới<br>2. Thêm "Cà phê sữa", số lượng 3 | Đồ uống id 2, SL 3 | Dòng: đơn giá 25.000, thành tiền 75.000. Tổng phiếu 75.000 | BR-25, BR-22 | P2 · EP · Integ | Chưa chạy |
| **TC_SALE_03** | Thêm đồ uống | Thêm trùng đồ uống thì cộng dồn | Positive | 1. Thêm tiếp "Cà phê sữa" SL 2 | SL 2 thêm | Không tạo dòng mới. SL = 5. Tổng 125.000 | BR-25 | P2 · BVA · Integ | Chưa chạy |
| **TC_SALE_04** | Thêm đồ uống | Số lượng không hợp lệ bị từ chối | Negative | 1. Thêm với SL lần lượt `0`, `-1`, `abc`, để trống | SL như trên | Mỗi lần: "Số lượng phải là số nguyên dương". Không ghi vào `bill_details` | BR-25 | P2 · EP · Unit | Chưa chạy |
| **TC_SALE_05** | Thanh toán | Thanh toán phiếu | Positive | 1. Thanh toán phiếu có tổng 125.000 | Phiếu từ TC_SALE_03 | `status = 1`, `checkout_at` có giá trị. Không sửa được nữa | BR-26 | P2 · ST · Integ | Chưa chạy |
| **TC_SALE_06** 🔶 | Thanh toán | Gửi POST thêm đồ uống vào phiếu đã thanh toán | Negative | 1. Gửi POST thêm đồ uống vào phiếu đã thanh toán | Phiếu đã thanh toán | Bị từ chối. Tổng tiền không đổi | BR-26 | P2 · SEC · Unit | Chưa chạy |
| **TC_SALE_07** | Ngừng bán | Không thêm đồ uống ngừng bán vào phiếu | Negative | 1. Thêm "Nước ép dưa hấu" vào phiếu đang xử lý | Đồ uống id 9 | Bị từ chối. Không tạo dòng | BR-27 | P2 · ST · Integ | Chưa chạy |
| **TC_SALE_08** | Lịch sử | Nhân viên chỉ xem phiếu của mình | Positive | 1. Đăng nhập `staff1`<br>2. Mở danh sách phiếu của mình | `staff1` | Thấy hóa đơn 1, 2. Không thấy hóa đơn 3 | BR-28 | P2 · EP · Integ | Chưa chạy |

---

## 8. THỐNG KÊ (9 CA)

| Test Case ID | Chức năng | Mô tả ca kiểm thử | Loại | Các bước thực hiện | Dữ liệu kiểm thử | Kết quả mong đợi | BR / NFR | P · Kỹ thuật · Tầng | Trạng thái |
| :---: | :---: | :--- | :---: | :--- | :--- | :--- | :---: | :---: | :---: |
| **TC_STAT_01** | Doanh thu | Doanh thu theo ngày trong khoảng | Positive | 1. Mở `/manager/stats/revenue?from=D−2&to=D−1` | Dữ liệu mẫu | 2 điểm: D−2 = **75.000** (1 hóa đơn). D−1 = **167.200** (1 hóa đơn). Hóa đơn 3 không tính | BR-29 | P1 · EP · Integ | Chưa chạy |
| **TC_STAT_02** | Ngày | Từ ngày lớn hơn đến ngày | Negative | 1. Mở `?from=D&to=D−5` | Khoảng ngược | Thông báo "Từ ngày phải nhỏ hơn hoặc bằng đến ngày". Không có dữ liệu | BR-30 | P1 · BVA · Unit | Chưa chạy |
| **TC_STAT_03** | Ngày | Định dạng ngày sai | Negative | 1. Mở `?from=2026/11/01&to=2026/11/30` | Định dạng `/` | Thông báo "Ngày không hợp lệ (định dạng yyyy-MM-dd)" | BR-30 | P1 · EP · Unit | Chưa chạy |
| **TC_STAT_04** | Ngày | Ngày đến được tính hết ngày | Positive | 1. Mở `?from=D−2&to=D−2` | Ngày của hóa đơn 1 | Có điểm D−2 = 75.000. Hóa đơn trong ngày được tính | BR-30 | P1 · BVA · Integ | Chưa chạy |
| **TC_STAT_05** | Ngày | Khoảng không có hóa đơn | Positive | 1. Mở `?from=2000-01-01&to=2000-01-31` | Khoảng rỗng | Danh sách rỗng. Không lỗi | BR-29 | P2 · BVA · Integ | Chưa chạy |
| **TC_STAT_06** | Ngày | Mặc định là 30 ngày gần nhất | Positive | 1. Mở `/manager/stats/revenue` không tham số | Ngày hiện tại | Ô "từ ngày" là hôm nay − 30, "đến ngày" là hôm nay | BR-30 | P2 · BVA · Unit | Chưa chạy |
| **TC_STAT_07** | Top bán chạy | Top 5 sắp theo số lượng, hòa thì theo doanh thu | Positive | 1. Mở `/manager/stats/top-drinks` với khoảng bao phủ D−2 đến D−1 | Dữ liệu mẫu | Thứ tự: Cà phê sữa (SL 3), Bạc xỉu (SL 2, 52.200), Cà phê đen (SL 2, 40.000), Sinh tố bơ (SL 1, 40.000), Trà đào cam sả (SL 1, 35.000) | BR-31 | P1 · EG · Integ | Chưa chạy |
| **TC_STAT_08** 🔶 | Top bán chạy | Hóa đơn đang xử lý không vào top | Negative | 1. Mở top với khoảng bao phủ cả hóa đơn 3 | Hóa đơn 3 có Trà vải × 2 | "Trà vải" không xuất hiện trong top | BR-29, BR-31 | P1 · EG · Integ | Chưa chạy |
| **TC_STAT_09** | Biểu đồ | Biểu đồ khớp với bảng số liệu | Positive | 1. Mở trang doanh thu có Internet<br>2. So nhãn và giá trị biểu đồ với TC_STAT_01 | Khoảng D−2 đến D−1 | Chart.js tải từ CDN. Cột khớp 75.000 và 167.200 | BR-32 | P2 · EP · Manual | Chưa chạy |

---

## 9. PHI CHỨC NĂNG VÀ BẢO MẬT (7 CA)

| Test Case ID | Chức năng | Mô tả ca kiểm thử | Loại | Các bước thực hiện | Dữ liệu kiểm thử | Kết quả mong đợi | BR / NFR | P · Kỹ thuật · Tầng | Trạng thái |
| :---: | :---: | :--- | :---: | :--- | :--- | :--- | :---: | :---: | :---: |
| **TC_NFR_01** | Bảo mật | Chuỗi injection trong ô tìm đồ uống | Negative | 1. Tìm `' OR 1=1 --` | Chuỗi injection | 0 kết quả. Bảng `drinks` không đổi | NFR-01 | P1 · SEC · Integ | Chưa chạy |
| **TC_NFR_02** | Bảo mật | Chuỗi script trong tên loại được hiển thị như văn bản | Negative | 1. Thêm loại `<script>document.title='x'</script>`<br>2. Mở danh sách loại | Chuỗi script | Hiển thị đúng chuỗi như chữ. Tiêu đề trang không đổi. Không chạy script | NFR-07 | P1 · SEC · Manual | Chưa chạy |
| **TC_NFR_03** | Lỗi CSDL | Khi CSDL tắt, trang lỗi không lộ thông tin nhạy cảm | Negative | 1. Tắt MySQL<br>2. Mở `/manager/drinks`<br>3. Xem nội dung trang lỗi | Không có | Trang lỗi chung. Không có mật khẩu, không có chuỗi kết nối. 🔶 Q-5: đặc tả chưa quy định nội dung trang lỗi | NFR-03 | P2 · SEC · Manual | Chưa chạy |
| **TC_NFR_04** | Lỗi CSDL | DAO ném lỗi, không nuốt lỗi | Negative | 1. Tắt MySQL<br>2. Gọi `DrinkDAO.findAll()` | Không có | Ném `RuntimeException`. Không trả về danh sách rỗng âm thầm | NFR-03 | P1 · EG · Integ | Chưa chạy |
| **TC_NFR_05** | Giao diện | Bảng đồ uống không tràn ngang ở 375px | Positive | 1. Đặt cửa sổ 375px<br>2. Mở `/manager/drinks` | Dữ liệu mẫu | Không có thanh cuộn ngang. Chữ đọc được | NFR-05 | P2 · EP · Manual | Chưa chạy |
| **TC_NFR_06** | Mã hóa | Tiếng Việt có dấu được lưu và hiển thị đúng | Positive | 1. Thêm loại `Cà phê sữa đá`<br>2. Đọc lại trong DB<br>3. Xem trên trang | `Cà phê sữa đá` | Trong DB và trên trang đều đúng dấu. Không có lỗi "?" | Quy ước §2.3 | P2 · EP · Integ | Chưa chạy |
| **TC_NFR_07** | Post-Redirect-Get | Tải lại trang sau khi lưu không gửi lại form | Positive | 1. Thêm loại mới, lưu<br>2. Tải lại trang | Loại `Tạm2` | Không hỏi gửi lại form. Không tạo thêm dòng | NFR-06 | P2 · ST · Manual | Chưa chạy |

---

## 10. TĨNH: MÃ NGUỒN (2 CA)

| Test Case ID | Chức năng | Mô tả ca kiểm thử | Loại | Các bước thực hiện | Dữ liệu kiểm thử | Kết quả mong đợi | BR / NFR | P · Kỹ thuật · Tầng | Trạng thái |
| :---: | :---: | :--- | :---: | :--- | :--- | :--- | :---: | :---: | :---: |
| **TC_STATIC_01** 🔶 | Bảo mật cấu hình | Không có mật khẩu CSDL và mail ghi cứng trong mã nguồn | Negative | 1. Tìm gán `password` và hằng số mật khẩu trong `src/main`<br>2. Kiểm tra cấu hình kết nối có đọc từ biến môi trường hoặc file ngoài repo | `src/main` | **Hiện tại Fail**: `JdbcUtil` trong repo có mật khẩu ghi cứng (R-02). Đạt khi cấu hình đọc từ ngoài mã nguồn và mật khẩu đã đổi | NFR-04, R-02 | P1 · SEC · Tĩnh | Chưa chạy |
| **TC_STATIC_02** | SQL | Không nối chuỗi người dùng vào câu SQL | Negative | 1. Tìm trong `dao/` các câu SQL có `+` ghép biến<br>2. Với mỗi câu, kiểm tra có dùng `?` | `src/main/.../dao/` | Mọi câu SQL có tham số dùng `?`. Không ghép chuỗi từ dữ liệu người dùng | NFR-01 | P1 · SEC · Tĩnh | Chưa chạy |

---

## TỔNG HỢP

| Nhóm | Số ca | Ca P1 | Ca 🔶 | Tầng chính |
| :--- | :---: | :---: | :---: | :--- |
| Đăng nhập, phân quyền, công khai | 17 | 15 | 0 | Unit, Integ |
| Trang chủ khách | 2 | 1 | 0 | Manual |
| Loại đồ uống | 9 | 7 | 1 | Unit, Integ |
| Đồ uống | 13 | 10 | 0 | Unit, Integ, Manual |
| Nhân viên | 12 | 11 | 1 | Unit, Integ, Manual |
| Hóa đơn | 12 | 10 | 1 | Unit, Integ |
| Phiếu bán hàng (chưa có code) | 8 | 0 | 1 | Unit, Integ |
| Thống kê | 9 | 6 | 1 | Unit, Integ, Manual |
| Phi chức năng và bảo mật | 7 | 3 | 0 | Integ, Manual |
| Tĩnh | 2 | 2 | 1 | Tĩnh |
| **Tổng** | **91** | **65** | **6** | |

> Các ca chưa chạy được trên repo hiện tại. Ca phiếu bán hàng (mục 7) chờ quyết định Q-3.
