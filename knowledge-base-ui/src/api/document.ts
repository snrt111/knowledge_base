import { http, unwrapData } from '@/api/http'
import type { Document, DocumentPreview, PageResult } from '@/types'
import type { ApiResponse } from '@/types'

export const documentApi = {
  list(page: number, size: number, knowledgeBaseId?: string, keyword?: string): Promise<PageResult<Document>> {
    return http
      .get<ApiResponse<PageResult<Document>>>('/document', {
        params: { page, size, knowledgeBaseId, keyword }
      })
      .then(unwrapData)
  },

  getById(id: string): Promise<Document> {
    return http.get<ApiResponse<Document>>(`/document/${id}`).then(unwrapData)
  },

  listByKnowledgeBase(kbId: string): Promise<Document[]> {
    return http.get<ApiResponse<Document[]>>(`/document/knowledge-base/${kbId}`).then(unwrapData)
  },

  upload(file: File, knowledgeBaseId: string): Promise<Document> {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('knowledgeBaseId', knowledgeBaseId)
    return http
      .post<ApiResponse<Document>>('/document/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      .then(unwrapData)
  },

  delete(id: string): Promise<void> {
    return http.delete<ApiResponse<null>>(`/document/${id}`).then(() => {})
  },

  /** 不重新上传文件，仅对已存储的文档再次向量化入库 */
  reprocess(id: string): Promise<Document> {
    return http.post<ApiResponse<Document>>(`/document/${id}/reprocess`).then(unwrapData)
  },

  preview(id: string): Promise<DocumentPreview> {
    return http.get<ApiResponse<DocumentPreview>>(`/document/${id}/preview`).then(unwrapData)
  },

  download(id: string): string {
    return `/api/document/${id}/download`
  }
}
