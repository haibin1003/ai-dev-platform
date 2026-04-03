<template>
  <div class="node-palette">
    <div class="palette-header">
      <el-icon><Tools /></el-icon>
      <span>节点工具箱</span>
    </div>

    <div class="palette-content">
      <div class="palette-section">
        <div class="section-title">基础节点</div>
        <div
          v-for="node in basicNodes"
          :key="node.shape"
          class="palette-item"
          :draggable="true"
          @dragstart="onDragStart($event, node)"
        >
          <div class="item-icon" :style="{ backgroundColor: node.color }">
            <el-icon size="20">
              <component :is="node.icon" />
            </el-icon>
          </div>
          <span class="item-label">{{ node.label }}</span>
        </div>
      </div>

      <div class="palette-section">
        <div class="section-title">使用说明</div>
        <div class="usage-tips">
          <p>1. 拖拽节点到画布</p>
          <p>2. 点击锚点连接</p>
          <p>3. 双击编辑属性</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { NodeShapeConfig } from '@/types/workflow'

const basicNodes: NodeShapeConfig[] = [
  {
    shape: 'start-node',
    width: 80,
    height: 80,
    label: '开始',
    icon: 'VideoPlay',
    color: '#67c23a',
  },
  {
    shape: 'task-node',
    width: 160,
    height: 80,
    label: '任务',
    icon: 'SetUp',
    color: '#409eff',
  },
  {
    shape: 'end-node',
    width: 80,
    height: 80,
    label: '结束',
    icon: 'CircleCheck',
    color: '#f56c6c',
  },
]

const onDragStart = (e: DragEvent, node: NodeShapeConfig) => {
  if (e.dataTransfer) {
    e.dataTransfer.setData('application/x-node-shape', JSON.stringify(node))
    e.dataTransfer.effectAllowed = 'copy'
  }
}
</script>

<style scoped>
.node-palette {
  width: 200px;
  background: #fff;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.palette-header {
  height: 48px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 16px;
  border-bottom: 1px solid #e4e7ed;
  font-weight: 500;
  color: #303133;
}

.palette-content {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
}

.palette-section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 12px;
  color: #909399;
  margin-bottom: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.palette-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: grab;
  transition: all 0.2s;
  border: 1px solid #e4e7ed;
  margin-bottom: 8px;
  background: #fff;
}

.palette-item:hover {
  background: #f5f7fa;
  border-color: #c6e2ff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
}

.palette-item:active {
  cursor: grabbing;
}

.item-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.item-label {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
}

.usage-tips {
  font-size: 12px;
  color: #909399;
  line-height: 1.8;
  padding: 8px;
  background: #f5f7fa;
  border-radius: 4px;
}

.usage-tips p {
  margin: 0;
}
</style>
