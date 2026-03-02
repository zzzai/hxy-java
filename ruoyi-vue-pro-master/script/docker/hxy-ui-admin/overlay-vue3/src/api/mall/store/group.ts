import request from '@/config/axios'

export interface HxyStoreTagGroup {
  id?: number
  code: string
  name: string
  required?: number
  mutex?: number
  editableByStore?: number
  status?: number
  sort?: number
  remark?: string
}

export const getStoreTagGroupList = (params?: any) => {
  return request.get({ url: '/product/store-tag-group/list', params })
}

export const getStoreTagGroup = (id: number) => {
  return request.get({ url: `/product/store-tag-group/get?id=${id}` })
}

export const saveStoreTagGroup = (data: HxyStoreTagGroup) => {
  return request.post({ url: '/product/store-tag-group/save', data })
}

export const deleteStoreTagGroup = (id: number) => {
  return request.delete({ url: `/product/store-tag-group/delete?id=${id}` })
}
