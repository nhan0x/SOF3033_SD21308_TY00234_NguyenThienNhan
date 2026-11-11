<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  ĐẶT HÀNG THÀNH CÔNG  (GET /order-success?code=... -> OrderSuccessServlet)
  Thuộc tính request:
    guest : boolean - true khi khách chưa đăng nhập (đổi lời dẫn và nút "Xem đơn hàng" thành đăng nhập)
    order : Order - đơn vừa đặt (orderCode, createdAt, receiverName, receiverPhone, shippingAddress, paymentMethod, totalAmount)
  Không có form.
--%>
<t:layout title="Đặt hàng thành công" chrome="checkout">
  <jsp:attribute name="head"><link rel="stylesheet" href="${ctx}/assets/css/pages/shopping.css?v=5"></jsp:attribute>
  <jsp:body>
    <section class="sh-success" aria-labelledby="sh-success-title">
      <div class="sh-success__icon"><t:icon name="check-circle"/></div>
      <h1 class="h-1 sh-enter" style="--d:2" id="sh-success-title">Đặt hàng thành công</h1>
      <p class="sh-success__lead sh-enter" style="--d:3">Cảm ơn bạn đã chọn Saltair. Chúng tôi đã nhận đơn và sẽ xác nhận sớm. <c:choose><c:when test="${guest}">Để theo dõi đơn, hãy đăng nhập bằng email bạn vừa dùng để đặt hàng và mã 6 số gửi về email đó.</c:when><c:otherwise>Bạn có thể theo dõi trạng thái trong mục Đơn hàng của tôi.</c:otherwise></c:choose></p>

      <div class="sh-success__card sh-enter" style="--d:4">
        <div class="sh-kv">
          <span>Mã đơn hàng</span>
          <span class="sh-code"><c:out value="${order.orderCode}"/>
            <button type="button" class="icon-btn" data-copy="${fn:escapeXml(order.orderCode)}" aria-label="Sao chép mã đơn hàng"><t:icon name="copy"/></button>
          </span>
        </div>
        <div class="sh-kv"><span>Ngày đặt</span><span><fmt:formatDate value="${order.createdAt}" pattern="dd/MM/yyyy HH:mm"/></span></div>
        <div class="sh-kv"><span>Người nhận</span><span><c:out value="${order.receiverName}"/> · <c:out value="${order.receiverPhone}"/></span></div>
        <div class="sh-kv"><span>Giao đến</span><span><c:out value="${order.shippingAddress}"/></span></div>
        <div class="sh-kv"><span>Thanh toán</span><span><c:out value="${order.paymentMethod.label}"/></span></div>
        <div class="sh-kv sh-kv--total"><span>Tổng thanh toán</span><span><t:price value="${order.totalAmount}"/></span></div>
      </div>

      <div class="sh-success__actions sh-enter" style="--d:5">
        <a class="btn btn--primary" href="${ctx}/order?code=${fn:escapeXml(order.orderCode)}">${guest ? 'Đăng nhập để xem đơn' : 'Xem đơn hàng'}</a>
        <a class="btn btn--outline" href="${ctx}/products">Tiếp tục mua sắm</a>
      </div>
    </section>
  </jsp:body>
</t:layout>
