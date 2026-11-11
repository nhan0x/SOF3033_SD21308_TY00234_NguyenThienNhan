-- Migration 03: khung ảnh trang chi tiết, mỗi sản phẩm nhiều ảnh (2 ảnh sản phẩm, 2 ảnh người mẫu mặc).
-- Dành cho máy đã có DB cũ. Máy mới chỉ cần chạy schema.sql rồi data.sql.
-- Chạy: mysql -u root -p saltair < database/migration_03_anh_san_pham.sql
-- Chạy lại nhiều lần vẫn an toàn.
--
-- Thêm ảnh cho một sản phẩm (ảnh trong assets/images/products/ hoặc URL Cloudinary), sort_order 1..4:
--   INSERT INTO product_images (product_id, image, sort_order)
--   SELECT id, CONCAT('assets/images/products/', slug, '-1.jpg'), 1 FROM products WHERE slug = 'ao-thun-co-tron';
-- Sản phẩm chưa có dòng nào ở bảng này thì trang chi tiết hiện ảnh chính (products.image) như cũ.

USE saltair;

CREATE TABLE IF NOT EXISTS product_images (
  id          INT UNSIGNED NOT NULL AUTO_INCREMENT,
  product_id  INT UNSIGNED NOT NULL,
  image       VARCHAR(255) NOT NULL,
  sort_order  INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  INDEX (product_id, sort_order),
  FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
) ENGINE=InnoDB;
