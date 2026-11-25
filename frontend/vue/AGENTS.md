# Vue 模板

- 使用 Vue 3 单文件组件、`<script setup lang="ts">` 和 Composition API。局部状态用 `ref`，可推导状态用 `computed`。
- 通过 props 和事件表达组件契约。只有复用需求清晰时才提取 composable。
- 处理组件拆分、响应式状态或 composable 时，查阅 [Vue Best Practices](../skills/vue-best-practices/SKILL.md) 的相关章节。以本模板的约定和依赖为准。
- 修改此模板后运行 `npm run build`，它会先执行 `vue-tsc` 再生成静态产物。

上游参考

- [Vue TypeScript 指南](https://vuejs.org/guide/typescript/overview.html)
