# YÊU CẦU REQ_001: Đăng nhập bằng email + mã 6 số, khung tài khoản dạng popover
> *Issue và PR của yêu cầu này nằm ở repo cũ `nhan0x/saltair-fashion-web`.*
> *Ví dụ đã điền đầy đủ theo mẫu `_TEMPLATE/01_YEU_CAU.md`. Yêu cầu mới copy thư mục `_TEMPLATE`, không copy thư mục này.*

---

## 1. THÔNG TIN
| Mục | Nội dung |
| :--- | :--- |
| **Mã yêu cầu** | REQ_001 |
| **Người yêu cầu** | Trưởng nhóm (`nhan0x`) |
| **Ngày nhận** | 10/11/2026 |
| **Nguồn** | Tin nhắn kèm ảnh chụp luồng đăng nhập của trang mẫu |
| **Mức ưu tiên** | Cao |
| **Hạn mong muốn** | Trước khi chốt giao diện khách hàng (25/11) |
| **Trạng thái** | **Đã nghiệm thu** |

## 2. NGUYÊN VĂN YÊU CẦU
> "... icon user ... xuất hiện theo kiểu drop down ... đăng nhập bằng email luôn ... lần lượt có các logic step như hình ... Hãy bắt đầu tạo issue."
>
> "Hãy bỏ hẳn mật khẩu, còn phần đăng nhập và login của admin thì bạn có phương án nào không?"

**Tài liệu tham chiếu:** ảnh chụp trang mẫu: bấm icon người → nhập email → nhận mã 6 số qua email → nhập mã → đăng nhập xong (chỉ mượn luồng và hiệu ứng, không dùng logo, chữ của trang mẫu).

## 3. MỤC TIÊU (USER STORY)
* Là **khách hàng**, tôi muốn **đăng nhập chỉ bằng email và mã gửi về hộp thư**, để **không phải nhớ mật khẩu**.
* Là **khách mới**, tôi muốn **nhập email là có tài khoản luôn**, để **không phải qua bước đăng ký riêng**.
* Là **quản trị viên**, tôi muốn **đăng nhập ở trang riêng an toàn hơn**, để **tách khỏi luồng của khách**.

## 4. TIÊU CHÍ CHẤP NHẬN CỦA NGƯỜI YÊU CẦU
- [x] Bấm icon người mở khung nhỏ, nhập email, sang trang nhập mã, nhập đúng mã là đăng nhập.
- [x] Email mới tự có tài khoản.
- [x] Không còn chỗ nào nhập mật khẩu.
- [x] Admin đăng nhập được ở trang riêng.
- [x] Tài khoản mẫu (email giả) vẫn đăng nhập được khi demo.

## 5. NGHIỆM THU (ĐIỀN Ở BƯỚC 7)
| Ngày demo | Người duyệt | Kết quả | Ghi chú |
| :--- | :--- | :---: | :--- |
| 11/11/2026 | Trưởng nhóm | Đạt | Sau merge phải tải lại cứng trình duyệt vì cache `app.js` cũ; đã sửa chống cache ở commit `fix(auth)` |
