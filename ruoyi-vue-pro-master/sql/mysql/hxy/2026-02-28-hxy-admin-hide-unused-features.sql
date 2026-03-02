-- HXY: 隐藏当前阶段不使用的后台模块菜单
-- 说明：仅做“禁止展示 + 禁用状态”，不删除数据，后续可回滚恢复。

SET NAMES utf8mb4;

UPDATE system_menu
SET visible = b'0',
    status = 1,
    updater = '1',
    update_time = NOW()
WHERE deleted = b'0'
  AND (
      path IN ('bpm', 'report', 'mp', 'ai', 'iot')
      OR permission LIKE 'bpm:%'
      OR permission LIKE 'report:%'
      OR permission LIKE 'mp:%'
      OR permission LIKE 'ai:%'
      OR permission LIKE 'iot:%'
      OR component LIKE 'bpm/%'
      OR component LIKE 'report/%'
      OR component LIKE 'mp/%'
      OR component LIKE 'ai/%'
      OR component LIKE 'iot/%'
  );
