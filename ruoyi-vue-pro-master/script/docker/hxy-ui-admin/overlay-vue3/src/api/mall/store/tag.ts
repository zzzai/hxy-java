import request from '@/config/axios'

export interface HxyStoreTag {
  id?: number
  code: string
  name: string
  groupId?: number
  groupName?: string
  status?: number
  sort?: number
  remark?: string
}

export const getStoreTagList = (params?: any) => {
  return request.get({ url: '/product/store-tag/list', params })
}

export const getStoreTag = (id: number) => {
  return request.get({ url: `/product/store-tag/get?id=${id}` })
}

export const saveStoreTag = (data: HxyStoreTag) => {
  return request.post({ url: '/product/store-tag/save', data })
}

export const deleteStoreTag = (id: number) => {
  return request.delete({ url: `/product/store-tag/delete?id=${id}` })
}
