import request from '@/config/axios'

export type RefundNotifyLogStatus = 'success' | 'fail' | 'pending' | string

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
}

export interface RefundNotifyLogReplayReq {
  id: number
}

export const getRefundNotifyLogPage = (params: RefundNotifyLogPageReq) => {
  return request.get<PageResult<RefundNotifyLogVO>>({ url: '/booking/refund-notify-log/page', params })
}

export const replayRefundNotifyLog = (data: RefundNotifyLogReplayReq) => {
  return request.post<boolean>({ url: '/booking/refund-notify-log/replay', data })
}
