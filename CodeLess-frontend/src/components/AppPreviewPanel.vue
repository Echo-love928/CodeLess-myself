<script setup lang="ts">
import { BulbOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import logoUrl from '@/assets/logo.png'

defineProps<{
  url: string
  ready: boolean
  generating: boolean
  version: number
  appName?: string
  stage?: string
  stages: readonly string[]
  stageIndex: number
}>()
const emit = defineEmits<{ refresh: [] }>()
</script>

<template>
  <section class="preview" aria-label="网页预览">
    <div class="preview__toolbar">
      <div class="traffic-lights"><i /><i /><i /></div>
      <div class="preview__address">{{ url || '网站生成完成后将在这里展示' }}</div>
      <a-button type="text" :disabled="!url" aria-label="刷新预览" @click="emit('refresh')"
        ><ReloadOutlined
      /></a-button>
    </div>
    <iframe
      v-if="ready && !generating"
      :key="version"
      :src="url"
      :title="`${appName || '应用'}预览`"
      sandbox="allow-scripts allow-forms allow-modals allow-popups allow-same-origin"
    />
    <div v-else-if="generating" class="preview__loading" role="status" aria-live="polite">
      <div class="builder-orbit" aria-hidden="true">
        <span class="builder-orbit__ring builder-orbit__ring--outer"><i /></span>
        <span class="builder-orbit__ring builder-orbit__ring--inner"><i /></span>
        <span class="builder-orbit__core"><img :src="logoUrl" alt="" /></span>
      </div>
      <h2>{{ stage }}</h2>
      <p>完成后会自动刷新右侧预览，无需离开当前页面。</p>
      <div class="stage-dots" aria-hidden="true">
        <i v-for="(_, index) in stages" :key="index" :class="{ active: index === stageIndex }" />
      </div>
    </div>
    <div v-else class="preview__empty">
      <div class="preview__orb"><BulbOutlined /></div>
      <h2>等待生成网站</h2>
      <p>在左侧描述修改需求，完成后会在这里展示。</p>
    </div>
  </section>
</template>

<style scoped>
.preview {
  display: flex;
  min-width: 0;
  min-height: 0;
  background: #f1f4f8;
  flex-direction: column;
}
.preview__toolbar {
  display: flex;
  height: 46px;
  padding: 0 12px;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid #dfe5ee;
  background: #fff;
}
.traffic-lights {
  display: flex;
  gap: 5px;
}
.traffic-lights i {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ff746c;
}
.traffic-lights i:nth-child(2) {
  background: #ffc65a;
}
.traffic-lights i:nth-child(3) {
  background: #55c985;
}
.preview__address {
  overflow: hidden;
  padding: 6px 12px;
  border-radius: 7px;
  background: #f3f5f8;
  color: #8a95a5;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}
.preview iframe {
  width: 100%;
  min-height: 0;
  border: 0;
  background: #fff;
  flex: 1;
}
.preview__loading {
  position: relative;
  display: flex;
  overflow: hidden;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(circle at 50% 42%, #ffffff 0 11%, transparent 34%),
    linear-gradient(145deg, #f7fbff, #eef8f7 54%, #f3f5ff);
  color: #65748a;
  text-align: center;
  flex: 1;
  flex-direction: column;
}
.preview__loading::before {
  position: absolute;
  width: 460px;
  height: 460px;
  border: 1px solid #2f74d90e;
  border-radius: 50%;
  box-shadow:
    0 0 0 58px #1ea99708,
    0 0 0 116px #2f74d908;
  content: '';
}
.builder-orbit {
  position: relative;
  width: 122px;
  height: 122px;
  margin-bottom: 24px;
}
.builder-orbit__ring {
  position: absolute;
  inset: 0;
  border: 1px solid #7e9fc83d;
  border-radius: 50%;
  animation: orbit-spin 2.8s linear infinite;
}
.builder-orbit__ring i {
  position: absolute;
  top: -5px;
  left: 50%;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #2875df;
  box-shadow: 0 0 0 6px #2875df1f;
  transform: translateX(-50%);
}
.builder-orbit__ring--inner {
  inset: 17px;
  border-color: #21a9974a;
  animation-direction: reverse;
  animation-duration: 2s;
}
.builder-orbit__ring--inner i {
  width: 8px;
  height: 8px;
  background: #18a894;
  box-shadow: 0 0 0 5px #18a8941f;
}
.builder-orbit__core {
  position: absolute;
  inset: 38px;
  display: grid;
  place-items: center;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 12px 30px #236ba43d;
  animation: core-breathe 1.8s ease-in-out infinite;
}
.builder-orbit__core img {
  width: 100%;
  height: 100%;
  border-radius: 16px;
  object-fit: cover;
}
.preview__loading h2 {
  position: relative;
  margin: 0 0 8px;
  color: #26354c;
  font-size: 21px;
}
.preview__loading p {
  position: relative;
  margin: 0;
  color: #7b8798;
  font-size: 13px;
}
.stage-dots {
  position: relative;
  display: flex;
  margin-top: 20px;
  gap: 7px;
}
.stage-dots i {
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: #c3cede;
  transition:
    width 0.25s ease,
    background 0.25s ease;
}
.stage-dots i.active {
  width: 23px;
  background: linear-gradient(90deg, #18a894, #2875df);
}
@keyframes orbit-spin {
  to {
    transform: rotate(360deg);
  }
}
@keyframes core-breathe {
  50% {
    box-shadow: 0 16px 36px #236ba45c;
    transform: scale(1.06);
  }
}
.preview__empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #7c899b;
  text-align: center;
  flex: 1;
  flex-direction: column;
}
.preview__orb {
  display: grid;
  width: 70px;
  height: 70px;
  margin-bottom: 18px;
  place-items: center;
  border-radius: 24px;
  background: linear-gradient(145deg, #d9f7f1, #dce9ff);
  color: #168f83;
  font-size: 28px;
  box-shadow: 0 15px 35px #3d7eaa1f;
}
.preview__empty h2 {
  margin: 0 0 7px;
  color: #334159;
  font-size: 20px;
}
.preview__empty p {
  margin: 0;
}
@media (max-width: 900px) {
  .preview {
    height: 620px;
  }
}
@media (max-width: 560px) {
  .preview {
    height: 520px;
  }
}
@media (prefers-reduced-motion: reduce) {
  .builder-orbit__ring,
  .builder-orbit__core {
    animation: none;
  }
  .stage-dots i {
    transition: none;
  }
}
</style>
