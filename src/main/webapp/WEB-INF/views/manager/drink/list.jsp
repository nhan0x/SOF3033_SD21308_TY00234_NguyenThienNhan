<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/_taglibs.jspf" %>
<c:set var="pageTitle" value="Quản lý đồ uống"/>
<%@ include file="/WEB-INF/views/_header.jspf" %>
<h2>Quản lý đồ uống</h2>
<p><a class="btn primary" href="${ctx}/manager/drinks/new">+ Thêm đồ uống</a></p>

<form method="get" action="${ctx}/manager/drinks" class="search">
  <label>Tên đồ uống
    <input type="text" name="keyword" value="<c:out value='${keyword}'/>" placeholder="Tìm theo tên">
  </label>
  <label>Loại
    <select name="categoryId">
      <option value="">Tất cả</option>
      <c:forEach var="c" items="${categories}">
        <option value="${c.id}" ${c.id == categoryId ? 'selected' : ''}><c:out value="${c.name}"/></option>
      </c:forEach>
    </select>
  </label>
  <label>Trạng thái
    <select name="status">
      <option value="">Tất cả</option>
      <option value="1" ${status == '1' ? 'selected' : ''}>Đang bán</option>
      <option value="0" ${status == '0' ? 'selected' : ''}>Ngừng bán</option>
    </select>
  </label>
  <button type="submit" class="primary">Tìm</button>
</form>

<div class="table-wrap">
  <table class="responsive">
    <thead>
      <tr><th>Ảnh</th><th>Tên</th><th>Loại</th><th class="num">Đơn giá</th><th class="num">Giảm giá</th><th>Trạng thái</th><th>Thao tác</th></tr>
    </thead>
    <tbody>
      <c:forEach var="d" items="${pg.items}">
        <c:set var="img" value="${empty d.image ? 'no-image.svg' : d.image}"/>
        <tr>
          <td data-label="Ảnh">
            <img class="thumb" src="<c:out value='${ctx}/images/${img}'/>" alt=""
                 onerror="this.onerror=null;this.src='${ctx}/images/no-image.svg'">
          </td>
          <td data-label="Tên"><c:out value="${d.name}"/></td>
          <td data-label="Loại"><c:out value="${categoryNames[d.categoryId]}"/></td>
          <td data-label="Đơn giá" class="num"><fmt:formatNumber value="${d.unitPrice}" pattern="#,##0"/></td>
          <td data-label="Giảm giá" class="num"><fmt:formatNumber value="${d.discount * 100}" pattern="0.##"/>%</td>
          <td data-label="Trạng thái">
            <c:choose>
              <c:when test="${d.available}"><span class="tag ok">Đang bán</span></c:when>
              <c:otherwise><span class="tag off">Ngừng bán</span></c:otherwise>
            </c:choose>
          </td>
          <td data-label="Thao tác">
            <div class="actions">
              <a href="${ctx}/manager/drinks/edit?id=${d.id}">Sửa</a>
              <form method="post" action="${ctx}/manager/drinks/delete"
                    onsubmit="return confirm('Xóa đồ uống này?')">
                <input type="hidden" name="id" value="${d.id}">
                <button type="submit" class="link danger">Xóa</button>
              </form>
            </div>
          </td>
        </tr>
      </c:forEach>
    </tbody>
  </table>
  <c:if test="${empty pg.items}"><p class="empty">Không có đồ uống nào phù hợp.</p></c:if>
</div>
<%@ include file="/WEB-INF/views/_pager.jspf" %>
<%@ include file="/WEB-INF/views/_footer.jspf" %>
