import request from '@/config/axios'

export interface CommissionSettlementPageReq extends PageParam {
  settlementNo?: string
  storeId?: number
  technicianId?: number
  status?: number
  reviewerId?: number
  reviewWarned?: boolean
  reviewEscalated?: boolean
  reviewDeadlineTime?: string[]
  overdue?: boolean
}

export interface CommissionSettlement {
  id: number
  settlementNo?: string
  storeId?: number
  technicianId?: number
  status?: number
  commissionCount?: number
  totalCommissionAmount?: number
  reviewSubmitTime?: string
  reviewDeadlineTime?: string
  reviewWarned?: boolean
  reviewEscalated?: boolean
  reviewedTime?: string
  reviewerId?: number
  reviewRemark?: string
  rejectReason?: string
  paidTime?: string
  payerId?: number
  payVoucherNo?: string
  payRemark?: string
  remark?: string
  overdue?: boolean
  createTime?: string
  updateTime?: string
}

export interface CommissionSettlementLog {
  id: number
  settlementId: number
  action?: string
  fromStatus?: number
  toStatus?: number
  operatorId?: number
  operatorType?: string
  operateRemark?: string
  actionTime?: string
}

export interface CommissionSettlementCreateReq {
  commissionIds: number[]
  remark?: string
}

export interface CommissionSettlementSubmitReq {
  id: number
  slaMinutes?: number
  remark?: string
}

export interface CommissionSettlementApproveReq {
  id: number
  remark?: string
}

export interface CommissionSettlementRejectReq {
  id: number
  rejectReason: string
}

export interface CommissionSettlementPayReq {
  id: number
  payVoucherNo: string
  payRemark: string
}

export interface CommissionSettlementNotifyOutboxPageReq extends PageParam {
  settlementId?: number
  status?: number
  notifyType?: string
  channel?: string
  lastActionCode?: string
  lastActionBizNo?: string
}

export interface CommissionSettlementNotifyOutbox {
  id: number
  settlementId: number
  notifyType?: string
  channel?: string
  severity?: string
  status?: number
  retryCount?: number
  nextRetryTime?: string
  sentTime?: string
  lastErrorMsg?: string
  lastActionCode?: string
  lastActionBizNo?: string
  lastActionTime?: string
  createTime?: string
  updateTime?: string
}

export interface CommissionSettlementNotifyOutboxRetryReq {
  ids: number[]
  reason?: string
}

export interface CommissionSettlementNotifyOutboxBatchRetryResp {
  totalCount: number
  retriedCount: number
  skippedNotExistsCount: number
  skippedStatusInvalidCount: number
  retriedIds: number[]
  skippedNotExistsIds: number[]
  skippedStatusInvalidIds: number[]
}

export const getSettlementPage = (params: CommissionSettlementPageReq) => {
  return request.get({ url: '/booking/commission-settlement/page', params })
}

export const getSettlement = (id: number) => {
  return request.get({ url: '/booking/commission-settlement/get', params: { id } })
}

export const createSettlement = (data: CommissionSettlementCreateReq) => {
  return request.post({ url: '/booking/commission-settlement/create', data })
}

export const submitSettlement = (data: CommissionSettlementSubmitReq) => {
  return request.post({ url: '/booking/commission-settlement/submit', data })
}

export const approveSettlement = (data: CommissionSettlementApproveReq) => {
  return request.post({ url: '/booking/commission-settlement/approve', data })
}

export const rejectSettlement = (data: CommissionSettlementRejectReq) => {
  return request.post({ url: '/booking/commission-settlement/reject', data })
}

export const paySettlement = (data: CommissionSettlementPayReq) => {
  return request.post({ url: '/booking/commission-settlement/pay', data })
}

export const getSettlementLogList = (settlementId: number) => {
  return request.get({ url: '/booking/commission-settlement/log-list', params: { settlementId } })
}

export const getNotifyOutboxPage = (params: CommissionSettlementNotifyOutboxPageReq) => {
  return request.get({ url: '/booking/commission-settlement/notify-outbox-page', params })
}

export const retryNotifyOutbox = (data: CommissionSettlementNotifyOutboxRetryReq) => {
  return request.post({ url: '/booking/commission-settlement/notify-outbox-retry', data })
}

export const retryNotifyOutboxBatch = (data: CommissionSettlementNotifyOutboxRetryReq) => {
  return request.post<CommissionSettlementNotifyOutboxBatchRetryResp>({
    url: '/booking/commission-settlement/notify-outbox-batch-retry',
    data
  })
}
