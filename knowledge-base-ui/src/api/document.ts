import axios from 'axios'
import type { Document, PageResult } from '@/types'

const API_BASE = '/api'

export const documentApi = {
  list(page: number, size: number, knowledgeBaseId?: string, keyword?: string): Promise<PageResult<Document>> {
    return axios.get(`${API_BASE}/document`, {
      params: { page, size, knowledgeBaseId, keyword }
    }).then(res => res.data.data)
  },

  getById(id: string): Promise<Document> {
    return axios.get(`${API_BASE}/document/${id}`).then(res => res.data.data)
  },

  listByKnowledgeBase(kbId: string): Promise<Document[]> {
    return axios.get(`${API_BASE}/document/knowledge-base/${kbId}`).then(res => res.data.data)
  },

  upload(file: File, knowledgeBaseId: string): Promise<Document> {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('knowledgeBaseId', knowledgeBaseId)
    return axios.post(`${API_BASE}/document/upload`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    }).then(res => res.data.data)
  },

  delete(id: string): Promise<void> {
    return axios.delete(`${API_BASE}/document/${id}`).then(res => res.data)
  }
}
