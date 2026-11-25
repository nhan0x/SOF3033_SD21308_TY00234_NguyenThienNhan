<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/_taglibs.jspf" %>
<c:set var="pageTitle" value="Quản lý loại đồ uống"/>
<%@ include file="/WEB-INF/views/_header.jspf" %>
<h2>Quản lý loại đồ uống</h2>
<p><a class="btn primary" href="${ctx}/manager/categories/new">+ Thêm loại</a></p>
<div class="table-wrap">
  <table class="responsive">
    <thead><tr><th>ID</th><th>Tên loại</th><th>Thao tác</th></tr></thead>
    <tbody>
      <c:forEach var="c" items="${categories}">
        <tr>
          <td data-label="ID">${c.id}</td>
          <td data-label="Tên loại"><c:out value="${c.name}"/></td>
          <td data-label="Thao tác">
            <div class="actions">
              <a href="${ctx}/manager/categories/edit?id=${c.id}">Sửa</a>
              <form method="post" action="${ctx}/manager/categories/delete"
                    onsubmit="return confirm('Xóa loại đồ uống này?')">
                <input type="hidden" name="id" value="${c.id}">
                <button type="submit" class="link danger">Xóa</button>
              </form>
            </div>
          </td>
        </tr>
      </c:forEach>
    </tbody>
  </table>
  <c:if test="${empty categories}"><p class="empty">Chưa có loại đồ uống nào.</p></c:if>
</div>
<%@ include file="/WEB-INF/views/_footer.jspf" %>
