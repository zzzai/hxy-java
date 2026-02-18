# Java DDD代码规范

> **适用范围**: CRMEB Java DDD单体架构  
> **强制等级**: ⭐⭐⭐ 必须遵守

---

## 📁 包命名规范

### 领域层包结构

```
com.zbkj.service.domain.{context}/
├── model/                      # 聚合根、实体
│   ├── Booking.java           # 聚合根（首字母大写）
│   └── TimeSlot.java          # 实体
├── valueobject/               # 值对象
│   ├── BookingStatus.java     # 枚举值对象
│   └── Duration.java          # 类值对象
├── service/                   # 领域服务
│   └── TechnicianScheduleService.java
├── repository/                # 仓储接口
│   ├── BookingRepository.java
│   └── impl/
│       └── BookingRepositoryImpl.java
└── event/                     # 领域事件
    └── BookingCreatedEvent.java
```

---

## 🎯 聚合根规范

### 命名规范

```java
// ✅ 正确：名词，业务概念
public class Booking { }
public class Member { }
public class Order { }

// ❌ 错误：动词、技术术语
public class BookingManager { }
public class BookingEntity { }
```

### 结构规范

```java
public class Booking {
    // 1. 标识（必须）
    private Integer id;
    private String bookingNo;  // 业务唯一标识
    
    // 2. 基本属性
    private Integer userId;
    private Integer storeId;
    
    // 3. 值对象（优先使用）
    private BookingStatus status;  // ✅ 而非 private int status;
    private Duration duration;
    
    // 4. 时间戳
    private Date createTime;
    private Date updateTime;
    
    // 5. 构造方法（私有）
    private Booking() { }
    
    // 6. 工厂方法（静态）⭐
    public static Booking create(...) {
        validateBookingTime(bookingTime);  // 校验
        Booking booking = new Booking();
        // 初始化
        return booking;
    }
    
    // 7. 业务方法（动词）⭐
    public void confirm() { }
    public void cancel(String reason) { }
    public void startService() { }
    
    // 8. 查询方法
    public boolean canCancel() { }
    public boolean isExpired() { }
    
    // 9. 私有校验方法
    private static void validateBookingTime(Date time) { }
    
    // 10. Getter（必要时）
    public Integer getId() { return id; }
    public BookingStatus getStatus() { return status; }
    
    // 11. Setter（禁止！）
    // ❌ public void setStatus(int status) { }
}
```

### 业务方法规范

```java
// ✅ 正确：封装业务规则
public void cancel(String reason) {
    if (!this.status.canCancel()) {
        throw new IllegalStateException("当前状态不能取消预约");
    }
    this.status = BookingStatus.CANCELLED;
    this.cancelReason = reason;
    this.cancelTime = new Date();
}

// ❌ 错误：只是简单赋值
public void cancel(String reason) {
    this.status = BookingStatus.CANCELLED;
    this.cancelReason = reason;
}
```

---

## 🎯 值对象规范

### 枚举值对象

```java
public enum BookingStatus {
    PENDING(0, "待确认"),
    CONFIRMED(1, "已确认"),
    CANCELLED(4, "已取消");
    
    private final int code;
    private final String desc;
    
    BookingStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    
    // ✅ 业务方法
    public boolean canCancel() {
        return this == PENDING || this == CONFIRMED;
    }
    
    // ✅ 工厂方法
    public static BookingStatus of(int code) {
        for (BookingStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("无效的状态码: " + code);
    }
    
    // Getter
    public int getCode() { return code; }
    public String getDesc() { return desc; }
}
```

### 类值对象

```java
public class Duration {
    private final int minutes;  // ✅ final 不可变
    private static final int BUFFER_MINUTES = 10;
    
    // 私有构造
    private Duration(int minutes) {
        if (minutes <= 0 || minutes > 480) {
            throw new IllegalArgumentException("服务时长必须在1-480分钟之间");
        }
        this.minutes = minutes;
    }
    
    // ✅ 工厂方法
    public static Duration of(int minutes) {
        return new Duration(minutes);
    }
    
    // ✅ 业务方法
    public int getTotalMinutes() {
        return minutes + BUFFER_MINUTES;
    }
    
    // ✅ equals/hashCode（值对象必须）
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Duration)) return false;
        Duration duration = (Duration) o;
        return minutes == duration.minutes;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(minutes);
    }
}
```

---

## 🎯 领域服务规范

### 命名规范

```java
// ✅ 正确：{领域概念}Service
public class TechnicianScheduleService { }
public class PriceCalculationService { }

// ❌ 错误：太泛化
public class BookingService { }  // 应该是聚合根
public class CommonService { }
```

### 结构规范

```java
@Service
public class TechnicianScheduleService {
    @Autowired
    private TimeSlotRepository timeSlotRepository;
    
    /**
     * 生成时间槽
     * 
     * @param technicianId 技师ID
     * @param date 日期
     * @param duration 服务时长
     * @return 时间槽列表
     */
    public List<TimeSlot> generateTimeSlots(
        Integer technicianId, 
        Date date, 
        Duration duration
    ) {
        // 1. 参数校验
        validateParams(technicianId, date, duration);
        
        // 2. 业务逻辑
        List<TimeSlot> slots = new ArrayList<>();
        // ...
        
        // 3. 返回结果
        return slots;
    }
    
    // 私有方法
    private void validateParams(...) { }
}
```

---

## 🎯 仓储规范

### 接口规范

```java
public interface BookingRepository {
    // ✅ 保存/更新
    Booking save(Booking booking);
    
    // ✅ 查询单个
    Booking findById(Integer id);
    Booking findByBookingNo(String bookingNo);
    
    // ✅ 查询列表
    List<Booking> findByUserId(Integer userId, BookingStatus status);
    
    // ✅ 删除
    void deleteById(Integer id);
    
    // ❌ 错误：不要暴露技术细节
    // BookingPO selectById(Integer id);
}
```

### 实现规范

```java
@Repository
public class BookingRepositoryImpl implements BookingRepository {
    @Autowired
    private BookingDao bookingDao;
    
    @Override
    public Booking save(Booking booking) {
        BookingPO po = toPO(booking);  // 领域对象 → PO
        
        if (booking.getId() == null) {
            bookingDao.insert(po);
            booking.setId(po.getId());  // 回填ID
        } else {
            bookingDao.updateById(po);
        }
        
        return booking;
    }
    
    @Override
    public Booking findById(Integer id) {
        BookingPO po = bookingDao.selectById(id);
        return toDomain(po);  // PO → 领域对象
    }
    
    // ✅ 转换方法（私有）
    private BookingPO toPO(Booking booking) {
        BookingPO po = new BookingPO();
        po.setId(booking.getId());
        po.setBookingNo(booking.getBookingNo());
        po.setStatus(booking.getStatus().getCode());  // 枚举 → int
        // ...
        return po;
    }
    
    private Booking toDomain(BookingPO po) {
        if (po == null) return null;
        
        // 重建值对象
        BookingStatus status = BookingStatus.of(po.getStatus());
        Duration duration = Duration.of(po.getDurationMinutes());
        
        // 重建聚合根
        return Booking.reconstruct(
            po.getId(),
            po.getBookingNo(),
            // ...
            status,
            duration
        );
    }
}
```

---

## 🎯 Controller规范

### 结构规范

```java
@RestController
@RequestMapping("/api/front/booking")
public class BookingController {
    @Autowired
    private BookingRepository bookingRepository;
    
    @Autowired
    private TechnicianScheduleService scheduleService;
    
    /**
     * 创建预约
     */
    @PostMapping("/create")
    public CommonResult<BookingVO> createBooking(@RequestBody BookingCreateRequest request) {
        // 1. 参数校验（简单校验）
        if (request.getUserId() == null) {
            return CommonResult.failed("用户ID不能为空");
        }
        
        // 2. 创建值对象
        Duration duration = Duration.of(request.getDurationMinutes());
        TechnicianSkill skill = TechnicianSkill.of(request.getSkillLevel());
        
        // 3. 调用领域层（业务规则在这里）
        Booking booking = Booking.create(
            generateBookingNo(),
            request.getUserId(),
            request.getStoreId(),
            // ...
            duration,
            skill
        );
        
        // 4. 保存
        booking = bookingRepository.save(booking);
        
        // 5. 转换为VO返回
        BookingVO vo = toVO(booking);
        return CommonResult.success(vo);
    }
    
    // 私有方法
    private String generateBookingNo() {
        return "BK" + System.currentTimeMillis();
    }
    
    private BookingVO toVO(Booking booking) {
        BookingVO vo = new BookingVO();
        vo.setId(booking.getId());
        vo.setBookingNo(booking.getBookingNo());
        vo.setStatus(booking.getStatus().getCode());
        vo.setStatusDesc(booking.getStatus().getDesc());
        return vo;
    }
}
```

---

## 🎯 异常处理规范

### 领域异常

```java
// ✅ 自定义领域异常
public class BookingException extends RuntimeException {
    private final String errorCode;
    
    public BookingException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public static BookingException cannotCancel() {
        return new BookingException("BOOKING_001", "当前状态不能取消预约");
    }
    
    public static BookingException timeSlotOccupied() {
        return new BookingException("BOOKING_002", "该时间段已被预约");
    }
}
```

### 使用规范

```java
// ✅ 在聚合根中抛出
public void cancel(String reason) {
    if (!this.status.canCancel()) {
        throw BookingException.cannotCancel();
    }
    // ...
}

// ✅ 在Controller中捕获
@ExceptionHandler(BookingException.class)
public CommonResult<Void> handleBookingException(BookingException e) {
    return CommonResult.failed(e.getMessage());
}
```

---

## 🎯 注释规范

### 类注释

```java
/**
 * 预约聚合根
 * 
 * <p>职责：
 * <ul>
 *   <li>管理预约的完整生命周期</li>
 *   <li>封装预约相关的业务规则</li>
 *   <li>控制状态流转</li>
 * </ul>
 * 
 * @author 荷小悦架构团队
 * @since 2026-02-12
 */
public class Booking {
}
```

### 方法注释

```java
/**
 * 取消预约
 * 
 * <p>业务规则：
 * <ul>
 *   <li>只有"待确认"和"已确认"状态可以取消</li>
 *   <li>取消后释放时间槽</li>
 *   <li>记录取消原因和时间</li>
 * </ul>
 * 
 * @param reason 取消原因（必填）
 * @throws BookingException 当前状态不允许取消时抛出
 */
public void cancel(String reason) {
}
```

---

## 🎯 测试规范

### 单元测试（聚合根）

```java
public class BookingTest {
    
    @Test
    public void testCancel_Success() {
        // Given
        Booking booking = Booking.create(...);
        booking.confirm();
        
        // When
        booking.cancel("测试取消");
        
        // Then
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertEquals("测试取消", booking.getCancelReason());
        assertNotNull(booking.getCancelTime());
    }
    
    @Test(expected = BookingException.class)
    public void testCancel_Fail_WhenCompleted() {
        // Given
        Booking booking = Booking.create(...);
        booking.complete();
        
        // When
        booking.cancel("测试取消");  // 应该抛出异常
    }
}
```

---

## 📊 代码质量检查清单

### 聚合根检查

- [ ] 是否有工厂方法（create/reconstruct）？
- [ ] 是否封装了业务规则？
- [ ] 是否避免了public setter？
- [ ] 是否使用值对象替代原始类型？

### 值对象检查

- [ ] 是否不可变（final字段）？
- [ ] 是否实现了equals/hashCode？
- [ ] 是否有工厂方法？
- [ ] 是否包含业务方法？

### 仓储检查

- [ ] 接口是否返回领域对象？
- [ ] 实现是否隐藏了PO转换？
- [ ] 是否避免了技术细节泄露？

---

**文档版本**: v1.0  
**更新时间**: 2026-02-12


