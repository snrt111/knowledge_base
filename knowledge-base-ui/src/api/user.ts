import http from './http'
import type { ApiResponse, PageResult } from '@/types'
import type { User, UserQuery, UserCreate, UserUpdate, AssignRole, ResetPassword, Role } from '@/types/user'

export const getUserPage = async (params: UserQuery) => {
  const response = await http.get<ApiResponse<PageResult<User>>>('/users', { params })
  return response.data.data
}

export const getUserById = async (id: string) => {
  const response = await http.get<ApiResponse<User>>(`/users/${id}`)
  return response.data.data
}

export const createUser = async (data: UserCreate) => {
  const response = await http.post<ApiResponse<User>>('/users', data)
  return response.data.data
}

export const updateUser = async (data: UserUpdate) => {
  const response = await http.put<ApiResponse<User>>(`/users/${data.id}`, data)
  return response.data.data
}

export const deleteUser = async (id: string) => {
  await http.delete<ApiResponse<void>>(`/users/${id}`)
}

export const toggleUser = async (id: string) => {
  await http.put<ApiResponse<void>>(`/users/${id}/toggle`)
}

export const resetPassword = async (id: string, newPassword: string) => {
  await http.put<ApiResponse<void>>(`/users/${id}/reset-password`, { userId: id, newPassword })
}

export const assignRoles = async (userId: string, roleIds: string[]) => {
  await http.put<ApiResponse<void>>(`/users/${userId}/roles`, { userId, roleIds })
}

export const getUserRoles = async (id: string) => {
  const response = await http.get<ApiResponse<Role[]>>(`/users/${id}/roles`)
  return response.data.data
}
