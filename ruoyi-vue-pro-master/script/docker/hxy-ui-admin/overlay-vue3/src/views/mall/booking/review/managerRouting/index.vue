<template>
  <doc-alert title="预约评价店长账号路由核查" url="https://doc.iocoder.cn/" />

  <ContentWrap>
    <el-alert
      :closable="false"
      description="这里只读核查门店联系人与店长双通道路由真值，不在当前页面直接修改绑定。BLOCKED_NO_OWNER 进入这里后，要继续区分是缺 App 账号、缺企微账号，还是整条路由未生效。"
      title="店长账号路由核查"
      type="info"
    />
  </ContentWrap>

  <ContentWrap>
    <el-form :inline="true" :model="queryParams" class="-mb-15px" label-width="110px">
      <el-form-item label="门店ID">
        <el-input v-model="queryParams.storeId" class="!w-180px" clearable placeholder="请输入门店ID" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="门店名称">
        <el-input v-model="queryParams.storeName" class="!w-220px" clearable placeholder="请输入门店名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="联系人手机号">
        <el-input
          v-model="queryParams.contactMobile"
          class="!w-220px"
          clearable
          placeholder="请输入联系人手机号"
          @keyup.enter="handleQuery"
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
        <el-button plain type="primary" @click="goBack">
          <Icon class="mr-5px" icon="ep:back" />
          返回
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap v-loading="summaryLoading">
    <div class="mb-12px flex items-center justify-between gap-12px">
      <div>
        <div class="text-16px font-600">覆盖率概览</div>
        <div class="mt-4px text-12px text-[var(--el-text-color-secondary)]">
          只统计当前筛选范围内的门店主数据，不代表真实送达率。
        </div>
      </div>
      <div class="text-12px text-[var(--el-text-color-secondary)]">
        总门店 {{ coverageSummary.totalStoreCount || 0 }}
      </div>
    </div>

    <div class="mb-12px flex flex-wrap gap-8px">
      <el-button plain @click="applyQuickFilter('ALL')">全部</el-button>
      <el-button plain type="warning" @click="applyQuickFilter('MISSING_ANY')">只看缺任一绑定</el-button>
      <el-button plain type="danger" @click="applyQuickFilter('MISSING_APP')">只看缺 App</el-button>
      <el-button plain type="danger" @click="applyQuickFilter('MISSING_WECOM')">只看缺企微</el-button>
      <el-button plain type="danger" @click="applyQuickFilter('MISSING_BOTH')">只看双缺失</el-button>
      <el-button plain type="success" @click="applyQuickFilter('READY')">只看双通道就绪</el-button>
    </div>

    <div class="grid gap-12px md:grid-cols-2 xl:grid-cols-4">
      <el-card v-for="item in coverageCards" :key="item.label" shadow="never">
        <div class="text-12px text-[var(--el-text-color-secondary)]">{{ item.label }}</div>
        <div class="mt-8px text-18px font-600">{{ item.value }}</div>
        <div class="mt-4px text-12px text-[var(--el-text-color-secondary)]">{{ item.detail }}</div>
      </el-card>
    </div>
  </ContentWrap>

  <ContentWrap v-if="currentRouting" v-loading="inspectLoading">
      <el-descriptions :column="2" border title="当前门店核查结论">
        <el-descriptions-item label="门店">{{ currentRouting.storeName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="门店ID">{{ currentRouting.storeId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ currentRouting.contactName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系人手机号">{{ currentRouting.contactMobile || '-' }}</el-descriptions-item>
        <el-descriptions-item label="路由结论">{{ currentRouting.routingLabel || '-' }}</el-descriptions-item>
        <el-descriptions-item label="店长后台账号ID">{{ currentRouting.managerAdminUserId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="店长企微账号ID">{{ currentRouting.managerWecomUserId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="App 路由">{{ currentRouting.appRoutingLabel || '-' }}</el-descriptions-item>
        <el-descriptions-item label="企微路由">{{ currentRouting.wecomRoutingLabel || '-' }}</el-descriptions-item>
        <el-descriptions-item label="App 修复建议" :span="2">{{ currentRouting.appRepairHint || '-' }}</el-descriptions-item>
        <el-descriptions-item label="企微修复建议" :span="2">{{ currentRouting.wecomRepairHint || '-' }}</el-descriptions-item>
        <el-descriptions-item label="路由说明" :span="2">{{ currentRouting.routingDetail || '-' }}</el-descriptions-item>
        <el-descriptions-item label="修复建议" :span="2">{{ currentRouting.repairHint || '-' }}</el-descriptions-item>
      </el-descriptions>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="门店ID" prop="storeId" width="100" />
      <el-table-column label="门店名称" prop="storeName" min-width="180" />
      <el-table-column label="联系人" prop="contactName" width="120" />
      <el-table-column label="联系人手机号" prop="contactMobile" width="140" />
      <el-table-column label="路由结论" min-width="160">
        <template #default="{ row }">
          {{ row.routingLabel || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="店长后台账号ID" prop="managerAdminUserId" width="140" />
      <el-table-column label="店长企微账号ID" prop="managerWecomUserId" min-width="180" />
      <el-table-column label="App 路由" min-width="140">
        <template #default="{ row }">
          {{ row.appRoutingLabel || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="企微路由" min-width="140">
        <template #default="{ row }">
          {{ row.wecomRoutingLabel || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="绑定状态" prop="bindingStatus" width="120" />
      <el-table-column label="来源" prop="source" width="140" />
      <el-table-column :formatter="dateFormatter" label="最近核验时间" prop="lastVerifiedTime" width="180" />
      <el-table-column label="修复建议" min-width="260" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.repairHint || '-' }}
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

defineOptions({ name: 'BookingReviewManagerRoutingIndex' })

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const inspectLoading = ref(false)
const summaryLoading = ref(false)
const total = ref(0)
const list = ref<BookingReviewApi.BookingReviewManagerAccountRouting[]>([])
const currentRouting = ref<BookingReviewApi.BookingReviewManagerAccountRouting>()
const coverageSummary = ref<BookingReviewApi.BookingReviewManagerAccountRoutingSummary>({
  totalStoreCount: 0,
  dualReadyCount: 0,
  appReadyCount: 0,
  wecomReadyCount: 0,
  missingAnyCount: 0,
  missingAppCount: 0,
  missingWecomCount: 0,
  missingBothCount: 0
})

const createDefaultQuery = (): BookingReviewApi.BookingReviewManagerAccountRoutingPageReq => ({
  pageNo: 1,
  pageSize: 10,
  storeId: undefined,
  storeName: undefined,
  contactMobile: undefined,
  onlyMissingAny: undefined,
  routingStatus: undefined,
  appRoutingStatus: undefined,
  wecomRoutingStatus: undefined
})

const queryParams = reactive<BookingReviewApi.BookingReviewManagerAccountRoutingPageReq>(createDefaultQuery())

const applyRouteQuery = () => {
  const storeId = Number(Array.isArray(route.query.storeId) ? route.query.storeId[0] : route.query.storeId)
  if (Number.isFinite(storeId) && storeId > 0) {
    queryParams.storeId = storeId
  }
}

const loadCurrentRouting = async () => {
  if (!queryParams.storeId) {
    currentRouting.value = undefined
    return
  }
  inspectLoading.value = true
  try {
    currentRouting.value = await BookingReviewApi.getReviewManagerAccountRouting(queryParams.storeId)
  } finally {
    inspectLoading.value = false
  }
}

const buildSummaryQueryParams = (): BookingReviewApi.BookingReviewManagerAccountRoutingPageReq => ({
  pageNo: 1,
  pageSize: 1,
  storeId: queryParams.storeId,
  storeName: queryParams.storeName,
  contactMobile: queryParams.contactMobile
})

const loadCoverageSummary = async () => {
  summaryLoading.value = true
  try {
    coverageSummary.value = (await BookingReviewApi.getReviewManagerAccountRoutingCoverageSummary(
      buildSummaryQueryParams(),
    )) || {
      totalStoreCount: 0,
      dualReadyCount: 0,
      appReadyCount: 0,
      wecomReadyCount: 0,
      missingAnyCount: 0,
      missingAppCount: 0,
      missingWecomCount: 0,
      missingBothCount: 0
    }
  } finally {
    summaryLoading.value = false
  }
}

const getList = async () => {
  loading.value = true
  try {
    const data = await BookingReviewApi.getReviewManagerAccountRoutingPage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

const formatCoverageValue = (count?: number) => {
  const totalCount = coverageSummary.value.totalStoreCount || 0
  const currentCount = count || 0
  if (!totalCount) {
    return '0 / 0 (0.0%)'
  }
  return `${currentCount} / ${totalCount} (${((currentCount / totalCount) * 100).toFixed(1)}%)`
}

const coverageCards = computed(() => [
  {
    label: '双通道覆盖率',
    value: formatCoverageValue(coverageSummary.value.dualReadyCount),
    detail: 'App 与企微都已具备可派发账号'
  },
  {
    label: 'App 覆盖率',
    value: formatCoverageValue(coverageSummary.value.appReadyCount),
    detail: '门店已命中 managerAdminUserId'
  },
  {
    label: '企微覆盖率',
    value: formatCoverageValue(coverageSummary.value.wecomReadyCount),
    detail: '门店已命中 managerWecomUserId'
  },
  {
    label: '缺任一绑定',
    value: String(coverageSummary.value.missingAnyCount || 0),
    detail: `缺 App ${coverageSummary.value.missingAppCount || 0} / 缺企微 ${coverageSummary.value.missingWecomCount || 0} / 双缺失 ${coverageSummary.value.missingBothCount || 0}`
  }
])

const handleQuery = async () => {
  queryParams.pageNo = 1
  await Promise.all([loadCurrentRouting(), loadCoverageSummary(), getList()])
}

const resetQuery = async () => {
  Object.assign(queryParams, createDefaultQuery())
  applyRouteQuery()
  await Promise.all([loadCurrentRouting(), loadCoverageSummary(), getList()])
}

const applyQuickFilter = async (mode: string) => {
  queryParams.pageNo = 1
  queryParams.onlyMissingAny = undefined
  queryParams.routingStatus = undefined
  queryParams.appRoutingStatus = undefined
  queryParams.wecomRoutingStatus = undefined
  if (mode === 'MISSING_ANY') {
    queryParams.onlyMissingAny = true
  } else if (mode === 'MISSING_APP') {
    queryParams.appRoutingStatus = 'APP_MISSING'
  } else if (mode === 'MISSING_WECOM') {
    queryParams.wecomRoutingStatus = 'WECOM_MISSING'
  } else if (mode === 'MISSING_BOTH') {
    queryParams.appRoutingStatus = 'APP_MISSING'
    queryParams.wecomRoutingStatus = 'WECOM_MISSING'
  } else if (mode === 'READY') {
    queryParams.routingStatus = 'ACTIVE_ROUTE'
  }
  await Promise.all([loadCoverageSummary(), getList()])
}

const goBack = () => {
  const reviewId = Array.isArray(route.query.reviewId) ? route.query.reviewId[0] : route.query.reviewId
  if (reviewId) {
    router.push(`/mall/booking/review/notify-outbox?reviewId=${reviewId}`)
    return
  }
  router.push('/mall/booking/review')
}

watch(
  () => route.query,
  async () => {
    Object.assign(queryParams, createDefaultQuery())
    applyRouteQuery()
    await Promise.all([loadCurrentRouting(), loadCoverageSummary(), getList()])
  },
  { immediate: true },
)
</script>
