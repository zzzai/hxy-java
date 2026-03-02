import request from '@/config/axios'

export interface HxyStoreCategory {
  id?: number
  code: string
  name: string
  parentId?: number
  level?: number
  status?: number
  sort?: number
  remark?: string
}

export const getStoreCategoryList = (params?: any) => {
  return request.get({ url: '/product/store-category/list', params })
}

export const getStoreCategory = (id: number) => {
  return request.get({ url: `/product/store-category/get?id=${id}` })
}

export const saveStoreCategory = (data: HxyStoreCategory) => {
  return request.post({ url: '/product/store-category/save', data })
}

export const deleteStoreCategory = (id: number) => {
  return request.delete({ url: `/product/store-category/delete?id=${id}` })
}
