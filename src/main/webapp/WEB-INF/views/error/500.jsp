<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ include file="/WEB-INF/views/_taglibs.jspf" %>
<c:set var="pageTitle" value="Đã xảy ra lỗi"/>
<%@ include file="/WEB-INF/views/_header.jspf" %>
<div class="error-page">
  <p class="code">500</p>
  <h2>Đã xảy ra lỗi</h2>
  <p>Hệ thống đang gặp sự cố. Vui lòng thử lại sau.</p>
  <p><a class="btn primary" href="${ctx}/home">Về trang chủ</a></p>
</div>
<%@ include file="/WEB-INF/views/_footer.jspf" %>
