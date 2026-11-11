/* ==========================================================================
   Saltair - JS nhóm "shopping" (giỏ hàng, thanh toán, đơn hàng). JS thuần, nạp sau app.js.
   Ở chế độ giao diện (demo) các form chưa có logic được xử lý tại chỗ để xem được luồng.
   Khi có backend (demoMode = false) các đoạn "if (!demo) return" để form gửi lên server bình thường.
   ========================================================================== */
(() => {
  'use strict';

  const $ = (sel, root = document) => root.querySelector(sel);
  const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));
  const S = window.Saltair || {};
  const demo = !!S.demo;
  const money = (n) => (S.formatVnd ? S.formatVnd(n) : new Intl.NumberFormat('vi-VN').format(Math.round(n)) + 'đ');

  /* ---------- Giỏ hàng ---------- */
  const cart = $('[data-cart]');
  if (cart) {
    const rows = () => $$('[data-cart-item]', cart);
    const emptyBox = $('[data-cart-empty]');
    const summary = $('[data-cart-summary]', cart);
    const shipping = Number((summary && summary.dataset.shipping) || 0);
    const setText = (sel, text) => $$(sel).forEach((el) => { el.textContent = text; });

    const recalc = () => {
      let sub = 0;
      let count = 0;
      let over = false;
      rows().forEach((row) => {
        const qty = Number($('input[name="quantity"]', row).value) || 0;
        sub += Number(row.dataset.unit) * qty;
        count += qty;
        if (qty > Number(row.dataset.stock)) over = true;
      });
      setText('[data-sum-subtotal]', money(sub));
      setText('[data-sum-total]', money(sub + shipping));
      setText('[data-sum-count]', count + ' sản phẩm');
      setText('[data-head-count]', count + ' sản phẩm');
      const headCount = $('[data-head-count]');
      if (headCount) headCount.hidden = count === 0;
      const btn = $('[data-checkout-btn]');
      if (btn) {
        btn.classList.toggle('is-disabled', over);
        btn.setAttribute('aria-disabled', String(over));
      }
      const note = $('[data-overstock-note]');
      if (note) note.hidden = !over;
    };

    // Đổi số lượng: cập nhật tiền tại chỗ rồi gửi form sau 0,7 giây (gom nhiều lần bấm)
    rows().forEach((row) => {
      const form = $('[data-cart-update]', row);
      const input = $('input[name="quantity"]', row);
      const warn = $('[data-stock-warn]', row);
      const lineTotal = $('[data-line-total]', row);
      if (!form || !input) return;
      let timer;
      input.addEventListener('change', () => {
        const qty = Math.max(1, Number(input.value) || 1);
        if (lineTotal) {
          lineTotal.textContent = money(Number(row.dataset.unit) * qty);
          lineTotal.classList.remove('is-updated');
          void lineTotal.offsetWidth;
          lineTotal.classList.add('is-updated');
        }
        if (warn) warn.hidden = qty <= Number(row.dataset.stock);
        recalc();
        clearTimeout(timer);
        timer = setTimeout(() => {
          if (typeof form.requestSubmit === 'function') form.requestSubmit(); else form.submit();
        }, 700);
      });
    });

    // Xóa dòng: ở chế độ giao diện thì thu gọn dòng ngay (hộp xác nhận của app.js chạy trước)
    $$('[data-cart-remove]', cart).forEach((form) => form.addEventListener('submit', () => {
      if (!demo || !form.hasAttribute('data-demo')) return;
      const row = form.closest('[data-cart-item]');
      if (!row) return;
      row.style.maxHeight = row.offsetHeight + 'px';
      requestAnimationFrame(() => row.classList.add('is-removing'));
      setTimeout(() => {
        row.remove();
        recalc();
        if (!rows().length) {
          cart.hidden = true;
          if (emptyBox) emptyBox.hidden = false;
        }
      }, 320);
    }));

    // Nút Thanh toán đang bị khóa (vượt tồn kho): chặn cả khi bấm bằng bàn phím
    const checkoutBtn = $('[data-checkout-btn]');
    if (checkoutBtn) checkoutBtn.addEventListener('click', (e) => {
      if (checkoutBtn.getAttribute('aria-disabled') === 'true') e.preventDefault();
    });

    recalc();
  }

  /* ---------- Thanh toán ---------- */
  const addrRadios = $$('input[type="radio"][name="addressId"]');
  const newBox = $('[data-new-address]');
  if (addrRadios.length && newBox) {
    const sync = () => {
      const sel = addrRadios.find((r) => r.checked);
      const isNew = !!sel && sel.value === 'new';
      newBox.hidden = !isNew;
      $$('[data-req]', newBox).forEach((i) => { i.required = isNew; });
    };
    addrRadios.forEach((r) => r.addEventListener('change', sync));
    sync();
  } else if (newBox) {
    // Khách chưa đăng nhập: không có sổ địa chỉ, luôn nhập địa chỉ mới
    $$('[data-req]', newBox).forEach((i) => { i.required = true; });
  }

  // Trang thanh toán mới (#86): chip gợi ý điền mã, gập mở tóm tắt trên điện thoại, chống bấm Đặt hàng hai lần
  const voucherInput = $('[data-voucher-input]');
  if (voucherInput) {
    $$('[data-voucher-fill]').forEach((b) => b.addEventListener('click', () => {
      voucherInput.value = b.dataset.voucherFill;
      voucherInput.focus();
    }));
    // Enter trong ô mã giảm giá thì áp mã, không đặt hàng
    voucherInput.addEventListener('keydown', (e) => {
      if (e.key !== 'Enter') return;
      e.preventDefault();
      $('[data-voucher-apply]')?.click();
    });
  }
  const coToggle = $('[data-co-toggle]');
  const coSummary = $('[data-co-summary]');
  if (coToggle && coSummary) {
    coToggle.addEventListener('click', () => {
      const open = coSummary.classList.toggle('is-open');
      coToggle.setAttribute('aria-expanded', String(open));
      const text = $('[data-co-toggle-text]', coToggle);
      if (text) text.textContent = open ? 'Ẩn tóm tắt đơn hàng' : 'Hiện tóm tắt đơn hàng';
    });
  }
  const coForm = $('form.co');
  if (coForm) {
    let sent = false;
    coForm.addEventListener('submit', (e) => {
      if (sent) { e.preventDefault(); return; }
      sent = true;
      const btn = e.submitter;
      if (btn && btn.hasAttribute('data-co-submit')) btn.classList.add('is-loading');
      // Khóa nút sau khi trình duyệt đã lấy tên/giá trị của nút bấm
      setTimeout(() => $$('button[type="submit"]', coForm).forEach((b) => { b.disabled = true; }), 0);
    });
    window.addEventListener('pageshow', (e) => {
      if (!e.persisted) return;
      sent = false;
      $$('button[type="submit"]', coForm).forEach((b) => { b.disabled = false; b.classList.remove('is-loading'); });
    });
  }

  const voucherForm = $('[data-voucher-form]');
  if (voucherForm) {
    // Chip gợi ý: điền mã vào ô nhập (app.js lo phần sao chép)
    $$('[data-voucher-fill]').forEach((b) => b.addEventListener('click', () => {
      const input = $('input[name="voucherCode"]', voucherForm);
      if (input) { input.value = b.dataset.voucherFill; input.focus(); }
    }));
    // Chế độ giao diện: xem trạng thái đã áp dụng qua ?voucher=MÃ
    voucherForm.addEventListener('submit', (e) => {
      if (!demo || !voucherForm.hasAttribute('data-demo')) return;
      e.preventDefault();
      const input = $('input[name="voucherCode"]', voucherForm);
      const code = ((input && input.value) || '').trim();
      if (code) window.location.href = voucherForm.getAttribute('action') + '?voucher=' + encodeURIComponent(code);
    });
  }

  // Đặt hàng: chế độ giao diện chuyển thẳng sang trang thành công (form đã qua kiểm tra hợp lệ)
  const placeOrder = $('[data-checkout-form]');
  if (placeOrder) {
    placeOrder.addEventListener('submit', (e) => {
      if (!demo || !placeOrder.hasAttribute('data-demo')) return;
      e.preventDefault();
      window.location.href = placeOrder.dataset.successUrl || '/';
    });
  }

  /* ---------- Hộp thoại hủy đơn: đóng sau khi gửi (chế độ giao diện) ---------- */
  $$('dialog form[data-demo]').forEach((f) => f.addEventListener('submit', () => {
    if (!demo) return;
    const d = f.closest('dialog');
    if (d && typeof d.close === 'function') d.close();
  }));
})();
