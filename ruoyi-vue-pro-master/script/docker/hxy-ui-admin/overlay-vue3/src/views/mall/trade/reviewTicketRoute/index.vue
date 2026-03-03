<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :inline="true" :model="queryParams" class="-mb-15px" label-width="90px">
      <el-form-item label="作用域" prop="scope">
        <el-select v-model="queryParams.scope" class="!w-220px" clearable placeholder="请选择作用域">
          <el-option v-for="item in scopeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="规则编码" prop="ruleCode">
        <el-input
          v-model="queryParams.ruleCode"
          class="!w-220px"
          clearable
          placeholder="请输入规则编码"
          @blur="handleQueryRuleCodeBlur"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="工单类型" prop="ticketType">
        <el-select v-model="queryParams.ticketType" class="!w-220px" clearable placeholder="请选择工单类型">
          <el-option v-for="item in ticketTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="严重级别" prop="severity">
        <el-select v-model="queryParams.severity" class="!w-220px" clearable placeholder="请选择严重级别">
          <el-option v-for="item in severityOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="启用状态" prop="enabled">
        <el-select v-model="queryParams.enabled" class="!w-220px" clearable placeholder="请选择启用状态">
          <el-option label="启用" :value="true" />
          <el-option label="停用" :value="false" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button :loading="queryLoading" @click="handleQuery">
          <Icon class="mr-5px" icon="ep:search" />
          搜索
        </el-button>
        <el-button :disabled="queryLoading" @click="resetQuery">
          <Icon class="mr-5px" icon="ep:refresh" />
          重置
        </el-button>
        <el-button v-hasPermi="['trade:after-sale:refund']" plain type="primary" @click="openForm('create')">
          <Icon class="mr-5px" icon="ep:plus" />
          新增
        </el-button>
        <el-button
          v-hasPermi="['trade:after-sale:refund']"
          plain
          type="success"
          :disabled="!hasSelection"
          :loading="batchActionLoading.enable"
          @click="handleBatchToggleEnabled(true)"
        >
          批量启用
        </el-button>
        <el-button
          v-hasPermi="['trade:after-sale:refund']"
          plain
          type="warning"
          :disabled="!hasSelection"
          :loading="batchActionLoading.disable"
          @click="handleBatchToggleEnabled(false)"
        >
          批量停用
        </el-button>
        <el-button
          v-hasPermi="['trade:after-sale:refund']"
          plain
          type="danger"
          :disabled="!hasSelection"
          :loading="batchActionLoading.delete"
          @click="handleBatchDelete"
        >
          批量删除
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" />
      <el-table-column label="编号" prop="id" width="80" />
      <el-table-column label="作用域" min-width="140">
        <template #default="{ row }">
          {{ scopeLabelMap[row.scope] || row.scope }}
        </template>
      </el-table-column>
      <el-table-column label="匹配键" min-width="260">
        <template #default="{ row }">
          <span>{{ buildRouteKey(row) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="升级对象" min-width="180" prop="escalateTo" />
      <el-table-column label="SLA(分钟)" min-width="100" prop="slaMinutes" />
      <el-table-column label="排序" min-width="80" prop="sort" />
      <el-table-column align="center" label="启用状态" min-width="120">
        <template #default="{ row }">
          <el-switch
            v-model="row.enabled"
            :active-value="true"
            :inactive-value="false"
            :loading="switchLoadingIds.includes(row.id)"
            @change="(value: boolean) => handleToggleEnabled(row, value)"
          />
        </template>
      </el-table-column>
      <el-table-column :formatter="dateFormatter" label="创建时间" prop="createTime" width="180" />
      <el-table-column label="备注" min-width="220" prop="remark" show-overflow-tooltip />
      <el-table-column align="center" fixed="right" label="操作" min-width="150">
        <template #default="{ row }">
          <el-button v-hasPermi="['trade:after-sale:refund']" link type="primary" @click="openForm('update', row.id)">
            编辑
          </el-button>
          <el-button v-hasPermi="['trade:after-sale:refund']" link type="danger" @click="handleDelete(row.id)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="handlePageChange" />
  </ContentWrap>

  <ContentWrap>
    <el-form :inline="true" :model="previewForm" class="-mb-15px" label-width="90px">
      <el-form-item label="规则编码">
        <el-input
          v-model="previewForm.ruleCode"
          class="!w-220px"
          clearable
          placeholder="可选，优先匹配 RULE"
          @blur="previewForm.ruleCode = normalizeUpper(previewForm.ruleCode)"
        />
      </el-form-item>
      <el-form-item label="工单类型">
        <el-select v-model="previewForm.ticketType" class="!w-220px" clearable placeholder="请选择工单类型">
          <el-option v-for="item in ticketTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="严重级别">
        <el-select v-model="previewForm.severity" class="!w-220px" clearable placeholder="请选择严重级别">
          <el-option v-for="item in severityOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button :loading="previewLoading" type="primary" @click="handlePreview">命中预览</el-button>
        <el-button :loading="enabledRouteLoading" @click="handleRefreshEnabledRoutes">刷新启用规则</el-button>
      </el-form-item>
    </el-form>

    <el-alert
      class="mb-12px"
      :closable="false"
      show-icon
      title="预览优先级：RULE > TYPE_SEVERITY > TYPE_DEFAULT > GLOBAL_DEFAULT"
      type="info"
    />

    <el-empty v-if="!previewResult" description="输入命中条件后点击“命中预览”" />
    <el-descriptions v-else :column="2" border>
      <el-descriptions-item label="命中 routeId">{{ previewResult.routeId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="命中作用域">{{ scopeLabelMap[previewResult.scope] || previewResult.scope }}</el-descriptions-item>
      <el-descriptions-item label="升级对象">{{ previewResult.escalateTo }}</el-descriptions-item>
      <el-descriptions-item label="SLA(分钟)">{{ previewResult.slaMinutes }}</el-descriptions-item>
      <el-descriptions-item label="严重级别">{{ previewResult.severity }}</el-descriptions-item>
      <el-descriptions-item label="排序">{{ previewResult.sort ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="命中说明" :span="2">{{ previewResult.reason }}</el-descriptions-item>
    </el-descriptions>
  </ContentWrap>

  <el-dialog v-model="formDialogVisible" :title="formTitle" width="720px">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
      <el-form-item label="作用域" prop="scope">
        <el-select v-model="formData.scope" class="!w-full" @change="handleScopeChange">
          <el-option v-for="item in scopeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-alert class="mb-12px" :closable="false" :title="scopeConstraintText" type="info" />
      <el-form-item v-if="formData.scope === 'RULE'" label="规则编码" prop="ruleCode">
        <el-input v-model="formData.ruleCode" placeholder="例如：BLACKLIST_USER" @blur="normalizeFormUpperField('ruleCode')" />
      </el-form-item>
      <el-form-item v-if="needTicketType" label="工单类型" prop="ticketType">
        <el-select v-model="formData.ticketType" class="!w-full" clearable placeholder="请选择工单类型">
          <el-option v-for="item in ticketTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="严重级别" prop="severity">
        <el-select
          v-model="formData.severity"
          :disabled="!isTypeSeverityScope"
          class="!w-full"
          placeholder="TYPE_SEVERITY 必填；其他作用域固定 P1"
        >
          <el-option v-for="item in severityOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="升级对象" prop="escalateTo">
        <el-input v-model="formData.escalateTo" placeholder="例如：HQ_AFTER_SALE" @blur="normalizeFormUpperField('escalateTo')" />
      </el-form-item>
      <el-form-item label="SLA(分钟)" prop="slaMinutes">
        <el-input-number v-model="formData.slaMinutes" :min="1" :max="10080" class="!w-full" controls-position="right" />
      </el-form-item>
      <el-form-item label="是否启用" prop="enabled">
        <el-switch v-model="formData.enabled" :active-value="true" :inactive-value="false" />
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="formData.sort" :min="0" class="!w-full" controls-position="right" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" :rows="3" placeholder="请输入备注" type="textarea" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" @click="formDialogVisible = false">取消</el-button>
      <el-button :loading="formLoading" type="primary" @click="submitForm">确定</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import * as ReviewTicketRouteApi from '@/api/mall/trade/reviewTicketRoute'
import { dateFormatter } from '@/utils/formatTime'

defineOptions({ name: 'TradeReviewTicketRoute' })

interface ReviewRoutePreviewResult {
  routeId?: number
  scope: string
  escalateTo: string
  slaMinutes: number
  severity: string
  sort?: number
  reason: string
}

const message = useMessage()
const { t } = useI18n()
const router = useRouter()
const route = useRoute()

const scopeOptions = [
  { label: '规则编码', value: 'RULE' },
  { label: '工单类型+严重级别', value: 'TYPE_SEVERITY' },
  { label: '工单类型默认', value: 'TYPE_DEFAULT' },
  { label: '全局默认', value: 'GLOBAL_DEFAULT' }
]
const scopeLabelMap: Record<string, string> = {
  RULE: '规则编码',
  TYPE_SEVERITY: '工单类型+严重级别',
  TYPE_DEFAULT: '工单类型默认',
  GLOBAL_DEFAULT: '全局默认',
  SYSTEM_FALLBACK: '系统兜底'
}
const ticketTypeOptions = [
  { label: '10 售后复核', value: 10 },
  { label: '20 服务履约', value: 20 },
  { label: '30 提成争议', value: 30 }
]
const ticketTypeLabelMap: Record<number, string> = {
  10: '售后复核',
  20: '服务履约',
  30: '提成争议'
}
const severityOptions = [
  { label: 'P0', value: 'P0' },
  { label: 'P1', value: 'P1' }
]

const loading = ref(false)
const queryLoading = ref(false)
const total = ref(0)
const list = ref<ReviewTicketRouteApi.ReviewTicketRouteVO[]>([])
const selectedRows = ref<ReviewTicketRouteApi.ReviewTicketRouteVO[]>([])
const switchLoadingIds = ref<number[]>([])
const batchActionLoading = reactive({
  enable: false,
  disable: false,
  delete: false
})
const queryFormRef = ref()
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  scope: undefined as string | undefined,
  ruleCode: undefined as string | undefined,
  ticketType: undefined as number | undefined,
  severity: undefined as string | undefined,
  enabled: undefined as boolean | undefined
})

const previewLoading = ref(false)
const enabledRouteLoading = ref(false)
const enabledRouteList = ref<ReviewTicketRouteApi.ReviewTicketRouteVO[]>([])
const previewResult = ref<ReviewRoutePreviewResult>()
const previewForm = reactive({
  ruleCode: '',
  ticketType: 10,
  severity: 'P1'
})

const formRef = ref()
const formDialogVisible = ref(false)
const formMode = ref<'create' | 'update'>('create')
const formLoading = ref(false)
const formData = reactive<ReviewTicketRouteApi.ReviewTicketRouteVO>({
  scope: 'RULE',
  ruleCode: '',
  ticketType: undefined,
  severity: 'P1',
  escalateTo: '',
  slaMinutes: 120,
  enabled: true,
  sort: 0,
  remark: ''
})
const formRules = {
  scope: [{ required: true, message: '作用域不能为空', trigger: 'change' }],
  ruleCode: [
    {
      trigger: 'blur',
      validator: (_rule, value, callback) => {
        if (formData.scope === 'RULE' && !normalizeUpper(value)) {
          callback(new Error('RULE 作用域必须填写规则编码'))
          return
        }
        callback()
      }
    }
  ],
  ticketType: [
    {
      trigger: 'change',
      validator: (_rule, value, callback) => {
        if ((formData.scope === 'TYPE_SEVERITY' || formData.scope === 'TYPE_DEFAULT') && !Number(value)) {
          callback(new Error('该作用域必须选择工单类型'))
          return
        }
        callback()
      }
    }
  ],
  severity: [
    {
      trigger: 'change',
      validator: (_rule, value, callback) => {
        if (formData.scope === 'TYPE_SEVERITY' && !normalizeUpper(value)) {
          callback(new Error('TYPE_SEVERITY 作用域必须选择严重级别'))
          return
        }
        callback()
      }
    }
  ],
  escalateTo: [{ required: true, message: '升级对象不能为空', trigger: 'blur' }],
  slaMinutes: [{ required: true, message: 'SLA 分钟不能为空', trigger: 'blur' }],
  sort: [{ required: true, message: '排序不能为空', trigger: 'blur' }]
}

const needTicketType = computed(() => formData.scope === 'TYPE_SEVERITY' || formData.scope === 'TYPE_DEFAULT')
const isTypeSeverityScope = computed(() => formData.scope === 'TYPE_SEVERITY')
const hasSelection = computed(() => selectedRows.value.length > 0)
const formTitle = computed(() => {
  return formMode.value === 'create' ? '新增工单 SLA 路由规则' : '编辑工单 SLA 路由规则'
})
const scopeConstraintText = computed(() => {
  if (formData.scope === 'RULE') {
    return 'RULE：仅按规则编码命中；需填写规则编码，工单类型固定为 0，严重级别固定为 P1。'
  }
  if (formData.scope === 'TYPE_SEVERITY') {
    return 'TYPE_SEVERITY：按工单类型+严重级别命中；需同时填写工单类型与严重级别。'
  }
  if (formData.scope === 'TYPE_DEFAULT') {
    return 'TYPE_DEFAULT：按工单类型兜底；需填写工单类型，严重级别固定为 P1。'
  }
  return 'GLOBAL_DEFAULT：全局兜底；规则编码清空、工单类型固定为 0、严重级别固定为 P1。'
})

const normalizeUpper = (value?: string) => {
  return (value || '').trim().toUpperCase()
}

const firstQueryValue = (value: any) => {
  if (Array.isArray(value)) {
    return value.length ? value[0] : undefined
  }
  return value
}

const parseNumberQuery = (value: any, fallback?: number): number | undefined => {
  const raw = firstQueryValue(value)
  if (raw === undefined || raw === null || raw === '') {
    return fallback
  }
  const parsed = Number(raw)
  if (Number.isNaN(parsed)) {
    return fallback
  }
  return parsed
}

const parseBooleanQuery = (value: any): boolean | undefined => {
  const raw = firstQueryValue(value)
  if (raw === undefined || raw === null || raw === '') {
    return undefined
  }
  if (raw === true || raw === 'true' || raw === '1') {
    return true
  }
  if (raw === false || raw === 'false' || raw === '0') {
    return false
  }
  return undefined
}

const restoreQueryFromRoute = () => {
  queryParams.pageNo = parseNumberQuery(route.query.pageNo, 1) || 1
  queryParams.pageSize = parseNumberQuery(route.query.pageSize, 10) || 10
  queryParams.scope = (firstQueryValue(route.query.scope) as string) || undefined
  queryParams.ruleCode = normalizeUpper((firstQueryValue(route.query.ruleCode) as string) || '') || undefined
  queryParams.ticketType = parseNumberQuery(route.query.ticketType)
  queryParams.severity = normalizeUpper((firstQueryValue(route.query.severity) as string) || '') || undefined
  queryParams.enabled = parseBooleanQuery(route.query.enabled)
}

const buildQueryForRoute = () => {
  const query: Record<string, string> = {}
  if (queryParams.pageNo !== 1) {
    query.pageNo = String(queryParams.pageNo)
  }
  if (queryParams.pageSize !== 10) {
    query.pageSize = String(queryParams.pageSize)
  }
  if (queryParams.scope) {
    query.scope = queryParams.scope
  }
  if (queryParams.ruleCode) {
    query.ruleCode = queryParams.ruleCode
  }
  if (queryParams.ticketType) {
    query.ticketType = String(queryParams.ticketType)
  }
  if (queryParams.severity) {
    query.severity = queryParams.severity
  }
  if (queryParams.enabled !== undefined) {
    query.enabled = String(queryParams.enabled)
  }
  return query
}

const querySignature = (query: Record<string, any>) => {
  return Object.keys(query)
    .sort()
    .map((key) => `${key}=${firstQueryValue(query[key]) || ''}`)
    .join('&')
}

const syncQueryToRoute = async () => {
  const nextQuery = buildQueryForRoute()
  if (querySignature(nextQuery) === querySignature(route.query as Record<string, any>)) {
    return
  }
  await router.replace({ query: nextQuery })
}

const formatTicketType = (ticketType?: number) => {
  if (!ticketType) {
    return '未指定'
  }
  const label = ticketTypeLabelMap[ticketType]
  if (!label) {
    return String(ticketType)
  }
  return `${label}（${ticketType}）`
}

const buildRouteKey = (row: ReviewTicketRouteApi.ReviewTicketRouteVO) => {
  if (row.scope === 'RULE') return `ruleCode=${row.ruleCode || ''}`
  if (row.scope === 'TYPE_SEVERITY') return `ticketType=${formatTicketType(row.ticketType)}, severity=${row.severity || ''}`
  if (row.scope === 'TYPE_DEFAULT') return `ticketType=${formatTicketType(row.ticketType)}`
  return 'GLOBAL_DEFAULT'
}

const resetFormData = () => {
  formData.id = undefined
  formData.scope = 'RULE'
  formData.ruleCode = ''
  formData.ticketType = undefined
  formData.severity = 'P1'
  formData.escalateTo = ''
  formData.slaMinutes = 120
  formData.enabled = true
  formData.sort = 0
  formData.remark = ''
}

const normalizeByScope = (source: ReviewTicketRouteApi.ReviewTicketRouteVO) => {
  const payload = { ...source } as ReviewTicketRouteApi.ReviewTicketRouteVO
  payload.scope = normalizeUpper(payload.scope)
  payload.ruleCode = normalizeUpper(payload.ruleCode)
  payload.severity = normalizeUpper(payload.severity || 'P1') || 'P1'
  payload.escalateTo = normalizeUpper(payload.escalateTo)
  payload.remark = (payload.remark || '').trim()
  if (payload.scope === 'RULE') {
    payload.ticketType = 0
    payload.severity = 'P1'
  } else if (payload.scope === 'TYPE_SEVERITY') {
    payload.ruleCode = ''
  } else if (payload.scope === 'TYPE_DEFAULT') {
    payload.ruleCode = ''
    payload.severity = 'P1'
  } else if (payload.scope === 'GLOBAL_DEFAULT') {
    payload.ruleCode = ''
    payload.ticketType = 0
    payload.severity = 'P1'
  }
  return payload
}

const validateScopeFields = (payload: ReviewTicketRouteApi.ReviewTicketRouteVO) => {
  if (payload.scope === 'RULE' && !payload.ruleCode) {
    message.warning('RULE 作用域必须填写规则编码')
    return false
  }
  if ((payload.scope === 'TYPE_SEVERITY' || payload.scope === 'TYPE_DEFAULT') && !payload.ticketType) {
    message.warning('该作用域必须选择工单类型')
    return false
  }
  if (payload.scope === 'TYPE_SEVERITY' && !payload.severity) {
    message.warning('TYPE_SEVERITY 作用域必须选择严重级别')
    return false
  }
  if (!payload.escalateTo) {
    message.warning('升级对象不能为空')
    return false
  }
  return true
}

const getList = async () => {
  loading.value = true
  try {
    await syncQueryToRoute()
    const params = {
      ...queryParams,
      ruleCode: normalizeUpper(queryParams.ruleCode) || undefined,
      severity: normalizeUpper(queryParams.severity) || undefined
    }
    const data = await ReviewTicketRouteApi.getReviewTicketRoutePage(params)
    list.value = data.list || []
    total.value = data.total || 0
    selectedRows.value = []
  } catch (error: any) {
    message.error(error?.msg || '查询路由规则失败')
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handlePageChange = async () => {
  await getList()
}

const handleQueryRuleCodeBlur = () => {
  queryParams.ruleCode = normalizeUpper(queryParams.ruleCode)
}

const handleQuery = async () => {
  if (queryLoading.value) {
    return
  }
  queryLoading.value = true
  try {
    queryParams.ruleCode = normalizeUpper(queryParams.ruleCode) || undefined
    queryParams.severity = normalizeUpper(queryParams.severity) || undefined
    queryParams.pageNo = 1
    await getList()
  } finally {
    queryLoading.value = false
  }
}

const resetQuery = async () => {
  queryFormRef.value?.resetFields()
  queryParams.pageNo = 1
  queryParams.pageSize = 10
  queryParams.scope = undefined
  queryParams.ruleCode = undefined
  queryParams.ticketType = undefined
  queryParams.severity = undefined
  queryParams.enabled = undefined
  await getList()
}

const openForm = async (mode: 'create' | 'update', id?: number) => {
  formMode.value = mode
  resetFormData()
  formRef.value?.clearValidate()
  if (mode === 'update' && id) {
    try {
      const data = await ReviewTicketRouteApi.getReviewTicketRoute(id)
      Object.assign(formData, {
        ...data,
        scope: normalizeUpper(data.scope),
        ruleCode: normalizeUpper(data.ruleCode),
        severity: normalizeUpper(data.severity || 'P1') || 'P1',
        escalateTo: normalizeUpper(data.escalateTo)
      })
    } catch (error: any) {
      message.error(error?.msg || '获取路由规则详情失败')
      return
    }
  }
  formDialogVisible.value = true
}

const handleScopeChange = () => {
  if (formData.scope === 'RULE') {
    formData.ticketType = 0
    formData.severity = 'P1'
  } else if (formData.scope === 'GLOBAL_DEFAULT') {
    formData.ruleCode = ''
    formData.ticketType = 0
    formData.severity = 'P1'
  } else {
    formData.ruleCode = ''
    if (formData.scope === 'TYPE_DEFAULT') {
      formData.severity = 'P1'
    }
  }
  formRef.value?.clearValidate?.(['ruleCode', 'ticketType', 'severity'])
}

const normalizeFormUpperField = (field: 'ruleCode' | 'escalateTo') => {
  if (field === 'ruleCode') {
    formData.ruleCode = normalizeUpper(formData.ruleCode)
    return
  }
  formData.escalateTo = normalizeUpper(formData.escalateTo)
}

const submitForm = async () => {
  if (formLoading.value) return
  const valid = await formRef.value?.validate()
  if (!valid) return
  const payload = normalizeByScope(formData)
  if (!validateScopeFields(payload)) return
  formLoading.value = true
  try {
    if (formMode.value === 'create') {
      await ReviewTicketRouteApi.createReviewTicketRoute(payload)
      message.success(t('common.createSuccess'))
    } else {
      await ReviewTicketRouteApi.updateReviewTicketRoute(payload)
      message.success(t('common.updateSuccess'))
    }
    formDialogVisible.value = false
    await getList()
    await loadEnabledRouteList(true)
  } catch (error: any) {
    message.error(error?.msg || (formMode.value === 'create' ? '创建失败' : '更新失败'))
  } finally {
    formLoading.value = false
  }
}

const handleDelete = async (id: number) => {
  if (!id) {
    return
  }
  try {
    await message.delConfirm()
    await ReviewTicketRouteApi.deleteReviewTicketRoute(id)
    message.success(t('common.delSuccess'))
    await getList()
    await loadEnabledRouteList(true)
  } catch (error: any) {
    if (error !== 'cancel') {
      message.error(error?.msg || '删除失败')
    }
  }
}

const buildUpdatePayloadByRow = (row: ReviewTicketRouteApi.ReviewTicketRouteVO, enabled: boolean) => {
  return normalizeByScope({
    id: row.id,
    scope: row.scope,
    ruleCode: row.ruleCode,
    ticketType: row.ticketType,
    severity: row.severity,
    escalateTo: row.escalateTo,
    slaMinutes: row.slaMinutes,
    enabled,
    sort: row.sort,
    remark: row.remark || ''
  })
}

const handleSelectionChange = (rows: ReviewTicketRouteApi.ReviewTicketRouteVO[]) => {
  selectedRows.value = rows.filter((item) => !!item.id)
}

const handleToggleEnabled = async (row: ReviewTicketRouteApi.ReviewTicketRouteVO, enabled: boolean) => {
  if (!row.id) return
  if (switchLoadingIds.value.includes(row.id)) return
  switchLoadingIds.value.push(row.id)
  try {
    const payload = buildUpdatePayloadByRow(row, enabled)
    await ReviewTicketRouteApi.updateReviewTicketRoute(payload)
    message.success('状态已更新')
    await loadEnabledRouteList(true)
  } catch (error: any) {
    row.enabled = !enabled
    message.error(error?.msg || '状态更新失败')
  } finally {
    switchLoadingIds.value = switchLoadingIds.value.filter((id) => id !== row.id)
  }
}

const summarizeBatchResult = (actionName: string, successIds: number[], failIds: number[]) => {
  const failIdText = failIds.length ? `，失败ID：[${failIds.join(', ')}]` : ''
  const text = `批量${actionName}完成：成功 ${successIds.length} 条，失败 ${failIds.length} 条${failIdText}`
  if (failIds.length) {
    message.warning(text)
    return
  }
  message.success(text)
}

const handleBatchToggleEnabled = async (enabled: boolean) => {
  if (!selectedRows.value.length) {
    message.warning('请先勾选至少一条规则')
    return
  }
  const actionName = enabled ? '启用' : '停用'
  try {
    await message.confirm(`确认批量${actionName} ${selectedRows.value.length} 条规则吗？`)
  } catch {
    return
  }
  const loadingKey = enabled ? 'enable' : 'disable'
  batchActionLoading[loadingKey] = true
  const successIds: number[] = []
  const failIds: number[] = []
  try {
    for (const row of selectedRows.value) {
      if (!row.id) {
        continue
      }
      try {
        const payload = buildUpdatePayloadByRow(row, enabled)
        await ReviewTicketRouteApi.updateReviewTicketRoute(payload)
        successIds.push(row.id)
      } catch {
        failIds.push(row.id)
      }
    }
    summarizeBatchResult(actionName, successIds, failIds)
    await getList()
    await loadEnabledRouteList(true)
  } finally {
    batchActionLoading[loadingKey] = false
  }
}

const handleBatchDelete = async () => {
  if (!selectedRows.value.length) {
    message.warning('请先勾选至少一条规则')
    return
  }
  try {
    await message.confirm(`确认批量删除 ${selectedRows.value.length} 条规则吗？删除后不可恢复。`)
  } catch {
    return
  }
  batchActionLoading.delete = true
  const successIds: number[] = []
  const failIds: number[] = []
  try {
    for (const row of selectedRows.value) {
      if (!row.id) {
        continue
      }
      try {
        await ReviewTicketRouteApi.deleteReviewTicketRoute(row.id)
        successIds.push(row.id)
      } catch {
        failIds.push(row.id)
      }
    }
    summarizeBatchResult('删除', successIds, failIds)
    await getList()
    await loadEnabledRouteList(true)
  } finally {
    batchActionLoading.delete = false
  }
}

const normalizeRouteForPreview = (row: ReviewTicketRouteApi.ReviewTicketRouteVO) => {
  return {
    ...row,
    scope: normalizeUpper(row.scope),
    ruleCode: normalizeUpper(row.ruleCode),
    severity: normalizeUpper(row.severity),
    escalateTo: (row.escalateTo || '').trim(),
    ticketType: Number(row.ticketType || 0),
    slaMinutes: Number(row.slaMinutes || 0)
  }
}

const isPreviewRouteValid = (row: ReviewTicketRouteApi.ReviewTicketRouteVO) => {
  return !!normalizeUpper(row.severity) && !!(row.escalateTo || '').trim() && Number(row.slaMinutes || 0) > 0
}

const pickLatestMatchedRoute = (
  routes: ReviewTicketRouteApi.ReviewTicketRouteVO[],
  matcher: (row: ReviewTicketRouteApi.ReviewTicketRouteVO) => boolean
) => {
  let matched: ReviewTicketRouteApi.ReviewTicketRouteVO | undefined
  routes.forEach((row) => {
    if (matcher(row)) {
      matched = row
    }
  })
  return matched
}

const buildPreviewResult = (row: ReviewTicketRouteApi.ReviewTicketRouteVO, reason: string): ReviewRoutePreviewResult => {
  return {
    routeId: row.id,
    scope: normalizeUpper(row.scope),
    escalateTo: (row.escalateTo || '').trim(),
    slaMinutes: Number(row.slaMinutes || 0),
    severity: normalizeUpper(row.severity || 'P1') || 'P1',
    sort: row.sort,
    reason
  }
}

const calculatePreviewResult = () => {
  const normalizedRuleCode = normalizeUpper(previewForm.ruleCode)
  const normalizedSeverity = normalizeUpper(previewForm.severity)
  const normalizedTicketType = Number(previewForm.ticketType || 0)
  const normalizedRoutes = (enabledRouteList.value || []).map(normalizeRouteForPreview)

  const byRule = pickLatestMatchedRoute(normalizedRoutes, (row) => {
    return row.scope === 'RULE' && !!row.ruleCode && isPreviewRouteValid(row) && row.ruleCode === normalizedRuleCode
  })
  if (byRule && normalizedRuleCode) {
    return buildPreviewResult(byRule, `命中 RULE：ruleCode=${normalizedRuleCode}`)
  }

  const byTypeSeverity = pickLatestMatchedRoute(normalizedRoutes, (row) => {
    return (
      row.scope === 'TYPE_SEVERITY' &&
      row.ticketType > 0 &&
      !!row.severity &&
      isPreviewRouteValid(row) &&
      row.ticketType === normalizedTicketType &&
      row.severity === normalizedSeverity
    )
  })
  if (byTypeSeverity) {
    return buildPreviewResult(
      byTypeSeverity,
      `命中 TYPE_SEVERITY：ticketType=${normalizedTicketType}，severity=${normalizedSeverity || '-'}`
    )
  }

  const byTypeDefault = pickLatestMatchedRoute(normalizedRoutes, (row) => {
    return row.scope === 'TYPE_DEFAULT' && row.ticketType > 0 && isPreviewRouteValid(row) && row.ticketType === normalizedTicketType
  })
  if (byTypeDefault) {
    return buildPreviewResult(byTypeDefault, `命中 TYPE_DEFAULT：ticketType=${normalizedTicketType}`)
  }

  const byGlobalDefault = pickLatestMatchedRoute(normalizedRoutes, (row) => {
    return row.scope === 'GLOBAL_DEFAULT' && isPreviewRouteValid(row)
  })
  if (byGlobalDefault) {
    return buildPreviewResult(byGlobalDefault, '命中 GLOBAL_DEFAULT 全局兜底')
  }

  return {
    scope: 'SYSTEM_FALLBACK',
    escalateTo: 'HQ_AFTER_SALE',
    slaMinutes: 120,
    severity: 'P1',
    reason: '未命中启用规则，使用系统内置兜底（P1 / HQ_AFTER_SALE / 120）'
  }
}

const loadEnabledRouteList = async (silent = false) => {
  enabledRouteLoading.value = true
  try {
    enabledRouteList.value = (await ReviewTicketRouteApi.getEnabledReviewTicketRouteList()) || []
    return true
  } catch (error: any) {
    enabledRouteList.value = []
    if (!silent) {
      message.error(error?.msg || '加载启用规则失败')
    }
    return false
  } finally {
    enabledRouteLoading.value = false
  }
}

const handleRefreshEnabledRoutes = async () => {
  const ok = await loadEnabledRouteList()
  if (ok) {
    message.success('启用规则已刷新')
  }
}

const handlePreview = async () => {
  if (previewLoading.value) {
    return
  }
  previewLoading.value = true
  try {
    previewForm.ruleCode = normalizeUpper(previewForm.ruleCode)
    previewForm.severity = normalizeUpper(previewForm.severity) || 'P1'
    if (!enabledRouteList.value.length) {
      await loadEnabledRouteList()
    }
    previewResult.value = calculatePreviewResult()
  } catch (error: any) {
    message.error(error?.msg || '命中预览失败')
  } finally {
    previewLoading.value = false
  }
}

onMounted(async () => {
  restoreQueryFromRoute()
  await getList()
  await loadEnabledRouteList(true)
})
</script>
