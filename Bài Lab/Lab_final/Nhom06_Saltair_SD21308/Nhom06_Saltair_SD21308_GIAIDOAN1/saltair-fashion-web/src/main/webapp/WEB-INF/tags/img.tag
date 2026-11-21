<%@ tag description="Ảnh có khung xám khi chưa có ảnh (ảnh rỗng). src là đường dẫn tương đối so với context path, ví dụ assets/images/products/x.jpg" pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="src" required="false" description="Đường dẫn ảnh. Để trống thì hiện khung xám" %>
<%@ attribute name="alt" required="false" %>
<%@ attribute name="cls" required="false" %>
<%@ attribute name="dark" required="false" type="java.lang.Boolean" description="Khung xám kiểu nền tối (dùng trên hero)" %>
<%@ attribute name="eager" required="false" type="java.lang.Boolean" description="true = không lazy-load (ảnh đầu trang)" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<c:choose>
  <c:when test="${empty src}">
    <div class="ph${dark ? ' ph--dark' : ''}" role="img" aria-label="${fn:escapeXml(empty alt ? 'Ảnh chưa có' : alt)}"><t:icon name="image"/></div>
  </c:when>
  <c:otherwise>
    <c:set var="rel" value="${fn:startsWith(src, '/') ? fn:substring(src, 1, fn:length(src)) : src}"/>
    <c:set var="url" value="${fn:startsWith(src, 'http') ? src : ''.concat(ctx).concat('/').concat(rel)}"/>
    <img src="${fn:escapeXml(url)}" alt="${fn:escapeXml(alt)}"${empty cls ? '' : ' class="'.concat(cls).concat('"')}${eager ? '' : ' loading="lazy"'} decoding="async">
  </c:otherwise>
</c:choose>
