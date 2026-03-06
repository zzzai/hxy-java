import request from '@/config/axios'

export type StoreSkuTransferOrderStatus = 0 | 10 | 20 | 30 | 40 | number

export interface StoreSkuTransferOrderItem {
  id: number
  orderNo?: string
  outStoreId?: number
  outStoreName?: string
  inStoreId?: number
  inStoreName?: string
  bizType?: string
  reason?: string
  remark?: string
  status?: StoreSkuTransferOrderStatus
  detailJson?: string
  applyOperator?: string
  applySource?: string
  approveOperator?: string
  approveRemark?: string
  approveTime?: string
  lastActionCode?: string
  lastActionOperator?: string
  lastActionTime?: string
  createTime?: string
}

export interface StoreSkuTransferOrderDetailItem {
  skuId?: number
  transferCount?: number
  outSkuId?: number
  inSkuId?: number
  incrCount?: number
}

export interface StoreSkuTransferOrderPageReq extends PageParam {
  orderNo?: string
  outStoreId?: number
  inStoreId?: number
  skuId?: number
  status?: StoreSkuTransferOrderStatus
  bizType?: string
  applyOperator?: string
  lastActionCode?: string
  lastActionOperator?: string
  createTime?: string[]
}

export interface StoreSkuTransferOrderActionReq {
  id: number
  remark?: string
}

export const getStoreSkuTransferOrderPage = (params: StoreSkuTransferOrderPageReq) => {
  return request.get({ url: '/product/store-sku/transfer-order/page', params })
}

export const getStoreSkuTransferOrder = (id: number) => {
  return request.get<StoreSkuTransferOrderItem>({
    url: '/product/store-sku/transfer-order/get',
    params: { id }
  })
}

export const submitStoreSkuTransferOrder = (data: StoreSkuTransferOrderActionReq) => {
  return request.post<boolean>({ url: '/product/store-sku/transfer-order/submit', data })
}

export const approveStoreSkuTransferOrder = (data: StoreSkuTransferOrderActionReq) => {
  return request.post<boolean>({ url: '/product/store-sku/transfer-order/approve', data })
}

export const rejectStoreSkuTransferOrder = (data: StoreSkuTransferOrderActionReq) => {
  return request.post<boolean>({ url: '/product/store-sku/transfer-order/reject', data })
}

export const cancelStoreSkuTransferOrder = (data: StoreSkuTransferOrderActionReq) => {
  return request.post<boolean>({ url: '/product/store-sku/transfer-order/cancel', data })
}
