#!/bin/bash

# ============================================
# CRMEB足疗预约系统 - 数据库初始化脚本
# ============================================

echo "=========================================="
echo "开始执行数据库初始化"
echo "=========================================="

# 配置信息
DB_HOST="localhost"
DB_PORT="3306"
DB_NAME="crmeb_java"
DB_USER="root"
DB_PASS=""  # 请填写你的MySQL密码

SQL_FILE="/root/crmeb-java/hxy/database/database_migration_v1.0.sql"

# 检查MySQL是否运行
echo "1. 检查MySQL服务状态..."
systemctl status mysqld | grep "active (running)"
if [ $? -eq 0 ]; then
    echo "✅ MySQL服务正在运行"
else
    echo "❌ MySQL服务未运行，尝试启动..."
    systemctl start mysqld
fi

# 检查数据库是否存在
echo ""
echo "2. 检查数据库是否存在..."
mysql -h$DB_HOST -P$DB_PORT -u$DB_USER -p$DB_PASS -e "USE $DB_NAME;" 2>/dev/null
if [ $? -eq 0 ]; then
    echo "✅ 数据库 $DB_NAME 已存在"
else
    echo "❌ 数据库 $DB_NAME 不存在，请先创建数据库"
    echo "执行命令: CREATE DATABASE $DB_NAME DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    exit 1
fi

# 检查SQL文件是否存在
echo ""
echo "3. 检查SQL文件..."
if [ -f "$SQL_FILE" ]; then
    echo "✅ SQL文件存在: $SQL_FILE"
else
    echo "❌ SQL文件不存在: $SQL_FILE"
    exit 1
fi

# 备份现有数据库
echo ""
echo "4. 备份现有数据库..."
BACKUP_DIR="/backup/mysql"
mkdir -p $BACKUP_DIR
BACKUP_FILE="$BACKUP_DIR/${DB_NAME}_$(date +%Y%m%d_%H%M%S).sql"
mysqldump -h$DB_HOST -P$DB_PORT -u$DB_USER -p$DB_PASS $DB_NAME > $BACKUP_FILE
if [ $? -eq 0 ]; then
    echo "✅ 数据库备份成功: $BACKUP_FILE"
else
    echo "⚠️  数据库备份失败，但继续执行"
fi

# 执行SQL脚本
echo ""
echo "5. 执行SQL脚本..."
mysql -h$DB_HOST -P$DB_PORT -u$DB_USER -p$DB_PASS $DB_NAME < $SQL_FILE
if [ $? -eq 0 ]; then
    echo "✅ SQL脚本执行成功"
else
    echo "❌ SQL脚本执行失败"
    exit 1
fi

# 验证表是否创建成功
echo ""
echo "6. 验证表结构..."

TABLES=(
    "eb_technician"
    "eb_technician_schedule"
    "eb_booking_order"
    "eb_member_card"
    "eb_user_member_card"
    "eb_member_card_usage"
    "eb_stock_flow"
    "eb_offpeak_rule"
)

for table in "${TABLES[@]}"; do
    mysql -h$DB_HOST -P$DB_PORT -u$DB_USER -p$DB_PASS $DB_NAME -e "DESC $table;" > /dev/null 2>&1
    if [ $? -eq 0 ]; then
        echo "✅ 表 $table 创建成功"
    else
        echo "❌ 表 $table 创建失败"
    fi
done

# 检查库存字段扩展
echo ""
echo "7. 验证库存字段扩展..."
mysql -h$DB_HOST -P$DB_PORT -u$DB_USER -p$DB_PASS $DB_NAME -e "DESC eb_store_product_sku;" | grep "available_stock"
if [ $? -eq 0 ]; then
    echo "✅ 库存字段扩展成功"
else
    echo "❌ 库存字段扩展失败"
fi

echo ""
echo "=========================================="
echo "数据库初始化完成！"
echo "=========================================="
echo ""
echo "下一步："
echo "1. 配置Redis"
echo "2. 启动应用"
echo "3. 访问 http://localhost:8080/swagger-ui.html"


