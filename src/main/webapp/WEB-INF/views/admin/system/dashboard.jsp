<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  ADMIN - TỔNG QUAN  (GET /admin -> AdminDashboardServlet)
  Tham số: dateFrom, dateTo (yyyy-MM-dd)
  Dữ liệu:
    stats: DashboardStats - totalOrders, totalRevenue, totalCustomers, pendingOrders, revenueByDay, topProducts, ordersByStatus
    recentOrders: List<Order> - 5 đơn mới nhất
    lowStockProducts: List<Product> - 5 sản phẩm có tồn kho thấp nhất
    dateFrom, dateTo: String - yyyy-MM-dd, mặc định 7 ngày gần nhất
    revenueMax: long - doanh thu ngày cao nhất, dùng tính chiều cao cột
    rangeRevenue: long - tổng doanh thu các ngày đang hiển thị trên biểu đồ
--%>
<t:admin-layout title="Tổng quan" active="dashboard">
  <jsp:attribute name="head"><link rel="stylesheet" href="${ctx}/assets/css/pages/admin-catalog.css"></jsp:attribute>
  <jsp:attribute name="scripts"><script src="${ctx}/assets/js/pages/admin-catalog.js" defer></script></jsp:attribute>
  <jsp:attribute name="actions"><a class="btn btn--outline btn--sm" href="${ctx}/admin/orders"><t:icon name="receipt"/> Xem đơn hàng</a></jsp:attribute>
  <jsp:body>
    <div class="ac-stack">

      <%-- Bộ lọc ngày --%>
      <form class="admin-card dash-filter" method="get" action="${ctx}/admin" data-date-range>
        <div class="field">
          <label class="field__label" for="dateFrom">Từ ngày</label>
          <input class="input" type="date" id="dateFrom" name="dateFrom" value="${fn:escapeXml(dateFrom)}" required>
        </div>
        <div class="field">
          <label class="field__label" for="dateTo">Đến ngày</label>
          <input class="input" type="date" id="dateTo" name="dateTo" value="${fn:escapeXml(dateTo)}" required>
        </div>
        <button class="btn btn--primary btn--sm" type="submit">Lọc</button>
        <p class="ac-note dash-filter__hint">Doanh thu chỉ tính các đơn đã hoàn thành.</p>
      </form>

      <%-- Bốn thẻ số liệu --%>
      <div class="admin-grid admin-grid--4">
        <div class="stat-card">
          <span class="stat-card__label"><t:icon name="receipt"/> Tổng đơn hàng</span>
          <span class="stat-card__value" data-countup="${stats.totalOrders}"><fmt:formatNumber value="${stats.totalOrders}"/></span>
          <span class="stat-card__note">Mọi trạng thái</span>
        </div>
        <div class="stat-card">
          <span class="stat-card__label"><t:icon name="banknote"/> Doanh thu</span>
          <span class="stat-card__value" data-countup="${stats.totalRevenue}" data-suffix="đ"><fmt:formatNumber value="${stats.totalRevenue}"/>đ</span>
          <span class="stat-card__note">Đơn hoàn thành</span>
        </div>
        <div class="stat-card">
          <span class="stat-card__label"><t:icon name="users"/> Khách hàng</span>
          <span class="stat-card__value" data-countup="${stats.totalCustomers}"><fmt:formatNumber value="${stats.totalCustomers}"/></span>
          <span class="stat-card__note">Tài khoản khách đã đăng ký</span>
        </div>
        <div class="stat-card">
          <span class="stat-card__label"><t:icon name="clock"/> Đơn chờ xác nhận</span>
          <span class="stat-card__value" data-countup="${stats.pendingOrders}"><fmt:formatNumber value="${stats.pendingOrders}"/></span>
          <span class="stat-card__note"><a class="link-underline" href="${ctx}/admin/orders?status=PENDING">Xử lý ngay</a></span>
        </div>
      </div>

      <div class="admin-grid admin-grid--main">

        <%-- Cột trái --%>
        <div class="ac-stack" data-stagger>

          <section class="admin-card" aria-labelledby="chart-title">
            <h2 class="admin-card__title" id="chart-title">
              <span>Doanh thu theo ngày</span>
              <span class="t-small t-muted">Tổng: <span class="bold"><t:price value="${rangeRevenue}"/></span></span>
            </h2>
            <c:choose>
              <c:when test="${empty stats.revenueByDay}">
                <div class="empty-state"><t:icon name="bar-chart"/><p>Chưa có doanh thu trong khoảng ngày đã chọn.</p></div>
              </c:when>
              <c:otherwise>
                <div class="bar-chart" role="list" style="--n:${fn:length(stats.revenueByDay)}">
                  <c:forEach items="${stats.revenueByDay}" var="d" varStatus="s">
                    <c:set var="pct" value="${revenueMax > 0 ? d.value * 100 / revenueMax : 0}"/>
                    <fmt:formatNumber var="pctText" value="${pct}" maxFractionDigits="0" groupingUsed="false"/>
                    <fmt:formatNumber var="fullText" value="${d.value}" maxFractionDigits="0"/>
                    <div class="bar-chart__col" role="listitem" aria-label="${fn:escapeXml(d.label)}: ${fullText}đ" title="${fn:escapeXml(d.label)}: ${fullText}đ">
                      <div class="bar-chart__bar" style="--v:${pctText};--i:${s.index}"></div>
                      <span class="bar-chart__val"><c:choose><c:when test="${d.value == 0}">0</c:when><c:otherwise><fmt:formatNumber value="${d.value / 1000000}" maxFractionDigits="1"/>tr</c:otherwise></c:choose></span>
                      <span class="bar-chart__lbl"><c:out value="${d.label}"/></span>
                    </div>
                  </c:forEach>
                </div>
              </c:otherwise>
            </c:choose>
          </section>

          <section class="admin-card admin-card--flush reveal" aria-labelledby="top-title">
            <h2 class="admin-card__title" id="top-title"><span>Top sản phẩm bán chạy</span><a class="t-small link-underline" href="${ctx}/admin/products">Tất cả sản phẩm</a></h2>
            <div class="admin-table-wrap">
              <table class="table">
                <thead><tr><th>Sản phẩm</th><th class="num">Đã bán</th><th class="num">Giá</th></tr></thead>
                <tbody>
                  <c:forEach items="${stats.topProducts}" var="p">
                    <tr>
                      <td>
                        <div class="cell-product">
                          <div class="thumb"><t:img src="${p.imageThumb}" alt="${p.name}"/></div>
                          <div>
                            <a class="bold link-underline" href="${ctx}/admin/products/form?id=${p.id}"><c:out value="${p.name}"/></a>
                            <div class="t-small t-muted"><c:out value="${p.categoryName}"/></div>
                          </div>
                        </div>
                      </td>
                      <td class="num"><fmt:formatNumber value="${p.soldCount}"/></td>
                      <td class="num nowrap"><t:price value="${p.price}"/></td>
                    </tr>
                  </c:forEach>
                  <c:if test="${empty stats.topProducts}"><tr class="empty-row"><td colspan="3">Chưa có sản phẩm nào được bán.</td></tr></c:if>
                </tbody>
              </table>
            </div>
          </section>

          <section class="admin-card admin-card--flush reveal" aria-labelledby="recent-title">
            <h2 class="admin-card__title" id="recent-title"><span>Đơn hàng gần đây</span><a class="t-small link-underline" href="${ctx}/admin/orders">Tất cả đơn hàng</a></h2>
            <div class="admin-table-wrap">
              <table class="table">
                <thead><tr><th>Mã đơn</th><th>Khách hàng</th><th>Ngày đặt</th><th class="num">Tổng tiền</th><th>Trạng thái</th></tr></thead>
                <tbody>
                  <c:forEach items="${recentOrders}" var="o">
                    <tr>
                      <td class="nowrap"><a class="bold link-underline" href="${ctx}/admin/order?code=${fn:escapeXml(o.orderCode)}"><c:out value="${o.orderCode}"/></a></td>
                      <td><c:out value="${o.customerName}"/></td>
                      <td class="nowrap"><fmt:formatDate value="${o.createdAt}" pattern="dd/MM/yyyy HH:mm"/></td>
                      <td class="num nowrap"><t:price value="${o.totalAmount}"/></td>
                      <td><span class="badge badge--${o.status.cssClass}"><c:out value="${o.status.label}"/></span></td>
                    </tr>
                  </c:forEach>
                  <c:if test="${empty recentOrders}"><tr class="empty-row"><td colspan="5">Chưa có đơn hàng nào.</td></tr></c:if>
                </tbody>
              </table>
            </div>
          </section>
        </div>

        <%-- Cột phải --%>
        <div class="ac-stack" data-stagger>

          <section class="admin-card" aria-labelledby="status-title">
            <h2 class="admin-card__title" id="status-title">Đơn hàng theo trạng thái</h2>
            <div class="stack stack--sm">
              <c:forEach items="${stats.ordersByStatus}" var="e">
                <c:set var="spct" value="${stats.totalOrders > 0 ? e.value * 100 / stats.totalOrders : 0}"/>
                <fmt:formatNumber var="spctText" value="${spct}" maxFractionDigits="0" groupingUsed="false"/>
                <div class="progress-row">
                  <div class="row row--between t-small"><span><c:out value="${e.key}"/></span><span class="bold">${e.value}</span></div>
                  <div class="progress-row__bar"><span style="--v:${spctText}"></span></div>
                </div>
              </c:forEach>
              <c:if test="${empty stats.ordersByStatus}"><p class="ac-note">Chưa có đơn hàng.</p></c:if>
            </div>
          </section>

          <section class="admin-card reveal" aria-labelledby="stock-title">
            <h2 class="admin-card__title" id="stock-title"><span>Tồn kho thấp</span><a class="t-small link-underline" href="${ctx}/admin/products">Quản lý</a></h2>
            <ul class="low-stock">
              <c:forEach items="${lowStockProducts}" var="p">
                <li class="low-stock__item">
                  <div class="thumb"><t:img src="${p.imageThumb}" alt="${p.name}"/></div>
                  <div class="low-stock__info">
                    <a class="link-underline" href="${ctx}/admin/products/form?id=${p.id}"><c:out value="${p.name}"/></a>
                    <span class="t-small t-muted"><c:out value="${p.categoryName}"/></span>
                  </div>
                  <c:choose>
                    <c:when test="${p.stockTotal == 0}"><span class="badge badge--danger">Hết hàng</span></c:when>
                    <c:otherwise><span class="badge badge--pending">Còn ${p.stockTotal}</span></c:otherwise>
                  </c:choose>
                </li>
              </c:forEach>
            </ul>
            <c:if test="${empty lowStockProducts}"><p class="ac-note">Chưa có sản phẩm nào.</p></c:if>
          </section>
        </div>
      </div>
    </div>
  </jsp:body>
</t:admin-layout>
