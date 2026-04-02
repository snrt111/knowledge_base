import http from './http'
import type { Permission, PageResult } from '@/types/user'

export const getPermissionTree = () => {
  return http.get<Permission[]>('/permissions/tree')
}

export const getPermissionPage = (params?: {
  name?: string
  code?: string
  type?: string
  pageNum?: number
  pageSize?: number
}) => {
  return http.get<PageResult<Permission>>('/permissions', { params })
}

export const getPermissionById = (id: string) => {
  return http.get<Permission>(`/permissions/${id}`)
}

export const createPermission = (data: Permission) => {
  return http.post<Permission>('/permissions', data)
}

export const updatePermission = (id: string, data: Permission) => {
  return http.put<Permission>(`/permissions/${id}`, data)
}

export const deletePermission = (id: string) => {
  return http.delete(`/permissions/${id}`)
}

export const togglePermission = (id: string) => {
  return http.put(`/permissions/${id}/toggle`)
}
