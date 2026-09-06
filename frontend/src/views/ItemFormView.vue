<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Check, Link as LinkIcon, Plus, Trash2 } from 'lucide-vue-next'

import {
  createItemApi,
  getCategoriesApi,
  getItemDetailApi,
  updateItemApi,
} from '../api'
import { authState } from '../stores/auth'

const route = useRoute()
const router = useRouter()

const formRef = ref()
const categories = ref([])
const loading = ref(false)
const submitting = ref(false)
const loadError = ref('')
const imageUrls = ref([''])

const isEdit = computed(() => Boolean(route.params.id))
const itemId = computed(() => Number(route.params.id))
const form = reactive({
  title: '',
  categoryId: null,
  price: null,
  description: '',
})

const rules = {
  title: [
    { required: true, message: '请输入商品标题', trigger: 'blur' },
    { max: 100, message: '标题不能超过 100 个字符', trigger: 'blur' },
  ],
  categoryId: [{ required: true, message: '请选择商品分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入商品价格', trigger: 'blur' }],
}

async function loadCategories() {
  try {
    categories.value = await getCategoriesApi()
  } catch (error) {
    if (error.status !== 401) {
      loadError.value = error.message
    }
  }
}

async function fillEditItem() {
  if (!isEdit.value) {
    return
  }
  loading.value = true
  try {
    const item = await getItemDetailApi(itemId.value)
    if (item.sellerId !== authState.user?.id) {
      ElMessage.error('只能编辑自己发布的商品')
      router.replace({ name: 'my-items' })
      return
    }
    form.title = item.title || ''
    form.categoryId = item.categoryId
    form.price = Number(item.price)
    form.description = item.description || ''
    imageUrls.value = Array.isArray(item.images) && item.images.length ? [...item.images] : ['']
  } catch (error) {
    if (error.status !== 401) {
      ElMessage.error(error.message || '商品加载失败')
      router.replace({ name: 'my-items' })
    }
  } finally {
    loading.value = false
  }
}

function addImageInput() {
  if (imageUrls.value.length < 5) {
    imageUrls.value.push('')
  }
}

function removeImageInput(index) {
  if (imageUrls.value.length > 1) {
    imageUrls.value.splice(index, 1)
  } else {
    imageUrls.value[0] = ''
  }
}

function isPreviewable(url) {
  return /^https?:\/\//.test((url || '').trim())
}

function resetForm() {
  form.title = ''
  form.categoryId = null
  form.price = null
  form.description = ''
  imageUrls.value = ['']
}

async function submitForm() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  const images = imageUrls.value
    .map((url) => (url || '').trim())
    .filter(Boolean)

  const payload = {
    title: form.title.trim(),
    categoryId: form.categoryId,
    price: Number(form.price),
    description: (form.description || '').trim() || null,
    images: images.length ? images : null,
  }

  submitting.value = true
  try {
    if (isEdit.value) {
      await updateItemApi(itemId.value, payload)
      ElMessage.success('商品信息已更新')
    } else {
      await createItemApi(payload)
      ElMessage.success('商品发布成功')
    }
    router.push({ name: 'my-items' })
  } catch (error) {
    if (error.status !== 401) {
      ElMessage.error(error.message || '提交失败，请稍后重试')
    }
  } finally {
    submitting.value = false
  }
}

watch(
  () => route.params.id,
  () => {
    if (route.name === 'item-edit') {
      fillEditItem()
    }
  },
)

watch(
  () => route.name,
  (name) => {
    if (name === 'item-publish') {
      resetForm()
    }
  },
)

onMounted(async () => {
  await loadCategories()
  if (isEdit.value) {
    await fillEditItem()
  } else {
    resetForm()
  }
})
</script>

<template>
  <div class="page-shell form-page">
    <button type="button" class="back-link" @click="router.push({ name: 'my-items' })">
      <ArrowLeft :size="16" />
      返回我的商品
    </button>

    <div class="page-head">
      <div>
        <h1 class="page-title">{{ isEdit ? '编辑商品' : '发布闲置商品' }}</h1>
        <p class="page-subtitle">
          请如实填写品牌、型号、成色与使用情况，方便同学准确判断
        </p>
      </div>
    </div>

    <section v-loading="loading" class="toolbar form-card">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="item-form"
      >
        <div class="form-grid">
          <el-form-item label="商品标题" prop="title" class="span-full">
            <el-input
              v-model="form.title"
              maxlength="100"
              show-word-limit
              placeholder="例如：九成新机械键盘，带包装"
            />
          </el-form-item>

          <el-form-item label="商品分类" prop="categoryId">
            <el-select v-model="form.categoryId" placeholder="选择分类" class="full-width">
              <el-option
                v-for="category in categories"
                :key="category.id"
                :label="category.name"
                :value="category.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="价格（元）" prop="price">
            <el-input-number
              v-model="form.price"
              class="full-width price-input"
              :min="0.01"
              :max="9999999"
              :precision="2"
              :step="10"
              controls-position="right"
              placeholder="0.00"
            />
          </el-form-item>

          <el-form-item label="商品描述" prop="description" class="span-full">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="6"
              maxlength="2000"
              show-word-limit
              placeholder="描述成色、购买时间、转手原因、当面交易地点等信息"
            />
          </el-form-item>

          <el-form-item label="图片链接" class="span-full">
            <div class="image-field">
              <div v-for="(url, index) in imageUrls" :key="index" class="image-input-row">
                <el-input
                  v-model="imageUrls[index]"
                  :placeholder="`图片链接 ${index + 1}（http:// 或 https://）`"
                  clearable
                >
                  <template #prefix><LinkIcon :size="15" /></template>
                </el-input>
                <el-button
                  class="remove-image"
                  :icon="Trash2"
                  aria-label="移除该图片"
                  @click="removeImageInput(index)"
                />
              </div>

              <button
                v-if="imageUrls.length < 5"
                type="button"
                class="add-image"
                @click="addImageInput"
              >
                <Plus :size="15" />
                再添加一张
              </button>
            </div>

            <div v-if="imageUrls.some(isPreviewable)" class="image-preview-row">
              <div
                v-for="(url, index) in imageUrls.filter(isPreviewable)"
                :key="url + index"
                class="image-preview"
              >
                <img :src="url.trim()" alt="图片预览" />
              </div>
            </div>
          </el-form-item>
        </div>

        <div class="submit-row">
          <el-button @click="router.back()">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitForm">
            <Check :size="16" />
            {{ isEdit ? '保存修改' : '确认发布' }}
          </el-button>
        </div>
      </el-form>
    </section>
  </div>
</template>

<style scoped>
.form-page {
  padding-top: 18px;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 14px;
  padding: 4px 0;
  color: var(--campus-text);
  background: none;
  border: 0;
  font-size: 13px;
  cursor: pointer;
}

.form-card {
  padding: 22px;
}

.item-form {
  max-width: 880px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px 22px;
}

.span-full {
  grid-column: 1 / -1;
}

.full-width {
  width: 100%;
}

.price-input {
  max-width: 240px;
}

.image-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.image-input-row {
  display: flex;
  gap: 8px;
  width: 100%;
}

.image-input-row :deep(.el-input) {
  flex: 1;
}

.remove-image {
  width: 40px;
  padding: 8px;
}

.add-image {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  align-self: flex-start;
  height: 32px;
  padding: 0 10px;
  color: var(--campus-green-dark);
  background: var(--el-color-primary-light-9);
  border: 1px dashed #9ac9b8;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.image-preview-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  width: 100%;
  margin-top: 10px;
}

.image-preview {
  width: 72px;
  height: 54px;
  overflow: hidden;
  background: #eef1ef;
  border: 1px solid var(--campus-line);
  border-radius: 6px;
}

.image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.submit-row {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 6px;
  padding-top: 18px;
  border-top: 1px solid var(--campus-line);
}

@media (max-width: 720px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .form-card {
    padding: 16px;
  }
}
</style>
