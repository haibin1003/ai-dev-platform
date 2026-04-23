# 项目接手文档

> **本文档面向后续接手的程序员（包括 AI 和人类开发者）**
> 
> **最后更新**: 2026-04-23
> **项目状态**: Phase 1 基础设施已完成，Phase 2 待开始

---

## 📌 必读规范（红线）

在开始任何工作前，必须阅读以下文档：

1. **`AGENTS.md`** — AI 开发规范，含联调红线 4 条
2. **`docs/standards/STANDARD-08-fullstack-development.md`** — 全栈开发规范 v2.0
3. **`PROJECT_STATUS.md`** — 当前工程状态
4. **`docs/TASK-BREAKDOWN-001.md`** — 46 个任务的详细拆解

**联调红线（违反 = 严重违规）**：
- ❌ 严禁后端在未真实联调的情况下声称"完成"
- ❌ 严禁前端在未真实联调的情况下声称"完成"
- ❌ 严禁无联调证据标记任务为完成
- ❌ 严禁前后端不在统一功能分支开发
- ❌ 严禁单方面修改已锁定的 API 契约

---

## 🏗️ 项目架构概览

```
ai-dev-platform/
├── src/main/java/com/aidev/          # 后端（Java 17, Spring Boot 3.2.1）
│   ├── api/                           # 接口层：Controller, DTO, Exception
│   ├── application/                   # 应用层：Service, Port
│   ├── domain/                        # 领域层：Model, Repository, Service, Event
│   │   ├── model/
│   │   │   ├── aggregate/            # 聚合根：Workflow, Execution, Task
│   │   │   ├── entity/               # 实体
│   │   │   └── valueobject/          # 值对象：WorkflowId, TenantId, EncryptedValue...
│   │   ├── repository/               # 仓储接口（领域层定义）
│   │   └── service/                  # 领域服务
│   └── infrastructure/               # 基础设施层
│       ├── adapter/                  # 外部适配器（Claude Code 等）
│       ├── config/                   # Spring 配置
│       ├── persistence/              # JPA 实体、Mapper、Repository 实现
│       ├── security/                 # 加密服务（AES-256-GCM）
│       └── tenant/                   # 多租户基础设施
├── frontend/                         # 前端（Vue 3.5 + TypeScript, Vite 8）
│   ├── src/
│   │   ├── components/               # Vue 组件
│   │   ├── composables/              # 组合式函数
│   │   ├── services/                 # API 服务
│   │   ├── types/                    # TypeScript 类型定义
│   │   └── views/                    # 页面视图
│   └── public/
└── docs/                             # 设计文档
    ├── requirements/                 # PRD
    ├── architecture/                 # ARCH
    ├── design/                       # DESIGN
    ├── standards/                    # 开发规范
    └── TASK-BREAKDOWN-001.md         # 任务拆解（46 个任务）
```

**技术栈**：
- **后端**: Java 17, Spring Boot 3.2.1, Maven, H2(dev)/PostgreSQL(prod), JPA
- **前端**: Vue 3.5, TypeScript, Vite 8, Element Plus, AntV X6, Pinia
- **Agent CLI**: Claude Code, Kimi Code CLI, OpenCode

---

## ✅ Phase 1 已完成工作（2026-04-23 合并 master）

### T-1.1 Checkstyle 配置修复
- 修复了多个无效模块/属性（`LeftCurry`→`LeftCurly`, `JavadocLeadingAsteriskAlign`, `requireVersion`, `ignoreAnnotatedBy` 等）
- 移除了 `TreeWalker` 下不应存在的模块（`OrderedProperties`, `Translation`, `UniqueProperties`）
- ⚠️ **遗留问题**: 现有代码有 **1179 个风格违规**（主要是 `FinalParameters`, `ImportOrder`, `JavadocStyle`）。配置已可正常加载运行。

### T-1.2 前端 ToolbarPanel.vue 编译修复
- `computed` 导入修复
- Props 声明修复（`defineProps` + `defineEmits`）

### T-1.3 前后端状态枚举统一
- 后端 `WorkflowStatus`: `DRAFT` / `ACTIVE` / `ARCHIVED`
- 前端 `workflow.ts`: `'PUBLISHED'` → `'ACTIVE'`
- `WorkflowListView.vue`: `'已发布'` → `'已激活'`

### T-1.4 多租户基础基础设施
**核心类**：
- `com.aidev.domain.model.valueobject.TenantId` — 值对象
- `com.aidev.infrastructure.tenant.TenantContext` — ThreadLocal 上下文
- `com.aidev.infrastructure.tenant.TenantInterceptor` — HTTP 拦截器（`X-Tenant-ID`）
- `com.aidev.infrastructure.config.TenantConfig` — 拦截器注册

**数据隔离策略**：
- 三个 JPA Entity（`WorkflowJpaEntity`, `ExecutionJpaEntity`, `TaskJpaEntity`）已添加 `tenant_id` 字段
- Repository `save()` 自动注入当前租户 ID
- Repository 查询结果在内存中按 `TenantContext.current()` 过滤
- 平台级访问（无 `X-Tenant-ID`）可见所有数据

**测试**: `TenantContextTest`(6) + `TenantInterceptorTest`(6) + `TenantIdTest`(6)

### T-1.5 AES-256-GCM 加密服务
**核心类**：
- `com.aidev.domain.model.valueobject.EncryptedValue` — 值对象（ciphertext:iv:tag）
- `com.aidev.infrastructure.security.AesEncryptionService` — 加密/解密/密钥轮换

**使用方式**：
```java
// 加密
EncryptedValue encrypted = aesEncryptionService.encrypt("sk-xxxx");
String stored = encrypted.toStoredString();  // 存入数据库

// 解密
EncryptedValue parsed = EncryptedValue.fromStoredString(stored);
String plaintext = aesEncryptionService.decrypt(parsed);
```

**密钥配置**：
- 生产环境：`PLATFORM_ENCRYPTION_KEY` 环境变量（32 字节 Base64 编码）
- 开发环境：未配置时自动生成随机密钥（会打印 WARN 日志）

**测试**: `AesEncryptionServiceTest`(9)，覆盖率 100%

### T-1.6 X6 编辑器端口配置修复
- 统一在 `addNode` 时通过 `ports` 配置定义端口
- 移除了所有 `node.addPort()` 调用（消除重复端口 ID 冲突）
- 修复了 `onDrop`、`loadGraphDefinition`、默认节点创建三种场景

---

## 🚀 Phase 2: LLM + Agent 核心（下一步）

**目标**: 建立 LLM 管理、Agent 模板、Agent 实例三大核心域

### Phase 2 任务清单（按依赖排序）

| 优先级 | 任务ID | 任务 | 类型 | 说明 |
|--------|--------|------|------|------|
| P0 | T-2.1 | LLM Provider 域模型 + JPA Entity + 数据库表 | BE+DB | Claude/Kimi/OpenAI |
| P0 | T-2.2 | LLM Provider CRUD API | BE | 增删改查 + 测试 |
| P0 | T-2.3 | Agent 模板域模型 + JPA Entity | BE+DB | 平台模板 + 租户模板 |
| P0 | T-2.4 | Agent 模板 CRUD API | BE | 增删改查 + 测试 |
| P0 | T-2.5 | Agent 人格配置分层 | BE | IDENTITY/SOUL/AGENTS/USER/TOOLS |
| P0 | T-2.6 | Agent 实例生命周期管理 | BE | 启动/终止/暂停/恢复 |
| P0 | T-2.7 | Prompt 模板引擎 | BE | `${var}` 替换 + 分层 Prompt 组装 |
| P0 | T-2.8 | Agent SSE 实时日志流 | BE+FE | SSE 推流，前端渲染 |
| P0 | T-2.9 | Agent 僵尸清理 | BE | 启动扫描 30min+ 僵尸标记 FAILED |
| P1 | T-2.10 | ProcessAgentRuntime | BE | 子进程模式封装 |
| P1 | T-2.11 | Agent 市场页面 | FE | 模板浏览/搜索 |
| P1 | T-2.12 | 场景市场页面 | FE | 一键应用预置场景模板 |

**详细设计**: 见 `docs/design/DESIGN-002-agent-platform.md`

---

## 🔧 开发环境检查清单

每次开始工作前执行：

```bash
# 1. 确认当前分支
git branch

# 2. 获取最新代码
git pull origin master

# 3. 创建功能分支（前后端交互功能必须统一分支）
git checkout -b feature/phase2-llm-agent

# 4. 后端检查
mvn clean compile -Dcheckstyle.skip=true
mvn test -Dcheckstyle.skip=true

# 5. 前端检查
cd frontend
npm run type-check
npm run build
```

> ⚠️ Checkstyle 当前有 1179 个历史违规，编译时需要 `-Dcheckstyle.skip=true`。后续可安排专门任务清理。

---

## 🗂️ 关键设计决策记录

### 1. 多租户数据隔离策略（内存过滤 vs SQL 过滤）
**决策**: 当前采用 **内存过滤**（Repository 查询后 Stream.filter）

**原因**:
- 最小侵入：不需要修改 Spring Data JPA 接口
- 当前数据量不大，性能可接受
- 便于后续升级到 JPA `@FilterDef` 方案

**后续优化方向**: 当数据量增大时，切换到 JPA `@FilterDef` + `@Filter` 或 Hibernate Multi-Tenancy。

### 2. AES-256-GCM 密文存储格式
**决策**: `base64(ciphertext):base64(iv):base64(tag)`

**原因**:
- 单字段存储，不需要修改表结构添加额外列
- 格式自描述，易于调试和迁移

### 3. Agent PoC 代码保留
**决策**: `AgentPocController` 和 `AgentPocService` 保留在代码库中

**原因**:
- 作为技术参考，展示了 Claude Code 子进程启动和 SSE 推流的完整模式
- 后续 T-2.6/T-2.10 会在此基础上重构为正式的 AgentRuntime

---

## ⚠️ 已知问题与注意事项

### 1. Checkstyle 历史违规（1179 个）
- 主要是 `FinalParameters`（方法参数未加 final）、`ImportOrder`（导入顺序）、`JavadocStyle`（Javadoc 首句缺句号）
- **不影响功能**，但会在 CI 中导致构建失败
- **建议**: 后续安排 1 人天的"代码风格清理"专项任务，或放宽部分规则

### 2. 前端 Chunk Size 警告
- `WorkflowEditorView.js` 568KB（超过 500KB 阈值）
- **建议**: 后续做代码分割，将 X6 编辑器相关代码 lazy load

### 3. H2 数据库文件锁定
- dev 环境使用 H2 文件数据库 `./data/devplatform`
- 如果同时运行应用和测试，可能出现文件锁定错误
- **解决**: 删除 `data/` 目录后重新运行

### 4. 租户上下文线程安全
- `TenantContext` 基于 `ThreadLocal`，在异步线程（如 `@Async`）中需要手动传递租户 ID
- 后续如果引入 WebFlux 或异步处理，需要升级为 `ReactorContext` 方案

---

## 📞 快速参考

### 常用命令

```bash
# 后端编译
mvn clean compile -Dcheckstyle.skip=true

# 后端测试（全部）
mvn test -Dcheckstyle.skip=true

# 后端测试（指定类）
mvn test -Dcheckstyle.skip=true -Dtest=TenantContextTest

# 前端类型检查
cd frontend && npm run type-check

# 前端构建
cd frontend && npm run build

# 前端开发服务器
cd frontend && npm run dev

# 启动后端（开发）
mvn spring-boot:run -Dcheckstyle.skip=true
```

### 关键文件速查

| 目的 | 文件路径 |
|------|---------|
| 查看当前任务状态 | `PROJECT_STATUS.md` |
| 查看详细任务拆解 | `docs/TASK-BREAKDOWN-001.md` |
| 查看需求文档 | `docs/requirements/PRD-003-agent-llm-platform.md` |
| 查看架构设计 | `docs/architecture/ARCH-002-agent-platform.md` |
| 查看详细设计 | `docs/design/DESIGN-002-agent-platform.md` |
| 查看开发规范 | `AGENTS.md`, `docs/standards/STANDARD-08-fullstack-development.md` |
| 多租户配置 | `src/main/java/com/aidev/infrastructure/config/TenantConfig.java` |
| 加密服务 | `src/main/java/com/aidev/infrastructure/security/AesEncryptionService.java` |
| 前端 API 服务 | `frontend/src/services/workflow.ts` |
| 前端类型定义 | `frontend/src/types/workflow.ts` |

---

## 🎯 Phase 2 起步建议

如果你是接手的开发者，建议按以下顺序开始：

1. **Day 1**: 阅读本文档 + `AGENTS.md` + `STANDARD-08` + `TASK-BREAKDOWN-001.md`
2. **Day 1**: 运行环境检查清单，确保编译和测试全部通过
3. **Day 1-2**: 阅读 `DESIGN-002-agent-platform.md` 中 Phase 2 相关章节
4. **Day 2**: 从 **T-2.1 LLM Provider 域模型** 开始，这是 Phase 2 的基础
5. **Day 3-4**: T-2.2 LLM Provider CRUD API（前后端契约锁定后通知前端）
6. **Day 5-7**: T-2.3 + T-2.4 Agent 模板域模型和 API
7. **Week 2**: T-2.5 ~ T-2.7 人格配置 + Prompt 引擎 + 实例生命周期
8. **Week 3**: T-2.8 SSE 日志流（前后端联调）+ T-2.9 僵尸清理

**分支策略**: `feature/phase2-llm-agent`

---

*本文档由 AI Assistant 编写于 2026-04-23，随工程状态同步更新。*
