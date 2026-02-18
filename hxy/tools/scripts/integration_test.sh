#!/bin/bash

# ============================================
# CRMEB足疗预约系统 - 集成测试脚本
# ============================================

echo "=========================================="
echo "开始执行集成测试"
echo "=========================================="

BASE_URL="http://localhost:8080"

# 检查应用是否运行
echo "1. 检查应用状态..."
curl -s $BASE_URL/actuator/health > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "✅ 应用正在运行"
else
    echo "❌ 应用未运行，请先启动应用"
    echo "启动命令: cd /root/crmeb-java/crmeb_java/crmeb && mvn spring-boot:run"
    exit 1
fi

# 测试技师管理API
echo ""
echo "2. 测试技师管理API..."
response=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/admin/technician/list?storeId=1")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" == "200" ]; then
    echo "✅ 技师列表查询成功"
else
    echo "❌ 技师列表查询失败 (HTTP $http_code)"
fi

# 测试排班管理API
echo ""
echo "3. 测试排班管理API..."
response=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/admin/schedule/store?storeId=1&startDate=2026-02-14&endDate=2026-02-20")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" == "200" ]; then
    echo "✅ 排班列表查询成功"
else
    echo "❌ 排班列表查询失败 (HTTP $http_code)"
fi

# 测试健康检查
echo ""
echo "4. 测试健康检查..."
health=$(curl -s "$BASE_URL/actuator/health" | grep -o '"status":"UP"')
if [ -n "$health" ]; then
    echo "✅ 健康检查通过"
else
    echo "❌ 健康检查失败"
fi

# 运行单元测试
echo ""
echo "5. 运行单元测试..."
cd /root/crmeb-java/crmeb_java/crmeb
mvn test -Dtest=DistributedLockUtilTest
if [ $? -eq 0 ]; then
    echo "✅ 分布式锁测试通过"
else
    echo "❌ 分布式锁测试失败"
fi

mvn test -Dtest=TimeSlotServiceTest
if [ $? -eq 0 ]; then
    echo "✅ 时间槽服务测试通过"
else
    echo "❌ 时间槽服务测试失败"
fi

mvn test -Dtest=StockServiceTest
if [ $? -eq 0 ]; then
    echo "✅ 库存服务测试通过"
else
    echo "❌ 库存服务测试失败"
fi

# 压力测试
echo ""
echo "6. 执行压力测试..."
which ab > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "执行并发测试（100并发，1000请求）..."
    ab -n 1000 -c 100 "$BASE_URL/api/admin/technician/list?storeId=1" > /tmp/ab_result.txt 2>&1
    
    # 提取关键指标
    echo "测试结果："
    grep "Requests per second" /tmp/ab_result.txt
    grep "Time per request" /tmp/ab_result.txt | head -1
    grep "Failed requests" /tmp/ab_result.txt
else
    echo "⚠️  ab工具未安装，跳过压力测试"
    echo "安装命令: yum install httpd-tools"
fi

echo ""
echo "=========================================="
echo "集成测试完成！"
echo "=========================================="
echo ""
echo "下一步："
echo "1. 查看测试报告"
echo "2. 开始UniApp前端开发"


