import type {
  ChatMessage,
  ChatRecordFilterOptions,
  ChatRecordQuery,
  ChatSessionRecordPage,
  ChatSessionsResponse,
  GetSessionsParams,
} from './types'
import { http } from '@/http/request/alova'

/**
 * 获取聊天会话列表
 * @param agentId 智能体ID
 * @param params 分页参数
 */
export function getChatSessions(agentId: string, params: GetSessionsParams) {
  return http.Get<ChatSessionsResponse>(`/agent/${agentId}/sessions`, {
    params,
    meta: {
      ignoreAuth: false,
      toast: false,
    },
    cacheFor: {
      expire: 0,
    },
  })
}

/**
 * 跨智能体多条件分页查询会话（按设备/智能体/角色/时间/关键词筛选）
 * @param params 查询参数，时间为 epoch 毫秒
 */
export function getChatSessionRecords(params: ChatRecordQuery) {
  return http.Get<ChatSessionRecordPage>('/agent/chat-history/sessions', {
    params,
    meta: {
      ignoreAuth: false,
      toast: false,
    },
    cacheFor: {
      expire: 0,
    },
  })
}

/**
 * 获取会话记录筛选下拉选项（智能体 + 设备）
 */
export function getChatRecordFilterOptions() {
  return http.Get<ChatRecordFilterOptions>('/agent/chat-history/filter-options', {
    meta: {
      ignoreAuth: false,
      toast: false,
    },
    cacheFor: {
      expire: 0,
    },
  })
}

/**
 * 获取会话记录详情
 * @param agentId 智能体ID
 * @param sessionId 会话ID
 */
export function getChatHistory(agentId: string, sessionId: string) {
  return http.Get<ChatMessage[]>(`/agent/${agentId}/chat-history/${sessionId}`, {
    meta: {
      ignoreAuth: false,
      toast: false,
    },
    cacheFor: {
      expire: -1,
    },
  })
}

/**
 * 获取音频下载ID
 * @param audioId 音频ID
 */
export function getAudioId(audioId: string) {
  return http.Post<string>(`/agent/audio/${audioId}`, {}, {
    meta: {
      ignoreAuth: false,
      toast: false,
    },
  })
}

/**
 * 获取音频播放地址
 * @param downloadId 下载ID
 */
export function getAudioPlayUrl(downloadId: string) {
  // 根据需求文档，这个是直接返回二进制的，所以我们直接构造URL
  return `/agent/play/${downloadId}`
}
