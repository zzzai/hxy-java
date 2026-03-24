<template>
  <doc-alert title="技师提成明细 / 计提管理" url="https://doc.iocoder.cn/" />

  <ContentWrap>
    <el-alert
      :closable="false"
      title="BO-004 真值说明"
      description="当前页面只代表 BO-004 的 admin-only 页面/API 真值已形成；所有写动作都必须按写后回读确认，接口返回 success(true) 不等于真实生效，也不代表当前能力已可放量。"
      type="info"
    />
  </ContentWrap>

  <ContentWrap>
    <el-form :inline="true" :model="queryParams" class="-mb-15px" label-width="88px">
      <el-form-item label="技师ID" prop="technicianId">
        <el-input-number
          v-model="queryParams.technicianId"
          :controls="false"
          :min="1"
          class="!w-180px"
          placeholder="请输入技师ID"
        />
      </el-form-item>
      <el-form-item label="订单ID" prop="orderId">
        <el-input-number
          v-model="queryParams.orderId"
          :controls="false"
          :min="1"
          class="!w-180px"
          placeholder="请输入订单ID"
        />
      </el-form-item>
      <el-form-item label="门店ID" prop="storeId">
        <el-input-number
          v-model="queryParams.storeId"
          :controls="false"
          :min="1"
          class="!w-180px"
          placeholder="请输入门店ID"
        />
      </el-form-item>
      <el-form-item>
        <el-button :loading="loading" @click="handleQuery">
          <Icon class="mr-5px" icon="ep:search" />
          搜索
        </el-button>
        <el-button @click="resetQuery">
          <Icon class="mr-5px" icon="ep:refresh" />
          重置
        </el-button>
        <el-button
          v-hasPermi="['booking:commission:settle']"
          :disabled="!queryParams.technicianId"
          plain
          type="warning"
          @click="handleBatchSettle"
        >
          <Icon class="mr-5px" icon="ep:finished" />
          批量直结
        </el-button>
        <el-button
          v-hasPermi="['booking:commission:config']"
          :disabled="!queryParams.storeId"
          plain
          type="primary"
          @click="openConfigDialog()"
        >
          <Icon class="mr-5px" icon="ep:plus" />
          新增配置
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-row :gutter="12">
      <el-col :lg="8" :md="8" :sm="24" :xs="24">
        <el-card shadow="never">
          <div class="text-12px text-[var(--el-text-color-secondary)]">当前记录数</div>
          <div class="mt-8px text-26px font-600">{{ commissionList.length }}</div>
          <div class="mt-6px text-12px text-[var(--el-text-color-secondary)]">
            查询模式：{{ queryModeLabel }}
          </div>
        </el-card>
      </el-col>
      <el-col :lg="8" :md="8" :sm="24" :xs="24">
        <el-card shadow="never">
          <div class="text-12px text-[var(--el-text-color-secondary)]">待结算金额（元）</div>
          <div class="mt-8px text-26px font-600">{{ pendingAmountText }}</div>
          <div class="mt-6px text-12px text-[var(--el-text-color-secondary)]">仅在传入技师ID时查询</div>
        </el-card>
      </el-col>
      <el-col :lg="8" :md="8" :sm="24" :xs="24">
        <el-card shadow="never">
          <div class="text-12px text-[var(--el-text-color-secondary)]">配置条数</div>
          <div class="mt-8px text-26px font-600">{{ configList.length }}</div>
          <div class="mt-6px text-12px text-[var(--el-text-color-secondary)]">仅在传入门店ID时查询</div>
        </el-card>
      </el-col>
    </el-row>
  </ContentWrap>

  <ContentWrap v-if="readbackEvidence.title">
    <el-alert
      :closable="false"
      :description="readbackEvidence.description"
      :title="readbackEvidence.title"
      :type="readbackEvidence.type"
    />
  </ContentWrap>

  <ContentWrap>
    <el-tabs v-model="activeTab">
      <el-tab-pane label="佣金记录" name="commission">
        <div class="mb-12px text-13px text-[var(--el-text-color-secondary)]">
          记录查询优先级：有订单ID时按订单查询，否则按技师查询。单条直结、批量直结都必须写后回读；若接口返回成功但读后未变，会明确提示 no-op 风险。
        </div>
        <el-empty
          v-if="!hasRecordQueryCondition && commissionList.length === 0"
          description="请输入技师ID或订单ID后查询佣金记录"
        />
        <el-table v-else v-loading="loading" :data="commissionList">
          <el-table-column label="佣金ID" prop="id" width="90" />
          <el-table-column label="技师ID" prop="technicianId" width="100" />
          <el-table-column label="订单ID" prop="orderId" width="100" />
          <el-table-column label="门店ID" prop="storeId" width="100" />
          <el-table-column label="佣金类型" width="120">
            <template #default="{ row }">
              {{ commissionTypeText(row.commissionType) }}
            </template>
          </el-table-column>
          <el-table-column label="订单金额(元)" width="120">
            <template #default="{ row }">
              {{ fenToYuan(row.baseAmount) }}
            </template>
          </el-table-column>
          <el-table-column label="佣金比例" width="110">
            <template #default="{ row }">
              {{ rateText(row.commissionRate) }}
            </template>
          </el-table-column>
          <el-table-column label="佣金金额(元)" width="120">
            <template #default="{ row }">
              {{ fenToYuan(row.commissionAmount) }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="commissionStatusTagType(row.status)">
                {{ commissionStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="追溯业务号" min-width="180" prop="sourceBizNo" show-overflow-tooltip />
          <el-table-column label="结算单ID" prop="settlementId" width="110" />
          <el-table-column :formatter="dateFormatter" label="结算时间" prop="settlementTime" width="180" />
          <el-table-column :formatter="dateFormatter" label="创建时间" prop="createTime" width="180" />
          <el-table-column align="center" fixed="right" label="操作" width="120">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 0"
                v-hasPermi="['booking:commission:settle']"
                link
                type="warning"
                @click="handleSettle(row)"
              >
                单条直结
              </el-button>
              <span v-else class="text-[var(--el-text-color-secondary)]">-</span>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="门店佣金配置" name="config">
        <div class="mb-12px text-13px text-[var(--el-text-color-secondary)]">
          配置新增、编辑、删除都只以写后回读为准；接口 success(true) 但列表未变化，会被标记为“接口返回成功但读后未变”。
        </div>
        <el-empty v-if="!queryParams.storeId && configList.length === 0" description="请输入门店ID后查询配置" />
        <el-table v-else v-loading="loading" :data="configList">
          <el-table-column label="配置ID" prop="id" width="90" />
          <el-table-column label="门店ID" prop="storeId" width="100" />
          <el-table-column label="佣金类型" width="120">
            <template #default="{ row }">
              {{ commissionTypeText(row.commissionType) }}
            </template>
          </el-table-column>
          <el-table-column label="佣金比例" width="110">
            <template #default="{ row }">
              {{ rateText(row.rate) }}
            </template>
          </el-table-column>
          <el-table-column label="固定金额(元)" width="120">
            <template #default="{ row }">
              {{ fenToYuan(row.fixedAmount) }}
            </template>
          </el-table-column>
          <el-table-column :formatter="dateFormatter" label="创建时间" prop="createTime" width="180" />
          <el-table-column :formatter="dateFormatter" label="更新时间" prop="updateTime" width="180" />
          <el-table-column align="center" fixed="right" label="操作" width="160">
            <template #default="{ row }">
              <el-button
                v-hasPermi="['booking:commission:config']"
                link
                type="primary"
                @click="openConfigDialog(row)"
              >
                编辑
              </el-button>
              <el-button
                v-hasPermi="['booking:commission:config']"
                link
                type="danger"
                @click="handleDeleteConfig(row)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </ContentWrap>

  <Dialog v-model="configDialogVisible" :title="configDialogTitle" width="520px">
    <el-form ref="configFormRef" :model="configForm" :rules="configRules" label-width="110px">
      <el-form-item label="门店ID" prop="storeId">
        <el-input-number v-model="configForm.storeId" :controls="false" :min="1" class="!w-220px" />
      </el-form-item>
      <el-form-item label="佣金类型" prop="commissionType">
        <el-select v-model="configForm.commissionType" class="!w-220px" placeholder="请选择">
          <el-option v-for="item in commissionTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="佣金比例" prop="rate">
        <el-input v-model="configForm.rate" class="!w-220px" maxlength="16" placeholder="例如 0.15" />
      </el-form-item>
      <el-form-item label="固定金额(分)" prop="fixedAmount">
        <el-input-number v-model="configForm.fixedAmount" :controls="false" :min="0" class="!w-220px" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="configDialogVisible = false">取消</el-button>
      <el-button :loading="configSubmitting" type="primary" @click="handleSaveConfig">保存</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { dateFormatter } from '@/utils/formatTime'
import * as CommissionApi from '@/api/mall/booking/commission'

type ReadbackAlertType = 'success' | 'warning' | 'info'

interface QueryParams {
  technicianId?: number
  orderId?: number
  storeId?: number
}

const message = ElMessage
const loading = ref(false)
const activeTab = ref('commission')
const commissionList = ref<CommissionApi.TechnicianCommission[]>([])
const configList = ref<CommissionApi.TechnicianCommissionConfig[]>([])
const pendingAmount = ref<number | null>(null)
const configDialogVisible = ref(false)
const configSubmitting = ref(false)
const configFormRef = ref<FormInstance>()
const queryParams = reactive<QueryParams>({
  technicianId: undefined,
  orderId: undefined,
  storeId: undefined
})
const readbackEvidence = reactive<{
  type: ReadbackAlertType
  title: string
  description: string
}>({
  type: 'info',
  title: '',
  description: ''
})
const configForm = reactive<CommissionApi.TechnicianCommissionConfigSaveReq>({
  id: undefined,
  storeId: undefined as unknown as number,
  commissionType: undefined as unknown as number,
  rate: '',
  fixedAmount: undefined
})

const commissionTypeOptions = [
  { label: '基础服务', value: 1 },
  { label: '点钟加成', value: 2 },
  { label: '加钟服务', value: 3 },
  { label: '卡项销售', value: 4 },
  { label: '商品推荐', value: 5 },
  { label: '好评奖励', value: 6 }
]

const configRules: FormRules = {
  storeId: [{ required: true, message: '请输入门店ID', trigger: 'blur' }],
  commissionType: [{ required: true, message: '请选择佣金类型', trigger: 'change' }],
  rate: [{ required: true, message: '请输入佣金比例', trigger: 'blur' }]
}

const hasRecordQueryCondition = computed(() => Boolean(queryParams.technicianId || queryParams.orderId))
const queryMode = computed(() => {
  if (queryParams.orderId) return 'ORDER'
  if (queryParams.technicianId) return 'TECHNICIAN'
  return 'NONE'
})
const queryModeLabel = computed(() => {
  if (queryMode.value === 'ORDER') return '按订单查询'
  if (queryMode.value === 'TECHNICIAN') return '按技师查询'
  return '未触发记录查询'
})
const pendingAmountText = computed(() => (pendingAmount.value == null ? '--' : fenToYuan(pendingAmount.value)))
const configDialogTitle = computed(() => (configForm.id ? '编辑佣金配置' : '新增佣金配置'))

const setReadbackEvidence = (type: ReadbackAlertType, title: string, description: string) => {
  readbackEvidence.type = type
  readbackEvidence.title = title
  readbackEvidence.description = description
}

const clearReadbackEvidence = () => {
  readbackEvidence.type = 'info'
  readbackEvidence.title = ''
  readbackEvidence.description = ''
}

const fenToYuan = (fen?: number) => {
  if (fen == null) return '--'
  return (Number(fen) / 100).toFixed(2)
}

const rateText = (rate?: string) => {
  return rate || '-'
}

const commissionTypeText = (commissionType?: number) => {
  return commissionTypeOptions.find((item) => item.value === commissionType)?.label || `类型${commissionType ?? '-'}`
}

const commissionStatusText = (status?: number) => {
  if (status === 0) return '待结算'
  if (status === 1) return '已结算'
  if (status === 2) return '已取消'
  return '-'
}

const commissionStatusTagType = (status?: number) => {
  if (status === 0) return 'warning'
  if (status === 1) return 'success'
  if (status === 2) return 'info'
  return 'info'
}

const normalizeRate = (rate?: string) => (rate || '').trim()

const buildConfigSignature = (item: Partial<CommissionApi.TechnicianCommissionConfigSaveReq>) => {
  return [
    item.storeId ?? '',
    item.commissionType ?? '',
    normalizeRate(item.rate),
    item.fixedAmount ?? ''
  ].join('|')
}

const countConfigSignature = (
  list: Array<Partial<CommissionApi.TechnicianCommissionConfig | CommissionApi.TechnicianCommissionConfigSaveReq>>,
  target: Partial<CommissionApi.TechnicianCommissionConfigSaveReq>
) => {
  const targetSignature = buildConfigSignature(target)
  return list.filter((item) => buildConfigSignature(item) === targetSignature).length
}

const loadCommissionData = async () => {
  if (!hasRecordQueryCondition.value) {
    commissionList.value = []
    pendingAmount.value = null
    return
  }
  if (queryMode.value === 'ORDER' && queryParams.orderId) {
    commissionList.value = await CommissionApi.getCommissionListByOrder(queryParams.orderId)
  } else if (queryParams.technicianId) {
    commissionList.value = await CommissionApi.getCommissionListByTechnician(queryParams.technicianId)
  }
  if (queryParams.technicianId) {
    pendingAmount.value = await CommissionApi.getPendingCommissionAmount(queryParams.technicianId)
  } else {
    pendingAmount.value = null
  }
}

const loadConfigData = async () => {
  if (!queryParams.storeId) {
    configList.value = []
    return
  }
  configList.value = await CommissionApi.getCommissionConfigList(queryParams.storeId)
}

const reloadCurrentState = async () => {
  await Promise.all([loadCommissionData(), loadConfigData()])
}

const handleQuery = async () => {
  loading.value = true
  try {
    clearReadbackEvidence()
    await reloadCurrentState()
  } finally {
    loading.value = false
  }
}

const resetQuery = async () => {
  queryParams.technicianId = undefined
  queryParams.orderId = undefined
  queryParams.storeId = undefined
  commissionList.value = []
  configList.value = []
  pendingAmount.value = null
  clearReadbackEvidence()
}

const handleSettle = async (row: CommissionApi.TechnicianCommission) => {
  const confirmed = await ElMessageBox.confirm(
    `将对佣金记录 ${row.id} 执行单条直结，执行后会立即写后回读确认。`,
    '确认单条直结',
    { type: 'warning' }
  )
    .then(() => true)
    .catch(() => false)
  if (!confirmed) return
  const beforeStatus = row.status
  const beforeSettlementTime = row.settlementTime
  const beforePendingAmount = pendingAmount.value

  await CommissionApi.settleCommission(row.id)
  await reloadCurrentState()

  const updated = commissionList.value.find((item) => item.id === row.id)
  const changed =
    (beforeStatus === 0 && updated?.status === 1) ||
    (!!updated?.settlementTime && updated.settlementTime !== beforeSettlementTime) ||
    (beforePendingAmount != null && pendingAmount.value != null && pendingAmount.value < beforePendingAmount)

  if (changed) {
    setReadbackEvidence('success', '单条直结写后回读成功', `佣金ID ${row.id} 已在回读结果中观察到状态变化。`)
    message.success('单条直结已完成写后回读确认')
    return
  }
  setReadbackEvidence(
    'warning',
    '接口返回成功但读后未变',
    `佣金ID ${row.id} 的直结请求返回 success(true)，但回读结果未观察到状态变化，请按 BO-004 no-op 风险处理。`
  )
  message.warning('接口返回成功但读后未变')
}

const handleBatchSettle = async () => {
  if (!queryParams.technicianId) {
    message.warning('请先输入技师ID再执行批量直结')
    return
  }
  const confirmed = await ElMessageBox.confirm(
    `将对技师 ${queryParams.technicianId} 的待结算佣金执行批量直结，执行后会立即写后回读确认。`,
    '确认批量直结',
    { type: 'warning' }
  )
    .then(() => true)
    .catch(() => false)
  if (!confirmed) return
  const beforePendingCount = commissionList.value.filter((item) => item.status === 0).length
  const beforePendingAmount = pendingAmount.value

  await CommissionApi.batchSettleCommission(queryParams.technicianId)
  await reloadCurrentState()

  const afterPendingCount = commissionList.value.filter((item) => item.status === 0).length
  const afterPendingAmount = pendingAmount.value
  const changed =
    afterPendingCount < beforePendingCount ||
    (beforePendingAmount != null && afterPendingAmount != null && afterPendingAmount < beforePendingAmount)

  if (changed) {
    setReadbackEvidence(
      'success',
      '批量直结写后回读成功',
      `技师 ${queryParams.technicianId} 的待结算数量或待结算金额已在回读结果中发生变化。`
    )
    message.success('批量直结已完成写后回读确认')
    return
  }
  setReadbackEvidence(
    'warning',
    '接口返回成功但读后未变',
    `技师 ${queryParams.technicianId} 的批量直结请求返回 success(true)，但回读结果未观察到待结算数量或金额变化，请按 BO-004 no-op 风险处理。`
  )
  message.warning('接口返回成功但读后未变')
}

const resetConfigForm = () => {
  configForm.id = undefined
  configForm.storeId = queryParams.storeId as number
  configForm.commissionType = undefined as unknown as number
  configForm.rate = ''
  configForm.fixedAmount = undefined
}

const openConfigDialog = (row?: CommissionApi.TechnicianCommissionConfig) => {
  if (row) {
    configForm.id = row.id
    configForm.storeId = Number(row.storeId)
    configForm.commissionType = Number(row.commissionType)
    configForm.rate = row.rate || ''
    configForm.fixedAmount = row.fixedAmount
  } else {
    resetConfigForm()
  }
  configDialogVisible.value = true
}

const handleSaveConfig = async () => {
  if (!configFormRef.value) return
  await configFormRef.value.validate()
  const payload = {
    id: configForm.id,
    storeId: Number(configForm.storeId),
    commissionType: Number(configForm.commissionType),
    rate: normalizeRate(configForm.rate),
    fixedAmount: configForm.fixedAmount
  }
  const beforeList = [...configList.value]

  configSubmitting.value = true
  try {
    await CommissionApi.saveCommissionConfig(payload)
    await loadConfigData()
    configDialogVisible.value = false

    let changed = false
    if (payload.id) {
      const updated = configList.value.find((item) => item.id === payload.id)
      changed = Boolean(
        updated &&
          Number(updated.storeId) === payload.storeId &&
          Number(updated.commissionType) === payload.commissionType &&
          normalizeRate(updated.rate) === payload.rate &&
          Number(updated.fixedAmount || 0) === Number(payload.fixedAmount || 0)
      )
    } else {
      changed = countConfigSignature(configList.value, payload) > countConfigSignature(beforeList, payload)
    }

    if (changed) {
      setReadbackEvidence(
        'success',
        '佣金配置写后回读成功',
        `${payload.id ? '配置更新' : '配置新增'}已在回读列表中观察到变化。`
      )
      message.success('佣金配置已完成写后回读确认')
      return
    }
    setReadbackEvidence(
      'warning',
      '接口返回成功但读后未变',
      `佣金配置保存请求返回 success(true)，但回读列表未观察到变化，请按 BO-004 no-op 风险处理。`
    )
    message.warning('接口返回成功但读后未变')
  } finally {
    configSubmitting.value = false
  }
}

const handleDeleteConfig = async (row: CommissionApi.TechnicianCommissionConfig) => {
  const confirmed = await ElMessageBox.confirm(`将删除配置 ${row.id}，执行后会立即写后回读确认。`, '确认删除', {
    type: 'warning'
  })
    .then(() => true)
    .catch(() => false)
  if (!confirmed) return
  await CommissionApi.deleteCommissionConfig(row.id)
  await loadConfigData()

  const stillExists = configList.value.some((item) => item.id === row.id)
  if (!stillExists) {
    setReadbackEvidence('success', '删除配置写后回读成功', `配置 ${row.id} 已在回读列表中消失。`)
    message.success('删除配置已完成写后回读确认')
    return
  }
  setReadbackEvidence(
    'warning',
    '接口返回成功但读后未变',
    `删除配置 ${row.id} 的请求返回 success(true)，但回读列表仍然存在该配置，请按 BO-004 no-op 风险处理。`
  )
  message.warning('接口返回成功但读后未变')
}

resetConfigForm()
</script>
