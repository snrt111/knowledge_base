import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { documentApi } from '@/api/document'
import { knowledgeBaseApi } from '@/api/knowledgeBase'
import type { Document, KnowledgeBase } from '@/types'

export interface UseDocumentsPageOptions {
  /** 挂载时是否拉取列表（默认 true） */
  fetchOnMount?: boolean
}

/**
 * 文档列表页：知识库选项、分页、筛选与列表加载（容器组件只负责 UI 与对话框状态）。
 */
export function useDocumentsPage(options: UseDocumentsPageOptions = {}) {
  const { fetchOnMount = true } = options
  const route = useRoute()

  const loading = ref(false)
  const knowledgeBases = ref<KnowledgeBase[]>([])

  const searchForm = reactive({
    knowledgeBaseId: (route.query.knowledgeBaseId as string) || '',
    keyword: ''
  })

  const documents = ref<Document[]>([])

  const page = reactive({
    current: 1,
    size: 10,
    total: 0
  })

  const fetchKnowledgeBases = async () => {
    try {
      knowledgeBases.value = await knowledgeBaseApi.listAll()
    } catch {
      ElMessage.error('获取知识库列表失败')
    }
  }

  const fetchDocuments = async () => {
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
    } catch {
      ElMessage.error('获取文档列表失败')
    } finally {
      loading.value = false
    }
  }

  const handleSearch = () => {
    page.current = 1
    fetchDocuments()
  }

  const handleReset = () => {
    searchForm.knowledgeBaseId = ''
    searchForm.keyword = ''
    handleSearch()
  }

  const handleSizeChange = () => {
    fetchDocuments()
  }

  const handleCurrentChange = () => {
    fetchDocuments()
  }

  onMounted(async () => {
    await Promise.all([
      fetchKnowledgeBases(),
      fetchOnMount ? fetchDocuments() : Promise.resolve()
    ])
  })

  return {
    loading,
    knowledgeBases,
    searchForm,
    documents,
    page,
    fetchKnowledgeBases,
    fetchDocuments,
    handleSearch,
    handleReset,
    handleSizeChange,
    handleCurrentChange
  }
}
