<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, shallowRef } from 'vue'
import { requestError, type RequestApi } from '../../lib/http'
import { bladeApi, type InspectionImage, type Page, type Summary } from './api'

const props = defineProps<{ request: RequestApi; id: string }>()
const summary = shallowRef<Summary>()
const largestClass = computed(() => Math.max(1, ...Object.values(summary.value?.classes ?? {})))
const images = shallowRef<Page<InspectionImage>>()
const selected = shallowRef<InspectionImage>()
const previewUrl = ref('')
const previewViewport = ref<HTMLElement>()
const zoomed = ref(false)
const previewLoading = ref(false)
const loading = ref(false)
const downloading = ref(false)
const error = ref('')
let previewRequest: AbortController | undefined
let disposed = false
const api = bladeApi(props.request)

function clearPreview() {
  previewRequest?.abort()
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = ''
  selected.value = undefined
  zoomed.value = false
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

function pickImage(event: Event) {
  const index = Number((event.target as HTMLSelectElement).value)
  const image = images.value?.items.find((item) => item.index === index)
  if (image) void select(image)
}

async function toggleZoom() {
  zoomed.value = !zoomed.value
  await nextTick()
  const viewport = previewViewport.value
  if (viewport && zoomed.value) {
    viewport.scrollLeft = (viewport.scrollWidth - viewport.clientWidth) / 2
    viewport.scrollTop = (viewport.scrollHeight - viewport.clientHeight) / 2
  }
}

async function load(page: number) {
  loading.value = true
  error.value = ''
  clearPreview()
  try {
    const [report, list] = await Promise.all([summary.value ?? api.summary(props.id), api.images(props.id, page)])
    if (disposed) return
    summary.value = report
    images.value = list
    const first = list.items.find((image) => image.detections.length) ?? list.items[0]
    if (first) void select(first)
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
onBeforeUnmount(() => {
  disposed = true
  clearPreview()
})
</script>

<template>
  <section aria-labelledby="blade-report-title" class="blade-report blade-sheet">
    <header class="blade-toolbar">
      <div>
        <h2 id="blade-report-title">AI 检测结果</h2>
        <p v-if="summary" class="report-model muted">{{ summary.model }}</p>
      </div>
      <ElButton type="primary" :loading="downloading" @click="download">下载 PDF</ElButton>
    </header>
    <ElAlert v-if="error" :title="error" type="error" :closable="false" />
    <p v-if="loading" class="report-loading muted" role="status">正在读取报告…</p>
    <dl v-if="summary" class="totals">
      <div><dt>处理图片</dt><dd>{{ summary.imageCount }}</dd><span>张</span></div>
      <div><dt>检出图片</dt><dd>{{ summary.defectImageCount }}</dd><span>张</span></div>
      <div><dt>检出总数</dt><dd>{{ summary.detectionCount }}</dd><span>处</span></div>
    </dl>
    <div v-if="images" class="gallery-layout">
      <section class="image-sidebar" aria-labelledby="report-images-title">
        <h3 id="report-images-title" class="pane-heading">逐图明细</h3>
        <div v-if="images.items.length" class="mobile-photo-picker">
          <label for="inspection-image-picker">巡检图片</label>
          <select id="inspection-image-picker" :value="selected?.index" @change="pickImage">
            <option v-for="image in images.items" :key="image.index" :value="image.index">
              {{ image.name.split(/[\\/]/).pop() }} · {{ image.detections.length ? `${image.detections.length} 处检出` : '未检出' }}
            </option>
          </select>
        </div>
        <ul class="report-images" aria-label="逐图明细">
          <li v-for="image in images.items" :key="image.index">
            <button type="button" :aria-pressed="selected?.index === image.index" :title="image.name" @click="select(image)">
              <span>{{ image.name.split(/[\\/]/).pop() }}</span>
              <strong :class="{ detected: image.detections.length }">{{ image.detections.length ? `${image.detections.length} 处检出` : '未检出' }}</strong>
            </button>
          </li>
        </ul>
        <div v-if="images.page > 1 || images.hasNext" class="blade-pagination">
          <ElButton :disabled="images.page === 1 || loading" @click="load(images.page - 1)">上一页</ElButton>
          <span>第 {{ images.page }} 页</span>
          <ElButton :disabled="!images.hasNext || loading" @click="load(images.page + 1)">下一页</ElButton>
        </div>
      </section>
      <section class="image-preview" aria-label="标注预览">
        <div class="image-preview-title">
          <span>{{ selected?.name || '标注预览' }}</span>
          <ElButton v-if="previewUrl" class="preview-zoom" size="small" :aria-pressed="zoomed" aria-controls="inspection-image-viewport" @click="toggleZoom">
            {{ zoomed ? '适应屏幕' : '放大图片' }}
          </ElButton>
        </div>
        <div id="inspection-image-viewport" ref="previewViewport" class="image-viewport" :data-zoomed="zoomed" :tabindex="zoomed ? 0 : undefined" :aria-label="zoomed ? '放大的标注图，可滚动查看' : undefined">
          <p v-if="previewLoading" role="status">正在读取预览…</p>
          <img v-else-if="previewUrl && selected" :src="previewUrl" :alt="`${selected.name} 的检测标注`" />
          <p v-else>{{ selected ? '标注预览暂不可用。' : '选择图片查看标注。' }}</p>
        </div>
        <div v-if="selected" class="image-preview-meta">原图 {{ selected.width }} × {{ selected.height }} 像素</div>
      </section>
      <div class="report-inspector">
        <section v-if="selected" aria-labelledby="current-detections">
          <h3 id="current-detections" class="pane-heading">当前图片缺陷</h3>
          <ul v-if="selected.detections.length" class="report-readouts" aria-label="检出明细">
            <li v-for="(item, index) in selected.detections" :key="index">
              <span>{{ item.label }}</span><span>{{ (item.confidence * 100).toFixed(1) }}%</span>
            </li>
          </ul>
          <p v-else class="inspector-empty muted">该图片未检出缺陷。</p>
        </section>
        <section v-if="summary" aria-labelledby="report-distribution">
          <h3 id="report-distribution" class="pane-heading">任务缺陷统计</h3>
          <dl class="classes">
            <div v-for="(count, label) in summary.classes" :key="label">
              <dt>{{ label }}</dt>
              <dd>
                <span class="class-track" aria-hidden="true"><span :style="{ width: `${count / largestClass * 100}%` }"></span></span>
                <span>{{ count }}</span>
              </dd>
            </div>
          </dl>
        </section>
      </div>
    </div>
  </section>
</template>

<style scoped>
.blade-report { min-width: 0; container-type: inline-size; }
.report-model { margin: .2rem 0 0; font-size: .8rem; }
.report-loading { margin: 0; padding: 1rem; }
.totals { display: flex; flex-wrap: wrap; align-items: center; column-gap: 1.5rem; row-gap: .5rem; padding: .7rem 1rem; margin: 0; background: var(--blade-soft); border-bottom: 1px solid var(--line); }
.totals div { display: flex; align-items: baseline; gap: .38rem; font-size: .8125rem; }
.totals dt, .totals div > span { color: var(--muted); }
.totals dd { margin: 0; font-size: 1.2rem; font-weight: 600; font-variant-numeric: tabular-nums; }
.gallery-layout { display: grid; grid-template-columns: minmax(11rem, 17%) minmax(0, 1fr) minmax(11rem, 17%); }
.image-sidebar, .report-inspector { min-width: 0; }
.mobile-photo-picker { display: none; }
.pane-heading { margin: 0; padding: .85rem .8rem; border-bottom: 1px solid var(--line); font-size: .85rem; }
.report-images, .report-readouts { list-style: none; margin: 0; padding: 0; max-height: 33rem; overflow: auto; }
.report-images button { display: flex; justify-content: space-between; align-items: center; gap: .45rem; min-height: 44px; width: 100%; padding: .5rem .8rem; font: inherit; font-size: .8125rem; color: inherit; text-align: start; cursor: pointer; background: var(--surface); border: 0; border-bottom: 1px solid var(--line); }
.report-images button:focus-visible { outline-offset: -3px; }
.report-images button:hover { background: var(--blade-soft); }
.report-images button[aria-pressed='true'] { box-shadow: inset 3px 0 var(--accent); background: var(--blade-blue-soft); }
.report-images button span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.report-images strong { color: var(--muted); flex-shrink: 0; font-size: .75rem; }
.report-images .detected { color: #91401a; }
.image-preview { min-width: 0; display: grid; grid-template-rows: auto 1fr auto; align-content: start; color: #e3e8ef; background: #191d23; }
.image-preview-title, .image-preview-meta { padding: .75rem 1rem; background: #252a32; font-size: .8125rem; overflow-wrap: anywhere; }
.image-preview-title { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: .4rem; }
.image-preview-title span { min-width: 0; }
.image-preview-meta { color: #c1cad6; }
.image-viewport { display: grid; place-items: center; min-height: clamp(18rem, 55vh, 42rem); padding: .75rem; }
.image-viewport img { display: block; width: 100%; max-height: clamp(18rem, 55vh, 42rem); object-fit: contain; }
.image-viewport[data-zoomed='true'] {
  display: block;
  min-height: 0;
  max-height: min(55vh, 42rem);
  overflow: auto;
  padding: 0;
}
.image-viewport[data-zoomed='true'] img { width: max(150%, 52rem); max-height: none; }
.image-viewport[data-zoomed='true']:focus-visible { outline-offset: -3px; }
.image-viewport p { padding: 1rem; text-align: center; margin: 0; }
.report-inspector section + section { border-top: 1px solid var(--line); }
.report-readouts { max-height: 11rem; }
.report-readouts li, .classes > div { display: flex; align-items: center; justify-content: space-between; gap: .5rem; padding: .45rem .8rem; font-size: .8125rem; border-bottom: 1px solid var(--line); font-variant-numeric: tabular-nums; }
.report-readouts li span:last-child { color: var(--muted); }
.classes { margin: 0; }
.classes dd { display: flex; width: 65%; gap: .5rem; align-items: center; justify-content: space-between; margin: 0; text-align: end; }
.classes dd > span:last-child { min-width: 3ch; }
.class-track { flex: 1; background: var(--blade-blue-soft); }
.class-track > span { display: block; background: var(--accent); }
.class-track, .class-track > span { height: 4px; }
.inspector-empty { padding: .8rem; margin: 0; font-size: .8125rem; }
@container (max-width: 830px) {
  .gallery-layout { grid-template-columns: minmax(10rem, 28%) minmax(0, 1fr); }
  .report-inspector { grid-column: 1 / -1; display: grid; grid-template-columns: 1fr 1fr; border-top: 1px solid var(--line); }
  .report-inspector section + section { border-top: 0; border-left: 1px solid var(--line); }
}
@container (max-width: 520px) {
  .gallery-layout { grid-template-columns: minmax(0, 1fr); }
  .image-sidebar > .pane-heading, .report-images { display: none; }
  .mobile-photo-picker { display: grid; gap: .35rem; padding: .7rem .8rem; }
  .mobile-photo-picker label { font-size: .875rem; color: var(--muted); }
  .mobile-photo-picker select {
    width: 100%;
    min-height: 44px;
    padding-inline: .55rem;
    border: 1px solid var(--blade-input-line);
    border-radius: var(--blade-radius);
    color: inherit;
    background: var(--surface);
    font: inherit;
    text-overflow: ellipsis;
  }
  .totals { column-gap: .85rem; }
  .image-viewport { min-height: 10rem; padding: .4rem; }
  .image-viewport img { max-height: 48vh; }
  .report-inspector { grid-template-columns: minmax(0, 1fr); }
  .report-inspector section + section { border-left: 0; border-top: 1px solid var(--line); }
}
</style>
