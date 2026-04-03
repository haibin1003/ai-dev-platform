import { describe, it, expect, beforeEach } from 'vitest'
import { ref } from 'vue'
import { useLogFilter, type LogLevel } from '../useLogFilter'
import type { LogMessage } from '../useWebSocket'

describe('useLogFilter', () => {
  const mockLogs: LogMessage[] = [
    { timestamp: '10:00:00', executionId: 'exec-1', level: 'INFO', message: 'Info message' },
    { timestamp: '10:00:01', executionId: 'exec-1', level: 'WARN', message: 'Warning message' },
    { timestamp: '10:00:02', executionId: 'exec-1', level: 'ERROR', message: 'Error message' },
    { timestamp: '10:00:03', executionId: 'exec-1', taskId: 'task-1', level: 'INFO', message: 'Task info' },
  ]

  it('should return all logs when filter is ALL', () => {
    const logs = ref([...mockLogs])
    const searchKeyword = ref('')
    const { filterLevel, filteredLogs } = useLogFilter({ logs, searchKeyword })

    filterLevel.value = 'ALL'
    expect(filteredLogs.value).toHaveLength(4)
  })

  it('should filter logs by INFO level', () => {
    const logs = ref([...mockLogs])
    const searchKeyword = ref('')
    const { filterLevel, filteredLogs } = useLogFilter({ logs, searchKeyword })

    filterLevel.value = 'INFO'
    expect(filteredLogs.value).toHaveLength(2)
    expect(filteredLogs.value.every(log => log.level === 'INFO')).toBe(true)
  })

  it('should filter logs by WARN level', () => {
    const logs = ref([...mockLogs])
    const searchKeyword = ref('')
    const { filterLevel, filteredLogs } = useLogFilter({ logs, searchKeyword })

    filterLevel.value = 'WARN'
    expect(filteredLogs.value).toHaveLength(1)
    expect(filteredLogs.value[0].level).toBe('WARN')
  })

  it('should filter logs by ERROR level', () => {
    const logs = ref([...mockLogs])
    const searchKeyword = ref('')
    const { filterLevel, filteredLogs } = useLogFilter({ logs, searchKeyword })

    filterLevel.value = 'ERROR'
    expect(filteredLogs.value).toHaveLength(1)
    expect(filteredLogs.value[0].level).toBe('ERROR')
  })

  it('should filter logs by search keyword in message', () => {
    const logs = ref([...mockLogs])
    const searchKeyword = ref('Warning')
    const { filteredLogs } = useLogFilter({ logs, searchKeyword })

    expect(filteredLogs.value).toHaveLength(1)
    expect(filteredLogs.value[0].message).toContain('Warning')
  })

  it('should filter logs by search keyword in taskId', () => {
    const logs = ref([...mockLogs])
    const searchKeyword = ref('task-1')
    const { filteredLogs } = useLogFilter({ logs, searchKeyword })

    expect(filteredLogs.value).toHaveLength(1)
    expect(filteredLogs.value[0].taskId).toBe('task-1')
  })

  it('should be case insensitive for search', () => {
    const logs = ref([...mockLogs])
    const searchKeyword = ref('WARNING')
    const { filteredLogs } = useLogFilter({ logs, searchKeyword })

    expect(filteredLogs.value).toHaveLength(1)
  })

  it('should combine level and search filters', () => {
    const logs = ref([...mockLogs])
    const searchKeyword = ref('message')
    const { filterLevel, filteredLogs } = useLogFilter({ logs, searchKeyword })

    filterLevel.value = 'INFO'
    expect(filteredLogs.value).toHaveLength(1)
    expect(filteredLogs.value[0].level).toBe('INFO')
    expect(filteredLogs.value[0].message).toContain('message')
  })

  it('should return empty array when no logs match', () => {
    const logs = ref([...mockLogs])
    const searchKeyword = ref('nonexistent')
    const { filteredLogs } = useLogFilter({ logs, searchKeyword })

    expect(filteredLogs.value).toHaveLength(0)
  })

  describe('logCounts', () => {
    it('should count logs by level correctly', () => {
      const logs = ref([...mockLogs])
      const searchKeyword = ref('')
      const { logCounts } = useLogFilter({ logs, searchKeyword })

      expect(logCounts.value.ALL).toBe(4)
      expect(logCounts.value.INFO).toBe(2)
      expect(logCounts.value.WARN).toBe(1)
      expect(logCounts.value.ERROR).toBe(1)
    })

    it('should update counts when logs change', () => {
      const logs = ref([...mockLogs])
      const searchKeyword = ref('')
      const { logCounts } = useLogFilter({ logs, searchKeyword })

      expect(logCounts.value.ALL).toBe(4)

      logs.value.push({
        timestamp: '10:00:04',
        executionId: 'exec-1',
        level: 'ERROR',
        message: 'Another error'
      })

      expect(logCounts.value.ALL).toBe(5)
      expect(logCounts.value.ERROR).toBe(2)
    })
  })

  describe('setFilterLevel', () => {
    it('should update filter level', () => {
      const logs = ref([...mockLogs])
      const searchKeyword = ref('')
      const { filterLevel, setFilterLevel } = useLogFilter({ logs, searchKeyword })

      setFilterLevel('ERROR')
      expect(filterLevel.value).toBe('ERROR')
    })
  })
})
