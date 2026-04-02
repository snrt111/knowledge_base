import http from './http'
import type { ApiResponse, PageResult } from '@/types'
import type { Role, RoleQuery, RoleCreate, RoleUpdate, AssignPermission, Permission } from '@/types/user'

export const getRolePage = async (params: RoleQuery) => {
  const response = await http.get<ApiResponse<PageResult<Role>>>('/roles', { params })
  return response.data.data
}

export const getAllRoles = async () => {
  const response = await http.get<ApiResponse<Role[]>>('/roles/all')
  return response.data.data
}

export const getRoleById = async (id: string) => {
  const response = await http.get<ApiResponse<Role>>(`/roles/${id}`)
  return response.data.data
}

export const createRole = async (data: RoleCreate) => {
  const response = await http.post<ApiResponse<Role>>('/roles', data)
  return response.data.data
}

export const updateRole = async (data: RoleUpdate) => {
  const response = await http.put<ApiResponse<Role>>(`/roles/${data.id}`, data)
  return response.data.data
}

export const deleteRole = async (id: string) => {
  await http.delete<ApiResponse<void>>(`/roles/${id}`)
}

export const toggleRole = async (id: string) => {
  await http.put<ApiResponse<void>>(`/roles/${id}/toggle`)
}

export const assignPermissions = async (roleId: string, permissionIds: string[]) => {
  await http.put<ApiResponse<void>>(`/roles/${roleId}/permissions`, { roleId, permissionIds })
}

export const getRolePermissions = async (id: string) => {
  const response = await http.get<ApiResponse<Permission[]>>(`/roles/${id}/permissions`)
  return response.data.data
}
