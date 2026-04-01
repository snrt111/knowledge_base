import http from './http'

// 登录请求
export const login = (data: {
  username: string
  password: string
}) => {
  return http.post('/auth/login', data)
}

// 注册请求
export const register = (data: {
  username: string
  password: string
  nickname?: string
  email?: string
  phone?: string
}) => {
  return http.post('/auth/register', data)
}

// 获取当前用户信息
export const getCurrentUser = () => {
  return http.get('/auth/user')
}
