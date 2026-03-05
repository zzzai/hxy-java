import request from '@/config/axios'

export interface StoreLifecycleBatchLogGuardItem {
  guardKey?: string
  count?: number
  mode?: string
  blocked?: boolean
}

export interface StoreLifecycleBatchLogDetailItem {
  storeId?: number
  storeName?: string
  result?: string
  message?: string
  blocked?: boolean
  blockedCode?: number
  blockedMessage?: string
  warnings?: string[]
  guardItems?: StoreLifecycleBatchLogGuardItem[]
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
  guardRuleVersion?: string
  guardConfigSnapshotJson?: string
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

export interface StoreLifecycleBatchLogGetResp extends StoreLifecycleBatchLogItem {
  detailView?: Record<string, any> | null
  detailParseError?: boolean
}

export interface StoreLifecycleGuardBatchRecheckReq {
  logId?: number
  batchNo?: string
}

export interface StoreLifecycleGuardBatchRecheckDetail {
  storeId?: number
  storeName?: string
  blocked?: boolean
  blockedCode?: number
  blockedMessage?: string
  warnings?: string[]
  guardItems?: StoreLifecycleBatchLogGuardItem[]
}

export interface StoreLifecycleGuardBatchRecheckResp {
  recheckNo?: string
  logId?: number
  batchNo?: string
  targetLifecycleStatus?: number
  totalCount?: number
  blockedCount?: number
  warningCount?: number
  detailParseError?: boolean
  guardRuleVersion?: string
  guardConfigSnapshotJson?: string
  details?: StoreLifecycleGuardBatchRecheckDetail[]
}

export const getStoreLifecycleBatchLogPage = async (params: StoreLifecycleBatchLogPageReq) => {
  return await request.get({ url: '/product/store/lifecycle-batch-log/page', params })
}

export const getStoreLifecycleBatchLog = async (id: number) => {
  return await request.get<StoreLifecycleBatchLogGetResp>({
    url: '/product/store/lifecycle-batch-log/get',
    params: { id }
  })
}

export const executeStoreLifecycleGuardRecheckByBatch = async (data: StoreLifecycleGuardBatchRecheckReq) => {
  return await request.post<StoreLifecycleGuardBatchRecheckResp>({
    url: '/product/store/lifecycle-guard/recheck-by-batch/execute',
    data
  })
}
