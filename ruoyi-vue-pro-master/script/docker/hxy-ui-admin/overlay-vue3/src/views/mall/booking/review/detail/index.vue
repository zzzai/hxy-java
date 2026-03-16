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
        <el-descriptions-item label="门店ID">{{ review.storeId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="技师ID">{{ review.technicianId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="会员ID">{{ review.memberId || '-' }}</el-descriptions-item>
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
</template>

<script lang="ts" setup>
import * as BookingReviewApi from '@/api/mall/booking/review'

defineOptions({ name: 'BookingReviewDetail' })

const route = useRoute()
const router = useRouter()
const message = useMessage()
const loading = ref(false)
const replyLoading = ref(false)
const followLoading = ref(false)
const review = ref<BookingReviewApi.BookingReview>({} as BookingReviewApi.BookingReview)

const replyForm = reactive<BookingReviewApi.BookingReviewReplyReq>({
  reviewId: 0,
  replyContent: ''
})

const followForm = reactive<BookingReviewApi.BookingReviewFollowUpdateReq>({
  reviewId: 0,
  followStatus: 1,
  followResult: ''
})

const reviewId = computed(() => {
  const value = Array.isArray(route.query.id) ? route.query.id[0] : route.query.id
  const id = Number(value)
  return Number.isFinite(id) && id > 0 ? id : 0
})

const loadDetail = async () => {
  if (!reviewId.value) {
    review.value = {} as BookingReviewApi.BookingReview
    replyForm.reviewId = 0
    followForm.reviewId = 0
    return
  }
  loading.value = true
  try {
    const data = await BookingReviewApi.getReview(reviewId.value)
    review.value = data || ({} as BookingReviewApi.BookingReview)
    replyForm.reviewId = reviewId.value
    replyForm.replyContent = data?.replyContent || ''
    followForm.reviewId = reviewId.value
    followForm.followStatus = data?.followStatus ?? 1
    followForm.followResult = data?.followResult || ''
  } finally {
    loading.value = false
  }
}

const reload = () => {
  loadDetail()
}

const goBack = () => {
  router.push('/mall/booking/review')
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

watch(
  () => route.query.id,
  () => {
    loadDetail()
  },
  { immediate: true }
)
</script>
