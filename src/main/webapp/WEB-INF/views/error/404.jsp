<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<% response.setStatus(404); %>
<t:layout title="Không tìm thấy trang">
  <section class="error-page">
    <p class="error-page__code">404</p>
    <h1 class="h-1">Không tìm thấy trang này</h1>
    <p class="t-muted">Đường dẫn có thể đã đổi hoặc sản phẩm không còn nữa.</p>
    <a class="btn btn--primary mt-16" href="${ctx}/home">Về trang chủ</a>
  </section>
</t:layout>
