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
      <el-form-item label="工单关联" prop="relatedTicketLinked">
        <el-select v-model="relatedTicketLinked" class="!w-140px" clearable placeholder="全部">
          <el-option :value="true" label="已关联" />
          <el-option :value="false" label="未关联" />
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
    <el-alert
      v-if="summaryFallback"
      :closable="false"
      :description="summaryFallbackReason || 'summary 接口不可用，当前展示列表近似统计。'"
      title="统计已降级"
      type="warning"
      class="mb-12px"
    />
    <div class="mb-10px flex items-center gap-8px">
      <span class="text-13px text-[var(--el-text-color-secondary)]">四账汇总</span>
      <el-tag v-if="summaryData.ticketSummaryDegraded" type="warning">ticket summary degraded</el-tag>
    </div>
    <el-row v-loading="summaryLoading" :gutter="12">
      <el-col :lg="8" :md="12" :sm="12" :xs="24">
        <el-card shadow="never">
          <div class="text-12px text-[var(--el-text-color-secondary)]">总笔数</div>
          <div class="mt-8px text-26px font-600">{{ countOrDash(summaryData.totalCount) }}</div>
        </el-card>
      </el-col>
      <el-col :lg="8" :md="12" :sm="12" :xs="24">
        <el-card shadow="never">
          <div class="text-12px text-[var(--el-text-color-secondary)]">通过数（PASS）</div>
          <div class="mt-8px text-26px font-600">{{ countOrDash(summaryData.passCount) }}</div>
        </el-card>
      </el-col>
      <el-col :lg="8" :md="12" :sm="12" :xs="24">
        <el-card shadow="never">
          <div class="text-12px text-[var(--el-text-color-secondary)]">告警数（WARN）</div>
          <div class="mt-8px text-26px font-600">{{ countOrDash(summaryData.warnCount) }}</div>
        </el-card>
      </el-col>
      <el-col :lg="8" :md="12" :sm="12" :xs="24">
        <el-card shadow="never">
          <div class="text-12px text-[var(--el-text-color-secondary)]">tradeMinusFulfillmentSum(元)</div>
          <div class="mt-8px text-26px font-600">{{ fenToYuanOrDash(summaryData.tradeMinusFulfillmentSum) }}</div>
        </el-card>
      </el-col>
      <el-col :lg="8" :md="12" :sm="12" :xs="24">
        <el-card shadow="never">
          <div class="text-12px text-[var(--el-text-color-secondary)]">tradeMinusCommissionSplitSum(元)</div>
          <div class="mt-8px text-26px font-600">{{ fenToYuanOrDash(summaryData.tradeMinusCommissionSplitSum) }}</div>
        </el-card>
      </el-col>
      <el-col :lg="8" :md="12" :sm="12" :xs="24">
        <el-card shadow="never">
          <div class="text-12px text-[var(--el-text-color-secondary)]">未收口工单数</div>
          <div class="mt-8px text-26px font-600">{{ countOrDash(summaryData.unresolvedTicketCount) }}</div>
        </el-card>
      </el-col>
    </el-row>
  </ContentWrap>

  <ContentWrap>
    <div class="mb-10px flex items-center gap-8px">
      <span class="text-13px text-[var(--el-text-color-secondary)]">退款佣金审计</span>
      <el-tag v-if="auditSummaryFallback" type="warning">汇总降级</el-tag>
    </div>
    <el-form :inline="true" :model="auditQueryParams" class="-mb-15px" label-width="96px">
      <el-form-item label="业务日期" prop="bizDateRange">
        <el-date-picker
          v-model="auditBizDateRange"
          class="!w-260px"
          end-placeholder="结束日期"
          range-separator="至"
          start-placeholder="开始日期"
          type="daterange"
          value-format="YYYY-MM-DD"
        />
      </el-form-item>
      <el-form-item label="退款时间" prop="refundTimeRange">
        <el-date-picker
          v-model="auditRefundTimeRange"
          class="!w-340px"
          end-placeholder="结束时间"
          range-separator="至"
          start-placeholder="开始时间"
          type="datetimerange"
          value-format="YYYY-MM-DD HH:mm:ss"
        />
      </el-form-item>
      <el-form-item label="审计状态" prop="refundAuditStatus">
        <el-select
          v-model="auditQueryParams.refundAuditStatus"
          class="!w-180px"
          clearable
          filterable
          placeholder="请选择状态"
        >
          <el-option
            v-for="item in refundAuditStatusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="异常类型" prop="refundExceptionType">
        <el-select
          v-model="auditQueryParams.refundExceptionType"
          class="!w-240px"
          clearable
          placeholder="请选择异常类型"
        >
          <el-option
            v-for="item in refundCommissionMismatchTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="上限来源" prop="refundLimitSource">
        <el-select
          v-model="auditQueryParams.refundLimitSource"
          class="!w-180px"
          clearable
          placeholder="请选择上限来源"
        >
          <el-option
            v-for="item in refundLimitSourceOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="订单号关键词" prop="keyword">
        <el-input
          v-model="auditQueryParams.keyword"
          class="!w-240px"
          clearable
          placeholder="请输入关键词"
          @keyup.enter="handleAuditQuery"
        />
      </el-form-item>
      <el-form-item label="订单ID" prop="orderId">
        <el-input-number v-model="auditQueryParams.orderId" :controls="false" :min="1" class="!w-180px" />
      </el-form-item>
      <el-form-item label="退款单ID" prop="payRefundId">
        <el-input-number v-model="auditQueryParams.payRefundId" :controls="false" :min="1" class="!w-180px" />
      </el-form-item>
      <el-form-item label="同步条数上限">
        <el-input-number
          v-model="auditSyncLimit"
          :controls="false"
          :max="1000"
          :min="1"
          class="!w-180px"
          placeholder="1~1000"
        />
      </el-form-item>
      <el-form-item>
        <el-button :loading="auditLoading" @click="handleAuditQuery">
          <Icon class="mr-5px" icon="ep:search" />
          查询巡检
        </el-button>
        <el-button @click="resetAuditQuery">
          <Icon class="mr-5px" icon="ep:refresh" />
          重置
        </el-button>
        <el-button
          v-hasPermi="['booking:commission:settlement']"
          :loading="auditSyncLoading"
          plain
          type="primary"
          @click="handleSyncAuditTickets"
        >
          <Icon class="mr-5px" icon="ep:upload" />
          同步工单
        </el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="12" class="mb-12px">
      <el-col :lg="8" :md="12" :sm="12" :xs="24">
        <el-card shadow="never">
          <div class="text-12px text-[var(--el-text-color-secondary)]">总数</div>
          <div class="mt-8px text-26px font-600">{{ countOrDash(auditSummaryData.totalCount) }}</div>
        </el-card>
      </el-col>
      <el-col :lg="8" :md="12" :sm="12" :xs="24">
        <el-card shadow="never">
          <div class="text-12px text-[var(--el-text-color-secondary)]">差异金额(元)</div>
          <div class="mt-8px text-26px font-600">{{ fenToYuanOrDash(auditSummaryData.diffAmount) }}</div>
        </el-card>
      </el-col>
      <el-col :lg="8" :md="12" :sm="12" :xs="24">
        <el-card shadow="never">
          <div class="text-12px text-[var(--el-text-color-secondary)]">未收口工单</div>
          <div class="mt-8px text-26px font-600">{{ countOrDash(auditSummaryData.unresolvedTicketCount) }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-table v-loading="auditLoading" :data="auditList">
      <el-table-column label="订单ID" min-width="120">
        <template #default="{ row }">
          {{ numberOrDash(row.orderId) }}
        </template>
      </el-table-column>
      <el-table-column label="订单号" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">
          {{ textOrDash(row.tradeOrderNo) }}
        </template>
      </el-table-column>
      <el-table-column label="退款金额(元)" min-width="130">
        <template #default="{ row }">
          <el-tooltip :disabled="!isValidNumber(row.refundPrice)" :content="`分值：${numberOrDash(row.refundPrice)}`" placement="top">
            <span>{{ fenToYuanOrDash(row.refundPrice) }}</span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="已结算提成(元)" min-width="140">
        <template #default="{ row }">
          <el-tooltip
            :disabled="!isValidNumber(row.settledCommissionAmount)"
            :content="`分值：${numberOrDash(row.settledCommissionAmount)}`"
            placement="top"
          >
            <span>{{ fenToYuanOrDash(row.settledCommissionAmount) }}</span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="冲正金额(元)" min-width="130">
        <template #default="{ row }">
          <el-tooltip
            :disabled="!isValidNumber(row.reversalCommissionAmountAbs)"
            :content="`分值：${numberOrDash(row.reversalCommissionAmountAbs)}`"
            placement="top"
          >
            <span>{{ fenToYuanOrDash(row.reversalCommissionAmountAbs) }}</span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="期望冲正(元)" min-width="130">
        <template #default="{ row }">
          <el-tooltip
            :disabled="!isValidNumber(row.expectedReversalAmount)"
            :content="`分值：${numberOrDash(row.expectedReversalAmount)}`"
            placement="top"
          >
            <span>{{ fenToYuanOrDash(row.expectedReversalAmount) }}</span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="退款单ID" min-width="120">
        <template #default="{ row }">
          {{ numberOrDash(row.payRefundId) }}
        </template>
      </el-table-column>
      <el-table-column label="退款时间" min-width="180">
        <template #default="{ row }">
          {{ textOrDash(row.refundTime) }}
        </template>
      </el-table-column>
      <el-table-column label="审计状态" min-width="120">
        <template #default="{ row }">
          <el-tag :type="refundAuditStatusTagType(row.refundAuditStatus)">
            {{ refundAuditStatusText(row.refundAuditStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="异常类型" min-width="170">
        <template #default="{ row }">
          {{ mismatchTypeText(row.refundExceptionType || row.mismatchType) }}
        </template>
      </el-table-column>
      <el-table-column label="退款上限来源" min-width="140">
        <template #default="{ row }">
          {{ refundLimitSourceText(row.refundLimitSource) }}
        </template>
      </el-table-column>
      <el-table-column label="异常原因" min-width="260" show-overflow-tooltip>
        <template #default="{ row }">
          {{ textOrDash(row.mismatchReason) }}
        </template>
      </el-table-column>
      <el-table-column label="审计备注" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">
          {{ textOrDash(row.refundAuditRemark) }}
        </template>
      </el-table-column>
      <el-table-column label="支付时间" min-width="180">
        <template #default="{ row }">
          {{ textOrDash(row.payTime) }}
        </template>
      </el-table-column>
      <el-table-column align="center" fixed="right" label="操作" width="120">
        <template #default="{ row }">
          <el-button link type="primary" @click="openAuditDetailDrawer(row)">查看证据</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      v-model:limit="auditQueryParams.pageSize"
      v-model:page="auditQueryParams.pageNo"
      :total="auditTotal"
      @pagination="getAuditList"
    />
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

  <Dialog v-model="auditSyncResultVisible" title="退款-提成巡检同步结果" width="700px">
    <el-descriptions :column="2" border>
      <el-descriptions-item label="命中异常总数">
        {{ countOrDash(auditSyncResult.totalMismatchCount) }}
      </el-descriptions-item>
      <el-descriptions-item label="本次尝试同步">
        {{ countOrDash(auditSyncResult.attemptedCount) }}
      </el-descriptions-item>
      <el-descriptions-item label="同步成功数">
        {{ countOrDash(auditSyncResult.successCount) }}
      </el-descriptions-item>
      <el-descriptions-item label="同步失败数">
        {{ countOrDash(auditSyncResult.failedCount) }}
      </el-descriptions-item>
    </el-descriptions>

    <el-collapse v-model="auditSyncCollapseActive" class="mt-12px">
      <el-collapse-item :title="`失败订单ID（${auditSyncFailedCountText}）`" name="failed-order-ids">
        <div class="mb-8px flex items-center justify-between">
          <span class="text-[var(--el-text-color-secondary)]">用于二次重试排查</span>
          <el-button link type="primary" @click="copyFailedOrderIds">复制失败订单ID</el-button>
        </div>
        <el-empty v-if="!auditSyncResult.failedOrderIds?.length" description="无失败订单" />
        <el-input
          v-else
          :model-value="auditSyncResult.failedOrderIds.join(',')"
          :rows="5"
          readonly
          type="textarea"
        />
      </el-collapse-item>
    </el-collapse>

    <template #footer>
      <el-button @click="auditSyncResultVisible = false">关闭</el-button>
    </template>
  </Dialog>

  <el-drawer v-model="auditDetailDrawerVisible" size="48%" title="退款佣金审计详情">
    <el-descriptions :column="2" border>
      <el-descriptions-item label="订单ID">{{ numberOrDash(auditDetailData.orderId) }}</el-descriptions-item>
      <el-descriptions-item label="订单号">{{ textOrDash(auditDetailData.tradeOrderNo) }}</el-descriptions-item>
      <el-descriptions-item label="退款单ID">{{ numberOrDash(auditDetailData.payRefundId) }}</el-descriptions-item>
      <el-descriptions-item label="退款时间">{{ textOrDash(auditDetailData.refundTime) }}</el-descriptions-item>
      <el-descriptions-item label="审计状态">
        <el-tag :type="refundAuditStatusTagType(auditDetailData.refundAuditStatus)">
          {{ refundAuditStatusText(auditDetailData.refundAuditStatus) }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="异常类型">
        {{ mismatchTypeText(auditDetailData.refundExceptionType || auditDetailData.mismatchType) }}
      </el-descriptions-item>
      <el-descriptions-item label="退款上限来源">
        {{ refundLimitSourceText(auditDetailData.refundLimitSource) }}
      </el-descriptions-item>
      <el-descriptions-item label="支付时间">{{ textOrDash(auditDetailData.payTime) }}</el-descriptions-item>
      <el-descriptions-item label="异常原因" :span="2">{{ textOrDash(auditDetailData.mismatchReason) }}</el-descriptions-item>
      <el-descriptions-item label="审计备注" :span="2">{{ textOrDash(auditDetailData.refundAuditRemark) }}</el-descriptions-item>
    </el-descriptions>

    <el-descriptions :column="2" border class="mt-16px">
      <el-descriptions-item label="退款金额(元)">{{ fenToYuanOrDash(auditDetailData.refundPrice) }}</el-descriptions-item>
      <el-descriptions-item label="已结算提成(元)">
        {{ fenToYuanOrDash(auditDetailData.settledCommissionAmount) }}
      </el-descriptions-item>
      <el-descriptions-item label="冲正金额(元)">
        {{ fenToYuanOrDash(auditDetailData.reversalCommissionAmountAbs) }}
      </el-descriptions-item>
      <el-descriptions-item label="期望冲正(元)">
        {{ fenToYuanOrDash(auditDetailData.expectedReversalAmount) }}
      </el-descriptions-item>
    </el-descriptions>

    <div class="mt-16px">
      <div class="mb-8px font-500">refundEvidenceJson（结构化）</div>
      <el-alert v-if="auditEvidenceParseError" :closable="false" title="证据解析失败（原文保留）" type="warning" />
      <el-empty v-else-if="!auditEvidenceAvailable" description="无可用证据" />
      <el-descriptions v-else :column="2" border>
        <el-descriptions-item
          v-for="(entry, index) in auditEvidenceEntries"
          :key="`${entry.key}-${index}`"
          :label="entry.key"
          :span="2"
        >
          {{ entry.value }}
        </el-descriptions-item>
      </el-descriptions>
    </div>

    <div class="mt-16px">
      <div class="mb-8px font-500">refundEvidenceJson 原文</div>
      <el-input
        :model-value="auditDetailData.refundEvidenceJson || EMPTY_TEXT"
        :rows="6"
        readonly
        type="textarea"
      />
    </div>
  </el-drawer>

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

interface AuditSummaryData {
  totalCount?: number
  diffAmount?: number
  unresolvedTicketCount?: number
}

interface AuditEvidenceEntry {
  key: string
  value: string
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
const summaryLoading = ref(false)
const summaryFallback = ref(false)
const summaryFallbackReason = ref('')
const summaryData = ref<FourAccountReconcileApi.FourAccountReconcileSummaryVO>({
  totalCount: undefined,
  passCount: undefined,
  warnCount: undefined,
  tradeMinusFulfillmentSum: undefined,
  tradeMinusCommissionSplitSum: undefined,
  unresolvedTicketCount: undefined,
  ticketSummaryDegraded: false
})
const relatedTicketLinked = ref<boolean>()
const refundCommissionMismatchTypeOptions = [
  { label: '退款未冲正', value: 'REFUND_WITHOUT_REVERSAL' },
  { label: '冲正未退款', value: 'REVERSAL_WITHOUT_REFUND' },
  { label: '冲正金额不一致', value: 'REVERSAL_AMOUNT_MISMATCH' }
]
const refundAuditStatusOptions = [
  { label: '待处理', value: 'PENDING' },
  { label: '通过', value: 'PASS' },
  { label: '告警', value: 'WARN' },
  { label: '已收口', value: 'CLOSED' }
]
const refundLimitSourceOptions = [
  { label: '子项台账优先', value: 'CHILD_LEDGER' },
  { label: '快照兜底', value: 'FALLBACK_SNAPSHOT' }
]
const auditLoading = ref(false)
const auditTotal = ref(0)
const auditList = ref<FourAccountReconcileApi.FourAccountRefundCommissionAuditVO[]>([])
const auditBizDateRange = ref<string[]>()
const auditRefundTimeRange = ref<string[]>()
const auditSyncLimit = ref(200)
const auditSyncLoading = ref(false)
const auditSyncResultVisible = ref(false)
const auditSyncCollapseActive = ref<string[]>([])
const auditSummaryFallback = ref(false)
const auditSummaryData = ref<AuditSummaryData>({
  totalCount: undefined,
  diffAmount: undefined,
  unresolvedTicketCount: undefined
})
const auditDetailDrawerVisible = ref(false)
const auditDetailData = ref<Partial<FourAccountReconcileApi.FourAccountRefundCommissionAuditVO>>({})
const auditEvidenceEntries = ref<AuditEvidenceEntry[]>([])
const auditEvidenceAvailable = ref(false)
const auditEvidenceParseError = ref(false)
const auditSyncResult = ref<FourAccountReconcileApi.FourAccountRefundCommissionAuditSyncResp>({
  totalMismatchCount: 0,
  attemptedCount: 0,
  successCount: 0,
  failedCount: 0,
  failedOrderIds: []
})
const auditQueryParams = reactive<FourAccountReconcileApi.FourAccountRefundCommissionAuditPageReq>({
  pageNo: 1,
  pageSize: 10,
  beginBizDate: undefined,
  endBizDate: undefined,
  refundAuditStatus: undefined,
  refundExceptionType: undefined,
  refundLimitSource: undefined,
  payRefundId: undefined,
  refundTimeRange: undefined,
  mismatchType: undefined,
  keyword: undefined,
  orderId: undefined
})

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

const createEmptySummaryData = (): FourAccountReconcileApi.FourAccountReconcileSummaryVO => {
  return {
    totalCount: undefined,
    passCount: undefined,
    warnCount: undefined,
    tradeMinusFulfillmentSum: undefined,
    tradeMinusCommissionSplitSum: undefined,
    unresolvedTicketCount: undefined,
    ticketSummaryDegraded: false
  }
}

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

const createEmptyAuditSyncResult = (): FourAccountReconcileApi.FourAccountRefundCommissionAuditSyncResp => {
  return {
    totalMismatchCount: 0,
    attemptedCount: 0,
    successCount: 0,
    failedCount: 0,
    failedOrderIds: []
  }
}

const createEmptyAuditSummaryData = (): AuditSummaryData => {
  return {
    totalCount: undefined,
    diffAmount: undefined,
    unresolvedTicketCount: undefined
  }
}

const resetAuditEvidenceView = () => {
  auditEvidenceEntries.value = []
  auditEvidenceAvailable.value = false
  auditEvidenceParseError.value = false
}

const isValidNumber = (value: any): value is number => {
  return typeof value === 'number' && !Number.isNaN(value)
}

const countOrDash = (value?: number) => {
  return isValidNumber(value) ? String(value) : EMPTY_TEXT
}

const textOrDash = (value: any) => {
  if (value === undefined || value === null) {
    return EMPTY_TEXT
  }
  const text = String(value).trim()
  return text ? text : EMPTY_TEXT
}

const numberOrDash = (value: any) => {
  if (value === undefined || value === null || value === '') {
    return EMPTY_TEXT
  }
  const parsed = Number(value)
  return Number.isFinite(parsed) ? String(parsed) : EMPTY_TEXT
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

const mismatchTypeText = (type?: FourAccountReconcileApi.FourAccountRefundCommissionMismatchType) => {
  if (type === 'REFUND_WITHOUT_REVERSAL') return '退款未冲正'
  if (type === 'REVERSAL_WITHOUT_REFUND') return '冲正未退款'
  if (type === 'REVERSAL_AMOUNT_MISMATCH') return '冲正金额不一致'
  return textOrDash(type)
}

const normalizeUpperText = (value: any): string | undefined => {
  const text = String(value || '').trim().toUpperCase()
  return text || undefined
}

const refundAuditStatusText = (status?: FourAccountReconcileApi.FourAccountRefundAuditStatus) => {
  const normalized = normalizeUpperText(status)
  if (normalized === 'PENDING') return '待处理'
  if (normalized === 'PASS') return '通过'
  if (normalized === 'WARN') return '告警'
  if (normalized === 'CLOSED') return '已收口'
  return textOrDash(status)
}

const refundAuditStatusTagType = (status?: FourAccountReconcileApi.FourAccountRefundAuditStatus) => {
  const normalized = normalizeUpperText(status)
  if (normalized === 'PASS') return 'success'
  if (normalized === 'WARN') return 'warning'
  if (normalized === 'PENDING') return 'info'
  if (normalized === 'CLOSED') return ''
  return 'info'
}

const refundLimitSourceText = (source?: string) => {
  const normalized = normalizeUpperText(source)
  if (normalized === 'CHILD_LEDGER') return '子项台账优先'
  if (normalized === 'FALLBACK_SNAPSHOT') return '快照兜底'
  return textOrDash(source)
}

const auditSyncFailedCountText = computed(() => {
  return String((auditSyncResult.value.failedOrderIds || []).length)
})

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

const stringifyEvidenceValue = (value: any): string => {
  if (value === undefined || value === null || value === '') {
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
    return JSON.stringify(value)
  } catch {
    return String(value)
  }
}

const parseAuditEvidenceJson = (rawJson?: string) => {
  resetAuditEvidenceView()
  const raw = String(rawJson || '').trim()
  if (!raw) {
    return
  }
  try {
    const parsed = JSON.parse(raw)
    if (Array.isArray(parsed)) {
      auditEvidenceEntries.value = parsed.map((item, index) => ({
        key: `[${index}]`,
        value: stringifyEvidenceValue(item)
      }))
      auditEvidenceAvailable.value = auditEvidenceEntries.value.length > 0
      return
    }
    if (parsed && typeof parsed === 'object') {
      auditEvidenceEntries.value = Object.entries(parsed).map(([key, value]) => ({
        key,
        value: stringifyEvidenceValue(value)
      }))
      auditEvidenceAvailable.value = auditEvidenceEntries.value.length > 0
      return
    }
    auditEvidenceEntries.value = [{ key: 'value', value: stringifyEvidenceValue(parsed) }]
    auditEvidenceAvailable.value = true
  } catch {
    auditEvidenceParseError.value = true
  }
}

const calculateFallbackAuditDiffAmount = (rows: FourAccountReconcileApi.FourAccountRefundCommissionAuditVO[]) => {
  return rows.reduce((totalAmount, row) => {
    const expected = parseNumber(row.expectedReversalAmount) || 0
    const reversal = parseNumber(row.reversalCommissionAmountAbs) || 0
    return totalAmount + Math.abs(expected - reversal)
  }, 0)
}

const calculateFallbackAuditUnresolvedTicketCount = (rows: FourAccountReconcileApi.FourAccountRefundCommissionAuditVO[]) => {
  return rows.filter((row) => {
    const status = normalizeUpperText(row.refundAuditStatus)
    if (!status) {
      return true
    }
    return status !== 'PASS' && status !== 'CLOSED'
  }).length
}

const parseAnyAuditSummaryField = (source: Record<string, any> | undefined, keys: string[]) => {
  if (!source) {
    return undefined
  }
  for (const key of keys) {
    const parsed = parseNumber(source[key])
    if (parsed !== undefined) {
      return parsed
    }
  }
  return undefined
}

const applyAuditSummary = (
  data: PageResult<FourAccountReconcileApi.FourAccountRefundCommissionAuditVO> | Record<string, any> | undefined
) => {
  const source = ((data || {}) as Record<string, any>) || {}
  const sourceSummary = source.summary && typeof source.summary === 'object'
    ? (source.summary as Record<string, any>)
    : undefined

  const totalCount = parseAnyAuditSummaryField(sourceSummary, ['totalCount'])
    ?? parseAnyAuditSummaryField(source, ['totalCount', 'totalMismatchCount', 'total'])
    ?? auditTotal.value
  const diffAmount = parseAnyAuditSummaryField(sourceSummary, ['diffAmount', 'differenceAmount', 'mismatchAmount'])
    ?? parseAnyAuditSummaryField(source, ['diffAmount', 'differenceAmount', 'mismatchAmount'])
  const unresolvedTicketCount = parseAnyAuditSummaryField(sourceSummary, ['unresolvedTicketCount', 'unresolvedCount'])
    ?? parseAnyAuditSummaryField(source, ['unresolvedTicketCount', 'unresolvedCount'])

  const hasServerSummary = diffAmount !== undefined || unresolvedTicketCount !== undefined
  auditSummaryFallback.value = !hasServerSummary
  auditSummaryData.value = {
    totalCount,
    diffAmount: diffAmount ?? calculateFallbackAuditDiffAmount(auditList.value),
    unresolvedTicketCount: unresolvedTicketCount ?? calculateFallbackAuditUnresolvedTicketCount(auditList.value)
  }
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

const buildSummaryReq = (): FourAccountReconcileApi.FourAccountReconcileSummaryReq => {
  return {
    bizDate: queryParams.bizDate,
    status: queryParams.status,
    relatedTicketLinked: relatedTicketLinked.value
  }
}

const calculateFallbackTradeMinusFulfillmentSum = (rows: FourAccountReconcileApi.FourAccountReconcileVO[]) => {
  return rows.reduce((totalAmount, row) => {
    return totalAmount + (parseNumber(row.tradeMinusFulfillment) || 0)
  }, 0)
}

const calculateFallbackTradeMinusCommissionSplitSum = (rows: FourAccountReconcileApi.FourAccountReconcileVO[]) => {
  return rows.reduce((totalAmount, row) => {
    return totalAmount + (parseNumber(row.tradeMinusCommissionSplit) || 0)
  }, 0)
}

const calculateFallbackSummary = () => {
  const passCount = list.value.filter((item) => item.status === 10).length
  const warnCount = list.value.filter((item) => item.status === 20).length
  const unresolvedTicketCount = list.value.filter((item) => item.relatedTicketId && item.relatedTicketStatus !== 20).length
  return {
    totalCount: total.value,
    passCount,
    warnCount,
    tradeMinusFulfillmentSum: calculateFallbackTradeMinusFulfillmentSum(list.value),
    tradeMinusCommissionSplitSum: calculateFallbackTradeMinusCommissionSplitSum(list.value),
    unresolvedTicketCount,
    ticketSummaryDegraded: true
  } as FourAccountReconcileApi.FourAccountReconcileSummaryVO
}

const loadSummary = async () => {
  summaryLoading.value = true
  try {
    const data = await FourAccountReconcileApi.getFourAccountReconcileSummary(buildSummaryReq())
    summaryData.value = {
      ...createEmptySummaryData(),
      ...(data || {})
    }
    summaryFallback.value = false
    summaryFallbackReason.value = ''
  } catch (error: any) {
    summaryData.value = calculateFallbackSummary()
    summaryFallback.value = true
    const msg = String(error?.msg || '').toLowerCase()
    summaryFallbackReason.value = msg.includes('404') || msg.includes('not found')
      ? 'summary 接口未就绪，当前展示列表近似统计。'
      : 'summary 查询失败，当前展示列表近似统计。'
  } finally {
    summaryLoading.value = false
  }
}

const getList = async () => {
  loading.value = true
  try {
    normalizeQuery()
    const data = await FourAccountReconcileApi.getFourAccountReconcilePage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
    await loadSummary()
  } catch (error: any) {
    list.value = []
    total.value = 0
    summaryData.value = createEmptySummaryData()
    summaryFallback.value = false
    summaryFallbackReason.value = ''
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
  relatedTicketLinked.value = undefined
  queryParams.source = undefined
  queryParams.issueCode = undefined
  getList()
}

const normalizeAuditQuery = () => {
  auditQueryParams.keyword = String(auditQueryParams.keyword || '').trim() || undefined
  auditQueryParams.refundAuditStatus = normalizeUpperText(auditQueryParams.refundAuditStatus)
  auditQueryParams.refundExceptionType = normalizeUpperText(
    auditQueryParams.refundExceptionType || auditQueryParams.mismatchType
  )
  auditQueryParams.mismatchType = auditQueryParams.refundExceptionType
  auditQueryParams.refundLimitSource = normalizeUpperText(auditQueryParams.refundLimitSource)
  if (Array.isArray(auditBizDateRange.value) && auditBizDateRange.value.length === 2) {
    auditQueryParams.beginBizDate = auditBizDateRange.value[0]
    auditQueryParams.endBizDate = auditBizDateRange.value[1]
  } else {
    auditQueryParams.beginBizDate = undefined
    auditQueryParams.endBizDate = undefined
  }
  if (Array.isArray(auditRefundTimeRange.value) && auditRefundTimeRange.value.length === 2) {
    auditQueryParams.refundTimeRange = [auditRefundTimeRange.value[0], auditRefundTimeRange.value[1]]
  } else {
    auditQueryParams.refundTimeRange = undefined
  }
}

const getAuditList = async () => {
  auditLoading.value = true
  try {
    normalizeAuditQuery()
    const data = await FourAccountReconcileApi.getFourAccountRefundCommissionAuditPage({
      pageNo: auditQueryParams.pageNo,
      pageSize: auditQueryParams.pageSize,
      beginBizDate: auditQueryParams.beginBizDate,
      endBizDate: auditQueryParams.endBizDate,
      refundAuditStatus: auditQueryParams.refundAuditStatus,
      refundExceptionType: auditQueryParams.refundExceptionType,
      refundLimitSource: auditQueryParams.refundLimitSource,
      payRefundId: parseNumber(auditQueryParams.payRefundId),
      refundTimeRange: auditQueryParams.refundTimeRange,
      mismatchType: auditQueryParams.mismatchType,
      keyword: auditQueryParams.keyword,
      orderId: parseNumber(auditQueryParams.orderId)
    })
    auditList.value = data.list || []
    auditTotal.value = data.total || 0
    applyAuditSummary(data as Record<string, any>)
  } catch (error: any) {
    auditList.value = []
    auditTotal.value = 0
    auditSummaryFallback.value = false
    auditSummaryData.value = createEmptyAuditSummaryData()
    message.error(error?.msg || '退款-提成巡检查询失败，请稍后重试')
  } finally {
    auditLoading.value = false
  }
}

const handleAuditQuery = () => {
  auditQueryParams.pageNo = 1
  getAuditList()
}

const resetAuditQuery = () => {
  auditQueryParams.pageNo = 1
  auditQueryParams.pageSize = 10
  auditBizDateRange.value = undefined
  auditRefundTimeRange.value = undefined
  auditSyncLimit.value = 200
  auditQueryParams.beginBizDate = undefined
  auditQueryParams.endBizDate = undefined
  auditQueryParams.refundAuditStatus = undefined
  auditQueryParams.refundExceptionType = undefined
  auditQueryParams.refundLimitSource = undefined
  auditQueryParams.payRefundId = undefined
  auditQueryParams.refundTimeRange = undefined
  auditQueryParams.mismatchType = undefined
  auditQueryParams.keyword = undefined
  auditQueryParams.orderId = undefined
  getAuditList()
}

const normalizeAuditSyncLimit = () => {
  const parsed = parseNumber(auditSyncLimit.value)
  if (parsed === undefined) {
    return 200
  }
  return Math.min(1000, Math.max(1, Math.floor(parsed)))
}

const handleSyncAuditTickets = async () => {
  if (auditSyncLoading.value) {
    return
  }
  normalizeAuditQuery()
  const payload: FourAccountReconcileApi.FourAccountRefundCommissionAuditSyncReq = {
    beginBizDate: auditQueryParams.beginBizDate,
    endBizDate: auditQueryParams.endBizDate,
    refundAuditStatus: auditQueryParams.refundAuditStatus,
    refundExceptionType: auditQueryParams.refundExceptionType,
    refundLimitSource: auditQueryParams.refundLimitSource,
    payRefundId: parseNumber(auditQueryParams.payRefundId),
    refundTimeRange: auditQueryParams.refundTimeRange,
    mismatchType: auditQueryParams.mismatchType,
    keyword: auditQueryParams.keyword,
    orderId: parseNumber(auditQueryParams.orderId),
    limit: normalizeAuditSyncLimit()
  }
  try {
    await message.confirm(`确认按当前筛选条件同步工单吗？本次最多同步 ${payload.limit} 条。`)
  } catch {
    return
  }

  auditSyncLoading.value = true
  try {
    const data = await FourAccountReconcileApi.syncFourAccountRefundCommissionAuditTickets(payload)
    const failedOrderIds = Array.isArray(data?.failedOrderIds)
      ? data.failedOrderIds.filter((id): id is number => Number.isFinite(Number(id))).map((id) => Number(id))
      : []
    auditSyncResult.value = {
      ...createEmptyAuditSyncResult(),
      ...(data || {}),
      failedOrderIds
    }
    auditSyncCollapseActive.value = failedOrderIds.length ? ['failed-order-ids'] : []
    auditSyncResultVisible.value = true
    if ((auditSyncResult.value.failedCount || 0) > 0) {
      message.warning(
        `同步完成：成功 ${auditSyncResult.value.successCount || 0}，失败 ${auditSyncResult.value.failedCount || 0}`
      )
    } else {
      message.success(`同步完成：成功 ${auditSyncResult.value.successCount || 0}`)
    }
  } catch (error: any) {
    message.error(error?.msg || '同步工单失败，请稍后重试')
  } finally {
    auditSyncLoading.value = false
  }
}

const copyFailedOrderIds = async () => {
  const ids = auditSyncResult.value.failedOrderIds || []
  if (!ids.length) {
    message.warning('当前无失败订单ID可复制')
    return
  }
  try {
    await navigator.clipboard.writeText(ids.join(','))
    message.success('失败订单ID已复制')
  } catch {
    message.error('复制失败，请检查浏览器剪贴板权限')
  }
}

const openAuditDetailDrawer = (row: FourAccountReconcileApi.FourAccountRefundCommissionAuditVO) => {
  auditDetailData.value = { ...(row || {}) }
  parseAuditEvidenceJson(auditDetailData.value.refundEvidenceJson)
  auditDetailDrawerVisible.value = true
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
  getAuditList()
})
</script>
