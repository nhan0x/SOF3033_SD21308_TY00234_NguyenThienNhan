CREATE DATABASE IF NOT EXISTS saltair
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE saltair;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS blogs, banners, reviews, order_details, orders, vouchers,
                     wishlists, cart_items, carts, product_variants, product_images, products,
                     sizes, colors, categories, addresses, login_codes, password_resets, users;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE users (
  id            INT UNSIGNED NOT NULL AUTO_INCREMENT,
  email         VARCHAR(150) NOT NULL,
  full_name     VARCHAR(100) NOT NULL,
  phone         VARCHAR(15)  NULL,
  role          ENUM('CUSTOMER','ADMIN') NOT NULL DEFAULT 'CUSTOMER',
  is_active     TINYINT(1)   NOT NULL DEFAULT 1,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE (email)
) ENGINE=InnoDB;

CREATE TABLE login_codes (
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

CREATE TABLE addresses (
  id            INT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id       INT UNSIGNED NOT NULL,
  receiver_name VARCHAR(100) NOT NULL,
  phone         VARCHAR(15)  NOT NULL,
  address_line  VARCHAR(255) NOT NULL,
  ward          VARCHAR(100) NULL,
  district      VARCHAR(100) NULL,
  city          VARCHAR(100) NOT NULL,
  is_default    TINYINT(1)   NOT NULL DEFAULT 0,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE categories (
  id          INT UNSIGNED NOT NULL AUTO_INCREMENT,
  name        VARCHAR(100) NOT NULL,
  slug        VARCHAR(120) NOT NULL,
  image       VARCHAR(255) NULL,
  is_deleted  TINYINT(1)   NOT NULL DEFAULT 0,
  deleted_at  DATETIME     NULL,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE (slug)
) ENGINE=InnoDB;

CREATE TABLE colors (
  id          INT UNSIGNED NOT NULL AUTO_INCREMENT,
  name        VARCHAR(50)  NOT NULL,
  code        VARCHAR(30)  NOT NULL,
  hex_code    CHAR(7)      NOT NULL,
  is_deleted  TINYINT(1)   NOT NULL DEFAULT 0,
  deleted_at  DATETIME     NULL,
  PRIMARY KEY (id),
  UNIQUE (code)
) ENGINE=InnoDB;

CREATE TABLE sizes (
  id          INT UNSIGNED NOT NULL AUTO_INCREMENT,
  name        VARCHAR(20)  NOT NULL,
  sort_order  INT          NOT NULL DEFAULT 0,
  is_deleted  TINYINT(1)   NOT NULL DEFAULT 0,
  deleted_at  DATETIME     NULL,
  PRIMARY KEY (id),
  UNIQUE (name)
) ENGINE=InnoDB;

CREATE TABLE products (
  id           INT UNSIGNED  NOT NULL AUTO_INCREMENT,
  category_id  INT UNSIGNED  NOT NULL,
  name         VARCHAR(150)  NOT NULL,
  slug         VARCHAR(170)  NOT NULL,
  description  TEXT          NULL,
  price        DECIMAL(12,0) NOT NULL,
  image        VARCHAR(255)  NULL,
  image_thumb  VARCHAR(255)  NULL,
  is_featured  TINYINT(1)    NOT NULL DEFAULT 0,
  is_deleted   TINYINT(1)    NOT NULL DEFAULT 0,
  deleted_at   DATETIME      NULL,
  created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE (slug),
  INDEX (is_deleted, created_at),
  INDEX (price),
  CHECK (price > 0),
  FOREIGN KEY (category_id) REFERENCES categories (id)
) ENGINE=InnoDB;

CREATE TABLE product_images (
  id          INT UNSIGNED NOT NULL AUTO_INCREMENT,
  product_id  INT UNSIGNED NOT NULL,
  image       VARCHAR(255) NOT NULL,
  sort_order  INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  INDEX (product_id, sort_order),
  FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE product_variants (
  id          INT UNSIGNED NOT NULL AUTO_INCREMENT,
  product_id  INT UNSIGNED NOT NULL,
  color_id    INT UNSIGNED NOT NULL,
  size_id     INT UNSIGNED NOT NULL,
  stock       INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE (product_id, color_id, size_id),
  CHECK (stock >= 0),
  FOREIGN KEY (product_id) REFERENCES products (id),
  FOREIGN KEY (color_id)   REFERENCES colors (id),
  FOREIGN KEY (size_id)    REFERENCES sizes (id)
) ENGINE=InnoDB;

CREATE TABLE carts (
  id          INT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id     INT UNSIGNED NOT NULL,
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE (user_id),
  FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE cart_items (
  id          INT UNSIGNED NOT NULL AUTO_INCREMENT,
  cart_id     INT UNSIGNED NOT NULL,
  variant_id  INT UNSIGNED NOT NULL,
  quantity    INT          NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE (cart_id, variant_id),
  CHECK (quantity > 0),
  FOREIGN KEY (cart_id)    REFERENCES carts (id) ON DELETE CASCADE,
  FOREIGN KEY (variant_id) REFERENCES product_variants (id)
) ENGINE=InnoDB;

CREATE TABLE wishlists (
  user_id     INT UNSIGNED NOT NULL,
  product_id  INT UNSIGNED NOT NULL,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, product_id),
  FOREIGN KEY (user_id)    REFERENCES users (id) ON DELETE CASCADE,
  FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB;

CREATE TABLE vouchers (
  id               INT UNSIGNED  NOT NULL AUTO_INCREMENT,
  code             VARCHAR(30)   NOT NULL,
  description      VARCHAR(255)  NULL,
  discount_type    ENUM('PERCENT','AMOUNT') NOT NULL,
  discount_value   DECIMAL(12,0) NOT NULL,
  max_discount     DECIMAL(12,0) NULL,
  min_order_amount DECIMAL(12,0) NOT NULL DEFAULT 0,
  category_id      INT UNSIGNED  NULL,
  start_date       DATETIME      NOT NULL,
  end_date         DATETIME      NOT NULL,
  usage_limit      INT           NOT NULL,
  used_count       INT           NOT NULL DEFAULT 0,
  per_user_limit   INT           NOT NULL DEFAULT 1,
  is_active        TINYINT(1)    NOT NULL DEFAULT 1,
  created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE (code),
  CHECK (discount_value > 0),
  CHECK (discount_type <> 'PERCENT' OR discount_value <= 100),
  CHECK (end_date >= start_date),
  CHECK (usage_limit > 0 AND per_user_limit > 0),
  CHECK (used_count >= 0 AND used_count <= usage_limit),
  FOREIGN KEY (category_id) REFERENCES categories (id)
) ENGINE=InnoDB;

CREATE TABLE orders (
  id               INT UNSIGNED  NOT NULL AUTO_INCREMENT,
  order_code       VARCHAR(20)   NOT NULL,
  user_id          INT UNSIGNED  NOT NULL,
  receiver_name    VARCHAR(100)  NOT NULL,
  receiver_phone   VARCHAR(15)   NOT NULL,
  shipping_address VARCHAR(300)  NOT NULL,
  note             VARCHAR(500)  NULL,
  payment_method   ENUM('COD')   NOT NULL DEFAULT 'COD',
  payment_status   ENUM('UNPAID','PAID') NOT NULL DEFAULT 'UNPAID',
  status           ENUM('PENDING','SHIPPING','COMPLETED','CANCELLED') NOT NULL DEFAULT 'PENDING',
  subtotal         DECIMAL(12,0) NOT NULL,
  discount_amount  DECIMAL(12,0) NOT NULL DEFAULT 0,
  shipping_fee     DECIMAL(12,0) NOT NULL DEFAULT 0,
  total_amount     DECIMAL(12,0) NOT NULL,
  voucher_id       INT UNSIGNED  NULL,
  voucher_code     VARCHAR(30)   NULL,
  cancel_reason    VARCHAR(255)  NULL,
  cancelled_by     ENUM('CUSTOMER','ADMIN') NULL,
  created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  completed_at     DATETIME      NULL,
  PRIMARY KEY (id),
  UNIQUE (order_code),
  INDEX (user_id, created_at),
  INDEX (status, created_at),
  CHECK (subtotal >= 0 AND discount_amount >= 0 AND shipping_fee >= 0),
  CHECK (discount_amount <= subtotal),
  CHECK (total_amount = subtotal - discount_amount + shipping_fee),
  FOREIGN KEY (user_id)    REFERENCES users (id),
  FOREIGN KEY (voucher_id) REFERENCES vouchers (id)
) ENGINE=InnoDB;

CREATE TABLE order_details (
  id           INT UNSIGNED  NOT NULL AUTO_INCREMENT,
  order_id     INT UNSIGNED  NOT NULL,
  variant_id   INT UNSIGNED  NOT NULL,
  product_id   INT UNSIGNED  NOT NULL,
  product_name VARCHAR(150)  NOT NULL,
  color_name   VARCHAR(50)   NOT NULL,
  size_name    VARCHAR(20)   NOT NULL,
  image        VARCHAR(255)  NULL,
  unit_price   DECIMAL(12,0) NOT NULL,
  quantity     INT           NOT NULL,
  line_total   DECIMAL(12,0) NOT NULL,
  PRIMARY KEY (id),
  CHECK (quantity > 0),
  CHECK (line_total = unit_price * quantity),
  FOREIGN KEY (order_id)   REFERENCES orders (id) ON DELETE CASCADE,
  FOREIGN KEY (variant_id) REFERENCES product_variants (id),
  FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB;

CREATE TABLE reviews (
  id          INT UNSIGNED     NOT NULL AUTO_INCREMENT,
  user_id     INT UNSIGNED     NOT NULL,
  product_id  INT UNSIGNED     NOT NULL,
  rating      TINYINT UNSIGNED NOT NULL,
  comment     VARCHAR(1000)    NULL,
  is_hidden   TINYINT(1)       NOT NULL DEFAULT 0,
  created_at  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE (user_id, product_id),
  INDEX (product_id, is_hidden),
  CHECK (rating BETWEEN 1 AND 5),
  FOREIGN KEY (user_id)    REFERENCES users (id) ON DELETE CASCADE,
  FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB;

CREATE TABLE banners (
  id          INT UNSIGNED NOT NULL AUTO_INCREMENT,
  title       VARCHAR(150) NOT NULL,
  image       VARCHAR(255) NOT NULL,
  link_url    VARCHAR(255) NULL,
  sort_order  INT          NOT NULL DEFAULT 0,
  is_active   TINYINT(1)   NOT NULL DEFAULT 1,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE blogs (
  id            INT UNSIGNED NOT NULL AUTO_INCREMENT,
  author_id     INT UNSIGNED NOT NULL,
  title         VARCHAR(200) NOT NULL,
  slug          VARCHAR(220) NOT NULL,
  summary       VARCHAR(500) NULL,
  content       MEDIUMTEXT   NOT NULL,
  cover_image   VARCHAR(255) NULL,
  is_published  TINYINT(1)   NOT NULL DEFAULT 1,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE (slug),
  INDEX (is_published, created_at),
  FOREIGN KEY (author_id) REFERENCES users (id)
) ENGINE=InnoDB;
