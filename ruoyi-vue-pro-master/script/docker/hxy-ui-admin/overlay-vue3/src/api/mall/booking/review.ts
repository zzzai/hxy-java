import request from '@/config/axios'

export interface BookingReviewPageReq extends PageParam {
  id?: number
  bookingOrderId?: number
  storeId?: number
  technicianId?: number
  memberId?: number
  reviewLevel?: number
  riskLevel?: number
  followStatus?: number
  onlyManagerTodo?: boolean
  onlyPendingInit?: boolean
  managerTodoStatus?: number
  managerSlaStatus?: string
  replyStatus?: boolean
  submitTime?: string[]
}

export interface BookingReview {
  id: number
  bookingOrderId?: number
  serviceOrderId?: number
  storeId?: number
  storeName?: string
  technicianId?: number
  technicianName?: string
  memberId?: number
  memberNickname?: string
  serviceSpuId?: number
  serviceSkuId?: number
  overallScore?: number
  serviceScore?: number
  technicianScore?: number
  environmentScore?: number
  tags?: string[]
  content?: string
  picUrls?: string[]
  anonymous?: boolean
  reviewLevel?: number
  riskLevel?: number
  displayStatus?: number
  followStatus?: number
  replyStatus?: boolean
  auditStatus?: number
  source?: string
  completedTime?: string
  submitTime?: string
  negativeTriggerType?: string
  managerContactName?: string
  managerContactMobile?: string
  managerTodoStatus?: number
  managerClaimDeadlineAt?: string
  managerFirstActionDeadlineAt?: string
  managerCloseDeadlineAt?: string
  managerClaimedByUserId?: number
  managerClaimedAt?: string
  managerFirstActionAt?: string
  managerClosedAt?: string
  managerLatestActionRemark?: string
  managerLatestActionByUserId?: number
  firstResponseAt?: string
  followOwnerId?: number
  followResult?: string
  replyUserId?: number
  replyContent?: string
  replyTime?: string
  createTime?: string
}

export interface BookingReviewReplyReq {
  reviewId: number
  replyContent: string
}

export interface BookingReviewFollowUpdateReq {
  reviewId: number
  followStatus: number
  followResult?: string
}

export interface BookingReviewManagerTodoClaimReq {
  reviewId: number
}

export interface BookingReviewManagerTodoFirstActionReq {
  reviewId: number
  remark: string
}

export interface BookingReviewManagerTodoCloseReq {
  reviewId: number
  remark: string
}

export interface BookingReviewDashboardSummary {
  totalCount?: number
  positiveCount?: number
  neutralCount?: number
  negativeCount?: number
  pendingFollowCount?: number
  urgentCount?: number
  repliedCount?: number
  managerTodoPendingCount?: number
  managerTodoClaimTimeoutCount?: number
  managerTodoFirstActionTimeoutCount?: number
  managerTodoCloseTimeoutCount?: number
  managerTodoClosedCount?: number
}

export interface BookingReviewHistoryScanReq extends PageParam {
  storeId?: number
  bookingOrderId?: number
  riskCategory?: string
  submitTime?: string[]
}

export interface BookingReviewHistoryScanSummary {
  scannedCount?: number
  manualReadyCount?: number
  highRiskCount?: number
  outOfScopeCount?: number
}

export interface BookingReviewHistoryScanItem {
  reviewId: number
  bookingOrderId?: number
  storeId?: number
  storeName?: string
  technicianId?: number
  technicianName?: string
  memberId?: number
  memberNickname?: string
  submitTime?: string
  managerTodoStatus?: number
  riskCategory?: string
  riskReasons?: string[]
  riskSummary?: string
}

export interface BookingReviewHistoryScanResp {
  summary?: BookingReviewHistoryScanSummary
  list?: BookingReviewHistoryScanItem[]
  total?: number
}

export const getReviewPage = async (params: BookingReviewPageReq) => {
  return await request.get({ url: '/booking/review/page', params })
}

export const getReviewHistoryScan = async (params: BookingReviewHistoryScanReq) => {
  return await request.get<BookingReviewHistoryScanResp>({ url: '/booking/review/history-scan', params })
}

export const getReview = async (id: number) => {
  return await request.get<BookingReview>({ url: '/booking/review/get', params: { id } })
}

export const replyReview = async (data: BookingReviewReplyReq) => {
  return await request.post({ url: '/booking/review/reply', data })
}

export const updateReviewFollowStatus = async (data: BookingReviewFollowUpdateReq) => {
  return await request.post({ url: '/booking/review/follow-status', data })
}

export const claimManagerTodo = async (data: BookingReviewManagerTodoClaimReq) => {
  return await request.post({ url: '/booking/review/manager-todo/claim', data })
}

export const recordManagerTodoFirstAction = async (data: BookingReviewManagerTodoFirstActionReq) => {
  return await request.post({ url: '/booking/review/manager-todo/first-action', data })
}

export const closeManagerTodo = async (data: BookingReviewManagerTodoCloseReq) => {
  return await request.post({ url: '/booking/review/manager-todo/close', data })
}

export const getReviewDashboardSummary = async () => {
  return await request.get<BookingReviewDashboardSummary>({ url: '/booking/review/dashboard-summary' })
}
