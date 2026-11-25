<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/_taglibs.jspf" %>
<c:set var="pageTitle" value="${drink.id == 0 ? 'Thêm đồ uống' : 'Cập nhật đồ uống'}"/>
<%@ include file="/WEB-INF/views/_header.jspf" %>
<h2><c:out value="${pageTitle}"/></h2>
<c:if test="${not empty error}"><div class="msg err" role="alert"><c:out value="${error}"/></div></c:if>
<form method="post" action="${ctx}/manager/drinks/save" enctype="multipart/form-data" class="form">
  <input type="hidden" name="id" value="${drink.id}">

  <label for="name">Tên đồ uống</label>
  <input type="text" id="name" name="name" maxlength="100" value="<c:out value='${drink.name}'/>" required>

  <label for="unitPrice">Đơn giá (đ)</label>
  <input type="number" id="unitPrice" name="unitPrice" min="0" step="1" required
         value="<c:if test='${not (drink.id == 0 and drink.unitPrice == 0 and empty error)}'><fmt:formatNumber value='${drink.unitPrice}' pattern='0' groupingUsed='false'/></c:if>">

  <label for="discount">Giảm giá (%)</label>
  <input type="number" id="discount" name="discount" min="0" max="100" step="any"
         value="<fmt:formatNumber value='${drink.discount * 100}' pattern='0.####' groupingUsed='false'/>">
  <span class="hint">Từ 0 đến 100.</span>

  <label for="categoryId">Loại đồ uống</label>
  <select id="categoryId" name="categoryId" required>
    <option value="">-- Chọn loại --</option>
    <c:forEach var="c" items="${categories}">
      <option value="${c.id}" ${c.id == drink.categoryId ? 'selected' : ''}><c:out value="${c.name}"/></option>
    </c:forEach>
  </select>

  <label for="image">Ảnh</label>
  <c:if test="${not empty drink.image}">
    <img class="preview" src="<c:out value='${ctx}/images/${drink.image}'/>" alt="Ảnh hiện tại"
         onerror="this.onerror=null;this.src='${ctx}/images/no-image.svg'">
  </c:if>
  <input type="file" id="image" name="image" accept=".jpg,.jpeg,.png,.gif,.webp,image/*">
  <span class="hint">jpg, png, gif, webp, tối đa 5 MB. Bỏ trống để giữ ảnh hiện tại.</span>

  <label class="check">
    <input type="checkbox" name="available" value="true" ${drink.available ? 'checked' : ''}> Đang bán
  </label>

  <div class="actions">
    <button type="submit" class="primary">Lưu</button>
    <a href="${ctx}/manager/drinks">Quay lại</a>
  </div>
</form>
<%@ include file="/WEB-INF/views/_footer.jspf" %>
