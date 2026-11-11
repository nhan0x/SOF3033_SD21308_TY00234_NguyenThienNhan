<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  ADMIN - THÊM / SỬA BÀI VIẾT
  Route:   GET /admin/blogs/form?id=
  Servlet: com.fashionweb.controller.admin.system.AdminBlogFormServlet (active menu: blogs)
  Request attributes:
    blog   : Blog                - bài đang sửa; null khi thêm mới
    mode   : String              - "create" | "edit"
    errors : Map<String,String>  - (tùy chọn) lỗi sau khi POST thất bại: title, slug, summary, content, coverImage
  Forms (POST ${ctx}/admin/blogs, multipart/form-data):
    action=save  fields: id (rỗng = thêm mới), title, slug, summary, content, coverImage (file), published
  Khi lưu lỗi, servlet forward lại trang này; các ô lấy lại giá trị từ param.
--%>
<t:admin-layout title="${mode == 'edit' ? 'Sửa bài viết' : 'Thêm bài viết'}" active="blogs">
  <jsp:attribute name="head"><link rel="stylesheet" href="${ctx}/assets/css/pages/admin-ops.css"></jsp:attribute>
  <jsp:attribute name="scripts"><script src="${ctx}/assets/js/pages/admin-ops.js" defer></script></jsp:attribute>
  <jsp:attribute name="actions"><a class="btn btn--ghost btn--sm" href="${ctx}/admin/blogs"><t:icon name="arrow-left"/> Danh sách bài viết</a></jsp:attribute>
  <jsp:body>
    <%@ include file="/WEB-INF/views/common/flash.jspf" %>

    <%-- Giá trị hiện tại: ưu tiên dữ liệu vừa gửi (khi báo lỗi), sau đó đến bài đang sửa --%>
    <c:set var="submitted" value="${not empty param.title}"/>
    <c:set var="vTitle" value="${submitted ? param.title : blog.title}"/>
    <c:set var="vSlug" value="${submitted ? param.slug : blog.slug}"/>
    <c:set var="vSummary" value="${submitted ? param.summary : blog.summary}"/>
    <c:set var="vContent" value="${submitted ? param.content : blog.content}"/>
    <c:set var="vPublished" value="${submitted ? param.published == 'true' : (empty blog ? true : blog.published)}"/>
    <c:set var="vCover" value="${empty blog ? '' : blog.coverImage}"/>

    <form class="ops-enter" id="blog-form" method="post" action="${ctx}/admin/blogs" enctype="multipart/form-data">
      <input type="hidden" name="action" value="save">
      <input type="hidden" name="id" value="${empty blog ? '' : blog.id}">

      <div class="admin-grid admin-grid--main">
        <%-- Nội dung bài viết --%>
        <section class="admin-card" aria-labelledby="blog-sec-content">
          <h2 class="admin-card__title" id="blog-sec-content">Nội dung bài viết</h2>
          <div class="admin-form__section">
            <div class="field">
              <label class="field__label" for="blog-title">Tiêu đề <span class="req" aria-hidden="true">*</span></label>
              <input class="input${not empty errors.title ? ' is-invalid' : ''}" id="blog-title" type="text" name="title" value="${fn:escapeXml(vTitle)}" required minlength="5" maxlength="200" placeholder="Ví dụ: Cách phối đồ basic cho ngày nắng" data-slug-source="blog-slug" autocomplete="off">
              <c:if test="${not empty errors.title}"><span class="field__error"><c:out value="${errors.title}"/></span></c:if>
            </div>

            <div class="field">
              <label class="field__label" for="blog-slug">Đường dẫn (slug) <span class="req" aria-hidden="true">*</span></label>
              <input class="input${not empty errors.slug ? ' is-invalid' : ''}" id="blog-slug" type="text" name="slug" value="${fn:escapeXml(vSlug)}" required maxlength="220" pattern="[a-z0-9]+(-[a-z0-9]+)*" placeholder="cach-phoi-do-basic" autocomplete="off">
              <span class="field__hint">Tự điền theo tiêu đề. Chỉ gồm chữ thường không dấu, số và dấu gạch ngang.</span>
              <c:if test="${not empty errors.slug}"><span class="field__error"><c:out value="${errors.slug}"/></span></c:if>
            </div>

            <div class="field">
              <label class="field__label" for="blog-summary">Tóm tắt</label>
              <textarea class="textarea${not empty errors.summary ? ' is-invalid' : ''}" id="blog-summary" name="summary" rows="3" maxlength="500" placeholder="Một hai câu giới thiệu hiện ở danh sách bài viết."><c:out value="${vSummary}"/></textarea>
              <c:if test="${not empty errors.summary}"><span class="field__error"><c:out value="${errors.summary}"/></span></c:if>
            </div>

            <div class="field">
              <label class="field__label" for="blog-content">Nội dung <span class="req" aria-hidden="true">*</span></label>
              <textarea class="textarea textarea--lg${not empty errors.content ? ' is-invalid' : ''}" id="blog-content" name="content" required minlength="20" placeholder="Viết nội dung bài viết tại đây."><c:out value="${vContent}"/></textarea>
              <span class="field__hint">Để một dòng trống giữa hai đoạn để bắt đầu đoạn văn mới.</span>
              <c:if test="${not empty errors.content}"><span class="field__error"><c:out value="${errors.content}"/></span></c:if>
            </div>
          </div>
        </section>

        <%-- Ảnh bìa, đăng bài, nút lưu --%>
        <div class="order-stack">
          <section class="admin-card" aria-labelledby="blog-sec-cover">
            <h2 class="admin-card__title" id="blog-sec-cover">Ảnh bìa</h2>
            <div class="field">
              <span class="field__label" id="blog-cover-label">Ảnh hiện ở danh sách và đầu bài</span>
              <label class="upload-box upload-box--cover${empty vCover ? '' : ' has-image'}" data-upload data-initial="${empty vCover ? '' : 'true'}">
                <input class="sr-only" id="blog-cover" type="file" name="coverImage" accept="image/jpeg,image/png,image/webp" aria-labelledby="blog-cover-label">
                <div class="upload-box__preview" data-upload-preview${empty vCover ? ' hidden' : ''}>
                  <c:if test="${not empty vCover}"><t:img src="${vCover}" alt="Ảnh bìa hiện tại"/></c:if>
                </div>
                <span class="upload-box__empty">
                  <t:icon name="upload"/>
                  <span class="bold">Chọn ảnh bìa</span>
                  <span class="t-small">JPG, PNG hoặc WebP, tối đa 2MB.</span>
                </span>
                <span class="upload-box__change">Bấm để đổi ảnh</span>
              </label>
              <c:if test="${not empty errors.coverImage}"><span class="field__error"><c:out value="${errors.coverImage}"/></span></c:if>
            </div>
          </section>

          <section class="admin-card" aria-labelledby="blog-sec-publish">
            <h2 class="admin-card__title" id="blog-sec-publish">Đăng bài</h2>
            <div class="switch-row">
              <span class="switch-row__text"><span class="bold" id="blog-published-label">Hiển thị trên website</span><span class="t-small t-muted">Tắt để lưu nháp, bài chưa hiện với khách.</span></span>
              <label class="switch"><input type="checkbox" name="published" value="true" aria-labelledby="blog-published-label"${vPublished ? ' checked' : ''}></label>
            </div>
            <c:if test="${not empty blog}">
              <p class="t-small t-muted mt-16">Tạo ngày <fmt:formatDate value="${blog.createdAt}" pattern="dd/MM/yyyy"/> bởi <c:out value="${blog.authorName}"/>.</p>
            </c:if>
          </section>

          <div class="admin-form__actions">
            <a class="btn btn--ghost btn--sm" href="${ctx}/admin/blogs">Hủy</a>
            <button class="btn btn--primary btn--sm" type="submit">Lưu bài viết</button>
          </div>
        </div>
      </div>
    </form>
  </jsp:body>
</t:admin-layout>
