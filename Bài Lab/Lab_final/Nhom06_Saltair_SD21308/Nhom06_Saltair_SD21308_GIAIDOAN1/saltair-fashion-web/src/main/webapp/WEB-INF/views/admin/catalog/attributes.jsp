<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  ADMIN - MÀU VÀ SIZE  (GET /admin/attributes -> AdminAttributeServlet)
  Tham số: không có
  Dữ liệu:
    colors: List<Color> - màu chưa xóa mềm
    sizes: List<Size> - size chưa xóa mềm, sắp theo sortOrder
    errors: Map<String,String> - lỗi theo tên trường khi lưu thất bại (có thể không có)
    openDialog: String - "color-dialog" hoặc "size-dialog" nếu cần tự mở lại hộp thoại sau khi lưu lỗi (có thể không có)
  Form: POST /admin/attributes action=saveColor: id, name, code, hexCode
        POST /admin/attributes action=deleteColor: id (xóa mềm)
        POST /admin/attributes action=saveSize: id, name, sortOrder
        POST /admin/attributes action=deleteSize: id (xóa mềm)
--%>
<t:admin-layout title="Màu và Size" active="attributes">
  <jsp:attribute name="head"><link rel="stylesheet" href="${ctx}/assets/css/pages/admin-catalog.css"></jsp:attribute>
  <jsp:attribute name="scripts"><script src="${ctx}/assets/js/pages/admin-catalog.js" defer></script></jsp:attribute>
  <jsp:body>
    <%@ include file="/WEB-INF/views/common/flash.jspf" %>

    <div class="ac-cols">

      <%-- Thẻ Màu --%>
      <section class="admin-card admin-card--flush" aria-labelledby="color-title">
        <h2 class="admin-card__title" id="color-title">
          <span>Màu sắc <span class="t-small t-muted">(${fn:length(colors)})</span></span>
          <button type="button" class="btn btn--outline btn--xs" data-dialog-open="color-dialog" data-dialog-title="Thêm màu" data-fill="{&quot;id&quot;:&quot;&quot;,&quot;name&quot;:&quot;&quot;,&quot;code&quot;:&quot;&quot;,&quot;hexCode&quot;:&quot;#cccccc&quot;}"><t:icon name="plus"/> Thêm màu</button>
        </h2>
        <div class="admin-table-wrap">
          <table class="table">
            <thead><tr><th>Màu</th><th>Mã</th><th>Mã hex</th><th><span class="sr-only">Thao tác</span></th></tr></thead>
            <tbody>
              <c:forEach items="${colors}" var="col">
                <tr>
                  <td class="nowrap"><span class="color-dot" style="--sw:${fn:escapeXml(col.hexCode)}"></span><span class="bold"><c:out value="${col.name}"/></span></td>
                  <td class="code-text"><c:out value="${col.code}"/></td>
                  <td class="hex-text"><c:out value="${col.hexCode}"/></td>
                  <td>
                    <div class="cell-actions">
                      <button type="button" class="icon-btn" data-dialog-open="color-dialog" data-dialog-title="Sửa màu" data-fill="{&quot;id&quot;:${col.id},&quot;name&quot;:&quot;${fn:escapeXml(fn:replace(col.name, '"', '\\"'))}&quot;,&quot;code&quot;:&quot;${fn:escapeXml(fn:replace(col.code, '"', '\\"'))}&quot;,&quot;hexCode&quot;:&quot;${fn:escapeXml(fn:replace(col.hexCode, '"', '\\"'))}&quot;}" aria-label="Sửa màu ${fn:escapeXml(col.name)}" title="Sửa"><t:icon name="pencil"/></button>
                      <form class="inline-form" method="post" action="${ctx}/admin/attributes" data-confirm="Xóa màu “${fn:escapeXml(col.name)}”? Màu sẽ được chuyển vào Thùng rác (xóa mềm), không thể chọn cho sản phẩm mới và có thể khôi phục lại sau.">
                        <input type="hidden" name="action" value="deleteColor">
                        <input type="hidden" name="id" value="${col.id}">
                        <button class="icon-btn icon-btn--danger" type="submit" aria-label="Xóa màu ${fn:escapeXml(col.name)}" title="Xóa"><t:icon name="trash"/></button>
                      </form>
                    </div>
                  </td>
                </tr>
              </c:forEach>
              <c:if test="${empty colors}"><tr class="empty-row"><td colspan="4">Chưa có màu nào.</td></tr></c:if>
            </tbody>
          </table>
        </div>
      </section>

      <%-- Thẻ Size --%>
      <section class="admin-card admin-card--flush" aria-labelledby="size-title">
        <h2 class="admin-card__title" id="size-title">
          <span>Size <span class="t-small t-muted">(${fn:length(sizes)})</span></span>
          <button type="button" class="btn btn--outline btn--xs" data-dialog-open="size-dialog" data-dialog-title="Thêm size" data-fill="{&quot;id&quot;:&quot;&quot;,&quot;name&quot;:&quot;&quot;,&quot;sortOrder&quot;:${fn:length(sizes) + 1}}"><t:icon name="plus"/> Thêm size</button>
        </h2>
        <div class="admin-table-wrap">
          <table class="table">
            <thead><tr><th>Size</th><th class="num">Thứ tự hiển thị</th><th><span class="sr-only">Thao tác</span></th></tr></thead>
            <tbody>
              <c:forEach items="${sizes}" var="sz">
                <tr>
                  <td class="bold"><c:out value="${sz.name}"/></td>
                  <td class="num">${sz.sortOrder}</td>
                  <td>
                    <div class="cell-actions">
                      <button type="button" class="icon-btn" data-dialog-open="size-dialog" data-dialog-title="Sửa size" data-fill="{&quot;id&quot;:${sz.id},&quot;name&quot;:&quot;${fn:escapeXml(fn:replace(sz.name, '"', '\\"'))}&quot;,&quot;sortOrder&quot;:${sz.sortOrder}}" aria-label="Sửa size ${fn:escapeXml(sz.name)}" title="Sửa"><t:icon name="pencil"/></button>
                      <form class="inline-form" method="post" action="${ctx}/admin/attributes" data-confirm="Xóa size “${fn:escapeXml(sz.name)}”? Size sẽ được chuyển vào Thùng rác (xóa mềm), không thể chọn cho sản phẩm mới và có thể khôi phục lại sau.">
                        <input type="hidden" name="action" value="deleteSize">
                        <input type="hidden" name="id" value="${sz.id}">
                        <button class="icon-btn icon-btn--danger" type="submit" aria-label="Xóa size ${fn:escapeXml(sz.name)}" title="Xóa"><t:icon name="trash"/></button>
                      </form>
                    </div>
                  </td>
                </tr>
              </c:forEach>
              <c:if test="${empty sizes}"><tr class="empty-row"><td colspan="3">Chưa có size nào.</td></tr></c:if>
            </tbody>
          </table>
        </div>
      </section>
    </div>

    <%-- Hộp thoại màu --%>
    <c:set var="colorErr" value="${openDialog == 'color-dialog' ? errors : null}"/>
    <dialog class="dialog" id="color-dialog" aria-labelledby="color-dialog-title" data-reset-on-close${openDialog == 'color-dialog' ? ' data-open-on-load' : ''}>
      <form method="post" action="${ctx}/admin/attributes">
        <input type="hidden" name="action" value="saveColor">
        <input type="hidden" name="id" value="${openDialog == 'color-dialog' ? fn:escapeXml(param.id) : ''}">
        <div class="dialog__head">
          <h2 class="h-2" id="color-dialog-title" data-dialog-title>Thêm màu</h2>
          <button type="button" class="icon-btn" data-dialog-close aria-label="Đóng"><t:icon name="x" size="md"/></button>
        </div>
        <div class="dialog__body">
          <div class="field">
            <label class="field__label" for="color-name">Tên màu <span class="req" aria-hidden="true">*</span></label>
            <input class="input" type="text" id="color-name" name="name" required maxlength="50" value="${openDialog == 'color-dialog' ? fn:escapeXml(param.name) : ''}" placeholder="Ví dụ: Xanh navy" autocomplete="off">
            <c:if test="${not empty colorErr.name}"><span class="field__error"><c:out value="${colorErr.name}"/></span></c:if>
          </div>
          <div class="field">
            <label class="field__label" for="color-code">Mã màu <span class="req" aria-hidden="true">*</span></label>
            <input class="input" type="text" id="color-code" name="code" required maxlength="30" pattern="[a-z0-9]+(-[a-z0-9]+)*" value="${openDialog == 'color-dialog' ? fn:escapeXml(param.code) : ''}" placeholder="xanh-navy" data-slug-from="#color-name" autocomplete="off">
            <span class="field__hint">Mã duy nhất, tự điền theo tên. Chữ thường, số và dấu gạch ngang.</span>
            <c:if test="${not empty colorErr.code}"><span class="field__error"><c:out value="${colorErr.code}"/></span></c:if>
          </div>
          <div class="field">
            <label class="field__label" for="color-hex">Mã hex <span class="req" aria-hidden="true">*</span></label>
            <input class="input" type="color" id="color-hex" name="hexCode" required value="${openDialog == 'color-dialog' and not empty param.hexCode ? fn:escapeXml(param.hexCode) : '#cccccc'}">
            <c:if test="${not empty colorErr.hexCode}"><span class="field__error"><c:out value="${colorErr.hexCode}"/></span></c:if>
          </div>
        </div>
        <div class="dialog__foot">
          <button type="button" class="btn btn--ghost" data-dialog-close>Hủy</button>
          <button type="submit" class="btn btn--primary">Lưu màu</button>
        </div>
      </form>
    </dialog>

    <%-- Hộp thoại size --%>
    <c:set var="sizeErr" value="${openDialog == 'size-dialog' ? errors : null}"/>
    <dialog class="dialog" id="size-dialog" aria-labelledby="size-dialog-title" data-reset-on-close${openDialog == 'size-dialog' ? ' data-open-on-load' : ''}>
      <form method="post" action="${ctx}/admin/attributes">
        <input type="hidden" name="action" value="saveSize">
        <input type="hidden" name="id" value="${openDialog == 'size-dialog' ? fn:escapeXml(param.id) : ''}">
        <div class="dialog__head">
          <h2 class="h-2" id="size-dialog-title" data-dialog-title>Thêm size</h2>
          <button type="button" class="icon-btn" data-dialog-close aria-label="Đóng"><t:icon name="x" size="md"/></button>
        </div>
        <div class="dialog__body">
          <div class="field">
            <label class="field__label" for="size-name">Tên size <span class="req" aria-hidden="true">*</span></label>
            <input class="input" type="text" id="size-name" name="name" required maxlength="20" value="${openDialog == 'size-dialog' ? fn:escapeXml(param.name) : ''}" placeholder="Ví dụ: XL" autocomplete="off">
            <c:if test="${not empty sizeErr.name}"><span class="field__error"><c:out value="${sizeErr.name}"/></span></c:if>
          </div>
          <div class="field">
            <label class="field__label" for="size-order">Thứ tự hiển thị <span class="req" aria-hidden="true">*</span></label>
            <input class="input" type="number" id="size-order" name="sortOrder" required min="0" max="99" step="1" inputmode="numeric" value="${openDialog == 'size-dialog' ? fn:escapeXml(param.sortOrder) : ''}" placeholder="1">
            <span class="field__hint">Số nhỏ hiển thị trước (S = 1, M = 2...).</span>
            <c:if test="${not empty sizeErr.sortOrder}"><span class="field__error"><c:out value="${sizeErr.sortOrder}"/></span></c:if>
          </div>
        </div>
        <div class="dialog__foot">
          <button type="button" class="btn btn--ghost" data-dialog-close>Hủy</button>
          <button type="submit" class="btn btn--primary">Lưu size</button>
        </div>
      </form>
    </dialog>
  </jsp:body>
</t:admin-layout>
