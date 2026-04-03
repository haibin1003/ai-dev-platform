<template>
  <div class="property-panel">
    <div class="panel-header">
      <el-icon><Setting /></el-icon>
      <span>属性配置</span>
    </div>

    <div v-if="!selectedNode && !selectedEdge" class="empty-state">
      <el-empty description="请选择节点或连线" :image-size="80" />
    </div>

    <!-- 开始节点属性 -->
    <div v-else-if="selectedNode?.shape === 'start-node'" class="panel-content">
      <h4>开始节点</h4>
      <el-form label-width="80px">
        <el-form-item label="节点ID">
          <el-input v-model="form.id" disabled />
        </el-form-item>
        <el-form-item label="显示名称">
          <el-input v-model="form.label" placeholder="开始" @change="onChange" />
        </el-form-item>
      </el-form>
    </div>

    <!-- 任务节点属性 -->
    <div v-else-if="selectedNode?.shape === 'task-node'" class="panel-content">
      <h4>任务节点</h4>
      <el-form label-width="80px">
        <el-form-item label="节点ID">
          <el-input v-model="form.id" disabled />
        </el-form-item>
        <el-form-item label="任务名称" required>
          <el-input v-model="form.label" placeholder="输入任务名称" @change="onChange" />
        </el-form-item>
        <el-form-item label="选择Agent" required>
          <el-select v-model="form.agentId" placeholder="选择执行Agent" @change="onChange">
            <el-option
              v-for="agent in agentOptions"
              :key="agent.id"
              :label="agent.name"
              :value="agent.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="输入变量">
          <el-input
            v-model="form.inputMapping"
            type="textarea"
            :rows="3"
            placeholder="JSON格式变量映射"
            @change="onChange"
          />
        </el-form-item>
        <el-form-item label="输出变量">
          <el-input
            v-model="form.outputMapping"
            type="textarea"
            :rows="3"
            placeholder="JSON格式输出变量"
            @change="onChange"
          />
        </el-form-item>
        <el-form-item label="超时时间">
          <el-input-number v-model="form.timeout" :min="0" :step="30" @change="onChange">
            <template #suffix>秒</template>
          </el-input-number>
        </el-form-item>
      </el-form>
    </div>

    <!-- 结束节点属性 -->
    <div v-else-if="selectedNode?.shape === 'end-node'" class="panel-content">
      <h4>结束节点</h4>
      <el-form label-width="80px">
        <el-form-item label="节点ID">
          <el-input v-model="form.id" disabled />
        </el-form-item>
        <el-form-item label="显示名称">
          <el-input v-model="form.label" placeholder="结束" @change="onChange" />
        </el-form-item>
      </el-form>
    </div>

    <!-- 边属性 -->
    <div v-else-if="selectedEdge" class="panel-content">
      <h4>连线属性</h4>
      <el-form label-width="80px">
        <el-form-item label="连线ID">
          <el-input v-model="form.id" disabled />
        </el-form-item>
        <el-form-item label="条件表达式">
          <el-input
            v-model="form.condition"
            type="textarea"
            :rows="3"
            placeholder="例如: $.status == 'success'"
            @change="onChange"
          />
        </el-form-item>
      </el-form>
    </div>

    <div v-if="selectedNode || selectedEdge" class="panel-actions">
      <el-button type="danger" link @click="onDelete">
        <el-icon><Delete /></el-icon>
        删除
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import type { Node, Edge } from '@antv/x6'
import type { AgentOption } from '@/types/workflow'
import { getAgents } from '@/services/agent'

const props = defineProps<{
  selectedNode?: Node
  selectedEdge?: Edge
}>()

const emit = defineEmits<{
  change: [data: any]
  delete: []
}>()

const form = ref<any>({
  id: '',
  label: '',
  agentId: '',
  inputMapping: '',
  outputMapping: '',
  timeout: 300,
  condition: '',
})

const agentOptions = ref<AgentOption[]>([])

onMounted(async () => {
  try {
    agentOptions.value = await getAgents()
  } catch (e) {
    agentOptions.value = []
  }
})

watch(
  () => props.selectedNode,
  (node) => {
    if (node) {
      const data = node.getData() || {}
      form.value = {
        id: node.id,
        label: data.label || '',
        agentId: data.agentId || '',
        inputMapping: data.inputMapping ? JSON.stringify(data.inputMapping, null, 2) : '',
        outputMapping: data.outputMapping ? JSON.stringify(data.outputMapping, null, 2) : '',
        timeout: data.timeout || 300,
      }
    }
  },
  { immediate: true },
)

watch(
  () => props.selectedEdge,
  (edge) => {
    if (edge) {
      const data = edge.getData() || {}
      form.value = {
        id: edge.id,
        condition: data.condition || '',
      }
    }
  },
  { immediate: true },
)

const onChange = () => {
  const data = { ...form.value }
  if (data.inputMapping) {
    try {
      data.inputMapping = JSON.parse(data.inputMapping)
    } catch {
      // keep as string
    }
  }
  if (data.outputMapping) {
    try {
      data.outputMapping = JSON.parse(data.outputMapping)
    } catch {
      // keep as string
    }
  }
  emit('change', data)
}

const onDelete = () => {
  emit('delete')
}
</script>

<style scoped>
.property-panel {
  width: 280px;
  background: #fff;
  border-left: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.panel-header {
  height: 48px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 16px;
  border-bottom: 1px solid #e4e7ed;
  font-weight: 500;
  color: #303133;
}

.panel-content {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
}

.panel-content h4 {
  margin: 0 0 16px;
  font-size: 14px;
  color: #303133;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
}

.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.panel-actions {
  padding: 12px 16px;
  border-top: 1px solid #e4e7ed;
  display: flex;
  justify-content: center;
}
</style>
