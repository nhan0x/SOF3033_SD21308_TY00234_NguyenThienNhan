<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  THANH TOÁN  (GET /checkout -> CheckoutServlet)
  Xem thử mã giảm giá: /checkout?voucher=WELCOME10 (hợp lệ) hoặc ?voucher=HETHAN20 (báo lỗi)
  Thuộc tính request:
    cart            : Cart            - giỏ hàng (items, subtotal, itemCount)
    addresses       : List<Address>   - sổ địa chỉ của khách
    defaultAddress  : Address | null  - địa chỉ mặc định (chọn sẵn)
    subtotal        : long            - tiền hàng
    shippingFee     : long            - 30.000 hoặc 0 khi có voucher hợp lệ
    discountAmount  : long            - số tiền được giảm (0 nếu không có voucher)
    total           : long            - tiền hàng - giảm giá + phí ship
    appliedVoucher  : Voucher | null  - voucher đã áp dụng
    hintVouchers    : List<Voucher>   - voucher đang dùng được, hiện thành chip gợi ý
    voucherCode     : String          - mã khách vừa nhập (giữ lại trong ô nhập khi báo lỗi)
    voucherError    : String | absent - lý do mã không hợp lệ
    errors          : Map<String,String> | absent - lỗi từng field khi đặt hàng thất bại
  Form:
    POST /checkout action=applyVoucher   fields: voucherCode
    POST /checkout action=placeOrder     fields: addressId ("new" = địa chỉ khác), receiverName, receiverPhone, addressLine,
                                                 ward, district, city, voucherCode, paymentMethod, note
  Các field của form đặt hàng nằm rải ở nhiều khối nên gắn vào form bằng thuộc tính form="checkout-form".
--%>
<t:layout title="Thanh toán" nav="shop">
  <jsp:attribute name="head"><link rel="stylesheet" href="${ctx}/assets/css/pages/shopping.css"></jsp:attribute>
  <jsp:attribute name="scripts"><script src="${ctx}/assets/js/pages/shopping.js" defer></script></jsp:attribute>
  <jsp:body>
    <c:set var="pickNew" value="${param.addressId == 'new' or empty addresses}"/>
    <c:set var="pickId" value="${empty param.addressId ? defaultAddress.id : param.addressId}"/>

    <div class="sh-head sh-enter">
      <h1 class="h-1">Thanh toán</h1>
      <span class="sh-head__count t-muted">${cart.itemCount} sản phẩm</span>
    </div>

    <div class="sh-flash"><%@ include file="/WEB-INF/views/common/flash.jspf" %></div>

    <%-- Form đặt hàng (rỗng); các ô nhập bên dưới gắn vào bằng form="checkout-form" --%>
    <form id="checkout-form" method="post" action="${ctx}/checkout" data-checkout-form data-success-url="${ctx}/order-success">
      <input type="hidden" name="action" value="placeOrder">
      <input type="hidden" name="paymentMethod" value="COD">
      <c:if test="${not empty appliedVoucher}"><input type="hidden" name="voucherCode" value="${fn:escapeXml(appliedVoucher.code)}"></c:if>
    </form>

    <div class="two-col">
      <div class="sh-steps">

        <%-- 1. Thông tin nhận hàng --%>
        <section class="sh-step sh-enter" style="--d:1" aria-labelledby="sh-s1">
          <div class="sh-step__head">
            <span class="sh-step__num" aria-hidden="true">1</span>
            <h2 class="h-2" id="sh-s1">Thông tin nhận hàng</h2>
            <a class="link-underline" href="${ctx}/addresses">Sổ địa chỉ</a>
          </div>

          <div class="sh-options" role="radiogroup" aria-labelledby="sh-s1">
            <c:forEach items="${addresses}" var="a">
              <label class="radio sh-option">
                <input type="radio" name="addressId" value="${a.id}" form="checkout-form" required<c:if test="${not pickNew and fn:trim(a.id) == fn:trim(pickId)}"> checked</c:if>>
                <span class="sh-option__body">
                  <span class="sh-option__title"><c:out value="${a.receiverName}"/><c:if test="${a.defaultAddress}"> <span class="badge badge--muted">Mặc định</span></c:if></span>
                  <span class="sh-option__text"><c:out value="${a.phone}"/></span>
                  <span class="sh-option__text"><c:out value="${a.fullAddress}"/></span>
                </span>
              </label>
            </c:forEach>

            <label class="radio sh-option">
              <input type="radio" name="addressId" value="new" form="checkout-form" required<c:if test="${pickNew}"> checked</c:if>>
              <span class="sh-option__body">
                <span class="sh-option__title">Dùng địa chỉ khác</span>
                <span class="sh-option__text">Giao đến người nhận hoặc địa chỉ khác với sổ địa chỉ.</span>
              </span>
            </label>
          </div>
          <c:if test="${not empty errors.addressId}"><span class="field__error"><c:out value="${errors.addressId}"/></span></c:if>

          <div class="sh-newaddr" data-new-address<c:if test="${not pickNew}"> hidden</c:if>>
            <div class="form-grid">
              <div class="field">
                <label class="field__label" for="f-receiverName">Họ và tên người nhận</label>
                <input class="input${empty errors.receiverName ? '' : ' is-invalid'}" id="f-receiverName" type="text" name="receiverName" form="checkout-form" value="${fn:escapeXml(param.receiverName)}" maxlength="100" autocomplete="name" data-req>
                <c:if test="${not empty errors.receiverName}"><span class="field__error"><c:out value="${errors.receiverName}"/></span></c:if>
              </div>
              <div class="field">
                <label class="field__label" for="f-receiverPhone">Số điện thoại</label>
                <input class="input${empty errors.receiverPhone ? '' : ' is-invalid'}" id="f-receiverPhone" type="tel" name="receiverPhone" form="checkout-form" value="${fn:escapeXml(param.receiverPhone)}" pattern="0[0-9]{9}" maxlength="10" inputmode="numeric" autocomplete="tel" placeholder="0900000000" data-req>
                <c:if test="${not empty errors.receiverPhone}"><span class="field__error"><c:out value="${errors.receiverPhone}"/></span></c:if>
              </div>
              <div class="field span-2">
                <label class="field__label" for="f-addressLine">Số nhà, tên đường</label>
                <input class="input${empty errors.addressLine ? '' : ' is-invalid'}" id="f-addressLine" type="text" name="addressLine" form="checkout-form" value="${fn:escapeXml(param.addressLine)}" maxlength="255" autocomplete="address-line1" data-req>
                <c:if test="${not empty errors.addressLine}"><span class="field__error"><c:out value="${errors.addressLine}"/></span></c:if>
              </div>
              <div class="field">
                <label class="field__label" for="f-ward">Phường, xã</label>
                <input class="input" id="f-ward" type="text" name="ward" form="checkout-form" value="${fn:escapeXml(param.ward)}" maxlength="100">
              </div>
              <div class="field">
                <label class="field__label" for="f-district">Quận, huyện</label>
                <input class="input" id="f-district" type="text" name="district" form="checkout-form" value="${fn:escapeXml(param.district)}" maxlength="100">
              </div>
              <div class="field span-2">
                <label class="field__label" for="f-city">Tỉnh, thành phố</label>
                <input class="input${empty errors.city ? '' : ' is-invalid'}" id="f-city" type="text" name="city" form="checkout-form" value="${fn:escapeXml(param.city)}" maxlength="100" autocomplete="address-level1" data-req>
                <c:if test="${not empty errors.city}"><span class="field__error"><c:out value="${errors.city}"/></span></c:if>
              </div>
            </div>
          </div>
        </section>

        <%-- 2. Mã giảm giá --%>
        <section class="sh-step sh-enter" style="--d:2" aria-labelledby="sh-s2">
          <div class="sh-step__head">
            <span class="sh-step__num" aria-hidden="true">2</span>
            <h2 class="h-2" id="sh-s2">Mã giảm giá</h2>
          </div>

          <c:choose>
            <c:when test="${not empty appliedVoucher}">
              <div class="sh-voucher-applied" role="status">
                <div class="sh-voucher-applied__main">
                  <span class="badge badge--success"><t:icon name="check" size="sm"/>&nbsp;<c:out value="${appliedVoucher.code}"/></span>
                  <span class="t-small">Giảm <t:price value="${discountAmount}"/> và miễn phí vận chuyển</span>
                </div>
                <a class="link-underline" href="${ctx}/checkout">Gỡ mã</a>
              </div>
            </c:when>
            <c:otherwise>
              <form class="sh-voucher-form" method="post" action="${ctx}/checkout" data-voucher-form>
                <input type="hidden" name="action" value="applyVoucher">
                <div class="field">
                  <label class="sr-only" for="f-voucherCode">Mã giảm giá</label>
                  <input class="input${empty voucherError ? '' : ' is-invalid'}" id="f-voucherCode" type="text" name="voucherCode" value="${fn:escapeXml(voucherCode)}" placeholder="Nhập mã giảm giá" maxlength="30" autocomplete="off" autocapitalize="characters" required>
                  <c:if test="${not empty voucherError}"><span class="field__error" role="alert"><c:out value="${voucherError}"/></span></c:if>
                </div>
                <button type="submit" class="btn btn--outline">Áp dụng</button>
              </form>

              <c:if test="${not empty hintVouchers}">
                <div class="sh-chips">
                  <span class="t-small t-muted">Mã bạn có thể dùng:</span>
                  <c:forEach items="${hintVouchers}" var="v">
                    <button type="button" class="chip sh-voucher-chip" data-copy="${fn:escapeXml(v.code)}" data-voucher-fill="${fn:escapeXml(v.code)}" aria-label="Sao chép và dùng mã ${fn:escapeXml(v.code)}">
                      <t:icon name="copy" size="sm"/><strong><c:out value="${v.code}"/></strong><span><c:out value="${v.description}"/></span>
                    </button>
                  </c:forEach>
                </div>
              </c:if>
            </c:otherwise>
          </c:choose>
        </section>

        <%-- 3. Phương thức thanh toán --%>
        <section class="sh-step sh-enter" style="--d:3" aria-labelledby="sh-s3">
          <div class="sh-step__head">
            <span class="sh-step__num" aria-hidden="true">3</span>
            <h2 class="h-2" id="sh-s3">Phương thức thanh toán</h2>
          </div>
          <div class="sh-options">
            <label class="radio sh-option sh-option--static">
              <input type="radio" name="paymentMethodView" value="COD" checked disabled>
              <span class="sh-option__body">
                <span class="sh-option__title">Thanh toán khi nhận hàng (COD)</span>
                <span class="sh-option__text">Bạn trả tiền mặt cho nhân viên giao hàng khi nhận đơn. Hiện Saltair chỉ hỗ trợ hình thức này.</span>
              </span>
              <span class="sh-option__icon"><t:icon name="banknote"/></span>
            </label>
          </div>
        </section>

        <%-- 4. Ghi chú --%>
        <section class="sh-step sh-enter" style="--d:4" aria-labelledby="sh-s4">
          <div class="sh-step__head">
            <span class="sh-step__num" aria-hidden="true">4</span>
            <h2 class="h-2" id="sh-s4">Ghi chú</h2>
          </div>
          <div class="field">
            <label class="field__label" for="f-note">Lời nhắn cho người giao hàng (không bắt buộc)</label>
            <textarea class="textarea${empty errors.note ? '' : ' is-invalid'}" id="f-note" name="note" form="checkout-form" maxlength="500" placeholder="Ví dụ: giao giờ hành chính, gọi trước khi đến"><c:out value="${param.note}"/></textarea>
            <c:if test="${not empty errors.note}"><span class="field__error"><c:out value="${errors.note}"/></span></c:if>
          </div>
        </section>
      </div>

      <%-- Cột phải: tóm tắt đơn --%>
      <aside class="summary sh-summary sh-enter" style="--d:2" aria-label="Đơn hàng của bạn">
        <div class="sh-summary__head">
          <h2 class="h-2">Đơn hàng của bạn</h2>
          <a class="link-underline t-small" href="${ctx}/cart">Sửa giỏ hàng</a>
        </div>

        <div class="sh-mini">
          <c:forEach items="${cart.items}" var="item">
            <div class="sh-mini__item">
              <div class="sh-mini__media">
                <div class="thumb"><t:img src="${item.image}" alt="${item.productName}"/></div>
                <span class="sh-mini__qty" aria-label="Số lượng ${item.quantity}">${item.quantity}</span>
              </div>
              <div>
                <span class="sh-mini__name"><c:out value="${item.productName}"/></span>
                <span class="sh-mini__meta"><c:out value="${item.colorName}"/> · <c:out value="${item.sizeName}"/></span>
              </div>
              <span class="sh-mini__price"><t:price value="${item.lineTotal}"/></span>
            </div>
          </c:forEach>
        </div>

        <hr>
        <div class="summary__row"><span>Tạm tính</span><span><t:price value="${subtotal}"/></span></div>
        <c:if test="${discountAmount > 0}">
          <div class="summary__row sh-summary__discount"><span>Giảm giá (<c:out value="${appliedVoucher.code}"/>)</span><span>-<t:price value="${discountAmount}"/></span></div>
        </c:if>
        <div class="summary__row"><span>Phí vận chuyển</span><span><c:choose><c:when test="${shippingFee == 0}">Miễn phí</c:when><c:otherwise><t:price value="${shippingFee}"/></c:otherwise></c:choose></span></div>
        <div class="summary__row summary__total"><span>Tổng cộng</span><span><t:price value="${total}"/></span></div>

        <button type="submit" form="checkout-form" class="btn btn--primary btn--block">Đặt hàng</button>
        <p class="sh-summary__note">Bạn chỉ thanh toán khi nhận hàng.</p>
      </aside>
    </div>
  </jsp:body>
</t:layout>
