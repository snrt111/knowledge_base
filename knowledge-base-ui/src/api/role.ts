import http from './http'
import type { Role, RoleQuery, RoleCreate, RoleUpdate, AssignPermission, PageResult } from '@/types'
import type { Permission } from '@/types/user'

export const getRolePage = (params: RoleQuery) => {
  return http.get<PageResult<Role>>('/roles', { params })
}

export const getAllRoles = () => {
  return http.get<Role[]>('/roles/all')
}

export const getRoleById = (id: string) => {
  return http.get<Role>(`/roles/${id}`)
}

export const createRole = (data: RoleCreate) => {
  return http.post<Role>('/roles', data)
}

export const updateRole = (data: RoleUpdate) => {
  return http.put<Role>(`/roles/${data.id}`, data)
}

export const deleteRole = (id: string) => {
  return http.delete(`/roles/${id}`)
}

export const toggleRole = (id: string) => {
  return http.put(`/roles/${id}/toggle`)
}

export const assignPermissions = (roleId: string, permissionIds: string[]) => {
  return http.put(`/roles/${roleId}/permissions`, { roleId, permissionIds })
}

export const getRolePermissions = (id: string) => {
  return http.get<Permission[]>(`/roles/${id}/permissions`)
}
