<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/_taglibs.jspf" %>
<c:set var="pageTitle" value="${staff.id == 0 ? 'Thêm nhân viên' : 'Cập nhật nhân viên'}"/>
<%@ include file="/WEB-INF/views/_header.jspf" %>
<h2><c:out value="${pageTitle}"/></h2>
<c:if test="${not empty error}"><div class="msg err" role="alert"><c:out value="${error}"/></div></c:if>
<form method="post" action="${ctx}/manager/staff/save" class="form">
  <input type="hidden" name="id" value="${staff.id}">

  <label for="fullname">Họ tên</label>
  <input type="text" id="fullname" name="fullname" maxlength="100" value="<c:out value='${staff.fullname}'/>" required>

  <label for="email">Email</label>
  <input type="email" id="email" name="email" maxlength="100" value="<c:out value='${staff.email}'/>" required>

  <c:if test="${staff.id == 0}">
    <label for="password">Mật khẩu</label>
    <input type="password" id="password" name="password" minlength="6" autocomplete="new-password" required>
    <span class="hint">Tối thiểu 6 ký tự.</span>
  </c:if>
  <c:if test="${staff.id != 0}">
    <span class="hint">Để đổi mật khẩu, dùng chức năng "Cấp lại mật khẩu" ở danh sách.</span>
  </c:if>

  <label class="check"><input type="checkbox" name="manager" value="true" ${staff.manager ? 'checked' : ''}> Quản lý</label>
  <label class="check"><input type="checkbox" name="enabled" value="true" ${staff.enabled ? 'checked' : ''}> Đang hoạt động</label>

  <div class="actions">
    <button type="submit" class="primary">Lưu</button>
    <a href="${ctx}/manager/staff">Quay lại</a>
  </div>
</form>
<%@ include file="/WEB-INF/views/_footer.jspf" %>
