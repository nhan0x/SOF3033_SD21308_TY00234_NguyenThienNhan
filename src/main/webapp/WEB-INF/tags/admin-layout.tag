<%@ tag description="Khung trang quản trị: menu bên trái, thanh trên, nội dung" pageEncoding="UTF-8" body-content="scriptless" %>
<%@ attribute name="title" required="true" description="Tiêu đề trang, hiện ở thanh trên và thẻ <title>" %>
<%@ attribute name="active" required="false" description="Mục menu đang chọn: dashboard | orders | products | categories | attributes | vouchers | banners | blogs | reviews | users | trash" %>
<%@ attribute name="actions" fragment="true" required="false" description="Nút ở góc phải tiêu đề trang (ví dụ nút Thêm mới)" %>
<%@ attribute name="head" fragment="true" required="false" %>
<%@ attribute name="scripts" fragment="true" required="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" scope="request"/>
<fmt:setLocale value="vi_VN" scope="request"/>
<!DOCTYPE html>
<html lang="vi" class="no-js">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="demo-mode" content="${initParam.demoMode}">
  <meta name="robots" content="noindex">
  <title><c:out value="${title}"/> | Quản trị Saltair</title>
  <link rel="icon" type="image/png" href="${ctx}/assets/brand/saltair-logo.png">
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;700;800&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="${ctx}/assets/css/style.css">
  <link rel="stylesheet" href="${ctx}/assets/css/admin.css">
  <jsp:invoke fragment="head"/>
</head>
<body class="admin">
<%@ include file="/WEB-INF/views/common/icons.jspf" %>
<div class="overlay" id="overlay"></div>

<aside class="admin-sidebar drawer drawer--left" id="admin-nav" aria-label="Menu quản trị">
  <a class="admin-sidebar__brand" href="${ctx}/admin"><img src="${ctx}/assets/brand/saltair-logo.png" alt="Saltair"><span class="t-label">Quản trị</span></a>
  <nav class="admin-sidebar__nav">
    <a class="${active == 'dashboard' ? 'is-active' : ''}" href="${ctx}/admin"><t:icon name="dashboard"/> Tổng quan</a>

    <p class="admin-sidebar__group">Bán hàng</p>
    <a class="${active == 'orders' ? 'is-active' : ''}" href="${ctx}/admin/orders"><t:icon name="receipt"/> Đơn hàng</a>
    <a class="${active == 'products' ? 'is-active' : ''}" href="${ctx}/admin/products"><t:icon name="shirt"/> Sản phẩm</a>
    <a class="${active == 'categories' ? 'is-active' : ''}" href="${ctx}/admin/categories"><t:icon name="tag"/> Danh mục</a>
    <a class="${active == 'attributes' ? 'is-active' : ''}" href="${ctx}/admin/attributes"><t:icon name="palette"/> Màu và Size</a>
    <a class="${active == 'vouchers' ? 'is-active' : ''}" href="${ctx}/admin/vouchers"><t:icon name="percent"/> Voucher</a>

    <p class="admin-sidebar__group">Nội dung</p>
    <a class="${active == 'banners' ? 'is-active' : ''}" href="${ctx}/admin/banners"><t:icon name="image"/> Banner</a>
    <a class="${active == 'blogs' ? 'is-active' : ''}" href="${ctx}/admin/blogs"><t:icon name="file-text"/> Blog</a>
    <a class="${active == 'reviews' ? 'is-active' : ''}" href="${ctx}/admin/reviews"><t:icon name="message"/> Bình luận</a>

    <p class="admin-sidebar__group">Hệ thống</p>
    <a class="${active == 'users' ? 'is-active' : ''}" href="${ctx}/admin/users"><t:icon name="users"/> Người dùng</a>
    <a class="${active == 'trash' ? 'is-active' : ''}" href="${ctx}/admin/trash"><t:icon name="trash"/> Thùng rác</a>
  </nav>
  <div class="admin-sidebar__foot">
    <a href="${ctx}/home"><t:icon name="home"/> Xem website</a>
    <a href="${ctx}/logout"><t:icon name="log-out"/> Đăng xuất</a>
  </div>
</aside>

<div class="admin-main">
  <header class="admin-topbar">
    <button type="button" class="icon-btn admin-topbar__burger" data-drawer-open="admin-nav" aria-label="Mở menu"><t:icon name="menu" size="md"/></button>
    <h1 class="admin-topbar__title"><c:out value="${title}"/></h1>
    <div class="admin-topbar__user">
      <c:if test="${not empty sessionScope.currentUser}">
        <span class="hide-mobile"><c:out value="${sessionScope.currentUser.fullName}"/></span>
        <span class="admin-avatar" aria-hidden="true">${fn:toUpperCase(fn:substring(sessionScope.currentUser.fullName, 0, 1))}</span>
      </c:if>
    </div>
  </header>
  <main class="admin-content" id="main">
    <div class="admin-page-head">
      <div>
        <p class="t-small t-muted">Saltair / <c:out value="${title}"/></p>
      </div>
      <div class="row row--wrap"><jsp:invoke fragment="actions"/></div>
    </div>
    <jsp:doBody/>
  </main>
</div>

<script src="${ctx}/assets/js/app.js" defer></script>
<jsp:invoke fragment="scripts"/>
</body>
</html>
