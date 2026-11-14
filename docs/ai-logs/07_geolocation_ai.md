# Nhật ký AI: Chức năng "Dùng vị trí hiện tại" ở trang thanh toán (nhánh `feature/geolocation-checkout`)

Công cụ: Claude Code (model Claude Fable 5.2, sau đó chuyển sang Claude Sonnet 5.5). Ngày làm: 12/11/2026.

## 1. Ngữ cảnh & Mục tiêu
- Chức năng: nút "Dùng vị trí hiện tại" ở `checkout.jsp` lấy tọa độ trình duyệt, tra địa chỉ qua OpenStreetMap (Nominatim) rồi điền form địa chỉ giao hàng.
- Phiên này làm 3 việc:
  1. Kiểm tra phần geolocation đã triển khai tới đâu.
  2. Thêm cột tọa độ vào database.
  3. Bỏ `data.sql` để dùng dữ liệu thật, sửa các tài liệu liên quan.

## 2. Câu lệnh đã gửi cho AI (Prompt)
> "check thử chức năng geolocation triển khai tới đâu rồi"

> "đầu tiên là add fields vào table database trước nhé."

> "chốt lại thành một script duy nhất. Ko ghi là có version V gì nha"

> "1 script là đủ và sắp tới chắc xoá luôn data.sql nhỉ?"

> "xoá luôn đc ko? dùng dữ liệu thật"

> "thực hiện theo ý bạn đi"

## 3. Kết quả do AI sinh ra (Raw Output)
**Kiểm tra tiến độ** (chỉ đọc code, chưa chạy trên trình duyệt):
- Đã có: nút và ghi chú quyền vị trí (`checkout.jsp`), CSS (`shopping.css`), JS lấy tọa độ + gọi Nominatim + điền `addressLine`, `ward`, `district`, `city` (`shopping.js`).
- Chưa có: phía server (Java, DB) chưa biết tọa độ; trang Sổ địa chỉ chưa có nút này.
- Điểm AI nghi ngờ (chưa kiểm chứng): thứ tự ưu tiên chọn phường (`quarter`/`neighbourhood` trước `suburb`) có thể điền sai ô Phường.

**Database** (`database/schema.sql`):
- `addresses`: thêm `latitude`, `longitude` `DECIMAL(10,7) NULL`.
- `orders`: thêm `shipping_latitude`, `shipping_longitude` `DECIMAL(10,7) NULL`.
- Mỗi bảng có CHECK: hai cột cùng NULL hoặc cùng có giá trị; vĩ độ trong ±90, kinh độ trong ±180.
- AI đã thử trên DB tạm: DB cũ + script cho cấu trúc giống schema mới, chạy script 2 lần vẫn an toàn, giá trị sai bị chặn. Sau đó áp dụng lên DB `saltair` (dữ liệu cũ giữ nguyên) và gộp thành một script duy nhất theo yêu cầu, cuối cùng chỉ giữ `schema.sql`.

**Dữ liệu mẫu và tài liệu**:
- Xóa `database/data.sql`.
- Cuối `schema.sql` thêm câu `INSERT` tạo Admin đầu tiên ở dạng comment.
- Sửa `docs/SPECIFICATIONS.md` (mục 3), `docs/TEST_PLAN.md`, `docs/TEST_CASES.md` (dòng Môi trường), `mail.properties.example`.

## 4. Chỉnh sửa của Sinh viên & Lý do sửa
- Những điểm AI làm sai hoặc thiếu: *(sinh viên tự điền)*
- Code / Tài liệu hoàn thiện sau khi bạn sửa: *(sinh viên tự điền)*
- Ghi chú để điền: AI chưa chạy thử nút trên trình duyệt và chưa viết code Java cho tọa độ; bước tiếp theo là `Address.java`, `Order.java`, repository, `CheckoutServlet`, form.
