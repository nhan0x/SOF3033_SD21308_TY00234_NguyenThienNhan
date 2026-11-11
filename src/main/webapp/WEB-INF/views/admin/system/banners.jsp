<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  ADMIN - BANNER
  Route:   GET /admin/banners
  Servlet: com.fashionweb.controller.admin.system.AdminBannerServlet (active menu: banners)
  Request attributes:
    banners : List<Banner>         - toàn bộ banner (cả banner đang tắt), sắp theo sortOrder
    errors  : Map<String,String>   - (tùy chọn) lỗi sau khi POST thất bại: title, linkUrl, sortOrder, imageFile
  Forms (POST ${ctx}/admin/banners):
    action=save    (multipart) fields: id (rỗng = thêm mới), title, linkUrl, sortOrder, imageFile, active
    action=toggle  fields: id, active (ô công tắc: có tham số = bật, không có = tắt)
    action=delete  fields: id (xóa hẳn)
--%>
<t:admin-layout title="Banner" active="banners">
  <jsp:attribute name="head"><link rel="stylesheet" href="${ctx}/assets/css/pages/admin-ops.css"></jsp:attribute>
  <jsp:attribute name="scripts"><script src="${ctx}/assets/js/pages/admin-ops.js" defer></script></jsp:attribute>
  <jsp:attribute name="actions"><button class="btn btn--primary btn--sm js-fill" type="button" data-dialog-open="banner-dialog" data-dialog-title="Thêm banner"><t:icon name="plus"/> Thêm banner</button></jsp:attribute>
  <jsp:body>
    <%@ include file="/WEB-INF/views/common/flash.jspf" %>

    <div class="admin-card admin-card--flush ops-enter">
      <div class="admin-toolbar">
        <p class="t-small t-muted">Banner đang bật hiển thị trên trang chủ, theo thứ tự từ nhỏ đến lớn.</p>
        <p class="t-small t-muted">${fn:length(banners)} banner</p>
      </div>

      <div class="admin-table-wrap">
        <table class="table responsive ops-table">
          <caption class="sr-only">Danh sách banner</caption>
          <thead>
            <tr>
              <th scope="col">Ảnh</th>
              <th scope="col">Tiêu đề</th>
              <th scope="col">Liên kết</th>
              <th scope="col" class="num">Thứ tự</th>
              <th scope="col">Hiển thị</th>
              <th scope="col"><span class="sr-only">Thao tác</span></th>
            </tr>
          </thead>
          <tbody>
            <c:forEach items="${banners}" var="b">
              <c:set var="imgRel" value="${fn:startsWith(b.image, '/') ? fn:substring(b.image, 1, fn:length(b.image)) : b.image}"/>
              <c:set var="imgUrl" value="${empty b.image ? '' : (fn:startsWith(b.image, 'http') ? b.image : ''.concat(ctx).concat('/').concat(imgRel))}"/>
              <tr>
                <td class="cell-lead"><div class="thumb thumb--wide"><t:img src="${b.image}" alt="${b.title}"/></div></td>
                <td data-label="Tiêu đề" class="bold"><c:out value="${b.title}"/></td>
                <td data-label="Liên kết"><span class="t-muted cell-clamp"><c:out value="${b.linkUrl}"/></span></td>
                <td data-label="Thứ tự" class="num">${b.sortOrder}</td>
                <td data-label="Hiển thị">
                  <form class="switch-form" method="post" action="${ctx}/admin/banners">
                    <input type="hidden" name="action" value="toggle">
                    <input type="hidden" name="id" value="${b.id}">
                    <label class="switch">
                      <input type="checkbox" name="active" value="true"${b.active ? ' checked' : ''} data-autosubmit aria-label="Hiển thị banner ${fn:escapeXml(b.title)}">
                    </label>
                  </form>
                </td>
                <td>
                  <div class="cell-actions">
                    <button class="icon-btn js-fill" type="button" aria-label="Sửa banner ${fn:escapeXml(b.title)}" title="Sửa"
                            data-dialog-open="banner-dialog" data-dialog-title="Sửa banner"
                            data-field-id="${b.id}" data-field-title="${fn:escapeXml(b.title)}" data-field-link-url="${fn:escapeXml(b.linkUrl)}"
                            data-field-sort-order="${b.sortOrder}" data-field-active="${b.active}" data-field-image="${fn:escapeXml(imgUrl)}"><t:icon name="pencil"/></button>
                    <form method="post" action="${ctx}/admin/banners" data-confirm="Xóa banner &quot;${fn:escapeXml(b.title)}&quot;? Thao tác này không thể hoàn tác.">
                      <input type="hidden" name="action" value="delete">
                      <input type="hidden" name="id" value="${b.id}">
                      <button class="icon-btn icon-btn--danger" type="submit" aria-label="Xóa banner ${fn:escapeXml(b.title)}" title="Xóa"><t:icon name="trash"/></button>
                    </form>
                  </div>
                </td>
              </tr>
            </c:forEach>
            <c:if test="${empty banners}">
              <tr class="empty-row"><td colspan="6">Chưa có banner nào. Bấm Thêm banner để tạo banner đầu tiên.</td></tr>
            </c:if>
          </tbody>
        </table>
      </div>
    </div>

    <%-- Hộp thoại thêm hoặc sửa banner --%>
    <dialog class="dialog" id="banner-dialog" aria-labelledby="banner-dialog-title" data-auto-open="${not empty errors}">
      <form method="post" action="${ctx}/admin/banners" enctype="multipart/form-data">
        <input type="hidden" name="action" value="save">
        <input type="hidden" name="id" value="${fn:escapeXml(param.id)}">
        <div class="dialog__head">
          <h2 class="h-2" id="banner-dialog-title" data-dialog-title>Thêm banner</h2>
          <button class="icon-btn" type="button" data-dialog-close aria-label="Đóng"><t:icon name="x" size="md"/></button>
        </div>
        <div class="dialog__body">
          <div class="field">
            <label class="field__label" for="banner-title">Tiêu đề <span class="req" aria-hidden="true">*</span></label>
            <input class="input${not empty errors.title ? ' is-invalid' : ''}" id="banner-title" type="text" name="title" value="${fn:escapeXml(param.title)}" required maxlength="120" placeholder="Ví dụ: Basic cho ngày nắng">
            <c:if test="${not empty errors.title}"><span class="field__error"><c:out value="${errors.title}"/></span></c:if>
          </div>
          <div class="field">
            <label class="field__label" for="banner-link">Liên kết khi bấm vào banner</label>
            <input class="input${not empty errors.linkUrl ? ' is-invalid' : ''}" id="banner-link" type="text" name="linkUrl" value="${fn:escapeXml(param.linkUrl)}" maxlength="255" pattern="(/|https?://).*" title="Bắt đầu bằng / hoặc https://" placeholder="/products?category=ao-khoac">
            <span class="field__hint">Đường dẫn trong website, ví dụ /products?sort=newest.</span>
            <c:if test="${not empty errors.linkUrl}"><span class="field__error"><c:out value="${errors.linkUrl}"/></span></c:if>
          </div>
          <div class="field">
            <label class="field__label" for="banner-sort">Thứ tự hiển thị</label>
            <input class="input${not empty errors.sortOrder ? ' is-invalid' : ''}" id="banner-sort" type="number" name="sortOrder" value="${empty param.sortOrder ? '1' : fn:escapeXml(param.sortOrder)}" min="1" max="99" required>
            <c:if test="${not empty errors.sortOrder}"><span class="field__error"><c:out value="${errors.sortOrder}"/></span></c:if>
          </div>
          <div class="field">
            <span class="field__label" id="banner-image-label">Ảnh banner</span>
            <label class="upload-box" data-upload>
              <input class="sr-only" id="banner-image" type="file" name="imageFile" accept="image/jpeg,image/png,image/webp" aria-labelledby="banner-image-label">
              <div class="upload-box__preview" data-upload-preview hidden></div>
              <span class="upload-box__empty">
                <t:icon name="upload"/>
                <span class="bold">Chọn ảnh banner</span>
                <span class="t-small">JPG, PNG hoặc WebP, tối đa 2MB. Nên dùng ảnh ngang.</span>
              </span>
              <span class="upload-box__change">Bấm để đổi ảnh</span>
            </label>
            <c:if test="${not empty errors.imageFile}"><span class="field__error"><c:out value="${errors.imageFile}"/></span></c:if>
          </div>
          <div class="switch-row">
            <span class="switch-row__text"><span class="bold">Hiển thị trên trang chủ</span><span class="t-small t-muted">Tắt để ẩn banner mà không cần xóa.</span></span>
            <label class="switch"><input type="checkbox" name="active" value="true" checked aria-label="Hiển thị trên trang chủ"></label>
          </div>
        </div>
        <div class="dialog__foot">
          <button class="btn btn--ghost btn--sm" type="button" data-dialog-close>Hủy</button>
          <button class="btn btn--primary btn--sm" type="submit">Lưu banner</button>
        </div>
      </form>
    </dialog>
  </jsp:body>
</t:admin-layout>
