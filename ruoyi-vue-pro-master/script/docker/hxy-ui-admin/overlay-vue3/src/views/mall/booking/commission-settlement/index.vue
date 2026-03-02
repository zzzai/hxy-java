<template>
  <doc-alert title="技师佣金结算审批流" url="https://doc.iocoder.cn/" />

  <ContentWrap>
    <el-form :inline="true" :model="queryParams" class="-mb-15px" label-width="88px">
      <el-form-item label="结算单号" prop="settlementNo">
        <el-input
          v-model="queryParams.settlementNo"
          class="!w-220px"
          clearable
          placeholder="请输入结算单号"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="门店ID" prop="storeId">
        <el-input
          v-model="queryParams.storeId"
          class="!w-150px"
          clearable
          placeholder="请输入门店ID"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="技师ID" prop="technicianId">
        <el-input
          v-model="queryParams.technicianId"
          class="!w-150px"
          clearable
          placeholder="请输入技师ID"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" class="!w-170px" clearable placeholder="请选择状态">
          <el-option :value="0" label="草稿" />
          <el-option :value="10" label="待审核" />
          <el-option :value="20" label="已通过" />
          <el-option :value="30" label="已驳回" />
          <el-option :value="40" label="已打款" />
          <el-option :value="50" label="已过期" />
        </el-select>
      </el-form-item>
      <el-form-item label="预警标记" prop="reviewWarned">
        <el-select v-model="queryParams.reviewWarned" class="!w-130px" clearable placeholder="全部">
          <el-option :value="true" label="已预警" />
          <el-option :value="false" label="未预警" />
        </el-select>
      </el-form-item>
      <el-form-item label="升级标记" prop="reviewEscalated">
        <el-select v-model="queryParams.reviewEscalated" class="!w-130px" clearable placeholder="全部">
          <el-option :value="true" label="已升级" />
          <el-option :value="false" label="未升级" />
        </el-select>
      </el-form-item>
      <el-form-item label="SLA状态" prop="overdue">
        <el-select v-model="queryParams.overdue" class="!w-130px" clearable placeholder="全部">
          <el-option :value="true" label="已超时" />
          <el-option :value="false" label="未超时" />
        </el-select>
      </el-form-item>
      <el-form-item label="SLA截止" prop="reviewDeadlineTime">
        <el-date-picker
          v-model="queryParams.reviewDeadlineTime"
          :default-time="[new Date('1 00:00:00'), new Date('1 23:59:59')]"
          class="!w-340px"
          end-placeholder="截止结束"
          start-placeholder="截止开始"
          type="datetimerange"
          value-format="YYYY-MM-DD HH:mm:ss"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery">
          <Icon class="mr-5px" icon="ep:search" />
          搜索
        </el-button>
        <el-button @click="resetQuery">
          <Icon class="mr-5px" icon="ep:refresh" />
          重置
        </el-button>
        <el-button
          v-hasPermi="['booking:commission:query']"
          plain
          type="primary"
          @click="goOutboxPage"
        >
          <Icon class="mr-5px" icon="ep:bell" />
          通知出站
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="ID" prop="id" width="90" />
      <el-table-column label="结算单号" min-width="220" prop="settlementNo" show-overflow-tooltip />
      <el-table-column label="门店ID" prop="storeId" width="100" />
      <el-table-column label="技师ID" prop="technicianId" width="100" />
      <el-table-column label="状态" prop="status" width="110">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">
            {{ statusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="佣金条数" prop="commissionCount" width="100" />
      <el-table-column label="总金额(元)" width="120">
        <template #default="{ row }">
          {{ fenToYuan(row.totalCommissionAmount) }}
        </template>
      </el-table-column>
      <el-table-column :formatter="dateFormatter" label="提审时间" prop="reviewSubmitTime" width="180" />
      <el-table-column :formatter="dateFormatter" label="SLA截止" prop="reviewDeadlineTime" width="180" />
      <el-table-column label="SLA状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.overdue ? 'danger' : 'success'">
            {{ row.overdue ? '已超时' : '正常' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :formatter="dateFormatter" label="审核时间" prop="reviewedTime" width="180" />
      <el-table-column :formatter="dateFormatter" label="打款时间" prop="paidTime" width="180" />
      <el-table-column align="center" fixed="right" label="操作" width="320">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 0"
            v-hasPermi="['booking:commission:settlement']"
            link
            type="primary"
            @click="openSubmitDialog(row)"
          >
            提审
          </el-button>
          <el-button
            v-if="row.status === 10"
            v-hasPermi="['booking:commission:settlement']"
            link
            type="success"
            @click="handleApprove(row)"
          >
            通过
          </el-button>
          <el-button
            v-if="row.status === 10"
            v-hasPermi="['booking:commission:settlement']"
            link
            type="danger"
            @click="openRejectDialog(row)"
          >
            驳回
          </el-button>
          <el-button
            v-if="row.status === 20"
            v-hasPermi="['booking:commission:settlement']"
            link
            type="warning"
            @click="openPayDialog(row)"
          >
            打款
          </el-button>
          <el-button v-hasPermi="['booking:commission:query']" link type="info" @click="openLogDrawer(row)">
            日志
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

  <Dialog v-model="submitDialogVisible" title="提交审核" width="520px">
    <el-form :model="submitForm" label-width="100px">
      <el-form-item label="结算单ID">
        <el-input v-model="submitForm.id" disabled />
      </el-form-item>
      <el-form-item label="SLA分钟">
        <el-input-number v-model="submitForm.slaMinutes" :max="10080" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="submitForm.remark" :rows="3" maxlength="255" show-word-limit type="textarea" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitDialogVisible = false">取消</el-button>
      <el-button :loading="submitLoading" type="primary" @click="handleSubmit">确认提审</el-button>
    </template>
  </Dialog>

  <Dialog v-model="rejectDialogVisible" title="驳回结算单" width="520px">
    <el-form :model="rejectForm" label-width="100px">
      <el-form-item label="结算单ID">
        <el-input v-model="rejectForm.id" disabled />
      </el-form-item>
      <el-form-item label="驳回原因">
        <el-input
          v-model="rejectForm.rejectReason"
          :rows="3"
          maxlength="255"
          placeholder="请输入驳回原因"
          show-word-limit
          type="textarea"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="rejectDialogVisible = false">取消</el-button>
      <el-button :loading="rejectLoading" type="danger" @click="handleReject">确认驳回</el-button>
    </template>
  </Dialog>

  <Dialog v-model="payDialogVisible" title="确认打款" width="560px">
    <el-form :model="payForm" label-width="100px">
      <el-form-item label="结算单ID">
        <el-input v-model="payForm.id" disabled />
      </el-form-item>
      <el-form-item label="打款凭证号">
        <el-input v-model="payForm.payVoucherNo" maxlength="64" placeholder="请输入打款凭证号" />
      </el-form-item>
      <el-form-item label="打款备注">
        <el-input
          v-model="payForm.payRemark"
          :rows="3"
          maxlength="255"
          placeholder="请输入打款备注"
          show-word-limit
          type="textarea"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="payDialogVisible = false">取消</el-button>
      <el-button :loading="payLoading" type="warning" @click="handlePay">确认打款</el-button>
    </template>
  </Dialog>

  <el-drawer v-model="logDrawerVisible" size="60%" title="结算单操作日志">
    <el-table v-loading="logLoading" :data="logList">
      <el-table-column label="动作" prop="action" width="160" />
      <el-table-column label="前状态" prop="fromStatus" width="100" />
      <el-table-column label="后状态" prop="toStatus" width="100" />
      <el-table-column label="操作人ID" prop="operatorId" width="120" />
      <el-table-column label="操作人类型" prop="operatorType" width="120" />
      <el-table-column label="备注" min-width="240" prop="operateRemark" show-overflow-tooltip />
      <el-table-column :formatter="dateFormatter" label="动作时间" prop="actionTime" width="180" />
    </el-table>
  </el-drawer>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import * as CommissionSettlementApi from '@/api/mall/booking/commissionSettlement'
import { ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'

defineOptions({ name: 'MallBookingCommissionSettlementIndex' })

const message = useMessage()
const router = useRouter()

const loading = ref(false)
const total = ref(0)
const list = ref<CommissionSettlementApi.CommissionSettlement[]>([])

const queryParams = ref<CommissionSettlementApi.CommissionSettlementPageReq>({
  pageNo: 1,
  pageSize: 10,
  settlementNo: undefined,
  storeId: undefined,
  technicianId: undefined,
  status: undefined,
  reviewerId: undefined,
  reviewWarned: undefined,
  reviewEscalated: undefined,
  reviewDeadlineTime: undefined,
  overdue: undefined
})

const submitDialogVisible = ref(false)
const submitLoading = ref(false)
const submitForm = ref<CommissionSettlementApi.CommissionSettlementSubmitReq>({
  id: 0,
  slaMinutes: 120,
  remark: ''
})

const rejectDialogVisible = ref(false)
const rejectLoading = ref(false)
const rejectForm = ref<CommissionSettlementApi.CommissionSettlementRejectReq>({
  id: 0,
  rejectReason: ''
})

const payDialogVisible = ref(false)
const payLoading = ref(false)
const payForm = ref<CommissionSettlementApi.CommissionSettlementPayReq>({
  id: 0,
  payVoucherNo: '',
  payRemark: ''
})

const logDrawerVisible = ref(false)
const logLoading = ref(false)
const logList = ref<CommissionSettlementApi.CommissionSettlementLog[]>([])

const statusText = (status?: number) => {
  if (status === 0) return '草稿'
  if (status === 10) return '待审核'
  if (status === 20) return '已通过'
  if (status === 30) return '已驳回'
  if (status === 40) return '已打款'
  if (status === 50) return '已过期'
  return '未知'
}

const statusTagType = (status?: number) => {
  if (status === 0) return 'info'
  if (status === 10) return 'warning'
  if (status === 20) return 'success'
  if (status === 30) return 'danger'
  if (status === 40) return 'primary'
  if (status === 50) return 'danger'
  return 'info'
}

const fenToYuan = (fen?: number) => {
  const safeFen = Number(fen || 0)
  return (safeFen / 100).toFixed(2)
}

const getList = async () => {
  loading.value = true
  try {
    const data = await CommissionSettlementApi.getSettlementPage(queryParams.value)
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.value.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryParams.value = {
    pageNo: 1,
    pageSize: 10,
    settlementNo: undefined,
    storeId: undefined,
    technicianId: undefined,
    status: undefined,
    reviewerId: undefined,
    reviewWarned: undefined,
    reviewEscalated: undefined,
    reviewDeadlineTime: undefined,
    overdue: undefined
  }
  getList()
}

const openSubmitDialog = (row: CommissionSettlementApi.CommissionSettlement) => {
  submitForm.value = {
    id: row.id,
    slaMinutes: 120,
    remark: ''
  }
  submitDialogVisible.value = true
}

const handleSubmit = async () => {
  submitLoading.value = true
  try {
    await CommissionSettlementApi.submitSettlement(submitForm.value)
    message.success('提审成功')
    submitDialogVisible.value = false
    await getList()
  } finally {
    submitLoading.value = false
  }
}

const handleApprove = async (row: CommissionSettlementApi.CommissionSettlement) => {
  const { value } = await ElMessageBox.prompt('请输入审批备注（可选）', '审批通过', {
    confirmButtonText: '确认通过',
    cancelButtonText: '取消',
    inputPlaceholder: '例如：核对无误',
    inputType: 'textarea'
  }).catch(() => ({ value: null }))
  if (value === null) return
  await CommissionSettlementApi.approveSettlement({ id: row.id, remark: value || '' })
  message.success('审批通过')
  await getList()
}

const openRejectDialog = (row: CommissionSettlementApi.CommissionSettlement) => {
  rejectForm.value = {
    id: row.id,
    rejectReason: ''
  }
  rejectDialogVisible.value = true
}

const handleReject = async () => {
  if (!rejectForm.value.rejectReason?.trim()) {
    message.warning('请输入驳回原因')
    return
  }
  rejectLoading.value = true
  try {
    await CommissionSettlementApi.rejectSettlement(rejectForm.value)
    message.success('已驳回')
    rejectDialogVisible.value = false
    await getList()
  } finally {
    rejectLoading.value = false
  }
}

const openPayDialog = (row: CommissionSettlementApi.CommissionSettlement) => {
  payForm.value = {
    id: row.id,
    payVoucherNo: '',
    payRemark: ''
  }
  payDialogVisible.value = true
}

const handlePay = async () => {
  if (!payForm.value.payVoucherNo?.trim()) {
    message.warning('请输入打款凭证号')
    return
  }
  if (!payForm.value.payRemark?.trim()) {
    message.warning('请输入打款备注')
    return
  }
  payLoading.value = true
  try {
    await CommissionSettlementApi.paySettlement(payForm.value)
    message.success('确认打款成功')
    payDialogVisible.value = false
    await getList()
  } finally {
    payLoading.value = false
  }
}

const openLogDrawer = async (row: CommissionSettlementApi.CommissionSettlement) => {
  logDrawerVisible.value = true
  logLoading.value = true
  try {
    logList.value = await CommissionSettlementApi.getSettlementLogList(row.id)
  } finally {
    logLoading.value = false
  }
}

const goOutboxPage = () => {
  router.push('/booking-commission-outbox')
}

onMounted(() => {
  getList()
})
</script>
