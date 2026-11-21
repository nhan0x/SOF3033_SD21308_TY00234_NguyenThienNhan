<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  ADMIN - BÌNH LUẬN / ĐÁNH GIÁ
  Route:   GET /admin/reviews?product=&status=&page=
  Servlet: com.fashionweb.controller.admin.sales.AdminReviewServlet (active menu: reviews)
  Request attributes:
    reviewPage : Page<Review>    - bình luận của trang hiện tại (ADMIN_PAGE_SIZE), cả bình luận đã ẩn, mới nhất trước;
                                   mỗi Review cần userName, productName, productImage
    products   : List<Product>   - sản phẩm cho ô lọc theo sản phẩm
    product    : String          - id sản phẩm đang lọc, "" = tất cả
    status     : String          - visible | hidden, "" = tất cả
  Forms (POST ${ctx}/admin/reviews):
    action=toggleHidden  fields: id  - ẩn hoặc hiện bình luận
--%>
<t:admin-layout title="Bình luận" active="reviews">
  <jsp:attribute name="head"><link rel="stylesheet" href="${ctx}/assets/css/pages/admin-ops.css"></jsp:attribute>
  <jsp:attribute name="scripts"><script src="${ctx}/assets/js/pages/admin-ops.js" defer></script></jsp:attribute>
  <jsp:body>
    <%@ include file="/WEB-INF/views/common/flash.jspf" %>

    <c:set var="pageBase" value="/admin/reviews?product=${product}&status=${status}"/>

    <div class="admin-card admin-card--flush ops-enter">
      <form class="admin-toolbar" method="get" action="${ctx}/admin/reviews" role="search" aria-label="Lọc bình luận">
        <div class="admin-toolbar__left">
          <label class="sr-only" for="filter-product">Sản phẩm</label>
          <select class="select" id="filter-product" name="product" data-autosubmit>
            <option value="">Tất cả sản phẩm</option>
            <c:forEach items="${products}" var="p">
              <option value="${p.id}"${product == p.id ? ' selected' : ''}><c:out value="${p.name}"/></option>
            </c:forEach>
          </select>
          <input type="hidden" name="status" value="${fn:escapeXml(status)}">
          <noscript><button class="btn btn--primary btn--sm" type="submit">Lọc</button></noscript>
          <c:if test="${not empty product or not empty status}">
            <a class="btn btn--ghost btn--sm" href="${ctx}/admin/reviews">Xóa bộ lọc</a>
          </c:if>
        </div>
        <p class="t-small t-muted">${reviewPage.totalItems} bình luận</p>
      </form>

      <nav class="chip-row" aria-label="Lọc theo hiển thị">
        <a class="chip${empty status ? ' is-active' : ''}" href="${ctx}/admin/reviews?product=${product}"${empty status ? ' aria-current="true"' : ''}>Tất cả</a>
        <a class="chip${status == 'visible' ? ' is-active' : ''}" href="${ctx}/admin/reviews?product=${product}&amp;status=visible"${status == 'visible' ? ' aria-current="true"' : ''}>Đang hiện</a>
        <a class="chip${status == 'hidden' ? ' is-active' : ''}" href="${ctx}/admin/reviews?product=${product}&amp;status=hidden"${status == 'hidden' ? ' aria-current="true"' : ''}>Đã ẩn</a>
      </nav>

      <div class="admin-table-wrap">
        <table class="table responsive ops-table">
          <caption class="sr-only">Danh sách bình luận</caption>
          <thead>
            <tr>
              <th scope="col">Sản phẩm</th>
              <th scope="col">Khách hàng</th>
              <th scope="col">Đánh giá</th>
              <th scope="col">Bình luận</th>
              <th scope="col">Ngày</th>
              <th scope="col">Hiển thị</th>
              <th scope="col"><span class="sr-only">Thao tác</span></th>
            </tr>
          </thead>
          <tbody>
            <c:forEach items="${reviewPage.items}" var="r">
              <tr${r.hidden ? ' class="is-muted"' : ''}>
                <td class="cell-lead">
                  <div class="cell-product">
                    <div class="thumb"><t:img src="${r.productImage}" alt="${r.productName}"/></div>
                    <span class="bold"><c:out value="${r.productName}"/></span>
                  </div>
                </td>
                <td data-label="Khách hàng" class="cell-nowrap"><c:out value="${r.userName}"/></td>
                <td data-label="Đánh giá"><span class="review-stars"><t:stars rating="${r.rating}"/><span class="t-small t-muted">${r.rating}/5</span></span></td>
                <td data-label="Bình luận">
                  <c:choose>
                    <c:when test="${empty r.comment}"><span class="t-muted">Không có nội dung</span></c:when>
                    <c:otherwise><span class="cell-clamp" title="${fn:escapeXml(r.comment)}"><c:out value="${r.comment}"/></span></c:otherwise>
                  </c:choose>
                </td>
                <td data-label="Ngày" class="cell-nowrap"><fmt:formatDate value="${r.createdAt}" pattern="dd/MM/yyyy"/></td>
                <td data-label="Hiển thị"><span class="badge ${r.hidden ? 'badge--muted' : 'badge--success'}">${r.hidden ? 'Đã ẩn' : 'Đang hiện'}</span></td>
                <td>
                  <div class="cell-actions">
                    <form method="post" action="${ctx}/admin/reviews">
                      <input type="hidden" name="action" value="toggleHidden">
                      <input type="hidden" name="id" value="${r.id}">
                      <button class="btn ${r.hidden ? 'btn--ghost' : 'btn--outline'} btn--xs" type="submit" aria-label="${r.hidden ? 'Hiện' : 'Ẩn'} bình luận của ${fn:escapeXml(r.userName)} về ${fn:escapeXml(r.productName)}"><t:icon name="${r.hidden ? 'eye' : 'eye-off'}" size="sm"/> ${r.hidden ? 'Hiện' : 'Ẩn'}</button>
                    </form>
                  </div>
                </td>
              </tr>
            </c:forEach>
            <c:if test="${empty reviewPage.items}">
              <tr class="empty-row"><td colspan="7">Không có bình luận nào phù hợp với bộ lọc.</td></tr>
            </c:if>
          </tbody>
        </table>
      </div>

      <c:set var="rangeFrom" value="${(reviewPage.page - 1) * reviewPage.pageSize + 1}"/>
      <c:set var="rangeTo" value="${rangeFrom + fn:length(reviewPage.items) - 1}"/>
      <div class="admin-table-foot">
        <span>
          <c:choose>
            <c:when test="${reviewPage.totalItems > 0}">Hiển thị ${rangeFrom} đến ${rangeTo} trong ${reviewPage.totalItems} bình luận</c:when>
            <c:otherwise>Chưa có bình luận nào</c:otherwise>
          </c:choose>
        </span>
        <t:pagination page="${reviewPage.page}" totalPages="${reviewPage.totalPages}" baseUrl="${pageBase}"/>
      </div>
    </div>
  </jsp:body>
</t:admin-layout>
