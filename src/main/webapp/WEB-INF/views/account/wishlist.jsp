<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  YÊU THÍCH  (GET /wishlist -> WishlistServlet)  [Customer, cần đăng nhập]
  Request attributes:
    wishlistProducts : List<Product>   - sản phẩm yêu thích (danh sách rỗng = trạng thái trống)
    message / messageType              - (tùy chọn) thông báo chung, xem flash.jspf
  Tham số URL: empty=1 (chỉ để xem trạng thái trống)
  Forms:
    POST /wishlist  action=remove  fields=productId
--%>
<t:account-layout title="Yêu thích" active="wishlist" description="Những sản phẩm Saltair bạn đã lưu lại.">
  <jsp:attribute name="scripts"><script src="${ctx}/assets/js/pages/account.js" defer></script></jsp:attribute>
  <jsp:body>
        <div class="acct-head acc-enter">
          <div>
            <h1 class="acct-title">Yêu thích</h1>
            <p class="t-muted"><span data-demo-count>${fn:length(wishlistProducts)}</span> sản phẩm bạn đã lưu lại.</p>
          </div>
          <a class="btn btn--outline" href="${ctx}/products">Tiếp tục mua sắm</a>
        </div>

        <%@ include file="/WEB-INF/views/common/flash.jspf" %>

        <c:if test="${not empty wishlistProducts}">
          <div class="product-grid wish-grid" data-stagger>
            <c:forEach items="${wishlistProducts}" var="p" varStatus="s">
              <div class="wish-item ${s.index < 3 ? 'acc-enter' : 'reveal'}" style="--d:${s.index + 1}">
                <t:product-card product="${p}"/>
                <form class="product-card__wish" method="post" action="${ctx}/wishlist">
                  <input type="hidden" name="action" value="remove">
                  <input type="hidden" name="productId" value="${p.id}">
                  <button type="submit" class="icon-btn wish-item__btn" aria-label="Bỏ khỏi yêu thích: ${fn:escapeXml(p.name)}"><t:icon name="heart"/></button>
                </form>
              </div>
            </c:forEach>
          </div>
        </c:if>

        <div class="empty-state" data-demo-empty${empty wishlistProducts ? '' : ' hidden'}>
          <t:icon name="heart"/>
          <h2 class="h-2">Chưa có sản phẩm yêu thích</h2>
          <p>Bấm vào biểu tượng trái tim trên sản phẩm bạn thích để lưu lại và xem sau.</p>
          <a class="btn btn--primary" href="${ctx}/products">Khám phá sản phẩm</a>
        </div>
  </jsp:body>
</t:account-layout>
