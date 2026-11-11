<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  DANH SÁCH SẢN PHẨM  (GET /products -> ProductListServlet)
  Tham số URL: q, category (slug), minPrice, maxPrice, color (mã màu), size (tên size),
               sort (newest|price_asc|price_desc|bestseller|rating), page
  Thuộc tính request:
    productPage    Page<Product>          trang sản phẩm hiện tại (items, page, totalPages, totalItems)
    categories     List<Category>         danh mục cho bộ lọc
    colors         List<Color>            màu cho bộ lọc
    sizes          List<Size>             size cho bộ lọc
    currentCategory Category | null       danh mục đang lọc
    q              String                 từ khóa ("" nếu không có)
    sort           String                 sort do người dùng chọn ("" nếu không có; mặc định hiểu là newest)
    selectedColor  String                 mã màu đang lọc ("" nếu không có)
    selectedSize   String                 tên size đang lọc ("" nếu không có)
    minPrice       Long | null            giá tối thiểu
    maxPrice       Long | null            giá tối đa
    baseUrl        String                 "/products?..." gồm các bộ lọc hiện tại, KHÔNG có page (cho t:pagination)
    activeFilters  List<Map<String,String>> chip bộ lọc đang bật: label, url (url đã bỏ bộ lọc đó, chưa có ctx)
--%>
<c:set var="effSort" value="${empty sort ? 'newest' : sort}"/>
<c:set var="navKey" value="${sort == 'newest' ? 'new' : (sort == 'bestseller' ? 'best' : 'shop')}"/>
<c:choose>
  <c:when test="${not empty currentCategory}">
    <c:set var="heading" value="${currentCategory.name}"/>
    <c:set var="intro" value="Những mẫu ${fn:toLowerCase(currentCategory.name)} basic của Saltair: mềm, thoáng và dễ phối với nhau."/>
  </c:when>
  <c:when test="${not empty q}">
    <c:set var="heading" value="Kết quả cho “${q}”"/>
    <c:set var="intro" value="Những sản phẩm khớp với từ khóa của bạn."/>
  </c:when>
  <c:when test="${sort == 'bestseller'}">
    <c:set var="heading" value="Bán chạy"/>
    <c:set var="intro" value="Những món được khách chọn nhiều nhất tại Saltair."/>
  </c:when>
  <c:when test="${sort == 'newest'}">
    <c:set var="heading" value="Hàng mới về"/>
    <c:set var="intro" value="Những mẫu vừa về, xếp từ mới nhất."/>
  </c:when>
  <c:otherwise>
    <c:set var="heading" value="Tất cả sản phẩm"/>
    <c:set var="intro" value="Đồ basic nhẹ, thoáng và dễ phối cho những ngày nắng."/>
  </c:otherwise>
</c:choose>

<t:layout title="${heading}" nav="${navKey}" description="${intro}">
  <jsp:attribute name="head"><link rel="stylesheet" href="${ctx}/assets/css/pages/catalog.css"></jsp:attribute>
  <jsp:attribute name="scripts"><script src="${ctx}/assets/js/pages/catalog.js" defer></script></jsp:attribute>
  <jsp:body>

    <%-- Tiêu đề trang --%>
    <header class="page-head">
      <h1 class="h-1 cat-enter"><c:out value="${heading}"/></h1>
      <p class="cat-enter" style="--i:1"><c:out value="${intro}"/></p>
    </header>

    <%-- Thanh công cụ: số sản phẩm, kiểu lưới, nút lọc --%>
    <div class="toolbar cat-enter" style="--i:2">
      <p class="toolbar__count" aria-live="polite"><strong><fmt:formatNumber value="${productPage.totalItems}"/></strong> sản phẩm</p>
      <div class="toolbar__right">
        <c:if test="${not empty productPage.items}">
          <div class="view-toggle" role="group" aria-label="Kiểu hiển thị lưới">
            <button type="button" data-view="large" aria-pressed="false" aria-label="Hiển thị ít cột, ảnh lớn"><t:icon name="grid-2"/></button>
            <button type="button" class="is-active" data-view="dense" aria-pressed="true" aria-label="Hiển thị nhiều cột"><t:icon name="grid-3"/></button>
          </div>
        </c:if>
        <button type="button" class="btn btn--outline btn--sm" data-drawer-open="filter-drawer" aria-haspopup="dialog" aria-controls="filter-drawer">
          <t:icon name="sliders"/> Lọc và sắp xếp<c:if test="${fn:length(activeFilters) > 0}"> <span class="filter-count" aria-label="${fn:length(activeFilters)} bộ lọc đang bật">${fn:length(activeFilters)}</span></c:if>
        </button>
      </div>
    </div>

    <%-- Chip bộ lọc đang bật --%>
    <c:if test="${not empty activeFilters}">
      <div class="active-filters" aria-label="Bộ lọc đang bật">
        <c:forEach items="${activeFilters}" var="f" varStatus="s">
          <a class="chip chip--remove" style="--i:${s.index}" href="${ctx}${fn:escapeXml(f.url)}" aria-label="Bỏ bộ lọc ${fn:escapeXml(f.label)}">
            <c:out value="${f.label}"/> <t:icon name="x"/>
          </a>
        </c:forEach>
        <a class="active-filters__clear t-small link-plain" href="${ctx}/products">Xóa tất cả</a>
      </div>
    </c:if>

    <%-- Lưới sản phẩm hoặc trạng thái rỗng --%>
    <c:choose>
      <c:when test="${not empty productPage.items}">
        <section class="product-grid" data-product-grid data-view="dense" aria-label="Danh sách sản phẩm">
          <c:forEach items="${productPage.items}" var="p" varStatus="s">
            <div class="${s.index >= 4 ? 'reveal' : 'cat-enter'}" style="--i:${s.index % 4}">
              <t:product-card product="${p}" eager="${s.index < 4}"/>
            </div>
          </c:forEach>
        </section>
        <t:pagination page="${productPage.page}" totalPages="${productPage.totalPages}" baseUrl="${baseUrl}"/>
      </c:when>
      <c:otherwise>
        <div class="empty-state">
          <t:icon name="search"/>
          <h2 class="h-2">Không tìm thấy sản phẩm phù hợp</h2>
          <p>Thử bỏ bớt bộ lọc hoặc đổi từ khóa để xem thêm những món khác của Saltair.</p>
          <a class="btn btn--primary" href="${ctx}/products">Xóa bộ lọc</a>
        </div>
      </c:otherwise>
    </c:choose>

    <div class="catalog-spacer" aria-hidden="true"></div>

    <%-- Ngăn kéo lọc và sắp xếp (bên phải). Form GET về chính /products --%>
    <aside class="drawer drawer--right" id="filter-drawer" aria-label="Lọc và sắp xếp">
      <form class="filter-form" method="get" action="${ctx}/products" data-filter-form data-sort-explicit="${not empty sort}">
        <div class="drawer__head">
          <h2 class="h-2">Lọc và sắp xếp</h2>
          <button type="button" class="icon-btn" data-panel-close aria-label="Đóng bộ lọc"><t:icon name="x" size="md"/></button>
        </div>

        <div class="drawer__body">
          <c:if test="${not empty q}"><input type="hidden" name="q" value="${fn:escapeXml(q)}"></c:if>

          <div class="filter-group" role="group" aria-labelledby="fg-sort">
            <p class="filter-group__title" id="fg-sort">Sắp xếp</p>
            <div class="filter-options">
              <label class="radio"><input type="radio" name="sort" value="newest"${effSort == 'newest' ? ' checked' : ''}><span>Mới nhất</span></label>
              <label class="radio"><input type="radio" name="sort" value="bestseller"${effSort == 'bestseller' ? ' checked' : ''}><span>Bán chạy</span></label>
              <label class="radio"><input type="radio" name="sort" value="rating"${effSort == 'rating' ? ' checked' : ''}><span>Đánh giá cao</span></label>
              <label class="radio"><input type="radio" name="sort" value="price_asc"${effSort == 'price_asc' ? ' checked' : ''}><span>Giá tăng dần</span></label>
              <label class="radio"><input type="radio" name="sort" value="price_desc"${effSort == 'price_desc' ? ' checked' : ''}><span>Giá giảm dần</span></label>
            </div>
          </div>

          <div class="filter-group" role="group" aria-labelledby="fg-category">
            <p class="filter-group__title" id="fg-category">Danh mục</p>
            <div class="filter-options">
              <label class="radio"><input type="radio" name="category" value=""${empty currentCategory ? ' checked' : ''}><span>Tất cả</span></label>
              <c:forEach items="${categories}" var="cat">
                <label class="radio"><input type="radio" name="category" value="${fn:escapeXml(cat.slug)}"${currentCategory.slug == cat.slug ? ' checked' : ''}><span><c:out value="${cat.name}"/></span></label>
              </c:forEach>
            </div>
          </div>

          <div class="filter-group" role="group" aria-labelledby="fg-price">
            <p class="filter-group__title" id="fg-price">Khoảng giá (đ)</p>
            <div class="filter-price">
              <input class="input" type="number" name="minPrice" min="0" step="1000" inputmode="numeric" placeholder="Từ" aria-label="Giá thấp nhất" value="${minPrice}">
              <input class="input" type="number" name="maxPrice" min="0" step="1000" inputmode="numeric" placeholder="Đến" aria-label="Giá cao nhất" value="${maxPrice}">
            </div>
            <div class="filter-quick">
              <button type="button" class="chip" data-price-quick data-min="" data-max="300000" aria-pressed="false">Dưới 300.000đ</button>
              <button type="button" class="chip" data-price-quick data-min="300000" data-max="500000" aria-pressed="false">300.000đ - 500.000đ</button>
              <button type="button" class="chip" data-price-quick data-min="500000" data-max="" aria-pressed="false">Trên 500.000đ</button>
            </div>
          </div>

          <div class="filter-group" role="group" aria-labelledby="fg-color">
            <p class="filter-group__title" id="fg-color"><span>Màu</span><small data-color-name></small></p>
            <div class="swatches" data-toggle-group>
              <c:forEach items="${colors}" var="col">
                <label class="swatch" title="${fn:escapeXml(col.name)}">
                  <input type="radio" name="color" value="${fn:escapeXml(col.code)}" data-name="${fn:escapeXml(col.name)}" aria-label="${fn:escapeXml(col.name)}"${selectedColor == col.code ? ' checked' : ''}>
                  <span style="--sw:${fn:escapeXml(col.hexCode)}"></span>
                </label>
              </c:forEach>
            </div>
          </div>

          <div class="filter-group" role="group" aria-labelledby="fg-size">
            <p class="filter-group__title" id="fg-size">Size</p>
            <div class="pill-options" data-toggle-group>
              <c:forEach items="${sizes}" var="sz">
                <label class="pill-option">
                  <input type="radio" name="size" value="${fn:escapeXml(sz.name)}"${selectedSize == sz.name ? ' checked' : ''}>
                  <span><c:out value="${sz.name}"/></span>
                </label>
              </c:forEach>
            </div>
          </div>
        </div>

        <div class="drawer__foot">
          <a class="btn btn--ghost" href="${ctx}/products">Xóa bộ lọc</a>
          <button type="submit" class="btn btn--primary">Áp dụng</button>
        </div>
      </form>
    </aside>

  </jsp:body>
</t:layout>
