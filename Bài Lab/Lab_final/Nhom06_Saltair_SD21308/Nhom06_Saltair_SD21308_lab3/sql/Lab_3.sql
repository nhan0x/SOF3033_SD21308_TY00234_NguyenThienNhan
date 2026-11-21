-- Lab 3 - Bai 1: CSDL PolyCoffee (SQL Server 2008+)
IF DB_ID('PolyCoffee') IS NULL CREATE DATABASE PolyCoffee;
GO
USE PolyCoffee;
GO
IF OBJECT_ID('bill_details') IS NOT NULL DROP TABLE bill_details;
IF OBJECT_ID('bills') IS NOT NULL DROP TABLE bills;
IF OBJECT_ID('drinks') IS NOT NULL DROP TABLE drinks;
IF OBJECT_ID('categories') IS NOT NULL DROP TABLE categories;
IF OBJECT_ID('users') IS NOT NULL DROP TABLE users;
GO
CREATE TABLE categories (
    id      INT IDENTITY(1,1) PRIMARY KEY,
    name    NVARCHAR(100) NOT NULL UNIQUE
);
CREATE TABLE users (
    id        INT IDENTITY(1,1) PRIMARY KEY,
    fullname  NVARCHAR(100) NOT NULL,
    email     VARCHAR(100)  NOT NULL UNIQUE,
    password  VARCHAR(100)  NOT NULL,          -- SHA-256 (hex)
    manager   BIT NOT NULL DEFAULT 0,          -- 1: quan ly, 0: nhan vien
    enabled   BIT NOT NULL DEFAULT 1           -- trang thai hoat dong
);
CREATE TABLE drinks (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    name        NVARCHAR(100) NOT NULL,
    image       VARCHAR(255)  NULL,
    unit_price  DECIMAL(18,0) NOT NULL CHECK (unit_price >= 0),
    discount    FLOAT NOT NULL DEFAULT 0 CHECK (discount BETWEEN 0 AND 1),
    available   BIT NOT NULL DEFAULT 1,        -- trang thai ban
    category_id INT NOT NULL,
    CONSTRAINT FK_drinks_categories FOREIGN KEY (category_id) REFERENCES categories(id)
);
CREATE TABLE bills (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id     INT NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT GETDATE(),
    checkout_at DATETIME NULL,
    status      INT NOT NULL DEFAULT 0,        -- 0: dang xu ly, 1: da thanh toan, 2: da huy
    CONSTRAINT FK_bills_users FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE TABLE bill_details (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    bill_id     BIGINT NOT NULL,
    drink_id    INT NOT NULL,
    unit_price  DECIMAL(18,0) NOT NULL,
    discount    FLOAT NOT NULL DEFAULT 0,
    quantity    INT NOT NULL CHECK (quantity > 0),
    CONSTRAINT FK_bd_bills  FOREIGN KEY (bill_id)  REFERENCES bills(id) ON DELETE CASCADE,
    CONSTRAINT FK_bd_drinks FOREIGN KEY (drink_id) REFERENCES drinks(id),
    CONSTRAINT UQ_bd UNIQUE (bill_id, drink_id)
);
GO
-- Du lieu mau. Mat khau mac dinh: 123456 (SHA-256)
INSERT INTO users(fullname,email,password,manager,enabled) VALUES
(N'Nguyễn Quản Lý','manager@polycoffee.com','8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92',1,1),
(N'Trần Văn Nhân','staff1@polycoffee.com','8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92',0,1),
(N'Lê Thị Viên','staff2@polycoffee.com','8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92',0,1);
INSERT INTO categories(name) VALUES (N'Cà phê'),(N'Trà'),(N'Sinh tố'),(N'Nước ép');
INSERT INTO drinks(name,image,unit_price,discount,available,category_id) VALUES
(N'Cà phê đen','cafe-den.jpg',20000,0,1,1),
(N'Cà phê sữa','cafe-sua.jpg',25000,0,1,1),
(N'Bạc xỉu','bac-xiu.jpg',29000,0.1,1,1),
(N'Trà đào cam sả','tra-dao.jpg',35000,0,1,2),
(N'Trà vải','tra-vai.jpg',32000,0,1,2),
(N'Sinh tố bơ','sinh-to-bo.jpg',40000,0,1,3),
(N'Sinh tố xoài','sinh-to-xoai.jpg',38000,0,1,3),
(N'Nước ép cam','ep-cam.jpg',35000,0,1,4),
(N'Nước ép dưa hấu','ep-dua-hau.jpg',30000,0,0,4);
INSERT INTO bills(user_id,created_at,checkout_at,status) VALUES
(2,DATEADD(day,-2,GETDATE()),DATEADD(day,-2,GETDATE()),1),
(2,DATEADD(day,-1,GETDATE()),DATEADD(day,-1,GETDATE()),1),
(3,GETDATE(),NULL,0);
INSERT INTO bill_details(bill_id,drink_id,unit_price,discount,quantity) VALUES
(1,1,20000,0,2),(1,4,35000,0,1),
(2,2,25000,0,3),(2,6,40000,0,1),(2,3,29000,0.1,2),
(3,5,32000,0,2);
GO
