import { ref, computed, type Ref } from 'vue'
import type { LogMessage } from './useWebSocket'

export type LogLevel = 'INFO' | 'WARN' | 'ERROR' | 'ALL'

export interface UseLogFilterOptions {
  logs: Ref<LogMessage[]>
  searchKeyword: Ref<string>
}

export interface UseLogFilterReturn {
  filterLevel: Ref<LogLevel>
  filteredLogs: Ref<LogMessage[]>
  logCounts: Ref<Record<string, number>>
  setFilterLevel: (level: LogLevel) => void
  clearSearch: () => void
}

export function useLogFilter(options: UseLogFilterOptions): UseLogFilterReturn {
  const filterLevel = ref<LogLevel>('ALL')

  const filteredLogs = computed(() => {
    let result = options.logs.value

    // Filter by level
    if (filterLevel.value !== 'ALL') {
      result = result.filter((log) => log.level === filterLevel.value)
    }

    // Filter by search keyword
    const keyword = options.searchKeyword.value.trim()
    if (keyword) {
      const lowerKeyword = keyword.toLowerCase()
      result = result.filter(
        (log) =>
          log.message.toLowerCase().includes(lowerKeyword) ||
          log.taskId?.toLowerCase().includes(lowerKeyword)
      )
    }

    return result
  })

  const logCounts = computed(() => {
    const counts: Record<string, number> = {
      ALL: options.logs.value.length,
      INFO: 0,
      WARN: 0,
      ERROR: 0,
    }

    options.logs.value.forEach((log) => {
      const key = log.level as LogLevel | 'ALL'
      if (key !== 'ALL' && counts[key] !== undefined) {
        counts[key]!++
      }
    })

    return counts
  })

  const setFilterLevel = (level: LogLevel) => {
    filterLevel.value = level
  }

  const clearSearch = () => {
    filterLevel.value = 'ALL'
  }

  return {
    filterLevel,
    filteredLogs,
    logCounts,
    setFilterLevel,
    clearSearch,
  }
}
