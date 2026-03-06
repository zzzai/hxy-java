<template>
  <ContentWrap>
    <el-form :inline="true" :model="queryParams" class="-mb-15px" label-width="104px">
      <el-form-item label="调拨单号" prop="orderNo">
        <el-input
          v-model="queryParams.orderNo"
          class="!w-220px"
          clearable
          placeholder="请输入调拨单号"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="调出门店ID" prop="outStoreId">
        <el-input-number
          v-model="queryParams.outStoreId"
          :controls="false"
          :min="1"
          class="!w-170px"
          placeholder="请输入调出门店ID"
        />
      </el-form-item>
      <el-form-item label="调入门店ID" prop="inStoreId">
        <el-input-number
          v-model="queryParams.inStoreId"
          :controls="false"
          :min="1"
          class="!w-170px"
          placeholder="请输入调入门店ID"
        />
      </el-form-item>
      <el-form-item label="SKU ID" prop="skuId">
        <el-input-number
          v-model="queryParams.skuId"
          :controls="false"
          :min="1"
          class="!w-160px"
          placeholder="请输入SKU ID"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" class="!w-150px" clearable placeholder="请选择状态">
          <el-option :value="0" label="草稿" />
          <el-option :value="10" label="待审批" />
          <el-option :value="20" label="已通过" />
          <el-option :value="30" label="已驳回" />
          <el-option :value="40" label="已取消" />
        </el-select>
      </el-form-item>
      <el-form-item label="业务类型" prop="bizType">
        <el-input
          v-model="queryParams.bizType"
          class="!w-170px"
          clearable
          placeholder="例如 STORE_TRANSFER"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="申请人" prop="applyOperator">
        <el-input
          v-model="queryParams.applyOperator"
          class="!w-170px"
          clearable
          placeholder="请输入申请人"
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
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-alert
      v-if="!transferApiReady"
      :closable="false"
      :description="transferApiNotReadyReason || '当前环境未发布 transfer-order 接口，请等待窗口A联调完成。'"
      title="调拨审批接口未就绪"
      type="warning"
      class="mb-12px"
    />

    <el-table v-loading="loading" :data="list">
      <el-table-column label="ID" prop="id" width="88" />
      <el-table-column label="调拨单号" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">
          {{ textOrDash(row.orderNo) }}
        </template>
      </el-table-column>
      <el-table-column label="调拨方向" min-width="260" show-overflow-tooltip>
        <template #default="{ row }">
          {{ transferStoreDisplay(row) }}
        </template>
      </el-table-column>
      <el-table-column label="业务类型" min-width="150">
        <template #default="{ row }">
          {{ textOrDash(row.bizType) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">
            {{ statusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="申请人" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">
          {{ textOrDash(row.applyOperator) }}
        </template>
      </el-table-column>
      <el-table-column label="申请时间" width="180">
        <template #default="{ row }">
          {{ textOrDash(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="最近动作" min-width="230" show-overflow-tooltip>
        <template #default="{ row }">
          {{ lastActionSummary(row) }}
        </template>
      </el-table-column>
      <el-table-column align="center" fixed="right" label="操作" width="340">
        <template #default="{ row }">
          <el-button
            :disabled="!transferApiReady"
            :title="!transferApiReady ? '接口未就绪' : ''"
            link
            type="primary"
            @click="openDetailDrawer(row)"
          >
            查看详情
          </el-button>
          <el-button
            v-if="canSubmit(row.status)"
            :disabled="!transferApiReady"
            :loading="isActionLoading(row.id, 'submit')"
            :title="!transferApiReady ? '接口未就绪' : ''"
            link
            type="success"
            @click="handleSubmit(row)"
          >
            提交
          </el-button>
          <el-button
            v-if="canApprove(row.status)"
            :disabled="!transferApiReady"
            :title="!transferApiReady ? '接口未就绪' : ''"
            link
            type="success"
            @click="openActionDialog(row, 'approve')"
          >
            审批通过
          </el-button>
          <el-button
            v-if="canReject(row.status)"
            :disabled="!transferApiReady"
            :title="!transferApiReady ? '接口未就绪' : ''"
            link
            type="danger"
            @click="openActionDialog(row, 'reject')"
          >
            驳回
          </el-button>
          <el-button
            v-if="canCancel(row.status)"
            :disabled="!transferApiReady"
            :title="!transferApiReady ? '接口未就绪' : ''"
            link
            type="info"
            @click="openActionDialog(row, 'cancel')"
          >
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

  <el-dialog v-model="actionDialogVisible" :title="actionDialogTitle" width="520px">
    <el-form :model="actionForm" label-width="110px">
      <el-form-item label="调拨单号">
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
      <el-button :disabled="!transferApiReady" :loading="actionLoading" type="primary" @click="handleAction">
        确认
      </el-button>
    </template>
  </el-dialog>

  <el-drawer v-model="detailDrawerVisible" size="62%" title="跨店调拨单详情">
    <div v-loading="detailLoading">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="ID">{{ numberOrDash(detailData.id) }}</el-descriptions-item>
        <el-descriptions-item label="调拨单号">{{ textOrDash(detailData.orderNo) }}</el-descriptions-item>
        <el-descriptions-item label="调出门店">{{ storeDisplay(detailData.outStoreName, detailData.outStoreId) }}</el-descriptions-item>
        <el-descriptions-item label="调入门店">{{ storeDisplay(detailData.inStoreName, detailData.inStoreId) }}</el-descriptions-item>
        <el-descriptions-item label="业务类型">{{ textOrDash(detailData.bizType) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(detailData.status)">
            {{ statusText(detailData.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="申请人">{{ textOrDash(detailData.applyOperator) }}</el-descriptions-item>
        <el-descriptions-item label="申请来源">{{ textOrDash(detailData.applySource) }}</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ textOrDash(detailData.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="审批人">{{ textOrDash(detailData.approveOperator) }}</el-descriptions-item>
        <el-descriptions-item label="审批时间">{{ textOrDash(detailData.approveTime) }}</el-descriptions-item>
        <el-descriptions-item label="审批备注" :span="2">{{ textOrDash(detailData.approveRemark) }}</el-descriptions-item>
        <el-descriptions-item label="原因" :span="2">{{ textOrDash(detailData.reason) }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ textOrDash(detailData.remark) }}</el-descriptions-item>
        <el-descriptions-item label="最近动作编码">{{ textOrDash(detailData.lastActionCode) }}</el-descriptions-item>
        <el-descriptions-item label="最近动作人">{{ textOrDash(detailData.lastActionOperator) }}</el-descriptions-item>
        <el-descriptions-item label="最近动作时间">{{ textOrDash(detailData.lastActionTime) }}</el-descriptions-item>
      </el-descriptions>

      <div class="mt-16px">
        <div class="mb-8px font-500">detailJson（结构化）</div>
        <el-alert v-if="detailParseFailed" :closable="false" title="明细解析失败（原文保留）" type="warning" />
        <el-empty v-else-if="!detailRows.length" description="无可用明细" />
        <el-table v-else :data="detailRows" border max-height="340">
          <el-table-column label="SKU ID" min-width="150">
            <template #default="{ row }">
              {{ numberOrDash(row.skuId) }}
            </template>
          </el-table-column>
          <el-table-column label="调出SKU ID" min-width="150">
            <template #default="{ row }">
              {{ numberOrDash(row.outSkuId) }}
            </template>
          </el-table-column>
          <el-table-column label="调入SKU ID" min-width="150">
            <template #default="{ row }">
              {{ numberOrDash(row.inSkuId) }}
            </template>
          </el-table-column>
          <el-table-column label="调拨数量" min-width="120">
            <template #default="{ row }">
              {{ transferCountText(row) }}
            </template>
          </el-table-column>
        </el-table>
        <el-input :model-value="detailRaw || EMPTY_TEXT" :rows="5" class="mt-8px" readonly type="textarea" />
      </div>
    </div>
  </el-drawer>
</template>

<script lang="ts" setup>
import * as StoreSkuTransferOrderApi from '@/api/mall/product/storeSkuTransferOrder'

defineOptions({ name: 'MallStoreSkuTransferOrderIndex' })

type ActionType = 'approve' | 'reject' | 'cancel' | 'submit'

const EMPTY_TEXT = '--'

const message = useMessage()

const loading = ref(false)
const total = ref(0)
const list = ref<StoreSkuTransferOrderApi.StoreSkuTransferOrderItem[]>([])

const transferApiReady = ref(true)
const transferApiNotReadyReason = ref('')

const queryParams = reactive<StoreSkuTransferOrderApi.StoreSkuTransferOrderPageReq>({
  pageNo: 1,
  pageSize: 10,
  orderNo: undefined,
  outStoreId: undefined,
  inStoreId: undefined,
  skuId: undefined,
  status: undefined,
  bizType: undefined,
  applyOperator: undefined,
  createTime: undefined
})

const detailDrawerVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref<Partial<StoreSkuTransferOrderApi.StoreSkuTransferOrderItem>>({})
const detailRows = ref<StoreSkuTransferOrderApi.StoreSkuTransferOrderDetailItem[]>([])
const detailParseFailed = ref(false)
const detailRaw = ref('')

const actionDialogVisible = ref(false)
const actionLoading = ref(false)
const actionType = ref<ActionType>('approve')
const actionTarget = ref<StoreSkuTransferOrderApi.StoreSkuTransferOrderItem | null>(null)
const actionForm = reactive({
  remark: ''
})
const rowActionLoadingId = ref<number>()
const rowActionLoadingType = ref<ActionType>()

const actionDialogTitle = computed(() => {
  if (actionType.value === 'approve') return '审批通过'
  if (actionType.value === 'reject') return '驳回调拨单'
  if (actionType.value === 'cancel') return '取消调拨单'
  return '提交调拨单'
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

const parseStatus = (status: any): number | undefined => {
  const parsed = Number(status)
  return Number.isFinite(parsed) ? parsed : undefined
}

const parseNumber = (value: any): number | undefined => {
  if (value === undefined || value === null || value === '') {
    return undefined
  }
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : undefined
}

const normalizeText = (text?: string) => {
  return String(text || '').trim()
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

const storeDisplay = (name?: string, id?: number) => {
  const textName = textOrDash(name)
  const textId = numberOrDash(id)
  if (textName === EMPTY_TEXT && textId === EMPTY_TEXT) {
    return EMPTY_TEXT
  }
  return `${textName}（#${textId}）`
}

const transferStoreDisplay = (row: Partial<StoreSkuTransferOrderApi.StoreSkuTransferOrderItem>) => {
  return `${storeDisplay(row.outStoreName, row.outStoreId)} → ${storeDisplay(row.inStoreName, row.inStoreId)}`
}

const lastActionSummary = (row: StoreSkuTransferOrderApi.StoreSkuTransferOrderItem) => {
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

const transferCountText = (row: StoreSkuTransferOrderApi.StoreSkuTransferOrderDetailItem) => {
  const transferCount = parseNumber(row.transferCount)
  if (transferCount !== undefined) {
    return String(transferCount)
  }
  const incrCount = parseNumber(row.incrCount)
  if (incrCount !== undefined) {
    return String(incrCount)
  }
  return EMPTY_TEXT
}

const isApiNotReadyError = (error: any) => {
  const text = `${error?.msg || ''} ${error?.message || ''}`.toLowerCase()
  const code = Number(error?.code ?? error?.status)
  if (code === 404) {
    return true
  }
  return (
    text.includes('transfer-order') &&
    (text.includes('404') || text.includes('not found') || text.includes('不存在') || text.includes('未找到'))
  )
}

const markTransferApiNotReady = (reason?: string) => {
  transferApiReady.value = false
  transferApiNotReadyReason.value = reason || '接口未就绪'
}

const clearTransferApiNotReady = () => {
  transferApiReady.value = true
  transferApiNotReadyReason.value = ''
}

const normalizeDetailRow = (item: Record<string, any>) => {
  return {
    skuId: parseNumber(item.skuId ?? item.storeSkuId ?? item.targetSkuId),
    outSkuId: parseNumber(item.outSkuId ?? item.fromSkuId),
    inSkuId: parseNumber(item.inSkuId ?? item.toSkuId),
    transferCount: parseNumber(item.transferCount ?? item.count ?? item.qty),
    incrCount: parseNumber(item.incrCount ?? item.delta)
  }
}

const resolveDetailArray = (payload: any): Array<Record<string, any>> => {
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
    detailRows.value = resolveDetailArray(parsed).map((item) => normalizeDetailRow(item))
  } catch {
    detailParseFailed.value = true
  }
}

const normalizeQuery = () => {
  queryParams.orderNo = normalizeText(queryParams.orderNo) || undefined
  queryParams.bizType = normalizeText(queryParams.bizType).toUpperCase() || undefined
  queryParams.applyOperator = normalizeText(queryParams.applyOperator) || undefined
}

const getList = async () => {
  loading.value = true
  try {
    normalizeQuery()
    const data = await StoreSkuTransferOrderApi.getStoreSkuTransferOrderPage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
    clearTransferApiNotReady()
  } catch (error: any) {
    list.value = []
    total.value = 0
    if (isApiNotReadyError(error)) {
      markTransferApiNotReady(error?.msg || '接口未就绪')
      return
    }
    message.error(error?.msg || '调拨单列表加载失败')
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
  queryParams.outStoreId = undefined
  queryParams.inStoreId = undefined
  queryParams.skuId = undefined
  queryParams.status = undefined
  queryParams.bizType = undefined
  queryParams.applyOperator = undefined
  queryParams.createTime = undefined
  await getList()
}

const ensureTransferApiReady = () => {
  if (transferApiReady.value) {
    return true
  }
  message.warning('调拨单接口未就绪，暂不可操作')
  return false
}

const openDetailDrawer = async (row: StoreSkuTransferOrderApi.StoreSkuTransferOrderItem) => {
  if (!ensureTransferApiReady()) {
    return
  }
  detailDrawerVisible.value = true
  detailLoading.value = true
  detailData.value = {}
  parseDetailJson(undefined)

  try {
    const data = await StoreSkuTransferOrderApi.getStoreSkuTransferOrder(row.id)
    detailData.value = data || {}
    parseDetailJson(data?.detailJson)
    clearTransferApiNotReady()
  } catch (error: any) {
    if (isApiNotReadyError(error)) {
      markTransferApiNotReady(error?.msg || '接口未就绪')
      detailDrawerVisible.value = false
      return
    }
    detailData.value = {
      id: row.id,
      orderNo: row.orderNo,
      outStoreId: row.outStoreId,
      outStoreName: row.outStoreName,
      inStoreId: row.inStoreId,
      inStoreName: row.inStoreName,
      status: row.status
    }
    parseDetailJson(undefined)
    message.error(error?.msg || '调拨单详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

const handleSubmit = async (row: StoreSkuTransferOrderApi.StoreSkuTransferOrderItem) => {
  if (!ensureTransferApiReady()) {
    return
  }
  if (!row?.id) {
    message.warning('调拨单ID为空，无法提交')
    return
  }
  try {
    await message.confirm(`确认提交调拨单 ${textOrDash(row.orderNo)} 吗？提交后将进入审批流程。`)
  } catch {
    return
  }

  rowActionLoadingId.value = row.id
  rowActionLoadingType.value = 'submit'
  try {
    await StoreSkuTransferOrderApi.submitStoreSkuTransferOrder({ id: row.id })
    clearTransferApiNotReady()
    message.success('调拨单提交成功')
    await getList()
  } catch (error: any) {
    if (isApiNotReadyError(error)) {
      markTransferApiNotReady(error?.msg || '接口未就绪')
      return
    }
    message.error(error?.msg || '调拨单提交失败，请稍后重试')
  } finally {
    rowActionLoadingId.value = undefined
    rowActionLoadingType.value = undefined
  }
}

const openActionDialog = (row: StoreSkuTransferOrderApi.StoreSkuTransferOrderItem, type: ActionType) => {
  if (!ensureTransferApiReady()) {
    return
  }
  actionType.value = type
  actionTarget.value = row
  actionForm.remark = ''
  actionDialogVisible.value = true
}

const handleAction = async () => {
  if (!ensureTransferApiReady()) {
    return
  }
  const target = actionTarget.value
  if (!target?.id) {
    message.warning('调拨单ID为空，无法执行操作')
    return
  }
  const remark = normalizeText(actionForm.remark)
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
    await message.confirm(`确认${actionName}调拨单 ${textOrDash(target.orderNo)} 吗？`)
  } catch {
    return
  }

  actionLoading.value = true
  rowActionLoadingId.value = target.id
  rowActionLoadingType.value = actionType.value
  try {
    if (actionType.value === 'approve') {
      await StoreSkuTransferOrderApi.approveStoreSkuTransferOrder({ id: target.id, remark })
    } else if (actionType.value === 'reject') {
      await StoreSkuTransferOrderApi.rejectStoreSkuTransferOrder({ id: target.id, remark })
    } else if (actionType.value === 'cancel') {
      await StoreSkuTransferOrderApi.cancelStoreSkuTransferOrder({ id: target.id, remark })
    }
    clearTransferApiNotReady()
    message.success(`${actionName}成功`)
    actionDialogVisible.value = false
    await getList()
  } catch (error: any) {
    if (isApiNotReadyError(error)) {
      markTransferApiNotReady(error?.msg || '接口未就绪')
      return
    }
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
