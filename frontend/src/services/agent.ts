import type { AgentOption } from '@/types/workflow'

// 获取可用Agent列表
export async function getAgents(): Promise<AgentOption[]> {
  const res = await fetch('/api/agents')
  if (!res.ok) {
    // 如果API不存在，返回默认Agent列表
    return getDefaultAgents()
  }
  return res.json()
}

// 默认Agent列表（用于开发测试）
export function getDefaultAgents(): AgentOption[] {
  return [
    { id: 'claude-code', name: 'Claude Code', description: 'Anthropic Claude Code 智能体' },
    { id: 'kimi-code', name: 'Kimi Code', description: 'Kimi Code 智能体' },
    { id: 'code-reviewer', name: 'Code Reviewer', description: '代码审查智能体' },
    { id: 'test-writer', name: 'Test Writer', description: '测试编写智能体' },
    { id: 'doc-writer', name: 'Doc Writer', description: '文档编写智能体' },
  ]
}
