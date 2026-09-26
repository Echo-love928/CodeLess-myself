<script setup lang="ts">
import { computed } from 'vue'
import { CalendarOutlined, DeleteOutlined, EditOutlined } from '@ant-design/icons-vue'
import logoUrl from '@/assets/logo.png'

const props = defineProps<{
  open: boolean
  app?: API.AppVO
  canEdit: boolean
  deleting: boolean
}>()

const emit = defineEmits<{
  'update:open': [open: boolean]
  edit: []
  delete: []
}>()

const modalOpen = computed({
  get: () => props.open,
  set: (open: boolean) => emit('update:open', open),
})

const formatDateTime = (value?: string) => {
  if (!value) return '暂无记录'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}
</script>

<template>
  <a-modal v-model:open="modalOpen" width="520px" :footer="null" centered destroy-on-close>
    <section class="app-details" aria-labelledby="app-details-title">
      <div class="app-details__heading">
        <img class="app-details__mark" :src="logoUrl" alt="" />
        <h2 id="app-details-title">{{ app?.appName || '未命名应用' }}</h2>
        <a-tag :color="app?.deployKey ? 'green' : 'default'">
          {{ app?.deployKey ? '已部署' : '未部署' }}
        </a-tag>
      </div>

      <div class="app-details__section-label">应用基础信息</div>
      <div class="detail-item detail-item--creator">
        <a-avatar :size="44" :src="app?.user?.userAvatar">
          {{ app?.user?.userName?.slice(0, 1) || '用' }}
        </a-avatar>
        <div>
          <span>创建者</span><strong>{{ app?.user?.userName || '未知用户' }}</strong>
        </div>
      </div>
      <div class="detail-item">
        <span class="detail-item__icon"><CalendarOutlined /></span>
        <div>
          <span>创建时间</span><strong>{{ formatDateTime(app?.createTime) }}</strong>
        </div>
      </div>

      <template v-if="canEdit">
        <a-divider />
        <div class="app-details__section-label">操作栏</div>
        <div class="app-details__actions">
          <a-button size="large" @click="emit('edit')">
            <template #icon><EditOutlined /></template>修改
          </a-button>
          <a-button size="large" danger :loading="deleting" @click="emit('delete')">
            <template #icon><DeleteOutlined /></template>删除
          </a-button>
        </div>
      </template>
    </section>
  </a-modal>
</template>

<style scoped>
.app-details {
  padding: 10px 4px 2px;
  color: #25334a;
}
.app-details__heading {
  display: grid;
  padding: 8px 0 24px;
  align-items: center;
  gap: 12px;
  grid-template-columns: 48px minmax(0, 1fr) auto;
}
.app-details__mark {
  width: 48px;
  height: 48px;
  border-radius: 15px;
  border: 1px solid #e7edf5;
  background: #fff;
  box-shadow: 0 10px 24px #236ba41a;
  object-fit: cover;
}
.app-details__heading h2 {
  margin: 0;
  overflow: hidden;
  color: #1f2d43;
  font-size: 21px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.app-details__section-label {
  margin-bottom: 10px;
  color: #8b96a7;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
}
.detail-item {
  display: flex;
  min-height: 68px;
  padding: 12px 14px;
  align-items: center;
  gap: 12px;
  border: 1px solid #e6ebf2;
  border-radius: 13px;
  background: #f9fbfd;
}
.detail-item + .detail-item {
  margin-top: 10px;
}
.detail-item--creator :deep(.ant-avatar) {
  flex: none;
  background: linear-gradient(145deg, #dff5f1, #e1ebff);
  color: #168f83;
  font-weight: 800;
}
.detail-item__icon {
  display: grid;
  width: 44px;
  height: 44px;
  flex: none;
  place-items: center;
  border-radius: 50%;
  background: #eaf1fc;
  color: #286fe0;
  font-size: 18px;
}
.detail-item div {
  min-width: 0;
}
.detail-item div span,
.detail-item div strong {
  display: block;
}
.detail-item div span {
  margin-bottom: 3px;
  color: #8a95a6;
  font-size: 11px;
}
.detail-item div strong {
  overflow: hidden;
  color: #2b394f;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.app-details :deep(.ant-divider) {
  margin: 22px 0 18px;
}
.app-details__actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
</style>
