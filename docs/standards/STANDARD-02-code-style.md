# Java 代码风格规范

**版本**: v1.0  
**适用范围**: 所有 Java 代码

---

## 1. 文件组织

### 1.1 目录结构

```
src/main/java/com/aidev/
├── api/                          # 接口层
│   ├── controller/              # Controller
│   ├── dto/                     # DTO（Request/Response）
│   ├── mapper/                  # DTO-Entity 转换
│   └── exception/               # 全局异常处理
│
├── application/                  # 应用层
│   ├── service/                 # Application Service
│   ├── event/                   # 事件处理器
│   ├── port/                    # 端口（输入/输出接口）
│   └── dto/                     # 应用层 DTO
│
├── domain/                       # 领域层【核心】
│   ├── model/                   # 领域模型
│   │   ├── aggregate/          # 聚合根
│   │   ├── entity/             # 实体
│   │   └── valueobject/        # 值对象
│   ├── service/                # 领域服务
│   ├── event/                  # 领域事件
│   ├── repository/             # 仓储接口
│   └── exception/              # 领域异常
│
└── infrastructure/              # 基础设施层
    ├── config/                 # 配置类
    ├── persistence/            # 数据持久化
    │   ├── entity/            # JPA Entity
    │   └── repository/        # 仓储实现
    ├── adapter/               # 外部适配器
    │   ├── claudecode/       # Claude Code
    │   └── kimicode/         # Kimi Code
    └── external/              # 外部服务
```

### 1.2 包命名

```java
// ✅ 正确
package com.aidev.workflow.domain.model.aggregate;
package com.aidev.task.application.service;

// ❌ 错误
package com.aidev.workflow.domainModel;  // 使用驼峰
package com.aidev.utils;                 // 过于笼统
```

---

## 2. 命名规范

### 2.1 类名

| 类型 | 规范 | 示例 |
|------|------|------|
| 普通类 | 大驼峰，名词 | `WorkflowEngine`, `TaskExecutor` |
| 接口 | 大驼峰，形容词/名词 | `CodeAdapter`, `TaskRepository` |
| 抽象类 | 大驼峰，以 `Abstract` 开头 | `AbstractTaskProcessor` |
| 异常类 | 大驼峰，以 `Exception` 结尾 | `WorkflowException` |
| 工具类 | 大驼峰，以 `Utils` 结尾 | `StringUtils` |
| 枚举 | 大驼峰，名词 | `TaskStatus`, `NodeType` |
| 测试类 | 被测类名 + `Test` | `WorkflowEngineTest` |

### 2.2 方法名

```java
// ✅ 正确
public WorkflowExecution startWorkflow(String workflowId) { }
public boolean isRunning() { }
public List<Task> findByStatus(TaskStatus status) { }

// ❌ 错误
public WorkflowExecution StartWorkflow() { }  // 首字母大写
public boolean running() { }                   // 不是动词
public List<Task> getTasksByStatus() { }      // 过于笼统
```

### 2.3 变量名

```java
// ✅ 正确
private String workflowId;
private final TaskRepository taskRepository;
private static final int MAX_RETRY_COUNT = 3;

// ❌ 错误
private String ID;                    // 非静态常量全大写
private TaskRepository repo;          // 缩写不清晰
private int max;                      // 含义不明
```

### 2.4 常量名

```java
// ✅ 正确
public static final int DEFAULT_TIMEOUT = 30;
public static final String STATUS_PENDING = "PENDING";

// ❌ 错误
public static final int defaultTimeout = 30;  // 小写
public static final int MAX = 100;            // 含义不明
```

---

## 3. 代码格式

### 3.1 缩进和空格

```java
// ✅ 正确
public class WorkflowEngine {
    private static final int POOL_SIZE = 10;
    
    public WorkflowExecution start(String id) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }
        return execute(id);
    }
}

// ❌ 错误
public class WorkflowEngine{
  private static final int POOL_SIZE=10;  // 等号两边无空格
    
    public WorkflowExecution start(String id){
        if(id==null){                       // if 后无空格
            throw new IllegalArgumentException("ID cannot be null");
        }
        return execute(id);
    }
}
```

### 3.2 行宽和换行

```java
// ✅ 正确（行宽 ≤ 120）
public WorkflowExecution startWorkflow(
        String workflowId,
        Map<String, String> inputs,
        ExecutionOptions options) throws WorkflowException {
    // ...
}

// 链式调用换行
List<Task> tasks = taskRepository
    .findByExecutionId(executionId)
    .stream()
    .filter(t -> t.getStatus() == TaskStatus.PENDING)
    .collect(Collectors.toList());

// ❌ 错误（超长）
public WorkflowExecution startWorkflow(String workflowId, Map<String, String> inputs, ExecutionOptions options) throws WorkflowException { }
```

### 3.3 空行使用

```java
public class WorkflowEngine {
    
    // 类变量和构造函数之间空一行
    private final TaskRepository repository;
    
    public WorkflowEngine(TaskRepository repository) {
        this.repository = repository;
    }
    
    // 方法之间空一行
    public WorkflowExecution start(String id) {
        // ...
    }
    
    public void stop(String id) {
        // ...
    }
    
}
```

---

## 4. 注释规范

### 4.1 JavaDoc 要求

```java
/**
 * 工作流引擎，负责 DAG 的解析和调度执行。
 * 
 * <p>核心职责：
 * <ul>
 *   <li>解析工作流定义</li>
 *   <li>执行拓扑排序</li>
 *   <li>调度任务执行</li>
 * </ul>
 * 
 * <p>使用示例：
 * <pre>
 * WorkflowEngine engine = new WorkflowEngine(repository);
 * WorkflowExecution execution = engine.startWorkflow("wf-001", inputs);
 * </pre>
 *
 * @author 张三
 * @since 1.0
 * @see WorkflowScheduler
 * @see TaskExecutor
 */
public class WorkflowEngine {
    
    /**
     * 启动工作流执行。
     *
     * @param workflowId 工作流 ID，不能为空
     * @param inputs 输入参数，可以为空
     * @return 执行实例，包含执行 ID 和状态
     * @throws WorkflowException 当工作流不存在或启动失败时抛出
     * @throws IllegalArgumentException 当 workflowId 为空时抛出
     */
    public WorkflowExecution startWorkflow(String workflowId, Map<String, String> inputs) 
            throws WorkflowException {
        // ...
    }
}
```

### 4.2 行内注释

```java
// ✅ 正确
// 计算节点的入度
int inDegree = calculateInDegree(node);

// 如果入度为 0，加入就绪队列
if (inDegree == 0) {
    readyQueue.offer(node);
}

// ❌ 错误
int inDegree = calculateInDegree(node); // 计算入度（ obvious 注释）
```

---

## 5. DDD 编码规范

### 5.1 领域层原则

```java
// ✅ 正确：领域层纯净，无外部依赖
package com.aidev.workflow.domain.model.aggregate;

public class Workflow {
    private WorkflowId id;
    private String name;
    private List<Node> nodes;
    
    // 领域行为
    public void addNode(Node node) {
        validateNode(node);
        this.nodes.add(node);
    }
    
    private void validateNode(Node node) {
        // 业务规则验证
    }
}

// ❌ 错误：领域层依赖 Spring
@Entity  // 基础设施关注点
public class Workflow {
    @Autowired // 领域层不能依赖 Spring
    private WorkflowRepository repository;
}
```

### 5.2 依赖注入

```java
// ✅ 正确：构造函数注入
@Service
public class WorkflowAppService {
    private final WorkflowRepository repository;
    private final DomainEventPublisher publisher;
    
    public WorkflowAppService(WorkflowRepository repository, 
                              DomainEventPublisher publisher) {
        this.repository = Objects.requireNonNull(repository);
        this.publisher = Objects.requireNonNull(publisher);
    }
}

// ❌ 错误：字段注入
@Service
public class WorkflowAppService {
    @Autowired  // 难以测试
    private WorkflowRepository repository;
}
```

### 5.3 仓储模式

```java
// 领域层：定义接口
package com.aidev.workflow.domain.repository;

public interface WorkflowRepository {
    Optional<Workflow> findById(WorkflowId id);
    List<Workflow> findAll();
    Workflow save(Workflow workflow);
    void delete(WorkflowId id);
}

// 基础设施层：实现
package com.aidev.workflow.infrastructure.persistence.repository;

@Repository
public class WorkflowJpaRepository implements WorkflowRepository {
    private final WorkflowJpaEntityRepository jpaRepository;
    
    @Override
    public Workflow save(Workflow workflow) {
        // 转换并保存
    }
}
```

---

## 6. 异常处理

### 6.1 自定义异常

```java
// 领域异常
public class WorkflowException extends RuntimeException {
    private final String errorCode;
    
    public WorkflowException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public WorkflowException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}

// 具体异常
public class WorkflowNotFoundException extends WorkflowException {
    public WorkflowNotFoundException(String workflowId) {
        super("WF-001", "Workflow not found: " + workflowId);
    }
}
```

### 6.2 异常使用

```java
// ✅ 正确
public Workflow findById(String id) {
    return repository.findById(id)
        .orElseThrow(() -> new WorkflowNotFoundException(id));
}

// 参数校验
public void addNode(Node node) {
    Assert.notNull(node, "Node must not be null");
    Assert.hasText(node.getId(), "Node ID must not be empty");
    // ...
}

// ❌ 错误
if (node == null) {
    return null;  // 隐式错误处理
}
```

---

## 7. 集合与流

### 7.1 不可变集合

```java
// ✅ 正确：返回不可变视图
public List<Node> getNodes() {
    return Collections.unmodifiableList(nodes);
}

// 或者使用拷贝
public List<Node> getNodes() {
    return new ArrayList<>(nodes);
}

// ❌ 错误：直接返回内部集合
public List<Node> getNodes() {
    return nodes;  // 外部可修改
}
```

### 7.2 Stream 使用

```java
// ✅ 正确
List<String> readyTaskIds = tasks.stream()
    .filter(t -> t.getStatus() == TaskStatus.READY)
    .map(Task::getId)
    .collect(Collectors.toList());

// 并行流（谨慎使用）
List<Result> results = tasks.parallelStream()
    .map(this::process)
    .collect(Collectors.toList());

// ❌ 错误：过度使用流
// 简单操作使用循环更清晰
for (Task task : tasks) {
    if (task.getStatus() == TaskStatus.READY) {
        readyTasks.add(task);
    }
}
```

---

## 8. 日志规范

### 8.1 日志级别

| 级别 | 使用场景 |
|------|---------|
| ERROR | 系统错误，需要立即处理 |
| WARN | 警告，可能需要关注 |
| INFO | 重要业务事件 |
| DEBUG | 调试信息，开发使用 |
| TRACE | 详细跟踪，性能敏感 |

### 8.2 日志格式

```java
// ✅ 正确
private static final Logger logger = LoggerFactory.getLogger(WorkflowEngine.class);

public void execute(Task task) {
    logger.info("Starting task execution: taskId={}, type={}", 
        task.getId(), task.getType());
    
    try {
        // 执行
        logger.debug("Task {} executed successfully", task.getId());
    } catch (Exception e) {
        logger.error("Task execution failed: taskId={}", task.getId(), e);
    }
}

// ❌ 错误
logger.info("Starting task execution: " + task.getId());  // 字符串拼接
logger.info("Task executed");  // 信息不足
```

---

## 9. 测试规范

### 9.1 测试类结构

```java
@DisplayName("工作流引擎测试")
class WorkflowEngineTest {
    
    private WorkflowEngine engine;
    private WorkflowRepository repository;
    
    @BeforeEach
    void setUp() {
        repository = mock(WorkflowRepository.class);
        engine = new WorkflowEngine(repository);
    }
    
    @Nested
    @DisplayName("拓扑排序")
    class TopologicalSortTests {
        
        @Test
        @DisplayName("应该正确排序简单依赖")
        void shouldSortSimpleDependencies() {
            // Given
            DAG dag = createSimpleDAG();
            
            // When
            List<Node> result = engine.topologicalSort(dag);
            
            // Then
            assertThat(result)
                .extracting(Node::getId)
                .containsExactly("A", "B", "C");
        }
    }
}
```

### 9.2 测试命名

```java
// ✅ 正确
shouldReturnEmptyListWhenNoTasksFound
shouldThrowExceptionWhenCycleExists
shouldUpdateStatusToRunning

// ❌ 错误
test1
testTopologicalSort
testExecute
```

---

## 10. Checkstyle 配置要点

```xml
<!-- checkstyle.xml 关键配置 -->
<module name="Checker">
    <!-- 文件长度 -->
    <module name="FileLength">
        <property name="max" value="500"/>
    </module>
    
    <!-- 行长度 -->
    <module name="LineLength">
        <property name="max" value="120"/>
    </module>
    
    <module name="TreeWalker">
        <!-- 方法长度 -->
        <module name="MethodLength">
            <property name="max" value="50"/>
        </module>
        
        <!-- 参数数量 -->
        <module name="ParameterNumber">
            <property name="max" value="5"/>
        </module>
        
        <!-- 圈复杂度 -->
        <module name="CyclomaticComplexity">
            <property name="max" value="10"/>
        </module>
        
        <!-- 导入检查 -->
        <module name="AvoidStarImport"/>
        <module name="UnusedImports"/>
        
        <!-- 命名检查 -->
        <module name="TypeName"/>
        <module name="MethodName"/>
        <module name="LocalVariableName"/>
    </module>
</module>
```

---

**最后更新**: 2024年
**版本**: v1.0
