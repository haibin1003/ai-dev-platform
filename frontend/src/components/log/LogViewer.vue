<template>
  <div class="log-viewer">
    <!-- Toolbar -->
    <div class="log-toolbar">
      <div class="toolbar-left">
        <el-radio-group v-model="filterLevel" size="small">
          <el-radio-button label="ALL">
            全部 ({{ logCounts.ALL }})
          </el-radio-button>
          <el-radio-button label="INFO">
            <el-icon color="#409eff"><Info-Filled /></el-icon>
            信息 ({{ logCounts.INFO }})
          </el-radio-button>
          <el-radio-button label="WARN">
            <el-icon color="#e6a23c"><Warning /></el-icon>
            警告 ({{ logCounts.WARN }})
          </el-radio-button>
          <el-radio-button label="ERROR">
            <el-icon color="#f56c6c"><Circle-Close /></el-icon>
            错误 ({{ logCounts.ERROR }})
          </el-radio-button>
        </el-radio-group>
      </div>

      <div class="toolbar-center">
        <el-input
          :model-value="searchInput"
          placeholder="搜索日志..."
          size="small"
          clearable
          style="width: 200px"
          @update:model-value="debounceSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>

      <div class="toolbar-right">
        <el-button
          :type="autoScroll ? 'primary' : 'default'"
          size="small"
          @click="autoScroll = !autoScroll"
        >
          <el-icon><Bottom /></el-icon>
          {{ autoScroll ? '自动滚动中' : '已暂停' }}
        </el-button>

        <el-button size="small" @click="clearLogs">
          <el-icon><Delete /></el-icon>
          清空
        </el-button>

        <el-button size="small" @click="copyLogs">
          <el-icon><Document-Copy /></el-icon>
          复制
        </el-button>
      </div>
    </div>

    <!-- Connection Status -->
    <div
      v-if="!isConnected || isConnecting"
      class="connection-status"
      :class="{ reconnecting: isReconnecting || isConnecting }"
    >
      <el-icon v-if="isReconnecting || isConnecting" class="is-loading"><Loading /></el-icon>
      <el-icon v-else><Warning /></el-icon>
      <span>{{ connectionStatusText }}</span>
    </div>

    <!-- Log Container -->
    <div ref="logContainer" class="log-container" @scroll="handleScroll">
      <div v-if="filteredLogs.length === 0" class="empty-logs">
        <el-empty description="暂无日志" :image-size="60" />
      </div>

      <div
        v-for="(log, index) in filteredLogs"
        :key="index"
        class="log-line"
        :class="['level-' + log.level.toLowerCase()]"
      >
        <span class="log-timestamp">{{ log.timestamp }}</span>
        <span class="log-level" :class="'level-' + log.level.toLowerCase()">{{ log.level }}</span>
        <span v-if="log.taskId" class="log-task-id">[{{ log.taskId }}]</span>
        <span class="log-message"><HighlightedText :text="log.message" :keyword="searchKeyword" /></span>
      </div>
    </div>

    <!-- Status Bar -->
    <div class="log-status-bar">
      <span>显示 {{ filteredLogs.length }} / {{ logs.length }} 条日志</span>
      <span v-if="executionStatus" class="execution-status" :class="executionStatus.toLowerCase()">
        执行状态: {{ executionStatus }}
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick, computed, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useWebSocket, type LogMessage } from '@/composables/useWebSocket'
import { useLogFilter } from '@/composables/useLogFilter'
import HighlightedText from './HighlightedText.vue'

const props = defineProps<{
  executionId: string
}>()

const logContainer = ref<HTMLElement>()
const autoScroll = ref(true)
const userScrolled = ref(false)

// Debounced search keyword
const searchInput = ref('')
const searchKeyword = ref('')

let searchDebounceTimer: number | null = null
const debounceSearch = (value: string) => {
  searchInput.value = value
  if (searchDebounceTimer) {
    clearTimeout(searchDebounceTimer)
  }
  searchDebounceTimer = window.setTimeout(() => {
    searchKeyword.value = value
  }, 300)
}

// WebSocket connection
const {
  isConnected,
  isReconnecting,
  isConnecting,
  logs,
  executionStatus,
  clearLogs: clearWebSocketLogs,
} = useWebSocket({
  executionId: props.executionId,
  onLogMessage: () => {
    if (autoScroll.value && !userScrolled.value) {
      scrollToBottom()
    }
  },
})

// Log filtering
const {
  filterLevel,
  filteredLogs,
  logCounts,
  setFilterLevel,
  clearSearch: clearLogFilter,
} = useLogFilter({
  logs,
  searchKeyword,
})

const connectionStatusText = computed(() => {
  if (isConnecting.value) return '正在连接...'
  if (isReconnecting.value) return '正在重新连接...'
  if (!isConnected.value) return '连接已断开'
  return '已连接'
})

const scrollToBottom = () => {
  nextTick(() => {
    if (logContainer.value) {
      logContainer.value.scrollTop = logContainer.value.scrollHeight
    }
  })
}

const handleScroll = () => {
  if (!logContainer.value) return

  const { scrollTop, scrollHeight, clientHeight } = logContainer.value
  const isAtBottom = scrollHeight - scrollTop - clientHeight < 20

  userScrolled.value = !isAtBottom
  if (isAtBottom) {
    userScrolled.value = false
  }
}

const clearLogs = () => {
  clearWebSocketLogs()
  searchInput.value = ''
  searchKeyword.value = ''
  filterLevel.value = 'ALL'
  ElMessage.success('日志已清空')
}

const copyLogs = async () => {
  const text = filteredLogs.value
    .map((log) => `${log.timestamp} [${log.level}] ${log.taskId ? `[${log.taskId}] ` : ''}${log.message}`)
    .join('\n')

  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('日志已复制到剪贴板')
  } catch (err) {
    console.error('Copy failed:', err)
    ElMessage.error('复制失败，请检查剪贴板权限')
  }
}

// Cleanup timer on unmount
onUnmounted(() => {
  if (searchDebounceTimer) {
    clearTimeout(searchDebounceTimer)
  }
})

// Watch for new logs and auto-scroll
watch(
  () => logs.value.length,
  () => {
    if (autoScroll.value && !userScrolled.value) {
      scrollToBottom()
    }
  }
)
</script>

<style scoped>
.log-viewer {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #1e1e1e;
  border-radius: 4px;
  overflow: hidden;
}

.log-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #2d2d2d;
  border-bottom: 1px solid #3d3d3d;
  gap: 12px;
}

.toolbar-left,
.toolbar-center,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.connection-status {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 6px 12px;
  background: #fef0f0;
  color: #f56c6c;
  font-size: 12px;
}

.connection-status.reconnecting {
  background: #fdf6ec;
  color: #e6a23c;
}

.log-container {
  flex: 1;
  overflow-y: auto;
  padding: 8px 12px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
}

.empty-logs {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
}

.log-line {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 2px 0;
  white-space: pre-wrap;
  word-break: break-all;
}

.log-line:hover {
  background: rgba(255, 255, 255, 0.05);
}

.log-timestamp {
  color: #858585;
  min-width: 85px;
  flex-shrink: 0;
}

.log-level {
  min-width: 50px;
  text-align: center;
  font-weight: bold;
  flex-shrink: 0;
  padding: 0 4px;
  border-radius: 2px;
}

.log-level.level-info {
  color: #409eff;
}

.log-level.level-warn {
  color: #e6a23c;
}

.log-level.level-error {
  color: #f56c6c;
}

.log-task-id {
  color: #67c23a;
  flex-shrink: 0;
}

.log-message {
  color: #d4d4d4;
  flex: 1;
}

.log-line.level-error .log-message {
  color: #f56c6c;
}

.log-status-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 12px;
  background: #2d2d2d;
  border-top: 1px solid #3d3d3d;
  font-size: 12px;
  color: #858585;
}

.execution-status {
  font-weight: bold;
}

.execution-status.running {
  color: #409eff;
}

.execution-status.completed {
  color: #67c23a;
}

.execution-status.failed {
  color: #f56c6c;
}

:deep(.log-highlight) {
  background: #e6a23c;
  color: #000;
  padding: 0 2px;
  border-radius: 2px;
}
</style>
