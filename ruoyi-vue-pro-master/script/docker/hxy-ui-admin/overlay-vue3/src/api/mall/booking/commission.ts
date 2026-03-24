import request from '@/config/axios'

export interface TechnicianCommission {
  id: number
  technicianId?: number
  orderId?: number
  orderItemId?: number
  serviceOrderId?: number
  userId?: number
  storeId?: number
  commissionType?: number
  baseAmount?: number
  commissionRate?: string
  commissionAmount?: number
  status?: number
  sourceBizNo?: string
  settlementId?: number
  settlementTime?: string
  createTime?: string
}

export interface TechnicianCommissionConfig {
  id: number
  storeId?: number
  commissionType?: number
  rate?: string
  fixedAmount?: number
  createTime?: string
  updateTime?: string
  creator?: string
  updater?: string
  deleted?: boolean
}

export interface TechnicianCommissionConfigSaveReq {
  id?: number
  storeId: number
  commissionType: number
  rate: string
  fixedAmount?: number
}

export const getCommissionListByTechnician = (technicianId: number) => {
  return request.get<TechnicianCommission[]>({
    url: '/booking/commission/list-by-technician',
    params: { technicianId }
  })
}

export const getCommissionListByOrder = (orderId: number) => {
  return request.get<TechnicianCommission[]>({
    url: '/booking/commission/list-by-order',
    params: { orderId }
  })
}

export const getPendingCommissionAmount = (technicianId: number) => {
  return request.get<number>({
    url: '/booking/commission/pending-amount',
    params: { technicianId }
  })
}

export const settleCommission = (commissionId: number) => {
  return request.post<boolean>({
    url: '/booking/commission/settle',
    params: { commissionId }
  })
}

export const batchSettleCommission = (technicianId: number) => {
  return request.post<boolean>({
    url: '/booking/commission/batch-settle',
    params: { technicianId }
  })
}

export const getCommissionConfigList = (storeId: number) => {
  return request.get<TechnicianCommissionConfig[]>({
    url: '/booking/commission/config/list',
    params: { storeId }
  })
}

export const saveCommissionConfig = (data: TechnicianCommissionConfigSaveReq) => {
  return request.post<boolean>({
    url: '/booking/commission/config/save',
    data
  })
}

export const deleteCommissionConfig = (id: number) => {
  return request.delete<boolean>({
    url: '/booking/commission/config/delete',
    params: { id }
  })
}
