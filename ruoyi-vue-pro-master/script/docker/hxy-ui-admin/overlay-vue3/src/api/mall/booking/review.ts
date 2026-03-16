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
  replyStatus?: boolean
  submitTime?: string[]
}

export interface BookingReview {
  id: number
  bookingOrderId?: number
  serviceOrderId?: number
  storeId?: number
  technicianId?: number
  memberId?: number
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

export interface BookingReviewDashboardSummary {
  totalCount?: number
  positiveCount?: number
  neutralCount?: number
  negativeCount?: number
  pendingFollowCount?: number
  urgentCount?: number
  repliedCount?: number
}

export const getReviewPage = async (params: BookingReviewPageReq) => {
  return await request.get({ url: '/booking/review/page', params })
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

export const getReviewDashboardSummary = async () => {
  return await request.get<BookingReviewDashboardSummary>({ url: '/booking/review/dashboard-summary' })
}
