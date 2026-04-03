# STANDARD-07: API 设计规范

> **版本**: v1.0
> **生效日期**: 2026-04-03
> **强制级别**: 🔴 所有 API 必须遵守

---

## 1. 目的

统一 API 设计规范，确保前后端接口契约清晰、可维护，减少联调成本。

---

## 2. RESTful 规范

### 2.1 资源命名

| 规范 | 正确示例 | 错误示例 |
|------|---------|---------|
| 使用名词复数 | `/api/v1/workflows` | `/api/v1/workflow`, `/api/v1/getWorkflow` |
| 使用小写字母 | `/api/v1/task-groups` | `/api/v1/taskGroups` |
| 使用连字符分隔 | `/api/v1/task-executions` | `/api/v1/task_executions` |
| 避免嵌套过深 | `/api/v1/workflows/{id}/tasks` | `/api/v1/workflows/{id}/tasks/{id}/steps/{id}` |

### 2.2 HTTP 方法使用

| 方法 | 用途 | 示例 |
|------|------|------|
| GET | 查询资源 | `GET /api/v1/workflows` 获取工作流列表 |
| POST | 创建资源 | `POST /api/v1/workflows` 创建工作流 |
| PUT | 完整更新资源 | `PUT /api/v1/workflows/{id}` 更新工作流 |
| PATCH | 部分更新资源 | `PATCH /api/v1/workflows/{id}` 更新部分字段 |
| DELETE | 删除资源 | `DELETE /api/v1/workflows/{id}` 删除工作流 |

### 2.3 URL 设计原则

```
# ✅ 正确
GET    /api/v1/workflows                    # 列出所有工作流
GET    /api/v1/workflows/{id}               # 获取单个工作流
POST   /api/v1/workflows                    # 创建工作流
DELETE /api/v1/workflows/{id}               # 删除工作流
POST   /api/v1/workflows/{id}/execute       # 执行工作流
GET    /api/v1/workflows/{id}/executions    # 获取工作流的执行历史

# ❌ 错误
POST   /api/v1/createWorkflow               # 使用动词
GET    /api/v1/getWorkflowById?id=xxx       # 使用查询参数代替路径参数
POST   /api/v1/workflow/{id}/start          # 使用动词替代标准方法
```

---

## 3. 请求规范

### 3.1 请求头

| 请求头 | 必须 | 说明 |
|--------|------|------|
| `Content-Type` | ✅ | `application/json; charset=utf-8` |
| `Accept` | ✅ | `application/json` |
| `Authorization` | △ | Bearer Token，敏感接口必须 |
| `X-Request-Id` | △ | 请求追踪 ID，建议添加 |
| `X-Correlation-Id` | △ | 关联追踪 ID |

### 3.2 请求体格式

```json
// ✅ 正确：简洁的 JSON
{
  "name": "用户认证工作流",
  "definition": {
    "nodes": [...],
    "edges": [...]
  },
  "variables": {
    "timeout": 300,
    "retryCount": 3
  }
}

// ❌ 错误：冗余包装
{
  "data": {
    "workflowName": "用户认证工作流",
    "workflowDefinition": {...},
    "workflowVariables": {...}
  },
  "meta": {
    "requestId": "xxx"
  }
}
```

### 3.3 字段命名

```json
// ✅ 正确：使用小驼峰（camelCase）
{
  "workflowId": "wf-001",
  "createdAt": "2026-04-03T10:00:00Z",
  "nodeCount": 5,
  "isEnabled": true
}

// ❌ 错误：蛇形命名（snake_case）用于 JSON
{
  "workflow_id": "wf-001",
  "created_at": "2026-04-03T10:00:00Z"
}

// ❌ 错误：全大写
{
  "WORKFLOW_ID": "wf-001"
}
```

---

## 4. 响应规范

### 4.1 成功响应

```json
// 单个资源 - 200 OK
{
  "id": "wf-001",
  "name": "用户认证工作流",
  "status": "ACTIVE",
  "createdAt": "2026-04-03T10:00:00Z",
  "updatedAt": "2026-04-03T10:00:00Z"
}

// 资源列表 - 200 OK
{
  "items": [
    { "id": "wf-001", "name": "工作流1" },
    { "id": "wf-002", "name": "工作流2" }
  ],
  "total": 100,
  "page": 1,
  "pageSize": 20
}

// 创建成功 - 201 Created
{
  "id": "wf-003",
  "name": "新工作流",
  "status": "DRAFT",
  "createdAt": "2026-04-03T10:00:00Z"
}

// 删除成功 - 204 No Content
// (无响应体)

// 异步任务创建 - 202 Accepted
{
  "executionId": "exec-001",
  "status": "PENDING",
  "_links": {
    "status": "/api/v1/executions/exec-001",
    "cancel": "/api/v1/executions/exec-001/cancel"
  }
}
```

### 4.2 分页响应

```json
// 分页列表格式
{
  "items": [...],
  "page": 1,
  "pageSize": 20,
  "total": 100,
  "totalPages": 5,
  "hasNext": true,
  "hasPrevious": false
}

// 分页参数
GET /api/v1/workflows?page=0&size=20&sort=createdAt,desc
```

### 4.3 错误响应格式

```json
// ✅ 统一错误格式
{
  "error": {
    "code": "WF-001",
    "message": "工作流不存在",
    "details": [
      {
        "field": "workflowId",
        "message": "工作流 ID 不能为空"
      }
    ],
    "traceId": "abc123",
    "timestamp": "2026-04-03T10:00:00Z"
  }
}

// ❌ 错误：不一致的错误格式
{
  "status": 404,
  "message": "Not found",
  "errCode": "WORKFLOW_NOT_FOUND"
}
```

### 4.4 错误码定义

| 错误码 | HTTP 状态码 | 说明 |
|--------|-------------|------|
| `WF-xxx` | 4xx | 工作流相关错误 |
| `TASK-xxx` | 4xx | 任务相关错误 |
| `AUTH-xxx` | 401/403 | 认证/授权错误 |
| `VAL-xxx` | 400 | 参数校验错误 |
| `SYS-xxx` | 5xx | 系统内部错误 |

---

## 5. HTTP 状态码规范

### 5.1 成功状态码

| 状态码 | 用途 | 说明 |
|--------|------|------|
| 200 OK | 成功查询/更新 | 响应包含资源 |
| 201 Created | 资源创建成功 | 响应包含新资源，Location 头 |
| 202 Accepted | 异步任务接受 | 响应包含任务 ID |
| 204 No Content | 成功删除 | 无响应体 |

### 5.2 客户端错误状态码

| 状态码 | 用途 | 说明 |
|--------|------|------|
| 400 Bad Request | 请求参数错误 | 响应包含校验错误详情 |
| 401 Unauthorized | 未认证 | 需要登录 |
| 403 Forbidden | 无权限 | 无权访问资源 |
| 404 Not Found | 资源不存在 | |
| 409 Conflict | 资源冲突 | 如重复创建 |
| 422 Unprocessable Entity | 语义错误 | 请求格式正确但无法处理 |
| 429 Too Many Requests | 请求过于频繁 | |

### 5.3 服务端错误状态码

| 状态码 | 用途 | 说明 |
|--------|------|------|
| 500 Internal Server Error | 服务器内部错误 | 响应不包含敏感信息 |
| 502 Bad Gateway | 外部服务错误 | |
| 503 Service Unavailable | 服务不可用 | |
| 504 Gateway Timeout | 外部服务超时 | |

---

## 6. 版本管理

### 6.1 URL 版本

```
# ✅ 正确：URL 中包含版本号
/api/v1/workflows
/api/v2/workflows

# ❌ 错误：使用查询参数或 Header
/api/workflows?version=1
Accept: application/vnd.api+json; version=1
```

### 6.2 版本升级策略

- **不破坏性变更**：新增字段、新增接口，无需升级版本
- **破坏性变更**：修改字段名、删除字段、修改接口行为，必须升级版本
- **废弃通知**：在响应头中添 `Deprecation` 通知

```http
# 废弃通知示例
Deprecation: true
Sunset: Sat, 01 Jan 2027 00:00:00 GMT
Link: <https://api.example.com/v2/workflows>; rel="successor-version"
```

---

## 7. API 设计检查清单

### 7.1 接口设计检查

- [ ] 资源命名使用名词复数
- [ ] 正确使用 HTTP 方法
- [ ] 嵌套层级不超过 2 层
- [ ] 路径参数命名清晰（如 `workflowId` 而非 `id`）
- [ ] 查询参数用于过滤/分页/排序

### 7.2 响应设计检查

- [ ] 错误响应格式统一
- [ ] 错误码有明确含义
- [ ] 时间格式使用 ISO 8601（`2026-04-03T10:00:00Z`）
- [ ] 布尔值使用 `is`/`has`/`can` 前缀
- [ ] 敏感信息不返回给客户端

### 7.3 文档检查

- [ ] OpenAPI/Swagger 文档完整
- [ ] 请求/响应示例完整
- [ ] 错误码有说明
- [ ] 接口有版本标注

---

## 8. OpenAPI 规范

### 8.1 OpenAPI 定义示例

```yaml
openapi: 3.0.3
info:
  title: AI 研发协同平台 API
  version: 1.0.0
  description: 工作流引擎 API 文档

paths:
  /api/v1/workflows:
    get:
      summary: 获取工作流列表
      operationId: listWorkflows
      parameters:
        - name: page
          in: query
          schema:
            type: integer
            default: 0
        - name: size
          in: query
          schema:
            type: integer
            default: 20
      responses:
        '200':
          description: 成功
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/WorkflowList'
    post:
      summary: 创建工作流
      operationId: createWorkflow
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateWorkflowRequest'
      responses:
        '201':
          description: 创建成功
          headers:
            Location:
              schema:
                type: string
              description: 新资源 URL

components:
  schemas:
    WorkflowList:
      type: object
      properties:
        items:
          type: array
          items:
            $ref: '#/components/schemas/Workflow'
        total:
          type: integer
        page:
          type: integer
        pageSize:
          type: integer
```

---

## 9. 违规处理

| 违规级别 | 行为 | 处理方式 |
|---------|------|---------|
| 🔴 严重 | API 响应格式不符合本规范 | PR 拒绝，必须修改 |
| 🔴 严重 | 错误码未定义或冲突 | PR 拒绝，必须补充 |
| 🟠 重要 | OpenAPI 文档缺失或不完整 | 补充后才能提测 |
| 🟡 一般 | URL 命名不符合规范 | 建议修改 |

---

## 10. 相关文档

| 文档 | 关系 |
|------|------|
| STANDARD-02-code-style.md | 代码风格基础 |
| STANDARD-04-testing.md | API 集成测试要求 |
| STANDARD-06-e2e-verification.md | API 验证流程 |

---

**最后更新**: 2026-04-03
**版本**: v1.0
**状态**: 生效
