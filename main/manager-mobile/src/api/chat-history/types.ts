// 聊天会话列表项
export interface ChatSession {
  sessionId: string
  createdAt: string
  chatCount: number
  title: string
}

// 聊天会话列表响应
export interface ChatSessionsResponse {
  total: number
  list: ChatSession[]
}

// 聊天消息
export interface ChatMessage {
  createdAt: string
  chatType: 1 | 2 | 3 // 1是用户，2是AI，3是参数说明
  content: string
  audioId: string | null
  macAddress: string
}

// 用户消息内容（需要解析JSON）
export interface UserMessageContent {
  speaker: string
  content: string
}

// 获取聊天会话列表参数
export interface GetSessionsParams {
  page: number
  limit: number
}

// 跨智能体会话记录查询参数（对应后端 ChatRecordQueryDTO，时间为 epoch 毫秒）
export interface ChatRecordQuery {
  page: number
  limit: number
  macAddress?: string
  agentId?: string
  keyword?: string
  chatType?: 1 | 2 | 3
  startTime?: number
  endTime?: number
}

// 跨智能体会话列表项（对应后端 ChatSessionVO，时间文本由服务端按中国时间格式化）
export interface ChatSessionRecord {
  sessionId: string
  sessionTitle: string | null
  macAddress: string | null
  deviceAlias: string | null
  agentId: string
  agentName: string | null
  startedAtMs: number | null
  startedAtText: string | null
  createdAtMs: number | null
  createdAtText: string | null
  chatCount: number
  summary: string | null
  hasAudio: boolean
  ownerUsername?: string | null
  ownerNickname?: string | null
  isOwn?: boolean | null
}

export interface ChatSessionRecordPage {
  total: number
  list: ChatSessionRecord[]
}

// 筛选下拉选项（对应后端 ChatRecordFilterVO）
export interface ChatFilterAgentOption {
  agentId: string
  agentName: string
  ownerUsername?: string | null
  ownerNickname?: string | null
  isOwn?: boolean | null
}

export interface ChatFilterDeviceOption {
  macAddress: string
  alias: string | null
  agentId: string
  ownerUsername?: string | null
  ownerNickname?: string | null
  isOwn?: boolean | null
}

export interface ChatRecordFilterOptions {
  agents: ChatFilterAgentOption[]
  devices: ChatFilterDeviceOption[]
}

// 音频播放相关
export interface AudioResponse {
  data: string // 音频下载ID
}
