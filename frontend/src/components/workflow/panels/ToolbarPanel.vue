<template>
  <div class="toolbar-panel">
    <div class="toolbar-section">
      <el-button-group>
        <el-button type="primary" @click="$emit('save')">
          <el-icon><DocumentChecked /></el-icon>
          保存
        </el-button>
        <el-button @click="$emit('validate')">
          <el-icon><CircleCheck /></el-icon>
          验证
        </el-button>
      </el-button-group>
    </div>

    <div class="toolbar-divider" />

    <div class="toolbar-section">
      <el-button-group>
        <el-button @click="$emit('zoomIn')">
          <el-icon><ZoomIn /></el-icon>
        </el-button>
        <el-button @click="$emit('zoomOut')">
          <el-icon><ZoomOut /></el-icon>
        </el-button>
        <el-button @click="$emit('fit')">
          <el-icon><FullScreen /></el-icon>
          适应
        </el-button>
      </el-button-group>
    </div>

    <div class="toolbar-divider" />

    <div class="toolbar-section">
      <el-button-group>
        <el-button @click="$emit('undo')" :disabled="!canUndo">
          <el-icon><Back /></el-icon>
          撤销
        </el-button>
        <el-button @click="$emit('redo')" :disabled="!canRedo">
          <el-icon><Right /></el-icon>
          重做
        </el-button>
      </el-button-group>
    </div>

    <div class="toolbar-divider" />

    <div class="toolbar-section">
      <el-button-group>
        <el-button @click="$emit('export')">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
        <el-button @click="$emit('clear')">
          <el-icon><Delete /></el-icon>
          清空
        </el-button>
      </el-button-group>
    </div>

    <div class="toolbar-spacer" />

    <div class="toolbar-section">
      <el-tag :type="statusType">{{ statusText }}</el-tag>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  canUndo?: boolean
  canRedo?: boolean
  status?: 'unsaved' | 'saved' | 'modified'
}>()

defineEmits<{
  save: []
  validate: []
  zoomIn: []
  zoomOut: []
  fit: []
  undo: []
  redo: []
  export: []
  clear: []
}>()

const statusMap = {
  unsaved: { type: 'warning' as const, text: '未保存' },
  saved: { type: 'success' as const, text: '已保存' },
  modified: { type: 'danger' as const, text: '已修改' },
}

const statusType = computed(() => statusMap[props.status || 'unsaved'].type)
const statusText = computed(() => statusMap[props.status || 'unsaved'].text)
</script>

<script lang="ts">
import { computed } from 'vue'
</script>

<style scoped>
.toolbar-panel {
  height: 48px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 12px;
}

.toolbar-section {
  display: flex;
  align-items: center;
}

.toolbar-divider {
  width: 1px;
  height: 24px;
  background: #dcdfe6;
}

.toolbar-spacer {
  flex: 1;
}
</style>
