/* ==========================================================================
   Saltair - JS nhóm account (bổ sung cho app.js, không sửa file dùng chung).
   - data-dialog-reset          : nút mở hộp thoại ở chế độ "thêm mới" (xóa dữ liệu cũ)
   - dialog[data-open]          : tự mở hộp thoại khi tải trang (dùng khi server trả lỗi validate)
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
})();
