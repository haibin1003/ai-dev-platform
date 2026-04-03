<template>
  <div class="end-node" :class="{ selected: isSelected }">
    <div class="node-circle">
      <el-icon size="20"><CircleCheck /></el-icon>
    </div>
    <div class="node-label">{{ label }}</div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  label?: string
  isSelected?: boolean
}>()
</script>

<style scoped>
.end-node {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 8px;
}

.node-circle {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 2px 8px rgba(245, 108, 108, 0.3);
  border: 3px solid #fff;
  transition: all 0.3s;
}

.node-label {
  font-size: 12px;
  color: #606266;
  white-space: nowrap;
}

.selected .node-circle {
  box-shadow: 0 0 0 3px #409eff, 0 4px 12px rgba(245, 108, 108, 0.4);
  transform: scale(1.05);
}
</style>
