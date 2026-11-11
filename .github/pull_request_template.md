## Tóm tắt

<!-- Làm gì, vì sao. Một đến ba câu. -->

## Issue

Closes #

## Đã làm

-

## Cách kiểm tra

<!-- Tài khoản, các bước bấm, kết quả mong đợi. Dữ liệu mẫu: docs/SPECIFICATIONS.md -->

1.

## Ảnh chụp màn hình

<!-- Nếu có thay đổi giao diện. Bỏ trống nếu không. -->

## Thay đổi dữ liệu / cấu hình

<!-- Sửa cấu trúc dữ liệu, file cấu hình, thư viện? Người khác cần làm gì sau khi pull? -->

- [ ] Không có
- [ ] Có (mô tả bên dưới, kèm lệnh chạy migration):

## Checklist

- [ ] Nhánh tách từ `develop`, PR vào `develop` (chỉ PR `develop` → `main` khi đưa bản ổn định lên), đã `git pull origin develop` trước khi mở PR
- [ ] Đổi quy tắc nghiệp vụ thì đã cập nhật `docs/SPECIFICATIONS.md`
- [ ] Ca kiểm thử liên quan trong `docs/TEST_CASES.md` đã điền Pass/Fail, luồng chính trong `docs/TEST_PLAN.md` vẫn chạy
- [ ] Dùng AI thì đã ghi vào `docs/ai-logs/`
- [ ] Commit theo dạng `<loại>(<phạm vi>): ... (#số)`
- [ ] Không commit file cấu hình bí mật, file build, mật khẩu, khóa API

## Checklist riêng dự án

- [ ] Đúng luồng `Servlet -> Service -> Repository`; Servlet không viết SQL, JSP không gọi Repository/Service
- [ ] SQL dùng `PreparedStatement`, không nối chuỗi từ dữ liệu người dùng
- [ ] Đổi `schema.sql` thì có `database/migration_<số>_<tên>.sql`
- [ ] Giao diện đúng ở 1440px và 375px, Console không có lỗi đỏ
- [ ] GitHub Actions báo xanh
