<template>
  <ContentWrap>
    <el-form :inline="true" :model="queryParams" class="-mb-15px" label-width="96px">
      <el-form-item label="工单类型" prop="ticketType">
        <el-select v-model="queryParams.ticketType" class="!w-180px" clearable placeholder="请选择工单类型">
          <el-option v-for="item in ticketTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="工单状态" prop="status">
        <el-select v-model="queryParams.status" class="!w-140px" clearable placeholder="请选择状态">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="严重级别" prop="severity">
        <el-select v-model="queryParams.severity" class="!w-140px" clearable placeholder="请选择严重级别">
          <el-option v-for="item in severityOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="升级对象" prop="escalateTo">
        <el-input
          v-model="queryParams.escalateTo"
          class="!w-180px"
          clearable
          placeholder="例如 HQ_AFTER_SALE"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="路由ID" prop="routeId">
        <el-input-number v-model="queryParams.routeId" :min="1" class="!w-180px" controls-position="right" />
      </el-form-item>
      <el-form-item label="路由作用域" prop="routeScope">
        <el-select v-model="queryParams.routeScope" class="!w-200px" clearable placeholder="请选择路由作用域">
          <el-option v-for="item in routeScopeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="是否逾期" prop="overdue">
        <el-select v-model="queryParams.overdue" class="!w-120px" clearable placeholder="全部">
          <el-option :value="true" label="是" />
          <el-option :value="false" label="否" />
        </el-select>
      </el-form-item>
      <el-form-item label="最近动作" prop="lastActionCode">
        <el-input
          v-model="queryParams.lastActionCode"
          class="!w-180px"
          clearable
          placeholder="例如 MANUAL_RESOLVE"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="来源业务号" prop="sourceBizNo">
        <el-input v-model="queryParams.sourceBizNo" class="!w-180px" clearable placeholder="请输入来源业务号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="售后单ID" prop="afterSaleId">
        <el-input-number v-model="queryParams.afterSaleId" :min="1" class="!w-180px" controls-position="right" />
      </el-form-item>
      <el-form-item label="订单ID" prop="orderId">
        <el-input-number v-model="queryParams.orderId" :min="1" class="!w-180px" controls-position="right" />
      </el-form-item>
      <el-form-item label="用户ID" prop="userId">
        <el-input-number v-model="queryParams.userId" :min="1" class="!w-180px" controls-position="right" />
      </el-form-item>
      <el-form-item label="创建时间" prop="createTime">
        <el-date-picker
          v-model="queryParams.createTime"
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
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="ID" prop="id" width="90" />
      <el-table-column label="工单类型" min-width="130">
        <template #default="{ row }">
          {{ ticketTypeLabel(row.ticketType) }}
        </template>
      </el-table-column>
      <el-table-column label="严重级别" prop="severity" width="100" />
      <el-table-column label="升级对象" prop="escalateTo" min-width="150" />
      <el-table-column label="路由ID" prop="routeId" width="90" />
      <el-table-column label="路由作用域" prop="routeScope" min-width="140">
        <template #default="{ row }">
          {{ routeScopeLabel(row.routeScope) }}
        </template>
      </el-table-column>
      <el-table-column label="决策顺序" prop="routeDecisionOrder" min-width="260" show-overflow-tooltip />
      <el-table-column label="是否逾期" width="100">
        <template #default="{ row }">
          <el-tag :type="row.overdue ? 'danger' : 'success'">{{ row.overdue ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 10 ? 'success' : 'warning'">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="最近动作编码" prop="lastActionCode" min-width="150" />
      <el-table-column :formatter="dateFormatter" label="最近动作时间" prop="lastActionTime" width="180" />
      <el-table-column :formatter="dateFormatter" label="创建时间" prop="createTime" width="180" />
      <el-table-column align="center" fixed="right" label="操作" min-width="160">
        <template #default="{ row }">
          <el-button v-hasPermi="['trade:after-sale:query']" link type="primary" @click="openDetail(row.id)">查看详情</el-button>
          <el-button
            v-if="row.status === 0"
            v-hasPermi="['trade:after-sale:refund']"
            link
            type="warning"
            @click="openResolve(row)"
          >
            收口
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

  <el-dialog v-model="detailVisible" title="工单详情" width="860px">
    <el-descriptions v-loading="detailLoading" :column="2" border>
      <el-descriptions-item label="工单ID">{{ detailData.id || '-' }}</el-descriptions-item>
      <el-descriptions-item label="工单类型">{{ ticketTypeLabel(detailData.ticketType) }}</el-descriptions-item>
      <el-descriptions-item label="售后单ID">{{ detailData.afterSaleId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="订单ID">{{ detailData.orderId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="用户ID">{{ detailData.userId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="来源业务号">{{ detailData.sourceBizNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="严重级别">{{ detailData.severity || '-' }}</el-descriptions-item>
      <el-descriptions-item label="升级对象">{{ detailData.escalateTo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="命中路由ID">{{ detailData.routeId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="路由作用域">{{ routeScopeLabel(detailData.routeScope) }}</el-descriptions-item>
      <el-descriptions-item label="决策顺序">{{ detailData.routeDecisionOrder || '-' }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ statusLabel(detailData.status) }}</el-descriptions-item>
      <el-descriptions-item label="最近动作编码">{{ detailData.lastActionCode || '-' }}</el-descriptions-item>
      <el-descriptions-item label="最近动作业务号">{{ detailData.lastActionBizNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="最近动作时间">{{ detailData.lastActionTime || '-' }}</el-descriptions-item>
      <el-descriptions-item label="收口时间">{{ detailData.resolvedTime || '-' }}</el-descriptions-item>
      <el-descriptions-item label="收口动作编码">{{ detailData.resolveActionCode || '-' }}</el-descriptions-item>
      <el-descriptions-item label="收口业务号">{{ detailData.resolveBizNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="命中原因" :span="2">{{ detailData.decisionReason || '-' }}</el-descriptions-item>
      <el-descriptions-item label="备注" :span="2">{{ detailData.remark || '-' }}</el-descriptions-item>
    </el-descriptions>
    <template #footer>
      <el-button @click="detailVisible = false">关闭</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="resolveVisible" title="工单收口" width="560px">
    <el-form ref="resolveFormRef" :model="resolveFormData" :rules="resolveRules" label-width="110px">
      <el-form-item label="工单ID" prop="id">
        <el-input :model-value="resolveFormData.id" disabled />
      </el-form-item>
      <el-form-item label="收口动作编码" prop="resolveActionCode">
        <el-input v-model="resolveFormData.resolveActionCode" maxlength="64" placeholder="例如 MANUAL_RESOLVE" />
      </el-form-item>
      <el-form-item label="收口业务号" prop="resolveBizNo">
        <el-input v-model="resolveFormData.resolveBizNo" maxlength="64" placeholder="例如 OPS-202603030001" />
      </el-form-item>
      <el-form-item label="收口备注" prop="resolveRemark">
        <el-input
          v-model="resolveFormData.resolveRemark"
          :rows="3"
          maxlength="255"
          placeholder="请输入收口备注（可选）"
          show-word-limit
          type="textarea"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="resolveLoading" @click="resolveVisible = false">取消</el-button>
      <el-button :loading="resolveLoading" type="primary" @click="submitResolve">确认收口</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import * as ReviewTicketApi from '@/api/mall/trade/reviewTicket'

defineOptions({ name: 'TradeReviewTicketIndex' })

const message = useMessage()

const ticketTypeOptions = [
  { label: '10 售后复核', value: 10 },
  { label: '20 服务履约', value: 20 },
  { label: '30 提成争议', value: 30 }
]
const statusOptions = [
  { label: '待处理', value: 0 },
  { label: '已收口', value: 10 }
]
const severityOptions = [
  { label: 'P0', value: 'P0' },
  { label: 'P1', value: 'P1' }
]
const routeScopeOptions = [
  { label: '规则编码', value: 'RULE' },
  { label: '工单类型+严重级别', value: 'TYPE_SEVERITY' },
  { label: '工单类型默认', value: 'TYPE_DEFAULT' },
  { label: '全局默认', value: 'GLOBAL_DEFAULT' }
]

const loading = ref(false)
const total = ref(0)
const list = ref<ReviewTicketApi.ReviewTicketVO[]>([])

const queryParams = reactive<ReviewTicketApi.ReviewTicketPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  ticketType: undefined,
  status: undefined,
  severity: undefined,
  escalateTo: undefined,
  routeId: undefined,
  routeScope: undefined,
  overdue: undefined,
  lastActionCode: undefined,
  sourceBizNo: undefined,
  afterSaleId: undefined,
  orderId: undefined,
  userId: undefined,
  createTime: undefined
})

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailData = reactive<ReviewTicketApi.ReviewTicketVO>({})

const resolveVisible = ref(false)
const resolveLoading = ref(false)
const resolveFormRef = ref()
const resolveFormData = reactive<ReviewTicketApi.ReviewTicketResolveReqVO>({
  id: 0,
  resolveActionCode: 'MANUAL_RESOLVE',
  resolveBizNo: '',
  resolveRemark: ''
})
const resolveRules = {
  id: [{ required: true, message: '工单ID不能为空', trigger: 'change' }],
  resolveActionCode: [{ max: 64, message: '收口动作编码长度不能超过 64', trigger: 'blur' }],
  resolveBizNo: [{ max: 64, message: '收口业务号长度不能超过 64', trigger: 'blur' }],
  resolveRemark: [{ max: 255, message: '收口备注长度不能超过 255', trigger: 'blur' }]
}

const ticketTypeLabel = (value?: number) => {
  if (value === 10) return '售后复核'
  if (value === 20) return '服务履约'
  if (value === 30) return '提成争议'
  return value === undefined ? '-' : String(value)
}

const statusLabel = (value?: number) => {
  if (value === 0) return '待处理'
  if (value === 10) return '已收口'
  return value === undefined ? '-' : String(value)
}

const routeScopeLabel = (value?: string) => {
  if (value === 'RULE') return '规则编码'
  if (value === 'TYPE_SEVERITY') return '工单类型+严重级别'
  if (value === 'TYPE_DEFAULT') return '工单类型默认'
  if (value === 'GLOBAL_DEFAULT') return '全局默认'
  return value || '-'
}

const normalizeQuery = () => {
  queryParams.escalateTo = (queryParams.escalateTo || '').trim().toUpperCase() || undefined
  queryParams.lastActionCode = (queryParams.lastActionCode || '').trim().toUpperCase() || undefined
  queryParams.routeScope = (queryParams.routeScope || '').trim().toUpperCase() || undefined
  queryParams.sourceBizNo = (queryParams.sourceBizNo || '').trim() || undefined
}

const getList = async () => {
  loading.value = true
  try {
    normalizeQuery()
    const data = await ReviewTicketApi.getReviewTicketPage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
  } catch (error: any) {
    list.value = []
    total.value = 0
    message.error(error?.msg || '工单列表查询失败')
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
  queryParams.ticketType = undefined
  queryParams.status = undefined
  queryParams.severity = undefined
  queryParams.escalateTo = undefined
  queryParams.routeId = undefined
  queryParams.routeScope = undefined
  queryParams.overdue = undefined
  queryParams.lastActionCode = undefined
  queryParams.sourceBizNo = undefined
  queryParams.afterSaleId = undefined
  queryParams.orderId = undefined
  queryParams.userId = undefined
  queryParams.createTime = undefined
  getList()
}

const openDetail = async (id?: number) => {
  if (!id) {
    return
  }
  detailVisible.value = true
  detailLoading.value = true
  try {
    const data = await ReviewTicketApi.getReviewTicket(id)
    Object.assign(detailData, data)
  } catch (error: any) {
    message.error(error?.msg || '工单详情获取失败')
  } finally {
    detailLoading.value = false
  }
}

const openResolve = (row: ReviewTicketApi.ReviewTicketVO) => {
  if (!row.id) {
    return
  }
  resolveFormData.id = row.id
  resolveFormData.resolveActionCode = 'MANUAL_RESOLVE'
  resolveFormData.resolveBizNo = ''
  resolveFormData.resolveRemark = ''
  resolveFormRef.value?.clearValidate()
  resolveVisible.value = true
}

const submitResolve = async () => {
  if (resolveLoading.value) {
    return
  }
  const valid = await resolveFormRef.value?.validate()
  if (!valid) {
    return
  }
  resolveLoading.value = true
  try {
    const payload: ReviewTicketApi.ReviewTicketResolveReqVO = {
      id: resolveFormData.id,
      resolveActionCode: (resolveFormData.resolveActionCode || '').trim().toUpperCase() || undefined,
      resolveBizNo: (resolveFormData.resolveBizNo || '').trim() || undefined,
      resolveRemark: (resolveFormData.resolveRemark || '').trim() || undefined
    }
    await ReviewTicketApi.resolveReviewTicket(payload)
    message.success('工单收口成功')
    resolveVisible.value = false
    await getList()
    if (detailVisible.value && detailData.id) {
      const latest = await ReviewTicketApi.getReviewTicket(detailData.id)
      Object.assign(detailData, latest)
    }
  } catch (error: any) {
    message.error(error?.msg || '工单收口失败')
  } finally {
    resolveLoading.value = false
  }
}

onMounted(() => {
  getList()
})
</script>
