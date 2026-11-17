<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  THANH TOÁN CHUYỂN KHOẢN  (GET /order-pay?code=... -> OrderPayServlet)
  Thuộc tính request:
    order     : Order - đơn đang thanh toán (orderCode, totalAmount)
    link      : PaymentLink - dữ liệu payOS (accountNumber, accountName, amount, description = nội dung chuyển khoản)
    qrSvg     : String - mã QR đã dựng thành SVG (do server tạo từ chuỗi VietQR, an toàn để in thẳng, không escape)
    bankName  : String - tên ngân hàng nhận tiền
    remaining : long - số giây mã còn hiệu lực, cho đồng hồ đếm ngược
    statusUrl : String - địa chỉ JSON {"paid":true|false} để trang hỏi mỗi 4 giây
  Form: POST /order-pay  field: code  (Hủy thanh toán)
  shopping.js: data-pay-page (đồng hồ, hỏi trạng thái), data-copy (sao chép)
--%>
<t:layout title="Thanh toán chuyển khoản" chrome="checkout">
  <jsp:attribute name="head"><link rel="stylesheet" href="${ctx}/assets/css/pages/shopping.css?v=8"></jsp:attribute>
  <jsp:attribute name="scripts"><script src="${ctx}/assets/js/pages/shopping.js?v=5" defer></script></jsp:attribute>
  <jsp:body>
    <section class="sh-pp" aria-labelledby="sh-pp-title"
             data-pay-page data-remaining="${remaining}" data-status="${fn:escapeXml(statusUrl)}"
             data-done="${ctx}/order-success?code=${fn:escapeXml(order.orderCode)}">
      <div class="sh-pp__head sh-enter" style="--d:1">
        <h1 class="h-1" id="sh-pp-title">Thanh toán chuyển khoản</h1>
        <p class="sh-pp__lead">Mở app ngân hàng, quét mã QR hoặc chuyển khoản đúng số tiền và nội dung bên dưới. Trang này tự cập nhật ngay khi Saltair nhận được tiền.</p>
      </div>

      <div class="sh-pp__grid sh-enter" style="--d:2">
        <div class="sh-pp__qr">
          <div class="sh-pp__code" data-pay-qr>
            ${qrSvg}
            <div class="sh-pp__expired" data-pay-expired hidden>
              <strong>Mã QR đã hết hạn</strong>
              <a class="btn btn--primary btn--sm" href="${ctx}/order-pay?code=${fn:escapeXml(order.orderCode)}">Tạo mã mới</a>
            </div>
          </div>
          <p class="sh-pp__timer" data-pay-timer-wrap><t:icon name="clock"/> Mã còn hiệu lực <strong data-pay-timer>--:--</strong></p>
          <p class="sh-pp__wait" role="status"><span class="sh-pp__dot" aria-hidden="true"></span> Đang chờ bạn thanh toán</p>
        </div>

        <div class="sh-pp__info">
          <div class="sh-pp__card">
            <div class="sh-kv"><span>Ngân hàng</span><span><c:out value="${bankName}"/></span></div>
            <div class="sh-kv"><span>Chủ tài khoản</span><span><c:out value="${link.accountName}"/></span></div>
            <div class="sh-kv">
              <span>Số tài khoản</span>
              <span class="sh-code"><c:out value="${link.accountNumber}"/>
                <button type="button" class="icon-btn" data-copy="${fn:escapeXml(link.accountNumber)}" aria-label="Sao chép số tài khoản"><t:icon name="copy"/></button>
              </span>
            </div>
            <div class="sh-kv">
              <span>Số tiền</span>
              <span class="sh-code"><fmt:formatNumber value="${link.amount}" pattern="#,##0"/> đ
                <button type="button" class="icon-btn" data-copy="${link.amount}" aria-label="Sao chép số tiền"><t:icon name="copy"/></button>
              </span>
            </div>
            <div class="sh-kv">
              <span>Nội dung</span>
              <span class="sh-code"><c:out value="${link.description}"/>
                <button type="button" class="icon-btn" data-copy="${fn:escapeXml(link.description)}" aria-label="Sao chép nội dung chuyển khoản"><t:icon name="copy"/></button>
              </span>
            </div>
          </div>

          <div class="alert alert--info" role="note">
            <t:icon name="info"/>
            <span>Nhập chính xác số tiền và nội dung để đơn được xác nhận tự động. Quét mã QR sẽ điền sẵn các thông tin này.</span>
          </div>

          <div class="sh-pp__order">
            <span class="t-muted">Đơn hàng</span>
            <strong><c:out value="${order.orderCode}"/></strong>
          </div>

          <form class="sh-pp__cancel" method="post" action="${ctx}/order-pay">
            <input type="hidden" name="code" value="${fn:escapeXml(order.orderCode)}">
            <button type="submit" class="btn btn--ghost btn--block">Hủy thanh toán</button>
          </form>
        </div>
      </div>
    </section>
  </jsp:body>
</t:layout>
