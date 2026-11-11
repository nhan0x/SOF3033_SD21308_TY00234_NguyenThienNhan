/**
 * CHỨC NĂNG: Đơn hàng phía khách (danh sách, chi tiết, hủy, xác nhận đã nhận), đánh giá sau khi nhận hàng; Service đơn hàng và đánh giá cũng được Admin dùng lại.
 *
 * Servlet: OrderServlet, OrderDetailServlet, OrderSuccessServlet.
 * Khung đã có: OrderService, OrderRepository, OrderDetailRepository, ReviewService, ReviewRepository.
 * JSP: WEB-INF/views/order/.
 * Luồng trạng thái: PENDING -> SHIPPING -> COMPLETED, hoặc CANCELLED (trả tồn kho và lượt voucher).
 */
package com.fashionweb.controller.order;
