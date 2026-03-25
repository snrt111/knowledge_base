export interface KnowledgeBase {
  id: string
  name: string
  description: string
  documentCount: number
  createTime: string
  updateTime: string
}

export interface Document {
  id: string
  name: string
  knowledgeBaseId: string
  knowledgeBaseName: string
  type: string
  size: number
  status: 'processing' | 'completed' | 'failed'
  uploadTime: string
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

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
