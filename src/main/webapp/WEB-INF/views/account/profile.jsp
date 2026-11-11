<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  HỒ SƠ  (GET /profile -> ProfileServlet)  [Customer, cần đăng nhập]  [#85: khung tài khoản riêng, sửa trong hộp thoại]
  Request attributes:
    user          : User                 - người dùng hiện tại (fullName, email, phone, createdAt)
    addresses     : List<Address>        - sổ địa chỉ của user (mặc định đứng đầu)
    errors        : Map<String,String>   - (tùy chọn) lỗi theo ô: fullName, phone. Có errors thì hộp thoại sửa thông tin tự mở lại
    message / messageType                - (tùy chọn) thông báo chung, xem flash.jspf
  Forms:
    POST /profile    action=updateProfile  fields=fullName,phone (email chỉ đọc)            - hộp thoại "Sửa thông tin cá nhân"
    POST /addresses  action=save           fields=id,receiverName,phone,addressLine,ward,district,city,defaultAddress
                                           - hộp thoại địa chỉ dùng chung (_address-dialog.jspf), lưu xong về /addresses
--%>
<t:account-layout title="Hồ sơ" active="profile" description="Xem và cập nhật thông tin cá nhân, địa chỉ giao hàng của tài khoản Saltair.">
  <jsp:attribute name="scripts"><script src="${ctx}/assets/js/pages/account.js" defer></script></jsp:attribute>
  <jsp:body>
    <div class="acct-head acc-enter">
      <div>
        <h1 class="acct-title">Hồ sơ</h1>
        <c:if test="${not empty user.createdAt}"><p class="t-muted">Thành viên từ <fmt:formatDate value="${user.createdAt}" pattern="dd/MM/yyyy"/></p></c:if>
      </div>
    </div>

    <%@ include file="/WEB-INF/views/common/flash.jspf" %>

    <%-- Thông tin cá nhân --%>
    <section class="acct-card acc-enter" style="--d:1" aria-labelledby="profile-info-title">
      <div class="acct-card__head">
        <h2 class="acct-card__title" id="profile-info-title">Thông tin cá nhân</h2>
        <button type="button" class="acct-edit" data-dialog-open="profile-dialog" aria-label="Sửa thông tin cá nhân"><t:icon name="pencil"/></button>
      </div>
      <dl class="acct-fields">
        <div class="acct-field">
          <dt>Họ và tên</dt>
          <dd><c:out value="${user.fullName}"/></dd>
        </div>
        <div class="acct-field">
          <dt>Email</dt>
          <dd><c:out value="${user.email}"/></dd>
        </div>
        <div class="acct-field">
          <dt>Số điện thoại</dt>
          <dd><c:choose><c:when test="${empty user.phone}"><span class="t-muted">Chưa có</span></c:when><c:otherwise><c:out value="${user.phone}"/></c:otherwise></c:choose></dd>
        </div>
      </dl>
    </section>

    <%-- Địa chỉ --%>
    <section class="acct-card acc-enter" style="--d:2" aria-labelledby="profile-addr-title">
      <div class="acct-card__head">
        <h2 class="acct-card__title" id="profile-addr-title">Địa chỉ</h2>
        <button type="button" class="acct-add" data-dialog-open="address-dialog" data-dialog-reset data-dialog-title="Thêm địa chỉ mới"><t:icon name="plus"/> Thêm</button>
      </div>
      <c:choose>
        <c:when test="${empty addresses}">
          <p class="acct-note">Bạn chưa lưu địa chỉ nào. Thêm địa chỉ giao hàng để đặt hàng nhanh hơn ở những lần sau.</p>
        </c:when>
        <c:otherwise>
          <div class="acct-addresses">
            <c:forEach items="${addresses}" var="a">
              <c:set var="fRecv" value="${fn:replace(fn:escapeXml(a.receiverName), '&#034;', '&#8221;')}"/>
              <c:set var="fPhone" value="${fn:replace(fn:escapeXml(a.phone), '&#034;', '&#8221;')}"/>
              <c:set var="fLine" value="${fn:replace(fn:escapeXml(a.addressLine), '&#034;', '&#8221;')}"/>
              <c:set var="fWard" value="${fn:replace(fn:escapeXml(a.ward), '&#034;', '&#8221;')}"/>
              <c:set var="fDistrict" value="${fn:replace(fn:escapeXml(a.district), '&#034;', '&#8221;')}"/>
              <c:set var="fCity" value="${fn:replace(fn:escapeXml(a.city), '&#034;', '&#8221;')}"/>
              <div class="acct-address">
                <div class="acct-address__text">
                  <c:if test="${a.defaultAddress}"><span class="acct-address__tag">Địa chỉ mặc định</span></c:if>
                  <span class="acct-address__name"><c:out value="${a.receiverName}"/></span>
                  <span><c:out value="${a.phone}"/></span>
                  <span>
                    <c:choose>
                      <c:when test="${not empty a.fullAddress}"><c:out value="${a.fullAddress}"/></c:when>
                      <c:otherwise><c:out value="${a.addressLine}"/><c:if test="${not empty a.ward}">, <c:out value="${a.ward}"/></c:if><c:if test="${not empty a.district}">, <c:out value="${a.district}"/></c:if>, <c:out value="${a.city}"/></c:otherwise>
                    </c:choose>
                  </span>
                </div>
                <button type="button" class="acct-edit" data-dialog-open="address-dialog" data-dialog-title="Sửa địa chỉ" aria-label="Sửa địa chỉ của ${fn:escapeXml(a.receiverName)}"
                        data-fill='{"id":${a.id},"receiverName":"${fRecv}","phone":"${fPhone}","addressLine":"${fLine}","ward":"${fWard}","district":"${fDistrict}","city":"${fCity}","defaultAddress":${a.defaultAddress}}'><t:icon name="pencil"/></button>
              </div>
            </c:forEach>
          </div>
          <a class="acct-more" href="${ctx}/addresses">Quản lý sổ địa chỉ</a>
        </c:otherwise>
      </c:choose>
    </section>

    <%-- Hộp thoại sửa thông tin cá nhân --%>
    <dialog class="dialog" id="profile-dialog" aria-labelledby="profile-dialog-title"${not empty errors ? ' data-open' : ''}>
      <form method="post" action="${ctx}/profile">
        <input type="hidden" name="action" value="updateProfile">
        <div class="dialog__head">
          <h2 class="h-2" id="profile-dialog-title">Sửa thông tin cá nhân</h2>
          <button type="button" class="icon-btn" data-dialog-close aria-label="Đóng"><t:icon name="x" size="md"/></button>
        </div>
        <div class="dialog__body">
          <div class="field">
            <label class="field__label" for="profile-name">Họ và tên</label>
            <input class="input${not empty errors.fullName ? ' is-invalid' : ''}" id="profile-name" type="text" name="fullName"
                   value="${fn:escapeXml(empty param.fullName ? user.fullName : param.fullName)}" autocomplete="name" minlength="2" maxlength="100" required>
            <c:if test="${not empty errors.fullName}"><span class="field__error"><c:out value="${errors.fullName}"/></span></c:if>
          </div>
          <div class="field">
            <label class="field__label" for="profile-phone">Số điện thoại</label>
            <input class="input${not empty errors.phone ? ' is-invalid' : ''}" id="profile-phone" type="tel" name="phone"
                   value="${fn:escapeXml(empty param.action ? user.phone : param.phone)}" autocomplete="tel" inputmode="numeric"
                   pattern="0[0-9]{9}" title="Số điện thoại gồm 10 chữ số, bắt đầu bằng 0">
            <c:if test="${not empty errors.phone}"><span class="field__error"><c:out value="${errors.phone}"/></span></c:if>
          </div>
          <div class="field">
            <label class="field__label" for="profile-email">Email</label>
            <input class="input" id="profile-email" type="email" value="${fn:escapeXml(user.email)}" readonly aria-describedby="profile-email-hint">
            <span class="field__hint" id="profile-email-hint">Email là tên đăng nhập nên không thể thay đổi.</span>
          </div>
        </div>
        <div class="dialog__foot">
          <button type="button" class="btn btn--ghost" data-dialog-close>Hủy</button>
          <button type="submit" class="btn btn--primary">Lưu</button>
        </div>
      </form>
    </dialog>

    <c:set var="addressDialogOpen" value="${false}"/>
    <%@ include file="/WEB-INF/views/account/_address-dialog.jspf" %>
  </jsp:body>
</t:account-layout>
