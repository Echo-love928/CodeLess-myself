<script setup lang="ts">
import { type MenuProps, message } from 'ant-design-vue'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import logoUrl from '@/assets/logo.png'
import { DownOutlined, LogoutOutlined, UserOutlined } from '@ant-design/icons-vue'
import { userLogout } from '@/api/userController.ts'

//获取登录用户状态
const loginUserStore = useLoginUserStore()

type NavigationItem = {
  key: string
  label: string
  path: string
}

// 在这里集中维护导航项，新增页面时只需补充一条配置。
const navigationItems: NavigationItem[] = [
  { key: 'home', label: '首页', path: '/' },
  { key: 'appManage', label: '应用管理', path: '/admin/appManage' },
  { key: 'userManage', label: '用户管理', path: '/admin/userManage' },
]

/**
 * 根据当前登录用户的权限过滤菜单项
 */
const filterMenus = (menus: NavigationItem[]) => {
  return menus.filter((menu) => {
    if (menu.path.startsWith('/admin')) {
      return loginUserStore.loginUser.userRole === 'admin'
    }
    return true
  })
}

const visibleNavigationItems = computed(() => filterMenus(navigationItems))

const route = useRoute()
const router = useRouter()

const selectedKeys = computed(() => {
  const activeItem = visibleNavigationItems.value.find((item) =>
    item.path === '/' ? route.path === '/' : route.path.startsWith(item.path),
  )

  return activeItem ? [activeItem.key] : []
})

const handleMenuClick: MenuProps['onClick'] = ({ key }) => {
  const target = visibleNavigationItems.value.find((item) => item.key === key)

  if (target && target.path !== route.path) {
    void router.push(target.path)
  }
}

const handleLoginClick = () => {
  void router.push('/user/login')
}

const handleUserMenuClick: MenuProps['onClick'] = ({ key }) => {
  if (key === 'profile') {
    void router.push('/account/profile')
  }
}

// 用户注销
const doLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.resetLoginUser()
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}
</script>

<template>
  <a-layout-header class="global-header">
    <div class="global-header__bar">
      <RouterLink class="global-header__brand" to="/" aria-label="返回首页">
        <img class="global-header__logo" :src="logoUrl" alt="CodeLess" />
        <span class="global-header__title">CodeLess</span>
      </RouterLink>

      <nav class="global-header__nav" aria-label="主导航">
        <a-menu
          class="global-header__menu"
          mode="horizontal"
          theme="light"
          :items="visibleNavigationItems"
          :selected-keys="selectedKeys"
          @click="handleMenuClick"
        />
      </nav>
      <div class="global-header__user">
        <a-dropdown v-if="loginUserStore.loginUser.id" :trigger="['hover', 'click']">
          <button class="global-header__user-trigger" type="button" aria-label="打开账户菜单">
            <a-avatar class="global-header__avatar" :src="loginUserStore.loginUser.userAvatar">
              {{ loginUserStore.loginUser.userName?.slice(0, 1) || '用' }}
            </a-avatar>

            <span class="global-header__username">
              {{ loginUserStore.loginUser.userName || '无名' }}
            </span>
            <DownOutlined class="global-header__chevron" />
          </button>
          <template #overlay>
            <a-menu class="global-header__user-menu" @click="handleUserMenuClick">
              <a-menu-item key="profile">
                <UserOutlined />
                个人中心
              </a-menu-item>
              <a-menu-divider />
              <a-menu-item key="logout" @click="doLogout">
                <LogoutOutlined />
                退出登录
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>

        <a-button v-else class="global-header__login" type="primary" @click="handleLoginClick">
          登录
        </a-button>
      </div>
    </div>
  </a-layout-header>
</template>

<style scoped>
.global-header {
  z-index: 10;
  height: auto;
  min-height: 64px;
  padding: 0 32px;
  border-bottom: 1px solid #e7edf6;
  background: rgb(255 255 255 / 94%);
  box-shadow: 0 4px 16px rgb(42 73 120 / 6%);
  line-height: normal;
}

.global-header__bar {
  display: flex;
  width: 100%;
  max-width: 1440px;
  min-height: 64px;
  margin: 0 auto;
  align-items: center;
  gap: 28px;
}

.global-header__brand {
  display: inline-flex;
  flex: none;
  align-items: center;
  gap: 11px;
  color: #24324a;
  text-decoration: none;
}

.global-header__brand:focus-visible,
.global-header__login:focus-visible {
  outline: 2px solid #4c9aff;
  outline-offset: 3px;
}

.global-header__logo {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  box-shadow: 0 3px 10px rgb(49 104 246 / 14%);
  outline: 1px solid #edf2fb;
  object-fit: contain;
}

.global-header__title {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.03em;
  white-space: nowrap;
}

.global-header__nav {
  min-width: 0;
  flex: 1;
  align-self: stretch;
}

.global-header__menu {
  display: flex;
  min-width: 0;
  height: 100%;
  align-items: center;
  border-bottom: 0;
  background: transparent;
  line-height: 64px;
}

.global-header__menu :deep(.ant-menu-item) {
  display: inline-flex;
  height: 38px;
  margin: 0 3px;
  padding-inline: 17px;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  color: #60708a;
  font-weight: 500;
  line-height: 38px;
  transition:
    color 180ms ease,
    background-color 180ms ease;
}

.global-header__menu :deep(.ant-menu-item::after) {
  display: none;
}

.global-header__menu :deep(.ant-menu-item:hover) {
  background: #f1f6ff !important;
  color: #2563eb !important;
}

.global-header__menu :deep(.ant-menu-item-selected) {
  background: #eaf3ff !important;
  color: #1677ff !important;
  font-weight: 600;
}

.global-header__user {
  display: flex;
  flex: none;
  align-items: center;
}

.global-header__user-trigger {
  border: 0;
  border-radius: 12px;
  background: transparent;
  display: flex;
  padding: 6px 8px;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition: background-color 180ms ease;
}

.global-header__user-trigger:hover,
.global-header__user-trigger:focus-visible {
  background: #f1f6ff;
}

.global-header__user-trigger:focus-visible {
  outline: 2px solid #4c9aff;
  outline-offset: 2px;
}

.global-header__avatar {
  flex: none;
  background: #eaf3ff;
  color: #1677ff;
  font-weight: 600;
}

.global-header__username {
  max-width: 120px;
  overflow: hidden;
  color: #34435c;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.global-header__chevron {
  color: #91a0b8;
  font-size: 11px;
}

.global-header__user-menu {
  min-width: 148px;
  padding: 6px;
}

.global-header__login {
  flex: none;
  min-width: 72px;
  border-color: #1677ff;
  border-radius: 9px;
  background: #1677ff;
  box-shadow: 0 5px 12px rgb(22 119 255 / 18%);
  font-weight: 600;
}

.global-header__login:hover {
  border-color: #4096ff !important;
  background: #4096ff !important;
}

@media (max-width: 767px) {
  .global-header {
    padding: 0 16px;
  }

  .global-header__bar {
    min-height: 0;
    flex-wrap: wrap;
    gap: 0 12px;
    padding-top: 10px;
  }

  .global-header__brand {
    min-width: 0;
    flex: 1;
  }

  .global-header__logo {
    width: 34px;
    height: 34px;
  }

  .global-header__title {
    overflow: hidden;
    font-size: 16px;
    text-overflow: ellipsis;
  }

  .global-header__login {
    min-width: 64px;
  }

  .global-header__username {
    max-width: 72px;
  }

  .global-header__nav {
    order: 3;
    width: 100%;
    flex-basis: 100%;
  }

  .global-header__menu {
    line-height: 48px;
  }

  .global-header__menu :deep(.ant-menu-item) {
    height: 36px;
    padding-inline: 14px;
    line-height: 36px;
  }
}
</style>
