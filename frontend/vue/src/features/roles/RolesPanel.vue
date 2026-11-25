<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { requestError } from '../../lib/http'
import { session } from '../auth/session'
import type { AnalystRole } from '../auth/model'
import { loadAnalyst, saveAnalyst } from './api'

const role = ref<AnalystRole>()
const savedRole = ref<AnalystRole>()
const busy = ref(false)
const error = ref('')
const notice = ref('')

async function load() {
  const current = session.value
  if (!current || busy.value) return
  busy.value = true
  error.value = ''
  try {
    savedRole.value = role.value = (await loadAnalyst(current.request)).role
  } catch (cause) {
    error.value = requestError(cause)
  } finally {
    busy.value = false
  }
}

async function save() {
  const current = session.value
  if (!current || !role.value || busy.value) return
  busy.value = true
  error.value = notice.value = ''
  try {
    savedRole.value = role.value = (await saveAnalyst(current.request, role.value)).role
    notice.value = '权限已更新。'
  } catch (cause) {
    error.value = requestError(cause)
  } finally {
    busy.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="panel max-w-xl" aria-labelledby="role-form-title">
    <h2 id="role-form-title">analyst</h2>
    <form @submit.prevent="save">
      <div class="field">
        <label for="analyst-role">角色</label>
        <ElSelect id="analyst-role" v-model="role" :disabled="busy || !savedRole" placeholder="选择角色">
          <ElOption label="查看者" value="VIEWER" /><ElOption label="操作员" value="OPERATOR" />
        </ElSelect>
      </div>
      <ElButton type="primary" native-type="submit" :disabled="!role || role === savedRole" :loading="busy">保存权限</ElButton>
      <ElButton v-if="error && !savedRole" @click="load">重试</ElButton>
    </form>
    <ElAlert v-if="error" class="mt-4" :title="error" type="error" :closable="false" />
    <p class="status-space" role="status">{{ notice }}</p>
  </section>
</template>
