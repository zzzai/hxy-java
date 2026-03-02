-- HXY: 修复小程序 social-login 500 的 member_user 表结构问题
-- 触发条件：
-- 1) 缺少 register_terminal 字段
-- 2) mobile 字段 NOT NULL 且无默认值，社交登录创建“未绑手机用户”时插入失败

SET NAMES utf8mb4;

-- 1) register_terminal 字段补齐（幂等）
SET @exists_register_terminal := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'member_user'
    AND COLUMN_NAME = 'register_terminal'
);
SET @sql_register_terminal := IF(
  @exists_register_terminal = 0,
  'ALTER TABLE member_user ADD COLUMN register_terminal TINYINT NULL COMMENT ''注册终端'' AFTER register_ip',
  'SELECT ''member_user.register_terminal already exists'' AS msg'
);
PREPARE stmt_register_terminal FROM @sql_register_terminal;
EXECUTE stmt_register_terminal;
DEALLOCATE PREPARE stmt_register_terminal;

-- 2) mobile 字段允许为空（社交登录初次注册无手机号场景）
ALTER TABLE member_user
  MODIFY COLUMN mobile VARCHAR(11) NULL COMMENT '手机号';

-- 3) 验证输出
SHOW COLUMNS FROM member_user LIKE 'register_terminal';
SHOW COLUMNS FROM member_user LIKE 'mobile';
