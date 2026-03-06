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
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list">
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
      <el-table-column :formatter="dateFormatter" label="创建时间" prop="createTime" width="180" />
      <el-table-column align="center" fixed="right" label="操作" width="190">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetailDrawer(row)">详情</el-button>
          <el-button
            v-hasPermi="['booking:refund-notify-log:replay']"
            :disabled="!canReplay(row)"
            :loading="isReplaying(row.id)"
            :title="canReplay(row) ? '' : '仅失败记录可重放'"
            link
            type="warning"
            @click="handleReplay(row)"
          >
            重放
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

const EMPTY_TEXT = '--'
const DEFAULT_STATUS = 'fail'
const REPLAY_HINT_BY_CODE: Record<number, string> = {
  1030004011: '商户退款单号不合法，请先核对 merchantRefundId 再重放',
  1030004013: '退款回调日志不存在，可能已被清理或 ID 无效',
  1030004014: '当前日志状态非法，仅失败记录允许重放'
}
const statusOptions = [
  { label: '失败（fail）', value: 'fail' },
  { label: '成功（success）', value: 'success' },
  { label: '待处理（pending）', value: 'pending' }
]

const message = useMessage()

const loading = ref(false)
const total = ref(0)
const list = ref<RefundNotifyLogApi.RefundNotifyLogVO[]>([])
const replayingIds = ref<number[]>([])
const detailDrawerVisible = ref(false)
const detailRow = ref<Partial<RefundNotifyLogApi.RefundNotifyLogVO>>({})
const rawPayloadView = ref(EMPTY_TEXT)
const rawPayloadParseFailed = ref(false)

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

const isReplaying = (id: any) => {
  const rowId = parsePositiveInteger(id)
  return rowId !== undefined && replayingIds.value.includes(rowId)
}

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

const buildReplayErrorMessage = (error: any) => {
  const code = resolveApiErrorCode(error)
  const rawMessage = resolveApiErrorMessage(error)
  if (code !== undefined && REPLAY_HINT_BY_CODE[code]) {
    const hint = REPLAY_HINT_BY_CODE[code]
    if (rawMessage) {
      return `${hint}（错误码: ${code}，后端信息：${rawMessage}）`
    }
    return `${hint}（错误码: ${code}）`
  }
  return buildApiErrorMessage(error, '退款回调重放失败')
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
  } catch (error: any) {
    list.value = []
    total.value = 0
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
  getList()
}

const handleReplay = async (row: RefundNotifyLogApi.RefundNotifyLogVO) => {
  const id = parsePositiveInteger(row.id)
  if (!id) {
    message.warning('日志ID为空，无法重放')
    return
  }
  if (!canReplay(row)) {
    message.warning('仅失败记录允许重放')
    return
  }
  try {
    await message.confirm(`确认重放日志 #${id} 吗？`)
  } catch {
    return
  }

  replayingIds.value.push(id)
  try {
    await RefundNotifyLogApi.replayRefundNotifyLog({ id })
    message.success('重放请求已提交')
    await getList()
  } catch (error: any) {
    message.error(buildReplayErrorMessage(error))
  } finally {
    replayingIds.value = replayingIds.value.filter((item) => item !== id)
  }
}

onMounted(() => {
  getList()
})
</script>
