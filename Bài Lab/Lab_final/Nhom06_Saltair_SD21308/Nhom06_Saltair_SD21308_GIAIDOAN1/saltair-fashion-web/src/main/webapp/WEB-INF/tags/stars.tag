<%@ tag description="Hàng 5 sao đánh giá (hỗ trợ nửa sao)" pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="rating" required="true" type="java.lang.Double" %>
<%@ attribute name="count" required="false" type="java.lang.Integer" description="Số lượt đánh giá, hiện sau số điểm" %>
<%@ attribute name="showScore" required="false" type="java.lang.Boolean" description="true = hiện (4,5) bên phải" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<span class="stars" role="img" aria-label="${rating} trên 5 sao"><c:forEach begin="1" end="5" var="i"><c:choose>
  <c:when test="${rating >= i}"><svg class="icon is-full" aria-hidden="true"><use href="#i-star"></use></svg></c:when>
  <c:when test="${rating >= i - 0.5}"><span class="star-half"><svg class="icon is-empty" aria-hidden="true"><use href="#i-star"></use></svg><svg class="icon is-full" aria-hidden="true"><use href="#i-star"></use></svg></span></c:when>
  <c:otherwise><svg class="icon is-empty" aria-hidden="true"><use href="#i-star"></use></svg></c:otherwise>
</c:choose></c:forEach></span><c:if test="${showScore}"><span class="rating-count"><fmt:setLocale value="vi_VN"/>(<fmt:formatNumber value="${rating}" minFractionDigits="1" maxFractionDigits="1"/><c:if test="${not empty count}"> · ${count} đánh giá</c:if>)</span></c:if>
