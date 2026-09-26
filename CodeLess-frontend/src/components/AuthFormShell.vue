<script setup lang="ts">
defineProps<{ title: string }>()
</script>

<template>
  <section class="auth-form-shell">
    <h2>{{ title }}</h2>
    <p>不写一行代码，生成完整应用</p>
    <slot />
  </section>
</template>

<style scoped>
.auth-form-shell {
  max-width: 360px;
  margin: 0 auto;
}
.auth-form-shell h2 {
  margin-bottom: 16px;
  text-align: center;
}
.auth-form-shell p {
  margin: 0 0 16px;
  color: #bbb;
  text-align: center;
}
.auth-form-shell :deep(.tips) {
  margin-bottom: 16px;
  color: #bbb;
  font-size: 13px;
  text-align: right;
}
</style>
