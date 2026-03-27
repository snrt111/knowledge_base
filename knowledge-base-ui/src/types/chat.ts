export interface ChatSession {
  id: string
  title: string
  knowledgeBaseId?: string
  knowledgeBaseName?: string
  messageCount: number
  createTime: string
  updateTime: string
}

export interface DocumentSource {
  documentId: string
  documentName: string
  knowledgeBaseName: string
  score?: number
  snippet?: string
  snippets?: string[]
}

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant' | 'system'
  content: string
  createTime: string
  documentSources?: DocumentSource[]
}

export interface ChatRequest {
  message: string
  sessionId?: string
  knowledgeBaseId?: string
  stream?: boolean
}

export interface ChatResponse {
  content: string
  sources?: DocumentSource[]
}
