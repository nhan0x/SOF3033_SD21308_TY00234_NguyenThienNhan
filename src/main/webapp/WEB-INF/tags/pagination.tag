<%@ tag description="Phân trang. baseUrl là đường dẫn kèm bộ lọc hiện tại, KHÔNG có tham số page" pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="page" required="true" type="java.lang.Integer" %>
<%@ attribute name="totalPages" required="true" type="java.lang.Integer" %>
<%@ attribute name="baseUrl" required="true" description="Ví dụ /products?category=ao-khoac&sort=price_asc" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<c:if test="${totalPages > 1}">
  <c:set var="sep" value="${baseUrl.contains('?') ? '&' : '?'}"/>
  <nav class="pagination" aria-label="Phân trang">
    <c:choose>
      <c:when test="${page > 1}"><a href="${ctx}${baseUrl}${sep}page=${page - 1}" aria-label="Trang trước"><t:icon name="chevron-left"/></a></c:when>
      <c:otherwise><span class="is-disabled"><t:icon name="chevron-left"/></span></c:otherwise>
    </c:choose>
    <c:forEach begin="1" end="${totalPages}" var="p">
      <c:if test="${p == 1 or p == totalPages or (p >= page - 1 and p <= page + 1)}">
        <c:choose>
          <c:when test="${p == page}"><span class="is-current" aria-current="page">${p}</span></c:when>
          <c:otherwise><a href="${ctx}${baseUrl}${sep}page=${p}">${p}</a></c:otherwise>
        </c:choose>
      </c:if>
      <c:if test="${(p == 2 and page > 3) or (p == totalPages - 1 and page < totalPages - 2)}"><span class="is-disabled">…</span></c:if>
    </c:forEach>
    <c:choose>
      <c:when test="${page < totalPages}"><a href="${ctx}${baseUrl}${sep}page=${page + 1}" aria-label="Trang sau"><t:icon name="chevron-right"/></a></c:when>
      <c:otherwise><span class="is-disabled"><t:icon name="chevron-right"/></span></c:otherwise>
    </c:choose>
  </nav>
</c:if>
