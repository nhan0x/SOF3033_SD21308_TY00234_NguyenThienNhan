<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  ADMIN - THÊM / SỬA SẢN PHẨM  (GET /admin/products/form?id= -> AdminProductFormServlet)
  Tham số: id (không có = thêm mới)
  Dữ liệu:
    product: Product - sản phẩm đang sửa, null khi thêm mới
    categories: List<Category> - đổ vào ô chọn danh mục
    colors: List<Color> - mọi màu có thể gắn cho sản phẩm
    sizes: List<Size> - mọi size, mỗi size là một cột của ma trận tồn kho
    mode: String - "create" hoặc "edit"
    stockMap: Map<String,Integer> - tồn kho theo khóa "colorId_sizeId" (rỗng khi thêm mới)
    selectedColorIds: Set<Integer> - id các màu đã gắn cho sản phẩm
    errors: Map<String,String> - lỗi theo tên trường (có thể không có)
  Form: POST /admin/products action=save (multipart), trường: id, name, slug, categoryId, price, description, featured, imageFile, colorIds, stock_{colorId}_{sizeId}
--%>
<t:admin-layout title="${mode == 'edit' ? 'Sửa sản phẩm' : 'Thêm sản phẩm'}" active="products">
  <jsp:attribute name="head"><link rel="stylesheet" href="${ctx}/assets/css/pages/admin-catalog.css"></jsp:attribute>
  <jsp:attribute name="scripts"><script src="${ctx}/assets/js/pages/admin-catalog.js" defer></script></jsp:attribute>
  <jsp:attribute name="actions"><a class="btn btn--ghost btn--sm" href="${ctx}/admin/products"><t:icon name="arrow-left"/> Danh sách sản phẩm</a></jsp:attribute>
  <jsp:body>
    <%@ include file="/WEB-INF/views/common/flash.jspf" %>

    <%-- Giá trị hiện tại của form: ưu tiên dữ liệu vừa gửi (khi báo lỗi), sau đó đến sản phẩm đang sửa --%>
    <c:set var="vName" value="${not empty param.name ? param.name : product.name}"/>
    <c:set var="vSlug" value="${not empty param.slug ? param.slug : product.slug}"/>
    <c:set var="vCat" value="${not empty param.categoryId ? param.categoryId : product.categoryId}"/>
    <c:set var="vPrice" value="${not empty param.price ? param.price : (empty product ? '' : product.price)}"/>
    <c:set var="vDesc" value="${not empty param.description ? param.description : product.description}"/>
    <c:set var="vFeatured" value="${not empty param.name ? param.featured == 'true' : product.featured}"/>

    <form class="ac-form" id="product-form" method="post" action="${ctx}/admin/products" enctype="multipart/form-data">
      <input type="hidden" name="action" value="save">
      <input type="hidden" name="id" value="${empty product ? '' : product.id}">

      <%-- 1. Thông tin --%>
      <section class="admin-card" aria-labelledby="sec-info">
        <h2 class="admin-card__title" id="sec-info">Thông tin sản phẩm</h2>
        <div class="form-grid">
          <div class="field span-2">
            <label class="field__label" for="name">Tên sản phẩm <span class="req" aria-hidden="true">*</span></label>
            <input class="input" type="text" id="name" name="name" required minlength="2" maxlength="150" value="${fn:escapeXml(vName)}" placeholder="Ví dụ: Áo thun cổ tròn" autocomplete="off">
            <c:if test="${not empty errors.name}"><span class="field__error"><c:out value="${errors.name}"/></span></c:if>
          </div>

          <div class="field">
            <label class="field__label" for="slug">Đường dẫn (slug) <span class="req" aria-hidden="true">*</span></label>
            <input class="input" type="text" id="slug" name="slug" required maxlength="170" pattern="[a-z0-9]+(-[a-z0-9]+)*" value="${fn:escapeXml(vSlug)}" placeholder="ao-thun-co-tron" data-slug-from="#name" autocomplete="off">
            <span class="field__hint">Tự điền theo tên. Chỉ gồm chữ thường, số và dấu gạch ngang.</span>
            <c:if test="${not empty errors.slug}"><span class="field__error"><c:out value="${errors.slug}"/></span></c:if>
          </div>

          <div class="field">
            <label class="field__label" for="categoryId">Danh mục <span class="req" aria-hidden="true">*</span></label>
            <select class="select" id="categoryId" name="categoryId" required>
              <option value="">Chọn danh mục</option>
              <c:forEach items="${categories}" var="cat">
                <option value="${cat.id}"${fn:trim(vCat) == fn:trim(cat.id) ? ' selected' : ''}><c:out value="${cat.name}"/></option>
              </c:forEach>
            </select>
            <c:if test="${not empty errors.categoryId}"><span class="field__error"><c:out value="${errors.categoryId}"/></span></c:if>
          </div>

          <div class="field">
            <label class="field__label" for="price">Giá bán (đ) <span class="req" aria-hidden="true">*</span></label>
            <input class="input" type="number" id="price" name="price" required min="1000" max="99999999" step="1000" inputmode="numeric" value="${fn:escapeXml(vPrice)}" placeholder="199000">
            <c:if test="${not empty errors.price}"><span class="field__error"><c:out value="${errors.price}"/></span></c:if>
          </div>

          <div class="field">
            <span class="field__label" id="featured-label">Hiển thị nổi bật</span>
            <div class="switch-row">
              <span class="switch-row__text"><span class="t-small t-muted">Sản phẩm nổi bật được ưu tiên ở trang chủ.</span></span>
              <label class="switch"><input type="checkbox" name="featured" value="true" aria-labelledby="featured-label"${vFeatured ? ' checked' : ''}></label>
            </div>
          </div>

          <div class="field span-2">
            <label class="field__label" for="description">Mô tả</label>
            <textarea class="textarea" id="description" name="description" rows="6" maxlength="2000" placeholder="Chất liệu, form dáng, cách bảo quản..."><c:out value="${vDesc}"/></textarea>
            <c:if test="${not empty errors.description}"><span class="field__error"><c:out value="${errors.description}"/></span></c:if>
          </div>
        </div>
      </section>

      <%-- 2. Ảnh --%>
      <section class="admin-card" aria-labelledby="sec-image">
        <h2 class="admin-card__title" id="sec-image">Ảnh sản phẩm</h2>
        <div class="upload-grid">
          <div class="upload-preview" id="image-preview"><t:img src="${product.image}" alt="${empty product ? 'Ảnh sản phẩm' : product.name}"/></div>
          <label class="upload-box">
            <t:icon name="upload"/>
            <span class="upload-box__title">Chọn hoặc kéo thả ảnh vào đây</span>
            <span class="t-small">Ảnh vuông 1200 x 1200, JPG hoặc PNG, dưới 2MB. Nền trơn sáng, chỉ có sản phẩm.</span>
            <c:if test="${mode == 'edit'}"><span class="t-small">Không chọn ảnh mới thì giữ ảnh hiện tại.</span></c:if>
            <span class="upload-box__file" id="image-name"></span>
            <span class="upload-box__error" id="image-error" role="alert"></span>
            <input class="sr-only" type="file" id="imageFile" name="imageFile" accept="image/jpeg,image/png" aria-label="Ảnh sản phẩm" data-image-preview="#image-preview" data-image-name="#image-name" data-image-error="#image-error">
          </label>
        </div>
        <c:if test="${not empty errors.imageFile}"><span class="field__error"><c:out value="${errors.imageFile}"/></span></c:if>
      </section>

      <%-- 3. Biến thể --%>
      <section class="admin-card" aria-labelledby="sec-variant" data-variant-matrix>
        <div>
          <h2 class="admin-card__title" id="sec-variant">Biến thể: màu và tồn kho</h2>
          <p class="ac-note">Chọn các màu sản phẩm có, sau đó nhập số lượng tồn cho từng size.</p>
        </div>

        <div class="field" role="group" aria-labelledby="color-label">
          <span class="field__label" id="color-label">Màu sắc</span>
          <div class="color-picker">
            <c:forEach items="${colors}" var="col">
              <label class="color-choice">
                <span class="swatch"><input type="checkbox" name="colorIds" value="${col.id}" data-color-toggle aria-label="${fn:escapeXml(col.name)}"${selectedColorIds.contains(col.id) ? ' checked' : ''}><span style="--sw:${fn:escapeXml(col.hexCode)}"></span></span>
                <span><c:out value="${col.name}"/></span>
              </label>
            </c:forEach>
          </div>
          <c:if test="${not empty errors.colorIds}"><span class="field__error"><c:out value="${errors.colorIds}"/></span></c:if>
        </div>

        <div class="variant-empty" data-variant-empty${empty selectedColorIds ? '' : ' hidden'}>Chọn ít nhất một màu để nhập tồn kho.</div>
        <div class="variant-scroll">
          <table class="table variant-table" data-variant-table${empty selectedColorIds ? ' hidden' : ''}>
            <thead>
              <tr>
                <th scope="col">Màu</th>
                <c:forEach items="${sizes}" var="sz"><th class="num" scope="col">Size <c:out value="${sz.name}"/></th></c:forEach>
                <th class="num" scope="col">Tổng</th>
              </tr>
            </thead>
            <tbody>
              <c:forEach items="${colors}" var="col">
                <c:set var="isSel" value="${selectedColorIds.contains(col.id)}"/>
                <tr data-color-row="${col.id}"${isSel ? '' : ' hidden'}>
                  <td><span class="color-dot" style="--sw:${fn:escapeXml(col.hexCode)}"></span><c:out value="${col.name}"/></td>
                  <c:forEach items="${sizes}" var="sz">
                    <c:set var="stockKey" value="${col.id}_${sz.id}"/>
                    <td class="num"><input class="input" type="number" min="0" max="99999" step="1" inputmode="numeric" name="stock_${col.id}_${sz.id}" value="${empty stockMap[stockKey] ? 0 : stockMap[stockKey]}" data-size="${sz.id}" aria-label="Tồn kho ${fn:escapeXml(col.name)} size ${fn:escapeXml(sz.name)}"${isSel ? '' : ' disabled'}></td>
                  </c:forEach>
                  <td class="num bold" data-row-total>0</td>
                </tr>
              </c:forEach>
            </tbody>
            <tfoot>
              <tr>
                <td>Tổng</td>
                <c:forEach items="${sizes}" var="sz"><td class="num" data-size-col="${sz.id}">0</td></c:forEach>
                <td class="num" data-grand-total>0</td>
              </tr>
            </tfoot>
          </table>
        </div>
        <c:if test="${not empty errors.stock}"><span class="field__error"><c:out value="${errors.stock}"/></span></c:if>
      </section>

      <%-- Thanh thao tác cố định phía dưới --%>
      <div class="form-bar">
        <span class="form-bar__hint">Các trường có dấu * là bắt buộc.</span>
        <a class="btn btn--ghost" href="${ctx}/admin/products">Hủy</a>
        <button class="btn btn--primary" type="submit"><t:icon name="check"/> Lưu sản phẩm</button>
      </div>
    </form>
  </jsp:body>
</t:admin-layout>
