-- V4: thanh toán chuyển khoản qua payOS.
-- payOS yêu cầu mã đơn (orderCode) là SỐ và không dùng lại được sau khi link bị hủy hoặc hết hạn. Mỗi lần khách bấm
-- "Thanh toán" ta tạo một link mới với orderCode = <số trong mã đơn> + payment_attempts * 100000000000, nên cột này đếm
-- số link đã tạo (0 = chưa tạo link nào). Webhook đổi ngược về mã đơn bằng phép chia lấy dư.
ALTER TABLE orders
  ADD COLUMN payment_attempts INT UNSIGNED NOT NULL DEFAULT 0 AFTER payment_status;
