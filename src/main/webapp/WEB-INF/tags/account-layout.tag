<%@ tag description="Khung khu vực tài khoản (#85): header tối giản, cột trái là các mục điều hướng chữ lớn, cột phải là nội dung" pageEncoding="UTF-8" body-content="scriptless" %>
<%@ attribute name="title" required="true" description="Tiêu đề trang (tự thêm ' | Saltair')" %>
<%@ attribute name="description" required="false" %>
<%@ attribute name="active" required="true" description="Mục đang mở: orders | profile | addresses | wishlist" %>
<%@ attribute name="head" fragment="true" required="false" description="Thêm vào <head>: CSS riêng của trang (account.css đã có sẵn)" %>
<%@ attribute name="scripts" fragment="true" required="false" description="Thêm cuối <body>: JS riêng của trang" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="${title}" description="${description}" chrome="account" bodyClass="acct-page">
  <jsp:attribute name="head"><link rel="stylesheet" href="${ctx}/assets/css/pages/account.css?v=4"><jsp:invoke fragment="head"/></jsp:attribute>
  <jsp:attribute name="scripts"><jsp:invoke fragment="scripts"/></jsp:attribute>
  <jsp:body>
    <div class="acct">
      <nav class="acct__nav" aria-label="Tài khoản">
        <a class="acct__link${active == 'orders' ? ' is-active' : ''}" href="${ctx}/orders"<c:if test="${active == 'orders'}"> aria-current="page"</c:if>>Đơn hàng</a>
        <a class="acct__link${active == 'profile' ? ' is-active' : ''}" href="${ctx}/profile"<c:if test="${active == 'profile'}"> aria-current="page"</c:if>>Hồ sơ</a>
        <a class="acct__link${active == 'addresses' ? ' is-active' : ''}" href="${ctx}/addresses"<c:if test="${active == 'addresses'}"> aria-current="page"</c:if>>Sổ địa chỉ</a>
        <a class="acct__link${active == 'wishlist' ? ' is-active' : ''}" href="${ctx}/wishlist"<c:if test="${active == 'wishlist'}"> aria-current="page"</c:if>>Yêu thích</a>
        <a class="acct__logout" href="${ctx}/logout">Đăng xuất</a>
      </nav>
      <div class="acct__main">
        <jsp:doBody/>
      </div>
    </div>
  </jsp:body>
</t:layout>
