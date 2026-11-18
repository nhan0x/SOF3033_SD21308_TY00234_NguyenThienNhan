# Nhật ký AI: Viết Unit Test cho Controller bằng JUnit 5 (Y3, Tuần 2)

> Công cụ: Claude Code (model Claude Sonnet 5.5) trong VS Code · Ngày: 18/11/2026 · Nhánh: `feat/Week-2`
> Các mục có dấu **[ĐIỀN]** là phần chỉ bạn mới biết. Hãy điền hoặc xóa trước khi nộp.

## 1. Ngữ cảnh và mục tiêu

- **Cần làm (theo README, Tuần 2):** viết Unit Test cho Controller trong `src/test/java/.../controllers/`; dùng `@ParameterizedTest` + `@CsvSource` kiểm tra biên form; dùng `assertThrows()` kiểm tra ngoại lệ; `@Timeout`; `mvn test` đạt 100% Pass.
- **Căn cứ thiết kế:** 47 ca tầng JUnit và 16 ca tầng API trong `docs/TEST_CASES.md` (sinh ở Tuần 1), ưu tiên cụm A (đặt hàng, voucher, thanh toán).
- **Khó khăn kỹ thuật:** các Servlet tự tạo Service bằng `new XxxService()` (không có DI) và Service gọi thẳng DAO/MySQL, nên không thể test Controller riêng lẻ nếu không có cách thay Service.

## 2. Câu lệnh đã gửi cho AI (Prompt)

> "Oke bây giờ là đến tiếp tục đến step tiếp theo trong readme.md là viết unit test cho controller và thực hiện đúng sản phẩm nghiệm thu trên github check list chuẩn rồi mới tự commit và tự tạo một branch riêng pull code từ nhánh main và đặt tên là feat/Week-2 rồi code commit trong đấy tự tạo pr và open pr tôi sẽ review và tự thao tác thủ công merge vào main nếu ch đat yêu cầu tôi sẽ closed pr và bạn làm lại."

**[ĐIỀN]** Nếu bạn gửi thêm prompt khác (ví dụ yêu cầu sửa sau khi review PR), ghi thêm vào đây.

## 3. Kết quả do AI sinh ra (Raw Output)

AI ghi trực tiếp vào repo (commit đầu tiên của nhánh `feat/Week-2`, **[ĐIỀN]** hash):

| Thành phần | Nội dung |
| :--- | :--- |
| `pom.xml` | Thêm `mockito-core`, `mockito-junit-jupiter` 5.17.0 (scope `test`); Surefire thêm `-Dnet.bytebuddy.experimental=true` |
| `controllers/support/ServletFixture` | Request, response, session giả: ghi lại redirect, `sendError`, JSP được forward, flash, thuộc tính request/session |
| `controllers/support/Harness` | Dựng Servlet với Service giả bằng `Mockito.mockConstruction` (chặn lời gọi `new`), kèm chế độ "partial" gọi hàm thật cho hàm thuần |
| `controllers/support/Users` | Người dùng mẫu |
| 10 test class `*ControllerTest` | Login, Register (+Logout), AccessControl (AuthFilter, AdminFilter), Cart, Checkout, Order, Shop (list, detail, quick search, wishlist), Account (profile, address), Admin (users, order, dashboard, `AdminOpsParams`), PaymentWebhook |
| `docs/TEST_CASES.md` mục 11 | Bảng truy vết TC → test class, sinh từ mã `TC_...` trong test; 11 ca đổi sang Pass, 50 ca "Một phần" |

**Kết quả đo được (18/11/2026):**

| Lệnh | JDK | Kết quả |
| :--- | :--- | :--- |
| `mvn clean test -Dtest='*ControllerTest'` | 27 (mặc định của Maven trên máy) | 345 lượt chạy, 0 lỗi, `BUILD SUCCESS` |
| `mvn clean test` (toàn bộ) | 17 (giống CI) | 381 lượt chạy (345 + 36 có sẵn), 0 lỗi, `BUILD SUCCESS` |

Số liệu: 85 phương thức `@Test`, 53 `@ParameterizedTest`, 6 `assertThrows`, `@Timeout` ở mọi class.

## 4. Chỉnh sửa và Lý do sửa

### 4.1. Điểm AI làm sai hoặc phải đổi cách làm trong phiên (có thật, nên đọc kỹ)

| # | Vấn đề | Cách phát hiện | Cách giải quyết |
| :-: | :--- | :--- | :--- |
| 1 | **Mockito không chạy trên JDK 27**: Byte Buddy báo "Java 27 (71) is not supported" khi chặn `new` | Chạy thử một test thăm dò trước khi viết hàng loạt | Thêm `-Dnet.bytebuddy.experimental=true` vào Surefire; kiểm lại trên JDK 17 như CI |
| 2 | Ý định ban đầu: tiêm Service giả bằng **reflection ghi đè trường `final`** | Cân nhắc: JDK mới đang siết việc sửa trường `final` bằng reflection | Đổi sang `mockConstruction` (không động vào trường `final`, không sửa mã ứng dụng) |
| 3 | `new PaymentConfig(...)` trả `null` khi gọi **sau** `mockStatic(PaymentConfig.class)`: 59 test lỗi cùng lúc | Chạy `CheckoutControllerTest` lần đầu | Tạo các `PaymentConfig` thật **trước** khi mock static (đã ghi chú trong code) |
| 4 | Bản nháp có đoạn "test giữ import" vô nghĩa, tham số `ignored` thừa, `assertTrue(true)` | Đọc lại trước khi chạy tổng | Xóa, vì test chỉ để cho đủ số lượng là tự lừa mình |
| 5 | Ca TC_CHECKOUT_09/10 (R-02) kỳ vọng "không được 500" nhưng ở tầng Controller chỉ thấy được việc Controller **không chặn** | Đọc `CheckoutServlet.shippingAddress` | Viết test **ghi nhận hành vi hiện tại** (đặt tên `..._gapR02`) và chú thích đổi assert khi sửa lỗi; không viết test fail để giữ "100% Pass" |

### 4.2. Kiểm tra test có thật sự bắt được lỗi (mutation thủ công)

AI cố ý phá 2 luật trong `src/main` rồi chạy test, sau đó hoàn tác bằng `git checkout`:

| Phá | Test báo lỗi |
| :--- | :--- |
| Bỏ chặn `//` và `/\` trong `LoginSupport.safeRedirect` | `safeRedirectOnlyAllowsInternalPaths` ca `//evil.com` và `/\evil.com` (2 ca fail) |
| Nới giới hạn địa chỉ checkout 200 → 255 | `invalidFieldIsRejected` ca `addressLine` 201 ký tự (1 ca fail) |

Sau khi hoàn tác: `git status src/main` không còn thay đổi.

### 4.3. Những gì **chưa** phủ (giới hạn trung thực)

- Phần cần database (trừ tồn kho thật, rollback transaction, đua dữ liệu TC_ORDER_04/05, ràng buộc DB): cần JUnit `@Tag("db")` với MySQL `saltair_test`, hoặc Selenium. Đây là 50 ca "Một phần" trong TEST_CASES.md.
- Hai ca R-02 (TC_CHECKOUT_09/10) **chưa được xác nhận có HTTP 500 hay không**: cần chạy trên DB thật.
- Luồng đăng nhập thành công (`LoginSupport.login` gọi `CartService` tĩnh) và `OrderPayServlet` chưa có test Controller.

### 4.4. Chỉnh sửa của riêng bạn

**[ĐIỀN]** Ví dụ: bạn đã đọc và hiểu `Harness`/`mockConstruction` chưa; chạy `mvn test` trên máy bạn có khớp 345/381 không; test nào bạn sửa hoặc thêm; nhận xét sau khi review PR.

### 4.5. Bài học

**[ĐIỀN]** 1 đến 2 câu bằng lời của bạn (gợi ý: vì sao phải chứng minh test bắt được lỗi, không chỉ thấy xanh).

Việc tiếp theo: Tuần 3 xây Selenium Page Object Model (`LoginPage`, `RegisterPage`, `ProductPage`); ghi tiếp `03_selenium_pom_ai.md`.
