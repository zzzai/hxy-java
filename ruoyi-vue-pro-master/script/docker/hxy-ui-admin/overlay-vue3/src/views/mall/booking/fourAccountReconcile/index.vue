<template>
  <ContentWrap>
    <el-form :inline="true" :model="queryParams" class="-mb-15px" label-width="88px">
      <el-form-item label="业务日期" prop="bizDate">
        <el-date-picker
          v-model="queryParams.bizDate"
          class="!w-260px"
          end-placeholder="结束日期"
          range-separator="至"
          start-placeholder="开始日期"
          type="daterange"
          value-format="YYYY-MM-DD"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" class="!w-140px" clearable placeholder="请选择状态">
          <el-option :value="10" label="通过" />
          <el-option :value="20" label="告警" />
        </el-select>
      </el-form-item>
      <el-form-item label="来源" prop="source">
        <el-select v-model="queryParams.source" class="!w-160px" clearable placeholder="请选择来源">
          <el-option label="JOB_DAILY" value="JOB_DAILY" />
          <el-option label="MANUAL" value="MANUAL" />
        </el-select>
      </el-form-item>
      <el-form-item label="问题编码" prop="issueCode">
        <el-input
          v-model="queryParams.issueCode"
          class="!w-220px"
          clearable
          placeholder="例如 FULFILLMENT_GT_TRADE"
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
        <el-button
          v-hasPermi="['booking:commission:settlement']"
          :loading="runLoading"
          plain
          type="primary"
          @click="openRunDialog"
        >
          <Icon class="mr-5px" icon="ep:video-play" />
          手工执行对账
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="业务日期" prop="bizDate" width="120" />
      <el-table-column label="交易账(元)" width="130">
        <template #default="{ row }">
          {{ fenToYuan(row.tradeAmount) }}
        </template>
      </el-table-column>
      <el-table-column label="履约账(元)" width="130">
        <template #default="{ row }">
          {{ fenToYuan(row.fulfillmentAmount) }}
        </template>
      </el-table-column>
      <el-table-column label="提成账(元)" width="130">
        <template #default="{ row }">
          {{ fenToYuan(row.commissionAmount) }}
        </template>
      </el-table-column>
      <el-table-column label="分账账(元)" width="130">
        <template #default="{ row }">
          {{ fenToYuan(row.splitAmount) }}
        </template>
      </el-table-column>
      <el-table-column label="差额(交易-履约)" width="160">
        <template #default="{ row }">
          <el-tag :type="diffTagType(row.tradeMinusFulfillment)">
            {{ fenToYuan(row.tradeMinusFulfillment) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="差额(交易-提成-分账)" width="190">
        <template #default="{ row }">
          <el-tag :type="diffTagType(row.tradeMinusCommissionSplit)">
            {{ fenToYuan(row.tradeMinusCommissionSplit) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" prop="status" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">
            {{ statusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="问题编码" min-width="220" prop="issueCodes" show-overflow-tooltip />
      <el-table-column label="来源" prop="source" width="120" />
      <el-table-column label="操作人" prop="operator" width="120" />
      <el-table-column :formatter="dateFormatter" label="执行时间" prop="reconciledAt" width="180" />
      <el-table-column align="center" fixed="right" label="操作" width="130">
        <template #default="{ row }">
          <el-button
            v-hasPermi="['trade:after-sale:query']"
            link
            type="primary"
            @click="openRelatedTicket(row)"
          >
            查看关联工单
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

  <Dialog v-model="runDialogVisible" title="手工执行四账对账" width="460px">
    <el-form :model="runForm" label-width="110px">
      <el-form-item label="业务日期（可选）">
        <el-date-picker
          v-model="runForm.bizDate"
          class="!w-full"
          clearable
          placeholder="不填则后端默认昨日"
          type="date"
          value-format="YYYY-MM-DD"
        />
      </el-form-item>
      <el-form-item label="触发来源">
        <el-input v-model="runForm.source" disabled />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="runLoading" @click="runDialogVisible = false">取消</el-button>
      <el-button :loading="runLoading" type="primary" @click="handleRun">确认执行</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import * as FourAccountReconcileApi from '@/api/mall/booking/fourAccountReconcile'
import { useRouter } from 'vue-router'

defineOptions({ name: 'MallBookingFourAccountReconcileIndex' })

const message = useMessage()
const router = useRouter()

const loading = ref(false)
const runLoading = ref(false)
const runDialogVisible = ref(false)
const total = ref(0)
const list = ref<FourAccountReconcileApi.FourAccountReconcileVO[]>([])

const queryParams = reactive<FourAccountReconcileApi.FourAccountReconcilePageReq>({
  pageNo: 1,
  pageSize: 10,
  bizDate: undefined,
  status: undefined,
  source: undefined,
  issueCode: undefined
})

const runForm = reactive<FourAccountReconcileApi.FourAccountReconcileRunReq>({
  bizDate: '',
  source: 'MANUAL'
})

const formatDate = (date: Date): string => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const getYesterday = () => {
  const date = new Date()
  date.setDate(date.getDate() - 1)
  return formatDate(date)
}

const normalizeQuery = () => {
  queryParams.source = (queryParams.source || '').trim().toUpperCase() || undefined
  queryParams.issueCode = (queryParams.issueCode || '').trim().toUpperCase() || undefined
}

const statusText = (status?: number) => {
  if (status === 10) return '通过'
  if (status === 20) return '告警'
  return '-'
}

const statusTagType = (status?: number) => {
  if (status === 10) return 'success'
  if (status === 20) return 'warning'
  return 'info'
}

const diffTagType = (amount?: number) => {
  return Number(amount || 0) === 0 ? 'success' : 'danger'
}

const fenToYuan = (fen?: number) => {
  return (Number(fen || 0) / 100).toFixed(2)
}

const getList = async () => {
  loading.value = true
  try {
    normalizeQuery()
    const data = await FourAccountReconcileApi.getFourAccountReconcilePage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
  } catch (error: any) {
    list.value = []
    total.value = 0
    message.error(error?.msg || '四账对账列表查询失败')
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
  queryParams.bizDate = undefined
  queryParams.status = undefined
  queryParams.source = undefined
  queryParams.issueCode = undefined
  getList()
}

const openRunDialog = () => {
  runForm.bizDate = getYesterday()
  runForm.source = 'MANUAL'
  runDialogVisible.value = true
}

const handleRun = async () => {
  if (runLoading.value) {
    return
  }
  runLoading.value = true
  try {
    const payload: FourAccountReconcileApi.FourAccountReconcileRunReq = {
      source: 'MANUAL'
    }
    const bizDate = (runForm.bizDate || '').trim()
    if (bizDate) {
      payload.bizDate = bizDate
    }
    const id = await FourAccountReconcileApi.runFourAccountReconcile(payload)
    message.success(`手工执行成功，记录ID：${id || '-'}`)
    runDialogVisible.value = false
    await getList()
  } catch (error: any) {
    message.error(error?.msg || '手工执行失败')
  } finally {
    runLoading.value = false
  }
}

const openRelatedTicket = (row: FourAccountReconcileApi.FourAccountReconcileVO) => {
  if (!row.bizDate) {
    message.warning('业务日期为空，无法跳转工单页')
    return
  }
  router.push({
    path: '/review-ticket',
    query: {
      ticketType: '40',
      sourceBizNo: `FOUR_ACCOUNT_RECONCILE:${row.bizDate}`
    }
  })
}

onMounted(() => {
  getList()
})
</script>
