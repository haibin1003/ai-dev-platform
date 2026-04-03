# AI 研发协同平台

> AI-driven Development Workflow Orchestration Platform

## 📋 项目简介

本项目是一个 **AI 驱动的开发工作流编排平台**，支持调度多个 AI Agent（Claude Code、Kimi Code 等）协作完成复杂的软件开发任务。

### 核心特性

- 🔄 **DAG 工作流编排** - 支持复杂依赖关系的任务调度
- 🤖 **多 Agent 协作** - 架构师、开发、测试等角色协同
- 📊 **实时状态追踪** - WebSocket 流式日志推送
- 📦 **产物自动收集** - 代码、文档产物管理
- 🔌 **插件化适配** - 支持多种 AI 代码助手

## 🏗️ 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 17, Spring Boot 3.x, WebFlux |
| 数据库 | H2 (开发), PostgreSQL (生产) |
| 流程引擎 | 自研轻量 DAG 引擎 |
| 前端 | React + TypeScript (计划中) |

## 📁 项目结构

```
ai-dev-platform/
├── AGENTS.md                    # AI 开发规范（必读）
├── docs/                        # 文档目录
│   ├── requirements/           # 需求文档 (PRD)
│   ├── architecture/           # 架构设计 (ARCH)
│   ├── design/                 # 详细设计
│   ├── api/                    # API 文档
│   └── standards/              # 规范文档
├── src/
│   ├── main/java/com/aidev/
│   │   ├── api/               # 接口层 (Controller, DTO)
│   │   ├── application/       # 应用层 (Service, Port)
│   │   ├── domain/            # 领域层 (Entity, Aggregate)
│   │   └── infrastructure/    # 基础设施层
│   └── test/java/             # 测试代码
└── workspace/                 # 工作目录
```

## 🚀 快速开始

### 环境要求

- JDK 17+
- Maven 3.9+
- Claude Code CLI (可选)

### 启动应用

```bash
# 1. 克隆项目
git clone <repository-url>
cd ai-dev-platform

# 2. 编译
./mvnw clean compile

# 3. 运行测试
./mvnw test

# 4. 启动应用
./mvnw spring-boot:run

# 应用将启动在 http://localhost:8080
```

### API 测试

```bash
# 创建工作流
curl -X POST http://localhost:8080/api/v1/workflows \
  -H "Content-Type: application/json" \
  -d '{
    "name": "feature-dev",
    "definition": "..."
  }'

# 执行工作流
curl -X POST http://localhost:8080/api/v1/workflows/{id}/execute \
  -H "Content-Type: application/json" \
  -d '{
    "variables": {
      "project_name": "my-app"
    }
  }'
```

## 📚 开发规范

**⚠️ 重要：所有开发者（包括 AI）必须遵守以下规范**

### 开发流程

```
需求文档 (PRD) → 架构设计 (ARCH) → AI 评审 → 详细设计 → 开发 → 代码评审 → 测试
```

### 必读文档

1. [AGENTS.md](./AGENTS.md) - AI 开发规范（红线规定）
2. [Git 工作流规范](./docs/standards/STANDARD-01-git-workflow.md)
3. [代码风格规范](./docs/standards/STANDARD-02-code-style.md)
4. [DDD 实践规范](./docs/standards/STANDARD-03-ddd-practice.md)
5. [测试规范](./docs/standards/STANDARD-04-testing.md)

### 代码提交前检查

```bash
# 必须全部通过才能提交
./mvnw clean test                    # 单元测试
./mvnw checkstyle:check             # 代码风格
./mvnw spotbugs:check               # Bug 检查
./mvnw jacoco:check                 # 覆盖率检查
```

## 🛠️ 开发指南

### 创建新功能

1. **编写需求文档**
   ```bash
   cp docs/requirements/PRD-TEMPLATE.md docs/requirements/PRD-XXX-feature-name.md
   ```

2. **创建设计分支**
   ```bash
   git checkout -b design/PRD-XXX-feature-name
   ```

3. **AI 评审通过后，开始开发**
   ```bash
   git checkout -b feature/PRD-XXX-feature-name
   ```

### DDD 分层示例

```java
// Domain 层 - 核心业务逻辑
public class Workflow {
    private WorkflowId id;
    private List<Node> nodes;
    
    public void addNode(Node node) {
        validateNode(node);
        nodes.add(node);
    }
}

// Application 层 - 用例编排
@Service
public class WorkflowAppService {
    public WorkflowExecution startWorkflow(String id) {
        // 编排领域对象
    }
}

// API 层 - 接口适配
@RestController
public class WorkflowController {
    @PostMapping("/api/v1/workflows")
    public ResponseEntity<WorkflowDTO> create(@RequestBody CreateRequest req) {
        // 参数校验、转换
    }
}
```

## 📊 测试覆盖

| 模块 | 目标覆盖率 |
|------|-----------|
| Domain | ≥ 95% |
| Application | ≥ 85% |
| 整体 | ≥ 80% |

```bash
# 生成覆盖率报告
./mvnw jacoco:report

# 报告位置: target/site/jacoco/index.html
```

## 🗺️ 路线图

| 阶段 | 功能 | 时间 |
|------|------|------|
| MVP | 工作流引擎 + Claude Code 适配 | 2 周 |
| V1.0 | Web UI + 多 Agent 支持 | 4 周 |
| V1.5 | 条件分支 + 并行执行 | 6 周 |
| V2.0 | Kimi Code + 沙箱隔离 | 8 周 |

## 🤝 贡献指南

### 提交规范

```
<type>(<scope>): <subject>

feat(workflow): 实现 DAG 拓扑排序

design(arch): 添加任务调度架构
doc(api): 更新工作流 API 文档
fix(task): 修复任务状态机转换错误
```

### 检查清单

- [ ] 代码遵循 DDD 架构
- [ ] 单元测试覆盖率 ≥ 80%
- [ ] 通过 Checkstyle 检查
- [ ] 通过 SpotBugs 检查
- [ ] 文档已同步更新

## 📄 许可证

[MIT License](./LICENSE)

## 📞 联系方式

- Issues: [GitHub Issues](https://github.com/xxx/ai-dev-platform/issues)
- Email: dev@aidev.com

---

> **提示**: 本项目采用严格的开发规范，请在开发前仔细阅读 `AGENTS.md` 和 `docs/standards/` 下的规范文档。
