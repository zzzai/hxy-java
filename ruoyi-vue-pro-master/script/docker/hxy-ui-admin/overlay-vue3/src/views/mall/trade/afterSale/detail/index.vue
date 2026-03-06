<template>
  <ContentWrap>
    <!-- 订单信息 -->
    <el-descriptions title="订单信息">
      <el-descriptions-item label="订单号: ">{{ formData.orderNo }}</el-descriptions-item>
      <el-descriptions-item label="配送方式: ">
        <dict-tag :type="DICT_TYPE.TRADE_DELIVERY_TYPE" :value="formData.order.deliveryType" />
      </el-descriptions-item>
      <el-descriptions-item label="订单类型: ">
        <dict-tag :type="DICT_TYPE.TRADE_ORDER_TYPE" :value="formData.order.type" />
      </el-descriptions-item>
      <el-descriptions-item label="收货人: ">
        {{ formData.order.receiverName }}
      </el-descriptions-item>
      <el-descriptions-item label="买家留言: ">
        {{ formData.order.userRemark }}
      </el-descriptions-item>
      <el-descriptions-item label="订单来源: ">
        <dict-tag :type="DICT_TYPE.TERMINAL" :value="formData.order.terminal" />
      </el-descriptions-item>
      <el-descriptions-item label="联系电话: ">
        {{ formData.order.receiverMobile }}
      </el-descriptions-item>
      <el-descriptions-item label="商家备注: ">{{ formData.order.remark }}</el-descriptions-item>
      <el-descriptions-item label="支付单号: ">
        {{ formData.order.payOrderId }}
      </el-descriptions-item>
      <el-descriptions-item label="付款方式: ">
        <dict-tag :type="DICT_TYPE.PAY_CHANNEL_CODE" :value="formData.order.payChannelCode" />
      </el-descriptions-item>
      <el-descriptions-item label="买家: ">{{ formData?.user?.nickname }}</el-descriptions-item>
    </el-descriptions>

    <!-- 售后信息 -->
    <el-descriptions title="售后信息">
      <el-descriptions-item label="退款编号: ">{{ formData.no }}</el-descriptions-item>
      <el-descriptions-item label="申请时间: ">
        {{ formatDate(formData.auditTime) }}
      </el-descriptions-item>
      <el-descriptions-item label="售后类型: ">
        <dict-tag :type="DICT_TYPE.TRADE_AFTER_SALE_TYPE" :value="formData.type" />
      </el-descriptions-item>
      <el-descriptions-item label="售后方式: ">
        <dict-tag :type="DICT_TYPE.TRADE_AFTER_SALE_WAY" :value="formData.way" />
      </el-descriptions-item>
      <el-descriptions-item label="退款金额: ">
        {{ fenToYuan(formData.refundPrice) }}
      </el-descriptions-item>
      <el-descriptions-item label="退款上限来源: ">
        <span>{{ refundLimitSourceDisplay }}</span>
        <el-tag v-if="formData.refundLimitSource" class="ml-8px" size="small" type="info">
          {{ formData.refundLimitSource }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="退款审计状态: ">
        {{ formData.refundAuditStatus || EMPTY_TEXT }}
      </el-descriptions-item>
      <el-descriptions-item label="退款异常类型: ">
        {{ formData.refundExceptionType || EMPTY_TEXT }}
      </el-descriptions-item>
      <el-descriptions-item label="支付退款ID: ">
        {{ formData.payRefundId || EMPTY_TEXT }}
      </el-descriptions-item>
      <el-descriptions-item label="退款时间: ">
        {{ formatDateTimeOrDash(formData.refundTime) }}
      </el-descriptions-item>
      <el-descriptions-item label="退款原因: ">{{ formData.applyReason }}</el-descriptions-item>
      <el-descriptions-item :span="2" label="规则提示: ">
        {{ formData.refundLimitRuleHint || '-' }}
      </el-descriptions-item>
      <el-descriptions-item :span="2" label="退款审计备注: ">
        {{ formData.refundAuditRemark || EMPTY_TEXT }}
      </el-descriptions-item>
      <el-descriptions-item :span="2" label="上限审计明细: ">
        <div v-if="refundLimitDetailView.parseSuccess && refundLimitDetailView.entries.length" class="refund-limit-detail">
          <div v-for="entry in refundLimitDetailView.entries" :key="entry.key" class="refund-limit-detail__row">
            <span class="refund-limit-detail__key">{{ entry.key }}：</span>
            <span>{{ entry.value }}</span>
          </div>
        </div>
        <div v-else-if="refundLimitDetailView.parseFailed">
          <el-alert :closable="false" title="退款上限审计明细解析失败，已保留原文" type="warning" />
          <el-input
            :model-value="refundLimitDetailView.rawText || '-'"
            :rows="4"
            class="mt-8px"
            readonly
            type="textarea"
          />
        </div>
        <span v-else>{{ refundLimitDetailView.rawText || '-' }}</span>
      </el-descriptions-item>
      <el-descriptions-item :span="2" label="退款审计证据: ">
        <div v-if="refundEvidenceView.parseSuccess && refundEvidenceView.entries.length" class="refund-limit-detail">
          <div v-for="entry in refundEvidenceView.entries" :key="entry.key" class="refund-limit-detail__row">
            <span class="refund-limit-detail__key">{{ entry.key }}：</span>
            <span>{{ entry.value }}</span>
          </div>
        </div>
        <div v-else-if="refundEvidenceView.parseFailed">
          <el-alert :closable="false" title="退款审计证据解析失败，已保留原文" type="warning" />
          <el-input
            :model-value="refundEvidenceView.rawText || EMPTY_TEXT"
            :rows="4"
            class="mt-8px"
            readonly
            type="textarea"
          />
        </div>
        <span v-else>{{ refundEvidenceView.rawText || EMPTY_TEXT }}</span>
      </el-descriptions-item>
      <el-descriptions-item label="补充描述: ">
        {{ formData.applyDescription }}
      </el-descriptions-item>
      <el-descriptions-item label="凭证图片: ">
        <el-image
          v-for="(item, index) in formData.applyPicUrls"
          :key="index"
          :src="item.url"
          class="mr-10px h-60px w-60px"
          @click="imagePreview(formData.applyPicUrls)"
        />
      </el-descriptions-item>
    </el-descriptions>

    <!-- 退款状态 -->
    <el-descriptions :column="1" title="退款状态">
      <el-descriptions-item label="退款状态: ">
        <dict-tag :type="DICT_TYPE.TRADE_AFTER_SALE_STATUS" :value="formData.status" />
      </el-descriptions-item>
      <el-descriptions-item label-class-name="no-colon">
        <el-button v-if="formData.status === 10" type="primary" @click="agree">同意售后</el-button>
        <el-button v-if="formData.status === 10" type="primary" @click="disagree">
          拒绝售后
        </el-button>
        <el-button v-if="formData.status === 30" type="primary" @click="receive">
          确认收货
        </el-button>
        <el-button v-if="formData.status === 30" type="primary" @click="refuse">拒绝收货</el-button>
        <el-button v-if="formData.status === 40" type="primary" @click="refund">确认退款</el-button>
      </el-descriptions-item>
      <el-descriptions-item>
        <template #label><span style="color: red">提醒: </span></template>
        如果未发货，请点击同意退款给买家。<br />
        如果实际已发货，请主动与买家联系。<br />
        如果订单整体退款后，优惠券和余额会退还给买家.
      </el-descriptions-item>
    </el-descriptions>

    <!-- 商品信息 -->
    <el-descriptions title="商品信息">
      <el-descriptions-item labelClassName="no-colon">
        <el-row :gutter="20">
          <el-col :span="15">
            <el-table v-if="formData.orderItem" :data="[formData.orderItem]" border>
              <el-table-column label="商品" prop="spuName" width="auto">
                <template #default="{ row }">
                  {{ row.spuName }}
                  <el-tag
                    v-for="property in row.properties"
                    :key="property.propertyId"
                    class="mr-10px"
                  >
                    {{ property.propertyName }}: {{ property.valueName }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="商品原价" prop="price" width="150">
                <template #default="{ row }">{{ fenToYuan(row.price) }} 元</template>
              </el-table-column>
              <el-table-column label="数量" prop="count" width="100" />
              <el-table-column label="合计" prop="payPrice" width="150">
                <template #default="{ row }">{{ fenToYuan(row.payPrice) }} 元</template>
              </el-table-column>
            </el-table>
          </el-col>
          <el-col :span="10" />
        </el-row>
      </el-descriptions-item>
    </el-descriptions>

    <!-- 操作日志 -->
    <el-descriptions title="售后日志">
      <el-descriptions-item labelClassName="no-colon">
        <el-timeline>
          <el-timeline-item
            v-for="saleLog in formData.logs"
            :key="saleLog.id"
            :timestamp="formatDate(saleLog.createTime)"
            placement="top"
          >
            <div class="el-timeline-right-content">
              <span>{{ saleLog.content }}</span>
            </div>
            <template #dot>
              <span
                :style="{ backgroundColor: getUserTypeColor(saleLog.userType) }"
                class="dot-node-style"
              >
                {{ getDictLabel(DICT_TYPE.USER_TYPE, saleLog.userType)[0] || '系' }}
              </span>
            </template>
          </el-timeline-item>
        </el-timeline>
      </el-descriptions-item>
    </el-descriptions>
  </ContentWrap>

  <!-- 各种操作的弹窗 -->
  <UpdateAuditReasonForm ref="updateAuditReasonFormRef" @success="getDetail" />
</template>
<script lang="ts" setup>
import * as AfterSaleApi from '@/api/mall/trade/afterSale/index'
import { fenToYuan } from '@/utils'
import { DICT_TYPE, getDictLabel, getDictObj } from '@/utils/dict'
import { formatDate } from '@/utils/formatTime'
import UpdateAuditReasonForm from '@/views/mall/trade/afterSale/form/AfterSaleDisagreeForm.vue'
import { createImageViewer } from '@/components/ImageViewer'
import { isArray } from '@/utils/is'
import { useTagsViewStore } from '@/store/modules/tagsView'

defineOptions({ name: 'TradeAfterSaleDetail' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗
const { params } = useRoute() // 查询参数
const { push, currentRoute } = useRouter() // 路由
const formData = ref({
  order: {},
  logs: []
})
const updateAuditReasonFormRef = ref() // 拒绝售后表单 Ref
const REFUND_LIMIT_BUNDLE_CHILD_FULFILLED_CODE = 1011000125
const BOOKING_ORDER_REFUND_NOTIFY_ORDER_ID_INVALID_CODE = 1030004011
const BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT_CODE = 1030004012
const EMPTY_TEXT = '-'

interface RefundLimitDetailEntry {
  key: string
  value: string
}

interface RefundLimitDetailView {
  parseSuccess: boolean
  parseFailed: boolean
  entries: RefundLimitDetailEntry[]
  rawText: string
}

const stringifyRefundLimitValue = (value: any): string => {
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

const parseRefundLimitDetailJson = (rawValue?: string): RefundLimitDetailView => {
  const rawText = String(rawValue || '').trim()
  if (!rawText) {
    return {
      parseSuccess: false,
      parseFailed: false,
      entries: [],
      rawText: ''
    }
  }
  try {
    const parsed = JSON.parse(rawText)
    if (Array.isArray(parsed)) {
      return {
        parseSuccess: true,
        parseFailed: false,
        entries: parsed.map((item, index) => ({ key: `[${index}]`, value: stringifyRefundLimitValue(item) })),
        rawText
      }
    }
    if (parsed && typeof parsed === 'object') {
      return {
        parseSuccess: true,
        parseFailed: false,
        entries: Object.entries(parsed).map(([key, value]) => ({
          key,
          value: stringifyRefundLimitValue(value)
        })),
        rawText
      }
    }
    return {
      parseSuccess: true,
      parseFailed: false,
      entries: [{ key: 'value', value: stringifyRefundLimitValue(parsed) }],
      rawText
    }
  } catch {
    return {
      parseSuccess: false,
      parseFailed: true,
      entries: [],
      rawText
    }
  }
}

const formatDateTimeOrDash = (value: any) => {
  if (!value) {
    return EMPTY_TEXT
  }
  try {
    return formatDate(value) || EMPTY_TEXT
  } catch {
    return EMPTY_TEXT
  }
}

const refundLimitSourceDisplay = computed(() => {
  return formData.value.refundLimitSourceLabel || formData.value.refundLimitSource || EMPTY_TEXT
})

const refundLimitDetailView = computed(() => {
  return parseRefundLimitDetailJson(formData.value.refundLimitDetailJson)
})

const refundEvidenceView = computed(() => {
  return parseRefundLimitDetailJson(formData.value.refundEvidenceJson)
})

const resolveRefundErrorCode = (error: any): number | undefined => {
  const candidates = [
    error?.code,
    error?.status,
    error?.data?.code,
    error?.response?.data?.code
  ]
  for (const candidate of candidates) {
    const parsed = Number(candidate)
    if (Number.isFinite(parsed)) {
      return parsed
    }
  }
  return undefined
}

const resolveRefundRawMessage = (error: any): string => {
  const text = String(
    error?.msg || error?.message || error?.data?.msg || error?.response?.data?.msg || error?.response?.data?.message || ''
  ).trim()
  return text
}

const buildRefundErrorMessage = (error: any) => {
  const rawMessage = resolveRefundRawMessage(error)
  const code = resolveRefundErrorCode(error)
  const rawMessageUpper = rawMessage.toUpperCase()
  if (code === REFUND_LIMIT_BUNDLE_CHILD_FULFILLED_CODE) {
    const hint = '退款失败：命中子项台账优先校验，存在已履约子项且超出可退上限。'
    return rawMessage ? `${hint} 原始信息：${rawMessage}` : hint
  }
  if (code === BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT_CODE || rawMessageUpper.includes('BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT')) {
    const hint = '退款失败：检测到退款回调幂等冲突，请核对是否已绑定其它退款单。'
    return rawMessage ? `${hint} 原始信息：${rawMessage}` : hint
  }
  const merchantRefundInvalid = code === BOOKING_ORDER_REFUND_NOTIFY_ORDER_ID_INVALID_CODE
    || rawMessageUpper.includes('BOOKING_ORDER_REFUND_NOTIFY_ORDER_ID_INVALID')
    || (rawMessageUpper.includes('MERCHANTREFUNDID') && (rawMessageUpper.includes('INVALID') || rawMessage.includes('不合法') || rawMessage.includes('非法')))
  if (merchantRefundInvalid) {
    const hint = '退款失败：merchantRefundId 非法，请检查退款单号格式后重试。'
    return rawMessage ? `${hint} 原始信息：${rawMessage}` : hint
  }
  return rawMessage || '退款失败'
}

/** 获得 userType 颜色 */
const getUserTypeColor = (type: number) => {
  const dict = getDictObj(DICT_TYPE.USER_TYPE, type)
  switch (dict?.colorType) {
    case 'success':
      return '#67C23A'
    case 'info':
      return '#909399'
    case 'warning':
      return '#E6A23C'
    case 'danger':
      return '#F56C6C'
  }
  return '#409EFF'
}

/** 获得详情 */
const getDetail = async () => {
  const id = params.id as unknown as number
  if (id) {
    const res = await AfterSaleApi.getAfterSale(id)
    // 没有表单信息则关闭页面返回
    if (res == null) {
      message.notifyError('售后订单不存在')
      close()
    }
    formData.value = res
  }
}

/** 同意售后 */
const agree = async () => {
  try {
    // 二次确认
    await message.confirm('是否同意售后？')
    await AfterSaleApi.agree(formData.value.id)
    // 提示成功
    message.success(t('common.success'))
    await getDetail()
  } catch {}
}

/** 拒绝售后 */
const disagree = async () => {
  updateAuditReasonFormRef.value?.open(formData.value)
}

/** 确认收货 */
const receive = async () => {
  try {
    // 二次确认
    await message.confirm('是否确认收货？')
    await AfterSaleApi.receive(formData.value.id)
    // 提示成功
    message.success(t('common.success'))
    await getDetail()
  } catch {}
}

/** 拒绝收货 */
const refuse = async () => {
  try {
    // 二次确认
    await message.confirm('是否拒绝收货？')
    await AfterSaleApi.refuse(formData.value.id)
    // 提示成功
    message.success(t('common.success'))
    await getDetail()
  } catch {}
}

/** 确认退款 */
const refund = async () => {
  try {
    // 二次确认
    await message.confirm('是否确认退款？')
    await AfterSaleApi.refund(formData.value.id)
    // 提示成功
    message.success(t('common.success'))
    await getDetail()
  } catch (error: any) {
    if (error !== 'cancel') {
      message.error(buildRefundErrorMessage(error))
    }
  }
}

/** 图片预览 */
const imagePreview = (args) => {
  const urlList = []
  if (isArray(args)) {
    args.forEach((item) => {
      urlList.push(item.url)
    })
  } else {
    urlList.push(args)
  }
  createImageViewer({
    urlList
  })
}
const { delView } = useTagsViewStore() // 视图操作
/** 关闭 tag */
const close = () => {
  delView(unref(currentRoute))
  push({ name: 'TradeAfterSale' })
}
onMounted(async () => {
  await getDetail()
})
</script>
<style lang="scss" scoped>
:deep(.el-descriptions) {
  &:not(:nth-child(1)) {
    margin-top: 20px;
  }

  .el-descriptions__title {
    display: flex;
    align-items: center;

    &::before {
      display: inline-block;
      width: 3px;
      height: 20px;
      margin-right: 10px;
      background-color: #409eff;
      content: '';
    }
  }

  .el-descriptions-item__container {
    margin: 0 10px;

    .no-colon {
      margin: 0;

      &::after {
        content: '';
      }
    }
  }
}

// 时间线样式调整
:deep(.el-timeline) {
  margin: 10px 0 0 160px;

  .el-timeline-item__wrapper {
    position: relative;
    top: -20px;

    .el-timeline-item__timestamp {
      position: absolute !important;
      top: 10px;
      left: -150px;
    }
  }

  .el-timeline-right-content {
    display: flex;
    align-items: center;
    min-height: 30px;
    padding: 10px;
    background-color: var(--app-content-bg-color);

    &::before {
      position: absolute;
      top: 10px;
      left: 13px;
      border-color: transparent var(--app-content-bg-color) transparent transparent; /* 尖角颜色，左侧朝向 */
      border-style: solid;
      border-width: 8px; /* 调整尖角大小 */
      content: '';
    }
  }

  .dot-node-style {
    position: absolute;
    left: -5px;
    display: flex;
    width: 20px;
    height: 20px;
    font-size: 10px;
    color: #fff;
    border-radius: 50%;
    justify-content: center;
    align-items: center;
  }
}

.refund-limit-detail {
  display: flex;
  flex-direction: column;
  gap: 6px;

  .refund-limit-detail__row {
    line-height: 20px;
  }

  .refund-limit-detail__key {
    color: var(--el-text-color-secondary);
  }
}
</style>
