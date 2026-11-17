<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<% response.setStatus(404); %>
<%-- Trang lỗi 404 (#91): khối màu phủ hết chiều ngang, số lỗi mờ lớn phía sau, tiêu đề và nút ở giữa --%>
<t:layout title="Không tìm thấy trang">
  <section class="error-page" data-code="404">
    <h1 class="error-page__title">Không tìm thấy trang</h1>
    <div class="error-page__actions">
      <a class="btn error-page__btn" href="${ctx}/products">Tiếp tục mua sắm</a>
    </div>
  </section>
</t:layout>
