<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/_taglibs.jspf" %>
<c:set var="pageTitle" value="Hóa đơn #${bill.id}"/>
<%@ include file="/WEB-INF/views/_header.jspf" %>
<h2>Hóa đơn #${bill.id}</h2>
<div class="summary">
  <div>Nhân viên<strong><c:out value="${bill.userFullname}"/></strong></div>
  <div>Ngày tạo<strong><fmt:formatDate value="${bill.createdAt}" pattern="dd/MM/yyyy HH:mm"/></strong></div>
  <c:if test="${not empty bill.checkoutAt}">
    <div>Thanh toán<strong><fmt:formatDate value="${bill.checkoutAt}" pattern="dd/MM/yyyy HH:mm"/></strong></div>
  </c:if>
  <div>Trạng thái<strong><c:out value="${bill.statusText}"/></strong></div>
</div>

<div class="table-wrap">
  <table class="responsive">
    <thead>
      <tr><th>Đồ uống</th><th class="num">Số lượng</th><th class="num">Đơn giá</th><th class="num">Giảm giá</th><th class="num">Thành tiền</th></tr>
    </thead>
    <tbody>
      <c:forEach var="l" items="${lines}">
        <tr>
          <td data-label="Đồ uống"><c:out value="${l.drinkName}"/></td>
          <td data-label="Số lượng" class="num">${l.quantity}</td>
          <td data-label="Đơn giá" class="num"><fmt:formatNumber value="${l.unitPrice}" pattern="#,##0"/></td>
          <td data-label="Giảm giá" class="num"><fmt:formatNumber value="${l.discount * 100}" pattern="0.##"/>%</td>
          <td data-label="Thành tiền" class="num"><fmt:formatNumber value="${l.amount}" pattern="#,##0"/></td>
        </tr>
      </c:forEach>
    </tbody>
    <tfoot>
      <tr>
        <td colspan="4" class="num">Tổng tiền</td>
        <td class="num"><fmt:formatNumber value="${bill.total}" pattern="#,##0"/> đ</td>
      </tr>
    </tfoot>
  </table>
</div>

<div class="actions" style="margin-top:16px">
  <a class="btn" href="${ctx}/manager/bills">Quay lại</a>
  <c:if test="${bill.status != 2}">
    <form method="post" action="${ctx}/manager/bills/cancel"
          onsubmit="return confirm('Hủy hóa đơn này?')">
      <input type="hidden" name="id" value="${bill.id}">
      <button type="submit" class="danger">Hủy hóa đơn</button>
    </form>
  </c:if>
</div>
<%@ include file="/WEB-INF/views/_footer.jspf" %>
