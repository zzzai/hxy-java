<template>
  <ContentWrap>
    <el-form :inline="true" :model="queryParams" class="-mb-15px" label-width="110px">
      <el-form-item label="调整单号" prop="orderNo">
        <el-input
          v-model="queryParams.orderNo"
          class="!w-220px"
          clearable
          placeholder="请输入调整单号"
          @keyup.enter="handleQuery"
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
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" class="!w-170px" clearable placeholder="请选择状态">
          <el-option :value="0" label="草稿" />
          <el-option :value="10" label="待审批" />
          <el-option :value="20" label="已通过" />
          <el-option :value="30" label="已驳回" />
          <el-option :value="40" label="已取消" />
        </el-select>
      </el-form-item>
      <el-form-item label="业务类型" prop="bizType">
        <el-select v-model="queryParams.bizType" class="!w-210px" clearable placeholder="请选择业务类型">
          <el-option v-for="item in bizTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="申请人" prop="applyOperator">
        <el-input
          v-model="queryParams.applyOperator"
          class="!w-180px"
          clearable
          placeholder="请输入申请人"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="最近动作编码" prop="lastActionCode">
        <el-input
          v-model="queryParams.lastActionCode"
          class="!w-190px"
          clearable
          placeholder="请输入动作编码"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="最近动作人" prop="lastActionOperator">
        <el-input
          v-model="queryParams.lastActionOperator"
          class="!w-180px"
          clearable
          placeholder="请输入动作人"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="申请时间" prop="createTime">
        <el-date-picker
          v-model="queryParams.createTime"
          :default-time="[new Date('1 00:00:00'), new Date('1 23:59:59')]"
          class="!w-340px"
          end-placeholder="结束时间"
          start-placeholder="开始时间"
          type="datetimerange"
          value-format="YYYY-MM-DD HH:mm:ss"
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
        <el-button plain type="primary" @click="openCreateDialog">
          <Icon class="mr-5px" icon="ep:plus" />
          新建调整单
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="ID" prop="id" width="88" />
      <el-table-column label="调整单号" min-width="240" show-overflow-tooltip>
        <template #default="{ row }">
          {{ textOrDash(row.orderNo) }}
        </template>
      </el-table-column>
      <el-table-column label="门店" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">
          {{ storeDisplay(row) }}
        </template>
      </el-table-column>
      <el-table-column label="业务类型" min-width="170">
        <template #default="{ row }">
          {{ bizTypeText(row.bizType) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">
            {{ statusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="申请人" min-width="130" show-overflow-tooltip>
        <template #default="{ row }">
          {{ textOrDash(row.applyOperator) }}
        </template>
      </el-table-column>
      <el-table-column label="申请时间" width="180">
        <template #default="{ row }">
          {{ textOrDash(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="最近动作" min-width="260" show-overflow-tooltip>
        <template #default="{ row }">
          {{ lastActionSummary(row) }}
        </template>
      </el-table-column>
      <el-table-column align="center" fixed="right" label="操作" width="320">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetailDrawer(row)">查看详情</el-button>
          <el-button
            v-if="canSubmit(row.status)"
            :loading="isActionLoading(row.id, 'submit')"
            link
            type="success"
            @click="handleSubmit(row)"
          >
            提交
          </el-button>
          <el-button v-if="canApprove(row.status)" link type="success" @click="openActionDialog(row, 'approve')">
            审批通过
          </el-button>
          <el-button v-if="canReject(row.status)" link type="danger" @click="openActionDialog(row, 'reject')">
            驳回
          </el-button>
          <el-button v-if="canCancel(row.status)" link type="info" @click="openActionDialog(row, 'cancel')">
            取消
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <Pagination
      v-model:limit="queryParams.pageSize"
      v-model:page="queryParams.pageNo"
      :total="total"
      @pagination="getList"
    />
  </ContentWrap>

  <el-dialog v-model="createDialogVisible" title="新建库存调整单" width="760px">
    <el-form :model="createForm" label-width="110px">
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="门店ID" required>
            <el-input-number v-model="createForm.storeId" :controls="false" :min="1" class="!w-full" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="业务类型" required>
            <el-select v-model="createForm.bizType" class="!w-full" placeholder="请选择业务类型">
              <el-option v-for="item in bizTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="原因" required>
        <el-input v-model="createForm.reason" :rows="2" maxlength="255" show-word-limit type="textarea" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="createForm.remark" :rows="2" maxlength="255" show-word-limit type="textarea" />
      </el-form-item>
      <el-form-item label="来源">
        <el-input v-model="createForm.applySource" maxlength="32" />
      </el-form-item>
      <el-form-item label="调整明细" required>
        <div class="w-full">
          <div class="mb-8px flex items-center justify-between">
            <div class="text-[var(--el-text-color-secondary)]">SKU ID + 库存变化值（正增负减，禁止 0）</div>
            <el-button link type="primary" @click="addCreateItem">新增一行</el-button>
          </div>
          <el-table :data="createForm.items" border>
            <el-table-column label="SKU ID" min-width="220">
              <template #default="{ row }">
                <el-input-number v-model="row.skuId" :controls="false" :min="1" class="!w-full" />
              </template>
            </el-table-column>
            <el-table-column label="库存变化" min-width="220">
              <template #default="{ row }">
                <el-input-number v-model="row.incrCount" :controls="false" class="!w-full" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90" align="center">
              <template #default="{ $index }">
                <el-button :disabled="createForm.items.length <= 1" link type="danger" @click="removeCreateItem($index)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="createDialogVisible = false">取消</el-button>
      <el-button :loading="createLoading" type="primary" @click="handleCreate">创建</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="actionDialogVisible" :title="actionDialogTitle" width="520px">
    <el-form :model="actionForm" label-width="110px">
      <el-form-item label="调整单号">
        <span>{{ textOrDash(actionTargetOrderNo) }}</span>
      </el-form-item>
      <el-form-item :label="actionRemarkLabel" :required="actionRemarkRequired">
        <el-input
          v-model="actionForm.remark"
          :rows="3"
          :placeholder="actionRemarkPlaceholder"
          maxlength="255"
          show-word-limit
          type="textarea"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="actionDialogVisible = false">取消</el-button>
      <el-button :loading="actionLoading" type="primary" @click="handleAction">确认</el-button>
    </template>
  </el-dialog>

  <el-drawer v-model="detailDrawerVisible" size="62%" title="库存调整单详情">
    <div v-loading="detailLoading">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="ID">{{ numberOrDash(detailData.id) }}</el-descriptions-item>
        <el-descriptions-item label="调整单号">{{ textOrDash(detailData.orderNo) }}</el-descriptions-item>
        <el-descriptions-item label="门店">{{ storeDisplay(detailData) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(detailData.status)">
            {{ statusText(detailData.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="业务类型">{{ bizTypeText(detailData.bizType) }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ textOrDash(detailData.applyOperator) }}</el-descriptions-item>
        <el-descriptions-item label="申请来源">{{ textOrDash(detailData.applySource) }}</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ textOrDash(detailData.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="审批人">{{ textOrDash(detailData.approveOperator) }}</el-descriptions-item>
        <el-descriptions-item label="审批时间">{{ textOrDash(detailData.approveTime) }}</el-descriptions-item>
        <el-descriptions-item :span="2" label="审批备注">{{ textOrDash(detailData.approveRemark) }}</el-descriptions-item>
        <el-descriptions-item :span="2" label="原因">{{ textOrDash(detailData.reason) }}</el-descriptions-item>
        <el-descriptions-item :span="2" label="备注">{{ textOrDash(detailData.remark) }}</el-descriptions-item>
        <el-descriptions-item label="最近动作编码">{{ textOrDash(detailData.lastActionCode) }}</el-descriptions-item>
        <el-descriptions-item label="最近动作人">{{ textOrDash(detailData.lastActionOperator) }}</el-descriptions-item>
        <el-descriptions-item label="最近动作时间">{{ textOrDash(detailData.lastActionTime) }}</el-descriptions-item>
      </el-descriptions>

      <div class="mt-16px">
        <div class="mb-8px font-500">detailJson（结构化）</div>
        <el-alert v-if="detailParseFailed" :closable="false" title="明细解析失败（原文保留）" type="warning" />
        <el-empty v-else-if="!detailRows.length" description="无可用明细" />
        <el-table v-else :data="detailRows" border max-height="320">
          <el-table-column label="SKU ID" min-width="180">
            <template #default="{ row }">
              {{ numberOrDash(row.skuId) }}
            </template>
          </el-table-column>
          <el-table-column label="库存变更" min-width="140">
            <template #default="{ row }">
              <el-tag :type="incrTagType(row.incrCount)">{{ incrText(row.incrCount) }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <el-input :model-value="detailRaw || EMPTY_TEXT" :rows="5" class="mt-8px" readonly type="textarea" />
      </div>
    </div>
  </el-drawer>
</template>

<script lang="ts" setup>
import * as StoreSkuStockAdjustOrderApi from '@/api/mall/product/storeSkuStockAdjustOrder'

defineOptions({ name: 'MallStoreSkuStockAdjustOrderIndex' })

type ActionType = 'approve' | 'reject' | 'cancel' | 'submit'

const EMPTY_TEXT = '--'

const message = useMessage()

const loading = ref(false)
const total = ref(0)
const list = ref<StoreSkuStockAdjustOrderApi.StoreSkuStockAdjustOrderItem[]>([])

const queryParams = reactive<StoreSkuStockAdjustOrderApi.StoreSkuStockAdjustOrderPageReq>({
  pageNo: 1,
  pageSize: 10,
  orderNo: undefined,
  storeId: undefined,
  status: undefined,
  bizType: undefined,
  applyOperator: undefined,
  lastActionCode: undefined,
  lastActionOperator: undefined,
  createTime: undefined
})

const createDialogVisible = ref(false)
const createLoading = ref(false)
const createForm = reactive<StoreSkuStockAdjustOrderApi.StoreSkuStockAdjustOrderCreateReq>({
  storeId: undefined as unknown as number,
  bizType: '',
  reason: '',
  remark: '',
  applySource: 'ADMIN_UI',
  items: [{ skuId: undefined as unknown as number, incrCount: undefined as unknown as number }]
})

const detailDrawerVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref<Partial<StoreSkuStockAdjustOrderApi.StoreSkuStockAdjustOrderItem>>({})
const detailRows = ref<StoreSkuStockAdjustOrderApi.StoreSkuStockAdjustOrderDetailItem[]>([])
const detailParseFailed = ref(false)
const detailRaw = ref('')

const actionDialogVisible = ref(false)
const actionLoading = ref(false)
const actionType = ref<ActionType>('approve')
const actionTarget = ref<StoreSkuStockAdjustOrderApi.StoreSkuStockAdjustOrderItem | null>(null)
const actionForm = reactive({
  remark: ''
})
const rowActionLoadingId = ref<number>()
const rowActionLoadingType = ref<ActionType>()

const bizTypeOptions = [
  { label: '补货入库', value: 'REPLENISH_IN' },
  { label: '调拨入库', value: 'TRANSFER_IN' },
  { label: '调拨出库', value: 'TRANSFER_OUT' },
  { label: '盘点调整', value: 'STOCKTAKE' },
  { label: '损耗扣减', value: 'LOSS' },
  { label: '报废扣减', value: 'SCRAP' }
]

const actionDialogTitle = computed(() => {
  if (actionType.value === 'approve') return '审批通过'
  if (actionType.value === 'reject') return '驳回调整单'
  if (actionType.value === 'cancel') return '取消调整单'
  return '提交调整单'
})

const actionRemarkLabel = computed(() => {
  if (actionType.value === 'approve') return '审批备注'
  if (actionType.value === 'reject') return '驳回原因'
  if (actionType.value === 'cancel') return '取消原因'
  return '备注'
})

const actionRemarkPlaceholder = computed(() => {
  if (actionType.value === 'approve') return '请输入审批备注（可选）'
  if (actionType.value === 'reject') return '请输入驳回原因（必填）'
  if (actionType.value === 'cancel') return '请输入取消原因（可选）'
  return '请输入备注'
})

const actionRemarkRequired = computed(() => actionType.value === 'reject')

const actionTargetOrderNo = computed(() => actionTarget.value?.orderNo || EMPTY_TEXT)

const textOrDash = (value: any) => {
  if (value === undefined || value === null) {
    return EMPTY_TEXT
  }
  const text = String(value).trim()
  return text || EMPTY_TEXT
}

const numberOrDash = (value: any) => {
  if (value === undefined || value === null || value === '') {
    return EMPTY_TEXT
  }
  const parsed = Number(value)
  return Number.isFinite(parsed) ? String(parsed) : EMPTY_TEXT
}

const parseNumber = (value: any): number | undefined => {
  if (value === undefined || value === null || value === '') {
    return undefined
  }
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : undefined
}

const normalizeBizType = (bizType?: string) => {
  return String(bizType || '').trim().toUpperCase()
}

const parseStatus = (status: any): number | undefined => {
  const parsed = Number(status)
  return Number.isFinite(parsed) ? parsed : undefined
}

const statusText = (status?: number) => {
  if (status === 0) return '草稿'
  if (status === 10) return '待审批'
  if (status === 20) return '已通过'
  if (status === 30) return '已驳回'
  if (status === 40) return '已取消'
  return status === undefined || status === null ? EMPTY_TEXT : `UNKNOWN(${status})`
}

const statusTagType = (status?: number) => {
  if (status === 0) return 'info'
  if (status === 10) return 'warning'
  if (status === 20) return 'success'
  if (status === 30) return 'danger'
  if (status === 40) return 'info'
  return 'info'
}

const bizTypeText = (bizType?: string) => {
  const normalized = normalizeBizType(bizType)
  if (!normalized) {
    return EMPTY_TEXT
  }
  const matched = bizTypeOptions.find((item) => item.value === normalized)
  return matched ? matched.label : normalized
}

const storeDisplay = (row: Partial<StoreSkuStockAdjustOrderApi.StoreSkuStockAdjustOrderItem>) => {
  const name = textOrDash(row.storeName)
  const id = numberOrDash(row.storeId)
  if (name === EMPTY_TEXT && id === EMPTY_TEXT) {
    return EMPTY_TEXT
  }
  return `${name}（#${id}）`
}

const lastActionSummary = (row: StoreSkuStockAdjustOrderApi.StoreSkuStockAdjustOrderItem) => {
  const code = textOrDash(row.lastActionCode)
  const operator = textOrDash(row.lastActionOperator)
  const time = textOrDash(row.lastActionTime)
  if (code === EMPTY_TEXT && operator === EMPTY_TEXT && time === EMPTY_TEXT) {
    return EMPTY_TEXT
  }
  return `${code} / ${operator} / ${time}`
}

const canSubmit = (status?: number) => parseStatus(status) === 0
const canApprove = (status?: number) => parseStatus(status) === 10
const canReject = (status?: number) => parseStatus(status) === 10
const canCancel = (status?: number) => {
  const normalized = parseStatus(status)
  return normalized === 0 || normalized === 10
}

const isActionLoading = (id?: number, type?: ActionType) => {
  return rowActionLoadingId.value === id && rowActionLoadingType.value === type
}

const incrText = (incrCount?: number) => {
  const normalized = parseNumber(incrCount)
  if (normalized === undefined) {
    return EMPTY_TEXT
  }
  return normalized > 0 ? `+${normalized}` : `${normalized}`
}

const incrTagType = (incrCount?: number) => {
  const normalized = parseNumber(incrCount)
  if (normalized === undefined) return 'info'
  if (normalized > 0) return 'success'
  if (normalized < 0) return 'danger'
  return 'warning'
}

const resetCreateForm = () => {
  createForm.storeId = undefined as unknown as number
  createForm.bizType = ''
  createForm.reason = ''
  createForm.remark = ''
  createForm.applySource = 'ADMIN_UI'
  createForm.items = [{ skuId: undefined as unknown as number, incrCount: undefined as unknown as number }]
}

const addCreateItem = () => {
  createForm.items.push({ skuId: undefined as unknown as number, incrCount: undefined as unknown as number })
}

const removeCreateItem = (index: number) => {
  if (createForm.items.length <= 1) {
    return
  }
  createForm.items.splice(index, 1)
}

const normalizeDetailRows = (payload: any) => {
  if (Array.isArray(payload)) {
    return payload.filter((item) => item && typeof item === 'object')
  }
  if (!payload || typeof payload !== 'object') {
    return []
  }
  const objectPayload = payload as Record<string, any>
  const candidates = [objectPayload.items, objectPayload.details, objectPayload.detailList, objectPayload.records]
  for (const candidate of candidates) {
    if (Array.isArray(candidate)) {
      return candidate.filter((item) => item && typeof item === 'object')
    }
  }
  return []
}

const parseDetailJson = (rawJson?: string) => {
  detailRaw.value = String(rawJson || '')
  detailParseFailed.value = false
  detailRows.value = []

  const raw = detailRaw.value.trim()
  if (!raw) {
    return
  }
  try {
    const parsed = JSON.parse(raw)
    detailRows.value = normalizeDetailRows(parsed).map((item: Record<string, any>) => ({
      skuId: parseNumber(item.skuId),
      incrCount: parseNumber(item.incrCount)
    }))
  } catch {
    detailParseFailed.value = true
  }
}

const normalizeQuery = () => {
  queryParams.orderNo = String(queryParams.orderNo || '').trim() || undefined
  queryParams.bizType = normalizeBizType(queryParams.bizType) || undefined
  queryParams.applyOperator = String(queryParams.applyOperator || '').trim() || undefined
  queryParams.lastActionCode = String(queryParams.lastActionCode || '').trim().toUpperCase() || undefined
  queryParams.lastActionOperator = String(queryParams.lastActionOperator || '').trim() || undefined
}

const getList = async () => {
  loading.value = true
  try {
    normalizeQuery()
    const data = await StoreSkuStockAdjustOrderApi.getStoreSkuStockAdjustOrderPage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
  } catch (error: any) {
    list.value = []
    total.value = 0
    message.error(error?.msg || '库存调整单列表加载失败')
  } finally {
    loading.value = false
  }
}

const handleQuery = async () => {
  queryParams.pageNo = 1
  await getList()
}

const resetQuery = async () => {
  queryParams.pageNo = 1
  queryParams.pageSize = 10
  queryParams.orderNo = undefined
  queryParams.storeId = undefined
  queryParams.status = undefined
  queryParams.bizType = undefined
  queryParams.applyOperator = undefined
  queryParams.lastActionCode = undefined
  queryParams.lastActionOperator = undefined
  queryParams.createTime = undefined
  await getList()
}

const openCreateDialog = () => {
  resetCreateForm()
  createDialogVisible.value = true
}

const handleCreate = async () => {
  if (!createForm.storeId) {
    message.warning('请输入门店ID')
    return
  }
  const bizType = normalizeBizType(createForm.bizType)
  if (!bizType) {
    message.warning('请选择业务类型')
    return
  }
  const reason = String(createForm.reason || '').trim()
  if (!reason) {
    message.warning('请输入原因')
    return
  }
  const normalizedItems = (createForm.items || [])
    .map((item) => ({ skuId: parseNumber(item.skuId), incrCount: parseNumber(item.incrCount) }))
    .filter((item) => item.skuId !== undefined && item.incrCount !== undefined)
  if (!normalizedItems.length) {
    message.warning('请至少填写一条有效明细')
    return
  }
  if (normalizedItems.some((item) => item.incrCount === 0)) {
    message.warning('库存变化值不能为 0')
    return
  }

  createLoading.value = true
  try {
    const id = await StoreSkuStockAdjustOrderApi.createStoreSkuStockAdjustOrder({
      storeId: Number(createForm.storeId),
      bizType,
      reason,
      remark: String(createForm.remark || '').trim() || undefined,
      applySource: String(createForm.applySource || '').trim() || 'ADMIN_UI',
      items: normalizedItems as StoreSkuStockAdjustOrderApi.StoreSkuStockAdjustOrderCreateItem[]
    })
    message.success(`调整单创建成功，ID：${id || EMPTY_TEXT}`)
    createDialogVisible.value = false
    await getList()
  } catch (error: any) {
    message.error(error?.msg || '调整单创建失败')
  } finally {
    createLoading.value = false
  }
}

const openDetailDrawer = async (row: StoreSkuStockAdjustOrderApi.StoreSkuStockAdjustOrderItem) => {
  detailDrawerVisible.value = true
  detailLoading.value = true
  detailData.value = {}
  parseDetailJson(undefined)

  try {
    const data = await StoreSkuStockAdjustOrderApi.getStoreSkuStockAdjustOrder(row.id)
    detailData.value = data || {}
    parseDetailJson(data?.detailJson)
  } catch (error: any) {
    detailData.value = {
      id: row.id,
      orderNo: row.orderNo,
      storeId: row.storeId,
      storeName: row.storeName,
      status: row.status
    }
    message.error(error?.msg || '库存调整单详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

const handleSubmit = async (row: StoreSkuStockAdjustOrderApi.StoreSkuStockAdjustOrderItem) => {
  if (!row?.id) {
    message.warning('调整单ID为空，无法提交')
    return
  }
  try {
    await message.confirm(`确认提交调整单 ${textOrDash(row.orderNo)} 吗？提交后将进入审批流程。`)
  } catch {
    return
  }

  rowActionLoadingId.value = row.id
  rowActionLoadingType.value = 'submit'
  try {
    await StoreSkuStockAdjustOrderApi.submitStoreSkuStockAdjustOrder({ id: row.id })
    message.success('调整单提交成功')
    await getList()
  } catch (error: any) {
    message.error(error?.msg || '调整单提交失败，请稍后重试')
  } finally {
    rowActionLoadingId.value = undefined
    rowActionLoadingType.value = undefined
  }
}

const openActionDialog = (row: StoreSkuStockAdjustOrderApi.StoreSkuStockAdjustOrderItem, type: ActionType) => {
  actionType.value = type
  actionTarget.value = row
  actionForm.remark = ''
  actionDialogVisible.value = true
}

const handleAction = async () => {
  const target = actionTarget.value
  if (!target?.id) {
    message.warning('调整单ID为空，无法执行操作')
    return
  }
  const remark = String(actionForm.remark || '').trim()
  if (actionType.value === 'reject' && !remark) {
    message.warning('请输入驳回原因')
    return
  }

  const actionName =
    actionType.value === 'approve'
      ? '审批通过'
      : actionType.value === 'reject'
        ? '驳回'
        : actionType.value === 'cancel'
          ? '取消'
          : '提交'
  try {
    await message.confirm(`确认${actionName}调整单 ${textOrDash(target.orderNo)} 吗？`)
  } catch {
    return
  }

  actionLoading.value = true
  rowActionLoadingId.value = target.id
  rowActionLoadingType.value = actionType.value
  try {
    if (actionType.value === 'approve') {
      await StoreSkuStockAdjustOrderApi.approveStoreSkuStockAdjustOrder({ id: target.id, remark })
    } else if (actionType.value === 'reject') {
      await StoreSkuStockAdjustOrderApi.rejectStoreSkuStockAdjustOrder({ id: target.id, remark })
    } else if (actionType.value === 'cancel') {
      await StoreSkuStockAdjustOrderApi.cancelStoreSkuStockAdjustOrder({ id: target.id, remark })
    }
    message.success(`${actionName}成功`)
    actionDialogVisible.value = false
    await getList()
  } catch (error: any) {
    message.error(error?.msg || `${actionName}失败，请稍后重试`)
  } finally {
    actionLoading.value = false
    rowActionLoadingId.value = undefined
    rowActionLoadingType.value = undefined
  }
}

onMounted(async () => {
  await getList()
})
</script>
