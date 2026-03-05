<template>
  <ContentWrap>
    <el-form :inline="true" :model="queryParams" class="-mb-15px" label-width="102px">
      <el-form-item label="复核编号" prop="recheckNo">
        <el-input
          v-model="queryParams.recheckNo"
          class="!w-220px"
          clearable
          placeholder="请输入复核编号"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="批次台账ID" prop="logId">
        <el-input-number v-model="queryParams.logId" :controls="false" :min="1" class="!w-180px" placeholder="请输入台账ID" />
      </el-form-item>
      <el-form-item label="批次号" prop="batchNo">
        <el-input
          v-model="queryParams.batchNo"
          class="!w-230px"
          clearable
          placeholder="请输入批次号"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="目标生命周期" prop="targetLifecycleStatus">
        <el-select v-model="queryParams.targetLifecycleStatus" class="!w-170px" clearable placeholder="请选择">
          <el-option :value="10" label="PREPARING" />
          <el-option :value="20" label="TRIAL" />
          <el-option :value="30" label="OPERATING" />
          <el-option :value="35" label="SUSPENDED" />
          <el-option :value="40" label="CLOSED" />
        </el-select>
      </el-form-item>
      <el-form-item label="操作人" prop="operator">
        <el-input
          v-model="queryParams.operator"
          class="!w-170px"
          clearable
          placeholder="请输入操作人"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="来源" prop="source">
        <el-input
          v-model="queryParams.source"
          class="!w-150px"
          clearable
          placeholder="请输入来源"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="创建时间" prop="createTime">
        <el-date-picker
          v-model="queryParams.createTime"
          :default-time="[new Date('1 00:00:00'), new Date('1 23:59:59')]"
          class="!w-340px"
          end-placeholder="结束时间"
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
        <el-button plain type="primary" @click="goBatchLog()">
          <Icon class="mr-5px" icon="ep:back" />
          返回批次台账
        </el-button>
        <el-button plain type="warning" @click="goChangeOrder()">
          <Icon class="mr-5px" icon="ep:tickets" />
          变更单审批
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="ID" prop="id" width="88" />
      <el-table-column label="复核编号" min-width="230" show-overflow-tooltip>
        <template #default="{ row }">
          {{ textOrDash(row.recheckNo) }}
        </template>
      </el-table-column>
      <el-table-column label="批次台账ID" width="110">
        <template #default="{ row }">
          {{ numberOrDash(row.logId) }}
        </template>
      </el-table-column>
      <el-table-column label="批次号" min-width="250" show-overflow-tooltip>
        <template #default="{ row }">
          {{ textOrDash(row.batchNo) }}
        </template>
      </el-table-column>
      <el-table-column label="目标生命周期" width="150">
        <template #default="{ row }">
          <el-tag :type="lifecycleTagType(row.targetLifecycleStatus)">
            {{ lifecycleText(row.targetLifecycleStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="总数" width="90">
        <template #default="{ row }">
          {{ numberOrDash(row.totalCount) }}
        </template>
      </el-table-column>
      <el-table-column label="阻塞" width="90">
        <template #default="{ row }">
          {{ numberOrDash(row.blockedCount) }}
        </template>
      </el-table-column>
      <el-table-column label="告警" width="90">
        <template #default="{ row }">
          {{ numberOrDash(row.warningCount) }}
        </template>
      </el-table-column>
      <el-table-column label="操作人" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">
          {{ textOrDash(row.operator) }}
        </template>
      </el-table-column>
      <el-table-column label="来源" width="120">
        <template #default="{ row }">
          {{ textOrDash(row.source) }}
        </template>
      </el-table-column>
      <el-table-column :formatter="dateFormatter" label="创建时间" prop="createTime" width="180" />
      <el-table-column align="center" fixed="right" label="操作" width="240">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetailDrawer(row)">查看详情</el-button>
          <el-button link type="warning" @click="copyText(row.batchNo, '批次号')">复制批次号</el-button>
          <el-button link type="warning" @click="copyText(row.source, '来源号')">复制来源号</el-button>
          <el-button link type="info" @click="goBatchLog(row)">批次台账</el-button>
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

  <el-drawer v-model="detailDrawerVisible" size="74%" title="复核台账详情">
    <div v-loading="detailLoading">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="台账ID">{{ numberOrDash(detailData.id) }}</el-descriptions-item>
        <el-descriptions-item label="复核编号">{{ textOrDash(detailData.recheckNo) }}</el-descriptions-item>
        <el-descriptions-item label="批次台账ID">{{ numberOrDash(detailData.logId) }}</el-descriptions-item>
        <el-descriptions-item label="批次号">{{ textOrDash(detailData.batchNo) }}</el-descriptions-item>
        <el-descriptions-item label="目标生命周期">
          <el-tag :type="lifecycleTagType(detailData.targetLifecycleStatus)">
            {{ lifecycleText(detailData.targetLifecycleStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="规则版本">{{ textOrDash(detailData.guardRuleVersion) }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ textOrDash(detailData.operator) }}</el-descriptions-item>
        <el-descriptions-item label="来源">{{ textOrDash(detailData.source) }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ textOrDash(detailData.createTime) }}</el-descriptions-item>
      </el-descriptions>

      <el-descriptions :column="3" border class="mt-14px">
        <el-descriptions-item label="总数">{{ numberOrDash(detailData.totalCount) }}</el-descriptions-item>
        <el-descriptions-item label="阻塞">{{ numberOrDash(detailData.blockedCount) }}</el-descriptions-item>
        <el-descriptions-item label="告警">{{ numberOrDash(detailData.warningCount) }}</el-descriptions-item>
      </el-descriptions>

      <div class="mt-16px">
        <div class="mb-8px flex items-center justify-between">
          <span class="font-500">守卫配置快照（guardConfigSnapshotJson）</span>
          <el-button link type="warning" @click="copyText(detailData.guardConfigSnapshotJson, '守卫配置快照')">
            复制原文
          </el-button>
        </div>
        <el-alert v-if="guardConfigParseFailed" :closable="false" title="配置快照解析失败（原文保留）" type="warning" />
        <el-table v-else-if="guardConfigRows.length" :data="guardConfigRows" border max-height="240">
          <el-table-column label="键" min-width="220" prop="key" />
          <el-table-column label="值" min-width="360" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.value }}
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="无配置快照" />
        <el-input :model-value="guardConfigRawJson || EMPTY_TEXT" :rows="4" class="mt-8px" readonly type="textarea" />
      </div>

      <div class="mt-16px">
        <div class="mb-8px font-500">明细（detailJson）</div>
        <el-alert v-if="detailParseFailed" :closable="false" title="明细解析失败（原文保留）" type="warning" />
        <div v-if="detailRows.length" class="mb-10px mt-8px flex flex-wrap items-center gap-8px">
          <el-tag type="info">总明细 {{ detailRows.length }}</el-tag>
          <el-tag type="danger">阻塞 {{ detailBlockedRows.length }}</el-tag>
          <el-tag type="warning">告警 {{ detailWarningRows.length }}</el-tag>
          <el-tag type="success">通过 {{ detailPassRows.length }}</el-tag>
        </div>
        <el-empty v-else-if="!detailParseFailed" description="无可用明细" />
        <el-table v-if="detailRows.length" :data="detailRows" border max-height="420">
          <el-table-column label="门店ID" width="100">
            <template #default="{ row }">
              {{ numberOrDash(row.storeId) }}
            </template>
          </el-table-column>
          <el-table-column label="门店名称" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">
              {{ textOrDash(row.storeName) }}
            </template>
          </el-table-column>
          <el-table-column label="结果" width="110">
            <template #default="{ row }">
              <el-tag :type="resultTagType(row.result)">{{ row.result }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="阻塞原因" min-width="240" show-overflow-tooltip>
            <template #default="{ row }">
              {{ textOrDash(row.blockedMessage) }}
            </template>
          </el-table-column>
          <el-table-column label="告警" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">
              {{ warningsText(row.warnings) }}
            </template>
          </el-table-column>
          <el-table-column label="guardItems" min-width="260" show-overflow-tooltip>
            <template #default="{ row }">
              {{ guardItemsText(row.guardItems) }}
            </template>
          </el-table-column>
        </el-table>
        <el-input :model-value="detailRawJson || EMPTY_TEXT" :rows="6" class="mt-8px" readonly type="textarea" />
      </div>
    </div>
  </el-drawer>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import * as LifecycleBatchLogApi from '@/api/mall/store/lifecycleBatchLog'
import * as LifecycleRecheckLogApi from '@/api/mall/store/lifecycleRecheckLog'
import { useRoute, useRouter } from 'vue-router'

defineOptions({ name: 'MallStoreLifecycleRecheckLogIndex' })

interface RecheckDetailRow {
  storeId?: number
  storeName?: string
  result: string
  blocked?: boolean
  blockedCode?: number
  blockedMessage?: string
  warnings: string[]
  guardItems: LifecycleBatchLogApi.StoreLifecycleBatchLogGuardItem[]
}

const EMPTY_TEXT = '--'

const message = useMessage()
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const total = ref(0)
const list = ref<LifecycleRecheckLogApi.StoreLifecycleRecheckLogItem[]>([])

const queryParams = reactive<LifecycleRecheckLogApi.StoreLifecycleRecheckLogPageReq>({
  pageNo: 1,
  pageSize: 10,
  recheckNo: undefined,
  logId: undefined,
  batchNo: undefined,
  targetLifecycleStatus: undefined,
  operator: undefined,
  source: undefined,
  createTime: undefined
})

const detailDrawerVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref<Partial<LifecycleRecheckLogApi.StoreLifecycleRecheckLogGetResp>>({})
const detailRows = ref<RecheckDetailRow[]>([])
const detailParseFailed = ref(false)
const detailRawJson = ref('')
const guardConfigParseFailed = ref(false)
const guardConfigRows = ref<Array<{ key: string; value: string }>>([])
const guardConfigRawJson = ref('')

const detailBlockedRows = computed(() => detailRows.value.filter((item) => item.result === 'BLOCKED'))
const detailWarningRows = computed(() => detailRows.value.filter((item) => item.result === 'WARNING'))
const detailPassRows = computed(() => detailRows.value.filter((item) => item.result === 'SUCCESS'))

const lifecycleText = (status?: number) => {
  if (status === 10) return 'PREPARING'
  if (status === 20) return 'TRIAL'
  if (status === 30) return 'OPERATING'
  if (status === 35) return 'SUSPENDED'
  if (status === 40) return 'CLOSED'
  return `UNKNOWN(${status || 0})`
}

const lifecycleTagType = (status?: number) => {
  if (status === 30) return 'success'
  if (status === 20) return 'warning'
  if (status === 35) return 'danger'
  if (status === 40) return 'info'
  return 'info'
}

const resultTagType = (result?: string) => {
  if (result === 'SUCCESS') return 'success'
  if (result === 'BLOCKED') return 'danger'
  if (result === 'WARNING') return 'warning'
  return 'info'
}

const textOrDash = (value: any) => {
  if (value === undefined || value === null) {
    return EMPTY_TEXT
  }
  const text = String(value).trim()
  return text || EMPTY_TEXT
}

const numberOrDash = (value: any) => {
  if (value === undefined || value === null || value === '') {
    return EMPTY_TEXT
  }
  const parsed = Number(value)
  return Number.isFinite(parsed) ? String(parsed) : EMPTY_TEXT
}

const normalizeTextArray = (value: any): string[] => {
  if (!Array.isArray(value)) {
    return []
  }
  return value.map((item) => String(item || '').trim()).filter(Boolean)
}

const normalizeGuardItems = (value: any): LifecycleBatchLogApi.StoreLifecycleBatchLogGuardItem[] => {
  if (!Array.isArray(value)) {
    return []
  }
  return value.map((item) => ({
    guardKey: item?.guardKey,
    count: Number.isFinite(Number(item?.count)) ? Number(item?.count) : undefined,
    mode: item?.mode,
    blocked: item?.blocked === true
  }))
}

const normalizeResult = (item: any) => {
  if (item?.blocked === true) {
    return 'BLOCKED'
  }
  const warnings = normalizeTextArray(item?.warnings)
  if (warnings.length) {
    return 'WARNING'
  }
  return 'SUCCESS'
}

const normalizeDetailRows = (source: any[]): RecheckDetailRow[] => {
  if (!Array.isArray(source)) {
    return []
  }
  return source.map((item) => ({
    storeId: typeof item?.storeId === 'number' ? item.storeId : Number(item?.storeId || 0) || undefined,
    storeName: item?.storeName,
    result: normalizeResult(item),
    blocked: item?.blocked === true,
    blockedCode: typeof item?.blockedCode === 'number' ? item.blockedCode : Number(item?.blockedCode || 0) || undefined,
    blockedMessage: item?.blockedMessage,
    warnings: normalizeTextArray(item?.warnings),
    guardItems: normalizeGuardItems(item?.guardItems)
  }))
}

const extractDetailRows = (payload: Record<string, any>) => {
  const rowsFromDetails = normalizeDetailRows(payload?.details)
  if (rowsFromDetails.length) {
    return rowsFromDetails
  }
  const blockedRows = normalizeDetailRows(payload?.blocked).map((item) => ({ ...item, result: 'BLOCKED' }))
  const warningRows = normalizeDetailRows(payload?.warnings).map((item) => ({ ...item, result: 'WARNING' }))
  return [...blockedRows, ...warningRows]
}

const parseStructuredPayload = (detailView: any, rawJson?: string) => {
  if (detailView && typeof detailView === 'object' && !Array.isArray(detailView)) {
    return {
      payload: detailView as Record<string, any>,
      parseFailed: false
    }
  }
  const raw = String(rawJson || '').trim()
  if (!raw) {
    return {
      payload: null,
      parseFailed: false
    }
  }
  try {
    const parsed = JSON.parse(raw)
    if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
      return {
        payload: parsed as Record<string, any>,
        parseFailed: false
      }
    }
    return {
      payload: null,
      parseFailed: true
    }
  } catch {
    return {
      payload: null,
      parseFailed: true
    }
  }
}

const stringifyValue = (value: any) => {
  if (value === undefined || value === null) {
    return EMPTY_TEXT
  }
  if (typeof value === 'string') {
    const text = value.trim()
    return text || EMPTY_TEXT
  }
  if (typeof value === 'number' || typeof value === 'boolean') {
    return String(value)
  }
  try {
    const serialized = JSON.stringify(value)
    return serialized || EMPTY_TEXT
  } catch {
    return EMPTY_TEXT
  }
}

const parseGuardConfigRows = (rawJson?: string) => {
  guardConfigRows.value = []
  guardConfigParseFailed.value = false
  guardConfigRawJson.value = String(rawJson || '')

  const raw = guardConfigRawJson.value.trim()
  if (!raw) {
    return
  }
  try {
    const parsed = JSON.parse(raw)
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      guardConfigParseFailed.value = true
      return
    }
    guardConfigRows.value = Object.keys(parsed).map((key) => ({
      key,
      value: stringifyValue((parsed as Record<string, any>)[key])
    }))
  } catch {
    guardConfigParseFailed.value = true
  }
}

const warningsText = (warnings?: string[]) => {
  if (!warnings || !warnings.length) {
    return EMPTY_TEXT
  }
  return warnings.join('；')
}

const guardItemsText = (items?: LifecycleBatchLogApi.StoreLifecycleBatchLogGuardItem[]) => {
  if (!items || !items.length) {
    return EMPTY_TEXT
  }
  return items
    .map((item) => {
      const key = textOrDash(item.guardKey)
      const count = numberOrDash(item.count)
      const mode = textOrDash(item.mode)
      const blocked = item.blocked ? '阻塞' : '通过'
      return `${key}[count=${count}, mode=${mode}, ${blocked}]`
    })
    .join('；')
}

const copyText = async (value: any, label: string) => {
  const content = String(value || '').trim()
  if (!content) {
    message.warning(`${label}为空，无法复制`)
    return
  }
  try {
    await navigator.clipboard.writeText(content)
    message.success(`${label}复制成功`)
  } catch {
    message.error('复制失败，请检查浏览器剪贴板权限')
  }
}

const firstQueryValue = (value: any): string => {
  if (Array.isArray(value)) {
    return String(value[0] || '')
  }
  return String(value || '')
}

const parseNumber = (value: any): number | undefined => {
  const text = firstQueryValue(value).trim()
  if (!text) {
    return undefined
  }
  const parsed = Number(text)
  if (Number.isFinite(parsed)) {
    return parsed
  }
  return undefined
}

const initQueryFromRoute = () => {
  queryParams.batchNo = firstQueryValue(route.query.batchNo).trim() || undefined
  queryParams.recheckNo = firstQueryValue(route.query.recheckNo).trim() || undefined
  queryParams.logId = parseNumber(route.query.logId)
}

const normalizeQuery = () => {
  queryParams.recheckNo = String(queryParams.recheckNo || '').trim() || undefined
  queryParams.batchNo = String(queryParams.batchNo || '').trim() || undefined
  queryParams.operator = String(queryParams.operator || '').trim() || undefined
  queryParams.source = String(queryParams.source || '').trim().toUpperCase() || undefined
}

const getList = async () => {
  loading.value = true
  try {
    normalizeQuery()
    const data = await LifecycleRecheckLogApi.getStoreLifecycleRecheckLogPage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

const handleQuery = async () => {
  queryParams.pageNo = 1
  await getList()
}

const resetQuery = async () => {
  queryParams.pageNo = 1
  queryParams.pageSize = 10
  queryParams.recheckNo = undefined
  queryParams.logId = undefined
  queryParams.batchNo = undefined
  queryParams.targetLifecycleStatus = undefined
  queryParams.operator = undefined
  queryParams.source = undefined
  queryParams.createTime = undefined
  await getList()
}

const goBatchLog = (row?: LifecycleRecheckLogApi.StoreLifecycleRecheckLogItem) => {
  router.push({
    path: '/mall/product/store-master/store-lifecycle-batch-log',
    query: row
      ? {
          batchNo: row.batchNo
        }
      : undefined
  })
}

const goChangeOrder = () => {
  router.push('/mall/product/store-master/store-lifecycle-change-order')
}

const openDetailDrawer = async (row: LifecycleRecheckLogApi.StoreLifecycleRecheckLogItem) => {
  detailDrawerVisible.value = true
  detailLoading.value = true
  detailRows.value = []
  detailParseFailed.value = false
  detailRawJson.value = ''
  guardConfigRows.value = []
  guardConfigParseFailed.value = false
  guardConfigRawJson.value = ''

  try {
    const data = await LifecycleRecheckLogApi.getStoreLifecycleRecheckLog(row.id)
    detailData.value = data || {}
    detailRawJson.value = String(data?.detailJson || '')
    parseGuardConfigRows(data?.guardConfigSnapshotJson)

    const structured = parseStructuredPayload(data?.detailView, data?.detailJson)
    detailRows.value = structured.payload ? extractDetailRows(structured.payload) : []
    detailParseFailed.value = Boolean(data?.detailParseError) || structured.parseFailed
  } catch {
    detailData.value = {
      id: row.id,
      recheckNo: row.recheckNo,
      batchNo: row.batchNo
    }
    message.error('加载复核详情失败')
  } finally {
    detailLoading.value = false
  }
}

onMounted(async () => {
  initQueryFromRoute()
  await getList()
})
</script>
