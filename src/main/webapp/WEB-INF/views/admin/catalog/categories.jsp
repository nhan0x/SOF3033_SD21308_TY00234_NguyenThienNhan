<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  ADMIN - DANH MỤC  (GET /admin/categories -> AdminCategoryServlet)
  Tham số: không có
  Dữ liệu:
    categories: List<Category> - danh mục chưa xóa mềm, kèm productCount
    errors: Map<String,String> - lỗi theo tên trường khi lưu thất bại (có thể không có)
    openDialog: String - "category-dialog" nếu cần tự mở lại hộp thoại sau khi lưu lỗi (có thể không có)
  Form: POST /admin/categories action=save (multipart): id, name, slug, imageFile
        POST /admin/categories action=delete: id (xóa mềm)
--%>
<t:admin-layout title="Danh mục" active="categories">
  <jsp:attribute name="head"><link rel="stylesheet" href="${ctx}/assets/css/pages/admin-catalog.css"></jsp:attribute>
  <jsp:attribute name="scripts"><script src="${ctx}/assets/js/pages/admin-catalog.js" defer></script></jsp:attribute>
  <jsp:attribute name="actions"><button type="button" class="btn btn--primary btn--sm" data-dialog-open="category-dialog" data-dialog-title="Thêm danh mục" data-fill="{&quot;id&quot;:&quot;&quot;,&quot;name&quot;:&quot;&quot;,&quot;slug&quot;:&quot;&quot;}"><t:icon name="plus"/> Thêm danh mục</button></jsp:attribute>
  <jsp:body>
    <%@ include file="/WEB-INF/views/common/flash.jspf" %>

    <section class="admin-card admin-card--flush" aria-label="Danh sách danh mục">
      <div class="admin-toolbar">
        <span class="admin-toolbar__count"><fmt:formatNumber value="${fn:length(categories)}"/> danh mục</span>
        <span class="ac-note">Xóa danh mục là xóa mềm, có thể khôi phục trong Thùng rác.</span>
      </div>
      <div class="admin-table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>Danh mục</th>
              <th>Đường dẫn (slug)</th>
              <th class="num">Số sản phẩm</th>
              <th>Ngày tạo</th>
              <th><span class="sr-only">Thao tác</span></th>
            </tr>
          </thead>
          <tbody>
            <c:forEach items="${categories}" var="cat">
              <tr>
                <td>
                  <div class="cell-product">
                    <div class="thumb"><t:img src="${cat.image}" alt="${cat.name}"/></div>
                    <span class="bold"><c:out value="${cat.name}"/></span>
                  </div>
                </td>
                <td class="code-text"><c:out value="${cat.slug}"/></td>
                <td class="num"><fmt:formatNumber value="${cat.productCount}"/></td>
                <td class="nowrap"><fmt:formatDate value="${cat.createdAt}" pattern="dd/MM/yyyy"/></td>
                <td>
                  <div class="cell-actions">
                    <button type="button" class="icon-btn" data-dialog-open="category-dialog" data-dialog-title="Sửa danh mục" data-fill="{&quot;id&quot;:${cat.id},&quot;name&quot;:&quot;${fn:escapeXml(fn:replace(cat.name, '"', '\\"'))}&quot;,&quot;slug&quot;:&quot;${fn:escapeXml(fn:replace(cat.slug, '"', '\\"'))}&quot;}" aria-label="Sửa danh mục ${fn:escapeXml(cat.name)}" title="Sửa"><t:icon name="pencil"/></button>
                    <form class="inline-form" method="post" action="${ctx}/admin/categories" data-confirm="Xóa danh mục “${fn:escapeXml(cat.name)}”?<c:if test='${cat.productCount > 0}'> Hiện còn ${cat.productCount} sản phẩm thuộc danh mục này.</c:if> Danh mục sẽ được chuyển vào Thùng rác (xóa mềm) và có thể khôi phục lại sau.">
                      <input type="hidden" name="action" value="delete">
                      <input type="hidden" name="id" value="${cat.id}">
                      <button class="icon-btn icon-btn--danger" type="submit" aria-label="Xóa danh mục ${fn:escapeXml(cat.name)}" title="Xóa"><t:icon name="trash"/></button>
                    </form>
                  </div>
                </td>
              </tr>
            </c:forEach>
            <c:if test="${empty categories}"><tr class="empty-row"><td colspan="5">Chưa có danh mục nào. Bấm “Thêm danh mục” để tạo mới.</td></tr></c:if>
          </tbody>
        </table>
      </div>
    </section>

    <%-- Hộp thoại thêm / sửa danh mục (mở bằng data-dialog-open, điền dữ liệu bằng data-fill) --%>
    <c:set var="dlgErr" value="${openDialog == 'category-dialog' ? errors : null}"/>
    <dialog class="dialog" id="category-dialog" aria-labelledby="category-dialog-title" data-reset-on-close${openDialog == 'category-dialog' ? ' data-open-on-load' : ''}>
      <form method="post" action="${ctx}/admin/categories" enctype="multipart/form-data">
        <input type="hidden" name="action" value="save">
        <input type="hidden" name="id" value="${openDialog == 'category-dialog' ? fn:escapeXml(param.id) : ''}">
        <div class="dialog__head">
          <h2 class="h-2" id="category-dialog-title" data-dialog-title>Thêm danh mục</h2>
          <button type="button" class="icon-btn" data-dialog-close aria-label="Đóng"><t:icon name="x" size="md"/></button>
        </div>
        <div class="dialog__body">
          <div class="field">
            <label class="field__label" for="category-name">Tên danh mục <span class="req" aria-hidden="true">*</span></label>
            <input class="input" type="text" id="category-name" name="name" required minlength="2" maxlength="100" value="${openDialog == 'category-dialog' ? fn:escapeXml(param.name) : ''}" placeholder="Ví dụ: Áo thun và Polo" autocomplete="off">
            <c:if test="${not empty dlgErr.name}"><span class="field__error"><c:out value="${dlgErr.name}"/></span></c:if>
          </div>
          <div class="field">
            <label class="field__label" for="category-slug">Đường dẫn (slug) <span class="req" aria-hidden="true">*</span></label>
            <input class="input" type="text" id="category-slug" name="slug" required maxlength="120" pattern="[a-z0-9]+(-[a-z0-9]+)*" value="${openDialog == 'category-dialog' ? fn:escapeXml(param.slug) : ''}" placeholder="ao-thun-polo" data-slug-from="#category-name" autocomplete="off">
            <span class="field__hint">Tự điền theo tên. Chỉ gồm chữ thường, số và dấu gạch ngang.</span>
            <c:if test="${not empty dlgErr.slug}"><span class="field__error"><c:out value="${dlgErr.slug}"/></span></c:if>
          </div>
          <div class="field">
            <label class="field__label" for="category-image">Ảnh danh mục</label>
            <div class="dialog-file">
              <div class="dialog-file__row">
                <div class="dialog-preview" id="category-image-preview"><t:img alt="Ảnh danh mục"/></div>
                <input type="file" id="category-image" name="imageFile" accept="image/jpeg,image/png" data-image-preview="#category-image-preview" data-image-error="#category-image-error">
              </div>
              <span class="field__hint">JPG hoặc PNG, dưới 2MB. Khi sửa mà không chọn ảnh mới thì giữ ảnh cũ.</span>
              <span class="upload-box__error" id="category-image-error" role="alert"></span>
              <c:if test="${not empty dlgErr.imageFile}"><span class="field__error"><c:out value="${dlgErr.imageFile}"/></span></c:if>
            </div>
          </div>
        </div>
        <div class="dialog__foot">
          <button type="button" class="btn btn--ghost" data-dialog-close>Hủy</button>
          <button type="submit" class="btn btn--primary">Lưu danh mục</button>
        </div>
      </form>
    </dialog>
  </jsp:body>
</t:admin-layout>
