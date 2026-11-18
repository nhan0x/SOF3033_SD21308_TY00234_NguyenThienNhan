# KẾ HOẠCH KIỂM THỬ (TEST PLAN)

> Đặc tả: [SPECIFICATIONS.md](SPECIFICATIONS.md) · Test case: [TEST_CASES.md](TEST_CASES.md) · Lộ trình: [HUONG_DAN_CHI_TIET_6_TUAN.md](HUONG_DAN_CHI_TIET_6_TUAN.md)

| Mục | Giá trị |
| :--- | :--- |
| Dự án | Saltair: website bán quần áo basic (Java 17, Servlet/JSP, MySQL 8, Tomcat 11, Flyway) |
| Người phụ trách | Nguyễn Thiện Nhân · TY00234 · SD21308 (`nhan0x`) |
| Môn / Tuần | SOF309 Kiểm thử nâng cao cùng AI · **Tuần 1** (02/11 đến 08/11, hoàn thiện 18/11/2026) |
| Phiên bản | 2.0. Thay thế bản 1.0 (viết cho cơ chế đăng nhập bằng mã 6 số đã bị gỡ khỏi code) |

---

## 1. MỤC TIÊU

1. Chứng minh các **quy tắc nghiệp vụ gây thiệt hại tiền bạc hoặc mất toàn vẹn dữ liệu** (đặt hàng, tồn kho, voucher, thanh toán payOS, phân quyền) hoạt động đúng.
2. Tìm lỗi **sớm và rẻ**: kiểm thử Service/Util bằng JUnit trước, Selenium chỉ dùng cho luồng người dùng thật sự cần trình duyệt.
3. Có **bằng chứng truy vết** từ từng quy tắc trong đặc tả đến từng test case, từng test script (Tuần 2 → 4) và kết quả CI (Tuần 5).
4. Đưa ra **lý do định lượng** cho việc dồn công sức vào đâu và bỏ qua gì: áp dụng nguyên lý *Defect Clustering* (mục 4) và *Exhaustive Testing is Impossible* (mục 5).

## 2. PHẠM VI

| Trong phạm vi | Ngoài phạm vi |
| :--- | :--- |
| Đăng nhập, đăng ký, phân quyền (`AuthFilter`, `AdminFilter`), phiên | Kiểm thử hiệu năng, tải |
| Giỏ hàng (DB và session), gộp giỏ khi đăng nhập | Kiểm thử bảo mật chuyên sâu (CSRF, pentest). Chỉ kiểm các lỗ hổng cơ bản có trong code: open redirect, IDOR, chữ ký webhook, XSS |
| Voucher (7 bước), phí ship, đặt hàng, hủy đơn, tồn kho, máy trạng thái đơn | Gọi **payOS thật** và gửi mail thật (dùng máy chủ giả, `auth.devCode=true`) |
| Thanh toán chuyển khoản: webhook, chữ ký, idempotent, `decide` | Đổi trả, hoàn tiền tự động, đăng nhập mạng xã hội |
| Hồ sơ, sổ địa chỉ, yêu thích, đánh giá | Tương thích nhiều trình duyệt (chỉ Chrome) |
| Quản trị: sản phẩm, danh mục, màu/size, voucher, người dùng, banner, blog, dashboard | Kiểm thử trên thiết bị thật (chỉ thu nhỏ cửa sổ 375px khi cần) |
| Migration Flyway V1 → V4 (dựng DB sạch, ràng buộc CHECK/UNIQUE/FK) | |

## 3. NGUYÊN LÝ KIỂM THỬ ÁP DỤNG

| Nguyên lý (ISTQB) | Áp dụng trong dự án này |
| :--- | :--- |
| **Exhaustive testing is impossible** | Mục 5: tính không gian đầu vào, rồi chọn mẫu bằng EP, BVA, bảng quyết định, đơn lỗi (single fault) |
| **Defect clustering** | Mục 4: lỗi dồn vào cụm Đặt hàng, Voucher, Thanh toán; phân bổ **54%** test case vào cụm này |
| Testing shows presence of defects | Mỗi ca đánh dấu 🔶 là *giả thuyết lỗi*; kết luận Pass không chứng minh hết lỗi |
| Early testing | Tuần 1 đã nêu 10 giả thuyết lỗi (R-01 đến R-10) ngay khi đọc code, trước khi viết test script |
| Pesticide paradox | Cuối Tuần 2 và Tuần 4 **thay đổi dữ liệu** (biên mới) và bổ sung ca cho mọi lỗi đã tìm thấy |
| Testing is context dependent | Ứng dụng JDBC thuần, không DI: Service gọi thẳng DAO nên unit test Service cần DB test (mục 6.2) |
| Absence-of-errors fallacy | Chỉ coi là xong khi các luồng **smoke** (mục 8) chạy được trên giao diện thật, không chỉ khi `mvn test` xanh |

---

## 4. DEFECT CLUSTERING: PHÂN TÍCH RỦI RO VÀ PHÂN BỔ CÔNG SỨC

Nguyên lý: một số ít module chứa phần lớn lỗi. Thay vì chia đều, **đo** từng module bằng bằng chứng có sẵn trong repo rồi dồn test vào nơi có điểm rủi ro cao.

### 4.1. Bằng chứng (đo ngày 18/11/2026 trên nhánh `feat/requirement`)

| Chỉ số | Auth + Account | Shop + Admin-catalog | Cart (giỏ, checkout, voucher) | Order + Payment | Admin (controller) |
| :--- | :---: | :---: | :---: | :---: | :---: |
| Dòng code (Service + DAO + Controller) | 1.411 | 3.029 | 1.399 | **2.051** | 1.657 (Servlet 1.453 + Service/DAO 204) |
| Điểm kiểm tra dữ liệu (`errors.put` / `new ...Exception`) | 36 | 56 | **51** | 17 | 1 |
| Transaction (`setAutoCommit(false)`) | 0 | 1 | 0 | **5** (OrderService 4, PaymentService 1) | 0 |
| Truy vấn khóa dòng (`FOR UPDATE` trong SQL) | 0 | 0 | 0 | **3** (sinh mã đơn, `lockByCode`, `payment_transactions`) | 0 |
| Phụ thuộc bên ngoài | SMTP (mail báo Admin) | Cloudinary | | **payOS (HTTP + webhook)** | |
| Hậu quả khi sai | Chiếm tài khoản | Hiển thị sai | **Sai tiền** | **Sai tiền, sai tồn kho, mất đơn** | Dữ liệu sai |

> Ghi chú đo: "Shop + Admin-catalog" gộp vì thư mục `shop/` chứa cả Service/DAO CRUD cho Admin. Cột "Admin" là các Servlet `controller/admin/**` cộng Service/DAO `admin/` (Service/DAO của Admin vận hành). Số liệu là **xấp xỉ theo thư mục**, dùng để so sánh tương đối, không phải thước đo chính xác. Lệnh đo nằm ở mục 11.

**Bằng chứng lịch sử:** `git log` có 30 commit, **chỉ 1 commit sửa lỗi** (`1569385 fix(order): tab "Tất cả" ... không hiện đơn đã hủy (#4)`) và nó nằm ở **Order** (`OrderService`, `OrderServlet`, `OrderRepository`). Mẫu quá nhỏ để thống kê, nhưng cùng chiều với số liệu bên trên.

**Bằng chứng từ đọc code (SPECIFICATIONS mục 11):** 10 giả thuyết lỗi R-01 đến R-10; trong đó **8/10 nằm ở Checkout, Order, Voucher, Payment và Auth** (R-01, R-02, R-03, R-05, R-07, R-08, R-09, R-10), không có giả thuyết nào ở trang hiển thị sản phẩm.

### 4.2. Điểm rủi ro = Khả năng lỗi (L) × Tác động (I), thang 1 đến 5

| Cụm | Module | L (vì sao) | I (vì sao) | Điểm | Tỷ trọng điểm | Số test case | Tỷ trọng test |
| :--- | :--- | :--- | :--- | :---: | :---: | :---: | :---: |
| **A1** | Order + Payment payOS | 5: 5/6 transaction, khóa dòng, đua dữ liệu, 3 hệ thống ngoài, webhook | 5: sai tiền, sai tồn, đơn mồ côi | **25** | 32% | 25 (ORDER 13 + PAY 12) | |
| **A2** | Checkout + Voucher + Cart | 4: 51 điểm kiểm tra, 7 bước voucher, 3 giới hạn độ dài lệch nhau (R-02, R-03) | 5: sai giá trị đơn | **20** | 26% | 37 (CART 8 + VOUCHER 16 + CHECKOUT 13) | |
| | **Cụm A cộng** | | | 45 | **58%** | **62** | **54%** |
| **B** | Auth + Account | 3: nhiều nhánh nhưng thuần logic | 5: chiếm tài khoản, vượt quyền | **15** | **19%** | **27** | **24%** |
| **C** | Quản trị CRUD | 3: nhiều validation, ít liên kết | 3: dữ liệu sai nhưng sửa được | **9** | **12%** | **13** | **11%** |
| **D** | Cửa hàng (đọc), đánh giá, vị trí | 3 / 2: truy vấn lọc phức tạp nhưng chỉ đọc | 2 / 1: hiển thị | **8** | **10%** | **12** | **11%** |
| | **Tổng** | | | **77** | 100% | **114** | 100% |

**Kết luận phân bổ:** Cụm A chiếm 36% dòng code nhưng nhận **54% test case** (1,5 lần tỷ lệ code). Cụm C + D chiếm 49% dòng code nhưng chỉ nhận **22% test case** (25 ca). Đây là *chủ ý*, không phải sơ suất: đọc danh sách sản phẩm sai thì khách tải lại trang; ghi sai đơn hàng thì mất tiền.

### 4.3. Hệ quả cho thiết kế test case

* Cụm A: **dùng đủ các kỹ thuật cùng lúc** (BVA + bảng quyết định + máy trạng thái + đoán lỗi) và là nơi duy nhất có ca đua dữ liệu (TC_ORDER_05, TC_PAY_08), rollback (TC_ORDER_04), idempotent (TC_PAY_08).
* **38 ca P1 (smoke)** chủ yếu rơi vào A (23) và B (11); cụm C chỉ 2, cụm D chỉ 2.
* Cụm D chỉ lấy đại diện (không thử tổ hợp bộ lọc): chấp nhận rủi ro tồn dư, ghi ở mục 10.

### 4.4. Vòng phản hồi (cập nhật sau mỗi lần chạy)

Sau Tuần 2 (JUnit) và Tuần 4 (Selenium), tính **mật độ lỗi = số lỗi / 1.000 dòng code** theo từng cụm và điền bảng dưới. Quy tắc điều chỉnh:

| Điều kiện | Hành động |
| :--- | :--- |
| Một cụm chứa **> 40%** tổng lỗi tìm thấy | Thêm 20% số ca cho cụm đó (đặc biệt quanh module chứa lỗi) |
| Một cụm có **0 lỗi** sau cả hai vòng | Giữ nguyên, không thêm ca (tránh *pesticide paradox*) |
| Mỗi lỗi tìm thấy | Viết thêm ≥ 1 ca hồi quy và 1 ca cho **mô-đun lân cận** (lỗi thường đi thành đàn) |

| Cụm | Lỗi sau Tuần 2 | Lỗi sau Tuần 4 | Lỗi / KLOC | Quyết định |
| :--- | :---: | :---: | :---: | :--- |
| A | _ | _ | _ | _ |
| B | _ | _ | _ | _ |
| C | _ | _ | _ | _ |
| D | _ | _ | _ | _ |

---

## 5. EXHAUSTIVE TESTING IS IMPOSSIBLE: CÁCH CHỌN MẪU

### 5.1. Không gian đầu vào (tính được, kiểm chứng được)

| Đầu vào | Phép tính | Số tổ hợp | Kết luận |
| :--- | :--- | :---: | :--- |
| Mật khẩu 8 ký tự ASCII in được (95 ký tự) | 95⁸ | **6,6 × 10¹⁵** | Ở 1.000 ca/giây mất ≈ 210.000 năm, mới chỉ độ dài 8 (hợp lệ là 8 đến 64) |
| Số điện thoại 10 chữ số | 10¹⁰ (hợp lệ `0[0-9]{9}`: 10⁹) | 10.000.000.000 | Chia thành 4 lớp tương đương |
| Form checkout của khách vãng lai, **mức lớp tương đương** | email 5 × tên 3 × SĐT 4 × địa chỉ 3 × tỉnh 3 × phường 2 × quận 2 × ghi chú 2 × thanh toán 2 × voucher 9 | **77.760** | Đơn lỗi (single fault): 1 + (4+2+3+2+2+1+1+1+1+8) = **26** ca |
| Bộ lọc sản phẩm | danh mục 5 × màu 7 × size 5 × sắp xếp 6 × giá 4 | **4.200** (12.600 nếu thêm 3 lớp trang) | Tổ hợp đôi (all-pairs) tối thiểu = 7 × 6 = **42**; chọn **3 ca** vì cụm D rủi ro thấp |
| Voucher: 7 điều kiện có/không | 2⁷ | 128 | Quy tắc "gặp lỗi đầu tiên thì dừng" nên chỉ còn **8 kết quả khác nhau** (hợp lệ + 7 lỗi): 8 ca + 1 ca kiểm thứ tự |
| Máy trạng thái đơn | 4 trạng thái × 5 hành động | 20 cặp | 6 chuyển hợp lệ (đều có ca), 14 chuyển không hợp lệ chia theo 3 nhánh kiểm tra trong code (`allowed.contains`, `== SHIPPING`, `switch`), mỗi nhánh có ca đại diện |
| Webhook payOS: phương thức × trạng thái đơn × số tiền | bảng quyết định `decide` | 6 kết quả | Đủ 6 + 3 biên số tiền (thiếu 1đ, đủ, dư 1đ) |

### 5.2. Kỹ thuật chọn mẫu và số ca thu được

| Kỹ thuật | Dùng cho | Số ca gắn nhãn |
| :--- | :--- | :---: |
| **Phân vùng tương đương (EP)** | Mọi trường nhập: mỗi lớp (hợp lệ, rỗng, sai định dạng, quá dài) một đại diện | 37 |
| **Giá trị biên (BVA)** | Mọi giới hạn trong SPECIFICATIONS mục 7: 7/8/64/65 (mật khẩu), 4/5/200/201 (địa chỉ), 999/1.000/99.999.999/100.000.000 (giá), 199.999/200.000 (đơn tối thiểu), 4/5/255/256 (lý do hủy)... Luôn thử **cả hai phía** biên | 41 |
| **Bảng quyết định (DT)** | Thứ tự 7 bước voucher, `decide` của payOS, phân quyền theo vai trò | 18 |
| **Chuyển trạng thái (ST)** | Đơn hàng, địa chỉ mặc định, ẩn/hiện, xóa mềm/khôi phục | 13 |
| **Đoán lỗi (EG)** | 10 giả thuyết R-01 đến R-10, đua dữ liệu, rollback, phát lại webhook | 12 |
| **An toàn cơ bản (SEC)** | Open redirect, IDOR, chữ ký webhook, XSS, chèn header | 11 |

(Một ca có thể gắn nhiều kỹ thuật nên tổng lớn hơn 114.)

### 5.3. Chỗ **không** kiểm thử và lý do (rủi ro tồn dư được chấp nhận)

* Không thử từng màu × size × danh mục ở bộ lọc (mục 5.1): lỗi ở đó chỉ làm sai danh sách hiển thị.
* Không thử mọi tổ hợp 24 sản phẩm × 276 biến thể: logic giống nhau, chọn 2 đến 3 biến thể đại diện (tồn 0, tồn thấp, tồn cao).
* Không test độ dài vượt `maxlength` ở trình duyệt bằng Selenium: dùng `API` (POST trực tiếp) vì trình duyệt chặn trước khi tới server.
* Không test CSRF, tải lớn, nhiều trình duyệt (ngoài phạm vi môn học).

---

## 6. CẤP ĐỘ KIỂM THỬ, CÔNG CỤ VÀ KẾ HOẠCH TỰ ĐỘNG HÓA

### 6.1. Phân tầng

| Tầng | Công cụ | Số ca | P1 | Tuần | Ghi chú |
| :--- | :--- | :---: | :---: | :---: | :--- |
| **JUnit** (Service, Util) | JUnit 5: `@ParameterizedTest` + `@CsvSource` cho BVA, `assertThrows` cho `ValidationException`/`VoucherException`, `@Timeout` | **47** | 11 | **2** | 5 ca (TC_PAY_01, 02, 03, 11, 12) đã có sẵn |
| **API** (HTTP trực tiếp) | `java.net.http.HttpClient` hoặc `curl` | **16** | 6 | 2 đến 3 | Vượt qua kiểm tra của trình duyệt: webhook, IDOR, tham số vượt `maxlength` |
| **Selenium POM** | Selenium 4.20 + TestNG (Tuần 4): `LoginPage`, `RegisterPage`, `ProductPage`, `CartPage`, `CheckoutPage`, `OrderPage`, `AdminPage` | **48** | 21 | **3 đến 4** | `groups = "smoke"` cho 21 ca P1; `dependsOnMethods` cho chuỗi đặt hàng → hủy đơn |
| **Thủ công** | Làm theo bảng | **3** | 0 | | Định vị (HTTPS/quyền), phiên 30 phút thật |

### 6.2. Khả năng kiểm thử của mã nguồn (quyết định cho Tuần 2)

* **Hàm thuần, không cần DB** (JUnit chạy mili giây, chạy được trên CI hiện tại): `AuthService.normalizeEmail`, `VoucherService.computeDiscount/shippingFee`, `PaymentService.decide/toPayOSCode/fromPayOSCode`, `PasswordUtil`, `SlugUtil`, `LoginSupport.safeRedirect` (cần test trong package `com.fashionweb.controller.auth` vì phạm vi package), `PayOSSignature`, `Page`, `WebUtil.parseInt`, `QuickSearchServlet.splitHighlight/foldOneToOne`.
* **Service cần DB** (Service tạo DAO trực tiếp, không có DI): `validate` voucher, `placeOrder`, `cancel`, `save` của Product/Voucher... Cần **MySQL `saltair_test`**. Gắn `@Tag("db")`. Hai hướng: (a) dùng MySQL thật cục bộ + service container trong CI; (b) không tái cấu trúc code ứng dụng, chỉ thêm test.
* **Servlet**: cần giả lập `HttpServletRequest`. `pom.xml` chưa có Mockito → **thêm `mockito-core` ở Tuần 2** (hoặc viết lớp giả `HttpServletRequest` tay).
* **CI hiện tại** (`mvn clean test`, không có MySQL): các test `@Tag("db")` và Selenium phải được loại khỏi lần chạy mặc định, sau đó Tuần 5 thêm `services: mysql` vào workflow.

### 6.3. Hợp đồng đặt tên (để truy vết)

Test ID `TC_<MODULE>_<NN>` ↔ tên phương thức JUnit/TestNG chứa đúng ID (ví dụ `TC_VOUCHER_03_minOrderBoundary`) và `@DisplayName("TC_VOUCHER_03 ...")`. Nhờ đó cột *Trạng thái* trong TEST_CASES.md điền được từ báo cáo Surefire.

---

## 7. MÔI TRƯỜNG VÀ DỮ LIỆU

| Mục | Quy định |
| :--- | :--- |
| Máy chạy | macOS, JDK 17, Maven, MySQL 8 cục bộ (cổng 3306), Tomcat 11, Chrome |
| CSDL | **`saltair_test`** (khác DB phát triển). Trỏ `db.url` bằng `db.properties`. Không bao giờ test trên DB dữ liệu thật |
| Dựng DB sạch | `DROP DATABASE IF EXISTS saltair_test;` rồi khởi động Tomcat: Flyway tự chạy V1 → V4 (log: `Flyway: đã chạy 4 migration mới`). Kiểm tra bảng `flyway_schema_history` có 4 dòng `success=1` |
| Dữ liệu nền | V2: 24 sản phẩm, 276 biến thể, 6 tài khoản, 5 voucher, 12 đơn (SPECIFICATIONS mục 10) |
| Tài khoản | `admin@saltair.vn` / `Admin@123`; `khach@saltair.vn` / `Khach@123`; `mai.pham@example.com` bị khóa |
| Cấu hình | `auth.devCode=true`; `payment.properties` **trống** (payOS tắt) cho phần lớn ca; khóa giả cho TC_PAY_04 đến 10 và TC_ORDER_09 |
| Cô lập dữ liệu | Ca làm thay đổi dữ liệu dùng **email ngẫu nhiên** (`tester<timestamp>@x.vn`) và đơn riêng; ca phá hủy (hủy đơn mẫu, khóa `khach@saltair.vn`) chạy cuối và **khôi phục** hoặc dựng lại DB trước lượt chạy kế |
| Giả lập payOS | Máy chủ HTTP giả (`com.sun.net.httpserver`, như `PayOSClientTest`) + ký webhook bằng `PayOSSignature.signData` với khóa giả |

---

## 8. LUỒNG SMOKE (7 LUỒNG BẮT BUỘC XANH TRƯỚC KHI MERGE)

| # | Luồng | Test case P1 |
| :-: | :--- | :--- |
| 1 | Khách xem hàng, tìm kiếm, thêm vào giỏ | TC_SHOP_01, TC_CART_01 |
| 2 | Đăng nhập, quay về đúng trang, giỏ được gộp | TC_AUTH_01, TC_AUTH_07, TC_CART_06 |
| 3 | Đặt hàng có voucher: ship = 0, tồn kho giảm | TC_VOUCHER_01, TC_CHECKOUT_01, TC_CHECKOUT_08, TC_ORDER_01 |
| 4 | Khách hủy đơn Chờ xác nhận: hoàn tồn và lượt voucher | TC_ORDER_06, TC_ORDER_08 |
| 5 | Admin đăng nhập, chuyển Đang giao rồi Hoàn thành | TC_AUTH_02, TC_AUTH_15, TC_ORDER_10 |
| 6 | Khách đánh giá đơn Hoàn thành | TC_REVIEW_01 |
| 7 | Tài khoản bị khóa không đăng nhập được; webhook thanh toán đúng | TC_AUTH_06, TC_ADM_08, TC_PAY_05, TC_PAY_07, TC_PAY_08 |

Ngoài ra bắt buộc xanh: toàn bộ ca 🔶 mức P1 (TC_CHECKOUT_09, TC_CHECKOUT_10, TC_ORDER_05, TC_ADDR_05, TC_ADDR_06, TC_AUTH_08): chúng kiểm tra **mất tiền hoặc 500**.

## 9. TIÊU CHÍ VÀO/RA

| | Tiêu chí |
| :--- | :--- |
| **Vào (bắt đầu chạy)** | Tomcat khởi động, log Flyway báo 4 migration, `/home` trả 200; đăng nhập được 2 tài khoản mẫu |
| **Dừng tạm** | > 30% ca P1 của một cụm fail do cùng một nguyên nhân gốc (sửa trước rồi chạy lại) |
| **Ra, Tuần 1** | `SPECIFICATIONS.md`, `TEST_PLAN.md`, `TEST_CASES.md` hoàn chỉnh; 36 test JUnit hiện có xanh |
| **Ra, Tuần 2** | `mvn test` 100% Pass ở các ca JUnit; ≥ 47 ca JUnit tự động |
| **Ra, Tuần 4** | 21 ca P1 Selenium `groups = "smoke"` xanh; có `testng.xml`; ảnh chụp khi fail |
| **Ra, Tuần 5** | GitHub Actions xanh trên `develop` và `main`; tải được artifact báo cáo |
| **Ra, hoàn tất** | 100% P1 Pass; ≥ 95% P2 Pass; mọi lỗi **High** đã sửa hoặc có quyết định ghi lại; log Tomcat không có `SEVERE` bất thường |

### Mức độ nghiêm trọng của lỗi

| Mức | Định nghĩa | Ví dụ |
| :--- | :--- | :--- |
| **Critical** | Mất tiền, mất/sai dữ liệu đơn hàng, vượt quyền | Hai đơn mua cùng món cuối; webhook sai chữ ký vẫn đánh dấu PAID |
| **High** | HTTP 500 ở luồng chính, rollback không đủ | TC_CHECKOUT_09/10, TC_ADDR_06 nếu xác nhận |
| **Medium** | Sai thông báo/ràng buộc nhưng có đường vòng; lỗ hổng cần điều kiện | TC_AUTH_13, TC_VOUCHER_16 |
| **Low** | Giao diện, văn bản | Sai chính tả thông báo |

## 10. RỦI RO DỰ ÁN

| Rủi ro | Khả năng | Giảm thiểu |
| :--- | :---: | :--- |
| Service không có DI nên khó test độc lập | Cao | Dùng DB test thật; ưu tiên hàm thuần; không sửa code ứng dụng chỉ để dễ test |
| Test làm bẩn dữ liệu, ca sau phụ thuộc ca trước | Cao | Email ngẫu nhiên, dựng lại DB trước lượt chạy; `dependsOnMethods` chỉ dùng cho chuỗi cố ý |
| CI không có MySQL và Chrome | Trung bình | Tuần 5: `services: mysql:8`, Chrome `--headless=new`, loại `@Tag("db")` khỏi mặc định |
| Gọi payOS thật gây tốn tiền/hỗn loạn | Thấp | Chỉ dùng máy chủ giả; không commit khóa thật (repo Public) |
| Mất tiến độ vì quá nhiều ca (114) | Trung bình | Làm theo thứ tự P1 → P2; P3 có thể bỏ nếu thiếu thời gian |
| Rủi ro tồn dư đã chấp nhận | | Bộ lọc sản phẩm không thử tổ hợp; không test nhiều trình duyệt; R-04 (đơn thứ 1000/ngày) và R-06 (CSRF) chỉ ghi nhận |

## 11. LỊCH VÀ SẢN PHẨM THEO TUẦN

| Tuần | Việc | Sản phẩm | Y |
| :---: | :--- | :--- | :---: |
| **1** (02/11 đến 08/11, hoàn thiện 18/11) | Đọc code, viết đặc tả, kế hoạch, 114 test case | `SPECIFICATIONS.md`, `TEST_PLAN.md`, `TEST_CASES.md`; `ai-logs/01_test_design_ai.md` | Y1, Y2 |
| 2 (09/11 đến 15/11) | JUnit 47 ca; thêm Mockito nếu cần | `src/test/java/.../controllers/`, `mvn test` xanh; `ai-logs/02` | Y3 |
| 3 (16/11 đến 22/11) | Selenium POM, `WebDriverWait` | `ui/pages`, kịch bản Chrome chạy mượt | Y4-P1 |
| 4 (23/11 đến 29/11) | TestNG: `groups=smoke`, `@DataProvider`, `ITestListener` chụp ảnh | `testng.xml`, `screenshots/`; `ai-logs/03` | Y4-P2 |
| 5 (30/11 đến 06/12) | GitHub Actions + MySQL + Chrome headless; debug với AI | Actions xanh, artifact; `ai-logs/04, 05` | Y5, Y6 |
| 6 (07/12 đến 10/12) | AI code review, slide, đóng gói | `ai-logs/06`, slide, zip | Y6 |

**Lệnh đo dùng ở mục 4.1** (chạy từ gốc dự án):

```bash
# Dòng code theo cụm
for m in auth account shop cart order; do
  echo "$m: $(find src/main/java -path "*/service/$m/*.java" -o -path "*/dao/$m/*.java" -o -path "*/controller/$m/*.java" | xargs cat | wc -l)"
done
# Điểm kiểm tra dữ liệu, transaction, khóa dòng
grep -rE 'errors\.put|new ValidationException|new VoucherException' src/main/java/com/fashionweb/{service,controller}/<module> | wc -l
grep -rn 'setAutoCommit(false)' src/main/java | cut -d: -f1 | sort | uniq -c
grep -rn 'FOR UPDATE' src/main/java | cut -d: -f1 | sort | uniq -c
git log --oneline | grep -iE 'fix'
```

## 12. TRUY VẾT: ĐẶC TẢ → TEST CASE

| Đặc tả (SPECIFICATIONS) | Test case |
| :--- | :--- |
| 6.1 FR-AUTH, 7.1 | TC_AUTH_01 đến 18 |
| 6.2 FR-ACC, 7.2 | TC_PROF_01 đến 03, TC_ADDR_01 đến 06 |
| 6.3 FR-SHOP | TC_SHOP_01 đến 06 |
| 6.4 FR-CART, 7.4 | TC_CART_01 đến 08 |
| 6.5 FR-CHECKOUT, 7.3 | TC_CHECKOUT_01 đến 13, TC_GEO_01 đến 02 |
| 6.6 FR-VOUCHER | TC_VOUCHER_01 đến 16, TC_ADM_09 |
| 6.7 FR-ORDER, 9 | TC_ORDER_01 đến 13 |
| 6.8 FR-PAY | TC_PAY_01 đến 12 |
| 6.9 FR-REVIEW, 7.4 | TC_REVIEW_01 đến 04 |
| 6.10 FR-ADMIN, 7.5 | TC_ADM_01 đến 13 |
| 3. Migration và ràng buộc DB | TC_ORDER_01, 02 (CHECK `total`), TC_ORDER_05 (`stock >= 0`), TC_PAY_08 (UNIQUE `provider, tx_id`) |
| 11. R-01 đến R-10 | TC_AUTH_13, TC_CHECKOUT_09, 10, TC_ADDR_06, TC_VOUCHER_16, TC_AUTH_17, TC_PAY_10, TC_ORDER_09, TC_ORDER_05 |
