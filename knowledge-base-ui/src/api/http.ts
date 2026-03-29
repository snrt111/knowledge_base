import axios, { type AxiosResponse } from 'axios'
import type { ApiResponse } from '@/types'

/**
 * 统一 Axios 实例：与后端 {@code ApiResponse} 约定一致，业务码非 200 时拒绝并携带 message。
 */
export const http = axios.create({
  baseURL: '/api',
  timeout: 120_000
})

function isApiEnvelope(body: unknown): body is ApiResponse<unknown> {
  return typeof body === 'object' && body !== null && 'code' in body && typeof (body as ApiResponse<unknown>).code === 'number'
}

http.interceptors.response.use(
  (response: AxiosResponse) => {
    const body = response.data
    if (isApiEnvelope(body) && body.code !== 200) {
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return response
  },
  (error) => {
    const data = error.response?.data
    const msg =
      isApiEnvelope(data) ? data.message
      : typeof data?.message === 'string' ? data.message
      : error.message || '网络错误'
    return Promise.reject(new Error(msg))
  }
)

/** 从成功响应中取出 data 字段（类型由调用方断言） */
export function unwrapData<T>(res: AxiosResponse<ApiResponse<T>>): T {
  return res.data.data
}
