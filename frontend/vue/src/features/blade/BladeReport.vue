<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, shallowRef } from 'vue'
import { requestError, type RequestApi } from '../../lib/http'
import { bladeApi, type InspectionImage, type Page, type Summary } from './api'

const props = defineProps<{ request: RequestApi; id: string }>()
const summary = shallowRef<Summary>()
const images = shallowRef<Page<InspectionImage>>()
const selected = shallowRef<InspectionImage>()
const previewUrl = ref('')
const previewLoading = ref(false)
const loading = ref(false)
const downloading = ref(false)
const error = ref('')
let previewRequest: AbortController | undefined
const api = bladeApi(props.request)

function clearPreview() {
  previewRequest?.abort()
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = ''
  selected.value = undefined
  previewLoading.value = false
}

async function select(image: InspectionImage) {
  clearPreview()
  selected.value = image
  error.value = ''
  const controller = new AbortController()
  previewRequest = controller
  previewLoading.value = true
  try {
    const blob = await api.preview(props.id, image.index, controller.signal)
    if (!controller.signal.aborted) previewUrl.value = URL.createObjectURL(blob)
  } catch (failure) {
    if (!controller.signal.aborted) error.value = requestError(failure)
  } finally {
    if (!controller.signal.aborted) previewLoading.value = false
  }
}

async function load(page: number) {
  loading.value = true
  error.value = ''
  clearPreview()
  try {
    const [report, list] = await Promise.all([summary.value ?? api.summary(props.id), api.images(props.id, page)])
    summary.value = report
    images.value = list
  } catch (failure) {
    error.value = requestError(failure)
  } finally {
    loading.value = false
  }
}

async function download() {
  downloading.value = true
  error.value = ''
  try {
    const url = URL.createObjectURL(await api.pdf(props.id))
    const link = document.createElement('a')
    link.href = url
    link.download = `blade-report-${props.id}.pdf`
    link.click()
    window.setTimeout(() => URL.revokeObjectURL(url), 1000)
  } catch (failure) {
    error.value = requestError(failure)
  } finally {
    downloading.value = false
  }
}

onMounted(() => void load(1))
onBeforeUnmount(clearPreview)
</script>

<template>
  <section aria-labelledby="blade-report-title" class="blade-report">
    <div class="blade-report-heading">
      <h2 id="blade-report-title">AI 检测结果</h2>
      <ElButton type="primary" :loading="downloading" @click="download">下载 PDF</ElButton>
    </div>
    <ElAlert v-if="error" :title="error" type="error" :closable="false" />
    <p v-if="loading" class="muted" role="status">正在读取报告…</p>
    <template v-if="summary">
      <dl class="totals">
        <div><dt>处理图片</dt><dd>{{ summary.imageCount }}</dd></div>
        <div><dt>检出图片</dt><dd>{{ summary.defectImageCount }}</dd></div>
        <div><dt>检出总数</dt><dd>{{ summary.detectionCount }}</dd></div>
      </dl>
      <p class="muted">模型 {{ summary.model }}</p>
      <dl class="classes" aria-label="缺陷分类统计"><div v-for="(count, label) in summary.classes" :key="label"><dt>{{ label }}</dt><dd>{{ count }}</dd></div></dl>
    </template>
    <template v-if="images">
      <h3>逐图明细</h3>
      <ul class="report-images" aria-label="逐图明细">
        <li v-for="image in images.items" :key="image.index">
          <button type="button" :aria-pressed="selected?.index === image.index" @click="select(image)">
            <span>{{ image.name }}</span><strong>{{ image.detections.length ? `${image.detections.length} 处检出` : '未检出' }}</strong>
          </button>
        </li>
      </ul>
      <div class="flex items-center gap-3">
        <ElButton :disabled="images.page === 1 || loading" @click="load(images.page - 1)">上一页</ElButton>
        <span>第 {{ images.page }} 页</span>
        <ElButton :disabled="!images.hasNext || loading" @click="load(images.page + 1)">下一页</ElButton>
      </div>
      <section v-if="selected" class="image-preview" aria-label="标注预览">
        <h3>{{ selected.name }}</h3>
        <p v-if="previewLoading" role="status" class="muted">正在读取预览…</p>
        <img v-if="previewUrl" :src="previewUrl" :alt="`${selected.name} 的检测标注`" />
        <ul v-if="selected.detections.length" class="flex flex-wrap gap-3" aria-label="检出明细">
          <li v-for="(item, index) in selected.detections" :key="index">{{ item.label }} · {{ (item.confidence * 100).toFixed(1) }}%</li>
        </ul>
      </section>
    </template>
  </section>
</template>

<style scoped>
.blade-report { display: grid; gap: 1rem; }
.blade-report-heading { display: flex; align-items: center; justify-content: space-between; gap: 1rem; flex-wrap: wrap; }
.totals, .classes { display: grid; grid-template-columns: repeat(auto-fit, minmax(6.5rem, 1fr)); gap: 0.65rem; }
.totals div, .classes div { background: #eef6f5; color: #163e43; padding: 0.65rem 0.85rem; border-radius: 0.45rem; }
.totals dd { font-size: 1.55rem; font-weight: 700; }
.classes div { display: flex; justify-content: space-between; gap: 0.4rem; }
.report-images { display: grid; gap: 0.3rem; }
.report-images button { display: flex; width: 100%; justify-content: space-between; gap: 1rem; text-align: left; padding: 0.6rem; border: 1px solid #dce7eb; border-radius: 0.35rem; }
.report-images button[aria-pressed='true'] { border-color: #137e73; background: #eef6f5; }
.report-images span, .image-preview h3 { overflow-wrap: anywhere; }
.report-images strong { flex-shrink: 0; }
.image-preview img { max-width: 100%; max-height: 46rem; object-fit: contain; margin: 0.5rem 0; }
</style>
