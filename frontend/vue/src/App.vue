<script setup lang="ts">
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { ElConfigProvider } from 'element-plus'
</script>

<template>
  <ElConfigProvider :locale="zhCn"><RouterView /></ElConfigProvider>
</template>
