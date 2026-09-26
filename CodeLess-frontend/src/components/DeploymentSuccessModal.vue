<script setup lang="ts">
import { computed } from 'vue'
import { message } from 'ant-design-vue'
import { CheckCircleFilled, CopyOutlined } from '@ant-design/icons-vue'

const props = defineProps<{ open: boolean; url: string }>()
const emit = defineEmits<{ 'update:open': [open: boolean] }>()

const modalOpen = computed({
  get: () => props.open,
  set: (open: boolean) => emit('update:open', open),
})

const copyUrl = async () => {
  if (!props.url) return
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(props.url)
    } else {
      const textarea = document.createElement('textarea')
      textarea.value = props.url
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
    }
    message.success('部署链接已复制')
  } catch {
    message.error('复制失败，请手动复制链接')
  }
}

const visitWebsite = () => {
  if (props.url) window.open(props.url, '_blank', 'noopener,noreferrer')
}
</script>

<template>
  <a-modal
    v-model:open="modalOpen"
    title="部署成功"
    width="620px"
    :footer="null"
    centered
    destroy-on-close
  >
    <section class="deploy-success" aria-labelledby="deploy-success-title">
      <CheckCircleFilled class="deploy-success__icon" />
      <h2 id="deploy-success-title">网站部署成功！</h2>
      <p>你的网站已经成功部署，可以通过以下链接访问：</p>
      <div class="deployment-url">
        <span>{{ url }}</span>
        <button type="button" aria-label="复制部署链接" title="复制链接" @click="copyUrl">
          <CopyOutlined />
        </button>
      </div>
      <div class="deploy-success__actions">
        <a-button type="primary" size="large" @click="visitWebsite">访问网站</a-button>
        <a-button size="large" @click="modalOpen = false">关闭</a-button>
      </div>
    </section>
  </a-modal>
</template>

<style scoped>
.deploy-success {
  padding: 25px 28px 14px;
  text-align: center;
}
.deploy-success__icon {
  color: #52c41a;
  font-size: 62px;
  filter: drop-shadow(0 9px 18px #52c41a26);
}
.deploy-success h2 {
  margin: 19px 0 10px;
  color: #202c3f;
  font-size: 24px;
  letter-spacing: -0.02em;
}
.deploy-success > p {
  margin: 0 0 23px;
  color: #768397;
  font-size: 14px;
}
.deployment-url {
  display: flex;
  min-height: 50px;
  padding-left: 16px;
  align-items: center;
  overflow: hidden;
  border: 1px solid #dce3ec;
  border-radius: 9px;
  background: #fbfcfe;
  box-shadow: inset 0 1px 2px #223d6508;
  text-align: left;
}
.deployment-url span {
  min-width: 0;
  overflow: hidden;
  color: #344258;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}
.deployment-url button {
  display: grid;
  width: 50px;
  height: 48px;
  flex: none;
  place-items: center;
  border: 0;
  border-left: 1px solid #e2e7ee;
  background: transparent;
  color: #53647b;
  cursor: pointer;
  font-size: 17px;
}
.deployment-url button:hover,
.deployment-url button:focus-visible {
  outline: 0;
  background: #eef5ff;
  color: #2468df;
}
.deploy-success__actions {
  display: flex;
  margin-top: 28px;
  justify-content: center;
  gap: 12px;
}
</style>
