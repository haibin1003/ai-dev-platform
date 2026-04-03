# 项目配置记录

> **重要：所有 AI 开发必须基于本仓库进行**

---

## 📍 仓库信息

| 项目 | 配置 |
|------|------|
| **仓库地址** | `git@github.com:haibin1003/ai-dev-platform.git` |
| **本地路径** | `C:\Users\51554\kimicode\ai-dev-platform` |
| **主分支** | `master` |
| **开发分支** | `develop`（待创建） |
| **技术栈** | Java 17 + Spring Boot 3.x |

---

## 🔧 开发环境配置

### 本地环境
```bash
# Java 版本
java -version  # OpenJDK 17

# Maven 版本
mvn -version   # 3.9+

# 工作目录
C:\Users\51554\kimicode\ai-dev-platform
```

### 代码质量工具
| 工具 | 配置文件 | 检查命令 |
|------|---------|---------|
| Checkstyle | `checkstyle.xml` | `./mvnw checkstyle:check` |
| SpotBugs | 内置 | `./mvnw spotbugs:check` |
| JaCoCo | `pom.xml` | `./mvnw jacoco:check` |

---

## 📋 开发流程（强制）

```
需求分析 (PRD) 
    ↓
架构设计 (ARCH)
    ↓
AI 评审 (REVIEW) ← 必须由 AI 审查设计文档
    ↓
详细设计 (DESIGN)
    ↓
开发实现 (DEV) ← TDD 模式，单元测试 > 80%
    ↓
代码评审 (CR)
    ↓
测试验收 (QA)
    ↓
合并到主分支
```

---

## 🌿 分支管理

### 分支命名规范
```
主分支: master, develop
功能分支: feature/PRD-{编号}-{描述}
设计分支: design/{主题}-{描述}
文档分支: doc/{类型}-{描述}
修复分支: hotfix/{问题编号}-{描述}
```

### 提交信息格式
```
<type>(<scope>): <subject>

feat(workflow): 实现 DAG 拓扑排序
design(arch): 添加任务调度架构
doc(api): 更新工作流 API 文档
fix(task): 修复任务状态机转换错误
```

---

## 📚 关键文档索引

| 文档 | 路径 | 用途 |
|------|------|------|
| **AI 开发规范** | `AGENTS.md` | 🔴 开发红线规定 |
| **Git 规范** | `docs/standards/STANDARD-01-git-workflow.md` | 分支、Commit 规范 |
| **代码规范** | `docs/standards/STANDARD-02-code-style.md` | Java 编码规范 |
| **DDD 规范** | `docs/standards/STANDARD-03-ddd-practice.md` | 架构分层规范 |
| **测试规范** | `docs/standards/STANDARD-04-testing.md` | 测试要求 |
| **项目说明** | `README.md` | 项目介绍 |

---

## 🚀 快速开始命令

```bash
# 1. 确保在正确目录
cd C:\Users\51554\kimicode\ai-dev-platform

# 2. 获取最新代码
git pull origin master

# 3. 创建功能分支
git checkout -b feature/PRD-XXX-xxx

# 4. 开发前检查
./mvnw clean compile

# 5. 提交前检查（必须全部通过）
./mvnw clean test
./mvnw checkstyle:check
./mvnw spotbugs:check
./mvnw jacoco:check

# 6. 提交代码
git add .
git commit -m "feat(xxx): xxx"
git push -u origin feature/PRD-XXX-xxx
```

---

## ⚠️ 特别提醒

### 每次开发前必须
1. [ ] 阅读 `AGENTS.md` 规范
2. [ ] 确认需求文档已存在（`docs/requirements/PRD-*.md`）
3. [ ] 确认设计文档已通过 AI 评审
4. [ ] 基于最新 `master` 创建分支

### 代码提交前必须
1. [ ] 单元测试覆盖率 ≥ 80%
2. [ ] 通过 Checkstyle 检查
3. [ ] 通过 SpotBugs 检查
4. [ ] 无 Blocker/Critical 问题

---

## 📝 变更记录

| 日期 | 变更内容 | 操作人 |
|------|---------|--------|
| 2026-04-03 | 初始化工程并提交到 GitHub | AI Assistant |

---

**记录时间**: 2026-04-03  
**版本**: v1.0  
**状态**: 活跃开发中
