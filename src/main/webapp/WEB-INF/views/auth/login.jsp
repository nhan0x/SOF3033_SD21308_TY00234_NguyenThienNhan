<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/_taglibs.jspf" %>
<c:set var="pageTitle" value="Đăng nhập"/>
<%@ include file="/WEB-INF/views/_header.jspf" %>
<div class="auth-wrap">
<h2>Đăng nhập</h2>
<p class="sub">Dành cho quản lý và nhân viên PolyCoffee</p>
<c:if test="${not empty error}"><div class="msg err" role="alert"><c:out value="${error}"/></div></c:if>
<form method="post" action="${ctx}/auth/login" class="form">
  <input type="hidden" name="redirect" value="<c:out value='${redirect}'/>">
  <label for="email">Email</label>
  <input type="email" id="email" name="email" value="<c:out value='${email}'/>" autocomplete="username" required>
  <label for="password">Mật khẩu</label>
  <input type="password" id="password" name="password" autocomplete="current-password" required>
  <button type="submit" class="primary">Đăng nhập</button>
</form>
</div>
<%@ include file="/WEB-INF/views/_footer.jspf" %>
