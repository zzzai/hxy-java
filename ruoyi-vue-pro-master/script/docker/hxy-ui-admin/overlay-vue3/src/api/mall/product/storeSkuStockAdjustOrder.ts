import request from '@/config/axios'

export type StoreSkuStockAdjustOrderStatus = 0 | 10 | 20 | 30 | 40 | number

export interface StoreSkuStockAdjustOrderItem {
  id: number
  orderNo?: string
  storeId?: number
  storeName?: string
  bizType?: string
  reason?: string
  remark?: string
  status?: StoreSkuStockAdjustOrderStatus
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

export interface StoreSkuStockAdjustOrderDetailItem {
  skuId?: number
  incrCount?: number
}

export interface StoreSkuStockAdjustOrderPageReq extends PageParam {
  orderNo?: string
  storeId?: number
  status?: StoreSkuStockAdjustOrderStatus
  bizType?: string
  applyOperator?: string
  lastActionCode?: string
  lastActionOperator?: string
  createTime?: string[]
}

export interface StoreSkuStockAdjustOrderCreateItem {
  skuId: number
  incrCount: number
}

export interface StoreSkuStockAdjustOrderCreateReq {
  storeId: number
  bizType: string
  reason: string
  remark?: string
  applySource?: string
  items: StoreSkuStockAdjustOrderCreateItem[]
}

export interface StoreSkuStockAdjustOrderActionReq {
  id: number
  remark?: string
}

export const createStoreSkuStockAdjustOrder = (data: StoreSkuStockAdjustOrderCreateReq) => {
  return request.post<number>({ url: '/product/store-sku/stock-adjust-order/create', data })
}

export const getStoreSkuStockAdjustOrderPage = (params: StoreSkuStockAdjustOrderPageReq) => {
  return request.get({ url: '/product/store-sku/stock-adjust-order/page', params })
}

export const getStoreSkuStockAdjustOrder = (id: number) => {
  return request.get<StoreSkuStockAdjustOrderItem>({
    url: '/product/store-sku/stock-adjust-order/get',
    params: { id }
  })
}

export const submitStoreSkuStockAdjustOrder = (data: StoreSkuStockAdjustOrderActionReq) => {
  return request.post<boolean>({ url: '/product/store-sku/stock-adjust-order/submit', data })
}

export const approveStoreSkuStockAdjustOrder = (data: StoreSkuStockAdjustOrderActionReq) => {
  return request.post<boolean>({ url: '/product/store-sku/stock-adjust-order/approve', data })
}

export const rejectStoreSkuStockAdjustOrder = (data: StoreSkuStockAdjustOrderActionReq) => {
  return request.post<boolean>({ url: '/product/store-sku/stock-adjust-order/reject', data })
}

export const cancelStoreSkuStockAdjustOrder = (data: StoreSkuStockAdjustOrderActionReq) => {
  return request.post<boolean>({ url: '/product/store-sku/stock-adjust-order/cancel', data })
}
