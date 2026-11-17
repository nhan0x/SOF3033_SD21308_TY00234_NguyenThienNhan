<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  ĐÃ HỦY THANH TOÁN  (GET /order-success?code=...&cancel=true -> OrderSuccessServlet, chỉ với đơn chuyển khoản chưa trả)
  Thuộc tính request:
    guest : boolean - khách chưa đăng nhập (ẩn nút "Xem đơn hàng")
    order : Order - đơn chuyển khoản chưa thanh toán (orderCode, createdAt, totalAmount)
  Không có form. "Thanh toán lại" là liên kết tới /order-pay (tạo mã QR mới).
--%>
<t:layout title="Đã hủy thanh toán" chrome="checkout">
  <jsp:attribute name="head"><link rel="stylesheet" href="${ctx}/assets/css/pages/shopping.css?v=8"></jsp:attribute>
  <jsp:attribute name="scripts"><script src="${ctx}/assets/js/pages/shopping.js?v=5" defer></script></jsp:attribute>
  <jsp:body>
    <section class="sh-success" aria-labelledby="sh-cancelled-title">
      <div class="sh-success__icon sh-success__icon--off"><t:icon name="clock"/></div>
      <h1 class="h-1 sh-enter" style="--d:2" id="sh-cancelled-title">Bạn đã hủy thanh toán</h1>
      <p class="sh-success__lead sh-enter" style="--d:3">Saltair chưa ghi nhận khoản thanh toán nào. Đơn hàng vẫn được giữ lại, bạn có thể thanh toán lại bất cứ lúc nào bằng mã QR mới.</p>

      <div class="sh-success__card sh-enter" style="--d:4">
        <div class="sh-kv">
          <span>Mã đơn hàng</span>
          <span class="sh-code"><c:out value="${order.orderCode}"/>
            <button type="button" class="icon-btn" data-copy="${fn:escapeXml(order.orderCode)}" aria-label="Sao chép mã đơn hàng"><t:icon name="copy"/></button>
          </span>
        </div>
        <div class="sh-kv"><span>Ngày đặt</span><span><fmt:formatDate value="${order.createdAt}" pattern="dd/MM/yyyy HH:mm"/></span></div>
        <div class="sh-kv"><span>Trạng thái thanh toán</span><span>Chưa thanh toán</span></div>
        <div class="sh-kv sh-kv--total"><span>Cần thanh toán</span><span><t:price value="${order.totalAmount}"/></span></div>
      </div>

      <div class="sh-success__actions sh-enter" style="--d:5">
        <a class="btn btn--primary" href="${ctx}/order-pay?code=${fn:escapeXml(order.orderCode)}">Thanh toán lại</a>
        <c:if test="${not guest}"><a class="btn btn--outline" href="${ctx}/order?code=${fn:escapeXml(order.orderCode)}">Xem đơn hàng</a></c:if>
        <a class="btn btn--ghost" href="${ctx}/products">Tiếp tục mua sắm</a>
      </div>
    </section>
  </jsp:body>
</t:layout>
