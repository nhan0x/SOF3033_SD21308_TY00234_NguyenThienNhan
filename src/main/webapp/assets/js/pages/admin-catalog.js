/* ==========================================================================
   Admin catalog: JavaScript riêng cho Tổng quan, Form sản phẩm, Danh mục, Màu và Size.
   Mỗi khối bật bằng thuộc tính data-* trong JSP, không phụ thuộc thư viện.
   ========================================================================== */
(() => {
  'use strict';

  const $ = (sel, root = document) => root.querySelector(sel);
  const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));
  const toast = (msg) => { if (window.Saltair && window.Saltair.toast) window.Saltair.toast(msg); };

  /* ---------- Bỏ dấu tiếng Việt để tạo slug ---------- */
  function slugify(text) {
    return String(text || '')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/đ/g, 'd').replace(/Đ/g, 'd')
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '')
      .slice(0, 160);
  }

  /* Ô slug tự điền theo ô tên, dừng khi người dùng tự sửa slug.
     Dùng: <input data-slug-from="#id-o-ten"> trên ô slug. */
  $$('[data-slug-from]').forEach((slug) => {
    const source = $(slug.dataset.slugFrom);
    if (!source) return;
    // Ô slug đã có giá trị (đang sửa) thì không tự đổi theo tên, tránh làm hỏng đường dẫn cũ
    let lastAuto = '';
    source.addEventListener('input', () => {
      if (slug.value === '' || slug.value === lastAuto) {
        lastAuto = slugify(source.value);
        slug.value = lastAuto;
      }
    });
    const form = slug.form;
    if (form) {
      form.addEventListener('reset', () => { lastAuto = ''; });
    }
    const dlg = slug.closest('dialog');
    if (dlg) dlg.addEventListener('close', () => { lastAuto = ''; });
  });

  /* ---------- Xem trước ảnh khi chọn file ----------
     <input type="file" data-image-preview="#khung-xem-truoc" data-image-name="#ten-file" data-image-error="#loi"> */
  const MAX_BYTES = 2 * 1024 * 1024;
  const TYPES = ['image/jpeg', 'image/png'];
  $$('input[type="file"][data-image-preview]').forEach((input) => {
    const box = $(input.dataset.imagePreview);
    const nameEl = input.dataset.imageName ? $(input.dataset.imageName) : null;
    const errEl = input.dataset.imageError ? $(input.dataset.imageError) : null;
    if (!box) return;
    let original = null;
    let objectUrl = null;

    const setError = (msg) => { if (errEl) errEl.textContent = msg || ''; };
    const restore = () => {
      if (original !== null) box.innerHTML = original;
      if (nameEl) nameEl.textContent = '';
    };
    const show = (file) => {
      if (original === null) original = box.innerHTML;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
      objectUrl = URL.createObjectURL(file);
      const img = document.createElement('img');
      img.src = objectUrl;
      img.alt = 'Ảnh đã chọn: ' + file.name;
      box.replaceChildren(img);
      if (nameEl) nameEl.textContent = file.name + ' (' + (file.size / 1024).toFixed(0) + ' KB)';
    };

    input.addEventListener('change', () => {
      setError('');
      const file = input.files && input.files[0];
      if (!file) { restore(); return; }
      if (!TYPES.includes(file.type)) {
        setError('Chỉ nhận ảnh JPG hoặc PNG.');
        input.value = '';
        restore();
        return;
      }
      if (file.size > MAX_BYTES) {
        setError('Ảnh lớn hơn 2MB, vui lòng chọn ảnh nhỏ hơn.');
        input.value = '';
        restore();
        return;
      }
      show(file);
    });

    // Đặt lại form hoặc đóng hộp thoại: trả khung về ảnh ban đầu
    const reset = () => { setError(''); restore(); original = null; };
    if (input.form) input.form.addEventListener('reset', () => setTimeout(reset, 0));

    // Kéo thả file vào khung tải ảnh
    const drop = input.closest('.upload-box');
    if (drop) {
      ['dragenter', 'dragover'].forEach((ev) => drop.addEventListener(ev, (e) => { e.preventDefault(); drop.classList.add('is-dragover'); }));
      ['dragleave', 'drop'].forEach((ev) => drop.addEventListener(ev, (e) => { e.preventDefault(); drop.classList.remove('is-dragover'); }));
      drop.addEventListener('drop', (e) => {
        if (e.dataTransfer && e.dataTransfer.files && e.dataTransfer.files.length) {
          input.files = e.dataTransfer.files;
          input.dispatchEvent(new Event('change', { bubbles: true }));
        }
      });
    }
  });

  /* ---------- Ma trận biến thể: hiện/ẩn dòng theo màu đã chọn, cộng tổng tồn ----------
     <div data-variant-matrix> chứa checkbox [data-color-toggle] và <tr data-color-row="ID"> */
  $$('[data-variant-matrix]').forEach((root) => {
    const toggles = $$('[data-color-toggle]', root);
    const rows = $$('[data-color-row]', root);
    const empty = $('[data-variant-empty]', root);
    const table = $('[data-variant-table]', root);
    const sizeCols = $$('[data-size-col]', root);

    const sync = () => {
      const on = new Set(toggles.filter((t) => t.checked).map((t) => t.value));
      rows.forEach((tr) => {
        const show = on.has(tr.dataset.colorRow);
        tr.hidden = !show;
        $$('input', tr).forEach((inp) => { inp.disabled = !show; });
      });
      if (empty) empty.hidden = on.size > 0;
      if (table) table.hidden = on.size === 0;
      totals();
    };

    const totals = () => {
      const sums = {};
      let all = 0;
      rows.forEach((tr) => {
        if (tr.hidden) return;
        let rowSum = 0;
        $$('input[data-size]', tr).forEach((inp) => {
          const v = Math.max(0, parseInt(inp.value, 10) || 0);
          rowSum += v;
          sums[inp.dataset.size] = (sums[inp.dataset.size] || 0) + v;
        });
        const cell = $('[data-row-total]', tr);
        if (cell) cell.textContent = String(rowSum);
        all += rowSum;
      });
      sizeCols.forEach((c) => { c.textContent = String(sums[c.dataset.sizeCol] || 0); });
      const grand = $('[data-grand-total]', root);
      if (grand) grand.textContent = String(all);
    };

    toggles.forEach((t) => t.addEventListener('change', sync));
    root.addEventListener('input', (e) => { if (e.target.matches('input[data-size]')) totals(); });
    sync();
  });

  /* ---------- Bộ lọc ngày: ngày kết thúc không trước ngày bắt đầu ---------- */
  $$('[data-date-range]').forEach((form) => {
    const from = $('input[name="dateFrom"]', form);
    const to = $('input[name="dateTo"]', form);
    if (!from || !to) return;
    const sync = () => { to.min = from.value || ''; from.max = to.value || ''; };
    from.addEventListener('change', sync);
    to.addEventListener('change', sync);
    sync();
  });

  /* ---------- Hộp thoại: xóa dữ liệu nhập khi đóng, tự mở lại khi có lỗi ---------- */
  $$('dialog[data-reset-on-close]').forEach((d) => {
    d.addEventListener('close', () => {
      const form = $('form', d);
      if (form) form.reset();
    });
  });
  // Chế độ giao diện: app.js chặn gửi form (preventDefault) thì đóng hộp thoại để người xem thấy kết quả
  $$('dialog form').forEach((form) => {
    form.addEventListener('submit', (e) => {
      if (e.defaultPrevented) { const d = form.closest('dialog'); if (d) d.close(); }
    });
  });
  $$('dialog[data-open-on-load]').forEach((d) => {
    if (typeof d.showModal === 'function' && !d.open) d.showModal();
  });

  /* ---------- Hộp thoại thêm mới: dọn lỗi cũ khi mở lại ---------- */
  $$('[data-dialog-open]').forEach((b) => b.addEventListener('click', () => {
    const d = document.getElementById(b.dataset.dialogOpen);
    if (!d) return;
    $$('.field__error', d).forEach((e) => { e.hidden = true; });
    $$('.is-invalid', d).forEach((e) => e.classList.remove('is-invalid'));
    const first = $('input:not([type="hidden"]):not([type="file"])', d);
    if (first) setTimeout(() => first.focus(), 60);
  }));

  /* ---------- Form sản phẩm: đánh dấu ô chưa hợp lệ và báo một lần ---------- */
  $$('form.ac-form').forEach((form) => {
    let notified = false;
    form.addEventListener('invalid', (e) => {
      const el = e.target;
      if (el && el.classList) el.classList.add('is-invalid');
      if (!notified) {
        notified = true;
        toast('Vui lòng kiểm tra lại các trường chưa hợp lệ.');
        setTimeout(() => { notified = false; }, 800);
      }
    }, true);
    form.addEventListener('input', (e) => {
      if (e.target.classList) e.target.classList.remove('is-invalid');
    });
  });
})();
