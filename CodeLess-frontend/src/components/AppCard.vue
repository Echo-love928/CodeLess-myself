<script setup lang="ts">
import { ClockCircleOutlined, EyeOutlined, MessageOutlined } from '@ant-design/icons-vue'

defineProps<{ app: API.AppVO; featured?: boolean }>()

const emit = defineEmits<{
  'view-conversation': [app: API.AppVO]
  'view-work': [app: API.AppVO]
}>()

const formatDate = (value?: string) =>
  value
    ? new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: 'short', day: 'numeric' }).format(
        new Date(value),
      )
    : '刚刚创建'
</script>

<template>
  <article class="app-card">
    <div class="app-card__cover">
      <img v-if="app.cover" :src="app.cover" :alt="`${app.appName || '应用'}封面`" />
      <div v-else :class="['app-card__placeholder', { featured }]">
        <span>{{ app.appName?.slice(0, 1) || (featured ? '精' : 'C') }}</span>
      </div>
      <b v-if="featured" class="featured-tag">精选</b>
      <div class="app-card__actions" aria-label="应用操作">
        <button
          v-if="app.deployKey?.trim()"
          class="card-action card-action--work"
          type="button"
          @click="emit('view-work', app)"
        >
          <EyeOutlined />查看作品
        </button>
        <button
          class="card-action card-action--chat"
          type="button"
          @click="emit('view-conversation', app)"
        >
          <MessageOutlined />查看对话
        </button>
      </div>
    </div>
    <div class="app-card__body">
      <div class="app-card__identity">
        <a-avatar
          class="app-card__avatar"
          :size="42"
          :src="app.user?.userAvatar || undefined"
          :aria-label="`${app.user?.userName || '未知用户'}的头像`"
        >
          {{ app.user?.userName?.trim().slice(0, 1) || '用' }}
        </a-avatar>
        <div class="app-card__identity-text">
          <h3 :title="app.appName || '未命名应用'">{{ app.appName || '未命名应用' }}</h3>
          <p :title="app.user?.userName || '未知用户'">
            {{ app.user?.userName || '未知用户' }}
          </p>
        </div>
      </div>
      <time class="app-card__created" :datetime="app.createTime">
        <ClockCircleOutlined /> {{ formatDate(app.createTime) }}
      </time>
    </div>
  </article>
</template>

<style scoped>
.app-card {
  overflow: hidden;
  border: 1px solid #e5eaf2;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 8px 22px rgb(42 61 113 / 9%);
  transition:
    box-shadow 0.18s,
    transform 0.18s;
}
.app-card:hover,
.app-card:focus-within {
  outline: 0;
  box-shadow: 0 18px 38px rgb(54 73 128 / 20%);
  transform: translateY(-4px);
}
.app-card__cover {
  position: relative;
  height: 196px;
  overflow: hidden;
  background: #eef3f9;
}
.app-card__cover::after {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, #10213b24, #10213bbd);
  content: '';
  opacity: 0;
  transition: opacity 0.2s;
}
.app-card__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: top;
  transition: transform 0.35s;
}
.app-card:hover .app-card__cover img,
.app-card:focus-within .app-card__cover img {
  transform: scale(1.025);
}
.featured-tag {
  position: absolute;
  z-index: 3;
  top: 12px;
  right: 12px;
  padding: 5px 9px;
  border-radius: 8px;
  background: #0e8f85e0;
  color: #fff;
  font-size: 11px;
  transition: opacity 0.2s;
}
.app-card__actions {
  position: absolute;
  z-index: 4;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 10px;
  opacity: 0;
  pointer-events: none;
  transform: translateY(8px);
  transition:
    opacity 0.2s,
    transform 0.2s;
}
.app-card:hover .app-card__cover::after,
.app-card:focus-within .app-card__cover::after {
  opacity: 1;
}
.app-card:hover .app-card__actions,
.app-card:focus-within .app-card__actions {
  opacity: 1;
  pointer-events: auto;
  transform: translateY(0);
}
.app-card:hover .featured-tag,
.app-card:focus-within .featured-tag {
  opacity: 0;
}
.card-action {
  display: inline-flex;
  width: 132px;
  min-height: 38px;
  align-items: center;
  justify-content: center;
  gap: 7px;
  border: 1px solid transparent;
  border-radius: 999px;
  box-shadow: 0 8px 20px #0713262e;
  cursor: pointer;
  font-size: 14px;
  font-weight: 700;
  transition:
    transform 0.15s,
    box-shadow 0.15s,
    background 0.15s;
}
.card-action:hover {
  box-shadow: 0 10px 24px #07132647;
  transform: translateY(-1px);
}
.card-action:focus-visible {
  outline: 3px solid #8bc6ff;
  outline-offset: 2px;
}
.card-action--work {
  border-color: #ffffff5c;
  background: #17243be8;
  color: #fff;
}
.card-action--work:hover {
  background: #0f192a;
}
.card-action--chat {
  border-color: #dfe6ef;
  background: #fffffff2;
  color: #17243b;
}
.card-action--chat:hover {
  background: #fff;
}
.app-card__placeholder {
  display: flex;
  height: 100%;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #dceeff, #d9faf3);
  color: #1f5fae;
  flex-direction: column;
  gap: 8px;
}
.app-card__placeholder.featured {
  background: linear-gradient(135deg, #e7f7f4, #e8edff);
  color: #118b80;
}
.app-card__placeholder span {
  display: grid;
  width: 62px;
  height: 62px;
  place-items: center;
  border-radius: 18px;
  background: #ffffffb3;
  box-shadow: 0 10px 30px #1c59961f;
  font-size: 28px;
  font-weight: 800;
}
.app-card__body {
  display: flex;
  min-height: 116px;
  padding: 16px 18px 18px;
  flex-direction: column;
}
.app-card__identity {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 12px;
}
.app-card__avatar {
  flex: none;
  background: linear-gradient(145deg, #5275d9, #26a69a);
  color: #fff;
  font-weight: 700;
}
.app-card__identity-text {
  min-width: 0;
  flex: 1;
}
.app-card__body h3 {
  margin: 0 0 3px;
  overflow: hidden;
  font-size: 17px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.app-card__identity-text p {
  margin: 0;
  overflow: hidden;
  color: #68758d;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.app-card__created {
  display: inline-flex;
  margin-top: auto;
  padding-top: 15px;
  align-items: center;
  gap: 6px;
  color: #97a1b2;
  font-size: 12px;
}
@media (hover: none) {
  .app-card__cover::after {
    opacity: 0.64;
    background: linear-gradient(180deg, transparent 35%, #10213bc7);
  }
  .app-card__actions {
    inset: auto 12px 12px;
    align-items: stretch;
    opacity: 1;
    pointer-events: auto;
    transform: none;
  }
  .card-action {
    width: 100%;
  }
  .featured-tag {
    opacity: 1;
  }
  .app-card__actions:has(.card-action--work) {
    display: grid;
    grid-template-columns: 1fr 1fr;
    flex-direction: row;
  }
  .app-card__actions:has(.card-action--work) .card-action {
    font-size: 13px;
  }
}
@media (prefers-reduced-motion: reduce) {
  .app-card,
  .app-card__cover img,
  .app-card__cover::after,
  .app-card__actions,
  .card-action {
    transition: none;
  }
}
</style>
