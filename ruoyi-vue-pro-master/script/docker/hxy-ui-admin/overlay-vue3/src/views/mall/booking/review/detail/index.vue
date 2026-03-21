<template>
  <ContentWrap>
    <div class="mb-12px flex items-center justify-between gap-12px">
      <div>
        <div class="text-18px font-600">预约服务评价详情</div>
        <div class="mt-4px text-13px text-[var(--el-text-color-secondary)]">
          面向低分评价回访、店长协同和运营复盘。
        </div>
      </div>
      <div class="flex gap-8px">
        <el-button @click="reload">
          <Icon class="mr-5px" icon="ep:refresh" />
          刷新
        </el-button>
        <el-button @click="goBack">
          <Icon class="mr-5px" icon="ep:back" />
          返回列表
        </el-button>
      </div>
    </div>
  </ContentWrap>

  <ContentWrap v-loading="loading">
    <el-alert
      v-if="!review.id"
      :closable="false"
      description="未获取到评价详情，请确认路由参数 id 是否正确。"
      title="评价不存在或参数缺失"
      type="warning"
    />

    <template v-else>
      <el-descriptions :column="2" border title="评价基础信息">
        <el-descriptions-item label="评价ID">{{ review.id }}</el-descriptions-item>
        <el-descriptions-item label="预约订单ID">{{ review.bookingOrderId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="服务履约单ID">{{ review.serviceOrderId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="门店 / ID">{{ readableEntityText(review.storeName, review.storeId) }}</el-descriptions-item>
        <el-descriptions-item label="技师 / ID">{{ readableEntityText(review.technicianName, review.technicianId) }}</el-descriptions-item>
        <el-descriptions-item label="会员 / ID">{{ readableEntityText(review.memberNickname, review.memberId) }}</el-descriptions-item>
        <el-descriptions-item label="服务SPU ID">{{ review.serviceSpuId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="服务SKU ID">{{ review.serviceSkuId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="总体评分">{{ review.overallScore || '-' }}</el-descriptions-item>
        <el-descriptions-item label="服务评分">{{ review.serviceScore || '-' }}</el-descriptions-item>
        <el-descriptions-item label="技师评分">{{ review.technicianScore || '-' }}</el-descriptions-item>
        <el-descriptions-item label="环境评分">{{ review.environmentScore || '-' }}</el-descriptions-item>
        <el-descriptions-item label="评价等级">
          <el-tag :type="reviewLevelTagType(review.reviewLevel)">{{ reviewLevelText(review.reviewLevel) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="风险等级">
          <el-tag :type="riskLevelTagType(review.riskLevel)">{{ riskLevelText(review.riskLevel) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="展示状态">
          <el-tag :type="displayStatusTagType(review.displayStatus)">{{ displayStatusText(review.displayStatus) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="跟进状态">
          <el-tag :type="followStatusTagType(review.followStatus)">{{ followStatusText(review.followStatus) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="回复状态">
          <el-tag :type="review.replyStatus ? 'success' : 'info'">{{ review.replyStatus ? '已回复' : '未回复' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="审核状态">{{ review.auditStatus ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="来源">{{ review.source || '-' }}</el-descriptions-item>
        <el-descriptions-item label="是否匿名">{{ review.anonymous ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="服务完成时间">{{ review.completedTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ review.submitTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="首次响应时间">{{ review.firstResponseAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="回复时间">{{ review.replyTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="回复人">{{ review.replyUserId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="跟进负责人">{{ review.followOwnerId || '-' }}</el-descriptions-item>
        <el-descriptions-item :span="2" label="评价内容">{{ review.content || '-' }}</el-descriptions-item>
        <el-descriptions-item :span="2" label="标签">
          <div v-if="review.tags?.length" class="flex flex-wrap gap-8px">
            <el-tag v-for="tag in review.tags" :key="tag" effect="plain">{{ tag }}</el-tag>
          </div>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item :span="2" label="评价图片">
          <div v-if="review.picUrls?.length" class="flex flex-wrap gap-12px">
            <el-image
              v-for="url in review.picUrls"
              :key="url"
              :preview-src-list="review.picUrls"
              :src="url"
              class="h-72px w-72px rounded-6px"
              fit="cover"
            />
          </div>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item :span="2" label="回复内容">{{ review.replyContent || '-' }}</el-descriptions-item>
        <el-descriptions-item :span="2" label="跟进结果">{{ review.followResult || '-' }}</el-descriptions-item>
      </el-descriptions>
    </template>
  </ContentWrap>

  <ContentWrap v-if="review.id">
    <el-card shadow="never" class="mb-16px">
      <template #header>
        <div class="flex flex-col gap-2">
          <div class="text-16px font-600">最近动作时间线</div>
          <div class="text-12px text-[var(--el-text-color-secondary)]">当前状态摘要与时间轴基于真实字段计算</div>
        </div>
      </template>

      <div class="mb-16px flex flex-wrap gap-12px">
        <div
          v-for="item in detailSummaryItems"
          :key="item.label"
          class="flex flex-col gap-4 px-12 py-8 border border-[var(--el-border-color)] rounded-6px bg-[var(--el-card-background-color)]"
        >
          <span class="text-11px text-[var(--el-text-color-secondary)]">{{ item.label }}</span>
          <span class="text-14px font-500">{{ item.value }}</span>
        </div>
      </div>

      <el-timeline align="left">
        <el-timeline-item
          v-for="item in detailTimelineItems"
          :key="item.label + item.time"
          :timestamp="item.time"
          placement="top"
        >
          <div class="el-timeline-right-content">
            <div class="font-500">{{ item.label }}</div>
            <div class="text-12px text-[var(--el-text-color-secondary)]">{{ item.description }}</div>
          </div>
        </el-timeline-item>
      </el-timeline>
    </el-card>

    <el-card shadow="never" class="mb-16px" v-loading="notifyOutboxLoading">
      <template #header>
        <div class="flex items-center justify-between gap-12px">
          <div class="flex flex-col gap-2">
            <div class="text-16px font-600">通知观测</div>
            <div class="text-12px text-[var(--el-text-color-secondary)]">只展示 notify outbox 真值，不代表门店店长账号已经收到了通知。</div>
          </div>
          <div class="flex gap-8px">
            <el-button plain type="warning" @click="goManagerRouting">
              查看店长路由
            </el-button>
            <el-button plain type="primary" @click="goNotifyOutbox">
              查看通知台账
            </el-button>
          </div>
        </div>
      </template>

      <el-alert
        :closable="false"
        class="mb-16px"
        description="差评提交成功后仅承诺立即生成通知意图；若没有稳定店长账号映射，会进入 BLOCKED_NO_OWNER，而不是伪发送成功。"
        title="notify outbox 口径"
        type="info"
      />

      <el-empty v-if="!notifyOutboxList.length" description="当前未生成通知意图记录" />

      <template v-else>
        <el-descriptions :column="2" border title="最新通知状态">
          <el-descriptions-item label="通知状态">
            <el-tag :type="notifyStatusTagType(latestNotifyOutbox?.status)">{{ notifyStatusText(latestNotifyOutbox?.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="诊断结论">{{ latestNotifyOutbox?.diagnosticLabel || '-' }}</el-descriptions-item>
          <el-descriptions-item label="接收角色">{{ latestNotifyOutbox?.receiverRole || '-' }}</el-descriptions-item>
          <el-descriptions-item label="接收账号ID">{{ latestNotifyOutbox?.receiverUserId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="通知渠道">{{ latestNotifyOutbox?.channel || '-' }}</el-descriptions-item>
          <el-descriptions-item label="通知类型">{{ latestNotifyOutbox?.notifyType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="重试次数">{{ latestNotifyOutbox?.retryCount ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="最近动作编码">{{ latestNotifyOutbox?.lastActionCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="最近动作时间">{{ latestNotifyOutbox?.lastActionTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="修复建议" :span="2">{{ latestNotifyOutbox?.repairHint || '-' }}</el-descriptions-item>
          <el-descriptions-item label="最近错误" :span="2">
            {{ notifyBlockReasonText(latestNotifyOutbox) }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="mt-16px">
          <el-table :data="notifyOutboxList" size="small">
            <el-table-column label="出站ID" prop="id" width="90" />
            <el-table-column label="状态" width="150">
              <template #default="{ row }">
                <el-tag :type="notifyStatusTagType(row.status)">{{ notifyStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="诊断结论" min-width="160" prop="diagnosticLabel" />
            <el-table-column label="接收账号ID" prop="receiverUserId" width="120" />
            <el-table-column label="渠道" prop="channel" width="110" />
            <el-table-column label="重试次数" prop="retryCount" width="100" />
            <el-table-column label="最近动作" prop="lastActionCode" width="160" />
            <el-table-column label="修复建议" min-width="220" prop="repairHint" show-overflow-tooltip />
            <el-table-column label="错误原因" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">
                {{ notifyBlockReasonText(row) }}
              </template>
            </el-table-column>
            <el-table-column label="最近动作时间" min-width="180" prop="lastActionTime" />
          </el-table>
        </div>
      </template>
    </el-card>

    <el-row :gutter="16">
      <el-col :lg="12" :md="24" :sm="24" :xs="24">
        <el-card shadow="never">
          <template #header>
            <div class="flex items-center justify-between">
              <span>回复评价</span>
              <el-tag :type="review.replyStatus ? 'success' : 'warning'">{{ review.replyStatus ? '已回复' : '待回复' }}</el-tag>
            </div>
          </template>
          <el-form label-width="90px">
            <el-form-item label="回复内容">
              <el-input
                v-model="replyForm.replyContent"
                :rows="5"
                maxlength="500"
                placeholder="请输入给用户的正式回复内容"
                show-word-limit
                type="textarea"
              />
            </el-form-item>
            <el-form-item>
              <el-button v-hasPermi="['booking:review:update']" :loading="replyLoading" type="primary" @click="submitReply">
                提交回复
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
      <el-col :lg="12" :md="24" :sm="24" :xs="24">
        <el-card shadow="never">
          <template #header>
            <div class="flex items-center justify-between">
              <span>跟进状态</span>
              <el-tag :type="followStatusTagType(review.followStatus)">{{ followStatusText(review.followStatus) }}</el-tag>
            </div>
          </template>
          <el-form label-width="90px">
            <el-form-item label="跟进状态">
              <el-select v-model="followForm.followStatus" class="!w-full" placeholder="请选择跟进状态">
                <el-option :value="0" label="无需跟进" />
                <el-option :value="1" label="待跟进" />
                <el-option :value="2" label="跟进中" />
                <el-option :value="3" label="已解决" />
                <el-option :value="4" label="已关闭" />
              </el-select>
            </el-form-item>
            <el-form-item label="跟进结果">
              <el-input
                v-model="followForm.followResult"
                :rows="5"
                maxlength="500"
                placeholder="请输入回访结论、补偿动作或协同说明"
                show-word-limit
                type="textarea"
              />
            </el-form-item>
            <el-form-item>
              <el-button
                v-hasPermi="['booking:review:update']"
                :loading="followLoading"
                type="warning"
                @click="submitFollowStatus"
              >
                更新跟进状态
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </ContentWrap>

  <ContentWrap v-if="review.id">
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between gap-12px">
          <div class="flex items-center gap-8px">
            <span>店长待办</span>
            <el-tag :type="managerTodoStatusTagType(review.managerTodoStatus, review.reviewLevel)">
              {{ managerTodoStatusText(review.managerTodoStatus, review.reviewLevel) }}
            </el-tag>
            <el-tag v-if="managerSlaStatusText(review) !== '-'" :type="managerSlaStatusTagType(review)">
              {{ managerSlaStatusText(review) }}
            </el-tag>
          </div>
          <span class="text-12px text-[var(--el-text-color-secondary)]">仅后台治理，不代表已自动通知店长</span>
        </div>
      </template>

      <el-alert
        :closable="false"
        class="mb-16px"
        description="第一版店长目标对象只认门店主数据 contactName/contactMobile；当前不承诺站内信、微信、短信或账号级路由。"
        title="店长待办真值"
        type="info"
      />

      <el-alert
        v-if="review.reviewLevel === 3 && !hasManagerTodo(review)"
        :closable="false"
        class="mb-16px"
        description="这是历史差评或未初始化记录；首次点击认领时，后端会补齐店长联系人快照与 SLA 截止时间。"
        title="待办初始化提示"
        type="warning"
      />

      <el-empty v-if="!showManagerTodoCard" description="当前评价未进入店长待办池" />

      <template v-else>
        <el-descriptions :column="2" border title="待办信息">
          <el-descriptions-item label="差评触发类型">{{ review.negativeTriggerType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="SLA状态">
            <el-tag :type="managerSlaStatusTagType(review)">{{ managerSlaStatusText(review) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="店长联系人">{{ review.managerContactName || '未核出联系人' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ review.managerContactMobile || '未核出手机号' }}</el-descriptions-item>
          <el-descriptions-item label="认领截止">{{ review.managerClaimDeadlineAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="首次处理截止">{{ review.managerFirstActionDeadlineAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="闭环截止">{{ review.managerCloseDeadlineAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="认领操作人">{{ review.managerClaimedByUserId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="认领时间">{{ review.managerClaimedAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="首次处理时间">{{ review.managerFirstActionAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="闭环时间">{{ review.managerClosedAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="最近处理人">{{ review.managerLatestActionByUserId || '-' }}</el-descriptions-item>
          <el-descriptions-item :span="2" label="最近处理备注">
            {{ review.managerLatestActionRemark || '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="mt-16px grid gap-16px lg:grid-cols-3">
          <el-card shadow="never">
            <template #header>
              <span>认领待办</span>
            </template>
            <div class="text-13px leading-22px text-[var(--el-text-color-secondary)]">
              差评触发后，后台运营或店长恢复负责人先认领，再进入首次处理与闭环动作。
            </div>
            <div class="mt-16px">
              <el-button
                v-hasPermi="['booking:review:update']"
                :disabled="!canClaimManagerTodo"
                :loading="managerTodoLoading"
                type="danger"
                @click="submitClaimManagerTodo"
              >
                认领店长待办
              </el-button>
            </div>
          </el-card>

          <el-card shadow="never">
            <template #header>
              <span>首次处理</span>
            </template>
            <el-input
              v-model="managerTodoForm.firstActionRemark"
              :rows="5"
              maxlength="500"
              placeholder="例如：已电话联系店长确认处理方案"
              show-word-limit
              type="textarea"
            />
            <div class="mt-16px">
              <el-button
                v-hasPermi="['booking:review:update']"
                :disabled="!canRecordFirstAction"
                :loading="managerTodoLoading"
                type="warning"
                @click="submitManagerTodoFirstAction"
              >
                记录首次处理
              </el-button>
            </div>
          </el-card>

          <el-card shadow="never">
            <template #header>
              <span>闭环收口</span>
            </template>
            <el-input
              v-model="managerTodoForm.closeRemark"
              :rows="5"
              maxlength="500"
              placeholder="例如：店长确认完成回访并闭环"
              show-word-limit
              type="textarea"
            />
            <div class="mt-16px">
              <el-button
                v-hasPermi="['booking:review:update']"
                :disabled="!canCloseManagerTodo"
                :loading="managerTodoLoading"
                type="success"
                @click="submitManagerTodoClose"
              >
                标记已闭环
              </el-button>
            </div>
          </el-card>
        </div>
      </template>
    </el-card>
  </ContentWrap>
</template>

<script lang="ts" setup>
import * as BookingReviewApi from '@/api/mall/booking/review'
import { buildReviewDetailTimeline } from './timelineHelpers.mjs'

defineOptions({ name: 'BookingReviewDetail' })

const route = useRoute()
const router = useRouter()
const message = useMessage()
const loading = ref(false)
const replyLoading = ref(false)
const followLoading = ref(false)
const managerTodoLoading = ref(false)
const notifyOutboxLoading = ref(false)
const review = ref<BookingReviewApi.BookingReview>({} as BookingReviewApi.BookingReview)
const notifyOutboxList = ref<BookingReviewApi.BookingReviewNotifyOutbox[]>([])

const replyForm = reactive<BookingReviewApi.BookingReviewReplyReq>({
  reviewId: 0,
  replyContent: ''
})

const followForm = reactive<BookingReviewApi.BookingReviewFollowUpdateReq>({
  reviewId: 0,
  followStatus: 1,
  followResult: ''
})

const managerTodoForm = reactive({
  firstActionRemark: '',
  closeRemark: ''
})

const reviewId = computed(() => {
  const value = Array.isArray(route.query.id) ? route.query.id[0] : route.query.id
  const id = Number(value)
  return Number.isFinite(id) && id > 0 ? id : 0
})

const hasManagerTodo = (data?: BookingReviewApi.BookingReview) => {
  return data?.managerTodoStatus !== undefined && data?.managerTodoStatus !== null
}

const parseTime = (value?: string) => {
  if (!value) {
    return NaN
  }
  return new Date(value.replace(/-/g, '/')).getTime()
}

const managerSlaStatusValue = (data: BookingReviewApi.BookingReview) => {
  if (!hasManagerTodo(data)) {
    return data.reviewLevel === 3 ? 'PENDING_INIT' : ''
  }
  if (data.managerTodoStatus === 4) {
    return 'NORMAL'
  }
  const now = Date.now()
  const closeDeadline = parseTime(data.managerCloseDeadlineAt)
  const firstActionDeadline = parseTime(data.managerFirstActionDeadlineAt)
  const claimDeadline = parseTime(data.managerClaimDeadlineAt)
  if (!Number.isNaN(closeDeadline) && now > closeDeadline) {
    return 'CLOSE_TIMEOUT'
  }
  if (!data.managerFirstActionAt && !Number.isNaN(firstActionDeadline) && now > firstActionDeadline) {
    return 'FIRST_ACTION_TIMEOUT'
  }
  if (!data.managerClaimedAt && !Number.isNaN(claimDeadline) && now > claimDeadline) {
    return 'CLAIM_TIMEOUT'
  }
  return 'NORMAL'
}

const showManagerTodoCard = computed(() => {
  return Boolean(review.value.id) && (review.value.reviewLevel === 3 || hasManagerTodo(review.value))
})

const canClaimManagerTodo = computed(() => {
  if (review.value.reviewLevel !== 3) {
    return false
  }
  return review.value.managerTodoStatus === undefined || review.value.managerTodoStatus === null || review.value.managerTodoStatus === 1
})

const canRecordFirstAction = computed(() => {
  return review.value.managerTodoStatus === 2 || review.value.managerTodoStatus === 3
})

const canCloseManagerTodo = computed(() => {
  return review.value.managerTodoStatus === 2 || review.value.managerTodoStatus === 3
})

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

const loadDetail = async () => {
  if (!reviewId.value) {
    review.value = {} as BookingReviewApi.BookingReview
    notifyOutboxList.value = []
    replyForm.reviewId = 0
    followForm.reviewId = 0
    return
  }
  loading.value = true
  try {
    const [data, notifyList] = await Promise.all([
      BookingReviewApi.getReview(reviewId.value),
      loadNotifyOutbox()
    ])
    review.value = data || ({} as BookingReviewApi.BookingReview)
    notifyOutboxList.value = notifyList
    replyForm.reviewId = reviewId.value
    replyForm.replyContent = data?.replyContent || ''
    followForm.reviewId = reviewId.value
    followForm.followStatus = data?.followStatus ?? 1
    followForm.followResult = data?.followResult || ''
    managerTodoForm.firstActionRemark = ''
    managerTodoForm.closeRemark = ''
  } finally {
    loading.value = false
  }
}

const loadNotifyOutbox = async () => {
  if (!reviewId.value) {
    return []
  }
  notifyOutboxLoading.value = true
  try {
    return (await BookingReviewApi.getReviewNotifyOutboxList({
      reviewId: reviewId.value,
      limit: 5
    })) || []
  } finally {
    notifyOutboxLoading.value = false
  }
}

const reload = () => {
  loadDetail()
}

const reviewTimeline = computed(() => buildReviewDetailTimeline(review.value))
const detailSummaryItems = computed(() => reviewTimeline.value.summaryItems)
const detailTimelineItems = computed(() => reviewTimeline.value.timelineItems)
const latestNotifyOutbox = computed(() => notifyOutboxList.value[0])

const goBack = () => {
  router.push('/mall/booking/review')
}

const goNotifyOutbox = () => {
  if (!reviewId.value) {
    return
  }
  router.push(`/mall/booking/review/notify-outbox?reviewId=${reviewId.value}`)
}

const goManagerRouting = () => {
  if (!review.value.storeId) {
    return
  }
  router.push(`/mall/booking/review/manager-routing?storeId=${review.value.storeId}&reviewId=${reviewId.value}`)
}

const submitReply = async () => {
  if (!replyForm.reviewId) {
    message.warning('缺少评价ID，无法提交回复')
    return
  }
  if (!replyForm.replyContent.trim()) {
    message.warning('请输入回复内容')
    return
  }
  await message.confirm('确认提交当前回复内容吗？')
  replyLoading.value = true
  try {
    await BookingReviewApi.replyReview({
      reviewId: replyForm.reviewId,
      replyContent: replyForm.replyContent.trim()
    })
    message.success('回复成功')
    await loadDetail()
  } finally {
    replyLoading.value = false
  }
}

const submitFollowStatus = async () => {
  if (!followForm.reviewId) {
    message.warning('缺少评价ID，无法更新跟进状态')
    return
  }
  if (followForm.followStatus === undefined || followForm.followStatus === null) {
    message.warning('请选择跟进状态')
    return
  }
  await message.confirm('确认更新当前跟进状态吗？')
  followLoading.value = true
  try {
    await BookingReviewApi.updateReviewFollowStatus({
      reviewId: followForm.reviewId,
      followStatus: followForm.followStatus,
      followResult: followForm.followResult?.trim() || undefined
    })
    message.success('跟进状态已更新')
    await loadDetail()
  } finally {
    followLoading.value = false
  }
}

const submitClaimManagerTodo = async () => {
  if (!reviewId.value || !canClaimManagerTodo.value) {
    return
  }
  await message.confirm('确认认领当前店长待办吗？')
  managerTodoLoading.value = true
  try {
    await BookingReviewApi.claimManagerTodo({ reviewId: reviewId.value })
    message.success('店长待办已认领')
    await loadDetail()
  } finally {
    managerTodoLoading.value = false
  }
}

const submitManagerTodoFirstAction = async () => {
  const remark = managerTodoForm.firstActionRemark.trim()
  if (!reviewId.value || !canRecordFirstAction.value) {
    return
  }
  if (!remark) {
    message.warning('请输入首次处理说明')
    return
  }
  await message.confirm('确认记录当前首次处理动作吗？')
  managerTodoLoading.value = true
  try {
    await BookingReviewApi.recordManagerTodoFirstAction({
      reviewId: reviewId.value,
      remark
    })
    message.success('首次处理已记录')
    await loadDetail()
  } finally {
    managerTodoLoading.value = false
  }
}

const submitManagerTodoClose = async () => {
  const remark = managerTodoForm.closeRemark.trim()
  if (!reviewId.value || !canCloseManagerTodo.value) {
    return
  }
  if (!remark) {
    message.warning('请输入闭环说明')
    return
  }
  await message.confirm('确认将当前店长待办标记为已闭环吗？')
  managerTodoLoading.value = true
  try {
    await BookingReviewApi.closeManagerTodo({
      reviewId: reviewId.value,
      remark
    })
    message.success('店长待办已闭环')
    await loadDetail()
  } finally {
    managerTodoLoading.value = false
  }
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

const displayStatusText = (value?: number) => {
  if (value === 0) return '可展示'
  if (value === 1) return '已隐藏'
  if (value === 2) return '待审核'
  return '-'
}

const displayStatusTagType = (value?: number) => {
  if (value === 0) return 'success'
  if (value === 1) return 'info'
  if (value === 2) return 'warning'
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

const managerTodoStatusText = (status?: number, reviewLevel?: number) => {
  if (status === 1) return '待认领'
  if (status === 2) return '已认领'
  if (status === 3) return '处理中'
  if (status === 4) return '已闭环'
  if (reviewLevel === 3) return '待初始化'
  return '-'
}

const managerTodoStatusTagType = (status?: number, reviewLevel?: number) => {
  if (status === 1) return 'danger'
  if (status === 2) return 'warning'
  if (status === 3) return 'primary'
  if (status === 4) return 'success'
  if (reviewLevel === 3) return 'warning'
  return 'info'
}

const notifyStatusText = (status?: string) => {
  if (status === 'PENDING') return '待派发'
  if (status === 'SENT') return '已发送'
  if (status === 'FAILED') return '发送失败'
  if (status === 'BLOCKED_NO_OWNER') return 'BLOCKED_NO_OWNER'
  return '-'
}

const notifyStatusTagType = (status?: string) => {
  if (status === 'PENDING') return 'warning'
  if (status === 'SENT') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'BLOCKED_NO_OWNER') return 'danger'
  return 'info'
}

const notifyBlockReasonText = (outbox?: BookingReviewApi.BookingReviewNotifyOutbox) => {
  if (!outbox) {
    return '-'
  }
  if (outbox.diagnosticDetail) {
    return outbox.diagnosticDetail
  }
  if (outbox.status === 'BLOCKED_NO_OWNER') {
    return 'BLOCKED_NO_OWNER：缺门店店长账号绑定'
  }
  return outbox.lastErrorMsg || '-'
}

const managerSlaStatusText = (data: BookingReviewApi.BookingReview) => {
  const value = managerSlaStatusValue(data)
  if (value === 'NORMAL') return '正常'
  if (value === 'CLAIM_TIMEOUT') return '认领超时'
  if (value === 'FIRST_ACTION_TIMEOUT') return '首次处理超时'
  if (value === 'CLOSE_TIMEOUT') return '闭环超时'
  if (value === 'PENDING_INIT') return '待初始化'
  return '-'
}

const managerSlaStatusTagType = (data: BookingReviewApi.BookingReview) => {
  const value = managerSlaStatusValue(data)
  if (value === 'NORMAL') return 'success'
  if (value === 'CLAIM_TIMEOUT') return 'warning'
  if (value === 'FIRST_ACTION_TIMEOUT') return 'warning'
  if (value === 'CLOSE_TIMEOUT') return 'danger'
  if (value === 'PENDING_INIT') return 'warning'
  return 'info'
}

watch(
  () => route.query.id,
  () => {
    loadDetail()
  },
  { immediate: true }
)
</script>
