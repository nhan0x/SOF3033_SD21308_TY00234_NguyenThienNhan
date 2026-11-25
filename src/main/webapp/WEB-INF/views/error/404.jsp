<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ include file="/WEB-INF/views/_taglibs.jspf" %>
<c:set var="pageTitle" value="Không tìm thấy trang"/>
<%@ include file="/WEB-INF/views/_header.jspf" %>
<div class="error-page">
  <p class="code">404</p>
  <h2>Không tìm thấy trang</h2>
  <p>Trang hoặc dữ liệu bạn yêu cầu không tồn tại.</p>
  <p><a class="btn primary" href="${ctx}/home">Về trang chủ</a></p>
</div>
<%@ include file="/WEB-INF/views/_footer.jspf" %>
