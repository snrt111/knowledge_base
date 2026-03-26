import axios from 'axios'
import type { ChatSession, ChatMessage, PageResult } from '@/types'

const API_BASE = '/api/chat'

export const chatApi = {
  sendMessage(message: string, sessionId?: string, knowledgeBaseId?: string): Promise<string> {
    return axios.post(API_BASE, {
      message,
      sessionId,
      knowledgeBaseId,
      stream: false
    }).then(res => res.data.data)
  },

  streamMessage(message: string, sessionId?: string, knowledgeBaseId?: string): {
    eventSource: EventSource;
    sessionId: string;
  } {
    const params = new URLSearchParams()
    params.append('message', message)
    if (sessionId) params.append('sessionId', sessionId)
    if (knowledgeBaseId) params.append('knowledgeBaseId', knowledgeBaseId)

    const eventSource = new EventSource(`${API_BASE}/stream?${params.toString()}`)
    return { eventSource, sessionId: sessionId || '' }
  },

  listSessions(page: number, size: number, keyword?: string): Promise<PageResult<ChatSession>> {
    return axios.get(`${API_BASE}/sessions`, {
      params: { page, size, keyword }
    }).then(res => res.data.data)
  },

  getSession(id: string): Promise<ChatSession> {
    return axios.get(`${API_BASE}/sessions/${id}`).then(res => res.data.data)
  },

  getSessionMessages(id: string): Promise<ChatMessage[]> {
    return axios.get(`${API_BASE}/sessions/${id}/messages`).then(res => res.data.data)
  },

  createSession(title?: string, knowledgeBaseId?: string): Promise<ChatSession> {
    return axios.post(`${API_BASE}/sessions`, { title, knowledgeBaseId }).then(res => res.data.data)
  },

  deleteSession(id: string): Promise<void> {
    return axios.delete(`${API_BASE}/sessions/${id}`).then(res => res.data)
  },

  healthCheck(): Promise<void> {
    return axios.get('/api/health').then(() => {})
  }
}
