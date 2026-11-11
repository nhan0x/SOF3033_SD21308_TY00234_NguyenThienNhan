<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  DANH SÁCH BLOG  (GET /blog -> BlogServlet)
  Thuộc tính request:
    blogs   List<Blog>   bài viết đã xuất bản, mới nhất trước (dùng title, slug, summary, content, coverImage, createdAt)
  Không có form.
--%>
<t:layout title="Blog" nav="blog" bodyClass="blog-page" description="Gợi ý phối đồ, chọn size và bảo quản quần áo từ Saltair.">
  <jsp:attribute name="head"><link rel="stylesheet" href="${ctx}/assets/css/pages/catalog.css?v=6"></jsp:attribute>
  <jsp:body>

    <header class="page-head cat-enter">
      <h1 class="h-1">Blog</h1>
      <p>Gợi ý phối đồ, cách chọn size và mẹo giữ áo luôn như mới.</p>
    </header>

    <c:choose>
      <c:when test="${not empty blogs}">
        <section class="blog-grid" data-stagger aria-label="Danh sách bài viết">
          <c:forEach items="${blogs}" var="b">
            <%-- Ước lượng thời gian đọc: khoảng 1.100 ký tự mỗi phút, tối thiểu 1 phút --%>
            <c:set var="readMinutes" value="${fn:length(b.content) / 1100 + 1}"/>
            <a class="blog-card reveal" href="${ctx}/blog/detail?slug=${fn:escapeXml(b.slug)}">
              <div class="media blog-card__media"><t:img src="${b.coverImage}" alt="${b.title}"/></div>
              <div class="blog-card__body">
                <div class="blog-card__meta">
                  <span><fmt:formatDate value="${b.createdAt}" pattern="dd/MM/yyyy"/></span>
                  <span aria-hidden="true">·</span>
                  <span><fmt:formatNumber value="${readMinutes}" maxFractionDigits="0" groupingUsed="false"/> phút đọc</span>
                </div>
                <h2 class="h-2 link-underline"><c:out value="${b.title}"/></h2>
                <p class="blog-card__summary"><c:out value="${b.summary}"/></p>
              </div>
            </a>
          </c:forEach>
        </section>
      </c:when>
      <c:otherwise>
        <section class="container section">
          <div class="review-empty">
            <p class="h-2">Chưa có bài viết nào</p>
            <p class="mt-8">Saltair sẽ sớm chia sẻ thêm. Trong lúc chờ, bạn có thể xem các sản phẩm mới.</p>
            <a class="btn btn--primary mt-24" href="${ctx}/products?sort=newest">Xem hàng mới về</a>
          </div>
        </section>
      </c:otherwise>
    </c:choose>

  </jsp:body>
</t:layout>
