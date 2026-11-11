# NHẬT KÝ DÙNG AI (AI LOG) REQ_001: Đăng nhập bằng email + mã 6 số, khung tài khoản dạng popover
> *Issue và PR của yêu cầu này nằm ở repo cũ `nhan0x/saltair-fashion-web`.*
> *Ví dụ đã điền theo mẫu `_TEMPLATE/04_AI_LOG.md`.*

---

## Lần 1: Phân tích yêu cầu từ ảnh chụp luồng đăng nhập mẫu, chia issue
* **Bước:** 2 Phân tích, 3 Tạo issue
* **Công cụ:** Claude Code

### 1. Ngữ cảnh & mục tiêu
- Có ảnh chụp luồng đăng nhập bằng mã của trang mẫu (bấm icon người, nhập email, nhận mã 6 số). Cần tách thành issue logic (#74) và issue giao diện (#75), có quy tắc cụ thể.

### 2. Câu lệnh đã gửi cho AI (Prompt)
> "Hiện tại tôi đang cần sửa lại giao diện những chỗ sau, và tôi cần bạn tạo issue để tôi tạo nhánh và create pr để code rõ ràng các phần giao diện và logic sau: ... icon user ... đăng nhập bằng email luôn ... lần lượt có các logic step như hình."

### 3. Kết quả do AI sinh ra (Raw Output)
- Hai issue #74 và #75 với các mục Mô tả, Cần làm, Nghiệm thu; đề xuất hạn mã 15 phút, sai 5 lần, gửi lại sau 60 giây, tối đa 5 mã/giờ, chỉ lưu băm SHA-256.
- Đề xuất cột `users.marketing_opt_in` và giữ mật khẩu song song.
- Đưa ra 3 phương án đăng nhập cho admin.

### 4. Chỉnh sửa của người làm & lý do sửa
- **Bỏ hẳn mật khẩu** thay vì giữ song song: tránh hai luồng đăng nhập, ít code phải bảo trì.
- **Chọn phương án 1 cho admin** (trang riêng, mã ngắn hạn hơn): tách rủi ro, admin không lẫn vào luồng khách.
- **Không thêm `marketing_opt_in`**: chỉ giữ cột thật sự dùng, để sơ đồ ERD và tài liệu gọn.
- **Thêm `auth.devCode`**: tài khoản mẫu trong `data.sql` là email giả nên không nhận được mail.

---

## Lần 2: Viết code và kiểm thử #74
* **Bước:** 4 Code, 5 Test
* **Công cụ:** Claude Code

### 1. Ngữ cảnh & mục tiêu
- Code toàn bộ #74 theo đặc tả, gỡ phần mật khẩu, cập nhật bộ kiểm thử toàn luồng sang đăng nhập bằng mã.

### 2. Câu lệnh đã gửi cho AI (Prompt)
> "Làm #74 trước"

### 3. Kết quả do AI sinh ra (Raw Output)
- Bảng `login_codes`, migration, `LoginCodeRepository`, `AuthService.requestCode/verifyCode`, 4 Servlet mới, sửa `AdminFilter`, xóa đăng ký/quên mật khẩu/đổi mật khẩu và `jbcrypt`.
- Bộ kiểm thử toàn luồng chạy 90/90.

### 4. Chỉnh sửa của người làm & lý do sửa
- **Những điểm AI làm sai hoặc thiếu:** lỗi tự phát hiện khi chạy thử: trang nhập mã dùng `${ctx}` trước khi `layout.tag` gán biến, form gửi sai đường dẫn. Đã sửa.
- **Rủi ro được ghi lại trong PR:** khi SMTP lỗi, trang admin chỉ báo lỗi với email admin nên có thể đoán email admin. Chấp nhận vì chỉ xảy ra khi cấu hình mail hỏng.
- **Sau khi merge:** trưởng nhóm sửa thêm kiểu trang nhập mã 6 ô và chống cache CSS cũ (commit `fix(auth)`), vì trình duyệt giữ bản `app.js` cũ làm icon người không mở được khung.

---

## Lần 3: Làm giao diện #75
* **Bước:** 4 Code, 5 Test
* **Công cụ:** Claude Code

### 1. Ngữ cảnh & mục tiêu
- Làm popover tài khoản và trang nhập mã 6 ô theo BR-17, BR-18, có số đo hiệu ứng từ trang mẫu.

### 2. Câu lệnh đã gửi cho AI (Prompt)
> "làm full issue đi rồi tạo pr để mình thấy được kết quả"

### 3. Kết quả do AI sinh ra (Raw Output)
- Popover trong `layout.tag`, CSS hiệu ứng 240ms `cubic-bezier(0.32, 0.72, 0, 1)`, JS mở/đóng, gửi email bằng `fetch`; trang nhập mã riêng 6 ô; các Servlet trả JSON khi gọi bằng `fetch`. Thử trên trình duyệt ở 1440px và 375px.

### 4. Chỉnh sửa của người làm & lý do sửa
- **Những điểm AI làm sai hoặc thiếu:** ô "Nhận ưu đãi" trong issue bị bỏ vì quyết định số 3 ở `02_PHAN_TICH.md`.
- **Sau khi merge:** icon người không mở được khung trên máy trưởng nhóm do trình duyệt giữ `app.js` cũ, đã thêm chống cache và chỉnh kiểu 6 ô.
