<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { requestError } from '../../lib/http'
import { hasAccess } from '../auth/model'
import { session } from '../auth/session'
import { formatDate, listDetections, submitDetection, submitRoles, type Detection } from './api'

const route = useRoute()
const router = useRouter()
const rows = ref<Detection[]>([])
const sample = ref('')
const busy = ref(false)
const error = ref('')
const notice = ref('')
const selected = ref<Detection>()
const drawerOpen = ref(false)
const canSubmit = computed(() => !!session.value && hasAccess(session.value.profile.role, submitRoles))
const creating = computed(() => canSubmit.value && route.query.new === '1')

function showForm(show: boolean) {
  const query = { ...route.query }
  if (show) query.new = '1'
  else delete query.new
  return router.replace({ query })
}

async function refresh() {
  if (!session.value || busy.value) return
  busy.value = true
  error.value = ''
  try {
    rows.value = await listDetections(session.value.request)
    if (selected.value) selected.value = rows.value.find((item) => item.id === selected.value?.id) ?? selected.value
  } catch (cause) {
    error.value = requestError(cause)
  } finally {
    busy.value = false
  }
}

async function submit() {
  if (!session.value || !canSubmit.value || busy.value || !sample.value.trim()) return
  busy.value = true
  error.value = notice.value = ''
  try {
    const job = await submitDetection(session.value.request, sample.value.trim())
    rows.value = [job, ...rows.value].slice(0, 20)
    sample.value = ''
    notice.value = job.status === 'COMPLETED' ? '检测已完成。' : '任务已受理，可刷新查看状态。'
    await showForm(false)
    openResult(job)
  } catch (cause) {
    error.value = requestError(cause)
  } finally {
    busy.value = false
  }
}

function openResult(job: Detection) {
  selected.value = job
  drawerOpen.value = true
}

onMounted(refresh)
</script>

<template>
  <div class="page-title">
    <div><h1>检测任务</h1><p class="muted">查看当前账号可访问的检测记录。</p></div>
    <div class="flex flex-wrap gap-2">
      <ElButton :loading="busy" @click="refresh">刷新</ElButton>
      <ElButton v-if="canSubmit && !creating" type="primary" @click="showForm(true)">新建检测任务</ElButton>
    </div>
  </div>
  <div class="grid gap-4">
    <section v-if="creating" class="panel" aria-labelledby="sample-form-title">
      <h2 id="sample-form-title">提交样本检测</h2>
      <form class="max-w-lg" @submit.prevent="submit">
        <div class="field"><label for="sample">样本编号</label><ElInput id="sample" v-model="sample" maxlength="120" placeholder="例如 sample-001" /></div>
        <ElButton type="primary" native-type="submit" :loading="busy" :disabled="!sample.trim()">开始检测</ElButton>
        <ElButton :disabled="busy" @click="showForm(false)">取消</ElButton>
      </form>
    </section>
    <ElAlert v-if="error" :title="error" type="error" :closable="false" />
    <section class="panel" aria-labelledby="history-title">
      <h2 id="history-title">最近记录</h2>
      <p v-if="busy && !rows.length" role="status">正在加载任务…</p>
      <ElTable v-else :data="rows" row-key="id" empty-text="暂无检测任务" class="surface-table">
        <ElTableColumn label="创建时间" min-width="165"><template #default="{ row }">{{ formatDate(row.createdAt) }}</template></ElTableColumn>
        <ElTableColumn prop="sample" label="样本编号" min-width="135" />
        <ElTableColumn prop="owner" label="提交者" min-width="100" />
        <ElTableColumn label="状态" min-width="100"><template #default="{ row }"><ElTag :type="row.status === 'COMPLETED' ? 'success' : 'info'">{{ row.status === 'COMPLETED' ? '已完成' : '待完成' }}</ElTag></template></ElTableColumn>
        <ElTableColumn label="检测结果" min-width="125"><template #default="{ row }"><ElButton link type="primary" @click="openResult(row)">{{ row.result ? '查看结果' : '查看状态' }}</ElButton></template></ElTableColumn>
      </ElTable>
    </section>
  </div>
  <p class="status-space" role="status">{{ notice }}</p>
  <ElDrawer v-model="drawerOpen" title="任务详情" size="min(100%, 30rem)">
    <div v-if="selected" class="grid gap-3">
      <h2>{{ selected.sample }}</h2>
      <p class="muted">{{ formatDate(selected.createdAt) }} · {{ selected.owner }}</p>
      <template v-if="selected.result"><p>检出项：{{ selected.result.label }}</p><p>置信度：{{ selected.result.score }}</p></template>
      <template v-else><p>任务待完成。</p><ElButton :loading="busy" @click="refresh">刷新任务状态</ElButton></template>
    </div>
  </ElDrawer>
</template>
