<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :inline="true" :model="queryParams" class="-mb-15px" label-width="90px">
      <el-form-item label="作用域" prop="scope">
        <el-select v-model="queryParams.scope" class="!w-220px" clearable placeholder="请选择作用域">
          <el-option
            v-for="item in scopeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="规则编码" prop="ruleCode">
        <el-input
          v-model="queryParams.ruleCode"
          class="!w-220px"
          clearable
          placeholder="请输入规则编码"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="工单类型" prop="ticketType">
        <el-input-number v-model="queryParams.ticketType" :min="1" class="!w-220px" controls-position="right" />
      </el-form-item>
      <el-form-item label="严重级别" prop="severity">
        <el-select v-model="queryParams.severity" class="!w-220px" clearable placeholder="请选择严重级别">
          <el-option
            v-for="item in severityOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="启用状态" prop="enabled">
        <el-select v-model="queryParams.enabled" class="!w-220px" clearable placeholder="请选择启用状态">
          <el-option label="启用" :value="true" />
          <el-option label="停用" :value="false" />
        </el-select>
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
          v-hasPermi="['trade:after-sale:refund']"
          plain
          type="primary"
          @click="openForm('create')"
        >
          <Icon class="mr-5px" icon="ep:plus" />
          新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="编号" prop="id" width="80" />
      <el-table-column label="作用域" min-width="140">
        <template #default="{ row }">
          {{ scopeLabelMap[row.scope] || row.scope }}
        </template>
      </el-table-column>
      <el-table-column label="匹配键" min-width="220">
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
    <Pagination
      v-model:limit="queryParams.pageSize"
      v-model:page="queryParams.pageNo"
      :total="total"
      @pagination="getList"
    />
  </ContentWrap>

  <el-dialog v-model="formDialogVisible" :title="formTitle" width="720px">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
      <el-form-item label="作用域" prop="scope">
        <el-select v-model="formData.scope" class="!w-full" @change="handleScopeChange">
          <el-option
            v-for="item in scopeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item v-if="formData.scope === 'RULE'" label="规则编码" prop="ruleCode">
        <el-input v-model="formData.ruleCode" placeholder="例如：BLACKLIST_USER" />
      </el-form-item>
      <el-form-item v-if="needTicketType" label="工单类型" prop="ticketType">
        <el-input-number v-model="formData.ticketType" :min="1" class="!w-full" controls-position="right" />
      </el-form-item>
      <el-form-item label="严重级别" prop="severity">
        <el-select
          v-model="formData.severity"
          :disabled="!isTypeSeverityScope"
          class="!w-full"
          placeholder="请选择严重级别"
        >
          <el-option
            v-for="item in severityOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="升级对象" prop="escalateTo">
        <el-input v-model="formData.escalateTo" placeholder="例如：HQ_AFTER_SALE" />
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
      <el-button @click="formDialogVisible = false">取消</el-button>
      <el-button :loading="formLoading" type="primary" @click="submitForm">确定</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import * as ReviewTicketRouteApi from '@/api/mall/trade/reviewTicketRoute'
import { dateFormatter } from '@/utils/formatTime'

defineOptions({ name: 'TradeReviewTicketRoute' })

const message = useMessage()
const { t } = useI18n()

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
  GLOBAL_DEFAULT: '全局默认'
}
const severityOptions = [
  { label: 'P0', value: 'P0' },
  { label: 'P1', value: 'P1' }
]

const loading = ref(false)
const total = ref(0)
const list = ref<ReviewTicketRouteApi.ReviewTicketRouteVO[]>([])
const switchLoadingIds = ref<number[]>([])
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
  severity: [{ required: true, message: '严重级别不能为空', trigger: 'change' }],
  escalateTo: [{ required: true, message: '升级对象不能为空', trigger: 'blur' }],
  slaMinutes: [{ required: true, message: 'SLA 分钟不能为空', trigger: 'blur' }],
  sort: [{ required: true, message: '排序不能为空', trigger: 'blur' }]
}

const needTicketType = computed(() => formData.scope === 'TYPE_SEVERITY' || formData.scope === 'TYPE_DEFAULT')
const isTypeSeverityScope = computed(() => formData.scope === 'TYPE_SEVERITY')
const formTitle = computed(() => {
  return formMode.value === 'create' ? '新增工单 SLA 路由规则' : '编辑工单 SLA 路由规则'
})

const buildRouteKey = (row: ReviewTicketRouteApi.ReviewTicketRouteVO) => {
  if (row.scope === 'RULE') return `ruleCode=${row.ruleCode || ''}, severity=${row.severity || ''}`
  if (row.scope === 'TYPE_SEVERITY') return `ticketType=${row.ticketType || 0}, severity=${row.severity || ''}`
  if (row.scope === 'TYPE_DEFAULT') return `ticketType=${row.ticketType || 0}`
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
  payload.scope = (payload.scope || '').trim().toUpperCase()
  payload.ruleCode = (payload.ruleCode || '').trim().toUpperCase()
  payload.severity = (payload.severity || 'P1').trim().toUpperCase()
  payload.escalateTo = (payload.escalateTo || '').trim().toUpperCase()
  payload.remark = (payload.remark || '').trim()
  if (payload.scope === 'RULE') {
    payload.ticketType = 0
  } else if (payload.scope === 'TYPE_SEVERITY' || payload.scope === 'TYPE_DEFAULT') {
    payload.ruleCode = ''
  } else if (payload.scope === 'GLOBAL_DEFAULT') {
    payload.ruleCode = ''
    payload.ticketType = 0
  }
  if (payload.scope !== 'TYPE_SEVERITY') {
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
    message.warning('该作用域必须填写工单类型')
    return false
  }
  if (!payload.severity) {
    message.warning('严重级别不能为空')
    return false
  }
  return true
}

const getList = async () => {
  loading.value = true
  try {
    const data = await ReviewTicketRouteApi.getReviewTicketRoutePage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleQuery = async () => {
  queryParams.pageNo = 1
  await getList()
}

const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}

const openForm = async (mode: 'create' | 'update', id?: number) => {
  formMode.value = mode
  resetFormData()
  formRef.value?.clearValidate()
  if (mode === 'update' && id) {
    const data = await ReviewTicketRouteApi.getReviewTicketRoute(id)
    Object.assign(formData, data)
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
  } finally {
    formLoading.value = false
  }
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await ReviewTicketRouteApi.deleteReviewTicketRoute(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

const handleToggleEnabled = async (row: ReviewTicketRouteApi.ReviewTicketRouteVO, enabled: boolean) => {
  if (!row.id) return
  switchLoadingIds.value.push(row.id)
  try {
    const payload = normalizeByScope({
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
    await ReviewTicketRouteApi.updateReviewTicketRoute(payload)
    message.success('状态已更新')
    await getList()
  } catch (error) {
    row.enabled = !enabled
    throw error
  } finally {
    switchLoadingIds.value = switchLoadingIds.value.filter((id) => id !== row.id)
  }
}

onMounted(() => {
  getList()
})
</script>
