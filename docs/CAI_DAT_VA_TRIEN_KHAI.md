# HƯỚNG DẪN CÀI ĐẶT VÀ TRIỂN KHAI POLYCOFFEE

Ứng dụng web quản lý quán cà phê: Java 17, Servlet/JSP (Jakarta), MySQL 8, Tomcat 10.1 trở lên (đã thử trên Tomcat 11).

## 1. Yêu cầu

| Thành phần | Phiên bản | Ghi chú |
| :--- | :--- | :--- |
| JDK | 17 | **Build bằng JDK 17.** Maven chạy trên JDK mới hơn (ví dụ 27) sẽ lỗi vì Lombok chưa tương thích |
| Maven | 3.6+ | |
| MySQL | 8.0.16+ | Cần để các ràng buộc `CHECK` có hiệu lực |
| Tomcat | 10.1 hoặc 11 | Dùng `jakarta.servlet`. Tomcat 9 trở xuống **không** chạy được bản này |

Nếu Maven mặc định dùng JDK khác 17, đặt trước khi build:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)   # macOS
```

## 2. Tạo cơ sở dữ liệu

```bash
mysql -u root -p --default-character-set=utf8mb4 < database/polycoffee.sql
```

> **Cảnh báo:** script `DROP` rồi tạo lại 5 bảng `categories`, `users`, `drinks`, `bills`, `bill_details` trong CSDL `polycoffee`. Nếu máy đã có CSDL cũ cùng tên, hãy sao lưu hoặc đổi tên CSDL ở hai dòng `CREATE DATABASE` và `USE` trong script (rồi đổi `db.url` cho khớp).

Dữ liệu mẫu gồm 3 tài khoản (mật khẩu `123456`), 4 loại, 9 đồ uống, 3 hóa đơn:

| Email | Vai trò |
| :--- | :--- |
| `manager@polycoffee.com` | Quản lý |
| `staff1@polycoffee.com`, `staff2@polycoffee.com` | Nhân viên |

## 3. Cấu hình kết nối (không ghi cứng trong mã nguồn)

Thứ tự ưu tiên: **biến môi trường** `DB_URL`, `DB_USER`, `DB_PASSWORD` → file `src/main/resources/db.properties` → mặc định (`localhost:3306/polycoffee`, user `root`, mật khẩu rỗng).

```bash
cp src/main/resources/db.properties.example src/main/resources/db.properties
# sửa db.user, db.password cho khớp máy
```

`db.properties` nằm trong `.gitignore`, **không commit**. Lưu ý file này sẽ được đóng vào WAR khi build, nên khi nộp hoặc chia sẻ WAR hãy dùng biến môi trường thay cho file.

## 4. Build và chạy

```bash
mvn clean package            # tạo target/PolyCoffee.war
cp target/PolyCoffee.war $CATALINA_HOME/webapps/
$CATALINA_HOME/bin/catalina.sh run      # Windows: bin\catalina.bat run
```

Mở <http://localhost:8080/PolyCoffee/>.

| Đường dẫn | Ai dùng |
| :--- | :--- |
| `/home` | Tất cả (thực đơn) |
| `/auth/login`, `/auth/logout` | Tất cả |
| `/manager/categories`, `/drinks`, `/staff`, `/bills` | Quản lý |
| `/manager/stats/revenue`, `/manager/stats/top-drinks` | Quản lý |

Nhân viên truy cập `/manager/*` nhận lỗi 403. Biểu đồ doanh thu dùng Chart.js từ CDN nên cần Internet.

## 5. Gửi email cấp lại mật khẩu

Thông tin mail đọc từ biến môi trường của tiến trình Tomcat, không nằm trong mã nguồn:

```bash
export MAIL_USER='tai-khoan@gmail.com'
export MAIL_PASSWORD='mat-khau-ung-dung'     # Gmail: dùng App Password
# Tùy chọn: MAIL_HOST (mặc định smtp.gmail.com), MAIL_PORT (mặc định 587)
```

Để thử mà không gửi thật, dùng máy chủ giả lập như Mailpit: `MAIL_HOST=localhost MAIL_PORT=1025` (bỏ trống `MAIL_USER`, gửi không xác thực).
Nếu chưa cấu hình hoặc gửi lỗi, mật khẩu cũ được khôi phục và hệ thống báo "Gửi email thất bại, mật khẩu chưa bị thay đổi".

## 6. Chạy kiểm thử

Test dùng CSDL riêng, **không dùng dữ liệu thật**:

```bash
# tạo CSDL test từ cùng script (chỉ đổi tên CSDL)
sed -E 's/^(CREATE DATABASE IF NOT EXISTS) polycoffee /\1 polycoffee_test /; s/^USE polycoffee;/USE polycoffee_test;/' \
  database/polycoffee.sql | mysql -u root -p --default-character-set=utf8mb4

export DB_URL='jdbc:mysql://localhost:3306/polycoffee_test?useSSL=false&allowPublicKeyRetrieval=true'
mvn clean test
```

## 7. Ảnh đồ uống

Ảnh tải lên lưu vào thư mục `images/` của ứng dụng đã triển khai và **mất khi deploy lại WAR** (BR-14, rủi ro R-06). Ảnh mẫu trong dữ liệu (`cafe-den.jpg`...) chưa có file, hệ thống tự hiện ảnh thay thế `no-image.svg` cho tới khi tải ảnh thật lên.

## 8. Những gì chưa có

- Phiếu bán hàng của nhân viên (BR-25 đến BR-28, Lab 7): chưa làm vì chưa nằm trong bảng đường dẫn của đặc tả (Q-3). Bảng và DAO (`BillDAO.create`, `BillDetailDAO.create`) đã sẵn sàng để bổ sung.
- Hủy được cả hóa đơn đã thanh toán, đúng theo BR-24 hiện tại (rủi ro R-05, câu hỏi Q-2).
