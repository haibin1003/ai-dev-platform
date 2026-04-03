# DDD 实践规范

**版本**: v1.0  
**适用范围**: 领域模型设计

---

## 1. 分层架构

### 1.1 四层架构

```
┌─────────────────────────────────────────────────────────┐
│  User Interface (用户接口层)                             │
│  - Controller, DTO, Assembler                          │
├─────────────────────────────────────────────────────────┤
│  Application (应用层)                                   │
│  - Application Service, Event Handler, Port            │
├─────────────────────────────────────────────────────────┤
│  Domain (领域层) 【核心】                                │
│  - Entity, Value Object, Aggregate, Domain Service     │
│  - Domain Event, Repository Interface                  │
├─────────────────────────────────────────────────────────┤
│  Infrastructure (基础设施层)                             │
│  - Repository Implementation, External Service         │
│  - Message Queue, Cache, Database                      │
└─────────────────────────────────────────────────────────┘
```

### 1.2 依赖规则

```
User Interface → Application → Domain ← Infrastructure
                      ↑                      ↑
                      └────── Port ──────────┘
```

**铁律**:
- ✅ 上层可以调用下层
- ✅ 领域层定义接口，基础设施实现
- ❌ **严禁下层调用上层**
- ❌ **严禁领域层依赖任何框架**

---

## 2. 领域模型

### 2.1 实体 (Entity)

```java
// ✅ 正确：实体有唯一标识，通过 ID 判断相等
public class Task {
    private TaskId id;           // 唯一标识
    private TaskStatus status;   // 值对象
    private String prompt;
    
    // 构造函数
    public Task(TaskId id, String prompt) {
        this.id = Objects.requireNonNull(id);
        this.prompt = Objects.requireNonNull(prompt);
        this.status = TaskStatus.PENDING;
    }
    
    // 业务方法
    public void start() {
        if (status != TaskStatus.PENDING) {
            throw new IllegalStateException("Task must be pending to start");
        }
        this.status = TaskStatus.RUNNING;
        recordEvent(new TaskStartedEvent(id));
    }
    
    // 相等性基于 ID
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task)) return false;
        Task task = (Task) o;
        return Objects.equals(id, task.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
```

### 2.2 值对象 (Value Object)

```java
// ✅ 正确：值对象无标识，通过属性判断相等，不可变
public class TaskStatus {
    public static final TaskStatus PENDING = new TaskStatus("PENDING");
    public static final TaskStatus RUNNING = new TaskStatus("RUNNING");
    public static final TaskStatus COMPLETED = new TaskStatus("COMPLETED");
    public static final TaskStatus FAILED = new TaskStatus("FAILED");
    
    private final String value;
    
    private TaskStatus(String value) {
        this.value = value;
    }
    
    public static TaskStatus of(String value) {
        return switch (value) {
            case "PENDING" -> PENDING;
            case "RUNNING" -> RUNNING;
            case "COMPLETED" -> COMPLETED;
            case "FAILED" -> FAILED;
            default -> throw new IllegalArgumentException("Unknown status: " + value);
        };
    }
    
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED;
    }
    
    // 值对象相等性基于属性
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskStatus)) return false;
        TaskStatus that = (TaskStatus) o;
        return Objects.equals(value, that.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}

// ✅ 正确：值对象可以包含多个属性
public class ExecutionResult {
    private final String output;
    private final List<String> artifacts;
    private final long durationMs;
    private final LocalDateTime completedAt;
    
    public ExecutionResult(String output, List<String> artifacts, long durationMs) {
        this.output = Objects.requireNonNull(output);
        this.artifacts = List.copyOf(artifacts);  // 不可变
        this.durationMs = durationMs;
        this.completedAt = LocalDateTime.now();
    }
    
    // 只有 getter，无 setter
    public String getOutput() { return output; }
    public List<String> getArtifacts() { return artifacts; }
    public long getDurationMs() { return durationMs; }
}
```

### 2.3 聚合根 (Aggregate Root)

```java
// ✅ 正确：聚合根是实体，负责维护聚合边界内的一致性
public class Workflow {
    private WorkflowId id;
    private String name;
    private List<Node> nodes;          // 内部实体
    private List<Edge> edges;          // 内部实体
    private DomainEventCollection domainEvents;
    
    // 工厂方法
    public static Workflow create(String name, List<Node> nodes) {
        Workflow workflow = new Workflow();
        workflow.id = WorkflowId.generate();
        workflow.name = Objects.requireNonNull(name);
        workflow.nodes = new ArrayList<>();
        workflow.edges = new ArrayList<>();
        
        // 通过聚合根方法添加节点，确保一致性
        nodes.forEach(workflow::addNode);
        
        return workflow;
    }
    
    // 聚合根方法：维护内部一致性
    public void addNode(Node node) {
        validateNode(node);
        ensureUniqueNodeId(node);
        nodes.add(node);
        recordEvent(new NodeAddedEvent(id, node.getId()));
    }
    
    public void connect(String fromId, String toId) {
        validateConnection(fromId, toId);
        edges.add(new Edge(fromId, toId));
    }
    
    // 验证循环依赖
    public void validateNoCycles() {
        // 拓扑排序检查
        TopologicalSorter.sort(nodes, edges);
    }
    
    // 领域事件
    private void recordEvent(DomainEvent event) {
        domainEvents.add(event);
    }
    
    public List<DomainEvent> drainEvents() {
        return domainEvents.drain();
    }
    
    // 返回不可变视图
    public List<Node> getNodes() {
        return Collections.unmodifiableList(nodes);
    }
}
```

---

## 3. 领域服务

### 3.1 何时使用领域服务

```java
// ✅ 正确：跨聚合操作使用领域服务
public class WorkflowScheduler {
    
    public ExecutionPlan schedule(Workflow workflow, Map<String, String> inputs) {
        // 协调多个聚合
        List<Node> sortedNodes = topologicalSort(workflow.getNodes());
        
        ExecutionPlan plan = new ExecutionPlan(workflow.getId());
        for (Node node : sortedNodes) {
            Task task = createTask(node, inputs);
            plan.addTask(task);
        }
        
        return plan;
    }
    
    private List<Node> topologicalSort(List<Node> nodes) {
        // 算法实现
    }
}

// ❌ 错误：将本应属于实体的行为放入服务
public class TaskService {
    // 错误：start 应该是 Task 实体的方法
    public void startTask(Task task) {
        task.setStatus("RUNNING");
    }
}
```

### 3.2 领域服务 vs 应用服务

| 特性 | 领域服务 | 应用服务 |
|------|---------|---------|
| 职责 | 业务逻辑，跨聚合协调 | 用例编排，事务控制 |
| 依赖 | 仅依赖领域层 | 可依赖所有层 |
| 无状态 | 是 | 是 |
| 事务 | 不控制 | 控制 |

```java
// 领域服务：处理业务规则
public class TaskDependencyResolver {
    public List<Task> findReadyTasks(List<Task> allTasks) {
        return allTasks.stream()
            .filter(t -> t.getStatus() == TaskStatus.PENDING)
            .filter(t -> areDependenciesComplete(t, allTasks))
            .collect(Collectors.toList());
    }
    
    private boolean areDependenciesComplete(Task task, List<Task> allTasks) {
        // 业务逻辑
    }
}

// 应用服务：编排用例
@Service
@Transactional
public class WorkflowAppService {
    private final WorkflowRepository workflowRepo;
    private final TaskRepository taskRepo;
    private final TaskDependencyResolver resolver;
    private final DomainEventPublisher eventPublisher;
    
    public void executeReadyTasks(String workflowId) {
        Workflow workflow = workflowRepo.findById(workflowId)
            .orElseThrow(() -> new WorkflowNotFoundException(workflowId));
        
        List<Task> tasks = taskRepo.findByWorkflowId(workflowId);
        List<Task> readyTasks = resolver.findReadyTasks(tasks);
        
        readyTasks.forEach(task -> {
            task.start();
            taskRepo.save(task);
        });
        
        // 发布领域事件
        workflow.drainEvents().forEach(eventPublisher::publish);
    }
}
```

---

## 4. 仓储模式

### 4.1 仓储接口（领域层）

```java
// 领域层只定义接口
package com.aidev.workflow.domain.repository;

public interface WorkflowRepository {
    Optional<Workflow> findById(WorkflowId id);
    Optional<Workflow> findByCode(String code);
    List<Workflow> findAll();
    Workflow save(Workflow workflow);
    void delete(WorkflowId id);
    
    // 规格查询
    List<Workflow> findByStatus(WorkflowStatus status);
}
```

### 4.2 仓储实现（基础设施层）

```java
// 基础设施层实现
package com.aidev.workflow.infrastructure.persistence.repository;

@Repository
public class WorkflowJpaRepository implements WorkflowRepository {
    private final WorkflowJpaEntityRepository jpaRepo;
    private final WorkflowMapper mapper;
    
    @Override
    public Workflow findById(WorkflowId id) {
        return jpaRepo.findById(id.getValue())
            .map(mapper::toDomain)
            .orElse(null);
    }
    
    @Override
    public Workflow save(Workflow workflow) {
        WorkflowJpaEntity entity = mapper.toEntity(workflow);
        WorkflowJpaEntity saved = jpaRepo.save(entity);
        return mapper.toDomain(saved);
    }
}

// JPA Entity（基础设施层）
@Entity
@Table(name = "workflows")
class WorkflowJpaEntity {
    @Id
    private String id;
    private String code;
    private String name;
    private String status;
    
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NodeJpaEntity> nodes;
}
```

### 4.3 仓储 vs DAO

| 特性 | 仓储 | DAO |
|------|------|-----|
| 所在层 | 领域层定义，基础设施实现 | 基础设施层 |
| 操作对象 | 聚合根 | 数据表 |
| 方法语义 | 业务语义 | 数据操作 |
| 返回类型 | 领域对象 | 数据实体 |

```java
// ✅ 正确：仓储方法具有业务语义
workflowRepo.findByStatus(WorkflowStatus.ACTIVE);
workflowRepo.findRecentlyExecuted(int limit);

// ❌ 错误：DAO 风格
workflowRepo.findByStatusEquals("ACTIVE");
workflowRepo.findTop10ByOrderByLastExecutedDesc();
```

---

## 5. 领域事件

### 5.1 定义领域事件

```java
// 领域事件是不可变的
public class TaskStartedEvent implements DomainEvent {
    private final String taskId;
    private final String workflowId;
    private final LocalDateTime occurredAt;
    
    public TaskStartedEvent(TaskId taskId, WorkflowId workflowId) {
        this.taskId = taskId.getValue();
        this.workflowId = workflowId.getValue();
        this.occurredAt = LocalDateTime.now();
    }
    
    // 只有 getter
    public String getTaskId() { return taskId; }
    public String getWorkflowId() { return workflowId; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
```

### 5.2 发布领域事件

```java
public class Task {
    private DomainEventCollection domainEvents = new DomainEventCollection();
    
    public void start() {
        validateState();
        this.status = TaskStatus.RUNNING;
        this.startedAt = LocalDateTime.now();
        
        // 记录领域事件
        recordEvent(new TaskStartedEvent(this.id, this.workflowId));
    }
    
    public void complete(ExecutionResult result) {
        this.status = TaskStatus.COMPLETED;
        this.result = result;
        this.completedAt = LocalDateTime.now();
        
        recordEvent(new TaskCompletedEvent(this.id, this.workflowId, result));
    }
    
    private void recordEvent(DomainEvent event) {
        domainEvents.add(event);
    }
    
    // 获取并清空事件（用于发布后清空）
    public List<DomainEvent> drainEvents() {
        return domainEvents.drain();
    }
}
```

### 5.3 处理领域事件

```java
// 应用层事件处理器
@Component
public class TaskEventHandler {
    private final ExecutionRepository executionRepo;
    private final NotificationService notificationService;
    
    @EventListener
    @Transactional
    public void onTaskCompleted(TaskCompletedEvent event) {
        // 更新执行统计
        executionRepo.incrementCompletedCount(event.getWorkflowId());
        
        // 发送通知
        notificationService.notify("Task completed: " + event.getTaskId());
    }
    
    @EventListener
    public void onTaskFailed(TaskFailedEvent event) {
        // 记录失败日志
        // 发送告警
    }
}
```

---

## 6. 工厂模式

### 6.1 聚合根工厂

```java
// ✅ 正确：复杂对象创建使用工厂
public class WorkflowFactory {
    public static Workflow createFromDefinition(WorkflowDefinition definition) {
        Workflow workflow = new Workflow();
        workflow.setName(definition.getName());
        
        // 创建节点
        for (NodeDef nodeDef : definition.getNodes()) {
            Node node = NodeFactory.create(nodeDef);
            workflow.addNode(node);
        }
        
        // 建立连接
        for (EdgeDef edgeDef : definition.getEdges()) {
            workflow.connect(edgeDef.getFrom(), edgeDef.getTo());
        }
        
        // 验证
        workflow.validateNoCycles();
        
        return workflow;
    }
}
```

### 6.2 领域对象构建器

```java
public class TaskBuilder {
    private TaskId id;
    private String prompt;
    private String agentCode;
    private int timeout = 30;
    
    public TaskBuilder withId(String id) {
        this.id = TaskId.of(id);
        return this;
    }
    
    public TaskBuilder withPrompt(String prompt) {
        this.prompt = prompt;
        return this;
    }
    
    public TaskBuilder withAgent(String agentCode) {
        this.agentCode = agentCode;
        return this;
    }
    
    public TaskBuilder withTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }
    
    public Task build() {
        Objects.requireNonNull(id, "ID is required");
        Objects.requireNonNull(prompt, "Prompt is required");
        
        return new Task(id, prompt, agentCode, timeout);
    }
}

// 使用
Task task = new TaskBuilder()
    .withId("task-001")
    .withPrompt("实现用户认证")
    .withAgent("claude-dev")
    .withTimeout(60)
    .build();
```

---

## 7. 贫血模型 vs 充血模型

### 7.1 贫血模型（❌ 错误）

```java
// 只有 getter/setter，没有业务逻辑
public class Task {
    private String id;
    private String status;
    
    // 只有 getter/setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

// 业务逻辑散落在外部
@Service
public class TaskService {
    public void startTask(Task task) {
        if (!"PENDING".equals(task.getStatus())) {
            throw new RuntimeException("Invalid status");
        }
        task.setStatus("RUNNING");
    }
}
```

### 7.2 充血模型（✅ 正确）

```java
// 封装业务逻辑
public class Task {
    private TaskId id;
    private TaskStatus status;
    private LocalDateTime startedAt;
    
    // 业务方法
    public void start() {
        if (status != TaskStatus.PENDING) {
            throw new IllegalStateException(
                "Task must be pending to start, current: " + status);
        }
        this.status = TaskStatus.RUNNING;
        this.startedAt = LocalDateTime.now();
    }
    
    public void complete(ExecutionResult result) {
        validateCanComplete();
        this.status = TaskStatus.COMPLETED;
        this.result = result;
    }
    
    private void validateCanComplete() {
        if (status != TaskStatus.RUNNING) {
            throw new IllegalStateException("Task must be running to complete");
        }
    }
    
    // 只有 getter，没有 setter
    public TaskId getId() { return id; }
    public TaskStatus getStatus() { return status; }
}
```

---

## 8. 常见反模式

### 8.1 反模式清单

| 反模式 | 说明 | 正确做法 |
|--------|------|---------|
| 贫血模型 | 实体只有 getter/setter | 充血模型，封装业务方法 |
| 事务脚本 | 所有逻辑在 Service 中 | 将逻辑下沉到领域对象 |
| 万能实体 | 一个实体包含所有数据 | 拆分为聚合，明确边界 |
| 跨越聚合修改 | 直接修改其他聚合 | 通过聚合根方法，或使用领域服务 |
| 领域层依赖框架 | 实体使用 JPA 注解 | 领域层纯净，基础设施负责持久化 |
| 缺失值对象 | 使用 String/Integer 表示概念 | 封装为值对象 |
| 仓储泄露 SQL | 方法名包含数据库术语 | 使用业务语义命名 |

### 8.2 检查清单

- [ ] 实体是否有业务方法？
- [ ] 值对象是否不可变？
- [ ] 聚合边界是否清晰？
- [ ] 领域层是否依赖框架？
- [ ] 业务逻辑是否在领域对象中？
- [ ] 仓储接口是否在领域层？
- [ ] 领域事件是否记录业务状态变化？

---

**最后更新**: 2024年
**版本**: v1.0
