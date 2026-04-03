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
            <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="progress" label="进度" width="200">
          <template #default="{ row }">
            <el-progress
              :percentage="Math.round((row.completedTasks / row.totalTasks) * 100)"
              :status="getProgressStatus(row.status)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="startedAt" label="开始时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.startedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewDetail(row.id)">详情</el-button>
            <el-button
              link
              type="danger"
              @click="cancelExecution(row.id)"
              v-if="row.status === 'RUNNING'"
            >
              取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadExecutions"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

interface Execution {
  id: string
  workflowName: string
  status: string
  completedTasks: number
  totalTasks: number
  startedAt: string
}

const executions = ref<Execution[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)

const loadExecutions = async () => {
  loading.value = true
  // TODO: 调用后端API
  setTimeout(() => {
    executions.value = [
      {
        id: 'exec-001',
        workflowName: '示例工作流',
        status: 'RUNNING',
        completedTasks: 2,
        totalTasks: 5,
        startedAt: '2026-04-03T10:00:00',
      },
    ]
    total.value = 1
    loading.value = false
  }, 500)
}

const viewDetail = (id: string) => {
  ElMessage.info(`查看执行详情: ${id}`)
}

const cancelExecution = async (id: string) => {
  // TODO: 调用后端API
  ElMessage.success('执行已取消')
  loadExecutions()
}

const getStatusType = (status: string) => {
  const map: Record<string, string> = {
    PENDING: 'info',
    RUNNING: 'primary',
    COMPLETED: 'success',
    FAILED: 'danger',
    CANCELLED: 'warning',
  }
  return map[status] || 'info'
}

const getProgressStatus = (status: string) => {
  if (status === 'COMPLETED') return 'success'
  if (status === 'FAILED') return 'exception'
  return ''
}

const formatDate = (date: string) => {
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

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
