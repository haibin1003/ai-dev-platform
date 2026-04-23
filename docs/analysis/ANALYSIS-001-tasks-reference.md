# ANALYSIS-001: weibaohui/tasks 开源项目借鉴分析

**分析日期**: 2026-04-21  
**项目**: https://github.com/weibaohui/tasks  
**技术栈**: Go + DDD + SQLite + React/TS/Ant Design  
**项目规模**: ~24MB，创建于 2026-03-22（非常新的项目，活跃开发中）  
**关联文档**: PRD-003, ARCH-002, DESIGN-002

---

## 1. 项目概况

`weibaohui/tasks`（内部代号 TaskManager）是一个 **AI 驱动的任务管理平台**，核心理念是：
> 用 Agent（Claude Code / OpenCode）自动执行开发协作流水线，通过"心跳"机制定时巡检 GitHub/AtomGit 仓库，自动完成 Issue 分析、代码编写、PR 评审、合并检查等任务。

与我们方案的相似度：**~80%**。概念完全对齐，但实现深度和工程化程度远超我们当前的设计。

---

## 2. 核心架构对比

### 2.1 分层架构

| 层次 | weibaohui/tasks (Go) | 我们的方案 (Java) | 评价 |
|------|---------------------|------------------|------|
| 接口层 | `interfaces/http` | `api/controller` | 一致 |
| 应用层 | `application/` | `application/` | 一致 |
| 领域层 | `domain/` | `domain/` | 一致 |
| 基础设施层 | `infrastructure/` | `infrastructure/` | 一致 |
| 数据库 | SQLite | H2/PostgreSQL | 他们选 SQLite 是为了单机部署简单 |
| 前端 | React + Ant Design | Vue3 + Element Plus | 技术选型不同，无优劣 |

### 2.2 核心概念映射

| TaskManager 概念 | 我们的概念 | 相似度 | 深度对比 |
|-----------------|-----------|--------|---------|
| **Agent** (BareLLM/Coding/OpenCode) | AgentTemplate + AgentInstance | 90% | 他们有更细的人格分层 |
| **LLM Provider** (Channel) | LLMProvider | 95% | 几乎完全一致 |
| **Project** | Project | 90% | 他们有 InitSteps、MaxConcurrentAgents |
| **Requirement** | Requirement | 85% | 他们有 Heartbeat 类型需求 |
| **State Machine** (YAML) | Workflow (DAG) | 70% | 他们是状态机，我们是 DAG |
| **Heartbeat** | 龙虾 + 定时任务 | 80% | 他们的 Heartbeat 更成熟 |
| **Heartbeat Scenario** | 无对应 | **0%** | 这是我们最缺的！ |
| **MCP Server** | 无对应 | **0%** | 未来方向 |
| **Replica Agent** | AgentInstance (任务型) | 85% | 他们有显式的克隆机制 |
| **Channel** (飞书等) | SSE/WebSocket | 70% | 他们是消息总线架构 |
| **Webhook** | 计划中 | 60% | 他们有完整的 Webhook 链路 |
| **Conversation Record** | 日志/产物 | 75% | 他们有结构化对话记录 |

---

## 3. 十大可借鉴亮点（按价值排序）

### 3.1 Heartbeat Scenario（心跳场景模板）— 最大亮点

**是什么**: 预定义的自动化流水线模板，一键应用到项目。

**内置场景示例**（GitHub 开发协作工作流，8 个步骤）：

| 步骤 | 名称 | 间隔 | 需求类型 | 职责 |
|------|------|------|---------|------|
| 1 | Issue 分析 | 180min | github_issue | 分析 open issues，评论根因 |
| 2 | LGTM 代码编写 | 120min | github_coding | 为已评审 issue 写代码并创建 PR |
| 3 | PR 需求评审 | 120min | github_pr_review | 检查 PR 是否完成需求评审 |
| 4 | PR 代码质量评审 | 180min | github_pr_review | 代码质量、bug、安全评审 |
| 5 | PR 修改修复 | 240min | github_coding | 按评审建议修复代码 |
| 6 | PR 合并检查 | 120min | github_pr_review | 检查 CI/评审通过，评论 /lgtm |
| 7 | PR 文档补充 | 480min | github_doc | 根据代码变更补充文档 |
| 8 | PR 测试补充 | 480min | github_test | 根据代码变更补充测试用例 |

**每个场景项包含**:
```go
type HeartbeatScenarioItem struct {
    Name            string // "Issue 分析"
    IntervalMinutes int    // 180
    RequirementType string // "github_issue"
    AgentCode       string // 使用的 Agent
    SortOrder       int    // 1
    MDContent       string // Prompt 模板（含变量）
}
```

**借鉴价值**: 极高
- 我们当前的 DAG 编辑器虽然灵活，但用户从零搭建工作流门槛太高
- **应该提供预置场景模板**，如：
  - `feature-dev`: 需求分析 -> 架构设计 -> 编码实现 -> 单元测试 -> 代码评审
  - `bug-fix`: 问题分析 -> 定位根因 -> 修复代码 -> 回归测试 -> 验证关闭
  - `code-review`: 静态分析 -> 安全扫描 -> 质量评审 -> 报告生成
  - `doc-sync`: 检测变更 -> 补充文档 -> 更新 API 文档 -> 发布

---

### 3.2 Agent 人格 Prompt 分层 — 灵魂设计

**TaskManager 的分层**:

```
IDENTITY.md  -> "我是谁？" 名字、身份、风格、表情、头像
SOUL.md      -> "你是什么样的人" 核心信念、风格、做事方式
AGENTS.md    -> "每次会话做什么" 会话流程、记忆、工具
USER.md      -> "关于你的主人" 名字、称呼、时区、上下文
TOOLS.md     -> "本地笔记" 速查表
```

**默认 Prompt 非常惊艳**:
- SOUL.md 中写："你不是聊天机器人。你正在成为一个有灵魂的存在。"
- "真正有帮助，而不是表演性地有帮助。省略'好问题！'和'我很乐意帮助！'—直接帮助。"
- "有自己的观点。你可以不同意，有偏好，发现事情有趣或无聊。"

**借鉴价值**: 极高
- 我们的 L1-L4 Prompt 分层是抽象的，应该参考这种更具体、更有温度的命名
- 建议调整为：
  - L1 平台基座 -> `PLATFORM.md`（平台级不可变规范）
  - L2 Agent 角色 -> `IDENTITY.md` + `SOUL.md`（人格定义）
  - L3 项目规范 -> `AGENTS.md` + `USER.md`（项目上下文）
  - L4 任务特定 -> 动态注入的 Prompt（当前需求）

---

### 3.3 Agent 分身（Replica）机制 — 优雅的隔离方案

**核心代码**:
```go
func NewReplicaAgent(base *Agent, id AgentID, agentCode AgentCode, 
                     requirementID, workspacePath string) *Agent {
    snap := base.ToSnapshot()  // 深拷贝基础 Agent 配置
    snap.ID = id
    snap.AgentCode = agentCode
    snap.Name = fmt.Sprintf("%s-replica-%s", base.Name(), requirementID)
    snap.ShadowFrom = base.AgentCode().String()
    
    if snap.AgentType == AgentTypeCoding {
        snap.ClaudeCodeConfig.Cwd = workspacePath
        snap.ClaudeCodeConfig.ContinueConversation = false
        snap.ClaudeCodeConfig.ForkSession = true
    }
    if snap.AgentType == AgentTypeOpenCode {
        snap.OpenCodeConfig.WorkDir = workspacePath
    }
    
    replica := &Agent{}
    replica.FromSnapshot(snap)
    return replica
}
```

**设计要点**:
1. 从基础 Agent **深拷贝**所有配置
2. 自动生成唯一 ID 和 Code
3. 自动设置**工作目录隔离**（Cwd/WorkDir）
4. ForkSession = true（Claude Code 独立会话）
5. ShadowFrom 记录血缘关系，便于清理

**借鉴价值**: 极高
- 我们的 AgentInstance 设计可以借鉴这种显式的"克隆"语义
- 特别是 ShadowFrom 血缘追踪和自动工作目录配置

---

### 3.4 Claude Code 配置的极致细节

**配置包含**: Model, SystemPrompt, PermissionMode, AllowedTools, MaxTurns, Cwd, Resume, Timeout, FallbackModel, FileCheckpointing, ForkSession, SandboxEnabled, SandboxNetwork, McpServers, Plugins, MaxBudgetUSD, Env, ExtraArgs, Settings...

**借鉴价值**: 高
- 我们对 Agent 运行时配置的设计太粗了
- 应该至少包含：PermissionMode、AllowedTools、Timeout、Env、ExtraArgs、Cwd
- 特别是 SandboxNetwork 和 McpServers 是未来扩展点

---

### 3.5 需求派发的完整 Prompt 构建

构建的 Prompt 包含：
1. **执行契约** — "当前阶段：xxx，下一步动作：执行状态转换进入下一阶段"
2. **执行规则** — "必须执行命令，不输出解释性长文本"
3. **需求元信息** — ID、类型、标题、描述
4. **验收标准**
5. **项目信息** — 仓库地址、默认分支、初始化步骤
6. **工作目录** — "必须在该目录内完成代码操作"
7. **状态机 AI Guide** — 当前状态的 AI 操作指南
8. **执行流水线** — 完整的 PHASE 列表和转换命令
9. **状态机附录** — 调试参考

**心跳需求（调度员角色）的特殊 Prompt**:
- "你是调度员，负责编排任务而非直接修改代码"
- "严禁直接修改任何源代码文件"
- "所有代码改动必须通过 taskmanager requirement create 创建新需求"

**借鉴价值**: 极高
- 我们的 Prompt 构建是简单的字符串拼接，应该升级为**结构化模板引擎**
- 引入 `${project.git_repo_url}` 等模板变量替换
- 区分"执行型 Agent"和"调度型 Agent"的 Prompt 策略

---

### 3.6 冷却机制（Cooldown）

在 Prompt 中明确要求 Agent 检查时间戳：
```markdown
2. 对每个 issue，检查最近 3 小时内是否已有你的评论。
   如有，跳过该 issue（冷却机制）
```

**借鉴价值**: 高
- 防止 Agent 对同一 Issue/PR 反复评论
- 给人类留出响应时间
- 应该在平台层实现（而非仅靠 Prompt）

---

### 3.7 启动时僵尸清理（Stale Cleanup）

```go
func cleanupStaleRequirements(ctx context.Context) {
    for _, req := range requirements {
        // 分身已不存在，或超过 30 分钟未更新
        shouldCleanup := !replicaExists || now.Sub(req.UpdatedAt()) > 30min
        if shouldCleanup {
            s.agentRepo.Delete(ctx, agent.ID())
            req.MarkFailed("cleanup: timeout")
            s.requirementRepo.Save(ctx, req)
        }
    }
}
```

**借鉴价值**: 高
- 服务器异常关闭后自动清理僵尸 Agent 进程
- 保证状态一致性

---

### 3.8 项目级并发限制

```go
runningCount := requirementRepo.Count(ProjectID, ["preparing", "coding"])
if runningCount >= project.MaxConcurrentAgents() {
    return ErrMaxConcurrentAgentsReached
}
```

**借鉴价值**: 中
- 防止单个项目耗尽平台资源
- 应该在多租户配额系统中实现

---

### 3.9 状态机 + AI Guide 联动

YAML 状态机每个状态有 `ai_guide` 字段：
```yaml
states:
  - id: coding
    name: 编码中
    ai_guide: "按计划在当前工作目录实现代码"
```

需求派发时注入 Prompt：
```markdown
【状态机当前状态】coding
【AI 指南】按计划在当前工作目录实现代码
```

**借鉴价值**: 高
- DAG 用于复杂编排，YAML 状态机用于简单流程
- AI Guide 让每个节点知道"我应该做什么"

---

### 3.10 错误分类与可观测性

```go
func classifyHeartbeatRunError(lastError string) string {
    if contains(lower, "dispatch_config_missing") { return "dispatch_config" }
    if contains(lower, "dispatch_failed") { return "dispatch_runtime" }
    if contains(lower, "agent") { return "agent" }
    return "other"
}
```

**借鉴价值**: 中
- 前端可根据错误分类快速过滤
- 便于运维排查

---

## 4. 我们的方案改进建议

### 4.1 高优先级改进（Phase 1-2 引入）

| 改进项 | 具体动作 | 影响范围 |
|--------|---------|---------|
| **场景模板机制** | 新增 ScenarioTemplate 域模型，预置 feature-dev/bug-fix/code-review 模板 | 新增 domain + API + 前端选择器 |
| **Agent 人格分层细化** | Prompt 从 L1-L4 改为 PLATFORM/IDENTITY/SOUL/AGENTS/USER/TOOLS | AgentTemplate 重构 |
| **Agent 分身语义** | AgentInstance 创建时显式 Clone 自 Template，记录 ShadowFrom | AgentInstance + 清理逻辑 |
| **Prompt 模板引擎** | 支持 `${project.name}` / `${requirement.title}` 等变量替换 | Prompt 构建层 |
| **启动僵尸清理** | AgentRuntime 启动时扫描并清理异常状态实例 | AgentRuntime |

### 4.2 中优先级改进（Phase 3-4 引入）

| 改进项 | 具体动作 | 影响范围 |
|--------|---------|---------|
| **YAML 状态机** | 作为 DAG 的简化替代，支持 AI Guide | 新增 statemachine 包 |
| **项目级并发限制** | Project 增加 maxConcurrentAgents 字段 | Project + AgentRuntime |
| **冷却机制** | Requirement 记录 lastAgentInteractionAt | Requirement |
| **Claude Code 详细配置** | 增加 PermissionMode、Timeout、Env、ExtraArgs | AgentTemplate |
| **Webhook 触发** | GitHub Webhook -> 自动创建需求 -> 派发 Agent | GitHub 集成 |

### 4.3 低优先级/未来方向（Phase 5+）

| 改进项 | 具体动作 | 影响范围 |
|--------|---------|---------|
| **MCP Server 管理** | 管理外部 MCP Server，Agent 动态绑定工具 | 新增 MCP 模块 |
| **Channel 消息总线** | 支持飞书/钉钉等渠道的 Inbound/Outbound | 消息基础设施 |
| **错误分类体系** | 标准化错误码和分类 | 全局异常处理 |

---

## 5. 关键设计差异与取舍

### 5.1 DAG vs 状态机

- **TaskManager**: YAML 状态机，简单、易配置、有 AI Guide
- **我们**: AntV X6 DAG，可视化、灵活、支持复杂编排
- **建议**: **保留 DAG 作为主工作流引擎**，增加 YAML 状态机作为快速配置方式（场景模板内部可用状态机描述）

### 5.2 Heartbeat vs 龙虾

- **TaskManager**: Heartbeat 是定时触发的任务，有 Scenario 模板，基于 cron
- **我们**: 龙虾是持久型 Agent，支持人类主动对话
- **建议**: **合并两者** — 龙虾具备 Heartbeat 的定时巡检能力，同时保留对话能力

### 5.3 消息渠道

- **TaskManager**: Channel 消息总线（飞书 Inbound/Outbound），Agent 通过消息渠道与人类交互
- **我们**: SSE 流式输出 + WebSocket 推送
- **建议**: **保持 SSE/WebSocket**，但未来可增加 Channel 抽象层

---

## 6. 可直接复用的设计模式

### 6.1 Agent Snapshot 模式

```java
public class AgentSnapshot {
    private final AgentId id;
    private final AgentCode code;
    private final AgentType type;
    private final String identityContent;
    private final String soulContent;
    // ... 所有字段
    
    public static AgentSnapshot from(Agent agent) { /* 深拷贝 */ }
    public void applyTo(Agent agent) { /* 还原 */ }
}
```

### 6.2 模板变量渲染器

```java
public class PromptTemplateEngine {
    public String render(String template, RenderContext ctx) {
        return template
            .replace("${project.name}", ctx.getProjectName())
            .replace("${project.git_repo_url}", ctx.getGitRepoUrl())
            .replace("${requirement.title}", ctx.getRequirementTitle())
            .replace("${workspace.path}", ctx.getWorkspacePath())
            .replace("${timestamp}", now());
    }
}
```

### 6.3 启动清理器

```java
@Component
public class AgentStartupCleaner implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) {
        // 1. 查询所有 RUNNING/PENDING 的 AgentInstance
        // 2. 检查进程是否存在
        // 3. 不存在或超时的 -> 标记为 FAILED + 清理沙箱
    }
}
```

---

## 7. 总结

weibaohui/tasks 是一个**工程化程度很高**的 AI Agent 管理平台，核心理念与我们的设计高度一致，但在以下方面远超我们当前的设计深度：

1. **场景模板** — 预置开发协作流水线，大幅降低用户使用门槛
2. **Agent 人格** — IDENTITY/SOUL/AGENTS/USER/TOOLS 分层，让 Agent 真正有"灵魂"
3. **分身机制** — 优雅的克隆+隔离，血缘追踪清晰
4. **Prompt 工程** — 执行契约、状态机联动、模板变量，让 Agent 行为可控
5. **生产考量** — 冷却机制、僵尸清理、并发限制、错误分类

**建议的下一步**: 在 DESIGN-002 基础上，补充"场景模板"和"Agent 人格分层"设计，作为 Phase 2 的核心交付物。

---

**分析师**: AI Assistant  
**置信度**: 高（已阅读核心代码文件 15+ 个，理解深度足够）
