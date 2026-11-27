<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { roleLabels } from '../features/auth/model'
import { session } from '../features/auth/session'
import { bladeMode, visibleSections } from '../router/sections'

const router = useRouter()
const collapsed = ref(false)
const menu = computed(() => session.value ? visibleSections(session.value.profile.role) : [])

function logout() {
  session.value = undefined
  void router.replace('/login')
}
</script>

<template>
  <div v-if="session" class="shell" :class="{ 'blade-shell': bladeMode }" :data-collapsed="collapsed">
    <a class="skip-link" href="#main-content" @click.prevent="($refs.main as HTMLElement).focus()">跳到主内容</a>
    <aside class="side">
      <div class="brand"><span class="brand-mark" aria-hidden="true">AI</span><span v-if="bladeMode || !collapsed" class="brand-label">{{ bladeMode ? '风机叶片巡检' : '检测工作台' }}</span></div>
      <nav id="primary-navigation" class="nav" aria-label="主导航">
        <RouterLink v-for="item in menu" :key="item.path" :to="item.path" :aria-label="item.label" :title="collapsed ? item.label : undefined">
          <span v-if="bladeMode && collapsed" class="blade-nav-mark" aria-hidden="true">{{ item.short.slice(0, 1) }}</span>
          <span class="nav-label">{{ !bladeMode && collapsed ? item.short : item.label }}</span>
        </RouterLink>
      </nav>
    </aside>
    <div class="min-w-0" :class="{ 'blade-shell-body': bladeMode }">
      <header class="topbar">
        <ElButton class="nav-toggle" size="small" :text="bladeMode" aria-controls="primary-navigation" :aria-expanded="!collapsed" @click="collapsed = !collapsed">{{ collapsed ? '展开导航' : '折叠导航' }}</ElButton>
        <div class="topbar-account flex flex-wrap items-center gap-3">
          <span>{{ session.profile.username }}</span><ElTag type="info">{{ roleLabels[session.profile.role] }}</ElTag>
          <ElButton size="small" @click="logout">退出</ElButton>
        </div>
      </header>
      <main id="main-content" ref="main" class="content" tabindex="-1"><RouterView /></main>
    </div>
  </div>
</template>
