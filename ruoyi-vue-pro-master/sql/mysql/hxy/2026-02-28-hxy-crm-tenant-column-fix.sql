-- HXY: CRM tenant column fix
-- Reason: multi-tenant interceptor appends tenant_id condition for CRM queries
SET NAMES utf8mb4;

ALTER TABLE `crm_business` ADD COLUMN `tenant_id` bigint NOT NULL DEFAULT 0;
ALTER TABLE `crm_business_product` ADD COLUMN `tenant_id` bigint NOT NULL DEFAULT 0;
ALTER TABLE `crm_business_status` ADD COLUMN `tenant_id` bigint NOT NULL DEFAULT 0;
ALTER TABLE `crm_business_status_type` ADD COLUMN `tenant_id` bigint NOT NULL DEFAULT 0;
ALTER TABLE `crm_clue` ADD COLUMN `tenant_id` bigint NOT NULL DEFAULT 0;
ALTER TABLE `crm_contact` ADD COLUMN `tenant_id` bigint NOT NULL DEFAULT 0;
ALTER TABLE `crm_contact_business` ADD COLUMN `tenant_id` bigint NOT NULL DEFAULT 0;
ALTER TABLE `crm_contract` ADD COLUMN `tenant_id` bigint NOT NULL DEFAULT 0;
ALTER TABLE `crm_contract_config` ADD COLUMN `tenant_id` bigint NOT NULL DEFAULT 0;
ALTER TABLE `crm_contract_product` ADD COLUMN `tenant_id` bigint NOT NULL DEFAULT 0;
ALTER TABLE `crm_customer` ADD COLUMN `tenant_id` bigint NOT NULL DEFAULT 0;
ALTER TABLE `crm_customer_limit_config` ADD COLUMN `tenant_id` bigint NOT NULL DEFAULT 0;
ALTER TABLE `crm_customer_pool_config` ADD COLUMN `tenant_id` bigint NOT NULL DEFAULT 0;
ALTER TABLE `crm_follow_up_record` ADD COLUMN `tenant_id` bigint NOT NULL DEFAULT 0;
ALTER TABLE `crm_permission` ADD COLUMN `tenant_id` bigint NOT NULL DEFAULT 0;
ALTER TABLE `crm_product` ADD COLUMN `tenant_id` bigint NOT NULL DEFAULT 0;
ALTER TABLE `crm_product_category` ADD COLUMN `tenant_id` bigint NOT NULL DEFAULT 0;
ALTER TABLE `crm_receivable` ADD COLUMN `tenant_id` bigint NOT NULL DEFAULT 0;
ALTER TABLE `crm_receivable_plan` ADD COLUMN `tenant_id` bigint NOT NULL DEFAULT 0;
