import request from '@/config/axios'

export type StoreLifecycleChangeOrderStatus = 0 | 10 | 20 | 30 | 40 | number

export interface StoreLifecycleChangeOrderItem {
  id: number
  orderNo?: string
  storeId?: number
  storeName?: string
  fromLifecycleStatus?: number
  toLifecycleStatus?: number
  status?: StoreLifecycleChangeOrderStatus
  reason?: string
  guardSnapshotJson?: string
  guardBlocked?: boolean
  guardWarnings?: string
  applyOperator?: string
  applySource?: string
  submitTime?: string
  slaDeadlineTime?: string
  overdue?: boolean
  lastActionCode?: string
  lastActionOperator?: string
  lastActionTime?: string
  approveOperator?: string
  approveTime?: string
  approveRemark?: string
  creator?: string
  createTime?: string
}

export interface StoreLifecycleChangeOrderPageReq extends PageParam {
  orderNo?: string
  storeId?: number
  status?: StoreLifecycleChangeOrderStatus
  fromLifecycleStatus?: number
  toLifecycleStatus?: number
  applyOperator?: string
  overdue?: boolean
  lastActionCode?: string
  lastActionOperator?: string
  createTime?: string[]
}

export interface StoreLifecycleChangeOrderCreateReq {
  storeId?: number
  toLifecycleStatus?: number
  reason?: string
  applySource?: string
}

export interface StoreLifecycleChangeOrderSubmitReq {
  id: number
}

export interface StoreLifecycleChangeOrderApproveReq {
  id: number
  remark?: string
}

export interface StoreLifecycleChangeOrderRejectReq {
  id: number
  remark?: string
}

export interface StoreLifecycleChangeOrderCancelReq {
  id: number
  remark?: string
}

export const createStoreLifecycleChangeOrder = async (data: StoreLifecycleChangeOrderCreateReq) => {
  return await request.post<number>({ url: '/product/store/lifecycle-change-order/create', data })
}

export const submitStoreLifecycleChangeOrder = async (data: StoreLifecycleChangeOrderSubmitReq) => {
  return await request.post<boolean>({ url: '/product/store/lifecycle-change-order/submit', data })
}

export const approveStoreLifecycleChangeOrder = async (data: StoreLifecycleChangeOrderApproveReq) => {
  return await request.post<boolean>({ url: '/product/store/lifecycle-change-order/approve', data })
}

export const rejectStoreLifecycleChangeOrder = async (data: StoreLifecycleChangeOrderRejectReq) => {
  return await request.post<boolean>({ url: '/product/store/lifecycle-change-order/reject', data })
}

export const cancelStoreLifecycleChangeOrder = async (data: StoreLifecycleChangeOrderCancelReq) => {
  return await request.post<boolean>({ url: '/product/store/lifecycle-change-order/cancel', data })
}

export const getStoreLifecycleChangeOrder = async (id: number) => {
  return await request.get<StoreLifecycleChangeOrderItem>({
    url: '/product/store/lifecycle-change-order/get',
    params: { id }
  })
}

export const getStoreLifecycleChangeOrderPage = async (params: StoreLifecycleChangeOrderPageReq) => {
  return await request.get({ url: '/product/store/lifecycle-change-order/page', params })
}
