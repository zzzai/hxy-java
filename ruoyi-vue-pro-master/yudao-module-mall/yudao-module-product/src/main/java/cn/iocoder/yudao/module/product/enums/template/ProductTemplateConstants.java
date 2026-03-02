package cn.iocoder.yudao.module.product.enums.template;

/**
 * 商品模板相关常量
 */
public interface ProductTemplateConstants {

    int ATTR_ROLE_SPU_ATTR = 1;
    int ATTR_ROLE_SKU_SPEC = 2;
    int ATTR_ROLE_SKU_ATTR = 3;
    int ATTR_ROLE_SALE_ATTR = 4;

    int DATA_TYPE_STRING = 1;
    int DATA_TYPE_NUMBER = 2;
    int DATA_TYPE_BOOLEAN = 3;
    int DATA_TYPE_ENUM = 4;
    int DATA_TYPE_MULTI_ENUM = 5;
    int DATA_TYPE_DATE = 6;
    int DATA_TYPE_DATETIME = 7;
    int DATA_TYPE_JSON = 8;

    int TASK_MODE_PREVIEW = 1;
    int TASK_MODE_COMMIT = 2;

    int TASK_STATUS_PENDING = 0;
    int TASK_STATUS_RUNNING = 1;
    int TASK_STATUS_SUCCESS = 2;
    int TASK_STATUS_PARTIAL_SUCCESS = 3;
    int TASK_STATUS_FAIL = 4;

    int TASK_ITEM_STATUS_PENDING = 0;
    int TASK_ITEM_STATUS_SUCCESS = 1;
    int TASK_ITEM_STATUS_FAIL = 2;
    int TASK_ITEM_STATUS_SKIPPED = 3;

    int TEMPLATE_STATUS_DRAFT = 0;
    int TEMPLATE_STATUS_PUBLISHED = 1;
    int TEMPLATE_STATUS_ARCHIVED = 2;
}
