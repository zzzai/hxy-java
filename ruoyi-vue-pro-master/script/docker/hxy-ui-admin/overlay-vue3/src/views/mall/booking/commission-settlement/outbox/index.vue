<template>
  <doc-alert
    title="技师佣金结算通知出站"
    url="https://doc.iocoder.cn/"
  />

  <ContentWrap>
    <el-form :inline="true" :model="queryParams" class="-mb-15px" label-width="90px">
      <el-form-item label="结算单ID" prop="settlementId">
        <el-input
          v-model="queryParams.settlementId"
          class="!w-180px"
          clearable
          placeholder="请输入结算单ID"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="通知状态" prop="status">
        <el-select v-model="queryParams.status" class="!w-170px" clearable placeholder="请选择状态">
          <el-option :value="0" label="待发送" />
          <el-option :value="1" label="已发送" />
          <el-option :value="2" label="发送失败" />
        </el-select>
      </el-form-item>
      <el-form-item label="通知类型" prop="notifyType">
        <el-select v-model="queryParams.notifyType" class="!w-170px" clearable placeholder="请选择类型">
          <el-option value="P1_WARN" label="P1 预警" />
          <el-option value="P0_ESCALATE" label="P0 升级" />
        </el-select>
      </el-form-item>
      <el-form-item label="通知渠道" prop="channel">
        <el-select v-model="queryParams.channel" class="!w-160px" clearable placeholder="请选择渠道">
          <el-option value="IN_APP" label="站内信" />
        </el-select>
      </el-form-item>
      <el-form-item label="审计动作" prop="lastActionCode">
        <el-select
          v-model="queryParams.lastActionCode"
          class="!w-180px"
          clearable
          placeholder="请选择动作"
        >
          <el-option value="CREATE" label="创建" />
          <el-option value="DISPATCH_SUCCESS" label="发送成功" />
          <el-option value="DISPATCH_FAILED" label="发送失败" />
          <el-option value="MANUAL_RETRY" label="人工重试" />
        </el-select>
      </el-form-item>
      <el-form-item label="审计业务号" prop="lastActionBizNo">
        <el-input
          v-model="queryParams.lastActionBizNo"
          class="!w-220px"
          clearable
          placeholder="请输入业务号"
          @keyup.enter="handleQuery"
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
          v-hasPermi="['booking:commission:settlement']"
          :disabled="selectedIds.length === 0"
          :loading="retryLoading"
          plain
          type="warning"
          @click="handleBatchRetry"
        >
          <Icon class="mr-5px" icon="ep:refresh-right" />
          批量重试
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" />
      <el-table-column label="ID" prop="id" width="90" />
      <el-table-column label="结算单ID" prop="settlementId" width="110" />
      <el-table-column label="通知类型" prop="notifyType" width="120">
        <template #default="{ row }">
          <el-tag :type="row.notifyType === 'P0_ESCALATE' ? 'danger' : 'warning'">
            {{ row.notifyType || '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="渠道" prop="channel" width="100" />
      <el-table-column label="优先级" prop="severity" width="90" />
      <el-table-column label="状态" prop="status" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">
            {{ statusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="重试次数" prop="retryCount" width="90" />
      <el-table-column :formatter="dateFormatter" label="下次重试时间" prop="nextRetryTime" width="180" />
      <el-table-column :formatter="dateFormatter" label="发送时间" prop="sentTime" width="180" />
      <el-table-column label="审计动作" prop="lastActionCode" width="140" />
      <el-table-column label="审计业务号" prop="lastActionBizNo" min-width="180" show-overflow-tooltip />
      <el-table-column :formatter="dateFormatter" label="动作时间" prop="lastActionTime" width="180" />
      <el-table-column label="最后错误" min-width="220" prop="lastErrorMsg" show-overflow-tooltip />
      <el-table-column :formatter="dateFormatter" label="更新时间" prop="updateTime" width="180" />
      <el-table-column align="center" fixed="right" label="操作" width="120">
        <template #default="{ row }">
          <el-button
            v-if="canRetry(row)"
            v-hasPermi="['booking:commission:settlement']"
            :loading="retryingIds.includes(row.id)"
            link
            type="warning"
            @click="handleSingleRetry(row)"
          >
            重试
          </el-button>
          <span v-else class="text-[var(--el-text-color-secondary)]">-</span>
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
import * as CommissionSettlementApi from '@/api/mall/booking/commissionSettlement'
import { ElMessageBox } from 'element-plus'

defineOptions({ name: 'MallBookingCommissionSettlementOutboxIndex' })

const message = useMessage()

const loading = ref(false)
const retryLoading = ref(false)
const total = ref(0)
const list = ref<CommissionSettlementApi.CommissionSettlementNotifyOutbox[]>([])
const selectedIds = ref<number[]>([])
const retryingIds = ref<number[]>([])

const queryParams = ref<CommissionSettlementApi.CommissionSettlementNotifyOutboxPageReq>({
  pageNo: 1,
  pageSize: 10,
  settlementId: undefined,
  status: undefined,
  notifyType: undefined,
  channel: undefined,
  lastActionCode: undefined,
  lastActionBizNo: undefined
})

const getList = async () => {
  loading.value = true
  try {
    const data = await CommissionSettlementApi.getNotifyOutboxPage(queryParams.value)
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
    settlementId: undefined,
    status: undefined,
    notifyType: undefined,
    channel: undefined,
    lastActionCode: undefined,
    lastActionBizNo: undefined
  }
  selectedIds.value = []
  getList()
}

const handleSelectionChange = (rows: CommissionSettlementApi.CommissionSettlementNotifyOutbox[]) => {
  selectedIds.value = rows.map((row) => row.id).filter((id) => !!id)
}

const statusText = (status?: number) => {
  if (status === 0) {
    return '待发送'
  }
  if (status === 1) {
    return '已发送'
  }
  if (status === 2) {
    return '发送失败'
  }
  return '未知'
}

const statusTagType = (status?: number) => {
  if (status === 0) {
    return 'warning'
  }
  if (status === 1) {
    return 'success'
  }
  if (status === 2) {
    return 'danger'
  }
  return 'info'
}

const canRetry = (row: CommissionSettlementApi.CommissionSettlementNotifyOutbox) => row.status !== 1

const promptRetryReason = async () => {
  const { value } = await ElMessageBox.prompt('请输入重试原因（可选）', '通知重试', {
    confirmButtonText: '确认重试',
    cancelButtonText: '取消',
    inputPlaceholder: '例如：模板已恢复，人工触发重试',
    inputType: 'textarea'
  })
  return value || ''
}

const handleBatchRetry = async () => {
  if (!selectedIds.value.length) {
    message.warning('请先勾选至少一条记录')
    return
  }
  try {
    const reason = await promptRetryReason()
    retryLoading.value = true
    const result = await CommissionSettlementApi.retryNotifyOutboxBatch({
      ids: selectedIds.value,
      reason
    })
    message.success(
      `批量重试完成：成功 ${result.retriedCount}，跳过(不存在) ${result.skippedNotExistsCount}，跳过(状态非法) ${result.skippedStatusInvalidCount}`
    )
    selectedIds.value = []
    await getList()
  } catch (error: any) {
    if (error !== 'cancel') {
      // 请求失败由全局错误拦截提示，这里不再重复提示
    }
  } finally {
    retryLoading.value = false
  }
}

const handleSingleRetry = async (row: CommissionSettlementApi.CommissionSettlementNotifyOutbox) => {
  if (!row?.id) {
    return
  }
  try {
    const reason = await promptRetryReason()
    retryingIds.value.push(row.id)
    const result = await CommissionSettlementApi.retryNotifyOutboxBatch({ ids: [row.id], reason })
    if (result.retriedCount > 0) {
      message.success('已提交重试 1 条')
    } else if (result.skippedNotExistsCount > 0) {
      message.warning('记录不存在，已跳过')
    } else if (result.skippedStatusInvalidCount > 0) {
      message.warning('状态不允许重试，已跳过')
    }
    await getList()
  } catch (error: any) {
    if (error !== 'cancel') {
      // 请求失败由全局错误拦截提示，这里不再重复提示
    }
  } finally {
    retryingIds.value = retryingIds.value.filter((id) => id !== row.id)
  }
}

onMounted(() => {
  getList()
})
</script>
