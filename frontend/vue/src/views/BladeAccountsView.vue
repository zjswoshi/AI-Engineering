<script setup lang="ts">
import { ref } from 'vue'
import { session } from '../features/auth/session'
import { requestError } from '../lib/http'

const username = ref('')
const password = ref('')
const busy = ref(false)
const success = ref('')
const error = ref('')

async function create() {
  if (!session.value || busy.value) return
  busy.value = true
  success.value = ''
  error.value = ''
  try {
    await session.value.request('admin/blade-operators', 'POST', { username: username.value.trim(), password: password.value })
    success.value = `已创建巡检账号 ${username.value.trim()}。`
    username.value = ''
    password.value = ''
  } catch (failure) {
    error.value = requestError(failure)
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="page-title"><div><h1>巡检账号</h1></div></div>
  <section class="panel">
    <form class="grid gap-4 max-w-md" @submit.prevent="create">
      <div class="field"><label for="operator-name">账号</label><ElInput id="operator-name" v-model="username" maxlength="40" autocomplete="off" /></div>
      <div class="field"><label for="operator-password">密码</label><ElInput id="operator-password" v-model="password" type="password" autocomplete="new-password" /><p class="muted">至少 12 个字符。账号使用字母、数字、点、下划线或短横线。</p></div>
      <ElAlert v-if="error" :title="error" type="error" :closable="false" />
      <ElAlert v-if="success" :title="success" type="success" :closable="false" />
      <ElButton type="primary" native-type="submit" :loading="busy" :disabled="!username.trim() || password.length < 12">创建巡检账号</ElButton>
    </form>
  </section>
</template>
