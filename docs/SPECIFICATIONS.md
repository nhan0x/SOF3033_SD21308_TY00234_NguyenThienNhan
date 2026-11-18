# ĐẶC TẢ HỆ THỐNG SALTAIR (SPECIFICATIONS)

> Tài liệu đầu vào cho bước Y2 (AI RAG sinh Test Plan và Test Case). Mọi nội dung được **đối chiếu trực tiếp với mã nguồn** trong `src/main/` (đọc toàn bộ Servlet, Service, DAO, Filter, migration, JSP), không chép lại từ tài liệu cũ.
> Liên quan: [TEST_PLAN.md](TEST_PLAN.md) · [TEST_CASES.md](TEST_CASES.md) · [HUONG_DAN_CHI_TIET_6_TUAN.md](HUONG_DAN_CHI_TIET_6_TUAN.md)

| Mục | Giá trị |
| :--- | :--- |
| Đề tài | Saltair: website bán quần áo basic unisex (thương hiệu giả, chỉ dùng để học) |
| Sinh viên | Nguyễn Thiện Nhân · TY00234 · SD21308 |
| Phiên bản đặc tả | 2.0 (Tuần 1, 18/11/2026) |
| Nguồn sự thật | Mã nguồn tại nhánh `feat/requirement`; các giá trị bên dưới là giá trị **thực tế trong code** |

> **Lưu ý khác biệt với bản đặc tả cũ (v1.0):** bản cũ mô tả đăng nhập bằng *email + mã 6 số*, "không làm thanh toán online" và dựng DB bằng `database/schema.sql`. Code hiện tại đăng nhập bằng **email + mật khẩu (PBKDF2)**, có **thanh toán chuyển khoản payOS** và dùng **Flyway migration**. Các phần đó đã được viết lại.

---

## 1. TECH STACK

| Lớp | Công nghệ | Phiên bản (theo `pom.xml`) | Ghi chú |
| :--- | :--- | :--- | :--- |
| Ngôn ngữ | Java | 17 (`maven.compiler.release`) | |
| Web | Jakarta Servlet / JSP / JSTL | 6.1.0 / 4.0.0 / 3.0.2 (impl Glassfish 3.0.1) | Servlet và JSP do container cung cấp (`provided`) |
| Container | Apache Tomcat | 11 | WAR `saltair.war`, context path `/saltair` |
| CSDL | MySQL | 8 (`mysql-connector-j` 9.1.0) | JDBC thuần, `PreparedStatement`, **không dùng ORM** |
| Migration | Flyway | 10.20.1 (`flyway-core` + `flyway-mysql`) | Chạy tự động lúc Tomcat khởi động |
| Mail | Angus Mail | 2.0.3 | Gửi mail báo Admin đăng nhập (SMTP Gmail) |
| JSON / HTTP | Jackson 2.15.2, `java.net.http.HttpClient` | | Gọi và nhận webhook payOS |
| QR | ZXing core | 3.5.3 | Vẽ mã QR chuyển khoản thành SVG |
| Giao diện | HTML, CSS, JS thuần, JSP tag file (`WEB-INF/tags`) | | Không framework JS |
| Ảnh | Cloudinary (tùy chọn) hoặc thư mục `assets/images` | | Cấu hình bằng `cloudinary.properties` |
| Thanh toán | payOS (chuyển khoản VietQR) | API `https://api-merchant.payos.vn` | Cấu hình bằng `payment.properties` |
| Kiểm thử | JUnit Jupiter 5.10.2, Selenium Java 4.20.0 | | `scope=test`; TestNG sẽ thêm ở Tuần 4 |
| CI | GitHub Actions | `.github/workflows/test.yml` | `mvn clean test` trên push/PR vào `main`, `master`, `develop` |

**Cách chạy:** (1) chép `db.properties`, `mail.properties`, `cloudinary.properties`, `payment.properties` từ các file `.example` trong `src/main/resources/` (các file thật đã nằm trong `.gitignore`); (2) `mvn package`; (3) deploy `target/saltair.war` lên Tomcat 11; (4) mở `http://localhost:8080/saltair/home`. **Không cần tạo database trước**: `createDatabaseIfNotExist=true` trong URL và Flyway tạo bảng.

---

## 2. TỔ CHỨC MÃ NGUỒN

### 2.1. Cây thư mục

```text
src/main/
├── java/com/fashionweb/
│   ├── controller/   Servlet nhận request, chuyển view (chia theo chức năng)
│   │   ├── auth/  account/  shop/  cart/  order/        Phía khách
│   │   └── admin/ {catalog, sales, system}              Phía quản trị (+ AdminOpsParams)
│   ├── service/      Nghiệp vụ + validation (auth, account, shop, cart, order, admin)
│   ├── dao/          Truy cập MySQL bằng JDBC (cùng cách chia chức năng)
│   ├── model/        POJO: User, Product, ProductVariant, Order, OrderDetail, Cart, CartItem, Voucher, Review, ...
│   ├── dto/          DashboardStats
│   ├── constant/     AppConstant + enum Role, OrderStatus, PaymentMethod, PaymentStatus, DiscountType, CancelledBy
│   ├── exception/    ValidationException, VoucherException, DataAccessException
│   ├── filter/       EncodingFilter, StaticCacheFilter, AuthFilter, AdminFilter
│   ├── listener/     AppStartupListener (chạy Flyway, nạp menu và thanh thông báo)
│   └── util/         DBContext, DBMigration, PasswordUtil, PayOSSignature, PaymentConfig, QrSvg,
│                     ImageUploadUtil, CloudinaryUtil, MailUtil, SlugUtil, Page, WebUtil
├── resources/
│   ├── db/migration/ V1__tao_bang.sql, V2__du_lieu_mau.sql, V3__chuyen_khoan.sql, V4__payos.sql
│   └── *.properties  (+ *.properties.example)
└── webapp/
    ├── WEB-INF/{web.xml, views/{auth,account,shop,cart,order,admin,error,common}, tags/}
    └── assets/{css, js, images, brand}
src/test/java/com/fashionweb/{util,service/order}/   36 test JUnit hiện có (Tuần 1: PASS)
```

### 2.2. Quy tắc kiến trúc (MVC + Service + DAO)

* Luồng bắt buộc: **`Servlet → Service → DAO → MySQL`**. Servlet không viết SQL; JSP không gọi Service/DAO; DAO không chứa quy tắc nghiệp vụ.
* **Validation đặt ở Service** (Servlet chỉ ép kiểu tham số và bắt exception). Ngoại lệ: `CheckoutServlet` tự kiểm tra form địa chỉ giao hàng (xem mục 7.3).
* **Transaction do Service mở** (`Connection.setAutoCommit(false)`), DAO nhận `Connection`: `OrderService.placeOrder/cancel/confirmReceived/updateStatus`, `PaymentService.recordTransfer`, `ProductService.save`.
* **Chống đua dữ liệu:** `SELECT ... FOR UPDATE` khi khóa đơn (`lockByCode`) và khi sinh mã đơn; `UPDATE ... WHERE stock >= ?` khi trừ tồn; `UPDATE ... WHERE used_count < usage_limit` khi tăng lượt voucher.
* Mẫu **PRG** (Post-Redirect-Get) với thông báo flash (`WebUtil.flash` → `flashMessage`, `flashType`: `success | error | info`).

### 2.3. Filter, Listener, Session

| Thành phần | Vai trò | Chi tiết |
| :--- | :--- | :--- |
| `EncodingFilter` (`/*`) | Đọc request bằng UTF-8 | Chạy đầu tiên (thứ tự khai báo trong `web.xml`) |
| `StaticCacheFilter` (`/assets/*`) | `Cache-Control: no-cache` | Tránh dùng CSS/JS cũ |
| `AuthFilter` | Bắt buộc đăng nhập | `/order`, `/orders`, `/profile`, `/addresses`, `/wishlist`. Chưa đăng nhập → `302 /login?redirect=<trang cần quay lại>`; với POST dùng `Referer` cùng host |
| `AdminFilter` | Chỉ `ADMIN` | `/admin`, `/admin/*`. Chưa đăng nhập → `302 /login?redirect=...`; khác ADMIN → **403**. `/admin/login*` được cho qua |
| `AppStartupListener` | Khởi động | Chạy `DBMigration.migrate()`, nạp `navCategories` và `announcements` vào application scope. Lỗi migration thì ứng dụng **không khởi động** |
| Session | 60 phút (web.xml), cookie `HttpOnly`, chỉ COOKIE | Admin đặt lại 30 phút sau khi đăng nhập (`ADMIN_SESSION_SECONDS=1800`) |

Thuộc tính session: `currentUser` (User), `cartCount`, `guestCart` (`Map<variantId, qty>`), `cartNote`, `guestOrderCode`, `payLink:<mã đơn>` (link payOS còn hạn), `flashMessage/flashType`.

### 2.4. Hằng số (`AppConstant`)

| Hằng | Giá trị | Ý nghĩa |
| :--- | :---: | :--- |
| `SHIPPING_FEE` | 30.000 | Phí ship cố định (VNĐ); = 0 khi dùng voucher hợp lệ |
| `PAGE_SIZE` | 12 | Sản phẩm mỗi trang `/products` |
| `ADMIN_PAGE_SIZE` | 10 | Dòng mỗi trang ở bảng Admin |
| `ORDER_PAGE_SIZE` | 5 | Đơn mỗi trang `/orders` |
| `ADMIN_SESSION_SECONDS` | 1800 | Hết phiên Admin |
| `CartService.MAX_ADD` | 10 | Số lượng tối đa mỗi lần thêm vào giỏ |

---

## 3. CƠ SỞ DỮ LIỆU VÀ MIGRATION

### 3.1. Cơ chế Flyway

* `AppStartupListener.contextInitialized` → `DBMigration.migrate()` → `Flyway.configure(...).dataSource(url, user, pass).locations("classpath:db/migration").load().migrate()`.
* Flyway ghi lịch sử vào bảng `flyway_schema_history`; file đã chạy **không chạy lại**. **Không được sửa file đã chạy** (Flyway báo lỗi checksum); muốn đổi cấu trúc thì **thêm** `V5__ten.sql`.
* Đặt tên: `V<số>__<mô_tả>.sql` (hai dấu gạch dưới). Thứ tự theo số.
* Kết quả: số migration vừa chạy được ghi vào log Tomcat (`Flyway: đã chạy N migration mới`). DB mới: N = 4; DB đã cập nhật: N = 0.

### 3.2. Các phiên bản migration

| Phiên bản | Nội dung | Điểm cần kiểm thử |
| :--- | :--- | :--- |
| **V1** `tao_bang` | Tạo 17 bảng, `ALTER DATABASE ... utf8mb4_unicode_ci`, toàn bộ ràng buộc | Ràng buộc CHECK/UNIQUE/FK (mục 3.3) |
| **V2** `du_lieu_mau` | Dữ liệu mẫu: 6 user, 6 địa chỉ, 4 danh mục, 6 màu, 4 size, **24 sản phẩm**, **276 biến thể**, 5 voucher, 8+ đơn demo, đánh giá, banner, blog | Dữ liệu nền cho mọi test (mục 10) |
| **V3** `chuyen_khoan` | `orders.payment_method` thêm `BANK_TRANSFER`; tạo `payment_transactions` (UNIQUE `provider, provider_tx_id` để chống xử lý trùng) | Webhook idempotent |
| **V4** `payos` | `orders.payment_attempts INT UNSIGNED DEFAULT 0` (đếm số link payOS đã tạo) | Mã payOS = `số trong mã đơn + attempts × 10^11` |

### 3.3. Mô hình dữ liệu và ràng buộc quan trọng

| Bảng | Khóa / ràng buộc đáng kiểm thử |
| :--- | :--- |
| `users` | `email` UNIQUE (≤150); `role ENUM(CUSTOMER, ADMIN)`; `is_active`; `password_hash` **NULL được** (tài khoản tạo khi khách đặt hàng không đăng nhập) |
| `addresses` | `user_id` FK **ON DELETE CASCADE**; `address_line` ≤255; `phone` ≤15 |
| `categories`, `colors`, `sizes` | Xóa mềm (`is_deleted`, `deleted_at`); `categories.slug`, `colors.code`, `sizes.name` UNIQUE **kể cả bản ghi trong thùng rác** |
| `products` | `slug` UNIQUE; `CHECK (price > 0)`; xóa mềm; `price DECIMAL(12,0)` |
| `product_variants` | UNIQUE `(product_id, color_id, size_id)`; `CHECK (stock >= 0)` |
| `carts`, `cart_items` | `carts.user_id` UNIQUE; UNIQUE `(cart_id, variant_id)`; `CHECK (quantity > 0)` |
| `vouchers` | `code` UNIQUE (≤30); CHECK: `discount_value > 0`, `PERCENT ≤ 100`, `end_date >= start_date`, `usage_limit > 0 AND per_user_limit > 0`, `0 <= used_count <= usage_limit` |
| `orders` | `order_code` UNIQUE (≤20); CHECK: `discount_amount <= subtotal`, `total_amount = subtotal - discount_amount + shipping_fee`; `shipping_address` **VARCHAR(300)**; `status ENUM(PENDING, SHIPPING, COMPLETED, CANCELLED)`; `payment_status ENUM(UNPAID, PAID)`; `cancelled_by ENUM(CUSTOMER, ADMIN)` |
| `order_details` | Lưu **snapshot** tên, màu, size, ảnh, giá; CHECK `line_total = unit_price * quantity`; `order_id` ON DELETE CASCADE |
| `reviews` | UNIQUE `(user_id, product_id)`; `CHECK (rating BETWEEN 1 AND 5)`; `comment` ≤1000; `is_hidden` |
| `payment_transactions` | `result ENUM(PAID, NO_ORDER, WRONG_METHOD, CANCELLED, ALREADY_PAID, AMOUNT_TOO_LOW)` |
| `banners`, `blogs`, `wishlists`, `product_images` | `blogs.slug` UNIQUE; `wishlists` PK `(user_id, product_id)`; `product_images` tối đa 4 ảnh hiển thị |

---

## 4. CẤU HÌNH MÔI TRƯỜNG

| File (không commit) | Khóa | Hành vi khi thiếu |
| :--- | :--- | :--- |
| `db.properties` | `db.url`, `db.username`, `db.password` | `IllegalStateException("Thiếu db.properties ...")`, ứng dụng không chạy |
| `mail.properties` | `mail.host/port/username/password/from.name`, `auth.devCode` | `auth.devCode=true`: không gửi mail, chỉ ghi log. Lỗi gửi mail chỉ ghi log (chạy luồng nền) |
| `payment.properties` | `payos.clientId`, `payos.apiKey`, `payos.checksumKey`, `payos.baseUrl` (tùy chọn) | **Thiếu bất kỳ khóa nào** → `PaymentConfig.isEnabled()=false`: ẩn lựa chọn chuyển khoản, webhook trả **503** |
| `cloudinary.properties` | `cloud_name`, `api_key`, `api_secret`, `folder` | Không có: ảnh lưu vào `assets/images/...` (mất khi deploy lại) |

---

## 5. VAI TRÒ VÀ BẢN ĐỒ URL

| Vai trò | Lưu ở | Quyền |
| :--- | :--- | :--- |
| Guest | Không | Xem hàng, tìm kiếm, giỏ hàng (session), **đặt hàng bằng email** (không cần đăng nhập), xem đơn vừa đặt trong phiên |
| Customer | `users.role=CUSTOMER` | Guest + hồ sơ, đổi mật khẩu, sổ địa chỉ, yêu thích, đơn hàng của mình, đánh giá |
| Admin | `users.role=ADMIN` | `/admin/*`. Admin **không có giỏ hàng** |

| URL | Phương thức | Servlet | Truy cập |
| :--- | :--- | :--- | :--- |
| `/`, `/home` | GET | `index.jsp` → `HomeServlet` | Tất cả |
| `/products`, `/product`, `/search/quick`, `/blog`, `/blog/detail`, `/about` | GET (`/product` thêm POST) | `ProductListServlet`, `ProductDetailServlet`, `QuickSearchServlet`, `Blog*Servlet`, `AboutServlet` | Tất cả (POST `/product` tự kiểm tra đăng nhập) |
| `/cart` | GET, POST (`add/update/remove/note`) | `CartServlet` | Tất cả |
| `/checkout` | GET, POST (`applyVoucher/removeVoucher/placeOrder`) | `CheckoutServlet` | Tất cả |
| `/order-success`, `/order-pay` | GET (`/order-pay` thêm POST) | `OrderSuccessServlet`, `OrderPayServlet` | Chủ đơn hoặc khách vãng lai vừa đặt trong phiên; còn lại **404** |
| `/api/payment/payos` | POST | `PaymentWebhookServlet` | payOS gọi vào; xác thực bằng chữ ký |
| `/login`, `/register`, `/logout` | GET/POST, GET | `LoginServlet`, `RegisterServlet`, `LogoutServlet` | Tất cả |
| `/admin/login` | GET | `LoginServlet` | Chuyển 302 về `/login` |
| `/orders`, `/order`, `/profile`, `/addresses`, `/wishlist` | GET, POST | `Order*`, `Profile`, `Address`, `Wishlist` | **Customer đã đăng nhập** (`AuthFilter`) |
| `/admin` | GET | `AdminDashboardServlet` | ADMIN |
| `/admin/products`, `/admin/products/form`, `/admin/categories`, `/admin/attributes`, `/admin/trash` | GET/POST | `Admin{Product,ProductForm,Category,Attribute,Trash}Servlet` | ADMIN |
| `/admin/orders`, `/admin/order`, `/admin/vouchers`, `/admin/reviews` | GET/POST | `Admin{Order,OrderDetail,Voucher,Review}Servlet` | ADMIN |
| `/admin/users`, `/admin/banners`, `/admin/blogs`, `/admin/blogs/form` | GET/POST | `Admin{User,Banner,Blog,BlogForm}Servlet` | ADMIN |

Trang lỗi: `403.jsp`, `404.jsp`, `500.jsp` (khai báo trong `web.xml`).

---

## 6. CHỨC NĂNG CHI TIẾT

### 6.1. FR-AUTH: Đăng nhập, đăng ký, đăng xuất
* **Một form `/login` dùng chung** cho khách và Admin (email + mật khẩu, `id`: `login-email`, `login-password`). Mật khẩu băm **PBKDF2-HMAC-SHA256**, 600.000 vòng, salt 16 byte ngẫu nhiên, định dạng `pbkdf2_sha256$<vòng>$<salt>$<băm>`.
* Sai email **hoặc** sai mật khẩu đều báo **cùng một câu** "Email hoặc mật khẩu không đúng." (không lộ email có tồn tại). Email không tồn tại vẫn băm thử với `DUMMY_HASH` để thời gian phản hồi giống nhau.
* Tài khoản `is_active=0`: nhập đúng mật khẩu mới báo "Tài khoản đã bị khóa...".
* Đăng nhập thành công: **đổi session ID** (chống session fixation), gộp giỏ session vào giỏ DB (chỉ Customer), xóa `guestOrderCode`, cập nhật `cartCount`. Admin: phiên 30 phút, gửi mail báo (hoặc ghi log nếu `auth.devCode`), chuyển về `/admin` (hoặc `/admin/...` đang xem dở). Customer về `redirect` hoặc `/home`.
* **`safeRedirect`** chỉ nhận đường dẫn nội bộ: loại `//host`, `/\`, URL tuyệt đối, có `\r` `\n`, và các đường dẫn bắt đầu `/login`, `/logout`, `/register`, `/admin/login` (chống open redirect, header injection).
* Đã đăng nhập mà mở `/login` hoặc `/register` → 302 về `/home` (Admin về `/admin`).
* **Đăng ký** (`/register`, `id`: `reg-name`, `reg-email`, `reg-password`, `reg-confirm`): tạo CUSTOMER và đăng nhập luôn. Email đã có tài khoản **chưa có mật khẩu** (tạo từ đơn khách vãng lai) thì **đặt mật khẩu cho tài khoản đó** để xem lại đơn cũ (xem rủi ro R-01).
* **Đăng xuất**: `GET /logout` hủy session rồi về `/home`.

### 6.2. FR-ACC: Tài khoản khách
* **Hồ sơ** `/profile`: sửa họ tên, số điện thoại (email, vai trò không sửa được); cập nhật `User` trong session. **Đổi mật khẩu**: cần mật khẩu hiện tại đúng.
* **Sổ địa chỉ** `/addresses`: thêm/sửa/xóa/đặt mặc định; địa chỉ đầu tiên (hoặc duy nhất) tự là mặc định; không bỏ mặc định bằng cách bỏ tick; xóa địa chỉ mặc định thì địa chỉ còn lại đầu tiên thành mặc định; chỉ thao tác trên địa chỉ của chính mình.
* **Yêu thích** `/wishlist`: `add | remove | toggle`; nút tim dùng fetch (`X-Requested-With: XMLHttpRequest`) nhận JSON `{"inWishlist": true|false}`. Dùng `INSERT IGNORE` nên thêm trùng không lỗi.

### 6.3. FR-SHOP: Cửa hàng
* **Danh sách** `/products`: chỉ sản phẩm chưa xóa mềm; 12/trang. Tham số tùy chọn, **giá trị sai được bỏ qua chứ không báo lỗi**: `q` (cắt ở 100 ký tự), `category` (slug), `minPrice`, `maxPrice` (số âm hoặc không phải số → bỏ; `min > max` → tự đổi chỗ), `color` (mã màu), `size` (tên, chuyển HOA), `sort` ∈ {`newest`, `price_asc`, `price_desc`, `bestseller`, `rating`}, `page` (<1 → 1; vượt trang cuối → lấy trang cuối).
* **Tìm nhanh** `/search/quick?q=`: `q` < 2 ký tự (sau trim) trả fragment rỗng, không truy vấn DB; tối đa 4 gợi ý tên, 8 sản phẩm; làm nổi bật phần khớp bằng `<mark>` (chuẩn hóa bỏ dấu 1-1), nội dung xuất qua `c:out` (chống XSS).
* **Chi tiết** `/product?slug=`: thiếu/không có/đã xóa mềm → **404**. Hiện ảnh (tối đa 4), biến thể màu × size, tồn kho, đánh giá chưa ẩn, sản phẩm liên quan (8). `canReview` chỉ khi có đơn `COMPLETED` chứa sản phẩm.
* **Trang chủ** `/home`: banner đang bật, mới nhất, nổi bật, bán chạy (tổng `quantity` đơn không hủy), danh mục, 3 đánh giá mới, 3 bài blog. Mục rỗng thì ẩn.
* **Blog**: khách chỉ thấy bài `is_published=1`; bài ẩn/không có → 404 ở `/blog/detail`.

### 6.4. FR-CART: Giỏ hàng
* **Khách đã đăng nhập**: giỏ lưu DB (`carts`, `cart_items`). **Khách vãng lai**: giỏ trong session (`Map<variantId, qty>`; `itemId` chính là `variantId`); biến thể không còn tồn tại tự bị gỡ.
* `add`: thiếu/≤0 `variantId` → "Vui lòng chọn màu và size."; số lượng 1..10; biến thể phải còn bán (sản phẩm chưa xóa mềm); `đã có trong giỏ + thêm ≤ tồn kho`.
* `update`: `quantity ≤ 0` xóa dòng; `> tồn kho` bị từ chối; dòng không thuộc giỏ → "Sản phẩm không còn trong giỏ hàng.".
* `note`: ghi chú lưu session, **cắt ở 500 ký tự**.
* **Gộp giỏ khi đăng nhập** (`mergeGuestCart`): cộng dồn, mỗi dòng không vượt tồn kho (`min(qty, stock − đã có)`), bỏ biến thể không bán được.
* `action` lạ → **400**.

### 6.5. FR-CHECKOUT: Thanh toán và đặt hàng
* `GET /checkout`: giỏ rỗng → flash "Giỏ hàng đang trống..." và 302 `/cart`.
* `POST action=applyVoucher | removeVoucher | placeOrder`; khác → **400**. Áp/gỡ mã **không redirect** (render lại trang, giữ dữ liệu đã nhập).
* **Khách vãng lai**: bắt buộc `email` (`f-email`); `findOrCreateCustomer` tìm hoặc tạo CUSTOMER (không mật khẩu), **không tự đăng nhập**; từ chối email Admin hoặc tài khoản bị khóa ("Email này không dùng để đặt hàng được..."); mã đơn lưu `guestOrderCode`.
* **Địa chỉ giao**: chọn từ sổ (`addressId`) hoặc nhập mới (`addressId=new`): `receiverName` (`f-receiverName`), `addressLine` (`f-addressLine`), `ward`, `district`, `city` (`f-city`), `receiverPhone` (`f-receiverPhone`). Tùy chọn `saveAddress=1` lưu vào sổ sau khi đặt (lỗi lưu thì bỏ qua).
* **Hình thức thanh toán**: `COD` (mặc định) hoặc `BANK_TRANSFER`. Gửi `BANK_TRANSFER` khi chưa cấu hình payOS → lỗi trường `paymentMethod` (kể cả khi tự sửa tham số).
* Sau khi đặt: xóa giỏ, `cartCount=0`, xóa `cartNote`; COD → `/order-success?code=`; chuyển khoản → `/order-pay?code=`.
* **Dùng vị trí hiện tại** (JS phía trình duyệt, gọi OpenStreetMap Nominatim; tối đa 1 lần/1,1 giây; hết hạn 12 giây; chỉ HTTPS hoặc `localhost`). Server **chưa** đọc/ lưu tọa độ.

### 6.6. FR-VOUCHER: Mã giảm giá
**Thứ tự kiểm tra (`VoucherService.validate`), gặp lỗi đầu tiên thì dừng; mã chuẩn hóa trim + HOA:**

| Bước | Điều kiện lỗi | Thông báo |
| :---: | :--- | :--- |
| 1 | Rỗng hoặc không tồn tại | "Mã giảm giá không tồn tại." |
| 2 | `is_active = 0` | "Mã giảm giá {mã} đang bị vô hiệu hóa." |
| 3a / 3b | `now < start_date` / `now > end_date` | "... chưa đến thời gian áp dụng." / "... đã hết hạn." |
| 4 | `used_count >= usage_limit` | "... đã hết lượt sử dụng." |
| 5 | Số đơn **chưa hủy** của user có mã này `>= per_user_limit` | "Bạn đã dùng mã {mã} đủ số lần cho phép." |
| 6 | `subtotal < min_order_amount` | "Đơn hàng cần tối thiểu {n}đ để dùng mã {mã}." |
| 7 | Có `category_id` mà giỏ không có sản phẩm thuộc danh mục đó | "Mã {mã} chỉ áp dụng cho đơn có sản phẩm thuộc danh mục "{tên}"." |

* **Giảm giá** (`computeDiscount`): `PERCENT` = `subtotal × % / 100` (số nguyên, chia lấy phần nguyên), không vượt `max_discount` nếu có; `AMOUNT` = giá trị cố định; kết quả luôn nằm trong `[0, subtotal]`.
* **Phí ship** (`shippingFee`): có voucher hợp lệ → **0**; không → **30.000**. `Tổng = subtotal − discount + shipping`.
* Mỗi đơn tối đa 1 voucher. Đặt thành công `used_count + 1`; hủy đơn `used_count − 1` (không âm).
* Khách vãng lai kiểm tra voucher với `userId = 0` ở bước xem trước, nhưng **kiểm tra lại với tài khoản của email** lúc đặt hàng.
* **Quản trị voucher**: mã `[A-Z0-9]{4,20}`, duy nhất, **không sửa mã** sau khi tạo; chỉ xóa khi chưa có lượt dùng và chưa nằm trong đơn nào, ngược lại chỉ tạm dừng. Nhãn trạng thái ưu tiên: Tạm dừng → Chưa bắt đầu → Hết hạn → Hết lượt → Đang hoạt động.

### 6.7. FR-ORDER: Đơn hàng

**Máy trạng thái đơn:**

```text
PENDING ──(Admin)──► SHIPPING ──(Admin hoặc Khách "Đã nhận hàng")──► COMPLETED   [paymentStatus := PAID, completedAt := now]
   │                    │
   │ Khách hoặc Admin   │ chỉ Admin
   ▼                    ▼
CANCELLED  (cộng trả tồn kho, trả 1 lượt voucher, ghi lý do + cancelled_by)
```

| Hành động | Ai | Điều kiện | Lỗi |
| :--- | :--- | :--- | :--- |
| `placeOrder` | Khách/Guest | Giỏ không rỗng; mỗi dòng `qty ≤ stock`; voucher hợp lệ; trừ tồn `stock >= qty` trong transaction | Giỏ trống; "... chỉ còn N sản phẩm. Hãy chỉnh lại giỏ hàng."; "... vừa hết hàng ..."; lỗi trường `voucherCode` |
| `cancelByCustomer` | Chủ đơn | Trạng thái `PENDING`; **chưa PAID** (đơn chuyển khoản đã trả → liên hệ Saltair); lý do **5..255** ký tự | "Chỉ hủy được đơn đang ở trạng thái Chờ xác nhận."; "Đơn đã thanh toán chuyển khoản..."; "Lý do hủy cần từ 5 đến 255 ký tự." |
| `confirmReceived` | Chủ đơn | `SHIPPING` | "Chỉ xác nhận đã nhận hàng khi đơn đang giao." |
| `updateStatus` | Admin | Chỉ đúng thứ tự `PENDING→SHIPPING→COMPLETED` | "Chỉ chuyển được theo thứ tự ..."; "Đơn đã hủy/hoàn thành, không đổi trạng thái được nữa." |
| `cancelByAdmin` | Admin | `PENDING` hoặc `SHIPPING`; lý do 5..255 | "Chỉ hủy được đơn đang Chờ xác nhận hoặc Đang giao." |

* Khóa đơn bằng `SELECT ... FOR UPDATE` nên hủy hai lần không cộng tồn kho hai lần. Không tìm thấy hoặc không phải chủ đơn → "Không tìm thấy đơn hàng.".
* **Mã đơn** `SAyyyyMMddNNN` (`SA` + 8 chữ số ngày + 3 chữ số thứ tự trong ngày, tăng dần theo `MAX(order_code)`).
* **Danh sách của khách** `/orders`: lọc `status` (sai → tất cả); **tab Tất cả không hiện đơn đã hủy** và `statusCounts.ALL` không tính đơn hủy; 5 đơn/trang; khách chưa có đơn nào → `welcomeProducts`.
* **Admin** `/admin/orders`: tìm theo mã đơn hoặc tên khách, lọc `status`, `payment`; trang vượt tổng → lấy trang cuối.

### 6.8. FR-PAY: Thanh toán chuyển khoản payOS
* **Tạo link** (`OrderPayServlet GET /order-pay?code=`): chỉ đơn `BANK_TRANSFER` chưa trả, chưa hủy; mỗi lần tạo `payment_attempts + 1`; **mã payOS = số trong mã đơn + attempts × 100.000.000.000** (vì payOS không cho dùng lại mã của link đã hủy/hết hạn). Link sống **15 phút**; dùng lại link trong session nếu còn > 60 giây và đúng số tiền. Trang hiện QR SVG, thông tin tài khoản nhận, đếm ngược, tự hỏi `?status=1` mỗi 4 giây.
* **Hủy thanh toán** (`POST /order-pay`): trước hết `syncPayment` (nếu tiền vừa về thì không hủy), sau đó hủy link ở payOS; đơn **giữ nguyên** (khách có thể thanh toán lại) → `/order-success?code=&cancel=true`.
* **Webhook** `POST /api/payment/payos` (không qua `AuthFilter`):

| Điều kiện | HTTP | Ghi chú |
| :--- | :---: | :--- |
| Chưa cấu hình đủ 3 khóa | **503** | Không nhận bất kỳ thông báo nào |
| Body > 64 KB | **413** | |
| Không phải JSON object, thiếu `data` (object) hoặc `signature` (chuỗi), JSON hỏng | **400** | |
| Chữ ký **sai** | **401** | HMAC-SHA256 trên `data` (khóa xếp A→Z, nối `k=v&k=v`), so sánh thời gian cố định |
| Không thành công (`success=false` hoặc `data.code≠"00"`) hoặc thiếu `orderCode/amount/paymentLinkId` | **200** `{"success":true}` | Bỏ qua nhưng vẫn báo đã nhận (để payOS không gửi lại) |
| Lỗi database | **500** | payOS sẽ gửi lại |
| Hợp lệ | **200** `{"success":true}` | Gọi `recordTransfer` |

* **Bảng quyết định `PaymentService.decide(order, amount)`** (kiểm tra theo thứ tự):

| # | Điều kiện | Kết quả | Hành động |
| :-: | :--- | :--- | :--- |
| 1 | Không có đơn | `NO_ORDER` | Chỉ ghi giao dịch |
| 2 | `payment_method ≠ BANK_TRANSFER` | `WRONG_METHOD` | Ghi giao dịch |
| 3 | Đơn `CANCELLED` | `CANCELLED` | Ghi giao dịch, cần hoàn tiền thủ công |
| 4 | Đơn đã `PAID` | `ALREADY_PAID` | Ghi giao dịch, cần hoàn tiền thủ công |
| 5 | `amount ≥ total_amount` | **`PAID`** | `markPaid` (đơn `SHIPPING` vẫn được) |
| 6 | `amount < total_amount` | `AMOUNT_TOO_LOW` | Không đánh dấu đã trả |

* **Idempotent**: cùng `(provider, paymentLinkId)` gửi lại chỉ xử lý một lần, các lần sau trả kết quả đã lưu; hai yêu cầu đồng thời → bắt `SQLIntegrityConstraintViolationException` rồi đọc lại.
* `syncPayment` hỏi payOS tối đa **3 link gần nhất**; nếu `PAID` thì ghi nhận như webhook. Lỗi mạng/payOS bị nuốt (lần sau thử lại).
* `PayOSException`: "payOS từ chối yêu cầu: ...", "Không kết nối được tới payOS...", "Không tạo được link thanh toán...", "payOS không trả mã QR...", "Kết nối tới payOS bị gián đoạn." (timeout kết nối 8 giây, mỗi request 10 giây).

### 6.9. FR-REVIEW: Đánh giá
* Chỉ được đánh giá khi có đơn **`COMPLETED`** của chính mình chứa sản phẩm. Mỗi (user, sản phẩm) **một** đánh giá: gửi lại là **sửa**; hai lần gửi đồng thời → vướng UNIQUE thì chuyển sang sửa. Khách xóa được đánh giá của mình.
* Admin ẩn/hiện: đánh giá ẩn không hiện ở trang sản phẩm và **không tính điểm** (trung bình làm tròn 1 chữ số).

### 6.10. FR-ADMIN: Quản trị
| Mục | Chức năng |
| :--- | :--- |
| **Dashboard** `/admin` | `dateFrom`, `dateTo` (`yyyy-MM-dd`; mặc định 7 ngày gần nhất; sai định dạng → mặc định; đảo nếu from > to; **tối đa 92 ngày** tính lùi từ ngày kết thúc). Tổng đơn, **doanh thu chỉ tính đơn COMPLETED**, doanh thu theo ngày, top 5 bán chạy (đơn không hủy), đơn theo trạng thái, 5 đơn mới, 5 sản phẩm tồn thấp nhất |
| **Sản phẩm** | Thêm/sửa/xóa mềm, upload ảnh (≤2MB), biến thể màu × size trong **một transaction**. Sửa: biến thể bỏ chọn thì xóa nếu chưa nằm trong đơn/giỏ, ngược lại đặt tồn = 0 |
| **Danh mục / Màu / Size** | CRUD xóa mềm; **chặn xóa** khi còn sản phẩm đang bán / đang gắn biến thể |
| **Thùng rác** `/admin/trash` | Khôi phục theo `type` ∈ {`products`, `categories`, `colors`, `sizes`}; không có xóa vĩnh viễn. Khôi phục sản phẩm mà danh mục đang trong thùng rác thì bị chặn |
| **Đơn hàng** | Xem, đổi trạng thái, hủy (mục 6.7) |
| **Voucher / Đánh giá / Người dùng / Banner / Blog** | Mục 6.6, 6.9; Người dùng: tìm theo email/tên, khóa/mở khóa, đổi vai trò, **không tự khóa / tự đổi quyền chính mình**; Banner: bật/tắt, thứ tự, xóa hẳn; Blog: ẩn/hiện, xóa |

---

## 7. DANH MỤC VALIDATION

Ký hiệu tầng: **C** = client (HTML5/JS), **S** = Service/Servlet (nguồn sự thật), **D** = ràng buộc DB.

### 7.1. Xác thực & hồ sơ

| Form / Trường | Quy tắc | Thông báo | Tầng |
| :--- | :--- | :--- | :---: |
| Email (đăng nhập, đăng ký, checkout khách) | Trim, chuyển thường; rỗng; ≤150; khớp `^[^@\s]+@[^@\s]+\.[^@\s]+$` | "Vui lòng nhập email." / "Email không đúng định dạng." | C, S, D |
| Mật khẩu (đăng nhập) | Bắt buộc; sai → câu chung | "Vui lòng nhập mật khẩu." / "Email hoặc mật khẩu không đúng." | S |
| Mật khẩu mới (đăng ký, đổi) | **8..64** ký tự; `confirm` phải khớp (chỉ kiểm tra khi độ dài hợp lệ) | "Mật khẩu tối thiểu 8 ký tự." / "tối đa 64" / "Mật khẩu nhập lại không khớp." | C, S |
| Họ tên (đăng ký) | Không rỗng, ≤100 (**không có tối thiểu 2**) | "Vui lòng nhập họ tên." / "Họ tên tối đa 100 ký tự." | S |
| Họ tên (hồ sơ) | **2..100** | "Họ và tên từ 2 đến 100 ký tự." | S |
| Số điện thoại (hồ sơ, địa chỉ, checkout) | `0[0-9]{9}` (đúng 10 số, bắt đầu 0) | "Số điện thoại gồm 10 chữ số, bắt đầu bằng 0." | C, S |
| Đổi mật khẩu | Mật khẩu hiện tại phải đúng | "Mật khẩu hiện tại không đúng." (trường `currentPassword`) | S |

### 7.2. Sổ địa chỉ (`AccountService.saveAddress`)

| Trường | Quy tắc | Thông báo |
| :--- | :--- | :--- |
| `receiverName` | 2..100 | "Tên người nhận từ 2 đến 100 ký tự." |
| `addressLine` | **5..255** | "Địa chỉ (số nhà, đường) từ 5 đến 255 ký tự." |
| `ward`, `district` | tùy chọn, ≤100 | "Phường/xã tối đa 100 ký tự." / "Quận/huyện tối đa 100 ký tự." |
| `city` | bắt buộc, ≤100 | "Vui lòng nhập tỉnh/thành phố (tối đa 100 ký tự)." |
| `id>0` | phải thuộc user | "Địa chỉ không tồn tại hoặc không thuộc tài khoản của bạn." |

### 7.3. Checkout (`CheckoutServlet.shippingAddress`) — **khác Sổ địa chỉ**

| Trường | Quy tắc | Thông báo |
| :--- | :--- | :--- |
| `receiverName` | 2..100 | "Tên người nhận từ 2 đến 100 ký tự." |
| `receiverPhone` | `0[0-9]{9}` | "Số điện thoại gồm 10 chữ số, bắt đầu bằng 0." |
| `addressLine` | **5..200** (sổ địa chỉ cho tới 255) | "Địa chỉ (số nhà, đường) từ 5 đến 200 ký tự." |
| `city` | bắt buộc, ≤100 | "Vui lòng nhập tỉnh/thành phố." |
| `ward`, `district` | **Server không kiểm tra độ dài** (chỉ `maxlength=100` ở client) | — |
| `note` | ≤500 | "Ghi chú tối đa 500 ký tự." |
| `addressId` (đã đăng nhập) | Địa chỉ phải thuộc user | "Vui lòng chọn địa chỉ giao hàng." |
| `paymentMethod` | `BANK_TRANSFER` cần payOS bật | "Hiện chưa hỗ trợ chuyển khoản. Vui lòng chọn thanh toán khi nhận hàng." |
| **Toàn chuỗi `shipping_address`** | DB `VARCHAR(300)` = `addressLine, ward, district, city` | **Không có kiểm tra tổng độ dài** (xem R-02) |

### 7.4. Giỏ hàng và đánh giá

| Trường | Quy tắc | Thông báo |
| :--- | :--- | :--- |
| `variantId` | > 0 | "Vui lòng chọn màu và size." |
| `quantity` (thêm) | 1..10; `đã có + thêm ≤ tồn` | "Số lượng mỗi lần thêm từ 1 đến 10." / "Phân loại này đã hết hàng." / "Chỉ còn N sản phẩm trong kho (giỏ của bạn đã có M)." |
| `quantity` (cập nhật) | ≤0 xóa; ≤ tồn | ""{tên}" chỉ còn N sản phẩm trong kho." |
| `rating` | 1..5 | "Hãy chọn từ 1 đến 5 sao." |
| `comment` | 10..1000 sau trim | "Nhận xét cần từ 10 đến 1000 ký tự." |
| Quyền đánh giá | Có đơn COMPLETED chứa sản phẩm | "Bạn chỉ đánh giá được sản phẩm nằm trong đơn đã hoàn thành của mình." |
| Lý do hủy đơn | 5..255 sau trim | "Lý do hủy cần từ 5 đến 255 ký tự." (trường `cancelReason`) |

### 7.5. Quản trị

| Đối tượng | Trường | Quy tắc | Thông báo (rút gọn) |
| :--- | :--- | :--- | :--- |
| **Sản phẩm** | `name` | 2..150 | "Tên sản phẩm cần từ 2 đến 150 ký tự." |
| | `slug` | trống → tự sinh; `[a-z0-9]+(-[a-z0-9]+)*`, ≤170, duy nhất (kể cả thùng rác) | "Slug chỉ gồm chữ thường không dấu, số và dấu gạch ngang." / "Slug đã được dùng..." |
| | `categoryId` | phải tồn tại, chưa xóa mềm | "Hãy chọn danh mục." |
| | `price` | **1.000..99.999.999** | "Giá bán từ 1.000đ đến 99.999.999đ." |
| | `description` | ≤2000 | "Mô tả tối đa 2000 ký tự." |
| | biến thể | ≥1; màu/size còn hiệu lực; cặp màu × size duy nhất; `stock` **0..99.999** | "Chọn ít nhất một màu." / "Màu hoặc size không hợp lệ..." / "Mỗi cặp màu x size chỉ được nhập một lần." / "Tồn kho phải là số nguyên từ 0 đến 99.999." |
| **Danh mục** | `name`, `slug` | `name` 2..100; `slug` ≤120 (cùng định dạng slug); xóa mềm bị **chặn** khi còn sản phẩm đang bán | "Tên danh mục từ 2 đến 100 ký tự." / lỗi slug / "Không thể xóa ... vì còn N sản phẩm đang bán..." |
| **Màu** | `name`, `code`, `hexCode` | `name` 1..50; `code` dạng slug ≤30, duy nhất; `hexCode` khớp `#[0-9A-Fa-f]{6}` (lưu HOA) | "Tên màu từ 1 đến 50 ký tự." / "Mã màu hex phải có dạng #RRGGBB." |
| **Size** | `name`, `sortOrder` | `name` 1..20 (lưu HOA, duy nhất kể cả thùng rác); `sortOrder` **0..99** | "Tên size từ 1 đến 20 ký tự." / "Thứ tự là số nguyên từ 0 đến 99." |
| **Voucher** | `code` | `[A-Z0-9]{4,20}`, duy nhất, không sửa sau khi tạo | "Mã gồm 4 đến 20 chữ in hoa không dấu hoặc số." / "Mã đã tồn tại." |
| | `discountValue`, `maxDiscount` | `PERCENT` 1..100; `AMOUNT` > 0; `maxDiscount` > 0 (chỉ dùng cho PERCENT); **không có cận trên** (xem R-05) | "Phần trăm giảm từ 1 đến 100." / "Số tiền giảm phải lớn hơn 0." |
| | `minOrderAmount`, `categoryId`, `description` | `minOrderAmount` ≥ 0; danh mục phải tồn tại, chưa xóa; `description` ≤255 | "Đơn tối thiểu không được âm." / "Danh mục không hợp lệ." |
| | `startDate`, `endDate`, `usageLimit`, `perUserLimit` | `endDate ≥ startDate`; `usageLimit` > 0 và ≥ số đã dùng; `perUserLimit` 1..`usageLimit` | "Ngày kết thúc không được trước ngày bắt đầu." / "Tổng lượt không được nhỏ hơn số lượt đã dùng (N)." / "Lượt mỗi khách từ 1 đến tổng lượt dùng." |
| **Banner** | `title`, `sortOrder`, ảnh | `title` 2..120; `sortOrder` **1..99**; ảnh bắt buộc khi thêm | "Tiêu đề cần từ 2 đến 120 ký tự." / "Thứ tự từ 1 đến 99." / "Chọn ảnh banner." |
| | `linkUrl` | Rỗng, hoặc khớp `(/(?!/)\|https?://)\S*` (bắt đầu bằng `/` nhưng không phải `//`, hoặc `http(s)://`), ≤255, không khoảng trắng | "Liên kết bắt đầu bằng / (trang trong web) hoặc http(s)://, không có khoảng trắng." |
| **Blog** | `title`, `slug` | `title` 5..200; `slug` ≤220, duy nhất | "Tiêu đề cần từ 5 đến 200 ký tự." / lỗi slug |
| | `summary`, `content` | `summary` ≤500; `content` **≥20** ký tự (không có tối đa ở Service; cột `MEDIUMTEXT`) | "Tóm tắt tối đa 500 ký tự." / "Nội dung cần ít nhất 20 ký tự." |
| **Người dùng** | `id`, `role` | Không tự khóa / tự đổi quyền chính mình; `role` hợp lệ | "Bạn không thể tự khóa tài khoản của chính mình." / "Bạn không thể tự đổi quyền của chính mình." / "Chọn vai trò hợp lệ." |
| **Ảnh tải lên** | file | Chỉ JPEG, PNG, WebP; ≤2MB (`@MultipartConfig`: mỗi file ≤5MB, cả request ≤6MB) | "Chỉ nhận ảnh JPG, PNG hoặc WebP." / "Ảnh tối đa 2MB." / "Ảnh quá lớn, chỉ nhận ảnh tối đa 2MB."

---

## 8. EXCEPTION VÀ XỬ LÝ LỖI

### 8.1. Các lớp ngoại lệ

| Lớp | Loại | Ném ở | Bắt ở | Hiển thị |
| :--- | :--- | :--- | :--- | :--- |
| `ValidationException` | `RuntimeException`; mang `Map<String,String> errors` (theo ô) và `getMessage()` (lỗi đầu tiên) | Service, `ImageUploadUtil`, `CheckoutServlet` | Servlet | `errors` rỗng → flash (thông báo chung); có `errors` → hiện lại form kèm lỗi từng ô. Lỗi trường `voucherCode` ở checkout được tách thành `voucherError` |
| `VoucherException` | **checked** `Exception` | `VoucherService.validate` | `CheckoutServlet.show`, `OrderService.placeOrder` (bọc lại thành `ValidationException("voucherCode", ...)`) | Thông báo từng bước (mục 6.6) |
| `DataAccessException` | `RuntimeException`, bọc `SQLException` | Mọi DAO, transaction của Service | Không bắt ở Servlet → **HTTP 500** (trang `500.jsp`); riêng webhook bắt để trả 500 cho payOS gửi lại | Trang 500 |
| `PayOSClient.PayOSException` | `RuntimeException` | `PayOSClient.call` | `OrderPayServlet`, `PaymentService` | Flash lỗi rồi về `/order-success` |
| `IllegalArgumentException` | | `PaymentService.toPayOSCode` (mã đơn sai định dạng) | Test | |
| `IllegalStateException` | | `DBContext.url()` thiếu cấu hình; `getParameter` multipart vượt kích thước | Servlet Admin bắt trường hợp tải ảnh quá lớn | Flash "Ảnh quá lớn..." |

### 8.2. Mã HTTP trả về

| Mã | Khi nào |
| :---: | :--- |
| **302** | Chưa đăng nhập vào trang cần đăng nhập → `/login?redirect=`; sau mọi POST thành công (PRG); `/admin/login` → `/login` |
| **400** | `action` không hợp lệ ở Cart, Checkout, Address, Profile, Wishlist, Order, Admin*; `productId ≤ 0` ở wishlist |
| **403** | Đã đăng nhập nhưng không phải ADMIN vào `/admin/*` |
| **404** | `slug`/`code`/`id` không tồn tại: sản phẩm, blog ẩn, đơn của người khác, `/admin/order`, `/admin/products/form?id=`, `/admin/blogs/form?id=` |
| **401 / 413 / 503** | Webhook: sai chữ ký / body quá lớn / chưa cấu hình |
| **500** | `DataAccessException` không được bắt (lỗi DB, ví dụ dữ liệu vượt độ dài cột) |

### 8.3. Hành vi "im lặng" cần nhớ khi thiết kế test
* Tham số lọc sản phẩm sai **không báo lỗi**, tự dùng giá trị mặc định.
* `AdminOpsParams` không bao giờ ném lỗi: chuỗi cắt ở 100 ký tự, số trang < 1 → 1, enum sai → `null`.
* `saveAddress` sau đặt hàng và lỗi gửi mail báo Admin đều **bị nuốt**.
* `syncPayment`/`cancelPayment` nuốt `PayOSException`.

---

## 9. CÔNG THỨC VÀ QUY TẮC TÍNH TOÁN

| Đại lượng | Công thức |
| :--- | :--- |
| `lineTotal` | `unitPrice × quantity` (khớp CHECK DB) |
| `subtotal` | `Σ lineTotal` |
| `discount` | `PERCENT`: `min(⌊subtotal × % / 100⌋, maxDiscount?)`; `AMOUNT`: `value`; rồi kẹp `[0, subtotal]` |
| `shippingFee` | `voucher hợp lệ ? 0 : 30000` |
| `total` | `subtotal − discount + shippingFee` (khớp CHECK DB) |
| Mã payOS | `phần số của mã đơn + attempts × 10^11`; ngược lại `"SA" + %011d(payOSCode mod 10^11)` |
| Điểm đánh giá TB | `round(Σ sao × 10 / n) / 10` (chỉ đánh giá chưa ẩn) |

**Ví dụ kiểm chứng:** `WELCOME10` (10%, tối đa 50.000, đơn tối thiểu 200.000) với `subtotal = 948.000` → `discount = min(94.800, 50.000) = 50.000`, `ship = 0`, `total = 898.000` (khớp đơn demo `SA20261103002`).

---

## 10. DỮ LIỆU MẪU (V2) VÀ TÀI KHOẢN KIỂM THỬ

| Email | Mật khẩu | Vai trò | Ghi chú |
| :--- | :--- | :--- | :--- |
| `admin@saltair.vn` | `Admin@123` | ADMIN | |
| `khach@saltair.vn` | `Khach@123` | CUSTOMER | 5 đơn (PENDING, SHIPPING, 2×COMPLETED, 1×CANCELLED `SA20261024005`), có giỏ, yêu thích, địa chỉ 2 |
| `lan.tran@example.com`, `hoang.le@example.com`, `duc.vo@example.com` | `Khach@123` | CUSTOMER | Có đơn demo |
| `mai.pham@example.com` | `Khach@123` | CUSTOMER | **Bị khóa** (`is_active=0`) |

| Voucher | Đặc điểm | Dùng để test |
| :--- | :--- | :--- |
| `WELCOME10` | 10%, tối đa 50.000, đơn tối thiểu 200.000, mọi danh mục, 500 lượt (đã dùng 3), 1 lần/khách | Đường hợp lệ, biên đơn tối thiểu, trần giảm |
| `GIAM50K` | −50.000, đơn tối thiểu 500.000, **chỉ Áo khoác** (category 3), 200 lượt | Bước 6, 7 |
| `HETHAN20` | Hết hạn (đến 01/10/2026) | Bước 3b |
| `HETLUOT` | 50/50 lượt | Bước 4 |
| `TAMDUNG` | `is_active=0`, category 1 | Bước 2 |

Danh mục (4): Áo thun và Polo, Áo nỉ và Hoodie, Áo khoác, Quần. Màu (6): Trắng ngà, Đen, Xanh navy, Xanh biển nhạt, Xám, Be cát. Size (4): S, M, L, XL.

---

## 11. RỦI RO VÀ ĐIỂM BẤT THƯỜNG PHÁT HIỆN KHI ĐỌC CODE

> Đây là **giả thuyết lỗi** cần xác nhận bằng test (không phải kết luận). Chúng là cơ sở để dồn test case theo nguyên lý *Defect Clustering* (xem [TEST_PLAN.md](TEST_PLAN.md) mục 4).

| ID | Phát hiện | Vị trí | Hậu quả dự kiến | Test case |
| :--- | :--- | :--- | :--- | :--- |
| R-01 | Đăng ký với email đã có tài khoản **chưa mật khẩu** sẽ đặt mật khẩu cho tài khoản đó mà **không xác thực email** | `AuthService.register` | Ai biết email của người khác (từng mua hàng không đăng nhập) có thể chiếm tài khoản, xem đơn cũ | TC_AUTH_13 |
| R-02 | `shipping_address` là `VARCHAR(300)`, nhưng server chỉ giới hạn `addressLine ≤ 200` (checkout) hoặc `≤ 255` (sổ địa chỉ); `ward`, `district` **không giới hạn ở checkout**; không kiểm tra tổng độ dài | `CheckoutServlet`, `OrderDAO.insert` | Chuỗi dài hơn 300 → `DataAccessException` → **HTTP 500** khi đặt hàng (và địa chỉ 255 ký tự trong sổ chọn ở checkout cũng có thể vượt) | TC_CHECKOUT_09, TC_CHECKOUT_10 |
| R-03 | Giới hạn `addressLine` **không nhất quán**: 200 (checkout) vs 255 (sổ địa chỉ, DB) | `CheckoutServlet`, `AccountService` | Địa chỉ 201..255 ký tự lưu được trong sổ nhưng không nhập được ở checkout | TC_ADDR_06 |
| R-04 | Mã đơn dùng `%03d`: quá **999 đơn/ngày** thành 4 chữ số; `toPayOSCode` yêu cầu đúng `SA\d{11}` | `OrderDAO.nextOrderCode`, `PaymentService` | Đơn thứ 1000 trong ngày không tạo được link payOS (`IllegalArgumentException`) | Ghi nhận, không test tự động |
| R-05 | Voucher `AMOUNT`/`minOrderAmount`/`maxDiscount` **không có cận trên** ở Service, trong khi cột `DECIMAL(12,0)` | `VoucherService.save` | Nhập số rất lớn → lỗi DB → HTTP 500 | TC_VOUCHER_16 |
| R-06 | `GET /logout` đổi trạng thái; form POST **không có CSRF token** | Toàn bộ | Có thể bị ép đăng xuất/thao tác chéo site (ngoài phạm vi môn học, ghi nhận) | Ngoài phạm vi |
| R-07 | Thời hạn session Admin 30 phút chỉ đặt **lúc đăng nhập**; session khách 60 phút | `LoginServlet` | Cần xác nhận phiên Admin tự hết hạn | TC_AUTH_17 |
| R-08 | Webhook: thông báo không thành công vẫn trả **200** | `PaymentWebhookServlet` | Đúng thiết kế, nhưng phải test để khỏi nhầm là lỗi | TC_PAY_10 |
| R-09 | `confirmReceived` đặt `PAID` cho mọi đơn `SHIPPING`, kể cả `BANK_TRANSFER` chưa thấy tiền về | `OrderService` | Đơn chuyển khoản có thể bị đánh dấu đã trả khi khách bấm "Đã nhận hàng" | TC_ORDER_09 |
| R-10 | Tồn kho: hai người cùng mua món cuối | `placeOrder` | Đã chống bằng `UPDATE ... stock >= ?`; cần chứng minh bằng test | TC_ORDER_05 |

---

## 12. NGOÀI PHẠM VI

Đăng nhập Google/mạng xã hội, quên mật khẩu bằng email, nhập kho riêng, đổi trả/hoàn tiền tự động, xuất Excel, kiểm thử hiệu năng và bảo mật chuyên sâu, gửi mail thật trong môi trường chấm, gọi payOS thật (dùng stub/ký giả lập).

## 13. QUY ƯỚC LÀM VIỆC TRÊN GITHUB

* `main`: bản ổn định giảng viên chấm, chỉ nhận PR từ `develop`. `develop`: nhánh phát triển; tách nhánh `feature/<số-issue>-<tên>`, `fix/...`, `test/...`, `docs/...` rồi mở PR vào `develop`.
* GitHub Actions (`mvn clean test`) phải **xanh** mới merge. Commit: `<loại>(<phạm vi>): <việc đã làm> (#<số-issue>)`, loại `feat | fix | test | refactor | docs | chore`.
* Nhãn issue: `logic`, `giao-dien`, `test`, `chore`, theo mảng (`auth`, `cart`...), `sof309-y1` → `sof309-y6`.
