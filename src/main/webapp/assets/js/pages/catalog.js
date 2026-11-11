/* ==========================================================================
   Saltair - JS riêng cho nhóm Catalog (nạp sau app.js).
   Mỗi khối chỉ chạy khi trang có phần tử tương ứng:
   - [data-product-grid] + .view-toggle : đổi kiểu lưới (product-list)
   - [data-filter-form]                  : ngăn kéo lọc (product-list)
   - [data-variant-form]                 : chọn màu/size/số lượng (product-detail)
   - [data-zoom], [data-pdp-sticky]      : phóng ảnh, thanh thêm vào giỏ trên điện thoại
   - [data-read-progress]                : thanh tiến độ đọc (blog-detail)
   ========================================================================== */
(() => {
  'use strict';

  const $ = (sel, root = document) => root.querySelector(sel);
  const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));
  const canHover = window.matchMedia('(hover: hover)').matches;
  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  // localStorage có thể bị chặn hoặc ném lỗi: luôn bọc try/catch, trang vẫn chạy khi không lưu được
  const store = {
    get(key) { try { return window.localStorage.getItem(key); } catch (e) { return null; } },
    set(key, value) { try { window.localStorage.setItem(key, value); } catch (e) { /* bỏ qua */ } }
  };

  /* ---------- 1. Đổi kiểu lưới sản phẩm ---------- */
  (() => {
    const grid = $('[data-product-grid]');
    const toggle = $('.view-toggle');
    if (!grid || !toggle) return;
    const buttons = $$('[data-view]', toggle);
    const apply = (view) => {
      grid.dataset.view = view;
      buttons.forEach((b) => {
        const on = b.dataset.view === view;
        b.classList.toggle('is-active', on);
        b.setAttribute('aria-pressed', String(on));
      });
    };
    const saved = store.get('saltair.catalogView');
    if (saved === 'large' || saved === 'dense') apply(saved);
    buttons.forEach((b) => b.addEventListener('click', () => {
      const view = b.dataset.view;
      if (view === grid.dataset.view) return;
      grid.classList.add('is-switching'); // mờ nhẹ rồi đổi cột cho đỡ giật
      setTimeout(() => {
        apply(view);
        store.set('saltair.catalogView', view);
        grid.classList.remove('is-switching');
      }, reduceMotion ? 0 : 150);
    }));
  })();

  /* ---------- 2. Ngăn kéo bộ lọc ---------- */
  (() => {
    const form = $('[data-filter-form]');
    if (!form) return;

    // Màu và size: bấm lại lựa chọn đang chọn thì bỏ chọn
    $$('[data-toggle-group]', form).forEach((group) => {
      const inputs = $$('input[type="radio"]', group);
      let last = inputs.find((i) => i.checked) || null;
      inputs.forEach((i) => i.addEventListener('click', () => {
        if (last === i) { i.checked = false; last = null; } else { last = i; }
        i.dispatchEvent(new Event('change', { bubbles: true }));
      }));
    });

    // Tên màu đang chọn cạnh tiêu đề "Màu"
    const colorName = $('[data-color-name]', form);
    const colorInputs = $$('input[name="color"]', form);
    const syncColor = () => {
      if (!colorName) return;
      const on = colorInputs.find((i) => i.checked);
      colorName.textContent = on ? (on.dataset.name || '') : '';
    };
    colorInputs.forEach((i) => i.addEventListener('change', syncColor));
    syncColor();

    // Khoảng giá nhanh
    const minInput = $('[name="minPrice"]', form);
    const maxInput = $('[name="maxPrice"]', form);
    const quick = $$('[data-price-quick]', form);
    const syncQuick = () => quick.forEach((b) => {
      const on = (b.dataset.min || '') === (minInput ? minInput.value : '') && (b.dataset.max || '') === (maxInput ? maxInput.value : '');
      b.classList.toggle('is-active', on);
      b.setAttribute('aria-pressed', String(on));
    });
    quick.forEach((b) => b.addEventListener('click', () => {
      if (!minInput || !maxInput) return;
      const wasOn = b.classList.contains('is-active');
      minInput.value = wasOn ? '' : (b.dataset.min || '');
      maxInput.value = wasOn ? '' : (b.dataset.max || '');
      syncQuick();
    }));
    [minInput, maxInput].forEach((i) => i && i.addEventListener('input', syncQuick));
    syncQuick();

    // Gọn URL: bỏ tham số rỗng; đảo khoảng giá nếu nhập ngược; không thêm sort=newest nếu trang chưa chọn sort
    form.addEventListener('submit', () => {
      if (minInput && maxInput && minInput.value !== '' && maxInput.value !== '' && Number(minInput.value) > Number(maxInput.value)) {
        const tmp = minInput.value; minInput.value = maxInput.value; maxInput.value = tmp;
      }
      $$('input', form).forEach((i) => {
        if (i.type === 'radio' && !i.checked) return;
        if (i.value === '') i.disabled = true;
        if (i.name === 'sort' && i.value === 'newest' && form.dataset.sortExplicit !== 'true') i.disabled = true;
      });
    });
    // Quay lại bằng nút Back: bật lại các ô đã bị tắt khi gửi
    window.addEventListener('pageshow', (e) => {
      if (e.persisted) $$('input:disabled', form).forEach((i) => { i.disabled = false; });
    });
  })();

  /* ---------- 3. Chi tiết sản phẩm: chọn màu / size / số lượng ---------- */
  (() => {
    const form = $('[data-variant-form]');
    if (!form) return;

    let variants = [];
    try { variants = JSON.parse(($('#pdp-variants') || {}).textContent || '[]'); } catch (e) { variants = []; }
    const find = (colorId, sizeId) => variants.find((v) => v.colorId === colorId && v.sizeId === sizeId);

    const colorInputs = $$('input[name="colorId"]', form);
    const sizeInputs = $$('input[name="sizeId"]', form);
    const variantInput = $('input[name="variantId"]', form);
    const qtyInput = $('.qty input', form);
    const hint = $('[data-stock-hint]', form);
    const addButtons = $$('[data-add-label]').map((l) => l.closest('button')).filter(Boolean);
    const labels = $$('[data-add-label]');
    const SIZE_MESSAGE = 'Vui lòng chọn size.';
    const soldOutProduct = form.hasAttribute('data-sold-out'); // sản phẩm hết hàng ở mọi biến thể (JSP đã khóa sẵn)

    // Đánh dấu màu đã hết hàng ở mọi size
    colorInputs.forEach((c) => {
      const id = Number(c.value);
      const any = variants.some((v) => v.colorId === id && v.stock > 0);
      const label = c.closest('.swatch');
      if (label) label.classList.toggle('is-soldout', !any);
    });

    function setHint(text, low) {
      if (!hint) return;
      hint.textContent = text;
      hint.classList.toggle('is-low', !!low);
    }

    function refresh() {
      if (soldOutProduct) { setHint('Sản phẩm tạm hết hàng.', true); return; }
      const colorEl = colorInputs.find((i) => i.checked);
      const colorId = colorEl ? Number(colorEl.value) : null;
      let anyAvailable = false;

      sizeInputs.forEach((inp) => {
        const v = colorId === null ? null : find(colorId, Number(inp.value));
        const ok = !!v && v.stock > 0;
        inp.disabled = !ok;
        if (!ok && inp.checked) inp.checked = false;
        if (ok) anyAvailable = true;
      });

      const sizeEl = sizeInputs.find((i) => i.checked);
      const sizeLabel = $('[data-selected-size]', form);
      if (sizeLabel) sizeLabel.textContent = sizeEl ? sizeEl.dataset.name : '';
      const v = sizeEl && colorId !== null ? find(colorId, Number(sizeEl.value)) : null;
      if (variantInput) variantInput.value = v ? String(v.id) : '';

      // Thông báo chọn size (kiểm tra form của trình duyệt)
      sizeInputs.forEach((inp) => inp.setCustomValidity(sizeEl ? '' : SIZE_MESSAGE));

      if (!anyAvailable) setHint('Màu này tạm hết hàng.', true);
      else if (!v) setHint('Chọn size để xem số lượng còn lại.', false);
      else if (v.stock <= 5) setHint('Chỉ còn ' + v.stock + ' sản phẩm.', true);
      else setHint('Còn hàng.', false);

      if (qtyInput) {
        const max = v ? Math.min(v.stock, 10) : 10;
        qtyInput.max = String(max);
        if (Number(qtyInput.value) > max) qtyInput.value = String(max);
      }

      addButtons.forEach((b) => { b.disabled = !anyAvailable; });
      labels.forEach((l) => { l.textContent = anyAvailable ? 'Thêm vào giỏ' : 'Hết hàng'; });
    }

    colorInputs.forEach((i) => i.addEventListener('change', refresh));
    sizeInputs.forEach((i) => i.addEventListener('change', refresh));

    // Đến từ thẻ sản phẩm (bấm nút size, như a-dam): ?size=M thì chọn sẵn size đó.
    // Màu đang chọn hết size này thì chuyển sang màu đầu tiên còn size đó.
    const wantedSize = new URLSearchParams(window.location.search).get('size');
    const sizeEl = wantedSize && sizeInputs.find((i) => i.dataset.name === wantedSize);
    if (sizeEl && !soldOutProduct) {
      const sizeId = Number(sizeEl.value);
      const checkedColor = colorInputs.find((i) => i.checked);
      const has = (c) => { const v = find(Number(c.value), sizeId); return !!v && v.stock > 0; };
      if (!checkedColor || !has(checkedColor)) {
        const other = colorInputs.find(has);
        if (other) {
          other.checked = true;
          other.dispatchEvent(new Event('change', { bubbles: true })); // cập nhật nhãn màu
        }
      }
      refresh();
      if (!sizeEl.disabled) sizeEl.checked = true;
    }
    refresh();

    // Phản hồi sau khi thêm vào giỏ (chế độ giao diện: app.js chặn gửi form và hiện toast)
    form.addEventListener('submit', () => {
      if (!(window.Saltair && window.Saltair.demo)) return;
      addButtons.forEach((b) => b.classList.add('is-added'));
      labels.forEach((l) => { l.textContent = 'Đã thêm vào giỏ'; });
      setTimeout(() => {
        addButtons.forEach((b) => b.classList.remove('is-added'));
        refresh();
      }, 1600);
    });
  })();

  /* ---------- 4. Phóng ảnh khi rê chuột (chỉ thiết bị có chuột) ---------- */
  $$('[data-zoom]').forEach((box) => {
    const img = $('img', box);
    if (!img || !canHover) return;
    box.classList.add('is-zoomable');
    box.addEventListener('mouseenter', () => box.classList.add('is-zoom'));
    box.addEventListener('mousemove', (e) => {
      const r = box.getBoundingClientRect();
      img.style.transformOrigin = ((e.clientX - r.left) / r.width * 100) + '% ' + ((e.clientY - r.top) / r.height * 100) + '%';
    });
    box.addEventListener('mouseleave', () => {
      box.classList.remove('is-zoom');
      img.style.transformOrigin = '';
    });
  });

  /* ---------- 5. Thanh thêm vào giỏ cố định trên điện thoại ---------- */
  (() => {
    const bar = $('[data-pdp-sticky]');
    const main = $('[data-add-main]');
    if (!bar || !main || !('IntersectionObserver' in window)) return;
    const io = new IntersectionObserver((entries) => {
      const en = entries[0];
      // Chỉ hiện khi nút chính đã cuộn lên khỏi màn hình (không hiện khi nó còn ở phía dưới)
      bar.classList.toggle('is-visible', !en.isIntersecting && en.boundingClientRect.top < 0);
    });
    io.observe(main);
  })();

  /* ---------- 6. Thanh tiến độ đọc bài viết ---------- */
  (() => {
    const bar = $('[data-read-progress]');
    const body = $('.article__body');
    if (!bar || !body) return;
    let ticking = false;
    const update = () => {
      const rect = body.getBoundingClientRect();
      const total = rect.height - window.innerHeight * 0.5;
      const done = Math.min(1, Math.max(0, (window.innerHeight * 0.5 - rect.top) / Math.max(total, 1)));
      bar.style.transform = 'scaleX(' + done.toFixed(3) + ')';
      ticking = false;
    };
    const onScroll = () => { if (!ticking) { ticking = true; requestAnimationFrame(update); } };
    window.addEventListener('scroll', onScroll, { passive: true });
    window.addEventListener('resize', onScroll);
    update();
  })();
})();
