<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ include file="/WEB-INF/views/_taglibs.jspf" %>
<c:set var="pageTitle" value="Không có quyền truy cập"/>
<%@ include file="/WEB-INF/views/_header.jspf" %>
<div class="error-page">
  <p class="code">403</p>
  <h2>Không có quyền truy cập</h2>
  <p>Bạn không có quyền truy cập chức năng này.</p>
  <p><a class="btn primary" href="${ctx}/home">Về trang chủ</a></p>
</div>
<%@ include file="/WEB-INF/views/_footer.jspf" %>
