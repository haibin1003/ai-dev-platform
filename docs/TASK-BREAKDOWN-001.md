# TASK-BREAKDOWN-001: AI 智能体平台详细需求拆解与开发任务书

**版本**: v1.0
**状态**: 执行中
**负责人**: 待分配
**关联文档**: PRD-003 v2.0, ARCH-002 v2.0, DESIGN-002 v2.0
**创建日期**: 2026-04-21
**更新日期**: 2026-04-21

---

## 文档说明

本文档基于 PRD-003 v2.0 / ARCH-002 v2.0 / DESIGN-002 v2.0，将每个功能需求细拆为可独立执行的开发任务。

**拆解原则**：
- 每个任务可在一个工作日内完成核心逻辑（含单元测试）
- 每个任务明确输入、输出、验收标准
- 每个任务标注前后端依赖关系
- 每个任务标注预估工时（人天）

**图例**：
- `BE` = 后端任务（Java / Spring Boot）
- `FE` = 前端任务（Vue3 / TypeScript）
- `DB` = 数据库任务（DDL / DML / 索引）
- `INT` = 联调任务
- `DEP` = 依赖其他任务


---

## Phase 1: 基础设施修复（Week 1）

> **Phase 目标**：修复现有工程缺陷，建立多租户和加密基础设施，确保后续开发在稳定基线上进行。

---

### T-1.1 Checkstyle 配置修复

| 属性 | 内容 |
|------|------|
| **任务ID** | T-1.1 |
| **任务名称** | Checkstyle 配置修复 |
| **类型** | 配置 |
| **预估工时** | 0.5 人天 |
| **优先级** | P0（阻塞） |

**需求描述**：
当前 `checkstyle.xml` 中引用了不存在的 `LeftCurry` 模块（正确名称为 `LeftCurly`），导致 `./mvnw checkstyle:check` 始终失败，开发者被迫使用 `-Dcheckstyle.skip=true` 编译。

**使用场景**：
开发者执行 `./mvnw clean compile` 时，Checkstyle 应正常通过，无需 skip 参数。

**输入**：
- `backend/src/main/resources/checkstyle.xml`
- `./mvnw checkstyle:check` 命令输出

**输出**：
- 修复后的 `checkstyle.xml`
- `./mvnw checkstyle:check` 执行成功（0 个 violation）

**验收标准**：
1. [ ] `checkstyle.xml` 中所有 module name 均为 Checkstyle 内置有效名称
2. [ ] `./mvnw checkstyle:check` 零错误通过
3. [ ] 现有 Java 源码无需为适配 checkstyle 做额外修改（或修改量 < 5 个文件）

**依赖**：无

---

### T-1.2 前端 ToolbarPanel.vue 编译修复

| 属性 | 内容 |
|------|------|
| **任务ID** | T-1.2 |
| **任务名称** | ToolbarPanel.vue Props 编译修复 |
| **类型** | FE |
| **预估工时** | 0.5 人天 |
| **优先级** | P0（阻塞） |

**需求描述**：
`ToolbarPanel.vue` 中 `computed` 未从 `vue` 导入导致运行时错误，同时 `props` 变量在 `script setup` 作用域中未正确声明。工具栏按钮点击无响应，工作流编辑器顶部完全不可用。

**使用场景**：
用户打开工作流编辑器，顶部工具栏的保存、撤销、重做、缩放等按钮可正常点击并触发对应操作。

**输入**：
- `frontend/src/components/workflow/panels/ToolbarPanel.vue`
- `npm run dev` / `npm run build` 输出

**输出**：
- 修复后的 `ToolbarPanel.vue`
- 无编译警告的构建产物

**验收标准**：
1. [ ] `npm run build` 零 TypeScript / Vue 编译错误
2. [ ] 工具栏所有按钮可点击，点击后正确触发 `emit` 事件
3. [ ] `status` prop 变化时，状态文字和颜色正确更新

**依赖**：无

---

### T-1.3 前端状态映射修复

| 属性 | 内容 |
|------|------|
| **任务ID** | T-1.3 |
| **任务名称** | 前后端状态枚举映射修复 |
| **类型** | FE + BE |
| **预估工时** | 0.5 人天 |
| **优先级** | P0 |

**需求描述**：
后端工作流状态使用 `ACTIVE`，前端展示和类型定义使用 `PUBLISHED`，导致状态显示不一致。同时 `workflowId` computed 属性在 `WorkflowEditorView.vue` 中被赋值，触发 Vue3 只读警告。

**使用场景**：
用户在工作流列表页看到的状态与后端实际状态一致；进入编辑器时无控制台警告。

**输入**：
- 后端 `WorkflowStatus` 枚举定义
- 前端 `workflow.ts` 类型定义
- `WorkflowEditorView.vue` 状态映射逻辑

**输出**：
- 统一的状态枚举映射表
- 修复后的前端类型定义文件
- 修复后的 `WorkflowEditorView.vue`

**验收标准**：
1. [ ] 前端 `WorkflowStatus` 类型与后端枚举完全一致（或明确映射关系）
2. [ ] `WorkflowEditorView.vue` 无 computed 赋值警告
3. [ ] 工作流列表页状态显示正确（ACTIVE=已激活，DRAFT=草稿等）

**依赖**：T-1.2

---

### T-1.4 多租户基础基础设施

| 属性 | 内容 |
|------|------|
| **任务ID** | T-1.4 |
| **任务名称** | 多租户基础基础设施（TenantContext + 过滤器） |
| **类型** | BE |
| **预估工时** | 2 人天 |
| **优先级** | P0 |

**需求描述**：
建立多租户技术基础设施，包括：1) 租户上下文（ThreadLocal + 请求头解析） 2) 租户拦截器（自动从 HTTP Header `X-Tenant-ID` 解析租户） 3) Repository 自动过滤（所有查询自动附加 `WHERE tenant_id = ?`）

**使用场景**：
- 用户 A（租户 T1）创建的工作流，用户 B（租户 T2）无法查看或修改
- 管理员请求不带 `X-Tenant-ID` 时，可访问平台级数据
- 非法租户 ID 请求返回 403

**输入**：
- HTTP Request Header: `X-Tenant-ID`
- 数据库表结构（所有业务表含 `tenant_id` 字段）

**输出**：
- `TenantContext.java` — ThreadLocal 持有当前租户 ID
- `TenantConfig.java` — 拦截器注册和路径排除配置
- `TenantAwareRepository.java` — 基础 Repository 自动过滤租户
- `TenantId.java` — 值对象

**验收标准**：
1. [ ] 带 `X-Tenant-ID: tenant-1` 的请求，`TenantContext.current()` 返回 `tenant-1`
2. [ ] 不带 `X-Tenant-ID` 的请求，`TenantContext.current()` 返回 `null`（平台级）
3. [ ] Repository 查询自动附加 `tenant_id` 条件（通过审计日志 / SQL 日志验证）
4. [ ] 租户 A 创建的数据，租户 B 的查询结果中不可见（单元测试覆盖）
5. [ ] 非法租户 ID（非 UUID 格式）返回 400 Bad Request

**依赖**：T-1.1

---

### T-1.5 AES-256 加密服务

| 属性 | 内容 |
|------|------|
| **任务ID** | T-1.5 |
| **任务名称** | AES-256-GCM 加密服务 |
| **类型** | BE |
| **预估工时** | 1.5 人天 |
| **优先级** | P0 |

**需求描述**：
API Key 等敏感信息必须使用 AES-256-GCM 加密存储，密钥由平台统一管理（dev 环境用环境变量，prod 建议用 KMS）。加密服务需支持：加密、解密、批量轮换密钥。

**使用场景**：
- 管理员在 LLM Provider 配置页面输入 API Key，保存时自动加密
- 调用 LLM API 前，从数据库读取加密值，解密后使用
- 密钥轮换时，可批量重新加密所有存量数据

**输入**：
- 明文 API Key（如 `sk-xxxxxxxxxxxxxxxx`）
- 加密密钥（32 字节，Base64 编码存储于环境变量 `PLATFORM_ENCRYPTION_KEY`）

**输出**：
- `AesEncryptionService.java` — 加密/解密/密钥轮换
- `EncryptedValue.java` — 值对象（存储密文 + IV + Tag）
- 加密后的数据库字段格式标准化

**验收标准**：
1. [ ] 加密后密文长度 > 明文长度（证明确实加密）
2. [ ] 相同明文两次加密结果不同（随机 IV）
3. [ ] 解密后明文与原始完全一致
4. [ ] 密钥错误时解密抛出 `DecryptionException`，不泄露任何明文信息
5. [ ] 密钥轮换：1000 条记录可在 5 秒内完成重新加密
6. [ ] 单元测试覆盖率 100%

**依赖**：无

---

### T-1.6 X6 编辑器端口修复

| 属性 | 内容 |
|------|------|
| **任务ID** | T-1.6 |
| **任务名称** | 工作流编辑器 X6 端口配置修复 |
| **类型** | FE |
| **预估工时** | 2 人天 |
| **优先级** | P0 |

**需求描述**：
当前 X6 DAG 编辑器存在三个端口相关问题：1) `onDrop` 创建节点时重复调用 `addPort()`，导致端口 ID 冲突 2) `loadGraphDefinition()` 加载已有工作流时未调用 `addPort()`，节点无连接点 3) 节点连接后无法正确建立 Edge 关系

**使用场景**：
- 用户从左侧面板拖拽节点到画布，节点自动显示输入/输出端口
- 保存工作流后刷新页面，重新加载时端口和连线完整恢复
- 用户可正常拖拽连线建立节点关系

**输入**：
- `WorkflowEditorView.vue` 中 `initGraph()` / `onDrop()` / `loadGraphDefinition()`
- `NodePalette.vue` 中节点定义

**输出**：
- 修复后的 `WorkflowEditorView.vue`
- 修复后的 `NodePalette.vue`（如需要）

**验收标准**：
1. [ ] 拖拽新增节点后，每个节点有且仅有 1 个输入端口 + 1 个输出端口
2. [ ] 加载已有工作流后，所有节点端口可见且位置正确
3. [ ] 端口间可正常连线，Edge 数据正确保存到后端
4. [ ] 控制台无 X6 重复端口警告
5. [ ] Playwright E2E 截图验证通过

**依赖**：T-1.2, T-1.3

---

## Phase 2: LLM + Agent 核心（Week 2-3）

> **Phase 目标**：建立 LLM 管理、Agent 模板、Agent 实例三大核心域，实现 Agent 的创建、启动、监控、终止全生命周期。同时建立 Prompt 模板引擎和人格分层机制。

---

### T-2.1 LLM Provider 域模型与数据库

| 属性 | 内容 |
|------|------|
| **任务ID** | T-2.1 |
| **任务名称** | LLM Provider 域模型 + JPA Entity + 数据库表 |
| **类型** | BE + DB |
| **预估工时** | 1.5 人天 |
| **优先级** | P0 |

**需求描述**：
建立 LLM Provider 的完整域模型，包括聚合根 `LLMProvider`、值对象 `ProviderId` / `Quota` / `EncryptedValue`，以及对应的 JPA Entity 和数据库表。

**使用场景**：
平台管理员添加 Claude Provider 时，数据按 DDD 分层正确存储到数据库。

**输入**：
- PRD-003 F-1 需求定义
- DESIGN-002 `llm_providers` 表结构

**输出**：
- `LLMProvider.java`（Domain 聚合根）
- `ProviderId.java`, `Quota.java`（值对象）
- `LLMProviderEntity.java`（JPA Entity）
- `LLMProviderRepository.java`（Domain 接口）
- `LLMProviderJpaRepository.java`（基础设施实现）
- Flyway / Liquibase 迁移脚本

**验收标准**：
1. [ ] 域模型不可变性：创建后 `id` / `code` 不可修改
2. [ ] `apiKey` 字段在 JPA Entity 中存储为加密字符串（调用 T-1.5 加密服务）
3. [ ] 值对象有正确的 `equals()` / `hashCode()` 实现
4. [ ] Repository 支持按租户 ID 过滤（集成 T-1.4 租户上下文）
5. [ ] Flyway 迁移脚本可正常执行，表结构和索引与设计一致

**依赖**：T-1.4（多租户）, T-1.5（加密）

---

### T-2.2 LLM Provider API

| 属性 | 内容 |
|------|------|
| **任务ID** | T-2.2 |
| **任务名称** | LLM Provider CRUD + 测试连接 API |
| **类型** | BE |
| **预估工时** | 2 人天 |
| **优先级** | P0 |

**需求描述**：
实现 LLM Provider 的 RESTful API：列表、详情、创建、更新、删除、测试连接。

**使用场景**：
- 管理员打开 LLM 管理页面，看到已配置的 Provider 列表
- 点击"测试连接"，后端发送一个简单请求验证 API Key 有效
- 创建 Provider 时 API Key 只传一次，返回后不可再查看明文

**输入**：
- `POST /api/v1/llm-providers` — 创建 Provider
- `GET /api/v1/llm-providers` — 列表（支持 `?tenantId=` 过滤）
- `POST /api/v1/llm-providers/{id}/test` — 测试连接

**输出**：
- `LLMProviderController.java`
- `LLMAppService.java`
- DTO 类（`CreateProviderRequest`, `ProviderResponse`, `TestConnectionResponse`）
- API 文档（自动生成的 OpenAPI / Swagger）

**验收标准**：
1. [ ] 创建 Provider 后返回 `201 Created`，响应体含 `id`
2. [ ] 再次查询该 Provider，`apiKey` 字段不返回（或返回 `***`）
3. [ ] 测试连接成功返回 `{ "success": true, "model": "...", "latencyMs": 230 }`
4. [ ] 测试连接失败（Key 无效）返回 `{ "success": false, "error": "..." }`，HTTP 200（业务错误不抛 500）
5. [ ] 列表接口自动过滤当前租户 + 平台级 Provider
6. [ ] 所有接口有 `@Validated` 参数校验（名称非空、URL 格式正确等）
7. [ ] 单元测试：Controller 层 Mock Service，覆盖正常/异常路径

**依赖**：T-2.1

---

### T-2.3 Agent 模板域模型与数据库

| 属性 | 内容 |
|------|------|
| **任务ID** | T-2.3 |
| **任务名称** | Agent 模板域模型 + 数据库表 |
| **类型** | BE + DB |
| **预估工时** | 2 人天 |
| **优先级** | P0 |

**需求描述**：
建立 Agent 模板的完整域模型，包括 `AgentTemplate` 聚合根、`PersonaConfig` 值对象（IDENTITY/SOUL/AGENTS/USER/TOOLS 五层）、`RuntimeConfig`、`LLMConfig`、`ToolSet`、`ConstraintSet`、`ClaudeCodeConfig`。

**使用场景**：
平台预置 "Claude Code 开发者" 模板，包含完整的 Prompt、工具集、约束条件和 Claude Code 详细配置。

**输入**：
- PRD-003 F-2 需求定义
- DESIGN-002 `agent_templates` 表结构

**输出**：
- `AgentTemplate.java`（聚合根）
- `PersonaConfig.java`, `RuntimeConfig.java`, `LLMConfig.java`, `ToolSet.java`, `ConstraintSet.java`, `ClaudeCodeConfig.java`（值对象）
- `AgentTemplateEntity.java`（JPA Entity，JSON 字段映射）
- `AgentTemplateRepository.java` + 实现
- Flyway 迁移脚本

**验收标准**：
1. [ ] `PersonaConfig` 五层内容各自独立存储，可独立编辑
2. [ ] `ClaudeCodeConfig` 包含至少 10 个配置字段（model, permissionMode, allowedTools, timeout, env, extraArgs, cwd 等）
3. [ ] JSON 字段（runtime_config, llm_config, tools, constraints）在 JPA 中用 `@Convert` 自动序列化/反序列化
4. [ ] 模板版本管理：同一模板可有多个版本，支持按版本查询
5. [ ] 模板克隆：基于平台模板创建租户模板时，深拷贝所有配置
6. [ ] 单元测试：创建 → 克隆 → 更新 → 查询链路覆盖

**依赖**：T-1.4, T-1.5

---

### T-2.4 Agent 模板 API

| 属性 | 内容 |
|------|------|
| **任务ID** | T-2.4 |
| **任务名称** | Agent 模板 CRUD + 克隆 API |
| **类型** | BE |
| **预估工时** | 2 人天 |
| **优先级** | P0 |

**需求描述**：
实现 Agent 模板的 RESTful API：列表、详情、创建、克隆、更新、发布、删除。

**使用场景**：
- 用户打开 Agent 市场，看到平台预置模板和自建模板
- 点击"基于此模板创建"，快速克隆一份自定义模板
- 编辑模板时，五层 Prompt 分别展示，支持实时预览

**输入**：
- `GET /api/v1/agent-templates` — 列表（支持 `?type=` `?status=` 过滤）
- `GET /api/v1/agent-templates/{id}` — 详情（含完整 Prompt）
- `POST /api/v1/agent-templates` — 创建（支持 `parentTemplateId` 克隆）
- `PATCH /api/v1/agent-templates/{id}` — 更新
- `POST /api/v1/agent-templates/{id}/publish` — 发布

**输出**：
- `AgentTemplateController.java`
- `AgentAppService.java`（模板相关方法）
- DTO 类

**验收标准**：
1. [ ] 列表接口返回平台模板 + 当前租户模板，平台模板标记 `isPlatformTemplate: true`
2. [ ] 详情接口返回完整的五层 Prompt 内容
3. [ ] 克隆接口创建的新模板 `parentTemplateId` 指向来源
4. [ ] 发布操作将状态从 `DRAFT` 改为 `PUBLISHED`，已发布模板不可修改核心字段
5. [ ] 删除仅支持 `DRAFT` 状态模板，`PUBLISHED` 返回 409 Conflict
6. [ ] 参数校验：名称长度 1-255，timeout > 0，allowedTools 不为空

**依赖**：T-2.3

---

### T-2.5 Prompt 模板引擎

| 属性 | 内容 |
|------|------|
| **任务ID** | T-2.5 |
| **任务名称** | Prompt 模板引擎（变量替换 + 分层组装） |
| **类型** | BE |
| **预估工时** | 2.5 人天 |
| **优先级** | P0 |

**需求描述**：
实现 Prompt 模板引擎，支持：1) 变量替换：`${project.name}`, `${requirement.title}`, `${workspace.path}`, `${state.current}` 等 2) 分层组装：按 PLATFORM -> IDENTITY -> SOUL -> AGENTS -> USER -> TOOLS -> TASK 顺序组装 3) 执行契约注入：自动注入阶段指令和规则约束 4) 模板缓存：编译后的模板缓存避免重复解析

**使用场景**：
- 创建 Agent 实例时，引擎自动组装完整 Prompt：平台执行契约 + Agent 人格 + 项目信息 + 需求描述 + 工作目录
- 心跳调度触发时，引擎为每个步骤生成对应的 Prompt，自动替换项目相关变量

**输入**：
- 模板字符串（含 `${var}` 占位符）
- `RenderContext`（项目、需求、工作目录、当前状态等上下文）
- `AgentTemplate`（含五层 Prompt）

**输出**：
- `PromptTemplateEngine.java`（Domain Service）
- `RenderContext.java`
- `CompiledTemplate.java`（缓存对象）

**验收标准**：
1. [ ] `${project.name}` 正确替换为项目名称
2. [ ] `${requirement.title}` 正确替换为需求标题
3. [ ] 不存在的变量保持原样（`${unknown.var}` -> 不替换，或抛出明确异常）
4. [ ] 分层组装顺序正确，每层之间用 `\n\n` 分隔
5. [ ] 执行契约包含：当前阶段、下一步动作、执行规则（不输出解释、自动重试3次等）
6. [ ] 相同模板 + 相同上下文第二次渲染直接返回缓存结果（< 1ms）
7. [ ] 单元测试覆盖变量替换、分层组装、缓存命中/失效、异常路径

**依赖**：T-2.3

---

### T-2.6 Agent 实例域模型与数据库

| 属性 | 内容 |
|------|------|
| **任务ID** | T-2.6 |
| **任务名称** | Agent 实例域模型 + 数据库表 |
| **类型** | BE + DB |
| **预估工时** | 2 人天 |
| **优先级** | P0 |

**需求描述**：
建立 Agent 实例的完整域模型，包括 `AgentInstance` 聚合根、`InstanceStatus` 状态机、`InstanceId` 值对象。实例表需支持 Replica 机制（shadow_from）、心跳追踪（last_heartbeat）、人格内容独立存储。

**使用场景**：
- 用户创建 "登录功能开发" Agent 实例，系统基于模板深拷贝生成独立配置
- 实例运行中定期更新 `last_heartbeat`
- 实例异常退出后，`shadow_from` 可追溯来源模板

**输入**：
- PRD-003 F-3 需求定义
- DESIGN-002 `agent_instances` 表结构

**输出**：
- `AgentInstance.java`（聚合根）
- `InstanceStatus.java`（状态机：PENDING -> RUNNING -> COMPLETED/FAILED/TERMINATED）
- `InstanceId.java`, `AgentCode.java`（值对象）
- `AgentInstanceEntity.java`（JPA Entity）
- `AgentInstanceRepository.java` + 实现
- Flyway 迁移脚本

**验收标准**：
1. [ ] 状态机转换正确：`PENDING` -> `RUNNING`（启动成功）-> `COMPLETED`（正常结束）/ `FAILED`（异常）/ `TERMINATED`（人工终止）
2. [ ] 非法状态转换抛出 `IllegalStateTransitionException`
3. [ ] `shadow_from` 存储来源模板 Code，非空时实例为 Replica
4. [ ] `last_heartbeat` 可由外部定时更新
5. [ ] 五层人格内容独立存储（`persona_identity` / `persona_soul` / `persona_agents` / `persona_user` / `persona_tools`）
6. [ ] 单元测试：状态机全路径覆盖

**依赖**：T-2.3

---

### T-2.7 Agent 运行时端口与进程实现

| 属性 | 内容 |
|------|------|
| **任务ID** | T-2.7 |
| **任务名称** | Agent 运行时端口 + ProcessAgentRuntime 实现 |
| **类型** | BE |
| **预估工时** | 3 人天 |
| **优先级** | P0 |

**需求描述**：
定义 Agent 运行时端口（Port），并实现基于进程的模式（ProcessAgentRuntime）。支持启动 Claude Code / Kimi Code / OpenCode 子进程，通过 PTY 或管道捕获 stdout/stderr，通过 SSE/WebSocket 推送输出。

**使用场景**：
- 用户点击"启动 Agent"，后端启动 `claude.cmd --bare -p "prompt" --allowedTools Read,Edit,Bash`
- 用户在前端实时看到 Agent 的输出（"Reading files..." / "Executing bash..."）
- 用户点击"终止"，后端发送 SIGTERM 优雅终止进程

**输入**：
- `AgentStartRequest`（实例 ID、模板配置、Prompt、沙箱路径）
- `AgentRuntime.java` 端口定义

**输出**：
- `AgentRuntime.java`（端口接口：start / terminate / pause / resume / getStatus / streamOutput / sendInput）
- `ProcessAgentRuntime.java`（进程模式实现）
- `ProcessManager.java`（进程生命周期管理）
- `IOForwarder.java`（stdout/stderr -> SSE 流）
- `PTYFactory.java`（伪终端创建，可选）

**验收标准**：
1. [ ] `start()` 启动真实进程，返回进程 PID > 0
2. [ ] `streamOutput()` 返回 `Stream<LogEntry>`，实时捕获 stdout 每一行
3. [ ] Agent 输出"Starting..."可在 5 秒内通过 SSE 推送到前端
4. [ ] `terminate()` 发送 SIGTERM，进程在 10 秒内退出
5. [ ] 进程异常退出（非零退出码）自动触发 `AgentFailedEvent`
6. [ ] 同时启动 5 个 Agent 进程，互不干扰（独立工作目录）
7. [ ] 单元测试：Mock ProcessBuilder 验证命令行参数组装正确
8. [ ] 集成测试：真实启动 `echo "hello"` 进程，验证输出捕获完整

**依赖**：T-2.6

---

### T-2.8 Agent 实例 API + SSE 日志流

| 属性 | 内容 |
|------|------|
| **任务ID** | T-2.8 |
| **任务名称** | Agent 实例生命周期 API + SSE 实时日志流 |
| **类型** | BE |
| **预估工时** | 2.5 人天 |
| **优先级** | P0 |

**需求描述**：
实现 Agent 实例的 RESTful API：创建并启动、查询详情、终止、发送消息（持久型）、SSE 日志流。

**使用场景**：
- 用户选择 "Claude Code 开发者" 模板，输入 Prompt "实现登录接口"，点击启动
- 后端返回 `202 Accepted` 和实例 ID，前端立即建立 SSE 连接
- 前端实时看到 Agent 的执行日志，如同本地终端

**输入**：
- `POST /api/v1/agent-instances` — 创建并启动
- `GET /api/v1/agent-instances/{id}` — 详情
- `POST /api/v1/agent-instances/{id}/terminate` — 终止
- `POST /api/v1/agent-instances/{id}/message` — 向持久型 Agent 发消息
- `GET /api/v1/agent-instances/{id}/stream` — SSE 日志流

**输出**：
- `AgentInstanceController.java`
- `AgentAppService.java`（实例生命周期方法）
- `AgentLogSseEmitter.java`（SSE 推送器）
- DTO 类

**验收标准**：
1. [ ] 创建接口返回 `202 Accepted`（异步启动，不阻塞 HTTP）
2. [ ] SSE 流事件类型：`start` / `output` / `error` / `end`
3. [ ] SSE 连接建立后，Agent 输出的每一行在前端 < 100ms 内展示
4. [ ] 实例详情接口返回：状态、进程 PID、沙箱路径、启动时间、日志（最近 100 条）
5. [ ] 终止接口发送 SIGTERM，实例状态变为 `TERMINATED`
6. [ ] 持久型 Agent 支持 `sendInput()`，消息通过 stdin 送入进程
7. [ ] 并发测试：10 个客户端同时订阅不同实例的 SSE，互不干扰

**依赖**：T-2.6, T-2.7

---

### T-2.9 Agent 启动僵尸清理器

| 属性 | 内容 |
|------|------|
| **任务ID** | T-2.9 |
| **任务名称** | Agent 启动僵尸清理器（AgentStartupCleaner） |
| **类型** | BE |
| **预估工时** | 1.5 人天 |
| **优先级** | P0 |

**需求描述**：
应用启动时自动扫描所有 `RUNNING` / `PENDING` 状态的 AgentInstance，检查进程是否存在和最后心跳时间，清理异常状态的实例。

**使用场景**：
- 服务器因 OOM 被 kill -9 重启，重启后自动将之前所有 RUNNING 实例标记为 FAILED
- 防止僵尸实例占用配额、污染监控数据

**输入**：
- 数据库中 `status IN ('RUNNING', 'PENDING')` 的实例列表
- 进程 PID 存在性检查（OS 级）
- `last_heartbeat` 时间戳

**输出**：
- `AgentStartupCleaner.java`（`ApplicationRunner` 实现）
- 清理日志（记录每个被清理实例的原因）

**验收标准**：
1. [ ] 应用启动后 10 秒内完成扫描
2. [ ] 进程不存在的实例：标记为 `FAILED`，原因 `replica agent missing`
3. [ ] 超过 30 分钟无心跳的实例：标记为 `FAILED`，原因 `timeout`
4. [ ] 清理后释放沙箱资源（删除或归档）
5. [ ] 清理操作记录到应用日志，格式 `[StartupCleaner] Cleaned N stale instances`
6. [ ] 单元测试：Mock 进程存在性检查和 Repository，验证清理逻辑

**依赖**：T-2.6, T-2.7

---

### T-2.10 前端 Agent 市场页面

| 属性 | 内容 |
|------|------|
| **任务ID** | T-2.10 |
| **任务名称** | Agent 市场前端页面（AgentMarketView.vue） |
| **类型** | FE |
| **预估工时** | 2 人天 |
| **优先级** | P0 |

**需求描述**：
实现 Agent 市场页面，卡片式展示平台预置模板和租户自定义模板，支持搜索、分类筛选、克隆、详情查看。

**使用场景**：
- 用户进入 Agent 市场，看到 "Claude Code 开发者"、"架构师"、"测试工程师" 等卡片
- 输入 "Java" 搜索，筛选出相关模板
- 点击模板卡片，弹出详情抽屉，展示 Prompt、工具集、约束条件
- 点击"基于此模板创建"，跳转到模板编辑页（预填充来源模板内容）

**输入**：
- `GET /api/v1/agent-templates` 接口数据
- Element Plus 组件库

**输出**：
- `AgentMarketView.vue`
- `AgentCard.vue`（卡片组件）
- `AgentDetailDrawer.vue`（详情抽屉）
- `agent.ts`（前端 Service 层）

**验收标准**：
1. [ ] 页面加载 < 2s，展示模板卡片网格
2. [ ] 每个卡片展示：图标、名称、描述、类型标签（EPHEMERAL/PERSISTENT）、状态标签
3. [ ] 搜索框实时过滤（前端本地过滤，不请求后端）
4. [ ] 分类筛选标签：全部 / 开发 / 测试 / 架构 / 安全 / 文档
5. [ ] 点击卡片弹出详情抽屉，展示完整的五层 Prompt（可折叠）
6. [ ] "基于此模板创建"按钮克隆模板并跳转到编辑页
7. [ ] 空状态：无模板时展示引导创建界面

**依赖**：T-2.4

---

### T-2.11 前端场景市场页面

| 属性 | 内容 |
|------|------|
| **任务ID** | T-2.11 |
| **任务名称** | 场景市场前端页面（ScenarioMarketView.vue） |
| **类型** | FE |
| **预估工时** | 1.5 人天 |
| **优先级** | P0 |

**需求描述**：
实现场景市场页面，展示预置场景模板（如 PR 巡检流水线、需求开发流水线），支持一键应用到项目。

**使用场景**：
- 用户进入场景市场，看到 "PR 巡检流水线"（8 步定时巡检）卡片
- 点击卡片预览步骤列表：获取 PR -> 分析 -> 编写 LGTM -> 代码审查 -> 合并检查
- 点击"应用到项目"，选择目标项目，确认后自动生成心跳定时任务

**输入**：
- `GET /api/v1/scenario-templates` 接口数据（后续 T-4.1 提供）
- 项目列表数据（后续 T-3.3 提供）

**输出**：
- `ScenarioMarketView.vue`
- `ScenarioCard.vue`
- `ScenarioApplyDialog.vue`（应用确认弹窗）
- `scenario.ts`（前端 Service 层）

**验收标准**：
1. [ ] 页面展示场景卡片，含名称、描述、图标、步骤数量
2. [ ] 点击卡片展示步骤列表，每个步骤显示名称、类型、默认 Agent
3. [ ] "应用到项目"弹窗：选择项目 -> 确认参数 -> 提交
4. [ ] 应用成功后提示"已创建心跳任务，将在下次触发时执行"
5. [ ] 空状态/加载状态处理完善

**依赖**：T-2.10（UI 风格一致）, T-4.1（后端接口）, T-3.3（项目列表）

---

### T-2.12 前端 LLM Provider 管理页面

| 属性 | 内容 |
|------|------|
| **任务ID** | T-2.12 |
| **任务名称** | LLM Provider 管理页面（LLMProviderView.vue） |
| **类型** | FE |
| **预估工时** | 1.5 人天 |
| **优先级** | P0 |

**需求描述**：
实现 LLM Provider 管理页面，支持列表展示、添加、编辑、删除、测试连接。

**使用场景**：
- 管理员打开 LLM 管理页，看到已配置的 Claude、Kimi 等 Provider
- 点击"添加 Provider"，填写名称、API Base URL、API Key、默认模型
- 点击"测试连接"，显示连接成功/失败状态
- API Key 输入框为密码类型，保存后不可查看明文

**输入**：
- `GET /POST /api/v1/llm-providers` 接口

**输出**：
- `LLMProviderView.vue`
- `ProviderFormDialog.vue`（添加/编辑弹窗）
- `llm.ts`（前端 Service 层）

**验收标准**：
1. [ ] 列表展示：名称、Code、默认模型、状态、操作按钮
2. [ ] 添加/编辑表单：名称（必填）、Code（必填）、API Base URL（URL 校验）、API Key（密码输入）、默认模型、可用模型列表
3. [ ] 测试连接按钮显示加载状态，结果以 Toast 提示
4. [ ] API Key 创建后可编辑（重新输入），但列表页不展示明文
5. [ ] 删除前确认对话框

**依赖**：T-2.2

---

## Phase 3: 项目 + 需求（Week 4）

> **Phase 目标**：建立项目维度管理，绑定 GitHub 仓库，维护工程上下文包。建立需求管理，支持指派 Agent 执行。

---

### T-3.1 项目域模型与数据库

| 属性 | 内容 |
|------|------|
| **任务ID** | T-3.1 |
| **任务名称** | 项目域模型 + 数据库表 |
| **类型** | BE + DB |
| **预估工时** | 1.5 人天 |
| **优先级** | P0 |

**需求描述**：
建立项目的完整域模型，包括 `Project` 聚合根、`GitHubRepo` 值对象、`ProjectId`。支持绑定 GitHub 仓库、存储 OAuth Token（加密）、维护工程上下文路径。

**使用场景**：
- 用户创建 "AI Dev Platform" 项目，绑定 GitHub 仓库
- 平台自动 clone 仓库到 `/sandboxes/{tenant}/{project}/repo/`
- 项目关联默认 Agent 模板

**输入**：
- PRD-003 F-4 需求定义
- DESIGN-002 `projects` 表结构

**输出**：
- `Project.java`（聚合根）
- `GitHubRepo.java`, `ProjectId.java`（值对象）
- `ProjectEntity.java`（JPA Entity）
- `ProjectRepository.java` + 实现
- Flyway 迁移脚本

**验收标准**：
1. [ ] 项目创建时自动生成 `workspace_path`
2. [ ] `github_oauth_token` 加密存储（调用 T-1.5 加密服务）
3. [ ] `default_agent_template_id` 可为空，非空时校验模板存在
4. [ ] 租户隔离：租户 A 的项目租户 B 不可见
5. [ ] 单元测试：CRUD + 租户隔离覆盖

**依赖**：T-1.4, T-1.5

---

### T-3.2 GitHub OAuth + 仓库同步

| 属性 | 内容 |
|------|------|
| **任务ID** | T-3.2 |
| **任务名称** | GitHub OAuth 授权 + 仓库 Clone/同步 |
| **类型** | BE |
| **预估工时** | 2.5 人天 |
| **优先级** | P0 |

**需求描述**：
实现 GitHub OAuth 授权流程，获取用户授权后平台可访问其仓库。支持首次绑定时自动 clone，后续手动/自动同步。

**使用场景**：
- 用户点击"绑定 GitHub 仓库"，跳转 GitHub OAuth 授权页
- 授权成功后，平台自动 clone 仓库到沙箱
- 用户可手动点击"同步"更新代码

**输入**：
- GitHub OAuth App 配置（Client ID / Client Secret）
- 用户授权回调（`?code=xxx&state=xxx`）
- 目标仓库地址

**输出**：
- `GitHubClient.java`（端口接口）
- `GitHubClientImpl.java`（GitHub API 实现）
- `GitHubOAuthService.java`（OAuth 流程）
- `RepoSyncService.java`（clone / pull / sparse checkout）
- `GitHubWebhookHandler.java`（Webhook 接收，预留）

**验收标准**：
1. [ ] OAuth 授权流程完整：跳转 -> 授权 -> 回调 -> 获取 Access Token -> 存储（加密）
2. [ ] 首次绑定自动 clone 仓库到 `workspace_path/repo/`
3. [ ] 手动同步执行 `git pull`，更新代码
4. [ ] 大仓库支持 sparse checkout（可选子目录）
5. [ ] 同步状态记录：`SYNCING` -> `SYNCED` / `FAILED`
6. [ ] Token 失效时（401）提示用户重新授权
7. [ ] 单元测试：Mock GitHub API，覆盖 OAuth 和同步逻辑

**依赖**：T-3.1

---

### T-3.3 工程上下文注入

| 属性 | 内容 |
|------|------|
| **任务ID** | T-3.3 |
| **任务名称** | 工程上下文包生成与注入 |
| **类型** | BE |
| **预估工时** | 1.5 人天 |
| **优先级** | P0 |

**需求描述**：
Agent 启动前，自动将工程上下文注入沙箱：代码仓库、项目规范、需求文档、历史摘要。

**使用场景**：
- Agent 启动时，工作目录已包含完整项目代码和编码规范
- Agent 可读取 `docs/conventions.md` 了解项目规范
- Agent 可读取 `docs/requirements/{id}.md` 了解当前需求

**输入**：
- 项目 ID、需求 ID
- 沙箱路径

**输出**：
- `ContextInjector.java`（注入器）
- `ConventionExtractor.java`（从仓库提取规范）
- `HistorySummarizer.java`（历史需求摘要生成）

**验收标准**：
1. [ ] 注入后沙箱目录结构包含 repo/、docs/conventions.md、docs/history/、requirements/{req-id}.md
2. [ ] `conventions.md` 从仓库根目录 `AGENTS.md` 或 `.cursorrules` 读取，不存在时生成默认规范
3. [ ] 需求文档以 Markdown 格式写入，包含标题、描述、验收标准
4. [ ] 注入操作幂等：多次注入不重复追加内容
5. [ ] 注入完成后触发 `ContextInjectedEvent`

**依赖**：T-3.2

---

### T-3.4 需求域模型与数据库

| 属性 | 内容 |
|------|------|
| **任务ID** | T-3.4 |
| **任务名称** | 需求域模型 + 数据库表 |
| **类型** | BE + DB |
| **预估工时** | 1.5 人天 |
| **优先级** | P0 |

**需求描述**：
建立需求的完整域模型，包括 `Requirement` 聚合根、`RequirementStatus` 状态机、`AcceptanceCriteria` 值对象。支持指派 Agent、状态流转、验收。

**使用场景**：
- 项目经理录入"实现用户登录功能"需求
- 需求状态从 `PENDING` 流转到 `ACCEPTED`
- 每个需求关联一个工作流执行

**输入**：
- PRD-003 F-5 需求定义
- DESIGN-002 `requirements` 表结构

**输出**：
- `Requirement.java`（聚合根）
- `RequirementStatus.java`（状态机）
- `AcceptanceCriteria.java`（值对象）
- `RequirementEntity.java`（JPA Entity）
- `RequirementRepository.java` + 实现
- Flyway 迁移脚本

**验收标准**：
1. [ ] 状态机：`PENDING` -> `ANALYZING` -> `DESIGNING` -> `DEVELOPING` -> `TESTING` -> `REVIEWING` -> `ACCEPTED` / `REJECTED`
2. [ ] 非法状态转换抛出异常
3. [ ] `criteria` 以 JSON 数组存储，每条含描述和通过状态
4. [ ] `assigned_agent_id` 可为空，非空时校验 Agent 模板存在
5. [ ] 单元测试：状态机全路径覆盖

**依赖**：T-3.1

---

### T-3.5 需求管理 API + 前端页面

| 属性 | 内容 |
|------|------|
| **任务ID** | T-3.5 |
| **任务名称** | 需求管理 API + 前端需求列表/详情页 |
| **类型** | BE + FE |
| **预估工时** | 3 人天 |
| **优先级** | P0 |

**需求描述**：
实现需求的 RESTful API 和前端页面：列表、详情、创建、编辑、指派 Agent、验收。

**使用场景**：
- 用户打开项目详情页，看到需求列表
- 点击"新建需求"，填写标题、描述、验收标准
- 点击"指派 Agent"，选择工作流和 Agent 模板分配
- 执行完成后点击"验收通过"或"打回"

**输入**：
- `GET /POST /api/v1/requirements` 等接口

**输出**：
- `RequirementController.java` + `RequirementAppService.java`
- `RequirementListView.vue` + `RequirementDetailView.vue`
- `requirement.ts`（前端 Service）

**验收标准**：
1. [ ] 需求列表展示：标题、状态、创建时间、指派的 Agent
2. [ ] 需求详情页展示：完整描述、验收标准清单（可勾选）、执行历史
3. [ ] 指派 Agent：选择工作流 -> 为每个节点分配 Agent 模板 -> 提交触发执行
4. [ ] 验收操作：通过后状态变为 `ACCEPTED`，打回后变为 `REJECTED` 并记录原因
5. [ ] Markdown 渲染：需求描述支持 Markdown 格式展示
6. [ ] 状态变更记录到审计日志

**依赖**：T-3.4, T-2.4（Agent 模板接口）

---

### T-3.6 前端项目列表/详情页

| 属性 | 内容 |
|------|------|
| **任务ID** | T-3.6 |
| **任务名称** | 项目列表 + 项目详情前端页面 |
| **类型** | FE |
| **预估工时** | 2 人天 |
| **优先级** | P0 |

**需求描述**：
实现项目列表页和项目详情页，支持创建项目、绑定 GitHub 仓库、查看同步状态、管理需求。

**使用场景**：
- 用户打开项目列表，看到所有项目卡片
- 点击项目进入详情页，看到：仓库信息、同步状态、需求列表、执行历史
- 点击"同步"手动触发仓库更新

**输入**：
- `GET /POST /api/v1/projects` 等接口

**输出**：
- `ProjectListView.vue`
- `ProjectDetailView.vue`
- `project.ts`（前端 Service）

**验收标准**：
1. [ ] 项目列表：卡片展示（名称、描述、GitHub 仓库、最后同步时间）
2. [ ] 项目详情：Tab 切换（概览 / 需求 / 执行历史 / 设置）
3. [ ] 概览 Tab：展示仓库地址、同步状态、默认 Agent 模板、文件数量
4. [ ] 设置 Tab：修改名称、重新绑定仓库、修改默认 Agent 模板
5. [ ] GitHub OAuth 绑定流程集成（跳转授权页 -> 回调处理）
6. [ ] 同步按钮显示加载状态，同步完成后刷新文件数量

**依赖**：T-3.1, T-3.5


---

## Phase 4: 工作流升级 + 多 Agent 编排（Week 5-6）

> **Phase 目标**：升级工作流引擎支持 Agent 节点类型，建立产物传递机制，实现场景模板和心跳调度，支持 Agent 分身工厂。

---

### T-4.1 场景模板域模型与数据库

| 属性 | 内容 |
|------|------|
| **任务ID** | T-4.1 |
| **任务名称** | 场景模板域模型 + 心跳任务域模型 + 数据库表 |
| **类型** | BE + DB |
| **预估工时** | 2 人天 |
| **优先级** | P0 |

**需求描述**：
建立场景模板（ScenarioTemplate）和心跳任务（Heartbeat）的域模型。场景模板定义预置流水线（如 PR 巡检 8 步骤），心跳任务按 Cron 表达式定时触发场景执行。

**使用场景**：
- 平台预置 "PR 巡检流水线" 场景模板（8 步骤）
- 用户为项目创建心跳任务：每天 9:00 执行 PR 巡检
- 心跳触发时，自动生成工作流并执行

**输入**：
- PRD-003 F-11 需求定义
- DESIGN-002 `scenario_templates` / `heartbeats` 表结构

**输出**：
- `ScenarioTemplate.java`（聚合根）
- `Heartbeat.java`（聚合根）
- `ScenarioStep.java`, `NodeMapping.java`（值对象）
- `ScenarioTemplateEntity.java`, `HeartbeatEntity.java`（JPA Entity）
- Repository 接口 + 实现
- Flyway 迁移脚本

**验收标准**：
1. [ ] `ScenarioTemplate.steps` 为有序步骤列表，每步含：id、name、type、defaultAgentTemplateId
2. [ ] `ScenarioTemplate.inputSchema` 定义用户输入参数 JSON Schema
3. [ ] `Heartbeat.cronExpression` 为标准 Quartz Cron 表达式（如 `0 0 9 * * ?`）
4. [ ] `Heartbeat.status`：ACTIVE / PAUSED / DELETED
5. [ ] `Heartbeat.maxConcurrentAgents` 默认 1，防止并发爆炸
6. [ ] 单元测试：场景模板创建 -> 应用到项目 -> 生成心跳链路覆盖

**依赖**：T-2.3（Agent 模板域）, T-3.1（项目域）

---

### T-4.2 场景模板 API

| 属性 | 内容 |
|------|------|
| **任务ID** | T-4.2 |
| **任务名称** | 场景模板 CRUD + 应用到项目 API |
| **类型** | BE |
| **预估工时** | 2 人天 |
| **优先级** | P0 |

**需求描述**：
实现场景模板的 RESTful API：列表、详情、应用到项目（生成工作流 + 心跳任务）。

**使用场景**：
- 用户打开场景市场，看到 "PR 巡检流水线"
- 点击"应用到项目"，选择项目、确认参数，系统自动创建工作流和心跳任务
- 用户可在心跳管理页暂停/恢复/手动触发

**输入**：
- `GET /api/v1/scenario-templates` — 列表
- `GET /api/v1/scenario-templates/{id}` — 详情（含步骤定义）
- `POST /api/v1/scenario-templates/{id}/apply` — 应用到项目

**输出**：
- `ScenarioTemplateController.java`
- `ScenarioAppService.java`
- DTO 类

**验收标准**：
1. [ ] 列表接口返回平台场景 + 租户自定义场景
2. [ ] 详情接口返回完整步骤列表和输入参数 Schema
3. [ ] 应用接口：根据场景步骤生成工作流（nodes + edges）、根据用户输入参数填充 Prompt 模板变量、创建 Heartbeat 记录（如场景含定时配置）、返回 `{ "workflowId": "...", "heartbeatId": "..." }`
4. [ ] 应用失败时事务回滚（工作流和心跳不同时存在半成功状态）
5. [ ] 参数校验：`projectId` 必填，`inputs` 必须符合 inputSchema

**依赖**：T-4.1, T-3.1

---

### T-4.3 心跳调度器（Quartz）

| 属性 | 内容 |
|------|------|
| **任务ID** | T-4.3 |
| **任务名称** | 心跳调度器（Quartz Cron 定时触发） |
| **类型** | BE |
| **预估工时** | 2.5 人天 |
| **优先级** | P0 |

**需求描述**：
基于 Quartz Scheduler 实现心跳任务调度：创建/暂停/恢复/删除 Cron 任务，触发时启动对应工作流执行。

**使用场景**：
- 用户创建"每日 PR 巡检"心跳，Cron 为 `0 0 9 * * ?`
- 每天 9:00 Quartz 自动触发，创建 Execution 并启动工作流
- 用户可在管理页暂停心跳（不再自动触发）
- 用户可点击"立即执行"手动触发一次

**输入**：
- `POST /api/v1/heartbeats` — 创建心跳
- `POST /api/v1/heartbeats/{id}/pause` — 暂停
- `POST /api/v1/heartbeats/{id}/resume` — 恢复
- `POST /api/v1/heartbeats/{id}/trigger` — 手动触发

**输出**：
- `HeartbeatScheduler.java`（Quartz Job 定义）
- `HeartbeatTriggerService.java`（调度逻辑）
- `HeartbeatController.java`
- `HeartbeatAppService.java`

**验收标准**：
1. [ ] 创建心跳后 Quartz 自动注册 Job + Trigger
2. [ ] 暂停后 Trigger 状态为 `PAUSED`，到期不执行
3. [ ] 恢复后 Trigger 重新激活
4. [ ] 手动触发立即创建 Execution（不等待 Cron 到期）
5. [ ] 触发时自动检查项目并发数（`maxConcurrentAgents`），超限则跳过并记录原因
6. [ ] 删除心跳时同时删除 Quartz Job + Trigger
7. [ ] 调度日志：每次触发记录到 `heartbeat_runs` 表
8. [ ] 单元测试：Mock Quartz Scheduler，验证注册/暂停/恢复/触发逻辑

**依赖**：T-4.1, T-4.2

---

### T-4.4 工作流节点类型扩展

| 属性 | 内容 |
|------|------|
| **任务ID** | T-4.4 |
| **任务名称** | 工作流节点类型扩展（AGENT_EXECUTION / HUMAN_APPROVAL / CONDITION） |
| **类型** | BE + FE |
| **预估工时** | 2 人天 |
| **优先级** | P0 |

**需求描述**：
在现有工作流引擎基础上扩展节点类型，支持 Agent 执行节点、人工审批节点、条件分支节点。

**使用场景**：
- 工作流包含：Start -> AgentExecution(架构设计) -> AgentExecution(编码) -> HumanApproval(人工审核) -> End
- AgentExecution 节点启动 Agent 进程并等待完成
- HumanApproval 节点暂停执行，推送 WebSocket 通知给用户，用户确认后继续

**输入**：
- PRD-003 F-6 / F-10 需求定义
- 现有 `NodeType` 枚举和 `Node` 实体

**输出**：
- 扩展 `NodeType`：新增 `AGENT_EXECUTION`, `HUMAN_APPROVAL`, `CONDITION`, `PARALLEL`
- `AgentExecutionNodeConfig.java`（Agent 模板选择、输入映射、超时）
- `HumanApprovalNodeConfig.java`（审批人、超时设置、默认动作）
- `ConditionNodeConfig.java`（条件表达式、分支映射）
- 前端 `AgentExecutionNode.vue`, `HumanApprovalNode.vue`, `ConditionNode.vue`

**验收标准**：
1. [ ] 后端 `NodeType` 枚举包含所有新类型，序列化/反序列化正确
2. [ ] 前端节点面板支持拖拽新节点类型到画布
3. [ ] 每种节点有独立的属性配置面板
4. [ ] AGENT_EXECUTION 节点属性：Agent 模板下拉选择、超时输入、输入映射配置
5. [ ] HUMAN_APPROVAL 节点属性：审批人选择、超时时间、超时后默认动作（通过/拒绝）
6. [ ] CONDITION 节点属性：条件表达式输入、True/False 分支连线
7. [ ] 现有 START / END / TASK 节点保持兼容

**依赖**：T-1.6（X6 端口修复）, T-2.3

---

### T-4.5 产物传递机制

| 属性 | 内容 |
|------|------|
| **任务ID** | T-4.5 |
| **任务名称** | 产物解析与传递机制 |
| **类型** | BE |
| **预估工时** | 2.5 人天 |
| **优先级** | P0 |

**需求描述**：
Agent 执行完成后，自动解析输出产物（代码 diff、测试报告、架构文档等），传递给下游节点。产物类型标准化，支持输入映射（下游节点引用上游产物）。

**使用场景**：
- 架构设计 Agent 完成后，产出 `ARCH_DOC.md`，下游编码 Agent 自动读取作为输入
- 编码 Agent 完成后，产出 `CODE_DIFF.patch`，下游测试 Agent 基于 diff 运行测试
- 用户可在工作流编辑器中配置输入映射：`node.dev.input.archDoc = ${node.arch.output.archDoc}`

**输入**：
- Agent 沙箱输出目录（含 `changes.patch`, `report.md`, `test-report.json` 等）
- 工作流节点输入映射配置

**输出**：
- `ArtifactResolver.java`（产物解析器）
- `Artifact.java`（产物聚合根）
- `ArtifactType.java`：CODE_DIFF, ARCH_DOC, TEST_REPORT, AUDIT_REPORT, LOG
- `InputMapping.java`（输入映射解析）
- `ArtifactRepository.java` + 实现

**验收标准**：
1. [ ] 自动检测沙箱输出目录，识别 `changes.patch` -> `CODE_DIFF` 类型产物
2. [ ] 自动识别 `report.md` -> `AUDIT_REPORT` 类型产物
3. [ ] 自动识别 `test-report.json` -> `TEST_REPORT` 类型产物
4. [ ] 产物存储到数据库 + 文件系统（大文件存文件系统，元信息存数据库）
5. [ ] 输入映射解析：`${node.arch.output.archDoc}` 正确解析为架构节点产物的 content
6. [ ] 上游产物不存在时，下游节点启动前抛出 `ArtifactNotFoundException`
7. [ ] 产物内容最大 10MB，超限截断并记录警告
8. [ ] 单元测试：解析各种产物类型、映射引用、异常路径

**依赖**：T-2.6（Agent 实例域）, T-4.4

---

### T-4.6 Agent 分身工厂（ReplicaAgentFactory）

| 属性 | 内容 |
|------|------|
| **任务ID** | T-4.6 |
| **任务名称** | Agent 分身工厂 + 工作目录隔离 |
| **类型** | BE |
| **预估工时** | 2 人天 |
| **优先级** | P0 |

**需求描述**：
实现 Agent 分身机制：基于模板深拷贝生成独立 Replica，自动配置隔离的工作目录，血缘追踪（shadowFrom）。

**使用场景**：
- 用户基于 "Claude Code 开发者" 模板创建任务实例
- 系统自动生成新 ID 和 Code，深拷贝所有配置
- 自动设置 `cwd` 为任务专属沙箱路径，`forkSession=true`
- 任务完成后可通过 `shadowFrom` 追溯到原始模板

**输入**：
- `AgentTemplate`（来源模板）
- `ProjectId`, `RequirementId`
- 工作目录路径

**输出**：
- `AgentSnapshot.java`（模板快照，深拷贝）
- `ReplicaAgentFactory.java`（分身工厂）
- `SandboxManager.java`（沙箱分配）

**验收标准**：
1. [ ] `AgentSnapshot.from(template)` 深拷贝所有值对象（修改 snapshot 不影响原模板）
2. [ ] `ReplicaAgentFactory.createReplica()` 生成唯一 `InstanceId` 和 `AgentCode`
3. [ ] 自动配置 `ClaudeCodeConfig.cwd = 任务沙箱路径`
4. [ ] 自动配置 `forkSession = true`, `continueConversation = false`
5. [ ] `shadowFrom` 存储来源模板 Code
6. [ ] 沙箱路径格式：`/sandboxes/{tenantId}/{projectId}/{instanceId}/`
7. [ ] 沙箱创建时自动建立目录结构，设置权限（只读/读写）
8. [ ] 单元测试：深拷贝验证、沙箱创建验证、配置隔离验证

**依赖**：T-2.3, T-2.6, T-3.1

---

### T-4.7 DAGScheduler 升级

| 属性 | 内容 |
|------|------|
| **任务ID** | T-4.7 |
| **任务名称** | DAGScheduler 升级（产物传递 + 并行执行） |
| **类型** | BE |
| **预估工时** | 3 人天 |
| **优先级** | P0 |

**需求描述**：
升级现有 DAGScheduler，支持：Agent 节点执行、产物传递、并行节点调度、条件分支、人工审批暂停/恢复。

**使用场景**：
- 工作流启动后，Scheduler 按拓扑排序执行
- 无依赖的节点并行执行（如架构设计和需求分析可同时启动）
- 条件节点根据表达式决定走 True/False 分支
- HumanApproval 节点暂停，等待用户确认后继续

**输入**：
- `Workflow` 定义（含新节点类型）
- `Execution` 执行记录
- 产物映射表

**输出**：
- `DAGScheduler.java`（升级后）
- `AgentExecutionTask.java`（Agent 执行单元）
- `ExecutionStateMachine.java`（执行状态机）
- `ParallelExecutor.java`（并行执行器）

**验收标准**：
1. [ ] 拓扑排序正确：有向无环图按依赖顺序执行
2. [ ] 并行执行：入度为 0 且无上依赖的节点同时启动
3. [ ] 产物传递：节点启动前，自动将上游产物注入输入映射
4. [ ] 条件分支：表达式求值后，只执行匹配分支的下游节点
5. [ ] 人工审批：节点到达时状态变为 `WAITING_APPROVAL`，推送 WebSocket 通知；用户确认后状态变为 `APPROVED` 继续执行
6. [ ] 超时处理：Agent 节点超时时自动终止并标记 `FAILED`
7. [ ] 失败策略：单节点失败时，可选"继续"或"终止整个执行"
8. [ ] 单元测试：线性执行、并行执行、条件分支、审批暂停/恢复、超时

**依赖**：T-4.4, T-4.5, T-4.6, T-2.7

---

### T-4.8 前端工作流编辑器重构

| 属性 | 内容 |
|------|------|
| **任务ID** | T-4.8 |
| **任务名称** | 工作流编辑器重构（Agent 节点 + 属性面板） |
| **类型** | FE |
| **预估工时** | 3.5 人天 |
| **优先级** | P0 |

**需求描述**：
重构工作流编辑器，支持新节点类型、属性面板配置、产物预览、执行时节点高亮。

**使用场景**：
- 用户从节点面板拖拽 "Agent Execution" 节点到画布
- 点击节点打开属性面板：选择 Agent 模板、配置输入映射、设置超时
- 保存后节点显示所选 Agent 模板的名称
- 执行时当前运行节点边框高亮为绿色

**输入**：
- X6 Graph API
- Agent 模板列表接口
- 工作流定义数据

**输出**：
- `WorkflowEditorView.vue`（重构）
- `PropertyPanel.vue`（重构：支持 Agent 配置）
- `AgentExecutionConfig.vue`（Agent 模板选择、输入映射配置）
- `HumanApprovalConfig.vue`（审批人、超时配置）
- `ConditionConfig.vue`（条件表达式）
- `ExecutionOverlay.vue`（执行时高亮）

**验收标准**：
1. [ ] 节点面板新增 AgentExecution / HumanApproval / Condition 节点图标
2. [ ] 拖拽新增节点后，属性面板自动打开
3. [ ] AgentExecution 属性面板：Agent 模板下拉选择（调用后端接口）、超时时间输入（默认 600 秒）、输入映射表格（上游节点 -> 当前节点输入参数）
4. [ ] HumanApproval 属性面板：审批人选择、超时输入、默认动作单选
5. [ ] Condition 属性面板：条件表达式输入、True/False 分支标签
6. [ ] 保存时校验：所有 AgentExecution 节点必须选择了 Agent 模板
7. [ ] 执行时高亮：当前运行节点边框绿色闪烁，已完成节点灰色，失败节点红色
8. [ ] 产物预览：点击已完成节点，侧边栏展示该节点产物的摘要（diff 行数、报告标题）

**依赖**：T-1.6, T-2.10, T-4.4

---

### T-4.9 执行监控升级

| 属性 | 内容 |
|------|------|
| **任务ID** | T-4.9 |
| **任务名称** | 执行监控页面升级（节点高亮 + 实时日志） |
| **类型** | FE |
| **预估工时** | 2 人天 |
| **优先级** | P0 |

**需求描述**：
升级执行监控页面，支持实时展示工作流执行状态、节点高亮、Agent 实时日志聚合展示。

**使用场景**：
- 用户打开执行监控页，看到工作流 DAG 图形化展示
- 当前运行的 Agent 节点实时显示日志（SSE 聚合）
- 点击节点查看该 Agent 的完整执行日志
- 执行完成后展示产物列表（可下载 diff / 报告）

**输入**：
- `GET /api/v1/executions/{id}` — 执行详情
- `GET /api/v1/agent-instances/{id}/stream` — SSE 日志流
- `GET /api/v1/artifacts?executionId=...` — 产物列表

**输出**：
- `ExecutionMonitorView.vue`（升级）
- `ExecutionTimeline.vue`（执行时间线）
- `NodeLogPanel.vue`（节点日志面板）
- `ArtifactDownloadPanel.vue`（产物下载面板）

**验收标准**：
1. [ ] 执行状态实时更新（WebSocket 或轮询，延迟 < 2s）
2. [ ] 节点颜色：等待=灰色，运行=绿色闪烁，完成=蓝色，失败=红色，审批=黄色
3. [ ] 点击运行中节点，侧边栏实时展示 Agent 日志（SSE 连接）
4. [ ] 点击已完成节点，侧边栏展示产物列表（diff / 报告 / 日志文件）
5. [ ] 执行时间线：横向时间轴展示每个节点的开始/结束时间
6. [ ] 失败节点可点击"查看错误"，展示错误摘要和完整日志
7. [ ] 审批节点显示"去审批"按钮，跳转审批页面

**依赖**：T-2.8（SSE 日志流）, T-4.7, T-4.8

---

## Phase 5: SaaS + 龙虾 + 报告（Week 7-8）

> **Phase 目标**：建立审计日志、资源配额、冷却机制，实现个人助理（龙虾）和报告系统，支持平台级 Agent 市场管理。

---

### T-5.1 产物收集与报告生成

| 属性 | 内容 |
|------|------|
| **任务ID** | T-5.1 |
| **任务名称** | 产物收集 + 报告自动生成 |
| **类型** | BE |
| **预估工时** | 2.5 人天 |
| **优先级** | P1 |

**需求描述**：
执行完成后自动收集所有产物，生成标准化执行报告（Markdown 格式），包含执行摘要、代码 diff、测试报告、审核意见。

**使用场景**：
- 工作流执行完成后，用户收到通知"执行完成，查看报告"
- 打开报告页面，看到：谁做了什么、改了哪些文件、测试结果如何
- 可下载报告为 Markdown 文件

**输入**：
- 执行 ID
- 所有 `AgentRun` 记录和关联产物

**输出**：
- `ReportAppService.java`
- `ReportGenerator.java`（报告生成器）
- `ReportTemplate.java`（报告模板）
- `Report.java`（聚合根）

**验收标准**：
1. [ ] 报告包含：执行摘要（时间、节点数、成功/失败数）、每个节点的产物摘要
2. [ ] 代码 diff 以语法高亮的代码块展示
3. [ ] 测试报告以表格展示（通过/失败/覆盖率）
4. [ ] 报告支持 Markdown 和 HTML 两种格式导出
5. [ ] 大 diff（> 1000 行）自动折叠，可展开查看
6. [ ] 成本统计：每个节点消耗的 Token 数、预估费用
7. [ ] 单元测试：Mock 产物数据，验证报告生成

**依赖**：T-4.5（产物机制）, T-4.7

---

### T-5.2 审计日志

| 属性 | 内容 |
|------|------|
| **任务ID** | T-5.2 |
| **任务名称** | 审计日志系统（全量操作记录） |
| **类型** | BE |
| **预估工时** | 2 人天 |
| **优先级** | P1 |

**需求描述**：
所有关键操作记录审计日志：谁、什么时间、做了什么、结果如何。支持按租户/用户/时间范围查询。

**使用场景**：
- 管理员查询某用户的操作历史
- 审计需求变更记录（谁修改了需求描述）
- 安全审计：追踪 Agent 的所有操作

**输入**：
- 所有 Controller 方法调用（通过 AOP 拦截）
- 领域事件（AgentStartedEvent / AgentCompletedEvent 等）

**输出**：
- `AuditLogInterceptor.java`（AOP 拦截器）
- `AuditLog.java`（实体）
- `AuditLogRepository.java` + 实现
- `AuditLogController.java`（查询接口）

**验收标准**：
1. [ ] 所有 REST API 调用自动记录：用户 ID、IP 地址、请求方法、URL、请求体摘要、响应状态
2. [ ] 敏感字段（apiKey、password）自动脱敏（替换为 `***`）
3. [ ] 领域事件自动记录：Agent 启动/完成/失败、需求状态变更
4. [ ] 查询接口支持分页、按时间范围过滤、按操作类型过滤
5. [ ] 审计日志保留 90 天，过期自动归档（或删除）
6. [ ] 单元测试：验证拦截器正确记录各种操作

**依赖**：T-1.4

---

### T-5.3 龙虾服务（持久型 Agent）

| 属性 | 内容 |
|------|------|
| **任务ID** | T-5.3 |
| **任务名称** | 龙虾服务（持久型 Agent + 聊天界面） |
| **类型** | BE + FE |
| **预估工时** | 3.5 人天 |
| **优先级** | P1 |

**需求描述**：
实现个人持久型 Agent（"龙虾"），长期运行，支持长期记忆、主动推送、任务委托、报告汇总。

**使用场景**：
- 用户创建"龙虾"助理，设定性格为"严谨的技术专家"
- 对龙虾说"帮我实现登录功能"，龙虾拆解需求并派发 Agent 执行
- 执行完成后龙虾主动推送："开发 Agent 已完成，diff 共 5 个文件，请验收"
- 用户问"今天有哪些执行结果"，龙虾汇总当天所有执行

**输入**：
- WebSocket 消息（用户聊天输入）
- 领域事件（Agent 完成 / 失败 / 需要审批）

**输出**：
- `LobsterAppService.java`
- `LobsterSession.java`（会话管理）
- `LobsterMemory.java`（长期记忆）
- `LobsterChatView.vue`（聊天界面）
- `ChatMessage.vue`（消息组件）

**验收标准**：
1. [ ] 龙虾创建：可选择模板、设定性格、专长领域
2. [ ] 聊天界面：对话式交互，支持 Markdown 渲染、代码块高亮
3. [ ] 任务委托：用户输入"实现登录功能"，龙虾调用 `RequirementAppService` 创建需求并触发执行
4. [ ] 主动推送：Agent 完成/失败/需要审批时，WebSocket 推送到前端，显示通知 Badge
5. [ ] 长期记忆：记录用户偏好（技术栈、编码风格），后续对话中自动引用
6. [ ] 报告汇总：用户输入"汇总今天结果"，龙虾查询当天执行并生成摘要
7. [ ] 空闲休眠：30 分钟无交互后进入休眠（释放进程资源），新消息时唤醒
8. [ ] 单元测试：Mock LLM 调用，验证任务委托和报告汇总逻辑

**依赖**：T-2.7（Agent 运行时）, T-3.5（需求管理）, T-5.4

---

### T-5.4 主动推送（WebSocket）

| 属性 | 内容 |
|------|------|
| **任务ID** | T-5.4 |
| **任务名称** | WebSocket 主动推送服务 |
| **类型** | BE + FE |
| **预估工时** | 2 人天 |
| **优先级** | P1 |

**需求描述**：
建立 WebSocket 推送通道，支持服务端主动向客户端推送：任务进度、异常告警、决策请求、系统通知。

**使用场景**：
- Agent 执行完成时，前端收到推送，显示桌面通知
- 需要人工审批时，前端弹出模态框
- 平台公告推送到所有在线用户

**输入**：
- 领域事件（`AgentCompletedEvent`, `HumanApprovalRequiredEvent`, `LobsterPushEvent`）
- WebSocket 连接（`ws://host/ws`）

**输出**：
- `WebSocketConfig.java`（Spring WebSocket 配置）
- `PushHandler.java`（事件 -> WebSocket 消息转换）
- `NotificationService.java`（通知管理）
- 前端 `useWebSocket.ts`（WebSocket 封装 Hook）

**验收标准**：
1. [ ] WebSocket 连接建立后，心跳保活（每 30 秒 ping/pong）
2. [ ] Agent 完成事件推送：前端 1 秒内收到通知
3. [ ] 审批请求推送：前端弹出模态框，用户选择后回复后端
4. [ ] 断线重连：WebSocket 断开 5 秒内自动重连，重连后恢复未读通知
5. [ ] 多标签页：同一用户打开多个标签页，每个标签页都收到推送
6. [ ] 通知持久化：离线期间的通知，用户上线后批量推送

**依赖**：无（可与 T-5.3 并行开发）

---

### T-5.5 冷却机制（CooldownTracker）

| 属性 | 内容 |
|------|------|
| **任务ID** | T-5.5 |
| **任务名称** | 冷却机制（防止重复操作） |
| **类型** | BE |
| **预估工时** | 1.5 人天 |
| **优先级** | P1 |

**需求描述**：
防止 Agent 对同一对象（Issue / PR / 需求）在短时间内反复操作。平台层 + Prompt 层双保险。

**使用场景**：
- 心跳巡检发现 PR #123 需要审查，Agent 执行审查并评论
- 2 小时后再次巡检，PR #123 无新变更，Agent 检查冷却记录发现 3 小时内已操作过，跳过
- 前端显示"本次跳过原因：3 小时内已执行过审查"

**输入**：
- 目标对象 ID（Issue / PR / Requirement）
- Agent 模板 ID
- 操作类型

**输出**：
- `CooldownTracker.java`
- `CooldownRecord.java`（实体）
- `CooldownRecordRepository.java`

**验收标准**：
1. [ ] 检查接口：`isCooldownActive(targetId, agentId, operationType)` 返回 boolean
2. [ ] 默认冷却时间 3 小时，可配置（1h / 3h / 6h / 24h）
3. [ ] 冷却记录存储到数据库（或 Redis），含目标 ID、Agent ID、操作类型、上次操作时间
4. [ ] Prompt 层兜底：在 Agent Prompt 中注入冷却规则，要求 Agent 自检时间戳
5. [ ] 跳过操作记录到审计日志，前端可查看跳过原因
6. [ ] 单元测试：冷却命中、冷却过期、不同操作类型隔离

**依赖**：T-5.2（审计日志）

---

### T-5.6 资源配额（QuotaChecker）

| 属性 | 内容 |
|------|------|
| **任务ID** | T-5.6 |
| **任务名称** | 资源配额检查（Token / 并发 / 存储） |
| **类型** | BE |
| **预估工时** | 2 人天 |
| **优先级** | P1 |

**需求描述**：
实现资源配额检查系统，限制每个租户的 Agent 并发数、LLM Token 月度消耗、沙箱存储空间。

**使用场景**：
- 租户 A 的月度 Token 配额 1000 万，已使用 950 万，再次请求时返回配额超限错误
- 租户 B 的并发上限 5，已运行 5 个 Agent，第 6 个创建请求被拒绝
- 管理员可在后台调整租户配额

**输入**：
- 租户配额配置（`tenants.quota_config` JSON）
- 当前用量（Token 已用、并发数、存储已用）

**输出**：
- `QuotaChecker.java`
- `QuotaConfig.java`（值对象）
- `UsageTracker.java`（用量追踪）

**验收标准**：
1. [ ] Token 配额检查：月度用量 >= 上限时，LLM 调用返回 `QuotaExceededException`
2. [ ] 并发配额检查：RUNNING + PENDING 实例数 >= 上限时，创建请求返回 429
3. [ ] 存储配额检查：沙箱总大小 >= 上限时，新同步请求返回 429
4. [ ] 用量统计准实时（延迟 < 1 分钟）
5. [ ] 管理员可动态调整配额，调整后立即生效
6. [ ] 单元测试：配额检查的各种边界条件

**依赖**：T-2.6（Agent 实例）, T-2.1（LLM Provider 配额）

---

### T-5.7 平台 Agent 市场管理

| 属性 | 内容 |
|------|------|
| **任务ID** | T-5.7 |
| **任务名称** | 平台 Agent 市场管理后台 |
| **类型** | BE + FE |
| **预估工时** | 2 人天 |
| **优先级** | P1 |

**需求描述**：
管理员后台：发布平台级 Agent 模板和场景模板，管理租户，查看平台用量统计。

**使用场景**：
- 平台管理员登录后台，发布新的 "Python 开发专家" 模板
- 查看各租户用量：Agent 执行次数、Token 消耗、存储使用
- 禁用违规租户的某些功能

**输入**：
- 管理员身份（role = PLATFORM_ADMIN）

**输出**：
- `AdminController.java`
- `PlatformStatsService.java`
- 前端管理后台页面

**验收标准**：
1. [ ] 平台模板发布：管理员创建模板，所有租户可见但不可修改
2. [ ] 模板下架：下架后租户无法基于该模板创建新实例（已有实例不受影响）
3. [ ] 租户管理：查看租户列表、调整配额、启用/禁用租户
4. [ ] 用量统计：仪表盘展示平台级指标（总执行次数、总 Token 消耗、活跃租户数）
5. [ ] 权限控制：非管理员访问返回 403

**依赖**：T-2.4, T-4.2, T-5.6

---

### T-5.8 心跳历史查询

| 属性 | 内容 |
|------|------|
| **任务ID** | T-5.8 |
| **任务名称** | 心跳执行记录 + 历史查询 |
| **类型** | BE + FE |
| **预估工时** | 1.5 人天 |
| **优先级** | P1 |

**需求描述**：
记录每次心跳触发的执行历史，支持查询、重试、查看详细日志。

**使用场景**：
- 用户打开心跳管理页，看到"每日 PR 巡检"的执行历史
- 昨天 9:00 的执行失败了，点击查看错误日志
- 点击"重试"手动重新执行该次心跳

**输入**：
- `HeartbeatRun` 记录
- 关联的 `Execution` 和 `Artifact`

**输出**：
- `HeartbeatRun.java`（实体）
- `HeartbeatRunRepository.java`
- `HeartbeatRunController.java`
- 前端心跳历史页面

**验收标准**：
1. [ ] 每次心跳触发自动创建 `HeartbeatRun` 记录（状态：RUNNING -> COMPLETED/FAILED）
2. [ ] 查询接口：按心跳 ID、时间范围、状态过滤
3. [ ] 重试接口：选择历史记录，重新创建 Execution 执行
4. [ ] 历史详情：展示触发时间、执行时长、状态、产物列表、日志链接
5. [ ] 保留 30 天历史，过期自动清理

**依赖**：T-4.3, T-4.9


---

## Phase 6: 测试与优化（Week 9）

> **Phase 目标**：全面测试覆盖，性能调优，安全扫描，确保平台可交付。

---

### T-6.1 单元测试补全

| 属性 | 内容 |
|------|------|
| **任务ID** | T-6.1 |
| **任务名称** | 单元测试补全（Domain >= 95%，整体 >= 80%） |
| **类型** | BE + FE |
| **预估工时** | 3 人天 |
| **优先级** | P0 |

**需求描述**：
补全所有核心模块的单元测试，确保 Domain 层覆盖率 >= 95%，整体 >= 80%。

**使用场景**：
- CI 流水线执行 `./mvnw test`，所有测试通过
- JaCoCo 报告展示覆盖率达标

**验收标准**：
1. [ ] Domain 层（聚合根、值对象、领域服务）覆盖率 >= 95%
2. [ ] Application 层（AppService）覆盖率 >= 80%
3. [ ] Infrastructure 层（Adapter、Repository 实现）覆盖率 >= 60%
4. [ ] 前端关键组件（Vue 组件、Pinia Store）单元测试覆盖
5. [ ] 所有测试可在 5 分钟内执行完毕
6. [ ] 无 flaky test（不稳定测试）

**依赖**：所有 Phase 1-5 任务

---

### T-6.2 集成测试

| 属性 | 内容 |
|------|------|
| **任务ID** | T-6.2 |
| **任务名称** | 关键 API 链路集成测试 |
| **类型** | BE + FE |
| **预估工时** | 2 人天 |
| **优先级** | P0 |

**需求描述**：
编写集成测试覆盖关键 API 链路，使用 Testcontainers（PostgreSQL）和真实进程启动验证。

**关键链路**：
1. 创建 LLM Provider -> 创建 Agent 模板 -> 创建 Agent 实例 -> 启动 -> 终止
2. 创建项目 -> 绑定 GitHub -> 创建需求 -> 指派 Agent -> 执行 -> 验收
3. 应用场景模板 -> 生成工作流 -> 心跳触发 -> 执行完成 -> 查看报告

**验收标准**：
1. [ ] 链路 1 集成测试：真实启动 `echo` 进程，验证 SSE 输出
2. [ ] 链路 2 集成测试：Mock GitHub API，验证完整需求交付流程
3. [ ] 链路 3 集成测试：Mock Quartz 触发，验证场景应用 + 执行
4. [ ] 使用 Testcontainers 启动 PostgreSQL，验证数据库迁移
5. [ ] 集成测试可在 10 分钟内执行完毕

**依赖**：T-6.1

---

### T-6.3 E2E 测试

| 属性 | 内容 |
|------|------|
| **任务ID** | T-6.3 |
| **任务名称** | E2E 端到端测试（Playwright） |
| **类型** | FE + BE |
| **预估工时** | 2 人天 |
| **优先级** | P0 |

**需求描述**：
使用 Playwright 编写端到端测试，覆盖用户核心操作流程。

**核心场景**：
1. 用户登录 -> 创建项目 -> 绑定 GitHub -> 创建需求 -> 指派 Agent -> 查看执行
2. 管理员登录 -> 添加 LLM Provider -> 创建 Agent 模板 -> 发布到市场
3. 用户进入场景市场 -> 应用 PR 巡检场景 -> 查看心跳任务

**验收标准**：
1. [ ] 3 个核心场景全部自动化
2. [ ] 每个场景执行时间 < 2 分钟
3. [ ] 截图对比：关键页面视觉回归检测
4. [ ] 失败时自动截图和录屏
5. [ ] CI 中自动执行 E2E 测试

**依赖**：T-6.2

---

### T-6.4 性能测试

| 属性 | 内容 |
|------|------|
| **任务ID** | T-6.4 |
| **任务名称** | 性能测试与调优 |
| **类型** | BE + FE |
| **预估工时** | 1.5 人天 |
| **优先级** | P1 |

**需求描述**：
验证非功能需求中的性能指标，识别并修复瓶颈。

**关键指标**：
| 指标 | 目标 | 测试方法 |
|------|------|---------|
| Agent 进程启动 | < 10s | k6 / JMeter 压测 |
| 首次输出延迟 | < 5s | 计时测试 |
| SSE 推送延迟 | < 100ms | 网络延迟测试 |
| 工作流保存 | < 500ms | API 响应时间测试 |
| 页面首屏加载 | < 2s | Lighthouse |

**验收标准**：
1. [ ] 所有指标达到目标值
2. [ ] 并发 10 个 Agent 启动，系统稳定（无 OOM、无端口冲突）
3. [ ] 数据库慢查询优化：所有查询 < 100ms
4. [ ] 生成性能测试报告

**依赖**：T-6.2

---

### T-6.5 安全扫描

| 属性 | 内容 |
|------|------|
| **任务ID** | T-6.5 |
| **任务名称** | 安全扫描与修复 |
| **类型** | BE + FE |
| **预估工时** | 1.5 人天 |
| **优先级** | P1 |

**需求描述**：
执行安全扫描，修复 Blocker / Critical 级别问题。

**扫描工具**：
- 后端：SpotBugs + FindSecBugs、OWASP Dependency Check
- 前端：npm audit
- 容器：Trivy（如使用 Docker）

**验收标准**：
1. [ ] SpotBugs 无 Blocker / Critical / High 问题
2. [ ] OWASP Dependency Check 无 High 级别漏洞
3. [ ] `npm audit` 无 High / Critical 漏洞
4. [ ] 敏感信息扫描：代码中无硬编码 API Key、密码
5. [ ] 生成安全扫描报告

**依赖**：所有开发任务


---

## 附录 A：任务依赖关系图

```
Phase 1 (Week 1)
├── T-1.1 Checkstyle
├── T-1.2 ToolbarPanel
├── T-1.3 状态映射 --DEP--> T-1.2
├── T-1.4 多租户 --DEP--> T-1.1
├── T-1.5 加密
└── T-1.6 X6 端口 --DEP--> T-1.2, T-1.3

Phase 2 (Week 2-3)
├── T-2.1 LLM 域 --DEP--> T-1.4, T-1.5
├── T-2.2 LLM API --DEP--> T-2.1
├── T-2.3 Agent 模板域 --DEP--> T-1.4, T-1.5
├── T-2.4 Agent 模板 API --DEP--> T-2.3
├── T-2.5 Prompt 引擎 --DEP--> T-2.3
├── T-2.6 Agent 实例域 --DEP--> T-2.3
├── T-2.7 Agent 运行时 --DEP--> T-2.6
├── T-2.8 Agent API --DEP--> T-2.6, T-2.7
├── T-2.9 僵尸清理 --DEP--> T-2.6, T-2.7
├── T-2.10 Agent 市场 --DEP--> T-2.4
├── T-2.11 场景市场 --DEP--> T-2.10, T-4.1, T-3.3
└── T-2.12 LLM 管理页 --DEP--> T-2.2

Phase 3 (Week 4)
├── T-3.1 项目域 --DEP--> T-1.4, T-1.5
├── T-3.2 GitHub OAuth --DEP--> T-3.1
├── T-3.3 上下文注入 --DEP--> T-3.2
├── T-3.4 需求域 --DEP--> T-3.1
├── T-3.5 需求 API --DEP--> T-3.4, T-2.4
└── T-3.6 项目页面 --DEP--> T-3.1, T-3.5

Phase 4 (Week 5-6)
├── T-4.1 场景模板域 --DEP--> T-2.3, T-3.1
├── T-4.2 场景 API --DEP--> T-4.1, T-3.1
├── T-4.3 心跳调度 --DEP--> T-4.1, T-4.2
├── T-4.4 节点扩展 --DEP--> T-1.6, T-2.3
├── T-4.5 产物传递 --DEP--> T-2.6, T-4.4
├── T-4.6 分身工厂 --DEP--> T-2.3, T-2.6, T-3.1
├── T-4.7 DAGScheduler --DEP--> T-4.4, T-4.5, T-4.6, T-2.7
├── T-4.8 编辑器重构 --DEP--> T-1.6, T-2.10, T-4.4
└── T-4.9 执行监控 --DEP--> T-2.8, T-4.7, T-4.8

Phase 5 (Week 7-8)
├── T-5.1 报告生成 --DEP--> T-4.5, T-4.7
├── T-5.2 审计日志 --DEP--> T-1.4
├── T-5.3 龙虾 --DEP--> T-2.7, T-3.5, T-5.4
├── T-5.4 WebSocket --DEP--> 无
├── T-5.5 冷却机制 --DEP--> T-5.2
├── T-5.6 配额 --DEP--> T-2.6, T-2.1
├── T-5.7 平台管理 --DEP--> T-2.4, T-4.2, T-5.6
└── T-5.8 心跳历史 --DEP--> T-4.3, T-4.9

Phase 6 (Week 9)
├── T-6.1 单元测试 --DEP--> ALL
├── T-6.2 集成测试 --DEP--> T-6.1
├── T-6.3 E2E 测试 --DEP--> T-6.2
├── T-6.4 性能测试 --DEP--> T-6.2
└── T-6.5 安全扫描 --DEP--> ALL
```

---

## 附录 B：人力排期建议

假设团队为 **2 后端 + 1 前端**：

| 周次 | 后端 A | 后端 B | 前端 |
|------|--------|--------|------|
| W1 | T-1.4, T-1.5 | T-1.1, T-1.3 | T-1.2, T-1.6 |
| W2 | T-2.1, T-2.2, T-2.3 | T-2.5, T-2.6 | T-2.10, T-2.11 |
| W3 | T-2.7, T-2.8, T-2.9 | T-2.4, T-2.12 | T-2.12（联调） |
| W4 | T-3.1, T-3.2 | T-3.3, T-3.4 | T-3.5, T-3.6 |
| W5 | T-4.1, T-4.2, T-4.3 | T-4.4, T-4.5 | T-4.8 |
| W6 | T-4.6, T-4.7 | T-4.7, T-4.9 | T-4.8, T-4.9 |
| W7 | T-5.1, T-5.2, T-5.5 | T-5.3, T-5.4 | T-5.3, T-5.4 |
| W8 | T-5.6, T-5.7 | T-5.8 | T-5.7, T-5.8 |
| W9 | T-6.1, T-6.2, T-6.4 | T-6.2, T-6.5 | T-6.3 |

**总计**：9 周，约 72 人天（后端 48 + 前端 24）

---

**最后更新**: 2026-04-21
**版本**: v1.0
**状态**: 执行中
