<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { session } from '../features/auth/session'
import { bladeApi, formatDate, statusLabel, type Page, type Task } from '../features/blade/api'
import BladeReport from '../features/blade/BladeReport.vue'
import BladeUpload from '../features/blade/BladeUpload.vue'
import { requestError } from '../lib/http'

const route = useRoute()
const router = useRouter()
const tasks = shallowRef<Page<Task>>()
const task = shallowRef<Task>()
const listPage = ref(1)
const busy = ref(false)
const error = ref('')
const taskId = computed(() => typeof route.query.task === 'string' ? route.query.task : '')
const canUpload = computed(() => ['OPERATOR', 'ADMIN'].includes(session.value?.profile.role ?? ''))
let timer: ReturnType<typeof setTimeout> | undefined
let generation = 0
let disposed = false

async function load() {
  if (timer) clearTimeout(timer)
  const current = ++generation
  const id = taskId.value
  const request = session.value?.request
  if (!request) return
  const api = bladeApi(request)
  busy.value = true
  error.value = ''
  try {
    if (id) {
      const result = await api.task(id)
      if (current !== generation || disposed) return
      task.value = result
      if (['PENDING', 'RUNNING'].includes(result.status)) timer = setTimeout(load, 2500)
    } else {
      const result = await api.list(listPage.value)
      if (current !== generation || disposed) return
      tasks.value = result
      if (result.items.some((item) => ['PENDING', 'RUNNING'].includes(item.status))) timer = setTimeout(load, 5000)
    }
  } catch (failure) {
    if (current === generation && !disposed) error.value = requestError(failure)
  } finally {
    if (current === generation && !disposed) busy.value = false
  }
}

function open(result: Task) {
  void router.push({ path: '/detections', query: { task: result.id } })
}

function changePage(page: number) {
  listPage.value = page
  void load()
}

watch(taskId, () => {
  task.value = undefined
  void load()
})
onMounted(() => void load())
onBeforeUnmount(() => {
  disposed = true
  if (timer) clearTimeout(timer)
})
</script>

<template>
  <div class="page-title"><div><h1>{{ taskId ? '检测任务详情' : '叶片检测任务' }}</h1></div></div>
  <div class="grid gap-4">
    <RouterLink v-if="taskId" to="/detections" class="blade-back">← 返回检测任务</RouterLink>
    <ElAlert v-if="error" :title="error" type="error" :closable="false" />
    <BladeUpload v-if="session && canUpload && !taskId" :request="session.request" @submitted="open" />
    <section v-if="!taskId" class="panel" aria-labelledby="blade-tasks">
      <div class="blade-heading"><h2 id="blade-tasks">历史检测任务</h2><ElButton :loading="busy" @click="load">刷新</ElButton></div>
      <ElTable v-if="tasks?.items.length" :data="tasks.items" row-key="id" class="mt-4">
        <ElTableColumn label="风场" prop="farmName" min-width="140" />
        <ElTableColumn label="风机编号" prop="turbineNumber" min-width="120" />
        <ElTableColumn label="提交时间" min-width="175"><template #default="{ row }">{{ formatDate(row.createdAt) }}</template></ElTableColumn>
        <ElTableColumn label="状态" min-width="95"><template #default="{ row }">{{ statusLabel[(row as Task).status] }}</template></ElTableColumn>
        <ElTableColumn label="操作" width="120"><template #default="{ row }"><ElButton link type="primary" @click="open(row)">{{ row.status === 'COMPLETED' ? '查看报告' : '查看任务' }}</ElButton></template></ElTableColumn>
      </ElTable>
      <p v-else-if="tasks && !error" class="muted mt-4">暂无检测任务。</p>
      <div v-if="tasks && (tasks.page > 1 || tasks.hasNext)" class="flex items-center gap-3 mt-4">
        <ElButton :disabled="tasks.page === 1 || busy" @click="changePage(tasks.page - 1)">上一页</ElButton>
        <span>第 {{ tasks.page }} 页</span>
        <ElButton :disabled="!tasks.hasNext || busy" @click="changePage(tasks.page + 1)">下一页</ElButton>
      </div>
    </section>
    <section v-else-if="task" class="panel" aria-labelledby="blade-task-title">
      <div class="blade-heading"><h2 id="blade-task-title">{{ task.farmName }} · {{ task.turbineNumber }}</h2><ElTag>{{ statusLabel[task.status] }}</ElTag></div>
      <p class="muted blade-filename">{{ task.originalName }} · {{ formatDate(task.createdAt) }}</p>
      <p class="muted blade-filename">任务编号 {{ task.id }}</p>
      <p v-if="task.status === 'RUNNING' || task.status === 'PENDING'" role="status" class="mt-4">可以离开页面，稍后回到历史任务查看报告。</p>
      <ElAlert v-if="task.failureMessage" :title="task.failureMessage" type="error" :closable="false" class="mt-4" />
    </section>
    <section v-if="session && task?.status === 'COMPLETED'" class="panel">
      <BladeReport :key="task.id" :request="session.request" :id="task.id" />
    </section>
    <ElButton v-if="taskId && error" :loading="busy" @click="load">重新加载</ElButton>
  </div>
</template>

<style scoped>
.blade-heading { display: flex; justify-content: space-between; align-items: center; gap: 0.8rem; flex-wrap: wrap; }
.blade-filename { overflow-wrap: anywhere; }
.blade-back { color: #137e73; width: fit-content; }
</style>
