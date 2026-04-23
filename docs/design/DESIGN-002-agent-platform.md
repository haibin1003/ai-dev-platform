# DESIGN-002: AI 智能体平台详细设计

**版本**: v2.0  
**状态**: 待评审（已吸收 weibaohui/tasks 参考项目 10 大工程实践）  
**负责人**: 待分配  
**关联**: ARCH-002, PRD-003  
**创建日期**: 2026-04-21

---

## 1. 设计目标

将 ARCH-002 架构设计转化为可执行的开发蓝图，明确：
- 后端模块划分和依赖关系
- 新增/修改的数据库表结构
- 核心类设计和接口定义
- RESTful API 详细契约
- 前端页面和组件设计
- 开发任务分解和验收标准

---

## 2. 模块划分

### 2.1 后端模块结构

```
src/main/java/com/aidev/
├── api/                                    # 接口层
│   ├── controller/
│   │   ├── LLMProviderController.java      # LLM 提供商管理
│   │   ├── AgentTemplateController.java    # Agent 模板管理
│   │   ├── AgentInstanceController.java    # Agent 实例管理
│   │   ├── ProjectController.java          # 项目管理
│   │   ├── RequirementController.java      # 需求管理
│   │   ├── WorkflowController.java         # 复用并扩展
│   │   ├── ExecutionController.java        # 复用并扩展
│   │   ├── ReportController.java           # 报告与审计
│   │   └── LobsterController.java          # 个人助理接口
│   ├── dto/
│   │   ├── llm/
│   │   ├── agent/
│   │   ├── project/
│   │   ├── requirement/
│   │   └── report/
│   └── exception/
│       └── GlobalExceptionHandler.java     # 扩展错误码
│
├── application/                            # 应用层
│   ├── service/
│   │   ├── LLMAppService.java
│   │   ├── AgentAppService.java
│   │   ├── AgentRuntimeService.java        # Agent 生命周期
│   │   ├── ProjectAppService.java
│   │   ├── RequirementAppService.java
│   │   ├── WorkflowAppService.java         # 复用并扩展
│   │   ├── ExecutionAppService.java        # 复用并扩展
│   │   ├── LobsterAppService.java
│   │   └── ReportAppService.java
│   ├── event/
│   │   ├── AgentEventHandler.java
│   │   ├── ExecutionEventHandler.java
│   │   └── LobsterEventHandler.java
│   └── port/
│       ├── AgentRuntime.java               # Agent 运行时端口
│       ├── LLMClient.java                  # LLM 客户端端口
│       ├── SandboxManager.java             # 沙箱管理端口
│       ├── GitHubClient.java               # GitHub 集成端口
│       └── ArtifactCollector.java          # 产物收集端口
│
├── domain/                                 # 领域层 【核心】
│   ├── model/
│   │   ├── aggregate/
│   │   │   ├── LLMProvider.java            # 新增
│   │   │   ├── AgentTemplate.java          # 新增
│   │   │   ├── AgentInstance.java          # 新增
│   │   │   ├── Project.java                # 新增
│   │   │   ├── Requirement.java            # 新增
│   │   │   ├── Workflow.java               # 复用并扩展
│   │   │   ├── Execution.java              # 复用并扩展
│   │   │   └── Artifact.java               # 新增
│   │   ├── entity/
│   │   │   ├── Node.java                   # 扩展：新增 AGENT_EXECUTION 等类型
│   │   │   ├── Edge.java                   # 复用
│   │   │   ├── AgentRun.java               # 新增：替代 Task
│   │   │   ├── SubTask.java                # 新增
│   │   │   └── Tenant.java                 # 新增
│   │   └── valueobject/
│   │       ├── ProviderId.java             # 新增
│   │       ├── TemplateId.java             # 新增
│   │       ├── InstanceId.java             # 新增
│   │       ├── ProjectId.java              # 新增
│   │       ├── RequirementId.java          # 新增
│   │       ├── ArtifactId.java             # 新增
│   │       ├── TenantId.java               # 新增
│   │       ├── EncryptedValue.java         # 新增：加密值对象
│   │       ├── AgentType.java              # 新增
│   │       ├── InstanceStatus.java         # 新增
│   │       ├── ArtifactType.java           # 新增
│   │       ├── NodeType.java               # 扩展
│   │       └── RequirementStatus.java      # 新增
│   ├── service/
│   │   ├── DAGScheduler.java               # 复用并扩展：支持产物传递
│   │   ├── TopologicalSorter.java          # 复用
│   │   ├── CycleDetector.java              # 复用
│   │   └── ArtifactResolver.java           # 新增：产物解析与传递
│   ├── repository/
│   │   ├── LLMProviderRepository.java      # 新增
│   │   ├── AgentTemplateRepository.java    # 新增
│   │   ├── AgentInstanceRepository.java    # 新增
│   │   ├── ProjectRepository.java          # 新增
│   │   ├── RequirementRepository.java      # 新增
│   │   ├── ArtifactRepository.java         # 新增
│   │   └── TenantRepository.java           # 新增
│   └── event/
│       ├── AgentStartedEvent.java          # 新增
│       ├── AgentCompletedEvent.java        # 新增
│       ├── AgentFailedEvent.java           # 新增
│       ├── ArtifactProducedEvent.java      # 新增
│       └── HumanApprovalRequiredEvent.java # 新增
│
└── infrastructure/                         # 基础设施层
    ├── config/
    │   ├── SecurityConfig.java             # 新增：加密、认证
    │   ├── TenantConfig.java               # 新增：多租户
    │   └── WorkflowEngineConfig.java       # 复用
    ├── persistence/
    │   ├── entity/                         # JPA Entity（新增约 8 个）
    │   ├── repository/                     # JPA Repository 实现
    │   └── mapper/                         # Domain ↔ JPA 转换
    ├── adapter/
    │   ├── agent/
    │   │   ├── ProcessAgentRuntime.java    # 进程模式实现
    │   │   ├── HttpAgentRuntime.java       # HTTP 模式实现
    │   │   └── IOForwarder.java            # I/O 转发器
    │   ├── llm/
    │   │   ├── ClaudeClient.java           # Claude API 客户端
    │   │   ├── KimiClient.java             # Kimi API 客户端
    │   │   ├── OpenAIClient.java           # OpenAI API 客户端
    │   │   └── LLMClientFactory.java       # 客户端工厂
    │   ├── github/
    │   │   ├── GitHubClientImpl.java       # GitHub API 集成
    │   │   └── GitHubWebhookHandler.java   # Webhook 处理
    │   └── sandbox/
    │       ├── DirectorySandboxManager.java # 目录沙箱
    │       └── DockerSandboxManager.java   # Docker 沙箱（预留）
    ├── security/
    │   ├── AesEncryptionService.java       # AES-256 加密
    │   └── TenantContext.java              # 租户上下文
    └── websocket/
        ├── AgentLogWebSocketHandler.java   # Agent 实时日志
        └── LobsterPushHandler.java         # 龙虾推送
```

### 2.2 前端模块结构

```
frontend/src/
├── views/                                  # 页面
│   ├── agent/
│   │   ├── AgentMarketView.vue             # Agent 市场
│   │   ├── AgentTemplateEditView.vue       # 模板编辑
│   │   └── AgentInstanceListView.vue       # 实例列表
│   ├── llm/
│   │   └── LLMProviderView.vue             # LLM 管理
│   ├── project/
│   │   ├── ProjectListView.vue             # 项目列表
│   │   └── ProjectDetailView.vue           # 项目详情
│   ├── requirement/
│   │   ├── RequirementListView.vue         # 需求列表
│   │   └── RequirementDetailView.vue       # 需求详情
│   ├── workflow/
│   │   ├── WorkflowEditorView.vue          # 重构：支持 Agent 节点
│   │   └── WorkflowListView.vue            # 复用
│   ├── execution/
│   │   ├── ExecutionMonitorView.vue        # 执行监控（升级）
│   │   └── ExecutionReportView.vue         # 执行报告
│   ├── scenario/
│   │   ├── ScenarioMarketView.vue          # 场景市场
│   │   └── ScenarioApplyView.vue           # 场景应用确认
│   └── lobster/
│       └── LobsterChatView.vue             # 龙虾聊天界面
├── components/
│   ├── agent/
│   │   ├── AgentNode.vue                   # 工作流中的 Agent 节点
│   │   ├── AgentSelector.vue               # Agent 选择器
│   │   └── AgentStatusBadge.vue            # 状态徽标
│   ├── workflow/
│   │   ├── nodes/
│   │   │   ├── AgentExecutionNode.vue      # Agent 执行节点
│   │   │   ├── HumanApprovalNode.vue       # 人工审批节点
│   │   │   └── ConditionNode.vue           # 条件节点
│   │   ├── panels/
│   │   │   ├── NodePalette.vue             # 重构：修复端口
│   │   │   ├── PropertyPanel.vue           # 重构：支持 Agent 配置
│   │   │   └── ToolbarPanel.vue            # 重构：修复 props
│   │   └── ArtifactPreview.vue             # 产物预览
│   ├── lobster/
│   │   ├── ChatMessage.vue                 # 聊天消息
│   │   └── TaskCard.vue                    # 任务卡片
│   └── report/
│       ├── DiffViewer.vue                  # 代码 diff 查看器
│       └── ReportRenderer.vue              # 报告渲染器
├── services/
│   ├── llm.ts
│   ├── agent.ts                            # 扩展
│   ├── project.ts
│   ├── requirement.ts
│   └── report.ts
├── stores/
│   ├── tenant.ts                           # 租户状态
│   ├── agent.ts                            # Agent 状态
│   └── lobster.ts                          # 龙虾状态
└── types/
    ├── agent.ts                            # 扩展
    ├── llm.ts
    ├── project.ts
    └── requirement.ts
```

---

## 3. 核心类设计

### 3.1 Agent 运行时类图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        AgentRuntime (Port Interface)                        │
├─────────────────────────────────────────────────────────────────────────────┤
│ + start(AgentStartRequest): AgentInstance                                   │
│ + terminate(InstanceId, boolean): void                                      │
│ + pause(InstanceId): void                                                   │
│ + resume(InstanceId): void                                                  │
│ + getStatus(InstanceId): AgentStatus                                        │
│ + streamOutput(InstanceId): Stream<LogEntry>                                │
│ + sendInput(InstanceId, String): void                                       │
└─────────────────────────────────────────────────────────────────────────────┘
        △                                    △
        │                                    │
┌───────┴──────────────┐          ┌─────────┴──────────┐
│ ProcessAgentRuntime  │          │ HttpAgentRuntime   │
├──────────────────────┤          ├────────────────────┤
│ - processManager     │          │ - httpClient       │
│ - ptyFactory         │          │ - sseSubscriber    │
│ - ioForwarder        │          │ - portAllocator    │
├──────────────────────┤          ├────────────────────┤
│ + start()            │          │ + start()          │
│ + terminate()        │          │ + terminate()      │
│ - buildCommand()     │          │ - startServer()    │
│ - createPTY()        │          │ - awaitReady()     │
└──────────────────────┘          └────────────────────┘
```

### 3.2 核心服务类设计

```java
/**
 * Agent 应用服务：编排 Agent 生命周期。
 */
@Service
@Transactional
public class AgentAppService {

    private final AgentTemplateRepository templateRepo;
    private final AgentInstanceRepository instanceRepo;
    private final AgentRuntime agentRuntime;
    private final SandboxManager sandboxManager;
    private final ContextInjector contextInjector;
    private final DomainEventPublisher eventPublisher;

    public AgentAppService(AgentTemplateRepository templateRepo,
                           AgentInstanceRepository instanceRepo,
                           AgentRuntime agentRuntime,
                           SandboxManager sandboxManager,
                           ContextInjector contextInjector,
                           DomainEventPublisher eventPublisher) {
        this.templateRepo = templateRepo;
        this.instanceRepo = instanceRepo;
        this.agentRuntime = agentRuntime;
        this.sandboxManager = sandboxManager;
        this.contextInjector = contextInjector;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 创建并启动任务型 Agent 实例。
     */
    public AgentInstanceResponse createAndStartInstance(CreateInstanceRequest request) {
        AgentTemplate template = templateRepo.findById(TemplateId.of(request.templateId()))
            .orElseThrow(() -> new AgentTemplateNotFoundException(request.templateId()));

        AgentInstance instance = AgentInstance.createEphemeral(
            template.getId(),
            request.name(),
            request.projectId(),
            request.requirementId()
        );

        instanceRepo.save(instance);

        // 异步启动（避免阻塞 HTTP 请求）
        CompletableFuture.runAsync(() -> startInstanceInternal(instance, request.prompt()));

        return toResponse(instance);
    }

    /**
     * 创建持久型 Agent 实例（龙虾）。
     */
    public AgentInstanceResponse createPersistentInstance(CreateInstanceRequest request) {
        AgentTemplate template = templateRepo.findById(TemplateId.of(request.templateId()))
            .orElseThrow(() -> new AgentTemplateNotFoundException(request.templateId()));

        AgentInstance instance = AgentInstance.createPersistent(
            template.getId(),
            request.name()
        );

        instanceRepo.save(instance);

        // 持久型 Agent 立即启动并常驻
        CompletableFuture.runAsync(() -> {
            startInstanceInternal(instance, request.prompt());
            // 持久型 Agent 启动后进入待命状态
        });

        return toResponse(instance);
    }

    /**
     * 终止 Agent 实例。
     */
    public void terminateInstance(String id) {
        AgentInstance instance = instanceRepo.findById(InstanceId.of(id))
            .orElseThrow(() -> new AgentInstanceNotFoundException(id));

        agentRuntime.terminate(instance.getId(), false);
        instance.terminate();
        instanceRepo.save(instance);
    }

    /**
     * 向持久型 Agent 发送消息。
     */
    public void sendMessageToPersistentAgent(String id, String message) {
        AgentInstance instance = instanceRepo.findById(InstanceId.of(id))
            .orElseThrow(() -> new AgentInstanceNotFoundException(id));

        if (!instance.isPersistent()) {
            throw new IllegalStateException("Only persistent agents can receive messages");
        }

        if (instance.getStatus() == InstanceStatus.HIBERNATING) {
            // 唤醒休眠的 Agent
            resumeInstance(id);
        }

        agentRuntime.sendInput(instance.getId(), message);
    }

    // ==================== 私有方法 ====================

    private void startInstanceInternal(AgentInstance instance, String prompt) {
        try {
            Sandbox sandbox = sandboxManager.create(
                instance.getTenantId(),
                instance.getProjectId()
            );

            if (instance.getProjectId() != null) {
                contextInjector.inject(sandbox, instance.getProjectId(), instance.getRequirementId());
            }

            AgentStartRequest startRequest = AgentStartRequest.builder()
                .instanceId(instance.getId())
                .templateId(instance.getTemplateId())
                .prompt(prompt)
                .sandbox(sandbox)
                .build();

            AgentInstance started = agentRuntime.start(startRequest);
            instance.markRunning(started.getProcessRef(), started.getSessionRef());
            instanceRepo.save(instance);

            eventPublisher.publish(new AgentStartedEvent(instance.getId()));

            // 启动 I/O 监听线程
            startOutputListener(instance);

        } catch (Exception e) {
            instance.markFailed(e.getMessage());
            instanceRepo.save(instance);
            eventPublisher.publish(new AgentFailedEvent(instance.getId(), e.getMessage()));
        }
    }

    private void startOutputListener(AgentInstance instance) {
        CompletableFuture.runAsync(() -> {
            try (Stream<LogEntry> stream = agentRuntime.streamOutput(instance.getId())) {
                stream.forEach(log -> {
                    // 存储日志
                    // 如果是持久型 Agent，分析是否需要主动推送
                    if (instance.isPersistent() && log.requiresHumanAttention()) {
                        eventPublisher.publish(new LobsterPushEvent(
                            instance.getId(),
                            log.getMessage(),
                            PushType.DECISION_REQUIRED
                        ));
                    }
                });

                // 流结束，检查进程状态
                AgentStatus status = agentRuntime.getStatus(instance.getId());
                if (status.isCompleted()) {
                    instance.markCompleted();
                    collectArtifacts(instance);
                    eventPublisher.publish(new AgentCompletedEvent(instance.getId()));
                } else if (status.isFailed()) {
                    instance.markFailed(status.getErrorMessage());
                    eventPublisher.publish(new AgentFailedEvent(
                        instance.getId(), status.getErrorMessage()
                    ));
                }

                instanceRepo.save(instance);

            } catch (Exception e) {
                logger.error("Output listener error for instance {}", instance.getId(), e);
            }
        });
    }

    private void collectArtifacts(AgentInstance instance) {
        // 从沙箱提取产物
        // 1. git diff
        // 2. 报告文件
        // 3. 日志归档
    }

    private AgentInstanceResponse toResponse(AgentInstance instance) {
        return new AgentInstanceResponse(
            instance.getId().getValue(),
            instance.getName(),
            instance.getStatus().name(),
            instance.getType().name(),
            instance.getCreatedAt()
        );
    }
}
```

### 3.3 Agent Snapshot 与 Replica 机制

```java
/**
 * Agent 快照：支持深拷贝和还原，用于 Replica 分身机制。
 */
public class AgentSnapshot {
    private final AgentId id;
    private final AgentCode code;
    private final AgentType type;
    private final String name;
    private final PersonaConfig persona;
    private final RuntimeConfig runtime;
    private final LLMConfig llm;
    private final ToolSet tools;
    private final ConstraintSet constraints;
    private final ClaudeCodeConfig claudeCode;
    private final OpenCodeConfig openCode;
    
    public static AgentSnapshot from(AgentTemplate template) {
        // 深拷贝所有配置
        return new AgentSnapshot(...);
    }
    
    public AgentInstance toInstance(InstanceId id, AgentCode code, 
                                     String name, String shadowFrom) {
        AgentInstance instance = new AgentInstance(id, code, name);
        instance.applySnapshot(this);
        instance.setShadowFrom(shadowFrom);
        return instance;
    }
}

/**
 * Agent 分身工厂：从模板创建独立的 Replica Agent 实例。
 */
@Component
public class ReplicaAgentFactory {
    
    public AgentInstance createReplica(AgentTemplate template, 
                                       ProjectId projectId,
                                       RequirementId requirementId,
                                       String workspacePath) {
        AgentSnapshot snap = AgentSnapshot.from(template);
        
        // 生成唯一 ID 和 Code
        InstanceId id = InstanceId.of(UUID.randomUUID().toString());
        AgentCode code = AgentCode.of("agt_" + idGenerator.generate());
        String name = template.getName() + "-replica-" + requirementId.getValue();
        
        AgentInstance replica = snap.toInstance(id, code, name, template.getCode());
        replica.setProjectId(projectId);
        replica.setRequirementId(requirementId);
        
        // 自动配置工作目录隔离
        if (template.getRuntime().getCliType().equals("claude")) {
            ClaudeCodeConfig cfg = snap.getClaudeCode().clone();
            cfg.setCwd(workspacePath);
            cfg.setForkSession(true);
            cfg.setContinueConversation(false);
            replica.setClaudeCodeConfig(cfg);
        }
        
        return replica;
    }
}
```

### 3.4 Prompt 模板引擎

```java
/**
 * Prompt 模板引擎：支持变量替换和分层组装。
 */
@Component
public class PromptTemplateEngine {
    
    private final Map<String, CompiledTemplate> cache = new ConcurrentHashMap<>();
    
    public String render(String template, RenderContext ctx) {
        // 变量替换
        String result = template
            .replace("${project.name}", ctx.getProjectName())
            .replace("${project.git_repo_url}", ctx.getGitRepoUrl())
            .replace("${project.default_branch}", ctx.getDefaultBranch())
            .replace("${requirement.title}", ctx.getRequirementTitle())
            .replace("${requirement.description}", ctx.getRequirementDescription())
            .replace("${workspace.path}", ctx.getWorkspacePath())
            .replace("${timestamp}", Instant.now().toString())
            .replace("${state.current}", ctx.getCurrentState())
            .replace("${state.ai_guide}", ctx.getAIGuide());
        
        return result;
    }
    
    public String buildDispatchPrompt(AgentTemplate template, Requirement req, 
                                       Project project, String workspacePath,
                                       String currentState, String aiGuide) {
        // 分层组装
        StringBuilder prompt = new StringBuilder();
        
        // L1: 平台基座（执行契约）
        prompt.append(buildExecutionContract(currentState));
        
        // L2: Agent 人格
        prompt.append(template.getPersona().getIdentityContent()).append("\n");
        prompt.append(template.getPersona().getSoulContent()).append("\n");
        prompt.append(template.getPersona().getAgentsContent()).append("\n");
        
        // L3: 项目上下文
        prompt.append(template.getPersona().getUserContent()).append("\n");
        prompt.append("【项目信息】\n")
              .append("- 仓库地址：").append(project.getGitRepoUrl()).append("\n")
              .append("- 默认分支：").append(project.getDefaultBranch()).append("\n");
        
        // L4: 任务特定
        prompt.append("【需求信息】\n")
              .append("- 标题：").append(req.getTitle()).append("\n")
              .append("- 描述：").append(req.getDescription()).append("\n");
        
        prompt.append("【工作目录】\n")
              .append("- 路径：").append(workspacePath).append("\n");
        
        if (aiGuide != null) {
            prompt.append("【AI 指南】\n").append(aiGuide).append("\n");
        }
        
        return prompt.toString();
    }
    
    private String buildExecutionContract(String currentState) {
        return """
            【执行契约 - 严格遵守】
            当前阶段：%s
            下一步动作：执行状态转换进入下一阶段
            
            执行规则：
            1. 必须执行命令，不输出解释性长文本
            2. 每个 PHASE 完成后必须执行 ON_PHASE_COMPLETE 命令
            3. 命令失败自动修复重试（最多3次）
            4. 优先执行操作，不做过多分析
            """.formatted(currentState);
    }
}
```

### 3.5 启动僵尸清理器

```java
/**
 * Agent 启动清理器：应用启动时扫描并清理异常状态的 Agent 实例。
 */
@Component
public class AgentStartupCleaner implements ApplicationRunner {
    
    private final AgentInstanceRepository instanceRepo;
    private final AgentRuntime agentRuntime;
    private final SandboxManager sandboxManager;
    
    @Override
    public void run(ApplicationArguments args) {
        log.info("[StartupCleaner] 开始扫描僵尸 Agent 实例...");
        
        List<AgentInstance> staleInstances = instanceRepo.findByStatusIn(
            List.of(InstanceStatus.RUNNING, InstanceStatus.PENDING)
        );
        
        int cleaned = 0;
        for (AgentInstance instance : staleInstances) {
            boolean shouldCleanup = false;
            String reason = "";
            
            // 检查进程是否存在
            if (instance.getProcessPid() != null) {
                boolean processExists = isProcessAlive(instance.getProcessPid());
                if (!processExists) {
                    shouldCleanup = true;
                    reason = "replica agent missing";
                }
            }
            
            // 检查超时
            if (!shouldCleanup && instance.getLastHeartbeat() != null) {
                Duration idle = Duration.between(instance.getLastHeartbeat(), Instant.now());
                if (idle.compareTo(Duration.ofMinutes(30)) > 0) {
                    shouldCleanup = true;
                    reason = "timeout - no update for 30+ minutes";
                }
            }
            
            if (shouldCleanup) {
                cleanup(instance, reason);
                cleaned++;
            }
        }
        
        log.info("[StartupCleaner] 清理完成，共清理 {} 个僵尸实例", cleaned);
    }
    
    private void cleanup(AgentInstance instance, String reason) {
        // 1. 终止进程（如果还在）
        if (instance.getProcessPid() != null) {
            agentRuntime.terminate(instance.getId(), true);
        }
        
        // 2. 清理沙箱
        if (instance.getSandboxPath() != null) {
            sandboxManager.remove(instance.getSandboxPath());
        }
        
        // 3. 标记为失败
        instance.markFailed("cleanup: " + reason);
        instanceRepo.save(instance);
    }
}
```

### 3.6 产物解析与传递

```java
/**
 * 产物解析器：负责解析 Agent 输出，提取结构化产物。
 */
@Component
public class ArtifactResolver {

    private final ArtifactRepository artifactRepo;

    public List<Artifact> resolveArtifacts(AgentInstance instance, Sandbox sandbox) {
        List<Artifact> artifacts = new ArrayList<>();

        // 1. 解析代码 diff
        Path diffPath = sandbox.getOutputPath().resolve("changes.patch");
        if (Files.exists(diffPath)) {
            artifacts.add(createCodeDiffArtifact(instance, diffPath));
        }

        // 2. 解析报告文件
        Path reportPath = sandbox.getOutputPath().resolve("report.md");
        if (Files.exists(reportPath)) {
            artifacts.add(createReportArtifact(instance, reportPath));
        }

        // 3. 解析测试报告
        Path testReportPath = sandbox.getOutputPath().resolve("test-report.json");
        if (Files.exists(testReportPath)) {
            artifacts.add(createTestReportArtifact(instance, testReportPath));
        }

        // 4. 收集执行日志
        Path logPath = sandbox.getLogPath();
        if (Files.exists(logPath)) {
            artifacts.add(createLogArtifact(instance, logPath));
        }

        return artifacts;
    }

    private Artifact createCodeDiffArtifact(AgentInstance instance, Path path) {
        try {
            String diff = Files.readString(path);
            return Artifact.createCodeDiff(
                instance.getId(),
                diff,
                Map.of("fileCount", String.valueOf(countFilesInDiff(diff)))
            );
        } catch (IOException e) {
            throw new ArtifactParseException("Failed to read diff", e);
        }
    }
}
```

---

## 4. 数据库详细设计

### 4.1 ER 关系图

```
┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│   Tenant     │1─────N│    User      │       │ LLMProvider  │
│  (租户)      │       │   (用户)     │       │ (LLM提供商)  │
└──────────────┘       └──────────────┘       └──────┬───────┘
                                                     │
                              ┌──────────────────────┘
                              │
┌──────────────┐       ┌─────┴────────┐       ┌──────────────┐
│   Project    │1─────N│AgentTemplate │1─────N│AgentInstance │
│  (项目)      │       │ (Agent模板)  │       │(Agent实例)   │
└──────┬───────┘       └──────────────┘       └──────┬───────┘
       │                                              │
       │1                                            N│
       │                                              │
┌──────┴───────┐       ┌──────────────┐       ┌─────┴────────┐
│  Requirement │1─────N│   Workflow   │1─────N│  Execution   │
│   (需求)     │       │  (工作流)    │       │  (执行记录)  │
└──────────────┘       └──────────────┘       └──────┬───────┘
                                                     │
                                                    N│
                                              ┌─────┴────────┐
                                              │  AgentRun    │
                                              │(Agent执行记录)│
                                              └──────┬───────┘
                                                     │
                                                    N│
                                              ┌─────┴────────┐
                                              │   Artifact   │
                                              │   (产物)     │
                                              └──────────────┘
```

### 4.2 表结构详情

```sql
-- ==================== 租户与用户 ====================

CREATE TABLE tenants (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) UNIQUE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    quota_config TEXT, -- JSON: 并发数、Token上限、存储上限
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(100),
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    UNIQUE (tenant_id, email)
);

-- ==================== LLM 提供商 ====================

CREATE TABLE llm_providers (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36), -- NULL = 平台级
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL,
    api_base_url VARCHAR(500),
    api_key_encrypted TEXT NOT NULL,
    default_model VARCHAR(100),
    available_models TEXT, -- JSON array
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    monthly_token_limit BIGINT,
    monthly_token_used BIGINT DEFAULT 0,
    rpm_limit INT DEFAULT 60,
    qpm_limit INT DEFAULT 60,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE INDEX idx_llm_providers_tenant ON llm_providers(tenant_id);

-- ==================== 场景模板 ====================

CREATE TABLE scenario_templates (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36), -- NULL = 平台模板
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL, -- HEARTBEAT / CODE_REVIEW / BUG_FIX / CUSTOM
    icon VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    version VARCHAR(20) NOT NULL DEFAULT '1.0.0',
    steps TEXT NOT NULL, -- JSON: 步骤定义数组
    node_mappings TEXT NOT NULL, -- JSON: 步骤 -> NodeType 映射
    default_agent_assignments TEXT, -- JSON: 默认 Agent 模板分配
    input_schema TEXT, -- JSON: 用户输入参数 Schema
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE INDEX idx_scenario_templates_tenant ON scenario_templates(tenant_id);
CREATE INDEX idx_scenario_templates_category ON scenario_templates(category);

-- ==================== Agent 模板 ====================

CREATE TABLE agent_templates (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36), -- NULL = 平台模板
    name VARCHAR(255) NOT NULL,
    description TEXT,
    icon VARCHAR(100),
    type VARCHAR(20) NOT NULL, -- EPHEMERAL / PERSISTENT
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    version VARCHAR(20) NOT NULL DEFAULT '1.0.0',
    parent_template_id VARCHAR(36), -- 克隆来源
    runtime_config TEXT NOT NULL, -- JSON
    llm_config TEXT NOT NULL, -- JSON
    system_prompt TEXT NOT NULL,
    tools TEXT NOT NULL, -- JSON
    constraints TEXT NOT NULL, -- JSON
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    FOREIGN KEY (parent_template_id) REFERENCES agent_templates(id)
);

CREATE INDEX idx_agent_templates_tenant ON agent_templates(tenant_id);
CREATE INDEX idx_agent_templates_status ON agent_templates(status);

-- ==================== Agent 实例 ====================

CREATE TABLE agent_instances (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL,
    template_id VARCHAR(36) NOT NULL,
    name VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    type VARCHAR(20) NOT NULL, -- EPHEMERAL / PERSISTENT
    project_id VARCHAR(36),
    requirement_id VARCHAR(36),
    sandbox_path VARCHAR(500),
    process_pid BIGINT,
    process_type VARCHAR(20), -- PROCESS / DOCKER / HTTP
    process_endpoint VARCHAR(500), -- HTTP 模式时的地址
    session_ref VARCHAR(255),
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    shadow_from VARCHAR(36), -- 基础模板 code（血缘追踪）
    last_heartbeat TIMESTAMP,
    persona_identity TEXT, -- IDENTITY 人格内容
    persona_soul TEXT,     -- SOUL 人格内容
    persona_agents TEXT,   -- AGENTS 会话规范
    persona_user TEXT,     -- USER 上下文
    persona_tools TEXT,    -- TOOLS 本地速查
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    FOREIGN KEY (template_id) REFERENCES agent_templates(id)
);

CREATE INDEX idx_agent_instances_tenant ON agent_instances(tenant_id);
CREATE INDEX idx_agent_instances_status ON agent_instances(status);
CREATE INDEX idx_agent_instances_project ON agent_instances(project_id);

-- ==================== 项目 ====================

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
    sync_status VARCHAR(20) DEFAULT 'PENDING',
    default_agent_template_id VARCHAR(36),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE INDEX idx_projects_tenant ON projects(tenant_id);

-- ==================== 需求 ====================

CREATE TABLE requirements (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL,
    project_id VARCHAR(36) NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    criteria TEXT, -- JSON array
    assigned_agent_id VARCHAR(36),
    metadata TEXT, -- JSON
    created_by VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE INDEX idx_requirements_project ON requirements(project_id);
CREATE INDEX idx_requirements_status ON requirements(status);

-- ==================== 工作流（扩展） ====================

-- workflows 表复用现有结构，扩展字段
ALTER TABLE workflows ADD COLUMN project_id VARCHAR(36);
ALTER TABLE workflows ADD COLUMN requirement_id VARCHAR(36);

-- nodes 表复用现有结构，扩展 type 枚举
-- 注意：需要在应用层处理新的 NodeType

-- ==================== 执行（扩展） ====================

-- executions 表复用现有结构

-- agent_runs 表（替代/扩展 tasks）
CREATE TABLE agent_runs (
    id VARCHAR(36) PRIMARY KEY,
    execution_id VARCHAR(36) NOT NULL,
    node_id VARCHAR(36) NOT NULL,
    agent_instance_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    input_artifact_id VARCHAR(36),
    output_artifact_id VARCHAR(36),
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (execution_id) REFERENCES executions(id),
    FOREIGN KEY (agent_instance_id) REFERENCES agent_instances(id)
);

CREATE INDEX idx_agent_runs_execution ON agent_runs(execution_id);

-- ==================== 产物 ====================

CREATE TABLE artifacts (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL,
    type VARCHAR(30) NOT NULL,
    run_id VARCHAR(36),
    agent_instance_id VARCHAR(36),
    name VARCHAR(255),
    content TEXT,
    file_path VARCHAR(500),
    metadata TEXT, -- JSON
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE INDEX idx_artifacts_tenant ON artifacts(tenant_id);
CREATE INDEX idx_artifacts_type ON artifacts(type);

-- ==================== 心跳任务 ====================

CREATE TABLE heartbeats (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL,
    project_id VARCHAR(36) NOT NULL,
    scenario_template_id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    cron_expression VARCHAR(100) NOT NULL, -- Quartz Cron
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE / PAUSED / DELETED
    next_trigger_at TIMESTAMP,
    last_trigger_at TIMESTAMP,
    last_run_status VARCHAR(20),
    max_concurrent_agents INT DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    FOREIGN KEY (project_id) REFERENCES projects(id),
    FOREIGN KEY (scenario_template_id) REFERENCES scenario_templates(id)
);

CREATE INDEX idx_heartbeats_tenant ON heartbeats(tenant_id);
CREATE INDEX idx_heartbeats_project ON heartbeats(project_id);
CREATE INDEX idx_heartbeats_status ON heartbeats(status);

-- ==================== 审计日志 ====================

CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36),
    action VARCHAR(50) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id VARCHAR(36),
    detail TEXT,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_tenant ON audit_logs(tenant_id);
CREATE INDEX idx_audit_logs_created ON audit_logs(created_at);
```

---

## 5. API 详细定义

### 5.1 LLM Provider API

```yaml
# GET /api/v1/llm-providers
# 获取 LLM Provider 列表（支持租户隔离）
Response 200:
  [
    {
      "id": "provider-001",
      "name": "Anthropic Claude",
      "code": "claude",
      "defaultModel": "claude-3-5-sonnet-20241022",
      "availableModels": ["claude-3-5-sonnet", "claude-3-opus"],
      "status": "ACTIVE",
      "quota": {
        "monthlyTokenLimit": 10000000,
        "monthlyTokenUsed": 2500000
      }
    }
  ]

# POST /api/v1/llm-providers
# 创建 Provider（apiKey 只传一次，返回后不可再查看明文）
Request:
  {
    "name": "Kimi Moonshot",
    "code": "kimi",
    "apiBaseUrl": "https://api.moonshot.cn",
    "apiKey": "sk-xxxxxxxx",
    "defaultModel": "kimi-k2.5",
    "availableModels": ["kimi-k2.5", "kimi-k2-turbo"],
    "monthlyTokenLimit": 5000000,
    "rpmLimit": 60
  }
Response 201:
  { "id": "provider-002" }

# POST /api/v1/llm-providers/{id}/test
# 测试连接
Response 200:
  { "success": true, "model": "kimi-k2.5", "latencyMs": 230 }
```

### 5.2 Agent Template API

```yaml
# GET /api/v1/agent-templates
# 列表（平台模板 + 当前租户模板）
Query: ?type=EPHEMERAL&status=PUBLISHED
Response 200:
  {
    "items": [
      {
        "id": "tpl-claude-dev",
        "name": "Claude Code 开发者",
        "description": "擅长 Java/Spring 后端开发",
        "icon": "💻",
        "type": "EPHEMERAL",
        "status": "PUBLISHED",
        "isPlatformTemplate": true,
        "llm": { "providerId": "provider-001", "model": "claude-3-5-sonnet" }
      }
    ],
    "total": 10
  }

# GET /api/v1/agent-templates/{id}
# 详情（包含完整的 Prompt、工具集、约束）
Response 200:
  {
    "id": "tpl-claude-dev",
    "name": "Claude Code 开发者",
    "systemPrompt": "你是一位经验丰富的 Java 后端开发工程师...",
    "tools": ["FILE_READ", "FILE_WRITE", "BASH", "GIT", "MVN"],
    "constraints": {
      "timeout": 300,
      "maxTokens": 100000,
      "allowNetwork": false,
      "allowedPaths": ["/workspace/repo/", "/workspace/output/"]
    }
  }

# POST /api/v1/agent-templates
# 创建（支持从平台模板克隆）
Request:
  {
    "name": "我的 Claude 开发者",
    "parentTemplateId": "tpl-claude-dev",
    "systemPrompt": "你是一位熟悉我司技术栈的开发者...",
    "constraints": { "timeout": 600 }
  }
```

### 5.3 Agent Instance API

```yaml
# POST /api/v1/agent-instances
# 创建并启动任务型 Agent
Request:
  {
    "templateId": "tpl-claude-dev",
    "name": "登录功能开发",
    "projectId": "proj-001",
    "requirementId": "req-001",
    "prompt": "实现用户登录接口，使用 JWT + Redis"
  }
Response 202 Accepted:
  {
    "id": "inst-001",
    "status": "PENDING",
    "type": "EPHEMERAL",
    "createdAt": "2026-04-21T10:00:00Z"
  }

# GET /api/v1/agent-instances/{id}
# 实例详情
Response 200:
  {
    "id": "inst-001",
    "name": "登录功能开发",
    "status": "RUNNING",
    "type": "EPHEMERAL",
    "template": { "id": "tpl-claude-dev", "name": "Claude Code 开发者" },
    "sandboxPath": "/sandboxes/tenant-1/proj-001/task-001/",
    "processPid": 12345,
    "startedAt": "2026-04-21T10:00:05Z",
    "logs": ["Starting...", "Reading files..."]
  }

# POST /api/v1/agent-instances/{id}/terminate
# 强制终止
Response 200:
  { "id": "inst-001", "status": "TERMINATED" }

# POST /api/v1/agent-instances/{id}/message
# 向持久型 Agent 发送消息
Request:
  { "message": "帮我优化这个登录接口的性能" }
Response 202:
  { "messageId": "msg-001" }

# GET /api/v1/agent-instances/{id}/stream
# SSE 实时日志流（核心接口）
# 返回 text/event-stream
# event: start / output / error / end
```

### 5.4 Project API

```yaml
# POST /api/v1/projects
Request:
  {
    "name": "AI Dev Platform",
    "description": "AI 研发协同平台",
    "githubRepo": "https://github.com/haibin1003/ai-dev-platform",
    "defaultAgentTemplateId": "tpl-claude-dev"
  }
Response 201:
  { "id": "proj-001" }

# POST /api/v1/projects/{id}/sync
# 同步 GitHub 仓库
Response 202:
  { "syncId": "sync-001", "status": "SYNCING" }

# GET /api/v1/projects/{id}/context
# 获取工程上下文包
Response 200:
  {
    "repoPath": "/sandboxes/tenant-1/proj-001/current/repo/",
    "conventions": "# 编码规范\n- 使用 Java 17...",
    "fileCount": 60,
    "lastSyncedAt": "2026-04-21T09:00:00Z"
  }
```

### 5.5 Requirement API

```yaml
# POST /api/v1/requirements
Request:
  {
    "projectId": "proj-001",
    "title": "实现用户登录功能",
    "description": "## 需求描述\n需要实现...",
    "criteria": [
      "单测覆盖率 ≥ 80%",
      "接口响应时间 < 100ms",
      "支持 JWT Token 刷新"
    ]
  }
Response 201:
  { "id": "req-001" }

# POST /api/v1/requirements/{id}/assign
# 指派 Agent 执行
Request:
  {
    "workflowId": "wf-001",
    "agentAssignments": [
      { "nodeId": "arch", "agentTemplateId": "tpl-architect" },
      { "nodeId": "dev", "agentTemplateId": "tpl-claude-dev" },
      { "nodeId": "test", "agentTemplateId": "tpl-test-engineer" }
    ]
  }
Response 202:
  { "executionId": "exec-001" }

# POST /api/v1/requirements/{id}/accept
# 人类验收通过
Response 200:
  { "status": "ACCEPTED" }
```

### 5.6 Workflow API（扩展）

```yaml
# POST /api/v1/workflows
# 创建工作流（节点支持 Agent 类型）
Request:
  {
    "name": "登录功能开发流水线",
    "projectId": "proj-001",
    "nodes": [
      { "id": "start", "type": "START", "name": "开始" },
      { "id": "arch", "type": "AGENT_EXECUTION", "name": "架构设计", "agentTemplateId": "tpl-architect", "timeout": 600 },
      { "id": "dev", "type": "AGENT_EXECUTION", "name": "编码实现", "agentTemplateId": "tpl-claude-dev", "timeout": 1800 },
      { "id": "test", "type": "AGENT_EXECUTION", "name": "测试", "agentTemplateId": "tpl-test-engineer", "timeout": 600 },
      { "id": "review", "type": "HUMAN_APPROVAL", "name": "人工审核" },
      { "id": "end", "type": "END", "name": "结束" }
    ],
    "edges": [
      { "from": "start", "to": "arch" },
      { "from": "arch", "to": "dev" },
      { "from": "dev", "to": "test" },
      { "from": "test", "to": "review" },
      { "from": "review", "to": "end" }
    ]
  }
```

### 5.7 Scenario Template API（新增）

```yaml
# GET /api/v1/scenario-templates
# 场景模板列表（平台模板 + 当前租户模板）
Query: ?category=HEARTBEAT&status=PUBLISHED
Response 200:
  {
    "items": [
      {
        "id": "sce-heartbeat-pr",
        "name": "PR 巡检流水线",
        "description": "自动分析新 PR，编写 LGTM，执行代码审查",
        "category": "HEARTBEAT",
        "icon": "🔄",
        "status": "PUBLISHED",
        "isPlatformTemplate": true,
        "stepCount": 8
      }
    ],
    "total": 5
  }

# GET /api/v1/scenario-templates/{id}
# 场景模板详情（含步骤定义）
Response 200:
  {
    "id": "sce-heartbeat-pr",
    "name": "PR 巡检流水线",
    "steps": [
      { "id": "start", "type": "START", "name": "开始" },
      { "id": "fetch", "type": "AGENT_EXECUTION", "name": "获取 PR 列表", "agentTemplateId": "tpl-github-fetcher" },
      { "id": "analyze", "type": "AGENT_EXECUTION", "name": "分析 PR", "agentTemplateId": "tpl-analyzer" },
      { "id": "lgtm", "type": "AGENT_EXECUTION", "name": "编写 LGTM", "agentTemplateId": "tpl-reviewer" },
      { "id": "review", "type": "AGENT_EXECUTION", "name": "代码审查", "agentTemplateId": "tpl-reviewer" },
      { "id": "merge", "type": "AGENT_EXECUTION", "name": "合并检查", "agentTemplateId": "tpl-merger" },
      { "id": "report", "type": "AGENT_EXECUTION", "name": "生成报告", "agentTemplateId": "tpl-reporter" },
      { "id": "end", "type": "END", "name": "结束" }
    ],
    "inputSchema": {
      "githubRepo": { "type": "string", "required": true },
      "targetBranch": { "type": "string", "default": "main" }
    }
  }

# POST /api/v1/scenario-templates/{id}/apply
# 将场景模板应用到项目，生成工作流
Request:
  {
    "projectId": "proj-001",
    "inputs": {
      "githubRepo": "https://github.com/haibin1003/ai-dev-platform",
      "targetBranch": "main"
    },
    "agentOverrides": {
      "analyze": { "agentTemplateId": "tpl-custom-analyzer" }
    }
  }
Response 201:
  { "workflowId": "wf-002", "status": "CREATED" }
```

### 5.8 Heartbeat API（新增）

```yaml
# GET /api/v1/heartbeats
# 心跳任务列表
Query: ?projectId=proj-001&status=ACTIVE
Response 200:
  [
    {
      "id": "hb-001",
      "name": "每日 PR 巡检",
      "projectId": "proj-001",
      "scenarioTemplateId": "sce-heartbeat-pr",
      "cronExpression": "0 0 9 * * ?",
      "status": "ACTIVE",
      "nextTriggerAt": "2026-04-22T09:00:00Z",
      "lastRunStatus": "COMPLETED",
      "maxConcurrentAgents": 3
    }
  ]

# POST /api/v1/heartbeats
# 创建心跳任务
Request:
  {
    "projectId": "proj-001",
    "scenarioTemplateId": "sce-heartbeat-pr",
    "name": "每日 PR 巡检",
    "cronExpression": "0 0 9 * * ?",
    "maxConcurrentAgents": 3,
    "inputs": {
      "targetBranch": "develop"
    }
  }
Response 201:
  { "id": "hb-001" }

# POST /api/v1/heartbeats/{id}/pause
# 暂停心跳任务
Response 200:
  { "id": "hb-001", "status": "PAUSED" }

# POST /api/v1/heartbeats/{id}/resume
# 恢复心跳任务
Response 200:
  { "id": "hb-001", "status": "ACTIVE" }

# POST /api/v1/heartbeats/{id}/trigger
# 手动触发一次心跳
Response 202:
  { "executionId": "exec-003" }
```

---

## 6. 前端架构升级设计

### 6.1 工作流编辑器重构

```
WorkflowEditorView.vue
├── ToolbarPanel.vue              # 修复 props 错误，新增导入按钮
├── NodePalette.vue               # 新增 Agent 节点类型
├── GraphCanvas.vue               # 提取：X6 画布逻辑
│   ├── initGraph()               # 修复端口配置
│   ├── onDrop()                  # 修复端口重复
│   └── loadGraphDefinition()     # 修复加载时端口丢失
├── PropertyPanel.vue             # 重构：支持 Agent 配置
│   ├── AgentExecutionConfig.vue  # Agent 模板选择、输入映射
│   ├── HumanApprovalConfig.vue   # 审批人、超时设置
│   └── ConditionConfig.vue       # 条件表达式
└── ExecutionOverlay.vue          # 新增：执行时节点高亮
```

### 6.2 状态管理（Pinia）

```typescript
// stores/agent.ts
export const useAgentStore = defineStore('agent', () => {
  const instances = ref<AgentInstance[]>([])
  const runningInstances = computed(() => instances.value.filter(i => i.status === 'RUNNING'))
  
  async function startInstance(templateId: string, prompt: string) {
    const instance = await agentApi.create({ templateId, prompt })
    instances.value.push(instance)
    // 建立 SSE 连接
    connectStream(instance.id)
  }
  
  function connectStream(instanceId: string) {
    const es = new EventSource(`/api/v1/agent-instances/${instanceId}/stream`)
    es.addEventListener('output', (e) => {
      appendLog(instanceId, e.data)
    })
  }
})

// stores/lobster.ts
export const useLobsterStore = defineStore('lobster', () => {
  const messages = ref<ChatMessage[]>([])
  const isConnected = ref(false)
  
  function sendMessage(content: string) {
    websocket.send(JSON.stringify({ type: 'chat', content }))
  }
  
  function onPush(notification: LobsterPush) {
    // 显示系统通知
    ElNotification({ title: notification.title, message: notification.content })
  }
})
```

### 6.3 关键组件设计

```vue
<!-- AgentExecutionNode.vue -->
<template>
  <div class="agent-node" :class="{ running: isRunning, completed: isCompleted }">
    <div class="node-header" :style="{ backgroundColor: agentColor }">
      <el-icon><SetUp /></el-icon>
      <span>{{ agentName }}</span>
      <el-icon v-if="isRunning" class="is-loading"><Loading /></el-icon>
    </div>
    <div class="node-body">
      <div class="node-title">{{ data.label }}</div>
      <div v-if="data.timeout" class="node-meta">超时: {{ data.timeout }}s</div>
    </div>
    <!-- 端口 -->
    <div v-if="type !== 'START'" class="port port-in" data-port-group="in" />
    <div v-if="type !== 'END'" class="port port-out" data-port-group="out" />
  </div>
</template>
```

---

## 7. 时序图

### 7.1 创建并启动任务型 Agent

```
Client    Controller    AgentAppService    AgentRuntime    SandboxManager    ClaudeProcess
  │           │              │                  │                │                │
  │ POST /agent-instances    │                  │                │                │
  │─────────────────────────>│                  │                │                │
  │           │              │                  │                │                │
  │           │    createAndStartInstance()    │                │                │
  │           │───────────────────────────────>│                │                │
  │           │              │                  │                │                │
  │           │              │   createSandbox()│                │                │
  │           │              │─────────────────>│                │                │
  │           │              │                  │                │                │
  │           │              │   injectContext()│                │                │
  │           │              │─────────────────>│                │                │
  │           │              │                  │                │                │
  │           │              │   start()        │                │                │
  │           │              │────────────────────────────────────────────────────>│
  │           │              │                  │                │                │
  │           │              │   <────── 进程启动，返回 PID ──────────────────────│
  │           │              │                  │                │                │
  │           │              │   startOutputListener()           │                │
  │           │              │   (异步线程读取 stdout)           │                │
  │           │              │                  │                │                │
  │           │              │   publishEvent(AgentStartedEvent) │                │
  │           │              │                  │                │                │
  │           │<─────────────│ 202 Accepted    │                │                │
  │<─────────────────────────│                  │                │                │
  │           │              │                  │                │                │
  │           │              │   <─────────── SSE 推送 stdout ────────────────────│
  │           │              │                  │                │                │
  │           │              │   streamOutput() │                │                │
  │           │              │   <──────────────│                │                │
  │           │              │                  │                │                │
  │<─────────────────────────────────────── SSE: event=output, data=xxx ──────────│
  │           │              │                  │                │                │
  │           │              │   <──────────── 进程结束 ──────────────────────────│
  │           │              │                  │                │                │
  │           │              │   collectArtifacts()              │                │
  │           │              │   publishEvent(AgentCompletedEvent)               │
  │           │              │                  │                │                │
  │<─────────────────────────────────────── SSE: event=end ───────────────────────│
```

### 7.2 多 Agent 协作执行

```
ExecutionAppService    DAGScheduler    AgentAppService    AgentRuntime
        │                  │                  │                │
        │   startExecution()                │                │
        │──────────────────────────────────>│                │
        │                  │                  │                │
        │                  │   initializeTasks()               │
        │                  │   (创建所有 AgentRun 记录)        │
        │                  │                  │                │
        │                  │   scheduleReadyTasks()            │
        │                  │   (拓扑排序，找到入度为0的节点)    │
        │                  │                  │                │
        │                  │   startAgent(node.arch)           │
        │                  │─────────────────>│                │
        │                  │                  │   start()      │
        │                  │                  │────────────────>
        │                  │                  │                │
        │                  │   <───────────── 等待完成 ────────│
        │                  │                  │                │
        │                  │   onAgentCompleted(arch)          │
        │                  │   (提取产物 ARCH_DOC)             │
        │                  │                  │                │
        │                  │   scheduleNextTasks()             │
        │                  │   (arch 下游节点：dev)            │
        │                  │                  │                │
        │                  │   startAgent(node.dev)            │
        │                  │   (传入 arch 的产物作为输入)      │
        │                  │─────────────────>│                │
        │                  │                  │                │
        │                  │   <───────────── 等待完成 ────────│
        │                  │                  │                │
        │                  │   onAgentCompleted(dev)           │
        │                  │   (提取产物 CODE_DIFF)            │
        │                  │                  │                │
        │                  │   scheduleNextTasks()             │
        │                  │   (dev 下游节点：test, review)    │
        │                  │   ...                            │
```

---

## 8. 开发任务分解

### Phase 1: 基础设施（Week 1）

| 任务 | 文件 | 优先级 | 验收标准 |
|------|------|--------|---------|
| 修复 Checkstyle 配置 | `checkstyle.xml` | P0 | `./mvnw checkstyle:check` 通过 |
| 多租户基础 | `TenantContext.java`, `TenantConfig.java` | P0 | 请求头解析租户 ID，Repository 自动过滤 |
| 加密服务 | `AesEncryptionService.java` | P0 | AES-256 加解密通过单元测试 |
| 前端修复 ToolbarPanel | `ToolbarPanel.vue` | P0 | 工具栏按钮可点击，无编译错误 |

### Phase 2: LLM + Agent 核心（Week 2-3）

| 任务 | 文件 | 优先级 | 验收标准 |
|------|------|--------|---------|
| LLM Provider 域模型 | `LLMProvider.java`, `ApiKey.java` | P0 | 值对象不可变，加密存储 |
| LLM Provider API | `LLMProviderController.java`, `LLMAppService.java` | P0 | CRUD + 测试连接通过 |
| Agent 模板域模型 | `AgentTemplate.java`, `SystemPrompt.java` | P0 | 模板版本管理可用 |
| Agent 模板 API | `AgentTemplateController.java` | P0 | 列表/详情/创建/克隆 |
| Agent 人格配置 | `PersonaConfig.java`, `ClaudeCodeConfig.java` | P0 | IDENTITY/SOUL/AGENTS/USER/TOOLS 分层 |
| Agent 实例域模型 | `AgentInstance.java`, `InstanceStatus.java` | P0 | 状态机转换正确，支持 shadow_from |
| Prompt 模板引擎 | `PromptTemplateEngine.java` | P0 | `${var}` 变量替换，分层 Prompt 组装 |
| Agent 运行时端口 | `AgentRuntime.java`, `ProcessAgentRuntime.java` | P0 | 启动 Claude Code 进程并捕获输出 |
| Agent 实例 API | `AgentInstanceController.java` | P0 | 创建/终止/SSE 日志流 |
| Agent 僵尸清理 | `AgentStartupCleaner.java` | P0 | 启动时扫描 30min+ 僵尸并标记 FAILED |
| 前端 Agent 市场页 | `AgentMarketView.vue` | P0 | 展示模板列表，支持搜索 |
| 前端场景市场页 | `ScenarioMarketView.vue` | P0 | 展示场景模板，一键应用 |

### Phase 3: 项目 + 需求（Week 4）

| 任务 | 文件 | 优先级 | 验收标准 |
|------|------|--------|---------|
| 项目域模型 | `Project.java`, `GitHubRepo.java` | P0 | 绑定 GitHub 仓库 |
| GitHub OAuth 集成 | `GitHubClientImpl.java` | P0 | OAuth 授权 + 仓库 clone |
| 工程上下文注入 | `ContextInjector.java` | P0 | 沙箱内包含代码 + 规范 |
| 需求域模型 | `Requirement.java`, `SubTask.java` | P0 | 状态机流转正确 |
| 需求管理 API | `RequirementController.java` | P0 | CRUD + 指派 Agent |
| 前端项目/需求页 | `ProjectListView.vue`, `RequirementListView.vue` | P0 | 页面可用 |

### Phase 4: 工作流升级 + 多 Agent 编排（Week 5-6）

| 任务 | 文件 | 优先级 | 验收标准 |
|------|------|--------|---------|
| 场景模板域模型 | `ScenarioTemplate.java`, `Heartbeat.java` | P0 | 预置场景模板 + 心跳调度 |
| 场景模板 API | `ScenarioTemplateController.java` | P0 | 列表/详情/应用到项目 |
| 心跳调度器 | `HeartbeatScheduler.java`, `HeartbeatTriggerService.java` | P0 | Quartz Cron 定时触发 |
| 工作流节点扩展 | `Node.java` (新增类型) | P0 | AGENT_EXECUTION / HUMAN_APPROVAL / CONDITION |
| 产物传递机制 | `ArtifactResolver.java`, `InputMapping.java` | P0 | 上游产物自动传递给下游 |
| Agent 分身工厂 | `ReplicaAgentFactory.java` | P0 | 模板深拷贝，工作目录隔离 |
| DAGScheduler 升级 | `DAGScheduler.java` | P0 | 支持产物传递、并行执行 |
| 前端工作流编辑器重构 | `WorkflowEditorView.vue` + 子组件 | P0 | 修复端口问题，支持 Agent 节点 |
| 执行监控升级 | `ExecutionMonitorView.vue` | P0 | 实时高亮当前节点 |

### Phase 5: 报告 + 龙虾 + SaaS（Week 7-8）

| 任务 | 文件 | 优先级 | 验收标准 |
|------|------|--------|---------|
| 产物收集与报告 | `ArtifactCollector.java`, `ReportAppService.java` | P1 | 自动生成 diff + 报告 |
| 审计日志 | `AuditLogInterceptor.java` | P1 | 所有操作记录审计日志 |
| 龙虾服务 | `LobsterAppService.java`, `LobsterChatView.vue` | P1 | 持久型 Agent + 聊天界面 |
| 主动推送 | `LobsterPushHandler.java`, WebSocket | P1 | 任务进度实时推送 |
| 冷却机制 | `CooldownTracker.java` | P1 | 3小时内不重复评论同一 issue/PR |
| 资源配额 | `QuotaChecker.java` | P1 | Token/并发数超限拦截 |
| 平台 Agent 市场 | 管理员后台 | P1 | 平台模板发布/下架 |
| 心跳历史查询 | `HeartbeatRun.java` | P1 | 心跳执行记录，支持重试 |

### Phase 6: 测试与优化（Week 9）

| 任务 | 验收标准 |
|------|---------|
| 单元测试 | Domain ≥ 95%，整体 ≥ 80% |
| 集成测试 | 关键 API 链路覆盖 |
| E2E 测试 | 创建项目 → 创建 Agent → 执行 → 查看报告 |
| 性能测试 | Agent 启动 < 10s，SSE 延迟 < 100ms |
| 安全扫描 | 无 Blocker/Critical 问题 |

---

## 9. 检查清单

### 设计评审检查项

- [ ] DDD 分层正确，无依赖倒置
- [ ] 新增 12+ 个核心域模型，聚合边界清晰
- [ ] 值对象不可变，有正确的 equals/hashCode
- [ ] 仓储接口在领域层，实现在基础设施层
- [ ] 适配器符合端口定义
- [ ] 数据库索引设计合理
- [ ] API 符合 RESTful 规范，错误码定义清晰
- [ ] 前端组件拆分合理，状态管理清晰
- [ ] 时序图覆盖核心流程
- [ ] 开发任务可执行，验收标准可量化

---

**最后更新**: 2026-04-21  
**版本**: v2.0  
**状态**: 待评审（已吸收 weibaohui/tasks 参考项目 10 大工程实践），待评审
