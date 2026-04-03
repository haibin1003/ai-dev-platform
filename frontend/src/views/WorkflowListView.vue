<template>
  <div class="workflow-list">
    <div class="page-header">
      <h2>工作流列表</h2>
      <el-button type="primary" @click="createWorkflow">
        <el-icon><Plus /></el-icon>
        创建工作流
      </el-button>
    </div>

    <el-card class="table-card">
      <el-table :data="workflows" v-loading="loading" stripe>
        <el-table-column prop="name" label="名称" min-width="200">
          <template #default="{ row }">
            <el-link type="primary" @click="editWorkflow(row.id)">{{ row.name }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="executeWorkflow(row.id)">
              <el-icon><VideoPlay /></el-icon>
              执行
            </el-button>
            <el-button link type="primary" @click="editWorkflow(row.id)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button link type="danger" @click="deleteWorkflow(row.id)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="workflows.length === 0 && !loading" description="暂无工作流" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { workflowApi } from '@/services/workflow'
import type { Workflow } from '@/types/workflow'

const router = useRouter()

const workflows = ref<Workflow[]>([])
const loading = ref(false)

const loadWorkflows = async () => {
  loading.value = true
  try {
    workflows.value = await workflowApi.list()
  } catch (error: any) {
    ElMessage.error(error.message || '加载失败')
    workflows.value = []
  } finally {
    loading.value = false
  }
}

const createWorkflow = () => {
  router.push('/workflows/create')
}

const editWorkflow = (id: string) => {
  router.push(`/workflows/${id}/edit`)
}

const executeWorkflow = async (id: string) => {
  try {
    const executionId = await workflowApi.execute(id)
    ElMessage.success(`工作流执行已启动，执行ID: ${executionId}`)
    router.push('/executions')
  } catch (error: any) {
    ElMessage.error(error.message || '执行失败')
  }
}

const deleteWorkflow = async (id: string) => {
  try {
    await ElMessageBox.confirm('确定要删除这个工作流吗？', '确认删除', {
      type: 'warning',
    })
    await workflowApi.delete(id)
    ElMessage.success('删除成功')
    loadWorkflows()
  } catch {
    // 取消删除
  }
}

const getStatusType = (status?: string) => {
  const map: Record<string, string> = {
    DRAFT: 'info',
    PUBLISHED: 'success',
    ARCHIVED: 'danger',
  }
  return map[status || 'DRAFT'] || 'info'
}

const getStatusText = (status?: string) => {
  const map: Record<string, string> = {
    DRAFT: '草稿',
    PUBLISHED: '已发布',
    ARCHIVED: '已归档',
  }
  return map[status || 'DRAFT'] || status
}

const formatDate = (date?: string) => {
  if (!date) return '-'
  return new Date(date).toLocaleString()
}

onMounted(() => {
  loadWorkflows()
})
</script>

<style scoped>
.workflow-list {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.table-card {
  min-height: 500px;
}
</style>
