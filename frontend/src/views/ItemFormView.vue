<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ElMessage from 'element-plus/es/components/message/index.mjs'
import {
  ArrowLeft,
  Check,
  ImagePlus,
  Link as LinkIcon,
  Plus,
  Trash2,
  UploadCloud,
} from 'lucide-vue-next'

import {
  createItemApi,
  getCategoriesApi,
  getItemDetailApi,
  uploadItemImageApi,
  updateItemApi,
} from '../api'
import { authState } from '../stores/auth'

const route = useRoute()
const router = useRouter()

const formRef = ref()
const categories = ref([])
const loading = ref(false)
const submitting = ref(false)
const uploading = ref(false)
const loadError = ref('')
const imageUrls = ref([])
const urlDraft = ref('')
const fileInput = ref()
const failedImages = ref(new Set())

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
    imageUrls.value = Array.isArray(item.images) ? item.images.filter(Boolean) : []
  } catch (error) {
    if (error.status !== 401) {
      ElMessage.error(error.message || '商品加载失败')
      router.replace({ name: 'my-items' })
    }
  } finally {
    loading.value = false
  }
}

function chooseImages() {
  if (imageUrls.value.length >= 5) {
    ElMessage.warning('最多上传 5 张图片')
    return
  }
  fileInput.value?.click()
}

async function handleFiles(event) {
  const files = Array.from(event.target.files || [])
  event.target.value = ''
  if (!files.length) {
    return
  }

  const remaining = 5 - imageUrls.value.length
  if (remaining <= 0) {
    ElMessage.warning('最多上传 5 张图片')
    return
  }

  const selected = files.slice(0, remaining)
  const invalid = selected.find(
    (file) => !['image/jpeg', 'image/png', 'image/webp', 'image/gif'].includes(file.type),
  )
  if (invalid) {
    ElMessage.error('仅支持 JPG、PNG、WebP 或 GIF 图片')
    return
  }
  const oversized = selected.find((file) => file.size > 5 * 1024 * 1024)
  if (oversized) {
    ElMessage.error('单张图片不能超过 5MB')
    return
  }

  uploading.value = true
  try {
    const results = await Promise.allSettled(
      selected.map((file) => uploadItemImageApi(file)),
    )
    const uploaded = results
      .filter((result) => result.status === 'fulfilled')
      .map((result) => result.value.url)
    imageUrls.value.push(...uploaded)

    if (uploaded.length) {
      ElMessage.success(`已上传 ${uploaded.length} 张图片`)
    }
    const failed = results.find((result) => result.status === 'rejected')
    if (failed) {
      const error = failed.reason
      if (error?.status !== 401) {
        ElMessage.error(error?.message || '部分图片上传失败')
      }
    }
  } finally {
    uploading.value = false
  }
}

function addImageUrl() {
  const url = urlDraft.value.trim()
  if (!url) {
    return
  }
  if (!/^https?:\/\//.test(url) && !url.startsWith('/api/files/images/')) {
    ElMessage.warning('请输入 http(s) 图片链接')
    return
  }
  if (imageUrls.value.length >= 5) {
    ElMessage.warning('最多保存 5 张图片')
    return
  }
  if (!imageUrls.value.includes(url)) {
    imageUrls.value.push(url)
  }
  urlDraft.value = ''
}

function removeImage(index) {
  const [removed] = imageUrls.value.splice(index, 1)
  failedImages.value.delete(removed)
}

function imageFailed(url) {
  failedImages.value.add(url)
}

function isImageFailed(url) {
  return failedImages.value.has(url)
}

function resetForm() {
  form.title = ''
  form.categoryId = null
  form.price = null
  form.description = ''
  imageUrls.value = []
  urlDraft.value = ''
  failedImages.value.clear()
}

async function submitForm() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  const images = imageUrls.value.map((url) => (url || '').trim()).filter(Boolean)

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

          <el-form-item label="商品图片" class="span-full">
            <div class="image-field">
              <div class="image-upload-grid">
                <div
                  v-for="(url, index) in imageUrls"
                  :key="url + index"
                  class="image-preview"
                >
                  <img
                    v-if="!isImageFailed(url)"
                    :src="url"
                    alt="商品图片预览"
                    @error="imageFailed(url)"
                  />
                  <div v-else class="image-broken">
                    <ImagePlus :size="20" />
                    无法预览
                  </div>
                  <button
                    type="button"
                    class="remove-image"
                    aria-label="移除该图片"
                    @click="removeImage(index)"
                  >
                    <Trash2 :size="14" />
                  </button>
                </div>

                <button
                  v-if="imageUrls.length < 5"
                  type="button"
                  class="upload-tile"
                  :disabled="uploading"
                  @click="chooseImages"
                >
                  <UploadCloud :size="22" />
                  <span>{{ uploading ? '上传中' : '上传图片' }}</span>
                  <small>{{ imageUrls.length }}/5</small>
                </button>
              </div>

              <input
                ref="fileInput"
                class="file-input"
                type="file"
                accept="image/jpeg,image/png,image/webp,image/gif"
                multiple
                @change="handleFiles"
              />

              <div class="url-add-row">
                <el-input
                  v-model="urlDraft"
                  clearable
                  placeholder="也可以粘贴网络图片链接"
                  @keyup.enter="addImageUrl"
                >
                  <template #prefix><LinkIcon :size="15" /></template>
                </el-input>
                <el-button :icon="Plus" plain @click="addImageUrl">添加链接</el-button>
              </div>
              <p class="image-hint">支持 JPG、PNG、WebP、GIF，单张不超过 5MB</p>
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
  gap: 12px;
  width: 100%;
}

.image-upload-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(112px, 1fr));
  gap: 10px;
  width: 100%;
}

.image-preview,
.upload-tile {
  position: relative;
  width: 100%;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  background: #f4f6f5;
  border: 1px solid var(--campus-line);
  border-radius: var(--campus-radius-sm);
}

.image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-broken {
  display: flex;
  height: 100%;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 5px;
  color: #8a948f;
  font-size: 11px;
}

.remove-image {
  position: absolute;
  top: 5px;
  right: 5px;
  display: grid;
  place-items: center;
  width: 25px;
  height: 25px;
  padding: 0;
  color: #fff;
  background: rgba(25, 34, 30, 0.68);
  border: 0;
  border-radius: 5px;
  cursor: pointer;
}

.upload-tile {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: var(--campus-green-dark);
  background: var(--campus-green-soft);
  border-style: dashed;
  cursor: pointer;
}

.upload-tile:disabled {
  cursor: wait;
  opacity: 0.7;
}

.upload-tile span {
  font-size: 12px;
  font-weight: 650;
}

.upload-tile small {
  color: #708078;
  font-size: 10px;
}

.file-input {
  display: none;
}

.url-add-row {
  display: flex;
  gap: 8px;
}

.url-add-row :deep(.el-input) {
  flex: 1;
}

.image-hint {
  margin: -4px 0 0;
  color: #8a948f;
  font-size: 11px;
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

  .url-add-row {
    flex-direction: column;
  }
}
</style>
