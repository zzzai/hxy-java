<template>
  <doc-alert title="预约评价通知出站台账" url="https://doc.iocoder.cn/" />

  <ContentWrap>
    <el-alert
      :closable="false"
      description="列表结果只展示当前筛选命中的 notify outbox 记录子集；审计摘要按 review 维度聚合同一条差评在 App / 企微的当前状态。PENDING 表示待派发，BLOCKED_NO_OWNER 表示当前通道缺店长账号或路由阻断，不代表系统已经自动补发。"
      title="通知出站台账"
      type="info"
    />
  </ContentWrap>

  <ContentWrap>
    <el-form :inline="true" :model="queryParams" class="-mb-15px" label-width="110px">
      <el-form-item label="评价ID">
        <el-input v-model="queryParams.reviewId" class="!w-180px" clearable placeholder="请输入评价ID" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="门店ID">
        <el-input v-model="queryParams.storeId" class="!w-180px" clearable placeholder="请输入门店ID" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="接收账号ID">
        <el-input
          v-model="queryParams.receiverUserId"
          class="!w-180px"
          clearable
          placeholder="请输入接收账号ID"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="接收账号">
        <el-input
          v-model="queryParams.receiverAccount"
          class="!w-220px"
          clearable
          placeholder="请输入接收账号"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="通知状态">
        <el-select v-model="queryParams.status" class="!w-180px" clearable placeholder="全部">
          <el-option label="待派发" value="PENDING" />
          <el-option label="已派发" value="SENT" />
          <el-option label="发送失败" value="FAILED" />
          <el-option label="BLOCKED_NO_OWNER" value="BLOCKED_NO_OWNER" />
        </el-select>
      </el-form-item>
      <el-form-item label="接收角色">
        <el-select v-model="queryParams.receiverRole" class="!w-180px" clearable placeholder="全部">
          <el-option label="STORE_MANAGER" value="STORE_MANAGER" />
        </el-select>
      </el-form-item>
      <el-form-item label="通知渠道">
        <el-select v-model="queryParams.channel" class="!w-180px" clearable placeholder="全部">
          <el-option label="IN_APP" value="IN_APP" />
          <el-option label="WECOM" value="WECOM" />
        </el-select>
      </el-form-item>
      <el-form-item label="最近动作">
        <el-select v-model="queryParams.lastActionCode" class="!w-180px" clearable placeholder="全部">
          <el-option label="CREATE_OUTBOX" value="CREATE_OUTBOX" />
          <el-option label="MANUAL_RETRY" value="MANUAL_RETRY" />
          <el-option label="DISPATCH_SUCCESS" value="DISPATCH_SUCCESS" />
          <el-option label="DISPATCH_FAILED" value="DISPATCH_FAILED" />
          <el-option label="BLOCKED_NO_OWNER" value="BLOCKED_NO_OWNER" />
        </el-select>
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
        <el-button plain type="primary" @click="goBackToLedger">
          <Icon class="mr-5px" icon="ep:back" />
          返回评价台账
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <div v-loading="summaryLoading" class="grid grid-cols-1 gap-12px md:grid-cols-5">
      <el-card v-for="item in auditCards" :key="item.label" shadow="never">
        <div class="text-12px text-[#909399]">{{ item.label }}</div>
        <div class="mt-8px text-24px font-600 text-[#303133]">{{ item.value }}</div>
        <div class="mt-8px text-12px text-[#606266]">{{ item.detail }}</div>
      </el-card>
    </div>
  </ContentWrap>

  <ContentWrap>
    <el-alert
      :closable="false"
      description="审计摘要按 review 维度聚合同一条差评在 App / 企微的当前状态，不代表全链路送达，不代表门店已处理完成，也不改变现有人工重试和阻断治理规则。"
      title="跨通道审计概览"
      type="warning"
    />
  </ContentWrap>

  <ContentWrap>
    <div class="mb-16px flex flex-wrap gap-8px">
      <el-button plain type="danger" @click="applyQuickStatus('BLOCKED_NO_OWNER')">只看阻断</el-button>
      <el-button plain type="warning" @click="applyQuickStatus('FAILED')">只看失败</el-button>
      <el-button plain type="primary" @click="applyQuickStatus('PENDING')">只看待派发</el-button>
      <el-button plain type="success" @click="applyQuickAction('MANUAL_RETRY')">只看人工重试</el-button>
      <el-button plain @click="applyQuickStatus()">查看全部</el-button>
    </div>

    <el-table v-loading="loading" :data="list">
      <el-table-column label="出站ID" prop="id" width="90" />
      <el-table-column label="评价ID" prop="reviewId" width="100" />
      <el-table-column label="门店ID" prop="storeId" width="100" />
      <el-table-column label="接收角色" prop="receiverRole" width="150" />
      <el-table-column label="接收账号ID" prop="receiverUserId" width="120" />
      <el-table-column label="接收账号" prop="receiverAccount" min-width="180" show-overflow-tooltip />
      <el-table-column label="通知类型" prop="notifyType" min-width="180" />
      <el-table-column label="通知渠道" prop="channel" width="120" />
      <el-table-column label="通知状态" width="150">
        <template #default="{ row }">
          <el-tag :type="notifyStatusTagType(row.status)">{{ notifyStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="诊断结论" min-width="180">
        <template #default="{ row }">
          {{ row.diagnosticLabel || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="修复建议" min-width="260" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.repairHint || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="最近动作说明" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.actionLabel || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="最近动作人" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.actionOperatorLabel || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="动作原因" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.actionReason || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="跨通道结论" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.reviewAuditLabel || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="跨通道说明" min-width="260" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.reviewAuditDetail || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="重试次数" prop="retryCount" width="100" />
      <el-table-column label="最后错误" prop="lastErrorMsg" min-width="240" show-overflow-tooltip />
      <el-table-column label="最近动作" prop="lastActionCode" width="160" />
      <el-table-column :formatter="dateFormatter" label="最近动作时间" prop="lastActionTime" width="180" />
      <el-table-column :formatter="dateFormatter" label="创建时间" prop="createTime" width="180" />
      <el-table-column align="center" fixed="right" label="操作" width="240">
        <template #default="{ row }">
          <el-button
            v-if="canRetry(row)"
            v-hasPermi="['booking:review:update']"
            :loading="retryingIds.includes(row.id)"
            link
            type="warning"
            @click="handleRetry(row)"
          >
            重试
          </el-button>
          <el-button v-if="row.storeId" link type="primary" @click="goManagerRouting(row)">查看店长路由</el-button>
          <el-button link type="primary" @click="goReviewDetail(row.reviewId)">查看评价</el-button>
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
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import * as BookingReviewApi from '@/api/mall/booking/review'
import { ElMessageBox } from 'element-plus'

defineOptions({ name: 'BookingReviewNotifyOutboxIndex' })

const message = useMessage()
const route = useRoute()
const router = useRouter()
const loading = ref(false)
const summaryLoading = ref(false)
const total = ref(0)
const list = ref<BookingReviewApi.BookingReviewNotifyOutbox[]>([])
const retryingIds = ref<number[]>([])
const createEmptySummary = (): BookingReviewApi.BookingReviewNotifyOutboxSummary => ({
  totalReviewCount: 0,
  dualSentReviewCount: 0,
  blockedReviewCount: 0,
  failedReviewCount: 0,
  manualRetryPendingReviewCount: 0,
  divergedReviewCount: 0,
})
const auditSummary = ref<BookingReviewApi.BookingReviewNotifyOutboxSummary>(createEmptySummary())

const createDefaultQuery = (): BookingReviewApi.BookingReviewNotifyOutboxPageReq => ({
  pageNo: 1,
  pageSize: 10,
  reviewId: undefined,
  storeId: undefined,
  receiverRole: undefined,
  receiverUserId: undefined,
  receiverAccount: undefined,
  status: undefined,
  channel: undefined,
  notifyType: undefined,
  lastActionCode: undefined
})

const queryParams = reactive<BookingReviewApi.BookingReviewNotifyOutboxPageReq>(createDefaultQuery())

const buildSummaryQueryParams = (): BookingReviewApi.BookingReviewNotifyOutboxPageReq => ({
  pageNo: 1,
  pageSize: 1,
  reviewId: queryParams.reviewId,
  storeId: queryParams.storeId,
  receiverRole: queryParams.receiverRole,
  notifyType: queryParams.notifyType,
})

const applyRouteQuery = () => {
  const reviewId = Number(Array.isArray(route.query.reviewId) ? route.query.reviewId[0] : route.query.reviewId)
  if (Number.isFinite(reviewId) && reviewId > 0) {
    queryParams.reviewId = reviewId
  }
  const status = Array.isArray(route.query.status) ? route.query.status[0] : route.query.status
  if (status) {
    queryParams.status = String(status)
  }
  const lastActionCode = Array.isArray(route.query.lastActionCode) ? route.query.lastActionCode[0] : route.query.lastActionCode
  if (lastActionCode) {
    queryParams.lastActionCode = String(lastActionCode)
  }
}

const loadSummary = async () => {
  summaryLoading.value = true
  try {
    auditSummary.value =
      (await BookingReviewApi.getReviewNotifyOutboxSummary(buildSummaryQueryParams())) || createEmptySummary()
  } finally {
    summaryLoading.value = false
  }
}

const getList = async () => {
  loading.value = true
  try {
    const data = await BookingReviewApi.getReviewNotifyOutboxPage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

const auditCards = computed(() => [
  {
    label: '双通道均已派发',
    value: String(auditSummary.value.dualSentReviewCount || 0),
    detail: `当前 review 范围内共 ${auditSummary.value.totalReviewCount || 0} 条评价`
  },
  {
    label: '存在阻断',
    value: String(auditSummary.value.blockedReviewCount || 0),
    detail: '至少一个通道仍卡在店长路由或账号阻断'
  },
  {
    label: '存在失败',
    value: String(auditSummary.value.failedReviewCount || 0),
    detail: '至少一个通道发送失败，需先排查失败原因'
  },
  {
    label: '人工重试后待观察',
    value: String(auditSummary.value.manualRetryPendingReviewCount || 0),
    detail: '人工重试后仍需继续观察双通道最新状态'
  },
  {
    label: '跨通道状态分裂',
    value: String(auditSummary.value.divergedReviewCount || 0),
    detail: 'App / 企微当前最新状态不一致'
  },
])

const refreshPage = async () => {
  await Promise.all([getList(), loadSummary()])
}

const handleQuery = () => {
  queryParams.pageNo = 1
  refreshPage()
}

const applyQuickStatus = (status?: string) => {
  queryParams.status = status
  queryParams.lastActionCode = undefined
  handleQuery()
}

const applyQuickAction = (lastActionCode?: string) => {
  queryParams.lastActionCode = lastActionCode
  if (lastActionCode) {
    queryParams.status = undefined
  }
  handleQuery()
}

const resetQuery = () => {
  Object.assign(queryParams, createDefaultQuery())
  applyRouteQuery()
  handleQuery()
}

const goBackToLedger = () => {
  router.push('/mall/booking/review')
}

const goReviewDetail = (reviewId?: number) => {
  if (!reviewId) {
    return
  }
  router.push(`/mall/booking/review/detail?id=${reviewId}`)
}

const goManagerRouting = (row: BookingReviewApi.BookingReviewNotifyOutbox) => {
  if (!row?.storeId) {
    return
  }
  router.push(`/mall/booking/review/manager-routing?storeId=${row.storeId}&reviewId=${row.reviewId || ''}`)
}

const canRetry = (row: BookingReviewApi.BookingReviewNotifyOutbox) => row.manualRetryAllowed === true

const promptRetryReason = async () => {
  const { value } = await ElMessageBox.prompt('请输入重试原因（可选）', '通知重试', {
    confirmButtonText: '确认重试',
    cancelButtonText: '取消',
    inputPlaceholder: '例如：模板已恢复，人工触发重试',
    inputType: 'textarea',
    inputValue: 'manual-retry'
  })
  return value?.trim() || 'manual-retry'
}

const handleRetry = async (row: BookingReviewApi.BookingReviewNotifyOutbox) => {
  if (!row?.id) {
    return
  }
  try {
    const reason = await promptRetryReason()
    retryingIds.value.push(row.id)
    await BookingReviewApi.retryReviewNotifyOutbox({ ids: [row.id], reason })
    message.success('已重新入队')
    await refreshPage()
  } catch (error: any) {
    if (error !== 'cancel') {
      // 请求失败由全局错误拦截处理
    }
  } finally {
    retryingIds.value = retryingIds.value.filter((id) => id !== row.id)
  }
}

const notifyStatusText = (status?: string) => {
  if (status === 'PENDING') return '待派发'
  if (status === 'SENT') return '已派发'
  if (status === 'FAILED') return '发送失败'
  if (status === 'BLOCKED_NO_OWNER') return '缺店长路由阻断'
  return '-'
}

const notifyStatusTagType = (status?: string) => {
  if (status === 'PENDING') return 'warning'
  if (status === 'SENT') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'BLOCKED_NO_OWNER') return 'danger'
  return 'info'
}

watch(
  () => route.query,
  () => {
    Object.assign(queryParams, createDefaultQuery())
    applyRouteQuery()
    refreshPage()
  },
  { immediate: true },
)
</script>
