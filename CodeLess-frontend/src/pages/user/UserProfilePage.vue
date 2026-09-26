<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, Rule } from 'ant-design-vue/es/form'
import { message } from 'ant-design-vue'
import {
  CheckCircleFilled,
  LockOutlined,
  SafetyCertificateOutlined,
  UploadOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'
import { changeMyPassword, updateMyUser } from '@/api/userController'
import { uploadMyAvatar } from '@/api/userAvatar'
import { useLoginUserStore } from '@/stores/loginUser'

type AccountSection = 'profile' | 'security'

const router = useRouter()
const loginUserStore = useLoginUserStore()
const activeSection = ref<AccountSection>('profile')
const profileFormRef = ref<FormInstance>()
const passwordFormRef = ref<FormInstance>()
const savingProfile = ref(false)
const uploadingAvatar = ref(false)
const changingPassword = ref(false)
const avatarAvailable = ref(true)
const avatarFileInput = ref<HTMLInputElement>()

const profileForm = reactive<API.UserUpdateMyRequest>({
  userName: '',
  userAvatar: '',
  userProfile: '',
})

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const profileRules: Record<string, Rule[]> = {
  userName: [
    { required: true, whitespace: true, message: '请输入昵称' },
    { max: 50, message: '昵称最多 50 个字符' },
  ],
  userAvatar: [
    { max: 1024, message: '头像地址最多 1024 个字符' },
    { type: 'url', message: '请输入以 http:// 或 https:// 开头的图片地址' },
  ],
  userProfile: [{ max: 500, message: '个人简介最多 500 个字符' }],
}

const validateConfirmPassword = async (_rule: Rule, value: string) => {
  if (!value) {
    return Promise.reject('请再次输入新密码')
  }
  if (value !== passwordForm.newPassword) {
    return Promise.reject('两次输入的新密码不一致')
  }
  return Promise.resolve()
}

const passwordRules: Record<string, Rule[]> = {
  currentPassword: [
    { required: true, message: '请输入当前密码' },
    { min: 8, max: 64, message: '密码长度应为 8～64 位' },
  ],
  newPassword: [
    { required: true, message: '请输入新密码' },
    { min: 8, max: 64, message: '密码长度应为 8～64 位' },
  ],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: ['change', 'blur'] }],
}

const displayName = computed(() => profileForm.userName?.trim() || '无名')
const avatarFallback = computed(() => displayName.value.slice(0, 1))
const roleLabel = computed(() =>
  loginUserStore.loginUser.userRole === 'admin' ? '管理员' : '普通用户',
)

const passwordStrength = computed(() => {
  const password = passwordForm.newPassword
  if (!password) return { level: 0, label: '尚未输入', className: 'empty' }
  let score = 0
  if (password.length >= 8) score += 1
  if (password.length >= 12) score += 1
  if (/[a-z]/.test(password) && /[A-Z]/.test(password)) score += 1
  if (/\d/.test(password)) score += 1
  if (/[^a-zA-Z0-9]/.test(password)) score += 1
  if (score <= 2) return { level: 1, label: '较弱', className: 'weak' }
  if (score <= 4) return { level: 2, label: '中等', className: 'medium' }
  return { level: 3, label: '较强', className: 'strong' }
})

const syncProfileForm = () => {
  profileForm.userName = loginUserStore.loginUser.userName || ''
  profileForm.userAvatar = loginUserStore.loginUser.userAvatar || ''
  profileForm.userProfile = loginUserStore.loginUser.userProfile || ''
  avatarAvailable.value = true
}

watch(
  () => loginUserStore.loginUser,
  () => syncProfileForm(),
  { immediate: true, deep: true },
)

watch(
  () => profileForm.userAvatar,
  () => {
    avatarAvailable.value = true
  },
)

const getErrorMessage = (error: unknown, fallback: string) => {
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const response = (error as { response?: { data?: { message?: string } } }).response
    return response?.data?.message || fallback
  }
  return fallback
}

const saveProfile = async () => {
  try {
    await profileFormRef.value?.validate()
    savingProfile.value = true
    const response = await updateMyUser({
      userName: profileForm.userName?.trim(),
      userAvatar: profileForm.userAvatar?.trim(),
      userProfile: profileForm.userProfile?.trim(),
    })
    if (response.data.code !== 0) {
      message.error(response.data.message || '个人资料保存失败')
      return
    }
    await loginUserStore.fetchLoginUser()
    message.success('个人资料已更新')
  } catch (error) {
    if (error && typeof error === 'object' && 'errorFields' in error) return
    message.error(getErrorMessage(error, '个人资料保存失败，请稍后重试'))
  } finally {
    savingProfile.value = false
  }
}

const resetProfile = () => {
  syncProfileForm()
  profileFormRef.value?.clearValidate()
}

const onAvatarFileChange = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  const supportedMimeType = ['image/png', 'image/jpeg', 'image/jpg', 'image/webp'].includes(file.type)
  const supportedFileName = /\.(png|jpe?g|webp)$/i.test(file.name)
  if (!supportedMimeType && !(file.type === '' && supportedFileName)) {
    message.warning('请选择 PNG、JPG/JPEG 或 WebP 图片')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    message.warning('头像图片不能超过 5 MB')
    return
  }

  uploadingAvatar.value = true
  try {
    const response = await uploadMyAvatar(file)
    const avatarUrl = response.data.data
    if (response.data.code !== 0 || !avatarUrl) {
      throw new Error(response.data.message || '头像上传失败')
    }
    // 上传接口已保存头像；刷新全站头像，同时保留尚未提交的昵称与简介。
    const draftName = profileForm.userName
    const draftProfile = profileForm.userProfile
    loginUserStore.setLoginUser({ ...loginUserStore.loginUser, userAvatar: avatarUrl })
    await nextTick()
    profileForm.userName = draftName
    profileForm.userProfile = draftProfile
    profileForm.userAvatar = avatarUrl
    message.success('头像已上传并保存')
  } catch (error) {
    message.error(getErrorMessage(error, error instanceof Error ? error.message : '头像上传失败'))
  } finally {
    uploadingAvatar.value = false
  }
}

const changePassword = async () => {
  try {
    await passwordFormRef.value?.validate()
    changingPassword.value = true
    const response = await changeMyPassword({
      currentPassword: passwordForm.currentPassword,
      newPassword: passwordForm.newPassword,
    })
    if (response.data.code !== 0) {
      message.error(response.data.message || '密码修改失败')
      return
    }
    loginUserStore.resetLoginUser()
    message.success('密码已修改，请使用新密码重新登录')
    await router.replace('/user/login')
  } catch (error) {
    if (error && typeof error === 'object' && 'errorFields' in error) return
    message.error(getErrorMessage(error, '密码修改失败，请检查当前密码'))
  } finally {
    changingPassword.value = false
  }
}
</script>

<template>
  <main class="account-page">
    <header class="account-page__heading">
      <div>
        <h1>个人中心</h1>
        <p>管理你的公开资料与账户安全设置。</p>
      </div>
      <div class="account-page__status">
        <CheckCircleFilled />
        已登录
      </div>
    </header>

    <div class="account-shell">
      <aside class="identity-panel">
        <div class="identity-panel__glow" aria-hidden="true"></div>
        <a-avatar
          v-if="profileForm.userAvatar && avatarAvailable"
          class="identity-panel__avatar"
          :size="84"
          :src="profileForm.userAvatar"
          @error="avatarAvailable = false"
        />
        <a-avatar v-else class="identity-panel__avatar" :size="84">
          {{ avatarFallback }}
        </a-avatar>
        <h2>{{ displayName }}</h2>
        <p class="identity-panel__account">@{{ loginUserStore.loginUser.userAccount }}</p>
        <span class="identity-panel__role">{{ roleLabel }}</span>
        <p class="identity-panel__bio">
          {{ profileForm.userProfile || '写下一段个人简介，让你的作品更有归属感。' }}
        </p>

        <nav class="account-nav" aria-label="账户设置">
          <button
            type="button"
            :class="{ 'account-nav__item--active': activeSection === 'profile' }"
            class="account-nav__item"
            @click="activeSection = 'profile'"
          >
            <UserOutlined />
            <span><strong>个人资料</strong><small>头像、昵称与简介</small></span>
          </button>
          <button
            type="button"
            :class="{ 'account-nav__item--active': activeSection === 'security' }"
            class="account-nav__item"
            @click="activeSection = 'security'"
          >
            <SafetyCertificateOutlined />
            <span><strong>账户安全</strong><small>更新登录密码</small></span>
          </button>
        </nav>
      </aside>

      <section class="settings-panel">
        <div v-if="activeSection === 'profile'" class="settings-section">
          <div class="settings-section__heading">
            <div>
              <span class="settings-section__icon"><UserOutlined /></span>
              <div>
                <h2>个人资料</h2>
                <p>这些信息将用于顶部账户区域和作品创建者信息。</p>
              </div>
            </div>
          </div>

          <a-form
            ref="profileFormRef"
            :model="profileForm"
            :rules="profileRules"
            layout="vertical"
            class="account-form"
          >
            <div class="account-form__grid">
              <a-form-item label="登录账号">
                <a-input :value="loginUserStore.loginUser.userAccount" disabled />
                <span class="account-form__hint">登录账号暂不支持修改</span>
              </a-form-item>
              <a-form-item label="账户角色">
                <a-input :value="roleLabel" disabled />
                <span class="account-form__hint">账户角色由系统管理员维护</span>
              </a-form-item>
            </div>

            <a-form-item label="昵称" name="userName">
              <a-input
                v-model:value="profileForm.userName"
                :maxlength="50"
                show-count
                placeholder="请输入昵称"
              />
            </a-form-item>

            <a-form-item label="头像地址" name="userAvatar">
              <div class="avatar-input-row">
                <a-input
                  v-model:value="profileForm.userAvatar"
                  :maxlength="1024"
                  placeholder="https://example.com/avatar.png"
                />
                <a-button :loading="uploadingAvatar" @click="avatarFileInput?.click()">
                  <template #icon><UploadOutlined /></template>
                  上传图片
                </a-button>
              </div>
              <input
                ref="avatarFileInput"
                class="avatar-file-input"
                type="file"
                accept="image/png,image/jpeg,image/jpg,image/webp,.png,.jpg,.jpeg,.webp"
                aria-label="选择头像图片"
                @change="onAvatarFileChange"
              />
              <span class="account-form__hint">可填写 HTTP/HTTPS 图片地址，或上传不超过 5 MB 的 PNG、JPG/JPEG、WebP 图片。上传后立即保存头像。</span>
            </a-form-item>

            <a-form-item label="个人简介" name="userProfile">
              <a-textarea
                v-model:value="profileForm.userProfile"
                :auto-size="{ minRows: 4, maxRows: 8 }"
                :maxlength="500"
                show-count
                placeholder="介绍一下你自己，以及你喜欢创建怎样的应用……"
              />
            </a-form-item>

            <div class="account-form__actions">
              <a-button :disabled="uploadingAvatar" @click="resetProfile">重置</a-button>
              <a-button type="primary" :loading="savingProfile" :disabled="uploadingAvatar" @click="saveProfile">
                保存修改
              </a-button>
            </div>
          </a-form>
        </div>

        <div v-else class="settings-section">
          <div class="settings-section__heading">
            <div>
              <span class="settings-section__icon settings-section__icon--security">
                <LockOutlined />
              </span>
              <div>
                <h2>账户安全</h2>
                <p>修改成功后，系统会退出当前登录以保护账户安全。</p>
              </div>
            </div>
          </div>

          <a-alert
            class="security-notice"
            type="info"
            show-icon
            message="使用未在其他网站使用过的独立密码，可以降低账户风险。"
          />

          <a-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            layout="vertical"
            class="account-form account-form--security"
          >
            <a-form-item label="当前密码" name="currentPassword">
              <a-input-password
                v-model:value="passwordForm.currentPassword"
                autocomplete="current-password"
                placeholder="请输入当前密码"
              />
            </a-form-item>

            <a-form-item label="新密码" name="newPassword">
              <a-input-password
                v-model:value="passwordForm.newPassword"
                autocomplete="new-password"
                placeholder="至少 8 位，建议组合大小写字母、数字和符号"
              />
              <div class="password-strength" :data-level="passwordStrength.level">
                <span v-for="index in 3" :key="index"></span>
                <small :class="`password-strength__label--${passwordStrength.className}`">
                  密码强度：{{ passwordStrength.label }}
                </small>
              </div>
            </a-form-item>

            <a-form-item label="确认新密码" name="confirmPassword">
              <a-input-password
                v-model:value="passwordForm.confirmPassword"
                autocomplete="new-password"
                placeholder="请再次输入新密码"
              />
            </a-form-item>

            <div class="account-form__actions">
              <a-button type="primary" :loading="changingPassword" @click="changePassword">
                更新密码
              </a-button>
            </div>
          </a-form>
        </div>
      </section>
    </div>
  </main>
</template>

<style scoped>
.account-page {
  --account-blue: #1677ff;
  --account-ink: #20304a;
  --account-muted: #71809a;
  max-width: 1160px;
  margin: 0 auto;
}

.account-page__heading {
  display: flex;
  margin-bottom: 24px;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
}

.account-page__heading h1 {
  margin: 0;
  color: var(--account-ink);
  font-size: clamp(28px, 4vw, 38px);
  letter-spacing: -0.04em;
}

.account-page__heading p {
  margin: 8px 0 0;
  color: var(--account-muted);
}

.account-page__status {
  display: inline-flex;
  padding: 8px 12px;
  border: 1px solid #d8eee8;
  border-radius: 999px;
  align-items: center;
  gap: 7px;
  background: #f2fbf8;
  color: #238467;
  font-size: 13px;
  font-weight: 600;
}

.account-shell {
  display: grid;
  overflow: hidden;
  border: 1px solid #e2e9f3;
  border-radius: 24px;
  background: #fff;
  box-shadow: 0 20px 55px rgb(39 73 125 / 10%);
  grid-template-columns: 300px minmax(0, 1fr);
}

.identity-panel {
  position: relative;
  overflow: hidden;
  min-height: 650px;
  padding: 46px 30px 32px;
  border-right: 1px solid #e8edf5;
  background: linear-gradient(
    155deg,
    rgb(236 247 255 / 96%),
    rgb(244 255 252 / 94%) 50%,
    #fff 100%
  );
  text-align: center;
}

.identity-panel__glow {
  position: absolute;
  top: -90px;
  right: -90px;
  width: 230px;
  height: 230px;
  border-radius: 50%;
  background: radial-gradient(circle, rgb(68 213 199 / 25%), transparent 68%);
  pointer-events: none;
}

.identity-panel__avatar {
  border: 5px solid rgb(255 255 255 / 85%);
  background: linear-gradient(145deg, #1677ff, #22b7a8);
  box-shadow: 0 12px 28px rgb(32 114 177 / 22%);
  color: #fff;
  font-size: 28px;
  font-weight: 700;
}

.identity-panel h2 {
  overflow: hidden;
  margin: 18px 0 4px;
  color: var(--account-ink);
  font-size: 22px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.identity-panel__account {
  margin: 0 0 12px;
  color: #8592a8;
  font-size: 13px;
}

.identity-panel__role {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 999px;
  background: #e8f2ff;
  color: #2569c9;
  font-size: 12px;
  font-weight: 700;
}

.identity-panel__bio {
  min-height: 44px;
  margin: 18px 0 28px;
  color: #65758d;
  font-size: 13px;
  line-height: 1.7;
}

.account-nav {
  display: grid;
  gap: 10px;
  text-align: left;
}

.account-nav__item {
  display: flex;
  width: 100%;
  padding: 13px 14px;
  border: 1px solid transparent;
  border-radius: 14px;
  align-items: center;
  gap: 12px;
  background: transparent;
  color: #60708a;
  cursor: pointer;
  text-align: left;
  transition: 180ms ease;
}

.account-nav__item > :first-child {
  font-size: 18px;
}

.account-nav__item span {
  display: grid;
  gap: 2px;
}

.account-nav__item strong {
  color: inherit;
  font-size: 14px;
}

.account-nav__item small {
  color: #91a0b5;
  font-size: 12px;
}

.account-nav__item:hover {
  background: rgb(255 255 255 / 75%);
  color: #356cb6;
}

.account-nav__item:focus-visible {
  outline: 2px solid #4c9aff;
  outline-offset: 2px;
}

.account-nav__item--active {
  border-color: #d7e7ff;
  background: #fff;
  box-shadow: 0 8px 20px rgb(48 94 160 / 9%);
  color: var(--account-blue);
}

.settings-panel {
  min-width: 0;
  padding: 42px 48px 50px;
}

.settings-section__heading {
  padding-bottom: 24px;
  border-bottom: 1px solid #edf1f6;
}

.settings-section__heading > div {
  display: flex;
  align-items: center;
  gap: 14px;
}

.settings-section__heading h2 {
  margin: 0;
  color: var(--account-ink);
  font-size: 22px;
}

.settings-section__heading p {
  margin: 5px 0 0;
  color: var(--account-muted);
  font-size: 13px;
}

.settings-section__icon {
  display: grid;
  width: 42px;
  height: 42px;
  border-radius: 13px;
  place-items: center;
  background: #eaf3ff;
  color: var(--account-blue);
  font-size: 19px;
}

.settings-section__icon--security {
  background: #e9f8f4;
  color: #198b77;
}

.account-form {
  margin-top: 28px;
}

.account-form__grid {
  display: grid;
  gap: 18px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.account-form :deep(.ant-form-item-label > label) {
  color: #34435b;
  font-weight: 650;
}

.account-form :deep(.ant-input),
.account-form :deep(.ant-input-affix-wrapper) {
  border-radius: 10px;
}

.account-form :deep(.ant-input-affix-wrapper) {
  padding-block: 7px;
}

.account-form :deep(.ant-input:focus),
.account-form :deep(.ant-input-affix-wrapper-focused) {
  box-shadow: 0 0 0 3px rgb(22 119 255 / 10%);
}

.account-form__hint {
  display: block;
  margin-top: 7px;
  color: #96a1b3;
  font-size: 12px;
}

.avatar-input-row {
  display: flex;
  gap: 10px;
}

.avatar-input-row :deep(.ant-input) {
  min-width: 0;
  flex: 1;
}

.avatar-input-row :deep(.ant-btn) {
  height: 34px;
  flex: none;
  border-radius: 10px;
}

.avatar-file-input {
  display: none;
}

.account-form__actions {
  display: flex;
  padding-top: 4px;
  justify-content: flex-end;
  gap: 10px;
}

.account-form__actions :deep(.ant-btn) {
  min-width: 96px;
  border-radius: 10px;
}

.account-form--security {
  max-width: 540px;
}

.security-notice {
  margin-top: 24px;
  border-radius: 12px;
}

.password-strength {
  display: grid;
  margin-top: 10px;
  align-items: center;
  gap: 6px;
  grid-template-columns: repeat(3, 1fr);
}

.password-strength > span {
  height: 4px;
  border-radius: 999px;
  background: #e7ebf1;
}

.password-strength[data-level='1'] > span:nth-child(1) {
  background: #ff7875;
}

.password-strength[data-level='2'] > span:nth-child(-n + 2) {
  background: #f5b642;
}

.password-strength[data-level='3'] > span {
  background: #35af85;
}

.password-strength small {
  grid-column: 1 / -1;
  color: #909caf;
  font-size: 12px;
}

.password-strength__label--weak {
  color: #d95c59 !important;
}

.password-strength__label--medium {
  color: #b9790d !important;
}

.password-strength__label--strong {
  color: #248565 !important;
}

@media (prefers-reduced-motion: reduce) {
  .account-nav__item {
    transition: none;
  }
}

@media (max-width: 860px) {
  .account-shell {
    grid-template-columns: 1fr;
  }

  .identity-panel {
    min-height: 0;
    padding: 30px 24px 20px;
    border-right: 0;
    border-bottom: 1px solid #e8edf5;
  }

  .identity-panel__bio {
    margin-bottom: 18px;
  }

  .account-nav {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .settings-panel {
    padding: 32px 28px 38px;
  }
}

@media (max-width: 560px) {
  .avatar-input-row {
    align-items: stretch;
    flex-direction: column;
  }

  .account-page__heading {
    align-items: flex-start;
  }

  .account-page__status {
    display: none;
  }

  .account-shell {
    border-radius: 18px;
  }

  .account-nav__item {
    padding: 11px;
  }

  .account-nav__item small {
    display: none;
  }

  .settings-panel {
    padding: 26px 20px 32px;
  }

  .settings-section__heading p {
    line-height: 1.6;
  }

  .account-form__grid {
    gap: 0;
    grid-template-columns: 1fr;
  }

  .account-form__actions {
    flex-direction: column-reverse;
  }

  .account-form__actions :deep(.ant-btn) {
    width: 100%;
  }
}
</style>
