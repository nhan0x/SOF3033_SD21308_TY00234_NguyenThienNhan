<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<% response.setStatus(500); %>
<t:layout title="Có lỗi xảy ra">
  <section class="error-page">
    <p class="error-page__code">500</p>
    <h1 class="h-1">Có lỗi xảy ra phía chúng tôi</h1>
    <p class="t-muted">Bạn thử tải lại trang hoặc quay về sau ít phút nhé.</p>
    <a class="btn btn--primary mt-16" href="${ctx}/home">Về trang chủ</a>
  </section>
</t:layout>
