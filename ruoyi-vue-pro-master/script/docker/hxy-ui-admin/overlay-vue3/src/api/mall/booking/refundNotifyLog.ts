import request from '@/config/axios'

export type RefundNotifyLogStatus = 'success' | 'fail' | 'pending' | string
export type RefundNotifyReplayResultStatus = 'SUCCESS' | 'SKIP' | 'FAIL' | string
export type RefundNotifyReplayRunStatus = 'RUNNING' | 'SUCCESS' | 'PARTIAL_FAIL' | 'FAIL' | string

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
  runId?: number | string
  triggerSource?: string
  operator?: string
  dryRun?: boolean
  limitSize?: number
  scannedCount?: number
  successCount?: number
  skipCount?: number
  failCount?: number
  status?: RefundNotifyReplayRunStatus
  errorMsg?: string
  startTime?: string
  endTime?: string
  details?: RefundNotifyLogReplayDetail[]
}

export interface RefundNotifyReplayDueReq {
  limit?: number
  dryRun?: boolean
}

export interface RefundNotifyReplayRunLogPageReq extends PageParam {
  runId?: string | number
  triggerSource?: string
  status?: RefundNotifyReplayRunStatus
  operator?: string
  hasWarning?: boolean
  minFailCount?: number
  timeRange?: string[]
}

export interface RefundNotifyReplayRunLogVO {
  id?: number
  runId?: number | string
  triggerSource?: string
  operator?: string
  dryRun?: boolean
  limitSize?: number
  scannedCount?: number
  successCount?: number
  skipCount?: number
  failCount?: number
  status?: RefundNotifyReplayRunStatus
  errorMsg?: string
  startTime?: string
  endTime?: string
  createTime?: string
}

export interface RefundNotifyReplayRunLogDetailPageReq extends PageParam {
  runId?: string | number
  notifyLogId?: number
  resultStatus?: string
  ticketSyncStatus?: string
  warningTag?: string
}

export interface RefundNotifyReplayRunLogDetailVO {
  id?: number
  runId?: string | number
  notifyLogId?: number | string
  orderId?: number
  merchantRefundId?: string
  payRefundId?: number
  resultStatus?: string
  resultCode?: string | number
  warningTag?: string
  ticketSyncStatus?: string
  ticketId?: string | number
  ticketSyncTime?: string
  errorMsg?: string
  createTime?: string
  updateTime?: string
}

export interface RefundNotifyReplayRunLogSummaryItem {
  key?: string
  code?: string
  tag?: string
  name?: string
  value?: string | number
  count?: number
}

export interface RefundNotifyReplayRunLogSummaryVO {
  runId?: string | number
  runStatus?: RefundNotifyReplayRunStatus
  status?: RefundNotifyReplayRunStatus
  triggerSource?: string
  operator?: string
  dryRun?: boolean
  start?: string
  startTime?: string
  end?: string
  endTime?: string
  scanned?: number
  scannedCount?: number
  success?: number
  successCount?: number
  skip?: number
  skipCount?: number
  fail?: number
  failCount?: number
  warning?: number
  warningCount?: number
  topFailCodes?: Array<string | number | RefundNotifyReplayRunLogSummaryItem>
  topWarningTags?: Array<string | number | RefundNotifyReplayRunLogSummaryItem>
  errorMsg?: string
}

export interface RefundNotifyReplayRunLogSyncTicketsReq {
  runId?: string | number
  dryRun?: boolean
  forceResync?: boolean
  onlyFail?: boolean
}

export interface RefundNotifyReplayRunLogSyncTicketsDetailVO {
  id?: number
  notifyLogId?: number | string
  resultStatus?: string
  resultCode?: string | number
  warningTag?: string
  ticketSyncStatus?: string
  ticketId?: string | number
  ticketSyncTime?: string
  errorMsg?: string
}

export interface RefundNotifyReplayRunLogSyncTicketsResp {
  runId?: string | number
  attempted?: number
  attemptedCount?: number
  success?: number
  successCount?: number
  skip?: number
  skipCount?: number
  fail?: number
  failCount?: number
  failed?: number
  failedCount?: number
  failedIds?: Array<string | number>
  details?: RefundNotifyReplayRunLogSyncTicketsDetailVO[]
  errorMsg?: string
}

export const getRefundNotifyLogPage = (params: RefundNotifyLogPageReq) => {
  return request.get<PageResult<RefundNotifyLogVO>>({ url: '/booking/refund-notify-log/page', params })
}

export const replayRefundNotifyLog = (data: RefundNotifyLogReplayReq) => {
  return request.post<RefundNotifyLogReplayResp | boolean>({ url: '/booking/refund-notify-log/replay', data })
}

export const replayDue = (data: RefundNotifyReplayDueReq) => {
  return request.post<RefundNotifyLogReplayResp>({ url: '/booking/refund-notify-log/replay-due', data })
}

export const getReplayRunLogPage = (params: RefundNotifyReplayRunLogPageReq) => {
  return request.get<PageResult<RefundNotifyReplayRunLogVO>>({
    url: '/booking/refund-notify-log/replay-run-log/page',
    params
  })
}

export const getReplayRunLog = (id: number) => {
  return request.get<RefundNotifyReplayRunLogVO>({
    url: '/booking/refund-notify-log/replay-run-log/get',
    params: { id }
  })
}

export const getReplayRunLogDetailPage = (params: RefundNotifyReplayRunLogDetailPageReq) => {
  return request.get<PageResult<RefundNotifyReplayRunLogDetailVO>>({
    url: '/booking/refund-notify-log/replay-run-log/detail/page',
    params
  })
}

export const getReplayRunLogDetail = (id: number | string) => {
  return request.get<RefundNotifyReplayRunLogDetailVO>({
    url: '/booking/refund-notify-log/replay-run-log/detail/get',
    params: { id }
  })
}

export const getReplayRunLogSummary = (runId: string | number) => {
  return request.get<RefundNotifyReplayRunLogSummaryVO>({
    url: '/booking/refund-notify-log/replay-run-log/summary',
    params: { runId }
  })
}

export const syncReplayRunLogTickets = (data: RefundNotifyReplayRunLogSyncTicketsReq) => {
  return request.post<RefundNotifyReplayRunLogSyncTicketsResp>({
    url: '/booking/refund-notify-log/replay-run-log/sync-tickets',
    data
  })
}
