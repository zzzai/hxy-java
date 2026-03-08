(function () {
  function parseQuery() {
    const query = new URLSearchParams(window.location.search);
    const map = {};
    query.forEach(function (value, key) {
      map[key] = value;
    });
    return map;
  }

  function fen2yuan(fen) {
    const value = Number(fen || 0);
    return (value / 100).toFixed(2);
  }

  function textStatus(status) {
    const map = {
      0: '待支付',
      10: '待发货',
      20: '待收货',
      30: '待评价',
      40: '已取消',
      50: '已完成',
    };
    return map[Number(status)] || `状态${status}`;
  }

  function afterSaleStatus(status) {
    const map = {
      10: '申请中',
      20: '商家审核中',
      30: '等待买家退货',
      40: '等待商家收货',
      50: '退款成功',
      61: '申请关闭',
      62: '商家拒绝',
      63: '超时关闭',
    };
    return map[Number(status)] || `状态${status}`;
  }

  function afterSaleDesc(status) {
    const map = {
      10: '退款申请待商家处理',
      20: '平台审核中，请耐心等待',
      30: '请尽快填写退货物流',
      40: '商家正在签收退货',
      50: '退款已原路退回',
      61: '售后单已关闭',
      62: '商家拒绝，建议联系人工客服',
      63: '超时未处理，系统自动关闭',
    };
    return map[Number(status)] || '状态处理中';
  }

  function escapeHtml(text) {
    return String(text || '')
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#39;');
  }

  function renderBanner(element, message, type, actionUrl) {
    if (!element) {
      return;
    }
    if (!message) {
      element.innerHTML = '';
      element.className = 'banner hidden';
      return;
    }
    const level = type || 'warning';
    element.className = `banner ${level}`;
    const actionLink = actionUrl
      ? `<a class=\"banner-link\" href=\"${escapeHtml(actionUrl)}\">查看异常页</a>`
      : '';
    element.innerHTML = `<span>${escapeHtml(message)}</span>${actionLink}`;
  }

  function syncApiBase() {
    const input = document.querySelector('[data-api-base]');
    const save = document.querySelector('[data-api-save]');
    const clear = document.querySelector('[data-api-clear]');
    if (!input || !window.ClosureP0Api) {
      return;
    }

    input.value = window.ClosureP0Api.getApiBase();

    if (save) {
      save.addEventListener('click', function () {
        window.ClosureP0Api.setApiBase(input.value || '');
        input.value = window.ClosureP0Api.getApiBase();
      });
    }

    if (clear) {
      clear.addEventListener('click', function () {
        window.ClosureP0Api.setApiBase('');
        input.value = '';
      });
    }
  }

  function withFallbackHint(result, bannerElement, errorPageType) {
    if (!result) {
      return;
    }
    if (result.degraded) {
      const suffix = result.errorType === 'network' ? '（网络异常）' : '';
      renderBanner(
        bannerElement,
        `${result.degradeReason || '已降级'}${suffix}，主流程继续。`,
        'warning',
        result.errorType ? `error.html?type=${result.errorType || errorPageType || 'service'}` : '',
      );
      return;
    }

    if (!result.ok) {
      renderBanner(
        bannerElement,
        `${result.msg || '请求失败'}，请稍后重试。`,
        'error',
        `error.html?type=${errorPageType || result.errorType || 'service'}&code=${
          result.code || ''
        }&msg=${encodeURIComponent(result.msg || '')}`,
      );
      return;
    }

    renderBanner(bannerElement, '', 'info');
  }

  window.ClosureP0Common = {
    parseQuery: parseQuery,
    fen2yuan: fen2yuan,
    textStatus: textStatus,
    afterSaleStatus: afterSaleStatus,
    afterSaleDesc: afterSaleDesc,
    escapeHtml: escapeHtml,
    renderBanner: renderBanner,
    syncApiBase: syncApiBase,
    withFallbackHint: withFallbackHint,
  };
})();
