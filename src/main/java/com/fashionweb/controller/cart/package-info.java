/**
 * CHỨC NĂNG: Giỏ hàng, thanh toán (COD), áp mã giảm giá, tính phí ship.
 *
 * Servlet: CartServlet, CheckoutServlet.
 * Khung đã có: CartService, CartRepository, VoucherService, VoucherRepository, VoucherException.
 * JSP: WEB-INF/views/cart/.
 * Quy tắc voucher và phí ship: xem project.md. Đặt hàng thành công gọi OrderService (gói order) trong một transaction.
 */
package com.fashionweb.controller.cart;
