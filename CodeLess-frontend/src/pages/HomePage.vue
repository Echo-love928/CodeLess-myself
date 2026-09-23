<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ArrowRightOutlined, BulbOutlined, SearchOutlined } from '@ant-design/icons-vue'
import AppCard from '@/components/AppCard.vue'
import { addApp, listGoodAppVoByPage, listMyAppVoByPage } from '@/api/appController'
import { useLoginUserStore } from '@/stores/loginUser'
import { getDeployUrl } from '@/config/urls'

const router = useRouter()
const loginUserStore = useLoginUserStore()
const prompt = ref('')
const creating = ref(false)
const activePrompt = ref('')
const suggestions = [
  {
    title: '个人作品集',
    prompt:
      '帮我创建一个个人作品集网站，首页展示姓名、职业定位和一句简短介绍；设置关于我、精选项目、技能清单和联系方式。每个项目有封面、简介和详情入口，支持按类别筛选。整体风格简洁现代，手机和电脑上都要清晰易用，并突出联系按钮。',
  },
  {
    title: '个人博客',
    prompt:
      '帮我创建一个个人博客网站，首页展示最新文章和作者简介，提供文章分类、关键词搜索、文章详情和阅读时间。详情页要有清晰的标题层级、目录和上一篇下一篇入口。整体采用舒适的阅读排版与柔和配色，适配手机屏幕，并预留订阅和联系方式。',
  },
  {
    title: '咖啡馆官网',
    prompt:
      '帮我创建一个咖啡馆官网，首页突出店名、品牌故事和主打饮品，包含菜单展示、门店环境、营业时间、地址地图和预约入口。菜单按咖啡、甜点分类，展示图片、价格和简短介绍。视觉风格温暖精致，按钮清晰，移动端也能方便查看位置并联系门店。',
  },
  {
    title: '商品展示网站',
    prompt:
      '帮我创建一个小型商品展示网站，首页有品牌介绍、主推商品和分类导航；商品列表支持筛选与搜索，详情页展示图片、价格、规格和购买说明。加入购物车交互和空状态提示，结算部分先用演示流程。整体风格简洁可信，兼顾手机和电脑的浏览体验。',
  },
] as const

type AppListState = {
  records: API.AppVO[]
  total: number
  loading: boolean
  pageNum: number
  pageSize: number
  appName: string
}
const myApps = reactive<AppListState>({
  records: [],
  total: 0,
  loading: false,
  pageNum: 1,
  pageSize: 6,
  appName: '',
})
const goodApps = reactive<AppListState>({
  records: [],
  total: 0,
  loading: false,
  pageNum: 1,
  pageSize: 6,
  appName: '',
})
const isLoggedIn = computed(() => Boolean(loginUserStore.loginUser.id))

const createApp = async () => {
  const initPrompt = prompt.value.trim()
  if (!initPrompt) return void message.warning('请先描述你想创建的网站')
  if (!isLoggedIn.value) {
    message.warning('登录后即可开始创建')
    await router.push({ path: '/user/login', query: { redirect: '/' } })
    return
  }
  creating.value = true
  try {
    const res = await addApp({ initPrompt })
    if (res.data.code === 0 && res.data.data) {
      await router.push({ path: `/app/chat/${res.data.data}`, query: { auto: '1' } })
    } else message.error(`创建失败：${res.data.message || '请稍后重试'}`)
  } catch {
    message.error('创建失败，请检查后端服务是否正常')
  } finally {
    creating.value = false
  }
}

const useSuggestion = (item: (typeof suggestions)[number]) => {
  prompt.value = item.prompt
  activePrompt.value = item.title
}

const loadMyApps = async () => {
  if (!isLoggedIn.value) {
    myApps.records = []
    myApps.total = 0
    return
  }
  myApps.loading = true
  try {
    const res = await listMyAppVoByPage({
      pageNum: myApps.pageNum,
      pageSize: Math.min(myApps.pageSize, 20),
      appName: myApps.appName || undefined,
      sortField: 'createTime',
      sortOrder: 'descend',
    })
    if (res.data.code === 0 && res.data.data) {
      myApps.records = res.data.data.records ?? []
      myApps.total = res.data.data.totalRow ?? 0
    } else message.error(`获取我的应用失败：${res.data.message || '未知错误'}`)
  } catch {
    message.error('获取我的应用失败')
  } finally {
    myApps.loading = false
  }
}

const loadGoodApps = async () => {
  goodApps.loading = true
  try {
    const res = await listGoodAppVoByPage({
      pageNum: goodApps.pageNum,
      pageSize: Math.min(goodApps.pageSize, 20),
      appName: goodApps.appName || undefined,
      sortField: 'priority',
      sortOrder: 'descend',
    })
    if (res.data.code === 0 && res.data.data) {
      goodApps.records = res.data.data.records ?? []
      goodApps.total = res.data.data.totalRow ?? 0
    } else message.error(`获取精选应用失败：${res.data.message || '未知错误'}`)
  } catch {
    message.error('获取精选应用失败')
  } finally {
    goodApps.loading = false
  }
}

const searchApps = (target: AppListState, loader: () => Promise<void>) => {
  target.pageNum = 1
  void loader()
}
const openConversation = (app: API.AppVO) => {
  if (app.id) void router.push({ path: `/app/chat/${app.id}`, query: { view: '1' } })
}
const openDeployedApp = (app: API.AppVO) => {
  const deployKey = app.deployKey?.trim()
  if (!deployKey) return
  window.open(getDeployUrl(deployKey), '_blank', 'noopener,noreferrer')
}
onMounted(() => void Promise.all([loadMyApps(), loadGoodApps()]))
</script>

<template>
  <main class="home-page">
    <section class="hero" aria-labelledby="hero-title">
      <div class="hero__glow hero__glow--one" />
      <div class="hero__glow hero__glow--two" />
      <div class="hero__content">
        <h1 id="hero-title">AI 应用生成平台</h1>
        <p>描述你的想法，CodeLess 会帮你生成、预览并部署一个真实可访问的网站。</p>
        <div class="prompt-box">
          <a-textarea
            v-model:value="prompt"
            class="prompt-box__input"
            :maxlength="1000"
            :auto-size="{ minRows: 4, maxRows: 8 }"
            placeholder="帮我创作建个人作品集"
            @press-enter.exact.prevent="createApp"
          />
          <div class="prompt-box__footer">
            <span>Enter 发送 · 详细描述会让结果更准确</span>
            <a-button
              type="primary"
              shape="circle"
              size="large"
              :loading="creating"
              aria-label="开始创建"
              @click="createApp"
              ><template #icon><ArrowRightOutlined /></template
            ></a-button>
          </div>
        </div>
        <div class="suggestions" aria-label="提示词示例">
          <button
            v-for="item in suggestions"
            :key="item.title"
            type="button"
            :class="{ active: activePrompt === item.title }"
            @click="useSuggestion(item)"
          >
            {{ item.title }}
          </button>
        </div>
      </div>
    </section>

    <section class="app-section" aria-labelledby="my-apps-title">
      <div class="section-heading">
        <div>
          <h2 id="my-apps-title">我的应用</h2>
          <p>继续打磨你创建的网站</p>
        </div>
        <a-input-search
          v-if="isLoggedIn"
          v-model:value="myApps.appName"
          class="app-search"
          placeholder="按名称搜索"
          allow-clear
          @search="searchApps(myApps, loadMyApps)"
        />
      </div>
      <a-spin :spinning="myApps.loading">
        <div v-if="!isLoggedIn" class="empty-state">
          <div class="empty-state__icon"><BulbOutlined /></div>
          <h3>登录后保存你的每一次创作</h3>
          <p>创建、管理和部署网站都需要先登录。</p>
          <a-button type="primary" @click="router.push('/user/login')">前往登录</a-button>
        </div>
        <div v-else-if="myApps.records.length" class="app-grid">
          <AppCard
            v-for="app in myApps.records"
            :key="app.id"
            :app="app"
            @view-conversation="openConversation"
            @view-work="openDeployedApp"
          />
        </div>
        <a-empty v-else description="还没有应用，从上方输入一句话开始创建吧" />
      </a-spin>
      <a-pagination
        v-if="myApps.total > myApps.pageSize"
        v-model:current="myApps.pageNum"
        :page-size="myApps.pageSize"
        :total="myApps.total"
        :show-size-changer="false"
        @change="loadMyApps"
      />
    </section>

    <section class="app-section app-section--featured" aria-labelledby="good-apps-title">
      <div class="section-heading">
        <div>
          <h2 id="good-apps-title">精选应用</h2>
          <p>看看社区里正在发生的好创意</p>
        </div>
        <a-input-search
          v-model:value="goodApps.appName"
          class="app-search"
          placeholder="搜索精选应用"
          allow-clear
          @search="searchApps(goodApps, loadGoodApps)"
          ><template #enterButton><SearchOutlined /></template
        ></a-input-search>
      </div>
      <a-spin :spinning="goodApps.loading">
        <div v-if="goodApps.records.length" class="app-grid">
          <AppCard
            v-for="app in goodApps.records"
            :key="app.id"
            :app="app"
            featured
            @view-conversation="openConversation"
            @view-work="openDeployedApp"
          />
        </div>
        <a-empty v-else description="暂时还没有精选应用" />
      </a-spin>
      <a-pagination
        v-if="goodApps.total > goodApps.pageSize"
        v-model:current="goodApps.pageNum"
        :page-size="goodApps.pageSize"
        :total="goodApps.total"
        :show-size-changer="false"
        @change="loadGoodApps"
      />
    </section>
  </main>
</template>

<style scoped>
.home-page {
  --ink: #1d2b45;
  --muted: #68758d;
  position: relative;
  isolation: isolate;
  overflow-x: clip;
  padding-bottom: 52px;
  color: var(--ink);
}
.hero {
  position: relative;
  min-height: 475px;
  overflow: visible;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}
.hero::after {
  position: absolute;
  right: -4%;
  bottom: 2%;
  width: 41%;
  height: 40%;
  border-radius: 50%;
  background: repeating-radial-gradient(circle, transparent 0 16px, rgb(86 108 205 / 8%) 17px 18px);
  content: '';
  mask-image: linear-gradient(180deg, #000, transparent 90%);
  pointer-events: none;
}
.hero__glow {
  position: absolute;
  z-index: 0;
  border-radius: 50%;
  filter: blur(32px);
  opacity: 0.64;
  pointer-events: none;
  animation: drift-glow 20s ease-in-out infinite alternate;
}
.hero__glow--one {
  top: -45px;
  right: 0;
  width: 360px;
  height: 300px;
  background: #c7f1e9;
}
.hero__glow--two {
  bottom: 4px;
  left: 5%;
  width: 400px;
  height: 280px;
  background: #dadfff;
  animation-delay: -9s;
}
.hero__content {
  position: relative;
  z-index: 1;
  width: min(860px, calc(100% - 48px));
  margin: auto;
  padding: 78px 0 54px;
  text-align: center;
}
.hero h1 {
  margin: 0 0 16px;
  background: linear-gradient(100deg, #236d83 0%, #365f96 56%, #526bb0 100%);
  background-clip: text;
  color: transparent;
  font-family: 'PingFang SC', 'Microsoft YaHei', system-ui, sans-serif;
  font-size: clamp(38px, 5vw, 64px);
  font-weight: 750;
  letter-spacing: 0.01em;
  line-height: 1.2;
}
.hero__content > p {
  margin: 0 0 32px;
  color: var(--muted);
  font-size: 16px;
  line-height: 1.7;
}
.prompt-box {
  padding: 17px 18px 13px 22px;
  border: 0;
  border-radius: 22px;
  background: #fff;
  box-shadow: 0 22px 55px rgb(69 87 160 / 16%);
  text-align: left;
  transition:
    box-shadow 0.18s,
    transform 0.18s;
}
.prompt-box:focus-within {
  box-shadow: 0 26px 62px rgb(78 102 192 / 23%);
  transform: translateY(-2px);
}
.prompt-box__input,
.prompt-box__input:focus,
.prompt-box :deep(textarea),
.prompt-box :deep(textarea:focus) {
  padding: 6px 2px;
  border: 0;
  outline: 0;
  background: transparent;
  box-shadow: none;
  font-size: 16px;
  resize: none;
}
.prompt-box :deep(textarea::placeholder) {
  color: #98a7bc;
}
.prompt-box__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #8190a5;
  font-size: 12px;
}
.prompt-box__footer :deep(.ant-btn) {
  border: 0;
  background: linear-gradient(135deg, #176bd4, #5259da);
  box-shadow: 0 8px 20px rgb(46 95 217 / 27%);
}
.suggestions {
  display: flex;
  margin-top: 18px;
  flex-wrap: wrap;
  justify-content: center;
  gap: 9px;
}
.suggestions button {
  padding: 7px 13px;
  border: 1px solid rgb(101 121 183 / 19%);
  border-radius: 999px;
  background: rgb(255 255 255 / 75%);
  color: #596983;
  cursor: pointer;
  font-size: 13px;
  transition:
    border-color 0.16s,
    color 0.16s,
    transform 0.16s;
}
.suggestions button:hover,
.suggestions button.active {
  border-color: #8b9de6;
  background: #fff;
  color: #4d62bd;
  transform: translateY(-1px);
}
.suggestions button:focus-visible {
  outline: 2px solid #8b9de6;
  outline-offset: 3px;
}
.app-section {
  position: relative;
  width: min(1200px, calc(100% - 48px));
  margin: 0 auto;
  padding: 38px 40px 42px;
  border: 1px solid rgb(255 255 255 / 85%);
  border-radius: 28px;
  background: rgb(255 255 255 / 97%);
  box-shadow: 0 28px 70px rgb(63 82 148 / 13%);
}
.app-section--featured {
  margin: 28px auto 0;
}
.section-heading {
  display: flex;
  margin-bottom: 26px;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
}
.section-heading h2 {
  margin: 0 0 2px;
  color: var(--ink);
  font-size: 30px;
  letter-spacing: -0.03em;
}
.section-heading p {
  margin: 0;
  color: var(--muted);
}
.app-search {
  width: 260px;
}
.app-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 22px;
}
.empty-state {
  padding: 48px 20px;
  border: 1px dashed #ccd7e5;
  border-radius: 18px;
  background: #fff;
  text-align: center;
}
.empty-state__icon {
  display: grid;
  width: 50px;
  height: 50px;
  margin: 0 auto 12px;
  place-items: center;
  border-radius: 15px;
  background: #eaf7f5;
  color: #14998c;
  font-size: 22px;
}
.empty-state h3 {
  margin: 0 0 5px;
}
.empty-state p {
  margin: 0 0 16px;
  color: var(--muted);
}
.app-section :deep(.ant-pagination) {
  margin-top: 28px;
  justify-content: center;
}
@keyframes drift-glow {
  from {
    transform: translate3d(-12px, 7px, 0);
  }
  to {
    transform: translate3d(16px, -9px, 0);
  }
}
@media (max-width: 920px) {
  .app-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .app-section {
    padding: 32px 28px 36px;
  }
}
@media (max-width: 620px) {
  .hero {
    min-height: 0;
  }
  .hero__content {
    width: calc(100% - 32px);
    padding: 54px 0 46px;
  }
  .hero h1 {
    font-size: 36px;
  }
  .hero__content > p {
    font-size: 14px;
  }
  .hero__glow {
    opacity: 0.24;
  }
  .prompt-box {
    padding: 14px;
  }
  .prompt-box__footer > span {
    display: none;
  }
  .prompt-box__footer {
    justify-content: flex-end;
  }
  .app-section {
    width: calc(100% - 28px);
    padding: 24px 18px 28px;
    border-radius: 21px;
  }
  .app-section--featured {
    margin-top: 20px;
  }
  .section-heading {
    align-items: stretch;
    flex-direction: column;
    gap: 16px;
  }
  .app-search {
    width: 100%;
  }
  .app-grid {
    grid-template-columns: 1fr;
  }
}
@media (prefers-reduced-motion: reduce) {
  .hero__glow {
    animation: none;
  }
  .prompt-box,
  .suggestions button {
    transition: none;
  }
}
</style>
