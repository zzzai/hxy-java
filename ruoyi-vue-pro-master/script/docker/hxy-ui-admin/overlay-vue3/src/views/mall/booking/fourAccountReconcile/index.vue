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
      <el-table-column label="业务日期" width="120">
        <template #default="{ row }">
          {{ textOrDash(row.bizDate) }}
        </template>
      </el-table-column>
      <el-table-column label="来源业务号" min-width="240" show-overflow-tooltip>
        <template #default="{ row }">
          {{ resolveSourceBizNo(row) }}
        </template>
      </el-table-column>
      <el-table-column label="交易账(元)" width="130">
        <template #default="{ row }">
          {{ fenToYuanOrDash(row.tradeAmount) }}
        </template>
      </el-table-column>
      <el-table-column label="履约账(元)" width="130">
        <template #default="{ row }">
          {{ fenToYuanOrDash(row.fulfillmentAmount) }}
        </template>
      </el-table-column>
      <el-table-column label="提成账(元)" width="130">
        <template #default="{ row }">
          {{ fenToYuanOrDash(row.commissionAmount) }}
        </template>
      </el-table-column>
      <el-table-column label="分账账(元)" width="130">
        <template #default="{ row }">
          {{ fenToYuanOrDash(row.splitAmount) }}
        </template>
      </el-table-column>
      <el-table-column label="差额(交易-履约)" width="160">
        <template #default="{ row }">
          <el-tag :type="diffTagType(row.tradeMinusFulfillment)">
            {{ fenToYuanOrDash(row.tradeMinusFulfillment) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="差额(交易-提成-分账)" width="190">
        <template #default="{ row }">
          <el-tag :type="diffTagType(row.tradeMinusCommissionSplit)">
            {{ fenToYuanOrDash(row.tradeMinusCommissionSplit) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">
            {{ statusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="问题编码" min-width="220" prop="issueCodes" show-overflow-tooltip>
        <template #default="{ row }">
          {{ textOrDash(row.issueCodes) }}
        </template>
      </el-table-column>
      <el-table-column label="关联工单ID" width="120">
        <template #default="{ row }">
          {{ textOrDash(row.relatedTicketId) }}
        </template>
      </el-table-column>
      <el-table-column label="工单状态" width="110">
        <template #default="{ row }">
          <el-tag :type="ticketStatusTagType(row.relatedTicketStatus)">
            {{ ticketStatusText(row.relatedTicketStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="严重级别" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.relatedTicketSeverity" :type="ticketSeverityTagType(row.relatedTicketSeverity)">
            {{ normalizeTicketSeverity(row.relatedTicketSeverity) }}
          </el-tag>
          <span v-else>{{ EMPTY_TEXT }}</span>
        </template>
      </el-table-column>
      <el-table-column label="来源" width="120">
        <template #default="{ row }">
          {{ textOrDash(row.source) }}
        </template>
      </el-table-column>
      <el-table-column label="操作人" width="120">
        <template #default="{ row }">
          {{ textOrDash(row.operator) }}
        </template>
      </el-table-column>
      <el-table-column :formatter="dateFormatter" label="执行时间" prop="reconciledAt" width="180" />
      <el-table-column align="center" fixed="right" label="操作" width="270">
        <template #default="{ row }">
          <el-button link type="info" @click="openDetailDrawer(row)">查看详情</el-button>
          <el-button link type="warning" @click="copySourceBizNo(row)">复制来源号</el-button>
          <el-button
            v-hasPermi="['trade:after-sale:query']"
            :disabled="!hasRelatedTicket(row)"
            :title="hasRelatedTicket(row) ? '' : '暂无关联工单'"
            link
            type="primary"
            @click="openRelatedTicket(row)"
          >
            跳转工单
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

  <el-drawer v-model="detailDrawerVisible" size="58%" title="四账对账详情">
    <div v-loading="detailLoading">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="记录ID">{{ textOrDash(detailData.id) }}</el-descriptions-item>
        <el-descriptions-item label="对账流水号">{{ textOrDash(detailData.reconcileNo) }}</el-descriptions-item>
        <el-descriptions-item label="业务日期">{{ textOrDash(detailData.bizDate) }}</el-descriptions-item>
        <el-descriptions-item label="来源业务号">{{ resolveSourceBizNo(detailData) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(detailData.status)">{{ statusText(detailData.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="问题编码">{{ textOrDash(detailData.issueCodes) }}</el-descriptions-item>
        <el-descriptions-item label="关联工单ID">{{ textOrDash(detailData.relatedTicketId) }}</el-descriptions-item>
        <el-descriptions-item label="工单状态">
          <el-tag :type="ticketStatusTagType(detailData.relatedTicketStatus)">
            {{ ticketStatusText(detailData.relatedTicketStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="严重级别">
          <el-tag v-if="detailData.relatedTicketSeverity" :type="ticketSeverityTagType(detailData.relatedTicketSeverity)">
            {{ normalizeTicketSeverity(detailData.relatedTicketSeverity) }}
          </el-tag>
          <span v-else>{{ EMPTY_TEXT }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="来源">{{ textOrDash(detailData.source) }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ textOrDash(detailData.operator) }}</el-descriptions-item>
        <el-descriptions-item label="执行时间">{{ textOrDash(detailData.reconciledAt) }}</el-descriptions-item>
      </el-descriptions>

      <el-descriptions :column="2" border class="mt-16px">
        <el-descriptions-item label="交易账(元)">{{ fenToYuanOrDash(detailData.tradeAmount) }}</el-descriptions-item>
        <el-descriptions-item label="履约账(元)">{{ fenToYuanOrDash(detailData.fulfillmentAmount) }}</el-descriptions-item>
        <el-descriptions-item label="提成账(元)">{{ fenToYuanOrDash(detailData.commissionAmount) }}</el-descriptions-item>
        <el-descriptions-item label="分账账(元)">{{ fenToYuanOrDash(detailData.splitAmount) }}</el-descriptions-item>
        <el-descriptions-item label="差额(交易-履约)">{{ fenToYuanOrDash(detailData.tradeMinusFulfillment) }}</el-descriptions-item>
        <el-descriptions-item label="差额(交易-提成-分账)">
          {{ fenToYuanOrDash(detailData.tradeMinusCommissionSplit) }}
        </el-descriptions-item>
      </el-descriptions>

      <div class="mt-16px">
        <div class="mb-8px font-500">issueDetailJson（结构化）</div>
        <el-alert v-if="detailIssueParseError" :closable="false" title="明细解析失败（原文保留）" type="warning" />
        <el-empty v-else-if="!detailIssueAvailable" description="无可用明细" />
        <el-descriptions v-else :column="2" border>
          <el-descriptions-item label="tradeAmount">{{ fenToYuanOrDash(detailIssueData.tradeAmount) }}</el-descriptions-item>
          <el-descriptions-item label="fulfillmentAmount">{{ fenToYuanOrDash(detailIssueData.fulfillmentAmount) }}</el-descriptions-item>
          <el-descriptions-item label="commissionAmount">{{ fenToYuanOrDash(detailIssueData.commissionAmount) }}</el-descriptions-item>
          <el-descriptions-item label="splitAmount">{{ fenToYuanOrDash(detailIssueData.splitAmount) }}</el-descriptions-item>
          <el-descriptions-item label="tradeMinusFulfillment">
            {{ fenToYuanOrDash(detailIssueData.tradeMinusFulfillment) }}
          </el-descriptions-item>
          <el-descriptions-item label="tradeMinusCommissionSplit">
            {{ fenToYuanOrDash(detailIssueData.tradeMinusCommissionSplit) }}
          </el-descriptions-item>
          <el-descriptions-item :span="2" label="issues">
            <div v-if="detailIssueTags.length" class="flex flex-wrap items-center gap-6px">
              <el-tag v-for="(issue, idx) in detailIssueTags" :key="`${issue}-${idx}`" type="warning">
                {{ issue }}
              </el-tag>
            </div>
            <span v-else>{{ EMPTY_TEXT }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="mt-16px">
        <div class="mb-8px font-500">issueDetailJson 原文</div>
        <el-input
          :model-value="detailData.issueDetailJson || EMPTY_TEXT"
          :rows="6"
          readonly
          type="textarea"
        />
      </div>
    </div>
  </el-drawer>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import * as FourAccountReconcileApi from '@/api/mall/booking/fourAccountReconcile'
import { useRouter } from 'vue-router'

defineOptions({ name: 'MallBookingFourAccountReconcileIndex' })

interface IssueDetailData {
  tradeAmount?: number
  fulfillmentAmount?: number
  commissionAmount?: number
  splitAmount?: number
  tradeMinusFulfillment?: number
  tradeMinusCommissionSplit?: number
}

const EMPTY_TEXT = '--'

const message = useMessage()
const router = useRouter()

const loading = ref(false)
const runLoading = ref(false)
const detailLoading = ref(false)
const runDialogVisible = ref(false)
const detailDrawerVisible = ref(false)
const total = ref(0)
const list = ref<FourAccountReconcileApi.FourAccountReconcileVO[]>([])
const detailData = ref<Partial<FourAccountReconcileApi.FourAccountReconcileVO>>({})
const detailIssueData = ref<IssueDetailData>({})
const detailIssueTags = ref<string[]>([])
const detailIssueAvailable = ref(false)
const detailIssueParseError = ref(false)

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

const createEmptyIssueDetailData = (): IssueDetailData => {
  return {
    tradeAmount: undefined,
    fulfillmentAmount: undefined,
    commissionAmount: undefined,
    splitAmount: undefined,
    tradeMinusFulfillment: undefined,
    tradeMinusCommissionSplit: undefined
  }
}

const isValidNumber = (value: any): value is number => {
  return typeof value === 'number' && !Number.isNaN(value)
}

const textOrDash = (value: any) => {
  if (value === undefined || value === null) {
    return EMPTY_TEXT
  }
  const text = String(value).trim()
  return text ? text : EMPTY_TEXT
}

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
  return EMPTY_TEXT
}

const statusTagType = (status?: number) => {
  if (status === 10) return 'success'
  if (status === 20) return 'warning'
  return 'info'
}

const ticketStatusText = (status?: number) => {
  if (status === 10) return '待处理'
  if (status === 20) return '已收口'
  return EMPTY_TEXT
}

const ticketStatusTagType = (status?: number) => {
  if (status === 10) return 'warning'
  if (status === 20) return 'success'
  return 'info'
}

const normalizeTicketSeverity = (severity?: string) => {
  return (severity || '').trim().toUpperCase()
}

const ticketSeverityTagType = (severity?: string) => {
  const normalized = normalizeTicketSeverity(severity)
  if (normalized === 'P0') return 'danger'
  if (normalized === 'P1') return 'warning'
  if (normalized === 'P2') return 'info'
  return ''
}

const diffTagType = (amount?: number) => {
  if (!isValidNumber(amount)) return 'info'
  return amount === 0 ? 'success' : 'danger'
}

const fenToYuan = (fen?: number) => {
  return (Number(fen || 0) / 100).toFixed(2)
}

const fenToYuanOrDash = (fen?: number) => {
  return isValidNumber(fen) ? fenToYuan(fen) : EMPTY_TEXT
}

const buildSourceBizNo = (bizDate?: string) => {
  return bizDate ? `FOUR_ACCOUNT_RECONCILE:${bizDate}` : ''
}

const resolveSourceBizNo = (row: Partial<FourAccountReconcileApi.FourAccountReconcileVO>) => {
  return row.sourceBizNo || buildSourceBizNo(row.bizDate) || EMPTY_TEXT
}

const copySourceBizNo = async (row: FourAccountReconcileApi.FourAccountReconcileVO) => {
  const sourceBizNo = resolveSourceBizNo(row)
  if (sourceBizNo === EMPTY_TEXT) {
    message.warning('来源业务号为空，无法复制')
    return
  }
  try {
    await navigator.clipboard.writeText(sourceBizNo)
    message.success('来源号复制成功')
  } catch {
    message.error('复制失败，请检查浏览器剪贴板权限')
  }
}

const hasRelatedTicket = (row: Partial<FourAccountReconcileApi.FourAccountReconcileVO>) => {
  return Boolean(row.relatedTicketId)
}

const parseNumber = (value: any): number | undefined => {
  if (value === undefined || value === null || value === '') {
    return undefined
  }
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : undefined
}

const normalizeIssueLabel = (issue: any): string => {
  if (typeof issue === 'string') {
    return issue.trim()
  }
  if (!issue || typeof issue !== 'object') {
    return ''
  }
  const code = String(issue.code || issue.issueCode || issue.type || '').trim()
  const messageText = String(issue.message || issue.msg || issue.reason || issue.detail || '').trim()
  if (code && messageText) {
    return `${code}: ${messageText}`
  }
  if (code) {
    return code
  }
  if (messageText) {
    return messageText
  }
  return ''
}

const parseIssueLabels = (payload: Record<string, any>) => {
  const issues = payload.issues || payload.issueList || payload.issueDetails
  if (!Array.isArray(issues)) {
    return []
  }
  return issues.map((item) => normalizeIssueLabel(item)).filter(Boolean)
}

const hasIssueDetail = (data: IssueDetailData, issues: string[]) => {
  const hasAmount = Object.values(data).some((value) => isValidNumber(value))
  return hasAmount || issues.length > 0
}

const parseDetailIssueJson = (rawJson?: string) => {
  detailIssueData.value = createEmptyIssueDetailData()
  detailIssueTags.value = []
  detailIssueAvailable.value = false
  detailIssueParseError.value = false

  const raw = (rawJson || '').trim()
  if (!raw) {
    return
  }
  try {
    const payload = JSON.parse(raw)
    if (!payload || typeof payload !== 'object' || Array.isArray(payload)) {
      detailIssueParseError.value = true
      return
    }
    const detailPayload = payload as Record<string, any>
    const parsedData: IssueDetailData = {
      tradeAmount: parseNumber(detailPayload.tradeAmount),
      fulfillmentAmount: parseNumber(detailPayload.fulfillmentAmount),
      commissionAmount: parseNumber(detailPayload.commissionAmount),
      splitAmount: parseNumber(detailPayload.splitAmount),
      tradeMinusFulfillment: parseNumber(detailPayload.tradeMinusFulfillment),
      tradeMinusCommissionSplit: parseNumber(detailPayload.tradeMinusCommissionSplit)
    }
    const parsedIssues = parseIssueLabels(detailPayload)
    detailIssueData.value = parsedData
    detailIssueTags.value = parsedIssues
    detailIssueAvailable.value = hasIssueDetail(parsedData, parsedIssues)
  } catch {
    detailIssueParseError.value = true
  }
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
    message.success(`手工执行成功，记录ID：${id || EMPTY_TEXT}`)
    runDialogVisible.value = false
    await getList()
  } catch (error: any) {
    message.error(error?.msg || '手工执行失败')
  } finally {
    runLoading.value = false
  }
}

const openDetailDrawer = async (row: FourAccountReconcileApi.FourAccountReconcileVO) => {
  if (!row.id) {
    message.warning('记录ID为空，无法查看详情')
    return
  }
  detailDrawerVisible.value = true
  detailLoading.value = true
  detailData.value = {}
  parseDetailIssueJson(undefined)
  try {
    const data = await FourAccountReconcileApi.getFourAccountReconcile(row.id)
    detailData.value = data || {}
    parseDetailIssueJson(detailData.value.issueDetailJson)
  } catch (error: any) {
    message.error(error?.msg || '详情获取失败')
    detailData.value = {}
    parseDetailIssueJson(undefined)
  } finally {
    detailLoading.value = false
  }
}

const openRelatedTicket = (row: FourAccountReconcileApi.FourAccountReconcileVO) => {
  if (!row.bizDate) {
    message.warning('业务日期为空，无法跳转工单页')
    return
  }
  router.push({
    path: '/mall/trade/after-sale/review-ticket',
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
