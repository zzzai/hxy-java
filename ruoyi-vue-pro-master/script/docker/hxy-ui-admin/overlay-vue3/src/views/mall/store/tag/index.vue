<template>
  <ContentWrap>
    <el-form :inline="true" :model="queryParams" class="-mb-15px" label-width="82px">
      <el-form-item label="标签编码" prop="code">
        <el-input v-model="queryParams.code" class="!w-180px" clearable placeholder="请输入标签编码" @keyup.enter="getList" />
      </el-form-item>
      <el-form-item label="标签名称" prop="name">
        <el-input v-model="queryParams.name" class="!w-220px" clearable placeholder="请输入标签名称" @keyup.enter="getList" />
      </el-form-item>
      <el-form-item label="标签组" prop="groupId">
        <el-select v-model="queryParams.groupId" class="!w-180px" clearable filterable placeholder="请选择标签组">
          <el-option v-for="item in groupOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" class="!w-140px" clearable placeholder="请选择状态">
          <el-option :value="1" label="启用" />
          <el-option :value="0" label="停用" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="getList">
          <Icon class="mr-5px" icon="ep:search" />
          搜索
        </el-button>
        <el-button @click="resetQuery">
          <Icon class="mr-5px" icon="ep:refresh" />
          重置
        </el-button>
        <el-button v-hasPermi="['product:store-tag:create']" type="primary" plain @click="openForm()">
          <Icon class="mr-5px" icon="ep:plus" />
          新增标签
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="ID" prop="id" width="90" />
      <el-table-column label="标签编码" prop="code" min-width="140" />
      <el-table-column label="标签名称" prop="name" min-width="180" />
      <el-table-column label="标签组" prop="groupName" min-width="140" />
      <el-table-column label="状态" prop="status" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="排序" prop="sort" width="90" />
      <el-table-column label="备注" prop="remark" min-width="200" />
      <el-table-column align="center" fixed="right" label="操作" width="160">
        <template #default="{ row }">
          <el-button v-hasPermi="['product:store-tag:update']" link type="primary" @click="openForm(row)">编辑</el-button>
          <el-button v-hasPermi="['product:store-tag:delete']" link type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </ContentWrap>

  <el-dialog v-model="formVisible" :title="formData.id ? '编辑标签' : '新增标签'" width="560px">
    <el-form ref="formRef" :model="formData" :rules="rules" label-width="92px">
      <el-form-item label="标签编码" prop="code">
        <el-input v-model="formData.code" maxlength="64" placeholder="例如 BUSINESS_AREA" />
      </el-form-item>
      <el-form-item label="标签名称" prop="name">
        <el-input v-model="formData.name" maxlength="128" placeholder="例如 商圈店" />
      </el-form-item>
      <el-form-item label="标签组" prop="groupId">
        <el-select v-model="formData.groupId" class="!w-full" filterable placeholder="请选择标签组">
          <el-option v-for="item in groupOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio :value="1">启用</el-radio>
          <el-radio :value="0">停用</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="formData.sort" :min="0" class="!w-full" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" maxlength="255" show-word-limit type="textarea" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="formVisible = false">取消</el-button>
      <el-button type="primary" :loading="formLoading" @click="submitForm">保存</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import * as TagApi from '@/api/mall/store/tag'
import * as GroupApi from '@/api/mall/store/group'

defineOptions({ name: 'MallStoreTagIndex' })

const message = useMessage()

const loading = ref(false)
const formLoading = ref(false)
const formVisible = ref(false)
const list = ref<TagApi.HxyStoreTag[]>([])
const formRef = ref()
const groupOptions = ref<GroupApi.HxyStoreTagGroup[]>([])

const queryParams = ref<any>({
  code: undefined,
  name: undefined,
  groupId: undefined,
  status: undefined
})

const formData = ref<TagApi.HxyStoreTag>({
  id: undefined,
  code: '',
  name: '',
  groupId: undefined,
  groupName: '',
  status: 1,
  sort: 0,
  remark: ''
})

const rules = {
  code: [{ required: true, message: '标签编码不能为空', trigger: 'blur' }],
  name: [{ required: true, message: '标签名称不能为空', trigger: 'blur' }],
  groupId: [{ required: true, message: '标签组不能为空', trigger: 'change' }]
}

const loadGroupOptions = async () => {
  groupOptions.value = await GroupApi.getStoreTagGroupList({ status: 1 })
}

const getList = async () => {
  loading.value = true
  try {
    list.value = await TagApi.getStoreTagList(queryParams.value)
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  queryParams.value = { code: undefined, name: undefined, groupId: undefined, status: undefined }
  getList()
}

const openForm = (row?: TagApi.HxyStoreTag) => {
  if (row) {
    formData.value = { ...row }
  } else {
    formData.value = {
      id: undefined,
      code: '',
      name: '',
      groupId: undefined,
      groupName: '',
      status: 1,
      sort: 0,
      remark: ''
    }
  }
  formVisible.value = true
}

const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    const selectedGroup = groupOptions.value.find((item) => item.id === formData.value.groupId)
    formData.value.groupName = selectedGroup?.name || ''
    await TagApi.saveStoreTag(formData.value)
    message.success(formData.value.id ? '更新成功' : '创建成功')
    formVisible.value = false
    await getList()
  } finally {
    formLoading.value = false
  }
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await TagApi.deleteStoreTag(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => {
  loadGroupOptions()
  getList()
})
</script>
