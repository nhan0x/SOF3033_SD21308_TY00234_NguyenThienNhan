-- Migration 02 (#74): bỏ đăng nhập bằng mật khẩu, chuyển sang email + mã 6 số.
-- Dành cho máy đã có DB cũ. Máy mới chỉ cần chạy schema.sql rồi data.sql.
-- Chạy: mysql -u root -p saltair < database/migration_02_bo_mat_khau.sql
-- Chạy lại nhiều lần vẫn an toàn.

USE saltair;

CREATE TABLE IF NOT EXISTS login_codes (
  id          INT UNSIGNED NOT NULL AUTO_INCREMENT,
  email       VARCHAR(150) NOT NULL,
  code_hash   CHAR(64)     NOT NULL,
  expires_at  DATETIME     NOT NULL,
  attempts    TINYINT      NOT NULL DEFAULT 0,
  used        TINYINT(1)   NOT NULL DEFAULT 0,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_login_codes_email (email, created_at)
) ENGINE=InnoDB;

DROP TABLE IF EXISTS password_resets;

-- MySQL 8 không có "DROP COLUMN IF EXISTS", nên kiểm tra trong information_schema trước.
SET @has_pw := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'password_hash');
SET @sql := IF(@has_pw > 0, 'ALTER TABLE users DROP COLUMN password_hash', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
