<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<% response.setStatus(403); %>
<%-- Trang lỗi 403 (#91): khối màu phủ hết chiều ngang, số lỗi mờ lớn phía sau, tiêu đề và nút ở giữa --%>
<t:layout title="Không có quyền truy cập">
  <section class="error-page" data-code="403">
    <h1 class="error-page__title">Bạn không có quyền vào trang này</h1>
    <div class="error-page__actions">
      <a class="btn error-page__btn" href="${ctx}/login">Đăng nhập</a>
      <a class="btn error-page__btn error-page__btn--ghost" href="${ctx}/home">Về trang chủ</a>
    </div>
  </section>
</t:layout>
