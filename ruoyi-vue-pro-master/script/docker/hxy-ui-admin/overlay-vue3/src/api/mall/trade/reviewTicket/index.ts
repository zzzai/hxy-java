import request from '@/config/axios'

export interface ReviewTicketVO {
  id?: number
  ticketType?: number
  afterSaleId?: number
  sourceBizNo?: string
  orderId?: number
  orderItemId?: number
  userId?: number
  ruleCode?: string
  decisionReason?: string
  severity?: string
  escalateTo?: string
  routeId?: number
  routeScope?: string
  routeDecisionOrder?: string
  slaDeadlineTime?: string
  status?: number
  overdue?: boolean
  firstTriggerTime?: string
  lastTriggerTime?: string
  triggerCount?: number
  resolvedTime?: string
  resolverId?: number
  resolverType?: number
  resolveActionCode?: string
  resolveBizNo?: string
  lastActionCode?: string
  lastActionBizNo?: string
  lastActionTime?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface ReviewTicketPageReqVO extends PageParam {
  ticketType?: number
  status?: number
  severity?: string
  escalateTo?: string
  routeId?: number
  routeScope?: string
  overdue?: boolean
  lastActionCode?: string
  sourceBizNo?: string
  afterSaleId?: number
  orderId?: number
  userId?: number
  createTime?: string[]
}

export interface ReviewTicketResolveReqVO {
  id: number
  resolveActionCode?: string
  resolveBizNo?: string
  resolveRemark?: string
}

export interface ReviewTicketBatchResolveReqVO {
  ids: number[]
  resolveActionCode?: string
  resolveBizNo?: string
  resolveRemark?: string
}

export interface ReviewTicketBatchResolveRespVO {
  totalCount: number
  successCount: number
  skippedNotFoundCount: number
  skippedNotPendingCount: number
  successIds?: number[]
  skippedNotFoundIds?: number[]
  skippedNotPendingIds?: number[]
}

// 分页查询人工复核工单
export const getReviewTicketPage = async (params: ReviewTicketPageReqVO) => {
  return await request.get({ url: '/trade/after-sale/review-ticket/page', params })
}

// 查询人工复核工单详情
export const getReviewTicket = async (id: number) => {
  return await request.get<ReviewTicketVO>({ url: '/trade/after-sale/review-ticket/get', params: { id } })
}

// 收口人工复核工单
export const resolveReviewTicket = async (data: ReviewTicketResolveReqVO) => {
  return await request.put({ url: '/trade/after-sale/review-ticket/resolve', data })
}

// 批量收口人工复核工单
export const batchResolveReviewTicket = async (data: ReviewTicketBatchResolveReqVO) => {
  return await request.post<ReviewTicketBatchResolveRespVO>({ url: '/trade/after-sale/review-ticket/batch-resolve', data })
}
