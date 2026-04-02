export interface User {
  id: string
  username: string
  nickname?: string
  email?: string
  phone?: string
  avatar?: string
  isActive: boolean
  createTime: string
  lastLoginTime?: string
  roles?: Role[]
  permissions?: string[]
}

export interface Role {
  id: string
  name: string
  code: string
  description?: string
  sort: number
  isActive: boolean
  createTime: string
  updateTime: string
  permissions?: Permission[]
}

export interface Permission {
  id: string
  name: string
  code: string
  type: string
  parentId?: string
  path?: string
  icon?: string
  component?: string
  sort: number
  isActive: boolean
  createTime: string
  updateTime: string
  children?: Permission[]
}

export interface UserQuery {
  username?: string
  nickname?: string
  email?: string
  isActive?: boolean
  pageNum?: number
  pageSize?: number
}

export interface RoleQuery {
  name?: string
  code?: string
  pageNum?: number
  pageSize?: number
}

export interface UserCreate {
  username: string
  password: string
  nickname?: string
  email?: string
  phone?: string
  avatar?: string
  roleIds?: string[]
}

export interface UserUpdate {
  id: string
  nickname?: string
  email?: string
  phone?: string
  avatar?: string
  roleIds?: string[]
}

export interface RoleCreate {
  name: string
  code: string
  description?: string
  sort?: number
  permissionIds?: string[]
}

export interface RoleUpdate {
  id: string
  name?: string
  code?: string
  description?: string
  sort?: number
  permissionIds?: string[]
}

export interface AssignRole {
  userId: string
  roleIds: string[]
}

export interface AssignPermission {
  roleId: string
  permissionIds: string[]
}

export interface ResetPassword {
  userId: string
  newPassword: string
}
