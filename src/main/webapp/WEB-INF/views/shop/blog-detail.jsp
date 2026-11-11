<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  CHI TIẾT BLOG  (GET /blog/detail?slug=... -> BlogDetailServlet; không tìm thấy hoặc bài đang ẩn thì Servlet trả 404)
  Thuộc tính request:
    blog        Blog         bài viết (title, summary, content, coverImage, createdAt, authorName)
    otherBlogs  List<Blog>   tối đa 3 bài khác, mới nhất trước
    lineBreaks  String       ký tự xuống dòng ("\r\n") dùng làm dấu tách đoạn cho fn:split
  Không có form.
--%>
<c:set var="paragraphs" value="${fn:split(blog.content, lineBreaks)}"/>
<t:layout title="${blog.title}" nav="blog" description="${blog.summary}">
  <jsp:attribute name="head"><link rel="stylesheet" href="${ctx}/assets/css/pages/catalog.css?v=6"></jsp:attribute>
  <jsp:attribute name="scripts"><script src="${ctx}/assets/js/pages/catalog.js" defer></script></jsp:attribute>
  <jsp:body>

    <div class="read-progress" data-read-progress aria-hidden="true"></div>

    <article class="article">
      <nav class="breadcrumb" aria-label="Đường dẫn">
        <a href="${ctx}/home">Trang chủ</a><span aria-hidden="true">/</span>
        <a href="${ctx}/blog">Blog</a><span aria-hidden="true">/</span>
        <span aria-current="page"><c:out value="${blog.title}"/></span>
      </nav>

      <header class="article__header cat-enter">
        <div class="article__meta">
          <span class="article__meta-item"><t:icon name="calendar"/> <fmt:formatDate value="${blog.createdAt}" pattern="dd/MM/yyyy"/></span>
          <c:if test="${not empty blog.authorName}">
            <span aria-hidden="true">·</span>
            <span>Tác giả: <c:out value="${blog.authorName}"/></span>
          </c:if>
        </div>
        <h1 class="h-1 article__title"><c:out value="${blog.title}"/></h1>
      </header>

      <div class="article__cover media media--16x9 cat-enter" style="--i:1"><t:img src="${blog.coverImage}" alt="${blog.title}" eager="true"/></div>

      <div class="article__body">
        <c:forEach items="${paragraphs}" var="para">
          <c:if test="${not empty fn:trim(para)}"><p><c:out value="${fn:trim(para)}"/></p></c:if>
        </c:forEach>
      </div>

      <div class="article__foot">
        <a class="btn btn--outline" href="${ctx}/blog"><t:icon name="arrow-left"/> Quay lại blog</a>
      </div>
    </article>

    <c:if test="${not empty otherBlogs}">
      <section class="article-others section--lg" aria-labelledby="others-title">
        <h2 class="h-1 section-title reveal" id="others-title">Bài viết khác</h2>
        <div class="blog-grid" data-stagger>
          <c:forEach items="${otherBlogs}" var="b">
            <a class="blog-card reveal" href="${ctx}/blog/detail?slug=${fn:escapeXml(b.slug)}">
              <div class="media media--4x5"><t:img src="${b.coverImage}" alt="${b.title}"/></div>
              <div class="blog-card__body">
                <div class="blog-card__meta"><span><fmt:formatDate value="${b.createdAt}" pattern="dd/MM/yyyy"/></span></div>
                <h3 class="h-2 link-underline"><c:out value="${b.title}"/></h3>
                <p class="blog-card__summary"><c:out value="${b.summary}"/></p>
              </div>
            </a>
          </c:forEach>
        </div>
      </section>
    </c:if>

  </jsp:body>
</t:layout>
