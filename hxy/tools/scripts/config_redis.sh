#!/bin/bash

# ============================================
# CRMEB足疗预约系统 - Redis配置脚本
# ============================================

echo "=========================================="
echo "开始配置Redis"
echo "=========================================="

# 检查Redis是否安装
echo "1. 检查Redis是否安装..."
which redis-server > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "✅ Redis已安装"
    redis-server --version
else
    echo "❌ Redis未安装，开始安装..."
    
    # CentOS
    if [ -f /etc/redhat-release ]; then
        yum install -y redis
    # Ubuntu
    elif [ -f /etc/lsb-release ]; then
        apt-get update
        apt-get install -y redis-server
    else
        echo "❌ 不支持的操作系统"
        exit 1
    fi
fi

# 检查Redis是否运行
echo ""
echo "2. 检查Redis服务状态..."
systemctl status redis | grep "active (running)"
if [ $? -eq 0 ]; then
    echo "✅ Redis服务正在运行"
else
    echo "⚠️  Redis服务未运行，尝试启动..."
    systemctl start redis
    systemctl enable redis
    
    sleep 2
    
    systemctl status redis | grep "active (running)"
    if [ $? -eq 0 ]; then
        echo "✅ Redis服务启动成功"
    else
        echo "❌ Redis服务启动失败"
        exit 1
    fi
fi

# 测试Redis连接
echo ""
echo "3. 测试Redis连接..."
redis-cli ping > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "✅ Redis连接成功"
else
    echo "❌ Redis连接失败"
    exit 1
fi

# 配置application.yml
echo ""
echo "4. 配置application.yml..."

APP_YML="/root/crmeb-java/crmeb_java/crmeb/crmeb-admin/src/main/resources/application.yml"

if [ -f "$APP_YML" ]; then
    echo "✅ 找到配置文件: $APP_YML"
    
    # 备份原配置
    cp $APP_YML ${APP_YML}.backup.$(date +%Y%m%d_%H%M%S)
    echo "✅ 配置文件已备份"
    
    # 检查是否已有Redis配置
    grep "spring.redis" $APP_YML > /dev/null 2>&1
    if [ $? -eq 0 ]; then
        echo "✅ Redis配置已存在"
    else
        echo "⚠️  Redis配置不存在，需要手动添加"
        echo ""
        echo "请在 $APP_YML 中添加以下配置："
        echo ""
        echo "spring:"
        echo "  redis:"
        echo "    host: localhost"
        echo "    port: 6379"
        echo "    database: 0"
        echo "    timeout: 3000"
        echo "    lettuce:"
        echo "      pool:"
        echo "        max-active: 8"
        echo "        max-wait: -1"
        echo "        max-idle: 8"
        echo "        min-idle: 0"
    fi
else
    echo "❌ 配置文件不存在: $APP_YML"
fi

# 测试Redis性能
echo ""
echo "5. 测试Redis性能..."
redis-cli --intrinsic-latency 1 | head -1

# 查看Redis信息
echo ""
echo "6. Redis信息..."
redis-cli info server | grep "redis_version"
redis-cli info memory | grep "used_memory_human"

echo ""
echo "=========================================="
echo "Redis配置完成！"
echo "=========================================="
echo ""
echo "下一步："
echo "1. 启动应用"
echo "2. 运行集成测试"


