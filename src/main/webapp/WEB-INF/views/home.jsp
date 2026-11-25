<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/_taglibs.jspf" %>
<c:set var="pageTitle" value="Thực đơn"/>
<%@ include file="/WEB-INF/views/_header.jspf" %>
<div class="hero">
  <h2>Thực đơn PolyCoffee</h2>
  <p>Cà phê, trà và đồ uống pha chế mỗi ngày. Chọn món bạn thích.</p>
</div>
<c:if test="${empty menu}"><p class="empty">Hiện chưa có đồ uống nào đang bán.</p></c:if>
<c:forEach var="entry" items="${menu}">
  <section class="menu-group">
    <h3><c:out value="${entry.key.name}"/></h3>
    <div class="grid">
      <c:forEach var="d" items="${entry.value}">
        <c:set var="img" value="${empty d.image ? 'no-image.svg' : d.image}"/>
        <div class="card">
          <img src="<c:out value='${ctx}/images/${img}'/>" alt="<c:out value='${d.name}'/>"
               onerror="this.onerror=null;this.src='${ctx}/images/no-image.svg'">
          <h4><c:out value="${d.name}"/></h4>
          <p class="price">
            <c:if test="${d.discount > 0}">
              <span class="old-price"><fmt:formatNumber value="${d.unitPrice}" pattern="#,##0"/></span>
            </c:if>
            <fmt:formatNumber value="${d.finalPrice}" pattern="#,##0"/> đ
          </p>
          <c:if test="${d.discount > 0}">
            <span class="tag ok">Giảm <fmt:formatNumber value="${d.discount * 100}" pattern="0.##"/>%</span>
          </c:if>
        </div>
      </c:forEach>
    </div>
  </section>
</c:forEach>
<%@ include file="/WEB-INF/views/_footer.jspf" %>
