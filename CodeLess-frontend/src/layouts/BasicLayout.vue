<script setup lang="ts">
import { RouterView } from 'vue-router'

import GlobalFooter from '@/components/GlobalFooter.vue'
import GlobalHeader from '@/components/GlobalHeader.vue'
</script>

<template>
  <a-layout class="basic-layout">
    <GlobalHeader />

    <a-layout-content class="basic-layout__content">
      <RouterView />
    </a-layout-content>

    <GlobalFooter />
  </a-layout>
</template>

<style>
:root {
  color: #24324a;
  background: #f6f8fc;
  font-family:
    Inter, "PingFang SC", "Microsoft YaHei", system-ui, -apple-system, BlinkMacSystemFont,
    "Segoe UI", sans-serif;
  font-synthesis: none;
  text-rendering: optimizeLegibility;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

*,
*::before,
*::after {
  box-sizing: border-box;
}

html,
body,
#app {
  min-width: 320px;
  min-height: 100%;
  margin: 0;
}

body {
  min-height: 100vh;
}

button,
input,
textarea,
select {
  font: inherit;
}

.basic-layout {
  min-height: 100vh;
  background: #f6f8fc;
}

.basic-layout__content {
  width: 100%;
  max-width: 1440px;
  margin: 0 auto;
  padding: 28px 32px 40px;
}

@media (max-width: 767px) {
  .basic-layout__content {
    padding: 20px 16px 32px;
  }
}
</style>
