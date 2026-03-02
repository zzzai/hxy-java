<template>
  <ContentWrap v-if="loadFailed">
    <el-alert
      title="标签组接口不可用，请先执行门店治理结构升级脚本并重启后端服务。"
      type="error"
      :closable="false"
      show-icon
    />
  </ContentWrap>

  <template v-else>
    <ContentWrap>
      <el-form :inline="true" :model="queryParams" class="-mb-15px" label-width="82px">
        <el-form-item label="组编码" prop="code">
          <el-input v-model="queryParams.code" class="!w-180px" clearable placeholder="请输入标签组编码" @keyup.enter="getList" />
        </el-form-item>
        <el-form-item label="组名称" prop="name">
          <el-input v-model="queryParams.name" class="!w-220px" clearable placeholder="请输入标签组名称" @keyup.enter="getList" />
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
          <el-button v-hasPermi="['product:store-tag-group:create']" type="primary" plain @click="openForm()">
            <Icon class="mr-5px" icon="ep:plus" />
            新增标签组
          </el-button>
        </el-form-item>
      </el-form>
    </ContentWrap>

    <ContentWrap>
      <el-table v-loading="loading" :data="list">
        <el-table-column label="ID" prop="id" width="90" />
        <el-table-column label="组编码" prop="code" min-width="140" />
        <el-table-column label="组名称" prop="name" min-width="180" />
        <el-table-column label="必选" prop="required" width="90">
          <template #default="{ row }">
            <el-tag :type="row.required === 1 ? 'danger' : 'info'">{{ row.required === 1 ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="互斥" prop="mutex" width="90">
          <template #default="{ row }">
            <el-tag :type="row.mutex === 1 ? 'warning' : 'info'">{{ row.mutex === 1 ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="门店可编辑" prop="editableByStore" width="110">
          <template #default="{ row }">
            <el-tag>{{ row.editableByStore === 1 ? '可编辑' : '仅总部' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" prop="status" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="排序" prop="sort" width="90" />
        <el-table-column label="备注" prop="remark" min-width="220" />
        <el-table-column align="center" fixed="right" label="操作" width="160">
          <template #default="{ row }">
            <el-button v-hasPermi="['product:store-tag-group:update']" link type="primary" @click="openForm(row)">编辑</el-button>
            <el-button v-hasPermi="['product:store-tag-group:delete']" link type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </ContentWrap>

    <el-dialog v-model="formVisible" :title="formData.id ? '编辑标签组' : '新增标签组'" width="620px">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="110px">
        <el-form-item label="标签组编码" prop="code">
          <el-input v-model="formData.code" maxlength="64" placeholder="例如 STORE_ATTR" />
        </el-form-item>
        <el-form-item label="标签组名称" prop="name">
          <el-input v-model="formData.name" maxlength="128" placeholder="例如 门店属性" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="是否必选" prop="required">
              <el-switch v-model="formData.required" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="是否互斥" prop="mutex">
              <el-switch v-model="formData.mutex" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="门店可编辑" prop="editableByStore">
              <el-switch v-model="formData.editableByStore" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>
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
</template>

<script lang="ts" setup>
import * as GroupApi from '@/api/mall/store/group'

defineOptions({ name: 'MallStoreTagGroupIndex' })

const message = useMessage()

const loading = ref(false)
const formLoading = ref(false)
const formVisible = ref(false)
const loadFailed = ref(false)
const list = ref<GroupApi.HxyStoreTagGroup[]>([])
const formRef = ref()

const queryParams = ref<any>({
  code: undefined,
  name: undefined,
  status: undefined
})

const formData = ref<GroupApi.HxyStoreTagGroup>({
  id: undefined,
  code: '',
  name: '',
  required: 0,
  mutex: 0,
  editableByStore: 0,
  status: 1,
  sort: 0,
  remark: ''
})

const rules = {
  code: [{ required: true, message: '标签组编码不能为空', trigger: 'blur' }],
  name: [{ required: true, message: '标签组名称不能为空', trigger: 'blur' }]
}

const getList = async () => {
  loading.value = true
  try {
    list.value = await GroupApi.getStoreTagGroupList(queryParams.value)
    loadFailed.value = false
  } catch (error: any) {
    loadFailed.value = true
    message.error(error?.msg || '标签组接口不可用')
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  queryParams.value = { code: undefined, name: undefined, status: undefined }
  getList()
}

const openForm = (row?: GroupApi.HxyStoreTagGroup) => {
  if (row) {
    formData.value = { ...row }
  } else {
    formData.value = {
      id: undefined,
      code: '',
      name: '',
      required: 0,
      mutex: 0,
      editableByStore: 0,
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
    await GroupApi.saveStoreTagGroup(formData.value)
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
    await GroupApi.deleteStoreTagGroup(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => {
  getList()
})
</script>
