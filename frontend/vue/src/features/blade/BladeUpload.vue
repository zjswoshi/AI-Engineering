<script setup lang="ts">
import { onBeforeUnmount, ref, shallowRef } from 'vue'
import { type RequestApi, requestError } from '../../lib/http'
import { bladeApi, type Task } from './api'

const props = defineProps<{ request: RequestApi }>()
const emit = defineEmits<{ submitted: [task: Task] }>()
const farmName = ref('')
const turbineNumber = ref('')
const file = shallowRef<File>()
const picker = ref<HTMLInputElement>()
const busy = ref(false)
const progress = ref(0)
const error = ref('')
let upload: AbortController | undefined
onBeforeUnmount(() => upload?.abort())

function chooseFile(event: Event) {
  const selected = (event.target as HTMLInputElement).files?.[0]
  file.value = undefined
  error.value = ''
  if (!selected) return
  if (!selected.name.toLowerCase().endsWith('.zip') || !selected.size || selected.size > 10_000_000_000) {
    error.value = '请选择一个不超过 10 GB 的非空 ZIP。'
    if (picker.value) picker.value.value = ''
    return
  }
  file.value = selected
}

async function submit() {
  if (!file.value || busy.value) return
  upload = new AbortController()
  busy.value = true
  progress.value = 0
  error.value = ''
  try {
    const task = await bladeApi(props.request).submit(farmName.value, turbineNumber.value, file.value,
      (percent) => { progress.value = percent }, upload.signal)
    file.value = undefined
    if (picker.value) picker.value.value = ''
    emit('submitted', task)
  } catch (failure) {
    error.value = requestError(failure)
  } finally {
    busy.value = false
    upload = undefined
  }
}
</script>

<template>
  <section class="panel" aria-labelledby="blade-upload-title">
    <h2 id="blade-upload-title">上传并检测</h2>
    <p class="muted">每个 ZIP 对应一台风机，支持 JPG、JPEG 和 PNG。ZIP 上限 10 GB。</p>
    <form class="blade-upload" @submit.prevent="submit">
      <div class="field"><label for="farm-name">风场</label><ElInput id="farm-name" v-model="farmName" maxlength="80" :disabled="busy" /></div>
      <div class="field"><label for="turbine-number">风机编号</label><ElInput id="turbine-number" v-model="turbineNumber" maxlength="60" :disabled="busy" /></div>
      <div class="field"><label for="inspection-zip">图片 ZIP</label><input id="inspection-zip" ref="picker" type="file" accept=".zip,application/zip" :disabled="busy" @change="chooseFile" /></div>
      <ElAlert v-if="error" :title="error" type="error" :closable="false" />
      <div v-if="busy" aria-live="polite">
        <ElProgress :percentage="progress" />
        <p v-if="progress === 100" class="muted">正在校验 ZIP 并创建任务。</p>
      </div>
      <div class="flex gap-2">
        <ElButton native-type="submit" type="primary" :disabled="!file || !farmName.trim() || !turbineNumber.trim()" :loading="busy">开始检测</ElButton>
        <ElButton v-if="busy" @click="upload?.abort()">取消上传</ElButton>
      </div>
    </form>
  </section>
</template>

<style scoped>
.blade-upload { display: grid; gap: 0.8rem; max-width: 38rem; margin-top: 1rem; }
.blade-upload :deep(input[type='file']) { display: block; max-width: 100%; }
</style>
