<template>
  <div class="execution-list">
    <div class="page-header">
      <h2>执行记录</h2>
    </div>

    <el-card class="table-card">
      <el-table :data="executions" v-loading="loading" stripe>
        <el-table-column prop="id" label="执行ID" min-width="220" />
        <el-table-column prop="workflowName" label="工作流" min-width="150" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startedAt" label="开始时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.startedAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="completedAt" label="完成时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.completedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewDetail(row.id)">详情</el-button>
            <el-button
              link
              type="danger"
              @click="cancelExecution(row.id)"
              v-if="row.status === 'RUNNING' || row.status === 'PENDING'"
            >
              取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="executions.length === 0 && !loading" description="暂无执行记录" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { executionApi } from '@/services/workflow'
import type { WorkflowExecution } from '@/types/workflow'

const executions = ref<WorkflowExecution[]>([])
const loading = ref(false)

const loadExecutions = async () => {
  loading.value = true
  try {
    executions.value = await executionApi.list()
  } catch (error: any) {
    ElMessage.error(error.message || '加载失败')
    executions.value = []
  } finally {
    loading.value = false
  }
}

const viewDetail = (id: string) => {
  ElMessage.info(`查看执行详情: ${id}`)
}

const cancelExecution = async (id: string) => {
  try {
    await ElMessageBox.confirm('确定要取消这个执行吗？', '确认取消', {
      type: 'warning',
    })
    await executionApi.cancel(id)
    ElMessage.success('执行已取消')
    loadExecutions()
  } catch {
    // 取消操作
  }
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

onMounted(() => {
  loadExecutions()
})
</script>

<style scoped>
.execution-list {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.table-card {
  min-height: 500px;
}
</style>
