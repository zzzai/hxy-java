export const BOOKING_REVIEW_LEDGER_QUERY_FIELDS = Object.freeze([
  'id',
  'bookingOrderId',
  'storeId',
  'technicianId',
  'memberId',
  'reviewLevel',
  'riskLevel',
  'followStatus',
  'onlyManagerTodo',
  'onlyPendingInit',
  'managerTodoStatus',
  'managerSlaStatus',
  'replyStatus',
  'submitTime',
]);

export const BOOKING_REVIEW_DISPLAY_ONLY_RETURN_FIELDS = Object.freeze([
  'priorityLevel',
  'priorityReason',
  'notifyRiskSummary',
]);

export const BOOKING_REVIEW_DASHBOARD_SUMMARY_FIELDS = Object.freeze([
  'totalCount',
  'positiveCount',
  'neutralCount',
  'negativeCount',
  'pendingFollowCount',
  'urgentCount',
  'repliedCount',
  'managerTodoPendingCount',
  'managerTodoClaimTimeoutCount',
  'managerTodoClaimDueSoonCount',
  'managerTodoFirstActionTimeoutCount',
  'managerTodoFirstActionDueSoonCount',
  'managerTodoCloseTimeoutCount',
  'managerTodoCloseDueSoonCount',
  'managerTodoClosedCount',
]);

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
  onlyPendingInit: undefined,
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
  managerTodoClaimDueSoon: { onlyManagerTodo: 'true', managerSlaStatus: 'CLAIM_DUE_SOON' },
  managerTodoClaimTimeout: { onlyManagerTodo: 'true', managerSlaStatus: 'CLAIM_TIMEOUT' },
  managerTodoFirstActionDueSoon: { onlyManagerTodo: 'true', managerSlaStatus: 'FIRST_ACTION_DUE_SOON' },
  managerTodoFirstActionTimeout: { onlyManagerTodo: 'true', managerSlaStatus: 'FIRST_ACTION_TIMEOUT' },
  managerTodoCloseDueSoon: { onlyManagerTodo: 'true', managerSlaStatus: 'CLOSE_DUE_SOON' },
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

const BOOLEAN_KEYS = ['onlyManagerTodo', 'onlyPendingInit', 'replyStatus'];
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

const parseTime = (value) => {
  if (!value) {
    return NaN;
  }
  return new Date(String(value).replace(/-/g, '/')).getTime();
};

const hasExplicitClosedSignal = (row) => row?.managerTodoStatus === 4 || Boolean(row?.managerClosedAt);

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

export const resolveManagerTodoSlaStage = (row = {}, { nowMs = Date.now() } = {}) => {
  if (!row || typeof row !== 'object') {
    return '';
  }
  if (hasExplicitClosedSignal(row)) {
    return 'CLOSED';
  }
  if (row.managerSlaStage) {
    return row.managerSlaStage;
  }
  if (row.managerTodoStatus === undefined || row.managerTodoStatus === null) {
    return row.reviewLevel === 3 ? 'PENDING_INIT' : '';
  }

  const closeDeadline = parseTime(row.managerCloseDeadlineAt);
  const firstActionDeadline = parseTime(row.managerFirstActionDeadlineAt);
  const claimDeadline = parseTime(row.managerClaimDeadlineAt);

  if (!Number.isNaN(closeDeadline) && nowMs > closeDeadline) {
    return 'CLOSE_TIMEOUT';
  }
  if (!row.managerFirstActionAt && !Number.isNaN(firstActionDeadline) && nowMs > firstActionDeadline) {
    return 'FIRST_ACTION_TIMEOUT';
  }
  if (!row.managerClaimedAt && !Number.isNaN(claimDeadline) && nowMs > claimDeadline) {
    return 'CLAIM_TIMEOUT';
  }
  if (!Number.isNaN(closeDeadline) && closeDeadline >= nowMs && closeDeadline - nowMs <= 120 * 60 * 1000) {
    return 'CLOSE_DUE_SOON';
  }
  if (
    !row.managerFirstActionAt
    && !Number.isNaN(firstActionDeadline)
    && firstActionDeadline >= nowMs
    && firstActionDeadline - nowMs <= 10 * 60 * 1000
  ) {
    return 'FIRST_ACTION_DUE_SOON';
  }
  if (
    !row.managerClaimedAt
    && !Number.isNaN(claimDeadline)
    && claimDeadline >= nowMs
    && claimDeadline - nowMs <= 5 * 60 * 1000
  ) {
    return 'CLAIM_DUE_SOON';
  }
  return 'NORMAL';
};
