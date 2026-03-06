<template>
  <ContentWrap>
    <el-form :inline="true" :model="queryParams" class="-mb-15px" label-width="108px">
      <el-form-item label="订单ID" prop="orderId">
        <el-input-number v-model="queryParams.orderId" :controls="false" :min="1" class="!w-180px" />
      </el-form-item>
      <el-form-item label="商户退款单号" prop="merchantRefundId">
        <el-input
          v-model="queryParams.merchantRefundId"
          class="!w-220px"
          clearable
          placeholder="请输入 merchantRefundId"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="支付退款单ID" prop="payRefundId">
        <el-input-number v-model="queryParams.payRefundId" :controls="false" :min="1" class="!w-180px" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select
          v-model="queryParams.status"
          allow-create
          class="!w-170px"
          clearable
          filterable
          placeholder="请选择或输入状态"
        >
          <el-option
            v-for="item in statusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="错误码" prop="errorCode">
        <el-input
          v-model="queryParams.errorCode"
          class="!w-170px"
          clearable
          placeholder="例如 1030004014"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="创建时间" prop="createTime">
        <el-date-picker
          v-model="queryParams.createTime"
          class="!w-340px"
          end-placeholder="结束时间"
          range-separator="至"
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
        <el-button
          v-hasPermi="['booking:refund-notify-log:replay']"
          :disabled="selectedReplayIds.length === 0"
          :loading="replayLoading"
          plain
          type="info"
          @click="handleDryRunReplay"
        >
          <Icon class="mr-5px" icon="ep:view" />
          预演重放（dry-run）
        </el-button>
        <el-button
          v-hasPermi="['booking:refund-notify-log:replay']"
          :disabled="selectedReplayIds.length === 0"
          :loading="replayLoading"
          plain
          type="warning"
          @click="handleExecuteReplay"
        >
          <Icon class="mr-5px" icon="ep:refresh-right" />
          执行重放
        </el-button>
      </el-form-item>
      <el-form-item>
        <span class="text-12px text-[var(--el-text-color-secondary)]">
          已选可重放 {{ selectedReplayIds.length }} 条
        </span>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" :selectable="isReplaySelectable" />
      <el-table-column label="ID" prop="id" width="90" />
      <el-table-column label="订单ID" width="120">
        <template #default="{ row }">
          {{ numberOrDash(row.orderId) }}
        </template>
      </el-table-column>
      <el-table-column label="商户退款单号" min-width="220" prop="merchantRefundId" show-overflow-tooltip>
        <template #default="{ row }">
          {{ textOrDash(row.merchantRefundId) }}
        </template>
      </el-table-column>
      <el-table-column label="支付退款单ID" width="140">
        <template #default="{ row }">
          {{ numberOrDash(row.payRefundId) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">
            {{ statusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="错误码" min-width="120" prop="errorCode" show-overflow-tooltip>
        <template #default="{ row }">
          {{ textOrDash(row.errorCode) }}
        </template>
      </el-table-column>
      <el-table-column label="错误信息" min-width="240" prop="errorMsg" show-overflow-tooltip>
        <template #default="{ row }">
          {{ textOrDash(row.errorMsg) }}
        </template>
      </el-table-column>
      <el-table-column label="重试次数" width="100">
        <template #default="{ row }">
          {{ numberOrDash(row.retryCount) }}
        </template>
      </el-table-column>
      <el-table-column :formatter="dateFormatter" label="下次重试时间" prop="nextRetryTime" width="180" />
      <el-table-column label="最近重放结果" width="130">
        <template #default="{ row }">
          <el-tag v-if="resolveReplayStatus(row.lastReplayResult)" :type="replayResultTagType(row.lastReplayResult)">
            {{ replayResultText(row.lastReplayResult) }}
          </el-tag>
          <span v-else>{{ EMPTY_TEXT }}</span>
        </template>
      </el-table-column>
      <el-table-column label="最近重放人" width="140" show-overflow-tooltip>
        <template #default="{ row }">
          {{ textOrDash(row.lastReplayOperator) }}
        </template>
      </el-table-column>
      <el-table-column :formatter="dateFormatter" label="最近重放时间" prop="lastReplayTime" width="180" />
      <el-table-column label="最近重放备注" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          {{ textOrDash(row.lastReplayRemark) }}
        </template>
      </el-table-column>
      <el-table-column :formatter="dateFormatter" label="创建时间" prop="createTime" width="180" />
      <el-table-column align="center" fixed="right" label="操作" width="90">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetailDrawer(row)">详情</el-button>
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

  <el-dialog v-model="replayResultVisible" :title="replayResultTitle" width="1000px">
    <el-alert
      v-if="replayResult.fallbackLegacy"
      :closable="false"
      title="当前后端为旧版本，已降级为逐条执行模式"
      type="warning"
      class="mb-12px"
    />
    <el-descriptions :column="5" border class="mb-12px">
      <el-descriptions-item label="模式">{{ replayResult.dryRun ? 'dry-run 预演' : '执行重放' }}</el-descriptions-item>
      <el-descriptions-item label="成功数">{{ replayResult.successCount }}</el-descriptions-item>
      <el-descriptions-item label="跳过数">{{ replayResult.skipCount }}</el-descriptions-item>
      <el-descriptions-item label="失败数">{{ replayResult.failCount }}</el-descriptions-item>
      <el-descriptions-item label="明细条数">{{ replayResult.details.length }}</el-descriptions-item>
    </el-descriptions>

    <el-alert
      v-if="!replayResult.details.length"
      :closable="false"
      title="后端未返回 details 明细，请结合汇总结果与台账刷新后状态核对"
      type="info"
      class="mb-12px"
    />

    <el-empty v-if="!replayResult.details.length" description="无重放明细" />

    <el-table v-else :data="replayResult.details" max-height="460">
      <el-table-column label="日志ID" min-width="90">
        <template #default="{ row }">
          {{ numberOrDash(row.id) }}
        </template>
      </el-table-column>
      <el-table-column label="订单ID" min-width="110">
        <template #default="{ row }">
          {{ numberOrDash(row.orderId) }}
        </template>
      </el-table-column>
      <el-table-column label="商户退款单号" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          {{ textOrDash(row.merchantRefundId) }}
        </template>
      </el-table-column>
      <el-table-column label="支付退款单ID" min-width="120">
        <template #default="{ row }">
          {{ numberOrDash(row.payRefundId) }}
        </template>
      </el-table-column>
      <el-table-column label="结果" width="100">
        <template #default="{ row }">
          <el-tag :type="replayResultTagType(row.resultStatus)">
            {{ replayResultText(row.resultStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="结果码" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">
          {{ textOrDash(row.resultCode) }}
        </template>
      </el-table-column>
      <el-table-column label="结果说明" min-width="320" show-overflow-tooltip>
        <template #default="{ row }">
          {{ textOrDash(row.resultMessage) }}
        </template>
      </el-table-column>
    </el-table>

    <template #footer>
      <el-button type="primary" @click="replayResultVisible = false">关闭</el-button>
    </template>
  </el-dialog>

  <el-drawer v-model="detailDrawerVisible" size="48%" title="退款回调日志详情">
    <el-descriptions :column="2" border>
      <el-descriptions-item label="台账ID">{{ numberOrDash(detailRow.id) }}</el-descriptions-item>
      <el-descriptions-item label="订单ID">{{ numberOrDash(detailRow.orderId) }}</el-descriptions-item>
      <el-descriptions-item label="商户退款单号">{{ textOrDash(detailRow.merchantRefundId) }}</el-descriptions-item>
      <el-descriptions-item label="支付退款单ID">{{ numberOrDash(detailRow.payRefundId) }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag :type="statusTagType(detailRow.status)">
          {{ statusText(detailRow.status) }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="错误码">{{ textOrDash(detailRow.errorCode) }}</el-descriptions-item>
      <el-descriptions-item label="错误信息" :span="2">{{ textOrDash(detailRow.errorMsg) }}</el-descriptions-item>
      <el-descriptions-item label="重试次数">{{ numberOrDash(detailRow.retryCount) }}</el-descriptions-item>
      <el-descriptions-item label="下次重试时间">{{ textOrDash(detailRow.nextRetryTime) }}</el-descriptions-item>
      <el-descriptions-item label="最近重放结果">
        <el-tag v-if="resolveReplayStatus(detailRow.lastReplayResult)" :type="replayResultTagType(detailRow.lastReplayResult)">
          {{ replayResultText(detailRow.lastReplayResult) }}
        </el-tag>
        <span v-else>{{ EMPTY_TEXT }}</span>
      </el-descriptions-item>
      <el-descriptions-item label="最近重放人">{{ textOrDash(detailRow.lastReplayOperator) }}</el-descriptions-item>
      <el-descriptions-item label="最近重放时间">{{ textOrDash(detailRow.lastReplayTime) }}</el-descriptions-item>
      <el-descriptions-item label="最近重放备注">{{ textOrDash(detailRow.lastReplayRemark) }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ textOrDash(detailRow.createTime) }}</el-descriptions-item>
      <el-descriptions-item label="更新时间">{{ textOrDash(detailRow.updateTime) }}</el-descriptions-item>
    </el-descriptions>

    <div class="mt-16px">
      <div class="mb-8px font-500">rawPayload</div>
      <el-alert
        v-if="rawPayloadParseFailed"
        :closable="false"
        title="rawPayload 非法 JSON，已降级展示原文"
        type="warning"
        class="mb-8px"
      />
      <el-input
        :model-value="rawPayloadView"
        :rows="14"
        readonly
        type="textarea"
      />
    </div>
  </el-drawer>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import * as RefundNotifyLogApi from '@/api/mall/booking/refundNotifyLog'

defineOptions({ name: 'MallBookingRefundNotifyLogIndex' })

type ReplayStatus = 'SUCCESS' | 'SKIP' | 'FAIL'

interface ReplayDetailView {
  id?: number
  orderId?: number
  merchantRefundId?: string
  payRefundId?: number
  resultStatus: ReplayStatus
  resultCode?: string
  resultMessage?: string
}

interface ReplayResultView {
  dryRun: boolean
  fallbackLegacy: boolean
  successCount: number
  skipCount: number
  failCount: number
  details: ReplayDetailView[]
}

const EMPTY_TEXT = '--'
const DEFAULT_STATUS = 'fail'
const RESULT_HINT_BY_CODE: Record<number, string> = {
  1030004011: '商户退款单号不合法，请先核对 merchantRefundId',
  1030004013: '退款回调日志不存在，可能已被清理或 ID 无效',
  1030004014: '日志状态非法，仅失败记录允许重放'
}
const statusOptions = [
  { label: '失败（fail）', value: 'fail' },
  { label: '成功（success）', value: 'success' },
  { label: '待处理（pending）', value: 'pending' }
]

const message = useMessage()

const loading = ref(false)
const replayLoading = ref(false)
const total = ref(0)
const list = ref<RefundNotifyLogApi.RefundNotifyLogVO[]>([])
const selectedRows = ref<RefundNotifyLogApi.RefundNotifyLogVO[]>([])
const detailDrawerVisible = ref(false)
const detailRow = ref<Partial<RefundNotifyLogApi.RefundNotifyLogVO>>({})
const rawPayloadView = ref(EMPTY_TEXT)
const rawPayloadParseFailed = ref(false)
const replayResultVisible = ref(false)
const replayResult = ref<ReplayResultView>({
  dryRun: true,
  fallbackLegacy: false,
  successCount: 0,
  skipCount: 0,
  failCount: 0,
  details: []
})

const queryParams = reactive<RefundNotifyLogApi.RefundNotifyLogPageReq>({
  pageNo: 1,
  pageSize: 10,
  orderId: undefined,
  merchantRefundId: undefined,
  payRefundId: undefined,
  status: DEFAULT_STATUS,
  errorCode: undefined,
  createTime: undefined
})

const replayResultTitle = computed(() => {
  return replayResult.value.dryRun ? '退款回调重放预演结果' : '退款回调重放执行结果'
})

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

const parsePositiveInteger = (value: any): number | undefined => {
  if (value === undefined || value === null || value === '') {
    return undefined
  }
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) {
    return undefined
  }
  const normalized = Math.trunc(parsed)
  return normalized > 0 ? normalized : undefined
}

const normalizeStatus = (value: any): string | undefined => {
  const text = String(value || '').trim().toLowerCase()
  return text || undefined
}

const normalizeUpperText = (value: any): string | undefined => {
  const text = String(value || '').trim().toUpperCase()
  return text || undefined
}

const isFailStatus = (status: any) => {
  const normalized = normalizeStatus(status)
  return normalized === 'fail' || normalized === 'failed' || normalized === '2'
}

const canReplay = (row: Partial<RefundNotifyLogApi.RefundNotifyLogVO>) => {
  return isFailStatus(row.status)
}

const isReplaySelectable = (row: RefundNotifyLogApi.RefundNotifyLogVO) => {
  return canReplay(row)
}

const selectedReplayIds = computed(() => {
  const idSet = new Set<number>()
  selectedRows.value.forEach((row) => {
    if (!canReplay(row)) {
      return
    }
    const id = parsePositiveInteger(row.id)
    if (id) {
      idSet.add(id)
    }
  })
  return Array.from(idSet)
})

const selectedInvalidCount = computed(() => {
  return selectedRows.value.filter((row) => {
    return !canReplay(row) || !parsePositiveInteger(row.id)
  }).length
})

const statusText = (status?: RefundNotifyLogApi.RefundNotifyLogStatus) => {
  const normalized = normalizeStatus(status)
  if (normalized === 'success' || normalized === '1') return '成功'
  if (normalized === 'fail' || normalized === 'failed' || normalized === '2') return '失败'
  if (normalized === 'pending' || normalized === '0') return '待处理'
  return textOrDash(status)
}

const statusTagType = (status?: RefundNotifyLogApi.RefundNotifyLogStatus) => {
  const normalized = normalizeStatus(status)
  if (normalized === 'success' || normalized === '1') return 'success'
  if (normalized === 'fail' || normalized === 'failed' || normalized === '2') return 'danger'
  if (normalized === 'pending' || normalized === '0') return 'warning'
  return 'info'
}

const resolveReplayStatus = (value: any): ReplayStatus | undefined => {
  const normalized = normalizeUpperText(value)
  if (!normalized) {
    return undefined
  }
  if (['SUCCESS', 'SUCCEED', 'OK', 'DONE', 'PASSED'].includes(normalized)) {
    return 'SUCCESS'
  }
  if (['SKIP', 'SKIPPED', 'IGNORE', 'IGNORED'].includes(normalized)) {
    return 'SKIP'
  }
  if (normalized === '1') {
    return 'SUCCESS'
  }
  if (normalized === '2') {
    return 'SKIP'
  }
  if (['FAIL', 'FAILED', 'ERROR'].includes(normalized)) {
    return 'FAIL'
  }
  return undefined
}

const normalizeReplayStatus = (value: any): ReplayStatus => {
  return resolveReplayStatus(value) || 'FAIL'
}

const replayResultText = (status?: any) => {
  const normalized = resolveReplayStatus(status)
  if (!normalized) {
    return EMPTY_TEXT
  }
  if (normalized === 'SUCCESS') return 'SUCCESS'
  if (normalized === 'SKIP') return 'SKIP'
  return 'FAIL'
}

const replayResultTagType = (status?: any) => {
  const normalized = resolveReplayStatus(status)
  if (!normalized) return 'info'
  if (normalized === 'SUCCESS') return 'success'
  if (normalized === 'SKIP') return 'warning'
  return 'danger'
}

const resolveApiErrorCode = (error: any): number | undefined => {
  const candidates = [
    error?.code,
    error?.data?.code,
    error?.response?.data?.code,
    error?.response?.status
  ]
  for (const candidate of candidates) {
    const parsed = Number(candidate)
    if (Number.isFinite(parsed)) {
      return parsed
    }
  }
  return undefined
}

const resolveApiErrorMessage = (error: any): string => {
  const candidates = [
    error?.msg,
    error?.message,
    error?.data?.msg,
    error?.data?.message,
    error?.response?.data?.msg,
    error?.response?.data?.message
  ]
  for (const candidate of candidates) {
    const text = String(candidate || '').trim()
    if (text) {
      return text
    }
  }
  return ''
}

const buildApiErrorMessage = (error: any, fallback: string) => {
  const code = resolveApiErrorCode(error)
  const rawMessage = resolveApiErrorMessage(error)
  if (code !== undefined && rawMessage) {
    return `${fallback}（错误码: ${code}）：${rawMessage}`
  }
  if (code !== undefined) {
    return `${fallback}（错误码: ${code}）`
  }
  return rawMessage || fallback
}

const buildReadableReasonByCode = (code?: string, messageText?: string, status?: ReplayStatus) => {
  const numericCode = Number(code)
  const hint = Number.isFinite(numericCode) ? RESULT_HINT_BY_CODE[numericCode] : ''
  if (status === 'SUCCESS') {
    return messageText || '执行成功'
  }
  if (status === 'SKIP') {
    return hint || messageText || '已跳过'
  }
  if (hint && messageText) {
    return `${hint}（${messageText}）`
  }
  return hint || messageText || '执行失败'
}

const isLegacyReplayContractError = (error: any) => {
  const code = resolveApiErrorCode(error)
  const messageText = resolveApiErrorMessage(error).toUpperCase()
  if (code === 400 || code === 500) {
    if (
      messageText.includes('ID不能为空') ||
      messageText.includes('CANNOT DESERIALIZE') ||
      messageText.includes('UNRECOGNIZED FIELD') ||
      messageText.includes('TYPE MISMATCH') ||
      (messageText.includes('IDS') && messageText.includes('ID'))
    ) {
      return true
    }
  }
  return false
}

const normalizeQuery = () => {
  queryParams.orderId = parsePositiveInteger(queryParams.orderId)
  queryParams.payRefundId = parsePositiveInteger(queryParams.payRefundId)
  queryParams.merchantRefundId = String(queryParams.merchantRefundId || '').trim() || undefined
  queryParams.status = normalizeStatus(queryParams.status)
  queryParams.errorCode = normalizeUpperText(queryParams.errorCode)
  queryParams.createTime =
    Array.isArray(queryParams.createTime) && queryParams.createTime.length === 2
      ? queryParams.createTime
      : undefined
}

const parseRawPayload = (rawPayload: any) => {
  const rawText = String(rawPayload || '').trim()
  if (!rawText) {
    rawPayloadParseFailed.value = false
    rawPayloadView.value = EMPTY_TEXT
    return
  }
  try {
    const parsed = JSON.parse(rawText)
    rawPayloadView.value = JSON.stringify(parsed, null, 2)
    rawPayloadParseFailed.value = false
  } catch {
    rawPayloadView.value = rawText
    rawPayloadParseFailed.value = true
  }
}

const openDetailDrawer = (row: RefundNotifyLogApi.RefundNotifyLogVO) => {
  detailRow.value = { ...(row || {}) }
  parseRawPayload(detailRow.value.rawPayload)
  detailDrawerVisible.value = true
}

const getList = async () => {
  loading.value = true
  try {
    normalizeQuery()
    const data = await RefundNotifyLogApi.getRefundNotifyLogPage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
    selectedRows.value = []
  } catch (error: any) {
    list.value = []
    total.value = 0
    selectedRows.value = []
    message.error(buildApiErrorMessage(error, '退款回调日志查询失败'))
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryParams.pageNo = 1
  queryParams.pageSize = 10
  queryParams.orderId = undefined
  queryParams.merchantRefundId = undefined
  queryParams.payRefundId = undefined
  queryParams.status = DEFAULT_STATUS
  queryParams.errorCode = undefined
  queryParams.createTime = undefined
  selectedRows.value = []
  getList()
}

const handleSelectionChange = (rows: RefundNotifyLogApi.RefundNotifyLogVO[]) => {
  selectedRows.value = rows || []
}

const ensureReplaySelection = () => {
  if (!selectedRows.value.length) {
    message.warning('请先勾选至少一条失败记录')
    return false
  }
  if (!selectedReplayIds.value.length) {
    message.warning('当前勾选项没有可重放失败记录')
    return false
  }
  if (selectedInvalidCount.value > 0) {
    message.warning(`已自动忽略 ${selectedInvalidCount.value} 条不可重放记录`)
  }
  return true
}

const unwrapReplayResponse = (rawResp: any) => {
  if (rawResp === undefined || rawResp === null) {
    return rawResp
  }
  if (typeof rawResp !== 'object' || Array.isArray(rawResp)) {
    return rawResp
  }
  const source = rawResp as Record<string, any>
  if (source.data && typeof source.data === 'object' && !Array.isArray(source.data)) {
    return source.data
  }
  return source
}

const parseReplayDetail = (item: Record<string, any>, index: number): ReplayDetailView => {
  const status = normalizeReplayStatus(item.resultStatus ?? item.result ?? item.status ?? item.resultType)
  const resultCode = String(item.resultCode ?? item.code ?? item.errorCode ?? '').trim() || undefined
  const rawReason = String(item.resultMessage ?? item.message ?? item.failReason ?? item.errorMsg ?? '').trim() || undefined
  return {
    id: parsePositiveInteger(item.id),
    orderId: parsePositiveInteger(item.orderId),
    merchantRefundId: String(item.merchantRefundId || '').trim() || undefined,
    payRefundId: parsePositiveInteger(item.payRefundId),
    resultStatus: status,
    resultCode,
    resultMessage: buildReadableReasonByCode(resultCode, rawReason, status) || `明细 #${index + 1}`
  }
}

const buildReplayResultView = (rawResp: any, dryRun: boolean, ids: number[]): ReplayResultView => {
  const source = unwrapReplayResponse(rawResp)
  if (typeof source === 'boolean') {
    return {
      dryRun,
      fallbackLegacy: false,
      successCount: source ? ids.length : 0,
      skipCount: 0,
      failCount: source ? 0 : ids.length,
      details: source
        ? ids.map((id) => ({
            id,
            resultStatus: 'SUCCESS',
            resultMessage: dryRun ? '预演通过' : '执行成功'
          }))
        : ids.map((id) => ({
            id,
            resultStatus: 'FAIL',
            resultMessage: '执行失败'
          }))
    }
  }

  const payload = ((source || {}) as Record<string, any>) || {}
  const rawDetails = Array.isArray(payload.details)
    ? payload.details
    : Array.isArray(payload.items)
      ? payload.items
      : []
  const details = rawDetails
    .filter((item) => item && typeof item === 'object')
    .map((item, index) => parseReplayDetail(item as Record<string, any>, index))

  const successCountRaw = parsePositiveInteger(payload.successCount)
  const skipCountRaw = parsePositiveInteger(payload.skipCount)
  const failCountRaw = parsePositiveInteger(payload.failCount)

  const successCount = successCountRaw ?? details.filter((item) => item.resultStatus === 'SUCCESS').length
  const skipCount = skipCountRaw ?? details.filter((item) => item.resultStatus === 'SKIP').length
  const failCount = failCountRaw ?? details.filter((item) => item.resultStatus === 'FAIL').length

  return {
    dryRun,
    fallbackLegacy: false,
    successCount,
    skipCount,
    failCount,
    details
  }
}

const replayByLegacyApi = async (ids: number[]): Promise<ReplayResultView> => {
  const details: ReplayDetailView[] = []
  for (const id of ids) {
    try {
      await RefundNotifyLogApi.replayRefundNotifyLog({ id })
      details.push({
        id,
        resultStatus: 'SUCCESS',
        resultMessage: '旧接口执行成功'
      })
    } catch (error: any) {
      const code = resolveApiErrorCode(error)
      const codeText = code !== undefined ? String(code) : undefined
      const rawMessage = resolveApiErrorMessage(error)
      const status = code === 1030004014 ? 'SKIP' : 'FAIL'
      details.push({
        id,
        resultStatus: status,
        resultCode: codeText,
        resultMessage: buildReadableReasonByCode(codeText, rawMessage, status)
      })
    }
  }

  return {
    dryRun: false,
    fallbackLegacy: true,
    successCount: details.filter((item) => item.resultStatus === 'SUCCESS').length,
    skipCount: details.filter((item) => item.resultStatus === 'SKIP').length,
    failCount: details.filter((item) => item.resultStatus === 'FAIL').length,
    details
  }
}

const showReplayResult = (result: ReplayResultView) => {
  replayResult.value = result
  replayResultVisible.value = true
  if (!result.details.length) {
    message.warning('后端未返回 details 明细，请结合汇总结果核对')
  }
}

const handleDryRunReplay = async () => {
  if (!ensureReplaySelection()) {
    return
  }
  replayLoading.value = true
  try {
    const ids = selectedReplayIds.value
    const resp = await RefundNotifyLogApi.replayRefundNotifyLog({
      dryRun: true,
      ids
    })
    const result = buildReplayResultView(resp, true, ids)
    showReplayResult(result)
    message.success(
      `预演完成：SUCCESS ${result.successCount} / SKIP ${result.skipCount} / FAIL ${result.failCount}`
    )
  } catch (error: any) {
    if (isLegacyReplayContractError(error)) {
      message.warning('当前后端未支持 dry-run 预演能力，请升级至 V2 后端后重试')
      return
    }
    message.error(buildApiErrorMessage(error, '预演重放失败'))
  } finally {
    replayLoading.value = false
  }
}

const handleExecuteReplay = async () => {
  if (!ensureReplaySelection()) {
    return
  }
  const ids = selectedReplayIds.value
  try {
    await message.confirm(`确认执行重放 ${ids.length} 条失败记录吗？`)
  } catch {
    return
  }

  replayLoading.value = true
  try {
    let result: ReplayResultView = {
      dryRun: false,
      fallbackLegacy: false,
      successCount: 0,
      skipCount: 0,
      failCount: 0,
      details: []
    }
    try {
      const resp = await RefundNotifyLogApi.replayRefundNotifyLog({
        dryRun: false,
        ids
      })
      result = buildReplayResultView(resp, false, ids)
    } catch (error: any) {
      if (!isLegacyReplayContractError(error)) {
        throw error
      }
      result = await replayByLegacyApi(ids)
      message.warning('已降级旧接口逐条执行重放')
    }

    showReplayResult(result)
    message.success(
      `执行完成：SUCCESS ${result.successCount} / SKIP ${result.skipCount} / FAIL ${result.failCount}`
    )
    await getList()
  } catch (error: any) {
    message.error(buildApiErrorMessage(error, '执行重放失败'))
  } finally {
    replayLoading.value = false
  }
}

onMounted(() => {
  getList()
})
</script>
