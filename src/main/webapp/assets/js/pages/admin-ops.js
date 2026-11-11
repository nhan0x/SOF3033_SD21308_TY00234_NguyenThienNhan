/* ==========================================================================
   Saltair - Admin vận hành (orders, users, banners, blogs, vouchers, reviews)
   JS thuần, bổ sung cho app.js. Mỗi khối kích hoạt bằng thuộc tính data-*:
   - [data-dialog-open].js-fill : đổ dữ liệu vào dialog từ thuộc tính data-field-* (kebab-case -> camelCase)
   - [data-bind="x"]            : phần tử trong dialog hiện giá trị data-field-x
   - [data-upload]              : khung tải ảnh có xem trước
   - [data-slug-source]         : tự tạo slug từ tiêu đề
   - [data-voucher-form]        : quy tắc ô giảm giá của voucher
   - dialog[data-auto-open]     : mở lại dialog khi server trả lỗi
   ========================================================================== */
(() => {
  'use strict';

  const $ = (sel, root = document) => root.querySelector(sel);
  const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));
  const toast = (msg) => (window.Saltair && window.Saltair.toast ? window.Saltair.toast(msg) : undefined);
  const isDemo = () => !!(window.Saltair && window.Saltair.demo);

  /* ---------- Đổ dữ liệu vào dialog trước khi app.js mở nó (pha capture) ---------- */
  function lowerFirst(s) { return s.charAt(0).toLowerCase() + s.slice(1); }

  document.addEventListener('click', (e) => {
    const btn = e.target.closest('[data-dialog-open].js-fill');
    if (!btn) return;
    const dialog = document.getElementById(btn.dataset.dialogOpen);
    if (!dialog) return;
    const form = $('form', dialog);
    if (form) {
      form.reset();
      // Ô ẩn (id...) không quay về rỗng sau reset() nếu lần trước đã gán giá trị; xóa tay, chỉ giữ ô action
      $$('input[type="hidden"]', form).forEach((h) => { if (h.name !== 'action') h.value = ''; });
    }
    $$('.field__error', dialog).forEach((el) => el.remove());
    $$('.is-invalid', dialog).forEach((el) => el.classList.remove('is-invalid'));
    $$('[data-upload]', dialog).forEach(resetUpload);
    $$('[data-bind]', dialog).forEach((el) => { el.textContent = ''; });

    Object.keys(btn.dataset).forEach((key) => {
      if (key.indexOf('field') !== 0 || key.length <= 5) return;
      const name = lowerFirst(key.slice(5));
      const value = btn.dataset[key];
      const field = form ? form.elements[name] : null;
      if (field && field.type === 'checkbox') field.checked = value === 'true';
      else if (field && field.type !== 'file') field.value = value;
      $$('[data-bind="' + name + '"]', dialog).forEach((el) => { el.textContent = value; });
    });

    // Ảnh hiện có (nếu có) hiện làm xem trước
    $$('[data-upload]', dialog).forEach((box) => {
      const url = btn.dataset.fieldImage;
      if (url) showPreview(box, url);
    });
    dialog.dispatchEvent(new CustomEvent('ops:filled', { bubbles: true }));
  }, true);

  /* ---------- Khung tải ảnh có xem trước ---------- */
  function showPreview(box, url) {
    const holder = $('[data-upload-preview]', box);
    if (!holder) return;
    let img = $('img', holder);
    if (!img) { img = document.createElement('img'); img.alt = 'Xem trước ảnh'; holder.appendChild(img); }
    img.src = url;
    holder.hidden = false;
    box.classList.add('has-image');
  }
  function resetUpload(box) {
    const holder = $('[data-upload-preview]', box);
    const initial = box.dataset.initial || '';
    if (holder && !initial) {
      holder.hidden = true;
      const img = $('img', holder);
      if (img) img.remove();
      box.classList.remove('has-image');
    }
  }
  $$('[data-upload]').forEach((box) => {
    const input = $('input[type="file"]', box);
    if (!input) return;
    let objectUrl = null;
    input.addEventListener('change', () => {
      const file = input.files && input.files[0];
      if (!file) return;
      if (!/^image\//.test(file.type)) {
        input.value = '';
        toast('Vui lòng chọn một tệp ảnh (JPG, PNG hoặc WebP).');
        return;
      }
      if (file.size > 2 * 1024 * 1024) {
        input.value = '';
        toast('Ảnh quá lớn, vui lòng chọn ảnh dưới 2MB.');
        return;
      }
      if (objectUrl) URL.revokeObjectURL(objectUrl);
      objectUrl = URL.createObjectURL(file);
      showPreview(box, objectUrl);
    });
  });

  /* ---------- Tự tạo slug từ tiêu đề (bỏ dấu tiếng Việt) ---------- */
  function slugify(text) {
    return text
      .normalize('NFD').replace(/[̀-ͯ]/g, '')
      .replace(/đ/g, 'd').replace(/Đ/g, 'd')
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '');
  }
  $$('[data-slug-source]').forEach((source) => {
    const target = document.getElementById(source.dataset.slugSource);
    if (!target) return;
    let manual = target.value.trim() !== '' && target.dataset.auto !== 'true';
    target.addEventListener('input', () => { manual = target.value.trim() !== ''; });
    source.addEventListener('input', () => { if (!manual) target.value = slugify(source.value); });
  });

  /* ---------- Voucher: mã viết hoa, ô giảm tối đa chỉ dùng cho PERCENT ---------- */
  $$('[data-voucher-form]').forEach((form) => {
    const code = form.elements.code;
    const type = form.elements.discountType;
    const value = form.elements.discountValue;
    const max = form.elements.maxDiscount;
    const unit = $('[data-discount-unit]', form);
    const hidden = form.elements.id;

    const sync = () => {
      const percent = type.value === 'PERCENT';
      max.disabled = !percent;
      if (!percent) max.value = '';
      value.max = percent ? '100' : '';
      if (unit) unit.textContent = percent ? '(từ 1 đến 100 %)' : '(đồng)';
      const editing = hidden && hidden.value !== '';
      code.readOnly = editing;
      code.title = editing ? 'Không sửa mã sau khi đã tạo' : '';
    };
    code.addEventListener('input', () => {
      const pos = code.selectionStart;
      code.value = code.value.toUpperCase().replace(/[^A-Z0-9]/g, '');
      try { code.setSelectionRange(pos, pos); } catch (err) { /* bỏ qua */ }
    });
    type.addEventListener('change', sync);
    const dialog = form.closest('dialog');
    if (dialog) dialog.addEventListener('ops:filled', sync);
    sync();
  });

  /* ---------- Công tắc tự gửi: trong chế độ giao diện chỉ hiện thông báo ---------- */
  document.addEventListener('change', (e) => {
    const el = e.target;
    if (!(el instanceof HTMLInputElement) || !el.hasAttribute('data-autosubmit')) return;
    const form = el.form;
    if (!isDemo() || !form || !form.hasAttribute('data-demo')) return;
    e.stopImmediatePropagation();
    toast(form.dataset.demoMessage || 'Đang ở chế độ giao diện: chức năng này sẽ được gắn logic sau.');
  }, true);

  /* ---------- Mở lại dialog khi server trả lỗi ---------- */
  $$('dialog[data-auto-open="true"]').forEach((d) => {
    if (typeof d.showModal === 'function' && !d.open) d.showModal();
  });
})();
