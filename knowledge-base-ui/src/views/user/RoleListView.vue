<template>
  <div class="role-list-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">
            <el-icon><UserFilled /></el-icon>
            角色管理
          </span>
          <el-button type="primary" @click="showCreateDialog = true">
            <el-icon><Plus /></el-icon>
            新增角色
          </el-button>
        </div>
      </template>

      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="角色名称">
          <el-input v-model="queryParams.name" placeholder="请输入角色名称" clearable />
        </el-form-item>
        <el-form-item label="角色编码">
          <el-input v-model="queryParams.code" placeholder="请输入角色编码" clearable />
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
        <el-table-column prop="name" label="角色名称" width="150" />
        <el-table-column prop="code" label="角色编码" width="150" />
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="sort" label="排序" width="100" />
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
            <el-button type="primary" link size="small" @click="handleAssignPermission(row)">
              <el-icon><Lock /></el-icon>
              分配权限
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

    <!-- 新增角色对话框 -->
    <el-dialog v-model="showCreateDialog" title="新增角色" width="500px">
      <el-form :model="createForm" :rules="createRules" ref="createFormRef" label-width="80px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="createForm.name" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" prop="code">
          <el-input v-model="createForm.code" placeholder="请输入角色编码" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" placeholder="请输入描述" :rows="3" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="createForm.sort" :min="0" :max="999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">确定</el-button>
      </template>
    </el-dialog>

    <!-- 编辑角色对话框 -->
    <el-dialog v-model="showEditDialog" title="编辑角色" width="500px">
      <el-form :model="editForm" :rules="editRules" ref="editFormRef" label-width="80px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="editForm.name" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" prop="code">
          <el-input v-model="editForm.code" placeholder="请输入角色编码" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" placeholder="请输入描述" :rows="3" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="editForm.sort" :min="0" :max="999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="submitEdit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配权限对话框 -->
    <el-dialog v-model="showAssignPermissionDialog" :title="`分配权限 - ${currentRole?.name}`" width="500px">
      <div class="permission-tree">
        <div v-for="permission in permissionTree" :key="permission.id" class="permission-item">
          <el-checkbox v-model="selectedPermissionIds" :label="permission.id">{{ permission.name }}</el-checkbox>
          <div v-if="permission.children && permission.children.length > 0" class="permission-children">
            <el-checkbox v-for="child in permission.children" :key="child.id" v-model="selectedPermissionIds" :label="child.id">{{ child.name }}</el-checkbox>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="showAssignPermissionDialog = false">取消</el-button>
        <el-button type="primary" @click="submitAssignPermission">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  UserFilled,
  Plus,
  Search,
  Edit,
  Lock,
  Switch,
  Delete
} from '@element-plus/icons-vue'
import {
  getRolePage,
  createRole,
  updateRole,
  deleteRole,
  toggleRole,
  assignPermissions
} from '@/api/role'
import { getPermissionTree } from '@/api/permission'
import type { Role, RoleQuery, RoleCreate, Permission } from '@/types/user'

const loading = ref(false)
const tableData = ref<Role[]>([])
const total = ref(0)
const permissionTree = ref<Permission[]>([])

const queryParams = reactive<RoleQuery>({
  name: '',
  code: '',
  pageNum: 1,
  pageSize: 10
})

// 新增角色对话框
const showCreateDialog = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive<RoleCreate>({
  name: '',
  code: '',
  description: '',
  sort: 0,
  permissionIds: []
})

const createRules: FormRules = {
  name: [
    { required: true, message: '请输入角色名称', trigger: 'blur' },
    { min: 2, max: 50, message: '角色名称长度必须在 2-50 个字符之间', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { min: 2, max: 50, message: '角色编码长度必须在 2-50 个字符之间', trigger: 'blur' }
  ]
}

// 编辑角色对话框
const showEditDialog = ref(false)
const editFormRef = ref<FormInstance>()
const editForm = reactive({
  id: '',
  name: '',
  code: '',
  description: '',
  sort: 0
})

const editRules: FormRules = {
  name: [
    { required: true, message: '请输入角色名称', trigger: 'blur' },
    { min: 2, max: 50, message: '角色名称长度必须在 2-50 个字符之间', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { min: 2, max: 50, message: '角色编码长度必须在 2-50 个字符之间', trigger: 'blur' }
  ]
}

// 分配权限对话框
const showAssignPermissionDialog = ref(false)
const currentRole = ref<Role | null>(null)
const selectedPermissionIds = ref<string[]>([])

const fetchData = async () => {
  loading.value = true
  try {
    const pageResult = await getRolePage(queryParams)
    tableData.value = pageResult.list || []
    total.value = pageResult.total || 0
  } catch (error) {
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const fetchPermissionTree = async () => {
  try {
    permissionTree.value = await getPermissionTree() || []
  } catch (error) {
    ElMessage.error('获取权限树失败')
  }
}

const handleSearch = () => {
  queryParams.pageNum = 1
  fetchData()
}

const handleReset = () => {
  queryParams.name = ''
  queryParams.code = ''
  queryParams.pageNum = 1
  handleSearch()
}

const submitCreate = async () => {
  if (!createFormRef.value) return

  await createFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await createRole(createForm)
        ElMessage.success('新增成功')
        showCreateDialog.value = false
        fetchData()
        // 重置表单
        createForm.name = ''
        createForm.code = ''
        createForm.description = ''
        createForm.sort = 0
        createForm.permissionIds = []
      } catch (error) {
        ElMessage.error('新增失败')
      }
    }
  })
}

const handleEdit = (row: Role) => {
  editForm.id = row.id
  editForm.name = row.name
  editForm.code = row.code
  editForm.description = row.description || ''
  editForm.sort = row.sort || 0
  showEditDialog.value = true
}

const submitEdit = async () => {
  if (!editFormRef.value) return

  await editFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await updateRole({
          id: editForm.id,
          name: editForm.name,
          code: editForm.code,
          description: editForm.description,
          sort: editForm.sort
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

const handleAssignPermission = (row: Role) => {
  currentRole.value = row
  selectedPermissionIds.value = row.permissions?.map(p => p.id) || []
  showAssignPermissionDialog.value = true
}

const submitAssignPermission = async () => {
  if (!currentRole.value) return

  try {
    await assignPermissions(currentRole.value.id, selectedPermissionIds.value)
    ElMessage.success('分配权限成功')
    showAssignPermissionDialog.value = false
    fetchData()
  } catch (error) {
    ElMessage.error('分配权限失败')
  }
}

const handleToggle = async (row: Role) => {
  try {
    await toggleRole(row.id)
    ElMessage.success(row.isActive ? '禁用成功' : '启用成功')
    fetchData()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const handleDelete = async (row: Role) => {
  try {
    await ElMessageBox.confirm('确定要删除该角色吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteRole(row.id)
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
  fetchPermissionTree()
})
</script>

<style scoped>
.role-list-container {
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

.permission-tree {
  max-height: 400px;
  overflow-y: auto;
}

.permission-item {
  margin-bottom: 15px;
}

.permission-children {
  margin-left: 20px;
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
</style>
