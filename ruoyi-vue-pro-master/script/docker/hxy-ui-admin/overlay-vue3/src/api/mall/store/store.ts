import request from '@/config/axios'

export interface HxyStore {
  id?: number
  code: string
  name: string
  shortName?: string
  categoryId: number
  categoryName?: string
  status?: number
  lifecycleStatus?: number
  contactName?: string
  contactMobile?: string
  provinceCode?: string
  cityCode?: string
  districtCode?: string
  address?: string
  longitude?: number
  latitude?: number
  openingTime?: string
  closingTime?: string
  sort?: number
  remark?: string
  tagIds?: number[]
  createTime?: string
}

export interface HxyStoreSimple {
  id: number
  code: string
  name: string
  shortName?: string
}

export interface HxyStoreLaunchReadiness {
  storeId: number
  ready: boolean
  reasons: string[]
}

export interface HxyStoreLifecycleGuardItem {
  guardKey: string
  count: number
  mode: string
  blocked: boolean
}

export interface HxyStoreLifecycleGuardResp {
  storeId: number
  targetLifecycleStatus: number
  blocked: boolean
  blockedCode?: number
  blockedMessage?: string
  warnings: string[]
  guardItems: HxyStoreLifecycleGuardItem[]
}

export const getStorePage = (params: PageParam) => {
  return request.get({ url: '/product/store/page', params })
}

export const getStore = (id: number) => {
  return request.get({ url: `/product/store/get?id=${id}` })
}

export const saveStore = (data: HxyStore) => {
  return request.post({ url: '/product/store/save', data })
}

export const deleteStore = (id: number) => {
  return request.delete({ url: `/product/store/delete?id=${id}` })
}

export const getStoreSimpleList = (keyword?: string) => {
  return request.get({ url: '/product/store/simple-list', params: { keyword } })
}

export const checkStoreLaunchReadiness = (id: number) => {
  return request.get<HxyStoreLaunchReadiness>({ url: `/product/store/check-launch-readiness?id=${id}` })
}

export const getStoreLifecycleGuard = (id: number, lifecycleStatus: number) => {
  return request.get<HxyStoreLifecycleGuardResp>({
    url: '/product/store/lifecycle-guard',
    params: { id, lifecycleStatus }
  })
}

export const getStoreLifecycleGuardBatch = (data: {
  storeIds: number[]
  lifecycleStatus: number
  reason?: string
}) => {
  return request.post<HxyStoreLifecycleGuardResp[]>({ url: '/product/store/lifecycle-guard/batch', data })
}

export const updateStoreLifecycle = (data: { id: number; lifecycleStatus: number; reason?: string }) => {
  return request.post({ url: '/product/store/update-lifecycle', data })
}

export const batchUpdateStoreCategory = (data: { storeIds: number[]; categoryId: number; reason?: string }) => {
  return request.post({ url: '/product/store/batch/category', data })
}

export const batchUpdateStoreTags = (data: { storeIds: number[]; tagIds: number[]; reason?: string }) => {
  return request.post({ url: '/product/store/batch/tags', data })
}

export const batchUpdateStoreLifecycle = (data: {
  storeIds: number[]
  lifecycleStatus: number
  reason?: string
}) => {
  return request.post({ url: '/product/store/batch/lifecycle', data })
}
