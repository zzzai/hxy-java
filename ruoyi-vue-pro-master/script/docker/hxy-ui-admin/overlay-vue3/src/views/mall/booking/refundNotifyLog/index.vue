<template>
  <ContentWrap>
    <el-form :inline="true" :model="queryParams" class="-mb-15px" label-width="108px">
      <el-form-item label="订单ID" prop="orderId">
        <el-input-number v-model="queryParams.orderId" :controls="false" :min="1" class="!w-180px" />
      </el-form-item>
      <el-form-item label="商户退款单号" prop="merchantRefundId">
        <el-input
          v-model="queryParams.merchantRefundId"
          class="!w-220px"
          clearable
          placeholder="请输入 merchantRefundId"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="支付退款单ID" prop="payRefundId">
        <el-input-number v-model="queryParams.payRefundId" :controls="false" :min="1" class="!w-180px" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select
          v-model="queryParams.status"
          allow-create
          class="!w-170px"
          clearable
          filterable
          placeholder="请选择或输入状态"
        >
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="错误码" prop="errorCode">
        <el-input
          v-model="queryParams.errorCode"
          class="!w-170px"
          clearable
          placeholder="例如 1030004014"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="创建时间" prop="createTime">
        <el-date-picker
          v-model="queryParams.createTime"
          class="!w-340px"
          end-placeholder="结束时间"
          range-separator="至"
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
      </el-form-item>

      <el-form-item label="自动重放上限">
        <el-input-number
          v-model="replayDueForm.limitSize"
          :controls="false"
          :max="1000"
          :min="1"
          class="!w-140px"
          placeholder="1~1000"
        />
      </el-form-item>
      <el-form-item label="自动重放模式">
        <el-select v-model="replayDueForm.dryRun" class="!w-180px">
          <el-option :value="true" label="dry-run 预演" />
          <el-option :value="false" label="执行落库" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button
          v-hasPermi="['booking:refund-notify-log:replay-due']"
          :loading="replayDueLoading"
          plain
          type="primary"
          @click="handleReplayDue"
        >
          <Icon class="mr-5px" icon="ep:video-play" />
          自动补偿重放
        </el-button>
        <el-button
          v-hasPermi="['booking:refund-notify-log:replay-run-log:query']"
          plain
          type="info"
          @click="openRunLogDialog"
        >
          <Icon class="mr-5px" icon="ep:document" />
          重放批次历史
        </el-button>
      </el-form-item>

      <el-form-item>
        <el-button
          v-hasPermi="['booking:refund-notify-log:replay']"
          :disabled="selectedReplayIds.length === 0"
          :loading="replayLoading"
          plain
          type="info"
          @click="handleDryRunReplay"
        >
          <Icon class="mr-5px" icon="ep:view" />
          预演重放（dry-run）
        </el-button>
        <el-button
          v-hasPermi="['booking:refund-notify-log:replay']"
          :disabled="selectedReplayIds.length === 0"
          :loading="replayLoading"
          plain
          type="warning"
          @click="handleExecuteReplay"
        >
          <Icon class="mr-5px" icon="ep:refresh-right" />
          执行重放
        </el-button>
      </el-form-item>

      <el-form-item>
        <span class="text-12px text-[var(--el-text-color-secondary)]">已选可重放 {{ selectedReplayIds.length }} 条</span>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" :selectable="isReplaySelectable" />
      <el-table-column label="ID" prop="id" width="90" />
      <el-table-column label="订单ID" width="120">
        <template #default="{ row }">
          {{ numberOrDash(row.orderId) }}
        </template>
      </el-table-column>
      <el-table-column label="商户退款单号" min-width="220" prop="merchantRefundId" show-overflow-tooltip>
        <template #default="{ row }">
          {{ textOrDash(row.merchantRefundId) }}
        </template>
      </el-table-column>
      <el-table-column label="支付退款单ID" width="140">
        <template #default="{ row }">
          {{ numberOrDash(row.payRefundId) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">
            {{ statusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="错误码" min-width="120" prop="errorCode" show-overflow-tooltip>
        <template #default="{ row }">
          {{ textOrDash(row.errorCode) }}
        </template>
      </el-table-column>
      <el-table-column label="错误信息" min-width="240" prop="errorMsg" show-overflow-tooltip>
        <template #default="{ row }">
          {{ textOrDash(row.errorMsg) }}
        </template>
      </el-table-column>
      <el-table-column label="重试次数" width="100">
        <template #default="{ row }">
          {{ numberOrDash(row.retryCount) }}
        </template>
      </el-table-column>
      <el-table-column :formatter="dateFormatter" label="下次重试时间" prop="nextRetryTime" width="180" />
      <el-table-column label="最近重放结果" width="130">
        <template #default="{ row }">
          <el-tag v-if="resolveReplayStatus(row.lastReplayResult)" :type="replayResultTagType(row.lastReplayResult)">
            {{ replayResultText(row.lastReplayResult) }}
          </el-tag>
          <span v-else>{{ EMPTY_TEXT }}</span>
        </template>
      </el-table-column>
      <el-table-column label="最近重放人" width="140" show-overflow-tooltip>
        <template #default="{ row }">
          {{ textOrDash(row.lastReplayOperator) }}
        </template>
      </el-table-column>
      <el-table-column :formatter="dateFormatter" label="最近重放时间" prop="lastReplayTime" width="180" />
      <el-table-column label="最近重放备注" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          {{ textOrDash(row.lastReplayRemark) }}
        </template>
      </el-table-column>
      <el-table-column :formatter="dateFormatter" label="创建时间" prop="createTime" width="180" />
      <el-table-column align="center" fixed="right" label="操作" width="90">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetailDrawer(row)">详情</el-button>
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

  <el-dialog v-model="replayResultVisible" :title="replayResultTitle" width="1080px">
    <el-alert
      v-if="replayResult.fallbackLegacy"
      :closable="false"
      title="当前后端为旧版本，已降级为逐条执行模式"
      type="warning"
      class="mb-12px"
    />
    <el-descriptions :column="4" border class="mb-12px">
      <el-descriptions-item label="模式">{{ replayResult.modeLabel }}</el-descriptions-item>
      <el-descriptions-item label="runId">{{ textOrDash(replayResult.runId) }}</el-descriptions-item>
      <el-descriptions-item label="执行状态">
        <el-tag :type="runLogStatusTagType(replayResult.status)">
          {{ runLogStatusText(replayResult.status) }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="触发人">{{ textOrDash(replayResult.operator) }}</el-descriptions-item>
      <el-descriptions-item label="触发来源">{{ textOrDash(replayResult.triggerSource) }}</el-descriptions-item>
      <el-descriptions-item label="dryRun">{{ boolText(replayResult.dryRun) }}</el-descriptions-item>
      <el-descriptions-item label="limit">{{ numberOrDash(replayResult.limitSize) }}</el-descriptions-item>
      <el-descriptions-item label="扫描数">{{ numberOrDash(replayResult.scannedCount) }}</el-descriptions-item>
      <el-descriptions-item label="成功数">{{ numberOrDash(replayResult.successCount) }}</el-descriptions-item>
      <el-descriptions-item label="跳过数">{{ numberOrDash(replayResult.skipCount) }}</el-descriptions-item>
      <el-descriptions-item label="失败数">{{ numberOrDash(replayResult.failCount) }}</el-descriptions-item>
      <el-descriptions-item label="开始时间">{{ textOrDash(replayResult.startTime) }}</el-descriptions-item>
      <el-descriptions-item label="结束时间">{{ textOrDash(replayResult.endTime) }}</el-descriptions-item>
      <el-descriptions-item label="错误信息" :span="3">{{ textOrDash(replayResult.errorMsg) }}</el-descriptions-item>
    </el-descriptions>

    <el-alert
      v-if="!replayResult.details.length"
      :closable="false"
      title="后端未返回 details 明细，请结合汇总结果与台账刷新后状态核对"
      type="info"
      class="mb-12px"
    />

    <el-empty v-if="!replayResult.details.length" description="无重放明细" />

    <el-table v-else :data="replayResult.details" max-height="460">
      <el-table-column label="runId" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">
          {{ textOrDash(row.runId || replayResult.runId) }}
        </template>
      </el-table-column>
      <el-table-column label="日志ID" min-width="90">
        <template #default="{ row }">
          {{ numberOrDash(row.id) }}
        </template>
      </el-table-column>
      <el-table-column label="订单ID" min-width="110">
        <template #default="{ row }">
          {{ numberOrDash(row.orderId) }}
        </template>
      </el-table-column>
      <el-table-column label="商户退款单号" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          {{ textOrDash(row.merchantRefundId) }}
        </template>
      </el-table-column>
      <el-table-column label="支付退款单ID" min-width="120">
        <template #default="{ row }">
          {{ numberOrDash(row.payRefundId) }}
        </template>
      </el-table-column>
      <el-table-column label="结果" width="100">
        <template #default="{ row }">
          <el-tag :type="replayResultTagType(row.resultStatus)">
            {{ replayResultText(row.resultStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="结果码" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">
          {{ textOrDash(row.resultCode) }}
        </template>
      </el-table-column>
      <el-table-column label="结果说明" min-width="320" show-overflow-tooltip>
        <template #default="{ row }">
          {{ textOrDash(row.resultMessage) }}
        </template>
      </el-table-column>
    </el-table>

    <template #footer>
      <el-button type="primary" @click="replayResultVisible = false">关闭</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="runLogDialogVisible" title="重放批次历史" width="1180px">
    <el-alert
      v-if="runLogFeatureUnavailable"
      :closable="false"
      title="后端未升级 V3，当前不可查询重放批次历史"
      type="warning"
      class="mb-12px"
    />

    <el-form :inline="true" :model="runLogQueryParams" class="-mb-10px" label-width="84px">
      <el-form-item label="runId" prop="runId">
        <el-input
          v-model="runLogQueryParams.runId"
          class="!w-220px"
          clearable
          placeholder="请输入 runId"
          @keyup.enter="handleRunLogQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="runLogQueryParams.status" class="!w-180px" clearable placeholder="请选择状态">
          <el-option v-for="item in runLogStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="触发来源" prop="triggerSource">
        <el-select
          v-model="runLogQueryParams.triggerSource"
          allow-create
          class="!w-180px"
          clearable
          filterable
          placeholder="MANUAL / AUTO"
        >
          <el-option
            v-for="item in runLogTriggerSourceOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="操作人" prop="operator">
        <el-input
          v-model="runLogQueryParams.operator"
          class="!w-180px"
          clearable
          placeholder="请输入操作人"
          @keyup.enter="handleRunLogQuery"
        />
      </el-form-item>
      <el-form-item label="含告警" prop="hasWarning">
        <el-select v-model="runLogQueryParams.hasWarning" class="!w-120px" clearable placeholder="全部">
          <el-option :value="true" label="是" />
          <el-option :value="false" label="否" />
        </el-select>
      </el-form-item>
      <el-form-item label="最小失败数" prop="minFailCount">
        <el-input-number
          v-model="runLogQueryParams.minFailCount"
          :controls="false"
          :min="0"
          class="!w-140px"
          placeholder=">=0"
        />
      </el-form-item>
      <el-form-item label="时间范围" prop="timeRange">
        <el-date-picker
          v-model="runLogTimeRange"
          class="!w-340px"
          end-placeholder="结束时间"
          range-separator="至"
          start-placeholder="开始时间"
          type="datetimerange"
          value-format="YYYY-MM-DD HH:mm:ss"
        />
      </el-form-item>
      <el-form-item>
        <el-button :loading="runLogLoading" @click="handleRunLogQuery">
          <Icon class="mr-5px" icon="ep:search" />
          查询
        </el-button>
        <el-button @click="resetRunLogQuery">
          <Icon class="mr-5px" icon="ep:refresh" />
          重置
        </el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="runLogLoading" :data="runLogList" class="mt-8px">
      <el-table-column label="runId" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">
          {{ textOrDash(row.runId) }}
        </template>
      </el-table-column>
      <el-table-column label="触发来源" min-width="130" show-overflow-tooltip>
        <template #default="{ row }">
          {{ textOrDash(row.triggerSource) }}
        </template>
      </el-table-column>
      <el-table-column label="操作人" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">
          {{ textOrDash(row.operator) }}
        </template>
      </el-table-column>
      <el-table-column label="dryRun" width="90">
        <template #default="{ row }">
          {{ boolText(row.dryRun) }}
        </template>
      </el-table-column>
      <el-table-column label="limit" width="90">
        <template #default="{ row }">
          {{ numberOrDash(row.limitSize) }}
        </template>
      </el-table-column>
      <el-table-column label="扫描" width="90">
        <template #default="{ row }">
          {{ numberOrDash(row.scannedCount) }}
        </template>
      </el-table-column>
      <el-table-column label="成功" width="90">
        <template #default="{ row }">
          {{ numberOrDash(row.successCount) }}
        </template>
      </el-table-column>
      <el-table-column label="跳过" width="90">
        <template #default="{ row }">
          {{ numberOrDash(row.skipCount) }}
        </template>
      </el-table-column>
      <el-table-column label="失败" width="90">
        <template #default="{ row }">
          {{ numberOrDash(row.failCount) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="130">
        <template #default="{ row }">
          <el-tag :type="runLogStatusTagType(row.status)">
            {{ runLogStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="错误信息" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">
          {{ textOrDash(row.errorMsg) }}
        </template>
      </el-table-column>
      <el-table-column :formatter="dateFormatter" label="开始时间" prop="startTime" width="180" />
      <el-table-column :formatter="dateFormatter" label="结束时间" prop="endTime" width="180" />
      <el-table-column align="center" fixed="right" label="操作" width="90">
        <template #default="{ row }">
          <el-button link type="primary" @click="openRunLogDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <Pagination
      v-model:limit="runLogQueryParams.pageSize"
      v-model:page="runLogQueryParams.pageNo"
      :total="runLogTotal"
      @pagination="getRunLogList"
    />

    <template #footer>
      <el-button @click="runLogDialogVisible = false">关闭</el-button>
    </template>
  </el-dialog>

  <el-drawer v-model="runLogDetailVisible" size="48%" title="重放批次详情">
    <div v-loading="runLogDetailLoading">
      <el-space wrap class="mb-12px">
        <el-button
          v-hasPermi="['booking:refund-notify-log:replay']"
          :disabled="!runLogCurrentRunId"
          :loading="runLogSyncLoading"
          type="primary"
          @click="handleSyncRunLogTickets(true)"
        >
          同步失败工单
        </el-button>
        <el-button
          v-hasPermi="['booking:refund-notify-log:replay']"
          :disabled="!runLogCurrentRunId"
          :loading="runLogSyncLoading"
          plain
          type="info"
          @click="handleSyncRunLogTickets(false)"
        >
          同步全部明细工单
        </el-button>
        <el-button
          v-if="runLogSyncFailedIdsText !== EMPTY_TEXT"
          link
          type="warning"
          @click="copyTextContent(runLogSyncFailedIdsText, '失败ID')"
        >
          复制失败ID
        </el-button>
      </el-space>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="runId">{{ textOrDash(runLogDetailData.runId) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="runLogStatusTagType(runLogDetailData.status)">
            {{ runLogStatusText(runLogDetailData.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="触发来源">{{ textOrDash(runLogDetailData.triggerSource) }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ textOrDash(runLogDetailData.operator) }}</el-descriptions-item>
        <el-descriptions-item label="dryRun">{{ boolText(runLogDetailData.dryRun) }}</el-descriptions-item>
        <el-descriptions-item label="limit">{{ numberOrDash(runLogDetailData.limitSize) }}</el-descriptions-item>
        <el-descriptions-item label="扫描数">{{ numberOrDash(runLogDetailData.scannedCount) }}</el-descriptions-item>
        <el-descriptions-item label="成功数">{{ numberOrDash(runLogDetailData.successCount) }}</el-descriptions-item>
        <el-descriptions-item label="跳过数">{{ numberOrDash(runLogDetailData.skipCount) }}</el-descriptions-item>
        <el-descriptions-item label="失败数">{{ numberOrDash(runLogDetailData.failCount) }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ textOrDash(runLogDetailData.startTime) }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ textOrDash(runLogDetailData.endTime) }}</el-descriptions-item>
      </el-descriptions>

      <div class="mt-16px">
        <div class="mb-8px flex items-center justify-between">
          <span class="font-500">批次汇总</span>
          <el-button
            :disabled="!runLogCurrentRunId"
            :loading="runLogSummaryLoading"
            link
            type="primary"
            @click="reloadRunLogSummary"
          >
            刷新
          </el-button>
        </div>
        <el-alert
          v-if="runLogSummaryFeatureUnavailable"
          :closable="false"
          title="后端版本暂不支持"
          type="warning"
          class="mb-8px"
        />
        <el-descriptions v-else v-loading="runLogSummaryLoading" :column="2" border>
          <el-descriptions-item label="runStatus">
            <el-tag :type="runLogStatusTagType(runLogSummaryStatus)">
              {{ runLogStatusText(runLogSummaryStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="triggerSource">{{ textOrDash(runLogSummaryTriggerSource) }}</el-descriptions-item>
          <el-descriptions-item label="operator">{{ textOrDash(runLogSummaryOperator) }}</el-descriptions-item>
          <el-descriptions-item label="dryRun">{{ boolText(runLogSummaryDryRun) }}</el-descriptions-item>
          <el-descriptions-item label="start">{{ textOrDash(runLogSummaryStartTime) }}</el-descriptions-item>
          <el-descriptions-item label="end">{{ textOrDash(runLogSummaryEndTime) }}</el-descriptions-item>
          <el-descriptions-item label="scanned">{{ numberOrDash(runLogSummaryScannedCount) }}</el-descriptions-item>
          <el-descriptions-item label="success">{{ numberOrDash(runLogSummarySuccessCount) }}</el-descriptions-item>
          <el-descriptions-item label="skip">{{ numberOrDash(runLogSummarySkipCount) }}</el-descriptions-item>
          <el-descriptions-item label="fail">{{ numberOrDash(runLogSummaryFailCount) }}</el-descriptions-item>
          <el-descriptions-item label="warning">{{ numberOrDash(runLogSummaryWarningCount) }}</el-descriptions-item>
          <el-descriptions-item label="topFailCodes">
            <el-space wrap>
              <template v-if="runLogSummaryTopFailCodeLabels.length">
                <el-tag v-for="item in runLogSummaryTopFailCodeLabels" :key="`fail-${item}`" type="danger">
                  {{ item }}
                </el-tag>
              </template>
              <span v-else>{{ EMPTY_TEXT }}</span>
            </el-space>
          </el-descriptions-item>
          <el-descriptions-item label="topWarningTags" :span="2">
            <el-space wrap>
              <template v-if="runLogSummaryTopWarningTagLabels.length">
                <el-tag v-for="item in runLogSummaryTopWarningTagLabels" :key="`warn-${item}`" type="warning">
                  {{ item }}
                </el-tag>
              </template>
              <span v-else>{{ EMPTY_TEXT }}</span>
            </el-space>
          </el-descriptions-item>
        </el-descriptions>

        <el-alert
          v-if="runLogSummaryErrorMsg"
          :closable="false"
          :title="runLogSummaryErrorMsg"
          type="error"
          class="mt-8px"
        />
        <el-button
          v-if="runLogSummaryErrorMsg"
          class="mt-6px"
          link
          type="warning"
          @click="copyTextContent(runLogSummaryErrorMsg, '批次汇总错误信息')"
        >
          复制错误信息
        </el-button>
      </div>

      <div class="mt-16px">
        <div class="mb-8px font-500">工单同步结果</div>
        <el-alert
          v-if="runLogSyncFeatureUnavailable"
          :closable="false"
          title="后端版本暂不支持"
          type="warning"
          class="mb-8px"
        />
        <el-descriptions v-else :column="2" border>
          <el-descriptions-item label="attempted">{{ numberOrDash(runLogSyncAttemptedCount) }}</el-descriptions-item>
          <el-descriptions-item label="success">{{ numberOrDash(runLogSyncSuccessCount) }}</el-descriptions-item>
          <el-descriptions-item label="failed">{{ numberOrDash(runLogSyncFailedCount) }}</el-descriptions-item>
          <el-descriptions-item label="failedIds" :span="2">
            {{ runLogSyncFailedIdsText }}
          </el-descriptions-item>
        </el-descriptions>
        <el-alert
          v-if="runLogSyncErrorMsg"
          :closable="false"
          :title="runLogSyncErrorMsg"
          type="error"
          class="mt-8px"
        />
        <el-button
          v-if="runLogSyncErrorMsg"
          class="mt-6px"
          link
          type="warning"
          @click="copyTextContent(runLogSyncErrorMsg, '同步工单错误信息')"
        >
          复制错误信息
        </el-button>
      </div>

      <div class="mt-16px">
        <div class="mb-8px flex items-center justify-between">
          <span class="font-500">错误信息</span>
          <el-button
            :disabled="textOrDash(runLogDetailData.errorMsg) === EMPTY_TEXT"
            link
            type="warning"
            @click="copyTextContent(runLogDetailData.errorMsg, '错误信息')"
          >
            复制错误信息
          </el-button>
        </div>
        <el-input :model-value="runLogDetailData.errorMsg || EMPTY_TEXT" :rows="5" readonly type="textarea" />
      </div>
    </div>
  </el-drawer>

  <el-drawer v-model="detailDrawerVisible" size="48%" title="退款回调日志详情">
    <el-descriptions :column="2" border>
      <el-descriptions-item label="台账ID">{{ numberOrDash(detailRow.id) }}</el-descriptions-item>
      <el-descriptions-item label="订单ID">{{ numberOrDash(detailRow.orderId) }}</el-descriptions-item>
      <el-descriptions-item label="商户退款单号">{{ textOrDash(detailRow.merchantRefundId) }}</el-descriptions-item>
      <el-descriptions-item label="支付退款单ID">{{ numberOrDash(detailRow.payRefundId) }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag :type="statusTagType(detailRow.status)">
          {{ statusText(detailRow.status) }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="错误码">{{ textOrDash(detailRow.errorCode) }}</el-descriptions-item>
      <el-descriptions-item label="错误信息" :span="2">{{ textOrDash(detailRow.errorMsg) }}</el-descriptions-item>
      <el-descriptions-item label="重试次数">{{ numberOrDash(detailRow.retryCount) }}</el-descriptions-item>
      <el-descriptions-item label="下次重试时间">{{ textOrDash(detailRow.nextRetryTime) }}</el-descriptions-item>
      <el-descriptions-item label="最近重放结果">
        <el-tag v-if="resolveReplayStatus(detailRow.lastReplayResult)" :type="replayResultTagType(detailRow.lastReplayResult)">
          {{ replayResultText(detailRow.lastReplayResult) }}
        </el-tag>
        <span v-else>{{ EMPTY_TEXT }}</span>
      </el-descriptions-item>
      <el-descriptions-item label="最近重放人">{{ textOrDash(detailRow.lastReplayOperator) }}</el-descriptions-item>
      <el-descriptions-item label="最近重放时间">{{ textOrDash(detailRow.lastReplayTime) }}</el-descriptions-item>
      <el-descriptions-item label="最近重放备注">{{ textOrDash(detailRow.lastReplayRemark) }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ textOrDash(detailRow.createTime) }}</el-descriptions-item>
      <el-descriptions-item label="更新时间">{{ textOrDash(detailRow.updateTime) }}</el-descriptions-item>
    </el-descriptions>

    <div class="mt-16px">
      <div class="mb-8px font-500">rawPayload</div>
      <el-alert
        v-if="rawPayloadParseFailed"
        :closable="false"
        title="rawPayload 非法 JSON，已降级展示原文"
        type="warning"
        class="mb-8px"
      />
      <el-input :model-value="rawPayloadView" :rows="14" readonly type="textarea" />
    </div>
  </el-drawer>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import * as RefundNotifyLogApi from '@/api/mall/booking/refundNotifyLog'

defineOptions({ name: 'MallBookingRefundNotifyLogIndex' })

type ReplayStatus = 'SUCCESS' | 'SKIP' | 'FAIL'

interface ReplayDetailView {
  runId?: string | number
  id?: number
  orderId?: number
  merchantRefundId?: string
  payRefundId?: number
  resultStatus: ReplayStatus
  resultCode?: string
  resultMessage?: string
}

interface ReplayResultView {
  modeLabel: string
  dryRun: boolean
  fallbackLegacy: boolean
  runId?: string | number
  triggerSource?: string
  operator?: string
  limitSize?: number
  scannedCount?: number
  successCount: number
  skipCount: number
  failCount: number
  status?: string
  errorMsg?: string
  startTime?: string
  endTime?: string
  details: ReplayDetailView[]
}

const EMPTY_TEXT = '--'
const DEFAULT_STATUS = 'fail'
const DEFAULT_REPLAY_DUE_LIMIT = 200
const RESULT_HINT_BY_CODE: Record<number, string> = {
  1030004011: '商户退款单号不合法，请先核对 merchantRefundId',
  1030004013: '退款回调日志不存在，可能已被清理或 ID 无效',
  1030004014: '日志状态非法，仅失败记录允许重放'
}
const statusOptions = [
  { label: '失败（fail）', value: 'fail' },
  { label: '成功（success）', value: 'success' },
  { label: '待处理（pending）', value: 'pending' }
]
const runLogStatusOptions = [
  { label: '运行中', value: 'RUNNING' },
  { label: '成功', value: 'SUCCESS' },
  { label: '部分失败', value: 'PARTIAL_FAIL' },
  { label: '失败', value: 'FAIL' }
]
const runLogTriggerSourceOptions = [
  { label: 'MANUAL', value: 'MANUAL' },
  { label: 'AUTO', value: 'AUTO' },
  { label: 'SCHEDULE', value: 'SCHEDULE' },
  { label: 'SYSTEM', value: 'SYSTEM' }
]

const message = useMessage()

const loading = ref(false)
const replayLoading = ref(false)
const replayDueLoading = ref(false)
const total = ref(0)
const list = ref<RefundNotifyLogApi.RefundNotifyLogVO[]>([])
const selectedRows = ref<RefundNotifyLogApi.RefundNotifyLogVO[]>([])

const detailDrawerVisible = ref(false)
const detailRow = ref<Partial<RefundNotifyLogApi.RefundNotifyLogVO>>({})
const rawPayloadView = ref(EMPTY_TEXT)
const rawPayloadParseFailed = ref(false)

const replayResultVisible = ref(false)
const replayResult = ref<ReplayResultView>({
  modeLabel: '手工勾选重放',
  dryRun: true,
  fallbackLegacy: false,
  runId: undefined,
  triggerSource: undefined,
  operator: undefined,
  limitSize: undefined,
  scannedCount: 0,
  successCount: 0,
  skipCount: 0,
  failCount: 0,
  status: undefined,
  errorMsg: undefined,
  startTime: undefined,
  endTime: undefined,
  details: []
})

const replayDueForm = reactive({
  dryRun: true,
  limitSize: DEFAULT_REPLAY_DUE_LIMIT
})

const runLogDialogVisible = ref(false)
const runLogLoading = ref(false)
const runLogFeatureUnavailable = ref(false)
const runLogTotal = ref(0)
const runLogList = ref<RefundNotifyLogApi.RefundNotifyReplayRunLogVO[]>([])
const runLogTimeRange = ref<string[]>()
const runLogQueryParams = reactive<RefundNotifyLogApi.RefundNotifyReplayRunLogPageReq>({
  pageNo: 1,
  pageSize: 10,
  runId: undefined,
  triggerSource: undefined,
  status: undefined,
  operator: undefined,
  hasWarning: undefined,
  minFailCount: undefined,
  timeRange: undefined
})

const runLogDetailVisible = ref(false)
const runLogDetailLoading = ref(false)
const runLogDetailData = ref<Partial<RefundNotifyLogApi.RefundNotifyReplayRunLogVO>>({})
const runLogSummaryLoading = ref(false)
const runLogSummaryFeatureUnavailable = ref(false)
const runLogSummaryErrorMsg = ref('')
const runLogSummaryData = ref<RefundNotifyLogApi.RefundNotifyReplayRunLogSummaryVO>()
const runLogSyncLoading = ref(false)
const runLogSyncFeatureUnavailable = ref(false)
const runLogSyncErrorMsg = ref('')
const runLogSyncResult = ref<RefundNotifyLogApi.RefundNotifyReplayRunLogSyncTicketsResp>()

const queryParams = reactive<RefundNotifyLogApi.RefundNotifyLogPageReq>({
  pageNo: 1,
  pageSize: 10,
  orderId: undefined,
  merchantRefundId: undefined,
  payRefundId: undefined,
  status: DEFAULT_STATUS,
  errorCode: undefined,
  createTime: undefined
})

const toNonNegativeNumber = (value: any): number | undefined => {
  if (value === undefined || value === null || value === '') {
    return undefined
  }
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) {
    return undefined
  }
  const normalized = Math.trunc(parsed)
  return normalized >= 0 ? normalized : undefined
}

const replayResultTitle = computed(() => {
  return replayResult.value.dryRun ? '退款回调重放预演结果' : '退款回调重放执行结果'
})
const runLogCurrentRunId = computed(() => {
  const text = String(runLogDetailData.value.runId || '').trim()
  return text || ''
})
const runLogSummaryStatus = computed(() => {
  return runLogSummaryData.value?.runStatus || runLogSummaryData.value?.status
})
const runLogSummaryTriggerSource = computed(() => {
  return runLogSummaryData.value?.triggerSource
})
const runLogSummaryOperator = computed(() => {
  return runLogSummaryData.value?.operator
})
const runLogSummaryDryRun = computed(() => {
  return runLogSummaryData.value?.dryRun
})
const runLogSummaryStartTime = computed(() => {
  return runLogSummaryData.value?.startTime || runLogSummaryData.value?.start
})
const runLogSummaryEndTime = computed(() => {
  return runLogSummaryData.value?.endTime || runLogSummaryData.value?.end
})
const runLogSummaryScannedCount = computed(() => {
  return toNonNegativeNumber(runLogSummaryData.value?.scannedCount)
    ?? toNonNegativeNumber(runLogSummaryData.value?.scanned)
})
const runLogSummarySuccessCount = computed(() => {
  return toNonNegativeNumber(runLogSummaryData.value?.successCount)
    ?? toNonNegativeNumber(runLogSummaryData.value?.success)
})
const runLogSummarySkipCount = computed(() => {
  return toNonNegativeNumber(runLogSummaryData.value?.skipCount)
    ?? toNonNegativeNumber(runLogSummaryData.value?.skip)
})
const runLogSummaryFailCount = computed(() => {
  return toNonNegativeNumber(runLogSummaryData.value?.failCount)
    ?? toNonNegativeNumber(runLogSummaryData.value?.fail)
})
const runLogSummaryWarningCount = computed(() => {
  return toNonNegativeNumber(runLogSummaryData.value?.warningCount)
    ?? toNonNegativeNumber(runLogSummaryData.value?.warning)
})
const runLogSyncAttemptedCount = computed(() => {
  return toNonNegativeNumber(runLogSyncResult.value?.attemptedCount)
    ?? toNonNegativeNumber(runLogSyncResult.value?.attempted)
})
const runLogSyncSuccessCount = computed(() => {
  return toNonNegativeNumber(runLogSyncResult.value?.successCount)
    ?? toNonNegativeNumber(runLogSyncResult.value?.success)
})
const runLogSyncFailedCount = computed(() => {
  return toNonNegativeNumber(runLogSyncResult.value?.failedCount)
    ?? toNonNegativeNumber(runLogSyncResult.value?.failed)
})
const runLogSyncFailedIdsText = computed(() => {
  const rawIds = runLogSyncResult.value?.failedIds
  if (!Array.isArray(rawIds) || !rawIds.length) {
    return EMPTY_TEXT
  }
  const ids = rawIds
    .map((item) => String(item || '').trim())
    .filter((item) => item)
  return ids.length ? ids.join(',') : EMPTY_TEXT
})

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

const boolText = (value?: boolean) => {
  if (value === undefined || value === null) {
    return EMPTY_TEXT
  }
  return value ? '是' : '否'
}

const parsePositiveInteger = (value: any): number | undefined => {
  if (value === undefined || value === null || value === '') {
    return undefined
  }
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) {
    return undefined
  }
  const normalized = Math.trunc(parsed)
  return normalized > 0 ? normalized : undefined
}

const parseNonNegativeInteger = (value: any): number | undefined => {
  if (value === undefined || value === null || value === '') {
    return undefined
  }
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) {
    return undefined
  }
  const normalized = Math.trunc(parsed)
  return normalized >= 0 ? normalized : undefined
}

const normalizeStatus = (value: any): string | undefined => {
  const text = String(value || '').trim().toLowerCase()
  return text || undefined
}

const normalizeUpperText = (value: any): string | undefined => {
  const text = String(value || '').trim().toUpperCase()
  return text || undefined
}

const formatSummaryBucketLabel = (item: any): string => {
  if (item === undefined || item === null) {
    return ''
  }
  if (typeof item !== 'object' || Array.isArray(item)) {
    return String(item).trim()
  }
  const source = item as Record<string, any>
  const key = String(
    source.key ?? source.code ?? source.tag ?? source.name ?? source.value ?? ''
  ).trim()
  const count = parseNonNegativeInteger(source.count)
  if (!key) {
    return ''
  }
  return count === undefined ? key : `${key}(${count})`
}

const toSummaryBucketLabels = (items: any[] | undefined): string[] => {
  if (!Array.isArray(items)) {
    return []
  }
  return items
    .map((item) => formatSummaryBucketLabel(item))
    .filter((item) => item)
}

const runLogSummaryTopFailCodeLabels = computed(() => {
  return toSummaryBucketLabels(runLogSummaryData.value?.topFailCodes as any[] | undefined)
})

const runLogSummaryTopWarningTagLabels = computed(() => {
  return toSummaryBucketLabels(runLogSummaryData.value?.topWarningTags as any[] | undefined)
})

const isFailStatus = (status: any) => {
  const normalized = normalizeStatus(status)
  return normalized === 'fail' || normalized === 'failed' || normalized === '2'
}

const canReplay = (row: Partial<RefundNotifyLogApi.RefundNotifyLogVO>) => {
  return isFailStatus(row.status)
}

const isReplaySelectable = (row: RefundNotifyLogApi.RefundNotifyLogVO) => {
  return canReplay(row)
}

const selectedReplayIds = computed(() => {
  const idSet = new Set<number>()
  selectedRows.value.forEach((row) => {
    if (!canReplay(row)) {
      return
    }
    const id = parsePositiveInteger(row.id)
    if (id) {
      idSet.add(id)
    }
  })
  return Array.from(idSet)
})

const selectedInvalidCount = computed(() => {
  return selectedRows.value.filter((row) => {
    return !canReplay(row) || !parsePositiveInteger(row.id)
  }).length
})

const statusText = (status?: RefundNotifyLogApi.RefundNotifyLogStatus) => {
  const normalized = normalizeStatus(status)
  if (normalized === 'success' || normalized === '1') return '成功'
  if (normalized === 'fail' || normalized === 'failed' || normalized === '2') return '失败'
  if (normalized === 'pending' || normalized === '0') return '待处理'
  return textOrDash(status)
}

const statusTagType = (status?: RefundNotifyLogApi.RefundNotifyLogStatus) => {
  const normalized = normalizeStatus(status)
  if (normalized === 'success' || normalized === '1') return 'success'
  if (normalized === 'fail' || normalized === 'failed' || normalized === '2') return 'danger'
  if (normalized === 'pending' || normalized === '0') return 'warning'
  return 'info'
}

const resolveReplayStatus = (value: any): ReplayStatus | undefined => {
  const normalized = normalizeUpperText(value)
  if (!normalized) {
    return undefined
  }
  if (['SUCCESS', 'SUCCEED', 'OK', 'DONE', 'PASSED'].includes(normalized)) {
    return 'SUCCESS'
  }
  if (['SKIP', 'SKIPPED', 'IGNORE', 'IGNORED'].includes(normalized)) {
    return 'SKIP'
  }
  if (normalized === '1') {
    return 'SUCCESS'
  }
  if (normalized === '2') {
    return 'SKIP'
  }
  if (['FAIL', 'FAILED', 'ERROR'].includes(normalized)) {
    return 'FAIL'
  }
  return undefined
}

const normalizeReplayStatus = (value: any): ReplayStatus => {
  return resolveReplayStatus(value) || 'FAIL'
}

const replayResultText = (status?: any) => {
  const normalized = resolveReplayStatus(status)
  if (!normalized) {
    return EMPTY_TEXT
  }
  if (normalized === 'SUCCESS') return 'SUCCESS'
  if (normalized === 'SKIP') return 'SKIP'
  return 'FAIL'
}

const replayResultTagType = (status?: any) => {
  const normalized = resolveReplayStatus(status)
  if (!normalized) return 'info'
  if (normalized === 'SUCCESS') return 'success'
  if (normalized === 'SKIP') return 'warning'
  return 'danger'
}

const runLogStatusText = (status?: RefundNotifyLogApi.RefundNotifyReplayRunStatus | string) => {
  const normalized = normalizeUpperText(status)
  if (normalized === 'RUNNING' || normalized === 'STARTED') return '运行中'
  if (normalized === 'SUCCESS') return '成功'
  if (normalized === 'PARTIAL_FAIL') return '部分失败'
  if (normalized === 'FAIL' || normalized === 'FAILED') return '失败'
  return textOrDash(status)
}

const runLogStatusTagType = (status?: RefundNotifyLogApi.RefundNotifyReplayRunStatus | string) => {
  const normalized = normalizeUpperText(status)
  if (normalized === 'RUNNING' || normalized === 'STARTED') return 'info'
  if (normalized === 'SUCCESS') return 'success'
  if (normalized === 'PARTIAL_FAIL') return 'warning'
  if (normalized === 'FAIL' || normalized === 'FAILED') return 'danger'
  return 'info'
}

const resolveApiErrorCode = (error: any): number | undefined => {
  const candidates = [
    error?.code,
    error?.data?.code,
    error?.response?.data?.code,
    error?.response?.status
  ]
  for (const candidate of candidates) {
    const parsed = Number(candidate)
    if (Number.isFinite(parsed)) {
      return parsed
    }
  }
  return undefined
}

const resolveApiErrorMessage = (error: any): string => {
  const candidates = [
    error?.msg,
    error?.message,
    error?.data?.msg,
    error?.data?.message,
    error?.response?.data?.msg,
    error?.response?.data?.message
  ]
  for (const candidate of candidates) {
    const text = String(candidate || '').trim()
    if (text) {
      return text
    }
  }
  return ''
}

const buildApiErrorMessage = (error: any, fallback: string) => {
  const code = resolveApiErrorCode(error)
  const rawMessage = resolveApiErrorMessage(error)
  if (code !== undefined && rawMessage) {
    return `${fallback}（错误码: ${code}）：${rawMessage}`
  }
  if (code !== undefined) {
    return `${fallback}（错误码: ${code}）`
  }
  return rawMessage || fallback
}

const isEndpointNotUpgraded = (error: any, pathHint = '') => {
  const code = resolveApiErrorCode(error)
  const msg = resolveApiErrorMessage(error).toLowerCase()
  if (code === 404) {
    return true
  }
  if (msg.includes('not found') || msg.includes('no handler found')) {
    return true
  }
  if (pathHint && msg.includes(pathHint.toLowerCase()) && msg.includes('not found')) {
    return true
  }
  return false
}

const buildReadableReasonByCode = (code?: string, messageText?: string, status?: ReplayStatus) => {
  const numericCode = Number(code)
  const hint = Number.isFinite(numericCode) ? RESULT_HINT_BY_CODE[numericCode] : ''
  if (status === 'SUCCESS') {
    return messageText || '执行成功'
  }
  if (status === 'SKIP') {
    return hint || messageText || '已跳过'
  }
  if (hint && messageText) {
    return `${hint}（${messageText}）`
  }
  return hint || messageText || '执行失败'
}

const isLegacyReplayContractError = (error: any) => {
  const code = resolveApiErrorCode(error)
  const messageText = resolveApiErrorMessage(error).toUpperCase()
  if (code === 400 || code === 500) {
    if (
      messageText.includes('ID不能为空') ||
      messageText.includes('CANNOT DESERIALIZE') ||
      messageText.includes('UNRECOGNIZED FIELD') ||
      messageText.includes('TYPE MISMATCH') ||
      (messageText.includes('IDS') && messageText.includes('ID'))
    ) {
      return true
    }
  }
  return false
}

const copyTextContent = async (value: any, label: string) => {
  const content = String(value || '').trim()
  if (!content || content === EMPTY_TEXT) {
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

const resetRunLogSummaryState = () => {
  runLogSummaryLoading.value = false
  runLogSummaryFeatureUnavailable.value = false
  runLogSummaryErrorMsg.value = ''
  runLogSummaryData.value = undefined
}

const resetRunLogSyncState = () => {
  runLogSyncLoading.value = false
  runLogSyncFeatureUnavailable.value = false
  runLogSyncErrorMsg.value = ''
  runLogSyncResult.value = undefined
}

const fetchRunLogSummary = async (silent = false) => {
  runLogSummaryErrorMsg.value = ''
  const runId = runLogCurrentRunId.value
  if (!runId) {
    runLogSummaryData.value = undefined
    return
  }

  runLogSummaryLoading.value = true
  try {
    const data = await RefundNotifyLogApi.getReplayRunLogSummary(runId)
    runLogSummaryData.value = data || undefined
    runLogSummaryFeatureUnavailable.value = false
  } catch (error: any) {
    runLogSummaryData.value = undefined
    if (isEndpointNotUpgraded(error, 'replay-run-log/summary')) {
      runLogSummaryFeatureUnavailable.value = true
      if (!silent) {
        message.warning('后端版本暂不支持')
      }
      return
    }
    runLogSummaryErrorMsg.value = buildApiErrorMessage(error, '批次汇总查询失败')
    message.error(runLogSummaryErrorMsg.value)
  } finally {
    runLogSummaryLoading.value = false
  }
}

const reloadRunLogSummary = () => {
  fetchRunLogSummary(false)
}

const handleSyncRunLogTickets = async (onlyFail: boolean) => {
  runLogSyncErrorMsg.value = ''
  const runId = runLogCurrentRunId.value
  if (!runId) {
    message.warning('runId 为空，无法同步工单')
    return
  }

  const syncTargetText = onlyFail ? '失败明细' : '全部明细'
  try {
    await message.confirm(`确认同步 runId=${runId} 的${syncTargetText}工单吗？`)
  } catch {
    return
  }

  runLogSyncLoading.value = true
  try {
    const data = await RefundNotifyLogApi.syncReplayRunLogTickets({ runId, onlyFail })
    runLogSyncResult.value = data || undefined
    runLogSyncFeatureUnavailable.value = false
    message.success(
      `同步完成：attempted ${numberOrDash(runLogSyncAttemptedCount.value)} / success ${numberOrDash(runLogSyncSuccessCount.value)} / failed ${numberOrDash(runLogSyncFailedCount.value)}`
    )
  } catch (error: any) {
    if (isEndpointNotUpgraded(error, 'replay-run-log/sync-tickets')) {
      runLogSyncFeatureUnavailable.value = true
      message.warning('后端版本暂不支持')
      return
    }
    runLogSyncErrorMsg.value = buildApiErrorMessage(error, '同步工单失败')
    message.error(runLogSyncErrorMsg.value)
  } finally {
    runLogSyncLoading.value = false
  }
}

const normalizeQuery = () => {
  queryParams.orderId = parsePositiveInteger(queryParams.orderId)
  queryParams.payRefundId = parsePositiveInteger(queryParams.payRefundId)
  queryParams.merchantRefundId = String(queryParams.merchantRefundId || '').trim() || undefined
  queryParams.status = normalizeStatus(queryParams.status)
  queryParams.errorCode = normalizeUpperText(queryParams.errorCode)
  queryParams.createTime =
    Array.isArray(queryParams.createTime) && queryParams.createTime.length === 2
      ? queryParams.createTime
      : undefined
}

const normalizeRunLogQuery = () => {
  runLogQueryParams.runId = String(runLogQueryParams.runId || '').trim() || undefined
  runLogQueryParams.triggerSource = normalizeUpperText(runLogQueryParams.triggerSource)
  runLogQueryParams.status = normalizeUpperText(runLogQueryParams.status)
  runLogQueryParams.operator = String(runLogQueryParams.operator || '').trim() || undefined
  runLogQueryParams.hasWarning =
    runLogQueryParams.hasWarning === true ? true : runLogQueryParams.hasWarning === false ? false : undefined
  runLogQueryParams.minFailCount = parseNonNegativeInteger(runLogQueryParams.minFailCount)
  const startTimeRange =
    Array.isArray(runLogTimeRange.value) && runLogTimeRange.value.length === 2
      ? [runLogTimeRange.value[0], runLogTimeRange.value[1]]
      : undefined
  runLogQueryParams.timeRange = startTimeRange
  ;(runLogQueryParams as any).startTime = startTimeRange
}

const normalizeReplayDueLimit = () => {
  const parsed = parsePositiveInteger(replayDueForm.limitSize)
  return parsed ? Math.min(1000, Math.max(1, parsed)) : DEFAULT_REPLAY_DUE_LIMIT
}

const parseRawPayload = (rawPayload: any) => {
  const rawText = String(rawPayload || '').trim()
  if (!rawText) {
    rawPayloadParseFailed.value = false
    rawPayloadView.value = EMPTY_TEXT
    return
  }
  try {
    const parsed = JSON.parse(rawText)
    rawPayloadView.value = JSON.stringify(parsed, null, 2)
    rawPayloadParseFailed.value = false
  } catch {
    rawPayloadView.value = rawText
    rawPayloadParseFailed.value = true
  }
}

const openDetailDrawer = (row: RefundNotifyLogApi.RefundNotifyLogVO) => {
  detailRow.value = { ...(row || {}) }
  parseRawPayload(detailRow.value.rawPayload)
  detailDrawerVisible.value = true
}

const getList = async () => {
  loading.value = true
  try {
    normalizeQuery()
    const data = await RefundNotifyLogApi.getRefundNotifyLogPage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
    selectedRows.value = []
  } catch (error: any) {
    list.value = []
    total.value = 0
    selectedRows.value = []
    message.error(buildApiErrorMessage(error, '退款回调日志查询失败'))
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
  queryParams.orderId = undefined
  queryParams.merchantRefundId = undefined
  queryParams.payRefundId = undefined
  queryParams.status = DEFAULT_STATUS
  queryParams.errorCode = undefined
  queryParams.createTime = undefined
  selectedRows.value = []
  getList()
}

const handleSelectionChange = (rows: RefundNotifyLogApi.RefundNotifyLogVO[]) => {
  selectedRows.value = rows || []
}

const ensureReplaySelection = () => {
  if (!selectedRows.value.length) {
    message.warning('请先勾选至少一条失败记录')
    return false
  }
  if (!selectedReplayIds.value.length) {
    message.warning('当前勾选项没有可重放失败记录')
    return false
  }
  if (selectedInvalidCount.value > 0) {
    message.warning(`已自动忽略 ${selectedInvalidCount.value} 条不可重放记录`)
  }
  return true
}

const unwrapReplayResponse = (rawResp: any) => {
  if (rawResp === undefined || rawResp === null) {
    return rawResp
  }
  if (typeof rawResp !== 'object' || Array.isArray(rawResp)) {
    return rawResp
  }
  const source = rawResp as Record<string, any>
  if (source.data && typeof source.data === 'object' && !Array.isArray(source.data)) {
    return source.data
  }
  return source
}

const parseReplayDetail = (item: Record<string, any>, index: number): ReplayDetailView => {
  const status = normalizeReplayStatus(item.resultStatus ?? item.result ?? item.status ?? item.resultType)
  const resultCode = String(item.resultCode ?? item.code ?? item.errorCode ?? '').trim() || undefined
  const rawReason = String(item.resultMessage ?? item.message ?? item.failReason ?? item.errorMsg ?? '').trim() || undefined
  return {
    runId: item.runId,
    id: parsePositiveInteger(item.id),
    orderId: parsePositiveInteger(item.orderId),
    merchantRefundId: String(item.merchantRefundId || '').trim() || undefined,
    payRefundId: parsePositiveInteger(item.payRefundId),
    resultStatus: status,
    resultCode,
    resultMessage: buildReadableReasonByCode(resultCode, rawReason, status) || `明细 #${index + 1}`
  }
}

const createReplayResultView = (): ReplayResultView => {
  return {
    modeLabel: '手工勾选重放',
    dryRun: true,
    fallbackLegacy: false,
    runId: undefined,
    triggerSource: undefined,
    operator: undefined,
    limitSize: undefined,
    scannedCount: 0,
    successCount: 0,
    skipCount: 0,
    failCount: 0,
    status: undefined,
    errorMsg: undefined,
    startTime: undefined,
    endTime: undefined,
    details: []
  }
}

const buildReplayResultView = (
  rawResp: any,
  options: {
    modeLabel: string
    dryRun: boolean
    defaultIds?: number[]
    fallbackLegacy?: boolean
  }
): ReplayResultView => {
  const source = unwrapReplayResponse(rawResp)
  if (typeof source === 'boolean') {
    return {
      ...createReplayResultView(),
      modeLabel: options.modeLabel,
      dryRun: options.dryRun,
      fallbackLegacy: Boolean(options.fallbackLegacy),
      scannedCount: options.defaultIds?.length || 0,
      successCount: source ? options.defaultIds?.length || 0 : 0,
      skipCount: 0,
      failCount: source ? 0 : options.defaultIds?.length || 0,
      status: source ? 'SUCCESS' : 'FAIL',
      details: source
        ? (options.defaultIds || []).map((id) => ({
            id,
            resultStatus: 'SUCCESS',
            resultMessage: options.dryRun ? '预演通过' : '执行成功'
          }))
        : (options.defaultIds || []).map((id) => ({
            id,
            resultStatus: 'FAIL',
            resultMessage: '执行失败'
          }))
    }
  }

  const payload = ((source || {}) as Record<string, any>) || {}
  const rawDetails = Array.isArray(payload.details)
    ? payload.details
    : Array.isArray(payload.items)
      ? payload.items
      : []
  const details = rawDetails
    .filter((item) => item && typeof item === 'object')
    .map((item, index) => parseReplayDetail(item as Record<string, any>, index))

  const successCount =
    parseNonNegativeInteger(payload.successCount) ?? details.filter((item) => item.resultStatus === 'SUCCESS').length
  const skipCount =
    parseNonNegativeInteger(payload.skipCount) ?? details.filter((item) => item.resultStatus === 'SKIP').length
  const failCount =
    parseNonNegativeInteger(payload.failCount) ?? details.filter((item) => item.resultStatus === 'FAIL').length
  const defaultScanCount = Array.isArray(options.defaultIds) ? options.defaultIds.length : undefined
  const scannedCount =
    parseNonNegativeInteger(payload.scannedCount)
    ?? defaultScanCount
    ?? successCount + skipCount + failCount

  return {
    ...createReplayResultView(),
    modeLabel: options.modeLabel,
    dryRun: options.dryRun,
    fallbackLegacy: Boolean(options.fallbackLegacy),
    runId: payload.runId ?? payload.batchRunId ?? payload.id,
    triggerSource: payload.triggerSource,
    operator: payload.operator,
    limitSize: parsePositiveInteger(payload.limitSize),
    scannedCount,
    successCount,
    skipCount,
    failCount,
    status: payload.status,
    errorMsg: String(payload.errorMsg || '').trim() || undefined,
    startTime: payload.startTime,
    endTime: payload.endTime,
    details
  }
}

const replayByLegacyApi = async (ids: number[]): Promise<ReplayResultView> => {
  const details: ReplayDetailView[] = []
  for (const id of ids) {
    try {
      await RefundNotifyLogApi.replayRefundNotifyLog({ id })
      details.push({
        id,
        resultStatus: 'SUCCESS',
        resultMessage: '旧接口执行成功'
      })
    } catch (error: any) {
      const code = resolveApiErrorCode(error)
      const codeText = code !== undefined ? String(code) : undefined
      const rawMessage = resolveApiErrorMessage(error)
      const status = code === 1030004014 ? 'SKIP' : 'FAIL'
      details.push({
        id,
        resultStatus: status,
        resultCode: codeText,
        resultMessage: buildReadableReasonByCode(codeText, rawMessage, status)
      })
    }
  }

  return {
    ...createReplayResultView(),
    modeLabel: '手工勾选重放（旧接口降级）',
    dryRun: false,
    fallbackLegacy: true,
    scannedCount: ids.length,
    successCount: details.filter((item) => item.resultStatus === 'SUCCESS').length,
    skipCount: details.filter((item) => item.resultStatus === 'SKIP').length,
    failCount: details.filter((item) => item.resultStatus === 'FAIL').length,
    status: details.some((item) => item.resultStatus === 'FAIL') ? 'PARTIAL_FAIL' : 'SUCCESS',
    details
  }
}

const showReplayResult = (result: ReplayResultView) => {
  replayResult.value = result
  replayResultVisible.value = true
  if (!result.details.length) {
    message.warning('后端未返回 details 明细，请结合汇总结果核对')
  }
}

const handleDryRunReplay = async () => {
  if (!ensureReplaySelection()) {
    return
  }
  replayLoading.value = true
  try {
    const ids = selectedReplayIds.value
    const resp = await RefundNotifyLogApi.replayRefundNotifyLog({
      dryRun: true,
      ids
    })
    const result = buildReplayResultView(resp, {
      modeLabel: '手工勾选重放',
      dryRun: true,
      defaultIds: ids
    })
    showReplayResult(result)
    message.success(
      `预演完成：runId=${textOrDash(result.runId)}，SUCCESS ${result.successCount} / SKIP ${result.skipCount} / FAIL ${result.failCount}`
    )
  } catch (error: any) {
    if (isLegacyReplayContractError(error)) {
      message.warning('当前后端未支持 dry-run 预演能力，请升级至 V2+ 后端后重试')
      return
    }
    message.error(buildApiErrorMessage(error, '预演重放失败'))
  } finally {
    replayLoading.value = false
  }
}

const handleExecuteReplay = async () => {
  if (!ensureReplaySelection()) {
    return
  }
  const ids = selectedReplayIds.value
  try {
    await message.confirm(`确认执行重放 ${ids.length} 条失败记录吗？`)
  } catch {
    return
  }

  replayLoading.value = true
  try {
    let result: ReplayResultView
    try {
      const resp = await RefundNotifyLogApi.replayRefundNotifyLog({
        dryRun: false,
        ids
      })
      result = buildReplayResultView(resp, {
        modeLabel: '手工勾选重放',
        dryRun: false,
        defaultIds: ids
      })
    } catch (error: any) {
      if (!isLegacyReplayContractError(error)) {
        throw error
      }
      result = await replayByLegacyApi(ids)
      message.warning('已降级旧接口逐条执行重放')
    }

    showReplayResult(result)
    message.success(
      `执行完成：runId=${textOrDash(result.runId)}，SUCCESS ${result.successCount} / SKIP ${result.skipCount} / FAIL ${result.failCount}`
    )
    await getList()
  } catch (error: any) {
    message.error(buildApiErrorMessage(error, '执行重放失败'))
  } finally {
    replayLoading.value = false
  }
}

const handleReplayDue = async () => {
  const limitSize = normalizeReplayDueLimit()
  replayDueForm.limitSize = limitSize
  const modeText = replayDueForm.dryRun ? 'dry-run 预演' : '执行落库'
  try {
    await message.confirm(`确认触发自动补偿重放吗？模式：${modeText}；limit：${limitSize}`)
  } catch {
    return
  }

  replayDueLoading.value = true
  try {
    const resp = await RefundNotifyLogApi.replayDue({
      dryRun: replayDueForm.dryRun,
      limit: limitSize
    })
    const result = buildReplayResultView(resp, {
      modeLabel: '自动补偿重放',
      dryRun: replayDueForm.dryRun,
      defaultIds: []
    })
    showReplayResult(result)
    message.success(
      `自动重放已触发：runId=${textOrDash(result.runId)}，SUCCESS ${result.successCount} / SKIP ${result.skipCount} / FAIL ${result.failCount}`
    )
    if (!replayDueForm.dryRun) {
      await getList()
    }
    if (runLogDialogVisible.value && !runLogFeatureUnavailable.value) {
      await getRunLogList()
    }
  } catch (error: any) {
    if (isEndpointNotUpgraded(error, 'replay-due')) {
      message.warning('后端未升级 V3，暂不支持自动补偿重放入口')
      return
    }
    message.error(buildApiErrorMessage(error, '自动补偿重放触发失败'))
  } finally {
    replayDueLoading.value = false
  }
}

const getRunLogList = async () => {
  runLogLoading.value = true
  try {
    normalizeRunLogQuery()
    const data = await RefundNotifyLogApi.getReplayRunLogPage(runLogQueryParams)
    runLogList.value = data.list || []
    runLogTotal.value = data.total || 0
    runLogFeatureUnavailable.value = false
  } catch (error: any) {
    runLogList.value = []
    runLogTotal.value = 0
    if (isEndpointNotUpgraded(error, 'replay-run-log')) {
      runLogFeatureUnavailable.value = true
      message.warning('后端未升级 V3，当前不可查询重放批次历史')
      return
    }
    message.error(buildApiErrorMessage(error, '重放批次历史查询失败'))
  } finally {
    runLogLoading.value = false
  }
}

const openRunLogDialog = () => {
  runLogDialogVisible.value = true
  getRunLogList()
}

const handleRunLogQuery = () => {
  runLogQueryParams.pageNo = 1
  getRunLogList()
}

const resetRunLogQuery = () => {
  runLogQueryParams.pageNo = 1
  runLogQueryParams.pageSize = 10
  runLogQueryParams.runId = undefined
  runLogQueryParams.triggerSource = undefined
  runLogQueryParams.status = undefined
  runLogQueryParams.operator = undefined
  runLogQueryParams.hasWarning = undefined
  runLogQueryParams.minFailCount = undefined
  runLogQueryParams.timeRange = undefined
  ;(runLogQueryParams as any).startTime = undefined
  runLogTimeRange.value = undefined
  getRunLogList()
}

const openRunLogDetail = async (row: RefundNotifyLogApi.RefundNotifyReplayRunLogVO) => {
  runLogDetailVisible.value = true
  runLogDetailLoading.value = true
  runLogDetailData.value = { ...(row || {}) }
  resetRunLogSummaryState()
  resetRunLogSyncState()

  const id = parsePositiveInteger(row.id)
  if (!id) {
    runLogDetailLoading.value = false
    await fetchRunLogSummary(true)
    return
  }

  try {
    const data = await RefundNotifyLogApi.getReplayRunLog(id)
    runLogDetailData.value = {
      ...(row || {}),
      ...(data || {})
    }
  } catch (error: any) {
    if (isEndpointNotUpgraded(error, 'replay-run-log/get')) {
      message.warning('后端未提供批次详情接口，已降级展示列表快照')
      return
    }
    message.error(buildApiErrorMessage(error, '重放批次详情查询失败'))
  } finally {
    runLogDetailLoading.value = false
    await fetchRunLogSummary(true)
  }
}

onMounted(() => {
  getList()
})
</script>
