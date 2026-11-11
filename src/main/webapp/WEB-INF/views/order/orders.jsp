<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  ĐƠN HÀNG CỦA TÔI  (GET /orders?status=...&page=... -> OrderServlet)  - khu vực tài khoản
  Thuộc tính request:
    orders       : List<Order>         - đơn của trang hiện tại, lọc theo trạng thái, mới nhất trước (có items, itemCount, totalAmount)
    orderPage    : Page<Order>         - thông tin phân trang
    baseUrl      : String              - "/orders?status=..." không có page (cho t:pagination)
    status       : String              - PENDING | SHIPPING | COMPLETED | CANCELLED, rỗng = tất cả
    statusCounts : Map<String,Integer> - số đơn theo trạng thái, khóa: ALL, PENDING, SHIPPING, COMPLETED, CANCELLED
    welcomeProducts : List<Product>    - chỉ có khi khách chưa có đơn nào: sản phẩm cho dải ảnh ở thẻ chào mừng (#85)
  Form (đều gửi tới /order, xử lý ở OrderDetailServlet):
    POST /order action=cancel          fields: orderCode, cancelReason, returnTo=orders   (hộp thoại dùng chung)
    POST /order action=confirmReceived fields: orderCode, returnTo=orders
--%>
<t:account-layout title="Đơn hàng" active="orders">
  <jsp:attribute name="head"><link rel="stylesheet" href="${ctx}/assets/css/pages/shopping.css?v=5"></jsp:attribute>
  <jsp:attribute name="scripts"><script src="${ctx}/assets/js/pages/shopping.js" defer></script></jsp:attribute>
  <jsp:body>
    <c:set var="dialogReturn" value="orders"/>
    <c:choose>
      <c:when test="${status == 'PENDING'}"><c:set var="statusName" value="Chờ xác nhận"/></c:when>
      <c:when test="${status == 'SHIPPING'}"><c:set var="statusName" value="Đang giao"/></c:when>
      <c:when test="${status == 'COMPLETED'}"><c:set var="statusName" value="Hoàn thành"/></c:when>
      <c:when test="${status == 'CANCELLED'}"><c:set var="statusName" value="Đã hủy"/></c:when>
      <c:otherwise><c:set var="statusName" value=""/></c:otherwise>
    </c:choose>

    <c:set var="noOrders" value="${statusCounts['ALL'] == 0}"/>
        <div class="acct-head sh-enter">
          <h1 class="acct-title">Đơn hàng</h1>
        </div>
        <%@ include file="/WEB-INF/views/common/flash.jspf" %>

        <c:if test="${noOrders}">
          <%-- Chưa có đơn nào: thẻ chào mừng kèm dải ảnh sản phẩm xếp nghiêng --%>
          <section class="acct-card acct-welcome sh-enter" style="--d:1" aria-labelledby="acct-welcome-title">
            <div class="acct-welcome__text">
              <h2 class="acct-welcome__title" id="acct-welcome-title">Chào mừng, sẵn sàng mua sắm?</h2>
              <p>Khi bạn đặt hàng, đơn sẽ xuất hiện ở đây để bạn theo dõi.</p>
              <a class="btn btn--primary" href="${ctx}/products">Mua sắm ngay</a>
            </div>
            <c:if test="${not empty welcomeProducts}">
              <div class="acct-welcome__strip" aria-hidden="true">
                <c:forEach items="${welcomeProducts}" var="p" varStatus="s">
                  <a class="acct-welcome__tile" style="--i:${s.index}" href="${ctx}/product?slug=${fn:escapeXml(p.slug)}" tabindex="-1"><t:img src="${empty p.imageThumb ? p.image : p.imageThumb}" alt="" eager="true"/></a>
                </c:forEach>
              </div>
            </c:if>
          </section>
        </c:if>

        <c:if test="${not noOrders}">
        <nav class="sh-filters sh-enter" style="--d:1" aria-label="Lọc đơn hàng theo trạng thái">
          <a class="chip${empty status ? ' is-active' : ''}" href="${ctx}/orders"<c:if test="${empty status}"> aria-current="page"</c:if>>Tất cả <span class="sh-chip-count">${statusCounts['ALL']}</span></a>
          <a class="chip${status == 'PENDING' ? ' is-active' : ''}" href="${ctx}/orders?status=PENDING"<c:if test="${status == 'PENDING'}"> aria-current="page"</c:if>>Chờ xác nhận <span class="sh-chip-count">${statusCounts['PENDING']}</span></a>
          <a class="chip${status == 'SHIPPING' ? ' is-active' : ''}" href="${ctx}/orders?status=SHIPPING"<c:if test="${status == 'SHIPPING'}"> aria-current="page"</c:if>>Đang giao <span class="sh-chip-count">${statusCounts['SHIPPING']}</span></a>
          <a class="chip${status == 'COMPLETED' ? ' is-active' : ''}" href="${ctx}/orders?status=COMPLETED"<c:if test="${status == 'COMPLETED'}"> aria-current="page"</c:if>>Hoàn thành <span class="sh-chip-count">${statusCounts['COMPLETED']}</span></a>
          <a class="chip${status == 'CANCELLED' ? ' is-active' : ''}" href="${ctx}/orders?status=CANCELLED"<c:if test="${status == 'CANCELLED'}"> aria-current="page"</c:if>>Đã hủy <span class="sh-chip-count">${statusCounts['CANCELLED']}</span></a>
        </nav>

        <c:choose>
          <c:when test="${empty orders}">
            <div class="acct-card empty-state sh-enter" style="--d:2">
              <t:icon name="package"/>
              <h2 class="h-2">Chưa có đơn nào ở trạng thái “<c:out value="${statusName}"/>”</h2>
              <p>Thử chọn trạng thái khác hoặc xem tất cả đơn hàng của bạn.</p>
              <a class="btn btn--outline" href="${ctx}/orders">Xem tất cả đơn</a>
            </div>
          </c:when>

          <c:otherwise>
            <div class="sh-order-list" data-stagger>
              <c:forEach items="${orders}" var="o" varStatus="s">
                <article class="sh-order ${s.index lt 3 ? 'sh-enter' : 'reveal'}" style="--d:${s.index + 2}">
                  <div class="sh-order__head">
                    <div>
                      <a class="sh-order__code link-underline" href="${ctx}/order?code=${fn:escapeXml(o.orderCode)}"><c:out value="${o.orderCode}"/></a>
                      <span class="sh-order__date t-small t-muted">Đặt ngày <fmt:formatDate value="${o.createdAt}" pattern="dd/MM/yyyy"/></span>
                    </div>
                    <span class="badge badge--${o.status.cssClass}"><c:out value="${o.status.label}"/></span>
                  </div>

                  <div class="sh-order__body">
                    <div class="sh-thumbs">
                      <c:forEach items="${o.items}" var="it" end="2">
                        <a class="thumb" href="${ctx}/product?slug=${fn:escapeXml(it.productSlug)}"><t:img src="${it.image}" alt="${it.productName}"/></a>
                      </c:forEach>
                      <c:if test="${fn:length(o.items) > 3}"><span class="sh-thumbs__more" aria-label="và ${fn:length(o.items) - 3} sản phẩm khác">+${fn:length(o.items) - 3}</span></c:if>
                    </div>
                    <div class="sh-order__sum">
                      <span class="t-small t-muted">${o.itemCount} sản phẩm</span>
                      <span class="sh-order__total"><t:price value="${o.totalAmount}"/></span>
                    </div>
                  </div>

                  <c:if test="${o.status == 'CANCELLED' and not empty o.cancelReason}">
                    <p class="t-small t-muted">Lý do hủy: <c:out value="${o.cancelReason}"/></p>
                  </c:if>

                  <div class="sh-order__foot">
                    <a class="btn btn--outline btn--sm" href="${ctx}/order?code=${fn:escapeXml(o.orderCode)}">Xem chi tiết</a>

                    <c:if test="${o.status == 'PENDING'}">
                      <button type="button" class="btn btn--ghost btn--sm" data-dialog-open="cancel-dialog" data-dialog-title="Hủy đơn ${fn:escapeXml(o.orderCode)}" data-fill='{"orderCode":"${fn:escapeXml(o.orderCode)}"}'>Hủy đơn</button>
                    </c:if>

                    <c:if test="${o.status == 'SHIPPING'}">
                      <form class="sh-inline-form" method="post" action="${ctx}/order" data-confirm="Bạn đã nhận được đơn hàng ${fn:escapeXml(o.orderCode)}?">
                        <input type="hidden" name="action" value="confirmReceived">
                        <input type="hidden" name="orderCode" value="${fn:escapeXml(o.orderCode)}">
                        <input type="hidden" name="returnTo" value="orders">
                        <button type="submit" class="btn btn--primary btn--sm">Đã nhận hàng</button>
                      </form>
                    </c:if>

                    <c:if test="${o.status == 'COMPLETED'}">
                      <c:set var="reviewSlug" value=""/>
                      <c:forEach items="${o.items}" var="it">
                        <c:if test="${empty reviewSlug and not it.reviewed}"><c:set var="reviewSlug" value="${it.productSlug}"/></c:if>
                      </c:forEach>
                      <c:choose>
                        <c:when test="${not empty reviewSlug}"><a class="btn btn--primary btn--sm" href="${ctx}/product?slug=${fn:escapeXml(reviewSlug)}#reviews">Đánh giá</a></c:when>
                        <c:otherwise><span class="badge badge--muted">Đã đánh giá</span></c:otherwise>
                      </c:choose>
                    </c:if>
                  </div>
                </article>
              </c:forEach>
            </div>
            <t:pagination page="${orderPage.page}" totalPages="${orderPage.totalPages}" baseUrl="${baseUrl}"/>
          </c:otherwise>
        </c:choose>
        </c:if>

    <%@ include file="/WEB-INF/views/order/_order-cancel-dialog.jspf" %>
  </jsp:body>
</t:account-layout>
