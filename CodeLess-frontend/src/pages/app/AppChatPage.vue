<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { ArrowUpOutlined, CloudUploadOutlined, InfoCircleOutlined } from '@ant-design/icons-vue'
import { deleteApp, deleteAppByAdmin, deployApp, getAppVoById } from '@/api/appController'
import { useLoginUserStore } from '@/stores/loginUser'
import { renderMarkdown } from '@/utils/markdown'
import { API_BASE_URL, PREVIEW_BASE_URL } from '@/config/urls'
import logoUrl from '@/assets/logo.png'
import AppDetailsModal from '@/components/AppDetailsModal.vue'
import DeploymentSuccessModal from '@/components/DeploymentSuccessModal.vue'
import AppPreviewPanel from '@/components/AppPreviewPanel.vue'
import { isValidAppId, useAppId } from '@/utils/appId'
import 'highlight.js/styles/github.css'

type ChatMessage = { role: 'user' | 'assistant'; content: string; renderedContent?: string }
const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()
const app = ref<API.AppVO>()
const chatMessages = ref<ChatMessage[]>([])
const inputMessage = ref('')
const loading = ref(true)
const generating = ref(false)
const deploying = ref(false)
const deploySuccessOpen = ref(false)
const deploymentUrl = ref('')
const detailsOpen = ref(false)
const deleting = ref(false)
const previewVersion = ref(0)
const previewReady = ref(false)
const messageListRef = ref<HTMLElement>()
const isAtBottom = ref(true)
const generationStageIndex = ref(0)
const generationStages = [
  '正在理解你的修改需求',
  '正在编写页面结构',
  '正在完善样式与交互',
  '正在准备网站预览',
]
let abortController: AbortController | undefined
let generationStageTimer: number | undefined
let markdownRenderTimer: number | undefined
let pendingMarkdownMessage: ChatMessage | undefined

const appId = useAppId()
const isAdmin = computed(() => loginUserStore.loginUser.userRole === 'admin')
const canEdit = computed(() => isAdmin.value || loginUserStore.loginUser.id === app.value?.userId)
const canChat = computed(
  () => Boolean(loginUserStore.loginUser.id) && loginUserStore.loginUser.id === app.value?.userId,
)
const isViewMode = computed(() => route.query.view === '1')
const staticKey = computed(() =>
  app.value?.codeGenType && app.value.id ? `${app.value.codeGenType}_${app.value.id}` : '',
)
const previewUrl = computed(() =>
  staticKey.value ? `${PREVIEW_BASE_URL}/${staticKey.value}/?v=${previewVersion.value}` : '',
)
const generationStage = computed(() => generationStages[generationStageIndex.value])

const updateScrollPosition = () => {
  const list = messageListRef.value
  if (!list) return
  isAtBottom.value = list.scrollHeight - list.scrollTop - list.clientHeight < 72
}
const scrollToBottom = async (force = false) => {
  await nextTick()
  const list = messageListRef.value
  if (!list || (!force && !isAtBottom.value)) return
  list.scrollTo({ top: list.scrollHeight, behavior: 'smooth' })
  isAtBottom.value = true
}
const startGenerationStages = () => {
  generationStageIndex.value = 0
  window.clearInterval(generationStageTimer)
  generationStageTimer = window.setInterval(() => {
    generationStageIndex.value = (generationStageIndex.value + 1) % generationStages.length
  }, 2200)
}
const stopGenerationStages = () => {
  window.clearInterval(generationStageTimer)
  generationStageTimer = undefined
}
const flushMarkdownRender = () => {
  window.clearTimeout(markdownRenderTimer)
  markdownRenderTimer = undefined
  if (!pendingMarkdownMessage) return
  pendingMarkdownMessage.renderedContent = renderMarkdown(pendingMarkdownMessage.content)
  pendingMarkdownMessage = undefined
}
const scheduleMarkdownRender = (assistant: ChatMessage) => {
  pendingMarkdownMessage = assistant
  if (markdownRenderTimer !== undefined) return
  markdownRenderTimer = window.setTimeout(flushMarkdownRender, 80)
}
const decodeEventData = (raw: string) => {
  if (!raw) return ''
  try {
    const parsed: unknown = JSON.parse(raw)
    if (typeof parsed === 'string') return parsed
    if (parsed && typeof parsed === 'object') {
      const payload = parsed as Record<string, unknown>
      if (typeof payload.d === 'string') return payload.d
      if (typeof payload.message === 'string') return payload.message
    }
    return raw
  } catch {
    return raw
  }
}

const checkPreview = async () => {
  if (!staticKey.value) {
    previewReady.value = false
    return
  }
  try {
    const response = await fetch(`${PREVIEW_BASE_URL}/${staticKey.value}/?v=${Date.now()}`, {
      credentials: 'include',
      cache: 'no-store',
    })
    previewReady.value = response.ok
  } catch {
    previewReady.value = false
  }
}

const appendSseEvent = (event: string, assistant: ChatMessage) => {
  const lines = event.split(/\r?\n/)
  const eventName =
    lines
      .find((line) => line.startsWith('event:'))
      ?.slice(6)
      .trim() || 'message'
  const data = lines
    .filter((line) => line.startsWith('data:'))
    .map((line) => line.slice(5).trimStart())
    .join('\n')
  if (eventName === 'done') return
  if (eventName === 'error') throw new Error(decodeEventData(data) || '代码生成失败')
  if (data && data !== '[DONE]') {
    assistant.content += decodeEventData(data)
    scheduleMarkdownRender(assistant)
  }
}

const loadApp = async () => {
  if (!isValidAppId(appId.value)) {
    message.error('应用参数无效')
    await router.replace('/')
    return
  }
  loading.value = true
  try {
    const res = await getAppVoById({ id: appId.value })
    if (res.data.code === 0 && res.data.data) app.value = res.data.data
    else {
      message.error(`获取应用失败：${res.data.message || '应用不存在'}`)
      await router.replace('/')
    }
  } catch {
    message.error('获取应用失败，请检查后端服务')
    await router.replace('/')
  } finally {
    loading.value = false
  }
}

const sendMessage = async (preset?: string) => {
  if (!canChat.value) {
    message.warning('无法在别人的作品下对话哦~')
    return
  }
  const content = (preset ?? inputMessage.value).trim()
  if (!content || generating.value) return
  const hasExistingWebsite = previewReady.value
  const requestContent =
    hasExistingWebsite && app.value?.initPrompt
      ? `原始建站需求：${app.value.initPrompt}\n\n本次修改需求：${content}\n\n请保留未要求修改的功能，并输出修改后的完整网站代码。`
      : content
  chatMessages.value.push(
    { role: 'user', content },
    { role: 'assistant', content: '', renderedContent: '' },
  )
  inputMessage.value = ''
  generating.value = true
  previewReady.value = false
  startGenerationStages()
  await scrollToBottom(true)
  const assistant = chatMessages.value[chatMessages.value.length - 1]!
  abortController = new AbortController()
  try {
    const url = new URL(`${API_BASE_URL}/app/chat/gen/code`)
    url.searchParams.set('appId', appId.value)
    url.searchParams.set('message', requestContent)
    const response = await fetch(url, {
      credentials: 'include',
      headers: { Accept: 'text/event-stream' },
      signal: abortController.signal,
    })
    if (!response.ok || !response.body) throw new Error(`请求失败（${response.status}）`)
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    while (true) {
      const { value, done } = await reader.read()
      buffer += decoder.decode(value, { stream: !done })
      const events = buffer.split(/\r?\n\r?\n/)
      buffer = events.pop() ?? ''
      for (const event of events) appendSseEvent(event, assistant)
      await scrollToBottom()
      if (done) break
    }
    if (buffer.trim()) appendSseEvent(buffer, assistant)
    if (!assistant.content) assistant.content = '网站已生成完成，可以在右侧查看效果。'
    scheduleMarkdownRender(assistant)
    previewVersion.value = Date.now()
    await loadApp()
    await checkPreview()
    if (!previewReady.value) throw new Error('代码生成完成，但没有找到可预览的网站文件')
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') return
    assistant.content ||= '生成过程中连接中断，请重试。'
    scheduleMarkdownRender(assistant)
    message.error(error instanceof Error ? error.message : '生成失败，请稍后重试')
  } finally {
    flushMarkdownRender()
    generating.value = false
    stopGenerationStages()
    abortController = undefined
    await scrollToBottom()
  }
}

const deploy = async () => {
  deploying.value = true
  try {
    const res = await deployApp({ appId: appId.value })
    if (res.data.code === 0 && res.data.data) {
      deploymentUrl.value = res.data.data
      deploySuccessOpen.value = true
      message.success('部署成功')
      await loadApp()
    } else message.error(`部署失败：${res.data.message || '请稍后重试'}`)
  } catch {
    message.error('部署失败，请检查后端服务')
  } finally {
    deploying.value = false
  }
}

const openEditPage = async () => {
  if (!canEdit.value) {
    message.error('你没有权限修改该应用')
    return
  }
  detailsOpen.value = false
  await router.push(isAdmin.value ? `/admin/app/edit/${appId.value}` : `/app/edit/${appId.value}`)
}

const deleteCurrentApp = () => {
  if (!canEdit.value) {
    message.error('你没有权限删除该应用')
    return
  }
  Modal.confirm({
    centered: true,
    title: '删除应用',
    content: `确定删除“${app.value?.appName || '未命名应用'}”吗？删除后无法恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      deleting.value = true
      try {
        const res = isAdmin.value
          ? await deleteAppByAdmin({ id: appId.value })
          : await deleteApp({ id: appId.value })
        if (res.data.code !== 0 || !res.data.data) {
          throw new Error(res.data.message || '删除失败')
        }
        detailsOpen.value = false
        message.success('应用已删除')
        await router.replace('/')
      } catch (error) {
        message.error(error instanceof Error ? error.message : '删除失败，请稍后重试')
        throw error
      } finally {
        deleting.value = false
      }
    },
  })
}

onMounted(async () => {
  await loadApp()
  await checkPreview()
  if (!isViewMode.value && route.query.auto === '1' && app.value?.initPrompt) {
    const key = `codeless:auto-generated:${appId.value}`
    if (!sessionStorage.getItem(key)) {
      sessionStorage.setItem(key, '1')
      await sendMessage(app.value.initPrompt)
    }
    await router.replace({ path: route.path })
  }
})
onBeforeUnmount(() => {
  abortController?.abort()
  stopGenerationStages()
  window.clearTimeout(markdownRenderTimer)
})
</script>

<template>
  <a-spin :spinning="loading" tip="正在加载应用…">
    <main class="chat-page">
      <header class="chat-page__header">
        <button class="app-name" type="button" @click="router.push('/')">
          <img class="app-name__mark" :src="logoUrl" alt="" />
          <span>{{ app?.appName || '未命名应用' }}</span>
        </button>
        <div class="header-actions">
          <a-button @click="detailsOpen = true"
            ><template #icon><InfoCircleOutlined /></template>应用详情</a-button
          >
          <a-button type="primary" :loading="deploying" @click="deploy"
            ><template #icon><CloudUploadOutlined /></template>部署</a-button
          >
        </div>
      </header>

      <div class="workspace">
        <section class="conversation" aria-label="AI 对话">
          <div ref="messageListRef" class="message-list" @scroll.passive="updateScrollPosition">
            <div v-if="!chatMessages.length" class="conversation-empty">
              <img class="conversation-empty__logo" :src="logoUrl" alt="" />
              <h2>{{ canChat ? '继续完善你的应用' : '正在查看这个应用' }}</h2>
              <p>
                {{
                  canChat
                    ? '描述你想调整的内容，AI 会修改代码并更新右侧预览。'
                    : '你可以查看作品效果，但不能修改别人的作品。'
                }}
              </p>
            </div>
            <div
              v-for="(item, index) in chatMessages"
              :key="index"
              :class="['message-row', `message-row--${item.role}`]"
            >
              <img
                v-if="item.role === 'assistant'"
                class="message-avatar"
                :src="logoUrl"
                alt="CodeLess AI"
              />
              <div class="message-bubble">
                <div
                  v-if="item.content && item.role === 'assistant'"
                  class="markdown-body"
                  v-html="item.renderedContent"
                />
                <span v-else-if="item.content">{{ item.content }}</span>
                <span v-else class="typing"><i /><i /><i /></span>
              </div>
              <a-avatar
                v-if="item.role === 'user'"
                class="message-avatar message-avatar--user"
                :src="loginUserStore.loginUser.userAvatar"
              >
                {{ loginUserStore.loginUser.userName?.slice(0, 1) || '用' }}
              </a-avatar>
            </div>
          </div>
          <button
            v-if="!isAtBottom && chatMessages.length"
            class="jump-to-latest"
            type="button"
            @click="scrollToBottom(true)"
          >
            ↓ 回到最新
          </button>
          <a-tooltip :title="canChat ? undefined : '无法在别人的作品下对话哦~'" placement="top">
            <div :class="['composer', { 'composer--readonly': !canChat }]">
              <a-textarea
                v-model:value="inputMessage"
                :auto-size="{ minRows: 3, maxRows: 7 }"
                :maxlength="2000"
                :disabled="!canChat"
                :placeholder="
                  canChat ? '请描述你想生成的网站，越详细效果越好哦' : '仅作品所有者可以继续对话'
                "
                @press-enter.exact.prevent="sendMessage()"
              />
              <div class="composer__footer">
                <span>{{
                  !canChat
                    ? '无法在别人的作品下对话哦~'
                    : generating
                      ? 'AI 正在生成，请稍候…'
                      : 'Enter 发送，Shift + Enter 换行'
                }}</span
                ><a-button
                  type="primary"
                  shape="circle"
                  :disabled="!canChat || !inputMessage.trim() || generating"
                  :loading="generating"
                  aria-label="发送消息"
                  @click="sendMessage()"
                  ><template #icon><ArrowUpOutlined /></template
                ></a-button>
              </div>
            </div>
          </a-tooltip>
        </section>

        <AppPreviewPanel
          :url="previewUrl"
          :ready="previewReady"
          :generating="generating"
          :version="previewVersion"
          :app-name="app?.appName"
          :stage="generationStage"
          :stages="generationStages"
          :stage-index="generationStageIndex"
          @refresh="previewVersion = Date.now()"
        />
      </div>
    </main>

    <AppDetailsModal
      v-model:open="detailsOpen"
      :app="app"
      :can-edit="canEdit"
      :deleting="deleting"
      @edit="openEditPage"
      @delete="deleteCurrentApp"
    />

    <DeploymentSuccessModal v-model:open="deploySuccessOpen" :url="deploymentUrl" />
  </a-spin>
</template>

<style scoped>
.chat-page {
  height: calc(100dvh - 64px);
  min-height: 650px;
  overflow: hidden;
  border: 0;
  border-radius: 0;
  background: #fff;
  box-shadow: none;
}
.chat-page__header {
  display: flex;
  height: 56px;
  padding: 0 16px;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e8edf4;
}
.app-name {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 10px;
  border: 0;
  background: transparent;
  color: #1d2a40;
  cursor: pointer;
  font-weight: 700;
}
.app-name__mark {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  border: 1px solid #e7edf5;
  background: #fff;
  object-fit: cover;
}
.header-actions {
  display: flex;
  gap: 10px;
}
.workspace {
  display: grid;
  height: calc(100% - 56px);
  grid-template-columns: minmax(0, 2fr) minmax(0, 3fr);
}
.conversation {
  position: relative;
  display: flex;
  min-width: 0;
  min-height: 0;
  height: 100%;
  overflow: hidden;
  box-sizing: border-box;
  padding: 12px;
  border-right: 1px solid #e8edf4;
  background: #fbfcfe;
  flex-direction: column;
}
.message-list {
  min-height: 0;
  padding: 4px 4px 12px 0;
  overflow-x: hidden;
  overflow-y: scroll;
  overscroll-behavior: contain;
  scrollbar-color: #bdcbe0 transparent;
  scrollbar-gutter: stable;
  scrollbar-width: thin;
  flex: 1;
}
.message-list::-webkit-scrollbar {
  width: 7px;
}
.message-list::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: #bdcbe0;
}
.message-list::-webkit-scrollbar-track {
  background: transparent;
}
.conversation-empty {
  padding: 60px 28px;
  text-align: center;
  color: #778398;
}
.conversation-empty__logo {
  display: block;
  width: 45px;
  height: 45px;
  margin: 0 auto 14px;
  border-radius: 14px;
  border: 1px solid #e7edf5;
  background: #fff;
  object-fit: cover;
}
.conversation-empty h2 {
  margin: 0 0 7px;
  color: #25334a;
  font-size: 18px;
}
.conversation-empty p {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
}
.message-row {
  display: flex;
  margin: 0 0 18px;
  align-items: flex-start;
  gap: 9px;
}
.message-row--user {
  justify-content: flex-end;
}
.message-avatar {
  width: 30px;
  height: 30px;
  flex: none;
  border-radius: 9px;
  border: 1px solid #e7edf5;
  background: #fff;
  object-fit: cover;
}
.message-bubble {
  max-width: 82%;
  min-width: 0;
  padding: 10px 13px;
  border-radius: 4px 14px 14px;
  background: #fff;
  box-shadow: 0 4px 16px #233b6112;
  color: #334159;
  font-size: 13px;
  line-height: 1.72;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
}
.message-row--user .message-bubble {
  border-radius: 14px 4px 14px 14px;
  background: #2468df;
  color: #fff;
}
.message-row--assistant .message-bubble {
  width: calc(100% - 39px);
  max-width: calc(100% - 39px);
  padding: 13px 15px;
}
.markdown-body {
  min-width: 0;
  overflow-wrap: anywhere;
  white-space: normal;
}
.markdown-body :deep(> :first-child) {
  margin-top: 0;
}
.markdown-body :deep(> :last-child) {
  margin-bottom: 0;
}
.markdown-body :deep(p) {
  margin: 0 0 11px;
  color: #3b4960;
  line-height: 1.75;
}
.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4) {
  margin: 18px 0 9px;
  color: #1f2d43;
  font-weight: 750;
  letter-spacing: -0.015em;
  line-height: 1.35;
}
.markdown-body :deep(h1) {
  padding-bottom: 8px;
  border-bottom: 1px solid #e4eaf2;
  font-size: 19px;
}
.markdown-body :deep(h2) {
  font-size: 17px;
}
.markdown-body :deep(h3),
.markdown-body :deep(h4) {
  font-size: 14px;
}
.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  margin: 8px 0 13px;
  padding-left: 22px;
}
.markdown-body :deep(li) {
  margin: 4px 0;
  padding-left: 2px;
}
.markdown-body :deep(a) {
  color: #1769d2;
  text-decoration: underline;
  text-decoration-color: #1769d24d;
  text-underline-offset: 3px;
}
.markdown-body :deep(blockquote) {
  margin: 12px 0;
  padding: 9px 12px;
  border-left: 3px solid #27a997;
  border-radius: 0 8px 8px 0;
  background: #eef8f6;
  color: #536579;
}
.markdown-body :deep(blockquote p) {
  margin: 0;
  color: inherit;
}
.markdown-body :deep(:not(pre) > code) {
  padding: 2px 5px;
  border: 1px solid #dce5ef;
  border-radius: 5px;
  background: #f2f6fa;
  color: #b53766;
  font-family: 'Cascadia Code', 'JetBrains Mono', Consolas, monospace;
  font-size: 0.9em;
}
.markdown-body :deep(.md-code-block) {
  margin: 13px 0 16px;
  overflow: hidden;
  border: 1px solid #d9dee6;
  border-radius: 8px;
  background: #fbfcfe;
  box-shadow: 0 2px 8px #243a5a0a;
}
.markdown-body :deep(.md-code-header) {
  display: flex;
  height: 31px;
  padding: 0 12px;
  align-items: center;
  border-bottom: 1px solid #e4e8ee;
  background: #f4f6f8;
}
.markdown-body :deep(.md-code-header span) {
  color: #7a8698;
  font-family: 'Cascadia Code', 'JetBrains Mono', Consolas, monospace;
  font-size: 9.5px;
  font-weight: 700;
  letter-spacing: 0.12em;
}
.markdown-body :deep(.md-code-block pre) {
  margin: 0;
  overflow-x: auto;
  scrollbar-color: #c3cad5 #f8fafc;
  scrollbar-width: thin;
}
.markdown-body :deep(.md-code-block code.hljs) {
  display: block;
  min-width: max-content;
  padding: 14px 15px 17px;
  background: transparent;
  color: #24292f;
  font-family: 'Cascadia Code', 'JetBrains Mono', Consolas, monospace;
  font-size: 11.5px;
  line-height: 1.65;
  tab-size: 2;
  white-space: pre;
}
.markdown-body :deep(table) {
  display: block;
  width: 100%;
  margin: 12px 0;
  overflow-x: auto;
  border-spacing: 0;
  border-collapse: collapse;
}
.markdown-body :deep(th),
.markdown-body :deep(td) {
  padding: 7px 10px;
  border: 1px solid #dfe6ef;
  text-align: left;
  white-space: nowrap;
}
.markdown-body :deep(th) {
  background: #f3f7fa;
  color: #293a52;
}
.markdown-body :deep(hr) {
  height: 1px;
  margin: 18px 0;
  border: 0;
  background: #e1e8f0;
}
.typing {
  display: flex;
  padding: 6px 2px;
  gap: 4px;
}
.typing i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #88a0bd;
  animation: pulse 1s infinite;
}
.typing i:nth-child(2) {
  animation-delay: 0.15s;
}
.typing i:nth-child(3) {
  animation-delay: 0.3s;
}
@keyframes pulse {
  50% {
    opacity: 0.25;
    transform: translateY(-3px);
  }
}
.composer {
  flex: none;
  padding: 11px;
  border: 1px solid #dce4ee;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 8px 25px #24416812;
}
.composer--readonly {
  border-color: #e2e6ec;
  background: #f4f6f8;
  box-shadow: none;
  cursor: not-allowed;
}
.composer--readonly :deep(textarea),
.composer--readonly :deep(.ant-input-disabled) {
  background: transparent;
  cursor: not-allowed;
}
.jump-to-latest {
  align-self: center;
  flex: none;
  margin: -3px 0 8px;
  padding: 5px 12px;
  border: 1px solid #d8e2ef;
  border-radius: 999px;
  background: #fff;
  box-shadow: 0 5px 16px #24416814;
  color: #56708f;
  cursor: pointer;
  font-size: 11px;
}
.jump-to-latest:hover,
.jump-to-latest:focus-visible {
  border-color: #7ba4dd;
  outline: 0;
  color: #2468df;
}
.composer :deep(textarea) {
  padding: 2px;
  border: 0;
  box-shadow: none;
  resize: none;
}
.composer__footer {
  display: flex;
  margin-top: 8px;
  align-items: center;
  justify-content: space-between;
  color: #9aa5b5;
  font-size: 11px;
}
@media (max-width: 900px) {
  .chat-page {
    height: auto;
    min-height: 0;
    overflow: visible;
  }
  .workspace {
    height: auto;
    grid-template-columns: 1fr;
  }
  .conversation {
    height: 620px;
    border-right: 0;
    border-bottom: 1px solid #e8edf4;
  }
}
@media (max-width: 560px) {
  .chat-page__header {
    padding: 0 12px;
  }
  .header-actions :deep(.ant-btn) {
    padding-inline: 10px;
  }
  .conversation {
    height: 560px;
    padding: 12px;
  }
  .composer__footer > span {
    display: none;
  }
  .composer__footer {
    justify-content: flex-end;
  }
}
.message-avatar--user {
  background: #e8f0ff;
  color: #2468df;
}
@media (prefers-reduced-motion: reduce) {
  .typing i {
    animation: none;
  }
}
</style>
