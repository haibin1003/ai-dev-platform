# Git 工作流规范

**版本**: v1.0  
**适用范围**: 所有开发人员

---

## 1. 分支策略

### 1.1 分支类型

| 分支 | 用途 | 保护级别 | 命名规范 |
|------|------|---------|---------|
| `main` | 生产环境代码 | 🔒 强制保护 | - |
| `develop` | 开发集成 | 🔒 保护 | - |
| `feature/*` | 新功能开发 | 📝 需 PR | `feature/PRD-{编号}-{描述}` |
| `design/*` | 设计文档 | 📝 需 PR | `design/{主题}-{描述}` |
| `doc/*` | 文档更新 | 📝 需 PR | `doc/{类型}-{描述}` |
| `hotfix/*` | 紧急修复 | 🔒 需审批 | `hotfix/{问题编号}-{描述}` |

### 1.2 分支生命周期

```
main (protected)
  ↑
  │ 定期合并
  │
develop (protected)
  ↑
  │ PR 合并
  │
feature/PRD-001-xxx ──→ 开发完成 ──→ PR ──→ 合并 ──→ 删除
```

---

## 2. Commit 规范

### 2.1 格式

```
<type>(<scope>): <subject>
│      │         │
│      │         └─⫸ 简短描述（不超过50字符）
│      │
│      └─⫸ 影响范围：workflow|task|agent|api|doc
│
└─⫸ 类型：feat|fix|design|doc|refactor|test|chore
```

### 2.2 Type 定义

| Type | 用途 | 示例 |
|------|------|------|
| `feat` | 新功能 | `feat(workflow): 实现 DAG 解析器` |
| `design` | 设计文档 | `design(arch): 添加任务调度架构` |
| `doc` | 文档更新 | `doc(api): 更新工作流 API 文档` |
| `fix` | Bug 修复 | `fix(task): 修复任务状态机转换错误` |
| `refactor` | 重构 | `refactor(agent): 重构适配器工厂` |
| `test` | 测试 | `test(workflow): 添加拓扑排序测试` |
| `chore` | 构建/工具 | `chore(build): 添加 Checkstyle 配置` |

### 2.3 Subject 规范

- 使用祈使句（现在时）
- 首字母小写
- 不以句号结尾
- 不超过 50 个字符

```
✅ feat: 实现工作流引擎核心功能
✅ fix: 修复任务重试计数错误
❌ Feat: 实现了工作流引擎（首字母大写）
❌ fix: 修复了任务重试计数错误。（有句号）
❌ feat: add workflow engine（英文不规范）
```

### 2.4 Body 规范

- 详细说明变更动机
- 解释实现方式
- 与 Subject 之间空一行

```
feat(workflow): 实现 DAG 拓扑排序

使用 Kahn 算法实现有向无环图的拓扑排序，
支持循环检测和错误报告。

- 添加 TopologicalSorter 类
- 实现入度计算和节点排序
- 添加循环检测逻辑
- 完善单元测试

Closes #123
```

---

## 3. Pull Request 规范

### 3.1 PR 标题

```
[<类型>] <简短描述>

示例：
[Feature] 实现工作流 DAG 解析和调度
[Design] 添加 Agent 适配器架构设计
[Fix] 修复任务状态机并发问题
```

### 3.2 PR 描述模板

```markdown
## 变更类型
- [ ] Feature
- [ ] Bug Fix
- [ ] Design
- [ ] Documentation
- [ ] Refactor

## 变更描述
<!-- 详细描述本次变更 -->

## 相关文档
<!-- 链接到相关设计文档 -->
- 需求文档：
- 架构设计：
- API 文档：

## 测试覆盖
- [ ] 单元测试已添加
- [ ] 集成测试已添加
- [ ] 测试覆盖率 ≥ 80%

## 代码质量
- [ ] 通过 Checkstyle 检查
- [ ] 通过 SpotBugs 检查
- [ ] SonarQube 无 Blocker/Critical

## 检查清单
- [ ] 代码自测通过
- [ ] 文档已同步更新
- [ ] 无 Breaking Changes（如有需说明）

##  reviewers
- @reviewer1
- @reviewer2
```

### 3.3 PR 合并条件

- [ ] 至少 1 人 approve
- [ ] 所有 CI 检查通过
- [ ] 代码评审问题已解决
- [ ] 分支已 rebase 到最新 develop

---

## 4. 提交前检查

### 4.1 本地检查脚本

```bash
#!/bin/bash
# pre-commit.sh

echo "=== 运行提交前检查 ==="

# 1. 代码编译
echo "[1/5] 编译代码..."
./mvnw compile -q || exit 1

# 2. 单元测试
echo "[2/5] 运行单元测试..."
./mvnw test -q || exit 1

# 3. 代码风格检查
echo "[3/5] Checkstyle 检查..."
./mvnw checkstyle:check -q || exit 1

# 4. Bug 检查
echo "[4/5] SpotBugs 检查..."
./mvnw spotbugs:check -q || exit 1

# 5. 覆盖率检查
echo "[5/5] 覆盖率检查..."
./mvnw jacoco:check -q || exit 1

echo "=== 所有检查通过 ==="
```

### 4.2 Git Hook 配置

`.git/hooks/pre-commit`:

```bash
#!/bin/bash
# 运行本地检查
./pre-commit.sh
```

---

## 5. 版本发布流程

### 5.1 版本号规范（语义化版本）

```
主版本号.次版本号.修订号
   │      │      │
   │      │      └─  Bug 修复
   │      └─  新功能（向下兼容）
   └─  不兼容的 API 修改

示例：1.2.3
```

### 5.2 发布流程

```
1. 从 develop 创建 release 分支
   git checkout -b release/v1.2.0

2. 版本号更新 + CHANGELOG 更新

3. 测试验证

4. 合并到 main
   git checkout main
   git merge --no-ff release/v1.2.0
   git tag -a v1.2.0 -m "Release v1.2.0"

5. 合并回 develop
   git checkout develop
   git merge --no-ff release/v1.2.0

6. 删除 release 分支
   git branch -d release/v1.2.0
```

---

## 6. 冲突解决规范

### 6.1 禁止行为

- ❌ 强制 push (`git push -f`)
- ❌ 直接修改 main/develop 分支
- ❌ 提交二进制冲突文件

### 6.2 推荐流程

```bash
# 1. 获取最新代码
git fetch origin

# 2. rebase 到最新分支
git rebase origin/develop

# 3. 解决冲突
# ... 手动解决 ...

# 4. 继续 rebase
git rebase --continue

# 5. 推送（如果已 push 过，使用 force-with-lease）
git push --force-with-lease
```

---

## 7. 文档同步

### 7.1 文档变更必须随代码一起提交

```
feat: 实现工作流引擎

- 添加 WorkflowEngine 类
- 实现拓扑排序算法
- 添加单元测试

文档变更：
- docs/design/DESIGN-workflow-engine.md
- docs/api/API-workflow.md

Closes #123
```

### 7.2 纯文档变更使用 doc 类型

```
doc: 更新架构设计文档

- 添加组件交互图
- 更新接口定义
```

---

**最后更新**: 2024年
**版本**: v1.0
