<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/_header.jspf" %>
<h2>Quản lý loại đồ uống</h2>
<p><a class="btn" href="${pageContext.request.contextPath}/manager/categories/new">+ Thêm loại</a></p>
<table>
  <tr><th>ID</th><th>Tên loại</th><th>Thao tác</th></tr>
  <c:forEach var="c" items="${categories}">
    <tr>
      <td>${c.id}</td>
      <td><c:out value="${c.name}"/></td>
      <td>
        <a href="${pageContext.request.contextPath}/manager/categories/edit?id=${c.id}">Sửa</a>
        <form method="post" action="${pageContext.request.contextPath}/manager/categories/delete" style="display:inline"
              onsubmit="return confirm('Xóa loại đồ uống này?')">
          <input type="hidden" name="id" value="${c.id}">
          <button type="submit" class="link">Xóa</button>
        </form>
      </td>
    </tr>
  </c:forEach>
</table>
<%@ include file="/WEB-INF/views/_footer.jspf" %>
