<script setup lang="ts">
defineProps<{ forbidden?: boolean }>()
</script>

<template>
  <section class="panel" aria-labelledby="status-title">
    <h1 id="status-title">{{ forbidden ? '403 无权限' : '404 页面不存在' }}</h1>
    <p class="muted">{{ forbidden ? '当前账号无权打开此页面。' : '请通过侧栏进入需要的功能。' }}</p>
    <RouterLink to="/" class="shortcut">返回工作台</RouterLink>
  </section>
</template>
