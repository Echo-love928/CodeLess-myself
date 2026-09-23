<script setup lang="ts">
import { RouterView, useRoute } from 'vue-router'

import GlobalFooter from '@/components/GlobalFooter.vue'
import GlobalHeader from '@/components/GlobalHeader.vue'

const route = useRoute()
</script>

<template>
  <a-layout class="basic-layout" :class="{ 'basic-layout--home': route.path === '/', 'basic-layout--chat': route.path.startsWith('/app/chat/') }">
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

.basic-layout--home {
  background:
    radial-gradient(ellipse 35% 14% at 20% 13%, rgb(199 241 233 / 86%), transparent 90%),
    radial-gradient(ellipse 40% 19% at 80% 23%, rgb(218 223 255 / 94%), transparent 90%),
    radial-gradient(ellipse 52% 24% at 52% 48%, rgb(111 135 233 / 39%), transparent 92%),
    radial-gradient(ellipse 39% 18% at 8% 76%, rgb(185 230 242 / 38%), transparent 92%),
    linear-gradient(180deg, #f7f9ff 0%, #eff5ff 30%, #e3eaff 48%, #edf5fa 74%, #f7f9ff 100%);
}

.basic-layout__content {
  width: 100%;
  max-width: 1440px;
  margin: 0 auto;
  padding: 28px 32px 40px;
}

.basic-layout--home .basic-layout__content,
.basic-layout--chat .basic-layout__content {
  max-width: none;
  padding: 0;
}

.basic-layout--chat {
  background: #fff;
}

@media (max-width: 767px) {
  .basic-layout--home {
    background:
      radial-gradient(ellipse 70% 13% at 15% 8%, rgb(199 241 233 / 60%), transparent 90%),
      radial-gradient(ellipse 80% 18% at 85% 28%, rgb(218 223 255 / 64%), transparent 90%),
      linear-gradient(180deg, #f7f9ff, #eef2ff 48%, #f7f9ff);
  }

  .basic-layout__content {
    padding: 20px 16px 32px;
  }
}
</style>
