## Tóm tắt

<!-- Làm gì, vì sao. Một đến ba câu. -->

## Issue

Closes #

## Đã làm

-

## Cách kiểm tra

<!-- Tài khoản, các bước bấm, kết quả mong đợi. -->

1.

## Ảnh chụp màn hình

<!-- Nếu có thay đổi giao diện. Bỏ trống nếu không. -->

## Thay đổi dữ liệu / cấu hình

<!-- Sửa cấu trúc dữ liệu, file cấu hình, thư viện? Người khác cần làm gì sau khi pull? -->

- [ ] Không có
- [ ] Có (mô tả bên dưới, kèm lệnh chạy migration):

## Checklist chung

- [ ] Nhánh tách từ nhánh phát triển chính, PR vào nhánh đó, đã cập nhật nhánh chính trước khi mở PR
- [ ] Đã tự chạy thử các bước ở mục "Cách kiểm tra"
- [ ] Commit theo dạng `<loại>(<phạm vi>): ... (#số)`
- [ ] Không commit file cấu hình bí mật, file build, mật khẩu, khóa API

## Checklist riêng dự án

- [ ] Đúng luồng `Servlet -> Service -> DAO`; Servlet không viết SQL, JSP không gọi DAO/Service
- [ ] SQL dùng `PreparedStatement`, không nối chuỗi từ dữ liệu người dùng
- [ ] Đổi bảng thì thêm file migration mới `src/main/resources/db/migration/V<số>__<tên>.sql`, không sửa file đã chạy
- [ ] Giao diện đúng ở 1440px và 375px, Console không có lỗi đỏ
