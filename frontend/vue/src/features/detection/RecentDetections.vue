<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { requestError } from '../../lib/http'
import { hasAccess } from '../auth/model'
import { session } from '../auth/session'
import { formatDate, listDetections, submitRoles, type Detection } from './api'

const rows = ref<Detection[]>([])
const busy = ref(false)
const error = ref('')
const canSubmit = computed(() => !!session.value && hasAccess(session.value.profile.role, submitRoles))

async function refresh() {
  if (!session.value || busy.value) return
  busy.value = true
  error.value = ''
  try {
    rows.value = (await listDetections(session.value.request)).slice(0, 5)
  } catch (cause) {
    error.value = requestError(cause)
  } finally {
    busy.value = false
  }
}
onMounted(refresh)
</script>

<template>
  <section class="panel" aria-labelledby="tasks-title">
    <div class="page-title">
      <div><h2 id="tasks-title">最近检测任务</h2><p class="muted">使用样本编号体验检测流程。</p></div>
      <div class="flex flex-wrap items-center gap-3">
        <ElButton v-if="canSubmit" tag="a" href="/detections?new=1" type="primary" @click.prevent="$router.push('/detections?new=1')">新建检测任务</ElButton>
        <RouterLink class="shortcut" to="/detections">查看历史结果</RouterLink>
      </div>
    </div>
    <ElAlert v-if="error" :title="error" type="error" :closable="false" />
    <p v-if="busy" role="status">正在加载任务…</p>
    <p v-else-if="!error && !rows.length" class="muted">暂无检测任务。</p>
    <ElTable v-if="rows.length" :data="rows" row-key="id" class="surface-table">
      <ElTableColumn prop="sample" label="样本编号" min-width="130" />
      <ElTableColumn label="状态" min-width="110"><template #default="{ row }"><ElTag :type="row.status === 'COMPLETED' ? 'success' : 'info'">{{ row.status === 'COMPLETED' ? '已完成' : '待完成' }}</ElTag></template></ElTableColumn>
      <ElTableColumn label="创建时间" min-width="165"><template #default="{ row }">{{ formatDate(row.createdAt) }}</template></ElTableColumn>
    </ElTable>
  </section>
</template>
