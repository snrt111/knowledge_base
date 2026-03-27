/**
 * 日期时间格式化工具函数
 */

/**
 * 格式化日期时间为 yyyy-MM-dd HH:mm:ss 格式
 * @param dateStr - ISO 8601 格式的日期字符串或 Date 对象
 * @returns 格式化后的日期字符串，格式为 yyyy-MM-dd HH:mm:ss
 */
export function formatDateTime(dateStr: string | Date | undefined | null): string {
  if (!dateStr) {
    return '-'
  }

  const date = typeof dateStr === 'string' ? new Date(dateStr) : dateStr

  // 检查日期是否有效
  if (isNaN(date.getTime())) {
    return '-'
  }

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')

  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

/**
 * 格式化日期为 yyyy-MM-dd 格式
 * @param dateStr - ISO 8601 格式的日期字符串或 Date 对象
 * @returns 格式化后的日期字符串，格式为 yyyy-MM-dd
 */
export function formatDate(dateStr: string | Date | undefined | null): string {
  if (!dateStr) {
    return '-'
  }

  const date = typeof dateStr === 'string' ? new Date(dateStr) : dateStr

  // 检查日期是否有效
  if (isNaN(date.getTime())) {
    return '-'
  }

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}`
}

/**
 * 格式化时间为 HH:mm:ss 格式
 * @param dateStr - ISO 8601 格式的日期字符串或 Date 对象
 * @returns 格式化后的时间字符串，格式为 HH:mm:ss
 */
export function formatTime(dateStr: string | Date | undefined | null): string {
  if (!dateStr) {
    return '-'
  }

  const date = typeof dateStr === 'string' ? new Date(dateStr) : dateStr

  // 检查日期是否有效
  if (isNaN(date.getTime())) {
    return '-'
  }

  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')

  return `${hours}:${minutes}:${seconds}`
}
