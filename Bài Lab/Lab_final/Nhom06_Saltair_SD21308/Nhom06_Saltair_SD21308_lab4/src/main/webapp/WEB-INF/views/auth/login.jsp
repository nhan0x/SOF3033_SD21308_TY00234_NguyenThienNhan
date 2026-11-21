<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/_header.jspf" %>
<h2>Đăng nhập</h2>
<c:if test="${not empty error}"><div class="msg err"><c:out value="${error}"/></div></c:if>
<form method="post" action="${pageContext.request.contextPath}/auth/login" class="form">
  <input type="hidden" name="redirect" value="<c:out value='${redirect}'/>">
  <label>Email</label>
  <input type="email" name="email" value="<c:out value='${email}'/>" required>
  <label>Mật khẩu</label>
  <input type="password" name="password" required>
  <button type="submit">Đăng nhập</button>
</form>
<%@ include file="/WEB-INF/views/_footer.jspf" %>
