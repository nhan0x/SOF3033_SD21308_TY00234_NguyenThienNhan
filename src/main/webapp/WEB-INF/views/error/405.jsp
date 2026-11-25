<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ include file="/WEB-INF/views/_taglibs.jspf" %>
<c:set var="pageTitle" value="Phương thức không được hỗ trợ"/>
<%@ include file="/WEB-INF/views/_header.jspf" %>
<div class="error-page">
  <p class="code">405</p>
  <h2>Phương thức không được hỗ trợ</h2>
  <p>Thao tác này không được phép thực hiện bằng đường dẫn trực tiếp.</p>
  <p><a class="btn primary" href="${ctx}/home">Về trang chủ</a></p>
</div>
<%@ include file="/WEB-INF/views/_footer.jspf" %>
