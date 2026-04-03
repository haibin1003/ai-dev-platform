import { ref, onMounted, onUnmounted, type Ref } from 'vue'
import { Client, type IFrame, type IMessage, type StompSubscription } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

const MAX_LOGS = 10000
const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws/logs'

export interface LogMessage {
  timestamp: string
  executionId: string
  taskId?: string
  level: 'INFO' | 'WARN' | 'ERROR'
  message: string
}

export interface StatusMessage {
  timestamp: string
  executionId: string
  status: string
}

export interface UseWebSocketOptions {
  executionId: string
  onLogMessage?: (log: LogMessage) => void
  onStatusMessage?: (status: StatusMessage) => void
  onConnect?: () => void
  onDisconnect?: () => void
  onError?: (error: Error) => void
}

export interface UseWebSocketReturn {
  isConnected: Ref<boolean>
  isConnecting: Ref<boolean>
  isReconnecting: Ref<boolean>
  logs: Ref<LogMessage[]>
  executionStatus: Ref<string | null>
  connect: () => void
  disconnect: () => void
  clearLogs: () => void
}

export function useWebSocket(options: UseWebSocketOptions): UseWebSocketReturn {
  const client = ref<Client | null>(null)
  const isConnected = ref(false)
  const isConnecting = ref(false)
  const isReconnecting = ref(false)
  const logs = ref<LogMessage[]>([])
  const executionStatus = ref<string | null>(null)

  let logSubscription: StompSubscription | null = null
  let statusSubscription: StompSubscription | null = null

  const createClient = (): Client => {
    return new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,

      onConnect: (frame: IFrame) => {
        isConnected.value = true
        isConnecting.value = false
        isReconnecting.value = false

        // Subscribe to execution logs
        logSubscription = client.value?.subscribe(
          `/topic/executions/${options.executionId}/logs`,
          (message: IMessage) => {
            try {
              const log: LogMessage = JSON.parse(message.body)
              // Limit log size to prevent memory issues
              if (logs.value.length >= MAX_LOGS) {
                logs.value.shift()
              }
              logs.value.push(log)
              options.onLogMessage?.(log)
            } catch (e) {
              console.error('Failed to parse log message:', e)
              options.onError?.(new Error('Failed to parse log message'))
            }
          }
        ) || null

        // Subscribe to execution status
        statusSubscription = client.value?.subscribe(
          `/topic/executions/${options.executionId}/status`,
          (message: IMessage) => {
            try {
              const status: StatusMessage = JSON.parse(message.body)
              executionStatus.value = status.status
              options.onStatusMessage?.(status)
            } catch (e) {
              console.error('Failed to parse status message:', e)
              options.onError?.(new Error('Failed to parse status message'))
            }
          }
        ) || null

        options.onConnect?.()
      },

      onDisconnect: () => {
        isConnected.value = false
        isConnecting.value = false
        options.onDisconnect?.()
      },

      onStompError: (frame: IFrame) => {
        console.error('STOMP error:', frame.headers['message'])
        options.onError?.(new Error(frame.headers['message'] || 'STOMP error'))
      },

      onWebSocketError: (event: Event) => {
        console.error('WebSocket error:', event)
        isConnecting.value = false
        options.onError?.(new Error('WebSocket connection error'))
      },

      onWebSocketClose: () => {
        isConnected.value = false
        isConnecting.value = false
        // Note: STOMP client handles reconnection automatically
      },
    })
  }

  const connect = () => {
    if (client.value?.active || isConnecting.value) {
      return
    }

    isConnecting.value = true
    client.value = createClient()
    client.value.activate()
  }

  const disconnect = () => {
    // Unsubscribe from topics
    logSubscription?.unsubscribe()
    statusSubscription?.unsubscribe()
    logSubscription = null
    statusSubscription = null

    if (client.value?.active) {
      client.value.deactivate()
    }
    client.value = null
    isConnected.value = false
    isConnecting.value = false
    isReconnecting.value = false
  }

  const clearLogs = () => {
    logs.value = []
  }

  onMounted(() => {
    connect()
  })

  onUnmounted(() => {
    disconnect()
  })

  return {
    isConnected,
    isConnecting,
    isReconnecting,
    logs,
    executionStatus,
    connect,
    disconnect,
    clearLogs,
  }
}
