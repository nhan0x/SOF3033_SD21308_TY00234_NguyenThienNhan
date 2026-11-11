<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<% response.setStatus(403); %>
<t:layout title="Không có quyền truy cập">
  <section class="error-page">
    <p class="error-page__code">403</p>
    <h1 class="h-1">Bạn không có quyền vào trang này</h1>
    <p class="t-muted">Hãy đăng nhập bằng tài khoản có quyền phù hợp.</p>
    <div class="row mt-16">
      <a class="btn btn--primary" href="${ctx}/login">Đăng nhập</a>
      <a class="btn btn--outline" href="${ctx}/home">Về trang chủ</a>
    </div>
  </section>
</t:layout>
