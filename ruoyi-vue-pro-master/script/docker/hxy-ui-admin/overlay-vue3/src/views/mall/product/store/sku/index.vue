<template>
  <doc-alert title="门店 SKU 映射（总部SKU -> 门店价格库存）" url="https://doc.iocoder.cn/mall/product-spu-sku/" />

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
      <el-form-item label="SKUID" prop="skuId">
        <el-input
          v-model="queryParams.skuId"
          class="!w-180px"
          clearable
          placeholder="请输入SKUID"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="销售状态" prop="saleStatus">
        <el-select v-model="queryParams.saleStatus" class="!w-160px" clearable placeholder="请选择状态">
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
        <el-button v-hasPermi="['product:store-sku:create']" plain type="primary" @click="openForm()">
          <Icon class="mr-5px" icon="ep:plus" />
          新增映射
        </el-button>
        <el-button
          v-hasPermi="['product:store-sku:create', 'product:store-sku:update']"
          plain
          type="warning"
          @click="openBatchSaveDialog"
        >
          <Icon class="mr-5px" icon="ep:connection" />
          批量铺货
        </el-button>
        <el-button
          v-hasPermi="['product:store-sku:update']"
          plain
          type="success"
          @click="openBatchAdjustDialog"
        >
          <Icon class="mr-5px" icon="ep:edit-pen" />
          批量调价/库存
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
      <el-table-column label="SKU" min-width="240">
        <template #default="{ row }">
          <div>{{ row.skuSpecText || '默认规格' }}</div>
          <div class="text-12px text-[var(--el-text-color-secondary)]">ID: {{ row.skuId }}</div>
        </template>
      </el-table-column>
      <el-table-column label="销售状态" prop="saleStatus" width="110">
        <template #default="{ row }">
          <el-tag :type="row.saleStatus === 0 ? 'success' : 'info'">
            {{ row.saleStatus === 0 ? '上架' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="销售价(分)" prop="salePrice" width="120" />
      <el-table-column label="划线价(分)" prop="marketPrice" width="120" />
      <el-table-column label="库存" prop="stock" width="100" />
      <el-table-column label="排序" prop="sort" width="90" />
      <el-table-column label="备注" min-width="220" prop="remark" />
      <el-table-column :formatter="dateFormatter" label="更新时间" prop="updateTime" width="180" />
      <el-table-column align="center" fixed="right" label="操作" width="160">
        <template #default="{ row }">
          <el-button v-hasPermi="['product:store-sku:update']" link type="primary" @click="openForm(row)">
            编辑
          </el-button>
          <el-button v-hasPermi="['product:store-sku:delete']" link type="danger" @click="handleDelete(row.id)">
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

  <el-dialog v-model="formVisible" :title="formData.id ? '编辑门店SKU映射' : '新增门店SKU映射'" width="620px">
    <el-form ref="formRef" :model="formData" :rules="rules" label-width="98px">
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
      <el-form-item label="SPU" prop="spuId">
        <el-select
          v-model="formData.spuId"
          class="!w-460px"
          clearable
          filterable
          placeholder="请输入商品名或ID检索"
          remote
          reserve-keyword
          :loading="spuOptionLoading"
          :remote-method="handleBatchSaveSpuSearch"
          @change="handleSpuChange"
        >
          <el-option
            v-for="item in spuOptions"
            :key="item.id"
            :label="formatSpuOptionLabel(item)"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="SKUID" prop="skuId">
        <el-select
          v-model="formData.skuId"
          class="!w-460px"
          clearable
          filterable
          placeholder="请选择SKU"
          :loading="skuOptionLoading"
          @change="handleSkuChange"
        >
          <el-option
            v-for="item in skuOptions"
            :key="item.id"
            :label="formatSkuOptionLabel(item)"
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
      <el-form-item label="销售价(分)" prop="salePrice">
        <el-input-number v-model="formData.salePrice" :min="0" class="!w-460px" controls-position="right" />
      </el-form-item>
      <el-form-item label="划线价(分)" prop="marketPrice">
        <el-input-number v-model="formData.marketPrice" :min="0" class="!w-460px" controls-position="right" />
      </el-form-item>
      <el-form-item label="库存" prop="stock">
        <el-input-number v-model="formData.stock" :min="0" class="!w-460px" controls-position="right" />
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

  <el-dialog v-model="batchSaveVisible" title="批量铺货 SKU 到门店" width="720px">
    <el-form ref="batchSaveFormRef" :model="batchSaveForm" :rules="batchSaveRules" label-width="120px">
      <el-form-item label="商品类型">
        <el-radio-group v-model="batchSaveProductType" @change="handleBatchSaveProductTypeChange">
          <el-radio :value="2">服务</el-radio>
          <el-radio :value="1">实物</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="门店列表" prop="storeIds">
        <el-select
          v-model="batchSaveForm.storeIds"
          class="!w-560px"
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
          v-model="batchSaveForm.spuId"
          class="!w-560px"
          clearable
          filterable
          remote
          reserve-keyword
          placeholder="请输入商品名或ID检索"
          :loading="spuOptionLoading"
          :remote-method="handleBatchAdjustSpuSearch"
          @change="handleBatchSaveSpuChange"
        >
          <el-option v-for="item in spuOptions" :key="item.id" :label="formatSpuOptionLabel(item)" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="SKU" prop="skuId">
        <el-select
          v-model="batchSaveForm.skuId"
          class="!w-560px"
          clearable
          filterable
          placeholder="请选择SKU"
          :loading="skuOptionLoading"
        >
          <el-option v-for="item in skuOptions" :key="item.id" :label="formatSkuOptionLabel(item)" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="销售状态">
        <el-radio-group v-model="batchSaveForm.saleStatus">
          <el-radio :value="0">上架</el-radio>
          <el-radio :value="1">下架</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="销售价(分)">
        <el-input-number v-model="batchSaveForm.salePrice" :min="0" class="!w-560px" controls-position="right" />
      </el-form-item>
      <el-form-item label="划线价(分)">
        <el-input-number v-model="batchSaveForm.marketPrice" :min="0" class="!w-560px" controls-position="right" />
      </el-form-item>
      <el-form-item label="库存">
        <el-input-number v-model="batchSaveForm.stock" :min="0" class="!w-560px" controls-position="right" />
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="batchSaveForm.sort" :min="0" class="!w-560px" controls-position="right" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="batchSaveForm.remark" maxlength="255" show-word-limit type="textarea" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="batchSaveVisible = false">取消</el-button>
      <el-button :loading="batchSaveLoading" type="primary" @click="submitBatchSaveForm">确认批量铺货</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="batchAdjustVisible" title="按门店集批量调价/调库存" width="720px">
    <el-form ref="batchAdjustFormRef" :model="batchAdjustForm" :rules="batchAdjustRules" label-width="120px">
      <el-form-item label="商品类型">
        <el-radio-group v-model="batchAdjustProductType" @change="handleBatchAdjustProductTypeChange">
          <el-radio :value="2">服务</el-radio>
          <el-radio :value="1">实物</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="门店列表" prop="storeIds">
        <el-select
          v-model="batchAdjustForm.storeIds"
          class="!w-560px"
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
          v-model="batchAdjustForm.spuId"
          class="!w-560px"
          clearable
          filterable
          remote
          reserve-keyword
          placeholder="请输入商品名或ID检索"
          :loading="spuOptionLoading"
          :remote-method="handleSpuSearch"
          @change="handleBatchAdjustSpuChange"
        >
          <el-option v-for="item in spuOptions" :key="item.id" :label="formatSpuOptionLabel(item)" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="SKU" prop="skuId">
        <el-select
          v-model="batchAdjustForm.skuId"
          class="!w-560px"
          clearable
          filterable
          placeholder="请选择SKU"
          :loading="skuOptionLoading"
        >
          <el-option v-for="item in skuOptions" :key="item.id" :label="formatSkuOptionLabel(item)" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="销售状态">
        <el-radio-group v-model="batchAdjustForm.saleStatus">
          <el-radio :value="0">上架</el-radio>
          <el-radio :value="1">下架</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="销售价(分)">
        <el-input-number v-model="batchAdjustForm.salePrice" :min="0" class="!w-560px" controls-position="right" />
      </el-form-item>
      <el-form-item label="划线价(分)">
        <el-input-number v-model="batchAdjustForm.marketPrice" :min="0" class="!w-560px" controls-position="right" />
      </el-form-item>
      <el-form-item label="库存">
        <el-input-number v-model="batchAdjustForm.stock" :min="0" class="!w-560px" controls-position="right" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="batchAdjustForm.remark" maxlength="255" show-word-limit type="textarea" />
      </el-form-item>
      <div class="text-12px text-[var(--el-text-color-secondary)] mb-12px">
        至少设置一个调整字段（销售状态/售价/划线价/库存/备注）
      </div>
    </el-form>
    <template #footer>
      <el-button @click="batchAdjustVisible = false">取消</el-button>
      <el-button :loading="batchAdjustLoading" type="primary" @click="submitBatchAdjustForm">确认批量更新</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import * as StoreSkuApi from '@/api/mall/product/storeSku'

defineOptions({ name: 'ProductStoreSkuMapping' })

const message = useMessage()

const loading = ref(false)
const formLoading = ref(false)
const formVisible = ref(false)
const batchSaveVisible = ref(false)
const batchAdjustVisible = ref(false)
const batchSaveLoading = ref(false)
const batchAdjustLoading = ref(false)
const total = ref(0)
const list = ref<StoreSkuApi.ProductStoreSku[]>([])
const formRef = ref()
const batchSaveFormRef = ref()
const batchAdjustFormRef = ref()
const spuOptionLoading = ref(false)
const skuOptionLoading = ref(false)
const storeOptionLoading = ref(false)
const formProductType = ref<number>(2)
const batchSaveProductType = ref<number>(2)
const batchAdjustProductType = ref<number>(2)

const spuOptions = ref<StoreSkuApi.ProductStoreSpuOption[]>([])
const skuOptions = ref<StoreSkuApi.ProductStoreSkuOption[]>([])
const storeOptions = ref<StoreSkuApi.ProductStoreOption[]>([])

const queryParams = ref<any>({
  pageNo: 1,
  pageSize: 10,
  storeId: undefined,
  spuId: undefined,
  skuId: undefined,
  saleStatus: undefined
})

const formData = ref<StoreSkuApi.ProductStoreSku>({
  id: undefined,
  storeId: 1,
  spuId: undefined,
  skuId: undefined,
  saleStatus: 0,
  salePrice: 0,
  marketPrice: 0,
  stock: 0,
  sort: 0,
  remark: ''
})

const batchSaveForm = ref<StoreSkuApi.ProductStoreSkuBatchSave>({
  storeIds: [],
  spuId: undefined,
  skuId: undefined,
  saleStatus: 0,
  salePrice: undefined,
  marketPrice: undefined,
  stock: undefined,
  sort: 0,
  remark: ''
})

const batchAdjustForm = ref<StoreSkuApi.ProductStoreSkuBatchAdjust>({
  storeIds: [],
  spuId: undefined,
  skuId: undefined,
  saleStatus: undefined,
  salePrice: undefined,
  marketPrice: undefined,
  stock: undefined,
  remark: ''
})

const rules = {
  storeId: [{ required: true, message: '门店ID不能为空', trigger: 'change' }],
  spuId: [{ required: true, message: 'SPU不能为空', trigger: 'change' }],
  skuId: [{ required: true, message: 'SKU不能为空', trigger: 'change' }]
}

const batchSaveRules = {
  storeIds: [{ required: true, message: '请至少选择一个门店', trigger: 'change' }],
  spuId: [{ required: true, message: 'SPU不能为空', trigger: 'change' }],
  skuId: [{ required: true, message: 'SKU不能为空', trigger: 'change' }]
}

const batchAdjustRules = {
  storeIds: [{ required: true, message: '请至少选择一个门店', trigger: 'change' }],
  spuId: [{ required: true, message: 'SPU不能为空', trigger: 'change' }],
  skuId: [{ required: true, message: 'SKU不能为空', trigger: 'change' }]
}

const getList = async () => {
  loading.value = true
  try {
    const data = await StoreSkuApi.getStoreSkuPage(queryParams.value)
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
    skuId: undefined,
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

const loadStoreOptions = async (keyword = '') => {
  storeOptionLoading.value = true
  try {
    storeOptions.value = await StoreSkuApi.getStoreOptions(keyword || undefined)
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
  const data = await StoreSkuApi.getSpuOptions(productType)
  const matched = data.find((item) => item.id === currentSpuId)
  if (!matched) {
    return
  }
  spuOptions.value = [...spuOptions.value, matched]
}

const loadSpuOptions = async (keyword = '', productType = formProductType.value, currentSpuId?: number) => {
  spuOptionLoading.value = true
  try {
    spuOptions.value = await StoreSkuApi.getSpuOptions(productType, keyword || undefined)
    await ensureCurrentSpuOption(currentSpuId, productType)
  } finally {
    spuOptionLoading.value = false
  }
}

const loadSkuOptions = async (spuId?: number) => {
  if (!spuId) {
    skuOptions.value = []
    return
  }
  skuOptionLoading.value = true
  try {
    skuOptions.value = await StoreSkuApi.getSkuOptions(spuId)
  } finally {
    skuOptionLoading.value = false
  }
}

const formatSpuOptionLabel = (item: StoreSkuApi.ProductStoreSpuOption) => {
  const typeLabel = item.productType === 2 ? '服务' : '实物'
  return `#${item.id} ${item.name}（${typeLabel}）`
}

const formatSkuOptionLabel = (item: StoreSkuApi.ProductStoreSkuOption) => {
  const spec = item.specText ? ` ${item.specText}` : ''
  return `#${item.id}${spec} 售价:${item.price ?? 0} 库存:${item.stock ?? 0}`
}

const formatStoreOptionLabel = (item: StoreSkuApi.ProductStoreOption) => {
  return `#${item.id} ${item.name || ''}`.trim()
}

const handleSpuSearch = (keyword: string) => {
  loadSpuOptions(keyword, formProductType.value, formData.value.spuId)
}

const handleBatchSaveSpuSearch = (keyword: string) => {
  loadSpuOptions(keyword, batchSaveProductType.value, batchSaveForm.value.spuId)
}

const handleBatchAdjustSpuSearch = (keyword: string) => {
  loadSpuOptions(keyword, batchAdjustProductType.value, batchAdjustForm.value.spuId)
}

const handleFormProductTypeChange = async () => {
  formData.value.spuId = undefined
  formData.value.skuId = undefined
  skuOptions.value = []
  await loadSpuOptions('', formProductType.value, formData.value.spuId)
}

const handleBatchSaveProductTypeChange = async () => {
  batchSaveForm.value.skuId = undefined
  batchSaveForm.value.spuId = undefined
  skuOptions.value = []
  await loadSpuOptions('', batchSaveProductType.value, batchSaveForm.value.spuId)
}

const handleBatchAdjustProductTypeChange = async () => {
  batchAdjustForm.value.skuId = undefined
  batchAdjustForm.value.spuId = undefined
  skuOptions.value = []
  await loadSpuOptions('', batchAdjustProductType.value, batchAdjustForm.value.spuId)
}

const handleSpuChange = async () => {
  formData.value.skuId = undefined
  await loadSkuOptions(formData.value.spuId)
}

const handleSkuChange = (skuId?: number) => {
  const selected = skuOptions.value.find((item) => item.id === skuId)
  if (!selected) {
    return
  }
  if (!formData.value.salePrice) {
    formData.value.salePrice = selected.price
  }
  if (!formData.value.marketPrice) {
    formData.value.marketPrice = selected.marketPrice
  }
  if (!formData.value.stock) {
    formData.value.stock = selected.stock
  }
}

const detectProductTypeBySpuId = async (spuId: number): Promise<number> => {
  try {
    const serviceOptions = await StoreSkuApi.getSpuOptions(2)
    if (serviceOptions.some((item) => item.id === spuId)) {
      return 2
    }
    return 1
  } catch {
    return 1
  }
}

const openForm = async (row?: StoreSkuApi.ProductStoreSku) => {
  spuOptions.value = []
  skuOptions.value = []
  if (row) {
    formData.value = { ...row }
    formProductType.value = row.spuId ? await detectProductTypeBySpuId(row.spuId) : 2
  } else {
    formData.value = {
      id: undefined,
      storeId: 1,
      spuId: undefined,
      skuId: undefined,
      saleStatus: 0,
      salePrice: 0,
      marketPrice: 0,
      stock: 0,
      sort: 0,
      remark: ''
    }
    formProductType.value = 2
  }
  await loadStoreOptions()
  await loadSpuOptions('', formProductType.value, formData.value.spuId)
  await loadSkuOptions(formData.value.spuId)
  formVisible.value = true
}

const openBatchSaveDialog = async () => {
  batchSaveProductType.value = 2
  batchSaveForm.value = {
    storeIds: [],
    skuId: undefined,
    saleStatus: 0,
    salePrice: undefined,
    marketPrice: undefined,
    stock: undefined,
    sort: 0,
    remark: '',
    spuId: undefined
  }
  spuOptions.value = []
  skuOptions.value = []
  await loadStoreOptions()
  await loadSpuOptions('', batchSaveProductType.value, batchSaveForm.value.spuId)
  batchSaveVisible.value = true
}

const openBatchAdjustDialog = async () => {
  batchAdjustProductType.value = 2
  batchAdjustForm.value = {
    storeIds: [],
    skuId: undefined,
    saleStatus: undefined,
    salePrice: undefined,
    marketPrice: undefined,
    stock: undefined,
    remark: '',
    spuId: undefined
  }
  spuOptions.value = []
  skuOptions.value = []
  await loadStoreOptions()
  await loadSpuOptions('', batchAdjustProductType.value, batchAdjustForm.value.spuId)
  batchAdjustVisible.value = true
}

const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    const payload: StoreSkuApi.ProductStoreSku = {
      ...formData.value,
      storeId: normalizeNumeric(formData.value.storeId)!,
      spuId: normalizeNumeric(formData.value.spuId),
      skuId: normalizeNumeric(formData.value.skuId)
    }
    await StoreSkuApi.saveStoreSku(payload)
    message.success(formData.value.id ? '更新成功' : '创建成功')
    formVisible.value = false
    await getList()
  } finally {
    formLoading.value = false
  }
}

const handleBatchSaveSpuChange = async () => {
  batchSaveForm.value.skuId = undefined
  await loadSkuOptions(batchSaveForm.value.spuId)
}

const handleBatchAdjustSpuChange = async () => {
  batchAdjustForm.value.skuId = undefined
  await loadSkuOptions(batchAdjustForm.value.spuId)
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await StoreSkuApi.deleteStoreSku(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

const submitBatchSaveForm = async () => {
  await batchSaveFormRef.value.validate()
  batchSaveLoading.value = true
  try {
    const payload: StoreSkuApi.ProductStoreSkuBatchSave = {
      ...batchSaveForm.value,
      storeIds: batchSaveForm.value.storeIds.map((id) => Number(id)),
      skuId: Number(batchSaveForm.value.skuId),
      salePrice: normalizeNumeric(batchSaveForm.value.salePrice),
      marketPrice: normalizeNumeric(batchSaveForm.value.marketPrice),
      stock: normalizeNumeric(batchSaveForm.value.stock),
      sort: normalizeNumeric(batchSaveForm.value.sort)
    }
    const affected = await StoreSkuApi.batchSaveStoreSku(payload)
    message.success(`批量铺货完成，影响 ${affected} 家门店`)
    batchSaveVisible.value = false
    await getList()
  } finally {
    batchSaveLoading.value = false
  }
}

const submitBatchAdjustForm = async () => {
  await batchAdjustFormRef.value.validate()
  batchAdjustLoading.value = true
  try {
    const payload: StoreSkuApi.ProductStoreSkuBatchAdjust = {
      storeIds: batchAdjustForm.value.storeIds.map((id) => Number(id)),
      skuId: Number(batchAdjustForm.value.skuId),
      saleStatus: normalizeNumeric(batchAdjustForm.value.saleStatus),
      salePrice: normalizeNumeric(batchAdjustForm.value.salePrice),
      marketPrice: normalizeNumeric(batchAdjustForm.value.marketPrice),
      stock: normalizeNumeric(batchAdjustForm.value.stock),
      remark: batchAdjustForm.value.remark
    }
    const affected = await StoreSkuApi.batchAdjustStoreSku(payload)
    message.success(`批量更新完成，影响 ${affected} 家门店`)
    batchAdjustVisible.value = false
    await getList()
  } finally {
    batchAdjustLoading.value = false
  }
}

onMounted(() => {
  loadStoreOptions()
  getList()
})
</script>
