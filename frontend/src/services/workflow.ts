import type { Workflow, WorkflowExecution, WorkflowNode, WorkflowEdge } from '@/types/workflow'

const API_BASE = '/api/v1/workflows'

// 后端 DTO 类型
interface BackendNodeDTO {
  id: string
  name: string
  type: string
  agentCode?: string
  config?: Record<string, any>
}

interface BackendEdgeDTO {
  from: string
  to: string
}

interface BackendDefinitionDTO {
  nodes: BackendNodeDTO[]
  edges: BackendEdgeDTO[]
}

interface BackendWorkflow {
  id: string
  name: string
  description?: string
  status: string
  definition: BackendDefinitionDTO
  variables?: Record<string, string>
  createdAt?: string
  updatedAt?: string
}

// 转换前端 Workflow 到后端格式
function toBackendWorkflow(workflow: Workflow): any {
  return {
    name: workflow.name,
    description: workflow.description,
    definition: {
      nodes: workflow.definition.nodes.map((n): BackendNodeDTO => ({
        id: n.id,
        name: n.label,
        type: n.type,
        agentCode: n.properties?.agentId,
        config: {
          ...n.properties,
          position: n.position,
        },
      })),
      edges: workflow.definition.edges.map((e): BackendEdgeDTO => ({
        from: e.source,
        to: e.target,
      })),
    },
  }
}

// 转换后端 Workflow 到前端格式
function fromBackendWorkflow(backend: BackendWorkflow): Workflow {
  const nodePositions = new Map<string, { x: number; y: number }>()

  return {
    id: backend.id,
    name: backend.name,
    description: backend.description,
    status: backend.status as any,
    definition: {
      nodes: backend.definition.nodes.map((n): WorkflowNode => {
        const position = n.config?.position || { x: 100, y: 100 }
        nodePositions.set(n.id, position)
        return {
          id: n.id,
          type: n.type as any,
          label: n.name,
          position,
          properties: {
            agentId: n.agentCode,
            agentName: n.agentCode,
            ...n.config,
          },
        }
      }),
      edges: backend.definition.edges.map((e, idx): WorkflowEdge => ({
        id: `edge-${idx}`,
        source: e.from,
        target: e.to,
      })),
    },
    createdAt: backend.createdAt,
    updatedAt: backend.updatedAt,
  }
}

export const workflowApi = {
  // 工作流列表
  async list(): Promise<Workflow[]> {
    const res = await fetch(API_BASE)
    if (!res.ok) throw new Error('获取工作流列表失败')
    const backends: BackendWorkflow[] = await res.json()
    return backends.map(fromBackendWorkflow)
  },

  // 工作流列表
  async list(): Promise<Workflow[]> {
    const res = await fetch(API_BASE)
    if (!res.ok) throw new Error('获取工作流列表失败')
    const backends: BackendWorkflow[] = await res.json()
    return backends.map(fromBackendWorkflow)
  },

  // 获取单个工作流
  async get(id: string): Promise<Workflow> {
    const res = await fetch(`${API_BASE}/${id}`)
    if (!res.ok) throw new Error('获取工作流失败')
    const backend: BackendWorkflow = await res.json()
    return fromBackendWorkflow(backend)
  },

  // 创建工作流
  async create(workflow: Workflow): Promise<string> {
    const res = await fetch(API_BASE, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(toBackendWorkflow(workflow)),
    })
    if (!res.ok) {
      const err = await res.text()
      throw new Error(err || '创建工作流失败')
    }
    const result = await res.json()
    return result.id
  },

  // 更新工作流
  async update(id: string, workflow: Workflow): Promise<void> {
    const res = await fetch(`${API_BASE}/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(toBackendWorkflow(workflow)),
    })
    if (!res.ok) {
      const err = await res.text()
      throw new Error(err || '更新工作流失败')
    }
  },

  // 删除工作流
  async delete(id: string): Promise<void> {
    const res = await fetch(`${API_BASE}/${id}`, { method: 'DELETE' })
    if (!res.ok) throw new Error('删除工作流失败')
  },

  // 验证工作流
  async validate(workflow: Workflow): Promise<{ valid: boolean; errors?: string[] }> {
    const res = await fetch(`${API_BASE}/validate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(toBackendWorkflow(workflow)),
    })
    if (!res.ok) {
      const err = await res.text()
      throw new Error(err || '验证工作流失败')
    }
    return res.json()
  },

  // 执行工作流
  async execute(id: string, variables?: Record<string, any>): Promise<string> {
    const res = await fetch(`${API_BASE}/${id}/execute`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ variables }),
    })
    if (!res.ok) throw new Error('执行工作流失败')
    const result = await res.json()
    return result.executionId
  },

  // 导出工作流JSON
  exportToJSON(workflow: Workflow): string {
    return JSON.stringify(workflow, null, 2)
  },

  // 导入工作流JSON
  importFromJSON(json: string): Workflow {
    return JSON.parse(json)
  },
}

// 转换后端执行记录到前端格式
function fromBackendExecution(backend: any): WorkflowExecution {
  return {
    id: backend.executionId,
    workflowId: backend.workflowId || '',
    workflowName: backend.workflowName || '',
    status: backend.status as any,
    startedAt: backend.startedAt,
    completedAt: backend.completedAt,
  }
}

export const executionApi = {
  // 执行列表
  async list(): Promise<WorkflowExecution[]> {
    const res = await fetch('/api/v1/executions')
    if (!res.ok) throw new Error('获取执行列表失败')
    const backends = await res.json()
    return backends.map(fromBackendExecution)
  },

  // 获取执行详情
  async get(id: string): Promise<WorkflowExecution> {
    const res = await fetch(`/api/v1/executions/${id}`)
    if (!res.ok) throw new Error('获取执行详情失败')
    const backend = await res.json()
    return fromBackendExecution(backend)
  },

  // 取消执行
  async cancel(id: string): Promise<void> {
    const res = await fetch(`/api/v1/executions/${id}/cancel`, { method: 'POST' })
    if (!res.ok) throw new Error('取消执行失败')
  },
}
