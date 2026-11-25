<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/_taglibs.jspf" %>
<c:set var="pageTitle" value="Top đồ uống bán chạy"/>
<%@ include file="/WEB-INF/views/_header.jspf" %>
<h2>Top 5 đồ uống bán chạy</h2>
<c:set var="filterAction" value="${ctx}/manager/stats/top-drinks"/>
<%@ include file="/WEB-INF/views/_dateFilter.jspf" %>

<c:if test="${empty error}">
  <c:if test="${empty top}"><p class="empty">Không có dữ liệu bán hàng trong khoảng này.</p></c:if>
  <c:if test="${not empty top}">
    <div class="table-wrap">
      <table class="responsive">
        <thead><tr><th>Hạng</th><th>Đồ uống</th><th class="num">Số lượng</th><th class="num">Doanh thu</th></tr></thead>
        <tbody>
          <c:forEach var="t" items="${top}" varStatus="s">
            <tr>
              <td data-label="Hạng">${s.count}</td>
              <td data-label="Đồ uống"><c:out value="${t.name}"/></td>
              <td data-label="Số lượng" class="num">${t.quantity}</td>
              <td data-label="Doanh thu" class="num"><fmt:formatNumber value="${t.revenue}" pattern="#,##0"/> đ</td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
    </div>
  </c:if>
</c:if>
<%@ include file="/WEB-INF/views/_footer.jspf" %>
