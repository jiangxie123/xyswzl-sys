<template>
  <div class="item-publish">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <h3>发布物品信息</h3>
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
            <el-icon><Check /></el-icon> 提交发布
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
          您的物品信息已提交，等待管理员审核通过后将显示在列表中。
        </template>
      </el-alert>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Check, RefreshLeft } from '@element-plus/icons-vue'
import { createItem } from '@/api/item'
import { getAllCategories } from '@/api/category'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const categories = ref([])
const submitted = ref(false)
const submitMessage = ref('')

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
      contactQq: form.contactQq || null
    }

    createItem(submitData).then(() => {
      submitted.value = true
      submitMessage.value = '发布成功！'
      ElMessage.success('发布成功，等待审核')
      setTimeout(() => {
        router.push('/items')
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
}

onMounted(() => {
  loadCategories()
})
</script>

<style scoped>
.item-publish {
  padding: 20px;
  max-width: 900px;
  margin: 0 auto;
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
</style>
