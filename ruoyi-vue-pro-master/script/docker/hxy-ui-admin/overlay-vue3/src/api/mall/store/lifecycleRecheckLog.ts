import request from '@/config/axios'
import type { StoreLifecycleBatchLogGuardItem } from './lifecycleBatchLog'

export interface StoreLifecycleRecheckLogItem {
  id: number
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
  detailJson?: string
  operator?: string
  source?: string
  createTime?: string
}

export interface StoreLifecycleRecheckLogPageReq extends PageParam {
  recheckNo?: string
  logId?: number
  batchNo?: string
  targetLifecycleStatus?: number
  operator?: string
  source?: string
  createTime?: string[]
}

export interface StoreLifecycleRecheckLogDetailItem {
  storeId?: number
  storeName?: string
  blocked?: boolean
  blockedCode?: number
  blockedMessage?: string
  warnings?: string[]
  guardItems?: StoreLifecycleBatchLogGuardItem[]
}

export interface StoreLifecycleRecheckLogGetResp extends StoreLifecycleRecheckLogItem {
  detailView?: Record<string, any> | null
  details?: StoreLifecycleRecheckLogDetailItem[]
}

export const getStoreLifecycleRecheckLogPage = async (params: StoreLifecycleRecheckLogPageReq) => {
  return await request.get({ url: '/product/store/lifecycle-recheck-log/page', params })
}

export const getStoreLifecycleRecheckLog = async (id: number) => {
  return await request.get<StoreLifecycleRecheckLogGetResp>({
    url: '/product/store/lifecycle-recheck-log/get',
    params: { id }
  })
}
