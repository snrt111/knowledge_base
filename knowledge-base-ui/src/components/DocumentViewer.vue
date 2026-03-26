<template>
  <div class="document-viewer">
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
    <div v-else-if="previewType === 'ppt'" ref="pptContainer" class="ppt-preview-container">
      <div v-if="!pptLoaded" class="ppt-placeholder">
        <el-result
          icon="info"
          title="PPT文档预览"
          sub-title="PPT预览需要特殊处理，请点击下载查看"
        >
          <template #extra>
            <el-button type="primary" @click="handleDownload">
              <el-icon><Download /></el-icon>
              下载查看
            </el-button>
          </template>
        </el-result>
      </div>
    </div>
    
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
import { Download } from '@element-plus/icons-vue'
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
const pptLoaded = ref(false)

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
  
  // PPT 预览较为复杂，暂时使用下载方式
  // 后续可以集成 pptxjs 或其他库
  pptLoaded.value = false
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
.word-preview-container,
.excel-preview-container {
  width: 100%;
  height: 600px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  overflow: auto;
}

.ppt-preview-container {
  width: 100%;
  min-height: 400px;
}

.ppt-placeholder {
  padding: 40px 0;
}

.unsupported-preview {
  padding: 40px 0;
}
</style>
