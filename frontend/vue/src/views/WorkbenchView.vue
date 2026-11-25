<script setup lang="ts">
import { computed } from 'vue'
import { session } from '../features/auth/session'
import { visibleSections, workbenchFeatures } from '../router/sections'

const shortcuts = computed(() => session.value ? visibleSections(session.value.profile.role).filter((item) => item.path !== '/') : [])
</script>

<template>
  <div class="page-title"><div><h1>工作台</h1><p class="muted">{{ session?.profile.username }}，欢迎回来</p></div></div>
  <div class="grid gap-4">
    <component :is="feature" v-for="(feature, index) in workbenchFeatures" :key="index" />
    <section v-if="shortcuts.length" class="panel" aria-labelledby="shortcuts-title">
      <h2 id="shortcuts-title">快捷入口</h2>
      <div class="flex flex-wrap gap-4">
        <RouterLink v-for="item in shortcuts" :key="item.path" :to="item.path" class="shortcut">{{ item.label }}</RouterLink>
      </div>
    </section>
  </div>
</template>
