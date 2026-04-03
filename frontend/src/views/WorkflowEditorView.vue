<template>
  <div class="workflow-editor">
    <!-- 顶部工具栏 -->
    <div class="editor-header">
      <div class="header-left">
        <el-button link @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <div class="workflow-info">
          <el-input
            v-model="form.name"
            placeholder="工作流名称"
            class="name-input"
            @change="markModified"
          />
        </div>
      </div>
      <ToolbarPanel
        :can-undo="canUndo"
        :can-redo="canRedo"
        :status="saveStatus"
        @save="saveWorkflow"
        @validate="validateWorkflow"
        @zoom-in="zoomIn"
        @zoom-out="zoomOut"
        @fit="fitContent"
        @undo="undo"
        @redo="redo"
        @export="exportJSON"
        @clear="clearCanvas"
      />
    </div>

    <!-- 主编辑区 -->
    <div class="editor-body">
      <!-- 左侧节点面板 -->
      <NodePalette />

      <!-- 中间画布 -->
      <div ref="graphContainer" class="graph-container" @drop="onDrop" @dragover.prevent />

      <!-- 右侧属性面板 -->
      <PropertyPanel
        :selected-node="selectedNode"
        :selected-edge="selectedEdge"
        @change="onPropertyChange"
        @delete="deleteSelected"
      />
    </div>

    <!-- 导入JSON对话框 -->
    <el-dialog v-model="importDialogVisible" title="导入工作流" width="600px">
      <el-input
        v-model="importJSONText"
        type="textarea"
        :rows="10"
        placeholder="粘贴工作流JSON"
      />
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmImport">导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Graph } from '@antv/x6'
import { register } from '@antv/x6-vue-shape'
import type { Node, Edge } from '@antv/x6'

import NodePalette from '@/components/workflow/panels/NodePalette.vue'
import ToolbarPanel from '@/components/workflow/panels/ToolbarPanel.vue'
import PropertyPanel from '@/components/workflow/panels/PropertyPanel.vue'
import StartNode from '@/components/workflow/nodes/StartNode.vue'
import TaskNode from '@/components/workflow/nodes/TaskNode.vue'
import EndNode from '@/components/workflow/nodes/EndNode.vue'

import { workflowApi } from '@/services/workflow'
import type { Workflow, WorkflowDefinition } from '@/types/workflow'

// 注册Vue节点组件
register({
  shape: 'start-node',
  component: StartNode,
  width: 80,
  height: 80,
})

register({
  shape: 'task-node',
  component: TaskNode,
  width: 160,
  height: 80,
})

register({
  shape: 'end-node',
  component: EndNode,
  width: 80,
  height: 80,
})

const route = useRoute()
const router = useRouter()

const graphContainer = ref<HTMLElement>()
let graph: Graph | null = null

const isEdit = computed(() => !!route.params.id)
const workflowId = computed(() => route.params.id as string)

const form = ref<Workflow>({
  name: '',
  description: '',
  definition: {
    nodes: [],
    edges: [],
  },
})

const saveStatus = ref<'unsaved' | 'saved' | 'modified'>('unsaved')
const selectedNode = ref<Node>()
const selectedEdge = ref<Edge>()
const canUndo = ref(false)
const canRedo = ref(false)
const importDialogVisible = ref(false)
const importJSONText = ref('')

// 初始化画布
const initGraph = () => {
  if (!graphContainer.value) return

  graph = new Graph({
    container: graphContainer.value,
    width: graphContainer.value.clientWidth,
    height: graphContainer.value.clientHeight,
    grid: {
      size: 10,
      visible: true,
      type: 'dot',
    },
    background: {
      color: '#f5f7fa',
    },
    panning: {
      enabled: true,
      modifiers: 'shift',
    },
    mousewheel: {
      enabled: true,
      modifiers: ['ctrl', 'meta'],
    },
    connecting: {
      router: 'manhattan',
      connector: {
        name: 'rounded',
        args: { radius: 8 },
      },
      anchor: 'center',
      connectionPoint: 'anchor',
      allowBlank: false,
      snap: { radius: 20 },
      createEdge() {
        return graph!.createEdge({
          attrs: {
            line: {
              stroke: '#909399',
              strokeWidth: 2,
              targetMarker: {
                name: 'classic',
                size: 8,
              },
            },
          },
          label: {
            attrs: {
              text: { text: '' },
            },
          },
          data: {},
        })
      },
      validateConnection({ sourceMagnet, targetMagnet }) {
        if (sourceMagnet?.getAttribute('port-group') === 'in') return false
        if (targetMagnet?.getAttribute('port-group') === 'out') return false
        return true
      },
    },
    highlighting: {
      magnetAdsorbed: {
        name: 'stroke',
        args: {
          attrs: {
            fill: '#5F95FF',
            stroke: '#5F95FF',
          },
        },
      },
    },
    history: {
      enabled: true,
    },
    clipboard: {
      enabled: true,
    },
    keyboard: {
      enabled: true,
    },
    selecting: {
      enabled: true,
      rubberband: true,
      showNodeSelectionBox: true,
    },
  })

  // 添加事件监听
  graph.on('node:click', ({ node }) => {
    selectedNode.value = node
    selectedEdge.value = undefined
  })

  graph.on('edge:click', ({ edge }) => {
    selectedEdge.value = edge
    selectedNode.value = undefined
  })

  graph.on('blank:click', () => {
    selectedNode.value = undefined
    selectedEdge.value = undefined
  })

  graph.on('history:change', () => {
    canUndo.value = graph?.canUndo() || false
    canRedo.value = graph?.canRedo() || false
  })

  graph.on('cell:changed', () => {
    markModified()
  })

  // 监听键盘删除
  graph.bindKey(['delete', 'backspace'], () => {
    deleteSelected()
  })

  // 窗口大小调整
  window.addEventListener('resize', resizeGraph)
}

const resizeGraph = () => {
  if (graph && graphContainer.value) {
    graph.resize(graphContainer.value.clientWidth, graphContainer.value.clientHeight)
  }
}

// 拖拽创建节点
const onDrop = (e: DragEvent) => {
  e.preventDefault()
  if (!graph) return

  const data = e.dataTransfer?.getData('application/x-node-shape')
  if (!data) return

  const nodeConfig = JSON.parse(data)
  const { x, y } = graph.clientToLocal(e.clientX, e.clientY)

  const nodeId = `${nodeConfig.shape}-${Date.now()}`
  const node = graph.addNode({
    id: nodeId,
    shape: nodeConfig.shape,
    x: x - nodeConfig.width / 2,
    y: y - nodeConfig.height / 2,
    data: {
      label: nodeConfig.label,
    },
    ports: {
      items: [
        { id: 'in', group: 'in' },
        { id: 'out', group: 'out' },
      ],
    },
  })

  // 添加锚点样式
  if (nodeConfig.shape !== 'start-node') {
    node.addPort({
      id: 'in',
      group: 'in',
      attrs: { circle: { magnet: true, r: 6, fill: '#fff', stroke: '#5F95FF', strokeWidth: 2 } },
    })
  }
  if (nodeConfig.shape !== 'end-node') {
    node.addPort({
      id: 'out',
      group: 'out',
      attrs: { circle: { magnet: true, r: 6, fill: '#fff', stroke: '#5F95FF', strokeWidth: 2 } },
    })
  }

  markModified()
}

// 属性变更
const onPropertyChange = (data: any) => {
  if (selectedNode.value) {
    selectedNode.value.setData({ ...selectedNode.value.getData(), ...data })
    if (data.label) {
      selectedNode.value.attr('label/text', data.label)
    }
  } else if (selectedEdge.value) {
    selectedEdge.value.setData({ ...selectedEdge.value.getData(), ...data })
    if (data.condition) {
      selectedEdge.value.setLabels([{ attrs: { text: { text: data.condition } } }])
    }
  }
  markModified()
}

// 删除选中元素
const deleteSelected = () => {
  if (!graph) return
  const cells = graph.getSelectedCells()
  if (cells.length > 0) {
    graph.removeCells(cells)
    selectedNode.value = undefined
    selectedEdge.value = undefined
    markModified()
  }
}

// 标记修改状态
const markModified = () => {
  if (saveStatus.value !== 'unsaved') {
    saveStatus.value = 'modified'
  }
}

// 工具栏功能
const zoomIn = () => graph?.zoom(0.2)
const zoomOut = () => graph?.zoom(-0.2)
const fitContent = () => graph?.zoomToFit({ padding: 20 })
const undo = () => graph?.undo()
const redo = () => graph?.redo()

const clearCanvas = async () => {
  try {
    await ElMessageBox.confirm('确定要清空画布吗？所有节点和连线将被删除。', '确认清空', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    graph?.clearCells()
    markModified()
    ElMessage.success('画布已清空')
  } catch {
    // 取消操作
  }
}

// 保存工作流
const saveWorkflow = async () => {
  if (!form.value.name) {
    ElMessage.warning('请输入工作流名称')
    return
  }

  try {
    const definition = getGraphDefinition()
    const workflow: Workflow = {
      ...form.value,
      definition,
    }

    if (isEdit.value) {
      await workflowApi.update(workflowId.value, workflow)
    } else {
      const id = await workflowApi.create(workflow)
      workflowId.value && router.replace(`/workflows/edit/${id}`)
    }

    saveStatus.value = 'saved'
    ElMessage.success('保存成功')
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  }
}

// 验证工作流
const validateWorkflow = async () => {
  try {
    const definition = getGraphDefinition()
    const result = await workflowApi.validate({ ...form.value, definition })
    if (result.valid) {
      ElMessage.success('工作流验证通过')
    } else {
      ElMessage.error('验证失败: ' + (result.errors?.join(', ') || '未知错误'))
    }
  } catch (error: any) {
    // 后端API可能不存在，前端简单验证
    const errors = validateDefinition()
    if (errors.length === 0) {
      ElMessage.success('工作流验证通过')
    } else {
      ElMessage.error('验证失败: ' + errors.join(', '))
    }
  }
}

// 前端简单验证
const validateDefinition = (): string[] => {
  if (!graph) return ['画布未初始化']

  const nodes = graph.getNodes()
  const edges = graph.getEdges()
  const errors: string[] = []

  // 检查开始节点
  const startNodes = nodes.filter((n) => n.shape === 'start-node')
  if (startNodes.length === 0) {
    errors.push('缺少开始节点')
  } else if (startNodes.length > 1) {
    errors.push('只能有一个开始节点')
  }

  // 检查结束节点
  const endNodes = nodes.filter((n) => n.shape === 'end-node')
  if (endNodes.length === 0) {
    errors.push('缺少结束节点')
  }

  // 检查任务节点配置
  const taskNodes = nodes.filter((n) => n.shape === 'task-node')
  for (const node of taskNodes) {
    const data = node.getData()
    if (!data?.agentId) {
      errors.push(`任务节点 "${data?.label || node.id}" 未配置Agent`)
    }
  }

  // 检查循环
  if (hasCycle(nodes, edges)) {
    errors.push('工作流存在循环依赖')
  }

  return errors
}

// 检测循环
const hasCycle = (nodes: Node[], edges: Edge[]): boolean => {
  const adjList = new Map<string, string[]>()
  nodes.forEach((n) => adjList.set(n.id, []))
  edges.forEach((e) => {
    const source = e.getSourceCellId()
    const target = e.getTargetCellId()
    if (source && target) {
      adjList.get(source)?.push(target)
    }
  })

  const visited = new Set<string>()
  const recStack = new Set<string>()

  const dfs = (nodeId: string): boolean => {
    visited.add(nodeId)
    recStack.add(nodeId)

    for (const neighbor of adjList.get(nodeId) || []) {
      if (!visited.has(neighbor)) {
        if (dfs(neighbor)) return true
      } else if (recStack.has(neighbor)) {
        return true
      }
    }

    recStack.delete(nodeId)
    return false
  }

  for (const nodeId of adjList.keys()) {
    if (!visited.has(nodeId)) {
      if (dfs(nodeId)) return true
    }
  }
  return false
}

// 获取图定义
const getGraphDefinition = (): WorkflowDefinition => {
  if (!graph) return { nodes: [], edges: [] }

  const nodes = graph.getNodes().map((n) => ({
    id: n.id,
    type: n.shape === 'start-node' ? 'START' : n.shape === 'end-node' ? 'END' : 'TASK',
    label: n.getData()?.label || n.shape,
    position: { x: n.getPosition().x, y: n.getPosition().y },
    properties: {
      agentId: n.getData()?.agentId,
      agentName: n.getData()?.agentName,
      inputMapping: n.getData()?.inputMapping,
      outputMapping: n.getData()?.outputMapping,
      timeout: n.getData()?.timeout,
    },
  }))

  const edges = graph.getEdges().map((e) => ({
    id: e.id,
    source: e.getSourceCellId() || '',
    target: e.getTargetCellId() || '',
    condition: e.getData()?.condition,
  }))

  return { nodes, edges }
}

// 加载工作流
const loadWorkflow = async (id: string) => {
  try {
    const workflow = await workflowApi.get(id)
    form.value = {
      name: workflow.name,
      description: workflow.description || '',
      definition: workflow.definition,
    }
    loadGraphDefinition(workflow.definition)
    saveStatus.value = 'saved'
  } catch (error: any) {
    ElMessage.error(error.message || '加载工作流失败')
  }
}

// 加载图定义
const loadGraphDefinition = (definition: WorkflowDefinition) => {
  if (!graph) return

  graph.clearCells()

  // 添加节点
  for (const nodeDef of definition.nodes) {
    const shape =
      nodeDef.type === 'START' ? 'start-node' : nodeDef.type === 'END' ? 'end-node' : 'task-node'

    const node = graph.addNode({
      id: nodeDef.id,
      shape,
      x: nodeDef.position.x,
      y: nodeDef.position.y,
      data: {
        label: nodeDef.label,
        ...nodeDef.properties,
      },
    })

    // 添加端口
    if (shape !== 'start-node') {
      node.addPort({
        id: 'in',
        group: 'in',
        attrs: { circle: { magnet: true, r: 6, fill: '#fff', stroke: '#5F95FF', strokeWidth: 2 } },
      })
    }
    if (shape !== 'end-node') {
      node.addPort({
        id: 'out',
        group: 'out',
        attrs: { circle: { magnet: true, r: 6, fill: '#fff', stroke: '#5F95FF', strokeWidth: 2 } },
      })
    }
  }

  // 添加边
  for (const edgeDef of definition.edges) {
    graph.addEdge({
      id: edgeDef.id,
      source: { cell: edgeDef.source, port: 'out' },
      target: { cell: edgeDef.target, port: 'in' },
      attrs: {
        line: {
          stroke: '#909399',
          strokeWidth: 2,
          targetMarker: { name: 'classic', size: 8 },
        },
      },
      labels: edgeDef.condition ? [{ attrs: { text: { text: edgeDef.condition } } }] : [],
      data: { condition: edgeDef.condition },
    })
  }
}

// 导出JSON
const exportJSON = () => {
  const workflow = {
    ...form.value,
    definition: getGraphDefinition(),
  }
  const json = workflowApi.exportToJSON(workflow)

  // 复制到剪贴板
  navigator.clipboard.writeText(json).then(() => {
    ElMessage.success('已复制到剪贴板')
  })
}

// 导入JSON
const confirmImport = () => {
  try {
    const workflow = workflowApi.importFromJSON(importJSONText.value)
    form.value.name = workflow.name || ''
    form.value.description = workflow.description || ''
    loadGraphDefinition(workflow.definition)
    importDialogVisible.value = false
    markModified()
    ElMessage.success('导入成功')
  } catch (e) {
    ElMessage.error('导入失败: JSON格式错误')
  }
}

// 返回
const goBack = () => {
  router.back()
}

onMounted(() => {
  nextTick(() => {
    initGraph()
    if (isEdit.value) {
      loadWorkflow(workflowId.value)
    } else {
      // 新工作流：添加默认开始节点
      setTimeout(() => {
        if (graph) {
          const startNode = graph.addNode({
            id: `start-${Date.now()}`,
            shape: 'start-node',
            x: 100,
            y: 200,
            data: { label: '开始' },
          })
          startNode.addPort({
            id: 'out',
            group: 'out',
            attrs: { circle: { magnet: true, r: 6, fill: '#fff', stroke: '#5F95FF', strokeWidth: 2 } },
          })

          const endNode = graph.addNode({
            id: `end-${Date.now()}`,
            shape: 'end-node',
            x: 400,
            y: 200,
            data: { label: '结束' },
          })
          endNode.addPort({
            id: 'in',
            group: 'in',
            attrs: { circle: { magnet: true, r: 6, fill: '#fff', stroke: '#5F95FF', strokeWidth: 2 } },
          })

          graph.addEdge({
            source: { cell: startNode.id, port: 'out' },
            target: { cell: endNode.id, port: 'in' },
            attrs: {
              line: { stroke: '#909399', strokeWidth: 2, targetMarker: { name: 'classic', size: 8 } },
            },
          })

          fitContent()
        }
      }, 100)
    }
  })
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeGraph)
  graph?.dispose()
  graph = null
})
</script>

<style scoped>
.workflow-editor {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f7fa;
}

.editor-header {
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.workflow-info {
  display: flex;
  align-items: center;
}

.name-input {
  width: 240px;
}

.name-input :deep(.el-input__wrapper) {
  box-shadow: none;
  background: transparent;
}

.name-input :deep(.el-input__inner) {
  font-size: 16px;
  font-weight: 500;
}

.editor-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.graph-container {
  flex: 1;
  overflow: hidden;
}
</style>
