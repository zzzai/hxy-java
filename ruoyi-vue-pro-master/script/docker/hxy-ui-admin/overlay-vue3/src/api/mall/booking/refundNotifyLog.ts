import request from '@/config/axios'

export type RefundNotifyLogStatus = 'success' | 'fail' | 'pending' | string
export type RefundNotifyReplayResultStatus = 'SUCCESS' | 'SKIP' | 'FAIL' | string

export interface RefundNotifyLogPageReq extends PageParam {
  orderId?: number
  merchantRefundId?: string
  payRefundId?: number
  status?: RefundNotifyLogStatus
  errorCode?: string
  createTime?: string[]
}

export interface RefundNotifyLogVO {
  id?: number
  orderId?: number
  merchantRefundId?: string
  payRefundId?: number
  status?: RefundNotifyLogStatus
  errorCode?: string
  errorMsg?: string
  rawPayload?: string
  retryCount?: number
  nextRetryTime?: string
  createTime?: string
  updateTime?: string
  lastReplayOperator?: string
  lastReplayTime?: string
  lastReplayResult?: RefundNotifyReplayResultStatus
  lastReplayRemark?: string
}

export interface RefundNotifyLogReplayReq {
  dryRun?: boolean
  ids?: number[]
  // 兼容旧后端 V1（仅支持单条 id）
  id?: number
}

export interface RefundNotifyLogReplayDetail {
  id?: number
  orderId?: number
  merchantRefundId?: string
  payRefundId?: number
  resultStatus?: RefundNotifyReplayResultStatus
  resultCode?: string | number
  resultMessage?: string
  failReason?: string
  errorCode?: string
  errorMsg?: string
}

export interface RefundNotifyLogReplayResp {
  successCount?: number
  skipCount?: number
  failCount?: number
  details?: RefundNotifyLogReplayDetail[]
}

export const getRefundNotifyLogPage = (params: RefundNotifyLogPageReq) => {
  return request.get<PageResult<RefundNotifyLogVO>>({ url: '/booking/refund-notify-log/page', params })
}

export const replayRefundNotifyLog = (data: RefundNotifyLogReplayReq) => {
  return request.post<RefundNotifyLogReplayResp | boolean>({ url: '/booking/refund-notify-log/replay', data })
}
