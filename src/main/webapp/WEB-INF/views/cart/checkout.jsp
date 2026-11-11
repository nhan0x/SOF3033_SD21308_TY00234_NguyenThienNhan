<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  THANH TOÁN  (GET /checkout -> CheckoutServlet)  [#86: bố cục hai cột theo trang tham chiếu]
  Khung trang tối giản (t:layout chrome="checkout"): chỉ có logo, không menu, không footer đầy đủ.
  Thuộc tính request:
    guest           : boolean         - true khi khách chưa đăng nhập: ô email bắt buộc, không có sổ địa chỉ
    cart            : Cart            - giỏ hàng (items, subtotal, itemCount)
    addresses       : List<Address>   - sổ địa chỉ của khách (rỗng nếu guest)
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
  Form (một form duy nhất, nút nào bấm thì gửi action của nút đó; áp mã hay gỡ mã đều giữ nguyên dữ liệu đã nhập):
    POST /checkout action=placeOrder    : email (chỉ khi guest), addressId ("new" = địa chỉ khác), receiverName, receiverPhone,
                                          addressLine, ward, district, city, saveAddress (tùy chọn), voucherCode, note
    POST /checkout action=applyVoucher  : voucherInput (mã mới nhập) + các field trên
    POST /checkout action=removeVoucher : các field trên
--%>
<t:layout title="Thanh toán" nav="shop" chrome="checkout" bodyClass="co-page">
  <jsp:attribute name="head"><link rel="stylesheet" href="${ctx}/assets/css/pages/shopping.css?v=5"></jsp:attribute>
  <jsp:attribute name="scripts"><script src="${ctx}/assets/js/pages/shopping.js?v=3" defer></script></jsp:attribute>
  <jsp:body>
    <c:set var="posted" value="${not empty param.action}"/>
    <c:set var="pickNew" value="${guest or param.addressId == 'new' or empty addresses}"/>
    <c:set var="pickId" value="${empty param.addressId ? defaultAddress.id : param.addressId}"/>
    <c:set var="me" value="${sessionScope.currentUser}"/>

    <form class="co" id="checkout-form" method="post" action="${ctx}/checkout" data-checkout-form data-success-url="${ctx}/order-success">
      <input type="hidden" name="paymentMethod" value="COD">
      <c:if test="${not empty appliedVoucher}"><input type="hidden" name="voucherCode" value="${fn:escapeXml(appliedVoucher.code)}"></c:if>

      <%-- Điện thoại: nút gập mở tóm tắt đơn nằm trên cùng, tóm tắt mở ra ngay bên dưới --%>
      <button type="button" class="co-toggle" data-co-toggle aria-expanded="false" aria-controls="co-summary">
        <span class="co-toggle__label"><span data-co-toggle-text>Hiện tóm tắt đơn hàng</span> <t:icon name="chevron-down" size="sm"/></span>
        <span class="co-toggle__total"><t:price value="${total}"/></span>
      </button>

      <%-- Cột trái: form --%>
      <div class="co__main">
        <div class="co__main-inner">
          <%@ include file="/WEB-INF/views/common/flash.jspf" %>

          <%-- Liên hệ --%>
          <section class="co-section" aria-labelledby="co-contact">
            <div class="co-section__head">
              <h2 class="co-h" id="co-contact">Liên hệ</h2>
              <c:if test="${guest}"><a class="co-link" href="${ctx}/login?redirect=%2Fcheckout">Đăng nhập</a></c:if>
            </div>
            <c:choose>
              <c:when test="${guest}">
                <div class="co-field${empty errors.email ? '' : ' is-invalid'}">
                  <input id="f-email" type="email" name="email" value="${fn:escapeXml(param.email)}" maxlength="150" autocomplete="email" placeholder=" " required aria-describedby="f-email-hint">
                  <label for="f-email">Email</label>
                </div>
                <c:choose>
                  <c:when test="${not empty errors.email}"><p class="co-error" id="f-email-hint"><c:out value="${errors.email}"/></p></c:when>
                  <c:otherwise><p class="co-hint" id="f-email-hint">Dùng để xem lại đơn hàng: đăng nhập bằng email này và mã 6 số.</p></c:otherwise>
                </c:choose>
              </c:when>
              <c:otherwise>
                <div class="co-account">
                  <span class="co-account__avatar" aria-hidden="true"><t:icon name="user"/></span>
                  <span class="co-account__text"><span class="co-muted">Tài khoản</span><strong><c:out value="${me.email}"/></strong></span>
                </div>
              </c:otherwise>
            </c:choose>
          </section>

          <%-- Giao hàng --%>
          <section class="co-section" aria-labelledby="co-delivery">
            <div class="co-section__head">
              <h2 class="co-h" id="co-delivery">Giao hàng</h2>
              <c:if test="${not guest}"><a class="co-link" href="${ctx}/addresses">Sổ địa chỉ</a></c:if>
            </div>

            <c:if test="${guest}"><input type="hidden" name="addressId" value="new"></c:if>
            <c:if test="${not guest}">
              <div class="co-choices" role="radiogroup" aria-labelledby="co-delivery">
                <c:forEach items="${addresses}" var="a">
                  <label class="co-choice">
                    <input type="radio" name="addressId" value="${a.id}"<c:if test="${not pickNew and fn:trim(a.id) == fn:trim(pickId)}"> checked</c:if>>
                    <span class="co-choice__body">
                      <span class="co-choice__title"><c:out value="${a.receiverName}"/> · <c:out value="${a.phone}"/><c:if test="${a.defaultAddress}"> <span class="co-tag">Mặc định</span></c:if></span>
                      <span class="co-muted"><c:out value="${a.fullAddress}"/></span>
                    </span>
                  </label>
                </c:forEach>
                <label class="co-choice">
                  <input type="radio" name="addressId" value="new"<c:if test="${pickNew}"> checked</c:if>>
                  <span class="co-choice__body"><span class="co-choice__title">Dùng địa chỉ khác</span></span>
                </label>
              </div>
              <c:if test="${not empty errors.addressId}"><p class="co-error"><c:out value="${errors.addressId}"/></p></c:if>
            </c:if>

            <div class="co-grid" data-new-address<c:if test="${not pickNew}"> hidden</c:if>>
              <div class="co-cell co-cell--full">
                <div class="co-field${empty errors.receiverName ? '' : ' is-invalid'}">
                  <input id="f-receiverName" type="text" name="receiverName" value="${fn:escapeXml(param.receiverName)}" maxlength="100" autocomplete="name" placeholder=" " data-req>
                  <label for="f-receiverName">Họ và tên người nhận</label>
                </div>
                <c:if test="${not empty errors.receiverName}"><p class="co-error"><c:out value="${errors.receiverName}"/></p></c:if>
              </div>
              <div class="co-cell co-cell--full">
                <div class="co-field${empty errors.addressLine ? '' : ' is-invalid'}">
                  <input id="f-addressLine" type="text" name="addressLine" value="${fn:escapeXml(param.addressLine)}" maxlength="200" autocomplete="address-line1" placeholder=" " data-req>
                  <label for="f-addressLine">Số nhà, tên đường</label>
                </div>
                <c:if test="${not empty errors.addressLine}"><p class="co-error"><c:out value="${errors.addressLine}"/></p></c:if>
              </div>
              <div class="co-cell">
                <div class="co-field">
                  <input id="f-ward" type="text" name="ward" value="${fn:escapeXml(param.ward)}" maxlength="100" placeholder=" ">
                  <label for="f-ward">Phường, xã</label>
                </div>
              </div>
              <div class="co-cell">
                <div class="co-field">
                  <input id="f-district" type="text" name="district" value="${fn:escapeXml(param.district)}" maxlength="100" placeholder=" ">
                  <label for="f-district">Quận, huyện</label>
                </div>
              </div>
              <div class="co-cell co-cell--full">
                <div class="co-field${empty errors.city ? '' : ' is-invalid'}">
                  <input id="f-city" type="text" name="city" value="${fn:escapeXml(param.city)}" maxlength="100" autocomplete="address-level1" placeholder=" " data-req>
                  <label for="f-city">Tỉnh, thành phố</label>
                </div>
                <c:if test="${not empty errors.city}"><p class="co-error"><c:out value="${errors.city}"/></p></c:if>
              </div>
              <div class="co-cell co-cell--full">
                <div class="co-field${empty errors.receiverPhone ? '' : ' is-invalid'}">
                  <input id="f-receiverPhone" type="tel" name="receiverPhone" value="${fn:escapeXml(param.receiverPhone)}" pattern="0[0-9]{9}" maxlength="10" inputmode="numeric" autocomplete="tel" placeholder=" " data-req>
                  <label for="f-receiverPhone">Số điện thoại</label>
                </div>
                <c:if test="${not empty errors.receiverPhone}"><p class="co-error"><c:out value="${errors.receiverPhone}"/></p></c:if>
              </div>
              <c:if test="${not guest}">
                <label class="co-check co-cell--full">
                  <input type="checkbox" name="saveAddress" value="1"<c:if test="${param.saveAddress == '1'}"> checked</c:if>>
                  <span>Lưu địa chỉ này vào sổ địa chỉ</span>
                </label>
              </c:if>
            </div>

            <h3 class="co-h co-h--sub">Phương thức vận chuyển</h3>
            <div class="co-choice co-choice--static">
              <span class="co-choice__body"><span class="co-choice__title">Giao hàng tiêu chuẩn</span><span class="co-muted">Giao trong 2 đến 4 ngày làm việc</span></span>
              <strong><c:choose><c:when test="${shippingFee == 0}">Miễn phí</c:when><c:otherwise><t:price value="${shippingFee}"/></c:otherwise></c:choose></strong>
            </div>
          </section>

          <%-- Thanh toán --%>
          <section class="co-section" aria-labelledby="co-payment">
            <div class="co-section__head"><h2 class="co-h" id="co-payment">Thanh toán</h2></div>
            <p class="co-muted co-section__lead">Bạn chỉ thanh toán khi nhận hàng.</p>
            <div class="co-pay">
              <label class="co-choice co-choice--on">
                <input type="radio" name="paymentMethodView" value="COD" checked>
                <span class="co-choice__body"><span class="co-choice__title">Thanh toán khi nhận hàng (COD)</span></span>
                <t:icon name="banknote"/>
              </label>
              <div class="co-pay__detail">Bạn trả tiền mặt cho nhân viên giao hàng khi nhận đơn. Hiện Saltair chỉ hỗ trợ hình thức này.</div>
            </div>

            <h3 class="co-h co-h--sub">Ghi chú</h3>
            <div class="co-field co-field--area${empty errors.note ? '' : ' is-invalid'}">
              <textarea id="f-note" name="note" maxlength="500" rows="3" placeholder=" "><c:out value="${posted ? param.note : sessionScope.cartNote}"/></textarea>
              <label for="f-note">Lời nhắn cho người giao hàng (không bắt buộc)</label>
            </div>
            <c:if test="${not empty errors.note}"><p class="co-error"><c:out value="${errors.note}"/></p></c:if>
          </section>

          <button type="submit" name="action" value="placeOrder" class="co-submit" data-co-submit>
            <span class="co-submit__label">Đặt hàng</span><span class="spinner" aria-hidden="true"></span>
          </button>

          <footer class="co-foot">
            <a class="co-link" href="${ctx}/cart">Quay lại giỏ hàng</a>
            <a class="co-link" href="${ctx}/products">Tiếp tục mua sắm</a>
          </footer>
        </div>
      </div>

      <%-- Cột phải: tóm tắt đơn --%>
      <aside class="co__aside" id="co-summary" aria-label="Tóm tắt đơn hàng" data-co-summary>
        <div class="co__aside-inner">
          <div class="co-items">
            <c:forEach items="${cart.items}" var="item">
              <div class="co-item">
                <div class="co-item__media">
                  <t:img src="${item.image}" alt=""/>
                  <span class="co-item__qty" aria-label="Số lượng ${item.quantity}">${item.quantity}</span>
                </div>
                <div class="co-item__text">
                  <span class="co-item__name"><c:out value="${item.productName}"/></span>
                  <span class="co-muted co-small"><c:out value="${item.colorName}"/> / <c:out value="${item.sizeName}"/></span>
                </div>
                <span class="co-item__price"><t:price value="${item.lineTotal}"/></span>
              </div>
            </c:forEach>
          </div>

          <%-- Mã giảm giá --%>
          <div class="co-voucher">
            <c:choose>
              <c:when test="${not empty appliedVoucher}">
                <div class="co-voucher__applied" role="status">
                  <span class="co-voucher__chip"><t:icon name="tag" size="sm"/><c:out value="${appliedVoucher.code}"/>
                    <button type="submit" name="action" value="removeVoucher" formnovalidate aria-label="Gỡ mã ${fn:escapeXml(appliedVoucher.code)}"><t:icon name="x" size="sm"/></button>
                  </span>
                </div>
              </c:when>
              <c:otherwise>
                <div class="co-voucher__row">
                  <div class="co-field${empty voucherError ? '' : ' is-invalid'}">
                    <input id="f-voucher" type="text" name="voucherInput" value="${fn:escapeXml(voucherCode)}" maxlength="30" autocomplete="off" autocapitalize="characters" placeholder=" " data-voucher-input>
                    <label for="f-voucher">Mã giảm giá</label>
                  </div>
                  <button type="submit" name="action" value="applyVoucher" formnovalidate class="co-voucher__btn" data-voucher-apply>Áp dụng</button>
                </div>
                <c:if test="${not empty voucherError}"><p class="co-error" role="alert"><c:out value="${voucherError}"/></p></c:if>
                <c:if test="${not empty hintVouchers}">
                  <div class="co-voucher__hints">
                    <c:forEach items="${hintVouchers}" var="v">
                      <button type="button" class="co-voucher__hint" data-voucher-fill="${fn:escapeXml(v.code)}" title="${fn:escapeXml(v.description)}"><strong><c:out value="${v.code}"/></strong></button>
                    </c:forEach>
                  </div>
                </c:if>
              </c:otherwise>
            </c:choose>
          </div>

          <div class="co-totals">
            <div class="co-row"><span>Tạm tính · ${cart.itemCount} sản phẩm</span><span><t:price value="${subtotal}"/></span></div>
            <c:if test="${discountAmount > 0}">
              <div class="co-row"><span>Giảm giá (<c:out value="${appliedVoucher.code}"/>)</span><span>-<t:price value="${discountAmount}"/></span></div>
            </c:if>
            <div class="co-row"><span>Vận chuyển</span><span><c:choose><c:when test="${shippingFee == 0}">Miễn phí</c:when><c:otherwise><t:price value="${shippingFee}"/></c:otherwise></c:choose></span></div>
            <div class="co-row co-row--total"><span>Tổng cộng</span><span><small>VND</small> <t:price value="${total}"/></span></div>
          </div>
        </div>
      </aside>
    </form>
  </jsp:body>
</t:layout>
