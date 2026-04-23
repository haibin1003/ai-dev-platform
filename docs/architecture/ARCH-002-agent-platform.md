# ARCH-002: AI 智能体平台架构设计

**版本**: v2.0  
**状态**: 待评审（已吸收 weibaohui/tasks 参考项目 10 大工程实践）  
**负责人**: 待分配  
**创建日期**: 2026-04-21  
**关联**: PRD-003, ARCH-001

---

## 1. 架构目标

基于 PRD-003 需求，设计一个支持以下特性的平台架构：

- **LLM 无关性**：Agent 与 LLM 解耦，可灵活切换 Provider
- **进程级 Agent**：启动真实的 CLI 进程（Claude Code / Kimi Code / OpenCode）
- **安全隔离**：每个 Agent 运行在独立沙箱中
- **多 Agent 编排**：DAG 引擎驱动多角色协作
- **双模 Agent**：支持持久型（龙虾）和任务型（用完即走）
- **SaaS 就绪**：多租户、资源配额、平台市场

---

## 2. 系统整体架构

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              前端层 (Vue3 + TypeScript)                      │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐         │
│  │ LLM管理  │ │Agent市场 │ │ 项目管理 │ │ 工作流编排│ │ 报告/审计 │         │
│  │ 页面     │ │ 页面     │ │ 页面     │ │ 编辑器   │ │ 页面     │         │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘         │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐                                     │
│  │ 需求管理 │ │ 执行监控 │ │ 龙虾聊天 │                                     │
│  │ 页面     │ │ 页面     │ │ 界面     │                                     │
│  └──────────┘ └──────────┘ └──────────┘                                     │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼ HTTP / WebSocket
┌─────────────────────────────────────────────────────────────────────────────┐
│                            API Gateway / BFF                                │
│  - 认证 (JWT/OAuth2)                                                        │
│  - 限流 (Rate Limiting)                                                     │
│  - 多租户路由 (Tenant Resolution)                                           │
│  - API 聚合                                                                  │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           应用服务层 (Application)                            │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐       │
│  │ LLMAppService│ │AgentAppServic│ │ProjectAppServ│ │WorkflowAppSer│       │
│  │ - ProviderCRUD│ │ - TemplateCRUD│ │ - ProjectCRUD│ │ - DAG编排    │       │
│  │ - Key管理    │ │ - Instance管理│ │ - Repo同步   │ │ - 执行调度   │       │
│  │ - 配额检查   │ │ - 生命周期   │ │ - 上下文生成 │ │ - 状态机     │       │
│  └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘       │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐       │
│  │ Requirement  │ │ ExecutionApp │ │ LobsterServic│ │ ReportAppServ│       │
│  │ AppService   │ │ Service      │ │ e            │ │ ice          │       │
│  │ - 需求CRUD   │ │ - 任务调度   │ │ - 持久型Agent│ │ - 报告生成   │       │
│  │ - 拆分指派   │ │ - 产物收集   │ │ - 主动推送   │ │ - 审计日志   │       │
│  │ - 验收流程   │ │ - 重试/取消  │ │ - 记忆管理   │ │ - 成本统计   │       │
│  └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘       │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              领域层 (Domain) 【核心】                         │
│                                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │   LLM域      │  │   Agent域    │  │   项目域     │  │   需求域     │   │
│  │ Provider     │  │ Template     │  │ Project      │  │ Requirement  │   │
│  │ ApiKey       │  │ Instance     │  │ GitHubRepo   │  │ Acceptance   │   │
│  │ Quota        │  │ Session      │  │ Workspace    │  │ Criteria     │   │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │   工作流域    │  │   执行域     │  │   产物域     │  │   租户域     │   │
│  │ Workflow     │  │ Execution    │  │ Artifact     │  │ Tenant       │   │
│  │ Node(Agent)  │  │ Task         │  │ Report       │  │ User         │   │
│  │ Edge         │  │ AgentRun     │  │ Diff         │  │ Quota        │   │
│  │ DAG Engine   │  │ SandboxRef   │  │ Decision     │  │ Role/Perm    │   │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘   │
│                                                                             │
│  领域事件:                                                                  │
│  - AgentStartedEvent / AgentCompletedEvent / AgentFailedEvent              │
│  - TaskCompletedEvent / TaskFailedEvent (复用 ARCH-001)                    │
│  - ArtifactProducedEvent / HumanApprovalRequiredEvent                      │
│  - LobsterPushEvent                                                        │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          基础设施层 (Infrastructure)                          │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                     Agent 运行时 (Agent Runtime)                     │   │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌────────────┐   │   │
│  │  │ Process    │  │ Docker     │  │ PTY        │  │ I/O        │   │   │
│  │  │ Manager    │  │ Sandbox    │  │ Terminal   │  │ Forwarder  │   │   │
│  │  │ (启动/停止)│  │ (目录/容器)│  │ (伪终端)   │  │ (WS/SSE)   │   │   │
│  │  └────────────┘  └────────────┘  └────────────┘  └────────────┘   │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │ LLM Adapter  │  │ GitHub       │  │ Persistence  │  │ Message      │   │
│  │ - Claude API │  │ Integration  │  │ - PostgreSQL │  │ Queue        │   │
│  │ - Kimi API   │  │ - OAuth      │  │ - Redis      │  │ - EventBus   │   │
│  │ - OpenAI API │  │ - Webhook    │  │ - FileStore  │  │ - WebSocket  │   │
│  │ - OpenCode   │  │ - PR API     │  │ - S3/OSS     │  │ - SSE        │   │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. 核心域模型

### 3.1 LLM 域

```java
// 聚合根
public class LLMProvider {
    private ProviderId id;
    private String name;              // "Anthropic Claude"
    private String code;              // "claude"
    private String apiBaseUrl;        // "https://api.anthropic.com"
    private EncryptedValue apiKey;    // AES-256 加密
    private String defaultModel;      // "claude-3-5-sonnet-20241022"
    private List<String> availableModels;
    private ProviderStatus status;    // ACTIVE / DISABLED
    private Quota quota;              // 月度 Token 上限
    private TenantId tenantId;        // null=平台级，有值=租户级
}

// 值对象
public class Quota {
    private long monthlyTokenLimit;
    private long monthlyTokenUsed;
    private int rpmLimit;             // Requests Per Minute
    private int qpmLimit;             // Queries Per Minute
}

public enum ProviderStatus { ACTIVE, DISABLED }
```

### 3.2 Agent 域

```java
// 聚合根：Agent 模板
public class AgentTemplate {
    private TemplateId id;
    private String name;
    private String description;
    private String icon;
    private AgentType type;           // TASK_EPHEMERAL / PERSISTENT
    private TemplateStatus status;    // DRAFT / PUBLISHED / DEPRECATED
    private Version version;
    private TenantId tenantId;        // null=平台模板

    // 运行时配置
    private RuntimeConfig runtime;
    private LLMConfig llm;
    private PersonaConfig persona;    // IDENTITY/SOUL/AGENTS/USER/TOOLS 人格分层
    private ToolSet tools;
    private ConstraintSet constraints;
    private ClaudeCodeConfig claudeCode; // Claude Code 详细配置
    private OpenCodeConfig openCode;     // OpenCode 详细配置
}

// 值对象
public class RuntimeConfig {
    private String cliType;           // "claude" / "kimi" / "opencode"
    private String cliPath;           // 可执行文件路径
    private List<String> launchArgs;  // 启动参数
}

public class LLMConfig {
    private ProviderId providerId;
    private String model;
    private String fallbackModel;
    private Map<String, String> extraSettings;
}

// Agent 人格配置（借鉴 IDENTITY/SOUL/AGENTS/USER/TOOLS 分层）
public class PersonaConfig {
    private String identityContent;   // IDENTITY.md — "我是谁？"
    private String soulContent;       // SOUL.md — "你是什么样的人"
    private String agentsContent;     // AGENTS.md — "每次会话做什么"
    private String userContent;       // USER.md — "关于你的主人"
    private String toolsContent;      // TOOLS.md — "本地笔记"
}

// Claude Code 详细配置
public class ClaudeCodeConfig {
    private String model;
    private String systemPrompt;
    private int maxThinkingTokens;
    private String permissionMode;    // default / acceptEdits / plan / bypassPermissions
    private List<String> allowedTools;
    private List<String> disallowedTools;
    private int maxTurns;
    private String cwd;               // 工作目录
    private boolean resume;
    private int timeout;              // 秒
    private String fallbackModel;
    private boolean fileCheckpointing;
    private boolean continueConversation;
    private boolean forkSession;      // 独立会话（Replica 用）
    private boolean sandboxEnabled;
    private Map<String, String> env;  // 环境变量
    private Map<String, String> extraArgs; // 额外参数
    private Map<String, McpServerConfig> mcpServers;
}

public class McpServerConfig {
    private String command;
    private List<String> args;
    private Map<String, String> env;
}

public class ToolSet {
    private List<Tool> allowedTools;  // FILE_READ, FILE_WRITE, BASH, GIT, MVN...
}

public class ConstraintSet {
    private Duration timeout;
    private long maxTokens;
    private boolean allowNetwork;
    private List<String> allowedPaths;
    private long maxFileSize;
}

// 聚合根：Agent 实例
public class AgentInstance {
    private InstanceId id;
    private TemplateId templateId;
    private String name;
    private InstanceStatus status;    // PENDING / RUNNING / PAUSED / COMPLETED / FAILED / TERMINATED
    private InstanceType type;        // EPHEMERAL / PERSISTENT
    private Sandbox sandbox;
    private ProcessRef processRef;    // 进程引用（PID / 容器ID）
    private SessionRef sessionRef;    // LLM 会话引用
    private List<Artifact> artifacts;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private String shadowFrom;        // 血缘：来源模板 Code（Replica 机制）
    private String replicaOf;         // 血缘：来源实例 ID（如果是克隆）
    private LocalDateTime lastHeartbeat; // 最后心跳时间（用于僵尸检测）
}

public enum InstanceStatus {
    PENDING, RUNNING, PAUSED, COMPLETED, FAILED, TERMINATED, HIBERNATING
}
```

### 3.3 项目域

```java
// 聚合根
public class Project {
    private ProjectId id;
    private String name;
    private String description;
    private TenantId tenantId;
    private GitHubRepo repo;
    private Workspace workspace;
    private List<ProjectConvention> conventions;
    private AgentTemplateId defaultAgentTemplateId;
    private int maxConcurrentAgents;  // 项目级并发限制（默认 3）
    private String dispatchChannelCode; // 消息渠道（如 feishu）
    private String dispatchSessionKey;  // 渠道会话 Key
}

public class GitHubRepo {
    private String owner;
    private String name;
    private String cloneUrl;
    private String defaultBranch;
    private String oauthToken;        // 加密存储
}

public class Workspace {
    private Path rootPath;            // /sandboxes/{tenant}/{project}/
    private LocalDateTime lastSyncedAt;
    private SyncStatus syncStatus;
}
```

### 3.4 需求域

```java
// 聚合根
public class Requirement {
    private RequirementId id;
    private ProjectId projectId;
    private String title;
    private String description;       // Markdown
    private List<AcceptanceCriterion> criteria;
    private RequirementStatus status;
    private List<SubTask> subTasks;
    private AgentInstanceId assignedAgentId;  // 指派的龙虾/执行 Agent
}

public class SubTask {
    private SubTaskId id;
    private String description;
    private AgentTemplateId agentTemplateId;
    private SubTaskStatus status;
    private ArtifactId outputArtifactId;
}

public enum RequirementStatus {
    PENDING, ANALYZING, DESIGNING, DEVELOPING, TESTING, REVIEWING, ACCEPTED, REJECTED
}
```

### 3.5 工作流域（升级自 ARCH-001）

```java
// 聚合根：Workflow（复用 ARCH-001，节点类型扩展）
public class Workflow {
    private WorkflowId id;
    private String name;
    private ProjectId projectId;
    private List<Node> nodes;
    private List<Edge> edges;
    private WorkflowStatus status;
}

// 节点类型升级
public class Node {
    private NodeId id;
    private String name;
    private NodeType type;            // START / AGENT_EXECUTION / HUMAN_APPROVAL / CONDITION / PARALLEL / END
    private AgentTemplateId agentTemplateId;  // AGENT_EXECUTION 类型使用
    private InputMapping inputMapping;        // 输入映射
    private OutputDefinition outputDef;       // 输出产物定义
    private Duration timeout;
    private Map<String, String> config;
}

public class InputMapping {
    private List<InputRef> refs;      // 引用上游产物或需求参数
}

public class OutputDefinition {
    private ArtifactType type;        // CODE_DIFF / TEST_REPORT / ARCH_DOC / AUDIT_REPORT
    private String filePattern;       // 产物文件匹配模式
}

public class Edge {
    private NodeId from;
    private NodeId to;
    private String condition;         // CONDITION 类型节点的分支条件表达式
}
```

### 3.6 执行域（复用并扩展 ARCH-001）

```java
// 聚合根：Execution（复用 ARCH-001）
public class Execution {
    private ExecutionId id;
    private WorkflowId workflowId;
    private RequirementId requirementId;
    private ExecutionStatus status;
    private List<AgentRun> agentRuns; // 替代原有的 Task，每个 AgentRun 对应一个 Agent 实例
}

// 新增：Agent 执行记录
public class AgentRun {
    private RunId id;
    private NodeId nodeId;
    private AgentInstanceId agentInstanceId;
    private RunStatus status;
    private ArtifactId inputArtifactId;
    private ArtifactId outputArtifactId;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
```

### 3.7 产物域

```java
// 聚合根
public class Artifact {
    private ArtifactId id;
    private ArtifactType type;
    private RunId runId;
    private String name;
    private String content;           // 文本内容或文件路径
    private Map<String, String> metadata;  // 影响范围、风险等级、文件列表等
    private LocalDateTime createdAt;
}

public enum ArtifactType {
    CODE_DIFF,           // 统一 diff 格式
    TEST_REPORT,         // 测试结果报告
    ARCH_DOC,            // 架构设计文档
    API_SPEC,            // API 接口规范
    AUDIT_REPORT,        // 代码/安全审计报告
    EXECUTION_LOG,       // 完整执行日志
    DECISION_RECORD      // 关键决策记录
}
```

### 3.8 场景模板域（新增）

```java
// 聚合根：场景模板
public class ScenarioTemplate {
    private ScenarioId id;
    private String code;              // "github_dev_workflow"
    private String name;              // "GitHub 开发协作工作流"
    private String description;
    private boolean builtIn;          // 平台内置（不可删除）
    private List<ScenarioItem> items;
    private TenantId tenantId;        // null=平台级
}

public class ScenarioItem {
    private String name;              // "Issue 分析"
    private int intervalMinutes;      // 180
    private String requirementType;   // "github_issue"
    private String agentTemplateCode; // 使用的 Agent 模板
    private int sortOrder;
    private String promptTemplate;    // 含 ${project.git_repo_url} 变量
}

// 应用到项目后生成的心跳
public class Heartbeat {
    private HeartbeatId id;
    private ProjectId projectId;
    private ScenarioId scenarioId;
    private String name;
    private int intervalMinutes;
    private boolean enabled;
    private String requirementType;
    private String agentCode;
    private String promptTemplate;
}
```

### 3.9 租户域（SaaS）

```java
// 聚合根
public class Tenant {
    private TenantId id;
    private String name;
    private TenantStatus status;
    private Quota quota;
    private List<User> users;
}

public class User {
    private UserId id;
    private TenantId tenantId;
    private String email;
    private UserRole role;            // ADMIN / MEMBER / VIEWER
}

public enum UserRole { ADMIN, MEMBER, VIEWER }
```

---

## 4. Agent 运行时架构

### 4.1 运行时组件

```
┌─────────────────────────────────────────────────────────────────┐
│                    AgentRuntimeManager                          │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  AgentInstance start(AgentStartRequest request)         │   │
│  │  void terminate(InstanceId id, boolean force)           │   │
│  │  void pause(InstanceId id)     // 持久型 Agent 休眠      │   │
│  │  void resume(InstanceId id)    // 持久型 Agent 唤醒      │   │
│  │  AgentStatus getStatus(InstanceId id)                   │   │
│  │  Stream<LogEntry> streamOutput(InstanceId id)           │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
        ┌──────────┐   ┌──────────┐   ┌──────────┐
        │ 进程模式 │   │ Docker   │   │ 远程模式 │
        │ (本地)   │   │ 容器模式 │   │ (HTTP)   │
        ├──────────┤   ├──────────┤   ├──────────┤
        │Process   │   │Docker    │   │OpenCode  │
        │Builder   │   │Container │   │serve API │
        │PTY       │   │Volume    │   │HTTP/SSE  │
        │          │   │mount     │   │          │
        └──────────┘   └──────────┘   └──────────┘
```

### 4.2 进程模式详解（Claude Code / Kimi Code）

```java
public class ProcessAgentRuntime implements AgentRuntime {

    public AgentInstance start(AgentStartRequest request) {
        // 1. 创建沙箱目录
        Sandbox sandbox = sandboxManager.create(request.getTenantId(), request.getProjectId());

        // 2. 注入工程上下文
        contextInjector.inject(sandbox, request.getProjectId(), request.getRequirementId());

        // 3. 构建启动命令
        ProcessBuilder pb = buildCommand(request, sandbox);

        // 4. 启动进程（合并 stderr 到 stdout）
        Process process = pb.redirectErrorStream(true).start();

        // 5. 创建 PTY（如果需要交互式输入）
        PTY pty = new PTY(process);

        // 6. 启动 I/O 转发线程
        ioForwarder.startForwarding(process.getInputStream(), request.getInstanceId());

        return new AgentInstance(process.pid(), sandbox, pty);
    }

    private ProcessBuilder buildCommand(AgentStartRequest request, Sandbox sandbox) {
        AgentTemplate template = templateRepo.findById(request.getTemplateId());
        LLMProvider provider = providerRepo.findById(template.getLlm().getProviderId());

        switch (template.getRuntime().getCliType()) {
            case "claude":
                return new ProcessBuilder(
                    template.getRuntime().getCliPath(),  // C:\...\claude.cmd
                    "--bare",
                    "-p", request.getPrompt(),
                    "--allowedTools", String.join(",", template.getTools().getAllowedToolNames()),
                    "--output-format", "text",
                    "--verbose"
                )
                .directory(sandbox.getRepoPath().toFile())
                .environment().put("ANTHROPIC_API_KEY", provider.getApiKey().decrypt());

            case "kimi":
                // Kimi Code 通过 PTY 交互式启动
                return new ProcessBuilder(
                    template.getRuntime().getCliPath(),
                    "acp"  // Agent Client Protocol 模式
                ).directory(sandbox.getRepoPath().toFile());

            case "opencode":
                // OpenCode serve 模式由外部管理
                throw new UnsupportedOperationException("Use HttpAgentRuntime for opencode");

            default:
                throw new IllegalArgumentException("Unknown CLI type");
        }
    }
}
```

### 4.3 HTTP 模式详解（OpenCode）

```java
public class HttpAgentRuntime implements AgentRuntime {

    public AgentInstance start(AgentStartRequest request) {
        // 1. 创建沙箱
        Sandbox sandbox = sandboxManager.create(request.getTenantId(), request.getProjectId());

        // 2. 启动 opencode serve
        int port = portAllocator.allocate();
        Process server = startOpenCodeServer(sandbox, port);

        // 3. 等待服务就绪
        awaitReady("http://localhost:" + port);

        // 4. 创建 HTTP 客户端
        OpenCodeClient client = new OpenCodeClient("http://localhost:" + port);

        // 5. 发送任务
        client.sendPrompt(request.getPrompt());

        // 6. 订阅 SSE 事件并转发
        client.subscribeEvents(event -> eventPublisher.publish(
            new AgentOutputEvent(request.getInstanceId(), event)
        ));

        return new AgentInstance(server.pid(), sandbox, client);
    }

    private Process startOpenCodeServer(Sandbox sandbox, int port) {
        return new ProcessBuilder("opencode", "serve", "--port", String.valueOf(port))
            .directory(sandbox.getRepoPath().toFile())
            .start();
    }
}
```

---

## 5. 沙箱与安全架构

### 5.1 沙箱目录结构

```
/sandboxes/
└── {tenant-id}/                          # 租户隔离
    └── {project-id}/                     # 项目隔离
        ├── current/                      # 当前活跃沙箱（符号链接）
        │   └── {task-id}/                # 任务级沙箱
        │       ├── repo/                 # git clone 的代码仓库
        │       ├── context/              # 注入的工程上下文
        │       │   ├── PRD-{id}.md       # 需求文档
        │       │   ├── ARCH-{id}.md      # 架构设计
        │       │   └── conventions.md    # 项目规范
        │       ├── tools/                # 注入的工具脚本
        │       │   ├── run-tests.sh
        │       │   └── lint-check.sh
        │       ├── output/               # Agent 产物输出
        │       │   ├── changes.patch
        │       │   └── report.md
        │       └── logs/                 # 执行日志
        │           └── agent.log
        └── archive/                      # 归档沙箱（保留 7-30 天）
            └── {task-id}-{timestamp}/
```

### 5.2 安全策略

| 层级 | 机制 | 说明 |
|------|------|------|
| **文件系统** | 目录隔离 | Agent 只能访问 `/sandboxes/{tenant}/{project}/{task}/` |
| **文件系统** | 禁止逃逸 | 禁止 `..` 路径遍历，禁止符号链接指向沙箱外 |
| **网络** | 白名单 | 默认禁止外网，可配置白名单（如 `api.github.com`） |
| **进程** | 资源限制 | CPU 限制、内存限制、执行超时 |
| **数据** | 加密 | API Key AES-256 加密，沙箱文件权限 700 |
| **审计** | 日志 | 所有文件操作、网络请求、命令执行记录审计日志 |

---

## 6. 数据架构

### 6.1 数据库 Schema 核心表

```sql
-- 租户表
CREATE TABLE tenants (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- LLM Provider 表
CREATE TABLE llm_providers (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36), -- null = 平台级
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL,
    api_base_url VARCHAR(500),
    api_key_encrypted TEXT NOT NULL,
    default_model VARCHAR(100),
    available_models TEXT, -- JSON 数组
    status VARCHAR(20) NOT NULL,
    monthly_token_limit BIGINT,
    monthly_token_used BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Agent 模板表
CREATE TABLE agent_templates (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36), -- null = 平台模板
    name VARCHAR(255) NOT NULL,
    description TEXT,
    icon VARCHAR(100),
    type VARCHAR(20) NOT NULL, -- EPHEMERAL / PERSISTENT
    status VARCHAR(20) NOT NULL,
    version VARCHAR(20) NOT NULL,
    runtime_config TEXT NOT NULL, -- JSON
    llm_config TEXT NOT NULL, -- JSON
    system_prompt TEXT NOT NULL,
    tools TEXT NOT NULL, -- JSON
    constraints TEXT NOT NULL, -- JSON
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Agent 实例表
CREATE TABLE agent_instances (
    id VARCHAR(36) PRIMARY KEY,
    template_id VARCHAR(36) NOT NULL,
    name VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    type VARCHAR(20) NOT NULL,
    sandbox_path VARCHAR(500),
    process_pid BIGINT,
    process_type VARCHAR(20), -- PROCESS / DOCKER / HTTP
    session_ref VARCHAR(255),
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL
);

-- 项目表
CREATE TABLE projects (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    github_owner VARCHAR(100),
    github_repo VARCHAR(100),
    github_clone_url VARCHAR(500),
    github_oauth_token_encrypted TEXT,
    workspace_path VARCHAR(500),
    last_synced_at TIMESTAMP,
    default_agent_template_id VARCHAR(36),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- 需求表
CREATE TABLE requirements (
    id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL,
    criteria TEXT, -- JSON 数组
    assigned_agent_id VARCHAR(36),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- 产物表
CREATE TABLE artifacts (
    id VARCHAR(36) PRIMARY KEY,
    type VARCHAR(30) NOT NULL,
    run_id VARCHAR(36),
    name VARCHAR(255),
    content TEXT,
    file_path VARCHAR(500),
    metadata TEXT, -- JSON
    created_at TIMESTAMP NOT NULL
);

-- 场景模板表
CREATE TABLE scenario_templates (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36), -- null = 平台级
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    built_in BOOLEAN NOT NULL DEFAULT FALSE,
    items TEXT NOT NULL, -- JSON 数组
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- 心跳任务表
CREATE TABLE heartbeats (
    id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL,
    scenario_id VARCHAR(36),
    name VARCHAR(255) NOT NULL,
    interval_minutes INT NOT NULL DEFAULT 60,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    requirement_type VARCHAR(50),
    agent_code VARCHAR(50),
    prompt_template TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- 审计日志表
CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36),
    action VARCHAR(50) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id VARCHAR(36),
    detail TEXT,
    ip_address VARCHAR(50),
    created_at TIMESTAMP NOT NULL
);
```

### 6.2 缓存策略

| 数据 | 缓存层 | TTL | 说明 |
|------|--------|-----|------|
| Agent 模板 | Redis | 5min | 读多写少 |
| LLM Provider 配置 | Redis | 1min | 含配额，需要较新鲜 |
| Agent 实例状态 | Redis | 实时 | Pub/Sub 推送 |
| 持久型 Agent 记忆 | Redis | 持久 | 结构化存储，定期归档 |
| GitHub Token | 无 | - | 不缓存，每次解密 |
| 沙箱文件 | 本地磁盘 | - | 直接文件系统访问 |

---

## 7. 技术决策记录 (ADR)

| 编号 | 决策 | 选项 | 选择 | 理由 |
|------|------|------|------|------|
| ADR-001 | Agent 运行时模式 | A) 自研内核 B) 包装现有 CLI | **B** | 复用成熟工具（Claude Code/OpenCode），降低开发成本 |
| ADR-002 | 进程隔离方案 | A) 目录隔离 B) Docker 容器 C) K8s Pod | **A+B** | MVP 用目录隔离（轻量），生产用 Docker（安全） |
| ADR-003 | Agent 透传协议 | A) WebSocket B) SSE C) 长轮询 | **A+B** | WebSocket 用于双向交互（人类干预），SSE 用于单向日志 |
| ADR-004 | LLM API Key 存储 | A) 环境变量 B) 数据库明文 C) 数据库加密 | **C** | 支持多租户动态配置，AES-256 加密 |
| ADR-005 | 持久型 Agent 会话 | A) 进程常驻 B) 休眠唤醒 C) 无状态每次重建 | **B** | 平衡资源占用和响应速度 |
| ADR-006 | 多租户隔离 | A) Schema 隔离 B) 行级隔离 C) 独立数据库 | **B** | 成本可控，Spring 过滤条件即可实现 |
| ADR-007 | 产物存储 | A) 数据库存大字段 B) 对象存储 C) 本地文件 | **B+C** | 小产物存本地文件，大产物/归档存 S3/OSS |
| ADR-008 | 前端状态管理 | A) Pinia B) Vuex C) Redux | **A** | 项目已用 Pinia，保持一致 |
| ADR-009 | Agent 分身机制 | A) 引用模板 B) 深拷贝 Replica | **B** | 隔离性更好，支持血缘追踪和独立配置 |
| ADR-010 | Prompt 构建方式 | A) 字符串拼接 B) 模板引擎 | **B** | 支持变量替换和分层组装，更易维护 |
| ADR-011 | 工作流 vs 场景 | A) 只保留 DAG B) 只保留场景 C) DAG+场景 | **C** | DAG 灵活但门槛高，场景模板降低入门成本 |
| ADR-012 | 定时任务调度 | A) Spring Scheduler B) Quartz C) 自定义 | **B** | Quartz 支持集群、持久化、容错，适合生产 |

---

## 8. 风险评估

| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|------|---------|
| LLM API 调用失败/限流 | 高 | 中 | 多 Provider 负载均衡，自动 Fallback |
| Agent 进程僵尸/泄漏 | 高 | 中 | 进程守护线程，超时强制 kill，资源监控 |
| 沙箱逃逸 | 高 | 低 | Docker 容器隔离，seccomp 配置，只读根目录 |
| 大仓库 Clone 超时 | 中 | 高 | Sparse checkout，增量同步，异步预加载 |
| 多 Agent 状态不一致 | 高 | 中 | 产物契约化，数据库事务，补偿机制 |
| Prompt 注入攻击 | 中 | 中 | 输入校验，模板转义，工具权限白名单 |
| Agent 过度交互 | 中 | 中 | 冷却机制（Cooldown），平台层 + Prompt 层双保险 |
| 服务器异常后僵尸状态 | 高 | 中 | 启动僵尸清理器，自动标记 FAILED 并释放资源 |
| Token 费用失控 | 中 | 中 | 配额硬限制，预算告警，成本实时展示 |

---

## 9. 验收标准

- [ ] DDD 分层正确，领域层无 Spring 依赖
- [ ] 新增 7 个核心域（LLM/Agent/Project/Requirement/Execution/Artifact/Tenant）
- [ ] Agent 运行时支持进程模式 + HTTP 模式
- [ ] 沙箱目录隔离可用，Agent 无法逃逸
- [ ] SSE 实时透传延迟 < 100ms
- [ ] LLM Provider 支持至少 3 家（Claude/Kimi/OpenAI）
- [ ] 多租户数据隔离验证通过
- [ ] 单元测试覆盖率：Domain ≥ 95%，整体 ≥ 80%
- [ ] Checkstyle 配置修复并通过检查
- [ ] 前端工作流编辑器重构完成，支持 Agent 节点类型
- [ ] **Agent 人格分层**（IDENTITY/SOUL/AGENTS/USER/TOOLS）在域模型中落地
- [ ] **Agent Replica 分身**机制：深拷贝 + shadowFrom 血缘 + 自动隔离
- [ ] **Prompt 模板引擎**支持变量替换和分层组装
- [ ] **场景模板**（ScenarioTemplate）域模型和内置场景可用
- [ ] **启动僵尸清理**（AgentStartupCleaner）自动恢复状态一致性
- [ ] **项目级并发限制**生效
- [ ] **冷却机制**（Cooldown）防止 Agent 过度交互
- [ ] **GitHub Webhook**自动触发心跳任务

---

**最后更新**: 2026-04-21  
**版本**: v2.0  
**状态**: 待评审（已吸收 weibaohui/tasks 参考项目 10 大工程实践），待 AI 评审
