<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/_taglibs.jspf" %>
<c:set var="pageTitle" value="Quản lý hóa đơn"/>
<%@ include file="/WEB-INF/views/_header.jspf" %>
<h2>Quản lý hóa đơn</h2>
<div class="table-wrap">
  <table class="responsive">
    <thead><tr><th>Mã</th><th>Nhân viên</th><th>Ngày tạo</th><th>Trạng thái</th><th class="num">Tổng tiền</th><th>Thao tác</th></tr></thead>
    <tbody>
      <c:forEach var="b" items="${pg.items}">
        <tr>
          <td data-label="Mã">#${b.id}</td>
          <td data-label="Nhân viên"><c:out value="${b.userFullname}"/></td>
          <td data-label="Ngày tạo"><fmt:formatDate value="${b.createdAt}" pattern="dd/MM/yyyy HH:mm"/></td>
          <td data-label="Trạng thái">
            <span class="tag ${b.status == 1 ? 'ok' : b.status == 2 ? 'err' : ''}"><c:out value="${b.statusText}"/></span>
          </td>
          <td data-label="Tổng tiền" class="num"><fmt:formatNumber value="${b.total}" pattern="#,##0"/> đ</td>
          <td data-label="Thao tác"><a href="${ctx}/manager/bills/detail?id=${b.id}">Chi tiết</a></td>
        </tr>
      </c:forEach>
    </tbody>
  </table>
  <c:if test="${empty pg.items}"><p class="empty">Chưa có hóa đơn nào.</p></c:if>
</div>
<%@ include file="/WEB-INF/views/_pager.jspf" %>
<%@ include file="/WEB-INF/views/_footer.jspf" %>
