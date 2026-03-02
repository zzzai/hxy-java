SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- HXY 佣金结算站内信模板（P1 预警 / P0 升级）
INSERT INTO `system_notify_template`
(`name`, `code`, `nickname`, `content`, `type`, `params`, `status`, `remark`, `creator`, `updater`, `deleted`)
SELECT 'HXY佣金结算P1预警',
       'hxy_booking_commission_p1_warn',
       '荷小悦系统',
       '结算单【{settlementNo}】审核即将超时，请在【{deadlineTime}】前处理。',
       2,
       '["settlementNo","deadlineTime"]',
       0,
       'booking 佣金结算 SLA 预警通知模板',
       'system',
       'system',
       b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM `system_notify_template`
    WHERE `code` = 'hxy_booking_commission_p1_warn' AND `deleted` = b'0'
);

INSERT INTO `system_notify_template`
(`name`, `code`, `nickname`, `content`, `type`, `params`, `status`, `remark`, `creator`, `updater`, `deleted`)
SELECT 'HXY佣金结算P0升级',
       'hxy_booking_commission_p0_escalate',
       '荷小悦系统',
       '结算单【{settlementNo}】审核已超时并升级P0，请立即处理。',
       2,
       '["settlementNo"]',
       0,
       'booking 佣金结算 SLA 升级通知模板',
       'system',
       'system',
       b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM `system_notify_template`
    WHERE `code` = 'hxy_booking_commission_p0_escalate' AND `deleted` = b'0'
);

SET FOREIGN_KEY_CHECKS = 1;
