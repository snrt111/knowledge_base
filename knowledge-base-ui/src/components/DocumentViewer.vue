<template>
  <div class="document-viewer" :class="{ 'is-fullscreen': isFullscreen }">
    <!-- 全屏切换按钮 -->
    <div class="fullscreen-toggle" @click="toggleFullscreen">
      <el-icon size="20">
        <FullScreen v-if="!isFullscreen" />
        <Close v-else />
      </el-icon>
    </div>

    <!-- Markdown 预览 -->
    <div v-if="previewType === 'text' && isMarkdown" class="markdown-preview" v-html="renderedMarkdown"></div>

    <!-- 文本预览 -->
    <div v-else-if="previewType === 'text'" class="text-preview">
      <pre class="text-content">{{ content }}</pre>
    </div>

    <!-- PDF 预览 -->
    <div v-else-if="previewType === 'pdf'" ref="pdfContainer" class="pdf-preview-container"></div>

    <!-- Word 预览 -->
    <div v-else-if="previewType === 'word'" ref="wordContainer" class="word-preview-container"></div>

    <!-- Excel 预览 -->
    <div v-else-if="previewType === 'excel'" ref="excelContainer" class="excel-preview-container"></div>

    <!-- PPT 预览 -->
    <div v-else-if="previewType === 'ppt'" ref="pptContainer" class="ppt-preview-container"></div>

    <!-- 不支持的格式 -->
    <div v-else class="unsupported-preview">
      <el-result
        :icon="errorMessage ? 'error' : 'warning'"
        :title="errorMessage ? '预览失败' : '暂不支持预览'"
        :sub-title="errorMessage || '该文件格式暂不支持在线预览'"
      >
        <template #extra>
          <el-button v-if="downloadUrl" type="primary" @click="handleDownload">
            <el-icon><Download /></el-icon>
            下载查看
          </el-button>
        </template>
      </el-result>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, FullScreen, Close } from '@element-plus/icons-vue'
import { marked } from 'marked'
import 'highlight.js/styles/github.css'

// 获取完整URL
const getFullUrl = (url: string): string => {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }
  // 相对路径转为完整URL
  const baseUrl = window.location.origin
  return url.startsWith('/') ? `${baseUrl}${url}` : `${baseUrl}/${url}`
}

// 动态导入预览库
const initPdfPreview = async () => {
  try {
    const module = await import('@js-preview/pdf')
    return module.default || module
  } catch (e) {
    console.error('PDF预览库加载失败:', e)
    throw e
  }
}

const initDocxPreview = async () => {
  try {
    const module = await import('@js-preview/docx')
    return module.default || module
  } catch (e) {
    console.error('Word预览库加载失败:', e)
    throw e
  }
}

const initExcelPreview = async () => {
  try {
    const module = await import('@js-preview/excel')
    return module.default || module
  } catch (e) {
    console.error('Excel预览库加载失败:', e)
    throw e
  }
}

const initPptxPreview = async () => {
  try {
    const module = await import('pptx-preview')
    return module.default || module
  } catch (e) {
    console.error('PPT预览库加载失败:', e)
    throw e
  }
}

// 预览库实例类型
type PdfPreviewType = {
  init: (container: HTMLElement, options?: any, requestOptions?: any) => { preview: (url: string) => void }
}
type DocxPreviewType = {
  init: (container: HTMLElement, options?: any, requestOptions?: any) => { preview: (url: string) => void }
}
type ExcelPreviewType = {
  init: (container: HTMLElement, options?: any, requestOptions?: any) => { preview: (url: string) => void }
}
type PptxPreviewOptions = {
  width?: number
  height?: number
  mode?: 'list' | 'slide'
}

type PptxPreviewType = {
  init: (container: HTMLElement, options: PptxPreviewOptions) => { preview: (file: ArrayBuffer) => Promise<unknown> }
}

interface Props {
  previewType: 'text' | 'pdf' | 'word' | 'excel' | 'ppt' | 'unsupported'
  content?: string
  downloadUrl?: string
  fileType?: string
  errorMessage?: string
}

const props = withDefaults(defineProps<Props>(), {
  content: '',
  downloadUrl: '',
  fileType: '',
  errorMessage: ''
})

const pdfContainer = ref<HTMLElement>()
const wordContainer = ref<HTMLElement>()
const excelContainer = ref<HTMLElement>()
const pptContainer = ref<HTMLElement>()
const isFullscreen = ref(false)

// 切换全屏
const toggleFullscreen = () => {
  isFullscreen.value = !isFullscreen.value
  // 全屏状态改变后重新初始化预览以适配新尺寸
  if (props.previewType && props.previewType !== 'text' && props.previewType !== 'unsupported') {
    nextTick(() => {
      initPreview()
    })
  }
}

// 判断是否是 Markdown 文件
const isMarkdown = computed(() => {
  return props.fileType?.toLowerCase() === 'md'
})

// 渲染 Markdown
const renderedMarkdown = computed(() => {
  if (!props.content) return ''
  return marked(props.content)
})

// 处理下载
const handleDownload = () => {
  if (props.downloadUrl) {
    const link = document.createElement('a')
    link.href = props.downloadUrl
    link.target = '_blank'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  }
}

// 预览 PDF
const previewPdf = async () => {
  if (!pdfContainer.value || !props.downloadUrl) return

  try {
    pdfContainer.value.innerHTML = ''
    const pdfPreview = await initPdfPreview() as PdfPreviewType
    const instance = pdfPreview.init(pdfContainer.value)
    const fullUrl = getFullUrl(props.downloadUrl)
    instance.preview(fullUrl)
  } catch (error) {
    console.error('PDF预览失败:', error)
    ElMessage.error('PDF预览加载失败')
  }
}

// 预览 Word
const previewWord = async () => {
  if (!wordContainer.value || !props.downloadUrl) return

  try {
    wordContainer.value.innerHTML = ''
    const docxPreview = await initDocxPreview() as DocxPreviewType
    const instance = docxPreview.init(wordContainer.value)
    const fullUrl = getFullUrl(props.downloadUrl)
    instance.preview(fullUrl)
  } catch (error) {
    console.error('Word预览失败:', error)
    ElMessage.error('Word预览加载失败')
  }
}

// 预览 Excel
const previewExcel = async () => {
  if (!excelContainer.value || !props.downloadUrl) return

  try {
    excelContainer.value.innerHTML = ''
    const excelPreview = await initExcelPreview() as ExcelPreviewType
    const instance = excelPreview.init(excelContainer.value)
    const fullUrl = getFullUrl(props.downloadUrl)
    instance.preview(fullUrl)
  } catch (error) {
    console.error('Excel预览失败:', error)
    ElMessage.error('Excel预览加载失败')
  }
}

// 预览 PPT
const previewPpt = async () => {
  if (!pptContainer.value || !props.downloadUrl) return

  try {
    pptContainer.value.innerHTML = ''
    const fullUrl = getFullUrl(props.downloadUrl)

    // 获取文件内容为 ArrayBuffer
    const response = await fetch(fullUrl)
    if (!response.ok) {
      throw new Error('获取PPT文件失败')
    }
    const arrayBuffer = await response.arrayBuffer()

    const pptxPreview = await initPptxPreview() as PptxPreviewType
    // 获取容器宽度，用于设置预览选项
    const containerWidth = pptContainer.value.clientWidth || 800
    const instance = pptxPreview.init(pptContainer.value, {
      width: containerWidth,
      mode: 'list' // 列表模式，显示所有幻灯片
    })
    await instance.preview(arrayBuffer)
  } catch (error) {
    console.error('PPT预览失败:', error)
    ElMessage.error('PPT预览加载失败')
  }
}

// 初始化预览
const initPreview = () => {
  nextTick(() => {
    switch (props.previewType) {
      case 'pdf':
        previewPdf()
        break
      case 'word':
        previewWord()
        break
      case 'excel':
        previewExcel()
        break
      case 'ppt':
        previewPpt()
        break
    }
  })
}

onMounted(() => {
  initPreview()
})

// 监听 downloadUrl 变化，当数据异步加载完成后重新初始化预览
watch(() => props.downloadUrl, (newUrl, oldUrl) => {
  if (newUrl && newUrl !== oldUrl) {
    initPreview()
  }
}, { immediate: false })
</script>

<style scoped>
.document-viewer {
  width: 100%;
  min-height: 400px;
  position: relative;
}

/* 全屏切换按钮 */
.fullscreen-toggle {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 1000;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(255, 255, 255, 0.9);
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.fullscreen-toggle:hover {
  background-color: #f5f7fa;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

/* 全屏模式样式 */
.document-viewer.is-fullscreen {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 9999;
  background-color: #fff;
  padding: 20px;
  box-sizing: border-box;
}

.document-viewer.is-fullscreen .fullscreen-toggle {
  top: 20px;
  right: 20px;
}

.document-viewer.is-fullscreen .pdf-preview-container,
.document-viewer.is-fullscreen .word-preview-container {
  height: calc(100vh - 40px);
}

.document-viewer.is-fullscreen .excel-preview-container {
  height: calc(100vh - 40px);
}

.document-viewer.is-fullscreen .excel-preview-container :deep(.vue-office-excel),
.document-viewer.is-fullscreen .excel-preview-container :deep(.x-spreadsheet) {
  height: calc(100vh - 40px) !important;
  max-height: calc(100vh - 40px) !important;
}

.document-viewer.is-fullscreen .excel-preview-container :deep(.x-spreadsheet-sheet) {
  height: calc(100vh - 81px) !important;
}

.document-viewer.is-fullscreen .ppt-preview-container {
  height: calc(100vh - 40px);
}

.document-viewer.is-fullscreen .markdown-preview {
  max-height: calc(100vh - 40px);
  overflow-y: auto;
}

.document-viewer.is-fullscreen .text-preview {
  max-height: calc(100vh - 40px);
  overflow-y: auto;
}

.document-viewer.is-fullscreen .text-content {
  max-height: calc(100vh - 72px);
}

.markdown-preview {
  padding: 20px;
  background-color: #fff;
  border-radius: 4px;
  line-height: 1.6;
}

.markdown-preview :deep(h1),
.markdown-preview :deep(h2),
.markdown-preview :deep(h3),
.markdown-preview :deep(h4),
.markdown-preview :deep(h5),
.markdown-preview :deep(h6) {
  margin-top: 24px;
  margin-bottom: 16px;
  font-weight: 600;
  line-height: 1.25;
}

.markdown-preview :deep(h1) {
  font-size: 2em;
  border-bottom: 1px solid #eaecef;
  padding-bottom: 0.3em;
}

.markdown-preview :deep(h2) {
  font-size: 1.5em;
  border-bottom: 1px solid #eaecef;
  padding-bottom: 0.3em;
}

.markdown-preview :deep(p) {
  margin-top: 0;
  margin-bottom: 16px;
}

.markdown-preview :deep(code) {
  background-color: rgba(27, 31, 35, 0.05);
  border-radius: 3px;
  font-size: 85%;
  margin: 0;
  padding: 0.2em 0.4em;
}

.markdown-preview :deep(pre) {
  background-color: #f6f8fa;
  border-radius: 6px;
  font-size: 85%;
  line-height: 1.45;
  overflow: auto;
  padding: 16px;
}

.markdown-preview :deep(pre code) {
  background-color: transparent;
  border: 0;
  display: inline;
  line-height: inherit;
  margin: 0;
  overflow: visible;
  padding: 0;
  word-wrap: normal;
}

.markdown-preview :deep(blockquote) {
  border-left: 0.25em solid #dfe2e5;
  color: #6a737d;
  padding: 0 1em;
  margin: 0 0 16px 0;
}

.markdown-preview :deep(ul),
.markdown-preview :deep(ol) {
  margin-top: 0;
  margin-bottom: 16px;
  padding-left: 2em;
}

.markdown-preview :deep(li + li) {
  margin-top: 0.25em;
}

.markdown-preview :deep(table) {
  border-collapse: collapse;
  border-spacing: 0;
  display: block;
  overflow: auto;
  width: 100%;
  margin-bottom: 16px;
}

.markdown-preview :deep(table th),
.markdown-preview :deep(table td) {
  border: 1px solid #dfe2e5;
  padding: 6px 13px;
}

.markdown-preview :deep(table tr:nth-child(2n)) {
  background-color: #f6f8fa;
}

.markdown-preview :deep(img) {
  max-width: 100%;
  box-sizing: content-box;
}

.text-preview {
  background-color: #f5f7fa;
  border-radius: 4px;
  padding: 16px;
}

.text-content {
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.6;
  color: #303133;
  max-height: 600px;
  overflow-y: auto;
}

.pdf-preview-container,
.word-preview-container {
  width: 100%;
  height: 600px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  overflow: auto;
}

.excel-preview-container {
  width: 100%;
  height: 600px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  overflow: hidden;
  position: relative;
}

/* Excel 预览内部样式修复 - 针对 @js-preview/excel */
.excel-preview-container :deep(.vue-office-excel) {
  height: 600px !important;
  max-height: 600px !important;
}

.excel-preview-container :deep(.x-spreadsheet) {
  height: 600px !important;
  max-height: 600px !important;
}

.excel-preview-container :deep(.x-spreadsheet-sheet) {
  height: calc(600px - 41px) !important;
}

.excel-preview-container :deep(.x-spreadsheet-scrollbar) {
  z-index: 100;
}

.ppt-preview-container {
  width: 100%;
  height: 600px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  overflow: auto;
}

.unsupported-preview {
  padding: 40px 0;
}
</style>
