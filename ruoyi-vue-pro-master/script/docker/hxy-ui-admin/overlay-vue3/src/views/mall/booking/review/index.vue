<template>
  <doc-alert title="预约服务评价运营台账" url="https://doc.iocoder.cn/" />

  <ContentWrap>
    <el-form :inline="true" :model="queryParams" class="-mb-15px" label-width="96px">
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
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="评价ID" prop="id" width="90" />
      <el-table-column label="预约订单ID" prop="bookingOrderId" width="120" />
      <el-table-column label="门店ID" prop="storeId" width="100" />
      <el-table-column label="技师ID" prop="technicianId" width="100" />
      <el-table-column label="会员ID" prop="memberId" width="100" />
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
      <el-table-column align="center" fixed="right" label="操作" width="180">
        <template #default="{ row }">
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

defineOptions({ name: 'BookingReviewIndex' })

const router = useRouter()
const loading = ref(false)
const total = ref(0)
const list = ref<BookingReviewApi.BookingReview[]>([])

const queryParams = reactive<BookingReviewApi.BookingReviewPageReq>({
  pageNo: 1,
  pageSize: 10,
  id: undefined,
  bookingOrderId: undefined,
  storeId: undefined,
  technicianId: undefined,
  memberId: undefined,
  reviewLevel: undefined,
  riskLevel: undefined,
  followStatus: undefined,
  replyStatus: undefined,
  submitTime: undefined
})

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
  queryParams.pageNo = 1
  queryParams.pageSize = 10
  queryParams.id = undefined
  queryParams.bookingOrderId = undefined
  queryParams.storeId = undefined
  queryParams.technicianId = undefined
  queryParams.memberId = undefined
  queryParams.reviewLevel = undefined
  queryParams.riskLevel = undefined
  queryParams.followStatus = undefined
  queryParams.replyStatus = undefined
  queryParams.submitTime = undefined
  getList()
}

const openDetail = (id: number) => {
  router.push({ path: '/mall/booking/review/detail', query: { id: String(id) } })
}

const goDashboard = () => {
  router.push('/mall/booking/review/dashboard')
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

onMounted(() => {
  getList()
})
</script>
