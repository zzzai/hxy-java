<template>
  <doc-alert title="预约评价通知出站台账" url="https://doc.iocoder.cn/" />

  <ContentWrap>
    <el-alert
      :closable="false"
      description="这里只看 notify outbox 真值：PENDING 表示待派发，BLOCKED_NO_OWNER 表示缺门店店长账号绑定，不代表系统已经自动补发。"
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
      <el-form-item label="通知状态">
        <el-select v-model="queryParams.status" class="!w-180px" clearable placeholder="全部">
          <el-option label="待派发" value="PENDING" />
          <el-option label="已发送" value="SENT" />
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
    <el-table v-loading="loading" :data="list">
      <el-table-column label="出站ID" prop="id" width="90" />
      <el-table-column label="评价ID" prop="reviewId" width="100" />
      <el-table-column label="门店ID" prop="storeId" width="100" />
      <el-table-column label="接收角色" prop="receiverRole" width="150" />
      <el-table-column label="接收账号ID" prop="receiverUserId" width="120" />
      <el-table-column label="通知类型" prop="notifyType" min-width="180" />
      <el-table-column label="通知渠道" prop="channel" width="120" />
      <el-table-column label="通知状态" width="150">
        <template #default="{ row }">
          <el-tag :type="notifyStatusTagType(row.status)">{{ notifyStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="重试次数" prop="retryCount" width="100" />
      <el-table-column label="最后错误" prop="lastErrorMsg" min-width="240" show-overflow-tooltip />
      <el-table-column label="最近动作" prop="lastActionCode" width="160" />
      <el-table-column :formatter="dateFormatter" label="最近动作时间" prop="lastActionTime" width="180" />
      <el-table-column :formatter="dateFormatter" label="创建时间" prop="createTime" width="180" />
      <el-table-column align="center" fixed="right" label="操作" width="110">
        <template #default="{ row }">
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

defineOptions({ name: 'BookingReviewNotifyOutboxIndex' })

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const total = ref(0)
const list = ref<BookingReviewApi.BookingReviewNotifyOutbox[]>([])

const createDefaultQuery = (): BookingReviewApi.BookingReviewNotifyOutboxPageReq => ({
  pageNo: 1,
  pageSize: 10,
  reviewId: undefined,
  storeId: undefined,
  receiverRole: undefined,
  receiverUserId: undefined,
  status: undefined,
  channel: undefined,
  notifyType: undefined
})

const queryParams = reactive<BookingReviewApi.BookingReviewNotifyOutboxPageReq>(createDefaultQuery())

const applyRouteQuery = () => {
  const reviewId = Number(Array.isArray(route.query.reviewId) ? route.query.reviewId[0] : route.query.reviewId)
  if (Number.isFinite(reviewId) && reviewId > 0) {
    queryParams.reviewId = reviewId
  }
  const status = Array.isArray(route.query.status) ? route.query.status[0] : route.query.status
  if (status) {
    queryParams.status = String(status)
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

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
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

watch(
  () => route.query,
  () => {
    Object.assign(queryParams, createDefaultQuery())
    applyRouteQuery()
    getList()
  },
  { immediate: true },
)
</script>
