<template>
  <doc-alert title="预约评价店长账号路由核查" url="https://doc.iocoder.cn/" />

  <ContentWrap>
    <el-alert
      :closable="false"
      description="这里只读核查门店联系人与店长后台账号路由真值，不在当前页面直接修改绑定。BLOCKED_NO_OWNER 进入这里后，先判断是没有路由、路由失效，还是门店联系人本身不完整。"
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

  <ContentWrap v-if="currentRouting" v-loading="inspectLoading">
    <el-descriptions :column="2" border title="当前门店核查结论">
      <el-descriptions-item label="门店">{{ currentRouting.storeName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="门店ID">{{ currentRouting.storeId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="联系人">{{ currentRouting.contactName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="联系人手机号">{{ currentRouting.contactMobile || '-' }}</el-descriptions-item>
      <el-descriptions-item label="路由结论">{{ currentRouting.routingLabel || '-' }}</el-descriptions-item>
      <el-descriptions-item label="店长后台账号ID">{{ currentRouting.managerAdminUserId || '-' }}</el-descriptions-item>
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
const total = ref(0)
const list = ref<BookingReviewApi.BookingReviewManagerAccountRouting[]>([])
const currentRouting = ref<BookingReviewApi.BookingReviewManagerAccountRouting>()

const createDefaultQuery = (): BookingReviewApi.BookingReviewManagerAccountRoutingPageReq => ({
  pageNo: 1,
  pageSize: 10,
  storeId: undefined,
  storeName: undefined,
  contactMobile: undefined
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

const handleQuery = async () => {
  queryParams.pageNo = 1
  await Promise.all([loadCurrentRouting(), getList()])
}

const resetQuery = async () => {
  Object.assign(queryParams, createDefaultQuery())
  applyRouteQuery()
  await Promise.all([loadCurrentRouting(), getList()])
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
    await Promise.all([loadCurrentRouting(), getList()])
  },
  { immediate: true },
)
</script>
