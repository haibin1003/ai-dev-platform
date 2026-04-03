<template>
  <el-dialog
    v-model="visible"
    title="执行详情"
    width="80%"
    :close-on-click-modal="false"
    destroy-on-close
    class="execution-detail-dialog"
  >
    <div class="execution-detail">
      <!-- Execution Info -->
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="执行ID">{{ execution?.id }}</el-descriptions-item>
        <el-descriptions-item label="工作流">{{ execution?.workflowName }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(execution?.status)">
            {{ getStatusText(execution?.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ formatDate(execution?.startedAt) }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ formatDate(execution?.completedAt) }}</el-descriptions-item>
        <el-descriptions-item label="操作">
          <el-button
            v-if="execution?.status === 'RUNNING' || execution?.status === 'PENDING'"
            type="danger"
            size="small"
            @click="cancelExecution"
          >
            取消执行
          </el-button>
        </el-descriptions-item>
      </el-descriptions>

      <!-- Tabs -->
      <el-tabs v-model="activeTab" class="detail-tabs">
        <el-tab-pane label="执行日志" name="logs">
          <div class="section-header">
            <el-button v-if="execution?.id" link type="primary" @click="refreshLogs">
              <el-icon><Refresh /></el-icon>
              刷新日志
            </el-button>
          </div>
          <LogViewer v-if="execution?.id" :execution-id="execution.id" class="viewer-wrapper" />
        </el-tab-pane>

        <el-tab-pane label="任务列表" name="tasks">
          <TaskRetryPanel
            v-if="execution?.id"
            :execution-id="execution.id"
            @task-retried="handleTaskRetried"
            class="viewer-wrapper"
          />
        </el-tab-pane>
      </el-tabs>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import LogViewer from '@/components/log/LogViewer.vue'
import TaskRetryPanel from './TaskRetryPanel.vue'
import { executionApi } from '@/services/workflow'
import type { WorkflowExecution } from '@/types/workflow'

const props = defineProps<{
  modelValue: boolean
  execution?: WorkflowExecution
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  cancelled: []
}>()

const visible = ref(props.modelValue)
const activeTab = ref('logs')

watch(() => props.modelValue, (val) => {
  visible.value = val
})

watch(() => visible.value, (val) => {
  emit('update:modelValue', val)
})

const handleTaskRetried = () => {
  emit('cancelled') // Refresh parent list
}

const cancelExecution = async () => {
  if (!props.execution?.id) return

  try {
    await ElMessageBox.confirm('确定要取消这个执行吗？', '确认取消', {
      type: 'warning',
    })
    await executionApi.cancel(props.execution.id)
    ElMessage.success('执行已取消')
    emit('cancelled')
    visible.value = false
  } catch {
    // Cancelled
  }
}

const refreshLogs = () => {
  ElMessage.success('日志已刷新')
}

const getStatusType = (status?: string) => {
  const map: Record<string, string> = {
    PENDING: 'info',
    RUNNING: 'primary',
    COMPLETED: 'success',
    FAILED: 'danger',
    CANCELLED: 'warning',
  }
  return map[status || 'PENDING'] || 'info'
}

const getStatusText = (status?: string) => {
  const map: Record<string, string> = {
    PENDING: '等待中',
    RUNNING: '运行中',
    COMPLETED: '已完成',
    FAILED: '失败',
    CANCELLED: '已取消',
  }
  return map[status || 'PENDING'] || status
}

const formatDate = (date?: string) => {
  if (!date) return '-'
  return new Date(date).toLocaleString()
}
</script>

<style scoped>
.execution-detail-dialog :deep(.el-dialog__body) {
  padding: 20px;
}

.execution-detail {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.detail-tabs :deep(.el-tabs__content) {
  padding: 0;
}

.section-header {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 8px;
}

.viewer-wrapper {
  height: 400px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}
</style>
