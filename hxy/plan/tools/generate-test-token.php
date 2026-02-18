<?php
/**
 * 生成测试Token脚本
 * 用于测试会员接口
 */

// 引入ThinkPHP框架
require_once '/root/crmeb/CRMEB-5.6.3.1/crmeb/vendor/autoload.php';

// 启动应用
$app = new think\App();
$app->initialize();

// 生成测试token
$uid = 1; // 使用uid=1的用户
$type = 'wechat';
$token = md5(uniqid() . time() . $uid);

echo "=== 测试Token生成 ===\n";
echo "UID: {$uid}\n";
echo "Token: {$token}\n";
echo "\n=== 测试命令 ===\n";
echo "curl -X GET \"http://127.0.0.1:8000/api/v2/member/info\" \\\n";
echo "  -H \"Content-Type: application/json\" \\\n";
echo "  -H \"Authori-zation: Bearer {$token}\"\n";
echo "\n";
echo "curl -X GET \"http://127.0.0.1:8000/api/v2/member/benefits\" \\\n";
echo "  -H \"Content-Type: application/json\" \\\n";
echo "  -H \"Authori-zation: Bearer {$token}\"\n";
echo "\n=== 简化测试（使用uid参数模拟） ===\n";
echo "curl -X GET \"http://127.0.0.1:8000/api/v2/member/info?test_uid={$uid}\"\n";
echo "curl -X GET \"http://127.0.0.1:8000/api/v2/member/benefits?test_uid={$uid}\"\n";


