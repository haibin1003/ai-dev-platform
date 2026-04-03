# ARCH-001: 工作流引擎架构设计

**版本**: v1.0
**状态**: 草稿
**负责人**: 待分配
**评审状态**: 待 AI 评审

---

## 1. 架构目标

设计一个**轻量级 DAG 工作流引擎**，具备以下特性：

- **可扩展**：通过适配器模式支持多种 AI Agent
- **可观测**：完整的事件和日志体系
- **可恢复**：任务状态持久化，支持故障恢复
- **高性能**：支持任务并发执行

---

## 2. 系统架构

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           API Layer (Spring MVC)                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  │
│  │ WorkflowCtrl│  │  TaskCtrl   │  │ ExecutionCtrl│ │  AgentCtrl  │  │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         Application Layer                               │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐   │
│  │ WorkflowAppService│  │  TaskAppService  │  │ ExecutionService │   │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘   │
│  ┌──────────────────┐  ┌──────────────────┐                          │
│  │   DAGScheduler    │  │ EventPublisher   │                          │
│  └──────────────────┘  └──────────────────┘                          │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                            Domain Layer                                 │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐          │
│  │    Workflow    │  │      Task      │  │  Execution     │          │
│  │   (Aggregate)  │  │   (Aggregate)  │  │  (Aggregate)   │          │
│  └────────────────┘  └────────────────┘  └────────────────┘          │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐          │
│  │  DAGService    │  │ StateMachine   │  │ TopologicalSort│          │
│  │ (Domain Service)│ │(Domain Service)│  │(Domain Service)│          │
│  └────────────────┘  └────────────────┘  └────────────────┘          │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        Infrastructure Layer                             │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐          │
│  │ WorkflowRepo    │  │  TaskRepo       │  │ ExecutionRepo  │          │
│  │ (JPA/H2)       │  │  (JPA/H2)      │  │ (JPA/H2)      │          │
│  └────────────────┘  └────────────────┘  └────────────────┘          │
│  ┌────────────────┐  ┌────────────────┐                               │
│  │ ClaudeAdapter   │  │  KimiAdapter   │    ← Agent Adapters          │
│  └────────────────┘  └────────────────┘                               │
│  ┌────────────────┐  ┌────────────────┐                               │
│  │ EventListener  │  │  WebSocket     │    ← Observability             │
│  └────────────────┘  └────────────────┘                               │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 核心组件职责

| 组件 | 层级 | 职责 |
|------|------|------|
| `WorkflowController` | API | 工作流 CRUD、操作入口 |
| `WorkflowAppService` | Application | 工作流用例编排、事务控制 |
| `Workflow` | Domain | 工作流聚合根，管理节点和边 |
| `Task` | Domain | 任务聚合根，管理执行状态 |
| `DAGScheduler` | Application | DAG 调度，触发任务执行 |
| `TopologicalSorter` | Domain | 拓扑排序算法实现 |
| `StateMachine` | Domain | 任务状态机转换 |
| `ClaudeAdapter` | Infrastructure | Claude Code 适配器 |
| `WorkflowRepository` | Domain | 工作流仓储接口 |

---

## 3. 核心领域模型

### 3.1 Workflow 聚合根

```java
// 领域模型 - 核心实体
public class Workflow {
    private WorkflowId id;
    private String name;
    private String description;
    private WorkflowStatus status;  // DRAFT, ACTIVE, ARCHIVED
    private List<Node> nodes;
    private List<Edge> edges;
    private Map<String, String> variables;
    private DomainEventCollection domainEvents;

    // 工厂方法
    public static Workflow create(String name, String description) { }

    // 核心行为
    public void addNode(Node node) { }
    public void connect(String fromNodeId, String toNodeId) { }
    public void validateNoCycles() { }
    public List<Node> getTopologicalOrder() { }
}

// 值对象
public class Node {
    private NodeId id;
    private String name;
    private NodeType type;  // TASK, CONDITION, PARALLEL
    private Map<String, String> config;
    private String agentCode;  // 关联的 Agent
}

public class Edge {
    private NodeId from;
    private NodeId to;
}
```

### 3.2 Task 聚合根

```java
public class Task {
    private TaskId id;
    private ExecutionId executionId;
    private WorkflowId workflowId;
    private NodeId nodeId;
    private TaskStatus status;  // PENDING, READY, RUNNING, COMPLETED, FAILED, CANCELLED
    private Map<String, String> inputs;
    private ExecutionResult result;
    private int retryCount;
    private int maxRetries;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    // 核心行为
    public void markReady() { }
    public void start() { }
    public void complete(ExecutionResult result) { }
    public void fail(String errorMessage) { }
    public void cancel() { }
    public boolean canRetry() { }
}
```

### 3.3 Execution 聚合根

```java
public class Execution {
    private ExecutionId id;
    private WorkflowId workflowId;
    private ExecutionStatus status;  // PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
    private Map<String, String> variables;
    private List<Task> tasks;
    private int completedTaskCount;
    private int totalTaskCount;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    // 核心行为
    public void start() { }
    public void addTask(Task task) { }
    public void taskCompleted(TaskId taskId, ExecutionResult result) { }
    public void taskFailed(TaskId taskId, String error) { }
    public boolean isComplete() { }
}
```

---

## 4. DAG 引擎设计

### 4.1 拓扑排序（Kahn 算法）

```java
public class TopologicalSorter {

    public static List<Node> sort(List<Node> nodes, List<Edge> edges) {
        // 1. 构建入度表
        Map<NodeId, Integer> inDegree = new HashMap<>();
        nodes.forEach(n -> inDegree.put(n.getId(), 0));
        edges.forEach(e -> inDegree.merge(e.getTo(), 1, Integer::sum));

        // 2. 入度为 0 的节点入队
        Queue<NodeId> queue = nodes.stream()
            .filter(n -> inDegree.get(n.getId()) == 0)
            .map(Node::getId)
            .collect(Collectors.toQueue());

        // 3. BFS 出队并更新
        List<Node> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            NodeId current = queue.poll();
            result.add(findNode(current));

            edges.stream()
                .filter(e -> e.getFrom().equals(current))
                .map(Edge::getTo)
                .forEach(dependent -> {
                    inDegree.merge(dependent, -1, Integer::sum);
                    if (inDegree.get(dependent) == 0) {
                        queue.offer(dependent);
                    }
                });
        }

        // 4. 检测循环
        if (result.size() != nodes.size()) {
            throw new CycleDetectedException();
        }

        return result;
    }
}
```

### 4.2 循环依赖检测

```java
public class CycleDetector {

    public boolean hasCycle(List<Node> nodes, List<Edge> edges) {
        Map<NodeId, List<NodeId>> adjacency = buildAdjacencyList(edges);
        Set<NodeId> visited = new HashSet<>();
        Set<NodeId> recursionStack = new HashSet<>();

        for (Node node : nodes) {
            if (dfsHasCycle(node.getId(), adjacency, visited, recursionStack)) {
                return true;
            }
        }
        return false;
    }

    private boolean dfsHasCycle(NodeId node, Map<NodeId, List<NodeId>> adjacency,
                                Set<NodeId> visited, Set<NodeId> recursionStack) {
        if (recursionStack.contains(node)) return true;
        if (visited.contains(node)) return false;

        visited.add(node);
        recursionStack.add(node);

        for (NodeId neighbor : adjacency.getOrDefault(node, List.of())) {
            if (dfsHasCycle(neighbor, adjacency, visited, recursionStack)) {
                return true;
            }
        }

        recursionStack.remove(node);
        return false;
    }
}
```

---

## 5. 任务调度设计

### 5.1 调度器架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        DAGScheduler                             │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │ TaskQueue       │  │ ExecutorPool    │  │ RetryHandler    │ │
│  │ (BlockingQueue) │  │ (ThreadPool)   │  │                 │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
              ┌───────────────────────────────────┐
              │         TaskExecutor               │
              │  ┌─────────┐  ┌─────────┐        │
              │  │ AgentA  │  │ AgentB  │  ...   │
              │  │ Adapter │  │ Adapter │        │
              │  └─────────┘  └─────────┘        │
              └───────────────────────────────────┘
```

### 5.2 调度流程

```java
@Service
public class DAGScheduler {

    private final TaskRepository taskRepository;
    private final ExecutionRepository executionRepository;
    private final AgentAdapterFactory agentAdapterFactory;
    private final ExecutorService executorService;

    public void scheduleReadyTasks(ExecutionId executionId) {
        Execution execution = executionRepository.findById(executionId)
            .orElseThrow(() -> new ExecutionNotFoundException(executionId));

        // 1. 获取所有待执行任务
        List<Task> pendingTasks = taskRepository.findByExecutionIdAndStatus(
            executionId, TaskStatus.PENDING);

        // 2. 获取已完成任务，用于判断依赖是否满足
        List<Task> completedTasks = taskRepository.findByExecutionIdAndStatus(
            executionId, TaskStatus.COMPLETED);

        // 3. 筛选出依赖已满足的任务
        List<Task> readyTasks = pendingTasks.stream()
            .filter(task -> areDependenciesMet(task, completedTasks))
            .collect(Collectors.toList());

        // 4. 提交到线程池执行
        readyTasks.forEach(this::submitTask);
    }

    private void submitTask(Task task) {
        task.markReady();
        taskRepository.save(task);

        executorService.submit(() -> {
            try {
                AgentAdapter adapter = agentAdapterFactory.getAdapter(task.getAgentCode());
                ExecutionResult result = adapter.execute(task);
                task.complete(result);
            } catch (Exception e) {
                task.fail(e.getMessage());
            } finally {
                taskRepository.save(task);
                // 调度下游任务
                scheduleReadyTasks(task.getExecutionId());
            }
        });
    }
}
```

### 5.3 状态机转换

```
     ┌─────────┐
     │ PENDING │
     └────┬────┘
          │ areDependenciesMet()
          ▼
     ┌─────────┐
     │  READY  │◄────────────── (retry)
     └────┬────┘
          │ submit()
          ▼
     ┌─────────┐
     │ RUNNING │
     └────┬────┘
          │
    ┌─────┴─────┐
    │           │
    ▼           ▼
┌────────┐  ┌────────┐
│COMPLETE│  │ FAILED │
└────────┘  └────┬───┘
                 │
                 ▼
            canRetry ? ──► READY (重新入队)
                 │
                 ▼
           CANCELLED (重试次数耗尽)
```

---

## 6. Agent 适配器设计

### 6.1 适配器接口

```java
// 端口接口 - 领域层定义
public interface AgentAdapter {
    String getAgentCode();

    ExecutionResult execute(Task task);

    boolean supports(String agentCode);
}

// 领域服务 - 选择适配器
@Service
public class AgentAdapterFactory {
    private final Map<String, AgentAdapter> adapters;

    public AgentAdapter getAdapter(String agentCode) {
        return adapters.values().stream()
            .filter(a -> a.supports(agentCode))
            .findFirst()
            .orElseThrow(() -> new AgentNotSupportedException(agentCode));
    }
}
```

### 6.2 Claude Code 适配器实现

```java
// 基础设施层 - 适配器实现
@Component
public class ClaudeCodeAdapter implements AgentAdapter {

    private static final String AGENT_CODE = "claude-code";
    private final CommandExecutor commandExecutor;

    @Override
    public String getAgentCode() {
        return AGENT_CODE;
    }

    @Override
    public ExecutionResult execute(Task task) {
        // 1. 构建 Claude Code 命令
        String prompt = buildPrompt(task);

        // 2. 执行命令
        CommandResult result = commandExecutor.execute("claude", args(prompt));

        // 3. 解析结果
        return ExecutionResult.builder()
            .output(result.getStdout())
            .errorOutput(result.getStderr())
            .exitCode(result.getExitCode())
            .durationMs(result.getDurationMs())
            .artifacts(extractArtifacts(result))
            .build();
    }

    @Override
    public boolean supports(String agentCode) {
        return AGENT_CODE.equals(agentCode);
    }
}
```

### 6.3 适配器注册表

```java
// 配置文件
@Bean
public Map<String, AgentAdapter> agentAdapters(
        ClaudeCodeAdapter claudeAdapter,
        KimiCodeAdapter kimiAdapter) {
    return Map.of(
        "claude-code", claudeAdapter,
        "kimi-code", kimiAdapter
    );
}
```

---

## 7. 数据模型设计

### 7.1 数据库 Schema

```sql
-- 工作流定义表
CREATE TABLE workflows (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    definition_json TEXT NOT NULL,  -- 存储节点和边的 JSON
    variables_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 执行记录表
CREATE TABLE executions (
    id VARCHAR(36) PRIMARY KEY,
    workflow_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    variables_json TEXT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (workflow_id) REFERENCES workflows(id)
);

-- 任务表
CREATE TABLE tasks (
    id VARCHAR(36) PRIMARY KEY,
    execution_id VARCHAR(36) NOT NULL,
    node_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    inputs_json TEXT,
    result_json TEXT,
    error_message TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 3,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (execution_id) REFERENCES executions(id)
);

-- 索引
CREATE INDEX idx_executions_workflow ON executions(workflow_id);
CREATE INDEX idx_tasks_execution ON tasks(execution_id);
CREATE INDEX idx_tasks_status ON tasks(status);
```

### 7.2 领域模型与持久化映射

```
Workflow (Domain)          WorkflowJpaEntity (Infrastructure)
├── id: WorkflowId    ←→   ├── id: String
├── name: String      ←→   ├── name: String
├── status: Status     ←→   ├── status: String
├── nodes: List<Node>  ←→   ├── definitionJson: String (JSON)
└── edges: List<Edge> ←→

Task (Domain)              TaskJpaEntity (Infrastructure)
├── id: TaskId        ←→   ├── id: String
├── status: Status    ←→   ├── status: String
├── result: Result    ←→   ├── resultJson: String (JSON)
└── retryCount: int   ←→   ├── retryCount: Integer
```

---

## 8. API 接口设计

### 8.1 工作流 API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/workflows` | 创建工作流 |
| GET | `/api/v1/workflows` | 获取工作流列表 |
| GET | `/api/v1/workflows/{id}` | 获取工作流详情 |
| PUT | `/api/v1/workflows/{id}` | 更新工作流 |
| DELETE | `/api/v1/workflows/{id}` | 删除工作流 |
| POST | `/api/v1/workflows/{id}/activate` | 激活工作流 |
| POST | `/api/v1/workflows/{id}/execute` | 执行工作流 |
| GET | `/api/v1/workflows/{id}/executions` | 获取执行历史 |

### 8.2 执行 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/executions/{id}` | 获取执行详情 |
| GET | `/api/v1/executions/{id}/tasks` | 获取执行任务列表 |
| POST | `/api/v1/executions/{id}/cancel` | 取消执行 |
| GET | `/api/v1/executions/{id}/logs` | 获取执行日志 (WebSocket) |

### 8.3 请求/响应示例

```json
// POST /api/v1/workflows - 创建工作流
// Request
{
  "name": "用户认证工作流",
  "description": "包含登录、验证、发 Token",
  "definition": {
    "nodes": [
      { "id": "n1", "name": "用户登录", "type": "TASK", "agentCode": "claude-code", "config": {} },
      { "id": "n2", "name": "验证凭据", "type": "TASK", "agentCode": "claude-code", "config": {} },
      { "id": "n3", "name": "生成 Token", "type": "TASK", "agentCode": "claude-code", "config": {} }
    ],
    "edges": [
      { "from": "n1", "to": "n2" },
      { "from": "n2", "to": "n3" }
    ]
  },
  "variables": {
    "timeout": "300",
    "enableRetry": "true"
  }
}

// Response - 201 Created
{
  "id": "wf-001",
  "name": "用户认证工作流",
  "status": "DRAFT",
  "createdAt": "2026-04-03T10:00:00Z"
}
```

```json
// POST /api/v1/workflows/{id}/execute - 执行工作流
// Request
{
  "variables": {
    "username": "test@example.com"
  }
}

// Response - 202 Accepted
{
  "executionId": "exec-001",
  "status": "PENDING",
  "_links": {
    "status": "/api/v1/executions/exec-001",
    "cancel": "/api/v1/executions/exec-001/cancel"
  }
}
```

---

## 9. 事件与日志设计

### 9.1 领域事件

```java
// 领域事件定义
public interface DomainEvent {
    String getEventType();
    LocalDateTime getOccurredAt();
}

public class TaskStartedEvent implements DomainEvent {
    private final TaskId taskId;
    private final ExecutionId executionId;
    private final LocalDateTime occurredAt;
}

public class TaskCompletedEvent implements DomainEvent {
    private final TaskId taskId;
    private final ExecutionId executionId;
    private final ExecutionResult result;
    private final LocalDateTime occurredAt;
}

public class ExecutionCompletedEvent implements DomainEvent {
    private final ExecutionId executionId;
    private final ExecutionStatus finalStatus;
}
```

### 9.2 事件处理

```java
// 应用层事件处理器
@Component
public class ExecutionEventHandler {

    @EventListener
    public void onTaskCompleted(TaskCompletedEvent event) {
        // 1. 更新执行进度
        executionRepository.incrementCompletedCount(event.getExecutionId());

        // 2. 发布 WebSocket 事件
        webSocketPublisher.publishTaskCompleted(event);

        // 3. 检查是否触发下游任务
        dagScheduler.scheduleReadyTasks(event.getExecutionId());
    }

    @EventListener
    public void onExecutionCompleted(ExecutionCompletedEvent event) {
        // 1. 发送通知
        notificationService.notifyExecutionCompleted(event);

        // 2. 收集产物
        artifactCollector.collect(event.getExecutionId());
    }
}
```

### 9.3 WebSocket 日志推送

```java
// 实时日志服务
@Service
public class LogStreamService {

    private final SimpMessagingTemplate messagingTemplate;

    public void streamTaskLog(TaskId taskId, String logLine) {
        messagingTemplate.convertAndSend(
            "/topic/executions/" + taskId.getExecutionId() + "/logs",
            new LogMessage(taskId, logLine, LocalDateTime.now())
        );
    }

    public void streamTaskResult(TaskId taskId, ExecutionResult result) {
        messagingTemplate.convertAndSend(
            "/topic/executions/" + taskId.getExecutionId() + "/tasks/" + taskId,
            new TaskResultMessage(taskId, result)
        );
    }
}
```

---

## 10. 配置设计

### 10.1 配置文件结构

```yaml
# application.yml
spring:
  application:
    name: ai-dev-platform

# 工作流引擎配置
workflow:
  scheduler:
    pool-size: 10              # 调度线程池大小
    max-retries: 3             # 默认最大重试次数
    task-timeout-seconds: 300   # 默认任务超时时间

  # Agent 配置
  agents:
    claude-code:
      enabled: true
      command: claude
      workspace: ${WORKSPACE_DIR:./workspace}
    kimicode:
      enabled: false
      command: kimi
      workspace: ${WORKSPACE_DIR:./workspace}

# 数据库配置
  datasource:
    url: jdbc:h2:mem:workflow;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
```

---

## 11. 技术决策记录

| 决策ID | 决策内容 | 理由 | 状态 |
|--------|---------|------|------|
| ADR-001 | 使用 H2 作为开发数据库 | 轻量、嵌入、零配置 | 已采纳 |
| ADR-002 | 自研 DAG 引擎而非引入 Activiti/Flowable | 轻量、无重型特性、学习成本低 | 已采纳 |
| ADR-003 | 使用 ThreadPool 而非消息队列 | 简化架构、MVP 阶段足够 | 已采纳 |
| ADR-004 | 使用 Spring MVC 而非 WebFlux | 减少响应式复杂度 | 待评审 |
| ADR-005 | 任务状态持久化到 DB | 支持故障恢复 | 已采纳 |

---

## 12. 风险评估

| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|------|---------|
| 并发执行导致状态不一致 | 高 | 低 | 使用乐观锁、事务控制 |
| Agent 执行超时 | 中 | 中 | 配置超时、优雅中断 |
| 循环依赖未检测到 | 高 | 低 | 拓扑排序时检测 |
| 长时间运行任务占用资源 | 中 | 中 | 资源隔离、任务超时 |

---

## 13. 验收标准

- [ ] 支持 YAML/JSON 定义工作流
- [ ] 正确实现 Kahn 拓扑排序算法
- [ ] 检测循环依赖并抛出明确异常
- [ ] 任务状态机正确转换
- [ ] 支持任务并发执行
- [ ] 支持任务失败重试
- [ ] 支持 WebSocket 实时日志
- [ ] 单元测试覆盖率 ≥ 80%
- [ ] API 符合 RESTful 规范

---

**评审记录**:
| 日期 | 评审人 | 意见 | 状态 |
|------|--------|------|------|
| 2026-04-03 | AI Assistant | 初始版本 | 待正式评审 |
| 2026-04-03 | AI Assistant | 架构方向确认通过 | ✅ 通过 |

---

**下一步**:
1. AI 评审通过后，更新 PROJECT_STATUS.md 中 ARCH-001 状态
2. 进入 DESIGN-001 详细设计阶段
3. 开始 feature/PRD-001-workflow-engine 分支开发
