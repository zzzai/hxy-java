-- HXY: 修复小程序装修页空模板导致首页/我的页白屏
-- 适用场景：
-- 1) /app-api/promotion/diy-template/used 返回 home={} / user={}
-- 2) promotion_diy_page.property 为 NULL、空串或 {}

SET NAMES utf8mb4;

-- 首页最小可渲染模板（TitleBar）
SET @home_page_property = JSON_OBJECT(
  'page', JSON_OBJECT('backgroundColor', '#f5f7fa'),
  'navigationBar', JSON_OBJECT(
    'styleType', 'normal',
    'type', 'normal',
    'title', '荷小悦',
    'color', '#303133',
    'alwaysShow', 1,
    'list', JSON_ARRAY()
  ),
  'components', JSON_ARRAY(
    JSON_OBJECT(
      'id', 'TitleBar',
      'property', JSON_OBJECT(
        'title', '欢迎使用荷小悦',
        'description', '首页模板待配置，请在管理后台完成装修',
        'textAlign', 'left',
        'titleColor', '#303133',
        'titleSize', 18,
        'titleWeight', '600',
        'descriptionColor', '#606266',
        'descriptionSize', 12,
        'descriptionWeight', '400',
        'marginLeft', 0,
        'height', 56,
        'bgImgUrl', '',
        'more', JSON_OBJECT('show', FALSE, 'type', 'text', 'text', '', 'url', ''),
        'style', JSON_OBJECT('marginTop', 0, 'marginBottom', 0)
      )
    )
  )
);

-- 我的最小可渲染模板（TitleBar）
SET @user_page_property = JSON_OBJECT(
  'page', JSON_OBJECT('backgroundColor', '#f5f7fa'),
  'navigationBar', JSON_OBJECT(
    'styleType', 'normal',
    'type', 'normal',
    'title', '我的',
    'color', '#303133',
    'alwaysShow', 1,
    'list', JSON_ARRAY()
  ),
  'components', JSON_ARRAY(
    JSON_OBJECT(
      'id', 'TitleBar',
      'property', JSON_OBJECT(
        'title', '个人中心',
        'description', '个人页模板待配置，请在管理后台完成装修',
        'textAlign', 'left',
        'titleColor', '#303133',
        'titleSize', 18,
        'titleWeight', '600',
        'descriptionColor', '#606266',
        'descriptionSize', 12,
        'descriptionWeight', '400',
        'marginLeft', 0,
        'height', 56,
        'bgImgUrl', '',
        'more', JSON_OBJECT('show', FALSE, 'type', 'text', 'text', '', 'url', ''),
        'style', JSON_OBJECT('marginTop', 0, 'marginBottom', 0)
      )
    )
  )
);

-- 仅修复当前“使用中模板”的 首页/我的 空配置，避免覆盖已配置数据
UPDATE promotion_diy_page p
JOIN promotion_diy_template t ON p.template_id = t.id
SET p.property = CASE
  WHEN p.name = '首页' THEN @home_page_property
  WHEN p.name = '我的' THEN @user_page_property
  ELSE p.property
END,
p.updater = 'hxy_bootstrap',
p.update_time = NOW()
WHERE p.deleted = b'0'
  AND t.deleted = b'0'
  AND t.used = b'1'
  AND p.name IN ('首页', '我的')
  AND (
    p.property IS NULL
    OR TRIM(p.property) = ''
    OR TRIM(p.property) = '{}'
  );

-- 验证输出
SELECT
  p.id,
  p.template_id,
  p.name,
  LEFT(p.property, 120) AS property_preview,
  p.update_time
FROM promotion_diy_page p
JOIN promotion_diy_template t ON p.template_id = t.id
WHERE p.deleted = b'0'
  AND t.deleted = b'0'
  AND t.used = b'1'
  AND p.name IN ('首页', '我的')
ORDER BY p.id;
