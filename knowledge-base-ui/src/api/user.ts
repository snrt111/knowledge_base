import http from './http'
import type { User, UserQuery, UserCreate, UserUpdate, AssignRole, ResetPassword, PageResult } from '@/types'
import type { Role } from '@/types/user'

export const getUserPage = (params: UserQuery) => {
  return http.get<PageResult<User>>('/users', { params })
}

export const getUserById = (id: string) => {
  return http.get<User>(`/users/${id}`)
}

export const createUser = (data: UserCreate) => {
  return http.post<User>('/users', data)
}

export const updateUser = (data: UserUpdate) => {
  return http.put<User>(`/users/${data.id}`, data)
}

export const deleteUser = (id: string) => {
  return http.delete(`/users/${id}`)
}

export const toggleUser = (id: string) => {
  return http.put(`/users/${id}/toggle`)
}

export const resetPassword = (id: string, newPassword: string) => {
  return http.put(`/users/${id}/reset-password`, { userId: id, newPassword })
}

export const assignRoles = (userId: string, roleIds: string[]) => {
  return http.put(`/users/${userId}/roles`, { userId, roleIds })
}

export const getUserRoles = (id: string) => {
  return http.get<Role[]>(`/users/${id}/roles`)
}
