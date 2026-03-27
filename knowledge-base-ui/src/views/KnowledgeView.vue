<template>
  <div class="knowledge-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">
            <el-icon><Collection /></el-icon>
            知识库管理
          </span>
          <el-button type="primary" @click="showCreateDialog">
            <el-icon><Plus /></el-icon>
            新建知识库
          </el-button>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="搜索知识库名称" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="knowledgeBases" v-loading="loading" stripe>
        <el-table-column type="index" width="60" label="序号" />
        <el-table-column prop="name" label="知识库名称" min-width="150">
          <template #default="{ row }">
            <div class="kb-name">
              <el-icon size="18" color="#409EFF"><Collection /></el-icon>
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="documentCount" label="文档数量" width="100" align="center">
          <template #default="{ row }">
            <el-tag>{{ row.documentCount || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.updateTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button type="primary" link @click="handleManageDocs(row)">
              <el-icon><Document /></el-icon>
              文档
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
      v-model="dialog.visible"
      :title="dialog.isEdit ? '编辑知识库' : '新建知识库'"
      width="500px"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="80px"
      >
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入知识库名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入知识库描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="dialog.loading">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useRouter } from 'vue-router'
import { knowledgeBaseApi } from '@/api/knowledgeBase'
import type { KnowledgeBase } from '@/types'
import { formatDateTime } from '@/utils/date'

const router = useRouter()
const loading = ref(false)
const formRef = ref<FormInstance>()

const knowledgeBases = ref<KnowledgeBase[]>([])

const searchForm = reactive({
  keyword: ''
})

const page = reactive({
  current: 1,
  size: 10,
  total: 0
})

const dialog = reactive({
  visible: false,
  isEdit: false,
  loading: false,
  currentId: ''
})

const form = reactive({
  name: '',
  description: ''
})

const rules: FormRules = {
  name: [
    { required: true, message: '请输入知识库名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  description: [
    { max: 200, message: '描述不能超过 200 个字符', trigger: 'blur' }
  ]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await knowledgeBaseApi.list(page.current, page.size, searchForm.keyword || undefined)
    knowledgeBases.value = res.list
    page.total = res.total
  } catch (error) {
    ElMessage.error('获取知识库列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.current = 1
  fetchData()
}

const handleReset = () => {
  searchForm.keyword = ''
  handleSearch()
}

const showCreateDialog = () => {
  dialog.isEdit = false
  dialog.currentId = ''
  form.name = ''
  form.description = ''
  dialog.visible = true
}

const handleEdit = (row: KnowledgeBase) => {
  dialog.isEdit = true
  dialog.currentId = row.id
  form.name = row.name
  form.description = row.description
  dialog.visible = true
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      dialog.loading = true
      try {
        if (dialog.isEdit) {
          await knowledgeBaseApi.update(dialog.currentId, {
            name: form.name,
            description: form.description
          })
          ElMessage.success('修改成功')
        } else {
          await knowledgeBaseApi.create({
            name: form.name,
            description: form.description
          })
          ElMessage.success('创建成功')
        }
        dialog.visible = false
        fetchData()
      } catch (error) {
        ElMessage.error(dialog.isEdit ? '修改失败' : '创建失败')
      } finally {
        dialog.loading = false
      }
    }
  })
}

const handleDelete = (row: KnowledgeBase) => {
  ElMessageBox.confirm(
    `确定要删除知识库 "${row.name}" 吗？删除后无法恢复！`,
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await knowledgeBaseApi.delete(row.id)
      ElMessage.success('删除成功')
      fetchData()
    } catch (error) {
      ElMessage.error('删除失败')
    }
  })
}

const handleManageDocs = (row: KnowledgeBase) => {
  router.push({
    path: '/documents',
    query: { knowledgeBaseId: row.id }
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
  fetchData()
})
</script>

<style scoped>
.knowledge-container {
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

.kb-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
