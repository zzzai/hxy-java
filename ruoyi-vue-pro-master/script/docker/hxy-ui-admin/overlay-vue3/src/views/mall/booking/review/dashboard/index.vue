<template>
  <ContentWrap>
    <div class="mb-12px flex items-center justify-between gap-12px">
      <div>
        <div class="text-18px font-600">预约服务评价看板</div>
        <div class="mt-4px text-13px text-[var(--el-text-color-secondary)]">
          聚合关注差评、待处理回访和整体评价温度。
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
    <el-row v-loading="loading" :gutter="12">
      <el-col v-for="card in cards" :key="card.key" :lg="8" :md="12" :sm="12" :xs="24">
        <el-card shadow="never">
          <div class="text-12px text-[var(--el-text-color-secondary)]">{{ card.label }}</div>
          <div class="mt-8px text-28px font-600">{{ card.value }}</div>
          <div class="mt-8px text-12px" :class="card.hintClass">{{ card.hint }}</div>
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
            <li>差评和紧急数用于首屏判断是否需要店长或区域运营立即介入。</li>
            <li>待处理数代表仍在 follow-up 池中的评价，适合做日清日结。</li>
            <li>已回复数用于观察门店回应率，不等价于问题已解决。</li>
          </ul>
        </el-card>
      </el-col>
      <el-col :lg="12" :md="24" :sm="24" :xs="24">
        <el-card shadow="never">
          <template #header>
            <span>动作建议</span>
          </template>
          <ul class="dashboard-list">
            <li>紧急差评优先安排人工回访，并同步店长确认补救动作。</li>
            <li>中评可优先看标签与内容，判断是服务体验还是门店环境问题。</li>
            <li>好评可以沉淀服务亮点，但当前页面不直接承接奖励策略。</li>
          </ul>
        </el-card>
      </el-col>
    </el-row>
  </ContentWrap>
</template>

<script lang="ts" setup>
import * as BookingReviewApi from '@/api/mall/booking/review'

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

const cards = computed(() => [
  {
    key: 'total',
    label: '总评价数',
    value: displayCount(summary.value.totalCount),
    hint: '用于观察评价覆盖率和样本规模',
    hintClass: 'text-[var(--el-text-color-secondary)]'
  },
  {
    key: 'positive',
    label: '好评数',
    value: displayCount(summary.value.positiveCount),
    hint: '适合沉淀服务亮点和门店口碑样本',
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
    key: 'negative',
    label: '差评数',
    value: displayCount(summary.value.negativeCount),
    hint: '需要优先判断是否触发人工补救',
    hintClass: 'text-[var(--el-color-danger)]'
  },
  {
    key: 'pending',
    label: '待处理数',
    value: displayCount(summary.value.pendingFollowCount),
    hint: '仍处于待跟进或跟进中队列',
    hintClass: 'text-[var(--el-color-warning)]'
  },
  {
    key: 'urgent',
    label: '紧急数',
    value: displayCount(summary.value.urgentCount),
    hint: '建议第一时间同步店长并确认回访动作',
    hintClass: 'text-[var(--el-color-danger)]'
  },
  {
    key: 'replied',
    label: '已回复数',
    value: displayCount(summary.value.repliedCount),
    hint: '用于观察门店响应率，不代表已闭环',
    hintClass: 'text-[var(--el-color-primary)]'
  }
])

const goLedger = () => {
  router.push('/mall/booking/review')
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
