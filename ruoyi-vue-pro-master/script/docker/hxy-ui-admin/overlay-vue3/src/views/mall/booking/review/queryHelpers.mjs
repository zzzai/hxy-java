const DEFAULT_LEDGER_QUERY = Object.freeze({
  pageNo: 1,
  pageSize: 10,
  id: undefined,
  bookingOrderId: undefined,
  storeId: undefined,
  technicianId: undefined,
  memberId: undefined,
  reviewLevel: undefined,
  riskLevel: undefined,
  followStatus: undefined,
  onlyManagerTodo: undefined,
  managerTodoStatus: undefined,
  managerSlaStatus: undefined,
  replyStatus: undefined,
  submitTime: undefined,
});

const DASHBOARD_CARD_QUERY_MAP = Object.freeze({
  negative: { reviewLevel: '3' },
  positive: { reviewLevel: '1' },
  neutral: { reviewLevel: '2' },
  pendingFollow: { followStatus: '1' },
  urgent: { riskLevel: '2' },
  replied: { replyStatus: 'true' },
  managerTodoPending: { onlyManagerTodo: 'true', managerTodoStatus: '1' },
  managerTodoClaimTimeout: { onlyManagerTodo: 'true', managerSlaStatus: 'CLAIM_TIMEOUT' },
  managerTodoFirstActionTimeout: { onlyManagerTodo: 'true', managerSlaStatus: 'FIRST_ACTION_TIMEOUT' },
  managerTodoCloseTimeout: { onlyManagerTodo: 'true', managerSlaStatus: 'CLOSE_TIMEOUT' },
  managerTodoClosed: { onlyManagerTodo: 'true', managerTodoStatus: '4' },
});

const INTEGER_KEYS = [
  'pageNo',
  'pageSize',
  'id',
  'bookingOrderId',
  'storeId',
  'technicianId',
  'memberId',
  'reviewLevel',
  'riskLevel',
  'followStatus',
  'managerTodoStatus',
];

const BOOLEAN_KEYS = ['onlyManagerTodo', 'replyStatus'];
const STRING_KEYS = ['managerSlaStatus'];

const takeFirst = (value) => {
  if (Array.isArray(value)) {
    return value[0];
  }
  return value;
};

const parseInteger = (value) => {
  const singleValue = takeFirst(value);
  if (singleValue === undefined || singleValue === null || singleValue === '') {
    return undefined;
  }
  const parsed = Number(singleValue);
  return Number.isInteger(parsed) ? parsed : undefined;
};

const parseBoolean = (value) => {
  const singleValue = takeFirst(value);
  if (singleValue === 'true') {
    return true;
  }
  if (singleValue === 'false') {
    return false;
  }
  return undefined;
};

const parseString = (value) => {
  const singleValue = takeFirst(value);
  return singleValue ? String(singleValue) : undefined;
};

export const createDefaultLedgerQuery = () => ({
  ...DEFAULT_LEDGER_QUERY,
});

export const buildLedgerQueryFromDashboardCardKey = (cardKey) => ({
  ...(DASHBOARD_CARD_QUERY_MAP[cardKey] || {}),
});

export const parseLedgerQuery = (routeQuery = {}) => {
  const parsed = createDefaultLedgerQuery();

  INTEGER_KEYS.forEach((key) => {
    const value = parseInteger(routeQuery[key]);
    if (value !== undefined) {
      parsed[key] = value;
    }
  });

  BOOLEAN_KEYS.forEach((key) => {
    const value = parseBoolean(routeQuery[key]);
    if (value !== undefined) {
      parsed[key] = value;
    }
  });

  STRING_KEYS.forEach((key) => {
    const value = parseString(routeQuery[key]);
    if (value !== undefined) {
      parsed[key] = value;
    }
  });

  if (Array.isArray(routeQuery.submitTime)) {
    const submitTime = routeQuery.submitTime.filter(Boolean).slice(0, 2);
    parsed.submitTime = submitTime.length ? submitTime : undefined;
  }

  return parsed;
};
