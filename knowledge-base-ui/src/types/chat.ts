export interface ChatSession {
  id: string
  title: string
  knowledgeBaseId?: string
  knowledgeBaseName?: string
  messageCount: number
  createTime: string
  updateTime: string
}

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant' | 'system'
  content: string
  createTime: string
}

export interface ChatRequest {
  message: string
  sessionId?: string
  knowledgeBaseId?: string
  stream?: boolean
}
