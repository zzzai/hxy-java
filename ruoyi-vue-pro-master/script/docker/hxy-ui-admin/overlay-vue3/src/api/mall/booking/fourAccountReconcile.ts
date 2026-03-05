import request from '@/config/axios'

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
  createTime?: string
}

export interface FourAccountReconcileRunReq {
  bizDate?: string
  source?: string
}

export const getFourAccountReconcilePage = (params: FourAccountReconcilePageReq) => {
  return request.get({ url: '/booking/four-account-reconcile/page', params })
}

export const runFourAccountReconcile = (data: FourAccountReconcileRunReq) => {
  return request.post<number>({ url: '/booking/four-account-reconcile/run', data })
}
