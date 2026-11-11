<%@ tag description="Thẻ sản phẩm (ô ảnh xám, hãng, tên, giá, sao). Rê chuột: ảnh phóng nhẹ, thanh size còn hàng trượt lên ở đáy ảnh." pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="product" required="true" type="com.fashionweb.model.Product" %>
<%@ attribute name="eager" required="false" type="java.lang.Boolean" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<a class="product-card" href="${ctx}/product?slug=${fn:escapeXml(product.slug)}">
  <div class="product-card__media">
    <c:if test="${product.stockTotal == 0}"><span class="product-card__badge product-card__badge--sold">Hết hàng</span></c:if>
    <t:img src="${product.imageThumb}" alt="${product.name}" eager="${eager}"/>
    <%-- Nút size nổi như a-dam: mỗi size còn hàng là một nút trắng; bấm thì sang trang chi tiết với size đó chọn sẵn
         (app.js bắt sự kiện data-size-href vì thẻ đã là liên kết, không lồng thẻ a). Bàn phím dùng liên kết của cả thẻ. --%>
    <c:if test="${not empty product.sizes}">
      <span class="product-card__sizes" aria-hidden="true">
        <c:forEach items="${product.sizes}" var="sz">
          <c:url var="sizeHref" value="/product"><c:param name="slug" value="${product.slug}"/><c:param name="size" value="${sz.name}"/></c:url>
          <span class="product-card__size" data-size-href="${fn:escapeXml(sizeHref)}"><c:out value="${sz.name}"/></span>
        </c:forEach>
      </span>
    </c:if>
  </div>
  <div class="product-card__body">
    <span class="product-card__brand">Saltair</span>
    <span class="product-card__name"><c:out value="${product.name}"/></span>
    <span class="product-card__price"><t:price value="${product.price}"/></span>
    <c:if test="${product.reviewCount > 0}">
      <span class="product-card__rating"><t:stars rating="${product.avgRating}"/><span class="rating-count">(<fmt:formatNumber value="${product.avgRating}" minFractionDigits="1" maxFractionDigits="1"/>)</span></span>
    </c:if>
  </div>
</a>
