<template>
  <div class="execution-list">
    <div class="page-header">
      <h2>执行记录</h2>
      <el-button type="primary" @click="exportExecutions">
        <el-icon><Download /></el-icon>
        导出
      </el-button>
    </div>

    <!-- Filter Panel -->
    <el-card class="filter-card">
      <el-form :model="filterForm" inline>
        <el-form-item label="工作流">
          <el-select
            v-model="filterForm.workflowId"
            placeholder="选择工作流"
            clearable
            style="width: 180px"
            @change="handleFilterChange"
          >
            <el-option
              v-for="wf in workflowOptions"
              :key="wf.id"
              :label="wf.name"
              :value="wf.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="状态">
          <el-select
            v-model="filterForm.status"
            placeholder="选择状态"
            clearable
            style="width: 120px"
            @change="handleFilterChange"
          >
            <el-option label="等待中" value="PENDING" />
            <el-option label="运行中" value="RUNNING" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="失败" value="FAILED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-form-item>

        <el-form-item label="时间范围">
          <el-date-picker
            v-model="filterForm.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            @change="handleFilterChange"
          />
        </el-form-item>

        <el-form-item>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table :data="filteredExecutions" v-loading="loading" stripe>
        <el-table-column prop="id" label="执行ID" min-width="220" />
        <el-table-column prop="workflowName" label="工作流" min-width="150" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startedAt" label="开始时间" width="170">
          <template #default="{ row }">
            {{ formatDate(row.startedAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="completedAt" label="完成时间" width="170">
          <template #default="{ row }">
            {{ formatDate(row.completedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewDetail(row)">
              <el-icon><View /></el-icon>
              查看日志
            </el-button>
            <el-button
              link
              type="danger"
              @click="cancelExecution(row.id)"
              v-if="row.status === 'RUNNING' || row.status === 'PENDING'"
            >
              <el-icon><CircleClose /></el-icon>
              取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>

      <el-empty v-if="filteredExecutions.length === 0 && !loading" description="暂无执行记录" />
    </el-card>

    <!-- Execution Detail Dialog -->
    <ExecutionDetailDialog
      v-model="detailVisible"
      :execution="selectedExecution"
      @cancelled="loadExecutions"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ExecutionDetailDialog from '@/components/execution/ExecutionDetailDialog.vue'
import { executionApi, workflowApi } from '@/services/workflow'
import type { WorkflowExecution, Workflow } from '@/types/workflow'

interface FilterForm {
  workflowId: string
  status: string
  timeRange: [string, string] | null
}

const executions = ref<WorkflowExecution[]>([])
const workflowOptions = ref<Workflow[]>([])
const loading = ref(false)
const detailVisible = ref(false)
const selectedExecution = ref<WorkflowExecution | undefined>()

const filterForm = reactive<FilterForm>({
  workflowId: '',
  status: '',
  timeRange: null,
})

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0,
})

// Client-side filtering
const filteredExecutions = computed(() => {
  let result = [...executions.value]

  // Filter by workflow
  if (filterForm.workflowId) {
    result = result.filter((e) => e.workflowId === filterForm.workflowId)
  }

  // Filter by status
  if (filterForm.status) {
    result = result.filter((e) => e.status === filterForm.status)
  }

  // Filter by time range
  if (filterForm.timeRange && filterForm.timeRange[0] && filterForm.timeRange[1]) {
    const startTime = new Date(filterForm.timeRange[0]).getTime()
    const endTime = new Date(filterForm.timeRange[1]).getTime()
    result = result.filter((e) => {
      if (!e.startedAt) return false
      const execTime = new Date(e.startedAt).getTime()
      return execTime >= startTime && execTime <= endTime
    })
  }

  // Update total
  pagination.total = result.length

  // Pagination
  const start = (pagination.page - 1) * pagination.pageSize
  const end = start + pagination.pageSize
  return result.slice(start, end)
})

const loadExecutions = async () => {
  loading.value = true
  try {
    executions.value = await executionApi.list()
    pagination.total = executions.value.length
  } catch (error: any) {
    ElMessage.error(error.message || '加载失败')
    executions.value = []
  } finally {
    loading.value = false
  }
}

const loadWorkflows = async () => {
  try {
    workflowOptions.value = await workflowApi.list()
  } catch {
    workflowOptions.value = []
  }
}

const handleFilterChange = () => {
  pagination.page = 1
}

const resetFilter = () => {
  filterForm.workflowId = ''
  filterForm.status = ''
  filterForm.timeRange = null
  pagination.page = 1
}

const handlePageChange = (page: number) => {
  pagination.page = page
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  pagination.page = 1
}

const viewDetail = (row: WorkflowExecution) => {
  selectedExecution.value = row
  detailVisible.value = true
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

const exportExecutions = () => {
  const headers = ['执行ID', '工作流ID', '工作流名称', '状态', '开始时间', '完成时间']
  const rows = executions.value.map((e) => [
    e.id,
    e.workflowId,
    e.workflowName,
    getStatusText(e.status),
    e.startedAt,
    e.completedAt,
  ])

  const csv = [headers.join(','), ...rows.map((r) => r.join(','))].join('\n')

  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `executions_${new Date().toISOString().slice(0, 10)}.csv`
  link.click()

  ElMessage.success('导出成功')
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
  loadWorkflows()
})
</script>

<style scoped>
.execution-list {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.filter-card {
  margin-bottom: 20px;
}

.table-card {
  min-height: 500px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
