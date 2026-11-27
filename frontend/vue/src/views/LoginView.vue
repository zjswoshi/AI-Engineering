<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { requestError } from '../lib/http'
import { authenticate } from '../features/auth/model'
import { session } from '../features/auth/session'
import { allowedReturn } from '../router'
import { bladeMode } from '../router/sections'

const route = useRoute()
const router = useRouter()
const username = ref('')
const password = ref('')
const busy = ref(false)
const error = ref('')

async function login() {
  if (busy.value || !username.value.trim() || !password.value) return
  busy.value = true
  error.value = ''
  try {
    const current = await authenticate(username.value, password.value)
    const target = allowedReturn(route.query.returnTo, current.profile.role)
    session.value = current
    await router.replace(target)
  } catch (cause) {
    error.value = requestError(cause)
  } finally {
    busy.value = false
    password.value = ''
  }
}
</script>

<template>
  <main class="login-shell" :class="{ 'blade-login': bladeMode }">
    <div v-if="bladeMode" class="blade-login-cover">
      <div class="blade-login-copy">
        <div class="brand"><span class="brand-mark" aria-hidden="true">AI</span>风机叶片巡检</div>
        <p class="blade-login-display">风机叶片<br />智能巡检平台</p>
        <p>从巡检图片到缺陷标注与检测报告。</p>
      </div>
    </div>
    <section class="panel login-panel" aria-labelledby="login-title">
      <div v-if="!bladeMode" class="brand"><span class="brand-mark" aria-hidden="true">AI</span>检测工作台</div>
      <h1 id="login-title">登录</h1>
      <p v-if="bladeMode" class="muted">使用管理员或巡检账号进入工作台。</p>
      <form class="mt-6" @submit.prevent="login">
        <div class="field"><label for="username">账号</label><ElInput id="username" v-model="username" autocomplete="username" maxlength="40" /></div>
        <div class="field"><label for="password">密码</label><ElInput id="password" v-model="password" type="password" autocomplete="current-password" maxlength="120" /></div>
        <ElAlert v-if="error" :title="error" type="error" :closable="false" class="mb-4" />
        <ElButton class="w-full" type="primary" native-type="submit" :loading="busy" :disabled="!username.trim() || !password">登录</ElButton>
      </form>
    </section>
  </main>
</template>
