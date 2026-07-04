<template>
  <div class="item-publish">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <h3>{{ isEditMode ? '编辑物品信息' : '发布物品信息' }}</h3>
          <el-button link @click="$router.back()">
            <el-icon><ArrowLeft /></el-icon> 返回
          </el-button>
        </div>
      </template>

      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" label-position="right">
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :value="0">寻物（我丢失了物品）</el-radio>
            <el-radio :value="1">拾物（我捡到了物品）</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入物品标题，如：丢失黑色书包一个" maxlength="100" />
        </el-form-item>

        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择物品分类" style="width: 300px">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="物品图片" prop="images">
          <div class="image-upload-wrapper">
            <!-- 已上传图片展示 -->
            <div class="image-preview-list">
              <div v-for="(url, index) in imageList" :key="url" class="image-preview-item">
                <img :src="url" alt="物品图片" class="preview-img" @click="previewImage(url)" />
                <el-icon class="remove-btn" @click="removeImage(index)"><Close /></el-icon>
              </div>

              <!-- 上传按钮（最多 6 张） -->
              <div v-if="imageList.length < 6" class="upload-placeholder" @click="$refs.fileInput.click()">
                <el-icon :size="28"><Plus /></el-icon>
                <div class="upload-tip">点击上传图片</div>
                <div class="upload-sub-tip">最多 6 张，每张 ≤ 10MB</div>
              </div>
            </div>

            <!-- 隐藏的 file input -->
            <input
              ref="fileInput"
              type="file"
              accept="image/jpeg,image/jpg,image/png,image/gif,image/webp"
              multiple
              style="display: none"
              @change="handleFileChange"
            />

            <!-- 上传进度 -->
            <div v-if="uploading" class="upload-progress">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>正在上传图片...</span>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="详细描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="5"
            placeholder="请详细描述物品的特征、丢失/拾到的经过等信息"
            maxlength="1000"
          />
        </el-form-item>

        <el-form-item label="地点" prop="location">
          <el-input v-model="form.location" placeholder="请输入物品丢失或拾到的地点" maxlength="200" />
        </el-form-item>

        <el-form-item label="时间" prop="lostTime">
          <el-date-picker
            v-model="form.lostTime"
            type="datetime"
            placeholder="选择日期时间"
            style="width: 300px"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>

        <el-divider content-position="left">联系方式（至少填写一种）</el-divider>

        <el-form-item label="联系电话">
          <el-input v-model="form.contactPhone" placeholder="选填，如：13800138000" maxlength="20" />
        </el-form-item>

        <el-form-item label="微信号">
          <el-input v-model="form.contactWechat" placeholder="选填，如：wx_2025" maxlength="50" />
        </el-form-item>

        <el-form-item label="QQ号">
          <el-input v-model="form.contactQq" placeholder="选填，如：123456789" maxlength="20" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" size="large" @click="submitForm" :loading="loading">
            <el-icon><Check /></el-icon> {{ isEditMode ? '保存修改' : '提交发布' }}
          </el-button>
          <el-button size="large" @click="resetForm">
            <el-icon><RefreshLeft /></el-icon> 重置
          </el-button>
        </el-form-item>
      </el-form>

      <el-alert
        v-if="submitted"
        :title="submitMessage"
        type="success"
        show-icon
        :closable="false"
        style="margin-top: 20px"
      >
        <template #default>
          {{ isEditMode ? '物品信息已更新，等待管理员重新审核。' : '您的物品信息已提交，等待管理员审核通过后将显示在列表中。' }}
        </template>
      </el-alert>
    </el-card>

    <!-- 图片大图预览 -->
    <el-dialog v-model="previewVisible" :title="'图片预览'" width="600px" align-center>
      <img :src="previewUrl" alt="preview" style="width: 100%; border-radius: 4px;" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Check, RefreshLeft, Plus, Close, Loading } from '@element-plus/icons-vue'
import { createItem, updateItem, uploadImage, getItemDetail } from '@/api/item'
import { getAllCategories } from '@/api/category'

const router = useRouter()
const route = useRoute()
const formRef = ref(null)
const fileInput = ref(null)
const loading = ref(false)
const uploading = ref(false)
const categories = ref([])
const submitted = ref(false)
const submitMessage = ref('')

// 编辑模式支持
const editId = ref(null)
const isEditMode = computed(() => editId.value !== null)

// 已上传图片 URL 列表
const imageList = ref([])

const form = reactive({
  type: 0,
  title: '',
  categoryId: null,
  description: '',
  location: '',
  lostTime: null,
  contactPhone: '',
  contactWechat: '',
  contactQq: ''
})

const rules = {
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  description: [{ required: true, message: '请输入详细描述', trigger: 'blur' }]
}

// 图片预览
const previewVisible = ref(false)
const previewUrl = ref('')

function previewImage(url) {
  previewUrl.value = url
  previewVisible.value = true
}

function removeImage(index) {
  imageList.value.splice(index, 1)
}

async function handleFileChange(event) {
  const files = Array.from(event.target.files || [])
  if (files.length === 0) return

  // 清除 input 值，允许重复上传同名文件
  event.target.value = ''

  // 限制总数量 ≤ 6
  const remainingSlots = 6 - imageList.value.length
  if (remainingSlots <= 0) {
    ElMessage.warning('最多只能上传 6 张图片')
    return
  }
  const filesToUpload = files.slice(0, remainingSlots)

  uploading.value = true
  try {
    for (const file of filesToUpload) {
      // 单文件大小校验
      if (file.size > 10 * 1024 * 1024) {
        ElMessage.warning(`${file.name} 超过 10MB，已跳过`)
        continue
      }
      // 类型校验
      const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp']
      if (!allowedTypes.includes(file.type)) {
        ElMessage.warning(`${file.name} 格式不支持，已跳过`)
        continue
      }

      const result = await uploadImage(file)
      if (result && result.url) {
        imageList.value.push(result.url)
      }
    }
    ElMessage.success(`成功上传 ${filesToUpload.length} 张图片`)
  } catch (err) {
    console.error('上传图片失败:', err)
  } finally {
    uploading.value = false
  }
}

function loadCategories() {
  getAllCategories().then((data) => {
    categories.value = data || []
  })
}

function submitForm() {
  formRef.value.validate((valid) => {
    if (!valid) return

    // 至少有一种联系方式
    if (!form.contactPhone && !form.contactWechat && !form.contactQq) {
      ElMessage.warning('请至少填写一种联系方式（电话、微信或QQ）')
      return
    }

    loading.value = true
    const submitData = {
      type: form.type,
      title: form.title,
      categoryId: form.categoryId,
      description: form.description,
      location: form.location || null,
      lostTime: form.lostTime || null,
      contactPhone: form.contactPhone || null,
      contactWechat: form.contactWechat || null,
      contactQq: form.contactQq || null,
      // 图片 URL 列表转为 JSON 字符串存储
      images: imageList.value.length > 0 ? JSON.stringify(imageList.value) : null
    }

    const apiCall = isEditMode.value
      ? updateItem(editId.value, submitData)
      : createItem(submitData)

    apiCall.then(() => {
      submitted.value = true
      submitMessage.value = isEditMode.value ? '修改成功！' : '发布成功！'
      ElMessage.success(isEditMode.value ? '修改成功，等待重新审核' : '发布成功，等待审核')
      setTimeout(() => {
        router.push('/profile')
      }, 1500)
    }).finally(() => {
      loading.value = false
    })
  })
}

function resetForm() {
  formRef.value.resetFields()
  form.type = 0
  form.title = ''
  form.categoryId = null
  form.description = ''
  form.location = ''
  form.lostTime = null
  form.contactPhone = ''
  form.contactWechat = ''
  form.contactQq = ''
  imageList.value = []
}

function loadItemDetail(id) {
  loading.value = true
  getItemDetail(id).then((data) => {
    editId.value = id
    form.type = data.type ?? 0
    form.title = data.title ?? ''
    form.categoryId = data.categoryId ?? null
    form.description = data.description ?? ''
    form.location = data.location ?? ''
    form.lostTime = data.lostTime ?? null
    form.contactPhone = data.contactPhone ?? ''
    form.contactWechat = data.contactWechat ?? ''
    form.contactQq = data.contactQq ?? ''

    // 图片解析（后端返回 images 可能是 JSON 字符串）
    if (data.images) {
      try {
        const parsed = typeof data.images === 'string' ? JSON.parse(data.images) : data.images
        if (Array.isArray(parsed)) {
          imageList.value = parsed
        } else {
          imageList.value = []
        }
      } catch (e) {
        imageList.value = []
      }
    } else {
      imageList.value = []
    }
  }).finally(() => {
    loading.value = false
  })
}

onMounted(() => {
  loadCategories()

  const qEditId = route.query.editId
  if (qEditId) {
    const id = Number(qEditId)
    if (!isNaN(id) && id > 0) {
      loadItemDetail(id)
    }
  }
})
</script>

<style scoped>
/* ========== 页面整体 ========== */
.item-publish {
  padding: 10px 20px;
  max-width: 1000px;
  margin: 0 auto;
  box-sizing: border-box;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
  color: #303133;
}

/* ========== 表单统一宽度 ========== */
.item-publish :deep(.el-form-item__content) {
  width: 100%;
  max-width: 600px;
}

.item-publish :deep(.el-input),
.item-publish :deep(.el-textarea),
.item-publish :deep(.el-select) {
  width: 100%;
}

/* ========== 图片上传区 ========== */
.image-upload-wrapper {
  width: 100%;
  box-sizing: border-box;
}

.image-preview-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: flex-start;
}

.image-preview-item {
  position: relative;
  width: 120px;
  height: 120px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #dcdfe6;
  background: #f5f7fa;
  flex-shrink: 0;
  box-sizing: border-box;
}

.preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  cursor: zoom-in;
  display: block;
}

.remove-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  font-size: 16px;
  padding: 4px;
  background: rgba(0, 0, 0, 0.5);
  color: white;
  border-radius: 50%;
  cursor: pointer;
  line-height: 1;
  z-index: 2;
}

.upload-placeholder {
  width: 120px;
  height: 120px;
  border: 2px dashed #dcdfe6;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #8c939d;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
  box-sizing: border-box;
}

.upload-placeholder:hover {
  border-color: #409EFF;
  color: #409EFF;
  background: #ecf5ff;
}

.upload-tip {
  font-size: 12px;
  margin-top: 4px;
  text-align: center;
}

.upload-sub-tip {
  font-size: 10px;
  color: #c0c4cc;
  margin-top: 2px;
  text-align: center;
  white-space: nowrap;
}

.upload-progress {
  margin-top: 10px;
  color: #8c939d;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

/* ========== 分割线 ========== */
.item-publish :deep(.el-divider__text) {
  font-weight: 500;
  color: #606266;
}
</style>
