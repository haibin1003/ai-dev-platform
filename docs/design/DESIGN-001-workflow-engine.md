# DESIGN-001: 工作流引擎详细设计

**版本**: v1.0
**状态**: 草稿
**负责人**: 待分配
**关联**: ARCH-001
**评审状态**: 待评审

---

## 1. 设计目标

本设计文档将 ARCH-001 架构设计转化为可执行的开发蓝图，明确：
- 模块划分和依赖关系
- 每个类的职责和接口
- 数据库表结构
- API 详细定义
- 开发任务分解

---

## 2. 模块划分

```
ai-dev-platform/
├── src/main/java/com/aidev/
│   ├── api/                              # 接口层
│   │   ├── controller/
│   │   │   ├── WorkflowController.java
│   │   │   ├── ExecutionController.java
│   │   │   └── LogStreamController.java
│   │   ├── dto/
│   │   │   ├── CreateWorkflowRequest.java
│   │   │   ├── WorkflowResponse.java
│   │   │   ├── ExecutionResponse.java
│   │   │   └── ErrorResponse.java
│   │   └── exception/
│   │       └── GlobalExceptionHandler.java
│   │
│   ├── application/                      # 应用层
│   │   ├── service/
│   │   │   ├── WorkflowAppService.java
│   │   │   ├── ExecutionAppService.java
│   │   │   └── DAGScheduler.java
│   │   ├── event/
│   │   │   ├── TaskStartedEvent.java
│   │   │   ├── TaskCompletedEvent.java
│   │   │   └── TaskFailedEvent.java
│   │   └── port/
│   │       └── AgentAdapter.java
│   │
│   ├── domain/                           # 领域层
│   │   ├── model/
│   │   │   ├── aggregate/
│   │   │   │   ├── Workflow.java
│   │   │   │   ├── Task.java
│   │   │   │   └── Execution.java
│   │   │   ├── entity/
│   │   │   │   ├── Node.java
│   │   │   │   └── Edge.java
│   │   │   └── valueobject/
│   │   │       ├── WorkflowId.java
│   │   │       ├── TaskId.java
│   │   │       ├── ExecutionId.java
│   │   │       ├── WorkflowStatus.java
│   │   │       ├── TaskStatus.java
│   │   │       └── ExecutionResult.java
│   │   ├── service/
│   │   │   ├── TopologicalSorter.java
│   │   │   ├── CycleDetector.java
│   │   │   └── TaskStateMachine.java
│   │   ├── repository/
│   │   │   ├── WorkflowRepository.java
│   │   │   ├── TaskRepository.java
│   │   │   └── ExecutionRepository.java
│   │   └── exception/
│   │       ├── DomainException.java
│   │       ├── WorkflowNotFoundException.java
│   │       └── CycleDetectedException.java
│   │
│   └── infrastructure/                   # 基础设施层
│       ├── persistence/
│       │   ├── entity/
│       │   │   ├── WorkflowJpaEntity.java
│       │   │   ├── TaskJpaEntity.java
│       │   │   └── ExecutionJpaEntity.java
│       │   ├── repository/
│       │   │   ├── WorkflowJpaRepository.java
│       │   │   ├── TaskJpaRepository.java
│       │   │   └── ExecutionJpaRepository.java
│       │   └── mapper/
│       │       ├── WorkflowMapper.java
│       │       ├── TaskMapper.java
│       │       └── ExecutionMapper.java
│       ├── adapter/
│       │   ├── claudecode/
│       │   │   └── ClaudeCodeAdapter.java
│       │   └── kimicode/
│       │       └── KimiCodeAdapter.java
│       ├── config/
│       │   └── WorkflowEngineConfig.java
│       └── websocket/
│           └── LogWebSocketHandler.java
│
└── src/test/java/com/aidev/
    ├── domain/
    │   ├── TopologicalSorterTest.java
    │   ├── CycleDetectorTest.java
    │   ├── TaskStateMachineTest.java
    │   └── model/
    │       ├── WorkflowTest.java
    │       └── TaskTest.java
    ├── application/
    │   ├── DAGSchedulerTest.java
    │   └── service/
    │       └── WorkflowAppServiceTest.java
    └── integration/
        └── WorkflowApiIntegrationTest.java
```

---

## 3. 类图设计

### 3.1 领域模型类图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Workflow (Aggregate)                           │
├─────────────────────────────────────────────────────────────────────────────┤
│ - id: WorkflowId                                                            │
│ - name: String                                                              │
│ - description: String                                                       │
│ - status: WorkflowStatus                                                    │
│ - nodes: List<Node>                                                         │
│ - edges: List<Edge>                                                         │
│ - variables: Map<String, String>                                            │
│ - domainEvents: DomainEventCollection                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│ + create(name, description): Workflow                                       │
│ + addNode(node: Node): void                                                 │
│ + connect(fromId, toId): void                                               │
│ + validateNoCycles(): void                                                  │
│ + getTopologicalOrder(): List<Node>                                         │
│ + activate(): void                                                          │
│ + archive(): void                                                           │
└─────────────────────────────────────────────────────────────────────────────┘
                                       │
                    ┌──────────────────┼──────────────────┐
                    │                  │                  │
                    ▼                  ▼                  ▼
┌────────────────────────┐  ┌────────────────────────┐  ┌────────────────────────┐
│        Node            │  │        Edge            │  │   WorkflowStatus       │
├────────────────────────┤  ├────────────────────────┤  ├────────────────────────┤
│ - id: NodeId           │  │ - from: NodeId         │  │ <<enumeration>>        │
│ - name: String         │  │ - to: NodeId           │  │ DRAFT                  │
│ - type: NodeType       │  ├────────────────────────┤  │ ACTIVE                 │
│ - config: Map          │  │ + Edge(from, to)       │  │ ARCHIVED               │
│ - agentCode: String    │  └────────────────────────┘  └────────────────────────┘
├────────────────────────┤
│ + Node(id, name)       │
└────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                              Task (Aggregate)                               │
├─────────────────────────────────────────────────────────────────────────────┤
│ - id: TaskId                                                                │
│ - executionId: ExecutionId                                                  │
│ - workflowId: WorkflowId                                                    │
│ - nodeId: NodeId                                                            │
│ - status: TaskStatus                                                        │
│ - inputs: Map<String, String>                                               │
│ - result: ExecutionResult                                                   │
│ - retryCount: int                                                           │
│ - maxRetries: int                                                           │
│ - startedAt: LocalDateTime                                                  │
│ - completedAt: LocalDateTime                                                │
├─────────────────────────────────────────────────────────────────────────────┤
│ + create(executionId, nodeId, inputs): Task                                 │
│ + markReady(): void                                                         │
│ + start(): void                                                             │
│ + complete(result: ExecutionResult): void                                   │
│ + fail(errorMessage: String): void                                          │
│ + cancel(): void                                                            │
│ + canRetry(): boolean                                                       │
└─────────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            TaskStatus (Value Object)                        │
├─────────────────────────────────────────────────────────────────────────────┤
│ <<enumeration>>                                                             │
│ PENDING, READY, RUNNING, COMPLETED, FAILED, CANCELLED                       │
├─────────────────────────────────────────────────────────────────────────────┤
│ + isTerminal(): boolean                                                     │
│ + canTransitionTo(newStatus): boolean                                       │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                            Execution (Aggregate)                            │
├─────────────────────────────────────────────────────────────────────────────┤
│ - id: ExecutionId                                                           │
│ - workflowId: WorkflowId                                                    │
│ - status: ExecutionStatus                                                   │
│ - variables: Map<String, String>                                            │
│ - tasks: List<Task>                                                         │
│ - completedTaskCount: int                                                   │
│ - totalTaskCount: int                                                       │
│ - startedAt: LocalDateTime                                                  │
│ - completedAt: LocalDateTime                                                │
├─────────────────────────────────────────────────────────────────────────────┤
│ + create(workflowId, variables): Execution                                  │
│ + addTask(task: Task): void                                                 │
│ + taskCompleted(taskId, result): void                                       │
│ + taskFailed(taskId, error): void                                           │
│ + isComplete(): boolean                                                     │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 服务类图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         TopologicalSorter (Domain Service)                  │
├─────────────────────────────────────────────────────────────────────────────┤
│ + sort(nodes, edges): List<Node>                                            │
│ - buildInDegreeMap(nodes, edges): Map<NodeId, Integer>                      │
│ - findZeroInDegreeNodes(inDegreeMap): Queue<NodeId>                         │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                         CycleDetector (Domain Service)                      │
├─────────────────────────────────────────────────────────────────────────────┤
│ + hasCycle(nodes, edges): boolean                                           │
│ - buildAdjacencyList(edges): Map<NodeId, List<NodeId>>                      │
│ - dfsHasCycle(node, adjacency, visited, recursionStack): boolean            │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                         TaskStateMachine (Domain Service)                   │
├─────────────────────────────────────────────────────────────────────────────┤
│ - transitions: Map<TaskStatus, Set<TaskStatus>>                             │
├─────────────────────────────────────────────────────────────────────────────┤
│ + canTransition(from, to): boolean                                          │
│ + validateTransition(from, to): void                                        │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                         DAGScheduler (Application Service)                  │
├─────────────────────────────────────────────────────────────────────────────┤
│ - taskRepository: TaskRepository                                            │
│ - executionRepository: ExecutionRepository                                  │
│ - agentAdapterFactory: AgentAdapterFactory                                  │
│ - executorService: ExecutorService                                          │
├─────────────────────────────────────────────────────────────────────────────┤
│ + scheduleReadyTasks(executionId): void                                     │
│ + submitTask(task): void                                                    │
│ - areDependenciesMet(task, completedTasks): boolean                         │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.3 仓储与适配器类图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      WorkflowRepository (Domain Interface)                  │
├─────────────────────────────────────────────────────────────────────────────┤
│ + findById(id): Optional<Workflow>                                          │
│ + findAll(): List<Workflow>                                                 │
│ + save(workflow): Workflow                                                  │
│ + delete(id): void                                                          │
└─────────────────────────────────────────────────────────────────────────────┘
                                       △
                                       │ implements
                                       │
┌─────────────────────────────────────────────────────────────────────────────┐
│                   WorkflowJpaRepository (Infrastructure)                    │
├─────────────────────────────────────────────────────────────────────────────┤
│ - jpaRepository: WorkflowJpaEntityRepository                                │
│ - mapper: WorkflowMapper                                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│ + findById(id): Optional<Workflow>                                          │
│ + save(workflow): Workflow                                                  │
│ ...                                                                         │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                       AgentAdapter (Domain Port)                            │
├─────────────────────────────────────────────────────────────────────────────┤
│ <<interface>>                                                               │
│ + getAgentCode(): String                                                    │
│ + execute(task): ExecutionResult                                            │
│ + supports(agentCode): boolean                                              │
└─────────────────────────────────────────────────────────────────────────────┘
                                       △
                         ┌─────────────┴─────────────┐
                         │                           │
            ┌────────────┴────────────┐  ┌──────────┴──────────┐
            │ ClaudeCodeAdapter       │  │  KimiCodeAdapter    │
            ├─────────────────────────┤  ├─────────────────────┤
            │ - commandExecutor       │  │ - commandExecutor   │
            │ - workspacePath         │  │ - workspacePath     │
            ├─────────────────────────┤  ├─────────────────────┤
            │ + execute(task)         │  │ + execute(task)     │
            │ + supports(agentCode)   │  │ + supports(agentCode)│
            └─────────────────────────┘  └─────────────────────┘
```

---

## 4. 时序图

### 4.1 创建工作流

```
Client          Controller    WorkflowAppService    Workflow    WorkflowRepo
  │                 │                  │                 │            │
  │ POST /workflows │                  │                 │            │
  │────────────────>│                  │                 │            │
  │                 │ createWorkflow() │                 │            │
  │                 │─────────────────>│                 │            │
  │                 │                  │    create()     │            │
  │                 │                  │────────────────>│            │
  │                 │                  │                 │ validate() │
  │                 │                  │<────────────────│            │
  │                 │                  │    save()       │            │
  │                 │                  │─────────────────────────────>│
  │                 │                  │<─────────────────────────────│
  │                 │<─────────────────│                 │            │
  │  201 Created    │                  │                 │            │
  │<────────────────│                  │                 │            │
```

### 4.2 执行工作流

```
Client    Controller  ExecutionAppService  DAGScheduler  AgentAdapter  TaskRepo
  │           │               │                  │             │         │
  │ POST /execute            │                  │             │         │
  │──────────>│              │                  │             │         │
  │           │ startExecution()               │             │         │
  │           │─────────────>│                  │             │         │
  │           │              │ createExecution()│             │         │
  │           │              │─────────────────>│             │         │
  │           │              │  scheduleReadyTasks()          │         │
  │           │              │────────────────────────────────>│         │
  │           │              │                  │ getReadyTasks│        │
  │           │              │                  │─────────────>│         │
  │           │              │                  │<─────────────│         │
  │           │              │                  │ submitTask() │         │
  │           │              │                  │─────────────>│         │
  │           │              │                  │ execute()    │         │
  │           │              │                  │──────────────────────>│
  │           │              │                  │<──────────────────────│
  │           │              │                  │ save(task)   │         │
  │           │              │                  │─────────────────────────>│
  │           │              │                  │ publishEvent │         │
  │           │              │<─────────────────│             │         │
  │           │<─────────────│                  │             │         │
  │ 202 Accept│              │                  │             │         │
  │<──────────│              │                  │             │         │
  │           │              │                  │             │         │
  │           │              │                  │ (async callback)      │
  │           │              │<─────────────────────────────────────────│
```

### 4.3 任务状态转换

```
TaskStateMachine       Task
        │               │
        │ markReady()   │
        │──────────────>│
        │               │ status = READY
        │               │
        │ start()       │
        │──────────────>│
        │ validate(READY→RUNNING)
        │<──────────────│
        │               │ status = RUNNING
        │               │
        │ complete()    │
        │──────────────>│
        │ validate(RUNNING→COMPLETED)
        │<──────────────│
        │               │ status = COMPLETED
        │               │
        │ fail()        │
        │──────────────>│
        │ validate(RUNNING→FAILED)
        │<──────────────│
        │               │ status = FAILED
```

---

## 5. 数据库详细设计

### 5.1 表结构

```sql
-- 工作流定义表
CREATE TABLE workflows (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    definition_json CLOB NOT NULL,  -- 存储 nodes 和 edges
    variables_json CLOB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_workflow_status CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED'))
);

CREATE INDEX idx_workflows_status ON workflows(status);

-- 执行记录表
CREATE TABLE executions (
    id VARCHAR(36) PRIMARY KEY,
    workflow_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    variables_json CLOB,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_executions_workflow
        FOREIGN KEY (workflow_id) REFERENCES workflows(id) ON DELETE CASCADE,
    CONSTRAINT chk_execution_status
        CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED'))
);

CREATE INDEX idx_executions_workflow ON executions(workflow_id);
CREATE INDEX idx_executions_status ON executions(status);

-- 任务表
CREATE TABLE tasks (
    id VARCHAR(36) PRIMARY KEY,
    execution_id VARCHAR(36) NOT NULL,
    node_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    inputs_json CLOB,
    result_json CLOB,
    error_message TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 3,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,  -- 乐观锁

    CONSTRAINT fk_tasks_execution
        FOREIGN KEY (execution_id) REFERENCES executions(id) ON DELETE CASCADE,
    CONSTRAINT chk_task_status
        CHECK (status IN ('PENDING', 'READY', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED'))
);

CREATE INDEX idx_tasks_execution ON tasks(execution_id);
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_execution_status ON tasks(execution_id, status);

-- 任务依赖关系表（用于存储 DAG 依赖）
CREATE TABLE task_dependencies (
    task_id VARCHAR(36) NOT NULL,
    depends_on_task_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (task_id, depends_on_task_id),
    CONSTRAINT fk_dep_task
        FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_dep_depends_on
        FOREIGN KEY (depends_on_task_id) REFERENCES tasks(id) ON DELETE CASCADE
);

CREATE INDEX idx_task_dependencies_depends_on ON task_dependencies(depends_on_task_id);
```

### 5.2 JPA Entity 设计

```java
@Entity
@Table(name = "workflows")
public class WorkflowJpaEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    private String status;

    @Lob
    @Column(name = "definition_json", nullable = false, columnDefinition = "CLOB")
    private String definitionJson;

    @Lob
    @Column(name = "variables_json", columnDefinition = "CLOB")
    private String variablesJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

@Entity
@Table(name = "tasks")
public class TaskJpaEntity {
    @Id
    private String id;

    @Column(name = "execution_id", nullable = false)
    private String executionId;

    @Column(name = "node_id", nullable = false)
    private String nodeId;

    @Column(nullable = false, length = 20)
    private String status;

    @Lob
    @Column(name = "inputs_json", columnDefinition = "CLOB")
    private String inputsJson;

    @Lob
    @Column(name = "result_json", columnDefinition = "CLOB")
    private String resultJson;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "max_retries", nullable = false)
    private Integer maxRetries = 3;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

---

## 6. API 详细定义

### 6.1 工作流 API

```yaml
# POST /api/v1/workflows - 创建工作流
request:
  content-type: application/json
  body:
    name: string (required, max 255)
    description: string (optional, max 1000)
    definition:
      nodes:
        - id: string (required)
          name: string (required)
          type: string (required, enum: [TASK, CONDITION, PARALLEL])
          agentCode: string (optional, default: claude-code)
          config: object (optional)
      edges:
        - from: string (required, must match node id)
          to: string (required, must match node id)
    variables: map<string, string> (optional)

response:
  status: 201 Created
  headers:
    Location: /api/v1/workflows/{id}
  body:
    id: string
    name: string
    status: string (DRAFT)
    createdAt: string (ISO 8601)

# GET /api/v1/workflows - 获取工作流列表
request:
  query:
    page: integer (default: 0)
    size: integer (default: 20, max: 100)
    status: string (optional, filter by status)

response:
  status: 200 OK
  body:
    items:
      - id: string
        name: string
        status: string
        createdAt: string
    total: integer
    page: integer
    pageSize: integer
    totalPages: integer

# GET /api/v1/workflows/{id} - 获取工作流详情
response:
  status: 200 OK
  body:
    id: string
    name: string
    description: string
    status: string
    definition:
      nodes: [...]
      edges: [...]
    variables: map<string, string>
    createdAt: string
    updatedAt: string

# POST /api/v1/workflows/{id}/activate - 激活工作流
response:
  status: 200 OK
  body:
    id: string
    status: ACTIVE

# POST /api/v1/workflows/{id}/execute - 执行工作流
request:
  body:
    variables: map<string, string> (optional, override workflow variables)

response:
  status: 202 Accepted
  body:
    executionId: string
    status: PENDING
    _links:
      status: string (URL to check execution status)
      cancel: string (URL to cancel execution)

# DELETE /api/v1/workflows/{id} - 删除工作流
response:
  status: 204 No Content
```

### 6.2 执行 API

```yaml
# GET /api/v1/executions/{id} - 获取执行详情
response:
  status: 200 OK
  body:
    id: string
    workflowId: string
    status: string
    variables: map<string, string>
    completedTaskCount: integer
    totalTaskCount: integer
    startedAt: string (optional)
    completedAt: string (optional)

# GET /api/v1/executions/{id}/tasks - 获取执行任务列表
response:
  status: 200 OK
  body:
    items:
      - id: string
        nodeId: string
        nodeName: string
        status: string
        startedAt: string (optional)
        completedAt: string (optional)
        retryCount: integer

# POST /api/v1/executions/{id}/cancel - 取消执行
response:
  status: 200 OK
  body:
    id: string
    status: CANCELLED
```

### 6.3 错误响应

```yaml
# 统一错误格式
status: 4xx/5xx
body:
  error:
    code: string (e.g., WF-001, TASK-002, VAL-003)
    message: string
    details:
      - field: string
        message: string
    traceId: string
    timestamp: string (ISO 8601)

# 错误码定义
WF-001: 工作流不存在
WF-002: 工作流名称不能为空
WF-003: 工作流定义无效
WF-004: 工作流包含循环依赖
WF-005: 工作流状态不允许此操作

TASK-001: 任务不存在
TASK-002: 任务状态转换无效
TASK-003: 任务执行超时
TASK-004: 任务执行失败

EXEC-001: 执行不存在
EXEC-002: 执行已在进行中
EXEC-003: 执行已完成，无法取消

VAL-001: 请求参数格式错误
VAL-002: 必填字段缺失
VAL-003: 字段值无效

SYS-001: 系统内部错误
SYS-002: 数据库操作失败
SYS-003: 外部服务调用失败
```

---

## 7. 开发任务分解

### Phase 1: 领域层核心（Week 1 Day 1-2）

| 任务 | 文件 | 优先级 | 验收标准 |
|------|------|--------|---------|
| 值对象实现 | WorkflowId, TaskId, ExecutionId | P0 | ✅ 不可变、正确 equals/hashCode |
| 枚举实现 | WorkflowStatus, TaskStatus | P0 | ✅ 包含状态转换验证 |
| Node/Edge 实体 | Node.java, Edge.java | P0 | ✅ 正确封装 |
| Workflow 聚合根 | Workflow.java | P0 | ✅ 工厂方法、业务方法、事件 |
| Task 聚合根 | Task.java | P0 | ✅ 状态机转换方法 |
| Execution 聚合根 | Execution.java | P0 | ✅ 任务管理方法 |

### Phase 2: 领域服务（Week 1 Day 3-4）

| 任务 | 文件 | 优先级 | 验收标准 |
|------|------|--------|---------|
| 拓扑排序 | TopologicalSorter.java | P0 | ✅ Kahn 算法、单元测试覆盖 |
| 循环检测 | CycleDetector.java | P0 | ✅ DFS 检测、单元测试覆盖 |
| 状态机 | TaskStateMachine.java | P0 | ✅ 状态转换表、验证方法 |

### Phase 3: 基础设施层（Week 1 Day 5 - Week 2 Day 1）

| 任务 | 文件 | 优先级 | 验收标准 |
|------|------|--------|---------|
| JPA Entity | *JpaEntity.java | P0 | ✅ 正确映射 |
| Repository 实现 | *JpaRepository.java | P0 | ✅ 实现接口、通过测试 |
| Mapper | *Mapper.java | P0 | ✅ 双向转换正确 |
| ClaudeCode 适配器 | ClaudeCodeAdapter.java | P0 | ✅ 命令行执行、结果解析 |

### Phase 4: 应用层（Week 2 Day 2-3）

| 任务 | 文件 | 优先级 | 验收标准 |
|------|------|--------|---------|
| WorkflowAppService | WorkflowAppService.java | P0 | ✅ CRUD、事务控制 |
| ExecutionAppService | ExecutionAppService.java | P0 | ✅ 执行生命周期 |
| DAGScheduler | DAGScheduler.java | P0 | ✅ 并发调度、事件发布 |
| 领域事件 | *Event.java | P0 | ✅ 事件定义完整 |

### Phase 5: API 层（Week 2 Day 4-5）

| 任务 | 文件 | 优先级 | 验收标准 |
|------|------|--------|---------|
| DTO | *Request.java, *Response.java | P0 | ✅ 字段完整、校验注解 |
| Controller | *Controller.java | P0 | ✅ RESTful 规范、异常处理 |
| WebSocket | LogWebSocketHandler.java | P1 | ✅ 日志推送 |

### Phase 6: 测试（持续进行）

| 任务 | 文件 | 优先级 | 验收标准 |
|------|------|--------|---------|
| 领域层单元测试 | *Test.java | P0 | ✅ 覆盖率 ≥ 95% |
| 应用层单元测试 | *Test.java | P0 | ✅ 覆盖率 ≥ 85% |
| 集成测试 | *IntegrationTest.java | P0 | ✅ 关键流程覆盖 |

---

## 8. 设计决策检查清单

- [ ] DDD 分层正确（无依赖倒置）
- [ ] 聚合边界清晰
- [ ] 值对象不可变
- [ ] 仓储接口在领域层
- [ ] 适配器实现符合端口定义
- [ ] 状态机转换规则完整
- [ ] 错误码定义清晰
- [ ] API 符合 RESTful 规范
- [ ] 数据库索引设计合理
- [ ] 乐观锁防止并发问题

---

## 9. 风险评估

| 风险 | 缓解措施 |
|------|---------|
| DAG 算法实现复杂 | 充分的单元测试，参考成熟实现 |
| 并发状态不一致 | 乐观锁 + 状态机验证 |
| Agent 调用失败 | 重试机制 + 错误处理 |
| JSON 序列化性能 | 使用 Jackson，必要时缓存 |

---

**评审记录**:
| 日期 | 评审人 | 意见 | 状态 |
|------|--------|------|------|
| | | | |

---

**下一步**:
1. 设计评审通过后，更新 PROJECT_STATUS.md
2. 创建 feature/PRD-001-workflow-engine 分支
3. 按照开发任务分解开始编码
