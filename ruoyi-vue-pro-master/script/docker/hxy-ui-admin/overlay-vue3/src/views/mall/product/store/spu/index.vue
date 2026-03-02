<template>
  <doc-alert title="门店 SPU 映射（总部商品 -> 门店上架）" url="https://doc.iocoder.cn/mall/product-spu-sku/" />

  <ContentWrap>
    <el-form :inline="true" :model="queryParams" class="-mb-15px" label-width="68px">
      <el-form-item label="门店ID" prop="storeId">
        <el-input
          v-model="queryParams.storeId"
          class="!w-180px"
          clearable
          placeholder="请输入门店ID"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="SPUID" prop="spuId">
        <el-input
          v-model="queryParams.spuId"
          class="!w-180px"
          clearable
          placeholder="请输入SPUID"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="商品类型" prop="productType">
        <el-select v-model="queryParams.productType" class="!w-160px" clearable placeholder="请选择类型">
          <el-option :value="2" label="服务" />
          <el-option :value="1" label="实物" />
        </el-select>
      </el-form-item>
      <el-form-item label="销售状态" prop="saleStatus">
        <el-select v-model="queryParams.saleStatus" class="!w-180px" clearable placeholder="请选择状态">
          <el-option :value="0" label="上架" />
          <el-option :value="1" label="下架" />
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
        <el-button v-hasPermi="['product:store-spu:create']" plain type="primary" @click="openForm()">
          <Icon class="mr-5px" icon="ep:plus" />
          新增映射
        </el-button>
        <el-button
          v-hasPermi="['product:store-spu:create', 'product:store-spu:update']"
          plain
          type="warning"
          @click="openBatchDialog"
        >
          <Icon class="mr-5px" icon="ep:connection" />
          批量铺货
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="ID" prop="id" width="90" />
      <el-table-column label="门店" min-width="220">
        <template #default="{ row }">
          <div>{{ row.storeName || '-' }}</div>
          <div class="text-12px text-[var(--el-text-color-secondary)]">ID: {{ row.storeId }}</div>
        </template>
      </el-table-column>
      <el-table-column label="SPU" min-width="260">
        <template #default="{ row }">
          <div>{{ row.spuName || '-' }}</div>
          <div class="text-12px text-[var(--el-text-color-secondary)]">ID: {{ row.spuId }}</div>
        </template>
      </el-table-column>
      <el-table-column label="商品类型" prop="productType" width="120">
        <template #default="{ row }">
          <el-tag v-if="row.productType === 2" type="success">服务</el-tag>
          <el-tag v-else-if="row.productType === 1">实物</el-tag>
          <el-tag v-else type="info">{{ row.productType ?? '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="销售状态" prop="saleStatus" width="120">
        <template #default="{ row }">
          <el-tag :type="row.saleStatus === 0 ? 'success' : 'info'">
            {{ row.saleStatus === 0 ? '上架' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="排序" prop="sort" width="90" />
      <el-table-column label="备注" min-width="220" prop="remark" />
      <el-table-column :formatter="dateFormatter" label="更新时间" prop="updateTime" width="180" />
      <el-table-column align="center" fixed="right" label="操作" width="160">
        <template #default="{ row }">
          <el-button v-hasPermi="['product:store-spu:update']" link type="primary" @click="openForm(row)">
            编辑
          </el-button>
          <el-button v-hasPermi="['product:store-spu:delete']" link type="danger" @click="handleDelete(row.id)">
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

  <el-dialog v-model="formVisible" :title="formData.id ? '编辑门店SPU映射' : '新增门店SPU映射'" width="620px">
    <el-form ref="formRef" :model="formData" :rules="rules" label-width="92px">
      <el-form-item label="门店ID" prop="storeId">
        <el-select
          v-model="formData.storeId"
          class="!w-460px"
          clearable
          filterable
          allow-create
          default-first-option
          placeholder="请选择或输入门店ID"
          :loading="storeOptionLoading"
          :remote-method="handleStoreSearch"
          remote
          reserve-keyword
        >
          <el-option v-for="item in storeOptions" :key="item.id" :label="formatStoreOptionLabel(item)" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="商品类型" prop="productType">
        <el-radio-group v-model="formProductType" @change="handleFormProductTypeChange">
          <el-radio :value="2">服务</el-radio>
          <el-radio :value="1">实物</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="SPUID" prop="spuId">
        <el-select
          v-model="formData.spuId"
          class="!w-460px"
          clearable
          filterable
          placeholder="请输入商品名或ID检索"
          remote
          reserve-keyword
          :loading="spuOptionLoading"
          :remote-method="handleSpuSearch"
        >
          <el-option
            v-for="item in spuOptions"
            :key="item.id"
            :label="formatSpuOptionLabel(item)"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="销售状态" prop="saleStatus">
        <el-radio-group v-model="formData.saleStatus">
          <el-radio :value="0">上架</el-radio>
          <el-radio :value="1">下架</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="formData.sort" :min="0" class="!w-460px" controls-position="right" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" maxlength="255" show-word-limit type="textarea" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="formVisible = false">取消</el-button>
      <el-button :loading="formLoading" type="primary" @click="submitForm">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="batchVisible" title="批量铺货 SPU 到门店" width="680px">
    <el-form ref="batchFormRef" :model="batchForm" :rules="batchRules" label-width="110px">
      <el-form-item label="商品类型">
        <el-radio-group v-model="batchProductType" @change="handleBatchProductTypeChange">
          <el-radio :value="2">服务</el-radio>
          <el-radio :value="1">实物</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="门店列表" prop="storeIds">
        <el-select
          v-model="batchForm.storeIds"
          class="!w-520px"
          multiple
          filterable
          remote
          reserve-keyword
          collapse-tags
          collapse-tags-tooltip
          placeholder="请选择门店，可搜索"
          :loading="storeOptionLoading"
          :remote-method="handleStoreSearch"
        >
          <el-option v-for="item in storeOptions" :key="item.id" :label="formatStoreOptionLabel(item)" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="SPU" prop="spuId">
        <el-select
          v-model="batchForm.spuId"
          class="!w-520px"
          clearable
          filterable
          remote
          reserve-keyword
          placeholder="请输入商品名或ID检索"
          :loading="spuOptionLoading"
          :remote-method="handleBatchSpuSearch"
        >
          <el-option
            v-for="item in spuOptions"
            :key="item.id"
            :label="formatSpuOptionLabel(item)"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="销售状态">
        <el-radio-group v-model="batchForm.saleStatus">
          <el-radio :value="0">上架</el-radio>
          <el-radio :value="1">下架</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="batchForm.sort" :min="0" class="!w-520px" controls-position="right" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="batchForm.remark" maxlength="255" show-word-limit type="textarea" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="batchVisible = false">取消</el-button>
      <el-button :loading="batchLoading" type="primary" @click="submitBatchForm">确认批量铺货</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import * as StoreSpuApi from '@/api/mall/product/storeSpu'

defineOptions({ name: 'ProductStoreSpuMapping' })

const message = useMessage()

const loading = ref(false)
const formLoading = ref(false)
const formVisible = ref(false)
const batchVisible = ref(false)
const batchLoading = ref(false)
const total = ref(0)
const list = ref<StoreSpuApi.ProductStoreSpu[]>([])
const formRef = ref()
const batchFormRef = ref()
const spuOptionLoading = ref(false)
const storeOptionLoading = ref(false)
const formProductType = ref<number>(2)

const spuOptions = ref<StoreSpuApi.ProductStoreSpuOption[]>([])
const storeOptions = ref<StoreSpuApi.ProductStoreOption[]>([])

const queryParams = ref<any>({
  pageNo: 1,
  pageSize: 10,
  storeId: undefined,
  spuId: undefined,
  productType: undefined,
  saleStatus: undefined
})

const formData = ref<StoreSpuApi.ProductStoreSpu>({
  id: undefined,
  storeId: 1,
  spuId: undefined,
  saleStatus: 0,
  sort: 0,
  remark: ''
})

const batchForm = ref<StoreSpuApi.ProductStoreSpuBatchSave>({
  storeIds: [],
  spuId: undefined,
  saleStatus: 0,
  sort: 0,
  remark: ''
})
const batchProductType = ref<number>(2)

const rules = {
  storeId: [{ required: true, message: '门店ID不能为空', trigger: 'change' }],
  spuId: [{ required: true, message: 'SPU不能为空', trigger: 'change' }]
}
const batchRules = {
  storeIds: [{ required: true, message: '请至少选择一个门店', trigger: 'change' }],
  spuId: [{ required: true, message: 'SPU不能为空', trigger: 'change' }]
}

const getList = async () => {
  loading.value = true
  try {
    const data = await StoreSpuApi.getStoreSpuPage(queryParams.value)
    list.value = data.list
    total.value = data.total
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
    storeId: undefined,
    spuId: undefined,
    productType: undefined,
    saleStatus: undefined
  }
  getList()
}

const normalizeNumeric = (value: any): number | undefined => {
  if (value === undefined || value === null || value === '') {
    return undefined
  }
  const parsed = Number(value)
  return Number.isNaN(parsed) ? undefined : parsed
}

const mergeSpuOptions = (items: StoreSpuApi.ProductStoreSpuOption[]) => {
  const merged = new Map<number, StoreSpuApi.ProductStoreSpuOption>()
  spuOptions.value.forEach((item) => merged.set(item.id, item))
  items.forEach((item) => merged.set(item.id, item))
  spuOptions.value = Array.from(merged.values())
}

const loadStoreOptions = async (keyword = '') => {
  storeOptionLoading.value = true
  try {
    storeOptions.value = await StoreSpuApi.getStoreOptions(keyword || undefined)
  } finally {
    storeOptionLoading.value = false
  }
}

const handleStoreSearch = (keyword: string) => {
  loadStoreOptions(keyword)
}

const ensureCurrentSpuOption = async (currentSpuId?: number, productType?: number) => {
  if (!currentSpuId) {
    return
  }
  if (spuOptions.value.some((item) => item.id === currentSpuId)) {
    return
  }
  const data = await StoreSpuApi.getSpuOptions(productType, undefined)
  mergeSpuOptions(data.filter((item) => item.id === currentSpuId))
}

const loadSpuOptions = async (keyword = '', productType = formProductType.value, currentSpuId?: number) => {
  spuOptionLoading.value = true
  try {
    const items = await StoreSpuApi.getSpuOptions(productType, keyword || undefined)
    spuOptions.value = items
    await ensureCurrentSpuOption(currentSpuId, productType)
  } finally {
    spuOptionLoading.value = false
  }
}

const handleSpuSearch = (keyword: string) => {
  loadSpuOptions(keyword, formProductType.value, formData.value.spuId)
}

const handleBatchSpuSearch = (keyword: string) => {
  loadSpuOptions(keyword, batchProductType.value, batchForm.value.spuId)
}

const handleFormProductTypeChange = async () => {
  formData.value.spuId = undefined
  await loadSpuOptions('', formProductType.value, formData.value.spuId)
}

const handleBatchProductTypeChange = async () => {
  batchForm.value.spuId = undefined
  await loadSpuOptions('', batchProductType.value, batchForm.value.spuId)
}

const formatSpuOptionLabel = (item: StoreSpuApi.ProductStoreSpuOption) => {
  const typeLabel = item.productType === 2 ? '服务' : '实物'
  return `#${item.id} ${item.name}（${typeLabel}）`
}

const formatStoreOptionLabel = (item: StoreSpuApi.ProductStoreOption) => {
  return `#${item.id} ${item.name || ''}`.trim()
}

const openForm = async (row?: StoreSpuApi.ProductStoreSpu) => {
  spuOptions.value = []
  if (row) {
    formData.value = { ...row }
    formProductType.value = Number(row.productType) === 2 ? 2 : 1
  } else {
    formData.value = {
      id: undefined,
      storeId: 1,
      spuId: undefined,
      saleStatus: 0,
      sort: 0,
      remark: ''
    }
    formProductType.value = 2
  }
  await loadStoreOptions()
  await loadSpuOptions('', formProductType.value, formData.value.spuId)
  formVisible.value = true
}

const openBatchDialog = async () => {
  batchProductType.value = 2
  batchForm.value = {
    storeIds: [],
    spuId: undefined,
    saleStatus: 0,
    sort: 0,
    remark: ''
  }
  await loadStoreOptions()
  await loadSpuOptions('', batchProductType.value, batchForm.value.spuId)
  batchVisible.value = true
}

const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    const payload: StoreSpuApi.ProductStoreSpu = {
      ...formData.value,
      storeId: normalizeNumeric(formData.value.storeId)!,
      spuId: normalizeNumeric(formData.value.spuId)
    }
    await StoreSpuApi.saveStoreSpu(payload)
    message.success(formData.value.id ? '更新成功' : '创建成功')
    formVisible.value = false
    await getList()
  } finally {
    formLoading.value = false
  }
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await StoreSpuApi.deleteStoreSpu(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

const submitBatchForm = async () => {
  await batchFormRef.value.validate()
  batchLoading.value = true
  try {
    const payload: StoreSpuApi.ProductStoreSpuBatchSave = {
      ...batchForm.value,
      storeIds: batchForm.value.storeIds.map((id) => Number(id)),
      spuId: Number(batchForm.value.spuId)
    }
    const affected = await StoreSpuApi.batchSaveStoreSpu(payload)
    message.success(`批量铺货完成，影响 ${affected} 家门店`)
    batchVisible.value = false
    await getList()
  } finally {
    batchLoading.value = false
  }
}

onMounted(() => {
  loadStoreOptions()
  getList()
})
</script>
