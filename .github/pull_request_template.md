## Tóm tắt

<!-- Làm gì, vì sao. Một đến ba câu. -->

## Issue & Yêu cầu

Closes #

- Yêu cầu: `docs/requirements/REQ_<số>_<TEN>/` <!-- Việc rất nhỏ không có REQ thì ghi "Không có" -->

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

## Checklist chung (theo docs/HUONG_DAN_QUY_TRINH_PHAT_TRIEN.md)

- [ ] Nhánh tách từ nhánh phát triển chính, PR vào nhánh đó, đã cập nhật nhánh chính trước khi mở PR
- [ ] `02_PHAN_TICH.md` khớp với code (quy tắc đổi trong lúc code đã cập nhật lại)
- [ ] `03_TEST_CASES.md` đã điền Pass/Fail, unit test xanh, luồng chính trong `TEST_PLAN.md` vẫn chạy
- [ ] Dùng AI thì đã ghi `04_AI_LOG.md`
- [ ] Commit theo dạng `<loại>(<phạm vi>): ... (#số)`
- [ ] Không commit file cấu hình bí mật, file build, mật khẩu, khóa API

## Checklist riêng dự án (theo docs/SPECIFICATIONS.md mục 7)

<!-- Sang dự án mới: thay các dòng dưới bằng checklist trong SPECIFICATIONS.md của dự án đó -->

- [ ] Đúng luồng `Servlet -> Service -> Repository`; Servlet không viết SQL, JSP không gọi Repository/Service
- [ ] SQL dùng `PreparedStatement`, không nối chuỗi từ dữ liệu người dùng
- [ ] Đổi `schema.sql` thì có `database/migration_<số>_<tên>.sql`
- [ ] Giao diện đúng ở 1440px và 375px, Console không có lỗi đỏ
- [ ] GitHub Actions báo xanh
