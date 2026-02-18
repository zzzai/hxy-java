# 荷小悦O2O系统_技术实施手册_V2.0

## 元数据
- 来源文件: external/raw/hxyo2o/荷小悦O2O系统_技术实施手册_V2.0.docx
- 来源类型: docx
- SHA256: e645794f909d2a5ea93c824023210c729c97d19ac68d17ba635ea1c5d5458ae5
- 转换时间: 2026-02-14 10:02:24 +0800

## 提取正文
**荷小悦O2O多门店系统  
技术实施手册**

版本：V2.0 技术实施版  
日期：2026年02月12日  
文档类型：开发手册  
包含：完整SQL脚本、代码示例、配置指南

# 一、完整建表SQL脚本

## 1.1 新增核心表

**技师基础信息表 (eb_technician)**

-- 技师基础信息表  
CREATE TABLE \`eb_technician\` (  
\`id\` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '技师ID',  
\`store_id\` int(11) NOT NULL COMMENT '所属门店ID',  
\`name\` varchar(50) NOT NULL COMMENT '技师姓名',  
\`avatar\` varchar(255) DEFAULT '' COMMENT '技师头像URL',  
\`level\` tinyint(1) DEFAULT 1 COMMENT '技师等级：1=初级 2=中级 3=高级
4=首席',  
\`service_years\` decimal(4,1) DEFAULT 0.0 COMMENT '从业年限',  
\`skill_tags\` varchar(255) DEFAULT '' COMMENT '技能标签（逗号分隔）',  
\`intro\` text COMMENT '技师介绍',  
\`rating\` decimal(3,2) DEFAULT 5.00 COMMENT '评分（5.00满分）',  
\`order_count\` int(11) DEFAULT 0 COMMENT '累计服务单数',  
\`status\` tinyint(1) DEFAULT 1 COMMENT '状态：1=在职 2=离职',  
\`created_at\` int(11) DEFAULT 0 COMMENT '创建时间',  
\`updated_at\` int(11) DEFAULT 0 COMMENT '更新时间',  
PRIMARY KEY (\`id\`),  
KEY \`idx_store\` (\`store_id\`),  
KEY \`idx_status\` (\`status\`)  
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技师基础信息表';

**技师排班表 (eb_technician_schedule)**

-- 技师排班表  
CREATE TABLE \`eb_technician_schedule\` (  
\`id\` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '排班ID',  
\`store_id\` int(11) NOT NULL COMMENT '门店ID',  
\`technician_id\` int(11) NOT NULL COMMENT '技师ID',  
\`service_sku_id\` int(11) NOT NULL COMMENT '服务SKU ID',  
\`work_date\` date NOT NULL COMMENT '排班日期',  
\`time_slots\` json NOT NULL COMMENT '时间槽JSON',  
\`total_slots\` int(11) DEFAULT 0 COMMENT '总时间槽数',  
\`available_slots\` int(11) DEFAULT 0 COMMENT '可预约时间槽数',  
\`status\` tinyint(1) DEFAULT 1 COMMENT '排班状态：1=正常 2=请假
3=已完成',  
\`is_offpeak_enabled\` tinyint(1) DEFAULT 0 COMMENT
'是否启用闲时优惠',  
\`created_at\` int(11) DEFAULT 0,  
\`updated_at\` int(11) DEFAULT 0,  
PRIMARY KEY (\`id\`),  
KEY \`idx_store_date\` (\`store_id\`,\`work_date\`),  
KEY \`idx_technician_date\` (\`technician_id\`,\`work_date\`),  
KEY \`idx_sku\` (\`service_sku_id\`)  
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技师排班表';

**库存变动流水表 (eb_stock_flow)**

-- 库存变动流水表  
CREATE TABLE \`eb_stock_flow\` (  
\`id\` bigint(20) unsigned NOT NULL AUTO_INCREMENT,  
\`store_id\` int(11) NOT NULL COMMENT '门店ID',  
\`sku_id\` int(11) NOT NULL COMMENT 'SKU ID',  
\`order_id\` bigint(20) DEFAULT 0 COMMENT '关联订单ID',  
\`change_type\` tinyint(1) NOT NULL COMMENT '变动类型：1=入库 2=锁定
3=释放 4=扣减 5=退款',  
\`change_quantity\` int(11) NOT NULL COMMENT '变动数量',  
\`before_available\` int(11) DEFAULT 0 COMMENT '变动前可售库存',  
\`after_available\` int(11) DEFAULT 0 COMMENT '变动后可售库存',  
\`before_locked\` int(11) DEFAULT 0 COMMENT '变动前锁定库存',  
\`after_locked\` int(11) DEFAULT 0 COMMENT '变动后锁定库存',  
\`operator_id\` int(11) DEFAULT 0 COMMENT '操作人ID',  
\`operator_type\` tinyint(1) DEFAULT 1 COMMENT '操作人类型：1=系统
2=店长 3=用户',  
\`remark\` varchar(255) DEFAULT '' COMMENT '备注',  
\`created_at\` int(11) DEFAULT 0,  
PRIMARY KEY (\`id\`),  
KEY \`idx_store_sku\` (\`store_id\`,\`sku_id\`),  
KEY \`idx_order\` (\`order_id\`),  
KEY \`idx_created\` (\`created_at\`)  
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存变动流水表';

## 1.2 现有表字段补充

**门店SKU定价表补充库存字段**

-- 门店SKU定价表补充库存字段  
ALTER TABLE \`eb_store_service_sku\`  
ADD COLUMN \`available_stock\` int(11) DEFAULT 0 COMMENT '可售库存',  
ADD COLUMN \`locked_stock\` int(11) DEFAULT 0 COMMENT '锁定库存',  
ADD COLUMN \`sold_stock\` int(11) DEFAULT 0 COMMENT '已售库存',  
ADD COLUMN \`total_stock\` int(11) DEFAULT 0 COMMENT '总库存',  
ADD COLUMN \`low_stock_threshold\` int(11) DEFAULT 10 COMMENT
'低库存阈值';

**订单子项表补充核销字段**

-- 订单子项表补充核销字段  
ALTER TABLE \`eb_order_item\`  
ADD COLUMN \`total_quantity\` int(11) NOT NULL DEFAULT 1 COMMENT
'购买总数量',  
ADD COLUMN \`used_quantity\` int(11) DEFAULT 0 COMMENT '已核销数量',  
ADD COLUMN \`remaining_quantity\` int(11) NOT NULL DEFAULT 1 COMMENT
'剩余数量',  
ADD COLUMN \`verification_status\` tinyint(1) DEFAULT 0 COMMENT
'核销状态：0=未核销 1=部分核销 2=已完成',  
ADD COLUMN \`verification_records\` json DEFAULT NULL COMMENT
'核销记录JSON',  
ADD COLUMN \`expire_time\` int(11) DEFAULT 0 COMMENT '过期时间';

# 二、核心业务代码示例

## 2.1 库存锁定（Python示例）

import redis  
import time  
from contextlib import contextmanager  
  
redis_client = redis.StrictRedis(host='localhost', port=6379)  
  
@contextmanager  
def distributed_lock(lock_key, timeout=10):  
"""分布式锁上下文管理器"""  
lock_id = str(uuid.uuid4())  
acquired = redis_client.set(lock_key, lock_id, nx=True, ex=timeout)  
  
if not acquired:  
raise Exception("获取锁失败")  
  
try:  
yield  
finally:  
\# 使用Lua脚本确保只删除自己的锁  
lua_script = """  
if redis.call("get", KEYS\[1\]) == ARGV\[1\] then  
return redis.call("del", KEYS\[1\])  
else  
return 0  
end  
"""  
redis_client.eval(lua_script, 1, lock_key, lock_id)  
  
  
def lock_stock(store_id, sku_id, quantity, order_id):  
"""下单时锁定库存"""  
lock_key = f"stock_lock:{store_id}:{sku_id}"  
  
with distributed_lock(lock_key):  
\# 查询当前库存（数据库行锁）  
store_sku = db.query_one(  
"SELECT \* FROM eb_store_service_sku WHERE store_id = %s AND sku_id = %s
FOR UPDATE",  
\[store_id, sku_id\]  
)  
  
if store_sku.available_stock \< quantity:  
return False, "库存不足"  
  
\# 执行原子化更新  
affected_rows = db.execute(  
"""  
UPDATE eb_store_service_sku  
SET available_stock = available_stock - %s,  
locked_stock = locked_stock + %s  
WHERE store_id = %s AND sku_id = %s AND available_stock \>= %s  
""",  
\[quantity, quantity, store_id, sku_id, quantity\]  
)  
  
if affected_rows == 0:  
return False, "库存已被占用"  
  
\# 记录库存流水  
db.execute(  
"INSERT INTO eb_stock_flow (store_id, sku_id, order_id, change_type,
...) VALUES (...)",  
\[...\]  
)  
  
return True, "锁定成功"

## 2.2 时间槽预约（Python示例）

def book_time_slot(user_id, schedule_id, slot_id):  
"""用户预约时间槽"""  
lock_key = f"schedule_lock:{schedule_id}:{slot_id}"  
  
if not redis_client.set(lock_key, user_id, nx=True, ex=30):  
raise Exception("该时段正在被预约")  
  
try:  
\# 查询排班记录（行锁）  
schedule = db.query_one(  
"SELECT \* FROM eb_technician_schedule WHERE id = %s FOR UPDATE",  
\[schedule_id\]  
)  
  
time_slots = json.loads(schedule.time_slots)  
  
\# 找到目标时间槽  
for slot in time_slots\["slots"\]:  
if slot\["slot_id"\] == slot_id:  
\# 检查状态  
if slot\["status"\] == 2: \# 已锁定  
if time.time() \> slot\["locked_expire"\]:  
slot\["status"\] = 1 \# 过期，自动释放  
else:  
raise Exception("该时段已被锁定")  
  
if slot\["status"\] != 1:  
raise Exception("该时段不可预约")  
  
\# 锁定时间槽  
slot\["status"\] = 2  
slot\["locked_at"\] = int(time.time())  
slot\["locked_expire"\] = int(time.time()) + 1800  
break  
  
\# 更新数据库  
db.execute(  
"UPDATE eb_technician_schedule SET time_slots = %s, available_slots =
available_slots - 1 WHERE id = %s",  
\[json.dumps(time_slots), schedule_id\]  
)  
  
return {"schedule_id": schedule_id, "slot_id": slot_id, "locked_expire":
slot\["locked_expire"\]}  
  
finally:  
redis_client.delete(lock_key)

# 三、部署与配置指南

## 3.1 环境要求

| 组件   | 版本  | 说明                 |
|--------|-------|----------------------|
| MySQL  | 5.7+  | 支持JSON字段和行级锁 |
| Redis  | 5.0+  | 用于分布式锁和缓存   |
| Python | 3.8+  | 后端开发语言         |
| Nginx  | 1.18+ | 反向代理和负载均衡   |

## 3.2 Redis配置

\# redis.conf 关键配置  
  
\# 持久化配置  
save 900 1  
save 300 10  
save 60 10000  
  
\# 内存管理  
maxmemory 2gb  
maxmemory-policy allkeys-lru  
  
\# 连接配置  
timeout 300  
tcp-keepalive 60

