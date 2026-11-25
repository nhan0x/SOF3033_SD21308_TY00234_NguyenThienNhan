<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/_taglibs.jspf" %>
<c:set var="pageTitle" value="Quản lý nhân viên"/>
<%@ include file="/WEB-INF/views/_header.jspf" %>
<h2>Quản lý nhân viên</h2>
<p><a class="btn primary" href="${ctx}/manager/staff/new">+ Thêm nhân viên</a></p>
<div class="table-wrap">
  <table class="responsive">
    <thead><tr><th>ID</th><th>Họ tên</th><th>Email</th><th>Vai trò</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
    <tbody>
      <c:forEach var="u" items="${users}">
        <tr>
          <td data-label="ID">${u.id}</td>
          <td data-label="Họ tên"><c:out value="${u.fullname}"/></td>
          <td data-label="Email"><c:out value="${u.email}"/></td>
          <td data-label="Vai trò"><c:out value="${u.manager ? 'Quản lý' : 'Nhân viên'}"/></td>
          <td data-label="Trạng thái">
            <c:choose>
              <c:when test="${u.enabled}"><span class="tag ok">Hoạt động</span></c:when>
              <c:otherwise><span class="tag err">Bị khóa</span></c:otherwise>
            </c:choose>
          </td>
          <td data-label="Thao tác">
            <div class="actions">
              <a href="${ctx}/manager/staff/edit?id=${u.id}">Sửa</a>
              <form method="post" action="${ctx}/manager/staff/reset-password"
                    onsubmit="return confirm('Cấp lại mật khẩu và gửi email cho nhân viên này?')">
                <input type="hidden" name="id" value="${u.id}">
                <button type="submit" class="link">Cấp lại mật khẩu</button>
              </form>
              <form method="post" action="${ctx}/manager/staff/delete"
                    onsubmit="return confirm('Xóa nhân viên này?')">
                <input type="hidden" name="id" value="${u.id}">
                <button type="submit" class="link danger">Xóa</button>
              </form>
            </div>
          </td>
        </tr>
      </c:forEach>
    </tbody>
  </table>
</div>
<%@ include file="/WEB-INF/views/_footer.jspf" %>
