const DEFAULT_COMPAT_HINT = '当前后端版本可能未支持该接口，已自动降级，不影响下单与支付主流程。';

export const isSuccessResult = (result) => {
  return !!result && typeof result === 'object' && result.code === 0;
};

export const attachCompat = (result, compat = {}) => {
  const base = result && typeof result === 'object' ? result : { code: 0, data: null, msg: '' };
  return {
    ...base,
    compat: {
      degraded: true,
      hint: base.msg || DEFAULT_COMPAT_HINT,
      ...compat,
    },
  };
};

export const degradedSuccess = ({
  endpoint,
  data,
  msg = DEFAULT_COMPAT_HINT,
  reason = 'LEGACY_ENDPOINT_UNSUPPORTED',
  fallbackEndpoint = '',
}) => {
  return {
    code: 0,
    data,
    msg,
    compat: {
      degraded: true,
      endpoint,
      reason,
      fallbackEndpoint,
      hint: msg,
    },
  };
};

export const degradedError = ({
  endpoint,
  msg = DEFAULT_COMPAT_HINT,
  reason = 'LEGACY_ENDPOINT_UNSUPPORTED',
  code = -501,
}) => {
  return {
    code,
    data: null,
    msg,
    compat: {
      degraded: true,
      endpoint,
      reason,
      hint: msg,
    },
  };
};
