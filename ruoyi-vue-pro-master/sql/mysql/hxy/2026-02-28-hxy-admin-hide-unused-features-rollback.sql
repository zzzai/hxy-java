-- HXY: 回滚“隐藏不使用模块菜单”

SET NAMES utf8mb4;

UPDATE system_menu
SET visible = b'1',
    status = 0,
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
