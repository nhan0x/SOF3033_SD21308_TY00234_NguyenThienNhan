<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  ADMIN - MÃ GIẢM GIÁ (VOUCHER)
  Route:   GET /admin/vouchers
  Servlet: com.fashionweb.controller.admin.sales.AdminVoucherServlet (active menu: vouchers)
  Request attributes:
    vouchers   : List<Voucher>  - toàn bộ voucher; mỗi Voucher cần usedCount, categoryName ("Tất cả danh mục" khi categoryId null) và statusLabel
                                  (Đang hoạt động | Hết hạn | Hết lượt | Tạm dừng | Chưa bắt đầu)
    categories : List<Category> - danh mục cho ô chọn "Áp dụng cho"
    errors     : Map<String,String> - (tùy chọn) lỗi sau khi POST thất bại: code, discountType, discountValue, maxDiscount,
                                      minOrderAmount, categoryId, startDate, endDate, usageLimit, perUserLimit
  Forms (POST ${ctx}/admin/vouchers):
    action=save          fields: id (rỗng = thêm mới), code, description, discountType, discountValue, maxDiscount, minOrderAmount,
                                 categoryId (rỗng = mọi danh mục), startDate, endDate (yyyy-MM-ddTHH:mm), usageLimit, perUserLimit, active
    action=toggleActive  fields: id - bật hoặc vô hiệu hóa
    action=delete        fields: id - chỉ khi usedCount = 0
--%>
<t:admin-layout title="Mã giảm giá" active="vouchers">
  <jsp:attribute name="head"><link rel="stylesheet" href="${ctx}/assets/css/pages/admin-ops.css"></jsp:attribute>
  <jsp:attribute name="scripts"><script src="${ctx}/assets/js/pages/admin-ops.js" defer></script></jsp:attribute>
  <jsp:attribute name="actions"><button class="btn btn--primary btn--sm js-fill" type="button" data-dialog-open="voucher-dialog" data-dialog-title="Thêm voucher"><t:icon name="plus"/> Thêm voucher</button></jsp:attribute>
  <jsp:body>
    <%@ include file="/WEB-INF/views/common/flash.jspf" %>

    <div class="alert alert--info rule-box ops-enter">
      <t:icon name="info"/>
      <div>
        <p class="bold">Quy tắc áp dụng voucher</p>
        <ul class="rule-list">
          <li><t:icon name="check" size="sm"/><span>Mỗi đơn hàng chỉ dùng một voucher.</span></li>
          <li><t:icon name="check" size="sm"/><span>Mức giảm tính trên tổng tiền hàng của cả đơn, không chỉ sản phẩm thuộc danh mục áp dụng.</span></li>
          <li><t:icon name="check" size="sm"/><span>Đơn dùng voucher hợp lệ được miễn phí vận chuyển.</span></li>
          <li><t:icon name="check" size="sm"/><span>Voucher đã có người dùng thì không xóa được, chỉ tạm dừng.</span></li>
        </ul>
      </div>
    </div>

    <div class="admin-card admin-card--flush ops-enter">
      <div class="admin-toolbar">
        <p class="t-small t-muted">Khách nhập mã ở bước thanh toán. Voucher tạm dừng hoặc hết hạn sẽ báo lỗi cho khách.</p>
        <p class="t-small t-muted">${fn:length(vouchers)} voucher</p>
      </div>

      <div class="admin-table-wrap">
        <table class="table responsive ops-table">
          <caption class="sr-only">Danh sách mã giảm giá</caption>
          <thead>
            <tr>
              <th scope="col">Mã</th>
              <th scope="col">Mô tả</th>
              <th scope="col">Mức giảm</th>
              <th scope="col" class="num">Đơn tối thiểu</th>
              <th scope="col">Áp dụng cho</th>
              <th scope="col">Hiệu lực</th>
              <th scope="col">Lượt dùng</th>
              <th scope="col">Trạng thái</th>
              <th scope="col"><span class="sr-only">Thao tác</span></th>
            </tr>
          </thead>
          <tbody>
            <c:forEach items="${vouchers}" var="v">
              <fmt:formatDate var="startFmt" value="${v.startDate}" pattern="yyyy-MM-dd'T'HH:mm"/>
              <fmt:formatDate var="endFmt" value="${v.endDate}" pattern="yyyy-MM-dd'T'HH:mm"/>
              <c:set var="pct" value="${v.usageLimit gt 0 ? (v.usedCount * 100 / v.usageLimit) : 0}"/>
              <c:set var="statusClass" value="${v.statusLabel == 'Đang hoạt động' ? 'badge--success' : (v.statusLabel == 'Hết lượt' ? 'badge--pending' : (v.statusLabel == 'Chưa bắt đầu' ? 'badge--shipping' : 'badge--muted'))}"/>
              <tr>
                <td class="cell-lead">
                  <span class="code-chip">
                    <c:out value="${v.code}"/>
                    <button class="icon-btn icon-btn--sm" type="button" data-copy="${fn:escapeXml(v.code)}" aria-label="Sao chép mã ${fn:escapeXml(v.code)}" title="Sao chép mã"><t:icon name="copy" size="sm"/></button>
                  </span>
                </td>
                <td data-label="Mô tả"><span class="cell-clamp"><c:out value="${v.description}"/></span></td>
                <td data-label="Mức giảm">
                  <div class="cell-stack">
                    <c:choose>
                      <c:when test="${v.discountType == 'PERCENT'}">
                        <span class="bold">${v.discountValue}%</span>
                        <small><c:choose><c:when test="${not empty v.maxDiscount}">Tối đa <t:price value="${v.maxDiscount}"/></c:when><c:otherwise>Không giới hạn mức giảm</c:otherwise></c:choose></small>
                      </c:when>
                      <c:otherwise>
                        <span class="bold"><t:price value="${v.discountValue}"/></span>
                        <small>Giảm cố định</small>
                      </c:otherwise>
                    </c:choose>
                  </div>
                </td>
                <td data-label="Đơn tối thiểu" class="num cell-nowrap">
                  <c:choose><c:when test="${v.minOrderAmount gt 0}"><t:price value="${v.minOrderAmount}"/></c:when><c:otherwise><span class="t-muted">Không</span></c:otherwise></c:choose>
                </td>
                <td data-label="Áp dụng cho"><c:out value="${empty v.categoryName ? 'Tất cả danh mục' : v.categoryName}"/></td>
                <td data-label="Hiệu lực">
                  <div class="cell-stack cell-nowrap">
                    <span><fmt:formatDate value="${v.startDate}" pattern="dd/MM/yyyy HH:mm"/></span>
                    <small>đến <fmt:formatDate value="${v.endDate}" pattern="dd/MM/yyyy HH:mm"/></small>
                  </div>
                </td>
                <td data-label="Lượt dùng">
                  <div class="usage">
                    <span class="t-small bold">${v.usedCount} / ${v.usageLimit}</span>
                    <div class="progress-row__bar" aria-hidden="true"><span style="--v:${pct gt 100 ? 100 : pct}"></span></div>
                    <span class="t-small t-muted">Mỗi khách tối đa ${v.perUserLimit} lần</span>
                  </div>
                </td>
                <td data-label="Trạng thái"><span class="badge ${statusClass}"><c:out value="${v.statusLabel}"/></span></td>
                <td>
                  <div class="cell-actions">
                    <button class="icon-btn js-fill" type="button" aria-label="Sửa voucher ${fn:escapeXml(v.code)}" title="Sửa"
                            data-dialog-open="voucher-dialog" data-dialog-title="Sửa voucher"
                            data-field-id="${v.id}" data-field-code="${fn:escapeXml(v.code)}" data-field-description="${fn:escapeXml(v.description)}"
                            data-field-discount-type="${v.discountType}" data-field-discount-value="${v.discountValue}" data-field-max-discount="${v.maxDiscount}"
                            data-field-min-order-amount="${v.minOrderAmount}" data-field-category-id="${v.categoryId}"
                            data-field-start-date="${startFmt}" data-field-end-date="${endFmt}"
                            data-field-usage-limit="${v.usageLimit}" data-field-per-user-limit="${v.perUserLimit}" data-field-active="${v.active}"><t:icon name="pencil"/></button>
                    <form method="post" action="${ctx}/admin/vouchers">
                      <input type="hidden" name="action" value="toggleActive">
                      <input type="hidden" name="id" value="${v.id}">
                      <button class="btn btn--ghost btn--xs" type="submit" aria-label="${v.active ? 'Tạm dừng' : 'Kích hoạt'} voucher ${fn:escapeXml(v.code)}">${v.active ? 'Tạm dừng' : 'Kích hoạt'}</button>
                    </form>
                    <c:choose>
                      <c:when test="${v.usedCount gt 0}">
                        <span title="Voucher đã có người dùng nên không thể xóa, chỉ có thể tạm dừng.">
                          <button class="icon-btn icon-btn--danger" type="button" disabled aria-label="Không thể xóa voucher ${fn:escapeXml(v.code)} vì đã có người dùng"><t:icon name="trash"/></button>
                        </span>
                      </c:when>
                      <c:otherwise>
                        <form method="post" action="${ctx}/admin/vouchers" data-confirm="Xóa voucher ${fn:escapeXml(v.code)}? Thao tác này không thể hoàn tác.">
                          <input type="hidden" name="action" value="delete">
                          <input type="hidden" name="id" value="${v.id}">
                          <button class="icon-btn icon-btn--danger" type="submit" aria-label="Xóa voucher ${fn:escapeXml(v.code)}" title="Xóa"><t:icon name="trash"/></button>
                        </form>
                      </c:otherwise>
                    </c:choose>
                  </div>
                </td>
              </tr>
            </c:forEach>
            <c:if test="${empty vouchers}">
              <tr class="empty-row"><td colspan="9">Chưa có voucher nào. Bấm Thêm voucher để tạo mã đầu tiên.</td></tr>
            </c:if>
          </tbody>
        </table>
      </div>
    </div>

    <%-- Hộp thoại thêm hoặc sửa voucher --%>
    <dialog class="dialog dialog--wide" id="voucher-dialog" aria-labelledby="voucher-dialog-title" data-auto-open="${not empty errors}">
      <form method="post" action="${ctx}/admin/vouchers" data-voucher-form>
        <input type="hidden" name="action" value="save">
        <input type="hidden" name="id" value="${fn:escapeXml(param.id)}">
        <div class="dialog__head">
          <h2 class="h-2" id="voucher-dialog-title" data-dialog-title>${empty param.id ? 'Thêm voucher' : 'Sửa voucher'}</h2>
          <button class="icon-btn" type="button" data-dialog-close aria-label="Đóng"><t:icon name="x" size="md"/></button>
        </div>
        <div class="dialog__body">
          <div class="form-grid">
            <div class="field">
              <label class="field__label" for="voucher-code">Mã voucher <span class="req" aria-hidden="true">*</span></label>
              <input class="input input--upper${not empty errors.code ? ' is-invalid' : ''}" id="voucher-code" type="text" name="code" value="${fn:escapeXml(param.code)}" required pattern="[A-Z0-9]{4,20}" minlength="4" maxlength="20" placeholder="SALE10" autocomplete="off" title="Từ 4 đến 20 ký tự gồm chữ in hoa không dấu và số">
              <span class="field__hint">4 đến 20 ký tự, chữ in hoa không dấu và số. Không sửa được sau khi tạo.</span>
              <c:if test="${not empty errors.code}"><span class="field__error"><c:out value="${errors.code}"/></span></c:if>
            </div>
            <div class="field">
              <label class="field__label" for="voucher-category">Áp dụng cho</label>
              <select class="select${not empty errors.categoryId ? ' is-invalid' : ''}" id="voucher-category" name="categoryId">
                <option value="">Tất cả danh mục</option>
                <c:forEach items="${categories}" var="cat">
                  <option value="${cat.id}"${param.categoryId eq ''.concat(cat.id) ? ' selected' : ''}><c:out value="${cat.name}"/></option>
                </c:forEach>
              </select>
              <span class="field__hint">Đơn cần có ít nhất một sản phẩm thuộc danh mục này.</span>
              <c:if test="${not empty errors.categoryId}"><span class="field__error"><c:out value="${errors.categoryId}"/></span></c:if>
            </div>

            <div class="field span-2">
              <label class="field__label" for="voucher-desc">Mô tả</label>
              <input class="input" id="voucher-desc" type="text" name="description" value="${fn:escapeXml(param.description)}" maxlength="255" placeholder="Ví dụ: Giảm 10% cho khách hàng mới">
            </div>

            <div class="field">
              <label class="field__label" for="voucher-type">Loại giảm giá <span class="req" aria-hidden="true">*</span></label>
              <select class="select${not empty errors.discountType ? ' is-invalid' : ''}" id="voucher-type" name="discountType" required>
                <option value="PERCENT"${param.discountType != 'AMOUNT' ? ' selected' : ''}>Phần trăm (%)</option>
                <option value="AMOUNT"${param.discountType == 'AMOUNT' ? ' selected' : ''}>Số tiền cố định (đ)</option>
              </select>
              <c:if test="${not empty errors.discountType}"><span class="field__error"><c:out value="${errors.discountType}"/></span></c:if>
            </div>
            <div class="field">
              <label class="field__label" for="voucher-value">Giá trị giảm <span class="req" aria-hidden="true">*</span> <span class="t-muted" data-discount-unit>(từ 1 đến 100 %)</span></label>
              <input class="input${not empty errors.discountValue ? ' is-invalid' : ''}" id="voucher-value" type="number" name="discountValue" value="${fn:escapeXml(param.discountValue)}" required min="1" step="1" inputmode="numeric" placeholder="10">
              <c:if test="${not empty errors.discountValue}"><span class="field__error"><c:out value="${errors.discountValue}"/></span></c:if>
            </div>

            <div class="field">
              <label class="field__label" for="voucher-max">Giảm tối đa (đ)</label>
              <input class="input${not empty errors.maxDiscount ? ' is-invalid' : ''}" id="voucher-max" type="number" name="maxDiscount" value="${fn:escapeXml(param.maxDiscount)}" min="1000" step="1000" inputmode="numeric" placeholder="50000">
              <span class="field__hint">Chỉ dùng cho giảm theo phần trăm. Để trống nếu không giới hạn.</span>
              <c:if test="${not empty errors.maxDiscount}"><span class="field__error"><c:out value="${errors.maxDiscount}"/></span></c:if>
            </div>
            <div class="field">
              <label class="field__label" for="voucher-min">Đơn tối thiểu (đ)</label>
              <input class="input${not empty errors.minOrderAmount ? ' is-invalid' : ''}" id="voucher-min" type="number" name="minOrderAmount" value="${empty param.minOrderAmount ? '0' : fn:escapeXml(param.minOrderAmount)}" min="0" step="1000" inputmode="numeric">
              <span class="field__hint">Tổng tiền hàng của cả đơn phải từ mức này. Nhập 0 nếu không yêu cầu.</span>
              <c:if test="${not empty errors.minOrderAmount}"><span class="field__error"><c:out value="${errors.minOrderAmount}"/></span></c:if>
            </div>

            <div class="field">
              <label class="field__label" for="voucher-start">Bắt đầu <span class="req" aria-hidden="true">*</span></label>
              <input class="input${not empty errors.startDate ? ' is-invalid' : ''}" id="voucher-start" type="datetime-local" name="startDate" value="${fn:escapeXml(param.startDate)}" required>
              <c:if test="${not empty errors.startDate}"><span class="field__error"><c:out value="${errors.startDate}"/></span></c:if>
            </div>
            <div class="field">
              <label class="field__label" for="voucher-end">Kết thúc <span class="req" aria-hidden="true">*</span></label>
              <input class="input${not empty errors.endDate ? ' is-invalid' : ''}" id="voucher-end" type="datetime-local" name="endDate" value="${fn:escapeXml(param.endDate)}" required>
              <c:if test="${not empty errors.endDate}"><span class="field__error"><c:out value="${errors.endDate}"/></span></c:if>
            </div>

            <div class="field">
              <label class="field__label" for="voucher-limit">Tổng lượt dùng <span class="req" aria-hidden="true">*</span></label>
              <input class="input${not empty errors.usageLimit ? ' is-invalid' : ''}" id="voucher-limit" type="number" name="usageLimit" value="${empty param.usageLimit ? '100' : fn:escapeXml(param.usageLimit)}" required min="1" step="1" inputmode="numeric">
              <c:if test="${not empty errors.usageLimit}"><span class="field__error"><c:out value="${errors.usageLimit}"/></span></c:if>
            </div>
            <div class="field">
              <label class="field__label" for="voucher-per-user">Số lần mỗi khách được dùng <span class="req" aria-hidden="true">*</span></label>
              <input class="input${not empty errors.perUserLimit ? ' is-invalid' : ''}" id="voucher-per-user" type="number" name="perUserLimit" value="${empty param.perUserLimit ? '1' : fn:escapeXml(param.perUserLimit)}" required min="1" step="1" inputmode="numeric">
              <c:if test="${not empty errors.perUserLimit}"><span class="field__error"><c:out value="${errors.perUserLimit}"/></span></c:if>
            </div>

            <div class="switch-row span-2">
              <span class="switch-row__text"><span class="bold" id="voucher-active-label">Kích hoạt voucher</span><span class="t-small t-muted">Tắt để tạm dừng, khách nhập mã sẽ nhận thông báo voucher bị vô hiệu hóa.</span></span>
              <label class="switch"><input type="checkbox" name="active" value="true" aria-labelledby="voucher-active-label" checked></label>
            </div>
          </div>
        </div>
        <div class="dialog__foot">
          <button class="btn btn--ghost btn--sm" type="button" data-dialog-close>Hủy</button>
          <button class="btn btn--primary btn--sm" type="submit">Lưu voucher</button>
        </div>
      </form>
    </dialog>
  </jsp:body>
</t:admin-layout>
