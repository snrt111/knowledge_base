<template>
  <div class="documents-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">
            <el-icon><Document /></el-icon>
            文档管理
          </span>
          <div class="header-actions">
            <el-button
              type="danger"
              :disabled="selectedDocuments.length === 0"
              @click="handleBatchDelete"
            >
              <el-icon><Delete /></el-icon>
              批量删除 ({{ selectedDocuments.length }})
            </el-button>
            <el-button
              type="warning"
              :disabled="selectedDocuments.length === 0"
              @click="handleBatchReprocess"
            >
              <el-icon><RefreshRight /></el-icon>
              批量重新处理 ({{ selectedDocuments.length }})
            </el-button>
            <el-button type="primary" @click="showUploadDialog">
              <el-icon><Upload /></el-icon>
              上传文档
            </el-button>
          </div>
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

      <el-table
        :data="documents"
        v-loading="loading"
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
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
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <div class="status-cell">
              <el-tag :type="getDocumentStatusTag(row.status)">
                {{ getDocumentStatusLabel(row.status) }}
              </el-tag>
              <el-button
                v-if="row.status !== 'completed'"
                type="warning"
                link
                size="small"
                :loading="reprocessingId === row.id"
                :disabled="reprocessingId !== null && reprocessingId !== row.id"
                @click="handleReprocess(row)"
                title="重新向量化"
              >
                <el-icon><RefreshRight /></el-icon>
              </el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="uploadTime" label="上传时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.uploadTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <div class="operation-buttons">
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
            </div>
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
                支持 PDF、Word、Excel、PPT、Markdown、TXT 格式，单个文件不超过 1GB
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
            <el-descriptions-item label="上传时间">{{ formatDateTime(previewDialog.data?.uploadTime) }}</el-descriptions-item>
            <el-descriptions-item label="预览类型">
              <el-tag :type="getPreviewTypeTag(previewDialog.data?.previewType)">
                {{ getPreviewTypeLabel(previewDialog.data?.previewType) }}
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
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadFile, UploadFiles } from 'element-plus'
import { documentApi } from '@/api/document'
import DocumentViewer from '@/components/DocumentViewer.vue'
import { useDocumentsPage } from '@/composables/useDocumentsPage'
import type { Document, DocumentPreview } from '@/types'
import { formatDateTime } from '@/utils/date'
import {
  formatFileSize,
  getDocumentStatusLabel,
  getDocumentStatusTag,
  getFileIconColor,
  getFileTypeTag,
  getPreviewTypeLabel,
  getPreviewTypeTag
} from '@/utils/documentDisplay'

const {
  loading,
  knowledgeBases,
  searchForm,
  documents,
  page,
  fetchDocuments,
  handleSearch,
  handleReset,
  handleSizeChange,
  handleCurrentChange
} = useDocumentsPage()

const reprocessingId = ref<string | null>(null)
const uploadRef = ref()

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
    fetchDocuments()
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

const handleReprocess = async (row: Document) => {
  try {
    await ElMessageBox.confirm(
      `将对「${row.name}」使用已保存的文件重新向量化并覆盖原有向量片段，是否继续？`,
      '重新向量化',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'info' }
    )
  } catch {
    return
  }
  reprocessingId.value = row.id
  try {
    await documentApi.reprocess(row.id)
    ElMessage.success('已提交重新向量化，请稍后刷新查看状态')
    await fetchDocuments()
  } catch (error) {
    ElMessage.error('提交失败，请稍后重试')
  } finally {
    reprocessingId.value = null
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
      fetchDocuments()
    } catch (error) {
      ElMessage.error('删除失败')
    }
  })
}

// 批量操作相关
const selectedDocuments = ref<Document[]>([])

const handleSelectionChange = (selection: Document[]) => {
  selectedDocuments.value = selection
}

const handleBatchDelete = async () => {
  if (selectedDocuments.value.length === 0) {
    ElMessage.warning('请先选择要删除的文档')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedDocuments.value.length} 个文档吗？`,
      '批量删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  const ids = selectedDocuments.value.map(doc => doc.id)
  try {
    const result = await documentApi.batchDelete(ids)
    if (result.failed > 0) {
      ElMessage.warning(`批量删除完成：成功 ${result.success} 个，失败 ${result.failed} 个`)
    } else {
      ElMessage.success(`成功删除 ${result.success} 个文档`)
    }
    selectedDocuments.value = []
    fetchDocuments()
  } catch (error) {
    ElMessage.error('批量删除失败')
  }
}

const handleBatchReprocess = async () => {
  if (selectedDocuments.value.length === 0) {
    ElMessage.warning('请先选择要重新处理的文档')
    return
  }

  const processingDocs = selectedDocuments.value.filter(doc => doc.status === 'processing')
  if (processingDocs.length > 0) {
    ElMessage.warning(`选中的文档中有 ${processingDocs.length} 个正在处理中，请取消选择或等待处理完成`)
    return
  }

  try {
    await ElMessageBox.confirm(
      `将对选中的 ${selectedDocuments.value.length} 个文档重新向量化，是否继续？`,
      '批量重新向量化',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
  } catch {
    return
  }

  const ids = selectedDocuments.value.map(doc => doc.id)
  try {
    const result = await documentApi.batchReprocess(ids)
    if (result.failed > 0) {
      ElMessage.warning(`批量重新处理完成：成功 ${result.success} 个，失败 ${result.failed} 个`)
    } else {
      ElMessage.success(`已提交 ${result.success} 个文档重新处理`)
    }
    selectedDocuments.value = []
    fetchDocuments()
  } catch (error) {
    ElMessage.error('批量重新处理失败')
  }
}

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

.header-actions {
  display: flex;
  gap: 10px;
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

.status-cell {
  display: flex;
  align-items: center;
  gap: 4px;
}

.operation-buttons {
  display: flex;
  flex-wrap: nowrap;
  white-space: nowrap;
  gap: 4px;
}

.operation-buttons .el-button {
  padding: 4px 8px;
  font-size: 13px;
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
