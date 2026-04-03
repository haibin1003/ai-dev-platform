# AI 研发协同平台 - Agent 开发规范

> **警告：所有 AI 必须严格遵守以下规范，违反规范将导致代码被拒绝。**

## 🚫 绝对禁止（Red Lines）

### 1. 文档红线
- ❌ **严禁跳过 AI 评审直接进入开发阶段**
- ❌ **严禁在没有设计文档的情况下编写代码**
- ❌ **严禁需求变更不更新文档直接修改代码**
- ❌ **严禁文档和代码不一致**

### 2. 代码红线
- ❌ **严禁下层调用上层**（如 Infrastructure 调用 Application）
- ❌ **严禁领域层依赖 Spring 注解**（如 `@Autowired`, `@Service`）
- ❌ **严禁在 main 分支直接提交代码**
- ❌ **严禁提交未通过单元测试的代码**
- ❌ **严禁 SonarQube 存在 Blocker/Critical 问题**

### 3. Git 红线
- ❌ **严禁混合多个无关变更到一个 commit**
- ❌ **严禁 commit message 含糊不清**
- ❌ **严禁提交包含密码、密钥的代码**

---

## ✅ 强制流程（Mandatory Process）

### 阶段 1：需求分析（PRD）
```
责任人：需求分析师 + AI
产出：docs/requirements/PRD-{编号}-{标题}.md
检查点：
- [ ] 用户故事清晰
- [ ] 验收标准可量化
- [ ] 风险评估完成
```

### 阶段 2：架构设计（ARCH）
```
责任人：架构师 + AI
产出：docs/architecture/ARCH-{编号}-{标题}.md
检查点：
- [ ] DDD 分层正确
- [ ] 接口定义完整
- [ ] 非功能需求考虑
```

### 阶段 3：AI 评审（REVIEW）
```
责任人：AI + 资深架构师
产出：docs/architecture/ARCH-{编号}-review.md
检查点：
- [ ] 架构合理性检查
- [ ] 风险识别
- [ ] 决策记录
```

### 阶段 4：详细设计（DESIGN）
```
责任人：开发负责人 + AI
产出：docs/design/DESIGN-{模块}.md
检查点：
- [ ] 类图完整
- [ ] 时序图清晰
- [ ] 数据库设计
```

### 阶段 5：开发实现（DEV）
```
责任人：开发人员 + AI
产出：src/main/java/... + src/test/java/...
检查点：
- [ ] TDD 开发
- [ ] 单元测试 > 80%
- [ ] 代码风格检查
```

### 阶段 6：代码评审（CR）
```
责任人：Reviewer + AI
产出：PR Comments
检查点：
- [ ] 设计实现一致性
- [ ] 代码质量检查
- [ ] 安全漏洞扫描
```

### 阶段 7：测试验收（QA）
```
责任人：测试人员
产出：测试报告
检查点：
- [ ] 功能测试通过
- [ ] 集成测试通过
- [ ] 性能测试达标
```

---

## 📝 规范索引

### 🔴 必读基础规范（全员）

| 规范 | 文件 | 说明 |
|------|------|------|
| AI 开发规范 | `AGENTS.md` | 红线规定、强制流程 |
| 工程状态 | `PROJECT_STATUS.md` | 项目进展、待开发功能 |
| 工程状态维护 | `docs/standards/STANDARD-05-project-status.md` | 状态更新要求 |

### 🟢 后端开发规范

| 规范 | 文件 | 强制要求 |
|------|------|---------|
| Git 工作流 | `docs/standards/STANDARD-01-git-workflow.md` | 分支命名、Commit 格式 |
| 代码风格 | `docs/standards/STANDARD-02-code-style.md` | Checkstyle 检查通过 |
| DDD 实践 | `docs/standards/STANDARD-03-ddd-practice.md` | 分层架构、依赖规则 |
| 测试规范 | `docs/standards/STANDARD-04-testing.md` | 单元测试 ≥ 80% |
| E2E 验证 | `docs/standards/STANDARD-06-e2e-verification.md` | API 必须验证可用 |

**后端开发强制流程**：
```
PRD → ARCH → AI 评审 → DESIGN → DEV → CR → QA
```

**后端提交前检查**：
```bash
./mvnw clean test           # 测试通过
./mvnw checkstyle:check     # 代码风格
./mvnw spotbugs:check       # Bug 检查
./mvnw jacoco:check          # 覆盖率 ≥ 80%
```

### 🟢 前端开发规范

| 规范 | 文件 | 强制要求 |
|------|------|---------|
| Git 工作流 | `docs/standards/STANDARD-01-git-workflow.md` | 分支命名、Commit 格式 |
| 代码风格 | `docs/standards/STANDARD-02-code-style.md` | ESLint/Prettier 检查 |
| 测试规范 | `docs/standards/STANDARD-04-testing.md` | 单元测试覆盖 |
| E2E 验证 | `docs/standards/STANDARD-06-e2e-verification.md` | **前端联调必须验证** |

**前端开发强制流程**：
```
PRD → ARCH → DESIGN → DEV → E2E 验证 → CR → QA
```

**前端提交前检查**：
```bash
npm run lint                  # ESLint 检查
npm run test                  # 单元测试
npm run build                 # 构建成功
npx playwright test           # E2E 测试通过
```

### 🟢 测试规范

| 规范 | 文件 | 说明 |
|------|------|------|
| 测试规范 | `docs/standards/STANDARD-04-testing.md` | 测试金字塔、覆盖率要求 |
| E2E 验证 | `docs/standards/STANDARD-06-e2e-verification.md` | 前后端联调验证流程 |

**测试覆盖率要求**：

| 层级 | 覆盖率目标 |
|------|-----------|
| Domain | ≥ 95% |
| Application | ≥ 85% |
| 整体 | ≥ 80% |

---

### 📋 文档类型与位置

| 文档类型 | 文件命名 | 位置 | 负责人 |
|---------|---------|------|--------|
| 需求文档 | `PRD-{编号}-{标题}.md` | `docs/requirements/` | 需求分析师 |
| 架构设计 | `ARCH-{编号}-{标题}.md` | `docs/architecture/` | 架构师 |
| 设计评审 | `ARCH-{编号}-review.md` | `docs/architecture/` | AI + 架构师 |
| 详细设计 | `DESIGN-{模块}.md` | `docs/design/` | 开发负责人 |
| API 文档 | `API-{模块}.md` | `docs/api/` | 开发人员 |
| 部署文档 | `DEPLOY-{环境}.md` | `docs/deployment/` | DevOps |
| 操作手册 | `OPS-{主题}.md` | `docs/operations/` | 运维 |
| 项目规范 | `STANDARD-*.md` | `docs/standards/` | Tech Lead |
| 工程状态 | `PROJECT_STATUS.md` | 根目录 | 全员 |

### AI 评审模板

```markdown
# AI 评审报告

## 评审对象
- 文档：[链接]
- 评审日期：YYYY-MM-DD
- 评审人：AI + {人工确认者}

## 检查项

### 架构合理性
- [ ] DDD 分层正确
- [ ] 领域模型充血
- [ ] 依赖方向正确
- [ ] 无循环依赖

### 代码质量
- [ ] 符合 SOLID 原则
- [ ] 无代码异味
- [ ] 异常处理完善
- [ ] 并发安全考虑

### 安全性
- [ ] 输入校验
- [ ] 无注入风险
- [ ] 权限控制

### 性能
- [ ] 复杂度合理
- [ ] 无性能瓶颈
- [ ] 可扩展性

## 问题列表
| 级别 | 问题 | 建议 | 状态 |
|-----|------|------|------|
| 🔴 | xxx | xxx | 待修复 |

## 评审结论
- [ ] 通过
- [ ] 有条件通过（需修复问题）
- [ ] 不通过（需重新设计）

## 确认签字
- AI 评审员：
- 人工确认：
```

---

## 🔧 代码规范

### 1. DDD 分层架构

```
src/main/java/com/aidev/
├── api/                    # 接口层
│   ├── controller/         # REST Controller
│   ├── dto/               # 数据传输对象
│   └── exception/         # 全局异常处理
│
├── application/           # 应用层
│   ├── service/           # Application Service
│   ├── event/             # 事件处理
│   └── port/              # 端口（输入/输出接口）
│
├── domain/                # 领域层【核心】
│   ├── model/             # 领域模型
│   │   ├── entity/        # 实体
│   │   ├── valueobject/   # 值对象
│   │   └── aggregate/     # 聚合根
│   ├── service/           # 领域服务
│   ├── event/             # 领域事件
│   └── repository/        # 仓储接口
│
└── infrastructure/        # 基础设施层
    ├── config/            # 配置
    ├── persistence/       # 数据持久化实现
    ├── adapter/           # 外部适配器
    │   ├── claudecode/    # Claude Code
    │   └── kimicode/      # Kimi Code
    └── external/          # 外部服务客户端
```

### 2. 依赖规则

```java
// ✅ 正确：Application 调用 Domain
@Service
public class WorkflowAppService {
    private final WorkflowRepository repository; // Domain 接口
    private final DomainEventPublisher publisher; // Domain 事件
}

// ❌ 错误：Domain 依赖 Application 或 Infrastructure
@Entity  // 领域层禁止使用 Spring 注解
public class Workflow {
    @Autowired // ❌ 严禁！领域层不能依赖 Spring
    private WorkflowRepository repository;
}
```

### 3. 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 类名 | 大驼峰，名词 | `WorkflowEngine`, `TaskExecutor` |
| 接口名 | 大驼峰，形容词 | `CodeAdapter`, `TaskRepository` |
| 方法名 | 小驼峰，动词开头 | `executeTask()`, `findById()` |
| 变量名 | 小驼峰 | `taskId`, `executionContext` |
| 常量名 | 全大写，下划线 | `MAX_RETRY_COUNT`, `DEFAULT_TIMEOUT` |
| 包名 | 全小写 | `com.aidev.workflow.domain` |

### 4. 代码风格

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
 * @author 作者
 * @since 1.0
 * @see WorkflowScheduler
 */
public class WorkflowEngine {
    
    private static final int DEFAULT_POOL_SIZE = 10;
    
    private final WorkflowRepository repository;
    private final TaskExecutor executor;
    
    // 构造函数注入
    public WorkflowEngine(WorkflowRepository repository, TaskExecutor executor) {
        this.repository = Objects.requireNonNull(repository);
        this.executor = Objects.requireNonNull(executor);
    }
    
    /**
     * 启动工作流执行。
     *
     * @param workflowId 工作流 ID，不能为空
     * @param inputs 输入参数
     * @return 执行实例
     * @throws WorkflowException 当工作流不存在或启动失败时抛出
     */
    public WorkflowExecution startWorkflow(String workflowId, Map<String, String> inputs) 
            throws WorkflowException {
        // 参数校验
        Assert.notNull(workflowId, "workflowId must not be null");
        
        // 实现...
    }
}
```

---

## 🧪 测试规范

### 1. 测试金字塔

```
        /\
       /  \     E2E 测试（5%）
      /____\
     /      \
    /________\   集成测试（15%）
   /          \
  /____________\
 /              \ 单元测试（80%）
/________________\
```

### 2. 单元测试要求

```java
@DisplayName("工作流引擎测试")
class WorkflowEngineTest {
    
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
        
        @Test
        @DisplayName("当存在循环依赖时应抛出异常")
        void shouldThrowExceptionWhenCycleExists() {
            // Given
            DAG cyclicDag = createCyclicDAG();
            
            // When & Then
            assertThatThrownBy(() -> engine.topologicalSort(cyclicDag))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("Cycle detected");
        }
    }
}
```

### 3. 测试覆盖率要求

| 模块 | 覆盖率 | 说明 |
|------|--------|------|
| Domain | ≥ 95% | 核心业务逻辑必须全覆盖 |
| Application | ≥ 85% | 业务流程测试 |
| Infrastructure | ≥ 60% | 复杂逻辑测试 |
| 整体 | ≥ 80% | 项目平均 |

---

## 🔄 Git 工作流

### 分支命名

```
main                    # 生产分支
├── develop             # 开发分支
├── feature/PRD-001-xxx # 功能分支
├── design/ARCH-001-xxx # 设计分支
├── doc/api-spec        # 文档分支
└── hotfix/bug-001      # 热修复分支
```

### Commit 规范

```
<type>(<scope>): <subject>

<body>

<footer>

示例：
feat(workflow): 实现 DAG 拓扑排序算法

- 使用 Kahn 算法实现拓扑排序
- 支持循环检测
- 添加单元测试

Closes #123
```

**Type 定义**：
- `feat`: 新功能
- `design`: 设计文档
- `doc`: 文档更新
- `fix`: Bug 修复
- `refactor`: 重构
- `test`: 测试
- `chore`: 构建/工具

---

## 🔍 质量门禁

### 提交前检查（Pre-commit）

```bash
# 必须全部通过才能提交
./mvnw clean test                    # 所有测试通过
./mvnw spotbugs:check               # SpotBugs 检查
./mvnw checkstyle:check             # 代码风格检查
```

### PR 合并检查（Pre-merge）

- [ ] 代码评审通过（至少 1 人）
- [ ] 单元测试覆盖率 ≥ 80%
- [ ] SonarQube 0 Blocker, 0 Critical
- [ ] 集成测试通过
- [ ] 文档已同步更新

---

## 🤖 AI 协作指南

### 代码生成 Prompt 模板

```markdown
# 代码生成请求

## 上下文
- 相关文档：[链接]
- 所在模块：{module}
- 依赖类：{dependencies}

## 需求描述
{清晰的需求描述}

## 约束条件
- [ ] 必须遵循 DDD 分层架构
- [ ] 必须使用构造函数注入
- [ ] 必须包含单元测试
- [ ] 异常必须使用自定义异常类
- [ ] 方法复杂度不超过 10
- [ ] 必须包含 JavaDoc 注释

## 输出要求
1. Java 代码（完整实现）
2. 单元测试代码（JUnit 5）
3. 关键设计决策说明
```

### 设计评审 Prompt 模板

```markdown
# 设计评审请求

## 评审对象
- 文档：[链接]

## 评审重点
- [ ] DDD 架构正确性
- [ ] 领域模型设计
- [ ] 接口设计合理性
- [ ] 性能考量
- [ ] 安全风险

## 输出格式
1. 检查项清单（通过/不通过）
2. 问题列表（级别、描述、建议）
3. 总体评估（通过/有条件通过/不通过）
```

---

## 📊 交付检查清单

### 开发完成标准（Definition of Done）

- [ ] 代码实现完成并通过自测
- [ ] 单元测试覆盖率达标（>80%）
- [ ] 代码评审通过（至少 1 人）
- [ ] 静态代码检查无 Blocker/Critical 问题
- [ ] 集成测试通过
- [ ] 文档已更新（设计文档、API 文档）
- [ ] CHANGELOG 已更新

### 发布检查清单

- [ ] 所有自动化测试通过
- [ ] 性能测试达标
- [ ] 安全扫描通过
- [ ] 部署文档已验证
- [ ] 回滚方案已准备
- [ ] 监控告警已配置

---

## ⚠️ 违规处理

| 违规级别 | 处理方式 |
|---------|---------|
| 🔴 严重（红线） | 代码拒绝，必须重新设计/开发 |
| 🟠 重要 | 必须修复后才能合并 |
| 🟡 一般 | 建议在下次迭代优化 |

---

**最后更新**: 2024年
**版本**: v1.0
**生效日期**: 立即生效

> **记住：质量是设计出来的，不是测试出来的。前期多花 1 小时设计，后期少花 10 小时修复。**
