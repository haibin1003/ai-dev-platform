<template>
  <div class="task-retry-panel">
    <div class="panel-header">
      <h4>任务列表</h4>
      <div class="header-actions">
        <el-button
          v-if="failedTasks.length > 0"
          type="warning"
          size="small"
          @click="retryAllFailed"
        >
          <el-icon><Refresh /></el-icon>
          批量重试 ({{ failedTasks.length }})
        </el-button>
        <el-button link size="small" @click="refreshTasks">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <el-table :data="tasks" v-loading="loading" size="small" stripe>
      <el-table-column prop="nodeId" label="任务节点" min-width="120" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getTaskStatusType(row.status)" size="small">
            {{ getTaskStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="retryCount" label="重试次数" width="90">
        <template #default="{ row }">
          {{ row.retryCount }} / {{ row.maxRetries }}
        </template>
      </el-table-column>
      <el-table-column prop="startedAt" label="开始时间" width="150">
        <template #default="{ row }">
          {{ formatDate(row.startedAt) }}
        </template>
      </el-table-column>
      <el-table-column prop="completedAt" label="完成时间" width="150">
        <template #default="{ row }">
          {{ formatDate(row.completedAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="canRetry(row)"
            link
            type="warning"
            size="small"
            @click="retryTask(row.id)"
          >
            重试
          </el-button>
          <el-button
            v-if="row.errorMessage"
            link
            type="danger"
            size="small"
            @click="showError(row)"
          >
            查看错误
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Error Dialog -->
    <el-dialog v-model="errorDialogVisible" title="错误详情" width="600px">
      <pre class="error-message">{{ selectedError }}</pre>
      <template #footer>
        <el-button @click="errorDialogVisible = false">关闭</el-button>
        <el-button
          v-if="selectedTask && canRetry(selectedTask)"
          type="warning"
          @click="retryTask(selectedTask.id)"
        >
          重试任务
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { TaskResponse } from '@/types/workflow'

const props = defineProps<{
  executionId: string
}>()

const emit = defineEmits<{
  taskRetried: []
}>()

const tasks = ref<TaskResponse[]>([])
const loading = ref(false)
const errorDialogVisible = ref(false)
const selectedError = ref('')
const selectedTask = ref<TaskResponse | null>(null)

const failedTasks = computed(() => {
  return tasks.value.filter((t) => t.status === 'FAILED' && t.retryCount < t.maxRetries)
})

const loadTasks = async () => {
  loading.value = true
  try {
    const res = await fetch(`/api/v1/executions/${props.executionId}/tasks`)
    if (!res.ok) throw new Error('加载任务失败')
    tasks.value = await res.json()
  } catch (error: any) {
    ElMessage.error(error.message || '加载任务失败')
  } finally {
    loading.value = false
  }
}

const retryTask = async (taskId: string) => {
  try {
    await ElMessageBox.confirm('确定要重试这个任务吗？', '确认重试', {
      type: 'warning',
    })

    const res = await fetch(
      `/api/v1/executions/${props.executionId}/tasks/${taskId}/retry`,
      { method: 'POST' }
    )

    if (!res.ok) {
      const err = await res.text()
      throw new Error(err || '重试失败')
    }

    ElMessage.success('任务重试已启动')
    emit('taskRetried')
    loadTasks()
  } catch {
    // 取消操作
  }
}

const retryAllFailed = async () => {
  if (failedTasks.value.length === 0) return

  try {
    await ElMessageBox.confirm(
      `确定要批量重试 ${failedTasks.value.length} 个失败任务吗？`,
      '确认批量重试',
      { type: 'warning' }
    )

    // Sequential retry to avoid overwhelming the server
    for (const task of failedTasks.value) {
      try {
        await fetch(
          `/api/v1/executions/${props.executionId}/tasks/${task.id}/retry`,
          { method: 'POST' }
        )
      } catch (e) {
        console.error(`Failed to retry task ${task.id}:`, e)
      }
    }

    ElMessage.success('批量重试已启动')
    emit('taskRetried')
    loadTasks()
  } catch {
    // 取消操作
  }
}

const refreshTasks = () => {
  loadTasks()
}

const canRetry = (task: TaskResponse): boolean => {
  return task.status === 'FAILED' && task.retryCount < task.maxRetries
}

const showError = (task: TaskResponse) => {
  selectedTask.value = task
  selectedError.value = task.errorMessage || '无错误信息'
  errorDialogVisible.value = true
}

const getTaskStatusType = (status?: string) => {
  const map: Record<string, string> = {
    PENDING: 'info',
    READY: 'info',
    RUNNING: 'primary',
    COMPLETED: 'success',
    FAILED: 'danger',
    CANCELLED: 'warning',
  }
  return map[status || 'PENDING'] || 'info'
}

const getTaskStatusText = (status?: string) => {
  const map: Record<string, string> = {
    PENDING: '等待中',
    READY: '就绪',
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

onMounted(() => {
  loadTasks()
})
</script>

<style scoped>
.task-retry-panel {
  padding: 16px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.panel-header h4 {
  margin: 0;
  font-size: 14px;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.error-message {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  font-family: monospace;
  font-size: 12px;
  color: #f56c6c;
  max-height: 300px;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
