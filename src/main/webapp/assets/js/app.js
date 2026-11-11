/* ==========================================================================
   Saltair - JavaScript dùng chung (thuần, không thư viện)
   Mỗi khối là một tính năng độc lập, kích hoạt bằng thuộc tính data-* trong JSP.
   Phần đánh dấu [EXTRA] là hiệu ứng thêm ngoài trang gốc.
   ========================================================================== */
(() => {
  'use strict';

  const $ = (sel, root = document) => root.querySelector(sel);
  const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));
  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const canHover = window.matchMedia('(hover: hover)').matches;

  document.documentElement.classList.remove('no-js');

  /** Chế độ giao diện: form chưa có logic chỉ hiện thông báo. Bật/tắt ở context-param demoMode trong web.xml. */
  const DEMO = ($('meta[name="demo-mode"]') || {}).content === 'true';
  const DEMO_MESSAGE = 'Đang ở chế độ giao diện: chức năng này sẽ được gắn logic sau.';

  /* ---------- Toast ---------- */
  function toast(message, ms = 2800) {
    let box = $('.toasts');
    if (!box) {
      box = document.createElement('div');
      box.className = 'toasts';
      box.setAttribute('role', 'status');
      box.setAttribute('aria-live', 'polite');
      document.body.appendChild(box);
    }
    const el = document.createElement('div');
    el.className = 'toast';
    el.textContent = message;
    box.appendChild(el);
    setTimeout(() => {
      el.classList.add('is-leaving');
      setTimeout(() => el.remove(), 260);
    }, ms);
  }

  function formatVnd(n) {
    return new Intl.NumberFormat('vi-VN').format(Math.round(n)) + 'đ';
  }

  window.Saltair = { toast, formatVnd, demo: DEMO };

  /* ---------- Thanh thông báo ---------- */
  $$('[data-announcements]').forEach((root) => {
    const msgs = $$('.announcement-bar__msg', root);
    if (!msgs.length) return;
    let i = 0;
    let timer;
    const show = (n) => {
      i = (n + msgs.length) % msgs.length;
      msgs.forEach((m, idx) => m.classList.toggle('is-active', idx === i));
    };
    const start = () => {
      if (msgs.length > 1 && !reduceMotion) timer = setInterval(() => show(i + 1), 5000);
    };
    const stop = () => clearInterval(timer);
    $('[data-ann-prev]', root)?.addEventListener('click', () => { stop(); show(i - 1); start(); });
    $('[data-ann-next]', root)?.addEventListener('click', () => { stop(); show(i + 1); start(); });
    root.addEventListener('mouseenter', stop);
    root.addEventListener('mouseleave', start);
    show(0);
    start();
  });

  /* ---------- Header (#87, đo trên a-dam): luôn dính trên cùng, không ẩn khi cuộn, cao cố định.
     Header trong suốt trên banner chuyển sang nền trắng khi cuộn quá 200px. ---------- */
  const header = $('.header');
  if (header) {
    const SOLID_AT = 200;
    const onScroll = () => header.classList.toggle('is-scrolled', window.scrollY > SOLID_AT);
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });

    /* Menu điện thoại: khung phủ phần dưới header; nút ba gạch đổi thành dấu X */
    const menuBtn = $('[data-menu-toggle]', header);
    const menu = $('#mobile-nav', header);
    if (menuBtn && menu) {
      const setMenu = (open) => {
        if (open) document.documentElement.style.setProperty('--menu-top', Math.max(0, Math.round(header.getBoundingClientRect().bottom)) + 'px');
        header.classList.toggle('is-menu-open', open);
        menu.setAttribute('aria-hidden', String(!open));
        menuBtn.setAttribute('aria-expanded', String(open));
        menuBtn.setAttribute('aria-label', open ? 'Đóng menu' : 'Mở menu');
        document.body.style.overflow = open ? 'hidden' : '';
      };
      const isOpen = () => header.classList.contains('is-menu-open');
      menuBtn.addEventListener('click', () => setMenu(!isOpen()));
      menu.addEventListener('click', (e) => { if (e.target === menu) setMenu(false); });
      document.addEventListener('keydown', (e) => { if (e.key === 'Escape' && isOpen()) { setMenu(false); menuBtn.focus(); } });
      // Mở tìm kiếm hoặc giỏ hàng từ header thì đóng menu trước; quay ngang hoặc kéo rộng cửa sổ cũng đóng
      $$('[data-search-open], [data-cart-open]', header).forEach((b) => b.addEventListener('click', () => { if (isOpen()) setMenu(false); }, true));
      window.matchMedia('(min-width: 991px)').addEventListener('change', (e) => { if (e.matches && isOpen()) setMenu(false); });
    }
  }

  /* ---------- Nút: bọc chữ để có hiệu ứng chữ cuộn lên khi rê chuột (như a-dam) ----------
     Chỉ bọc nút mà chữ nằm trực tiếp trong nút (có thể kèm .icon); nút có phần tử khác (ví dụ span đổi chữ bằng JS) thì bỏ qua. */
  function wrapRollButtons(root = document) {
    if (!canHover || reduceMotion) return;
    $$('.btn', root).forEach((b) => {
      if (b.querySelector('.btn__roll')) return;
      const others = Array.from(b.children).filter((el) => !el.classList.contains('icon'));
      if (others.length) return;
      Array.from(b.childNodes).forEach((n) => {
        if (n.nodeType !== Node.TEXT_NODE || !n.textContent.trim()) return;
        const text = n.textContent.trim();
        const wrap = document.createElement('span');
        wrap.className = 'btn__roll';
        wrap.dataset.text = text;
        const inner = document.createElement('span');
        inner.textContent = text;
        wrap.appendChild(inner);
        n.replaceWith(wrap);
      });
    });
  }
  wrapRollButtons();

  /* ---------- Menu thả xuống (cảm ứng: bấm để mở) ---------- */
  $$('.has-dropdown').forEach((item) => {
    const trigger = $('[data-dropdown-trigger]', item);
    if (!trigger) return;
    trigger.addEventListener('click', (e) => {
      if (canHover) return; // máy tính: mở bằng hover
      e.preventDefault();
      $$('.has-dropdown.is-open').forEach((o) => o !== item && o.classList.remove('is-open'));
      item.classList.toggle('is-open');
    });
  });
  document.addEventListener('click', (e) => {
    if (!e.target.closest('.has-dropdown')) $$('.has-dropdown.is-open').forEach((o) => o.classList.remove('is-open'));
  });

  /* ---------- Lớp phủ, khung tìm kiếm trượt từ phải, ngăn kéo ---------- */
  const overlay = $('#overlay');
  const searchDrawer = $('#search-drawer');
  const searchOpenBtns = $$('[data-search-open]');
  const cartDrawer = $('#cart-drawer');
  const cartOpenBtns = $$('[data-cart-open]');
  // Hai khung trượt từ phải dùng chung hiệu ứng, lớp phủ và focus trap
  const slideDrawers = [searchDrawer, cartDrawer].filter(Boolean);
  const drawerBtns = (d) => (d === searchDrawer ? searchOpenBtns : cartOpenBtns);
  const openSlideDrawer = () => slideDrawers.find((d) => d.classList.contains('is-open'));
  const lockScroll = (lock) => { document.body.style.overflow = lock ? 'hidden' : ''; };
  let lastTrigger = null;
  let willChangeTimer = null;

  function markDrawerAnimating(el) {
    if (!el) return;
    el.classList.add('is-animating');
    clearTimeout(willChangeTimer);
    const cleanup = () => {
      el.classList.remove('is-animating');
      el.removeEventListener('transitionend', cleanup);
    };
    el.addEventListener('transitionend', cleanup);
    willChangeTimer = setTimeout(cleanup, 600);
  }

  function closeAllPanels() {
    const slide = openSlideDrawer();
    const anyOpen = slide || $$('.drawer.is-open').length > 0;
    if (!anyOpen) return;

    if (slide) {
      markDrawerAnimating(slide);
      slide.classList.remove('is-open');
      slide.setAttribute('aria-hidden', 'true');
      drawerBtns(slide).forEach((b) => b.setAttribute('aria-expanded', 'false'));
    }
    $$('.drawer.is-open').forEach((p) => p.classList.remove('is-open'));
    overlay?.classList.remove('is-open');
    lockScroll(false);

    if (slide) {
      const target = lastTrigger || drawerBtns(slide)[0];
      target?.focus();
      lastTrigger = null;
    }
  }

  function openPanel(panel, focusSelector, triggerEl) {
    if (!panel) return;
    closeAccount(false);
    closeAllPanels();
    lastTrigger = triggerEl || null;
    if (slideDrawers.includes(panel)) {
      markDrawerAnimating(panel);
      panel.setAttribute('aria-hidden', 'false');
      drawerBtns(panel).forEach((b) => b.setAttribute('aria-expanded', 'true'));
    }
    panel.classList.add('is-open');
    overlay?.classList.add('is-open');
    lockScroll(true);
    if (focusSelector) {
      setTimeout(() => $(focusSelector, panel)?.focus(), reduceMotion ? 20 : 120);
    }
  }

  searchOpenBtns.forEach((b) => b.addEventListener('click', () => openPanel(searchDrawer, '[data-search-input]', b)));
  $$('[data-drawer-open]').forEach((b) => b.addEventListener('click', () => openPanel(document.getElementById(b.dataset.drawerOpen), null, b)));
  $$('[data-panel-close]').forEach((b) => b.addEventListener('click', closeAllPanels));
  overlay?.addEventListener('click', closeAllPanels);

  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
      if (accountPop?.classList.contains('is-open')) { closeAccount(true); return; }
      closeAllPanels();
      return;
    }
    // Focus trap trong khung tìm kiếm hoặc khung giỏ hàng khi đang mở
    const trap = openSlideDrawer();
    if (e.key === 'Tab' && trap) {
      const focusables = $$(
        'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])',
        trap
      ).filter((el) => !el.closest('[hidden]') && el.offsetParent !== null);
      if (!focusables.length) return;
      const first = focusables[0];
      const last = focusables[focusables.length - 1];
      if (e.shiftKey && (document.activeElement === first || !trap.contains(document.activeElement))) {
        e.preventDefault();
        last.focus();
      } else if (!e.shiftKey && (document.activeElement === last || !trap.contains(document.activeElement))) {
        e.preventDefault();
        first.focus();
      }
    }
  });

  /* ---------- Khung giỏ hàng trượt từ phải (#84) ---------- */
  if (cartDrawer && !DEMO) {
    const cartUrl = cartDrawer.dataset.cartUrl;
    const slot = $('[data-cart-content]', cartDrawer);
    const countEls = $$('[data-cart-count]', cartDrawer);

    const setCount = (n) => {
      countEls.forEach((el) => { el.textContent = String(n); });
      $$('.cart-badge').forEach((b) => {
        const changed = b.textContent !== String(n);
        b.textContent = String(n);
        b.hidden = n <= 0;
        if (changed && n > 0) { b.classList.remove('is-bump'); void b.offsetWidth; b.classList.add('is-bump'); }
      });
    };

    /** Nạp lại ruột khung giỏ từ server. keepErrors: Map itemId -> thông báo lỗi cần hiện lại sau khi nạp. */
    const refresh = async (keepErrors) => {
      slot.classList.add('is-loading');
      try {
        const res = await fetch(cartUrl + '?fragment=drawer', { headers: { 'X-Requested-With': 'fetch' }, credentials: 'same-origin' });
        if (!res.ok) throw new Error(String(res.status));
        slot.innerHTML = await res.text();
        const root = $('.cart-drawer__content', slot);
        setCount(Number(root?.dataset.count) || 0);
        keepErrors?.forEach((msg, id) => {
          const box = $('[data-cart-line][data-item-id="' + id + '"] [data-cart-error]', slot);
          if (box) { box.textContent = msg; box.hidden = false; }
        });
      } catch (err) {
        slot.innerHTML = '<div class="cart-drawer__body cart-drawer__body--empty"><p class="cart-drawer__empty-title">Không tải được giỏ hàng</p><a class="btn btn--primary" href="' + cartUrl + '">Mở trang giỏ hàng</a></div>';
      } finally {
        slot.classList.remove('is-loading');
      }
    };

    /** Gửi một thao tác giỏ hàng, trả {ok, message, count}. */
    const post = async (params) => {
      const res = await fetch(cartUrl, {
        method: 'POST',
        headers: { 'Accept': 'application/json', 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
        credentials: 'same-origin',
        body: new URLSearchParams(params).toString(),
      });
      if (!res.ok) throw new Error(String(res.status));
      return res.json();
    };

    const closePops = () => {
      $$('[data-cart-pop]', slot).forEach((p) => { p.classList.remove('is-open'); p.setAttribute('aria-hidden', 'true'); });
      $('.cart-pop-overlay', slot)?.classList.remove('is-open');
      $$('[data-cart-pop-open]', slot).forEach((b) => b.setAttribute('aria-expanded', 'false'));
    };

    const openCart = (trigger) => {
      openPanel(cartDrawer, '[data-panel-close]', trigger);
      refresh();
    };
    cartOpenBtns.forEach((b) => b.addEventListener('click', (e) => { e.preventDefault(); openCart(b); }));

    const change = async (line, params) => {
      const id = line.dataset.itemId;
      slot.classList.add('is-loading');
      try {
        const data = await post(Object.assign({ itemId: id }, params));
        await refresh(data.ok ? null : new Map([[id, data.message]]));
      } catch (err) {
        slot.classList.remove('is-loading');
        toast('Không cập nhật được giỏ hàng, vui lòng thử lại.');
      }
    };

    slot.addEventListener('click', (e) => {
      const line = e.target.closest('[data-cart-line]');
      const step = e.target.closest('[data-cart-qty]');
      if (step && line) {
        const input = $('[data-cart-input]', line);
        const next = (Number(input.value) || 1) + Number(step.dataset.cartQty);
        // Giảm về 0 thì xóa dòng (giống trang tham chiếu)
        change(line, next <= 0 ? { action: 'remove' } : { action: 'update', quantity: next });
        return;
      }
      if (e.target.closest('[data-cart-remove]') && line) {
        change(line, { action: 'remove' });
        return;
      }
      const popOpen = e.target.closest('[data-cart-pop-open]');
      if (popOpen) {
        const target = document.getElementById(popOpen.dataset.cartPopOpen);
        const wasOpen = target.classList.contains('is-open');
        closePops();
        if (!wasOpen) {
          target.classList.add('is-open');
          target.setAttribute('aria-hidden', 'false');
          $('.cart-pop-overlay', slot)?.classList.add('is-open');
          popOpen.setAttribute('aria-expanded', 'true');
          setTimeout(() => $('textarea, button', target)?.focus(), reduceMotion ? 20 : 150);
        }
        return;
      }
      if (e.target.closest('[data-cart-pop-close]')) closePops();
    });
    slot.addEventListener('change', (e) => {
      const input = e.target.closest('[data-cart-input]');
      const line = input?.closest('[data-cart-line]');
      if (!line) return;
      const q = Math.floor(Number(input.value));
      change(line, !Number.isFinite(q) || q <= 0 ? { action: 'remove' } : { action: 'update', quantity: q });
    });
    slot.addEventListener('submit', async (e) => {
      const form = e.target.closest('[data-cart-note]');
      if (!form) return;
      e.preventDefault();
      try {
        const data = await post({ action: 'note', note: form.elements.note.value });
        toast(data.message || 'Đã lưu ghi chú.');
        closePops();
      } catch (err) {
        toast('Không lưu được ghi chú, vui lòng thử lại.');
      }
    });

    // Thêm vào giỏ ở bất kỳ trang nào: không chuyển trang, mở khung giỏ hàng
    document.addEventListener('submit', async (e) => {
      const form = e.target;
      if (!(form instanceof HTMLFormElement) || form.method.toLowerCase() !== 'post') return;
      if (!form.elements.action || form.elements.action.value !== 'add' || !/\/cart$/.test(form.getAttribute('action') || '')) return;
      e.preventDefault();
      const btns = $$('button[type="submit"]', form).concat($$('button[form="' + form.id + '"]'));
      const fields = Object.fromEntries(new FormData(form));
      const wasDisabled = btns.map((b) => b.disabled);
      btns.forEach((b) => { b.disabled = true; });
      try {
        const data = await post(fields);
        if (data.ok) {
          setCount(data.count);
          openCart(e.submitter || btns[0]);
        } else {
          toast(data.message || 'Không thêm được vào giỏ hàng.');
        }
      } catch (err) {
        toast('Không thêm được vào giỏ hàng, vui lòng thử lại.');
      } finally {
        btns.forEach((b, i) => { b.disabled = wasDisabled[i]; });
      }
    });
  }

  /* ---------- Khung tài khoản (#75): popover trồi lên, đăng nhập bằng email ---------- */
  const accountPop = $('#account-pop');
  const accountBtns = $$('[data-account-open]');
  const ctxPath = document.body.dataset.ctx || '';
  const loggedIn = document.body.dataset.loggedIn === 'true';
  let accountTrigger = null;

  function closeAccount(returnFocus) {
    if (!accountPop || !accountPop.classList.contains('is-open')) return;
    accountPop.classList.remove('is-open');
    accountPop.setAttribute('aria-hidden', 'true');
    accountBtns.forEach((b) => b.setAttribute('aria-expanded', 'false'));
    if (returnFocus) (accountTrigger || accountBtns[0])?.focus();
  }

  function openAccount(trigger) {
    if (!accountPop) return;
    closeAllPanels();
    accountTrigger = trigger || accountBtns[0] || null;
    accountPop.classList.add('is-open');
    accountPop.setAttribute('aria-hidden', 'false');
    accountBtns.forEach((b) => b.setAttribute('aria-expanded', 'true'));
    const target = loggedIn ? $('.account-menu__row', accountPop) : $('[data-account-email]', accountPop);
    // visibility đổi theo transition nên có lúc phần tử chưa nhận focus ngay: thử lại vài lần
    let tries = 0;
    const focusTarget = () => {
      if (!target || !accountPop.classList.contains('is-open')) return;
      target.focus({ preventScroll: true });
      if (document.activeElement !== target && ++tries < 6) setTimeout(focusTarget, 50);
    };
    setTimeout(focusTarget, reduceMotion ? 20 : 60);
  }

  if (accountPop) {
    accountBtns.forEach((b) => b.addEventListener('click', (e) => {
      e.stopPropagation();
      if (accountPop.classList.contains('is-open')) closeAccount(true); else openAccount(b);
    }));
    $('[data-account-close]', accountPop)?.addEventListener('click', () => closeAccount(true));
    document.addEventListener('click', (e) => {
      if (accountPop.classList.contains('is-open') && !accountPop.contains(e.target)
          && !e.target.closest('[data-account-open]')) closeAccount(false);
    });

    const form = $('[data-account-login]', accountPop);
    const emailInput = $('[data-account-email]', accountPop);
    const msg = $('[data-account-msg]', accountPop);
    const esc = (t) => String(t).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
    const showMsg = (html, hint) => {
      if (!msg) return;
      msg.innerHTML = html;
      msg.hidden = !html;
      msg.classList.toggle('is-hint', !!hint);
      emailInput?.setAttribute('aria-invalid', html && !hint ? 'true' : 'false');
    };

    // Chưa đăng nhập: bấm một dòng menu thì focus ô email và hiện gợi ý, không chuyển trang
    if (!loggedIn) {
      $$('[data-account-need]', accountPop).forEach((row) => row.addEventListener('click', (e) => {
        e.preventDefault();
        const redirect = $('[data-account-redirect]', accountPop);
        if (redirect) redirect.value = row.getAttribute('href').slice(ctxPath.length);
        showMsg(esc(row.dataset.accountNeed), true);
        emailInput?.focus();
      }));
    }

    form?.addEventListener('submit', async (e) => {
      e.preventDefault();
      if (form.classList.contains('is-busy')) return;
      const email = emailInput.value.trim();
      if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) {
        showMsg(email ? 'Email không đúng định dạng.' : 'Vui lòng nhập email.');
        emailInput.focus();
        return;
      }
      const redirect = $('[data-account-redirect]', form);
      if (redirect && !redirect.value) redirect.value = location.pathname.slice(ctxPath.length) + location.search;
      form.classList.add('is-busy');
      emailInput.readOnly = true;
      showMsg('');
      try {
        const res = await fetch(form.action, {
          method: 'POST',
          headers: { Accept: 'application/json', 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
          body: new URLSearchParams(new FormData(form)),
          credentials: 'same-origin'
        });
        const data = await res.json();
        if (data.ok) { location.href = data.redirect; return; }
        let html = esc(data.error || 'Không gửi được mã, vui lòng thử lại.');
        if (data.adminUrl) html += ' <a href="' + esc(data.adminUrl) + '">Tới trang quản trị</a>';
        showMsg(html);
      } catch (err) {
        showMsg('Không gửi được mã, vui lòng thử lại.');
      }
      form.classList.remove('is-busy');
      emailInput.readOnly = false;
      emailInput.focus();
    });

    // /home?login=1 (link "Đổi email" ở trang nhập mã): mở sẵn khung
    const params = new URLSearchParams(location.search);
    if (params.get('login') === '1' && !loggedIn) {
      if (emailInput && params.get('email')) emailInput.value = params.get('email');
      openAccount();
    }
  }

  /* ---------- Tìm kiếm nhanh khi đang gõ (debounce 250ms + AbortController) ---------- */
  if (searchDrawer) {
    const searchInput = $('[data-search-input]', searchDrawer);
    const resultsBox = $('[data-search-results]', searchDrawer);
    const progressBar = $('[data-search-progress]', searchDrawer);
    const quickUrl = searchDrawer.dataset.quickUrl || '/search/quick';
    let debounceTimer = null;
    let activeController = null;

    const animateProgress = () => {
      if (!progressBar || typeof progressBar.animate !== 'function') return;
      progressBar.getAnimations().forEach((a) => a.cancel());
      if (reduceMotion) {
        progressBar.animate([{ opacity: 1 }, { opacity: 0 }], { duration: 150, easing: 'ease-in-out' });
      } else {
        progressBar.animate(
          [
            { transform: 'scaleX(0)', opacity: 1 },
            { transform: 'scaleX(1)', opacity: 1, offset: 0.85 },
            { transform: 'scaleX(1)', opacity: 0 }
          ],
          { duration: 250, easing: 'linear', fill: 'forwards' }
        );
      }
    };

    const animateResultsOut = () => {
      if (!resultsBox || !resultsBox.children.length || typeof resultsBox.animate !== 'function') {
        return Promise.resolve();
      }
      resultsBox.getAnimations().forEach((a) => a.cancel());
      if (reduceMotion) {
        return resultsBox.animate([{ opacity: 1 }, { opacity: 0 }], { duration: 150, easing: 'ease-in-out', fill: 'forwards' })
          .finished.catch(() => {});
      }
      return resultsBox.animate(
        [
          { opacity: 1, transform: 'translateY(0)' },
          { opacity: 0, transform: 'translateY(-10px)' }
        ],
        { duration: 250, easing: 'cubic-bezier(0.55, 0.055, 0.675, 0.19)', fill: 'forwards' }
      ).finished.catch(() => {});
    };

    const animateResultsIn = () => {
      if (!resultsBox || typeof resultsBox.animate !== 'function') return;
      resultsBox.getAnimations().forEach((a) => a.cancel());
      if (reduceMotion) {
        resultsBox.animate([{ opacity: 0 }, { opacity: 1 }], { duration: 150, easing: 'ease-in-out', fill: 'forwards' });
      } else {
        resultsBox.animate(
          [
            { opacity: 0, transform: 'translateY(10px)' },
            { opacity: 1, transform: 'translateY(0)' }
          ],
          { duration: 400, easing: 'ease', fill: 'forwards' }
        );
      }
    };

    const fetchQuickResults = async (q) => {
      if (activeController) activeController.abort();
      const controller = new AbortController();
      activeController = controller;

      animateProgress();
      const outPromise = animateResultsOut();

      try {
        const res = await fetch(quickUrl + '?q=' + encodeURIComponent(q), {
          signal: controller.signal,
          headers: { 'X-Requested-With': 'XMLHttpRequest' }
        });
        const html = await res.text();
        await outPromise;
        if (controller.signal.aborted || !resultsBox) return;
        resultsBox.innerHTML = html;
        wrapRollButtons(resultsBox);
        animateResultsIn();
      } catch (err) {
        if (err && err.name === 'AbortError') return;
      }
    };

    searchInput?.addEventListener('input', () => {
      clearTimeout(debounceTimer);
      if (activeController) {
        activeController.abort();
        activeController = null;
      }
      const q = searchInput.value.trim();
      if (q.length < 2) {
        if (resultsBox && resultsBox.innerHTML.trim() !== '') {
          const currentController = new AbortController();
          activeController = currentController;
          animateResultsOut().then(() => {
            if (!currentController.signal.aborted && searchInput.value.trim().length < 2) {
              resultsBox.innerHTML = '';
              resultsBox.getAnimations?.().forEach((a) => a.cancel());
            }
          });
        }
        return;
      }
      debounceTimer = setTimeout(() => fetchQuickResults(q), 250);
    });

    // Chuyển tab "Sản phẩm" | "Danh mục" trong kết quả tìm kiếm nhanh
    searchDrawer.addEventListener('click', (e) => {
      const tab = e.target.closest('[data-search-tab]');
      if (!tab) return;
      const targetPanel = tab.dataset.searchTab;
      $$('[data-search-tab]', searchDrawer).forEach((t) => {
        const active = t === tab;
        t.classList.toggle('is-active', active);
        t.setAttribute('aria-selected', String(active));
      });
      $$('[data-quick-panel]', searchDrawer).forEach((p) => {
        p.hidden = p.dataset.quickPanel !== targetPanel;
      });
    });
  }

  /* ---------- Băng chuyền ---------- */
  $$('[data-carousel]').forEach((root) => {
    const track = $('.carousel__track', root);
    if (!track) return;
    const prev = $('[data-carousel-prev]', root);
    const next = $('[data-carousel-next]', root);
    const bar = $('.carousel__bar', root);
    const update = () => {
      const max = track.scrollWidth - track.clientWidth;
      const ratio = track.scrollWidth ? track.clientWidth / track.scrollWidth : 1;
      if (bar) {
        bar.style.width = Math.min(100, ratio * 100) + '%';
        bar.style.left = (max > 0 ? (track.scrollLeft / max) * (100 - ratio * 100) : 0) + '%';
      }
      if (prev) prev.disabled = track.scrollLeft <= 2;
      if (next) next.disabled = track.scrollLeft >= max - 2;
    };
    const step = () => Math.max(track.clientWidth * 0.8, 240);
    prev?.addEventListener('click', () => track.scrollBy({ left: -step(), behavior: 'smooth' }));
    next?.addEventListener('click', () => track.scrollBy({ left: step(), behavior: 'smooth' }));
    track.addEventListener('scroll', update, { passive: true });
    window.addEventListener('resize', update);
    update();

    // Kéo bằng chuột trên máy tính [EXTRA]
    let down = false, startX = 0, startLeft = 0, moved = false;
    track.addEventListener('pointerdown', (e) => {
      if (e.pointerType !== 'mouse') return;
      down = true; moved = false; startX = e.clientX; startLeft = track.scrollLeft;
    });
    window.addEventListener('pointermove', (e) => {
      if (!down) return;
      const dx = e.clientX - startX;
      if (Math.abs(dx) > 4) { moved = true; track.style.scrollSnapType = 'none'; track.style.scrollBehavior = 'auto'; }
      track.scrollLeft = startLeft - dx;
    });
    window.addEventListener('pointerup', () => {
      if (!down) return;
      down = false;
      track.style.scrollSnapType = ''; track.style.scrollBehavior = '';
    });
    track.addEventListener('click', (e) => { if (moved) { e.preventDefault(); e.stopPropagation(); moved = false; } }, true);
  });

  /* ---------- Nút size trên thẻ sản phẩm: sang trang chi tiết với size đó chọn sẵn ----------
     Thẻ đã là thẻ <a> nên nút size là span có data-size-href; chặn liên kết của thẻ và chuyển trang theo nút. */
  document.addEventListener('click', (e) => {
    const size = e.target.closest('[data-size-href]');
    if (!size) return;
    e.preventDefault();
    e.stopPropagation();
    window.location.href = size.dataset.sizeHref;
  });

  /* ---------- Hero nhiều ảnh ---------- */
  $$('[data-hero-slider]').forEach((root) => {
    const slides = $$('.hero__slide', root);
    const dots = $$('.hero__dot', root);
    if (slides.length < 2) return;
    const DELAY = 6500;
    root.style.setProperty('--hero-delay', DELAY + 'ms');
    let i = 0, timer;
    const show = (n) => {
      i = (n + slides.length) % slides.length;
      slides.forEach((s, k) => s.classList.toggle('is-active', k === i));
      dots.forEach((d, k) => {
        d.classList.toggle('is-active', false);
        if (k === i) { void d.offsetWidth; d.classList.toggle('is-active', true); } // chạy lại thanh thời gian
      });
    };
    const start = () => { if (!reduceMotion) timer = setInterval(() => show(i + 1), DELAY); };
    const stop = () => clearInterval(timer);
    dots.forEach((d, k) => d.addEventListener('click', () => { stop(); show(k); start(); }));
    root.addEventListener('mouseenter', stop);
    root.addEventListener('mouseleave', () => { show(i); start(); });
    show(0);
    start();
  });

  /* ---------- Hiện dần khi cuộn tới [EXTRA] ---------- */
  $$('[data-stagger]').forEach((group) => {
    $$(':scope > .reveal, :scope .reveal', group).forEach((el, idx) => el.style.setProperty('--i', Math.min(idx, 8)));
  });
  const revealEls = $$('.reveal');
  if ('IntersectionObserver' in window && !reduceMotion) {
    const io = new IntersectionObserver((entries) => {
      entries.forEach((en) => {
        if (en.isIntersecting) { en.target.classList.add('is-visible'); io.unobserve(en.target); }
      });
    }, { threshold: 0.12, rootMargin: '0px 0px -40px 0px' });
    revealEls.forEach((el) => io.observe(el));
  } else {
    revealEls.forEach((el) => el.classList.add('is-visible'));
  }

  /* ---------- Đếm số tăng dần cho Dashboard [EXTRA] ---------- */
  $$('[data-countup]').forEach((el) => {
    const target = Number(el.dataset.countup);
    const suffix = el.dataset.suffix || '';
    if (!Number.isFinite(target) || reduceMotion) return;
    const run = () => {
      const t0 = performance.now(), dur = 900;
      const tick = (t) => {
        const p = Math.min(1, (t - t0) / dur);
        const eased = 1 - Math.pow(1 - p, 3);
        el.textContent = new Intl.NumberFormat('vi-VN').format(Math.round(target * eased)) + suffix;
        if (p < 1) requestAnimationFrame(tick);
      };
      requestAnimationFrame(tick);
    };
    if ('IntersectionObserver' in window) {
      const io = new IntersectionObserver((en) => { if (en[0].isIntersecting) { run(); io.disconnect(); } });
      io.observe(el);
    } else run();
  });

  /* ---------- Số lượng ---------- */
  $$('.qty').forEach((q) => {
    const input = $('input', q);
    if (!input) return;
    const clamp = (v) => {
      const min = Number(input.min || 1), max = Number(input.max || 99);
      return Math.min(max, Math.max(min, v));
    };
    const set = (v) => { input.value = clamp(v); input.dispatchEvent(new Event('change', { bubbles: true })); };
    $('[data-qty-minus]', q)?.addEventListener('click', () => set(Number(input.value) - 1));
    $('[data-qty-plus]', q)?.addEventListener('click', () => set(Number(input.value) + 1));
    input.addEventListener('blur', () => { input.value = clamp(Number(input.value) || 1); });
  });

  /* ---------- Hộp thoại ---------- */
  $$('[data-dialog-open]').forEach((b) => b.addEventListener('click', () => {
    const d = document.getElementById(b.dataset.dialogOpen);
    if (d && typeof d.showModal === 'function') {
      // Cho phép truyền dữ liệu vào form trong dialog: data-fill='{"name":"x"}'
      if (b.dataset.fill) {
        try {
          const data = JSON.parse(b.dataset.fill);
          Object.entries(data).forEach(([k, v]) => {
            const f = d.querySelector('[name="' + k + '"]');
            if (!f) return;
            if (f.type === 'checkbox') f.checked = !!v; else f.value = v;
          });
        } catch (err) { /* bỏ qua */ }
      }
      const title = b.dataset.dialogTitle;
      if (title) { const t = $('[data-dialog-title]', d); if (t) t.textContent = title; }
      d.showModal();
    }
  }));
  $$('dialog').forEach((d) => {
    d.addEventListener('click', (e) => { if (e.target === d) d.close(); });
    $$('[data-dialog-close]', d).forEach((b) => b.addEventListener('click', () => d.close()));
  });

  /* ---------- Yêu thích ---------- */
  $$('[data-wish]').forEach((b) => b.addEventListener('click', (e) => {
    e.preventDefault();
    e.stopPropagation();
    const paint = (on) => {
      b.classList.toggle('is-on', on);
      b.classList.remove('is-pop'); void b.offsetWidth; b.classList.add('is-pop');
      b.setAttribute('aria-pressed', String(on));
    };
    const url = b.dataset.wishUrl;
    if (!url || !b.dataset.productId) { // nút chưa nối server: chỉ đổi giao diện
      const on = !b.classList.contains('is-on');
      paint(on);
      if (DEMO) toast(on ? 'Đã thêm vào yêu thích (chế độ giao diện)' : 'Đã bỏ khỏi yêu thích (chế độ giao diện)');
      return;
    }
    // Gửi lên WishlistServlet (action=toggle). Chưa đăng nhập thì AuthFilter chuyển sang trang đăng nhập.
    fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'X-Requested-With': 'XMLHttpRequest' },
      body: 'action=toggle&productId=' + encodeURIComponent(b.dataset.productId)
    }).then((res) => {
      if (res.redirected || !(res.headers.get('Content-Type') || '').includes('json')) {
        window.location.href = res.redirected ? res.url : url;
        return null;
      }
      return res.json();
    }).then((data) => {
      if (!data) return;
      paint(data.inWishlist);
      toast(data.inWishlist ? 'Đã thêm vào yêu thích' : 'Đã bỏ khỏi yêu thích');
    }).catch(() => toast('Không cập nhật được yêu thích, vui lòng thử lại.'));
  }));

  /* ---------- Chế độ giao diện: chặn form/nút chưa có logic ---------- */
  function bumpCart(n) {
    const badge = $('.cart-badge');
    if (!badge) return;
    const cur = Number(badge.textContent) || 0;
    badge.textContent = String(cur + n);
    badge.hidden = false;
    badge.classList.remove('is-bump'); void badge.offsetWidth; badge.classList.add('is-bump');
  }
  $$('form[data-demo]').forEach((form) => form.addEventListener('submit', (e) => {
    if (!DEMO) return;
    if (!form.checkValidity()) return; // để trình duyệt tự báo lỗi
    e.preventDefault();
    if (form.hasAttribute('data-demo-cart')) bumpCart(1);
    toast(form.dataset.demoMessage || DEMO_MESSAGE);
  }));
  $$('[data-demo]:not(form)').forEach((el) => el.addEventListener('click', (e) => {
    if (!DEMO) return;
    e.preventDefault();
    toast(el.dataset.demoMessage || DEMO_MESSAGE);
  }));

  /* ---------- Xác nhận trước khi thực hiện ---------- */
  $$('[data-confirm]').forEach((el) => {
    const evt = el.tagName === 'FORM' ? 'submit' : 'click';
    el.addEventListener(evt, (e) => { if (!window.confirm(el.dataset.confirm)) { e.preventDefault(); e.stopImmediatePropagation(); } }, true);
  });

  /* ---------- Tab ---------- */
  $$('[data-tabs]').forEach((root) => {
    const tabs = $$('[role="tab"]', root);
    const panels = $$('[role="tabpanel"]', root);
    const select = (idx) => {
      tabs.forEach((t, i) => { t.classList.toggle('is-active', i === idx); t.setAttribute('aria-selected', String(i === idx)); });
      panels.forEach((p, i) => { p.hidden = i !== idx; });
    };
    tabs.forEach((t, i) => t.addEventListener('click', () => select(i)));
    select(Math.max(0, tabs.findIndex((t) => t.classList.contains('is-active'))));
  });

  /* ---------- Tự gửi form khi đổi ô chọn ---------- */
  $$('select[data-autosubmit], input[data-autosubmit]').forEach((el) => el.addEventListener('change', () => el.form && el.form.submit()));

  /* ---------- Sao chép mã ---------- */
  $$('[data-copy]').forEach((b) => b.addEventListener('click', async () => {
    try { await navigator.clipboard.writeText(b.dataset.copy); toast('Đã sao chép: ' + b.dataset.copy); }
    catch (err) { toast(b.dataset.copy); }
  }));

  /* ---------- Chọn size/màu: cập nhật nhãn hiển thị ---------- */
  $$('[data-variant-form]').forEach((form) => {
    const colorLabel = $('[data-selected-color]', form);
    $$('input[name="colorId"]', form).forEach((r) => r.addEventListener('change', () => {
      if (colorLabel) colorLabel.textContent = r.dataset.name || '';
    }));
    const checked = $('input[name="colorId"]:checked', form);
    if (checked && colorLabel) colorLabel.textContent = checked.dataset.name || '';
  });

  /* ---------- Ảnh lỗi/trống: thay bằng khung xám ---------- */
  document.addEventListener('error', (e) => {
    const img = e.target;
    if (!(img instanceof HTMLImageElement) || img.dataset.failed) return;
    img.dataset.failed = '1';
    const ph = document.createElement('div');
    ph.className = 'ph';
    ph.setAttribute('role', 'img');
    ph.setAttribute('aria-label', img.alt || 'Ảnh chưa có');
    ph.innerHTML = '<svg class="icon" aria-hidden="true"><use href="#i-image"></use></svg>';
    img.replaceWith(ph);
  }, true);
})();
