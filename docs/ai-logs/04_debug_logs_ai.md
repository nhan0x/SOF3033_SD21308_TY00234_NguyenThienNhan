# Nhật ký AI: Debug lỗi HTTP 500 khi mở trang chủ (không load được sản phẩm)

## 1. Ngữ cảnh & Mục tiêu
- Ngày: 25/11/2026. Chạy PolyCoffee bằng Smart Tomcat (Tomcat 11.0.27, JDK 17) trong IntelliJ.
- Hiện tượng: mở `http://localhost:8080/PolyCoffee` (chuyển sang `/home`) hiện trang "500 – Đã xảy ra lỗi", không hiển thị thực đơn. Trang `/auth/login` vẫn mở bình thường (HTTP 200).
- Mục tiêu: tìm nguyên nhân gốc bằng log của server, không sửa mò.

## 2. Câu lệnh đã gửi cho AI (Prompt)
> "Hiện tại đang không load được sản phẩm. Kiểm tra dưới server xem? Và vấn đề chính là ở đâu vậy? Ghi vào AI-log cho tôi."
> (kèm ảnh chụp màn hình trang 500)

## 3. Kết quả do AI sinh ra (Raw Output)

**Các bước kiểm tra và kết quả**

1. Tomcat đang chạy (tiến trình java, cổng 8080 LISTEN) và MySQL đang chạy. Vậy không phải lỗi "server chưa bật" hay "mất kết nối CSDL".
2. Access log (`~/.SmartTomcat/.../PolyCoffee/logs/localhost_access_log.2026-11-25.txt`):
   - `GET /PolyCoffee/home` → **500**, `GET /PolyCoffee/auth/login` → 200.
   - Chỉ trang có truy vấn bảng `drinks` bị lỗi.
3. `localhost.2026-11-25.log`, lặp lại ở mọi lần tải trang:
   ```
   SEVERE ... Servlet.service() for servlet [com.polycoffee.Servlet.HomeServlet] threw exception
   java.lang.RuntimeException: Lỗi truy vấn CSDL
   Caused by: java.sql.SQLSyntaxErrorException: Unknown column 'available' in 'where clause'
   ```
4. Truy vết trong mã: `HomeServlet.java:35` gọi `DrinkDAO.findAvailable()` → `DrinkDAO.java:23`
   `SELECT * FROM drinks WHERE available = 1 ORDER BY category_id, id`.
5. Đối chiếu cấu trúc bảng `drinks` trong CSDL **thật** (`polycoffee` trên MySQL cục bộ) với `database/polycoffee.sql`:

   | | Script `database/polycoffee.sql` (code đang dùng) | CSDL thật trên máy |
   | :--- | :--- | :--- |
   | giá | `unit_price DECIMAL(18,0)` | `price INT` |
   | giảm giá | `discount DOUBLE` | (không có) |
   | trạng thái bán | `available BOOLEAN` | `active TINYINT(1)` |
   | mô tả | (không có) | `description` |

   Các bảng khác cũng lệch (ví dụ `bills.create_at`/`code`, `bill_details.quanity`). Thời điểm tạo bảng trong CSDL thật là **12/11/2026**, tức là CSDL của phiên bản cũ.

**Kết luận – vấn đề chính**

Không phải lỗi logic Java hay lỗi Tomcat. **Cấu trúc CSDL `polycoffee` trên máy là bản cũ (tạo ngày 12/11), còn mã nguồn mới (Entity/DAO) viết theo schema trong `database/polycoffee.sql`. Script này chưa từng được chạy lên máy**, nên câu lệnh `WHERE available = 1` tìm cột không tồn tại → MySQL báo `Unknown column` → DAO bọc thành `RuntimeException` → trang 500.

Không chỉ trang chủ: mọi chức năng đụng tới `drinks`, `bills`, `bill_details`, `categories`, `users` đều sẽ lỗi tương tự vì schema lệch.

**Hướng xử lý (AI chưa thực hiện, chờ sinh viên quyết định)**

- Script `polycoffee.sql` có `DROP TABLE` rồi tạo lại 5 bảng, nên **sẽ xóa dữ liệu cũ** (hiện có 20 đồ uống, 20 user). AI không tự chạy vì đây là thao tác phá dữ liệu.
- Cách A (nếu dữ liệu cũ không cần): `mysql -u root -p --default-character-set=utf8mb4 < database/polycoffee.sql`, rồi reload ứng dụng.
- Cách B (nếu muốn giữ dữ liệu cũ): đổi tên CSDL trong script (hai dòng `CREATE DATABASE`/`USE`) và `db.url` trong `db.properties` sang CSDL mới, ví dụ `polycoffee_v2`.
- Đây đúng là cảnh báo đã ghi ở mục 2 của `docs/CAI_DAT_VA_TRIEN_KHAI.md`.

## 4. Chỉnh sửa của Sinh viên & Lý do sửa
- Quyết định của sinh viên: chọn **Cách A** (dữ liệu cũ không cần giữ).
- Đã thực hiện: chạy `database/polycoffee.sql` bằng `mysql --default-character-set=utf8mb4`. Script chạy không lỗi.
- Kiểm chứng sau khi chạy: bảng `drinks` có đủ cột `unit_price`, `discount`, `available`; dữ liệu mẫu gồm 9 đồ uống và 3 user; `GET /PolyCoffee/home` trả **HTTP 200** (trước đó là 500). Không cần restart Tomcat vì ứng dụng mở kết nối mới theo từng truy vấn.
- Những điểm AI làm sai hoặc thiếu: ban đầu AI chỉ chẩn đoán và chờ xác nhận trước khi chạy script `DROP` (đúng, vì thao tác này xóa 20 đồ uống và 20 user cũ).
- Bài học: khi đổi schema phải có bước migrate/chạy lại script trên mọi máy; trang 500 chỉ nói "đã xảy ra lỗi", **nguyên nhân thật nằm ở `localhost.<ngày>.log` (stack trace), không phải ở trình duyệt**.
- Cách đọc nhanh lần sau: mở `~/.SmartTomcat/<project>/<app>/logs/localhost.<ngày>.log`, tìm dòng `Caused by:` ở cuối mỗi stack trace.
