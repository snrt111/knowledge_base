import http from './http'
import type { ApiResponse, PageResult } from '@/types'
import type { Permission } from '@/types/user'

export const getPermissionTree = async () => {
  const response = await http.get<ApiResponse<Permission[]>>('/permissions/tree')
  return response.data.data
}

export const getPermissionPage = async (params?: {
  name?: string
  code?: string
  type?: string
  pageNum?: number
  pageSize?: number
}) => {
  const response = await http.get<ApiResponse<PageResult<Permission>>>('/permissions', { params })
  return response.data.data
}

export const getPermissionById = async (id: string) => {
  const response = await http.get<ApiResponse<Permission>>(`/permissions/${id}`)
  return response.data.data
}

export const createPermission = async (data: Permission) => {
  const response = await http.post<ApiResponse<Permission>>('/permissions', data)
  return response.data.data
}

export const updatePermission = async (id: string, data: Permission) => {
  const response = await http.put<ApiResponse<Permission>>(`/permissions/${id}`, data)
  return response.data.data
}

export const deletePermission = async (id: string) => {
  await http.delete<ApiResponse<void>>(`/permissions/${id}`)
}

export const togglePermission = async (id: string) => {
  await http.put<ApiResponse<void>>(`/permissions/${id}/toggle`)
}
