<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  ADMIN - THÙNG RÁC  (GET /admin/trash?tab= -> AdminTrashServlet)
  Tham số: tab = products | categories | colors | sizes
  Dữ liệu:
    tab: String - tab đang chọn
    items: List - mục đã xóa mềm của tab: Product | Category | Color | Size (có deletedAt)
    tabCounts: Map<String,Integer> - số mục của từng tab, hiện cạnh tên tab
  Form: POST /admin/trash action=restore: type (products|categories|colors|sizes), id
  Không có xóa vĩnh viễn trong phạm vi đồ án.
--%>
<t:admin-layout title="Thùng rác" active="trash">
  <jsp:attribute name="head"><link rel="stylesheet" href="${ctx}/assets/css/pages/admin-catalog.css"></jsp:attribute>
  <jsp:body>
    <%@ include file="/WEB-INF/views/common/flash.jspf" %>

    <nav class="trash-tabs" aria-label="Loại dữ liệu đã xóa">
      <a class="chip${tab == 'products' ? ' is-active' : ''}" href="${ctx}/admin/trash?tab=products"${tab == 'products' ? ' aria-current="page"' : ''}>Sản phẩm <span class="chip__count">${tabCounts['products']}</span></a>
      <a class="chip${tab == 'categories' ? ' is-active' : ''}" href="${ctx}/admin/trash?tab=categories"${tab == 'categories' ? ' aria-current="page"' : ''}>Danh mục <span class="chip__count">${tabCounts['categories']}</span></a>
      <a class="chip${tab == 'colors' ? ' is-active' : ''}" href="${ctx}/admin/trash?tab=colors"${tab == 'colors' ? ' aria-current="page"' : ''}>Màu <span class="chip__count">${tabCounts['colors']}</span></a>
      <a class="chip${tab == 'sizes' ? ' is-active' : ''}" href="${ctx}/admin/trash?tab=sizes"${tab == 'sizes' ? ' aria-current="page"' : ''}>Size <span class="chip__count">${tabCounts['sizes']}</span></a>
    </nav>

    <p class="trash-note"><t:icon name="info"/><span>Các mục đã xóa vẫn được giữ trong cơ sở dữ liệu. Khôi phục để hiển thị lại trên cửa hàng và trang quản trị.</span></p>

    <section class="admin-card admin-card--flush" aria-label="Mục đã xóa">
      <c:choose>

        <%-- Sản phẩm --%>
        <c:when test="${tab == 'products'}">
          <c:choose>
            <c:when test="${empty items}">
              <div class="empty-state"><t:icon name="trash"/><h2 class="h-3">Thùng rác trống</h2><p>Chưa có sản phẩm nào bị xóa.</p></div>
            </c:when>
            <c:otherwise>
              <div class="admin-table-wrap">
                <table class="table">
                  <thead><tr><th>Sản phẩm</th><th>Danh mục</th><th class="num">Giá</th><th>Ngày xóa</th><th><span class="sr-only">Thao tác</span></th></tr></thead>
                  <tbody>
                    <c:forEach items="${items}" var="it">
                      <tr>
                        <td>
                          <div class="cell-product">
                            <div class="thumb"><t:img src="${it.imageThumb}" alt="${it.name}"/></div>
                            <div>
                              <span class="bold"><c:out value="${it.name}"/></span>
                              <div class="t-small t-muted"><c:out value="${it.slug}"/></div>
                            </div>
                          </div>
                        </td>
                        <td><c:out value="${it.categoryName}"/></td>
                        <td class="num nowrap"><t:price value="${it.price}"/></td>
                        <td class="nowrap"><fmt:formatDate value="${it.deletedAt}" pattern="dd/MM/yyyy HH:mm"/></td>
                        <td>
                          <div class="cell-actions">
                            <form class="inline-form" method="post" action="${ctx}/admin/trash" data-confirm="Khôi phục sản phẩm “${fn:escapeXml(it.name)}”? Sản phẩm sẽ hiển thị lại trên cửa hàng.">
                              <input type="hidden" name="action" value="restore">
                              <input type="hidden" name="type" value="products">
                              <input type="hidden" name="id" value="${it.id}">
                              <button class="btn btn--outline btn--xs" type="submit"><t:icon name="restore"/> Khôi phục</button>
                            </form>
                          </div>
                        </td>
                      </tr>
                    </c:forEach>
                  </tbody>
                </table>
              </div>
            </c:otherwise>
          </c:choose>
        </c:when>

        <%-- Danh mục --%>
        <c:when test="${tab == 'categories'}">
          <c:choose>
            <c:when test="${empty items}">
              <div class="empty-state"><t:icon name="trash"/><h2 class="h-3">Thùng rác trống</h2><p>Chưa có danh mục nào bị xóa.</p></div>
            </c:when>
            <c:otherwise>
              <div class="admin-table-wrap">
                <table class="table">
                  <thead><tr><th>Danh mục</th><th>Đường dẫn (slug)</th><th>Ngày xóa</th><th><span class="sr-only">Thao tác</span></th></tr></thead>
                  <tbody>
                    <c:forEach items="${items}" var="it">
                      <tr>
                        <td>
                          <div class="cell-product">
                            <div class="thumb"><t:img src="${it.image}" alt="${it.name}"/></div>
                            <span class="bold"><c:out value="${it.name}"/></span>
                          </div>
                        </td>
                        <td class="code-text"><c:out value="${it.slug}"/></td>
                        <td class="nowrap"><fmt:formatDate value="${it.deletedAt}" pattern="dd/MM/yyyy HH:mm"/></td>
                        <td>
                          <div class="cell-actions">
                            <form class="inline-form" method="post" action="${ctx}/admin/trash" data-confirm="Khôi phục danh mục “${fn:escapeXml(it.name)}”? Danh mục sẽ hiển thị lại trên cửa hàng.">
                              <input type="hidden" name="action" value="restore">
                              <input type="hidden" name="type" value="categories">
                              <input type="hidden" name="id" value="${it.id}">
                              <button class="btn btn--outline btn--xs" type="submit"><t:icon name="restore"/> Khôi phục</button>
                            </form>
                          </div>
                        </td>
                      </tr>
                    </c:forEach>
                  </tbody>
                </table>
              </div>
            </c:otherwise>
          </c:choose>
        </c:when>

        <%-- Màu --%>
        <c:when test="${tab == 'colors'}">
          <c:choose>
            <c:when test="${empty items}">
              <div class="empty-state"><t:icon name="trash"/><h2 class="h-3">Thùng rác trống</h2><p>Chưa có màu nào bị xóa.</p></div>
            </c:when>
            <c:otherwise>
              <div class="admin-table-wrap">
                <table class="table">
                  <thead><tr><th>Màu</th><th>Mã</th><th>Mã hex</th><th>Ngày xóa</th><th><span class="sr-only">Thao tác</span></th></tr></thead>
                  <tbody>
                    <c:forEach items="${items}" var="it">
                      <tr>
                        <td class="nowrap"><span class="color-dot" style="--sw:${fn:escapeXml(it.hexCode)}"></span><span class="bold"><c:out value="${it.name}"/></span></td>
                        <td class="code-text"><c:out value="${it.code}"/></td>
                        <td class="hex-text"><c:out value="${it.hexCode}"/></td>
                        <td class="nowrap"><fmt:formatDate value="${it.deletedAt}" pattern="dd/MM/yyyy HH:mm"/></td>
                        <td>
                          <div class="cell-actions">
                            <form class="inline-form" method="post" action="${ctx}/admin/trash" data-confirm="Khôi phục màu “${fn:escapeXml(it.name)}”? Màu sẽ có thể chọn lại cho sản phẩm.">
                              <input type="hidden" name="action" value="restore">
                              <input type="hidden" name="type" value="colors">
                              <input type="hidden" name="id" value="${it.id}">
                              <button class="btn btn--outline btn--xs" type="submit"><t:icon name="restore"/> Khôi phục</button>
                            </form>
                          </div>
                        </td>
                      </tr>
                    </c:forEach>
                  </tbody>
                </table>
              </div>
            </c:otherwise>
          </c:choose>
        </c:when>

        <%-- Size --%>
        <c:otherwise>
          <c:choose>
            <c:when test="${empty items}">
              <div class="empty-state"><t:icon name="trash"/><h2 class="h-3">Thùng rác trống</h2><p>Chưa có size nào bị xóa.</p></div>
            </c:when>
            <c:otherwise>
              <div class="admin-table-wrap">
                <table class="table">
                  <thead><tr><th>Size</th><th class="num">Thứ tự hiển thị</th><th>Ngày xóa</th><th><span class="sr-only">Thao tác</span></th></tr></thead>
                  <tbody>
                    <c:forEach items="${items}" var="it">
                      <tr>
                        <td class="bold"><c:out value="${it.name}"/></td>
                        <td class="num">${it.sortOrder}</td>
                        <td class="nowrap"><fmt:formatDate value="${it.deletedAt}" pattern="dd/MM/yyyy HH:mm"/></td>
                        <td>
                          <div class="cell-actions">
                            <form class="inline-form" method="post" action="${ctx}/admin/trash" data-confirm="Khôi phục size “${fn:escapeXml(it.name)}”? Size sẽ có thể chọn lại cho sản phẩm.">
                              <input type="hidden" name="action" value="restore">
                              <input type="hidden" name="type" value="sizes">
                              <input type="hidden" name="id" value="${it.id}">
                              <button class="btn btn--outline btn--xs" type="submit"><t:icon name="restore"/> Khôi phục</button>
                            </form>
                          </div>
                        </td>
                      </tr>
                    </c:forEach>
                  </tbody>
                </table>
              </div>
            </c:otherwise>
          </c:choose>
        </c:otherwise>
      </c:choose>
    </section>
  </jsp:body>
</t:admin-layout>
