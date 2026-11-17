<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  ADMIN - DANH SÁCH SẢN PHẨM  (GET /admin/products -> AdminProductServlet)
  Tham số: q, category (slug), page
  Dữ liệu:
    productPage: Page<Product> - sản phẩm của trang hiện tại (ADMIN_PAGE_SIZE dòng)
    categories: List<Category> - đổ vào ô lọc danh mục
    q: String - từ khóa đang tìm (rỗng nếu không có)
    category: String - slug danh mục đang lọc (rỗng nếu không có)
    baseUrl: String - đường dẫn kèm bộ lọc hiện tại, không có page, dùng cho t:pagination
  Form: POST /admin/products action=delete id (xóa mềm)
--%>
<t:admin-layout title="Sản phẩm" active="products">
  <jsp:attribute name="head"><link rel="stylesheet" href="${ctx}/assets/css/pages/admin-catalog.css"></jsp:attribute>
  <jsp:attribute name="actions"><a class="btn btn--primary btn--sm" href="${ctx}/admin/products/form"><t:icon name="plus"/> Thêm sản phẩm</a></jsp:attribute>
  <jsp:body>
    <%@ include file="/WEB-INF/views/common/flash.jspf" %>

    <section class="admin-card admin-card--flush" aria-label="Danh sách sản phẩm">
      <form class="admin-toolbar" method="get" action="${ctx}/admin/products">
        <div class="admin-toolbar__left">
          <label class="sr-only" for="filter-q">Tìm theo tên sản phẩm</label>
          <input class="input" type="search" id="filter-q" name="q" value="${fn:escapeXml(q)}" placeholder="Tìm theo tên sản phẩm..." autocomplete="off">
          <label class="sr-only" for="filter-category">Danh mục</label>
          <select class="select" id="filter-category" name="category">
            <option value="">Tất cả danh mục</option>
            <c:forEach items="${categories}" var="c">
              <option value="${fn:escapeXml(c.slug)}"${c.slug == category ? ' selected' : ''}><c:out value="${c.name}"/></option>
            </c:forEach>
          </select>
          <button class="btn btn--primary btn--sm" type="submit">Lọc</button>
          <c:if test="${not empty q or not empty category}"><a class="btn btn--ghost btn--sm" href="${ctx}/admin/products">Xóa lọc</a></c:if>
        </div>
        <span class="admin-toolbar__count"><fmt:formatNumber value="${productPage.totalItems}"/> sản phẩm</span>
      </form>

      <div class="admin-table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>Sản phẩm</th>
              <th>Danh mục</th>
              <th class="num">Giá</th>
              <th class="num">Tồn kho</th>
              <th>Nổi bật</th>
              <th>Ngày tạo</th>
              <th><span class="sr-only">Thao tác</span></th>
            </tr>
          </thead>
          <tbody>
            <c:forEach items="${productPage.items}" var="p">
              <tr>
                <td>
                  <div class="cell-product">
                    <div class="thumb"><t:img src="${p.imageThumb}" alt="${p.name}"/></div>
                    <div>
                      <a class="bold link-underline" href="${ctx}/admin/products/form?id=${p.id}"><c:out value="${p.name}"/></a>
                      <div class="t-small t-muted"><c:out value="${p.slug}"/></div>
                    </div>
                  </div>
                </td>
                <td><c:out value="${p.categoryName}"/></td>
                <td class="num nowrap"><t:price value="${p.price}"/></td>
                <td class="num nowrap">
                  <c:choose>
                    <c:when test="${p.stockTotal == 0}"><span class="badge badge--danger">Hết hàng</span></c:when>
                    <c:otherwise><fmt:formatNumber value="${p.stockTotal}"/></c:otherwise>
                  </c:choose>
                </td>
                <td>
                  <c:choose>
                    <c:when test="${p.featured}"><span class="badge badge--success">Nổi bật</span></c:when>
                    <c:otherwise><span class="muted-dash"><span aria-hidden="true">-</span><span class="sr-only">Không nổi bật</span></span></c:otherwise>
                  </c:choose>
                </td>
                <td class="nowrap"><fmt:formatDate value="${p.createdAt}" pattern="dd/MM/yyyy"/></td>
                <td>
                  <div class="cell-actions">
                    <a class="icon-btn" href="${ctx}/admin/products/form?id=${p.id}" aria-label="Sửa ${fn:escapeXml(p.name)}" title="Sửa"><t:icon name="pencil"/></a>
                    <form class="inline-form" method="post" action="${ctx}/admin/products" data-confirm="Xóa sản phẩm “${fn:escapeXml(p.name)}”? Sản phẩm sẽ được chuyển vào Thùng rác (xóa mềm), ẩn khỏi cửa hàng và có thể khôi phục lại sau.">
                      <input type="hidden" name="action" value="delete">
                      <input type="hidden" name="id" value="${p.id}">
                      <button class="icon-btn icon-btn--danger" type="submit" aria-label="Xóa ${fn:escapeXml(p.name)}" title="Xóa"><t:icon name="trash"/></button>
                    </form>
                  </div>
                </td>
              </tr>
            </c:forEach>
            <c:if test="${empty productPage.items}">
              <tr class="empty-row"><td colspan="7">Không tìm thấy sản phẩm phù hợp.</td></tr>
            </c:if>
          </tbody>
        </table>
      </div>

      <div class="admin-table-foot">
        <c:choose>
          <c:when test="${productPage.totalItems > 0}">
            <span>Hiển thị ${(productPage.page - 1) * productPage.pageSize + 1} - ${(productPage.page - 1) * productPage.pageSize + fn:length(productPage.items)} trong <fmt:formatNumber value="${productPage.totalItems}"/> sản phẩm</span>
          </c:when>
          <c:otherwise><span>Không có sản phẩm</span></c:otherwise>
        </c:choose>
        <t:pagination page="${productPage.page}" totalPages="${productPage.totalPages}" baseUrl="${baseUrl}"/>
      </div>
    </section>
  </jsp:body>
</t:admin-layout>
