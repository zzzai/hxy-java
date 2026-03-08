(function () {
  const STORAGE_KEY = 'closure_p0_api_base';

  const endpointMap = {
    tradeOrderPage: '/trade/order/page',
    tradeOrderGetDetail: '/trade/order/get-detail',
    tradeAfterSaleCreate: '/trade/after-sale/create',
    tradeAfterSalePage: '/trade/after-sale/page',
    tradeAfterSaleGet: '/trade/after-sale/get',
    payOrderGet: '/pay/order/get',
  };

  const mockMethodMap = {
    tradeOrderPage: 'getOrderPage',
    tradeOrderGetDetail: 'getOrderDetail',
    tradeAfterSaleCreate: 'createAfterSale',
    tradeAfterSalePage: 'getAfterSalePage',
    tradeAfterSaleGet: 'getAfterSale',
    payOrderGet: 'getPayOrder',
  };

  function getApiBase() {
    const saved = window.localStorage.getItem(STORAGE_KEY);
    if (saved) {
      return saved;
    }
    return '';
  }

  function setApiBase(value) {
    const cleaned = String(value || '').trim().replace(/\/$/, '');
    if (cleaned) {
      window.localStorage.setItem(STORAGE_KEY, cleaned);
    } else {
      window.localStorage.removeItem(STORAGE_KEY);
    }
    return cleaned;
  }

  function buildQuery(params) {
    const search = new URLSearchParams();
    Object.keys(params || {}).forEach((key) => {
      const val = params[key];
      if (val === undefined || val === null || val === '') {
        return;
      }
      search.append(key, val);
    });
    return search.toString();
  }

  function isUnsupported(httpStatus, code, msg) {
    const text = String(msg || '');
    const unsupportedByHttp = [404, 405, 410, 501].includes(Number(httpStatus));
    const unsupportedByCode = [404, 405, 501, 40401, 50100].includes(Number(code));
    const unsupportedByMsg = /not\s*found|不支持|未实现|不存在|unsupported|no\s*handler/i.test(text);
    return unsupportedByHttp || unsupportedByCode || unsupportedByMsg;
  }

  function getMockData(endpointKey, payload) {
    const mock = window.ClosureP0Mock || {};
    const method = mockMethodMap[endpointKey];
    if (!method || typeof mock[method] !== 'function') {
      return null;
    }
    return mock[method](payload);
  }

  function normalizeError(error, response) {
    if (!error) {
      return { errorType: 'unknown', msg: '未知错误' };
    }

    if (error.name === 'AbortError') {
      return { errorType: 'network', msg: '请求超时，请检查网络' };
    }

    if (response && response.status >= 500) {
      return { errorType: 'service', msg: `服务降级（HTTP ${response.status}）` };
    }

    if (error instanceof TypeError) {
      return { errorType: 'network', msg: '网络异常，无法连接服务端' };
    }

    return { errorType: 'biz', msg: error.message || '请求失败' };
  }

  async function request(endpointKey, options) {
    const opts = options || {};
    const method = (opts.method || 'GET').toUpperCase();
    const params = opts.params || {};
    const data = opts.data || {};

    const endpoint = endpointMap[endpointKey];
    if (!endpoint) {
      return {
        ok: false,
        code: -1,
        msg: `未定义 endpointKey: ${endpointKey}`,
        endpoint: '',
        errorType: 'biz',
      };
    }

    const query = buildQuery(params);
    const baseUrl = getApiBase();
    const url = `${baseUrl}${endpoint}${query ? `?${query}` : ''}`;

    const fallbackData = getMockData(endpointKey, { params: params, data: data });

    if (!baseUrl) {
      return {
        ok: true,
        code: 0,
        msg: '未配置 API Base，已使用本地原型数据。',
        data: fallbackData,
        endpoint: endpoint,
        degraded: true,
        degradeReason: '未配置 API Base',
      };
    }

    try {
      const response = await fetch(url, {
        method: method,
        headers: {
          'Content-Type': 'application/json',
        },
        body: method === 'GET' ? undefined : JSON.stringify(data),
      });

      let payload = {};
      try {
        payload = await response.json();
      } catch (parseError) {
        payload = {};
      }

      const code = payload.code !== undefined ? payload.code : response.status;
      const msg = payload.msg || `HTTP ${response.status}`;
      const responseData = payload.data;

      if (response.ok && Number(code) === 0) {
        return {
          ok: true,
          code: code,
          msg: msg,
          data: responseData,
          endpoint: endpoint,
          degraded: false,
          httpStatus: response.status,
        };
      }

      if (isUnsupported(response.status, code, msg)) {
        return {
          ok: true,
          code: code,
          msg: msg,
          data: fallbackData,
          endpoint: endpoint,
          degraded: true,
          httpStatus: response.status,
          degradeReason: `旧后端未支持 ${endpoint}，已降级到原型数据`,
        };
      }

      if (response.status >= 500) {
        return {
          ok: false,
          code: code,
          msg: msg,
          endpoint: endpoint,
          data: responseData,
          errorType: 'service',
          httpStatus: response.status,
        };
      }

      return {
        ok: false,
        code: code,
        msg: msg,
        endpoint: endpoint,
        data: responseData,
        errorType: 'biz',
        httpStatus: response.status,
      };
    } catch (error) {
      const normalized = normalizeError(error, null);
      return {
        ok: true,
        code: -2,
        msg: normalized.msg,
        endpoint: endpoint,
        data: fallbackData,
        degraded: true,
        errorType: normalized.errorType,
        degradeReason: `请求 ${endpoint} 失败，已降级到原型数据`,
      };
    }
  }

  window.ClosureP0Api = {
    endpointMap: endpointMap,
    getApiBase: getApiBase,
    setApiBase: setApiBase,
    request: request,
  };
})();
