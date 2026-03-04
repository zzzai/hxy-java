import request from '@/config/axios'

export interface StoreLifecycleBatchLogDetailItem {
  storeId?: number
  storeName?: string
  result?: string
  message?: string
}

export interface StoreLifecycleBatchLogItem {
  id: number
  batchNo: string
  targetLifecycleStatus: number
  totalCount: number
  successCount: number
  blockedCount: number
  warningCount: number
  auditSummary?: string
  detailJson?: string
  operator?: string
  source?: string
  createTime?: string
}

export interface StoreLifecycleBatchLogPageReq extends PageParam {
  batchNo?: string
  targetLifecycleStatus?: number
  operator?: string
  source?: string
  createTime?: string[]
}

export const getStoreLifecycleBatchLogPage = async (params: StoreLifecycleBatchLogPageReq) => {
  return await request.get({ url: '/product/store/lifecycle-batch-log/page', params })
}
