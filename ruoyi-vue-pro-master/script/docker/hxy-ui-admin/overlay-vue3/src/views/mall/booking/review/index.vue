<template>
  <doc-alert title="预约服务评价运营台账" url="https://doc.iocoder.cn/" />

  <ContentWrap>
    <el-alert
      :closable="false"
      description="当前“店长待办”仅是后台治理台账，不代表系统已经自动通知店长，也不代表 booking review 已可放量。"
      title="后台待办真值说明"
      type="info"
    />
  </ContentWrap>

  <ContentWrap>
    <el-form :inline="true" :model="queryParams" class="-mb-15px" label-width="120px">
      <el-form-item label="评价ID" prop="id">
        <el-input v-model="queryParams.id" class="!w-160px" clearable placeholder="请输入评价ID" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="预约订单ID" prop="bookingOrderId">
        <el-input
          v-model="queryParams.bookingOrderId"
          class="!w-180px"
          clearable
          placeholder="请输入预约订单ID"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="门店ID" prop="storeId">
        <el-input v-model="queryParams.storeId" class="!w-160px" clearable placeholder="请输入门店ID" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="技师ID" prop="technicianId">
        <el-input
          v-model="queryParams.technicianId"
          class="!w-160px"
          clearable
          placeholder="请输入技师ID"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="会员ID" prop="memberId">
        <el-input
          v-model="queryParams.memberId"
          class="!w-160px"
          clearable
          placeholder="请输入会员ID"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="评价等级" prop="reviewLevel">
        <el-select v-model="queryParams.reviewLevel" class="!w-160px" clearable placeholder="全部">
          <el-option :value="1" label="好评" />
          <el-option :value="2" label="中评" />
          <el-option :value="3" label="差评" />
        </el-select>
      </el-form-item>
      <el-form-item label="风险等级" prop="riskLevel">
        <el-select v-model="queryParams.riskLevel" class="!w-160px" clearable placeholder="全部">
          <el-option :value="0" label="正常" />
          <el-option :value="1" label="关注" />
          <el-option :value="2" label="紧急" />
        </el-select>
      </el-form-item>
      <el-form-item label="跟进状态" prop="followStatus">
        <el-select v-model="queryParams.followStatus" class="!w-160px" clearable placeholder="全部">
          <el-option :value="0" label="无需跟进" />
          <el-option :value="1" label="待跟进" />
          <el-option :value="2" label="跟进中" />
          <el-option :value="3" label="已解决" />
          <el-option :value="4" label="已关闭" />
        </el-select>
      </el-form-item>
      <el-form-item label="历史未初始化差评" prop="onlyPendingInit">
        <el-select v-model="queryParams.onlyPendingInit" class="!w-180px" clearable placeholder="全部">
          <el-option :value="true" label="是" />
        </el-select>
      </el-form-item>
      <el-form-item label="只看店长待办" prop="onlyManagerTodo">
        <el-select v-model="queryParams.onlyManagerTodo" class="!w-160px" clearable placeholder="全部">
          <el-option :value="true" label="是" />
        </el-select>
      </el-form-item>
      <el-form-item label="待办状态" prop="managerTodoStatus">
        <el-select v-model="queryParams.managerTodoStatus" class="!w-160px" clearable placeholder="全部">
          <el-option :value="1" label="待认领" />
          <el-option :value="2" label="已认领" />
          <el-option :value="3" label="处理中" />
          <el-option :value="4" label="已闭环" />
        </el-select>
      </el-form-item>
      <el-form-item label="SLA状态" prop="managerSlaStatus">
        <el-select v-model="queryParams.managerSlaStatus" class="!w-180px" clearable placeholder="全部">
          <el-option label="正常" value="NORMAL" />
          <el-option label="即将认领超时" value="CLAIM_DUE_SOON" />
          <el-option label="认领超时" value="CLAIM_TIMEOUT" />
          <el-option label="即将首次处理超时" value="FIRST_ACTION_DUE_SOON" />
          <el-option label="首次处理超时" value="FIRST_ACTION_TIMEOUT" />
          <el-option label="即将闭环超时" value="CLOSE_DUE_SOON" />
          <el-option label="闭环超时" value="CLOSE_TIMEOUT" />
          <el-option label="已闭环" value="CLOSED" />
        </el-select>
      </el-form-item>
      <el-form-item label="回复状态" prop="replyStatus">
        <el-select v-model="queryParams.replyStatus" class="!w-160px" clearable placeholder="全部">
          <el-option :value="true" label="已回复" />
          <el-option :value="false" label="未回复" />
        </el-select>
      </el-form-item>
      <el-form-item label="提交时间" prop="submitTime">
        <el-date-picker
          v-model="queryParams.submitTime"
          :default-time="[new Date('1 00:00:00'), new Date('1 23:59:59')]"
          class="!w-360px"
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
        <el-button v-hasPermi="['booking:review:query']" plain type="primary" @click="goDashboard">
          <Icon class="mr-5px" icon="ep:data-analysis" />
          看板汇总
        </el-button>
        <el-button v-hasPermi="['booking:review:query']" plain @click="goHistoryScan">
          <Icon class="mr-5px" icon="ep:document-search" />
          历史治理扫描
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap v-if="queryParams.onlyPendingInit === true">
    <el-alert
      :closable="false"
      description="这类记录属于历史差评且尚未初始化店长待办；系统不会在查询时自动补齐，只有首次认领、首次处理、闭环等写动作才会触发初始化。"
      title="历史未初始化差评说明"
      type="warning"
    />
  </ContentWrap>

  <ContentWrap>
    <div class="mb-12px text-13px text-[var(--el-text-color-secondary)]">SLA 提醒：认领超时提醒、首次处理超时提醒、闭环超时提醒会继续走 App / 企微双通道，但当前仍是 admin-only 治理视角。</div>
    <div class="mb-16px flex flex-wrap gap-8px">
      <el-button plain type="danger" @click="applyQuickFilter('todoPending')">待认领优先</el-button>
      <el-button plain type="warning" @click="applyQuickFilter('claimDueSoon')">即将认领超时</el-button>
      <el-button plain type="warning" @click="applyQuickFilter('claimTimeout')">认领超时</el-button>
      <el-button plain type="warning" @click="applyQuickFilter('firstActionDueSoon')">即将首次处理超时</el-button>
      <el-button plain type="warning" @click="applyQuickFilter('firstActionTimeout')">首次处理超时</el-button>
      <el-button plain type="danger" @click="applyQuickFilter('closeDueSoon')">即将闭环超时</el-button>
      <el-button plain type="danger" @click="applyQuickFilter('closeTimeout')">闭环超时</el-button>
      <el-button plain @click="applyQuickFilter('pendingInit')">历史待初始化</el-button>
      <el-button plain @click="applyQuickFilter()">查看全部</el-button>
    </div>

    <el-table v-loading="loading" :data="list">
      <el-table-column label="评价ID" prop="id" width="90" />
      <el-table-column label="预约订单ID" prop="bookingOrderId" width="120" />
      <el-table-column label="门店 / ID" min-width="160">
        <template #default="{ row }">
          <div>{{ readableEntityText(row.storeName, row.storeId) }}</div>
        </template>
      </el-table-column>
      <el-table-column label="技师 / ID" min-width="160">
        <template #default="{ row }">
          <div>{{ readableEntityText(row.technicianName, row.technicianId) }}</div>
        </template>
      </el-table-column>
      <el-table-column label="会员 / ID" min-width="160">
        <template #default="{ row }">
          <div>{{ readableEntityText(row.memberNickname, row.memberId) }}</div>
        </template>
      </el-table-column>
      <el-table-column label="总体评分" prop="overallScore" width="90" />
      <el-table-column label="评价等级" width="100">
        <template #default="{ row }">
          <el-tag :type="reviewLevelTagType(row.reviewLevel)">{{ reviewLevelText(row.reviewLevel) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="风险等级" width="100">
        <template #default="{ row }">
          <el-tag :type="riskLevelTagType(row.riskLevel)">{{ riskLevelText(row.riskLevel) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="跟进状态" width="110">
        <template #default="{ row }">
          <el-tag :type="followStatusTagType(row.followStatus)">{{ followStatusText(row.followStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="店长待办" width="120">
        <template #default="{ row }">
          <el-tag :type="managerTodoStatusTagType(row)">{{ managerTodoStatusText(row) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="SLA状态" width="130">
        <template #default="{ row }">
          <el-tag :type="managerSlaStatusTagType(row)">{{ managerSlaStatusText(row) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="优先级" width="100">
        <template #default="{ row }">
          <el-tag :type="priorityLevelTagType(row.priorityLevel)">{{ row.priorityLevel || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="优先级原因" min-width="180" prop="priorityReason" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.priorityReason || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="通知风险" min-width="160" prop="notifyRiskSummary" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.notifyRiskSummary || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="店长联系人" min-width="170">
        <template #default="{ row }">
          <div>{{ row.managerContactName || '-' }}</div>
          <div class="text-[12px] text-[var(--el-text-color-secondary)]">{{ row.managerContactMobile || '-' }}</div>
        </template>
      </el-table-column>
      <el-table-column :formatter="dateFormatter" label="认领截止" prop="managerClaimDeadlineAt" width="180" />
      <el-table-column :formatter="dateFormatter" label="首次处理截止" prop="managerFirstActionDeadlineAt" width="180" />
      <el-table-column :formatter="dateFormatter" label="闭环截止" prop="managerCloseDeadlineAt" width="180" />
      <el-table-column label="回复状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.replyStatus ? 'success' : 'info'">{{ row.replyStatus ? '已回复' : '未回复' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="评价内容" min-width="260" prop="content" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.content || '-' }}
        </template>
      </el-table-column>
      <el-table-column :formatter="dateFormatter" label="提交时间" prop="submitTime" width="180" />
      <el-table-column :formatter="dateFormatter" label="回复时间" prop="replyTime" width="180" />
      <el-table-column align="center" fixed="right" label="操作" width="280">
        <template #default="{ row }">
          <el-button
            v-if="canQuickClaim(row)"
            v-hasPermi="['booking:review:update']"
            :loading="actingIds.includes(row.id)"
            link
            type="danger"
            @click="handleClaimManagerTodo(row)"
          >
            快速认领
          </el-button>
          <el-button
            v-if="canQuickRecordFirstAction(row)"
            v-hasPermi="['booking:review:update']"
            :loading="actingIds.includes(row.id)"
            link
            type="warning"
            @click="handleRecordFirstAction(row)"
          >
            记录首次处理
          </el-button>
          <el-button
            v-if="canQuickClose(row)"
            v-hasPermi="['booking:review:update']"
            :loading="actingIds.includes(row.id)"
            link
            type="success"
            @click="handleCloseManagerTodo(row)"
          >
            标记闭环
          </el-button>
          <el-button v-hasPermi="['booking:review:query']" link type="primary" @click="openDetail(row.id)">
            详情
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
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import * as BookingReviewApi from '@/api/mall/booking/review'
import { createDefaultLedgerQuery, parseLedgerQuery } from './queryHelpers.mjs'
import { ElMessageBox } from 'element-plus'

defineOptions({ name: 'BookingReviewIndex' })

const route = useRoute()
const router = useRouter()
const message = useMessage()
const loading = ref(false)
const total = ref(0)
const list = ref<BookingReviewApi.BookingReview[]>([])
const actingIds = ref<number[]>([])

const QUICK_FILTER_MAP = {
  todoPending: { onlyManagerTodo: true, managerTodoStatus: 1 },
  claimDueSoon: { onlyManagerTodo: true, managerSlaStatus: 'CLAIM_DUE_SOON' },
  claimTimeout: { onlyManagerTodo: true, managerSlaStatus: 'CLAIM_TIMEOUT' },
  firstActionDueSoon: { onlyManagerTodo: true, managerSlaStatus: 'FIRST_ACTION_DUE_SOON' },
  firstActionTimeout: { onlyManagerTodo: true, managerSlaStatus: 'FIRST_ACTION_TIMEOUT' },
  closeDueSoon: { onlyManagerTodo: true, managerSlaStatus: 'CLOSE_DUE_SOON' },
  closeTimeout: { onlyManagerTodo: true, managerSlaStatus: 'CLOSE_TIMEOUT' },
  pendingInit: { onlyPendingInit: true, reviewLevel: 3 }
} as const

const queryParams = reactive<BookingReviewApi.BookingReviewPageReq>(
  createDefaultLedgerQuery() as BookingReviewApi.BookingReviewPageReq,
)

const getList = async () => {
  loading.value = true
  try {
    const data = await BookingReviewApi.getReviewPage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  Object.assign(queryParams, createDefaultLedgerQuery())
  getList()
}

const applyQuickFilter = (presetKey?: keyof typeof QUICK_FILTER_MAP) => {
  Object.assign(queryParams, createDefaultLedgerQuery(), presetKey ? QUICK_FILTER_MAP[presetKey] : {})
  handleQuery()
}

const openDetail = (id: number) => {
  router.push({ path: '/mall/booking/review/detail', query: { id: String(id) } })
}

const goDashboard = () => {
  router.push('/mall/booking/review/dashboard')
}

const goHistoryScan = () => {
  router.push('/mall/booking/review/history-scan')
}

const readableEntityText = (name?: string, id?: number) => {
  if (name && id !== undefined && id !== null) {
    return `${name} (ID: ${id})`
  }
  if (name) {
    return name
  }
  if (id !== undefined && id !== null) {
    return `ID: ${id}`
  }
  return '-'
}

const hasManagerTodo = (row?: BookingReviewApi.BookingReview) => {
  return row?.managerTodoStatus !== undefined && row?.managerTodoStatus !== null
}

const canQuickClaim = (row: BookingReviewApi.BookingReview) => {
  return !!row?.id && row.reviewLevel === 3
    && (row.managerTodoStatus === undefined || row.managerTodoStatus === null || row.managerTodoStatus === 1)
}

const canQuickRecordFirstAction = (row: BookingReviewApi.BookingReview) => {
  return !!row?.id && (row.managerTodoStatus === 2 || row.managerTodoStatus === 3)
}

const canQuickClose = (row: BookingReviewApi.BookingReview) => {
  return !!row?.id && (row.managerTodoStatus === 2 || row.managerTodoStatus === 3)
}

const markActing = (reviewId?: number) => {
  if (!reviewId || actingIds.value.includes(reviewId)) {
    return false
  }
  actingIds.value.push(reviewId)
  return true
}

const unmarkActing = (reviewId?: number) => {
  actingIds.value = actingIds.value.filter((id) => id !== reviewId)
}

const promptActionRemark = async (title: string, inputPlaceholder: string, defaultValue = '') => {
  const { value } = await ElMessageBox.prompt(`请输入${title}说明`, title, {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    inputPlaceholder,
    inputType: 'textarea',
    inputValue: defaultValue
  })
  return value?.trim() || ''
}

const handleClaimManagerTodo = async (row: BookingReviewApi.BookingReview) => {
  if (!canQuickClaim(row) || !markActing(row.id)) {
    return
  }
  try {
    await message.confirm('确认认领当前店长待办吗？')
    await BookingReviewApi.claimManagerTodo({ reviewId: row.id! })
    message.success('店长待办已认领')
    await getList()
  } catch (error: any) {
    if (error !== 'cancel') {
      // 请求失败由全局错误拦截处理
    }
  } finally {
    unmarkActing(row.id)
  }
}

const handleRecordFirstAction = async (row: BookingReviewApi.BookingReview) => {
  if (!canQuickRecordFirstAction(row) || !markActing(row.id)) {
    return
  }
  try {
    const remark = await promptActionRemark('首次处理', '例如：已电话联系店长确认处理方案')
    if (!remark) {
      message.warning('请输入首次处理说明')
      return
    }
    await message.confirm('确认记录当前首次处理动作吗？')
    await BookingReviewApi.recordManagerTodoFirstAction({ reviewId: row.id!, remark })
    message.success('首次处理已记录')
    await getList()
  } catch (error: any) {
    if (error !== 'cancel') {
      // 请求失败由全局错误拦截处理
    }
  } finally {
    unmarkActing(row.id)
  }
}

const handleCloseManagerTodo = async (row: BookingReviewApi.BookingReview) => {
  if (!canQuickClose(row) || !markActing(row.id)) {
    return
  }
  try {
    const remark = await promptActionRemark('闭环', '例如：店长确认完成回访并闭环')
    if (!remark) {
      message.warning('请输入闭环说明')
      return
    }
    await message.confirm('确认将当前店长待办标记为已闭环吗？')
    await BookingReviewApi.closeManagerTodo({ reviewId: row.id!, remark })
    message.success('店长待办已闭环')
    await getList()
  } catch (error: any) {
    if (error !== 'cancel') {
      // 请求失败由全局错误拦截处理
    }
  } finally {
    unmarkActing(row.id)
  }
}

const parseTime = (value?: string) => {
  if (!value) {
    return NaN
  }
  return new Date(value.replace(/-/g, '/')).getTime()
}

const managerSlaStatusValue = (row: BookingReviewApi.BookingReview) => {
  if (row?.managerSlaStage) {
    return row.managerSlaStage
  }
  if (!hasManagerTodo(row)) {
    return row.reviewLevel === 3 ? 'PENDING_INIT' : ''
  }
  if (row.managerTodoStatus === 4) {
    return 'NORMAL'
  }
  const now = Date.now()
  const closeDeadline = parseTime(row.managerCloseDeadlineAt)
  const firstActionDeadline = parseTime(row.managerFirstActionDeadlineAt)
  const claimDeadline = parseTime(row.managerClaimDeadlineAt)
  if (!Number.isNaN(closeDeadline) && now > closeDeadline) {
    return 'CLOSE_TIMEOUT'
  }
  if (!row.managerFirstActionAt && !Number.isNaN(firstActionDeadline) && now > firstActionDeadline) {
    return 'FIRST_ACTION_TIMEOUT'
  }
  if (!row.managerClaimedAt && !Number.isNaN(claimDeadline) && now > claimDeadline) {
    return 'CLAIM_TIMEOUT'
  }
  if (!Number.isNaN(closeDeadline) && closeDeadline >= now && closeDeadline - now <= 120 * 60 * 1000) {
    return 'CLOSE_DUE_SOON'
  }
  if (!row.managerFirstActionAt && !Number.isNaN(firstActionDeadline)
    && firstActionDeadline >= now && firstActionDeadline - now <= 10 * 60 * 1000) {
    return 'FIRST_ACTION_DUE_SOON'
  }
  if (!row.managerClaimedAt && !Number.isNaN(claimDeadline)
    && claimDeadline >= now && claimDeadline - now <= 5 * 60 * 1000) {
    return 'CLAIM_DUE_SOON'
  }
  return 'NORMAL'
}

const reviewLevelText = (value?: number) => {
  if (value === 1) return '好评'
  if (value === 2) return '中评'
  if (value === 3) return '差评'
  return '-'
}

const reviewLevelTagType = (value?: number) => {
  if (value === 1) return 'success'
  if (value === 2) return 'warning'
  if (value === 3) return 'danger'
  return 'info'
}

const riskLevelText = (value?: number) => {
  if (value === 2) return '紧急'
  if (value === 1) return '关注'
  if (value === 0) return '正常'
  return '-'
}

const riskLevelTagType = (value?: number) => {
  if (value === 2) return 'danger'
  if (value === 1) return 'warning'
  if (value === 0) return 'success'
  return 'info'
}

const followStatusText = (value?: number) => {
  if (value === 0) return '无需跟进'
  if (value === 1) return '待跟进'
  if (value === 2) return '跟进中'
  if (value === 3) return '已解决'
  if (value === 4) return '已关闭'
  return '-'
}

const followStatusTagType = (value?: number) => {
  if (value === 1) return 'danger'
  if (value === 2) return 'warning'
  if (value === 3) return 'success'
  if (value === 4) return 'info'
  return ''
}

const managerTodoStatusText = (row: BookingReviewApi.BookingReview) => {
  if (row.managerTodoStatus === 1) return '待认领'
  if (row.managerTodoStatus === 2) return '已认领'
  if (row.managerTodoStatus === 3) return '处理中'
  if (row.managerTodoStatus === 4) return '已闭环'
  if (row.reviewLevel === 3) return '待初始化'
  return '-'
}

const managerTodoStatusTagType = (row: BookingReviewApi.BookingReview) => {
  if (row.managerTodoStatus === 1) return 'danger'
  if (row.managerTodoStatus === 2) return 'warning'
  if (row.managerTodoStatus === 3) return 'primary'
  if (row.managerTodoStatus === 4) return 'success'
  if (row.reviewLevel === 3) return 'warning'
  return 'info'
}

const managerSlaStatusText = (row: BookingReviewApi.BookingReview) => {
  const value = managerSlaStatusValue(row)
  if (value === 'NORMAL') return '正常'
  if (value === 'CLAIM_DUE_SOON') return '即将认领超时'
  if (value === 'CLAIM_TIMEOUT') return '认领超时'
  if (value === 'FIRST_ACTION_DUE_SOON') return '即将首次处理超时'
  if (value === 'FIRST_ACTION_TIMEOUT') return '首次处理超时'
  if (value === 'CLOSE_DUE_SOON') return '即将闭环超时'
  if (value === 'CLOSE_TIMEOUT') return '闭环超时'
  if (value === 'CLOSED') return '已闭环'
  if (value === 'PENDING_INIT') return '待初始化'
  return '-'
}

const managerSlaStatusTagType = (row: BookingReviewApi.BookingReview) => {
  const value = managerSlaStatusValue(row)
  if (value === 'NORMAL') return 'success'
  if (value === 'CLAIM_DUE_SOON') return 'warning'
  if (value === 'CLAIM_TIMEOUT') return 'warning'
  if (value === 'FIRST_ACTION_DUE_SOON') return 'warning'
  if (value === 'FIRST_ACTION_TIMEOUT') return 'warning'
  if (value === 'CLOSE_DUE_SOON') return 'danger'
  if (value === 'CLOSE_TIMEOUT') return 'danger'
  if (value === 'CLOSED') return 'success'
  if (value === 'PENDING_INIT') return 'warning'
  return 'info'
}

const priorityLevelTagType = (value?: string) => {
  if (value === 'P0') return 'danger'
  if (value === 'P1') return 'warning'
  if (value === 'P2') return 'primary'
  if (value === 'P3') return 'info'
  return 'info'
}

onMounted(() => {
  Object.assign(queryParams, parseLedgerQuery(route.query))
  getList()
})
</script>
