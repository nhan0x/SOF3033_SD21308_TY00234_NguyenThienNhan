# ĐẶC TẢ BỐI CẢNH DỰ ÁN (SPECIFICATIONS)
> *File RIÊNG của dự án. Dùng làm ngữ cảnh (Context) gửi cho AI ở bước Phân tích và Test, và là nơi duy nhất [`HUONG_DAN_QUY_TRINH_PHAT_TRIEN.md`](HUONG_DAN_QUY_TRINH_PHAT_TRIEN.md) tham chiếu khi cần thông tin cụ thể.*
> *Sang dự án mới: giữ nguyên các mục, viết lại nội dung.*

---

## 1. THÔNG TIN DỰ ÁN
* **Tên đề tài:** Saltair - website bán quần áo basic (web của môn JAV102, dùng làm dự án kiểm thử cho môn SOF309)
* **Repo chính (từ 12/11/2026):** `nhan0x/SOF3033_SD21308_TY00234_NguyenThienNhan` (Public, repo nộp bài SOF309)
* **Repo cũ:** `nhan0x/saltair-fashion-web` (lịch sử issue #1 → #91 và PR #28 → #92; không phát triển tiếp ở đó)
* **Trưởng nhóm:** `nhan0x` · **Thành viên:** thêm sau (Settings → Collaborators)
* **Lộ trình môn SOF309:** [`HUONG_DAN_CHI_TIET_6_TUAN.md`](HUONG_DAN_CHI_TIET_6_TUAN.md)
* **Nghiệp vụ chi tiết** (role, chức năng, trạng thái đơn, voucher, phí ship): [`project.md`](../project.md)

## 2. CÔNG NGHỆ & CÁCH CHẠY
* **Công nghệ:** Java 17, Jakarta Servlet 6.1 / JSP 4 / JSTL 3, JDBC `PreparedStatement`, MySQL 8, Tomcat 11, Maven (WAR), HTML/CSS/JS thuần.
* **URL chạy local:** `http://localhost:8080/saltair/home`
* **Lệnh build:** `mvn package` → deploy `target/saltair.war`
* **Lệnh test:** `mvn test` (JUnit 5). GitHub Actions (`.github/workflows/test.yml`) tự chạy `mvn clean test` khi push hoặc mở PR vào `main`.
* **Thư mục test:**
  * `src/test/java/com/fashionweb/`: unit test nghiệp vụ (Service, Util), chia theo tầng như code chính.
  * `src/test/java/com/poly/sof309/controllers/`: unit test Controller theo đề SOF309 (Y3).
  * `src/test/java/com/poly/sof309/ui/pages/` và `ui/tests/`: Selenium Page Object Model, TestNG (Y4).
* **Tạo database:** chạy `database/schema.sql` rồi `database/data.sql`
* **Cấu hình không commit** (copy từ file `.example`): `db.properties`, `mail.properties`, `cloudinary.properties` trong `src/main/resources/`

## 3. KIẾN TRÚC & QUY TẮC CODE
* **Luồng bắt buộc:** `Servlet -> Service -> Repository -> MySQL` (MVC + Repository + Service Layer).
  * Servlet: đọc tham số, gọi Service, forward JSP hoặc redirect. Không viết SQL.
  * Service: nghiệp vụ, kiểm tra dữ liệu, ném `ValidationException` với câu tiếng Việt.
  * Repository: chỉ SQL, luôn `PreparedStatement`.
  * JSP: chỉ hiển thị, đầu file ghi comment route, thuộc tính request, các form.
* **Package:** chia theo tầng rồi theo chức năng: `controller|service|repository/<auth|account|shop|cart|order|admin>/`.
* **Đổi database:** sửa `schema.sql`, `data.sql` (nếu dữ liệu mẫu đổi) và thêm `database/migration_<số>_<tên>.sql`.
* **Giao diện:** chỉ animate `transform`/`opacity`, hỗ trợ `prefers-reduced-motion`, điểm ngắt 990px và 750px. Chỉ mượn bố cục/hiệu ứng của trang mẫu, không dùng logo, ảnh, chữ của họ.

## 4. QUY ƯỚC GIT
* **Nhánh phát triển chính:** `main` (mặc định, là bản giảng viên chấm và GitHub Actions chạy). Mọi việc đi qua nhánh riêng + PR vào `main`, không push thẳng.
* **Tên nhánh:** `feature/<số-issue>-<ten-ngan>`, `fix/<số-issue>-<ten-ngan>`, `test/<số-issue>-<ten-ngan>` (bài kiểm thử SOF309), `docs/<ten-ngan>`, `chore/<ten-ngan>`.
* **Commit:** `<loại>(<phạm vi>): <việc đã làm> (#<số-issue>)`, viết tiếng Việt. Phạm vi: `auth`, `account`, `shop`, `cart`, `order`, `admin`, `ui`, `db`.
* **Merge:** *Create a merge commit*, chỉ merge khi GitHub Actions báo **xanh**. Repo bật tự xóa nhánh sau khi merge.
* **Repo Public:** tuyệt đối không commit `db.properties`, `mail.properties`, `cloudinary.properties` (đã có trong `.gitignore`).

## 5. NHÃN ISSUE
| Nhóm | Nhãn |
| :--- | :--- |
| Loại | `logic`, `giao-dien`, `test`, `bug`, `chore`, `documentation` |
| Mảng | `auth`, `account`, `shop`, `cart`, `order`, `admin` |
| Môn SOF309 | `sof309-y1` → `sof309-y6` (yêu cầu Y1 đến Y6 trong lộ trình 6 tuần) |
| Trạng thái | `can-quyet-dinh` (chờ trưởng nhóm chốt) |

## 6. DỮ LIỆU MẪU ĐỂ TEST
* **Bật `auth.devCode=true`** trong `mail.properties`: mã đăng nhập hiện trên trang (email mẫu là email giả).
* **Tài khoản:** `admin@saltair.vn` (ADMIN, đăng nhập ở `/admin/login`), `khach@saltair.vn` (có giỏ, đơn đủ trạng thái), `lan.tran@example.com`, `hoang.le@example.com`, `duc.vo@example.com`, `mai.pham@example.com` (đang bị khóa).
* **Voucher:** `WELCOME10`, `GIAM50K` (dùng được); `HETHAN20` (hết hạn), `HETLUOT` (hết lượt), `TAMDUNG` (vô hiệu hóa).

## 7. CHECKLIST RIÊNG CỦA DỰ ÁN (DÙNG KHI REVIEW PR)
- [ ] Đúng luồng `Servlet -> Service -> Repository`; Servlet không viết SQL, JSP không gọi Repository/Service.
- [ ] SQL dùng `PreparedStatement`, không nối chuỗi từ dữ liệu người dùng.
- [ ] Không commit `*.properties` thật, `target/`, mật khẩu, khóa API.
- [ ] Đổi `schema.sql` thì có `database/migration_<số>_<tên>.sql`.
- [ ] Giao diện đúng ở 1440px và 375px, Console không có lỗi đỏ.
- [ ] GitHub Actions trên PR báo xanh.
