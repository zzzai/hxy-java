import request from '@/config/axios'

export interface ProductStoreSpu {
  id?: number
  storeId: number
  storeName?: string
  spuId?: number
  spuName?: string
  productType?: number
  saleStatus?: number
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

export interface ProductStoreSpuBatchSave {
  storeIds: number[]
  spuId?: number
  saleStatus?: number
  sort?: number
  remark?: string
}

export const getStoreSpuPage = (params: PageParam) => {
  return request.get({ url: '/product/store-spu/page', params })
}

export const getStoreSpu = (id: number) => {
  return request.get({ url: `/product/store-spu/get?id=${id}` })
}

export const saveStoreSpu = (data: ProductStoreSpu) => {
  return request.post({ url: '/product/store-spu/save', data })
}

export const batchSaveStoreSpu = (data: ProductStoreSpuBatchSave) => {
  return request.post({ url: '/product/store-spu/batch-save', data })
}

export const deleteStoreSpu = (id: number) => {
  return request.delete({ url: `/product/store-spu/delete?id=${id}` })
}

export const getStoreOptions = (keyword?: string) => {
  return request.get({ url: '/product/store-spu/store-options', params: { keyword } })
}

export const getSpuOptions = (productType?: number, keyword?: string) => {
  return request.get({ url: '/product/store-spu/spu-options', params: { productType, keyword } })
}
