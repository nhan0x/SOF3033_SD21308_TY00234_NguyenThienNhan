<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  SỔ ĐỊA CHỈ  (GET /addresses -> AddressServlet)  [Customer, cần đăng nhập]
  Request attributes:
    addresses : List<Address>         - địa chỉ của user (mặc định đứng đầu); fullAddress có thể rỗng, JSP tự ghép từ addressLine, ward, district, city
    errors    : Map<String,String>    - (tùy chọn) lỗi theo ô của hộp thoại: receiverName, phone, addressLine, ward, district, city. Có errors thì hộp thoại tự mở lại
    message / messageType             - (tùy chọn) thông báo chung, xem flash.jspf
  Forms:
    POST /addresses  action=save        fields=id,receiverName,phone,addressLine,ward,district,city,defaultAddress  (id = 0: thêm mới, id > 0: sửa)
    POST /addresses  action=setDefault  fields=id
    POST /addresses  action=delete      fields=id
--%>
<t:account-layout title="Sổ địa chỉ" active="addresses" description="Quản lý địa chỉ giao hàng của bạn tại Saltair.">
  <jsp:attribute name="scripts"><script src="${ctx}/assets/js/pages/account.js" defer></script></jsp:attribute>
  <jsp:body>
        <div class="acct-head acc-enter">
          <div>
            <h1 class="acct-title">Sổ địa chỉ</h1>
            <p class="t-muted">Bạn đã lưu <span data-demo-count>${fn:length(addresses)}</span> địa chỉ. Địa chỉ mặc định sẽ được chọn sẵn khi thanh toán.</p>
          </div>
          <button type="button" class="btn btn--primary" data-dialog-open="address-dialog" data-dialog-reset data-dialog-title="Thêm địa chỉ mới">
            <t:icon name="plus"/> Thêm địa chỉ
          </button>
        </div>

        <%@ include file="/WEB-INF/views/common/flash.jspf" %>

        <c:if test="${not empty addresses}">
          <div class="address-grid" data-stagger>
            <c:forEach items="${addresses}" var="a" varStatus="s">
              <c:set var="fRecv" value="${fn:replace(fn:escapeXml(a.receiverName), '&#034;', '&#8221;')}"/>
              <c:set var="fPhone" value="${fn:replace(fn:escapeXml(a.phone), '&#034;', '&#8221;')}"/>
              <c:set var="fLine" value="${fn:replace(fn:escapeXml(a.addressLine), '&#034;', '&#8221;')}"/>
              <c:set var="fWard" value="${fn:replace(fn:escapeXml(a.ward), '&#034;', '&#8221;')}"/>
              <c:set var="fDistrict" value="${fn:replace(fn:escapeXml(a.district), '&#034;', '&#8221;')}"/>
              <c:set var="fCity" value="${fn:replace(fn:escapeXml(a.city), '&#034;', '&#8221;')}"/>
              <article class="address-card ${s.index < 2 ? 'acc-enter' : 'reveal'}${a.defaultAddress ? ' is-default' : ''}" style="--d:${s.index + 2}">
                <div class="address-card__head">
                  <span class="address-card__name"><c:out value="${a.receiverName}"/></span>
                  <c:if test="${a.defaultAddress}"><span class="badge badge--solid">Mặc định</span></c:if>
                </div>
                <div class="address-card__meta">
                  <div class="address-card__row"><t:icon name="phone"/><span><c:out value="${a.phone}"/></span></div>
                  <div class="address-card__row"><t:icon name="map-pin"/>
                    <span>
                      <c:choose>
                        <c:when test="${not empty a.fullAddress}"><c:out value="${a.fullAddress}"/></c:when>
                        <c:otherwise><c:out value="${a.addressLine}"/><c:if test="${not empty a.ward}">, <c:out value="${a.ward}"/></c:if><c:if test="${not empty a.district}">, <c:out value="${a.district}"/></c:if>, <c:out value="${a.city}"/></c:otherwise>
                      </c:choose>
                    </span>
                  </div>
                </div>
                <div class="address-card__actions">
                  <button type="button" class="btn btn--ghost btn--xs" data-dialog-open="address-dialog" data-dialog-title="Sửa địa chỉ"
                          data-fill='{"id":${a.id},"receiverName":"${fRecv}","phone":"${fPhone}","addressLine":"${fLine}","ward":"${fWard}","district":"${fDistrict}","city":"${fCity}","defaultAddress":${a.defaultAddress}}'>
                    <t:icon name="pencil"/> Sửa
                  </button>
                  <c:if test="${not a.defaultAddress}">
                    <form method="post" action="${ctx}/addresses">
                      <input type="hidden" name="action" value="setDefault">
                      <input type="hidden" name="id" value="${a.id}">
                      <button type="submit" class="btn btn--ghost btn--xs">Đặt làm mặc định</button>
                    </form>
                  </c:if>
                  <form method="post" action="${ctx}/addresses" data-confirm="Bạn có chắc muốn xóa địa chỉ này?">
                    <input type="hidden" name="action" value="delete">
                    <input type="hidden" name="id" value="${a.id}">
                    <button type="submit" class="btn btn--danger btn--xs"><t:icon name="trash"/> Xóa</button>
                  </form>
                </div>
              </article>
            </c:forEach>
          </div>
        </c:if>

        <div class="empty-state" data-demo-empty${empty addresses ? '' : ' hidden'}>
          <t:icon name="map-pin"/>
          <h2 class="h-2">Bạn chưa có địa chỉ nào</h2>
          <p>Thêm địa chỉ giao hàng để đặt hàng nhanh hơn ở những lần sau.</p>
          <button type="button" class="btn btn--primary" data-dialog-open="address-dialog" data-dialog-reset data-dialog-title="Thêm địa chỉ mới">Thêm địa chỉ đầu tiên</button>
        </div>

    <c:set var="addressDialogOpen" value="${not empty errors}"/>
    <%@ include file="/WEB-INF/views/account/_address-dialog.jspf" %>
  </jsp:body>
</t:account-layout>
