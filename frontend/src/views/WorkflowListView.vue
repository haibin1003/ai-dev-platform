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
            <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="executeWorkflow(row.id)" :disabled="row.status !== 'ACTIVE'">
              执行
            </el-button>
            <el-button link type="primary" @click="editWorkflow(row.id)">编辑</el-button>
            <el-button link type="success" @click="activateWorkflow(row.id)" v-if="row.status === 'DRAFT'">
              激活
            </el-button>
            <el-button link type="danger" @click="deleteWorkflow(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadWorkflows"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()

interface Workflow {
  id: string
  name: string
  status: string
  createdAt: string
}

const workflows = ref<Workflow[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)

const loadWorkflows = async () => {
  loading.value = true
  // TODO: 调用后端API
  setTimeout(() => {
    workflows.value = [
      { id: '1', name: '示例工作流', status: 'DRAFT', createdAt: '2026-04-03T10:00:00' },
    ]
    total.value = 1
    loading.value = false
  }, 500)
}

const createWorkflow = () => {
  router.push('/workflows/create')
}

const editWorkflow = (id: string) => {
  router.push(`/workflows/${id}/edit`)
}

const executeWorkflow = async (id: string) => {
  // TODO: 调用后端API
  ElMessage.success('工作流执行已启动')
}

const activateWorkflow = async (id: string) => {
  // TODO: 调用后端API
  ElMessage.success('工作流已激活')
  loadWorkflows()
}

const deleteWorkflow = async (id: string) => {
  try {
    await ElMessageBox.confirm('确定要删除这个工作流吗？', '确认删除', {
      type: 'warning',
    })
    // TODO: 调用后端API
    ElMessage.success('删除成功')
    loadWorkflows()
  } catch {
    // 取消删除
  }
}

const getStatusType = (status: string) => {
  const map: Record<string, string> = {
    DRAFT: 'info',
    ACTIVE: 'success',
    ARCHIVED: 'danger',
  }
  return map[status] || 'info'
}

const formatDate = (date: string) => {
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

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
