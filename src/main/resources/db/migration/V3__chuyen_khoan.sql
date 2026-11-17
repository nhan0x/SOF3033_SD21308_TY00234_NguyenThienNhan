-- V3: thanh toán chuyển khoản ngân hàng.
-- 1. Đơn hàng có thêm phương thức BANK_TRANSFER.
-- 2. Bảng payment_transactions lưu MỌI giao dịch tiền vào mà ngân hàng báo về (qua webhook SePay), kể cả giao dịch
--    không khớp đơn nào. Cột (provider, provider_tx_id) là duy nhất nên cùng một giao dịch gửi lại nhiều lần
--    chỉ được xử lý một lần.

ALTER TABLE orders
  MODIFY payment_method ENUM('COD','BANK_TRANSFER') NOT NULL DEFAULT 'COD';

CREATE TABLE payment_transactions (
  id             INT UNSIGNED NOT NULL AUTO_INCREMENT,
  provider       VARCHAR(20)  NOT NULL,
  provider_tx_id VARCHAR(64)  NOT NULL,
  order_id       INT UNSIGNED NULL,
  order_code     VARCHAR(20)  NULL,
  amount         DECIMAL(14,0) NOT NULL,
  content        VARCHAR(500) NULL,
  result         ENUM('PAID','NO_ORDER','WRONG_METHOD','CANCELLED','ALREADY_PAID','AMOUNT_TOO_LOW') NOT NULL,
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE (provider, provider_tx_id),
  INDEX (order_id),
  FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB;
