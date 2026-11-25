<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/_taglibs.jspf" %>
<c:set var="pageTitle" value="Thống kê doanh thu"/>
<%@ include file="/WEB-INF/views/_header.jspf" %>
<h2>Doanh thu theo ngày</h2>
<c:set var="filterAction" value="${ctx}/manager/stats/revenue"/>
<%@ include file="/WEB-INF/views/_dateFilter.jspf" %>

<c:if test="${empty error}">
  <div class="summary">
    <div>Tổng doanh thu<strong><fmt:formatNumber value="${grandTotal}" pattern="#,##0"/> đ</strong></div>
    <div>Số ngày có doanh thu<strong>${fn:length(points)}</strong></div>
  </div>

  <c:if test="${empty points}"><p class="empty">Không có doanh thu trong khoảng này.</p></c:if>
  <c:if test="${not empty points}">
    <div class="chart-box">
      <canvas id="revenueChart" aria-label="Biểu đồ doanh thu theo ngày" role="img"></canvas>
      <p id="chartFallback" class="empty" hidden>Không tải được biểu đồ (cần kết nối Internet). Xem bảng số liệu bên dưới.</p>
    </div>
    <div class="table-wrap">
      <table class="responsive">
        <thead><tr><th>Ngày</th><th class="num">Doanh thu</th></tr></thead>
        <tbody>
          <c:forEach var="p" items="${points}">
            <tr>
              <td data-label="Ngày"><c:out value="${p.day}"/></td>
              <td data-label="Doanh thu" class="num"><fmt:formatNumber value="${p.revenue}" pattern="#,##0"/> đ</td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
    </div>
    <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js"></script>
    <script>
      (function () {
        if (typeof Chart === 'undefined') {
          document.getElementById('revenueChart').hidden = true;
          document.getElementById('chartFallback').hidden = false;
          return;
        }
        var labels = [<c:forEach var="p" items="${points}" varStatus="s">'${p.day}'<c:if test="${!s.last}">,</c:if></c:forEach>];
        var values = [<c:forEach var="p" items="${points}" varStatus="s"><fmt:formatNumber value="${p.revenue}" pattern="0" groupingUsed="false"/><c:if test="${!s.last}">,</c:if></c:forEach>];
        new Chart(document.getElementById('revenueChart'), {
          type: 'bar',
          data: { labels: labels, datasets: [{ label: 'Doanh thu (đ)', data: values, backgroundColor: '#6f4e37' }] },
          options: { responsive: true, scales: { y: { beginAtZero: true } } }
        });
      })();
    </script>
  </c:if>
</c:if>
<%@ include file="/WEB-INF/views/_footer.jspf" %>
