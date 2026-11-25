-- PolyCoffee - Script CSDL MySQL 8 (tạo bảng + dữ liệu mẫu)
-- Chuyển đổi từ Lab_3.sql (SQL Server). Xem SPECIFICATIONS.md mục 3.
--
-- CẢNH BÁO: script xóa (DROP) và tạo lại 5 bảng trong CSDL `polycoffee`.
-- Nếu đã có dữ liệu cần giữ, hãy sao lưu trước hoặc đổi tên CSDL ở hai dòng đầu.
--
-- Chạy:  mysql -u root -p --default-character-set=utf8mb4 < database/polycoffee.sql
-- Mật khẩu mặc định của mọi tài khoản mẫu: 123456 (lưu SHA-256 hex)

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS polycoffee CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE polycoffee;

DROP TABLE IF EXISTS bill_details, bills, drinks, categories, users;

CREATE TABLE categories (
    id    INT AUTO_INCREMENT PRIMARY KEY,
    name  VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE users (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    fullname  VARCHAR(100) NOT NULL,
    email     VARCHAR(100) NOT NULL UNIQUE,
    password  VARCHAR(100) NOT NULL,              -- SHA-256 dạng hex, 64 ký tự
    manager   BOOLEAN NOT NULL DEFAULT FALSE,     -- 1: quản lý, 0: nhân viên
    enabled   BOOLEAN NOT NULL DEFAULT TRUE       -- 1: đang hoạt động, 0: bị khóa
);

CREATE TABLE drinks (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    image        VARCHAR(255) NULL,
    unit_price   DECIMAL(18,0) NOT NULL CHECK (unit_price >= 0),
    discount     DOUBLE NOT NULL DEFAULT 0 CHECK (discount BETWEEN 0 AND 1),
    available    BOOLEAN NOT NULL DEFAULT TRUE,   -- 1: đang bán, 0: ngừng bán
    category_id  INT NOT NULL,
    CONSTRAINT fk_drinks_categories FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE bills (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      INT NOT NULL,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    checkout_at  DATETIME NULL,
    status       INT NOT NULL DEFAULT 0 CHECK (status IN (0, 1, 2)),  -- 0: đang xử lý, 1: đã thanh toán, 2: đã hủy
    CONSTRAINT fk_bills_users FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE bill_details (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    bill_id     BIGINT NOT NULL,
    drink_id    INT NOT NULL,
    unit_price  DECIMAL(18,0) NOT NULL,           -- giá tại thời điểm bán (chụp lại)
    discount    DOUBLE NOT NULL DEFAULT 0,        -- giảm giá tại thời điểm bán
    quantity    INT NOT NULL CHECK (quantity > 0),
    CONSTRAINT fk_bd_bills  FOREIGN KEY (bill_id)  REFERENCES bills(id) ON DELETE CASCADE,
    CONSTRAINT fk_bd_drinks FOREIGN KEY (drink_id) REFERENCES drinks(id),
    CONSTRAINT uq_bd UNIQUE (bill_id, drink_id)
);

-- ---------------------------------------------------------------------------
-- Dữ liệu mẫu
-- ---------------------------------------------------------------------------
INSERT INTO users(fullname, email, password, manager, enabled) VALUES
('Nguyễn Quản Lý', 'manager@polycoffee.com', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 1, 1),
('Trần Văn Nhân',  'staff1@polycoffee.com',  '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 0, 1),
('Lê Thị Viên',    'staff2@polycoffee.com',  '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 0, 1);

INSERT INTO categories(name) VALUES ('Cà phê'), ('Trà'), ('Sinh tố'), ('Nước ép');

INSERT INTO drinks(name, image, unit_price, discount, available, category_id) VALUES
('Cà phê đen',      'cafe-den.jpg',    20000, 0,   1, 1),
('Cà phê sữa',      'cafe-sua.jpg',    25000, 0,   1, 1),
('Bạc xỉu',         'bac-xiu.jpg',     29000, 0.1, 1, 1),
('Trà đào cam sả',  'tra-dao.jpg',     35000, 0,   1, 2),
('Trà vải',         'tra-vai.jpg',     32000, 0,   1, 2),
('Sinh tố bơ',      'sinh-to-bo.jpg',  40000, 0,   1, 3),
('Sinh tố xoài',    'sinh-to-xoai.jpg',38000, 0,   1, 3),
('Nước ép cam',     'ep-cam.jpg',      35000, 0,   1, 4),
('Nước ép dưa hấu', 'ep-dua-hau.jpg',  30000, 0,   0, 4);

INSERT INTO bills(user_id, created_at, checkout_at, status) VALUES
(2, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), 1),
(2, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 1),
(3, NOW(), NULL, 0);

INSERT INTO bill_details(bill_id, drink_id, unit_price, discount, quantity) VALUES
(1, 1, 20000, 0,   2), (1, 4, 35000, 0, 1),
(2, 2, 25000, 0,   3), (2, 6, 40000, 0, 1), (2, 3, 29000, 0.1, 2),
(3, 5, 32000, 0,   2);
