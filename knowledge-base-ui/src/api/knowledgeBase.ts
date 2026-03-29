import { http, unwrapData } from '@/api/http'
import type { KnowledgeBase, PageResult } from '@/types'
import type { ApiResponse } from '@/types'

export const knowledgeBaseApi = {
  list(page: number, size: number, keyword?: string): Promise<PageResult<KnowledgeBase>> {
    return http
      .get<ApiResponse<PageResult<KnowledgeBase>>>('/knowledge-base', {
        params: { page, size, keyword }
      })
      .then(unwrapData)
  },

  listAll(): Promise<KnowledgeBase[]> {
    return http.get<ApiResponse<KnowledgeBase[]>>('/knowledge-base/all').then(unwrapData)
  },

  getById(id: string): Promise<KnowledgeBase> {
    return http.get<ApiResponse<KnowledgeBase>>(`/knowledge-base/${id}`).then(unwrapData)
  },

  create(data: { name: string; description?: string }): Promise<KnowledgeBase> {
    return http.post<ApiResponse<KnowledgeBase>>('/knowledge-base', data).then(unwrapData)
  },

  update(id: string, data: { name: string; description?: string }): Promise<KnowledgeBase> {
    return http.put<ApiResponse<KnowledgeBase>>(`/knowledge-base/${id}`, data).then(unwrapData)
  },

  delete(id: string): Promise<void> {
    return http.delete<ApiResponse<null>>(`/knowledge-base/${id}`).then(() => {})
  }
}
