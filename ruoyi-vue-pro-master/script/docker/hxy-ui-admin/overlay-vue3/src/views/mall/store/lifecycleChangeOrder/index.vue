<template>
  <ContentWrap>
    <el-form :inline="true" :model="queryParams" class="-mb-15px" label-width="116px">
      <el-form-item label="变更单号" prop="orderNo">
        <el-input
          v-model="queryParams.orderNo"
          class="!w-220px"
          clearable
          placeholder="请输入变更单号"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="门店ID" prop="storeId">
        <el-input-number
          v-model="queryParams.storeId"
          :controls="false"
          :min="1"
          class="!w-170px"
          placeholder="请输入门店ID"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" class="!w-160px" clearable placeholder="请选择状态">
          <el-option :value="0" label="草稿" />
          <el-option :value="10" label="待审批" />
          <el-option :value="20" label="已通过" />
          <el-option :value="30" label="已驳回" />
          <el-option :value="40" label="已取消" />
        </el-select>
      </el-form-item>
      <el-form-item label="原生命周期" prop="fromLifecycleStatus">
        <el-select v-model="queryParams.fromLifecycleStatus" class="!w-160px" clearable placeholder="请选择">
          <el-option :value="10" label="PREPARING" />
          <el-option :value="20" label="TRIAL" />
          <el-option :value="30" label="OPERATING" />
          <el-option :value="35" label="SUSPENDED" />
          <el-option :value="40" label="CLOSED" />
        </el-select>
      </el-form-item>
      <el-form-item label="目标生命周期" prop="toLifecycleStatus">
        <el-select v-model="queryParams.toLifecycleStatus" class="!w-160px" clearable placeholder="请选择">
          <el-option :value="10" label="PREPARING" />
          <el-option :value="20" label="TRIAL" />
          <el-option :value="30" label="OPERATING" />
          <el-option :value="35" label="SUSPENDED" />
          <el-option :value="40" label="CLOSED" />
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
      <el-form-item label="超时" prop="overdue">
        <el-select v-model="queryParams.overdue" class="!w-130px" clearable placeholder="全部">
          <el-option :value="true" label="已超时" />
          <el-option :value="false" label="未超时" />
        </el-select>
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
          新建变更单
        </el-button>
        <el-button plain type="info" @click="goLifecycleBatchLog">
          <Icon class="mr-5px" icon="ep:document" />
          生命周期批次台账
        </el-button>
        <el-button plain type="info" @click="goLifecycleRecheckLog">
          <Icon class="mr-5px" icon="ep:clock" />
          生命周期复核历史
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <div class="mb-12px flex flex-wrap items-center gap-8px">
      <el-button :type="quickFilter === 'all' ? 'primary' : 'default'" @click="setQuickFilter('all')">全部</el-button>
      <el-button :type="quickFilter === 'pending' ? 'primary' : 'default'" @click="setQuickFilter('pending')">
        待审批
      </el-button>
      <el-button :type="quickFilter === 'overdue' ? 'danger' : 'default'" @click="setQuickFilter('overdue')">
        已超时
      </el-button>
    </div>

    <el-table v-loading="loading" :data="list">
      <el-table-column label="ID" prop="id" width="88" />
      <el-table-column label="变更单号" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">
          {{ textOrDash(row.orderNo) }}
        </template>
      </el-table-column>
      <el-table-column label="门店" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">
          <span>{{ textOrDash(row.storeName) }}</span>
          <span class="text-[var(--el-text-color-secondary)]">（#{{ numberOrDash(row.storeId) }}）</span>
        </template>
      </el-table-column>
      <el-table-column label="变更方向" min-width="240">
        <template #default="{ row }">
          <el-tag :type="lifecycleTagType(row.fromLifecycleStatus)">
            {{ lifecycleText(row.fromLifecycleStatus) }}
          </el-tag>
          <span class="mx-8px text-[var(--el-text-color-secondary)]">→</span>
          <el-tag :type="lifecycleTagType(row.toLifecycleStatus)">
            {{ lifecycleText(row.toLifecycleStatus) }}
          </el-tag>
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
      <el-table-column label="提交时间" width="180">
        <template #default="{ row }">
          {{ textOrDash(row.submitTime) }}
        </template>
      </el-table-column>
      <el-table-column label="SLA 截止" width="180">
        <template #default="{ row }">
          {{ textOrDash(row.slaDeadlineTime) }}
        </template>
      </el-table-column>
      <el-table-column label="SLA 状态" width="120">
        <template #default="{ row }">
          <el-tag :type="slaTagType(row)">
            {{ slaText(row) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="申请时间" width="180">
        <template #default="{ row }">
          {{ textOrDash(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="最近动作编码" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">
          {{ textOrDash(row.lastActionCode) }}
        </template>
      </el-table-column>
      <el-table-column label="最近动作人" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">
          {{ textOrDash(row.lastActionOperator) }}
        </template>
      </el-table-column>
      <el-table-column label="最近动作时间" width="180">
        <template #default="{ row }">
          {{ textOrDash(row.lastActionTime) }}
        </template>
      </el-table-column>
      <el-table-column label="审批信息" min-width="280" show-overflow-tooltip>
        <template #default="{ row }">
          <div>{{ approvalHeader(row) }}</div>
          <div class="text-[var(--el-text-color-secondary)]">{{ approvalRemark(row) }}</div>
        </template>
      </el-table-column>
      <el-table-column align="center" fixed="right" label="操作" width="420">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetailDrawer(row)">查看详情</el-button>
          <el-button link type="warning" @click="copyText(row.orderNo, '变更单号')">复制单号</el-button>
          <el-button link type="warning" @click="copyText(row.storeId, '门店ID')">复制门店ID</el-button>
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

  <el-dialog v-model="createDialogVisible" title="新建生命周期变更单" width="540px">
    <el-form :model="createForm" label-width="128px">
      <el-form-item label="门店ID" required>
        <el-input-number v-model="createForm.storeId" :controls="false" :min="1" class="!w-full" />
      </el-form-item>
      <el-form-item label="目标生命周期" required>
        <el-select v-model="createForm.toLifecycleStatus" class="!w-full" placeholder="请选择目标生命周期">
          <el-option :value="10" label="PREPARING" />
          <el-option :value="20" label="TRIAL" />
          <el-option :value="30" label="OPERATING" />
          <el-option :value="35" label="SUSPENDED" />
          <el-option :value="40" label="CLOSED" />
        </el-select>
      </el-form-item>
      <el-form-item label="申请原因" required>
        <el-input v-model="createForm.reason" :rows="3" maxlength="255" show-word-limit type="textarea" />
      </el-form-item>
      <el-form-item label="来源">
        <el-input v-model="createForm.applySource" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="createDialogVisible = false">取消</el-button>
      <el-button :loading="createLoading" type="primary" @click="handleCreate">创建</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="actionDialogVisible" :title="actionDialogTitle" width="520px">
    <el-form :model="actionForm" label-width="110px">
      <el-form-item label="变更单号">
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

  <el-drawer v-model="detailDrawerVisible" size="62%" title="生命周期变更单详情">
    <div v-loading="detailLoading">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="ID">{{ numberOrDash(detailData.id) }}</el-descriptions-item>
        <el-descriptions-item label="变更单号">{{ textOrDash(detailData.orderNo) }}</el-descriptions-item>
        <el-descriptions-item label="门店">{{ storeDisplay(detailData) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(detailData.status)">
            {{ statusText(detailData.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="原生命周期">
          {{ lifecycleText(detailData.fromLifecycleStatus) }}
        </el-descriptions-item>
        <el-descriptions-item label="目标生命周期">
          {{ lifecycleText(detailData.toLifecycleStatus) }}
        </el-descriptions-item>
        <el-descriptions-item label="申请人">{{ textOrDash(detailData.applyOperator) }}</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ textOrDash(detailData.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ textOrDash(detailData.submitTime) }}</el-descriptions-item>
        <el-descriptions-item label="SLA 截止">{{ textOrDash(detailData.slaDeadlineTime) }}</el-descriptions-item>
        <el-descriptions-item label="SLA 状态">
          <el-tag :type="slaTagType(detailData)">
            {{ slaText(detailData) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="最近动作编码">{{ textOrDash(detailData.lastActionCode) }}</el-descriptions-item>
        <el-descriptions-item label="最近动作人">{{ textOrDash(detailData.lastActionOperator) }}</el-descriptions-item>
        <el-descriptions-item label="最近动作时间">{{ textOrDash(detailData.lastActionTime) }}</el-descriptions-item>
        <el-descriptions-item label="守卫阻塞">{{ booleanText(detailData.guardBlocked) }}</el-descriptions-item>
        <el-descriptions-item label="守卫告警">{{ textOrDash(detailData.guardWarnings) }}</el-descriptions-item>
        <el-descriptions-item label="审批人">{{ textOrDash(detailData.approveOperator) }}</el-descriptions-item>
        <el-descriptions-item label="审批时间">{{ textOrDash(detailData.approveTime) }}</el-descriptions-item>
      </el-descriptions>

      <div class="mt-16px">
        <div class="mb-8px font-500">申请原因</div>
        <el-input :model-value="textOrDash(detailData.reason)" :rows="3" readonly type="textarea" />
      </div>

      <div class="mt-16px">
        <div class="mb-8px flex items-center justify-between">
          <span class="font-500">guardSnapshotJson（结构化）</span>
          <el-button link type="warning" @click="copyText(guardSnapshotRaw, 'guardSnapshotJson')">复制原文</el-button>
        </div>
        <el-alert
          v-if="guardSnapshotParseFailed"
          :closable="false"
          title="guardSnapshotJson 解析失败（原文保留）"
          type="warning"
        />
        <el-empty v-else-if="!guardSnapshotRows.length" description="无可用明细" />
        <el-table v-else :data="guardSnapshotRows" border max-height="320">
          <el-table-column label="键" min-width="220" prop="key" />
          <el-table-column label="值" min-width="360" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.value }}
            </template>
          </el-table-column>
        </el-table>
        <el-input :model-value="guardSnapshotRaw || EMPTY_TEXT" :rows="5" class="mt-8px" readonly type="textarea" />
      </div>
    </div>
  </el-drawer>
</template>

<script lang="ts" setup>
import * as LifecycleChangeOrderApi from '@/api/mall/store/lifecycleChangeOrder'
import { useRoute, useRouter } from 'vue-router'

defineOptions({ name: 'MallStoreLifecycleChangeOrderIndex' })

type ActionType = 'approve' | 'reject' | 'cancel' | 'submit'
type QuickFilterType = 'all' | 'pending' | 'overdue'

const EMPTY_TEXT = '--'
const SLA_NEAR_TIMEOUT_MINUTES = 120

const message = useMessage()
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const total = ref(0)
const list = ref<LifecycleChangeOrderApi.StoreLifecycleChangeOrderItem[]>([])

const queryParams = reactive<LifecycleChangeOrderApi.StoreLifecycleChangeOrderPageReq>({
  pageNo: 1,
  pageSize: 10,
  orderNo: undefined,
  storeId: undefined,
  status: undefined,
  fromLifecycleStatus: undefined,
  toLifecycleStatus: undefined,
  applyOperator: undefined,
  overdue: undefined,
  lastActionCode: undefined,
  lastActionOperator: undefined,
  createTime: undefined
})
const quickFilter = ref<QuickFilterType>('all')

const createDialogVisible = ref(false)
const createLoading = ref(false)
const createForm = reactive<LifecycleChangeOrderApi.StoreLifecycleChangeOrderCreateReq>({
  storeId: undefined,
  toLifecycleStatus: undefined,
  reason: '',
  applySource: 'ADMIN_UI'
})

const actionDialogVisible = ref(false)
const actionLoading = ref(false)
const actionType = ref<ActionType>('approve')
const actionTarget = ref<LifecycleChangeOrderApi.StoreLifecycleChangeOrderItem | null>(null)
const actionForm = reactive({
  remark: ''
})
const rowActionLoadingId = ref<number>()
const rowActionLoadingType = ref<ActionType>()

const detailDrawerVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref<Partial<LifecycleChangeOrderApi.StoreLifecycleChangeOrderItem>>({})
const guardSnapshotRows = ref<Array<{ key: string; value: string }>>([])
const guardSnapshotParseFailed = ref(false)
const guardSnapshotRaw = ref('')

const actionDialogTitle = computed(() => {
  if (actionType.value === 'approve') return '审批通过'
  if (actionType.value === 'reject') return '驳回变更单'
  if (actionType.value === 'cancel') return '取消变更单'
  return '提交变更单'
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

const booleanText = (value: any) => {
  if (value === true) return '是'
  if (value === false) return '否'
  return EMPTY_TEXT
}

const stringifyValue = (value: any) => {
  if (value === undefined || value === null) {
    return EMPTY_TEXT
  }
  if (typeof value === 'string') {
    const text = value.trim()
    return text || EMPTY_TEXT
  }
  if (typeof value === 'number' || typeof value === 'boolean') {
    return String(value)
  }
  try {
    const serialized = JSON.stringify(value)
    return serialized || EMPTY_TEXT
  } catch {
    return EMPTY_TEXT
  }
}

const firstQueryValue = (value: any): string => {
  if (Array.isArray(value)) {
    return String(value[0] || '')
  }
  return String(value || '')
}

const parseQueryNumber = (value: any): number | undefined => {
  const text = firstQueryValue(value).trim()
  if (!text) {
    return undefined
  }
  const parsed = Number(text)
  return Number.isFinite(parsed) ? parsed : undefined
}

const parseStatus = (status: any): number | undefined => {
  const parsed = Number(status)
  return Number.isFinite(parsed) ? parsed : undefined
}

const parseDateMs = (value?: string) => {
  const text = String(value || '').trim()
  if (!text) {
    return undefined
  }
  const time = new Date(text).getTime()
  return Number.isFinite(time) ? time : undefined
}

const isPendingStatus = (status?: number) => parseStatus(status) === 10

const isRowOverdue = (row: Partial<LifecycleChangeOrderApi.StoreLifecycleChangeOrderItem>) => {
  if (!isPendingStatus(row.status)) {
    return false
  }
  if (row.overdue === true) {
    return true
  }
  const deadlineMs = parseDateMs(row.slaDeadlineTime)
  if (deadlineMs === undefined) {
    return false
  }
  return Date.now() > deadlineMs
}

const isNearTimeout = (row: Partial<LifecycleChangeOrderApi.StoreLifecycleChangeOrderItem>) => {
  if (!isPendingStatus(row.status) || isRowOverdue(row)) {
    return false
  }
  const deadlineMs = parseDateMs(row.slaDeadlineTime)
  if (deadlineMs === undefined) {
    return false
  }
  return deadlineMs - Date.now() <= SLA_NEAR_TIMEOUT_MINUTES * 60 * 1000
}

const slaText = (row: Partial<LifecycleChangeOrderApi.StoreLifecycleChangeOrderItem>) => {
  if (!isPendingStatus(row.status)) {
    return EMPTY_TEXT
  }
  if (isRowOverdue(row)) {
    return '已超时'
  }
  if (isNearTimeout(row)) {
    return '即将超时'
  }
  return '正常'
}

const slaTagType = (row: Partial<LifecycleChangeOrderApi.StoreLifecycleChangeOrderItem>) => {
  if (!isPendingStatus(row.status)) {
    return 'info'
  }
  if (isRowOverdue(row)) {
    return 'danger'
  }
  if (isNearTimeout(row)) {
    return 'warning'
  }
  return 'success'
}

const lifecycleText = (status?: number) => {
  if (status === 10) return 'PREPARING'
  if (status === 20) return 'TRIAL'
  if (status === 30) return 'OPERATING'
  if (status === 35) return 'SUSPENDED'
  if (status === 40) return 'CLOSED'
  return status === undefined || status === null ? EMPTY_TEXT : `UNKNOWN(${status})`
}

const lifecycleTagType = (status?: number) => {
  if (status === 30) return 'success'
  if (status === 20) return 'warning'
  if (status === 35) return 'danger'
  if (status === 40) return 'info'
  return 'info'
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

const approvalHeader = (row: LifecycleChangeOrderApi.StoreLifecycleChangeOrderItem) => {
  const parts = [row.approveOperator, row.approveTime].map((item) => String(item || '').trim()).filter(Boolean)
  return parts.length ? parts.join(' / ') : EMPTY_TEXT
}

const approvalRemark = (row: LifecycleChangeOrderApi.StoreLifecycleChangeOrderItem) => {
  return textOrDash(row.approveRemark)
}

const storeDisplay = (row: Partial<LifecycleChangeOrderApi.StoreLifecycleChangeOrderItem>) => {
  const name = textOrDash(row.storeName)
  const id = numberOrDash(row.storeId)
  if (name === EMPTY_TEXT && id === EMPTY_TEXT) {
    return EMPTY_TEXT
  }
  return `${name}（#${id}）`
}

const canSubmit = (status?: number) => {
  const normalized = parseStatus(status)
  return normalized === 0
}

const canApprove = (status?: number) => parseStatus(status) === 10

const canReject = (status?: number) => parseStatus(status) === 10

const canCancel = (status?: number) => {
  const normalized = parseStatus(status)
  return normalized === 0 || normalized === 10
}

const isActionLoading = (id?: number, type?: ActionType) => {
  return rowActionLoadingId.value === id && rowActionLoadingType.value === type
}

const copyText = async (value: any, label: string) => {
  const content = String(value || '').trim()
  if (!content) {
    message.warning(`${label}为空，无法复制`)
    return
  }
  try {
    await navigator.clipboard.writeText(content)
    message.success(`${label}复制成功`)
  } catch {
    message.error('复制失败，请检查浏览器剪贴板权限')
  }
}

const parseGuardSnapshotRows = (rawJson?: string) => {
  guardSnapshotRaw.value = String(rawJson || '')
  guardSnapshotParseFailed.value = false
  guardSnapshotRows.value = []

  const raw = guardSnapshotRaw.value.trim()
  if (!raw) {
    return
  }
  try {
    const parsed = JSON.parse(raw)
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      guardSnapshotParseFailed.value = true
      return
    }
    guardSnapshotRows.value = Object.keys(parsed).map((key) => ({
      key,
      value: stringifyValue((parsed as Record<string, any>)[key])
    }))
  } catch {
    guardSnapshotParseFailed.value = true
  }
}

const initQueryFromRoute = () => {
  queryParams.orderNo = firstQueryValue(route.query.orderNo).trim() || undefined
  queryParams.storeId = parseQueryNumber(route.query.storeId)
  queryParams.status = parseQueryNumber(route.query.status)
  const overdueText = firstQueryValue(route.query.overdue).trim().toLowerCase()
  queryParams.overdue = overdueText === 'true' ? true : overdueText === 'false' ? false : undefined
  queryParams.lastActionCode = firstQueryValue(route.query.lastActionCode).trim() || undefined
  queryParams.lastActionOperator = firstQueryValue(route.query.lastActionOperator).trim() || undefined
  if (queryParams.status === 10 && queryParams.overdue === true) {
    quickFilter.value = 'overdue'
  } else if (queryParams.status === 10) {
    quickFilter.value = 'pending'
  } else {
    quickFilter.value = 'all'
  }
}

const normalizeQuery = () => {
  queryParams.orderNo = String(queryParams.orderNo || '').trim() || undefined
  queryParams.applyOperator = String(queryParams.applyOperator || '').trim() || undefined
  queryParams.lastActionCode = String(queryParams.lastActionCode || '').trim().toUpperCase() || undefined
  queryParams.lastActionOperator = String(queryParams.lastActionOperator || '').trim() || undefined
}

const setQuickFilter = async (type: QuickFilterType) => {
  quickFilter.value = type
  queryParams.pageNo = 1
  if (type === 'all') {
    queryParams.status = undefined
    queryParams.overdue = undefined
  } else if (type === 'pending') {
    queryParams.status = 10
    queryParams.overdue = undefined
  } else {
    queryParams.status = 10
    queryParams.overdue = true
  }
  await getList()
}

const getList = async () => {
  loading.value = true
  try {
    normalizeQuery()
    const data = await LifecycleChangeOrderApi.getStoreLifecycleChangeOrderPage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
  } catch {
    message.error('加载变更单列表失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const handleQuery = async () => {
  if (queryParams.status === 10 && queryParams.overdue === true) {
    quickFilter.value = 'overdue'
  } else if (queryParams.status === 10) {
    quickFilter.value = 'pending'
  } else {
    quickFilter.value = 'all'
  }
  queryParams.pageNo = 1
  await getList()
}

const resetQuery = async () => {
  queryParams.pageNo = 1
  queryParams.pageSize = 10
  queryParams.orderNo = undefined
  queryParams.storeId = undefined
  queryParams.status = undefined
  queryParams.fromLifecycleStatus = undefined
  queryParams.toLifecycleStatus = undefined
  queryParams.applyOperator = undefined
  queryParams.overdue = undefined
  queryParams.lastActionCode = undefined
  queryParams.lastActionOperator = undefined
  queryParams.createTime = undefined
  quickFilter.value = 'all'
  await getList()
}

const goLifecycleBatchLog = () => {
  router.push('/mall/product/store-master/store-lifecycle-batch-log')
}

const goLifecycleRecheckLog = () => {
  router.push('/mall/product/store-master/store-lifecycle-recheck-log')
}

const openCreateDialog = () => {
  createDialogVisible.value = true
  createForm.storeId = undefined
  createForm.toLifecycleStatus = undefined
  createForm.reason = ''
  createForm.applySource = 'ADMIN_UI'
}

const handleCreate = async () => {
  if (!createForm.storeId) {
    message.warning('请输入门店ID')
    return
  }
  if (!createForm.toLifecycleStatus) {
    message.warning('请选择目标生命周期')
    return
  }
  if (!String(createForm.reason || '').trim()) {
    message.warning('请输入申请原因')
    return
  }

  createLoading.value = true
  try {
    await LifecycleChangeOrderApi.createStoreLifecycleChangeOrder({
      storeId: createForm.storeId,
      toLifecycleStatus: createForm.toLifecycleStatus,
      reason: String(createForm.reason || '').trim(),
      applySource: String(createForm.applySource || '').trim() || 'ADMIN_UI'
    })
    message.success('变更单创建成功')
    createDialogVisible.value = false
    await getList()
  } catch {
    message.error('变更单创建失败，请检查参数后重试')
  } finally {
    createLoading.value = false
  }
}

const openDetailDrawer = async (row: LifecycleChangeOrderApi.StoreLifecycleChangeOrderItem) => {
  detailDrawerVisible.value = true
  detailLoading.value = true
  detailData.value = {}
  guardSnapshotRows.value = []
  guardSnapshotParseFailed.value = false
  guardSnapshotRaw.value = ''

  try {
    const data = await LifecycleChangeOrderApi.getStoreLifecycleChangeOrder(row.id)
    detailData.value = data || {}
    parseGuardSnapshotRows(data?.guardSnapshotJson)
  } catch {
    detailData.value = {
      id: row.id,
      orderNo: row.orderNo,
      storeId: row.storeId,
      storeName: row.storeName
    }
    message.error('加载变更单详情失败')
  } finally {
    detailLoading.value = false
  }
}

const handleSubmit = async (row: LifecycleChangeOrderApi.StoreLifecycleChangeOrderItem) => {
  if (!row?.id) {
    message.warning('变更单ID为空，无法提交')
    return
  }
  try {
    await message.confirm(`确认提交变更单 ${textOrDash(row.orderNo)} 吗？提交后将进入审批流程。`)
  } catch {
    return
  }

  rowActionLoadingId.value = row.id
  rowActionLoadingType.value = 'submit'
  try {
    await LifecycleChangeOrderApi.submitStoreLifecycleChangeOrder({ id: row.id })
    message.success('变更单提交成功')
    await getList()
  } catch {
    message.error('变更单提交失败，请稍后重试')
  } finally {
    rowActionLoadingId.value = undefined
    rowActionLoadingType.value = undefined
  }
}

const openActionDialog = (row: LifecycleChangeOrderApi.StoreLifecycleChangeOrderItem, type: ActionType) => {
  actionType.value = type
  actionTarget.value = row
  actionForm.remark = ''
  actionDialogVisible.value = true
}

const handleAction = async () => {
  const target = actionTarget.value
  if (!target?.id) {
    message.warning('变更单ID为空，无法执行操作')
    return
  }
  const remark = String(actionForm.remark || '').trim()
  if (actionType.value === 'reject' && !remark) {
    message.warning('请输入驳回原因')
    return
  }

  const actionName =
    actionType.value === 'approve' ? '审批通过' : actionType.value === 'reject' ? '驳回' : actionType.value === 'cancel' ? '取消' : '提交'
  try {
    await message.confirm(`确认${actionName}变更单 ${textOrDash(target.orderNo)} 吗？`)
  } catch {
    return
  }

  actionLoading.value = true
  rowActionLoadingId.value = target.id
  rowActionLoadingType.value = actionType.value
  try {
    if (actionType.value === 'approve') {
      await LifecycleChangeOrderApi.approveStoreLifecycleChangeOrder({ id: target.id, remark })
    } else if (actionType.value === 'reject') {
      await LifecycleChangeOrderApi.rejectStoreLifecycleChangeOrder({ id: target.id, remark })
    } else if (actionType.value === 'cancel') {
      await LifecycleChangeOrderApi.cancelStoreLifecycleChangeOrder({ id: target.id, remark })
    }
    message.success(`${actionName}成功`)
    actionDialogVisible.value = false
    await getList()
  } catch {
    message.error(`${actionName}失败，请稍后重试`)
  } finally {
    actionLoading.value = false
    rowActionLoadingId.value = undefined
    rowActionLoadingType.value = undefined
  }
}

onMounted(async () => {
  initQueryFromRoute()
  await getList()
})
</script>
