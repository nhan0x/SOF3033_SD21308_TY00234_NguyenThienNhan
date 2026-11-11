<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  CHI TIẾT ĐƠN HÀNG  (GET /order?code=... -> OrderDetailServlet)  - khu vực tài khoản
  Thuộc tính request:
    order : Order - đơn của khách, có items (List<OrderDetail>), subtotal, discountAmount, voucherCode, shippingFee,
                    totalAmount, status, paymentMethod, paymentStatus, cancelReason, cancelledBy, createdAt, completedAt
  Form (đều gửi tới /order, xử lý ở OrderDetailServlet):
    POST /order action=cancel          fields: orderCode, cancelReason, returnTo=detail   (hộp thoại dùng chung)
    POST /order action=confirmReceived fields: orderCode, returnTo=detail
--%>
<t:account-layout title="Đơn hàng ${order.orderCode}" active="orders">
  <jsp:attribute name="head"><link rel="stylesheet" href="${ctx}/assets/css/pages/shopping.css?v=5"></jsp:attribute>
  <jsp:attribute name="scripts"><script src="${ctx}/assets/js/pages/shopping.js" defer></script></jsp:attribute>
  <jsp:body>
    <c:set var="dialogReturn" value="detail"/>
    <c:set var="st" value="${order.status}"/>

      <div class="acct-card acct-order">
        <a class="sh-back sh-enter" href="${ctx}/orders"><t:icon name="arrow-left"/> Quay lại danh sách đơn hàng</a>

        <div class="sh-detail-head sh-enter" style="--d:1">
          <div>
            <div class="sh-detail-title">
              <h1 class="h-1">Đơn <c:out value="${order.orderCode}"/></h1>
              <span class="badge badge--${order.status.cssClass}"><c:out value="${order.status.label}"/></span>
            </div>
            <p class="sh-detail-meta t-small t-muted">Đặt ngày <fmt:formatDate value="${order.createdAt}" pattern="dd/MM/yyyy HH:mm"/> · ${order.itemCount} sản phẩm</p>
          </div>

          <div class="sh-detail-actions">
            <c:if test="${st == 'PENDING'}">
              <button type="button" class="btn btn--outline" data-dialog-open="cancel-dialog" data-dialog-title="Hủy đơn ${fn:escapeXml(order.orderCode)}" data-fill='{"orderCode":"${fn:escapeXml(order.orderCode)}"}'>Hủy đơn</button>
            </c:if>
            <c:if test="${st == 'SHIPPING'}">
              <form class="sh-inline-form" method="post" action="${ctx}/order" data-confirm="Bạn đã nhận được đơn hàng ${fn:escapeXml(order.orderCode)}?">
                <input type="hidden" name="action" value="confirmReceived">
                <input type="hidden" name="orderCode" value="${fn:escapeXml(order.orderCode)}">
                <input type="hidden" name="returnTo" value="detail">
                <button type="submit" class="btn btn--primary">Đã nhận hàng</button>
              </form>
            </c:if>
            <a class="btn btn--ghost" href="${ctx}/products">Tiếp tục mua sắm</a>
          </div>
        </div>

        <%@ include file="/WEB-INF/views/common/flash.jspf" %>

        <%-- Tiến trình đơn hàng --%>
        <section class="sh-enter" style="--d:2" aria-label="Tiến trình đơn hàng">
          <c:choose>
            <c:when test="${st == 'CANCELLED'}">
              <ol class="timeline timeline--2">
                <li class="timeline__step is-done">
                  <b>Đã đặt hàng</b>
                  <small><fmt:formatDate value="${order.createdAt}" pattern="dd/MM/yyyy HH:mm"/></small>
                </li>
                <li class="timeline__step is-cancelled" aria-current="step">
                  <b>Đã hủy</b>
                  <small>
                    <c:if test="${not empty order.cancelledBy}">Bởi <c:out value="${order.cancelledBy.label}"/> · </c:if><fmt:formatDate value="${order.updatedAt}" pattern="dd/MM/yyyy HH:mm"/>
                  </small>
                </li>
              </ol>
            </c:when>
            <c:otherwise>
              <ol class="timeline timeline--3">
                <li class="timeline__step is-done${st == 'PENDING' ? ' is-current' : ''}"<c:if test="${st == 'PENDING'}"> aria-current="step"</c:if>>
                  <b>Chờ xác nhận</b>
                  <small><fmt:formatDate value="${order.createdAt}" pattern="dd/MM/yyyy HH:mm"/></small>
                </li>
                <li class="timeline__step${st == 'SHIPPING' ? ' is-done is-current' : ''}${st == 'COMPLETED' ? ' is-done' : ''}"<c:if test="${st == 'SHIPPING'}"> aria-current="step"</c:if>>
                  <b>Đang giao</b>
                  <small><c:choose><c:when test="${st == 'SHIPPING' or st == 'COMPLETED'}">Đã bàn giao vận chuyển</c:when><c:otherwise>Chưa giao</c:otherwise></c:choose></small>
                </li>
                <li class="timeline__step${st == 'COMPLETED' ? ' is-done is-current' : ''}"<c:if test="${st == 'COMPLETED'}"> aria-current="step"</c:if>>
                  <b>Hoàn thành</b>
                  <small><c:choose><c:when test="${st == 'COMPLETED' and not empty order.completedAt}"><fmt:formatDate value="${order.completedAt}" pattern="dd/MM/yyyy HH:mm"/></c:when><c:otherwise>Chưa nhận hàng</c:otherwise></c:choose></small>
                </li>
              </ol>
            </c:otherwise>
          </c:choose>
        </section>

        <c:if test="${st == 'CANCELLED'}">
          <div class="alert alert--error sh-notice sh-enter" style="--d:3" role="status">
            <t:icon name="info"/>
            <div>
              <strong>Đơn hàng đã bị hủy<c:if test="${not empty order.cancelledBy}"> bởi <c:out value="${order.cancelledBy.label}"/></c:if>.</strong>
              <c:if test="${not empty order.cancelReason}"><div>Lý do: <c:out value="${order.cancelReason}"/></div></c:if>
            </div>
          </div>
        </c:if>

        <%-- Người nhận, thanh toán --%>
        <div class="sh-info-grid reveal">
          <section class="sh-info" aria-labelledby="sh-recv-title">
            <h2 class="sh-info__title" id="sh-recv-title">Thông tin nhận hàng</h2>
            <p class="sh-info__name"><c:out value="${order.receiverName}"/></p>
            <p><c:out value="${order.receiverPhone}"/></p>
            <p><c:out value="${order.shippingAddress}"/></p>
            <p class="sh-info__note">
              <c:choose>
                <c:when test="${not empty order.note}">Ghi chú: <c:out value="${order.note}"/></c:when>
                <c:otherwise>Không có ghi chú.</c:otherwise>
              </c:choose>
            </p>
          </section>

          <section class="sh-info" aria-labelledby="sh-pay-title">
            <h2 class="sh-info__title" id="sh-pay-title">Thanh toán</h2>
            <div class="sh-pay-row">
              <t:icon name="banknote"/>
              <span class="sh-info__name"><c:out value="${order.paymentMethod.label}"/></span>
            </div>
            <p>
              <span class="badge ${order.paymentStatus == 'PAID' ? 'badge--completed' : 'badge--pending'}"><c:out value="${order.paymentStatus.label}"/></span>
            </p>
            <p class="sh-info__note">
              <c:choose>
                <c:when test="${order.paymentStatus == 'PAID'}">Bạn đã thanh toán cho đơn hàng này.</c:when>
                <c:when test="${st == 'CANCELLED'}">Đơn đã hủy nên không phát sinh thanh toán.</c:when>
                <c:otherwise>Vui lòng chuẩn bị tiền mặt khi nhận hàng.</c:otherwise>
              </c:choose>
            </p>
          </section>
        </div>

        <%-- Sản phẩm --%>
        <h2 class="h-2 sh-section-title reveal">Sản phẩm trong đơn</h2>
        <div class="sh-items reveal">
          <c:forEach items="${order.items}" var="it">
            <article class="line-item">
              <a class="thumb" href="${ctx}/product?slug=${fn:escapeXml(it.productSlug)}"><t:img src="${it.image}" alt="${it.productName}"/></a>
              <div class="sh-line__info">
                <a class="h-3 link-underline" href="${ctx}/product?slug=${fn:escapeXml(it.productSlug)}"><c:out value="${it.productName}"/></a>
                <p class="t-small t-muted">Màu: <c:out value="${it.colorName}"/> · Size: <c:out value="${it.sizeName}"/></p>
                <p class="t-small"><t:price value="${it.unitPrice}"/> × ${it.quantity}</p>
                <c:if test="${st == 'COMPLETED'}">
                  <c:choose>
                    <c:when test="${it.reviewed}"><span class="badge badge--muted">Đã đánh giá</span></c:when>
                    <c:otherwise><a class="btn btn--outline btn--sm" href="${ctx}/product?slug=${fn:escapeXml(it.productSlug)}#reviews">Viết đánh giá</a></c:otherwise>
                  </c:choose>
                </c:if>
              </div>
              <div class="sh-line__side">
                <span class="sh-line__total"><t:price value="${it.lineTotal}"/></span>
              </div>
            </article>
          </c:forEach>
        </div>

        <%-- Tổng kết --%>
        <aside class="summary sh-detail-summary reveal" aria-label="Tổng kết đơn hàng">
          <div class="summary__row"><span>Tạm tính</span><span><t:price value="${order.subtotal}"/></span></div>
          <c:if test="${order.discountAmount > 0}">
            <div class="summary__row sh-summary__discount">
              <span>Giảm giá<c:if test="${not empty order.voucherCode}"> (<c:out value="${order.voucherCode}"/>)</c:if></span>
              <span>−<t:price value="${order.discountAmount}"/></span>
            </div>
          </c:if>
          <div class="summary__row">
            <span>Phí vận chuyển</span>
            <span><c:choose><c:when test="${order.shippingFee > 0}"><t:price value="${order.shippingFee}"/></c:when><c:otherwise>Miễn phí</c:otherwise></c:choose></span>
          </div>
          <div class="summary__row summary__total"><span>Tổng cộng</span><span><t:price value="${order.totalAmount}"/></span></div>
        </aside>
      </div>

    <%@ include file="/WEB-INF/views/order/_order-cancel-dialog.jspf" %>
  </jsp:body>
</t:account-layout>
