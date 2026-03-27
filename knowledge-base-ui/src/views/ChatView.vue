<template>
  <div class="chat-container">
    <div class="chat-sidebar">
      <div class="sidebar-header">
        <el-button type="primary" @click="createNewSession" style="width: 100%">
          <el-icon><Plus /></el-icon>
          新建对话
        </el-button>
      </div>
      <div class="session-list">
        <div
          v-for="session in sessions"
          :key="session.id"
          :class="['session-item', currentSession?.id === session.id ? 'active' : '']"
          @click="selectSession(session)"
        >
          <div class="session-title">
            <el-icon><ChatDotRound /></el-icon>
            <span>{{ session.title }}</span>
          </div>
          <div class="session-meta">
            <span v-if="session.knowledgeBaseName" class="kb-tag">
              {{ session.knowledgeBaseName }}
            </span>
            <span class="message-count">{{ session.messageCount }}条消息</span>
          </div>
          <el-button
            class="delete-btn"
            type="danger"
            link
            size="small"
            @click.stop="deleteSession(session)"
          >
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>
    </div>

    <div class="chat-main">
      <div class="chat-header">
        <div class="header-title">
          <span v-if="currentSession">{{ currentSession.title }}</span>
          <span v-else>AI 智能助手</span>
        </div>
        <div class="header-actions">
          <el-select
            v-model="selectedKnowledgeBase"
            placeholder="选择知识库"
            clearable
            style="width: 200px"
            size="small"
          >
            <el-option
              v-for="kb in knowledgeBases"
              :key="kb.id"
              :label="kb.name"
              :value="kb.id"
            />
          </el-select>
        </div>
      </div>

      <div class="chat-messages" ref="messagesContainer">
        <div v-if="!currentSession" class="empty-state">
          <el-icon size="64" color="#dcdfe6"><ChatDotRound /></el-icon>
          <p>选择一个对话或创建新对话开始聊天</p>
        </div>

        <template v-else>
          <div
            v-for="(msg, index) in messages"
            :key="index"
            :class="['message', msg.role]"
          >
            <div class="message-avatar">
              <el-avatar
                :size="40"
                :src="msg.role === 'user' ? userAvatar : aiAvatar"
              />
            </div>
            <div class="message-content">
              <div class="message-header">
                <span class="role-name">{{ msg.role === 'user' ? '我' : 'AI助手' }}</span>
                <span class="time">{{ formatTime(msg.createTime) }}</span>
              </div>
              <div class="message-body" v-html="renderMarkdown(msg.content)"></div>
              <!-- 文档来源 -->
              <DocumentSources
                v-if="msg.role === 'assistant' && msg.documentSources && msg.documentSources.length > 0"
                :sources="msg.documentSources"
                :default-collapsed="true"
                @view-document="viewDocument"
              />
            </div>
          </div>

          <div v-if="isLoading" class="message assistant">
            <div class="message-avatar">
              <el-avatar :size="40" :src="aiAvatar" />
            </div>
            <div class="message-content">
              <div class="message-header">
                <span class="role-name">AI助手</span>
              </div>
              <div class="message-body">
                <el-skeleton :rows="2" animated />
              </div>
            </div>
          </div>
        </template>
      </div>

      <div class="chat-input" v-if="currentSession">
        <el-input
          v-model="inputMessage"
          type="textarea"
          :rows="3"
          placeholder="请输入您的问题..."
          @keydown.enter.prevent="handleSend"
        />
        <div class="input-actions">
          <el-checkbox v-model="isStream">流式输出</el-checkbox>
          <el-button
            type="primary"
            :loading="isLoading"
            @click="handleSend"
            :disabled="!inputMessage.trim()"
          >
            <el-icon><Promotion /></el-icon>
            发送
          </el-button>
        </div>
      </div>
    </div>

    <!-- 文档预览对话框 -->
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
            <el-descriptions-item label="文件类型">{{ previewDialog.data?.type?.toUpperCase() }}</el-descriptions-item>
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
import { ref, nextTick, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { chatApi } from '@/api/chat'
import { knowledgeBaseApi } from '@/api/knowledgeBase'
import { documentApi } from '@/api/document'
import { renderMarkdown } from '@/utils/markdown'
import DocumentSources from '@/components/DocumentSources.vue'
import DocumentViewer from '@/components/DocumentViewer.vue'
import type { ChatSession, ChatMessage, KnowledgeBase, DocumentPreview } from '@/types'

const sessions = ref<ChatSession[]>([])
const currentSession = ref<ChatSession | null>(null)
const messages = ref<ChatMessage[]>([])
const knowledgeBases = ref<KnowledgeBase[]>([])
const selectedKnowledgeBase = ref('')

const inputMessage = ref('')
const isLoading = ref(false)
const isStream = ref(true)
const messagesContainer = ref<HTMLDivElement>()

// 预览对话框数据
const previewDialog = reactive({
  visible: false,
  loading: false,
  data: null as DocumentPreview | null,
  currentDocId: ''
})

const userAvatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
const aiAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

const formatTime = (time: string) => {
  return new Date(time).toLocaleString()
}

const fetchSessions = async () => {
  try {
    const res = await chatApi.listSessions(1, 50)
    sessions.value = res.list
  } catch (error) {
    ElMessage.error('获取会话列表失败')
  }
}

const fetchKnowledgeBases = async () => {
  try {
    knowledgeBases.value = await knowledgeBaseApi.listAll()
  } catch (error) {
    console.error('获取知识库列表失败')
  }
}

const createNewSession = async () => {
  try {
    const session = await chatApi.createSession('新对话', selectedKnowledgeBase.value)
    sessions.value.unshift(session)
    selectSession(session)
    ElMessage.success('创建成功')
  } catch (error) {
    ElMessage.error('创建会话失败')
  }
}

const selectSession = async (session: ChatSession) => {
  currentSession.value = session
  selectedKnowledgeBase.value = session.knowledgeBaseId || ''
  await loadSessionMessages(session.id)
}

const loadSessionMessages = async (sessionId: string) => {
  try {
    messages.value = await chatApi.getSessionMessages(sessionId)
    scrollToBottom()
  } catch (error) {
    ElMessage.error('获取消息失败')
  }
}

const deleteSession = async (session: ChatSession) => {
  ElMessageBox.confirm(
    `确定要删除对话 "${session.title}" 吗？`,
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await chatApi.deleteSession(session.id)
      sessions.value = sessions.value.filter(s => s.id !== session.id)
      if (currentSession.value?.id === session.id) {
        currentSession.value = null
        messages.value = []
      }
      ElMessage.success('删除成功')
    } catch (error) {
      ElMessage.error('删除失败')
    }
  })
}

const handleSend = async () => {
  const message = inputMessage.value.trim()
  if (!message || isLoading.value || !currentSession.value) return

  const userMessage: ChatMessage = {
    id: Date.now().toString(),
    role: 'user',
    content: message,
    createTime: new Date().toISOString()
  }
  messages.value.push(userMessage)
  inputMessage.value = ''
  scrollToBottom()

  isLoading.value = true

  try {
    if (isStream.value) {
      await handleStreamChat(message)
    } else {
      await handleNormalChat(message)
    }
    fetchSessions()
  } catch (error) {
    ElMessage.error('发送消息失败')
  } finally {
    isLoading.value = false
    scrollToBottom()
  }
}

const handleNormalChat = async (message: string) => {
  const response = await chatApi.sendMessage(
    message,
    currentSession.value?.id,
    selectedKnowledgeBase.value || undefined
  )
  messages.value.push({
    id: Date.now().toString(),
    role: 'assistant',
    content: response.content,
    createTime: new Date().toISOString(),
    documentSources: response.sources || []
  })
}

const handleStreamChat = (message: string) => {
  return new Promise<void>((resolve) => {
    const assistantMessage: ChatMessage = {
      id: Date.now().toString(),
      role: 'assistant',
      content: '',
      createTime: new Date().toISOString(),
      documentSources: []
    }
    messages.value.push(assistantMessage)
    const messageIndex = messages.value.length - 1

    const { eventSource } = chatApi.streamMessage(
      message,
      currentSession.value?.id,
      selectedKnowledgeBase.value || undefined
    )

    eventSource.onmessage = (event) => {
      if (event.data && messages.value[messageIndex]) {
        try {
          const response = JSON.parse(event.data)
          const currentMsg = messages.value[messageIndex]

          if (response.type === 'content' && response.content) {
            messages.value[messageIndex] = {
              ...currentMsg,
              content: currentMsg.content + response.content
            }
          } else if (response.type === 'sources' && response.sources) {
            messages.value[messageIndex] = {
              ...currentMsg,
              documentSources: response.sources
            }
          } else if (response.type === 'complete') {
            // 完成标记，不做处理
          }
        } catch (e) {
          // 兼容旧格式，直接追加内容
          const currentMsg = messages.value[messageIndex]
          messages.value[messageIndex] = {
            ...currentMsg,
            content: currentMsg.content + event.data
          }
        }
        scrollToBottom()
      }
    }

    eventSource.onerror = () => {
      eventSource.close()
      resolve()
    }

    eventSource.onopen = () => {
      isLoading.value = false
    }
  })
}

// 格式化文件大小
const formatFileSize = (bytes: number) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 获取预览类型标签样式
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

// 获取预览类型文本
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

// 查看文档 - 打开预览对话框
const viewDocument = async (documentId: string) => {
  previewDialog.currentDocId = documentId
  previewDialog.visible = true
  previewDialog.loading = true

  try {
    const data = await documentApi.preview(documentId)
    previewDialog.data = data
  } catch (error) {
    ElMessage.error('获取文档预览失败')
    previewDialog.data = null
  } finally {
    previewDialog.loading = false
  }
}

// 从预览对话框下载文档
const handleDownloadFromPreview = () => {
  if (previewDialog.currentDocId) {
    const url = documentApi.download(previewDialog.currentDocId)
    const link = document.createElement('a')
    link.href = url
    link.target = '_blank'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  }
}

onMounted(() => {
  fetchSessions()
  fetchKnowledgeBases()
})
</script>

<style scoped>
.chat-container {
  display: flex;
  height: calc(100vh - 140px);
  gap: 16px;
}

.chat-sidebar {
  width: 280px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid #e4e7ed;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.session-item {
  padding: 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 4px;
  position: relative;
  transition: all 0.2s;
}

.session-item:hover {
  background-color: #f5f7fa;
}

.session-item.active {
  background-color: #ecf5ff;
}

.session-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 4px;
}

.session-title span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #909399;
}

.kb-tag {
  background-color: #e6f7ff;
  color: #1890ff;
  padding: 2px 6px;
  border-radius: 4px;
}

.delete-btn {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  opacity: 0;
  transition: opacity 0.2s;
}

.session-item:hover .delete-btn {
  opacity: 1;
}

.chat-main {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
}

.chat-header {
  padding: 16px 20px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background-color: #f5f7fa;
}

.empty-state {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.empty-state p {
  margin-top: 16px;
}

.message {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.message.user {
  flex-direction: row-reverse;
}

.message.user .message-content {
  align-items: flex-end;
}

.message.user .message-body {
  background-color: #409EFF;
  color: #fff;
}

.message-content {
  display: flex;
  flex-direction: column;
  max-width: 70%;
}

.message-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  font-size: 12px;
  color: #909399;
}

.message.user .message-header {
  flex-direction: row-reverse;
}

.role-name {
  font-weight: 500;
}

.message-body {
  background-color: #fff;
  padding: 12px 16px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  line-height: 1.6;
}

.message-body :deep(pre) {
  background-color: #1e1e1e;
  border-radius: 6px;
  padding: 12px;
  overflow-x: auto;
  margin: 8px 0;
}

.message-body :deep(code) {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
}

.message-body :deep(p) {
  margin: 0 0 8px 0;
}

.message-body :deep(p:last-child) {
  margin-bottom: 0;
}

/* Markdown 标题样式 */
.message-body :deep(h1),
.message-body :deep(h2),
.message-body :deep(h3),
.message-body :deep(h4),
.message-body :deep(h5),
.message-body :deep(h6) {
  margin: 16px 0 8px 0;
  font-weight: 600;
  line-height: 1.4;
}

.message-body :deep(h1) {
  font-size: 1.5em;
  border-bottom: 1px solid #e4e7ed;
  padding-bottom: 8px;
}

.message-body :deep(h2) {
  font-size: 1.3em;
  border-bottom: 1px solid #e4e7ed;
  padding-bottom: 6px;
}

.message-body :deep(h3) {
  font-size: 1.15em;
}

.message-body :deep(h4) {
  font-size: 1em;
}

.message-body :deep(h5),
.message-body :deep(h6) {
  font-size: 0.9em;
}

/* Markdown 列表样式 */
.message-body :deep(ul),
.message-body :deep(ol) {
  margin: 8px 0;
  padding-left: 24px;
}

.message-body :deep(li) {
  margin: 4px 0;
}

/* Markdown 引用样式 */
.message-body :deep(blockquote) {
  margin: 8px 0;
  padding: 8px 16px;
  border-left: 4px solid #409eff;
  background-color: #f5f7fa;
  color: #606266;
}

/* Markdown 表格样式 */
.message-body :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 8px 0;
}

.message-body :deep(th),
.message-body :deep(td) {
  border: 1px solid #dcdfe6;
  padding: 8px 12px;
  text-align: left;
}

.message-body :deep(th) {
  background-color: #f5f7fa;
  font-weight: 600;
}

/* Markdown 水平线样式 */
.message-body :deep(hr) {
  border: none;
  border-top: 1px solid #dcdfe6;
  margin: 16px 0;
}

/* Markdown 链接样式 */
.message-body :deep(a) {
  color: #409eff;
  text-decoration: none;
}

.message-body :deep(a:hover) {
  text-decoration: underline;
}

/* Markdown 粗体和斜体 */
.message-body :deep(strong) {
  font-weight: 600;
}

.message-body :deep(em) {
  font-style: italic;
}

/* 行内代码样式 */
.message-body :deep(code:not(pre code)) {
  background-color: #f5f7fa;
  padding: 2px 6px;
  border-radius: 4px;
  color: #e83e8c;
  font-size: 0.9em;
}

.chat-input {
  padding: 16px 20px;
  border-top: 1px solid #e4e7ed;
  background-color: #fff;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}

/* 预览对话框样式 */
.preview-content {
  min-height: 400px;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.preview-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.preview-info {
  margin-bottom: 16px;
}

.preview-body {
  min-height: 300px;
  max-height: 500px;
  overflow: auto;
}
</style>
