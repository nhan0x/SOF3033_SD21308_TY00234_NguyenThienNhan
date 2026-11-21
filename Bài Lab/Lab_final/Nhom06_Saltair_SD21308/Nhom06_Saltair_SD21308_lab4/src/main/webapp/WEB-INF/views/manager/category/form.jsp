<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/_header.jspf" %>
<h2>${category.id == 0 ? 'Thêm' : 'Cập nhật'} loại đồ uống</h2>
<c:if test="${not empty error}"><div class="msg err"><c:out value="${error}"/></div></c:if>
<form method="post" action="${pageContext.request.contextPath}/manager/categories/save" class="form">
  <input type="hidden" name="id" value="${category.id}">
  <label>Tên loại</label>
  <input type="text" name="name" maxlength="100" value="<c:out value='${category.name}'/>" required>
  <button type="submit">Lưu</button>
  <a href="${pageContext.request.contextPath}/manager/categories">Quay lại</a>
</form>
<%@ include file="/WEB-INF/views/_footer.jspf" %>
