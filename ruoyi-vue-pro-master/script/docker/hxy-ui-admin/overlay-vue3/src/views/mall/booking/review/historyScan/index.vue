<template>
  <ContentWrap>
    <div class="mb-12px flex items-center justify-between gap-12px">
      <div>
        <div class="text-18px font-600">预约服务评价历史治理扫描</div>
        <div class="mt-4px text-13px text-[var(--el-text-color-secondary)]">
          只识别治理候选，帮助运营判断哪些历史差评可以人工推进、哪些需要先核实真值。
        </div>
      </div>
      <div class="flex gap-8px">
        <el-button @click="goLedger">
          <Icon class="mr-5px" icon="ep:back" />
          返回台账
        </el-button>
      </div>
    </div>
  </ContentWrap>

  <ContentWrap>
    <el-alert
      :closable="false"
      title="扫描边界说明"
      type="warning"
      description="本页只识别治理候选，不会自动修复历史数据，不代表已进入店长待办池，也不代表历史数据已经治理完成。"
    />
  </ContentWrap>

  <ContentWrap>
    <el-form :inline="true" :model="queryParams" class="-mb-15px" label-width="120px">
      <el-form-item label="门店ID" prop="storeId">
        <el-input v-model="queryParams.storeId" class="!w-180px" clearable placeholder="请输入门店ID" />
      </el-form-item>
      <el-form-item label="预约订单ID" prop="bookingOrderId">
        <el-input v-model="queryParams.bookingOrderId" class="!w-200px" clearable placeholder="请输入预约订单ID" />
      </el-form-item>
      <el-form-item label="风险分类" prop="riskCategory">
        <el-select v-model="queryParams.riskCategory" class="!w-180px" clearable placeholder="全部">
          <el-option label="可人工推进" value="MANUAL_READY" />
          <el-option label="高风险待核实" value="HIGH_RISK" />
          <el-option label="不在本轮范围" value="OUT_OF_SCOPE" />
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
        <el-button :loading="loading" type="primary" @click="handleScan">
          <Icon class="mr-5px" icon="ep:search" />
          开始扫描
        </el-button>
        <el-button @click="resetQuery">
          <Icon class="mr-5px" icon="ep:refresh" />
          重置条件
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-row :gutter="12">
      <el-col v-for="card in summaryCards" :key="card.key" :lg="6" :md="12" :sm="12" :xs="24">
        <el-card shadow="never">
          <div class="text-12px text-[var(--el-text-color-secondary)]">{{ card.label }}</div>
          <div class="mt-8px text-28px font-600">{{ card.value }}</div>
          <div class="mt-8px text-12px" :class="card.hintClass">{{ card.hint }}</div>
        </el-card>
      </el-col>
    </el-row>
  </ContentWrap>

  <ContentWrap>
    <el-alert
      :closable="false"
      title="风险提示"
      type="info"
      description="可人工推进仅表示满足当前详情页触发 lazy-init 的前置条件；高风险待核实需先核实订单、门店或联系人真值；不在本轮范围默认不作为运营主视角。"
    />
  </ContentWrap>

  <ContentWrap v-if="!hasScanned">
    <el-empty description="尚未开始扫描，请先点击“开始扫描”获取候选清单。" />
  </ContentWrap>

  <ContentWrap v-else>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="评价ID" prop="reviewId" width="100" />
      <el-table-column label="预约订单ID" prop="bookingOrderId" width="130" />
      <el-table-column label="门店 / ID" min-width="160">
        <template #default="{ row }">
          {{ readableEntityText(row.storeName, row.storeId) }}
        </template>
      </el-table-column>
      <el-table-column label="技师 / ID" min-width="160">
        <template #default="{ row }">
          {{ readableEntityText(row.technicianName, row.technicianId) }}
        </template>
      </el-table-column>
      <el-table-column label="会员 / ID" min-width="160">
        <template #default="{ row }">
          {{ readableEntityText(row.memberNickname, row.memberId) }}
        </template>
      </el-table-column>
      <el-table-column :formatter="dateFormatter" label="提交时间" prop="submitTime" width="180" />
      <el-table-column label="当前店长待办状态" min-width="150">
        <template #default="{ row }">
          <el-tag :type="managerTodoStatusTagType(row.managerTodoStatus)">{{ managerTodoStatusText(row.managerTodoStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="分类" width="140">
        <template #default="{ row }">
          <el-tag :type="riskCategoryTagType(row.riskCategory)">{{ riskCategoryText(row.riskCategory) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="风险说明" min-width="320">
        <template #default="{ row }">
          <div>{{ row.riskSummary || '-' }}</div>
          <div class="mt-4px text-[12px] text-[var(--el-text-color-secondary)]">
            {{ (row.riskReasons || []).join('；') || '-' }}
          </div>
        </template>
      </el-table-column>
      <el-table-column align="center" fixed="right" label="操作" width="120">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row.reviewId)">查看详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <Pagination
      v-model:limit="queryParams.pageSize"
      v-model:page="queryParams.pageNo"
      :total="total"
      @pagination="loadScanResult"
    />
  </ContentWrap>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import * as BookingReviewApi from '@/api/mall/booking/review'

defineOptions({ name: 'BookingReviewHistoryScan' })

const router = useRouter()
const loading = ref(false)
const hasScanned = ref(false)
const total = ref(0)
const list = ref<BookingReviewApi.BookingReviewHistoryScanItem[]>([])
const summary = ref<BookingReviewApi.BookingReviewHistoryScanSummary>({})

const createDefaultQuery = (): BookingReviewApi.BookingReviewHistoryScanReq => ({
  pageNo: 1,
  pageSize: 10,
  storeId: undefined,
  bookingOrderId: undefined,
  riskCategory: undefined,
  submitTime: undefined,
})

const queryParams = reactive<BookingReviewApi.BookingReviewHistoryScanReq>(createDefaultQuery())

const loadScanResult = async () => {
  loading.value = true
  try {
    const data = (await BookingReviewApi.getReviewHistoryScan(queryParams)) || {}
    summary.value = data.summary || {}
    list.value = data.list || []
    total.value = data.total || 0
    hasScanned.value = true
  } finally {
    loading.value = false
  }
}

const handleScan = async () => {
  queryParams.pageNo = 1
  await loadScanResult()
}

const resetQuery = () => {
  Object.assign(queryParams, createDefaultQuery())
  hasScanned.value = false
  total.value = 0
  list.value = []
  summary.value = {}
}

const goLedger = () => {
  router.push('/mall/booking/review')
}

const openDetail = (reviewId: number) => {
  router.push({ path: '/mall/booking/review/detail', query: { id: String(reviewId) } })
}

const displayCount = (value?: number) => value ?? 0

const summaryCards = computed(() => [
  {
    key: 'scannedCount',
    label: '扫描总量',
    value: displayCount(summary.value.scannedCount),
    hint: '满足当前筛选条件的历史评价样本',
    hintClass: 'text-[var(--el-text-color-secondary)]',
  },
  {
    key: 'manualReadyCount',
    label: '可人工推进',
    value: displayCount(summary.value.manualReadyCount),
    hint: '满足当前详情页人工触发初始化前置条件',
    hintClass: 'text-[var(--el-color-success)]',
  },
  {
    key: 'highRiskCount',
    label: '高风险待核实',
    value: displayCount(summary.value.highRiskCount),
    hint: '需先核实订单、门店或联系人真值',
    hintClass: 'text-[var(--el-color-danger)]',
  },
  {
    key: 'outOfScopeCount',
    label: '不在本轮范围',
    value: displayCount(summary.value.outOfScopeCount),
    hint: '非历史未初始化差评，不默认作为运营主视角',
    hintClass: 'text-[var(--el-color-warning)]',
  },
])

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

const managerTodoStatusText = (status?: number) => {
  if (status === 1) return '待认领'
  if (status === 2) return '已认领'
  if (status === 3) return '处理中'
  if (status === 4) return '已闭环'
  return '未初始化'
}

const managerTodoStatusTagType = (status?: number) => {
  if (status === 1) return 'danger'
  if (status === 2) return 'warning'
  if (status === 3) return 'primary'
  if (status === 4) return 'success'
  return 'info'
}

const riskCategoryText = (value?: string) => {
  if (value === 'MANUAL_READY') return '可人工推进'
  if (value === 'HIGH_RISK') return '高风险待核实'
  if (value === 'OUT_OF_SCOPE') return '不在本轮范围'
  return '-'
}

const riskCategoryTagType = (value?: string) => {
  if (value === 'MANUAL_READY') return 'success'
  if (value === 'HIGH_RISK') return 'danger'
  if (value === 'OUT_OF_SCOPE') return 'warning'
  return 'info'
}
</script>
