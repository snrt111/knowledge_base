<template>
  <div class="documents-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">
            <el-icon><Document /></el-icon>
            文档管理
          </span>
          <el-button type="primary" @click="showUploadDialog">
            <el-icon><Upload /></el-icon>
            上传文档
          </el-button>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="知识库">
          <el-select v-model="searchForm.knowledgeBaseId" placeholder="选择知识库" clearable style="width: 200px" @change="handleSearch">
            <el-option
              v-for="kb in knowledgeBases"
              :key="kb.id"
              :label="kb.name"
              :value="kb.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="文档名称">
          <el-input v-model="searchForm.keyword" placeholder="请输入文档名称" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="documents" v-loading="loading" stripe>
        <el-table-column type="index" width="60" label="序号" />
        <el-table-column prop="name" label="文档名称" min-width="200">
          <template #default="{ row }">
            <div class="doc-name">
              <el-icon size="20" :color="getFileIconColor(row.type)">
                <Document />
              </el-icon>
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="knowledgeBaseName" label="所属知识库" width="150" />
        <el-table-column prop="type" label="文件类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="getFileTypeTag(row.type)">
              {{ row.type.toUpperCase() }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="size" label="文件大小" width="120">
          <template #default="{ row }">
            {{ formatFileSize(row.size) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="uploadTime" label="上传时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handlePreview(row)">
              <el-icon><View /></el-icon>
              预览
            </el-button>
            <el-button type="primary" link @click="handleDownload(row)">
              <el-icon><Download /></el-icon>
              下载
            </el-button>
            <el-button type="danger" link @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="page.current"
          v-model:page-size="page.size"
          :page-sizes="[10, 20, 50]"
          :total="page.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="uploadDialog.visible"
      title="上传文档"
      width="600px"
    >
      <el-form :model="uploadForm" label-width="100px">
        <el-form-item label="选择知识库" required>
          <el-select v-model="uploadForm.knowledgeBaseId" placeholder="请选择知识库" style="width: 100%">
            <el-option
              v-for="kb in knowledgeBases"
              :key="kb.id"
              :label="kb.name"
              :value="kb.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="选择文件" required>
          <el-upload
            ref="uploadRef"
            drag
            action="#"
            :auto-upload="false"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :file-list="uploadForm.fileList"
            accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.md,.txt"
            multiple
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">
              拖拽文件到此处或 <em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持 PDF、Word、Excel、PPT、Markdown、TXT 格式，单个文件不超过 50MB
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="handleUpload" :loading="uploadDialog.loading">
          开始上传
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="previewDialog.visible"
      title="文档预览"
      width="900px"
      class="preview-dialog"
      destroy-on-close
    >
      <div v-loading="previewDialog.loading" class="preview-content">
        <div class="preview-header">
          <h3>{{ previewDialog.data?.name }}</h3>
          <div class="preview-actions">
            <el-button type="primary" link @click="handleDownloadFromPreview">
              <el-icon><Download /></el-icon>
              下载
            </el-button>
          </div>
        </div>
        <div class="preview-info">
          <el-descriptions :column="3" size="small" border>
            <el-descriptions-item label="所属知识库">{{ previewDialog.data?.knowledgeBaseName }}</el-descriptions-item>
            <el-descriptions-item label="文件类型">{{ previewDialog.data?.type.toUpperCase() }}</el-descriptions-item>
            <el-descriptions-item label="文件大小">{{ formatFileSize(previewDialog.data?.size || 0) }}</el-descriptions-item>
            <el-descriptions-item label="上传时间">{{ previewDialog.data?.uploadTime }}</el-descriptions-item>
            <el-descriptions-item label="预览类型">
              <el-tag :type="getPreviewTypeTag(previewDialog.data?.previewType)">
                {{ getPreviewTypeText(previewDialog.data?.previewType) }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </div>
        <el-divider />
        <div class="preview-body">
          <!-- 使用 DocumentViewer 组件预览 -->
          <DocumentViewer
            v-if="previewDialog.data"
            :preview-type="previewDialog.data.previewType"
            :content="previewDialog.data.content"
            :download-url="previewDialog.data.downloadUrl"
            :file-type="previewDialog.data.type"
            :error-message="previewDialog.data.errorMessage"
          />
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadFile, UploadFiles } from 'element-plus'
import { useRoute } from 'vue-router'
import { documentApi } from '@/api/document'
import { knowledgeBaseApi } from '@/api/knowledgeBase'
import DocumentViewer from '@/components/DocumentViewer.vue'
import type { Document, DocumentPreview, KnowledgeBase } from '@/types'

const route = useRoute()
const loading = ref(false)
const uploadRef = ref()

const knowledgeBases = ref<KnowledgeBase[]>([])

const searchForm = reactive({
  knowledgeBaseId: route.query.knowledgeBaseId as string || '',
  keyword: ''
})

const documents = ref<Document[]>([])

const page = reactive({
  current: 1,
  size: 10,
  total: 0
})

const uploadDialog = reactive({
  visible: false,
  loading: false
})

const uploadForm = reactive({
  knowledgeBaseId: '',
  fileList: [] as UploadFiles
})

const previewDialog = reactive({
  visible: false,
  loading: false,
  data: null as DocumentPreview | null,
  currentDoc: null as Document | null
})

const fetchKnowledgeBases = async () => {
  try {
    knowledgeBases.value = await knowledgeBaseApi.listAll()
  } catch (error) {
    ElMessage.error('获取知识库列表失败')
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await documentApi.list(
      page.current,
      page.size,
      searchForm.knowledgeBaseId || undefined,
      searchForm.keyword || undefined
    )
    documents.value = res.list
    page.total = res.total
  } catch (error) {
    ElMessage.error('获取文档列表失败')
  } finally {
    loading.value = false
  }
}

const getFileIconColor = (type: string) => {
  const colors: Record<string, string> = {
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
  return colors[type] || '#909399'
}

const getFileTypeTag = (type: string) => {
  const types: Record<string, any> = {
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
  return types[type] || 'info'
}

const formatFileSize = (bytes: number) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const getStatusType = (status: string) => {
  const types: Record<string, any> = {
    processing: 'warning',
    completed: 'success',
    failed: 'danger'
  }
  return types[status]
}

const getStatusText = (status: string) => {
  const texts: Record<string, string> = {
    processing: '处理中',
    completed: '已完成',
    failed: '失败'
  }
  return texts[status]
}

const handleSearch = () => {
  page.current = 1
  fetchData()
}

const handleReset = () => {
  searchForm.knowledgeBaseId = ''
  searchForm.keyword = ''
  handleSearch()
}

const showUploadDialog = () => {
  uploadForm.knowledgeBaseId = searchForm.knowledgeBaseId || ''
  uploadForm.fileList = []
  uploadDialog.visible = true
}

const handleFileChange = (uploadFile: UploadFile, uploadFiles: UploadFiles) => {
  uploadForm.fileList = uploadFiles
}

const handleFileRemove = (uploadFile: UploadFile, uploadFiles: UploadFiles) => {
  uploadForm.fileList = uploadFiles
}

const handleUpload = async () => {
  if (!uploadForm.knowledgeBaseId) {
    ElMessage.warning('请选择知识库')
    return
  }
  if (uploadForm.fileList.length === 0) {
    ElMessage.warning('请选择要上传的文件')
    return
  }

  uploadDialog.loading = true
  try {
    for (const file of uploadForm.fileList) {
      if (file.raw) {
        await documentApi.upload(file.raw, uploadForm.knowledgeBaseId)
      }
    }
    ElMessage.success(`成功上传 ${uploadForm.fileList.length} 个文件`)
    uploadDialog.visible = false
    fetchData()
  } catch (error) {
    ElMessage.error('上传失败')
  } finally {
    uploadDialog.loading = false
  }
}

const handlePreview = async (row: Document) => {
  previewDialog.currentDoc = row
  previewDialog.visible = true
  previewDialog.loading = true

  try {
    const data = await documentApi.preview(row.id)
    previewDialog.data = data
  } catch (error) {
    ElMessage.error('获取文档预览失败')
    previewDialog.data = null
  } finally {
    previewDialog.loading = false
  }
}

const handleDownload = (row: Document) => {
  const url = documentApi.download(row.id)
  const link = document.createElement('a')
  link.href = url
  link.download = row.name
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  ElMessage.success(`开始下载: ${row.name}`)
}

const handleDownloadFromPreview = () => {
  if (previewDialog.currentDoc) {
    handleDownload(previewDialog.currentDoc)
  }
}

const getPreviewTypeTag = (type?: string) => {
  const tags: Record<string, any> = {
    text: 'success',
    pdf: 'warning',
    word: 'primary',
    excel: 'success',
    ppt: 'danger',
    unsupported: 'info'
  }
  return tags[type || ''] || 'info'
}

const getPreviewTypeText = (type?: string) => {
  const texts: Record<string, string> = {
    text: '文本预览',
    pdf: 'PDF预览',
    word: 'Word文档',
    excel: 'Excel文档',
    ppt: 'PPT文档',
    unsupported: '不支持预览'
  }
  return texts[type || ''] || '未知'
}

const handleDelete = (row: Document) => {
  ElMessageBox.confirm(
    `确定要删除文档 "${row.name}" 吗？`,
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await documentApi.delete(row.id)
      ElMessage.success('删除成功')
      fetchData()
    } catch (error) {
      ElMessage.error('删除失败')
    }
  })
}

const handleSizeChange = (val: number) => {
  page.size = val
  fetchData()
}

const handleCurrentChange = (val: number) => {
  page.current = val
  fetchData()
}

onMounted(() => {
  fetchKnowledgeBases()
  fetchData()
})
</script>

<style scoped>
.documents-container {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-form {
  margin-bottom: 20px;
}

.doc-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.preview-content {
  max-height: 700px;
  overflow-y: auto;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.preview-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.preview-info {
  margin-bottom: 16px;
}

.preview-body {
  min-height: 300px;
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
  max-height: 500px;
  overflow-y: auto;
}

.pdf-preview {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  overflow: hidden;
}

.word-preview,
.excel-preview,
.ppt-preview,
.unsupported-preview {
  padding: 40px 0;
}
</style>

<style>
/* 预览对话框全局样式 */
.preview-dialog .el-dialog__body {
  max-height: calc(90vh - 200px);
  overflow: hidden;
}
</style>
