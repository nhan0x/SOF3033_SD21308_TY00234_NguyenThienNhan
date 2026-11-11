<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<% response.setStatus(500); %>
<%-- Trang lỗi 500 (#91): khối màu phủ hết chiều ngang, số lỗi mờ lớn phía sau, tiêu đề và nút ở giữa --%>
<t:layout title="Có lỗi xảy ra">
  <section class="error-page" data-code="500">
    <h1 class="error-page__title">Có lỗi xảy ra phía chúng tôi</h1>
    <div class="error-page__actions">
      <a class="btn error-page__btn" href="${ctx}/home">Về trang chủ</a>
    </div>
  </section>
</t:layout>
