<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  ADMIN - DANH SÁCH ĐƠN HÀNG
  Route:   GET /admin/orders?q=&status=&payment=&page=
  Servlet: com.fashionweb.controller.admin.sales.AdminOrderServlet (active menu: orders)
  Request attributes:
    orderPage    : Page<Order>          - đơn của trang hiện tại (ADMIN_PAGE_SIZE), mới nhất trước
    q            : String               - từ khóa (mã đơn hoặc tên khách), "" nếu không lọc
    status       : String               - PENDING | SHIPPING | COMPLETED | CANCELLED, "" = tất cả
    payment      : String               - UNPAID | PAID, "" = tất cả
    statusCounts : Map<String,Integer>  - số đơn theo trạng thái: ALL, PENDING, SHIPPING, COMPLETED, CANCELLED (áp dụng q và payment)
  Không có form gửi dữ liệu ở trang này (form tìm kiếm dùng GET).
--%>
<t:admin-layout title="Đơn hàng" active="orders">
  <jsp:attribute name="head"><link rel="stylesheet" href="${ctx}/assets/css/pages/admin-ops.css"></jsp:attribute>
  <jsp:attribute name="scripts"><script src="${ctx}/assets/js/pages/admin-ops.js" defer></script></jsp:attribute>
  <jsp:body>
    <%@ include file="/WEB-INF/views/common/flash.jspf" %>

    <%-- q đã mã hóa để dùng trong liên kết lọc và phân trang --%>
    <c:url var="qUrl" value="/admin/orders"><c:param name="q" value="${q}"/></c:url>
    <c:set var="qs" value="${fn:substringAfter(qUrl, '?q=')}"/>
    <c:set var="pageBase" value="/admin/orders?q=${qs}&status=${status}&payment=${payment}"/>

    <div class="admin-card admin-card--flush ops-enter">
      <form class="admin-toolbar" method="get" action="${ctx}/admin/orders" role="search" aria-label="Tìm và lọc đơn hàng">
        <div class="admin-toolbar__left">
          <label class="search-field">
            <span class="sr-only">Tìm theo mã đơn hoặc tên khách</span>
            <t:icon name="search"/>
            <input class="input" type="search" name="q" value="${fn:escapeXml(q)}" placeholder="Mã đơn hoặc tên khách" maxlength="100" autocomplete="off">
          </label>
          <label class="sr-only" for="filter-payment">Thanh toán</label>
          <select class="select" id="filter-payment" name="payment" data-autosubmit>
            <option value="">Mọi thanh toán</option>
            <option value="UNPAID"${payment == 'UNPAID' ? ' selected' : ''}>Chưa thanh toán</option>
            <option value="PAID"${payment == 'PAID' ? ' selected' : ''}>Đã thanh toán</option>
          </select>
          <input type="hidden" name="status" value="${fn:escapeXml(status)}">
          <button class="btn btn--primary btn--sm" type="submit">Tìm kiếm</button>
          <c:if test="${not empty q or not empty status or not empty payment}">
            <a class="btn btn--ghost btn--sm" href="${ctx}/admin/orders">Xóa bộ lọc</a>
          </c:if>
        </div>
        <p class="t-small t-muted">${orderPage.totalItems} đơn hàng</p>
      </form>

      <nav class="chip-row" aria-label="Lọc theo trạng thái">
        <a class="chip${empty status ? ' is-active' : ''}" href="${ctx}/admin/orders?q=${qs}&amp;payment=${payment}"${empty status ? ' aria-current="true"' : ''}>Tất cả <span class="chip__count">${statusCounts['ALL']}</span></a>
        <a class="chip${status == 'PENDING' ? ' is-active' : ''}" href="${ctx}/admin/orders?q=${qs}&amp;payment=${payment}&amp;status=PENDING"${status == 'PENDING' ? ' aria-current="true"' : ''}>Chờ xác nhận <span class="chip__count">${statusCounts['PENDING']}</span></a>
        <a class="chip${status == 'SHIPPING' ? ' is-active' : ''}" href="${ctx}/admin/orders?q=${qs}&amp;payment=${payment}&amp;status=SHIPPING"${status == 'SHIPPING' ? ' aria-current="true"' : ''}>Đang giao <span class="chip__count">${statusCounts['SHIPPING']}</span></a>
        <a class="chip${status == 'COMPLETED' ? ' is-active' : ''}" href="${ctx}/admin/orders?q=${qs}&amp;payment=${payment}&amp;status=COMPLETED"${status == 'COMPLETED' ? ' aria-current="true"' : ''}>Hoàn thành <span class="chip__count">${statusCounts['COMPLETED']}</span></a>
        <a class="chip${status == 'CANCELLED' ? ' is-active' : ''}" href="${ctx}/admin/orders?q=${qs}&amp;payment=${payment}&amp;status=CANCELLED"${status == 'CANCELLED' ? ' aria-current="true"' : ''}>Đã hủy <span class="chip__count">${statusCounts['CANCELLED']}</span></a>
      </nav>

      <div class="admin-table-wrap">
        <table class="table responsive ops-table">
          <caption class="sr-only">Danh sách đơn hàng</caption>
          <thead>
            <tr>
              <th scope="col">Mã đơn</th>
              <th scope="col">Khách hàng</th>
              <th scope="col">Ngày đặt</th>
              <th scope="col" class="num">Số SP</th>
              <th scope="col" class="num">Tổng tiền</th>
              <th scope="col">Thanh toán</th>
              <th scope="col">Trạng thái</th>
              <th scope="col"><span class="sr-only">Thao tác</span></th>
            </tr>
          </thead>
          <tbody>
            <c:forEach items="${orderPage.items}" var="o">
              <tr>
                <td class="cell-lead">
                  <a class="link-code link-underline" href="${ctx}/admin/order?code=${fn:escapeXml(o.orderCode)}"><c:out value="${o.orderCode}"/></a>
                </td>
                <td data-label="Khách hàng">
                  <div class="cell-stack">
                    <span><c:out value="${o.customerName}"/></span>
                    <small><c:out value="${o.customerEmail}"/></small>
                  </div>
                </td>
                <td data-label="Ngày đặt" class="cell-nowrap"><fmt:formatDate value="${o.createdAt}" pattern="dd/MM/yyyy HH:mm"/></td>
                <td data-label="Số SP" class="num">${o.itemCount}</td>
                <td data-label="Tổng tiền" class="num cell-nowrap bold"><t:price value="${o.totalAmount}"/></td>
                <td data-label="Thanh toán">
                  <span class="badge ${o.paymentStatus == 'PAID' ? 'badge--success' : 'badge--muted'}">${o.paymentStatus.label}</span>
                </td>
                <td data-label="Trạng thái"><span class="badge badge--${o.status.cssClass}">${o.status.label}</span></td>
                <td>
                  <div class="cell-actions">
                    <a class="icon-btn" href="${ctx}/admin/order?code=${fn:escapeXml(o.orderCode)}" aria-label="Xem đơn ${fn:escapeXml(o.orderCode)}" title="Xem chi tiết"><t:icon name="eye"/></a>
                  </div>
                </td>
              </tr>
            </c:forEach>
            <c:if test="${empty orderPage.items}">
              <tr class="empty-row"><td colspan="8">Không có đơn hàng nào phù hợp với bộ lọc.</td></tr>
            </c:if>
          </tbody>
        </table>
      </div>

      <c:set var="rangeFrom" value="${(orderPage.page - 1) * orderPage.pageSize + 1}"/>
      <c:set var="rangeTo" value="${rangeFrom + fn:length(orderPage.items) - 1}"/>
      <div class="admin-table-foot">
        <span>
          <c:choose>
            <c:when test="${orderPage.totalItems > 0}">Hiển thị ${rangeFrom} đến ${rangeTo} trong ${orderPage.totalItems} đơn</c:when>
            <c:otherwise>Chưa có đơn nào</c:otherwise>
          </c:choose>
        </span>
        <t:pagination page="${orderPage.page}" totalPages="${orderPage.totalPages}" baseUrl="${pageBase}"/>
      </div>
    </div>
  </jsp:body>
</t:admin-layout>
