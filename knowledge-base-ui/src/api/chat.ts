import { http, unwrapData } from '@/api/http'
import type { ChatSession, ChatMessage, PageResult, ChatResponse } from '@/types'
import type { ApiResponse } from '@/types'

export const chatApi = {
  sendMessage(message: string, sessionId?: string, knowledgeBaseId?: string): Promise<ChatResponse> {
    return http
      .post<ApiResponse<ChatResponse>>('/chat', {
        message,
        sessionId,
        knowledgeBaseId,
        stream: false
      })
      .then(unwrapData)
  },

  streamMessage(message: string, sessionId?: string, knowledgeBaseId?: string): {
    eventSource: EventSource
    sessionId: string
  } {
    const params = new URLSearchParams()
    params.append('message', message)
    if (sessionId) params.append('sessionId', sessionId)
    if (knowledgeBaseId) params.append('knowledgeBaseId', knowledgeBaseId)

    const eventSource = new EventSource(`/api/chat/stream?${params.toString()}`)
    return { eventSource, sessionId: sessionId || '' }
  },

  listSessions(page: number, size: number, keyword?: string): Promise<PageResult<ChatSession>> {
    return http
      .get<ApiResponse<PageResult<ChatSession>>>('/chat/sessions', {
        params: { page, size, keyword }
      })
      .then(unwrapData)
  },

  getSession(id: string): Promise<ChatSession> {
    return http.get<ApiResponse<ChatSession>>(`/chat/sessions/${id}`).then(unwrapData)
  },

  getSessionMessages(id: string): Promise<ChatMessage[]> {
    return http.get<ApiResponse<ChatMessage[]>>(`/chat/sessions/${id}/messages`).then(unwrapData)
  },

  createSession(title?: string, knowledgeBaseId?: string): Promise<ChatSession> {
    return http.post<ApiResponse<ChatSession>>('/chat/sessions', { title, knowledgeBaseId }).then(unwrapData)
  },

  deleteSession(id: string): Promise<void> {
    return http.delete<ApiResponse<null>>(`/chat/sessions/${id}`).then(() => {})
  },

  healthCheck(): Promise<void> {
    return http.get<ApiResponse<Record<string, string>>>('/health').then(() => {})
  }
}
