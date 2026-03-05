package cn.iocoder.yudao.module.product.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

/**
 * Product 错误码枚举类
 *
 * product 系统，使用 1-008-000-000 段
 */
public interface ErrorCodeConstants {

    // ========== 商品分类相关 1-008-001-000 ============
    ErrorCode CATEGORY_NOT_EXISTS = new ErrorCode(1_008_001_000, "商品分类不存在");
    ErrorCode CATEGORY_PARENT_NOT_EXISTS = new ErrorCode(1_008_001_001, "父分类不存在");
    ErrorCode CATEGORY_PARENT_NOT_FIRST_LEVEL = new ErrorCode(1_008_001_002, "父分类不能是二级分类");
    ErrorCode CATEGORY_EXISTS_CHILDREN = new ErrorCode(1_008_001_003, "存在子分类，无法删除");
    ErrorCode CATEGORY_DISABLED = new ErrorCode(1_008_001_004, "商品分类({})已禁用，无法使用");
    ErrorCode CATEGORY_HAVE_BIND_SPU = new ErrorCode(1_008_001_005, "类别下存在商品，无法删除");

    // ========== 商品品牌相关编号 1-008-002-000 ==========
    ErrorCode BRAND_NOT_EXISTS = new ErrorCode(1_008_002_000, "品牌不存在");
    ErrorCode BRAND_DISABLED = new ErrorCode(1_008_002_001, "品牌已禁用");
    ErrorCode BRAND_NAME_EXISTS = new ErrorCode(1_008_002_002, "品牌名称已存在");

    // ========== 商品属性项 1-008-003-000 ==========
    ErrorCode PROPERTY_NOT_EXISTS = new ErrorCode(1_008_003_000, "属性项不存在");
    ErrorCode PROPERTY_EXISTS = new ErrorCode(1_008_003_001, "属性项的名称已存在");
    ErrorCode PROPERTY_DELETE_FAIL_VALUE_EXISTS = new ErrorCode(1_008_003_002, "属性项下存在属性值，无法删除");

    // ========== 商品属性值 1-008-004-000 ==========
    ErrorCode PROPERTY_VALUE_NOT_EXISTS = new ErrorCode(1_008_004_000, "属性值不存在");
    ErrorCode PROPERTY_VALUE_EXISTS = new ErrorCode(1_008_004_001, "属性值的名称已存在");

    // ========== 商品 SPU 1-008-005-000 ==========
    ErrorCode SPU_NOT_EXISTS = new ErrorCode(1_008_005_000, "商品 SPU 不存在");
    ErrorCode SPU_SAVE_FAIL_CATEGORY_LEVEL_ERROR = new ErrorCode(1_008_005_001, "商品分类不正确，原因：必须使用第二级的商品分类及以下");
    ErrorCode SPU_SAVE_FAIL_COUPON_TEMPLATE_NOT_EXISTS = new ErrorCode(1_008_005_002, "商品 SPU 保存失败，原因：优惠劵不存在");
    ErrorCode SPU_NOT_ENABLE = new ErrorCode(1_008_005_003, "商品 SPU【{}】不处于上架状态");
    ErrorCode SPU_NOT_RECYCLE = new ErrorCode(1_008_005_004, "商品 SPU 不处于回收站状态");
    ErrorCode SPU_TYPE_MISMATCH = new ErrorCode(1_008_005_005, "商品 SPU 类型不匹配，当前类型【{}】，期望类型【{}】");
    ErrorCode SPU_TEMPLATE_VERSION_REQUIRED = new ErrorCode(1_008_005_006, "服务商品必须绑定模板版本");
    ErrorCode SPU_TEMPLATE_VERSION_NOT_FOUND = new ErrorCode(1_008_005_007, "商品模板版本不存在");
    ErrorCode SPU_TEMPLATE_VERSION_NOT_PUBLISHED = new ErrorCode(1_008_005_008, "商品模板版本未发布");
    ErrorCode SPU_TEMPLATE_VERSION_CATEGORY_MISMATCH = new ErrorCode(1_008_005_009, "商品模板版本与商品类目不匹配");

    // ========== 商品 SKU 1-008-006-000 ==========
    ErrorCode SKU_NOT_EXISTS = new ErrorCode(1_008_006_000, "商品 SKU 不存在");
    ErrorCode SKU_PROPERTIES_DUPLICATED = new ErrorCode(1_008_006_001, "商品 SKU 的属性组合存在重复");
    ErrorCode SPU_ATTR_NUMBERS_MUST_BE_EQUALS = new ErrorCode(1_008_006_002, "一个 SPU 下的每个 SKU，其属性项必须一致");
    ErrorCode SPU_SKU_NOT_DUPLICATE = new ErrorCode(1_008_006_003, "一个 SPU 下的每个 SKU，必须不重复");
    ErrorCode SKU_STOCK_NOT_ENOUGH = new ErrorCode(1_008_006_004, "商品 SKU 库存不足");
    ErrorCode SKU_TEMPLATE_VERSION_MISMATCH = new ErrorCode(1_008_006_005, "SKU 模板版本必须与 SPU 保持一致");

    // ========== 商品 评价 1-008-007-000 ==========
    ErrorCode COMMENT_NOT_EXISTS = new ErrorCode(1_008_007_000, "商品评价不存在");
    ErrorCode COMMENT_ORDER_EXISTS = new ErrorCode(1_008_007_001, "订单的商品评价已存在");

    // ========== 商品 收藏 1-008-008-000 ==========
    ErrorCode FAVORITE_EXISTS = new ErrorCode(1_008_008_000, "该商品已经被收藏");
    ErrorCode FAVORITE_NOT_EXISTS = new ErrorCode(1_008_008_001, "商品收藏不存在");

    // ========== 门店商品映射 1-008-009-000 ==========
    ErrorCode STORE_SPU_MAPPING_NOT_EXISTS = new ErrorCode(1_008_009_000, "门店 SPU 映射不存在");
    ErrorCode STORE_SKU_MAPPING_NOT_EXISTS = new ErrorCode(1_008_009_001, "门店 SKU 映射不存在");
    ErrorCode STORE_BATCH_TARGETS_EMPTY = new ErrorCode(1_008_009_002, "批量操作门店列表不能为空");
    ErrorCode STORE_SKU_BATCH_ADJUST_FIELDS_EMPTY = new ErrorCode(1_008_009_003, "批量调价/调库存至少需要设置一个字段");
    ErrorCode STORE_SKU_STOCK_BIZ_KEY_REQUIRED = new ErrorCode(1_008_009_004, "门店 SKU 库存变更业务标识不能为空");
    ErrorCode STORE_SKU_STOCK_BIZ_FIELD_TOO_LONG = new ErrorCode(1_008_009_005, "门店 SKU 库存变更业务标识过长：{} 最大长度 {}");
    ErrorCode STORE_SKU_STOCK_BIZ_KEY_CONFLICT = new ErrorCode(1_008_009_006, "门店 SKU 库存变更幂等冲突：业务键对应的库存变化值不一致");
    ErrorCode STORE_SKU_STOCK_MANUAL_BIZ_TYPE_INVALID = new ErrorCode(1_008_009_007, "门店库存人工调整业务类型非法：{}");
    ErrorCode STORE_SKU_STOCK_MANUAL_INCR_COUNT_INVALID = new ErrorCode(1_008_009_008, "门店库存人工调整数量非法：{}");
    ErrorCode STORE_SKU_STOCK_MANUAL_SKU_DUPLICATED = new ErrorCode(1_008_009_009, "门店库存人工调整存在重复 SKU：{}");
    ErrorCode STORE_SKU_STOCK_FLOW_TARGETS_EMPTY = new ErrorCode(1_008_009_010, "库存流水重试目标不能为空");
    ErrorCode STORE_SKU_STOCK_SERVICE_FORBIDDEN = new ErrorCode(1_008_009_011, "服务商品不允许走库存变更：SKU {}");

    // ========== 门店主数据 1-008-010-000 ==========
    ErrorCode STORE_NOT_EXISTS = new ErrorCode(1_008_010_000, "门店不存在");
    ErrorCode STORE_CODE_EXISTS = new ErrorCode(1_008_010_001, "门店编码已存在");
    ErrorCode STORE_HAS_PRODUCT_MAPPING = new ErrorCode(1_008_010_002, "门店已存在商品映射，无法删除");
    ErrorCode STORE_CATEGORY_NOT_EXISTS = new ErrorCode(1_008_010_003, "门店分类不存在");
    ErrorCode STORE_TAG_NOT_EXISTS = new ErrorCode(1_008_010_004, "门店标签不存在");

    // ========== 门店分类 1-008-011-000 ==========
    ErrorCode STORE_CATEGORY_CODE_EXISTS = new ErrorCode(1_008_011_000, "门店分类编码已存在");
    ErrorCode STORE_CATEGORY_NAME_EXISTS = new ErrorCode(1_008_011_001, "门店分类名称已存在");
    ErrorCode STORE_CATEGORY_HAS_STORE = new ErrorCode(1_008_011_002, "门店分类下存在门店，无法删除");

    // ========== 门店标签 1-008-012-000 ==========
    ErrorCode STORE_TAG_CODE_EXISTS = new ErrorCode(1_008_012_000, "门店标签编码已存在");
    ErrorCode STORE_TAG_NAME_EXISTS = new ErrorCode(1_008_012_001, "门店标签名称已存在");
    ErrorCode STORE_TAG_HAS_STORE = new ErrorCode(1_008_012_002, "门店标签已被门店使用，无法删除");

    // ========== 门店标签组 1-008-013-000 ==========
    ErrorCode STORE_TAG_GROUP_NOT_EXISTS = new ErrorCode(1_008_013_000, "门店标签组不存在");
    ErrorCode STORE_TAG_GROUP_CODE_EXISTS = new ErrorCode(1_008_013_001, "门店标签组编码已存在");
    ErrorCode STORE_TAG_GROUP_NAME_EXISTS = new ErrorCode(1_008_013_002, "门店标签组名称已存在");
    ErrorCode STORE_TAG_GROUP_HAS_TAG = new ErrorCode(1_008_013_003, "门店标签组下存在标签，无法删除");
    ErrorCode STORE_TAG_GROUP_REQUIRED_MISSING = new ErrorCode(1_008_013_004, "门店缺少必选标签组：{}");
    ErrorCode STORE_TAG_GROUP_MUTEX_CONFLICT = new ErrorCode(1_008_013_005, "门店标签组选项互斥冲突：{}");

    // ========== 门店生命周期 1-008-014-000 ==========
    ErrorCode STORE_LIFECYCLE_STATUS_INVALID = new ErrorCode(1_008_014_000, "门店生命周期状态非法");
    ErrorCode STORE_LIFECYCLE_TRANSITION_NOT_ALLOWED = new ErrorCode(1_008_014_001, "门店生命周期状态流转不允许：{} -> {}");
    ErrorCode STORE_LIFECYCLE_CLOSE_BLOCKED_BY_MAPPING = new ErrorCode(1_008_014_002, "门店存在商品映射，无法停用或闭店");
    ErrorCode STORE_LIFECYCLE_REASON_REQUIRED = new ErrorCode(1_008_014_003, "门店停业或闭店必须填写原因");
    ErrorCode STORE_LIFECYCLE_CLOSE_BLOCKED_BY_STOCK = new ErrorCode(1_008_014_004, "门店存在正库存，无法停业或闭店");
    ErrorCode STORE_LIFECYCLE_CLOSE_BLOCKED_BY_STOCK_FLOW = new ErrorCode(1_008_014_005, "门店存在库存流水待处理，无法停业或闭店");
    ErrorCode STORE_LIFECYCLE_CLOSE_BLOCKED_BY_PENDING_ORDER = new ErrorCode(1_008_014_006, "门店存在未结订单，无法停业或闭店");
    ErrorCode STORE_LIFECYCLE_CLOSE_BLOCKED_BY_INFLIGHT_TICKET = new ErrorCode(1_008_014_007, "门店存在在途售后工单，无法停业或闭店");
    ErrorCode STORE_LIFECYCLE_BATCH_LOG_NOT_EXISTS = new ErrorCode(1_008_014_008, "门店生命周期批次台账不存在");
    ErrorCode STORE_LIFECYCLE_BATCH_LOG_QUERY_REQUIRED = new ErrorCode(1_008_014_009, "批次复核入参错误：logId 或 batchNo 至少传一个");
    ErrorCode STORE_LIFECYCLE_RECHECK_LOG_NOT_EXISTS = new ErrorCode(1_008_014_010, "门店生命周期复核台账不存在");
    ErrorCode STORE_LIFECYCLE_CHANGE_ORDER_NOT_EXISTS = new ErrorCode(1_008_014_011, "门店生命周期变更单不存在");
    ErrorCode STORE_LIFECYCLE_CHANGE_ORDER_STATUS_INVALID = new ErrorCode(1_008_014_012, "门店生命周期变更单状态非法：当前 {}，期望 {}");
    ErrorCode STORE_LIFECYCLE_CHANGE_ORDER_GUARD_BLOCKED = new ErrorCode(1_008_014_013, "门店生命周期变更单守卫阻塞，无法审批通过");
    ErrorCode STORE_LIFECYCLE_CHANGE_ORDER_FROM_STATUS_CHANGED = new ErrorCode(1_008_014_014, "门店当前生命周期已变化，请刷新后重新创建变更单");

    // ========== 类目模板与 SKU 生成 1-008-015-000 ==========
    ErrorCode CATEGORY_TEMPLATE_NOT_EXISTS = new ErrorCode(1_008_015_000, "类目模板不存在");
    ErrorCode CATEGORY_TEMPLATE_VALIDATE_FAILED = new ErrorCode(1_008_015_001, "类目模板校验失败");
    ErrorCode SKU_GENERATE_COMBINATION_EXCEED_LIMIT = new ErrorCode(1_008_015_002, "SKU 规格组合数量超限");
    ErrorCode SKU_GENERATE_TASK_NOT_EXISTS = new ErrorCode(1_008_015_003, "SKU 生成任务不存在");
    ErrorCode SKU_GENERATE_TASK_STATUS_INVALID = new ErrorCode(1_008_015_004, "SKU 生成任务状态非法");
    ErrorCode SKU_GENERATE_COMMIT_IDEMPOTENT_HIT = new ErrorCode(1_008_015_005, "SKU 生成提交命中幂等");
    ErrorCode SKU_GENERATE_PREVIEW_TASK_REQUIRED = new ErrorCode(1_008_015_006, "提交前请先执行预览");
    ErrorCode CATEGORY_TEMPLATE_SKU_SPEC_DATA_TYPE_INVALID = new ErrorCode(1_008_015_007, "SKU 规格属性数据类型非法");
    ErrorCode CATEGORY_TEMPLATE_SERVICE_STOCK_AFFECT_FORBIDDEN = new ErrorCode(1_008_015_008, "服务类目不允许配置影响库存的属性");
    ErrorCode SKU_GENERATE_SPEC_SELECTION_EMPTY = new ErrorCode(1_008_015_009, "SKU 规格选项不能为空");
    ErrorCode SKU_GENERATE_COMMIT_PREVIEW_TASK_MISMATCH = new ErrorCode(1_008_015_010, "提交任务和预览任务不匹配");
    ErrorCode CATEGORY_TEMPLATE_SKU_SPEC_AFFECT_FLAG_INVALID = new ErrorCode(1_008_015_011, "SKU 规格属性必须至少影响价格或库存");
    ErrorCode CATEGORY_TEMPLATE_NON_SPEC_AFFECT_FORBIDDEN = new ErrorCode(1_008_015_012, "非 SKU 规格属性不允许影响价格或库存");
    ErrorCode CATEGORY_TEMPLATE_VERSION_SNAPSHOT_REQUIRED = new ErrorCode(1_008_015_013, "类目模板版本快照缺失");

}
