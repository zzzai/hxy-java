SET NAMES utf8mb4;

-- HXY: 门店管理预置数据（兼容当前 v1 结构）
-- 目标：开箱可用，分类/标签/标签组（通过 group_name）可直接展示

SET @tenant_id := 1;

-- 修正历史预置行，统一到当前标签组体系
UPDATE hxy_store_category
SET name = '直营门店',
    status = 1,
    sort = 100
WHERE tenant_id = @tenant_id
  AND code = 'DIRECT';

UPDATE hxy_store_tag
SET name = '商圈店',
    group_name = '客群定位',
    status = 1,
    sort = 95
WHERE tenant_id = @tenant_id
  AND code = 'CBD';

-- 1) 预置门店分类
INSERT INTO hxy_store_category (code, name, status, sort, remark, tenant_id)
SELECT 'DIRECT', '直营门店', 1, 100, '总部直营门店', @tenant_id
WHERE NOT EXISTS (
    SELECT 1 FROM hxy_store_category WHERE tenant_id = @tenant_id AND code = 'DIRECT'
);

INSERT INTO hxy_store_category (code, name, status, sort, remark, tenant_id)
SELECT 'JOINT', '联营门店', 1, 90, '联营合作门店', @tenant_id
WHERE NOT EXISTS (
    SELECT 1 FROM hxy_store_category WHERE tenant_id = @tenant_id AND code = 'JOINT'
);

INSERT INTO hxy_store_category (code, name, status, sort, remark, tenant_id)
SELECT 'COMMUNITY', '社区门店', 1, 80, '社区周边门店', @tenant_id
WHERE NOT EXISTS (
    SELECT 1 FROM hxy_store_category WHERE tenant_id = @tenant_id AND code = 'COMMUNITY'
);

INSERT INTO hxy_store_category (code, name, status, sort, remark, tenant_id)
SELECT 'CBD', '商圈门店', 1, 70, '核心商圈门店', @tenant_id
WHERE NOT EXISTS (
    SELECT 1 FROM hxy_store_category WHERE tenant_id = @tenant_id AND code = 'CBD'
);

INSERT INTO hxy_store_category (code, name, status, sort, remark, tenant_id)
SELECT 'MEDI_WELLNESS', '医养结合门店', 1, 60, '医养协同服务门店', @tenant_id
WHERE NOT EXISTS (
    SELECT 1 FROM hxy_store_category WHERE tenant_id = @tenant_id AND code = 'MEDI_WELLNESS'
);

-- 2) 预置门店标签（group_name 即标签组）
INSERT INTO hxy_store_tag (code, name, group_name, status, sort, remark, tenant_id)
SELECT 'BIZ_DIRECT', '直营', '经营属性', 1, 100, '总部直营属性', @tenant_id
WHERE NOT EXISTS (
    SELECT 1 FROM hxy_store_tag WHERE tenant_id = @tenant_id AND code = 'BIZ_DIRECT'
);

INSERT INTO hxy_store_tag (code, name, group_name, status, sort, remark, tenant_id)
SELECT 'BIZ_JOINT', '联营', '经营属性', 1, 90, '联营合作属性', @tenant_id
WHERE NOT EXISTS (
    SELECT 1 FROM hxy_store_tag WHERE tenant_id = @tenant_id AND code = 'BIZ_JOINT'
);

INSERT INTO hxy_store_tag (code, name, group_name, status, sort, remark, tenant_id)
SELECT 'CAP_STORE', '到店服务', '履约类型', 1, 100, '支持到店履约', @tenant_id
WHERE NOT EXISTS (
    SELECT 1 FROM hxy_store_tag WHERE tenant_id = @tenant_id AND code = 'CAP_STORE'
);

INSERT INTO hxy_store_tag (code, name, group_name, status, sort, remark, tenant_id)
SELECT 'CAP_HOME', '到家服务', '履约类型', 1, 90, '支持到家履约', @tenant_id
WHERE NOT EXISTS (
    SELECT 1 FROM hxy_store_tag WHERE tenant_id = @tenant_id AND code = 'CAP_HOME'
);

INSERT INTO hxy_store_tag (code, name, group_name, status, sort, remark, tenant_id)
SELECT 'CAP_NIGHT', '夜间服务', '服务能力', 1, 80, '支持夜间服务时段', @tenant_id
WHERE NOT EXISTS (
    SELECT 1 FROM hxy_store_tag WHERE tenant_id = @tenant_id AND code = 'CAP_NIGHT'
);

INSERT INTO hxy_store_tag (code, name, group_name, status, sort, remark, tenant_id)
SELECT 'CAP_REHAB', '康复理疗', '服务能力', 1, 70, '具备康复理疗能力', @tenant_id
WHERE NOT EXISTS (
    SELECT 1 FROM hxy_store_tag WHERE tenant_id = @tenant_id AND code = 'CAP_REHAB'
);

INSERT INTO hxy_store_tag (code, name, group_name, status, sort, remark, tenant_id)
SELECT 'SEG_OFFICE', '白领客群', '客群定位', 1, 100, '以办公人群为主', @tenant_id
WHERE NOT EXISTS (
    SELECT 1 FROM hxy_store_tag WHERE tenant_id = @tenant_id AND code = 'SEG_OFFICE'
);

INSERT INTO hxy_store_tag (code, name, group_name, status, sort, remark, tenant_id)
SELECT 'SEG_FAMILY', '家庭客群', '客群定位', 1, 90, '以社区家庭为主', @tenant_id
WHERE NOT EXISTS (
    SELECT 1 FROM hxy_store_tag WHERE tenant_id = @tenant_id AND code = 'SEG_FAMILY'
);

INSERT INTO hxy_store_tag (code, name, group_name, status, sort, remark, tenant_id)
SELECT 'SEG_SENIOR', '银发客群', '客群定位', 1, 80, '以中老年客群为主', @tenant_id
WHERE NOT EXISTS (
    SELECT 1 FROM hxy_store_tag WHERE tenant_id = @tenant_id AND code = 'SEG_SENIOR'
);
