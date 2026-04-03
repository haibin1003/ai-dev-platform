# STANDARD-06: E2E 功能验证规范

> **版本**: v1.0
> **生效日期**: 2026-04-03
> **强制级别**: 🔴 前后端联调必须遵守

---

## 1. 目的

解决前后端开发过程中常见的对接问题：

- 后端 API 完成后，前端无法正常调用
- 字段命名、格式不一致导致的数据解析错误
- 边界条件、异常处理未考虑前端需求
- 前后端对 API 契约理解不一致

---

## 2. 验证时机

### 2.1 必须验证的场景

| 阶段 | 触发条件 | 验证内容 |
|------|---------|---------|
| API 开发完成 | 后端 PR 提交前 | API 功能可用 |
| 前端联调完成 | 提 PR 前 | 前后端对接正常 |
| 功能验收 | QA 测试前 | 功能完整可用 |

### 2.2 验证责任人

```
API 开发完成 → 后端开发者负责验证
前端联调完成 → 前端开发者负责验证
功能验收阶段 → 测试工程师负责验证
```

---

## 3. 验证方式

### 3.1 使用 playwright-cli 进行 API 验证

**前置条件**：
```bash
# 安装 playwright-cli
npm install -g playwright-cli

# 或使用 npx
npx playwright-cli
```

**验证命令**：

```bash
# 1. 启动应用（后台运行）
./mvnw spring-boot:run

# 2. 使用 playwright-cli 验证 API
playwright-cli api verify --url http://localhost:8080/api/v1/workflows \
  --method POST \
  --body '{"name":"test","definition":{}}' \
  --expected-status 201

# 3. 验证响应结构
playwright-cli api verify --url http://localhost:8080/api/v1/workflows/1 \
  --expected-schema workflow-schema.json
```

### 3.2 常用验证命令示例

```bash
# GET 请求验证
playwright-cli api verify \
  --url http://localhost:8080/api/v1/workflows \
  --method GET \
  --expected-status 200

# POST 请求验证（创建资源）
playwright-cli api verify \
  --url http://localhost:8080/api/v1/workflows \
  --method POST \
  --body '{"name":"test","definition":{"nodes":[]}}' \
  --expected-status 201

# 验证错误响应
playwright-cli api verify \
  --url http://localhost:8080/api/v1/workflows \
  --method POST \
  --body '{"name":""}' \
  --expected-status 400 \
  --expected-body Contains '"error"'

# 验证 WebSocket 连接
playwright-cli ws verify \
  --url ws://localhost:8080/ws/logs \
  --timeout 5000
```

### 3.3 验证检查清单

每个 API 必须通过以下检查：

- [ ] **功能正常**：API 能正确处理请求并返回预期结果
- [ ] **状态码正确**：2xx/4xx/5xx 符合业务逻辑
- [ ] **响应结构**：字段名称、类型与前端约定一致
- [ ] **错误处理**：异常情况返回清晰的错误信息
- [ ] **参数校验**：非法输入能被正确拒绝
- [ ] **边界条件**：空值、极值等边界情况处理正确

---

## 4. 前端联调验证流程

### 4.1 后端开发者完成 API 后

```bash
# 1. 启动后端服务
./mvnw spring-boot:run

# 2. 使用 playwright-cli 验证 API 可用
playwright-cli api verify --url http://localhost:8080/api/v1/workflows --method GET

# 3. 生成 API 契约文档（可选）
playwright-cli api contract --url http://localhost:8080/api/v1 --output api-contract.json

# 4. 通知前端开发者 API 已就绪
```

### 4.2 前端开发者联调完成后

```bash
# 1. 启动前端应用
npm run dev

# 2. 启动后端服务
./mvnw spring-boot:run

# 3. 使用 playwright 进行端到端验证
npx playwright-cli e2e verify \
  --url http://localhost:3000 \
  --story "创建工作流" \
  --steps "点击新建 -> 填写名称 -> 点击保存 -> 验证出现在列表"
```

### 4.3 验证报告

每次验证后，记录验证结果：

```markdown
## API 验证报告 - F-01 工作流定义管理

### 验证信息
- 验证日期：2026-04-03
- 验证人：后端开发者
- 验证环境：http://localhost:8080

### 验证结果

| API | 方法 | 状态 | 响应时间 | 结果 |
|-----|------|------|---------|------|
| /api/v1/workflows | GET | 200 | 45ms | ✅ 通过 |
| /api/v1/workflows | POST | 201 | 32ms | ✅ 通过 |
| /api/v1/workflows/{id} | GET | 200 | 28ms | ✅ 通过 |
| /api/v1/workflows/{id} | DELETE | 204 | 20ms | ✅ 通过 |

### 问题记录
| 问题 | 严重度 | 状态 | 修复人 |
|------|--------|------|--------|
| 无 | - | - | - |

### 确认签字
- 后端开发者：✅
- 前端开发者：⏳ 待联调
```

---

## 5. E2E 测试规范

### 5.1 测试用例命名

```markdown
# E2E 测试文件命名
e2e/
├── workflows/
│   ├── create-workflow.spec.ts      # 创建工作流
│   ├── execute-workflow.spec.ts    # 执行工作流
│   └── cancel-workflow.spec.ts      # 取消工作流
```

### 5.2 测试用例结构

```typescript
import { test, expect } from '@playwright/test';

test.describe('工作流管理', () => {
  test.beforeEach(async ({ page }) => {
    // 前置条件
    await page.goto('/workflows');
  });

  test('应该能创建工作流', async ({ page }) => {
    // Given
    await page.click('button:has-text("新建")');

    // When
    await page.fill('input[name="name"]', '测试工作流');
    await page.click('button:has-text("保存")');

    // Then
    await expect(page.locator('text=测试工作流')).toBeVisible();
  });

  test('创建时名称不能为空', async ({ page }) => {
    // Given
    await page.click('button:has-text("新建")');

    // When
    await page.click('button:has-text("保存")');

    // Then
    await expect(page.locator('text=名称不能为空')).toBeVisible();
  });
});
```

### 5.3 CI/CD 集成

```yaml
# .github/workflows/e2e.yml
name: E2E Tests

on:
  pull_request:
    paths:
      - 'src/main/**'
      - 'src/test/e2e/**'

jobs:
  e2e:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          java-version: '17'
      - name: Setup Node
        uses: actions/setup-node@v4
        with:
          node-version: '20'
      - name: Build Backend
        run: ./mvnw clean package -DskipTests
      - name: Start Backend
        run: ./mvnw spring-boot:run &
      - name: Run E2E Tests
        run: npx playwright test
```

---

## 6. 违规处理

| 违规级别 | 行为 | 处理方式 |
|---------|------|---------|
| 🔴 严重 | 后端 API 未验证直接提交 PR | PR 拒绝，必须完成 API 验证 |
| 🔴 严重 | 前端联调未验证直接提交 PR | PR 拒绝，必须完成 E2E 验证 |
| 🟠 重要 | 验证发现问题未记录 | 补充问题记录后才能继续 |
| 🟡 一般 | 验证报告不完整 | 建议补充完整 |

---

## 7. 相关文档

| 文档 | 关系 |
|------|------|
| STANDARD-04-testing.md | 测试规范基础 |
| STANDARD-05-project-status.md | 验证完成后更新工程状态 |
| AGENTS.md | E2E 验证是开发的必须环节 |

---

## 8. 工具推荐

| 工具 | 用途 | 安装 |
|------|------|------|
| playwright-cli | API 快速验证 | `npm i -g playwright-cli` |
| @playwright/test | E2E 测试框架 | `npm i -D @playwright/test` |
| httpie | API 调试 | `brew install httpie` |

---

**最后更新**: 2026-04-03
**版本**: v1.0
**状态**: 生效
