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
const uploadOpen = ref(false)
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
  uploadOpen.value = false
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
  <header class="blade-page-header">
    <div>
      <RouterLink v-if="taskId" to="/detections" class="blade-back">← 返回检测任务</RouterLink>
      <h1>{{ task ? `${task.farmName} · ${task.turbineNumber}` : taskId ? '检测任务详情' : '叶片检测' }}</h1>
      <p v-if="task" class="muted blade-filename" :title="`任务编号 ${task.id}`">{{ task.originalName }} · {{ formatDate(task.createdAt) }}</p>
    </div>
    <span v-if="task" class="blade-status" :data-status="task.status">{{ statusLabel[task.status] }}</span>
  </header>
  <ElAlert v-if="error" :title="error" type="error" :closable="false" />
  <ElDrawer v-if="session && canUpload && !taskId" v-model="uploadOpen" class="blade-drawer" title="新建检测" size="min(100%, 34rem)" :close-on-click-modal="false" destroy-on-close>
    <BladeUpload :request="session.request" @submitted="open" />
  </ElDrawer>
  <template v-if="!taskId">
    <section class="blade-sheet" aria-labelledby="blade-tasks" :aria-busy="busy">
      <div class="blade-toolbar">
        <h2 id="blade-tasks">历史检测任务</h2>
        <div class="blade-toolbar-actions">
          <ElButton :loading="busy" @click="load">刷新</ElButton>
          <ElButton v-if="canUpload" type="primary" @click="uploadOpen = true">新建检测</ElButton>
        </div>
      </div>
      <div v-if="tasks?.items.length" class="surface-table">
        <table class="blade-task-table" role="table" aria-label="历史检测任务">
          <thead role="rowgroup">
            <tr role="row"><th scope="col" role="columnheader">风场</th><th scope="col" role="columnheader">风机编号</th><th scope="col" role="columnheader">提交时间</th><th scope="col" role="columnheader">状态</th><th scope="col" role="columnheader">操作</th></tr>
          </thead>
          <tbody role="rowgroup">
            <tr v-for="row in tasks.items" :key="row.id" role="row">
              <td role="cell" class="task-farm">{{ row.farmName }}</td>
              <td role="cell" class="task-turbine"><span class="task-field-label">风机编号 </span>{{ row.turbineNumber }}</td>
              <td role="cell" class="task-date"><span class="task-field-label">提交时间 </span>{{ formatDate(row.createdAt) }}</td>
              <td role="cell" class="task-state"><span class="blade-status" :data-status="row.status">{{ statusLabel[row.status] }}</span></td>
              <td role="cell" class="task-action"><ElButton link type="primary" @click="open(row)">{{ row.status === 'COMPLETED' ? '查看报告' : '查看任务' }}</ElButton></td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-else-if="busy" class="blade-empty muted" role="status">正在读取检测任务…</p>
      <p v-else-if="tasks && !error" class="blade-empty muted">暂无检测任务。</p>
      <div v-if="tasks && (tasks.page > 1 || tasks.hasNext)" class="blade-pagination">
        <ElButton :disabled="tasks.page === 1 || busy" @click="changePage(tasks.page - 1)">上一页</ElButton>
        <span>第 {{ tasks.page }} 页</span>
        <ElButton :disabled="!tasks.hasNext || busy" @click="changePage(tasks.page + 1)">下一页</ElButton>
      </div>
    </section>
  </template>
  <template v-else-if="task">
    <section v-if="task.status !== 'COMPLETED'" class="blade-sheet blade-task-state" aria-label="任务状态">
      <p class="muted blade-filename">任务编号 {{ task.id }}</p>
      <p v-if="task.status === 'RUNNING' || task.status === 'PENDING'" role="status">可以离开页面，稍后回到历史任务查看报告。</p>
      <ElAlert v-if="task.failureMessage" :title="task.failureMessage" type="error" :closable="false" />
    </section>
    <BladeReport v-else-if="session" :key="task.id" :request="session.request" :id="task.id" />
  </template>
  <p v-else-if="busy" class="blade-empty muted" role="status">正在读取检测任务…</p>
  <ElButton v-if="taskId && error" :loading="busy" @click="load">重新加载</ElButton>
</template>

<style scoped>
.blade-page-header h1, .blade-filename { overflow-wrap: anywhere; }
.blade-back { display: inline-flex; align-items: center; min-height: 44px; font-size: .875rem; text-decoration: none; }
.blade-back:hover { text-decoration: underline; }
.blade-task-state { padding: 1rem; }
.blade-empty { text-align: center; padding: 3.5rem 1rem; margin: 0; }
.blade-task-table { width: 100%; min-width: 39rem; border-collapse: collapse; font-size: .875rem; }
.blade-task-table th { text-align: start; background: var(--blade-soft); color: var(--muted); font-weight: 600; }
.blade-task-table th, .blade-task-table td { padding: .35rem 1rem; }
.blade-task-table th { height: 40px; }
.blade-task-table td { border-top: 1px solid var(--line); overflow-wrap: anywhere; }
.blade-task-table tbody tr:hover { background: var(--blade-soft); }
.blade-task-table .el-button { min-height: 44px; }
.task-farm { font-weight: 600; }
.task-date, .task-state, .task-action { white-space: nowrap; }
.task-field-label { display: none; }
@media (max-width: 56rem) {
  .blade-task-table, .blade-task-table tbody { display: block; min-width: 0; }
  .blade-task-table thead { position: absolute; inline-size: 1px; block-size: 1px; overflow: hidden; clip-path: inset(50%); }
  .blade-task-table tbody tr {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    align-items: center;
    column-gap: .75rem;
    row-gap: .2rem;
    border-bottom: 1px solid var(--line);
    padding: .72rem .85rem .38rem;
  }
  .blade-task-table tbody tr:last-child { border-bottom: 0; }
  .blade-task-table td { border: 0; padding: 0; }
  .task-field-label { display: inline; color: var(--muted); margin-inline-end: .25rem; }
  .task-farm { grid-area: 1 / 1; font-size: 1rem; }
  .task-state { grid-area: 1 / 2; justify-self: end; }
  .task-turbine { grid-row: 2; grid-column: 1 / -1; }
  .task-date { grid-area: 3 / 1; white-space: normal; }
  .task-action { grid-area: 3 / 2; justify-self: end; }
  .task-action .el-button { padding-inline: .4rem; }
}
</style>
