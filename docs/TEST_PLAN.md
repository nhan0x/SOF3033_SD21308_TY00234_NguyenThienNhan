# KẾ HOẠCH KIỂM THỬ (TEST PLAN)

> Đặc tả: [SPECIFICATIONS.md](SPECIFICATIONS.md) · Kịch bản: [TEST_CASES.md](TEST_CASES.md)

| Mục | Giá trị |
| :--- | :--- |
| Dự án | PolyCoffee: website quản lý quán cà phê (Java Servlet/JSP, JDBC, MySQL 8, Tomcat 9) |
| Người phụ trách | Nguyễn Thiện Nhân · TY00234 · SD21308 (`nhan0x`) |
| Phiên bản | 4.0 (21/11/2026). Đi theo đặc tả 4.0. Thay thế bản 3.0 viết cho app console |

---

## 1. MỤC TIÊU

1. Xác nhận từng quy tắc nghiệp vụ **BR-01 đến BR-32** trong SPECIFICATIONS mục 5 hoạt động đúng, mỗi quy tắc có ít nhất một ca kiểm thử.
2. Xác nhận **validate dữ liệu** ở server theo bảng mục 6 của đặc tả, và phần validate phía client (HTML5).
3. Xác nhận **phân quyền** (công khai, khách, nhân viên, quản lý) ở mọi đường dẫn trong mục 4 của đặc tả. Đây là chức năng dễ lọt lỗi nhất.
4. Xác nhận các luồng tiền: tổng tiền hóa đơn, doanh thu, top bán chạy, và việc hóa đơn đã hủy không được tính.
5. Kiểm tra các rủi ro R-01 đến R-10 bằng ca kiểm thử có kết quả đo được.

## 2. PHẠM VI

| Trong phạm vi | Ngoài phạm vi |
| :--- | :--- |
| Đăng nhập, đăng xuất, phân quyền (BR-01 đến BR-07) | Hiệu năng, kiểm thử tải nhiều người dùng đồng thời |
| Trang chủ khách (BR-08) | Bảo mật chuyên sâu (pentest). Chỉ kiểm tra SQL injection, XSS, lộ mật khẩu ở mức cơ bản |
| Quản lý loại đồ uống (BR-09, BR-10) | Tương thích trình duyệt cũ (IE) |
| Quản lý đồ uống, bao gồm tìm kiếm, phân trang và ảnh (BR-11 đến BR-14) | Thanh toán điện tử, in hóa đơn |
| Quản lý nhân viên, bao gồm cấp lại mật khẩu qua email (BR-15 đến BR-20) | Dịch vụ gửi mail của bên thứ ba (Gmail SMTP). Chỉ kiểm tra tích hợp, không kiểm thử dịch vụ |
| Quản lý hóa đơn: danh sách, chi tiết, hủy (BR-21 đến BR-24) | Hạ tầng server, mạng, sao lưu CSDL |
| Thống kê doanh thu và top bán chạy (BR-29 đến BR-32) | |
| Phiếu bán hàng (BR-25 đến BR-28). **Đặc tả từ Lab 7–8, chưa có code.** Giữ trong kế hoạch, đánh dấu ở TEST_CASES | |
| Validate, NFR-01 đến NFR-07 | |

## 3. CHIẾN LƯỢC KIỂM THỬ

Kế hoạch đi theo các tầng của lộ trình 6 tuần. Mỗi tầng dùng một công cụ riêng.

| Tầng | Công cụ | Đối tượng | Ghi chú |
| :--- | :--- | :--- | :--- |
| Unit: Servlet | JUnit 5 + Mockito | Servlet và Filter (`AuthFilter`, `Utf8Filter`, `CategoryServlet`, `DrinkServlet`, `StaffServlet`, `BillManageServlet`, `StatsServlet`) | Mock DAO và request/response. Dùng `@ParameterizedTest` cho bảng validate, `assertThrows` cho lỗi, `@Timeout` cho mọi ca |
| Integration: DAO | JUnit 5 + MySQL 8 thật (DB `polycoffee_test`) | Các DAO (`UserDAO`, `CategoryDAO`, `DrinkDAO`, `BillDAO`, `BillDetailDAO`) | Không mock DAO. Mỗi ca bắt đầu từ dữ liệu mẫu được nạp lại |
| Hệ thống: trình duyệt | Thủ công (giai đoạn đầu), sau đó Selenium POM | Luồng đầu cuối theo vai trò khách, nhân viên, quản lý | Selenium được thêm ở tuần 3–4 theo lộ trình |
| Tĩnh | Tìm kiếm trong mã nguồn, `git log` | Mật khẩu cứng (NFR-04, R-02), tham số SQL nối chuỗi | Tự động hóa bằng script `grep` nếu cần |

> **Lưu ý về mock:** Lab 4–6 đặt `JdbcUtil` ở dạng tĩnh, gọi `DriverManager` trực tiếp. Vì vậy DAO được kiểm thử trên MySQL thật, không mock. Servlet được kiểm thử bằng mock DAO.

## 4. MÔI TRƯỜNG

| Thành phần | Giá trị |
| :--- | :--- |
| JDK | 17 (theo `pom.xml`). Đặc tả ghi Java 8+, xem R-07 về `javax` và `jakarta` |
| Maven | 3.6+. Lệnh: `mvn clean test` |
| Tomcat | 9.x, cho chạy thủ công và Selenium |
| MySQL | 8, cục bộ, cổng 3306 |
| Cơ sở dữ liệu | `polycoffee` (dữ liệu thật) và `polycoffee_test` (chỉ dùng cho test). **Cấu hình test phải trỏ tới `polycoffee_test`** |
| Tài khoản test | Dùng tài khoản trong dữ liệu mẫu, mật khẩu `123456`. Không dùng `root` cho ca test |
| Trình duyệt | Chrome bản mới nhất, máy tính và giả lập điện thoại 375px |
| Mail | Dùng máy chủ mail giả lập (ví dụ Mailpit). Đặt `MAIL_USER`, `MAIL_PASSWORD` trỏ tới máy đó |

## 5. KỸ THUẬT THIẾT KẾ CA KIỂM THỬ

| Kỹ thuật | Áp dụng cho |
| :--- | :--- |
| Phân vùng tương đương (EP) | Email, mật khẩu, tên, đơn giá, giảm giá, ngày thống kê, số lượng |
| Giá trị biên (BVA) | Tên 100 và 101 ký tự. Mật khẩu 5 và 6 ký tự. Giảm giá 0%, 100%, và ngoài khoảng. Đơn giá 0 và -1. Phân trang 10 và 11 dòng. Ngày thống kê từ bằng đến |
| Bảng quyết định | Đăng nhập: (email đúng/sai) × (mật khẩu đúng/sai) × (tài khoản bị khóa/không). Phân quyền: (vai trò) × (đường dẫn công khai/`/manager`) |
| Chuyển trạng thái | Hóa đơn: `0 → 1` (thanh toán), `0 → 2` (hủy), `1 → 2` (hủy), `2 → ?` (không hủy lại). Phiếu đã thanh toán không sửa được |
| Đoán lỗi (EG) | Lỗi CSDL bị nuốt (R-01), chuỗi SQL injection (NFR-01), chuỗi XSS (NFR-07), gọi POST trái quy định |
| Đơn lỗi | Mỗi ca chỉ đổi một yếu tố so với ca cơ sở |

Ca đánh dấu 🔶 là **giả thuyết lỗi**: ca nhằm xác nhận một rủi ro đã nêu ở đặc tả mục 9. Nếu kết quả khác mong đợi thì ghi nhận là lỗi thật.

## 6. DỮ LIỆU KIỂM THỬ

- **Dữ liệu nền:** nạp theo DDL và dữ liệu mẫu của đặc tả mục 3.2 trước mỗi lần chạy. Ví dụ: 3 tài khoản, 4 loại, 9 đồ uống (1 ngừng bán), 3 hóa đơn (2 đã thanh toán, 1 đang xử lý), 6 dòng chi tiết.
- **Dữ liệu bổ sung do ca kiểm thử tự tạo:** một tài khoản bị khóa (`enabled = 0`), các đồ uống và loại để thêm/xóa, 8 hóa đơn thêm để kiểm tra phân trang.
- **Nguyên tắc:** mỗi ca bắt đầu từ trạng thái nạp lại. Không ca nào dựa vào kết quả ca trước, để chạy theo thứ tự nào cũng được.
- Mọi số tiền trong kết quả mong đợi đã được tính từ dữ liệu mẫu (xem TEST_CASES, ghi chú trong từng ca).

## 7. PHÂN BỔ CA KIỂM THỬ

| Nhóm | Số ca (dự kiến) | Ghi chú |
| :--- | :---: | :--- |
| Đăng nhập, phân quyền, công khai | 17 | BR-01 đến BR-07. Gồm cả ca bảo mật redirect |
| Trang chủ khách | 2 | BR-08 |
| Loại đồ uống | 9 | BR-09, BR-10 |
| Đồ uống | 13 | BR-11 đến BR-14 |
| Nhân viên | 12 | BR-15 đến BR-20 |
| Hóa đơn | 12 | BR-21 đến BR-24, kể cả 🔶 R-05 |
| Phiếu bán hàng (đặc tả Lab 7) | 8 | BR-25 đến BR-28. Chờ xác nhận đưa vào phạm vi |
| Thống kê | 9 | BR-29 đến BR-32 |
| Phi chức năng và bảo mật | 7 | NFR-01 đến NFR-07, bao gồm SQL injection, XSS, PRG |
| Tĩnh (mã nguồn) | 2 | NFR-04, R-02 |

Tổng: **91 ca**, trong đó 65 ca P1 và 6 ca 🔶. Danh sách đầy đủ ở [TEST_CASES.md](TEST_CASES.md).

## 8. TIÊU CHÍ

| Loại | Điều kiện |
| :--- | :--- |
| Bắt đầu kiểm thử | Lệnh `mvn clean test` chạy được. Schema `polycoffee_test` đã tạo từ DDL mục 3.2 và nạp dữ liệu mẫu. Mail giả lập đang chạy |
| Hoàn thành kiểm thử | Mọi ca P1 đạt. Mọi ca 🔶 có kết luận (đạt, hoặc ghi nhận lỗi). Không còn lỗi Critical hoặc High chưa đóng |
| Đình chỉ | Lỗi làm hỏng schema test, hoặc test ghi nhầm vào `polycoffee` |

**Mức ưu tiên:** P1 là ca bắt buộc đạt (đăng nhập, phân quyền, tiền, validate, SQL injection). P2 là ca hồi quy. P3 là ca chấp nhận rủi ro.

## 9. RỦI RO KIỂM THỬ VÀ ĐIỂM CHƯA CHỐT

| Rủi ro hoặc điểm chưa chốt | Cách xử lý trong kế hoạch |
| :--- | :--- |
| Test ghi vào dữ liệu thật vì cấu hình trỏ `polycoffee` | Tách cấu hình test sang `polycoffee_test`. Ca tĩnh kiểm tra điều này |
| Ràng buộc DDL chưa chạy thử trên MySQL thật (đặc tả mục 3.2) | Chạy DDL trước. Ca ràng buộc chỉ kết luận sau khi DDL chạy được |
| **Q-1: `javax` (Tomcat 9) hay `jakarta` (Tomcat 11)?** | Kế hoạch theo đặc tả (`javax`, Tomcat 9). Nếu đổi, cập nhật mục 4 và chạy lại toàn bộ |
| **Q-2: Hủy hóa đơn đã thanh toán (BR-24, R-05)** | Ca TC_BILL_09 kiểm theo hành vi của Lab 6 và ghi nhận là 🔶. Nếu quy tắc đổi, cập nhật đặc tả trước |
| **Q-3: Phiếu bán hàng (BR-25 đến BR-28) có thuộc phạm vi không?** | Giữ ca trong kế hoạch, ghi chú "chưa có code". Nếu không đưa vào phạm vi thì bỏ nhóm này khỏi báo cáo |
| **Q-4: Cắt khoảng trắng đầu cuối khi nhập tên** | Ca TC_CAT_02 giả định tên toàn khoảng trắng bị coi là rỗng. Cần xác nhận `ParamUtil` có cắt khoảng trắng hay không |
| **Q-5: Trang lỗi khi CSDL không khả dụng** | Đặc tả chưa quy định người dùng thấy gì. Ca TC_NFR_03 kiểm tra không lộ thông tin nhạy cảm, không kiểm tra nội dung trang |
| Mật khẩu `root` đã lộ trong repo public (R-02) | Đổi mật khẩu và chuyển cấu hình ra ngoài mã nguồn trước khi chạy ca tĩnh |
| Mật khẩu băm không có muối (R-04) | Ghi nhận, không kiểm thử chống tấn công bảng băm |
| Ảnh tải lên không có quy định định dạng file | Đặc tả chưa có quy tắc. Không viết ca kiểm tra định dạng cho đến khi bổ sung |

## 10. LỊCH TRÌNH

Lịch chia theo tuần của lộ trình SOF309 (xem `HUONG_DAN_CHI_TIET_6_TUAN.md`).

| Giai đoạn | Nội dung | Đầu ra |
| :--- | :--- | :--- |
| Tuần 1 (đặc tả và test plan) | Hoàn thiện SPECIFICATIONS 4.0, TEST_PLAN, TEST_CASES. Chốt Q-1 đến Q-5 | Ba file tài liệu đã nộp |
| Tuần 2 (Y3) | Unit test cho Servlet và Filter bằng JUnit 5 và Mockito. Bảng validate dùng `@ParameterizedTest`. Ca lỗi dùng `assertThrows` | `mvn test` đạt. Thư mục `src/test/java/.../controllers/` |
| Tuần 3 (Y4 phần 1) | Integration test cho DAO trên `polycoffee_test`. Page Object cho đăng nhập, loại, đồ uống, hóa đơn | Test DAO đạt. Thư mục `ui/pages/` |
| Tuần 4 (Y4 phần 2) | TestNG: nhóm `smoke` và `regression`, `@DataProvider` cho bảng đăng nhập, chụp ảnh khi lỗi | `testng.xml`, thư mục `screenshots/` |
| Tuần 5 (Y5) | GitHub Actions chạy `smoke` với Chrome headless. Đưa báo cáo lên Artifacts | Tab Actions xanh |
| Tuần 6 (Y6) | Đối chiếu lại ca kiểm thử với đặc tả. Cập nhật cột Trạng thái | Kết quả cuối |

> Lịch này phụ thuộc việc code `src/main` được hoàn thiện. Các ca trong TEST_CASES chưa thể chạy trên repo hiện tại, vì repo hiện chỉ có app console.
