<template>
  <div class="user-list-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">
            <el-icon><User /></el-icon>
            用户管理
          </span>
          <el-button type="primary" @click="showCreateDialog = true">
            <el-icon><Plus /></el-icon>
            新增用户
          </el-button>
        </div>
      </template>

      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="用户名">
          <el-input v-model="queryParams.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="queryParams.nickname" placeholder="请输入昵称" clearable />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="queryParams.email" placeholder="请输入邮箱" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.isActive" placeholder="请选择状态" clearable>
            <el-option label="启用" :value="true" />
            <el-option label="禁用" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" stripe border v-loading="loading">
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="nickname" label="昵称" width="150" />
        <el-table-column prop="email" label="邮箱" width="200" />
        <el-table-column prop="phone" label="手机号" width="150" />
        <el-table-column prop="isActive" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isActive ? 'success' : 'danger'">
              {{ row.isActive ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button type="primary" link size="small" @click="handleAssignRole(row)">
              <el-icon><UserFilled /></el-icon>
              分配角色
            </el-button>
            <el-button type="warning" link size="small" @click="handleResetPassword(row)">
              <el-icon><Key /></el-icon>
              重置密码
            </el-button>
            <el-button type="warning" link size="small" @click="handleToggle(row)">
              <el-icon><Switch /></el-icon>
              {{ row.isActive ? '禁用' : '启用' }}
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSearch"
          @current-change="handleSearch"
        />
      </div>
    </el-card>

    <!-- 新增用户对话框 -->
    <el-dialog v-model="showCreateDialog" title="新增用户" width="500px">
      <el-form :model="createForm" :rules="createRules" ref="createFormRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="createForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="createForm.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="createForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="createForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="createForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="createForm.roleIds" multiple placeholder="请选择角色" style="width: 100%">
            <el-option v-for="role in allRoles" :key="role.id" :label="role.name" :value="role.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">确定</el-button>
      </template>
    </el-dialog>

    <!-- 编辑用户对话框 -->
    <el-dialog v-model="showEditDialog" title="编辑用户" width="500px">
      <el-form :model="editForm" :rules="editRules" ref="editFormRef" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="editForm.username" disabled />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="editForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="editForm.phone" placeholder="请输入手机号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="submitEdit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配角色对话框 -->
    <el-dialog v-model="showAssignRoleDialog" :title="`分配角色 - ${currentUser?.username}`" width="400px">
      <el-checkbox-group v-model="selectedRoleIds">
        <el-checkbox v-for="role in allRoles" :key="role.id" :label="role.id">{{ role.name }}</el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="showAssignRoleDialog = false">取消</el-button>
        <el-button type="primary" @click="submitAssignRole">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  User,
  Plus,
  Search,
  Edit,
  UserFilled,
  Key,
  Switch,
  Delete
} from '@element-plus/icons-vue'
import {
  getUserPage,
  createUser,
  updateUser,
  deleteUser,
  toggleUser,
  resetPassword,
  assignRoles
} from '@/api/user'
import { getAllRoles } from '@/api/role'
import type { User as UserType, UserQuery, UserCreate, UserUpdate, Role } from '@/types/user'

const loading = ref(false)
const tableData = ref<UserType[]>([])
const total = ref(0)
const allRoles = ref<Role[]>([])

const queryParams = reactive<UserQuery>({
  username: '',
  nickname: '',
  email: '',
  isActive: undefined,
  pageNum: 1,
  pageSize: 10
})

// 新增用户对话框
const showCreateDialog = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive<UserCreate>({
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
  avatar: '',
  roleIds: []
})

const createRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 50, message: '用户名长度必须在 2-50 个字符之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 100, message: '密码长度必须在 6-100 个字符之间', trigger: 'blur' }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' }
  ]
}

// 编辑用户对话框
const showEditDialog = ref(false)
const editFormRef = ref<FormInstance>()
const editForm = reactive({
  id: '',
  username: '',
  nickname: '',
  email: '',
  phone: '',
  avatar: ''
})

const editRules: FormRules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' }
  ]
}

// 分配角色对话框
const showAssignRoleDialog = ref(false)
const currentUser = ref<UserType | null>(null)
const selectedRoleIds = ref<string[]>([])

const fetchData = async () => {
  loading.value = true
  try {
    const response = await getUserPage(queryParams)
    // response.data 是 PageResult<User>
    tableData.value = response.data.list || []
    total.value = response.data.total || 0
  } catch (error) {
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const fetchAllRoles = async () => {
  try {
    const response = await getAllRoles()
    // response.data 是 Role[]
    allRoles.value = response.data || []
  } catch (error) {
    ElMessage.error('获取角色列表失败')
  }
}

const handleSearch = () => {
  queryParams.pageNum = 1
  fetchData()
}

const handleReset = () => {
  queryParams.username = ''
  queryParams.nickname = ''
  queryParams.email = ''
  queryParams.isActive = undefined
  queryParams.pageNum = 1
  handleSearch()
}

const submitCreate = async () => {
  if (!createFormRef.value) return

  await createFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await createUser(createForm)
        ElMessage.success('新增成功')
        showCreateDialog.value = false
        fetchData()
        // 重置表单
        createForm.username = ''
        createForm.password = ''
        createForm.nickname = ''
        createForm.email = ''
        createForm.phone = ''
        createForm.roleIds = []
      } catch (error) {
        ElMessage.error('新增失败')
      }
    }
  })
}

const handleEdit = (row: UserType) => {
  editForm.id = row.id
  editForm.username = row.username
  editForm.nickname = row.nickname || ''
  editForm.email = row.email || ''
  editForm.phone = row.phone || ''
  editForm.avatar = row.avatar || ''
  showEditDialog.value = true
}

const submitEdit = async () => {
  if (!editFormRef.value) return

  await editFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await updateUser({
          id: editForm.id,
          nickname: editForm.nickname,
          email: editForm.email,
          phone: editForm.phone,
          avatar: editForm.avatar
        })
        ElMessage.success('编辑成功')
        showEditDialog.value = false
        fetchData()
      } catch (error) {
        ElMessage.error('编辑失败')
      }
    }
  })
}

const handleAssignRole = (row: UserType) => {
  currentUser.value = row
  selectedRoleIds.value = row.roles?.map(r => r.id) || []
  showAssignRoleDialog.value = true
}

const submitAssignRole = async () => {
  if (!currentUser.value) return

  try {
    await assignRoles(currentUser.value.id, selectedRoleIds.value)
    ElMessage.success('分配角色成功')
    showAssignRoleDialog.value = false
    fetchData()
  } catch (error) {
    ElMessage.error('分配角色失败')
  }
}

const handleResetPassword = async (row: UserType) => {
  try {
    await ElMessageBox.confirm('确定要重置该用户的密码吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await resetPassword(row.id, '123456')
    ElMessage.success('密码已重置为：123456')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('重置密码失败')
    }
  }
}

const handleToggle = async (row: UserType) => {
  try {
    await toggleUser(row.id)
    ElMessage.success(row.isActive ? '禁用成功' : '启用成功')
    fetchData()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const handleDelete = async (row: UserType) => {
  try {
    await ElMessageBox.confirm('确定要删除该用户吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteUser(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  fetchData()
  fetchAllRoles()
})
</script>

<style scoped>
.user-list-container {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
}

.search-form {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.el-checkbox-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.el-checkbox {
  margin-right: 0;
}
</style>
