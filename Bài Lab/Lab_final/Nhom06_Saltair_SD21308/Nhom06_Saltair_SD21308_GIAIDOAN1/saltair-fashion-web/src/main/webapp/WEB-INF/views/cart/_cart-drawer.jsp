<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  RUỘT KHUNG GIỎ HÀNG TRƯỢT (#84)  (GET /cart?fragment=drawer -> CartServlet, nạp bằng fetch từ app.js)
  Thuộc tính request: cart (Cart: items, subtotal, itemCount), shippingFee (long).
  Phần tử gốc mang data-count để app.js cập nhật tiêu đề "Giỏ hàng (n)" và số trên icon giỏ.
  Thao tác (app.js gửi POST /cart, Accept: application/json rồi nạp lại ruột này):
    [data-cart-qty="-1|1"], input[data-cart-input] : action=update (itemId, quantity)
    [data-cart-remove]                             : action=remove (itemId)
    form[data-cart-note]                           : action=note (note)
--%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<div class="cart-drawer__content" data-count="${cart.itemCount}">
  <c:choose>
    <c:when test="${empty cart.items}">
      <div class="cart-drawer__body cart-drawer__body--empty">
        <p class="cart-drawer__empty-title">Giỏ hàng của bạn đang trống</p>
        <a class="btn btn--primary" href="${ctx}/products">Tiếp tục mua sắm</a>
      </div>
    </c:when>
    <c:otherwise>
      <div class="cart-drawer__body">
        <div class="cart-lines">
          <c:forEach items="${cart.items}" var="item">
            <div class="cart-line" data-cart-line data-item-id="${item.id}" data-stock="${item.stock}">
              <a class="cart-line__media" href="${ctx}/product?slug=${fn:escapeXml(item.productSlug)}" tabindex="-1" aria-hidden="true"><t:img src="${item.image}" alt=""/></a>
              <div class="cart-line__main">
                <div class="cart-line__info">
                  <div class="cart-line__text">
                    <span class="cart-line__vendor">Saltair</span>
                    <a class="cart-line__name" href="${ctx}/product?slug=${fn:escapeXml(item.productSlug)}"><c:out value="${item.productName}"/></a>
                    <p class="cart-line__variant"><c:out value="${item.colorName}"/> / <c:out value="${item.sizeName}"/></p>
                  </div>
                  <div class="cart-line__controls">
                    <div class="cart-qty">
                      <button type="button" class="cart-qty__btn" data-cart-qty="-1" aria-label="Giảm số lượng"><svg aria-hidden="true" width="8" height="2" viewBox="0 0 8 2" fill="none"><path d="M0 1H8" stroke="currentColor" stroke-width="1.5"/></svg></button>
                      <input class="cart-qty__input" type="number" value="${item.quantity}" min="1" max="${item.stock > 0 ? item.stock : 99}" step="1" inputmode="numeric" data-cart-input aria-label="Số lượng của ${fn:escapeXml(item.productName)}" style="--ch:${fn:length(fn:trim(item.quantity))}ch">
                      <button type="button" class="cart-qty__btn" data-cart-qty="1" aria-label="Tăng số lượng"><svg aria-hidden="true" width="8" height="8" viewBox="0 0 8 8" fill="none"><path d="M0 4H8" stroke="currentColor" stroke-width="1.5"/><path d="M4 0V8" stroke="currentColor" stroke-width="1.5"/></svg></button>
                    </div>
                    <button type="button" class="cart-line__remove" data-cart-remove aria-label="Xóa ${fn:escapeXml(item.productName)} khỏi giỏ hàng">
                      <svg aria-hidden="true" width="16" height="17" viewBox="0 0 16 17"><path stroke="currentColor" fill="none" d="M2 4.5h12M3.5 4.5h9v10h-9v-10ZM6.5 7v5M9.5 7v5M5.5 4.5a2.5 2.5 0 1 1 5 0"/></svg><span aria-hidden="true">Xóa</span>
                    </button>
                  </div>
                  <p class="cart-line__error" data-cart-error role="alert"<c:if test="${item.quantity <= item.stock}"> hidden</c:if>><c:if test="${item.quantity > item.stock}">${item.stock > 0 ? 'Chỉ còn '.concat(item.stock).concat(' sản phẩm trong kho.') : 'Sản phẩm đã hết hàng.'}</c:if></p>
                </div>
                <span class="cart-line__price"><t:price value="${item.lineTotal}"/></span>
              </div>
            </div>
          </c:forEach>
        </div>
      </div>

      <div class="cart-drawer__foot">
        <div class="cart-drawer__actions">
          <button type="button" class="cart-drawer__action" data-cart-pop-open="cart-pop-note" aria-controls="cart-pop-note" aria-expanded="false">
            <svg aria-hidden="true" width="13" height="12" viewBox="0 0 13 12"><path stroke="currentColor" fill="none" d="M7.691 1.547h-5.41v8.906l1.875-1.172h6.563V4.845"/><path stroke="currentColor" fill="none" d="M11.187 2.667 7.665 6.19H6.31V4.836l3.523-3.524 1.354 1.355Z"/></svg>Ghi chú đơn hàng
          </button>
          <button type="button" class="cart-drawer__action" data-cart-pop-open="cart-pop-ship" aria-controls="cart-pop-ship" aria-expanded="false">
            <svg aria-hidden="true" width="12" height="12" viewBox="0 0 12 12"><path stroke="currentColor" fill="none" d="M11.5 3.5v8H.5v-8m11 0-2-3H6m5.5 3H6m-5.5 0 2-3H6m-5.5 3H6m0 0v-3"/></svg>Phí vận chuyển
          </button>
        </div>
        <div class="cart-drawer__foot-inner">
          <div class="cart-drawer__total"><span>Tổng</span><span><t:price value="${cart.subtotal}"/></span></div>
          <a class="btn btn--primary btn--block cart-drawer__checkout" href="${ctx}/checkout">Thanh toán</a>
          <p class="cart-drawer__note">Phí vận chuyển và mã giảm giá được tính ở bước thanh toán</p>
        </div>
      </div>

      <%-- Hai khung nhỏ trượt lên từ đáy khung giỏ, phủ mờ phần còn lại của khung (giống drawer popover của trang tham chiếu) --%>
      <div class="cart-pop-overlay" data-cart-pop-close></div>
      <div class="cart-pop" id="cart-pop-note" data-cart-pop role="dialog" aria-label="Ghi chú đơn hàng" aria-hidden="true">
        <div class="cart-pop__head">
          <button type="button" class="cart-drawer__close" data-cart-pop-close aria-label="Đóng"><svg aria-hidden="true" width="10" height="10" viewBox="0 0 10 10" fill="none"><path stroke="currentColor" d="m1 1 8 8m0-8L1 9"/></svg></button>
          <p class="cart-pop__title">Ghi chú đơn hàng</p>
        </div>
        <form class="cart-pop__body" data-cart-note>
          <label class="sr-only" for="cart-note-input">Ghi chú đơn hàng</label>
          <textarea class="textarea" id="cart-note-input" name="note" rows="5" maxlength="500" placeholder="Thêm ghi chú cho đơn hàng"><c:out value="${sessionScope.cartNote}"/></textarea>
          <button type="submit" class="btn btn--primary cart-pop__save">Lưu</button>
        </form>
      </div>
      <div class="cart-pop" id="cart-pop-ship" data-cart-pop role="dialog" aria-label="Phí vận chuyển" aria-hidden="true">
        <div class="cart-pop__head">
          <button type="button" class="cart-drawer__close" data-cart-pop-close aria-label="Đóng"><svg aria-hidden="true" width="10" height="10" viewBox="0 0 10 10" fill="none"><path stroke="currentColor" d="m1 1 8 8m0-8L1 9"/></svg></button>
          <p class="cart-pop__title">Phí vận chuyển</p>
        </div>
        <div class="cart-pop__body">
          <p>Phí vận chuyển cố định <strong><t:price value="${shippingFee}"/></strong> cho mọi đơn hàng trên toàn quốc.</p>
          <p>Miễn phí vận chuyển khi bạn dùng mã giảm giá hợp lệ ở bước thanh toán.</p>
        </div>
      </div>
    </c:otherwise>
  </c:choose>
</div>
