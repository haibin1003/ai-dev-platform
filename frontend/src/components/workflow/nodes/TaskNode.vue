<template>
  <div class="task-node" :class="{ selected: isSelected, [status || '']: true }">
    <div class="node-header">
      <el-icon size="16" class="node-icon"><SetUp /></el-icon>
      <span class="node-type">任务</span>
      <el-icon v-if="status" size="14" class="status-icon">
        <CircleCheck v-if="status === 'completed'" />
        <CircleClose v-else-if="status === 'failed'" />
        <Loading v-else-if="status === 'running'" />
      </el-icon>
    </div>
    <div class="node-body">
      <div class="node-name">{{ label }}</div>
      <div v-if="agentName" class="node-agent">
        <el-icon size="12"><User /></el-icon>
        {{ agentName }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  label?: string
  agentName?: string
  status?: 'pending' | 'running' | 'completed' | 'failed'
  isSelected?: boolean
}>()
</script>

<style scoped>
.task-node {
  width: 160px;
  background: #fff;
  border-radius: 8px;
  border: 2px solid #dcdfe6;
  overflow: hidden;
  transition: all 0.3s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.node-header {
  height: 32px;
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  display: flex;
  align-items: center;
  padding: 0 10px;
  gap: 6px;
  color: white;
}

.node-icon {
  flex-shrink: 0;
}

.node-type {
  font-size: 12px;
  flex: 1;
}

.node-body {
  padding: 10px;
}

.node-name {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-agent {
  font-size: 11px;
  color: #909399;
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.status-icon {
  margin-left: auto;
}

.task-node.selected {
  border-color: #409eff;
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.3);
}

.task-node.running {
  border-color: #e6a23c;
}

.task-node.completed {
  border-color: #67c23a;
}

.task-node.failed {
  border-color: #f56c6c;
}
</style>
