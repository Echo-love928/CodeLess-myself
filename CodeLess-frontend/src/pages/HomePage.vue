<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ArrowRightOutlined, BulbOutlined, ClockCircleOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { addApp, listGoodAppVoByPage, listMyAppVoByPage } from '@/api/appController'
import { useLoginUserStore } from '@/stores/loginUser'

const router = useRouter()
const loginUserStore = useLoginUserStore()
const prompt = ref('')
const creating = ref(false)
const activePrompt = ref('')

type AppListState = { records: API.AppVO[]; total: number; loading: boolean; pageNum: number; pageSize: number; appName: string }
const myApps = reactive<AppListState>({ records: [], total: 0, loading: false, pageNum: 1, pageSize: 6, appName: '' })
const goodApps = reactive<AppListState>({ records: [], total: 0, loading: false, pageNum: 1, pageSize: 6, appName: '' })
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
  } catch { message.error('创建失败，请检查后端服务是否正常') }
  finally { creating.value = false }
}

const useSuggestion = (value: string) => {
  prompt.value = `创建一个设计精致的${value}`
  activePrompt.value = value
}

const loadMyApps = async () => {
  if (!isLoggedIn.value) { myApps.records = []; myApps.total = 0; return }
  myApps.loading = true
  try {
    const res = await listMyAppVoByPage({ pageNum: myApps.pageNum, pageSize: Math.min(myApps.pageSize, 20), appName: myApps.appName || undefined, sortField: 'createTime', sortOrder: 'descend' })
    if (res.data.code === 0 && res.data.data) { myApps.records = res.data.data.records ?? []; myApps.total = res.data.data.totalRow ?? 0 }
    else message.error(`获取我的应用失败：${res.data.message || '未知错误'}`)
  } catch { message.error('获取我的应用失败') }
  finally { myApps.loading = false }
}

const loadGoodApps = async () => {
  goodApps.loading = true
  try {
    const res = await listGoodAppVoByPage({ pageNum: goodApps.pageNum, pageSize: Math.min(goodApps.pageSize, 20), appName: goodApps.appName || undefined, sortField: 'priority', sortOrder: 'descend' })
    if (res.data.code === 0 && res.data.data) { goodApps.records = res.data.data.records ?? []; goodApps.total = res.data.data.totalRow ?? 0 }
    else message.error(`获取精选应用失败：${res.data.message || '未知错误'}`)
  } catch { message.error('获取精选应用失败') }
  finally { goodApps.loading = false }
}

const searchApps = (target: AppListState, loader: () => Promise<void>) => { target.pageNum = 1; void loader() }
const openApp = (app: API.AppVO) => { if (app.id) void router.push(`/app/chat/${app.id}`) }
const formatDate = (value?: string) => value ? new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: 'short', day: 'numeric' }).format(new Date(value)) : '刚刚创建'

onMounted(() => void Promise.all([loadMyApps(), loadGoodApps()]))
</script>

<template>
  <main class="home-page">
    <section class="hero" aria-labelledby="hero-title">
      <div class="hero__glow hero__glow--one" /><div class="hero__glow hero__glow--two" />
      <div class="hero__content">
        <div class="hero__eyebrow"><BulbOutlined /> AI 网站工坊</div>
        <h1 id="hero-title">一句话，<span>让创意成为网站</span></h1>
        <p>描述你的想法，CodeLess 会帮你生成、预览并部署一个真实可访问的网站。</p>
        <div class="prompt-box">
          <a-textarea v-model:value="prompt" class="prompt-box__input" :maxlength="1000" :auto-size="{ minRows: 4, maxRows: 8 }" placeholder="例如：创建一个极简风格的个人作品集，包含项目展示、个人介绍和联系方式……" @press-enter.exact.prevent="createApp" />
          <div class="prompt-box__footer">
            <span>Enter 发送 · 详细描述会让结果更准确</span>
            <a-button type="primary" shape="circle" size="large" :loading="creating" aria-label="开始创建" @click="createApp"><template #icon><ArrowRightOutlined /></template></a-button>
          </div>
        </div>
        <div class="suggestions" aria-label="提示词示例">
          <button v-for="item in ['个人作品集', '咖啡馆官网', '旅行计划工具', '产品落地页']" :key="item" type="button" :class="{ active: activePrompt === item }" @click="useSuggestion(item)">{{ item }}</button>
        </div>
      </div>
    </section>

    <section class="app-section" aria-labelledby="my-apps-title">
      <div class="section-heading"><div><span>01 / YOUR WORK</span><h2 id="my-apps-title">我的应用</h2><p>继续打磨你创建的网站</p></div><a-input-search v-if="isLoggedIn" v-model:value="myApps.appName" class="app-search" placeholder="按名称搜索" allow-clear @search="searchApps(myApps, loadMyApps)" /></div>
      <a-spin :spinning="myApps.loading">
        <div v-if="!isLoggedIn" class="empty-state"><div class="empty-state__icon"><BulbOutlined /></div><h3>登录后保存你的每一次创作</h3><p>创建、管理和部署网站都需要先登录。</p><a-button type="primary" @click="router.push('/user/login')">前往登录</a-button></div>
        <div v-else-if="myApps.records.length" class="app-grid">
          <article v-for="app in myApps.records" :key="app.id" class="app-card" tabindex="0" @click="openApp(app)" @keydown.enter="openApp(app)">
            <div class="app-card__cover"><img v-if="app.cover" :src="app.cover" :alt="`${app.appName || '应用'}封面`" /><div v-else class="app-card__placeholder"><span>{{ app.appName?.slice(0, 1) || 'C' }}</span><small>CODELESS PREVIEW</small></div><b>{{ app.codeGenType || 'AI 网站' }}</b></div>
            <div class="app-card__body"><h3>{{ app.appName || '未命名应用' }}</h3><p>{{ app.initPrompt || '继续与 AI 对话来完善这个应用' }}</p><span><ClockCircleOutlined /> {{ formatDate(app.createTime) }}</span></div>
          </article>
        </div>
        <a-empty v-else description="还没有应用，从上方输入一句话开始创建吧" />
      </a-spin>
      <a-pagination v-if="myApps.total > myApps.pageSize" v-model:current="myApps.pageNum" :page-size="myApps.pageSize" :total="myApps.total" :show-size-changer="false" @change="loadMyApps" />
    </section>

    <section class="app-section app-section--featured" aria-labelledby="good-apps-title">
      <div class="section-heading"><div><span>02 / INSPIRATION</span><h2 id="good-apps-title">精选应用</h2><p>看看社区里正在发生的好创意</p></div><a-input-search v-model:value="goodApps.appName" class="app-search" placeholder="搜索精选应用" allow-clear @search="searchApps(goodApps, loadGoodApps)"><template #enterButton><SearchOutlined /></template></a-input-search></div>
      <a-spin :spinning="goodApps.loading">
        <div v-if="goodApps.records.length" class="app-grid">
          <article v-for="app in goodApps.records" :key="app.id" class="app-card" tabindex="0" @click="openApp(app)" @keydown.enter="openApp(app)">
            <div class="app-card__cover"><img v-if="app.cover" :src="app.cover" :alt="`${app.appName || '应用'}封面`" /><div v-else class="app-card__placeholder featured"><span>{{ app.appName?.slice(0, 1) || '精' }}</span><small>FEATURED BUILD</small></div><b class="featured-tag">精选</b></div>
            <div class="app-card__body"><h3>{{ app.appName || '未命名应用' }}</h3><p>{{ app.initPrompt || '一个由 AI 创作的精选网站' }}</p><span>{{ app.user?.userName || 'CodeLess 创作者' }}</span></div>
          </article>
        </div>
        <a-empty v-else description="暂时还没有精选应用" />
      </a-spin>
      <a-pagination v-if="goodApps.total > goodApps.pageSize" v-model:current="goodApps.pageNum" :page-size="goodApps.pageSize" :total="goodApps.total" :show-size-changer="false" @change="loadGoodApps" />
    </section>
  </main>
</template>

<style scoped>
.home-page{--ink:#14213d;--muted:#68758d;color:var(--ink)}.hero{position:relative;min-height:510px;overflow:hidden;border:1px solid #ffffffcc;border-radius:32px;background:linear-gradient(135deg,#ffffffef,#f2fffcdf 48%,#e2f4ffeb);box-shadow:0 30px 70px #336da31f}.hero:after{position:absolute;right:-8%;bottom:-44%;width:65%;height:75%;border-radius:50%;background:repeating-radial-gradient(circle,transparent 0 14px,#2563eb0d 15px 16px);content:''}.hero__glow{position:absolute;border-radius:50%;filter:blur(12px)}.hero__glow--one{top:-140px;right:5%;width:380px;height:380px;background:#3de0c240}.hero__glow--two{bottom:-180px;left:0;width:430px;height:430px;background:#49a0ff33}.hero__content{position:relative;z-index:1;width:min(820px,calc(100% - 48px));margin:auto;padding:64px 0 54px;text-align:center}.hero__eyebrow{display:inline-flex;padding:7px 13px;align-items:center;gap:7px;border:1px solid #15a7932e;border-radius:999px;background:#ffffffb3;color:#148679;font-size:12px;font-weight:700;letter-spacing:.12em}.hero h1{margin:18px 0 8px;font-family:STKaiti,KaiTi,"Songti SC",serif;font-size:clamp(38px,5vw,62px);font-weight:800;letter-spacing:-.04em;line-height:1.14}.hero h1 span{background:linear-gradient(100deg,#0e8f85,#2563eb 72%);background-clip:text;color:transparent}.hero__content>p{margin:0 0 30px;color:var(--muted);font-size:16px}.prompt-box{padding:12px 14px 12px 18px;border:1px solid #7b97b933;border-radius:22px;background:#fffffff0;box-shadow:0 20px 50px #38629329;text-align:left;transition:.18s}.prompt-box:focus-within{box-shadow:0 24px 60px #2563eb33;transform:translateY(-2px)}.prompt-box__input,.prompt-box__input:focus{padding:6px 2px;border:0;background:transparent;box-shadow:none;font-size:16px;resize:none}.prompt-box__footer{display:flex;align-items:center;justify-content:space-between;color:#99a4b6;font-size:12px}.prompt-box__footer :deep(.ant-btn){border:0;background:linear-gradient(135deg,#14b8a6,#2563eb);box-shadow:0 8px 20px #2563eb47}.suggestions{display:flex;margin-top:18px;flex-wrap:wrap;justify-content:center;gap:9px}.suggestions button{padding:7px 13px;border:1px solid #7b97b92e;border-radius:999px;background:#ffffffbd;color:#66748a;cursor:pointer;font-size:13px;transition:.16s}.suggestions button:hover,.suggestions button.active{border-color:#56b9c2;color:#0e8f85;transform:translateY(-1px)}.app-section{padding:72px 4px 10px}.app-section--featured{padding-bottom:50px}.section-heading{display:flex;margin-bottom:24px;align-items:flex-end;justify-content:space-between;gap:24px}.section-heading span{color:#168c83;font-size:11px;font-weight:800;letter-spacing:.14em}.section-heading h2{margin:4px 0 2px;font-size:30px;letter-spacing:-.03em}.section-heading p{margin:0;color:var(--muted)}.app-search{width:260px}.app-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:22px}.app-card{overflow:hidden;border:1px solid #e5eaf2;border-radius:16px;background:#fff;box-shadow:0 10px 28px #24416812;cursor:pointer;transition:.18s}.app-card:hover,.app-card:focus-visible{outline:0;box-shadow:0 18px 38px #24416826;transform:translateY(-4px)}.app-card__cover{position:relative;height:196px;overflow:hidden;background:#eef3f9}.app-card__cover img{width:100%;height:100%;object-fit:cover}.app-card__cover>b{position:absolute;top:12px;right:12px;padding:5px 9px;border-radius:8px;background:#14213dd1;color:white;font-size:11px}.app-card__cover>.featured-tag{background:#0e8f85e0}.app-card__placeholder{display:flex;height:100%;align-items:center;justify-content:center;background:linear-gradient(135deg,#dceeff,#d9faf3);color:#1f5fae;flex-direction:column;gap:8px}.app-card__placeholder.featured{background:linear-gradient(135deg,#e7f7f4,#e8edff);color:#118b80}.app-card__placeholder span{display:grid;width:62px;height:62px;place-items:center;border-radius:18px;background:#ffffffb3;box-shadow:0 10px 30px #1c59961f;font-size:28px;font-weight:800}.app-card__placeholder small{font-size:10px;font-weight:700;letter-spacing:.15em;opacity:.65}.app-card__body{padding:16px 18px 18px}.app-card__body h3{margin:0 0 7px;overflow:hidden;font-size:17px;text-overflow:ellipsis;white-space:nowrap}.app-card__body p{display:-webkit-box;min-height:42px;margin:0 0 13px;overflow:hidden;color:var(--muted);font-size:13px;line-height:1.6;-webkit-box-orient:vertical;-webkit-line-clamp:2}.app-card__body>span{display:inline-flex;align-items:center;gap:6px;color:#97a1b2;font-size:12px}.empty-state{padding:48px 20px;border:1px dashed #ccd7e5;border-radius:18px;background:#fff;text-align:center}.empty-state__icon{display:grid;width:50px;height:50px;margin:0 auto 12px;place-items:center;border-radius:15px;background:#eaf7f5;color:#14998c;font-size:22px}.empty-state h3{margin:0 0 5px}.empty-state p{margin:0 0 16px;color:var(--muted)}.app-section :deep(.ant-pagination){margin-top:28px;justify-content:center}@media(max-width:920px){.app-grid{grid-template-columns:repeat(2,minmax(0,1fr))}}@media(max-width:620px){.hero{min-height:0;border-radius:22px}.hero__content{width:calc(100% - 28px);padding:42px 0 34px}.hero h1{font-size:36px}.prompt-box__footer>span{display:none}.prompt-box__footer{justify-content:flex-end}.app-section{padding-top:52px}.section-heading{align-items:stretch;flex-direction:column;gap:16px}.app-search{width:100%}.app-grid{grid-template-columns:1fr}}@media(prefers-reduced-motion:reduce){.prompt-box,.app-card,.suggestions button{transition:none}}
.app-card__cover img{object-position:top}
</style>
