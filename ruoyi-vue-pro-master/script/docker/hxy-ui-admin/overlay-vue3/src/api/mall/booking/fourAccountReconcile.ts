import request from '@/config/axios'

export type FourAccountRelatedTicketStatus = 10 | 20 | number
export type FourAccountRelatedTicketSeverity = 'P0' | 'P1' | 'P2' | string

export interface FourAccountIssueDetail {
  tradeAmount?: number
  fulfillmentAmount?: number
  commissionAmount?: number
  splitAmount?: number
  tradeMinusFulfillment?: number
  tradeMinusCommissionSplit?: number
  issues?: Array<{
    code?: string
    message?: string
    issueCode?: string
    msg?: string
    reason?: string
    detail?: string
  }>
}

export interface FourAccountReconcilePageReq extends PageParam {
  reconcileNo?: string
  bizDate?: string[]
  status?: number
  source?: string
  issueCode?: string
}

export interface FourAccountReconcileVO {
  id: number
  reconcileNo?: string
  bizDate?: string
  sourceBizNo?: string
  tradeAmount?: number
  fulfillmentAmount?: number
  commissionAmount?: number
  splitAmount?: number
  tradeMinusFulfillment?: number
  tradeMinusCommissionSplit?: number
  status?: number
  issueCount?: number
  issueCodes?: string
  issueDetailJson?: string
  source?: string
  operator?: string
  reconciledAt?: string
  relatedTicketId?: number
  relatedTicketStatus?: FourAccountRelatedTicketStatus
  relatedTicketSeverity?: FourAccountRelatedTicketSeverity
  createTime?: string
}

export interface FourAccountReconcileRunReq {
  bizDate?: string
  source?: string
}

export interface FourAccountReconcileSummaryReq {
  bizDate?: string[]
  status?: number
  relatedTicketLinked?: boolean
}

export interface FourAccountReconcileSummaryVO {
  totalCount?: number
  passCount?: number
  warnCount?: number
  tradeMinusFulfillmentSum?: number
  tradeMinusCommissionSplitSum?: number
  unresolvedTicketCount?: number
  ticketSummaryDegraded?: boolean
}

export interface FourAccountRefundAuditSummaryCountItem {
  key?: string
  count?: number
}

export interface FourAccountRefundAuditSummaryReq {
  bizDate?: string[]
  status?: number
  relatedTicketLinked?: boolean
  beginBizDate?: string
  endBizDate?: string
  mismatchType?: FourAccountRefundCommissionMismatchType
  refundAuditStatus?: FourAccountRefundAuditStatus
  refundExceptionType?: FourAccountRefundExceptionType
  refundLimitSource?: string
  payRefundId?: number
  refundTimeRange?: string[]
  keyword?: string
  orderId?: number
}

export interface FourAccountRefundAuditSummaryVO {
  totalCount?: number
  differenceAmountSum?: number
  unresolvedTicketCount?: number
  ticketSummaryDegraded?: boolean
  refundPriceSum?: number
  settledCommissionAmountSum?: number
  reversalCommissionAmountAbsSum?: number
  activeCommissionAmountSum?: number
  expectedReversalAmountSum?: number
  statusAgg?: FourAccountRefundAuditSummaryCountItem[]
  exceptionTypeAgg?: FourAccountRefundAuditSummaryCountItem[]
}

export type FourAccountRefundCommissionMismatchType =
  | 'REFUND_WITHOUT_REVERSAL'
  | 'REVERSAL_WITHOUT_REFUND'
  | 'REVERSAL_AMOUNT_MISMATCH'
  | string

export type FourAccountRefundAuditStatus = 'PENDING' | 'PASS' | 'WARN' | 'CLOSED' | string

export type FourAccountRefundExceptionType = FourAccountRefundCommissionMismatchType | string

export interface FourAccountRefundCommissionAuditPageReq extends PageParam {
  beginBizDate?: string
  endBizDate?: string
  mismatchType?: FourAccountRefundCommissionMismatchType
  refundAuditStatus?: FourAccountRefundAuditStatus
  refundExceptionType?: FourAccountRefundExceptionType
  refundLimitSource?: string
  payRefundId?: number
  refundTimeRange?: string[]
  keyword?: string
  orderId?: number
}

export interface FourAccountRefundCommissionAuditVO {
  orderId?: number
  tradeOrderNo?: string
  userId?: number
  payTime?: string
  refundPrice?: number
  settledCommissionAmount?: number
  reversalCommissionAmountAbs?: number
  activeCommissionAmount?: number
  expectedReversalAmount?: number
  mismatchType?: FourAccountRefundCommissionMismatchType
  refundAuditStatus?: FourAccountRefundAuditStatus
  refundExceptionType?: FourAccountRefundExceptionType
  refundLimitSource?: string
  payRefundId?: number
  refundTime?: string
  refundEvidenceJson?: string
  refundAuditRemark?: string
  mismatchReason?: string
}

export interface FourAccountRefundCommissionAuditSyncReq {
  beginBizDate?: string
  endBizDate?: string
  mismatchType?: FourAccountRefundCommissionMismatchType
  refundAuditStatus?: FourAccountRefundAuditStatus
  refundExceptionType?: FourAccountRefundExceptionType
  refundLimitSource?: string
  payRefundId?: number
  refundTimeRange?: string[]
  keyword?: string
  orderId?: number
  limit?: number
}

export interface FourAccountRefundCommissionAuditSyncResp {
  totalMismatchCount?: number
  attemptedCount?: number
  successCount?: number
  failedCount?: number
  failedOrderIds?: number[]
}

export const getFourAccountReconcilePage = (params: FourAccountReconcilePageReq) => {
  return request.get({ url: '/booking/four-account-reconcile/page', params })
}

export const getFourAccountReconcile = (id: number) => {
  return request.get<FourAccountReconcileVO>({ url: '/booking/four-account-reconcile/get', params: { id } })
}

export const getFourAccountReconcileSummary = (params: FourAccountReconcileSummaryReq) => {
  return request.get<FourAccountReconcileSummaryVO>({ url: '/booking/four-account-reconcile/summary', params })
}

export const getFourAccountRefundCommissionAuditPage = (params: FourAccountRefundCommissionAuditPageReq) => {
  return request.get<PageResult<FourAccountRefundCommissionAuditVO>>({
    url: '/booking/four-account-reconcile/refund-commission-audit-page',
    params
  })
}

export const getFourAccountRefundAuditSummary = (params: FourAccountRefundAuditSummaryReq) => {
  return request.get<FourAccountRefundAuditSummaryVO>({
    url: '/booking/four-account-reconcile/refund-audit-summary',
    params
  })
}

export const syncFourAccountRefundCommissionAuditTickets = (data: FourAccountRefundCommissionAuditSyncReq) => {
  return request.post<FourAccountRefundCommissionAuditSyncResp>({
    url: '/booking/four-account-reconcile/refund-commission-audit/sync-tickets',
    data
  })
}

export const runFourAccountReconcile = (data: FourAccountReconcileRunReq) => {
  return request.post<number>({ url: '/booking/four-account-reconcile/run', data })
}
