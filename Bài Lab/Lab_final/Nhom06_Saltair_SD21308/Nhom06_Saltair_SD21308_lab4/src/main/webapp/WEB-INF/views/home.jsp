<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/_header.jspf" %>
<h2>Thực đơn</h2>
<div class="grid">
  <c:forEach var="d" items="${drinks}">
    <div class="card">
      <img src="${pageContext.request.contextPath}/images/${empty d.image ? 'no-image.png' : d.image}" alt="<c:out value='${d.name}'/>">
      <h3><c:out value="${d.name}"/></h3>
      <p class="price"><fmt:formatNumber value="${d.unitPrice * (1 - d.discount)}" pattern="#,##0"/> đ</p>
    </div>
  </c:forEach>
</div>
<%@ include file="/WEB-INF/views/_footer.jspf" %>
