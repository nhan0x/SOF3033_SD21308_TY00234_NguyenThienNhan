<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  TÀI KHOẢN CỦA TÔI  (GET /profile -> ProfileServlet)  [Customer, cần đăng nhập]
  Request attributes:
    user          : User                 - người dùng hiện tại (fullName, email, phone, createdAt)
    orderCount    : Integer              - số đơn hàng của user
    wishlistCount : Integer              - số sản phẩm yêu thích
    errors        : Map<String,String>   - (tùy chọn) lỗi theo ô: fullName, phone
    message / messageType                - (tùy chọn) thông báo chung, xem flash.jspf
  Forms:
    POST /profile  action=updateProfile   fields=fullName,phone (email chỉ đọc)
--%>
<t:layout title="Tài khoản của tôi" description="Xem và cập nhật thông tin cá nhân, đổi mật khẩu tài khoản Saltair.">
  <jsp:attribute name="head"><link rel="stylesheet" href="${ctx}/assets/css/pages/account.css"></jsp:attribute>
  <jsp:attribute name="scripts"><script src="${ctx}/assets/js/pages/account.js" defer></script></jsp:attribute>
  <jsp:body>
    <c:set var="accountActive" value="profile"/>
    <c:set var="nameParts" value="${fn:split(user.fullName, ' ')}"/>
    <c:set var="initial" value="${fn:toUpperCase(fn:substring(nameParts[fn:length(nameParts) - 1], 0, 1))}"/>
    <div class="account">
      <%@ include file="/WEB-INF/views/common/account-nav.jspf" %>

      <div class="account__main">
        <div class="profile-head acc-enter">
          <div class="avatar" aria-hidden="true"><c:out value="${initial}"/></div>
          <div>
            <h1 class="h-1"><c:out value="${user.fullName}"/></h1>
            <p class="t-muted mt-8"><c:out value="${user.email}"/><c:if test="${not empty user.createdAt}"> · Thành viên từ <fmt:formatDate value="${user.createdAt}" pattern="dd/MM/yyyy"/></c:if></p>
          </div>
        </div>

        <%@ include file="/WEB-INF/views/common/flash.jspf" %>

        <div class="stat-row acc-enter" style="--d:1">
          <a class="stat" href="${ctx}/orders">
            <div>
              <span class="stat__label">Đơn hàng</span>
              <span class="stat__value h-1" data-countup="${orderCount}">${orderCount}</span>
            </div>
            <t:icon name="arrow-right"/>
          </a>
          <a class="stat" href="${ctx}/wishlist">
            <div>
              <span class="stat__label">Yêu thích</span>
              <span class="stat__value h-1" data-countup="${wishlistCount}">${wishlistCount}</span>
            </div>
            <t:icon name="arrow-right"/>
          </a>
          <a class="stat" href="${ctx}/addresses">
            <div>
              <span class="stat__label">Sổ địa chỉ</span>
              <span class="stat__value"><t:icon name="map-pin" size="lg"/></span>
            </div>
            <t:icon name="arrow-right"/>
          </a>
        </div>

        <section class="card acc-enter" style="--d:2" aria-labelledby="profile-info-title">
          <div class="card__head">
            <h2 class="h-2" id="profile-info-title">Thông tin cá nhân</h2>
            <p class="t-small t-muted">Họ tên và số điện thoại dùng để liên hệ khi giao hàng.</p>
          </div>
          <form method="post" action="${ctx}/profile">
            <input type="hidden" name="action" value="updateProfile">
            <div class="form-grid">
              <div class="field">
                <label class="field__label" for="profile-name">Họ và tên</label>
                <input class="input${not empty errors.fullName ? ' is-invalid' : ''}" id="profile-name" type="text" name="fullName"
                       value="${fn:escapeXml(empty param.fullName ? user.fullName : param.fullName)}" autocomplete="name" minlength="2" maxlength="100" required>
                <c:if test="${not empty errors.fullName}"><span class="field__error"><c:out value="${errors.fullName}"/></span></c:if>
              </div>
              <div class="field">
                <label class="field__label" for="profile-phone">Số điện thoại</label>
                <input class="input${not empty errors.phone ? ' is-invalid' : ''}" id="profile-phone" type="tel" name="phone"
                       value="${fn:escapeXml(empty param.phone ? user.phone : param.phone)}" autocomplete="tel" inputmode="numeric"
                       pattern="0[0-9]{9}" title="Số điện thoại gồm 10 chữ số, bắt đầu bằng 0">
                <c:if test="${not empty errors.phone}"><span class="field__error"><c:out value="${errors.phone}"/></span></c:if>
              </div>
              <div class="field span-2">
                <label class="field__label" for="profile-email">Email</label>
                <input class="input" id="profile-email" type="email" name="email" value="${fn:escapeXml(user.email)}" readonly aria-describedby="profile-email-hint">
                <span class="field__hint" id="profile-email-hint">Email là tên đăng nhập nên không thể thay đổi.</span>
              </div>
            </div>
            <div class="form-actions">
              <button class="btn btn--primary" type="submit">Lưu thay đổi</button>
            </div>
          </form>
        </section>
      </div>
    </div>
  </jsp:body>
</t:layout>
