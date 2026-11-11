<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  GIỎ HÀNG  (GET /cart -> CartServlet)
  Xem thử: /cart?empty=1 (giỏ trống), /cart?lowstock=1 (có dòng vượt tồn kho)
  Thuộc tính request:
    cart        : Cart         - giỏ hàng, có items (List<CartItem>), subtotal, itemCount
    shippingFee : long         - phí ship cố định (AppConstant.SHIPPING_FEE)
    suggestions : List<Product> - sản phẩm gợi ý ở cuối trang
  Form (POST /cart): action=update (itemId, quantity) | action=remove (itemId)
--%>
<t:layout title="Giỏ hàng" nav="shop">
  <jsp:attribute name="head"><link rel="stylesheet" href="${ctx}/assets/css/pages/shopping.css?v=5"></jsp:attribute>
  <jsp:attribute name="scripts"><script src="${ctx}/assets/js/pages/shopping.js" defer></script></jsp:attribute>
  <jsp:body>
    <c:set var="cartEmpty" value="${empty cart or empty cart.items}"/>

    <div class="sh-head sh-enter">
      <h1 class="h-1">Giỏ hàng</h1>
      <span class="sh-head__count t-muted" data-head-count<c:if test="${cartEmpty}"> hidden</c:if>>${cart.itemCount} sản phẩm</span>
    </div>

    <div class="sh-flash"><%@ include file="/WEB-INF/views/common/flash.jspf" %></div>

    <%-- Trạng thái giỏ trống (cũng được JS hiện ra khi xóa dòng cuối cùng) --%>
    <div class="empty-state sh-enter" style="--d:1" data-cart-empty<c:if test="${not cartEmpty}"> hidden</c:if>>
      <t:icon name="bag"/>
      <h2 class="h-2">Giỏ hàng của bạn đang trống</h2>
      <p>Chọn vài món basic nhẹ và dễ phối, rồi quay lại đây để thanh toán.</p>
      <a class="btn btn--primary" href="${ctx}/products">Bắt đầu mua sắm</a>
    </div>

    <c:if test="${not cartEmpty}">
      <c:set var="overStock" value="false"/>
      <c:forEach items="${cart.items}" var="item">
        <c:if test="${item.quantity > item.stock}"><c:set var="overStock" value="true"/></c:if>
      </c:forEach>

      <div class="two-col" data-cart>
        <%-- Cột trái: danh sách dòng hàng --%>
        <section aria-label="Sản phẩm trong giỏ">
          <div class="sh-lines">
            <c:forEach items="${cart.items}" var="item" varStatus="s">
              <article class="line-item sh-line sh-enter" style="--d:${s.index + 1}" data-cart-item data-unit="${item.unitPrice}" data-stock="${item.stock}">
                <a class="thumb" href="${ctx}/product?slug=${fn:escapeXml(item.productSlug)}"><t:img src="${item.image}" alt="${item.productName}"/></a>

                <div class="sh-line__info">
                  <a class="h-3 link-underline" href="${ctx}/product?slug=${fn:escapeXml(item.productSlug)}"><c:out value="${item.productName}"/></a>
                  <p class="t-small t-muted">Màu: <c:out value="${item.colorName}"/> · Size: <c:out value="${item.sizeName}"/></p>
                  <p class="t-small"><t:price value="${item.unitPrice}"/></p>

                  <form class="sh-line__form" method="post" action="${ctx}/cart" data-cart-update>
                    <input type="hidden" name="action" value="update">
                    <input type="hidden" name="itemId" value="${item.id}">
                    <div class="qty">
                      <button type="button" data-qty-minus aria-label="Giảm số lượng"><t:icon name="minus"/></button>
                      <input type="number" name="quantity" value="${item.quantity}" min="1" max="${item.stock > 0 ? item.stock : 99}" inputmode="numeric" aria-label="Số lượng của ${fn:escapeXml(item.productName)}" required>
                      <button type="button" data-qty-plus aria-label="Tăng số lượng"><t:icon name="plus"/></button>
                    </div>
                  </form>

                  <p class="sh-warn" data-stock-warn role="alert"<c:if test="${item.quantity <= item.stock}"> hidden</c:if>>
                    <t:icon name="alert"/>
                    <span>Kho chỉ còn ${item.stock} sản phẩm cho lựa chọn này. Vui lòng giảm số lượng.</span>
                  </p>
                </div>

                <div class="sh-line__side">
                  <span class="sh-line__total" data-line-total><t:price value="${item.lineTotal}"/></span>
                  <form method="post" action="${ctx}/cart" data-cart-remove data-confirm="Xóa “${fn:escapeXml(item.productName)}” khỏi giỏ hàng?">
                    <input type="hidden" name="action" value="remove">
                    <input type="hidden" name="itemId" value="${item.id}">
                    <button type="submit" class="icon-btn" aria-label="Xóa ${fn:escapeXml(item.productName)} khỏi giỏ hàng"><t:icon name="trash"/></button>
                  </form>
                </div>
              </article>
            </c:forEach>
          </div>
        </section>

        <%-- Cột phải: tóm tắt --%>
        <aside class="summary sh-summary sh-enter" style="--d:2" aria-label="Tóm tắt đơn hàng" data-cart-summary data-shipping="${shippingFee}">
          <h2 class="h-2">Tóm tắt đơn hàng</h2>
          <div class="summary__row"><span>Tạm tính (<span data-sum-count>${cart.itemCount} sản phẩm</span>)</span><span data-sum-subtotal><t:price value="${cart.subtotal}"/></span></div>
          <div class="summary__row"><span>Phí vận chuyển</span><span><t:price value="${shippingFee}"/></span></div>
          <p class="sh-summary__hint">Miễn phí vận chuyển khi bạn dùng mã giảm giá hợp lệ ở bước thanh toán.</p>
          <div class="summary__row summary__total"><span>Tổng tạm tính</span><span data-sum-total><t:price value="${cart.subtotal + shippingFee}"/></span></div>

          <p class="sh-warn" data-overstock-note<c:if test="${not overStock}"> hidden</c:if>><t:icon name="alert"/><span>Một số sản phẩm vượt quá tồn kho. Hãy chỉnh số lượng trước khi thanh toán.</span></p>

          <div class="sh-summary__actions">
            <a class="btn btn--primary btn--block${overStock ? ' is-disabled' : ''}" href="${ctx}/checkout" data-checkout-btn<c:if test="${overStock}"> aria-disabled="true"</c:if>>Thanh toán</a>
            <a class="link-underline" href="${ctx}/products">Tiếp tục mua sắm</a>
          </div>

          <ul class="sh-perks">
            <li><t:icon name="banknote"/><span>Thanh toán khi nhận hàng (COD)</span></li>
            <li><t:icon name="truck"/><span>Phí ship cố định 30.000đ, miễn phí khi dùng mã giảm giá</span></li>
          </ul>
        </aside>
      </div>
    </c:if>

    <%-- Gợi ý --%>
    <c:if test="${not empty suggestions}">
      <section class="section--lg sh-suggest" aria-labelledby="sh-suggest-title">
        <div class="container section__head reveal">
          <h2 class="h-1" id="sh-suggest-title">Có thể bạn sẽ thích</h2>
          <a class="btn btn--outline" href="${ctx}/products">Xem tất cả</a>
        </div>
        <div class="product-grid" data-stagger>
          <c:forEach items="${suggestions}" var="p"><div class="reveal"><t:product-card product="${p}"/></div></c:forEach>
        </div>
      </section>
    </c:if>
  </jsp:body>
</t:layout>
