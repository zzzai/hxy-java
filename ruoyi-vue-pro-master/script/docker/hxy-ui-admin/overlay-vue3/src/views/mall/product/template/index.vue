<template>
  <ContentWrap>
    <el-alert
      title="类目模板与 SKU 生成联调台"
      type="info"
      show-icon
      :closable="false"
      description="用于校验模板、预览规格组合、提交 SKU 生成。"
    />
  </ContentWrap>

  <ContentWrap>
    <el-form :inline="true" :model="baseForm" label-width="92px" class="-mb-15px">
      <el-form-item label="类目ID">
        <el-input-number v-model="baseForm.categoryId" :min="1" class="!w-180px" />
      </el-form-item>
      <el-form-item label="模板版本ID">
        <el-input-number v-model="baseForm.templateVersionId" :min="1" class="!w-180px" />
      </el-form-item>
      <el-form-item label="SPU ID">
        <el-input-number v-model="baseForm.spuId" :min="1" class="!w-180px" />
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <template #header>
      <div class="flex items-center justify-between w-full">
        <span class="text-base font-bold">1. 模板校验</span>
        <div class="flex gap-8px">
          <el-button type="primary" plain @click="appendValidateItem">
            <Icon icon="ep:plus" class="mr-4px" />
            增加模板项
          </el-button>
          <el-button :loading="validateLoading" type="success" @click="handleValidate">校验模板</el-button>
        </div>
      </div>
    </template>

    <el-table :data="validateItems" border>
      <el-table-column label="属性ID" min-width="120">
        <template #default="{ row }">
          <el-input-number v-model="row.attributeId" :min="1" class="!w-full" />
        </template>
      </el-table-column>
      <el-table-column label="角色" min-width="180">
        <template #default="{ row }">
          <el-select v-model="row.attrRole" class="!w-full">
            <el-option :value="1" label="SPU_ATTR" />
            <el-option :value="2" label="SKU_SPEC" />
            <el-option :value="3" label="SKU_ATTR" />
            <el-option :value="4" label="SALE_ATTR" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="必填" width="90">
        <template #default="{ row }">
          <el-switch v-model="row.required" />
        </template>
      </el-table-column>
      <el-table-column label="影响价格" width="110">
        <template #default="{ row }">
          <el-switch v-model="row.affectsPrice" />
        </template>
      </el-table-column>
      <el-table-column label="影响库存" width="110">
        <template #default="{ row }">
          <el-switch v-model="row.affectsStock" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ $index }">
          <el-button type="danger" link @click="removeValidateItem($index)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="validateResult" class="mt-12px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="校验结果">
          <el-tag :type="validateResult.pass ? 'success' : 'danger'">
            {{ validateResult.pass ? '通过' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="错误列表">
          <div v-if="!validateResult.errors?.length" class="text-gray-500">无</div>
          <div v-for="(item, idx) in validateResult.errors" :key="`e-${idx}`" class="text-red-500">
            [{{ item.code }}] {{ item.message }}
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="告警列表">
          <div v-if="!validateResult.warnings?.length" class="text-gray-500">无</div>
          <div v-for="(item, idx) in validateResult.warnings" :key="`w-${idx}`" class="text-orange-500">
            [{{ item.code }}] {{ item.message }}
          </div>
        </el-descriptions-item>
      </el-descriptions>
    </div>
  </ContentWrap>

  <ContentWrap>
    <template #header>
      <div class="flex items-center justify-between w-full">
        <span class="text-base font-bold">2. SKU 预览</span>
        <div class="flex gap-8px">
          <el-button type="primary" plain @click="appendSpecSelection">
            <Icon icon="ep:plus" class="mr-4px" />
            增加规格维度
          </el-button>
          <el-button :loading="previewLoading" type="success" @click="handlePreview">预览组合</el-button>
        </div>
      </div>
    </template>

    <el-form :inline="true" :model="previewForm" label-width="90px" class="-mb-10px">
      <el-form-item label="销售价(分)">
        <el-input-number v-model="previewForm.baseSku.price" :min="0" class="!w-160px" />
      </el-form-item>
      <el-form-item label="划线价(分)">
        <el-input-number v-model="previewForm.baseSku.marketPrice" :min="0" class="!w-160px" />
      </el-form-item>
      <el-form-item label="成本价(分)">
        <el-input-number v-model="previewForm.baseSku.costPrice" :min="0" class="!w-160px" />
      </el-form-item>
      <el-form-item label="库存">
        <el-input-number v-model="previewForm.baseSku.stock" :min="0" class="!w-160px" />
      </el-form-item>
    </el-form>

    <el-table :data="specSelections" border class="mt-10px">
      <el-table-column label="属性ID" width="140">
        <template #default="{ row }">
          <el-input-number v-model="row.attributeId" :min="1" class="!w-full" />
        </template>
      </el-table-column>
      <el-table-column label="选项ID列表(逗号分隔)">
        <template #default="{ row }">
          <el-input v-model="row.optionIdsText" placeholder="例如 11,12,13" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90">
        <template #default="{ $index }">
          <el-button link type="danger" @click="removeSpecSelection($index)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="previewResult" class="mt-12px">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="任务号">{{ previewResult.taskNo }}</el-descriptions-item>
        <el-descriptions-item label="组合数">{{ previewResult.combinationCount }}</el-descriptions-item>
        <el-descriptions-item label="是否截断">
          <el-tag :type="previewResult.truncated ? 'warning' : 'success'">
            {{ previewResult.truncated ? '是' : '否' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <el-table :data="previewResult.items" border class="mt-10px" max-height="320">
        <el-table-column label="specHash" prop="specHash" min-width="220" show-overflow-tooltip />
        <el-table-column label="规格摘要" prop="specSummary" min-width="220" show-overflow-tooltip />
        <el-table-column label="已存在SKU" prop="existsSkuId" width="120" />
        <el-table-column label="建议售价(分)" min-width="120">
          <template #default="{ row }">{{ row.suggestedSku?.price }}</template>
        </el-table-column>
      </el-table>
    </div>
  </ContentWrap>

  <ContentWrap>
    <template #header>
      <div class="flex items-center justify-between w-full">
        <span class="text-base font-bold">3. 提交生成</span>
        <el-button :loading="commitLoading" type="primary" @click="handleCommit">提交</el-button>
      </div>
    </template>

    <el-form :inline="true" :model="commitForm" label-width="100px">
      <el-form-item label="预览任务号">
        <el-input v-model="commitForm.taskNo" class="!w-360px" placeholder="预览后自动填充，可手工改" />
      </el-form-item>
      <el-form-item label="幂等键">
        <el-input v-model="commitForm.idempotencyKey" class="!w-300px" placeholder="例如 SPU30001-V12-R1" />
      </el-form-item>
    </el-form>

    <el-descriptions v-if="commitResult" :column="4" border>
      <el-descriptions-item label="提交任务号">{{ commitResult.taskNo }}</el-descriptions-item>
      <el-descriptions-item label="状态码">{{ commitResult.status }}</el-descriptions-item>
      <el-descriptions-item label="accepted">
        <el-tag :type="commitResult.accepted ? 'success' : 'danger'">{{ commitResult.accepted }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="命中幂等">
        <el-tag :type="commitResult.idempotentHit ? 'warning' : 'success'">{{ commitResult.idempotentHit }}</el-tag>
      </el-descriptions-item>
    </el-descriptions>
  </ContentWrap>
</template>

<script lang="ts" setup>
import * as TemplateApi from '@/api/mall/product/template'

defineOptions({ name: 'MallProductTemplateIndex' })

const message = useMessage()

const validateLoading = ref(false)
const previewLoading = ref(false)
const commitLoading = ref(false)

const baseForm = reactive({
  categoryId: undefined as number | undefined,
  templateVersionId: undefined as number | undefined,
  spuId: undefined as number | undefined
})

const validateItems = ref<TemplateApi.TemplateValidateItem[]>([
  {
    attributeId: 1,
    attrRole: 2,
    required: true,
    affectsPrice: true,
    affectsStock: false
  }
])

const previewForm = reactive({
  baseSku: {
    price: 9800,
    marketPrice: 12800,
    costPrice: 5200,
    stock: 100
  }
})

const specSelections = ref<Array<{ attributeId: number | undefined; optionIdsText: string }>>([
  { attributeId: 1, optionIdsText: '11,12' },
  { attributeId: 2, optionIdsText: '21,22' }
])

const commitForm = reactive({
  taskNo: '',
  idempotencyKey: ''
})

const validateResult = ref<TemplateApi.ProductCategoryTemplateValidateResp>()
const previewResult = ref<TemplateApi.ProductSkuGeneratePreviewResp>()
const commitResult = ref<TemplateApi.ProductSkuGenerateCommitResp>()

const appendValidateItem = () => {
  validateItems.value.push({
    attributeId: 0,
    attrRole: 2,
    required: true,
    affectsPrice: false,
    affectsStock: false
  })
}

const removeValidateItem = (index: number) => {
  validateItems.value.splice(index, 1)
}

const appendSpecSelection = () => {
  specSelections.value.push({ attributeId: undefined, optionIdsText: '' })
}

const removeSpecSelection = (index: number) => {
  specSelections.value.splice(index, 1)
}

const parseOptionIds = (text: string): number[] => {
  return text
    .split(',')
    .map((s) => Number(s.trim()))
    .filter((n) => Number.isFinite(n) && n > 0)
}

const buildPreviewSelections = () => {
  return specSelections.value
    .filter((row) => row.attributeId && row.optionIdsText)
    .map((row) => ({
      attributeId: row.attributeId as number,
      optionIds: parseOptionIds(row.optionIdsText)
    }))
    .filter((row) => row.optionIds.length > 0)
}

const ensureBaseRequired = () => {
  if (!baseForm.categoryId) {
    message.warning('请先填写类目ID')
    return false
  }
  if (!baseForm.spuId) {
    message.warning('请先填写SPU ID')
    return false
  }
  return true
}

const handleValidate = async () => {
  if (!baseForm.categoryId) {
    message.warning('请先填写类目ID')
    return
  }
  if (!validateItems.value.length) {
    message.warning('请至少保留一个模板项')
    return
  }
  validateLoading.value = true
  try {
    validateResult.value = await TemplateApi.validateCategoryTemplate({
      categoryId: baseForm.categoryId,
      templateVersionId: baseForm.templateVersionId,
      items: validateItems.value
    })
    message.success(validateResult.value.pass ? '模板校验通过' : '模板校验未通过')
  } finally {
    validateLoading.value = false
  }
}

const handlePreview = async () => {
  if (!ensureBaseRequired()) {
    return
  }
  const selections = buildPreviewSelections()
  if (!selections.length) {
    message.warning('请填写至少一个规格维度')
    return
  }
  previewLoading.value = true
  try {
    previewResult.value = await TemplateApi.previewSkuGenerate({
      spuId: baseForm.spuId as number,
      categoryId: baseForm.categoryId as number,
      templateVersionId: baseForm.templateVersionId,
      baseSku: previewForm.baseSku,
      specSelections: selections
    })
    commitForm.taskNo = previewResult.value.taskNo || ''
    if (!commitForm.idempotencyKey) {
      const ver = baseForm.templateVersionId || 0
      commitForm.idempotencyKey = `SPU${baseForm.spuId}-V${ver}-${Date.now()}`
    }
    message.success('预览成功')
  } finally {
    previewLoading.value = false
  }
}

const handleCommit = async () => {
  if (!commitForm.taskNo) {
    message.warning('请先预览并生成任务号')
    return
  }
  if (!commitForm.idempotencyKey) {
    message.warning('请填写幂等键')
    return
  }
  commitLoading.value = true
  try {
    commitResult.value = await TemplateApi.commitSkuGenerate({
      taskNo: commitForm.taskNo,
      idempotencyKey: commitForm.idempotencyKey
    })
    message.success(commitResult.value.idempotentHit ? '提交命中幂等' : '提交成功')
  } finally {
    commitLoading.value = false
  }
}
</script>
