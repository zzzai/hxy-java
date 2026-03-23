<template>
  <ContentWrap>
    <div class="mb-12px flex items-center justify-between gap-12px">
      <div>
        <div class="text-18px font-600">预约服务评价看板</div>
        <div class="mt-4px text-13px text-[var(--el-text-color-secondary)]">
          聚合关注差评、待处理回访和店长待办 SLA。
        </div>
      </div>
      <div class="flex gap-8px">
        <el-button :loading="loading" @click="getSummary">
          <Icon class="mr-5px" icon="ep:refresh" />
          刷新
        </el-button>
        <el-button @click="goLedger">
          <Icon class="mr-5px" icon="ep:tickets" />
          返回台账
        </el-button>
      </div>
    </div>
  </ContentWrap>

  <ContentWrap>
    <el-alert
      :closable="false"
      description="当前看板只使用 dashboard-summary 已正式返回的计数，店长待办指标仅用于后台值班与 SLA 观察，不代表系统已经打通外部通知链路。新增聚合字段也只用于只读观察，不代表 release capability。"
      title="看板口径说明"
      type="info"
    />
  </ContentWrap>

  <ContentWrap>
    <el-card shadow="never">
      <template #header>
        <span>高标准只读观察聚合</span>
      </template>
      <el-row :gutter="12">
        <el-col v-for="item in readonlyObserveCards" :key="item.label" :lg="8" :md="24" :sm="24" :xs="24">
          <div class="rounded-8px border border-[var(--el-border-color-lighter)] p-12px">
            <div class="text-12px text-[var(--el-text-color-secondary)]">{{ item.label }}</div>
            <div class="mt-8px text-15px font-600">{{ item.summary }}</div>
            <div class="mt-8px text-12px text-[var(--el-text-color-secondary)]">{{ item.detail }}</div>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </ContentWrap>

  <ContentWrap>
    <el-row v-loading="loading" :gutter="12">
      <el-col v-for="card in cards" :key="card.key" :lg="8" :md="12" :sm="12" :xs="24">
        <el-card shadow="never">
          <div class="text-12px text-[var(--el-text-color-secondary)]">{{ card.label }}</div>
          <div class="mt-8px text-28px font-600">{{ card.value }}</div>
          <div class="mt-8px text-12px" :class="card.hintClass">{{ card.hint }}</div>
          <div class="mt-12px">
            <el-button link type="primary" @click="goLedgerWithCard(card.key)">查看台账</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </ContentWrap>

  <ContentWrap>
    <el-row :gutter="12">
      <el-col :lg="12" :md="24" :sm="24" :xs="24">
        <el-card shadow="never">
          <template #header>
            <span>运营解读</span>
          </template>
          <ul class="dashboard-list">
            <li>差评和紧急数用于首屏判断是否需要服务恢复 owner 立即介入。</li>
            <li>待认领、即将超时、已超时共同描述店长待办的执行质量。</li>
            <li>已回复数用于观察门店响应率，不等价于问题已解决或已闭环。</li>
          </ul>
        </el-card>
      </el-col>
      <el-col :lg="12" :md="24" :sm="24" :xs="24">
        <el-card shadow="never">
          <template #header>
            <span>动作建议</span>
          </template>
          <ul class="dashboard-list">
            <li>待认领高企时，先排查值班 owner 是否及时领取差评待办。</li>
            <li>即将认领超时、即将首次处理超时、即将闭环超时属于预警池，适合提前介入。</li>
            <li>首次处理超时说明联系人虽已锁定，但实际回访动作没有及时落账。</li>
            <li>闭环超时优先复核门店联系人真值与恢复链路，不要把后台待办误判成外部通知已完成。</li>
          </ul>
        </el-card>
      </el-col>
    </el-row>
  </ContentWrap>
</template>

<script lang="ts" setup>
import * as BookingReviewApi from '@/api/mall/booking/review'
import { buildLedgerQueryFromDashboardCardKey } from '../queryHelpers.mjs'

defineOptions({ name: 'BookingReviewDashboard' })

const router = useRouter()
const loading = ref(false)
const summary = ref<BookingReviewApi.BookingReviewDashboardSummary>({})

const getSummary = async () => {
  loading.value = true
  try {
    summary.value = (await BookingReviewApi.getReviewDashboardSummary()) || {}
  } finally {
    loading.value = false
  }
}

const displayCount = (value?: number) => {
  return value ?? 0
}

const readonlyObserveCards = computed(() => [
  {
    label: '优先级观察',
    summary: `P0 ${displayCount(summary.value.priorityP0Count)} / P1 ${displayCount(summary.value.priorityP1Count)} / P2 ${displayCount(summary.value.priorityP2Count)} / P3 ${displayCount(summary.value.priorityP3Count)}`,
    detail: '只读观察当前差评优先级堆积，不代表升级、送达或 release capability。'
  },
  {
    label: '超时池观察',
    summary: `即将超时 ${displayCount(summary.value.managerTimeoutDueSoonCount)} / 已超时 ${displayCount(summary.value.managerTimeoutCount)}`,
    detail: '只用于值班排队与人工介入顺序，不改变 CLOSED、timeout 和观察态边界。'
  },
  {
    label: '跨通道审计观察',
    summary: `阻断 ${displayCount(summary.value.notifyAuditBlockedCount)} / 失败 ${displayCount(summary.value.notifyAuditFailedCount)} / 重试待观察 ${displayCount(summary.value.notifyAuditManualRetryPendingCount)} / 分裂 ${displayCount(summary.value.notifyAuditDivergedCount)}`,
    detail: '只读反映 review 维度审计聚合，不代表全链路送达，也不代表门店已处理完成。'
  }
])

const cards = computed(() => [
  {
    key: 'total',
    label: '总评价数',
    value: displayCount(summary.value.totalCount),
    hint: '用于观察评价覆盖率和样本规模',
    hintClass: 'text-[var(--el-text-color-secondary)]'
  },
  {
    key: 'negative',
    label: '差评数',
    value: displayCount(summary.value.negativeCount),
    hint: '需要优先判断是否触发人工补救',
    hintClass: 'text-[var(--el-color-danger)]'
  },
  {
    key: 'pendingFollow',
    label: '待处理数',
    value: displayCount(summary.value.pendingFollowCount),
    hint: '仍处于 follow-up 队列',
    hintClass: 'text-[var(--el-color-warning)]'
  },
  {
    key: 'urgent',
    label: '紧急数',
    value: displayCount(summary.value.urgentCount),
    hint: '建议第一时间人工同步门店确认处理',
    hintClass: 'text-[var(--el-color-danger)]'
  },
  {
    key: 'managerTodoPending',
    label: '店长待认领',
    value: displayCount(summary.value.managerTodoPendingCount),
    hint: '差评进入待办池后仍未被领取',
    hintClass: 'text-[var(--el-color-warning)]'
  },
  {
    key: 'managerTodoClaimTimeout',
    label: '认领超时',
    value: displayCount(summary.value.managerTodoClaimTimeoutCount),
    hint: '超过 10 分钟仍未认领',
    hintClass: 'text-[var(--el-color-warning)]'
  },
  {
    key: 'managerTodoClaimDueSoon',
    label: '即将认领超时',
    value: displayCount(summary.value.managerTodoClaimDueSoonCount),
    hint: '距离认领超时不足 5 分钟',
    hintClass: 'text-[var(--el-color-warning)]'
  },
  {
    key: 'managerTodoFirstActionTimeout',
    label: '首次处理超时',
    value: displayCount(summary.value.managerTodoFirstActionTimeoutCount),
    hint: '超过 30 分钟未记录首次处理',
    hintClass: 'text-[var(--el-color-danger)]'
  },
  {
    key: 'managerTodoFirstActionDueSoon',
    label: '即将首次处理超时',
    value: displayCount(summary.value.managerTodoFirstActionDueSoonCount),
    hint: '距离首次处理超时不足 10 分钟',
    hintClass: 'text-[var(--el-color-warning)]'
  },
  {
    key: 'managerTodoCloseTimeout',
    label: '闭环超时',
    value: displayCount(summary.value.managerTodoCloseTimeoutCount),
    hint: '超过 24 小时仍未闭环',
    hintClass: 'text-[var(--el-color-danger)]'
  },
  {
    key: 'managerTodoCloseDueSoon',
    label: '即将闭环超时',
    value: displayCount(summary.value.managerTodoCloseDueSoonCount),
    hint: '距离闭环超时不足 120 分钟',
    hintClass: 'text-[var(--el-color-danger)]'
  },
  {
    key: 'managerTodoClosed',
    label: '店长待办 CLOSED',
    value: displayCount(summary.value.managerTodoClosedCount),
    hint: '仅表示后台待办收口，不代表提醒已派发成功或门店已处理完成',
    hintClass: 'text-[var(--el-color-success)]'
  },
  {
    key: 'positive',
    label: '好评数',
    value: displayCount(summary.value.positiveCount),
    hint: '可沉淀服务亮点，当前不直接接奖励策略',
    hintClass: 'text-[var(--el-color-success)]'
  },
  {
    key: 'neutral',
    label: '中评数',
    value: displayCount(summary.value.neutralCount),
    hint: '建议结合标签与内容做主题归因',
    hintClass: 'text-[var(--el-color-warning)]'
  },
  {
    key: 'replied',
    label: '已回复数',
    value: displayCount(summary.value.repliedCount),
    hint: '用于观察响应率，不代表已闭环',
    hintClass: 'text-[var(--el-color-primary)]'
  }
])

const goLedger = () => {
  router.push('/mall/booking/review')
}

const goLedgerWithCard = (cardKey: string) => {
  router.push({
    path: '/mall/booking/review',
    query: buildLedgerQueryFromDashboardCardKey(cardKey),
  })
}

onMounted(() => {
  getSummary()
})
</script>

<style lang="scss" scoped>
.dashboard-list {
  margin: 0;
  padding-left: 18px;
  color: var(--el-text-color-primary);
  line-height: 1.8;
}
</style>
