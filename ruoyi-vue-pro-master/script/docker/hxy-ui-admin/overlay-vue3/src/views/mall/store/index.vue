<template>
  <ContentWrap>
    <el-form :inline="true" :model="queryParams" class="-mb-15px" label-width="82px">
      <el-form-item label="门店编码" prop="code">
        <el-input v-model="queryParams.code" class="!w-180px" clearable placeholder="请输入门店编码" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="门店名称" prop="name">
        <el-input v-model="queryParams.name" class="!w-220px" clearable placeholder="请输入门店名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="门店分类" prop="categoryId">
        <el-select v-model="queryParams.categoryId" class="!w-180px" clearable placeholder="请选择分类">
          <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="门店状态" prop="status">
        <el-select v-model="queryParams.status" class="!w-140px" clearable placeholder="请选择状态">
          <el-option :value="1" label="启用" />
          <el-option :value="0" label="停用" />
        </el-select>
      </el-form-item>
      <el-form-item label="生命周期" prop="lifecycleStatus">
        <el-select v-model="queryParams.lifecycleStatus" class="!w-160px" clearable placeholder="请选择">
          <el-option :value="10" label="筹备中" />
          <el-option :value="20" label="试营业" />
          <el-option :value="30" label="营业中" />
          <el-option :value="40" label="闭店" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery">
          <Icon class="mr-5px" icon="ep:search" />
          搜索
        </el-button>
        <el-button @click="resetQuery">
          <Icon class="mr-5px" icon="ep:refresh" />
          重置
        </el-button>
        <el-button v-hasPermi="['product:store:create']" type="primary" plain @click="openForm()">
          <Icon class="mr-5px" icon="ep:plus" />
          新增门店
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
      <el-table v-loading="loading" :data="list">
      <el-table-column type="selection" width="55" />
      <el-table-column label="ID" prop="id" width="90" />
      <el-table-column label="门店编码" prop="code" min-width="120" />
      <el-table-column label="门店名称" prop="name" min-width="180" />
      <el-table-column label="门店简称" prop="shortName" min-width="120" />
      <el-table-column label="门店分类" prop="categoryName" min-width="120" />
      <el-table-column label="联系电话" prop="contactMobile" min-width="130" />
      <el-table-column label="状态" prop="status" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="生命周期" prop="lifecycleStatus" width="110">
        <template #default="{ row }">
          <el-tag :type="lifecycleColor(row.lifecycleStatus)">{{ lifecycleText(row.lifecycleStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="排序" prop="sort" width="90" />
      <el-table-column :formatter="dateFormatter" label="更新时间" prop="updateTime" width="180" />
      <el-table-column align="center" fixed="right" label="操作" width="220">
        <template #default="{ row }">
          <el-button v-hasPermi="['product:store:check-launch-readiness']" link type="success" @click="handleReadiness(row)">门禁</el-button>
          <el-button v-hasPermi="['product:store:update']" link type="primary" @click="openForm(row)">编辑</el-button>
          <el-button v-hasPermi="['product:store:delete']" link type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      v-model:limit="queryParams.pageSize"
      v-model:page="queryParams.pageNo"
      :total="total"
      @pagination="getList"
    />
  </ContentWrap>

  <el-dialog v-model="formVisible" :title="formData.id ? '编辑门店' : '新增门店'" width="760px">
    <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
      <el-row :gutter="14">
        <el-col :span="12">
          <el-form-item label="门店编码" prop="code">
            <el-input v-model="formData.code" maxlength="64" placeholder="例如 SH-001" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="门店名称" prop="name">
            <el-input v-model="formData.name" maxlength="128" placeholder="请输入门店名称" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="14">
        <el-col :span="12">
          <el-form-item label="门店简称" prop="shortName">
            <el-input v-model="formData.shortName" maxlength="128" placeholder="请输入门店简称" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="门店分类" prop="categoryId">
            <el-select v-model="formData.categoryId" class="!w-full" filterable placeholder="请选择分类">
              <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="14">
        <el-col :span="12">
          <el-form-item label="门店标签" prop="tagIds">
            <el-select v-model="formData.tagIds" class="!w-full" filterable multiple collapse-tags placeholder="请选择标签">
              <el-option v-for="item in tagOptions" :key="item.id" :label="formatTag(item)" :value="item.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="门店状态" prop="status">
            <el-radio-group v-model="formData.status">
              <el-radio :value="1">启用</el-radio>
              <el-radio :value="0">停用</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="14">
        <el-col :span="12">
          <el-form-item label="生命周期" prop="lifecycleStatus">
            <el-select v-model="formData.lifecycleStatus" class="!w-full" placeholder="请选择生命周期">
              <el-option :value="10" label="筹备中" />
              <el-option :value="20" label="试营业" />
              <el-option :value="30" label="营业中" />
              <el-option :value="40" label="闭店" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="14">
        <el-col :span="12">
          <el-form-item label="联系人" prop="contactName">
            <el-input v-model="formData.contactName" maxlength="64" placeholder="请输入联系人" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="联系电话" prop="contactMobile">
            <el-input v-model="formData.contactMobile" maxlength="32" placeholder="请输入联系电话" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="14">
        <el-col :span="8">
          <el-form-item label="省编码" prop="provinceCode">
            <el-input v-model="formData.provinceCode" maxlength="32" placeholder="省编码" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="市编码" prop="cityCode">
            <el-input v-model="formData.cityCode" maxlength="32" placeholder="市编码" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="区编码" prop="districtCode">
            <el-input v-model="formData.districtCode" maxlength="32" placeholder="区编码" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="详细地址" prop="address">
        <el-input v-model="formData.address" maxlength="255" placeholder="请输入详细地址" />
      </el-form-item>
      <el-row :gutter="14">
        <el-col :span="12">
          <el-form-item label="门店坐标" prop="longitude">
            <div class="w-full">
              <div class="flex items-center gap-8px">
                <el-input
                  :model-value="coordinateDisplay"
                  readonly
                  placeholder="点击“获取当前位置”自动填充坐标"
                />
                <el-button :loading="locating" type="primary" plain @click="locateCurrentPosition">获取当前位置</el-button>
                <el-button link type="danger" @click="clearCoordinates">清空</el-button>
              </div>
              <div class="mt-4px text-xs text-gray-500">需浏览器允许定位权限；建议在 HTTPS 或微信内打开。</div>
            </div>
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="营业开始" prop="openingTime">
            <el-input v-model="formData.openingTime" maxlength="16" placeholder="10:00" />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="营业结束" prop="closingTime">
            <el-input v-model="formData.closingTime" maxlength="16" placeholder="23:00" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="14">
        <el-col :span="12">
          <el-form-item label="排序" prop="sort">
            <el-input-number v-model="formData.sort" :min="0" class="!w-full" />
          </el-form-item>
        </el-col>
      </el-row>
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
import { dateFormatter } from '@/utils/formatTime'
import * as StoreApi from '@/api/mall/store/store'
import * as StoreCategoryApi from '@/api/mall/store/category'
import * as StoreTagApi from '@/api/mall/store/tag'
import { ElMessageBox } from 'element-plus'

defineOptions({ name: 'MallStoreIndex' })

const message = useMessage()

const loading = ref(false)
const formLoading = ref(false)
const formVisible = ref(false)
const total = ref(0)
const list = ref<StoreApi.HxyStore[]>([])
const formRef = ref()
const locating = ref(false)

const categoryOptions = ref<StoreCategoryApi.HxyStoreCategory[]>([])
const tagOptions = ref<StoreTagApi.HxyStoreTag[]>([])

const queryParams = ref<any>({
  pageNo: 1,
  pageSize: 10,
  code: undefined,
  name: undefined,
  categoryId: undefined,
  status: undefined,
  lifecycleStatus: undefined
})

const formData = ref<StoreApi.HxyStore>({
  id: undefined,
  code: '',
  name: '',
  shortName: '',
  categoryId: undefined as any,
  status: 1,
  lifecycleStatus: 10,
  tagIds: [],
  contactName: '',
  contactMobile: '',
  provinceCode: '',
  cityCode: '',
  districtCode: '',
  address: '',
  longitude: undefined,
  latitude: undefined,
  openingTime: '10:00',
  closingTime: '23:00',
  sort: 0,
  remark: ''
})

const rules = {
  code: [{ required: true, message: '门店编码不能为空', trigger: 'blur' }],
  name: [{ required: true, message: '门店名称不能为空', trigger: 'blur' }],
  categoryId: [{ required: true, message: '门店分类不能为空', trigger: 'change' }]
}

const coordinateDisplay = computed(() => {
  if (formData.value.longitude === undefined || formData.value.latitude === undefined) {
    return ''
  }
  return `${Number(formData.value.longitude).toFixed(6)}, ${Number(formData.value.latitude).toFixed(6)}`
})

const loadOptions = async () => {
  categoryOptions.value = await StoreCategoryApi.getStoreCategoryList({ status: 1 })
  tagOptions.value = await StoreTagApi.getStoreTagList({ status: 1 })
}

const getList = async () => {
  loading.value = true
  try {
    const data = await StoreApi.getStorePage(queryParams.value)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.value.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryParams.value = {
    pageNo: 1,
    pageSize: 10,
    code: undefined,
    name: undefined,
    categoryId: undefined,
    status: undefined,
    lifecycleStatus: undefined
  }
  getList()
}

const formatTag = (item: StoreTagApi.HxyStoreTag) => {
  return item.groupName ? `${item.name}（${item.groupName}）` : item.name
}

const openForm = async (row?: StoreApi.HxyStore) => {
  if (row?.id) {
    const detail = await StoreApi.getStore(row.id)
    formData.value = {
      ...detail,
      tagIds: detail.tagIds || []
    }
  } else {
    formData.value = {
      id: undefined,
      code: '',
      name: '',
      shortName: '',
      categoryId: undefined as any,
      status: 1,
      lifecycleStatus: 10,
      tagIds: [],
      contactName: '',
      contactMobile: '',
      provinceCode: '',
      cityCode: '',
      districtCode: '',
      address: '',
      longitude: undefined,
      latitude: undefined,
      openingTime: '10:00',
      closingTime: '23:00',
      sort: 0,
      remark: ''
    }
  }
  formVisible.value = true
}

const lifecycleText = (status?: number) => {
  if (status === 10) return '筹备中'
  if (status === 20) return '试营业'
  if (status === 30) return '营业中'
  if (status === 40) return '闭店'
  return '未设置'
}

const lifecycleColor = (status?: number) => {
  if (status === 30) return 'success'
  if (status === 20) return 'warning'
  if (status === 40) return 'info'
  return ''
}

const locateCurrentPosition = () => {
  if (typeof window === 'undefined' || !navigator.geolocation) {
    message.warning('当前浏览器不支持定位，请在 HTTPS 或微信内置浏览器中操作')
    return
  }
  locating.value = true
  navigator.geolocation.getCurrentPosition(
    (position) => {
      formData.value.longitude = Number(position.coords.longitude.toFixed(6))
      formData.value.latitude = Number(position.coords.latitude.toFixed(6))
      locating.value = false
      message.success('已自动填充门店坐标')
    },
    (error) => {
      locating.value = false
      if (error.code === error.PERMISSION_DENIED) {
        message.warning('定位权限被拒绝，请在浏览器中允许定位权限后重试')
        return
      }
      if (error.code === error.TIMEOUT) {
        message.warning('定位超时，请重试')
        return
      }
      message.warning('定位失败，请确认网络和浏览器定位设置')
    },
    { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
  )
}

const clearCoordinates = () => {
  formData.value.longitude = undefined
  formData.value.latitude = undefined
}

const handleReadiness = async (row: StoreApi.HxyStore) => {
  const data = await StoreApi.checkStoreLaunchReadiness(row.id as number)
  if (data.ready) {
    message.success(`门店 ${row.name} 已满足上线门禁`)
    return
  }
  const lines = data.reasons?.length ? data.reasons.map((item) => `- ${item}`).join('\n') : '- 未知原因'
  await ElMessageBox.alert(lines, `门店 ${row.name} 门禁未通过`, {
    confirmButtonText: '知道了',
    type: 'warning',
    dangerouslyUseHTMLString: false
  })
}

const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    await StoreApi.saveStore(formData.value)
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
    await StoreApi.deleteStore(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(async () => {
  await loadOptions()
  await getList()
})
</script>
