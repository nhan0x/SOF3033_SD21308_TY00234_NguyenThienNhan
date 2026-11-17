<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  ADMIN - NGƯỜI DÙNG
  Route:   GET /admin/users?q=&role=&page=
  Servlet: com.fashionweb.controller.admin.system.AdminUserServlet (active menu: users)
  Request attributes:
    userPage       : Page<User> - người dùng của trang hiện tại; mỗi User cần orderCount (số đơn của khách)
    q              : String     - từ khóa (email hoặc họ tên), "" nếu không lọc
    role           : String     - CUSTOMER | ADMIN, "" = tất cả
    currentAdminId : Integer    - id Admin đang đăng nhập; dòng này bị vô hiệu hóa các nút thao tác
    errors         : Map<String,String> - (tùy chọn) lỗi sau khi POST thất bại (khóa role)
  Forms (POST ${ctx}/admin/users):
    action=toggleActive  fields: id                 - khóa hoặc mở khóa tài khoản
    action=changeRole    fields: id, role (CUSTOMER | ADMIN)
--%>
<t:admin-layout title="Người dùng" active="users">
  <jsp:attribute name="head"><link rel="stylesheet" href="${ctx}/assets/css/pages/admin-ops.css"></jsp:attribute>
  <jsp:attribute name="scripts"><script src="${ctx}/assets/js/pages/admin-ops.js" defer></script></jsp:attribute>
  <jsp:body>
    <%@ include file="/WEB-INF/views/common/flash.jspf" %>

    <c:url var="qUrl" value="/admin/users"><c:param name="q" value="${q}"/></c:url>
    <c:set var="qs" value="${fn:substringAfter(qUrl, '?q=')}"/>
    <c:set var="pageBase" value="/admin/users?q=${qs}&role=${role}"/>

    <div class="admin-card admin-card--flush ops-enter">
      <form class="admin-toolbar" method="get" action="${ctx}/admin/users" role="search" aria-label="Tìm và lọc người dùng">
        <div class="admin-toolbar__left">
          <label class="search-field">
            <span class="sr-only">Tìm theo email hoặc họ tên</span>
            <t:icon name="search"/>
            <input class="input" type="search" name="q" value="${fn:escapeXml(q)}" placeholder="Email hoặc họ tên" maxlength="100" autocomplete="off">
          </label>
          <label class="sr-only" for="filter-role">Vai trò</label>
          <select class="select" id="filter-role" name="role" data-autosubmit>
            <option value="">Mọi vai trò</option>
            <option value="CUSTOMER"${role == 'CUSTOMER' ? ' selected' : ''}>Khách hàng</option>
            <option value="ADMIN"${role == 'ADMIN' ? ' selected' : ''}>Quản trị viên</option>
          </select>
          <button class="btn btn--primary btn--sm" type="submit">Tìm kiếm</button>
          <c:if test="${not empty q or not empty role}">
            <a class="btn btn--ghost btn--sm" href="${ctx}/admin/users">Xóa bộ lọc</a>
          </c:if>
        </div>
        <p class="t-small t-muted">${userPage.totalItems} tài khoản</p>
      </form>

      <div class="admin-table-wrap">
        <table class="table responsive ops-table">
          <caption class="sr-only">Danh sách người dùng</caption>
          <thead>
            <tr>
              <th scope="col">Người dùng</th>
              <th scope="col">Email</th>
              <th scope="col">Điện thoại</th>
              <th scope="col">Vai trò</th>
              <th scope="col">Trạng thái</th>
              <th scope="col">Ngày tạo</th>
              <th scope="col" class="num">Số đơn</th>
              <th scope="col"><span class="sr-only">Thao tác</span></th>
            </tr>
          </thead>
          <tbody>
            <c:forEach items="${userPage.items}" var="u">
              <c:set var="isSelf" value="${u.id == currentAdminId}"/>
              <tr>
                <td class="cell-lead">
                  <div class="cell-user">
                    <span class="admin-avatar${u.role == 'ADMIN' ? '' : ' admin-avatar--muted'}" aria-hidden="true">${fn:toUpperCase(fn:substring(u.fullName, 0, 1))}</span>
                    <span class="bold"><c:out value="${u.fullName}"/><c:if test="${isSelf}"> <span class="badge badge--muted">Bạn</span></c:if></span>
                  </div>
                </td>
                <td data-label="Email"><c:out value="${u.email}"/></td>
                <td data-label="Điện thoại" class="cell-nowrap"><c:out value="${u.phone}"/></td>
                <td data-label="Vai trò"><span class="badge ${u.role == 'ADMIN' ? 'badge--shipping' : 'badge--muted'}">${u.role.label}</span></td>
                <td data-label="Trạng thái">
                  <span class="status-line"><span class="status-dot ${u.active ? 'status-dot--on' : 'status-dot--off'}"></span>${u.active ? 'Hoạt động' : 'Bị khóa'}</span>
                </td>
                <td data-label="Ngày tạo" class="cell-nowrap"><fmt:formatDate value="${u.createdAt}" pattern="dd/MM/yyyy"/></td>
                <td data-label="Số đơn" class="num">${u.orderCount}</td>
                <td>
                  <div class="cell-actions">
                    <c:choose>
                      <c:when test="${isSelf}">
                        <span title="Không thể thao tác trên tài khoản đang đăng nhập">
                          <button class="btn btn--ghost btn--xs" type="button" disabled>${u.active ? 'Khóa' : 'Mở khóa'}</button>
                        </span>
                        <span title="Không thể thao tác trên tài khoản đang đăng nhập">
                          <button class="btn btn--ghost btn--xs" type="button" disabled>Đổi quyền</button>
                        </span>
                      </c:when>
                      <c:otherwise>
                        <form method="post" action="${ctx}/admin/users" data-confirm="${u.active ? 'Khóa' : 'Mở khóa'} tài khoản ${fn:escapeXml(u.email)}?">
                          <input type="hidden" name="action" value="toggleActive">
                          <input type="hidden" name="id" value="${u.id}">
                          <button class="btn ${u.active ? 'btn--danger' : 'btn--ghost'} btn--xs" type="submit">${u.active ? 'Khóa' : 'Mở khóa'}</button>
                        </form>
                        <button class="btn btn--ghost btn--xs js-fill" type="button"
                                data-dialog-open="role-dialog" data-dialog-title="Đổi quyền"
                                data-field-id="${u.id}" data-field-role="${u.role}"
                                data-field-full-name="${fn:escapeXml(u.fullName)}" data-field-email="${fn:escapeXml(u.email)}">Đổi quyền</button>
                      </c:otherwise>
                    </c:choose>
                  </div>
                </td>
              </tr>
            </c:forEach>
            <c:if test="${empty userPage.items}">
              <tr class="empty-row"><td colspan="8">Không tìm thấy người dùng nào phù hợp.</td></tr>
            </c:if>
          </tbody>
        </table>
      </div>

      <c:set var="rangeFrom" value="${(userPage.page - 1) * userPage.pageSize + 1}"/>
      <c:set var="rangeTo" value="${rangeFrom + fn:length(userPage.items) - 1}"/>
      <div class="admin-table-foot">
        <span>
          <c:choose>
            <c:when test="${userPage.totalItems > 0}">Hiển thị ${rangeFrom} đến ${rangeTo} trong ${userPage.totalItems} tài khoản</c:when>
            <c:otherwise>Chưa có tài khoản nào</c:otherwise>
          </c:choose>
        </span>
        <t:pagination page="${userPage.page}" totalPages="${userPage.totalPages}" baseUrl="${pageBase}"/>
      </div>
    </div>

    <%-- Hộp thoại đổi quyền --%>
    <dialog class="dialog" id="role-dialog" aria-labelledby="role-dialog-title" data-auto-open="${not empty errors.role}">
      <form method="post" action="${ctx}/admin/users">
        <input type="hidden" name="action" value="changeRole">
        <input type="hidden" name="id" value="${fn:escapeXml(param.id)}">
        <div class="dialog__head">
          <h2 class="h-2" id="role-dialog-title" data-dialog-title>Đổi quyền</h2>
          <button class="icon-btn" type="button" data-dialog-close aria-label="Đóng"><t:icon name="x" size="md"/></button>
        </div>
        <div class="dialog__body">
          <p class="mb-24">Tài khoản <strong data-bind="fullName"></strong> <span class="t-muted" data-bind="email"></span></p>
          <div class="field">
            <label class="field__label" for="role-select">Vai trò</label>
            <select class="select" id="role-select" name="role" required>
              <option value="CUSTOMER">Khách hàng</option>
              <option value="ADMIN">Quản trị viên</option>
            </select>
            <span class="field__hint">Quản trị viên có thể vào trang quản trị và thay đổi dữ liệu của cửa hàng.</span>
            <c:if test="${not empty errors.role}"><span class="field__error"><c:out value="${errors.role}"/></span></c:if>
          </div>
        </div>
        <div class="dialog__foot">
          <button class="btn btn--ghost btn--sm" type="button" data-dialog-close>Hủy</button>
          <button class="btn btn--primary btn--sm" type="submit">Lưu thay đổi</button>
        </div>
      </form>
    </dialog>
  </jsp:body>
</t:admin-layout>
