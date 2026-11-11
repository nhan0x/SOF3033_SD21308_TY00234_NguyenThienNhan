<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  ADMIN - CHI TIẾT ĐƠN HÀNG
  Route:   GET /admin/order?code=
  Servlet: com.fashionweb.controller.admin.sales.AdminOrderDetailServlet (active menu: orders). Không tìm thấy đơn thì trả 404.
  Request attributes:
    order  : Order               - đơn kèm items (List<OrderDetail>), customerName, customerEmail, voucherCode, cancelReason, cancelledBy
    errors : Map<String,String>  - (tùy chọn) lỗi theo ô, hiện tại chỉ có khóa cancelReason
  Forms (POST ${ctx}/admin/order):
    action=updateStatus  fields: orderCode, status (SHIPPING | COMPLETED)
    action=cancel        fields: orderCode, cancelReason
--%>
<t:admin-layout title="Đơn ${order.orderCode}" active="orders">
  <jsp:attribute name="head"><link rel="stylesheet" href="${ctx}/assets/css/pages/admin-ops.css"></jsp:attribute>
  <jsp:attribute name="scripts"><script src="${ctx}/assets/js/pages/admin-ops.js" defer></script></jsp:attribute>
  <jsp:attribute name="actions"><a class="btn btn--ghost btn--sm" href="${ctx}/admin/orders"><t:icon name="arrow-left"/> Danh sách đơn</a></jsp:attribute>
  <jsp:body>
    <%@ include file="/WEB-INF/views/common/flash.jspf" %>

    <div class="order-head ops-enter">
      <div class="order-head__meta">
        <span class="badge badge--${order.status.cssClass}">${order.status.label}</span>
        <span class="badge ${order.paymentStatus == 'PAID' ? 'badge--success' : 'badge--muted'}">${order.paymentStatus.label}</span>
        <span class="t-small t-muted"><t:icon name="clock"/> Đặt lúc <fmt:formatDate value="${order.createdAt}" pattern="dd/MM/yyyy HH:mm"/></span>
        <c:if test="${not empty order.voucherCode}">
          <span class="t-small t-muted"><t:icon name="percent"/> Mã giảm giá <strong><c:out value="${order.voucherCode}"/></strong></span>
        </c:if>
      </div>
    </div>

    <div class="admin-grid admin-grid--main">
      <%-- Cột trái: tiến trình, sản phẩm, tổng tiền --%>
      <div class="order-stack">
        <section class="admin-card ops-enter" aria-labelledby="order-progress-title">
          <h2 class="admin-card__title" id="order-progress-title">Tiến trình đơn hàng</h2>
          <c:choose>
            <c:when test="${order.status == 'CANCELLED'}">
              <ol class="timeline timeline--2">
                <li class="timeline__step is-done">
                  <span class="timeline__label">Chờ xác nhận</span>
                  <span class="timeline__time"><fmt:formatDate value="${order.createdAt}" pattern="dd/MM/yyyy HH:mm"/></span>
                </li>
                <li class="timeline__step is-cancelled">
                  <span class="timeline__label">Đã hủy</span>
                  <span class="timeline__time"><fmt:formatDate value="${order.updatedAt}" pattern="dd/MM/yyyy HH:mm"/></span>
                </li>
              </ol>
            </c:when>
            <c:otherwise>
              <ol class="timeline timeline--3">
                <li class="timeline__step is-done">
                  <span class="timeline__label">Chờ xác nhận</span>
                  <span class="timeline__time"><fmt:formatDate value="${order.createdAt}" pattern="dd/MM/yyyy HH:mm"/></span>
                </li>
                <li class="timeline__step${order.status == 'SHIPPING' or order.status == 'COMPLETED' ? ' is-done' : ''}">
                  <span class="timeline__label">Đang giao</span>
                </li>
                <li class="timeline__step${order.status == 'COMPLETED' ? ' is-done' : ''}">
                  <span class="timeline__label">Hoàn thành</span>
                  <c:if test="${order.status == 'COMPLETED' and not empty order.completedAt}">
                    <span class="timeline__time"><fmt:formatDate value="${order.completedAt}" pattern="dd/MM/yyyy HH:mm"/></span>
                  </c:if>
                </li>
              </ol>
            </c:otherwise>
          </c:choose>
        </section>

        <section class="admin-card admin-card--flush reveal" aria-labelledby="order-items-title">
          <h2 class="admin-card__title" id="order-items-title">Sản phẩm (${order.itemCount})</h2>
          <div class="admin-table-wrap">
            <table class="table responsive">
              <caption class="sr-only">Sản phẩm trong đơn</caption>
              <thead>
                <tr>
                  <th scope="col">Sản phẩm</th>
                  <th scope="col">Màu / Size</th>
                  <th scope="col" class="num">Đơn giá</th>
                  <th scope="col" class="num">SL</th>
                  <th scope="col" class="num">Thành tiền</th>
                </tr>
              </thead>
              <tbody>
                <c:forEach items="${order.items}" var="it">
                  <tr>
                    <td class="cell-lead">
                      <div class="cell-product">
                        <div class="thumb"><t:img src="${it.image}" alt="${it.productName}"/></div>
                        <a class="link-underline" href="${ctx}/product?slug=${fn:escapeXml(it.productSlug)}"><c:out value="${it.productName}"/></a>
                      </div>
                    </td>
                    <td data-label="Màu / Size"><c:out value="${it.colorName}"/> / <c:out value="${it.sizeName}"/></td>
                    <td data-label="Đơn giá" class="num cell-nowrap"><t:price value="${it.unitPrice}"/></td>
                    <td data-label="SL" class="num">${it.quantity}</td>
                    <td data-label="Thành tiền" class="num cell-nowrap bold"><t:price value="${it.lineTotal}"/></td>
                  </tr>
                </c:forEach>
                <c:if test="${empty order.items}">
                  <tr class="empty-row"><td colspan="5">Đơn này chưa có sản phẩm.</td></tr>
                </c:if>
              </tbody>
            </table>
          </div>
          <div class="summary" aria-label="Tổng tiền đơn hàng">
            <div class="summary__row"><span>Tạm tính</span><span><t:price value="${order.subtotal}"/></span></div>
            <div class="summary__row">
              <span class="t-muted">Giảm giá<c:if test="${not empty order.voucherCode}"> <span class="badge badge--muted"><c:out value="${order.voucherCode}"/></span></c:if></span>
              <span><c:if test="${order.discountAmount > 0}">- </c:if><t:price value="${order.discountAmount}"/></span>
            </div>
            <div class="summary__row">
              <span>Phí vận chuyển</span>
              <span><c:choose><c:when test="${order.shippingFee == 0}">Miễn phí</c:when><c:otherwise><t:price value="${order.shippingFee}"/></c:otherwise></c:choose></span>
            </div>
            <div class="summary__row summary__total"><span>Tổng cộng</span><span><t:price value="${order.totalAmount}"/></span></div>
          </div>
        </section>
      </div>

      <%-- Cột phải: xử lý đơn, khách hàng, giao hàng, thanh toán, lý do hủy --%>
      <div class="order-stack" data-stagger>
        <section class="admin-card ops-enter" aria-labelledby="order-action-title">
          <h2 class="admin-card__title" id="order-action-title">Xử lý đơn</h2>
          <div class="action-stack">
            <c:choose>
              <c:when test="${order.status == 'PENDING'}">
                <form method="post" action="${ctx}/admin/order">
                  <input type="hidden" name="action" value="updateStatus">
                  <input type="hidden" name="orderCode" value="${fn:escapeXml(order.orderCode)}">
                  <input type="hidden" name="status" value="SHIPPING">
                  <button class="btn btn--primary btn--block" type="submit">Xác nhận và giao hàng</button>
                </form>
                <p class="action-note">Đơn sẽ chuyển sang Đang giao. Hãy kiểm tra hàng và địa chỉ trước khi xác nhận.</p>
              </c:when>
              <c:when test="${order.status == 'SHIPPING'}">
                <form method="post" action="${ctx}/admin/order">
                  <input type="hidden" name="action" value="updateStatus">
                  <input type="hidden" name="orderCode" value="${fn:escapeXml(order.orderCode)}">
                  <input type="hidden" name="status" value="COMPLETED">
                  <button class="btn btn--primary btn--block" type="submit">Đánh dấu hoàn thành</button>
                </form>
                <p class="action-note">Khách đã nhận hàng và thanh toán. Đơn hoàn thành sẽ được ghi nhận là Đã thanh toán.</p>
              </c:when>
              <c:when test="${order.status == 'COMPLETED'}">
                <p class="action-note">Đơn đã hoàn thành, không thể thay đổi trạng thái.</p>
              </c:when>
              <c:otherwise>
                <p class="action-note">Đơn đã hủy, không thể thay đổi trạng thái.</p>
              </c:otherwise>
            </c:choose>
            <c:if test="${order.status == 'PENDING' or order.status == 'SHIPPING'}">
              <button class="btn btn--danger btn--block" type="button" data-dialog-open="cancel-dialog">Hủy đơn</button>
            </c:if>
          </div>
        </section>

        <section class="admin-card reveal" aria-labelledby="order-customer-title">
          <h2 class="admin-card__title" id="order-customer-title">Khách hàng</h2>
          <dl class="kv">
            <div><dt>Họ tên</dt><dd><c:out value="${order.customerName}"/></dd></div>
            <div><dt>Email</dt><dd><a href="mailto:${fn:escapeXml(order.customerEmail)}"><c:out value="${order.customerEmail}"/></a></dd></div>
            <div><dt>Số điện thoại</dt><dd><a href="tel:${fn:escapeXml(order.receiverPhone)}"><c:out value="${order.receiverPhone}"/></a></dd></div>
          </dl>
        </section>

        <section class="admin-card reveal" aria-labelledby="order-ship-title">
          <h2 class="admin-card__title" id="order-ship-title">Địa chỉ giao hàng</h2>
          <dl class="kv">
            <div><dt>Người nhận</dt><dd><c:out value="${order.receiverName}"/></dd></div>
            <div><dt>Địa chỉ</dt><dd><c:out value="${order.shippingAddress}"/></dd></div>
            <c:if test="${not empty order.note}">
              <div><dt>Ghi chú của khách</dt><dd><c:out value="${order.note}"/></dd></div>
            </c:if>
          </dl>
        </section>

        <section class="admin-card reveal" aria-labelledby="order-pay-title">
          <h2 class="admin-card__title" id="order-pay-title">Thanh toán</h2>
          <dl class="kv">
            <div><dt>Phương thức</dt><dd>${order.paymentMethod.label}</dd></div>
            <div><dt>Trạng thái</dt><dd><span class="badge ${order.paymentStatus == 'PAID' ? 'badge--success' : 'badge--muted'}">${order.paymentStatus.label}</span></dd></div>
          </dl>
        </section>

        <c:if test="${order.status == 'CANCELLED'}">
          <section class="admin-card reveal" aria-labelledby="order-cancel-title">
            <h2 class="admin-card__title" id="order-cancel-title">Thông tin hủy đơn</h2>
            <dl class="kv">
              <div><dt>Hủy bởi</dt><dd>${order.cancelledBy.label}</dd></div>
              <div><dt>Lý do</dt><dd><c:out value="${order.cancelReason}"/></dd></div>
              <div><dt>Thời gian</dt><dd><fmt:formatDate value="${order.updatedAt}" pattern="dd/MM/yyyy HH:mm"/></dd></div>
            </dl>
          </section>
        </c:if>
      </div>
    </div>

    <%-- Hộp thoại hủy đơn --%>
    <c:if test="${order.status == 'PENDING' or order.status == 'SHIPPING'}">
      <dialog class="dialog" id="cancel-dialog" aria-labelledby="cancel-dialog-title" data-auto-open="${not empty errors.cancelReason}">
        <form method="post" action="${ctx}/admin/order" data-confirm="Hủy đơn ${fn:escapeXml(order.orderCode)}? Tồn kho và lượt dùng voucher (nếu có) sẽ được trả lại.">
          <input type="hidden" name="action" value="cancel">
          <input type="hidden" name="orderCode" value="${fn:escapeXml(order.orderCode)}">
          <div class="dialog__head">
            <h2 class="h-2" id="cancel-dialog-title" data-dialog-title>Hủy đơn hàng</h2>
            <button class="icon-btn" type="button" data-dialog-close aria-label="Đóng"><t:icon name="x" size="md"/></button>
          </div>
          <div class="dialog__body">
            <div class="field">
              <label class="field__label" for="cancelReason">Lý do hủy đơn <span class="req" aria-hidden="true">*</span></label>
              <textarea class="textarea" id="cancelReason" name="cancelReason" rows="4" required minlength="5" maxlength="255" placeholder="Ví dụ: hết hàng màu đã chọn, khách yêu cầu hủy..."><c:out value="${param.cancelReason}"/></textarea>
              <span class="field__hint">Lý do sẽ được lưu cùng đơn hàng để khách hàng xem lại.</span>
              <c:if test="${not empty errors.cancelReason}"><span class="field__error"><c:out value="${errors.cancelReason}"/></span></c:if>
            </div>
          </div>
          <div class="dialog__foot">
            <button class="btn btn--ghost btn--sm" type="button" data-dialog-close>Không hủy</button>
            <button class="btn btn--danger btn--sm" type="submit">Hủy đơn</button>
          </div>
        </form>
      </dialog>
    </c:if>
  </jsp:body>
</t:admin-layout>
