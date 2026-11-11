/* ==========================================================================
   Saltair - JS nhóm account (bổ sung cho app.js, không sửa file dùng chung).
   - data-dialog-reset          : nút mở hộp thoại ở chế độ "thêm mới" (xóa dữ liệu cũ)
   - dialog[data-open]          : tự mở hộp thoại khi tải trang (dùng khi server trả lỗi validate)
   - data-demo-redirect="url"   : chỉ ở demoMode: sau khi gửi form hợp lệ thì chuyển sang url để xem trạng thái kế tiếp
   - data-demo-remove="selector": chỉ ở demoMode: sau khi gửi form hợp lệ thì gỡ phần tử gần nhất khớp selector
   ========================================================================== */
(() => {
  'use strict';

  const $ = (sel, root = document) => root.querySelector(sel);
  const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));

  /* ---------- Hộp thoại: thêm mới (xóa dữ liệu cũ) ---------- */
  $$('[data-dialog-reset]').forEach((btn) => btn.addEventListener('click', () => {
    const dlg = document.getElementById(btn.dataset.dialogOpen);
    const form = dlg && $('form', dlg);
    if (!form) return;
    $$('input, select, textarea', form).forEach((f) => {
      if (f.type === 'hidden') { if (f.name === 'id') f.value = '0'; return; }
      if (f.type === 'checkbox' || f.type === 'radio') f.checked = false; else f.value = '';
    });
    $$('.field__error', form).forEach((e) => e.remove());
    $$('.is-invalid', form).forEach((e) => e.classList.remove('is-invalid'));
  }));

  /* ---------- Hộp thoại: tự mở lại khi form có lỗi ---------- */
  $$('dialog[data-open]').forEach((dlg) => {
    if (typeof dlg.showModal === 'function' && !dlg.open) dlg.showModal();
  });

  /* ---------- Chế độ giao diện (demoMode): hiển thị kết quả giả lập ---------- */
  const demo = () => !!(window.Saltair && window.Saltair.demo);
  const wait = (el, ms, done) => {
    let called = false;
    const fin = () => { if (!called) { called = true; done(); } };
    el.addEventListener('transitionend', fin, { once: true });
    setTimeout(fin, ms);
  };

  $$('form[data-demo]').forEach((form) => form.addEventListener('submit', () => {
    // app.js đã chặn gửi thật (preventDefault) khi ở demoMode và form hợp lệ
    if (!demo() || !form.checkValidity()) return;

    const dlg = form.closest('dialog');
    if (dlg) dlg.close();

    if (form.dataset.demoRedirect) {
      setTimeout(() => { window.location.href = form.dataset.demoRedirect; }, 700);
    }

    if (form.dataset.demoRemove) {
      const target = form.closest(form.dataset.demoRemove);
      if (!target) return;
      const parent = target.parentElement;
      target.style.animation = 'none';
      void target.offsetWidth;
      target.classList.add('is-removing');
      wait(target, 320, () => {
        target.remove();
        const counter = $('[data-demo-count]');
        const left = parent ? parent.children.length : 0;
        if (counter) counter.textContent = String(left);
        if (!left) {
          if (parent) parent.hidden = true;
          const empty = $('[data-demo-empty]');
          if (empty) empty.hidden = false;
        }
      });
    }
  }));
})();
