<template>
  <ContentWrap>
    <el-form :inline="true" :model="queryParams" class="-mb-15px" label-width="116px">
      <el-form-item label="Batch No" prop="batchNo">
        <el-input
          v-model="queryParams.batchNo"
          class="!w-240px"
          clearable
          placeholder="Input batch no"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="Target Lifecycle" prop="targetLifecycleStatus">
        <el-select v-model="queryParams.targetLifecycleStatus" class="!w-180px" clearable placeholder="Select">
          <el-option :value="10" label="PREPARING" />
          <el-option :value="20" label="TRIAL" />
          <el-option :value="30" label="OPERATING" />
          <el-option :value="35" label="SUSPENDED" />
          <el-option :value="40" label="CLOSED" />
        </el-select>
      </el-form-item>
      <el-form-item label="Operator" prop="operator">
        <el-input
          v-model="queryParams.operator"
          class="!w-200px"
          clearable
          placeholder="Input operator"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="Source" prop="source">
        <el-select v-model="queryParams.source" class="!w-170px" clearable placeholder="Select source">
          <el-option label="ADMIN_UI" value="ADMIN_UI" />
        </el-select>
      </el-form-item>
      <el-form-item label="Create Time" prop="createTime">
        <el-date-picker
          v-model="queryParams.createTime"
          :default-time="[new Date('1 00:00:00'), new Date('1 23:59:59')]"
          class="!w-340px"
          end-placeholder="End"
          start-placeholder="Start"
          type="datetimerange"
          value-format="YYYY-MM-DD HH:mm:ss"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery">
          <Icon class="mr-5px" icon="ep:search" />
          Search
        </el-button>
        <el-button @click="resetQuery">
          <Icon class="mr-5px" icon="ep:refresh" />
          Reset
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="ID" prop="id" width="90" />
      <el-table-column label="Batch No" min-width="230" prop="batchNo" show-overflow-tooltip />
      <el-table-column label="Target Lifecycle" width="160">
        <template #default="{ row }">
          <el-tag :type="lifecycleTagType(row.targetLifecycleStatus)">
            {{ lifecycleText(row.targetLifecycleStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Total" prop="totalCount" width="90" />
      <el-table-column label="Success" prop="successCount" width="90" />
      <el-table-column label="Blocked" prop="blockedCount" width="90" />
      <el-table-column label="Warning" prop="warningCount" width="90" />
      <el-table-column label="Operator" prop="operator" width="140" show-overflow-tooltip />
      <el-table-column label="Source" prop="source" width="120" />
      <el-table-column :formatter="dateFormatter" label="Create Time" prop="createTime" width="180" />
      <el-table-column align="center" fixed="right" label="Actions" width="110">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetailDialog(row)">Detail</el-button>
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

  <el-dialog v-model="detailDialogVisible" :title="detailDialogTitle" width="980px">
    <el-alert v-if="detailParseError" :closable="false" :title="detailParseError" show-icon type="warning" />

    <div class="mb-12px mt-8px flex flex-wrap items-center gap-8px">
      <el-tag type="info">All {{ detailRows.length }}</el-tag>
      <el-tag type="success">SUCCESS {{ successRows.length }}</el-tag>
      <el-tag type="danger">BLOCKED {{ blockedRows.length }}</el-tag>
      <el-tag type="warning">WARNING {{ warningRows.length }}</el-tag>
    </div>

    <el-tabs v-model="activeDetailTab">
      <el-tab-pane label="All" name="all">
        <el-table :data="detailRows" border max-height="420px">
          <el-table-column label="Store ID" prop="storeId" width="120" />
          <el-table-column label="Store Name" min-width="180" prop="storeName" show-overflow-tooltip />
          <el-table-column label="Result" width="120">
            <template #default="{ row }">
              <el-tag :type="resultTagType(row.result)">{{ row.result }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="Message" min-width="320" prop="message" show-overflow-tooltip />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="SUCCESS" name="success">
        <el-table :data="successRows" border max-height="420px">
          <el-table-column label="Store ID" prop="storeId" width="120" />
          <el-table-column label="Store Name" min-width="180" prop="storeName" show-overflow-tooltip />
          <el-table-column label="Message" min-width="420" prop="message" show-overflow-tooltip />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="BLOCKED" name="blocked">
        <el-table :data="blockedRows" border max-height="420px">
          <el-table-column label="Store ID" prop="storeId" width="120" />
          <el-table-column label="Store Name" min-width="180" prop="storeName" show-overflow-tooltip />
          <el-table-column label="Message" min-width="420" prop="message" show-overflow-tooltip />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="WARNING" name="warning">
        <el-table :data="warningRows" border max-height="420px">
          <el-table-column label="Store ID" prop="storeId" width="120" />
          <el-table-column label="Store Name" min-width="180" prop="storeName" show-overflow-tooltip />
          <el-table-column label="Message" min-width="420" prop="message" show-overflow-tooltip />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </el-dialog>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import * as LifecycleBatchLogApi from '@/api/mall/store/lifecycleBatchLog'

defineOptions({ name: 'MallStoreLifecycleBatchLogIndex' })

type DetailTab = 'all' | 'success' | 'blocked' | 'warning'

interface DetailRow {
  storeId?: number
  storeName?: string
  result: string
  message?: string
}

const loading = ref(false)
const total = ref(0)
const list = ref<LifecycleBatchLogApi.StoreLifecycleBatchLogItem[]>([])

const queryParams = reactive<LifecycleBatchLogApi.StoreLifecycleBatchLogPageReq>({
  pageNo: 1,
  pageSize: 10,
  batchNo: undefined,
  targetLifecycleStatus: undefined,
  operator: undefined,
  source: undefined,
  createTime: undefined
})

const detailDialogVisible = ref(false)
const detailDialogTitle = ref('Lifecycle Batch Detail')
const detailParseError = ref('')
const activeDetailTab = ref<DetailTab>('all')
const detailRows = ref<DetailRow[]>([])

const successRows = computed(() => detailRows.value.filter((item) => item.result === 'SUCCESS'))
const blockedRows = computed(() => detailRows.value.filter((item) => item.result === 'BLOCKED'))
const warningRows = computed(() => detailRows.value.filter((item) => item.result === 'WARNING'))

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
  return ''
}

const resultTagType = (result?: string) => {
  if (result === 'SUCCESS') return 'success'
  if (result === 'BLOCKED') return 'danger'
  if (result === 'WARNING') return 'warning'
  return 'info'
}

const normalizeDetailRows = (source: any[]): DetailRow[] => {
  return (source || []).map((item) => ({
    storeId: item?.storeId,
    storeName: item?.storeName,
    result: String(item?.result || 'UNKNOWN').toUpperCase(),
    message: item?.message || ''
  }))
}

const parseDetailRows = (detailJson?: string) => {
  detailParseError.value = ''
  detailRows.value = []
  activeDetailTab.value = 'all'

  if (!detailJson) {
    detailParseError.value = 'detailJson is empty.'
    return
  }
  try {
    const payload = JSON.parse(detailJson)
    if (Array.isArray(payload?.details)) {
      detailRows.value = normalizeDetailRows(payload.details)
      return
    }
    const blocked = Array.isArray(payload?.blocked)
      ? normalizeDetailRows(payload.blocked).map((item) => ({ ...item, result: 'BLOCKED' }))
      : []
    const warnings = Array.isArray(payload?.warnings)
      ? normalizeDetailRows(payload.warnings).map((item) => ({ ...item, result: 'WARNING' }))
      : []
    detailRows.value = [...blocked, ...warnings]
    if (!detailRows.value.length) {
      detailParseError.value = 'No detail rows found in detailJson.'
    }
  } catch (error: any) {
    detailParseError.value = `detailJson parse failed: ${error?.message || 'unknown error'}`
  }
}

const getList = async () => {
  loading.value = true
  try {
    const data = await LifecycleBatchLogApi.getStoreLifecycleBatchLogPage(queryParams)
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
  queryParams.batchNo = undefined
  queryParams.targetLifecycleStatus = undefined
  queryParams.operator = undefined
  queryParams.source = undefined
  queryParams.createTime = undefined
  await getList()
}

const openDetailDialog = (row: LifecycleBatchLogApi.StoreLifecycleBatchLogItem) => {
  detailDialogTitle.value = `Lifecycle Batch Detail - ${row.batchNo}`
  detailDialogVisible.value = true
  parseDetailRows(row.detailJson)
}

onMounted(async () => {
  await getList()
})
</script>
