<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  ĐĂNG NHẬP BẰNG EMAIL - bước 1 (#74)
    GET /login        -> LoginServlet       (mode = "customer"), form POST /login/email
    GET /admin/login  -> AdminLoginServlet  (mode = "admin"),    form POST /admin/login
  Request attributes:
    mode      : "customer" | "admin"
    errors    : Map<String,String>   - (tùy chọn) lỗi theo ô: email; admin (email quản trị nhập ở trang khách)
  Tham số URL: redirect (trang quay lại sau khi đăng nhập), email (điền sẵn, ví dụ từ ô đăng ký nhận tin ở trang chủ)
  Form fields: email, redirect
--%>
<c:set var="isAdmin" value="${mode eq 'admin'}"/>
<t:layout title="${isAdmin ? 'Đăng nhập quản trị' : 'Đăng nhập'}" description="Đăng nhập hoặc tạo tài khoản Saltair bằng email, không cần mật khẩu.">
  <jsp:attribute name="head"><link rel="stylesheet" href="${ctx}/assets/css/pages/account.css?v=5"></jsp:attribute>
  <jsp:body>
    <%-- Tối giản (#6): không ảnh, form canh giữa trang --%>
    <div class="auth">
      <div class="auth__box">
        <div class="auth__intro acc-enter">
          <span class="t-label t-muted">${isAdmin ? 'Trang quản trị' : 'Không cần mật khẩu'}</span>
          <h1 class="auth__title">${isAdmin ? 'Đăng nhập quản trị' : 'Đăng nhập hoặc tạo tài khoản'}</h1>
          <p class="t-muted">${isAdmin ? 'Nhập email quản trị để nhận mã 6 số, mã hết hạn sau 5 phút.' : 'Nhập email để nhận mã đăng nhập 6 số.'}</p>
        </div>

        <div class="acc-enter" style="--d:1">
          <%@ include file="/WEB-INF/views/common/flash.jspf" %>
          <form class="auth-form" method="post" action="${ctx}${isAdmin ? '/admin/login' : '/login/email'}">
            <input type="hidden" name="redirect" value="${fn:escapeXml(param.redirect)}">

            <div class="field">
              <label class="sr-only" for="login-email">Email</label>
              <input class="input auth__input${not empty errors.email or not empty errors.admin ? ' is-invalid' : ''}" id="login-email" type="email" name="email"
                     value="${fn:escapeXml(param.email)}" placeholder="Email của bạn" autocomplete="email" maxlength="150" required autofocus>
              <c:if test="${not empty errors.email}"><span class="field__error"><c:out value="${errors.email}"/></span></c:if>
              <c:if test="${not empty errors.admin}">
                <span class="field__error"><c:out value="${errors.admin}"/> <a class="link-underline" href="${ctx}/admin/login">Tới trang đăng nhập quản trị</a></span>
              </c:if>
            </div>

            <button class="btn btn--primary btn--block" type="submit">Nhận mã đăng nhập</button>
          </form>
        </div>

        <c:if test="${not isAdmin}">
          <p class="auth__note acc-enter" style="--d:2">Email mới sẽ được tạo tài khoản tự động.</p>
        </c:if>
      </div>
    </div>
  </jsp:body>
</t:layout>
