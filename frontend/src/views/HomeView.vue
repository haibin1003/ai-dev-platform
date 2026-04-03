<template>
  <div class="dashboard">
    <h2>Dashboard</h2>

    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon blue">
            <el-icon size="40"><Share /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.workflowCount }}</div>
            <div class="stat-label">工作流总数</div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon green">
            <el-icon size="40"><VideoPlay /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.runningExecutions }}</div>
            <div class="stat-label">运行中</div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon orange">
            <el-icon size="40"><CircleCheck /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.completedExecutions }}</div>
            <div class="stat-label">已完成</div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon red">
            <el-icon size="40"><CircleClose /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.failedExecutions }}</div>
            <div class="stat-label">失败</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="content-row">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>最近执行</span>
          </template>
          <el-empty v-if="recentExecutions.length === 0" description="暂无执行记录" />
          <el-timeline v-else>
            <el-timeline-item
              v-for="execution in recentExecutions"
              :key="execution.id"
              :type="getTimelineType(execution.status)"
              :timestamp="formatDate(execution.startedAt)"
            >
              {{ execution.workflowName }} - {{ execution.status }}
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>
            <span>快速操作</span>
          </template>
          <div class="quick-actions">
            <el-button type="primary" @click="goToWorkflows">
              <el-icon><Plus /></el-icon>
              创建工作流
            </el-button>
            <el-button @click="goToExecutions">
              <el-icon><List /></el-icon>
              查看执行记录
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const stats = ref({
  workflowCount: 0,
  runningExecutions: 0,
  completedExecutions: 0,
  failedExecutions: 0,
})

const recentExecutions = ref<any[]>([])

const loadStats = async () => {
  // TODO: 调用后端API
  stats.value = {
    workflowCount: 5,
    runningExecutions: 2,
    completedExecutions: 15,
    failedExecutions: 1,
  }

  recentExecutions.value = [
    { id: '1', workflowName: '示例工作流', status: 'COMPLETED', startedAt: '2026-04-03T10:00:00' },
    { id: '2', workflowName: '测试工作流', status: 'RUNNING', startedAt: '2026-04-03T09:30:00' },
  ]
}

const goToWorkflows = () => {
  router.push('/workflows')
}

const goToExecutions = () => {
  router.push('/executions')
}

const getTimelineType = (status: string) => {
  const map: Record<string, any> = {
    COMPLETED: 'success',
    FAILED: 'danger',
    RUNNING: 'primary',
    CANCELLED: 'warning',
  }
  return map[status] || 'info'
}

const formatDate = (date: string) => {
  return new Date(date).toLocaleString()
}

onMounted(() => {
  loadStats()
})
</script>

<style scoped>
.dashboard {
  padding: 20px;
}

h2 {
  margin-bottom: 20px;
  color: #303133;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 10px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
}

.stat-icon.blue {
  background-color: #ecf5ff;
  color: #409eff;
}

.stat-icon.green {
  background-color: #f0f9eb;
  color: #67c23a;
}

.stat-icon.orange {
  background-color: #fdf6ec;
  color: #e6a23c;
}

.stat-icon.red {
  background-color: #fef0f0;
  color: #f56c6c;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
}

.content-row {
  margin-top: 20px;
}

.quick-actions {
  display: flex;
  gap: 15px;
}
</style>
