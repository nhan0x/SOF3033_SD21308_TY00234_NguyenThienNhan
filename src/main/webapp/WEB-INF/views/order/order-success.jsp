<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  ĐẶT HÀNG THÀNH CÔNG  (GET /order-success?code=... -> OrderSuccessServlet)
  Thuộc tính request:
    guest : boolean - true khi khách chưa đăng nhập (đổi lời dẫn và nút "Xem đơn hàng" thành đăng nhập)
    order : Order - đơn vừa đặt (orderCode, createdAt, receiverName, receiverPhone, shippingAddress, paymentMethod, paymentStatus, totalAmount)
    payable : boolean - true khi đơn chuyển khoản còn chờ thanh toán (hiện nút "Thanh toán" tới /order-pay)
  Không có form.
--%>
<t:layout title="Đặt hàng thành công" chrome="checkout">
  <jsp:attribute name="head"><link rel="stylesheet" href="${ctx}/assets/css/pages/shopping.css?v=7"></jsp:attribute>
  <jsp:attribute name="scripts"><script src="${ctx}/assets/js/pages/shopping.js?v=4" defer></script></jsp:attribute>
  <jsp:body>
    <section class="sh-success" aria-labelledby="sh-success-title">
      <div class="sh-success__icon"><t:icon name="check-circle"/></div>
      <h1 class="h-1 sh-enter" style="--d:2" id="sh-success-title">Đặt hàng thành công</h1>
      <p class="sh-success__lead sh-enter" style="--d:3">Cảm ơn bạn đã chọn Saltair. Chúng tôi đã nhận đơn và sẽ xác nhận sớm. <c:choose><c:when test="${guest}">Để theo dõi đơn, hãy đăng ký tài khoản bằng email bạn vừa dùng để đặt hàng.</c:when><c:otherwise>Bạn có thể theo dõi trạng thái trong mục Đơn hàng của tôi.</c:otherwise></c:choose></p>

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

      <%-- Đơn chuyển khoản qua payOS: chưa trả thì có nút "Thanh toán" (sang trang payOS có mã QR) và trang tự hỏi server
           mỗi 4 giây (shopping.js: data-pay-watch); khi payOS xác nhận đã nhận tiền thì tự đổi sang "Đã nhận thanh toán". --%>
      <c:if test="${order.paymentMethod eq 'BANK_TRANSFER' and (payable or order.paymentStatus eq 'PAID')}">
        <div class="sh-pay sh-enter" style="--d:5" data-pay-watch="${ctx}/order-success?code=${fn:escapeXml(order.orderCode)}&amp;status=1"
             data-paid="${order.paymentStatus eq 'PAID'}">
          <div class="sh-pay__waiting">
            <h2 class="h-2">Chờ thanh toán chuyển khoản</h2>
            <p class="t-muted">Bấm nút bên dưới để quét mã QR bằng app ngân hàng. Trang này tự cập nhật ngay khi Saltair nhận được tiền.</p>
            <%@ include file="/WEB-INF/views/common/flash.jspf" %>
            <a class="btn btn--primary" href="${ctx}/order-pay?code=${fn:escapeXml(order.orderCode)}">Thanh toán <t:price value="${order.totalAmount}"/></a>
          </div>
          <div class="sh-pay__done" role="status">
            <t:icon name="check-circle"/>
            <strong>Đã nhận thanh toán</strong>
            <span class="t-muted">Cảm ơn bạn. Saltair sẽ chuẩn bị và giao đơn sớm nhất.</span>
          </div>
        </div>
      </c:if>

      <div class="sh-success__actions sh-enter" style="--d:5">
        <a class="btn btn--primary" href="${ctx}/order?code=${fn:escapeXml(order.orderCode)}">${guest ? 'Đăng nhập để xem đơn' : 'Xem đơn hàng'}</a>
        <a class="btn btn--outline" href="${ctx}/products">Tiếp tục mua sắm</a>
      </div>
    </section>
  </jsp:body>
</t:layout>
