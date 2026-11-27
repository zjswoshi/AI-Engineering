<script setup lang="ts">
import { computed } from 'vue'
import { session } from '../features/auth/session'
import { bladeMode, visibleSections, workbenchFeatures } from '../router/sections'

const shortcuts = computed(() => {
  if (!session.value) return []
  return visibleSections(session.value.profile.role).filter((item) => item.path !== '/')
})
</script>

<template>
  <template v-if="bladeMode">
    <header class="blade-page-header"><h1>工作台</h1></header>
    <section class="blade-sheet blade-home" aria-labelledby="blade-home-title">
      <div class="blade-home-content">
        <h2 id="blade-home-title">风机叶片巡检</h2>
        <nav aria-label="工作台快捷入口" class="blade-home-links">
          <RouterLink v-for="item in shortcuts" :key="item.path" :to="item.path" class="blade-home-link"
            :class="{ 'blade-home-link-primary': item.path === '/detections' }"
            :aria-labelledby="`shortcut-title-${item.short}`" :aria-describedby="`shortcut-description-${item.short}`">
            <div>
              <span :id="`shortcut-title-${item.short}`" class="blade-home-link-title">{{ item.label }}</span>
              <span :id="`shortcut-description-${item.short}`" class="blade-home-link-description">
                {{ item.path === '/detections' ? '查看历史任务和缺陷报告' : '为巡检人员开通账号' }}
              </span>
            </div>
            <span class="blade-home-link-action" aria-hidden="true">
              <template v-if="item.path === '/detections'">打开任务</template>
            </span>
          </RouterLink>
        </nav>
      </div>
      <div class="blade-home-image" aria-hidden="true"></div>
    </section>
  </template>
  <template v-else>
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
</template>
