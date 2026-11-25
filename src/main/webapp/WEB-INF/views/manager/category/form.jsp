<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/_taglibs.jspf" %>
<c:set var="pageTitle" value="${category.id == 0 ? 'Thêm loại đồ uống' : 'Cập nhật loại đồ uống'}"/>
<%@ include file="/WEB-INF/views/_header.jspf" %>
<h2><c:out value="${pageTitle}"/></h2>
<c:if test="${not empty error}"><div class="msg err" role="alert"><c:out value="${error}"/></div></c:if>
<form method="post" action="${ctx}/manager/categories/save" class="form">
  <input type="hidden" name="id" value="${category.id}">
  <label for="name">Tên loại</label>
  <input type="text" id="name" name="name" maxlength="100" value="<c:out value='${category.name}'/>" required>
  <div class="actions">
    <button type="submit" class="primary">Lưu</button>
    <a href="${ctx}/manager/categories">Quay lại</a>
  </div>
</form>
<%@ include file="/WEB-INF/views/_footer.jspf" %>
