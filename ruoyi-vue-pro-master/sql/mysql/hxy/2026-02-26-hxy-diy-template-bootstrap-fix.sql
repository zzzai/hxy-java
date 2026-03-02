-- HXY: 修复商城装修模板初始化失败（小程序首页模板为空）
-- 场景：
-- 1) promotion_diy_template / promotion_diy_page 的 property 字段为 varchar(255)，不足以保存页面 JSON；
-- 2) 后台创建装修模板报 500，前端 /app-api/promotion/diy-template/used 长期返回 null。

SET NAMES utf8mb4;

-- 1) 修复表结构（兼容已有数据）
ALTER TABLE promotion_diy_template
  MODIFY COLUMN used bit(1) NOT NULL DEFAULT b'0',
  MODIFY COLUMN used_time datetime NULL,
  MODIFY COLUMN preview_pic_urls text NULL,
  MODIFY COLUMN property longtext NOT NULL;

ALTER TABLE promotion_diy_page
  MODIFY COLUMN preview_pic_urls text NULL,
  MODIFY COLUMN property longtext NULL;

-- 2) 若无模板则创建一条默认模板（tenant_id=1）
INSERT INTO promotion_diy_template
  (name, used, used_time, remark, preview_pic_urls, property, creator, updater, deleted, tenant_id)
SELECT
  '默认模板', b'1', NOW(), 'hxy bootstrap', '[]', '{}', 'hxy_bootstrap', 'hxy_bootstrap', b'0', 1
WHERE NOT EXISTS (
  SELECT 1 FROM promotion_diy_template WHERE tenant_id = 1 AND deleted = b'0'
);

-- 3) 选择 tenant_id=1 的模板作为当前使用模板（优先已使用，再按 ID）
SET @template_id := (
  SELECT id
  FROM promotion_diy_template
  WHERE tenant_id = 1 AND deleted = b'0'
  ORDER BY used DESC, id ASC
  LIMIT 1
);

UPDATE promotion_diy_template
SET used = b'0',
    used_time = NULL
WHERE tenant_id = 1
  AND deleted = b'0';

UPDATE promotion_diy_template
SET used = b'1',
    used_time = NOW()
WHERE id = @template_id;

-- 4) 补齐默认页面：首页 + 我的（若不存在）
INSERT INTO promotion_diy_page
  (template_id, name, remark, preview_pic_urls, property, creator, updater, deleted, tenant_id)
SELECT
  @template_id, '首页', 'hxy bootstrap', '[]', '{}', 'hxy_bootstrap', 'hxy_bootstrap', b'0', 1
WHERE @template_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM promotion_diy_page
    WHERE template_id = @template_id
      AND tenant_id = 1
      AND deleted = b'0'
      AND name = '首页'
  );

INSERT INTO promotion_diy_page
  (template_id, name, remark, preview_pic_urls, property, creator, updater, deleted, tenant_id)
SELECT
  @template_id, '我的', 'hxy bootstrap', '[]', '{}', 'hxy_bootstrap', 'hxy_bootstrap', b'0', 1
WHERE @template_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM promotion_diy_page
    WHERE template_id = @template_id
      AND tenant_id = 1
      AND deleted = b'0'
      AND name = '我的'
  );

-- 5) 验证输出
SELECT id, name, used, used_time, tenant_id
FROM promotion_diy_template
WHERE tenant_id = 1
  AND deleted = b'0'
ORDER BY id;

SELECT id, template_id, name, tenant_id
FROM promotion_diy_page
WHERE template_id = @template_id
  AND tenant_id = 1
  AND deleted = b'0'
ORDER BY id;
