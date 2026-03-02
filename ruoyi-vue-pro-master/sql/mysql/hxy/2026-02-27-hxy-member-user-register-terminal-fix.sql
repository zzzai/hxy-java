-- HXY: 修复小程序社交登录 500（member_user 缺少 register_terminal 字段）
-- 症状：
-- /app-api/member/auth/social-login 返回 {"code":500,"msg":"系统异常"}
-- 根因：
-- member_user 插入时 SQL 包含 register_terminal，库表缺字段导致
-- Unknown column 'register_terminal' in 'field list'

SET NAMES utf8mb4;

SET @exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'member_user'
    AND COLUMN_NAME = 'register_terminal'
);

SET @sql := IF(
  @exists = 0,
  'ALTER TABLE member_user ADD COLUMN register_terminal TINYINT NULL COMMENT ''注册终端'' AFTER register_ip',
  'SELECT ''member_user.register_terminal already exists'' AS msg'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 验证
SHOW COLUMNS FROM member_user LIKE 'register_terminal';
