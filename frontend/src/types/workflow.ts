export interface WorkflowNode {
  id: string
  type: 'START' | 'TASK' | 'END'
  label: string
  position: { x: number; y: number }
  properties?: {
    agentId?: string
    agentName?: string
    inputMapping?: Record<string, string>
    outputMapping?: Record<string, string>
    timeout?: number
  }
}

export interface WorkflowEdge {
  id: string
  source: string
  target: string
  condition?: string
}

export interface WorkflowDefinition {
  nodes: WorkflowNode[]
  edges: WorkflowEdge[]
}

export interface Workflow {
  id?: string
  name: string
  description?: string
  definition: WorkflowDefinition
  status?: 'DRAFT' | 'ACTIVE' | 'ARCHIVED'
  createdAt?: string
  updatedAt?: string
}

export interface AgentOption {
  id: string
  name: string
  description?: string
}

export interface WorkflowExecution {
  id: string
  workflowId: string
  workflowName: string
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'
  startedAt: string
  completedAt?: string
}

export interface TaskResponse {
  id: string
  nodeId: string
  status: 'PENDING' | 'READY' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'
  inputs?: Record<string, string>
  result?: string
  errorMessage?: string
  retryCount: number
  maxRetries: number
  startedAt?: string
  completedAt?: string
}

export interface NodeShapeConfig {
  shape: string
  width: number
  height: number
  label: string
  icon: string
  color: string
}
