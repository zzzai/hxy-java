import request from '@/config/axios'

export interface ProductStoreSku {
  id?: number
  storeId: number
  storeName?: string
  spuId?: number
  spuName?: string
  skuId?: number
  skuSpecText?: string
  saleStatus?: number
  salePrice?: number
  marketPrice?: number
  stock?: number
  sort?: number
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface ProductStoreOption {
  id: number
  name: string
}

export interface ProductStoreSpuOption {
  id: number
  name: string
  productType?: number
  status?: number
}

export interface ProductStoreSkuOption {
  id: number
  spuId: number
  specText?: string
  price?: number
  marketPrice?: number
  stock?: number
}

export interface ProductStoreSkuBatchSave {
  storeIds: number[]
  spuId?: number
  skuId?: number
  saleStatus?: number
  salePrice?: number
  marketPrice?: number
  stock?: number
  sort?: number
  remark?: string
}

export interface ProductStoreSkuBatchAdjust {
  storeIds: number[]
  spuId?: number
  skuId?: number
  saleStatus?: number
  salePrice?: number
  marketPrice?: number
  stock?: number
  remark?: string
}

export interface ProductStoreSkuManualStockAdjustItem {
  skuId: number
  incrCount: number
}

export interface ProductStoreSkuManualStockAdjust {
  storeId: number
  bizType: string
  bizNo: string
  remark?: string
  items: ProductStoreSkuManualStockAdjustItem[]
}

export interface ProductStoreSkuStockFlow {
  id?: number
  storeId?: number
  storeName?: string
  skuId?: number
  bizType?: string
  bizNo?: string
  incrCount?: number
  status?: number
  retryCount?: number
  nextRetryTime?: string
  lastErrorMsg?: string
  executeTime?: string
  lastRetryOperator?: string
  lastRetrySource?: string
  operator?: string
  source?: string
  canRetry?: boolean
  retryable?: boolean
  allowRetry?: boolean
  canBatchRetry?: boolean
  createTime?: string
}

export interface ProductStoreSkuStockFlowPageReq extends PageParam {
  storeId?: number
  skuId?: number
  bizType?: string
  bizNo?: string
  status?: number
  operator?: string
  source?: string
  executeTime?: string[]
}

export interface ProductStoreSkuStockFlowBatchRetryReq {
  ids: number[]
  source?: string
}

export interface ProductStoreSkuStockFlowBatchRetryItem {
  id?: number
  storeId?: number
  skuId?: number
  resultType?: 'SUCCESS' | 'SKIPPED' | 'FAILED' | string
  resultStatus?: 'SUCCESS' | 'SKIPPED' | 'FAILED' | string
  reason?: string
  failReason?: string
  message?: string
  status?: number
  retryOperator?: string
  retrySource?: string
  operator?: string
  source?: string
}

export interface ProductStoreSkuStockFlowBatchRetryResp {
  totalCount?: number
  successCount?: number
  skippedCount?: number
  failedCount?: number
  items?: ProductStoreSkuStockFlowBatchRetryItem[]
}

export const getStoreSkuPage = (params: PageParam) => {
  return request.get({ url: '/product/store-sku/page', params })
}

export const getStoreSku = (id: number) => {
  return request.get({ url: `/product/store-sku/get?id=${id}` })
}

export const saveStoreSku = (data: ProductStoreSku) => {
  return request.post({ url: '/product/store-sku/save', data })
}

export const batchSaveStoreSku = (data: ProductStoreSkuBatchSave) => {
  return request.post({ url: '/product/store-sku/batch-save', data })
}

export const batchAdjustStoreSku = (data: ProductStoreSkuBatchAdjust) => {
  return request.post({ url: '/product/store-sku/batch-adjust', data })
}

export const manualAdjustStoreSkuStock = (data: ProductStoreSkuManualStockAdjust) => {
  return request.post({ url: '/product/store-sku/manual-stock-adjust', data })
}

export const getStoreSkuStockFlowPage = (params: ProductStoreSkuStockFlowPageReq) => {
  return request.get({ url: '/product/store-sku/stock-flow/page', params })
}

export const batchRetryStoreSkuStockFlow = (data: ProductStoreSkuStockFlowBatchRetryReq) => {
  return request.post<ProductStoreSkuStockFlowBatchRetryResp>({
    url: '/product/store-sku/stock-flow/batch-retry',
    data
  })
}

export const deleteStoreSku = (id: number) => {
  return request.delete({ url: `/product/store-sku/delete?id=${id}` })
}

export const getStoreOptions = (keyword?: string) => {
  return request.get({ url: '/product/store-sku/store-options', params: { keyword } })
}

export const getSpuOptions = (productType?: number, keyword?: string) => {
  return request.get({ url: '/product/store-sku/spu-options', params: { productType, keyword } })
}

export const getSkuOptions = (spuId: number) => {
  return request.get({ url: '/product/store-sku/sku-options', params: { spuId } })
}
