#!/bin/bash
# 荷小悦会员接口测试脚本
# 创建时间：2026-02-11

echo "=========================================="
echo "荷小悦会员接口测试"
echo "=========================================="
echo ""

# 配置
API_BASE="http://115.190.245.14:8088/api/v2"
TOKEN="your-token-here"  # 需要替换为真实token

echo "1. 测试会员信息接口"
echo "接口：GET /api/v2/member/info"
echo "------------------------------------------"
curl -X GET "${API_BASE}/member/info" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  2>/dev/null | python3 -m json.tool
echo ""
echo ""

echo "2. 测试会员权益接口"
echo "接口：GET /api/v2/member/benefits"
echo "------------------------------------------"
curl -X GET "${API_BASE}/member/benefits" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  2>/dev/null | python3 -m json.tool
echo ""
echo ""

echo "=========================================="
echo "测试完成"
echo "=========================================="
echo ""
echo "说明："
echo "1. 如果返回 '请登录'，说明需要先登录获取token"
echo "2. 如果返回 '用户不存在'，说明需要创建测试用户"
echo "3. 如果返回正常数据，说明接口工作正常"
echo ""
echo "获取token的方法："
echo "1. 使用小程序登录"
echo "2. 或使用 H5 登录"
echo "3. 从响应中获取 token 字段"

