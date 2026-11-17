<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  ĐĂNG NHẬP BẰNG EMAIL + MẬT KHẨU: một form cho cả khách và admin
    GET/POST /login -> LoginServlet. Đăng nhập xong: Admin vào /admin, khách về /home (hoặc trang redirect).
  Request attributes:
    errors : Map<String,String> - (tùy chọn) lỗi theo ô: email, password
  Tham số URL: redirect (trang quay lại sau khi đăng nhập), email (điền sẵn)
  Form fields: email, password, redirect
  app.js: data-pw-toggle (nút hiện/ẩn mật khẩu)
--%>
<t:layout title="Đăng nhập" description="Đăng nhập tài khoản Saltair.">
  <jsp:attribute name="head"><link rel="stylesheet" href="${ctx}/assets/css/pages/account.css?v=5"></jsp:attribute>
  <jsp:body>
    <div class="auth">
      <div class="auth__form">
        <div class="auth__box">
          <div class="auth__intro acc-enter">
            <span class="t-label t-muted">Tài khoản</span>
            <h1 class="h-1">Đăng nhập</h1>
            <p class="t-muted">Chưa có tài khoản? <a class="link-underline" href="${ctx}/register${empty param.redirect ? '' : '?redirect='}${fn:escapeXml(param.redirect)}">Đăng ký</a></p>
          </div>

          <div class="acc-enter" style="--d:1">
            <%@ include file="/WEB-INF/views/common/flash.jspf" %>
            <form class="auth-form" method="post" action="${ctx}/login">
              <input type="hidden" name="redirect" value="${fn:escapeXml(param.redirect)}">

              <div class="field">
                <label class="field__label" for="login-email">Email</label>
                <input class="input${not empty errors.email ? ' is-invalid' : ''}" id="login-email" type="email" name="email"
                       value="${fn:escapeXml(param.email)}" placeholder="ban@email.com" autocomplete="username" maxlength="150" required autofocus>
                <c:if test="${not empty errors.email}"><span class="field__error"><c:out value="${errors.email}"/></span></c:if>
              </div>

              <div class="field">
                <label class="field__label" for="login-password">Mật khẩu</label>
                <div class="input-group">
                  <input class="input${not empty errors.password ? ' is-invalid' : ''}" id="login-password" type="password" name="password"
                         autocomplete="current-password" maxlength="64" required>
                  <button type="button" class="icon-btn input-group__btn" data-pw-toggle aria-label="Hiện mật khẩu" aria-pressed="false"><t:icon name="eye"/></button>
                </div>
                <c:if test="${not empty errors.password}"><span class="field__error"><c:out value="${errors.password}"/></span></c:if>
              </div>

              <button class="btn btn--primary btn--block" type="submit">Đăng nhập</button>
            </form>
          </div>
        </div>
      </div>
    </div>
  </jsp:body>
</t:layout>
