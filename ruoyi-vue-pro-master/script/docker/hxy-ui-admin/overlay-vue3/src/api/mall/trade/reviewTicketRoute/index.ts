import request from '@/config/axios'

export interface ReviewTicketRouteVO {
  id?: number
  scope: string
  ruleCode?: string
  ticketType?: number
  severity?: string
  escalateTo: string
  slaMinutes: number
  enabled: boolean
  sort: number
  remark?: string
  createTime?: string
}

// 分页查询工单 SLA 路由规则
export const getReviewTicketRoutePage = async (params: any) => {
  return await request.get({ url: '/trade/after-sale/review-ticket-route/page', params })
}

// 查询工单 SLA 路由规则详情
export const getReviewTicketRoute = async (id: number) => {
  return await request.get({ url: '/trade/after-sale/review-ticket-route/get', params: { id } })
}

// 创建工单 SLA 路由规则
export const createReviewTicketRoute = async (data: ReviewTicketRouteVO) => {
  return await request.post({ url: '/trade/after-sale/review-ticket-route/create', data })
}

// 更新工单 SLA 路由规则
export const updateReviewTicketRoute = async (data: ReviewTicketRouteVO) => {
  return await request.put({ url: '/trade/after-sale/review-ticket-route/update', data })
}

// 删除工单 SLA 路由规则
export const deleteReviewTicketRoute = async (id: number) => {
  return await request.delete({ url: '/trade/after-sale/review-ticket-route/delete', params: { id } })
}

// 查询启用的工单 SLA 路由规则
export const getEnabledReviewTicketRouteList = async () => {
  return await request.get({ url: '/trade/after-sale/review-ticket-route/list-enabled' })
}
