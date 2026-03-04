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
        <el-button
          v-hasPermi="['product:store-sku:update']"
          plain
          type="danger"
          @click="openManualAdjustDialog"
        >
          <Icon class="mr-5px" icon="ep:operation" />
          人工库存调整
        </el-button>
        <el-button
          v-hasPermi="['product:store-sku:query']"
          plain
          type="info"
          @click="openStockFlowDialog"
        >
          <Icon class="mr-5px" icon="ep:list" />
          库存流水台账
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
          :remote-method="handleSpuSearch"
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
          :remote-method="handleBatchSaveSpuSearch"
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
          :remote-method="handleBatchAdjustSpuSearch"
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

  <el-dialog v-model="manualAdjustVisible" title="门店人工库存调整" width="760px">
    <el-form ref="manualAdjustFormRef" :model="manualAdjustForm" :rules="manualAdjustRules" label-width="120px">
      <el-form-item label="门店ID" prop="storeId">
        <el-select
          v-model="manualAdjustForm.storeId"
          class="!w-560px"
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
      <el-form-item label="业务类型" prop="bizType">
        <el-select v-model="manualAdjustForm.bizType" class="!w-560px" placeholder="请选择业务类型">
          <el-option
            v-for="item in manualBizTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="业务单号" prop="bizNo">
        <el-input
          v-model="manualAdjustForm.bizNo"
          class="!w-560px"
          maxlength="64"
          placeholder="如 SUPPLY-20260304-001"
        />
      </el-form-item>
      <el-form-item label="商品类型">
        <el-radio-group v-model="manualAdjustProductType" @change="handleManualAdjustProductTypeChange">
          <el-radio :value="2">服务</el-radio>
          <el-radio :value="1">实物</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="SPU" prop="spuId">
        <el-select
          v-model="manualAdjustForm.spuId"
          class="!w-560px"
          clearable
          filterable
          remote
          reserve-keyword
          placeholder="请输入商品名或ID检索"
          :loading="spuOptionLoading"
          :remote-method="handleManualAdjustSpuSearch"
          @change="handleManualAdjustSpuChange"
        >
          <el-option v-for="item in spuOptions" :key="item.id" :label="formatSpuOptionLabel(item)" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="SKU" prop="skuId">
        <el-select
          v-model="manualAdjustForm.skuId"
          class="!w-560px"
          clearable
          filterable
          placeholder="请选择SKU"
          :loading="skuOptionLoading"
        >
          <el-option v-for="item in skuOptions" :key="item.id" :label="formatSkuOptionLabel(item)" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="库存变化值" prop="incrCount">
        <el-input-number v-model="manualAdjustForm.incrCount" class="!w-560px" controls-position="right" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="manualAdjustForm.remark" maxlength="255" show-word-limit type="textarea" />
      </el-form-item>
      <div class="text-12px text-[var(--el-text-color-secondary)] mb-12px">
        说明：补货/调入请填正数；调出/损耗/报废请填负数；盘点可正可负。
      </div>
    </el-form>
    <template #footer>
      <el-button @click="manualAdjustVisible = false">取消</el-button>
      <el-button :loading="manualAdjustLoading" type="primary" @click="submitManualAdjustForm">确认调整</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="stockFlowVisible" title="库存流水台账" width="1280px">
    <el-form :inline="true" :model="stockFlowQueryParams" class="-mb-15px" label-width="86px">
      <el-form-item label="门店ID" prop="storeId">
        <el-input
          v-model="stockFlowQueryParams.storeId"
          class="!w-170px"
          clearable
          placeholder="请输入门店ID"
          @keyup.enter="handleStockFlowQuery"
        />
      </el-form-item>
      <el-form-item label="SKUID" prop="skuId">
        <el-input
          v-model="stockFlowQueryParams.skuId"
          class="!w-170px"
          clearable
          placeholder="请输入SKUID"
          @keyup.enter="handleStockFlowQuery"
        />
      </el-form-item>
      <el-form-item label="业务类型" prop="bizType">
        <el-select v-model="stockFlowQueryParams.bizType" class="!w-210px" clearable placeholder="请选择业务类型">
          <el-option v-for="item in stockFlowBizTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="业务单号" prop="bizNo">
        <el-input
          v-model="stockFlowQueryParams.bizNo"
          class="!w-220px"
          clearable
          placeholder="请输入业务单号"
          @keyup.enter="handleStockFlowQuery"
        />
      </el-form-item>
      <el-form-item label="流水状态" prop="status">
        <el-select v-model="stockFlowQueryParams.status" class="!w-150px" clearable placeholder="请选择状态">
          <el-option :value="0" label="待执行" />
          <el-option :value="1" label="成功" />
          <el-option :value="2" label="失败" />
          <el-option :value="3" label="执行中" />
        </el-select>
      </el-form-item>
      <el-form-item label="执行时间" prop="executeTime">
        <el-date-picker
          v-model="stockFlowQueryParams.executeTime"
          class="!w-320px"
          end-placeholder="结束时间"
          range-separator="至"
          start-placeholder="开始时间"
          type="datetimerange"
          value-format="YYYY-MM-DD HH:mm:ss"
        />
      </el-form-item>
      <el-form-item>
        <el-button :loading="stockFlowLoading" @click="handleStockFlowQuery">
          <Icon class="mr-5px" icon="ep:search" />
          搜索
        </el-button>
        <el-button @click="resetStockFlowQuery">
          <Icon class="mr-5px" icon="ep:refresh" />
          重置
        </el-button>
        <el-button @click="selectFailedStockFlowRows">
          <Icon class="mr-5px" icon="ep:select" />
          一键勾选失败项
        </el-button>
        <el-button :loading="stockFlowRetryLoading" type="warning" @click="submitStockFlowBatchRetry">
          <Icon class="mr-5px" icon="ep:refresh-right" />
          批量重试失败流水
        </el-button>
      </el-form-item>
    </el-form>

    <el-table
      ref="stockFlowTableRef"
      v-loading="stockFlowLoading"
      :data="stockFlowList"
      class="mt-12px"
      @selection-change="handleStockFlowSelectionChange"
    >
      <el-table-column type="selection" width="45" />
      <el-table-column label="ID" prop="id" width="90" />
      <el-table-column label="门店" min-width="190">
        <template #default="{ row }">
          <div>{{ row.storeName || '-' }}</div>
          <div class="text-12px text-[var(--el-text-color-secondary)]">ID: {{ row.storeId }}</div>
        </template>
      </el-table-column>
      <el-table-column label="SKUID" prop="skuId" width="110" />
      <el-table-column label="业务类型" min-width="180">
        <template #default="{ row }">
          {{ formatStockFlowBizType(row.bizType) }}
        </template>
      </el-table-column>
      <el-table-column label="业务单号" prop="bizNo" min-width="220" show-overflow-tooltip />
      <el-table-column label="变化值" prop="incrCount" width="100">
        <template #default="{ row }">
          <span :class="row.incrCount > 0 ? 'text-[var(--el-color-success)]' : 'text-[var(--el-color-danger)]'">
            {{ row.incrCount }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="stockFlowStatusTag(row.status)">
            {{ stockFlowStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="重试次数" prop="retryCount" width="100" />
      <el-table-column label="最近重试人" prop="lastRetryOperator" min-width="130" show-overflow-tooltip />
      <el-table-column label="最近来源" prop="lastRetrySource" width="120" />
      <el-table-column label="错误信息" prop="lastErrorMsg" min-width="220" show-overflow-tooltip />
      <el-table-column :formatter="dateFormatter" label="执行时间" prop="executeTime" width="180" />
      <el-table-column :formatter="dateFormatter" label="创建时间" prop="createTime" width="180" />
    </el-table>
    <Pagination
      v-model:limit="stockFlowQueryParams.pageSize"
      v-model:page="stockFlowQueryParams.pageNo"
      :total="stockFlowTotal"
      @pagination="getStockFlowList"
    />
    <template #footer>
      <el-button @click="stockFlowVisible = false">关闭</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="stockFlowRetryResultVisible" title="库存流水重试结果" width="980px">
    <el-descriptions :column="4" border class="mb-12px">
      <el-descriptions-item label="总处理数">{{ stockFlowRetryResult.totalCount || 0 }}</el-descriptions-item>
      <el-descriptions-item label="成功">{{ stockFlowRetryResult.successCount || 0 }}</el-descriptions-item>
      <el-descriptions-item label="跳过">{{ stockFlowRetryResult.skippedCount || 0 }}</el-descriptions-item>
      <el-descriptions-item label="失败">{{ stockFlowRetryResult.failedCount || 0 }}</el-descriptions-item>
    </el-descriptions>
    <el-table :data="stockFlowRetryResult.items || []" max-height="460">
      <el-table-column label="流水ID" prop="id" width="100" />
      <el-table-column label="结果" min-width="120">
        <template #default="{ row }">
          <el-tag :type="stockFlowRetryResultTag(row.resultType)">
            {{ stockFlowRetryResultLabel(row.resultType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="原因" prop="reason" min-width="220" show-overflow-tooltip />
      <el-table-column label="处理后状态" min-width="130">
        <template #default="{ row }">
          {{ stockFlowStatusLabel(row.status) }}
        </template>
      </el-table-column>
      <el-table-column label="操作人" prop="retryOperator" min-width="120" show-overflow-tooltip />
      <el-table-column label="来源" prop="retrySource" width="120" />
    </el-table>
    <template #footer>
      <el-button type="primary" @click="stockFlowRetryResultVisible = false">我知道了</el-button>
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
const manualAdjustVisible = ref(false)
const stockFlowVisible = ref(false)
const stockFlowRetryResultVisible = ref(false)
const batchSaveLoading = ref(false)
const batchAdjustLoading = ref(false)
const manualAdjustLoading = ref(false)
const stockFlowLoading = ref(false)
const stockFlowRetryLoading = ref(false)
const total = ref(0)
const stockFlowTotal = ref(0)
const list = ref<StoreSkuApi.ProductStoreSku[]>([])
const stockFlowList = ref<StoreSkuApi.ProductStoreSkuStockFlow[]>([])
const stockFlowSelectedIds = ref<number[]>([])
const stockFlowTableRef = ref()
const stockFlowRetryResult = ref<StoreSkuApi.ProductStoreSkuStockFlowBatchRetryResp>({
  totalCount: 0,
  successCount: 0,
  skippedCount: 0,
  failedCount: 0,
  items: []
})
const formRef = ref()
const batchSaveFormRef = ref()
const batchAdjustFormRef = ref()
const manualAdjustFormRef = ref()
const spuOptionLoading = ref(false)
const skuOptionLoading = ref(false)
const storeOptionLoading = ref(false)
const formProductType = ref<number>(2)
const batchSaveProductType = ref<number>(2)
const batchAdjustProductType = ref<number>(2)
const manualAdjustProductType = ref<number>(1)

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

const manualAdjustForm = ref({
  storeId: undefined as number | string | undefined,
  bizType: 'REPLENISH_IN',
  bizNo: '',
  remark: '',
  spuId: undefined as number | undefined,
  skuId: undefined as number | undefined,
  incrCount: undefined as number | undefined
})

const stockFlowQueryParams = ref<StoreSkuApi.ProductStoreSkuStockFlowPageReq>({
  pageNo: 1,
  pageSize: 10,
  storeId: undefined,
  skuId: undefined,
  bizType: undefined,
  bizNo: undefined,
  status: undefined,
  executeTime: undefined
})

const manualBizTypeOptions = [
  { label: '补货入库（仅正数）', value: 'REPLENISH_IN' },
  { label: '调拨入库（仅正数）', value: 'TRANSFER_IN' },
  { label: '调拨出库（仅负数）', value: 'TRANSFER_OUT' },
  { label: '盘点修正（可正可负）', value: 'STOCKTAKE' },
  { label: '损耗出库（仅负数）', value: 'LOSS' },
  { label: '报废出库（仅负数）', value: 'SCRAP' }
]

const stockFlowBizTypeOptions = [
  { label: '交易下单预占', value: 'TRADE_ORDER_RESERVE' },
  { label: '交易取消释放', value: 'TRADE_ORDER_RELEASE' },
  { label: '交易支付扣减', value: 'TRADE_ORDER_DEDUCT' },
  { label: '人工补货入库', value: 'MANUAL_REPLENISH_IN' },
  { label: '人工调拨入库', value: 'MANUAL_TRANSFER_IN' },
  { label: '人工调拨出库', value: 'MANUAL_TRANSFER_OUT' },
  { label: '人工盘点修正', value: 'MANUAL_STOCKTAKE' },
  { label: '人工损耗出库', value: 'MANUAL_LOSS' },
  { label: '人工报废出库', value: 'MANUAL_SCRAP' }
]

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

const manualAdjustRules = {
  storeId: [{ required: true, message: '门店ID不能为空', trigger: 'change' }],
  bizType: [{ required: true, message: '业务类型不能为空', trigger: 'change' }],
  bizNo: [{ required: true, message: '业务单号不能为空', trigger: 'blur' }],
  spuId: [{ required: true, message: 'SPU不能为空', trigger: 'change' }],
  skuId: [{ required: true, message: 'SKU不能为空', trigger: 'change' }],
  incrCount: [
    { required: true, message: '库存变化值不能为空', trigger: 'blur' },
    {
      validator: (_rule: any, value: number, callback: (err?: Error) => void) => {
        if (value === 0) {
          callback(new Error('库存变化值不能为 0'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
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

const handleManualAdjustSpuSearch = (keyword: string) => {
  loadSpuOptions(keyword, manualAdjustProductType.value, manualAdjustForm.value.spuId)
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

const handleManualAdjustProductTypeChange = async () => {
  manualAdjustForm.value.skuId = undefined
  manualAdjustForm.value.spuId = undefined
  skuOptions.value = []
  await loadSpuOptions('', manualAdjustProductType.value, manualAdjustForm.value.spuId)
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

const openManualAdjustDialog = async () => {
  manualAdjustProductType.value = 1
  manualAdjustForm.value = {
    storeId: queryParams.value.storeId,
    bizType: 'REPLENISH_IN',
    bizNo: '',
    remark: '',
    spuId: undefined,
    skuId: undefined,
    incrCount: undefined
  }
  spuOptions.value = []
  skuOptions.value = []
  await loadStoreOptions()
  await loadSpuOptions('', manualAdjustProductType.value, manualAdjustForm.value.spuId)
  manualAdjustVisible.value = true
}

const openStockFlowDialog = async () => {
  stockFlowVisible.value = true
  stockFlowQueryParams.value.pageNo = 1
  stockFlowQueryParams.value.storeId = normalizeNumeric(queryParams.value.storeId)
  await getStockFlowList()
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

const handleManualAdjustSpuChange = async () => {
  manualAdjustForm.value.skuId = undefined
  await loadSkuOptions(manualAdjustForm.value.spuId)
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

const normalizeManualBizType = (bizType?: string) => {
  if (!bizType) {
    return undefined
  }
  return bizType.trim().toUpperCase()
}

const manualBizTypeDirectionMap: Record<string, number> = {
  REPLENISH_IN: 1,
  TRANSFER_IN: 1,
  TRANSFER_OUT: -1,
  STOCKTAKE: 0,
  LOSS: -1,
  SCRAP: -1
}

const submitManualAdjustForm = async () => {
  await manualAdjustFormRef.value.validate()
  const bizType = normalizeManualBizType(manualAdjustForm.value.bizType)
  const bizNo = String(manualAdjustForm.value.bizNo || '').trim()
  const storeId = normalizeNumeric(manualAdjustForm.value.storeId)
  const skuId = normalizeNumeric(manualAdjustForm.value.skuId)
  const incrCount = normalizeNumeric(manualAdjustForm.value.incrCount)
  const direction = bizType ? manualBizTypeDirectionMap[bizType] : undefined
  if (!bizType) {
    message.error('业务类型不能为空')
    return
  }
  if (!bizNo) {
    message.error('业务单号不能为空')
    return
  }
  if (!storeId) {
    message.error('门店ID不能为空')
    return
  }
  if (!skuId) {
    message.error('SKU 不能为空')
    return
  }
  if (!incrCount || incrCount === 0) {
    message.error('库存变化值不能为 0')
    return
  }
  if (direction === 1 && incrCount < 0) {
    message.error('当前业务类型仅允许正数库存变化值')
    return
  }
  if (direction === -1 && incrCount > 0) {
    message.error('当前业务类型仅允许负数库存变化值')
    return
  }
  manualAdjustLoading.value = true
  try {
    const payload: StoreSkuApi.ProductStoreSkuManualStockAdjust = {
      storeId,
      bizType,
      bizNo,
      remark: manualAdjustForm.value.remark?.trim() || undefined,
      items: [
        {
          skuId,
          incrCount: incrCount!
        }
      ]
    }
    const affected = await StoreSkuApi.manualAdjustStoreSkuStock(payload)
    message.success(`人工库存调整成功，影响 ${affected} 条 SKU`)
    manualAdjustVisible.value = false
    await getList()
    if (stockFlowVisible.value) {
      await getStockFlowList()
    }
  } finally {
    manualAdjustLoading.value = false
  }
}

const getStockFlowList = async () => {
  stockFlowLoading.value = true
  try {
    const params: StoreSkuApi.ProductStoreSkuStockFlowPageReq = {
      pageNo: stockFlowQueryParams.value.pageNo,
      pageSize: stockFlowQueryParams.value.pageSize,
      storeId: normalizeNumeric(stockFlowQueryParams.value.storeId),
      skuId: normalizeNumeric(stockFlowQueryParams.value.skuId),
      bizType: stockFlowQueryParams.value.bizType || undefined,
      bizNo: stockFlowQueryParams.value.bizNo?.trim() || undefined,
      status: normalizeNumeric(stockFlowQueryParams.value.status),
      executeTime:
        stockFlowQueryParams.value.executeTime && stockFlowQueryParams.value.executeTime.length === 2
          ? stockFlowQueryParams.value.executeTime
          : undefined
    }
    const data = await StoreSkuApi.getStoreSkuStockFlowPage(params)
    stockFlowList.value = data.list || []
    stockFlowTotal.value = data.total || 0
    stockFlowSelectedIds.value = []
    if (stockFlowTableRef.value) {
      stockFlowTableRef.value.clearSelection()
    }
  } finally {
    stockFlowLoading.value = false
  }
}

const handleStockFlowQuery = () => {
  stockFlowQueryParams.value.pageNo = 1
  getStockFlowList()
}

const resetStockFlowQuery = () => {
  stockFlowQueryParams.value = {
    pageNo: 1,
    pageSize: 10,
    storeId: undefined,
    skuId: undefined,
    bizType: undefined,
    bizNo: undefined,
    status: undefined,
    executeTime: undefined
  }
  getStockFlowList()
}

const handleStockFlowSelectionChange = (rows: StoreSkuApi.ProductStoreSkuStockFlow[]) => {
  stockFlowSelectedIds.value = rows
    .map((item) => Number(item.id))
    .filter((id) => Number.isInteger(id) && id > 0)
}

const selectFailedStockFlowRows = () => {
  if (!stockFlowTableRef.value) {
    return
  }
  stockFlowTableRef.value.clearSelection()
  const failedRows = stockFlowList.value.filter((item) => item.status === 2)
  failedRows.forEach((row) => stockFlowTableRef.value.toggleRowSelection(row, true))
  if (!failedRows.length) {
    message.warning('当前页没有失败状态流水')
    return
  }
  message.success(`已勾选 ${failedRows.length} 条失败流水`)
}

const submitStockFlowBatchRetry = async () => {
  if (!stockFlowSelectedIds.value.length) {
    message.warning('请先勾选至少一条库存流水')
    return
  }
  try {
    await message.confirm(`确认重试 ${stockFlowSelectedIds.value.length} 条库存流水吗？`)
  } catch {
    return
  }
  stockFlowRetryLoading.value = true
  try {
    const result = await StoreSkuApi.batchRetryStoreSkuStockFlow({
      ids: stockFlowSelectedIds.value,
      source: 'ADMIN_UI'
    })
    stockFlowRetryResult.value = {
      totalCount: result.totalCount || 0,
      successCount: result.successCount || 0,
      skippedCount: result.skippedCount || 0,
      failedCount: result.failedCount || 0,
      items: result.items || []
    }
    stockFlowRetryResultVisible.value = true
    message.success(
      `批量重试完成：成功${stockFlowRetryResult.value.successCount}，跳过${stockFlowRetryResult.value.skippedCount}，失败${stockFlowRetryResult.value.failedCount}`
    )
    await getStockFlowList()
  } finally {
    stockFlowRetryLoading.value = false
  }
}

const stockFlowRetryResultLabel = (resultType?: string) => {
  if (resultType === 'SUCCESS') {
    return '成功'
  }
  if (resultType === 'SKIPPED') {
    return '跳过'
  }
  if (resultType === 'FAILED') {
    return '失败'
  }
  return resultType || '未知'
}

const stockFlowRetryResultTag = (resultType?: string) => {
  if (resultType === 'SUCCESS') {
    return 'success'
  }
  if (resultType === 'SKIPPED') {
    return 'info'
  }
  if (resultType === 'FAILED') {
    return 'danger'
  }
  return 'info'
}

const formatStockFlowBizType = (bizType?: string) => {
  if (!bizType) {
    return '-'
  }
  const matched = stockFlowBizTypeOptions.find((item) => item.value === bizType)
  return matched ? matched.label : bizType
}

const stockFlowStatusLabel = (status?: number) => {
  if (status === 0) {
    return '待执行'
  }
  if (status === 1) {
    return '执行成功'
  }
  if (status === 2) {
    return '执行失败'
  }
  if (status === 3) {
    return '执行中'
  }
  return '未知'
}

const stockFlowStatusTag = (status?: number) => {
  if (status === 0) {
    return 'warning'
  }
  if (status === 1) {
    return 'success'
  }
  if (status === 2) {
    return 'danger'
  }
  if (status === 3) {
    return 'info'
  }
  return 'info'
}

onMounted(() => {
  loadStoreOptions()
  getList()
})
</script>
