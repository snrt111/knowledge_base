import type { DocumentPreview } from '@/types'

/** 文件扩展名 -> 图标颜色（与 Element 主题协调） */
export const FILE_ICON_COLORS: Record<string, string> = {
  pdf: '#FF6B6B',
  doc: '#409EFF',
  docx: '#409EFF',
  xls: '#67C23A',
  xlsx: '#67C23A',
  ppt: '#E6A23C',
  pptx: '#E6A23C',
  txt: '#909399',
  md: '#409EFF'
}

/** 文件扩展名 -> el-tag type */
export const FILE_TAG_TYPES: Record<string, 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
  pdf: 'danger',
  doc: 'primary',
  docx: 'primary',
  xls: 'success',
  xlsx: 'success',
  ppt: 'warning',
  pptx: 'warning',
  txt: 'info',
  md: 'primary'
}

export const DOCUMENT_STATUS_TAG: Record<string, 'warning' | 'success' | 'danger'> = {
  processing: 'warning',
  completed: 'success',
  failed: 'danger'
}

export const DOCUMENT_STATUS_LABEL: Record<string, string> = {
  processing: '处理中',
  completed: '已完成',
  failed: '失败'
}

const PREVIEW_TYPE_TAG: Record<string, 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
  text: 'success',
  pdf: 'warning',
  word: 'primary',
  excel: 'success',
  ppt: 'danger',
  unsupported: 'info'
}

const PREVIEW_TYPE_LABEL: Record<string, string> = {
  text: '文本预览',
  pdf: 'PDF预览',
  word: 'Word文档',
  excel: 'Excel文档',
  ppt: 'PPT文档',
  unsupported: '不支持预览'
}

export function getFileIconColor(type: string): string {
  return FILE_ICON_COLORS[type] ?? '#909399'
}

export function getFileTypeTag(type: string): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  return FILE_TAG_TYPES[type] ?? 'info'
}

export function getDocumentStatusTag(status: string): 'warning' | 'success' | 'danger' | undefined {
  return DOCUMENT_STATUS_TAG[status]
}

export function getDocumentStatusLabel(status: string): string {
  return DOCUMENT_STATUS_LABEL[status] ?? status
}

export function getPreviewTypeTag(type?: DocumentPreview['previewType']): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  return type ? (PREVIEW_TYPE_TAG[type] ?? 'info') : 'info'
}

export function getPreviewTypeLabel(type?: DocumentPreview['previewType']): string {
  return type ? (PREVIEW_TYPE_LABEL[type] ?? '未知') : '未知'
}

export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${parseFloat((bytes / k ** i).toFixed(2))} ${sizes[i]}`
}
