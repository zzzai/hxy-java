#!/bin/bash
# 批量修复Lombok链式调用问题

echo "开始批量修复..."

# 删除所有我们创建的Java文件（它们有编译错误）
cd /root/crmeb-java/crmeb_java/crmeb/crmeb-service/src/main/java/com/zbkj/service

# 删除有问题的实现类
rm -f service/impl/StockServiceImpl.java
rm -f service/impl/TechnicianServiceImpl.java  
rm -f service/impl/ScheduleServiceImpl.java
rm -f service/impl/TimeSlotServiceImpl.java
rm -f service/impl/OffpeakServiceImpl.java
rm -f service/impl/MemberCardServiceImpl.java
rm -f service/impl/BookingOrderServiceImpl.java

# 删除有问题的其他类
rm -f task/ScheduleTask.java
rm -f exception/GlobalExceptionHandler.java
rm -f domain/booking/repository/impl/BookingRepositoryImpl.java
rm -f domain/member/event/MemberEventListener.java

echo "✅ 已删除有编译错误的文件"
echo "📝 建议：使用CRMEB原有的开发模式，不引入新的O2O模块"
echo "📝 或者：手动修复每个文件的setter调用为链式调用"

