<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  ADMIN - DANH SÁCH BÀI VIẾT (BLOG)
  Route:   GET /admin/blogs?q=&page=
  Servlet: com.fashionweb.controller.admin.system.AdminBlogServlet (active menu: blogs)
  Request attributes:
    blogPage : Page<Blog> - bài viết của trang hiện tại (ADMIN_PAGE_SIZE), cả bài đang ẩn, mới nhất trước; mỗi Blog cần authorName
    q        : String     - từ khóa tìm theo tiêu đề, "" nếu không lọc
  Forms (POST ${ctx}/admin/blogs):
    action=togglePublish  fields: id   - đảo trạng thái đăng/ẩn
    action=delete         fields: id   - xóa bài viết
  Thêm/sửa bài viết: liên kết sang GET /admin/blogs/form[?id=] (form lưu nằm ở blog-form.jsp).
--%>
<t:admin-layout title="Bài viết" active="blogs">
  <jsp:attribute name="head"><link rel="stylesheet" href="${ctx}/assets/css/pages/admin-ops.css"></jsp:attribute>
  <jsp:attribute name="scripts"><script src="${ctx}/assets/js/pages/admin-ops.js" defer></script></jsp:attribute>
  <jsp:attribute name="actions"><a class="btn btn--primary btn--sm" href="${ctx}/admin/blogs/form"><t:icon name="plus"/> Thêm bài viết</a></jsp:attribute>
  <jsp:body>
    <%@ include file="/WEB-INF/views/common/flash.jspf" %>

    <c:url var="qUrl" value="/admin/blogs"><c:param name="q" value="${q}"/></c:url>
    <c:set var="qs" value="${fn:substringAfter(qUrl, '?q=')}"/>
    <c:set var="pageBase" value="/admin/blogs?q=${qs}"/>

    <div class="admin-card admin-card--flush ops-enter">
      <form class="admin-toolbar" method="get" action="${ctx}/admin/blogs" role="search" aria-label="Tìm bài viết">
        <div class="admin-toolbar__left">
          <label class="search-field">
            <span class="sr-only">Tìm theo tiêu đề</span>
            <t:icon name="search"/>
            <input class="input" type="search" name="q" value="${fn:escapeXml(q)}" placeholder="Tiêu đề bài viết" maxlength="100" autocomplete="off">
          </label>
          <button class="btn btn--primary btn--sm" type="submit">Tìm kiếm</button>
          <c:if test="${not empty q}">
            <a class="btn btn--ghost btn--sm" href="${ctx}/admin/blogs">Xóa bộ lọc</a>
          </c:if>
        </div>
        <p class="t-small t-muted">${blogPage.totalItems} bài viết</p>
      </form>

      <div class="admin-table-wrap">
        <table class="table responsive ops-table">
          <caption class="sr-only">Danh sách bài viết</caption>
          <thead>
            <tr>
              <th scope="col">Ảnh bìa</th>
              <th scope="col">Tiêu đề</th>
              <th scope="col">Tác giả</th>
              <th scope="col">Ngày tạo</th>
              <th scope="col">Trạng thái</th>
              <th scope="col"><span class="sr-only">Thao tác</span></th>
            </tr>
          </thead>
          <tbody>
            <c:forEach items="${blogPage.items}" var="b">
              <tr>
                <td class="cell-lead"><div class="thumb thumb--wide"><t:img src="${b.coverImage}" alt="${b.title}"/></div></td>
                <td data-label="Tiêu đề">
                  <div class="cell-stack">
                    <a class="bold link-underline" href="${ctx}/admin/blogs/form?id=${b.id}"><c:out value="${b.title}"/></a>
                    <small class="cell-clamp"><c:out value="${b.summary}"/></small>
                  </div>
                </td>
                <td data-label="Tác giả" class="cell-nowrap"><c:out value="${b.authorName}"/></td>
                <td data-label="Ngày tạo" class="cell-nowrap"><fmt:formatDate value="${b.createdAt}" pattern="dd/MM/yyyy"/></td>
                <td data-label="Trạng thái"><span class="badge ${b.published ? 'badge--success' : 'badge--muted'}">${b.published ? 'Đã đăng' : 'Đang ẩn'}</span></td>
                <td>
                  <div class="cell-actions">
                    <a class="icon-btn" href="${ctx}/admin/blogs/form?id=${b.id}" aria-label="Sửa bài viết ${fn:escapeXml(b.title)}" title="Sửa"><t:icon name="pencil"/></a>
                    <form method="post" action="${ctx}/admin/blogs">
                      <input type="hidden" name="action" value="togglePublish">
                      <input type="hidden" name="id" value="${b.id}">
                      <button class="icon-btn" type="submit" aria-label="${b.published ? 'Ẩn' : 'Đăng'} bài viết ${fn:escapeXml(b.title)}" title="${b.published ? 'Ẩn bài viết' : 'Đăng bài viết'}"><t:icon name="${b.published ? 'eye-off' : 'eye'}"/></button>
                    </form>
                    <form method="post" action="${ctx}/admin/blogs" data-confirm="Xóa bài viết &quot;${fn:escapeXml(b.title)}&quot;? Thao tác này không thể hoàn tác.">
                      <input type="hidden" name="action" value="delete">
                      <input type="hidden" name="id" value="${b.id}">
                      <button class="icon-btn icon-btn--danger" type="submit" aria-label="Xóa bài viết ${fn:escapeXml(b.title)}" title="Xóa"><t:icon name="trash"/></button>
                    </form>
                  </div>
                </td>
              </tr>
            </c:forEach>
            <c:if test="${empty blogPage.items}">
              <tr class="empty-row"><td colspan="6">
                <c:choose>
                  <c:when test="${not empty q}">Không tìm thấy bài viết nào phù hợp.</c:when>
                  <c:otherwise>Chưa có bài viết nào. Bấm Thêm bài viết để viết bài đầu tiên.</c:otherwise>
                </c:choose>
              </td></tr>
            </c:if>
          </tbody>
        </table>
      </div>

      <c:set var="rangeFrom" value="${(blogPage.page - 1) * blogPage.pageSize + 1}"/>
      <c:set var="rangeTo" value="${rangeFrom + fn:length(blogPage.items) - 1}"/>
      <div class="admin-table-foot">
        <span>
          <c:choose>
            <c:when test="${blogPage.totalItems > 0}">Hiển thị ${rangeFrom} đến ${rangeTo} trong ${blogPage.totalItems} bài viết</c:when>
            <c:otherwise>Chưa có bài viết nào</c:otherwise>
          </c:choose>
        </span>
        <t:pagination page="${blogPage.page}" totalPages="${blogPage.totalPages}" baseUrl="${pageBase}"/>
      </div>
    </div>
  </jsp:body>
</t:admin-layout>
