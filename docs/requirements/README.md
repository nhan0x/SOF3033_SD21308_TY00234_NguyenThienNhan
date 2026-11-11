# BẢNG TỔNG HỢP YÊU CẦU (REQUIREMENTS)
> Mỗi yêu cầu một thư mục `REQ_<số 3 chữ số>_<TEN_KHONG_DAU>/`, gồm 4 file theo từng bước của [HUONG_DAN_QUY_TRINH_PHAT_TRIEN.md](../HUONG_DAN_QUY_TRINH_PHAT_TRIEN.md):

| File | Bước | Nội dung |
| :--- | :---: | :--- |
| `01_YEU_CAU.md` | 1, 7 | Nguyên văn yêu cầu, user story, tiêu chí chấp nhận, kết quả nghiệm thu |
| `02_PHAN_TICH.md` | 2, 3 | Câu hỏi & quyết định, quy tắc BR-x, luồng, ảnh hưởng, ngoài phạm vi, chia issue |
| `03_TEST_CASES.md` | 5 | Ca kiểm thử trỏ tới BR-x, unit test, kết quả |
| `04_AI_LOG.md` | Mọi bước | Nhật ký dùng AI |

**Nhận yêu cầu mới:**
```bash
cp -R docs/requirements/_TEMPLATE docs/requirements/REQ_<số>_<TEN>
```
Ví dụ đã điền đầy đủ: [REQ_001_DANG_NHAP_BANG_MA](REQ_001_DANG_NHAP_BANG_MA/).

---

## DANH SÁCH YÊU CẦU
> Trạng thái: **Mới** → **Đã phân tích** → **Đang làm** → **Chờ nghiệm thu** → **Đã nghiệm thu** (hoặc **Hủy**).

| Mã | Yêu cầu | Người yêu cầu | Ngày nhận | Issue | Trạng thái |
| :---: | :--- | :--- | :--- | :--- | :---: |
| [REQ_001](REQ_001_DANG_NHAP_BANG_MA/) | Đăng nhập bằng email + mã 6 số, khung tài khoản dạng popover | Trưởng nhóm | 10/11/2026 | saltair-fashion-web#74, #75 | Đã nghiệm thu |

> Các chức năng làm trước khi có quy trình này (issue #1 → #27, #72, #73, #80, #82 → #91) không có thư mục REQ; lịch sử xem trong issue và PR của repo cũ `nhan0x/saltair-fashion-web`. Từ REQ_002 trở đi, issue và PR nằm ở repo này.
