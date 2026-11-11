<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  TRANG CHỦ  (GET /home -> HomeServlet)
  Dữ liệu: banners (đang bật, theo sort_order), newProducts, featuredProducts, bestSellers, tileGroups, latestReviews,
           reviewAvg, reviewTotal, latestBlogs, splitImage1, splitImage2
  Mục nào không có dữ liệu thì ẩn.
--%>
<t:layout title="Trang chủ" nav="home" overlay="true" description="Saltair - đồ basic phong cách biển, nhẹ, thoáng và dễ phối.">

  <%-- 1. Hero --%>
  <c:if test="${not empty banners}">
  <section class="hero" data-hero-slider aria-label="Nổi bật">
    <div class="hero__slides">
      <c:forEach items="${banners}" var="b" varStatus="s">
        <div class="hero__slide${s.first ? ' is-active' : ''}">
          <div class="hero__media"><t:img src="${b.image}" alt="${b.title}" dark="true" eager="${s.first}"/></div>
          <div class="hero__content">
            <p class="hero__label entrance">Bộ sưu tập mới</p>
            <c:choose><c:when test="${s.first}"><h1 class="h-1 hero__title entrance"><c:out value="${b.title}"/></h1></c:when><c:otherwise><p class="h-1 hero__title entrance"><c:out value="${b.title}"/></p></c:otherwise></c:choose>
            <div class="hero__cta entrance"><c:set var="bLink" value="${empty b.linkUrl ? '/products' : b.linkUrl}"/><a class="btn btn--white" href="${fn:escapeXml(fn:startsWith(bLink, 'http') ? bLink : ctx.concat(bLink))}">Mua ngay</a></div>
          </div>
        </div>
      </c:forEach>
    </div>
    <c:if test="${fn:length(banners) > 1}">
      <div class="hero__dots">
        <c:forEach items="${banners}" var="b" varStatus="s"><button type="button" class="hero__dot${s.first ? ' is-active' : ''}" aria-label="Banner ${s.count}"></button></c:forEach>
      </div>
    </c:if>
  </section>
  </c:if>

  <%-- 2. Dải lợi ích --%>
  <section class="usp" aria-label="Lợi ích">
    <c:if test="${reviewTotal > 0}"><div class="usp__item reveal"><t:icon name="star"/><p class="usp__title"><fmt:formatNumber value="${reviewAvg}" minFractionDigits="1" maxFractionDigits="1"/>/5 từ khách hàng</p><p class="usp__text">Dựa trên ${reviewTotal} đánh giá của khách đã nhận hàng.</p></div></c:if>
    <div class="usp__item reveal"><t:icon name="truck"/><p class="usp__title">Phí ship 30.000đ</p><p class="usp__text">Giao tận nơi, thanh toán khi nhận hàng.</p></div>
    <div class="usp__item reveal"><t:icon name="percent"/><p class="usp__title">Miễn phí ship với mã giảm giá</p><p class="usp__text">Áp dụng mã hợp lệ ở bước thanh toán.</p></div>
    <div class="usp__item reveal"><t:icon name="tag"/><p class="usp__title">Ưu đãi đơn đầu tiên</p><p class="usp__text">Nhập WELCOME10 giảm 10%, tối đa 50.000đ.</p></div>
  </section>

  <%-- 3. Hàng mới về (giống khối "Must-haves" của a-dam) --%>
  <c:if test="${not empty newProducts}">
  <section class="section--lg carousel--floating" data-carousel>
    <div class="container section__head reveal">
      <h2 class="h-1">Hàng mới về</h2>
      <a class="btn btn--outline btn--sm hide-mobile" href="${ctx}/products?sort=newest">Xem tất cả <t:icon name="arrow-right"/></a>
    </div>
    <div class="carousel__viewport">
      <button type="button" class="carousel__float carousel__float--prev" data-carousel-prev aria-label="Sản phẩm trước"><t:icon name="chevron-left"/></button>
      <div class="carousel__track">
        <c:forEach items="${newProducts}" var="p"><div class="carousel__item"><t:product-card product="${p}"/></div></c:forEach>
      </div>
      <button type="button" class="carousel__float carousel__float--next" data-carousel-next aria-label="Sản phẩm sau"><t:icon name="chevron-right"/></button>
    </div>
    <div class="carousel__progress"><div class="carousel__bar"></div></div>
  </section>
  </c:if>

  <%-- 4. Khối chữ nền navy --%>
  <section class="dark-text reveal">
    <span class="dark-text__label">Basic cho ngày nắng</span>
    <h2 class="h-1 dark-text__title">Thứ đầu tiên bạn mặc vào mỗi sáng nên thật dễ chịu.</h2>
    <p class="dark-text__body">Saltair làm đồ basic từ cotton mềm, form dễ mặc và màu dễ phối. Ít chi tiết, nhiều thoải mái, để bạn mặc nhiều hơn là đắn đo.</p>
  </section>

  <%-- 5. Hai thẻ ảnh --%>
  <section class="split-cards" data-stagger>
    <a class="split-card reveal" href="${ctx}/products?category=ao-ni-hoodie">
      <div class="split-card__media"><t:img src="${splitImage1}" alt="Áo nỉ và hoodie" dark="true"/></div>
      <div class="split-card__body">
        <span class="t-label">Áo nỉ và Hoodie</span>
        <p class="h-1 mt-8">Mềm đến ngỡ ngàng</p>
        <p>Nỉ bông dày dặn, ấm vừa phải. Mặc một lần là muốn mặc cả tuần.</p>
        <span class="btn btn--white">Khám phá</span>
      </div>
    </a>
    <a class="split-card reveal" href="${ctx}/products?category=ao-khoac">
      <div class="split-card__media"><t:img src="${splitImage2}" alt="Áo khoác" dark="true"/></div>
      <div class="split-card__body">
        <span class="t-label">Áo khoác</span>
        <p class="h-1 mt-8">Nhẹ cho những ngày nhiều gió</p>
        <p>Áo khoác gió, bomber, linen: khoác ngoài là xong một set đồ.</p>
        <span class="btn btn--white">Xem áo khoác</span>
      </div>
    </a>
  </section>

  <%-- 6. Danh mục: 2 nhóm, mỗi nhóm có tiêu đề và 3 ô vuông chữ trắng (như khối "Underwear / Socks" của a-dam).
       tileGroups: List<Map> mỗi nhóm có title và tiles (name, image, href). Điện thoại: cuộn ngang. --%>
  <c:forEach items="${tileGroups}" var="g">
    <h2 class="h-1 section-title section-title--group reveal"><c:out value="${g.title}"/></h2>
    <section class="tile-row tile-row--square" data-stagger aria-label="${fn:escapeXml(g.title)}">
      <c:forEach items="${g.tiles}" var="tl">
        <a class="tile reveal${empty tl.image ? ' tile--empty' : ''}" href="${fn:escapeXml(ctx.concat(tl.href))}">
          <div class="tile__media"><t:img src="${tl.image}" alt="${tl.name}"/></div>
          <span class="tile__title"><c:out value="${tl.name}"/></span>
        </a>
      </c:forEach>
    </section>
  </c:forEach>

  <%-- 7a. Nổi bật (sản phẩm Admin bật "Hiển thị nổi bật") --%>
  <c:if test="${not empty featuredProducts}">
  <section class="section--lg carousel--floating" data-carousel>
    <div class="container section__head reveal">
      <h2 class="h-1">Nổi bật</h2>
      <a class="btn btn--outline btn--sm hide-mobile" href="${ctx}/products">Xem tất cả <t:icon name="arrow-right"/></a>
    </div>
    <div class="carousel__viewport">
      <button type="button" class="carousel__float carousel__float--prev" data-carousel-prev aria-label="Sản phẩm trước"><t:icon name="chevron-left"/></button>
      <div class="carousel__track">
        <c:forEach items="${featuredProducts}" var="p"><div class="carousel__item"><t:product-card product="${p}"/></div></c:forEach>
      </div>
      <button type="button" class="carousel__float carousel__float--next" data-carousel-next aria-label="Sản phẩm sau"><t:icon name="chevron-right"/></button>
    </div>
    <div class="carousel__progress"><div class="carousel__bar"></div></div>
  </section>
  </c:if>

  <%-- 7b. Bán chạy --%>
  <c:if test="${not empty bestSellers}">
  <section class="section--lg carousel--floating" data-carousel>
    <div class="container section__head reveal">
      <h2 class="h-1">Bán chạy</h2>
      <a class="btn btn--outline btn--sm hide-mobile" href="${ctx}/products?sort=bestseller">Xem tất cả <t:icon name="arrow-right"/></a>
    </div>
    <div class="carousel__viewport">
      <button type="button" class="carousel__float carousel__float--prev" data-carousel-prev aria-label="Sản phẩm trước"><t:icon name="chevron-left"/></button>
      <div class="carousel__track">
        <c:forEach items="${bestSellers}" var="p"><div class="carousel__item"><t:product-card product="${p}"/></div></c:forEach>
      </div>
      <button type="button" class="carousel__float carousel__float--next" data-carousel-next aria-label="Sản phẩm sau"><t:icon name="chevron-right"/></button>
    </div>
    <div class="carousel__progress"><div class="carousel__bar"></div></div>
  </section>
  </c:if>

  <%-- 8. Đánh giá khách hàng --%>
  <c:if test="${not empty latestReviews}">
  <section class="container section reveal">
    <div class="reviews-strip">
      <div class="reviews-summary">
        <span class="reviews-summary__score"><fmt:formatNumber value="${reviewAvg}" minFractionDigits="1" maxFractionDigits="1"/></span>
        <t:stars rating="${reviewAvg}"/>
        <span class="t-small t-muted">Từ ${reviewTotal} đánh giá</span>
      </div>
      <div class="review-grid">
        <c:forEach items="${latestReviews}" var="r">
          <article class="review-card">
            <div class="review-card__meta"><span class="review-card__author"><c:out value="${r.userName}"/></span><span class="review-card__date"><fmt:formatDate value="${r.createdAt}" pattern="dd/MM/yyyy"/></span></div>
            <t:stars rating="${r.rating}"/>
            <p><c:out value="${r.comment}"/></p>
            <a class="review-card__product link-underline" href="${ctx}/product?slug=${fn:escapeXml(r.productSlug)}"><c:out value="${r.productName}"/></a>
          </article>
        </c:forEach>
      </div>
    </div>
  </section>
  </c:if>

  <%-- 9. Khám phá thêm (Blog), bố cục như khối "More to explore" của a-dam --%>
  <c:if test="${not empty latestBlogs}">
  <section class="explore section--lg">
    <div class="container explore__head reveal">
      <span class="t-label">Khám phá thêm</span>
      <h2 class="h-1">Không chỉ là đồ basic</h2>
      <p>Cách phối đồ cho ngày nắng, chọn size cho vừa vặn, giữ áo thun luôn như mới và câu chuyện phía sau Saltair. Đọc thêm trên blog của chúng tôi.</p>
    </div>
    <div class="tile-row tile-row--blog" data-stagger>
      <c:forEach items="${latestBlogs}" var="b">
        <a class="explore__card reveal" href="${ctx}/blog/detail?slug=${fn:escapeXml(b.slug)}">
          <div class="media media--4x5"><t:img src="${b.coverImage}" alt="${b.title}"/></div>
          <span class="explore__title link-reverse"><c:out value="${b.title}"/></span>
        </a>
      </c:forEach>
    </div>
  </section>
  </c:if>

  <%-- 10. Ưu đãi: hộp lùi vào, chữ lớn, ô email kèm nút mũi tên (như khối "Good mail" của a-dam).
       Ô email chuyển sang trang Đăng nhập và điền sẵn email (login.jsp đọc param.email). --%>
  <section class="newsletter reveal">
    <div>
      <p class="newsletter__label">Tham gia cùng Saltair</p>
      <h2 class="h-display newsletter__title">Mã ưu đãi.<br>Đơn đầu tiên.</h2>
    </div>
    <div class="newsletter__side">
      <form class="newsletter__form" action="${ctx}/login" method="get">
        <label class="sr-only" for="newsletter-email">Email</label>
        <input class="newsletter__input" id="newsletter-email" type="email" name="email" placeholder="Email của bạn" autocomplete="email" required>
        <button class="newsletter__submit" type="submit" aria-label="Đăng ký"><t:icon name="arrow-right" size="md"/></button>
      </form>
      <p class="t-small t-muted">Tạo tài khoản rồi nhập <button type="button" class="link-reverse newsletter__code" data-copy="WELCOME10">WELCOME10</button> khi thanh toán: giảm 10% (tối đa 50.000đ) cho đơn từ 200.000đ và miễn phí ship.</p>
    </div>
  </section>
</t:layout>
