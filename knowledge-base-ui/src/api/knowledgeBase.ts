import axios from 'axios'
import type { KnowledgeBase, PageResult } from '@/types'

const API_BASE = '/api'

export const knowledgeBaseApi = {
  list(page: number, size: number, keyword?: string): Promise<PageResult<KnowledgeBase>> {
    return axios.get(`${API_BASE}/knowledge-base`, {
      params: { page, size, keyword }
    }).then(res => res.data.data)
  },

  listAll(): Promise<KnowledgeBase[]> {
    return axios.get(`${API_BASE}/knowledge-base/all`).then(res => res.data.data)
  },

  getById(id: string): Promise<KnowledgeBase> {
    return axios.get(`${API_BASE}/knowledge-base/${id}`).then(res => res.data.data)
  },

  create(data: { name: string; description?: string }): Promise<KnowledgeBase> {
    return axios.post(`${API_BASE}/knowledge-base`, data).then(res => res.data.data)
  },

  update(id: string, data: { name: string; description?: string }): Promise<KnowledgeBase> {
    return axios.put(`${API_BASE}/knowledge-base/${id}`, data).then(res => res.data.data)
  },

  delete(id: string): Promise<void> {
    return axios.delete(`${API_BASE}/knowledge-base/${id}`).then(res => res.data)
  }
}
