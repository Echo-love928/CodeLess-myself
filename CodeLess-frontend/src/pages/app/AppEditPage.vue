<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ArrowLeftOutlined,
  CommentOutlined,
  LinkOutlined,
  ReloadOutlined,
  SaveOutlined,
} from '@ant-design/icons-vue'
import { getAppVoById, getAppVoByIdByAdmin, updateApp, updateAppByAdmin } from '@/api/appController'
import { useLoginUserStore } from '@/stores/loginUser'
import { getDeployUrl } from '@/config/urls'
import { getCodeGenTypeLabel } from '@/constants/codeGenType'
import { isValidAppId, useAppId } from '@/utils/appId'

type EditForm = {
  appName: string
  cover: string
  priority: number
}

const router = useRouter()
const loginUserStore = useLoginUserStore()
const loading = ref(true)
const saving = ref(false)
const coverPreviewFailed = ref(false)
const app = ref<API.AppVO>()
const form = reactive<EditForm>({ appName: '', cover: '', priority: 0 })

const appId = useAppId()
const isAdmin = computed(() => loginUserStore.loginUser.userRole === 'admin')
const hasPermission = computed(
  () => isAdmin.value || loginUserStore.loginUser.id === app.value?.userId,
)
const deployUrl = computed(() => {
  const deployKey = app.value?.deployKey?.trim()
  return deployKey ? getDeployUrl(deployKey) : ''
})

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

const resetForm = () => {
  form.appName = app.value?.appName ?? ''
  form.cover = app.value?.cover ?? ''
  form.priority = app.value?.priority ?? 0
  coverPreviewFailed.value = false
}

const loadApp = async () => {
  if (!isValidAppId(appId.value)) {
    message.error('应用参数无效')
    await router.replace('/')
    return
  }
  loading.value = true
  try {
    const res = isAdmin.value
      ? await getAppVoByIdByAdmin({ id: appId.value })
      : await getAppVoById({ id: appId.value })
    if (res.data.code !== 0 || !res.data.data) {
      throw new Error(res.data.message || '应用不存在')
    }
    app.value = res.data.data
    if (!hasPermission.value) {
      message.error('你只能修改自己的应用')
      await router.replace('/')
      return
    }
    resetForm()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '加载应用失败')
    await router.replace('/')
  } finally {
    loading.value = false
  }
}

const save = async () => {
  const appName = form.appName.trim()
  if (!appName) return void message.warning('请输入应用名称')
  if (!hasPermission.value) return void message.error('你没有权限修改该应用')

  saving.value = true
  try {
    const cover = form.cover.trim()
    const res = isAdmin.value
      ? await updateAppByAdmin({ id: appId.value, appName, cover, priority: form.priority })
      : await updateApp({ id: appId.value, appName, cover })
    if (res.data.code !== 0 || !res.data.data) {
      throw new Error(res.data.message || '保存失败')
    }
    message.success('应用信息已更新')
    await loadApp()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

const enterConversation = () =>
  router.push({ path: `/app/chat/${appId.value}`, query: { view: '1' } })

onMounted(loadApp)
</script>

<template>
  <a-spin :spinning="loading" tip="正在加载应用…">
    <main class="edit-page">
      <header class="page-header">
        <button class="back-button" type="button" @click="router.back()">
          <ArrowLeftOutlined /> 返回
        </button>
        <div>
          <h1>编辑应用信息</h1>
          <p>{{ isAdmin ? '管理员可编辑全部信息与精选优先级。' : '修改应用名称和展示封面。' }}</p>
        </div>
      </header>

      <a-form class="settings-card" layout="vertical" :model="form" @finish="save">
        <div class="card-heading">
          <div>
            <h2>基本信息</h2>
            <p>这些信息会展示在首页应用卡片上。</p>
          </div>
          <a-tag :color="app?.deployKey ? 'green' : 'default'">{{
            app?.deployKey ? '已部署' : '未部署'
          }}</a-tag>
        </div>
        <div class="form-content">
          <a-form-item
            label="应用名称"
            name="appName"
            :rules="[
              { required: true, message: '请输入应用名称' },
              { max: 50, message: '应用名称不能超过 50 个字符' },
            ]"
          >
            <a-input
              v-model:value="form.appName"
              size="large"
              :maxlength="50"
              show-count
              placeholder="输入应用名称"
            />
          </a-form-item>

          <a-form-item
            label="应用封面"
            name="cover"
            extra="支持图片链接，建议使用 4:3 或 16:9 比例"
          >
            <a-input
              v-model:value="form.cover"
              size="large"
              allow-clear
              placeholder="输入封面图片地址"
              @update:value="coverPreviewFailed = false"
              ><template #prefix><LinkOutlined /></template
            ></a-input>
            <div class="cover-preview">
              <img
                v-if="form.cover && !coverPreviewFailed"
                :src="form.cover"
                alt="应用封面预览"
                @error="coverPreviewFailed = true"
              />
              <div v-else class="cover-placeholder">
                <strong>{{ form.appName.slice(0, 1) || 'C' }}</strong
                ><span>{{ coverPreviewFailed ? '无法加载该图片' : '封面预览' }}</span>
              </div>
            </div>
          </a-form-item>

          <a-form-item
            v-if="isAdmin"
            label="优先级"
            name="priority"
            extra="仅管理员可修改；设置为 99 时展示在首页精选应用中"
          >
            <a-input-number v-model:value="form.priority" size="large" :min="0" :max="999" />
          </a-form-item>

          <div class="readonly-grid">
            <a-form-item label="初始提示词" extra="初始提示词不可修改"
              ><a-textarea :value="app?.initPrompt" :rows="4" disabled
            /></a-form-item>
            <div class="readonly-row">
              <a-form-item label="生成类型" extra="生成类型不可修改"
                ><a-input :value="getCodeGenTypeLabel(app?.codeGenType)" disabled
              /></a-form-item>
              <a-form-item label="部署密钥" extra="部署密钥不可修改"
                ><a-input :value="app?.deployKey || '尚未部署'" disabled
              /></a-form-item>
            </div>
          </div>

          <div class="form-actions">
            <a-button type="primary" size="large" html-type="submit" :loading="saving"
              ><template #icon><SaveOutlined /></template>保存修改</a-button
            >
            <a-button size="large" :disabled="saving" @click="resetForm"
              ><template #icon><ReloadOutlined /></template>重置</a-button
            >
            <a-button type="link" size="large" @click="enterConversation"
              ><template #icon><CommentOutlined /></template>进入对话</a-button
            >
          </div>
        </div>
      </a-form>

      <section class="info-card">
        <div class="card-heading">
          <div>
            <h2>应用信息</h2>
            <p>应用身份、所有者与时间记录。</p>
          </div>
        </div>
        <div class="info-table-scroll">
          <dl class="info-grid">
            <div>
              <dt>应用 ID</dt>
              <dd>{{ app?.id || '-' }}</dd>
            </div>
            <div class="creator-info">
              <dt>创建者</dt>
              <dd>
                <a-avatar :size="26" :src="app?.user?.userAvatar">{{
                  app?.user?.userName?.slice(0, 1) || '用'
                }}</a-avatar
                >{{ app?.user?.userName || '未知用户' }}
              </dd>
            </div>
            <div>
              <dt>创建时间</dt>
              <dd>{{ formatDateTime(app?.createTime) }}</dd>
            </div>
            <div>
              <dt>更新时间</dt>
              <dd>{{ formatDateTime(app?.updateTime) }}</dd>
            </div>
            <div>
              <dt>部署时间</dt>
              <dd>{{ formatDateTime(app?.deployedTime) }}</dd>
            </div>
            <div>
              <dt>访问作品</dt>
              <dd>
                <a v-if="deployUrl" :href="deployUrl" target="_blank" rel="noopener noreferrer"
                  >查看已部署作品</a
                ><span v-else>尚未部署</span>
              </dd>
            </div>
          </dl>
        </div>
      </section>
    </main>
  </a-spin>
</template>

<style scoped>
.edit-page {
  width: min(980px, 100%);
  margin: 8px auto 64px;
  color: #243149;
}
.page-header {
  display: flex;
  margin-bottom: 22px;
  align-items: flex-start;
  gap: 20px;
}
.page-header h1 {
  margin: 3px 0 4px;
  font-size: 28px;
  letter-spacing: -0.03em;
}
.page-header p {
  margin: 0;
  color: #7a8799;
  font-size: 13px;
}
.back-button {
  display: flex;
  margin-top: 6px;
  padding: 6px 0;
  align-items: center;
  gap: 7px;
  border: 0;
  background: transparent;
  color: #65738a;
  cursor: pointer;
}
.back-button:hover {
  color: #2468df;
}
.settings-card,
.info-card {
  overflow: hidden;
  border: 1px solid #e3e9f1;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 15px 42px #2441680e;
}
.info-card {
  margin-top: 20px;
}
.card-heading {
  display: flex;
  padding: 20px 24px;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  border-bottom: 1px solid #e9edf3;
  background: linear-gradient(100deg, #fbfdfd, #f7faff);
}
.card-heading h2 {
  margin: 0 0 4px;
  font-size: 17px;
}
.card-heading p {
  margin: 0;
  color: #8a95a5;
  font-size: 12px;
}
.form-content {
  padding: 26px 28px 30px;
}
.form-content :deep(.ant-form-item-label > label) {
  color: #35435a;
  font-weight: 650;
}
.form-content :deep(.ant-form-item-extra) {
  margin-top: 5px;
  color: #9aa4b3;
  font-size: 11px;
}
.form-content :deep(.ant-input-number) {
  width: 220px;
}
.cover-preview {
  height: 225px;
  margin-top: 10px;
  overflow: hidden;
  border: 1px solid #e1e7ef;
  border-radius: 12px;
  background: #f5f7fa;
}
.cover-preview img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #f7f9fc;
}
.cover-placeholder {
  display: flex;
  height: 100%;
  align-items: center;
  justify-content: center;
  background: linear-gradient(145deg, #edf7f5, #edf2fc);
  color: #7e8ca0;
  flex-direction: column;
  gap: 8px;
}
.cover-placeholder strong {
  display: grid;
  width: 58px;
  height: 58px;
  place-items: center;
  border-radius: 17px;
  background: #ffffffb8;
  color: #168f83;
  font-size: 26px;
  box-shadow: 0 10px 28px #24416812;
}
.cover-placeholder span {
  font-size: 12px;
}
.readonly-grid {
  padding: 18px 18px 2px;
  border: 1px solid #e8edf3;
  border-radius: 14px;
  background: #f8fafc;
}
.readonly-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.readonly-grid :deep(.ant-input-disabled),
.readonly-grid :deep(.ant-input-textarea-disabled textarea) {
  color: #67758a;
  background: #f0f3f7;
}
.form-actions {
  display: flex;
  margin-top: 26px;
  align-items: center;
  gap: 10px;
}
.info-grid a {
  color: #2468df;
}
.creator-info :deep(.ant-avatar) {
  flex: none;
  background: #e5f4f1;
  color: #168f83;
}
@media (max-width: 720px) {
  .edit-page {
    margin-top: 0;
  }
  .page-header {
    gap: 12px;
  }
  .page-header h1 {
    font-size: 24px;
  }
  .form-content {
    padding: 22px 18px;
  }
  .readonly-row {
    grid-template-columns: 1fr;
  }
  .cover-preview {
    height: 190px;
  }
  .form-actions {
    align-items: stretch;
    flex-direction: column;
  }
  .form-actions :deep(.ant-btn) {
    width: 100%;
  }
}
@media (max-width: 480px) {
  .page-header p {
    display: none;
  }
}
.info-table-scroll {
  padding: 24px;
  overflow-x: auto;
  scrollbar-color: #c8d2df transparent;
  scrollbar-width: thin;
}
.info-grid {
  display: grid;
  min-width: 700px;
  margin: 0;
  padding: 0;
  overflow: hidden;
  border: 1px solid #e2e7ee;
  border-radius: 11px;
  grid-template-columns: 1fr 1fr;
}
.info-grid > div {
  display: grid;
  min-height: 64px;
  padding: 0;
  align-items: stretch;
  border-right: 1px solid #e2e7ee;
  border-bottom: 1px solid #e2e7ee;
  grid-template-columns: 126px minmax(0, 1fr);
}
.info-grid > div:nth-child(2n) {
  border-right: 0;
}
.info-grid > div:nth-last-child(-n + 2) {
  border-bottom: 0;
}
.info-grid dt,
.info-grid dd {
  display: flex;
  margin: 0;
  padding: 12px 20px;
  align-items: center;
}
.info-grid dt {
  border-right: 1px solid #e2e7ee;
  background: #f7f9fb;
  color: #465268;
  font-size: 12px;
  font-weight: 700;
}
.info-grid dd {
  min-width: 0;
  background: #fff;
  color: #2e3c53;
  font-size: 13px;
  overflow-wrap: anywhere;
}
.creator-info dd {
  display: flex;
  align-items: center;
  gap: 8px;
}
@media (max-width: 720px) {
  .info-table-scroll {
    padding: 18px;
  }
  .info-grid {
    grid-template-columns: 1fr 1fr;
  }
  .info-grid > div {
    border-right: 1px solid #e2e7ee;
    grid-template-columns: 126px minmax(0, 1fr);
  }
  .info-grid > div:nth-child(2n) {
    border-right: 0;
  }
}
@media (max-width: 480px) {
  .info-table-scroll {
    padding: 14px;
  }
  .info-grid {
    padding: 0;
  }
  .info-grid > div {
    padding: 0;
    grid-template-columns: 126px minmax(0, 1fr);
  }
}
</style>
