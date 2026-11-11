<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  NHẬP MÃ 6 SỐ (#74 logic, #75 giao diện). Trang riêng, không có header và footer của shop.
    GET /login/code        -> LoginCodeServlet   (mode = "customer"), POST /login/code, gửi lại: POST /login/code/resend
    GET /admin/login/code  -> AdminLoginServlet  (mode = "admin"),    POST /admin/login/code, gửi lại: action=resend
  Request attributes:
    mode       : "customer" | "admin"
    email      : String              - email đang chờ (lấy từ session, không có trên URL)
    devCode    : String              - (chỉ khi auth.devCode=true) mã vừa tạo
    resendWait : int                 - số giây còn phải chờ trước khi gửi lại được
    errors     : Map<String,String>  - (tùy chọn) lỗi: code, admin
  Có JS: gõ đủ 6 số thì tự gửi bằng fetch (Accept: application/json). Tắt JS: 6 ô tên "d" gửi bằng nút "Xác nhận".
--%>
<c:set var="ctx" value="${pageContext.request.contextPath}" scope="request"/>
<c:set var="isAdmin" value="${mode eq 'admin'}"/>
<c:set var="codeAction" value="${isAdmin ? '/admin/login/code' : '/login/code'}"/>
<c:set var="resendAction" value="${isAdmin ? '/admin/login/code' : '/login/code/resend'}"/>
<c:set var="codeError" value="${not empty errors.code ? errors.code : errors.admin}"/>
<!DOCTYPE html>
<html lang="vi" class="no-js">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="robots" content="noindex">
  <title>Nhập mã | Saltair</title>
  <link rel="icon" type="image/png" href="${ctx}/assets/brand/saltair-logo.png">
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;700;800&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="${ctx}/assets/css/style.css">
  <link rel="stylesheet" href="${ctx}/assets/css/pages/account.css">
</head>
<body class="code-page">
<%@ include file="/WEB-INF/views/common/icons.jspf" %>
<header class="code-page__logo">
  <a href="${ctx}/home" aria-label="Saltair - Trang chủ"><img src="${ctx}/assets/brand/saltair-logo.png" alt="Saltair"></a>
</header>

<main class="code-page__main">
  <h1 class="code-page__title">Nhập mã</h1>
  <p class="code-page__sub">Đã gửi tới <strong><c:out value="${email}"/></strong> ·
    <a class="code-page__link" href="${ctx}${isAdmin ? '/admin/login' : '/home?login=1'}">Đổi email</a></p>

  <%@ include file="/WEB-INF/views/common/flash.jspf" %>
  <c:if test="${not empty devCode}">
    <p class="auth__devcode" data-dev-code="${devCode}">Chế độ thử: mã là <strong>${devCode}</strong></p>
  </c:if>

  <form class="otp${not empty codeError ? ' is-error' : ''}" method="post" action="${ctx}${codeAction}" data-otp novalidate>
    <div class="otp__boxes" role="group" aria-label="Mã 6 số">
      <c:forEach begin="1" end="6" var="i">
        <input class="otp__box" name="d" type="text" inputmode="numeric" maxlength="6"
               autocomplete="${i == 1 ? 'one-time-code' : 'off'}" aria-label="Số thứ ${i}" ${i == 1 ? 'autofocus' : ''} required>
      </c:forEach>
      <span class="otp__spinner spinner" aria-hidden="true"></span>
    </div>
    <p class="otp__msg" role="alert" data-otp-msg${empty codeError ? ' hidden' : ''}>
      <c:out value="${codeError}"/>
      <c:if test="${not empty errors.admin}"> <a href="${ctx}/admin/login">Tới trang quản trị</a></c:if>
    </p>
    <noscript><button class="btn btn--primary btn--block" type="submit">Xác nhận</button></noscript>
  </form>

  <form class="code-page__resend" method="post" action="${ctx}${resendAction}">
    <c:if test="${isAdmin}"><input type="hidden" name="action" value="resend"></c:if>
    <span>Chưa nhận được mã?</span>
    <button type="submit" data-resend-wait="${resendWait}">Gửi lại</button>
  </form>

  <p class="code-page__terms">Khi tiếp tục, bạn đồng ý với Điều khoản sử dụng của Saltair.</p>
</main>

<script>
  document.documentElement.classList.remove('no-js');
  (() => {
    'use strict';
    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    /* Đếm ngược nút "Gửi lại" */
    const resend = document.querySelector('[data-resend-wait]');
    if (resend) {
      let left = parseInt(resend.dataset.resendWait, 10) || 0;
      const label = resend.textContent;
      const tick = () => {
        if (left <= 0) { resend.disabled = false; resend.textContent = label; return; }
        resend.disabled = true; resend.textContent = label + ' sau ' + left + 's'; left--; setTimeout(tick, 1000);
      };
      tick();
    }

    /* 6 ô số: tự nhảy ô, Backspace lùi ô, dán cả mã, đủ 6 số thì tự gửi */
    const form = document.querySelector('[data-otp]');
    if (!form) return;
    const boxes = Array.from(form.querySelectorAll('.otp__box'));
    const msg = form.querySelector('[data-otp-msg]');
    let busy = false;

    const value = () => boxes.map((b) => b.value).join('');
    const fill = (digits, from) => {
      let i = from;
      for (const d of digits) { if (i >= boxes.length) break; boxes[i].value = d; i++; }
      boxes[Math.min(i, boxes.length - 1)].focus();
      if (value().length === boxes.length) submit();
    };
    const clearError = () => { form.classList.remove('is-error'); if (msg) msg.hidden = true; };

    const fail = (text, adminUrl) => {
      busy = false;
      form.classList.remove('is-busy');
      boxes.forEach((b) => { b.value = ''; b.disabled = false; });
      if (msg) {
        msg.textContent = text;
        if (adminUrl) {
          const a = document.createElement('a'); a.href = adminUrl; a.textContent = 'Tới trang quản trị';
          msg.append(' ', a);
        }
        msg.hidden = false;
      }
      form.classList.remove('is-error');
      void form.offsetWidth; // chạy lại hiệu ứng rung
      form.classList.add('is-error');
      boxes[0].focus();
    };

    async function submit() {
      if (busy) return;
      const code = value();
      if (!/^\d{6}$/.test(code)) return;
      busy = true;
      form.classList.add('is-busy');
      boxes.forEach((b) => { b.disabled = true; });
      try {
        const res = await fetch(form.action, {
          method: 'POST',
          headers: { Accept: 'application/json', 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
          body: new URLSearchParams({ code }),
          credentials: 'same-origin'
        });
        const data = await res.json();
        if (data.ok) { location.href = data.redirect; return; }
        fail(data.error || 'Mã không đúng.', data.adminUrl);
      } catch (err) {
        fail('Không kết nối được, vui lòng thử lại.');
      }
    }

    boxes.forEach((box, i) => {
      box.addEventListener('focus', () => box.select());
      box.addEventListener('input', () => {
        clearError();
        const digits = box.value.replace(/\D/g, '');
        box.value = '';
        if (digits) fill(digits, i);
      });
      box.addEventListener('keydown', (e) => {
        if (e.key === 'Backspace' && !box.value && i > 0) {
          e.preventDefault();
          boxes[i - 1].value = '';
          boxes[i - 1].focus();
        } else if (e.key === 'ArrowLeft' && i > 0) {
          e.preventDefault(); boxes[i - 1].focus();
        } else if (e.key === 'ArrowRight' && i < boxes.length - 1) {
          e.preventDefault(); boxes[i + 1].focus();
        } else if (e.key === 'Enter') {
          e.preventDefault(); submit();
        }
      });
      box.addEventListener('paste', (e) => {
        const digits = (e.clipboardData || window.clipboardData).getData('text').replace(/\D/g, '').slice(0, 6);
        if (!digits) return;
        e.preventDefault();
        clearError();
        fill(digits, digits.length === 6 ? 0 : i);
      });
    });
    form.addEventListener('submit', (e) => { e.preventDefault(); submit(); });
    if (reduceMotion) form.classList.add('no-shake');
  })();
</script>
</body>
</html>
