/* Fullmoon client — source walkthrough.
   Shared progressive enhancement. No build step, no dependencies.
   Everything degrades to the plain readable page if JS is off:
   reveal classes are only ever ADDED here, and CSS gates them on html.js. */
(function () {
  'use strict';
  var doc = document.documentElement;
  if (!doc.classList.contains('js')) return;
  var reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  /* ── reading progress ──────────────────────────── */
  var bar = document.createElement('div');
  bar.className = 'progress';
  document.body.appendChild(bar);
  var ticking = false;
  function paintBar() {
    ticking = false;
    var max = doc.scrollHeight - window.innerHeight;
    bar.style.transform = 'scaleX(' + (max > 0 ? Math.min(window.scrollY / max, 1) : 0) + ')';
  }
  window.addEventListener('scroll', function () {
    if (!ticking) { ticking = true; requestAnimationFrame(paintBar); }
  }, { passive: true });
  paintBar();

  /* ── scroll reveal (staggered) ─────────────────── */
  if ('IntersectionObserver' in window && !reduced) {
    var targets = document.querySelectorAll(
      'main > section, .page-head, .panel, .path-card, pre, .tree-line, .section-head'
    );
    Array.prototype.forEach.call(targets, function (el, i) {
      el.classList.add('reveal');
      // stagger within the same reveal batch, capped
      el.style.transitionDelay = Math.min(i % 8, 5) * 60 + 'ms';
    });
    var io = new IntersectionObserver(function (entries) {
      entries.forEach(function (e) {
        if (!e.isIntersecting) return;
        io.unobserve(e.target);
        var el = e.target;
        el.classList.add('in');
        // reveal 끝나면 원 상태로 복귀 — 인라인 delay와 transition이
        // 이후 hover 등 기본 트랜지션을 하이재크하지 않게
        function cleanup() {
          el.classList.remove('reveal', 'in');
          el.style.transitionDelay = '';
        }
        el.addEventListener('transitionend', cleanup, { once: true });
        setTimeout(cleanup, 900); // transitionend 누락(탭 전환 등) 폴백
      });
    }, { rootMargin: '0px 0px -8% 0px', threshold: 0.06 });
    Array.prototype.forEach.call(targets, function (el) { io.observe(el); });
  }

  /* ── scrollspy for same-page anchors ───────────── */
  var here = location.pathname;
  var hashLinks = Array.prototype.filter.call(
    document.querySelectorAll('.nav-links a[href*="#"]'),
    function (a) {
      var u = document.createElement('a');
      u.href = a.getAttribute('href');
      return u.pathname === here && u.hash.length > 1;
    }
  );
  if (hashLinks.length && 'IntersectionObserver' in window) {
    var spy = new IntersectionObserver(function (entries) {
      entries.forEach(function (e) {
        var link = document.querySelector('.nav-links a[href$="#' + CSS.escape(e.target.id) + '"]');
        if (!link) return;
        if (e.isIntersecting) {
          hashLinks.forEach(function (a) { a.classList.remove('here'); });
          link.classList.add('here');
        }
      });
    }, { rootMargin: '-40% 0px -55% 0px' });
    hashLinks.forEach(function (a) {
      var t = document.getElementById(a.href.split('#')[1]);
      if (t) spy.observe(t);
    });
  }

  /* ── copy buttons on code blocks ───────────────── */
  Array.prototype.forEach.call(document.querySelectorAll('pre'), function (pre) {
    var wrap = document.createElement('div');
    wrap.className = 'pre-wrap';
    pre.parentNode.insertBefore(wrap, pre);
    wrap.appendChild(pre);
    var btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'copy';
    btn.textContent = '복사';
    btn.setAttribute('aria-label', '코드 복사');
    btn.addEventListener('click', function () {
      (navigator.clipboard ? navigator.clipboard.writeText(pre.textContent) :
        Promise.reject()).then(function () {
        btn.textContent = '복사됨';
        btn.classList.add('ok');
        setTimeout(function () { btn.textContent = '복사'; btn.classList.remove('ok'); }, 1400);
      }).catch(function () {
        btn.textContent = '복사 실패';
        setTimeout(function () { btn.textContent = '복사'; }, 1400);
      });
    });
    wrap.appendChild(btn);
  });

  /* ── count-up for stat numbers ─────────────────── */
  if (!reduced && 'IntersectionObserver' in window) {
    var nums = document.querySelectorAll('.facts b');
    var cio = new IntersectionObserver(function (entries) {
      entries.forEach(function (e) {
        if (!e.isIntersecting) return;
        cio.unobserve(e.target);
        var el = e.target;
        var end = parseInt(el.textContent, 10);
        if (isNaN(end) || end === 0) return;
        var t0 = null;
        function step(ts) {
          if (!t0) t0 = ts;
          var p = Math.min((ts - t0) / 900, 1);
          el.textContent = String(Math.round(end * (1 - Math.pow(1 - p, 3))));
          if (p < 1) requestAnimationFrame(step);
        }
        requestAnimationFrame(step);
      });
    }, { threshold: 0.5 });
    Array.prototype.forEach.call(nums, function (el) { cio.observe(el); });
  }

  /* ── cursor spotlight on cards ─────────────────── */
  if (window.matchMedia('(hover: hover) and (pointer: fine)').matches) {
    document.querySelectorAll('.panel, .path-card').forEach(function (card) {
      card.classList.add('spot');
      card.addEventListener('pointermove', function (ev) {
        var r = card.getBoundingClientRect();
        card.style.setProperty('--mx', (ev.clientX - r.left) + 'px');
        card.style.setProperty('--my', (ev.clientY - r.top) + 'px');
      });
    });
  }

  /* ── starfield + moon glow (decorative) ────────── */
  if (!reduced) {
    var c = document.createElement('canvas');
    c.className = 'stars';
    c.setAttribute('aria-hidden', 'true');
    document.body.appendChild(c);
    var ctx = c.getContext('2d');
    var starInk = (getComputedStyle(doc).getPropertyValue('--star') || '').trim() || '#e8ecff';
    var stars = [], dpr = Math.min(window.devicePixelRatio || 1, 2);
    function seed() {
      c.width = window.innerWidth * dpr;
      c.height = window.innerHeight * dpr;
      stars = [];
      var n = Math.floor(window.innerWidth * window.innerHeight / 16000);
      for (var i = 0; i < n; i++) {
        stars.push({
          x: Math.random() * c.width, y: Math.random() * c.height,
          r: (Math.random() * 1.1 + 0.3) * dpr,
          p: Math.random() * Math.PI * 2,
          s: 0.4 + Math.random() * 1.2
        });
      }
    }
    seed();
    window.addEventListener('resize', seed);
    var t0 = performance.now();
    (function draw(t) {
      ctx.clearRect(0, 0, c.width, c.height);
      for (var i = 0; i < stars.length; i++) {
        var st = stars[i];
        var tw = 0.35 + 0.65 * (0.5 + 0.5 * Math.sin(st.p + (t - t0) / 1000 * st.s));
        ctx.globalAlpha = tw * 0.5;
        ctx.fillStyle = starInk;
        ctx.beginPath();
        ctx.arc(st.x, st.y, st.r, 0, 6.283);
        ctx.fill();
      }
      ctx.globalAlpha = 1;
      requestAnimationFrame(draw);
    })(t0);

    // hero moon glow, only on the index page
    var hero = document.querySelector('.hero .wrap');
    if (hero) {
      var glow = document.createElement('div');
      glow.className = 'moonglow';
      glow.setAttribute('aria-hidden', 'true');
      // bleeds past the wrap on purpose; html/body clip it
      glow.setAttribute('data-bleed', '');
      hero.appendChild(glow);
    }
  }
})();
