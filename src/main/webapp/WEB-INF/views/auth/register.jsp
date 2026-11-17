<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  ĐĂNG KÝ TÀI KHOẢN KHÁCH
    GET/POST /register -> RegisterServlet
  Request attributes:
    errors    : Map<String,String>   - (tùy chọn) lỗi theo ô: email, fullName, password, confirm
  Tham số URL: redirect (trang quay lại sau khi đăng ký)
  Form fields: email, fullName, password, confirm, redirect
  app.js: data-pw-toggle (nút hiện/ẩn mật khẩu)
--%>
<t:layout title="Đăng ký" description="Tạo tài khoản Saltair.">
  <jsp:attribute name="head"><link rel="stylesheet" href="${ctx}/assets/css/pages/account.css?v=5"></jsp:attribute>
  <jsp:body>
    <div class="auth">
      <div class="auth__form">
        <div class="auth__box">
          <div class="auth__intro acc-enter">
            <span class="t-label t-muted">Tài khoản</span>
            <h1 class="h-1">Tạo tài khoản</h1>
            <p class="t-muted">Đã có tài khoản? <a class="link-underline" href="${ctx}/login${empty param.redirect ? '' : '?redirect='}${fn:escapeXml(param.redirect)}">Đăng nhập</a></p>
          </div>

          <div class="acc-enter" style="--d:1">
            <%@ include file="/WEB-INF/views/common/flash.jspf" %>
            <form class="auth-form" method="post" action="${ctx}/register">
              <input type="hidden" name="redirect" value="${fn:escapeXml(param.redirect)}">

              <div class="field">
                <label class="field__label" for="reg-name">Họ tên</label>
                <input class="input${not empty errors.fullName ? ' is-invalid' : ''}" id="reg-name" type="text" name="fullName"
                       value="${fn:escapeXml(param.fullName)}" autocomplete="name" maxlength="100" required autofocus>
                <c:if test="${not empty errors.fullName}"><span class="field__error"><c:out value="${errors.fullName}"/></span></c:if>
              </div>

              <div class="field">
                <label class="field__label" for="reg-email">Email</label>
                <input class="input${not empty errors.email ? ' is-invalid' : ''}" id="reg-email" type="email" name="email"
                       value="${fn:escapeXml(param.email)}" placeholder="ban@email.com" autocomplete="username" maxlength="150" required>
                <c:if test="${not empty errors.email}"><span class="field__error"><c:out value="${errors.email}"/></span></c:if>
              </div>

              <div class="field">
                <label class="field__label" for="reg-password">Mật khẩu</label>
                <div class="input-group">
                  <input class="input${not empty errors.password ? ' is-invalid' : ''}" id="reg-password" type="password" name="password"
                         autocomplete="new-password" minlength="8" maxlength="64" required>
                  <button type="button" class="icon-btn input-group__btn" data-pw-toggle aria-label="Hiện mật khẩu" aria-pressed="false"><t:icon name="eye"/></button>
                </div>
                <c:choose>
                  <c:when test="${not empty errors.password}"><span class="field__error"><c:out value="${errors.password}"/></span></c:when>
                  <c:otherwise><span class="field__hint">Tối thiểu 8 ký tự.</span></c:otherwise>
                </c:choose>
              </div>

              <div class="field">
                <label class="field__label" for="reg-confirm">Nhập lại mật khẩu</label>
                <div class="input-group">
                  <input class="input${not empty errors.confirm ? ' is-invalid' : ''}" id="reg-confirm" type="password" name="confirm"
                         autocomplete="new-password" maxlength="64" required>
                  <button type="button" class="icon-btn input-group__btn" data-pw-toggle aria-label="Hiện mật khẩu nhập lại" aria-pressed="false"><t:icon name="eye"/></button>
                </div>
                <c:if test="${not empty errors.confirm}"><span class="field__error"><c:out value="${errors.confirm}"/></span></c:if>
              </div>

              <button class="btn btn--primary btn--block" type="submit">Tạo tài khoản</button>
            </form>
          </div>
        </div>
      </div>
    </div>
  </jsp:body>
</t:layout>
