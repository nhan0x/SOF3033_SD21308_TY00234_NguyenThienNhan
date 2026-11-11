<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  FRAGMENT TÌM KIẾM NHANH
  Route:   GET /search/quick?q=...
  Servlet: com.fashionweb.controller.shop.QuickSearchServlet
  Request attributes:
    q           : String                   - từ khóa đã trim (rỗng nếu dưới 2 ký tự)
    suggestions : List<Map<String,String>> - tối đa 4 gợi ý từ khóa (name, prefix, match, suffix)
    productPage : Page<Product>            - tối đa 8 sản phẩm đang bán khớp q (mới nhất trước), kèm totalItems
    categories  : List<Category>           - danh mục chưa xóa có tên chứa q, kèm productCount
--%>
<c:if test="${not empty q}">
  <c:set var="ctx" value="${pageContext.request.contextPath}" scope="request"/>
  <fmt:setLocale value="vi_VN" scope="request"/>
  <c:choose>
    <c:when test="${empty productPage.items and empty categories}">
      <div class="quick-search__empty">
        <p>Không tìm thấy sản phẩm cho «<c:out value="${q}"/>»</p>
        <a class="link-underline bold" href="${ctx}/products">Xem tất cả sản phẩm</a>
      </div>
    </c:when>
    <c:otherwise>
      <c:if test="${not empty suggestions}">
        <div class="quick-search__suggestions" aria-label="Gợi ý từ khóa">
          <c:forEach items="${suggestions}" var="s">
            <c:url var="sugHref" value="/products"><c:param name="q" value="${s.name}"/></c:url>
            <a class="link-underline quick-search__suggestion" href="${fn:escapeXml(sugHref)}"><c:out value="${s.prefix}"/><c:if test="${not empty s.match}"><mark><c:out value="${s.match}"/></mark></c:if><c:out value="${s.suffix}"/></a>
          </c:forEach>
        </div>
      </c:if>

      <c:set var="defaultTab" value="${not empty productPage.items ? 'products' : 'categories'}"/>
      <div class="quick-search__tabs" role="tablist" aria-label="Kết quả tìm kiếm">
        <button type="button" class="quick-search__tab${defaultTab == 'products' ? ' is-active' : ''}" role="tab" aria-selected="${defaultTab == 'products'}" data-search-tab="products">Sản phẩm (${productPage.totalItems})</button>
        <button type="button" class="quick-search__tab${defaultTab == 'categories' ? ' is-active' : ''}" role="tab" aria-selected="${defaultTab == 'categories'}" data-search-tab="categories">Danh mục (${fn:length(categories)})</button>
      </div>

      <div class="quick-search__panel" role="tabpanel" data-quick-panel="products"${defaultTab == 'products' ? '' : ' hidden'}>
        <c:choose>
          <c:when test="${not empty productPage.items}">
            <div class="quick-search__grid">
              <c:forEach items="${productPage.items}" var="p">
                <t:product-card product="${p}" eager="true"/>
              </c:forEach>
            </div>
            <c:url var="allHref" value="/products"><c:param name="q" value="${q}"/></c:url>
            <div class="quick-search__more">
              <a class="btn btn--ghost btn--block" href="${fn:escapeXml(allHref)}">Xem tất cả ${productPage.totalItems} kết quả</a>
            </div>
          </c:when>
          <c:otherwise>
            <div class="quick-search__empty">
              <p>Không tìm thấy sản phẩm cho «<c:out value="${q}"/>»</p>
              <a class="link-underline bold" href="${ctx}/products">Xem tất cả sản phẩm</a>
            </div>
          </c:otherwise>
        </c:choose>
      </div>

      <div class="quick-search__panel" role="tabpanel" data-quick-panel="categories"${defaultTab == 'categories' ? '' : ' hidden'}>
        <c:choose>
          <c:when test="${not empty categories}">
            <ul class="quick-search__categories">
              <c:forEach items="${categories}" var="c">
                <li>
                  <a class="quick-search__category link-underline" href="${ctx}/products?category=${fn:escapeXml(c.slug)}">
                    <span><c:out value="${c.name}"/></span>
                    <span class="t-muted">(${c.productCount} sản phẩm)</span>
                  </a>
                </li>
              </c:forEach>
            </ul>
          </c:when>
          <c:otherwise>
            <p class="t-muted">Không có danh mục phù hợp.</p>
          </c:otherwise>
        </c:choose>
      </div>
    </c:otherwise>
  </c:choose>
</c:if>
