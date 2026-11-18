# Nhật ký AI: Thiết kế Test Plan và Test Case (Y2, Tuần 1)

> Công cụ: Claude Code (model Claude Sonnet 5.5) trong VS Code · Ngày: 18/11/2026 · Nhánh: `feat/requirement`
> Các mục có dấu **[ĐIỀN]** là phần chỉ bạn mới biết (ý kiến, chỉnh sửa của bạn). Hãy điền hoặc xóa trước khi nộp; không nộp phần để trống.

## 1. Ngữ cảnh và mục tiêu

- **Cần làm:** đọc toàn bộ dự án Saltair (tech stack, cách tổ chức code, migration DB, chức năng, exception, validation), ghi vào `docs/SPECIFICATIONS.md`; sau đó viết `docs/TEST_PLAN.md` trước, rồi dựa vào Test Plan sinh `docs/TEST_CASES.md`.
- **Ràng buộc kiểm thử:** áp dụng *Defect Clustering* và *Exhaustive testing is impossible* để chọn và phân bổ test case.
- **Áp dụng cho:** toàn bộ ứng dụng (xác thực, giỏ hàng, voucher, đặt hàng, thanh toán payOS, quản trị, cửa hàng).
- **Bước trong lộ trình:** Tuần 1, Y2 (đặc tả + Test Plan + Test Case).

## 2. Câu lệnh đã gửi cho AI (Prompt)

Prompt 1 (nguyên văn):

> "Bạn hãy đọc full dự án này về các khía cạnh như: Tech stack, cách tổ chức code base, sử dụng migration database, các chức năng, các exception và các validation của project này và ghi vào file specification.md để thực hiện sau đó viết test plan trước rồi dựa vào test plan tạo ra test case. Test case thì sử dụng principle defect clustering và exhautive testing is impossible để hoàn thiện được tuần 1."

Prompt 2 (nguyên văn):

> "soạn khung ai-logs/01_test_design_ai.md giúp mình"

**[ĐIỀN]** Nếu bạn gửi thêm prompt khác ngoài hai prompt trên (ví dụ dán `SPECIFICATIONS.md` cho AI khác để đối chiếu), ghi thêm vào đây.

## 3. Kết quả do AI sinh ra (Raw Output)

AI không dán nội dung vào log; nó ghi trực tiếp ba file vào repo. Bản gốc AI sinh nằm ở commit đầu tiên chứa ba file này (**[ĐIỀN]** hash commit sau khi bạn commit), gồm:

| File | Nội dung chính AI sinh ra |
| :--- | :--- |
| `docs/SPECIFICATIONS.md` | Tech stack theo `pom.xml`; cây thư mục và quy tắc kiến trúc; Flyway V1 → V4; bản đồ URL/vai trò; 10 nhóm chức năng; danh mục validation theo từng form; bảng exception và mã HTTP; 10 giả thuyết lỗi R-01 đến R-10 |
| `docs/TEST_PLAN.md` | Phạm vi; bảng đo rủi ro theo cụm; tính không gian đầu vào; kỹ thuật chọn mẫu; môi trường; 7 luồng smoke; tiêu chí vào/ra; truy vết đặc tả → test case |
| `docs/TEST_CASES.md` | 114 test case, 12 cụm module, gắn ưu tiên P1/P2/P3, kỹ thuật (EP, BVA, DT, ST, EG, SEC) và tầng tự động (JUnit, Selenium, API, Tay) |

Số liệu AI tạo ra và cách kiểm chứng lại:

| Số liệu | Giá trị | Kiểm chứng |
| :--- | :--- | :--- |
| Test JUnit hiện có | 36 test, 0 lỗi | AI chạy `mvn -o test`, đọc `target/surefire-reports` |
| Dòng code theo cụm, số transaction | Cụm đặt hàng + thanh toán 2.051 dòng, 5 transaction | `wc -l`, `grep setAutoCommit` (lệnh ghi ở TEST_PLAN mục 11) |
| Tỷ lệ test theo cụm | A 54% · B 24% · C 11% · D 11% | Script Python đếm lại từ bảng TEST_CASES |

## 4. Chỉnh sửa và Lý do sửa

### 4.1. Những điểm AI làm sai hoặc thiếu, được phát hiện và sửa trong cùng phiên

(Đây là sự thật từ phiên làm việc; bạn nên đọc lại để chắc chắn mình hiểu từng điểm vì giảng viên sẽ hỏi "AI sai ở đâu".)

| # | AI làm sai / thiếu | Cách phát hiện | Đã sửa thành |
| :-: | :--- | :--- | :--- |
| 1 | Bản đặc tả và test case cũ trong repo mô tả đăng nhập bằng mã 6 số, "không có thanh toán online" | Đọc `AuthService`, `LoginServlet`, `PaymentService`, migration V3/V4: code dùng email + mật khẩu và payOS | Viết lại toàn bộ theo code thực tế; bỏ test case cũ của cơ chế đã gỡ |
| 2 | Bản nháp đánh **71/114 ca là P1**: "smoke" gần như là tất cả nên không còn tác dụng ưu tiên | Script đếm cho thấy 62% là P1 | Chọn lại bằng danh sách tường minh: còn **38 ca P1** (33%) |
| 3 | Ghi "21 ca đã được phủ bởi 36 test JUnit có sẵn" | Đối chiếu tên test: thực tế chỉ khớp 5 ca (TC_PAY_01, 02, 03, 11, 12) | Sửa thành 5 ca |
| 4 | Ghi "5 truy vấn `FOR UPDATE`" | `grep` ra 5 dòng nhưng 2 dòng là chú thích | Sửa thành **3** truy vấn thật; ghi chú số liệu theo thư mục chỉ mang tính tương đối |
| 5 | Ghi "hai lần chạy `mvn test`" | Thực tế chạy một lần | Sửa thành một lần |
| 6 | Bảng Markdown vỡ cột (ký tự `\|` trong mã `a\|b`, hàng thiếu cột ở mục 7.5) | Script kiểm số cột mọi bảng | Sửa, chạy lại script: 0 hàng lệch |
| 7 | Dùng đường dẫn Tomcat/MySQL lấy từ trí nhớ cũ, bước 2 của TC_VOUCHER_09 viết rối | Rà lại tài liệu | Bỏ đường dẫn cụ thể; viết lại các bước |

### 4.2. Chỉnh sửa của riêng bạn

**[ĐIỀN]** Liệt kê điều bạn tự đọc và sửa, ví dụ:
- Bạn đã chạy thử ca nào và kết quả có khớp "Kết quả mong đợi" không?
- Điểm bất thường R-0x nào bạn đã xác nhận bằng cách chạy thật (hoặc bác bỏ)?
- Test case nào bạn thấy thừa hoặc thiếu so với nghiệp vụ bạn hiểu?
- Dữ liệu mẫu hoặc thông báo lỗi nào AI ghi sai so với màn hình thật?

### 4.3. Kết luận sau khi chỉnh sửa

- Tài liệu hoàn thiện: `docs/SPECIFICATIONS.md`, `docs/TEST_PLAN.md`, `docs/TEST_CASES.md` (114 ca; P1 38, P2 70, P3 6).
- Bài học: **[ĐIỀN]** 1 đến 2 câu bằng lời của bạn (ví dụ: vì sao không nên tin số liệu AI nếu chưa kiểm tra lại bằng lệnh).
- Việc tiếp theo: Tuần 2 viết JUnit cho 47 ca tầng `JUnit`; ghi tiếp nhật ký vào `02_junit_script_ai.md`.
