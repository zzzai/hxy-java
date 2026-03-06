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
  source?: string
  issueCode?: string
}

export interface FourAccountReconcileSummaryVO {
  totalCount?: number
  passCount?: number
  warnCount?: number
  diffAmount?: number
  openTicketCount?: number
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

export const runFourAccountReconcile = (data: FourAccountReconcileRunReq) => {
  return request.post<number>({ url: '/booking/four-account-reconcile/run', data })
}
