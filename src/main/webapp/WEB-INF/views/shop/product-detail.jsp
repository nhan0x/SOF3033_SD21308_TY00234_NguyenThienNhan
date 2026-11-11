<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  CHI TIẾT SẢN PHẨM  (GET /product?slug=... -> ProductDetailServlet; POST /product action=review | deleteReview cũng do Servlet này xử lý)
  Thuộc tính request:
    product          Product            sản phẩm kèm colors, sizes, variants (không tìm thấy thì Servlet trả 404)
    galleryImages    List<String>       ảnh khung ảnh, tối đa 4: 2 ảnh sản phẩm rồi 2 ảnh người mẫu mặc (chưa có ảnh phụ thì 0 hoặc 1 phần tử)
    reviews          List<Review>       đánh giá hiển thị, mới nhất trước
    relatedProducts  List<Product>      sản phẩm cùng danh mục
    canReview        boolean            true khi được viết đánh giá (khách có đơn Hoàn thành chứa sản phẩm)
    myReview         Review             đánh giá của chính khách (null nếu chưa có); có thì form là form sửa
    inWishlist       boolean            sản phẩm đã có trong danh sách yêu thích
    ratingBreakdown  int[6]             số lượt đánh giá theo sao, dùng chỉ số 1..5
    reviewTotal      int                tổng số đánh giá (tổng ratingBreakdown)
    reviewAvg        double             điểm trung bình (làm tròn 1 chữ số)
    shippingFee      long               phí ship cố định
    errors           Map<String,String> (tùy chọn) lỗi form đánh giá: rating, comment
  Form:
    POST ${ctx}/cart    action=add     productId, variantId, quantity
    POST ${ctx}/product action=review        productId, slug, rating, comment (thêm hoặc sửa)
    POST ${ctx}/product action=deleteReview  productId, slug
--%>
<c:set var="soldOut" value="${product.stockTotal == 0}"/>
<t:layout title="${product.name}" nav="shop" bodyClass="pdp-page" description="${product.description}">
  <jsp:attribute name="head"><link rel="stylesheet" href="${ctx}/assets/css/pages/catalog.css?v=6"></jsp:attribute>
  <jsp:attribute name="scripts"><script src="${ctx}/assets/js/pages/catalog.js?v=2" defer></script></jsp:attribute>
  <jsp:body>

    <div class="pdp">
      <%-- Cột trái: ảnh --%>
      <div class="pdp__gallery${fn:length(galleryImages) > 1 ? ' pdp__gallery--grid' : ''} cat-enter">
        <c:choose>
          <c:when test="${not empty galleryImages}">
            <c:forEach items="${galleryImages}" var="img" varStatus="s">
              <div class="media" data-zoom><t:img src="${img}" alt="${product.name}" eager="${s.first}"/></div>
            </c:forEach>
          </c:when>
          <c:otherwise>
            <div class="media" data-zoom><t:img src="${product.image}" alt="${product.name}" eager="true"/></div>
          </c:otherwise>
        </c:choose>
      </div>

      <%-- Cột phải: thông tin --%>
      <div class="pdp__info cat-enter" style="--i:1">
        <div class="pdp__head">
          <h1 class="h-2"><c:out value="${product.name}"/></h1>
          <p class="pdp__price"><t:price value="${product.price}"/></p>
          <div class="pdp__rating">
            <c:choose>
              <c:when test="${reviewTotal > 0}">
                <t:stars rating="${reviewAvg}"/>
                <a class="t-small" href="#reviews"><fmt:formatNumber value="${reviewAvg}" minFractionDigits="1" maxFractionDigits="1"/> · ${reviewTotal} đánh giá</a>
              </c:when>
              <c:otherwise><a class="t-small" href="#reviews">Chưa có đánh giá</a></c:otherwise>
            </c:choose>
          </div>
        </div>

        <form class="pdp__form" id="add-form" method="post" action="${ctx}/cart" data-variant-form${soldOut ? ' data-sold-out' : ''}>
          <input type="hidden" name="action" value="add">
          <input type="hidden" name="productId" value="${product.id}">
          <input type="hidden" name="variantId" value="">

          <%-- Màu --%>
          <div>
            <p class="pdp__label" id="pdp-color-label"><span>Màu: <b data-selected-color><c:out value="${product.colors[0].name}"/></b></span></p>
            <div class="swatches" role="radiogroup" aria-labelledby="pdp-color-label">
              <c:forEach items="${product.colors}" var="col" varStatus="s">
                <label class="swatch" title="${fn:escapeXml(col.name)}">
                  <input type="radio" name="colorId" value="${col.id}" data-name="${fn:escapeXml(col.name)}" aria-label="${fn:escapeXml(col.name)}"${s.first ? ' checked' : ''}>
                  <span style="--sw:${fn:escapeXml(col.hexCode)}"></span>
                </label>
              </c:forEach>
            </div>
          </div>

          <%-- Size: nhãn hiện size đang chọn, bảng size mở ngay bên dưới (giống trang tham chiếu) --%>
          <div>
            <p class="pdp__label" id="pdp-size-label"><span>Size: <b data-selected-size></b></span></p>
            <div class="pill-options" role="radiogroup" aria-labelledby="pdp-size-label">
              <c:forEach items="${product.sizes}" var="sz">
                <label class="pill-option">
                  <input type="radio" name="sizeId" value="${sz.id}" data-name="${fn:escapeXml(sz.name)}" required${soldOut ? ' disabled' : ''}>
                  <span><c:out value="${sz.name}"/></span>
                </label>
              </c:forEach>
            </div>
            <p class="pdp__stock" data-stock-hint aria-live="polite"><c:choose><c:when test="${soldOut}">Sản phẩm tạm hết hàng.</c:when><c:otherwise>Chọn size để xem số lượng còn lại.</c:otherwise></c:choose></p>
          </div>

          <details class="size-guide" data-size-guide>
            <summary>
              <svg aria-hidden="true" width="15" height="15" viewBox="0 0 24 24" fill="none"><path d="M4 17L17 4L20 7L7 20L4 17Z" stroke="currentColor" stroke-width="1.8"/><path d="M14 7L17 10" stroke="currentColor" stroke-width="1.5"/><path d="M11 10L13 12" stroke="currentColor" stroke-width="1.5"/><path d="M8 13L11 16" stroke="currentColor" stroke-width="1.5"/></svg>
              <span class="size-guide__link">Bảng size</span>
            </summary>
            <div class="size-guide__content">
              <p class="size-guide__note">Số đo tính bằng cm, có thể lệch 1 đến 2 cm. Thích mặc rộng thì chọn lên một size.</p>
              <c:choose>
                <c:when test="${product.categorySlug == 'quan'}">
                  <table class="size-guide__table">
                    <tbody>
                      <tr><th>Size</th><th>S</th><th>M</th><th>L</th><th>XL</th></tr>
                      <tr><td>Vòng eo</td><td>72 - 76</td><td>77 - 81</td><td>82 - 86</td><td>87 - 91</td></tr>
                      <tr><td>Dài quần</td><td>98</td><td>100</td><td>102</td><td>104</td></tr>
                    </tbody>
                  </table>
                </c:when>
                <c:otherwise>
                  <table class="size-guide__table">
                    <tbody>
                      <tr><th>Size</th><th>S</th><th>M</th><th>L</th><th>XL</th></tr>
                      <tr><td>Vòng ngực</td><td>96</td><td>100</td><td>104</td><td>108</td></tr>
                      <tr><td>Dài áo</td><td>66</td><td>68</td><td>70</td><td>72</td></tr>
                    </tbody>
                  </table>
                </c:otherwise>
              </c:choose>
            </div>
          </details>

          <%-- Thêm vào giỏ: nút rộng hết cột. Số lượng mặc định 1, đổi trong khung giỏ hàng (#84) --%>
          <input type="hidden" name="quantity" value="1">
          <div class="pdp__buy">
            <c:choose>
              <c:when test="${soldOut}">
                <button type="button" class="btn btn--primary btn--block" disabled>Hết hàng</button>
              </c:when>
              <c:otherwise>
                <button type="submit" class="btn btn--primary btn--block" data-add-main><span data-add-label>Thêm vào giỏ</span></button>
              </c:otherwise>
            </c:choose>
          </div>
        </form>

        <div class="pdp__meta">
          <p>Phí ship cố định <t:price value="${shippingFee}"/> · Miễn phí khi dùng mã giảm giá</p>
          <button type="button" class="pdp__wish${inWishlist ? ' is-on' : ''}" data-wish data-wish-url="${ctx}/wishlist" data-product-id="${product.id}" aria-pressed="${inWishlist}"><t:icon name="heart"/><span>Yêu thích</span></button>
        </div>

        <div class="accordion-group pdp__accordions">
          <details class="accordion">
            <summary><span>Mô tả</span><svg class="accordion__arrow" aria-hidden="true" width="11" height="7" viewBox="0 0 11 7" fill="none"><path stroke="currentColor" stroke-width="1.5" d="M10 1 5.5 5.5 1 1"/></svg></summary>
            <div class="accordion__body"><p><c:out value="${product.description}"/></p></div>
          </details>
          <details class="accordion">
            <summary><span>Chất liệu</span><svg class="accordion__arrow" aria-hidden="true" width="11" height="7" viewBox="0 0 11 7" fill="none"><path stroke="currentColor" stroke-width="1.5" d="M10 1 5.5 5.5 1 1"/></svg></summary>
            <div class="accordion__body">
              <p>Vải cotton mềm, thoáng và dễ chịu khi mặc cả ngày. Thành phần chi tiết ghi trên nhãn của từng sản phẩm.</p>
            </div>
          </details>
          <details class="accordion">
            <summary><span>Hướng dẫn bảo quản</span><svg class="accordion__arrow" aria-hidden="true" width="11" height="7" viewBox="0 0 11 7" fill="none"><path stroke="currentColor" stroke-width="1.5" d="M10 1 5.5 5.5 1 1"/></svg></summary>
            <div class="accordion__body">
              <p>Giặt với nước mát, lộn trái khi giặt, không dùng chất tẩy mạnh.</p>
              <p>Phơi nơi thoáng, tránh nắng gắt để giữ màu. Ủi ở nhiệt độ thấp.</p>
            </div>
          </details>
          <details class="accordion">
            <summary><span>Giao hàng và đổi trả</span><svg class="accordion__arrow" aria-hidden="true" width="11" height="7" viewBox="0 0 11 7" fill="none"><path stroke="currentColor" stroke-width="1.5" d="M10 1 5.5 5.5 1 1"/></svg></summary>
            <div class="accordion__body">
              <p>Phí ship cố định <t:price value="${shippingFee}"/> cho mỗi đơn, giao tận nơi và thanh toán khi nhận hàng. Đơn dùng mã giảm giá hợp lệ được miễn phí ship.</p>
              <p>Bạn có thể hủy đơn khi đơn còn ở trạng thái Chờ xác nhận, trong mục Đơn hàng của tài khoản.</p>
            </div>
          </details>
        </div>
          </details>
          <details class="accordion">
            <summary><span>Vận chuyển</span><svg class="accordion__arrow" aria-hidden="true" width="11" height="7" viewBox="0 0 11 7" fill="none"><path stroke="currentColor" stroke-width="1.5" d="M10 1 5.5 5.5 1 1"/></svg></summary>
            <div class="accordion__body">
              <p>Phí ship cố định <t:price value="${shippingFee}"/> cho mỗi đơn, giao tận nơi và thanh toán khi nhận hàng.</p>
              <p>Đơn dùng mã giảm giá hợp lệ được miễn phí ship.</p>
            </div>
          </details>
        </div>
      </div>
    </div>

    <%-- Đánh giá --%>
    <section class="container section" id="reviews" aria-labelledby="reviews-title">
      <h2 class="h-1 mb-32 reveal" id="reviews-title">Đánh giá</h2>
      <%@ include file="/WEB-INF/views/common/flash.jspf" %>
      <div class="reviews-layout">
        <aside class="rating-summary reveal" aria-label="Tổng quan đánh giá">
          <div class="rating-summary__top">
            <span class="rating-summary__score"><fmt:formatNumber value="${reviewAvg}" minFractionDigits="1" maxFractionDigits="1"/></span>
            <div class="stack--sm">
              <t:stars rating="${reviewAvg}"/>
              <p class="t-small t-muted">${reviewTotal} đánh giá</p>
            </div>
          </div>
          <div class="rating-bars">
            <c:forEach begin="1" end="5" var="k">
              <c:set var="star" value="${6 - k}"/>
              <fmt:formatNumber var="pct" value="${reviewTotal > 0 ? ratingBreakdown[star] * 100 / reviewTotal : 0}" maxFractionDigits="0" groupingUsed="false"/>
              <div class="rating-row">
                <span>${star} sao</span>
                <span class="rating-row__bar" aria-hidden="true"><span style="--w:${pct}%"></span></span>
                <span>${ratingBreakdown[star]}</span>
              </div>
            </c:forEach>
          </div>
        </aside>

        <div>
          <c:choose>
            <c:when test="${not empty reviews}">
              <div class="review-list" data-stagger>
                <c:forEach items="${reviews}" var="r">
                  <article class="review-card reveal">
                    <div class="review-card__meta">
                      <span class="review-card__author"><c:out value="${r.userName}"/></span>
                      <span class="review-card__date"><fmt:formatDate value="${r.createdAt}" pattern="dd/MM/yyyy"/></span>
                    </div>
                    <t:stars rating="${r.rating}"/>
                    <p><c:out value="${r.comment}"/></p>
                  </article>
                </c:forEach>
              </div>
            </c:when>
            <c:otherwise>
              <p class="review-empty">Chưa có đánh giá nào cho sản phẩm này.</p>
            </c:otherwise>
          </c:choose>

          <c:choose>
            <c:when test="${canReview}">
              <c:set var="ratingValue" value="${not empty param.rating ? param.rating : myReview.rating}"/>
              <form class="review-form reveal" id="review-form" method="post" action="${ctx}/product">
                <input type="hidden" name="action" value="review">
                <input type="hidden" name="productId" value="${product.id}">
                <input type="hidden" name="slug" value="${fn:escapeXml(product.slug)}">
                <h3 class="h-2">${empty myReview ? 'Viết đánh giá' : 'Sửa đánh giá của bạn'}</h3>
                <c:if test="${myReview.hidden}"><p class="t-small t-muted">Đánh giá của bạn đang bị quản trị viên ẩn khỏi trang sản phẩm.</p></c:if>
                <div class="field">
                  <span class="field__label" id="rating-label">Số sao</span>
                  <div class="star-input" role="radiogroup" aria-labelledby="rating-label">
                    <c:forEach begin="1" end="5" var="k">
                      <c:set var="star" value="${6 - k}"/>
                      <input class="sr-only" type="radio" name="rating" id="rate-${star}" value="${star}" required${ratingValue == star ? ' checked' : ''}>
                      <label for="rate-${star}" title="${star} sao"><span class="sr-only">${star} sao</span><t:icon name="star"/></label>
                    </c:forEach>
                  </div>
                  <c:if test="${not empty errors.rating}"><span class="field__error">${errors.rating}</span></c:if>
                </div>
                <div class="field">
                  <label class="field__label" for="review-comment">Nhận xét</label>
                  <textarea class="textarea" id="review-comment" name="comment" required minlength="10" maxlength="1000" placeholder="Chia sẻ cảm nhận về chất vải, form dáng, màu sắc..."><c:out value="${not empty param.comment ? param.comment : myReview.comment}"/></textarea>
                  <c:if test="${not empty errors.comment}"><span class="field__error">${errors.comment}</span></c:if>
                </div>
                <div class="row row--wrap">
                  <button type="submit" class="btn btn--primary">${empty myReview ? 'Gửi đánh giá' : 'Lưu thay đổi'}</button>
                  <c:if test="${not empty myReview}"><button type="submit" form="review-delete-form" class="btn btn--ghost">Xóa đánh giá</button></c:if>
                </div>
              </form>
              <c:if test="${not empty myReview}">
                <form id="review-delete-form" method="post" action="${ctx}/product" data-confirm="Xóa đánh giá của bạn cho sản phẩm này?">
                  <input type="hidden" name="action" value="deleteReview">
                  <input type="hidden" name="productId" value="${product.id}">
                  <input type="hidden" name="slug" value="${fn:escapeXml(product.slug)}">
                </form>
              </c:if>
            </c:when>
            <c:otherwise>
              <div class="note-box review-note reveal">
                <t:icon name="info" size="md"/>
                <p class="t-small">Chỉ khách hàng có đơn đã hoàn thành chứa sản phẩm này mới có thể viết đánh giá.<c:if test="${empty sessionScope.currentUser}"> <a class="link-plain" href="${ctx}/login">Đăng nhập</a> để kiểm tra đơn hàng của bạn.</c:if></p>
              </div>
            </c:otherwise>
          </c:choose>
        </div>
      </div>
    </section>

    <%-- Sản phẩm liên quan --%>
    <c:if test="${not empty relatedProducts}">
      <section class="section--lg" data-carousel aria-labelledby="related-title">
        <div class="container section__head reveal">
          <h2 class="h-2" id="related-title">Có thể bạn cũng thích</h2>
          <div class="carousel__nav hide-mobile">
            <button type="button" class="carousel__btn" data-carousel-prev aria-label="Trước"><t:icon name="chevron-left"/></button>
            <button type="button" class="carousel__btn" data-carousel-next aria-label="Sau"><t:icon name="chevron-right"/></button>
          </div>
        </div>
        <div class="carousel__track">
          <c:forEach items="${relatedProducts}" var="p"><div class="carousel__item"><t:product-card product="${p}"/></div></c:forEach>
        </div>
        <div class="carousel__progress"><div class="carousel__bar"></div></div>
      </section>
    </c:if>

    <%-- Dữ liệu tồn kho từng biến thể cho JS (màu + size -> số lượng) --%>
    <script type="application/json" id="pdp-variants">[<c:forEach items="${product.variants}" var="v" varStatus="s">{"id":${v.id},"colorId":${v.colorId},"sizeId":${v.sizeId},"stock":${v.stock}}${s.last ? '' : ','}</c:forEach>]</script>

    <%-- Thanh thêm vào giỏ cố định (chỉ hiện trên điện thoại khi nút chính đã cuộn khỏi màn hình) --%>
    <c:if test="${not soldOut}">
      <div class="pdp-sticky" data-pdp-sticky>
        <div class="pdp-sticky__info">
          <span class="pdp-sticky__name"><c:out value="${product.name}"/></span>
          <span class="bold"><t:price value="${product.price}"/></span>
        </div>
        <button type="submit" form="add-form" class="btn btn--primary btn--sm"><span data-add-label>Thêm vào giỏ</span></button>
      </div>
    </c:if>


  </jsp:body>
</t:layout>
