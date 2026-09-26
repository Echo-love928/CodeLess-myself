<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { DeleteOutlined, EditOutlined, StarOutlined } from '@ant-design/icons-vue'
import { deleteAppByAdmin, listAppVoByPageByAdmin, updateAppByAdmin } from '@/api/appController'
import { CODE_GEN_TYPE_OPTIONS, getCodeGenTypeLabel } from '@/constants/codeGenType'

const router = useRouter()
const data = ref<API.AppVO[]>([])
const total = ref(0)
const loading = ref(false)
const searchParams = reactive<API.AppQueryRequest>({ pageNum: 1, pageSize: 10 })
const columns = [
  { title: 'ID', dataIndex: 'id', width: 90 },
  { title: '应用', dataIndex: 'appName', width: 260 },
  { title: '用户 ID', dataIndex: 'userId', width: 100 },
  { title: '生成类型', dataIndex: 'codeGenType', width: 130 },
  { title: '优先级', dataIndex: 'priority', width: 90 },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
  { title: '操作', key: 'action', fixed: 'right', width: 240 },
]

const fetchData = async () => {
  loading.value = true
  try {
    const res = await listAppVoByPageByAdmin({ ...searchParams })
    if (res.data.code === 0 && res.data.data) { data.value = res.data.data.records ?? []; total.value = res.data.data.totalRow ?? 0 }
    else message.error(`获取应用失败：${res.data.message || '未知错误'}`)
  } catch { message.error('获取应用失败，请检查后端服务') }
  finally { loading.value = false }
}

const pagination = computed(() => ({ current: searchParams.pageNum, pageSize: searchParams.pageSize, total: total.value, showSizeChanger: true, showTotal: (value: number) => `共 ${value} 条` }))
const doSearch = () => { searchParams.pageNum = 1; void fetchData() }
const resetSearch = () => { Object.assign(searchParams, { pageNum: 1, pageSize: 10, id: undefined, appName: undefined, codeGenType: undefined, userId: undefined, priority: undefined }); void fetchData() }
const tableChange = (page: { current?: number; pageSize?: number }) => { searchParams.pageNum = page.current ?? 1; searchParams.pageSize = page.pageSize ?? 10; void fetchData() }

const removeApp = (record: API.AppVO) => {
  Modal.confirm({ title: '删除应用', content: `确定删除“${record.appName || '未命名应用'}”吗？删除后无法恢复。`, okText: '删除', okType: 'danger', cancelText: '取消', async onOk() {
    if (!record.id) return
    const res = await deleteAppByAdmin({ id: record.id })
    if (res.data.code === 0 && res.data.data) { message.success('删除成功'); await fetchData() }
    else throw new Error(res.data.message || '删除失败')
  } })
}

const featureApp = (record: API.AppVO) => {
  Modal.confirm({ title: '设为精选应用', content: `将“${record.appName || '未命名应用'}”的优先级设置为 99？`, okText: '设为精选', cancelText: '取消', async onOk() {
    if (!record.id) return
    const res = await updateAppByAdmin({ id: record.id, priority: 99 })
    if (res.data.code === 0 && res.data.data) { message.success('已设为精选'); await fetchData() }
    else throw new Error(res.data.message || '操作失败')
  } })
}

const formatTime = (value?: string) => value ? new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value)) : '-'
onMounted(fetchData)
</script>

<template>
  <main class="manage-page">
    <div class="page-title"><div><h1>应用管理</h1><p>查看、编辑、精选或删除平台中的应用。</p></div><a-tag color="blue">{{ total }} 个应用</a-tag></div>
    <a-card class="search-card" :bordered="false">
      <a-form layout="inline" :model="searchParams" @finish="doSearch">
        <a-form-item label="应用 ID"><a-input v-model:value="searchParams.id" placeholder="输入 ID" /></a-form-item>
        <a-form-item label="应用名称"><a-input v-model:value="searchParams.appName" placeholder="输入名称" allow-clear /></a-form-item>
        <a-form-item label="用户 ID"><a-input v-model:value="searchParams.userId" placeholder="输入用户 ID" /></a-form-item>
        <a-form-item label="生成类型"><a-select v-model:value="searchParams.codeGenType" :options="CODE_GEN_TYPE_OPTIONS" placeholder="全部类型" allow-clear style="width: 180px" /></a-form-item>
        <a-form-item><a-space><a-button type="primary" html-type="submit">搜索</a-button><a-button @click="resetSearch">重置</a-button></a-space></a-form-item>
      </a-form>
    </a-card>
    <a-table row-key="id" :columns="columns" :data-source="data" :loading="loading" :pagination="pagination" :scroll="{ x: 1100 }" @change="tableChange">
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'appName'"><div class="app-cell"><a-avatar shape="square" :src="record.cover">{{ record.appName?.slice(0, 1) || '应' }}</a-avatar><div><strong>{{ record.appName || '未命名应用' }}</strong><small>{{ record.initPrompt || '暂无初始提示词' }}</small></div></div></template>
        <template v-else-if="column.dataIndex === 'codeGenType'"><a-tag>{{ getCodeGenTypeLabel(record.codeGenType) }}</a-tag></template>
        <template v-else-if="column.dataIndex === 'priority'"><a-tag :color="record.priority === 99 ? 'gold' : 'default'">{{ record.priority ?? 0 }}<span v-if="record.priority === 99"> · 精选</span></a-tag></template>
        <template v-else-if="column.dataIndex === 'createTime'">{{ formatTime(record.createTime) }}</template>
        <template v-else-if="column.key === 'action'"><a-space><a-button size="small" @click="router.push(`/admin/app/edit/${record.id}`)"><EditOutlined />编辑</a-button><a-button size="small" :disabled="record.priority === 99" @click="featureApp(record)"><StarOutlined />精选</a-button><a-button size="small" danger @click="removeApp(record)"><DeleteOutlined />删除</a-button></a-space></template>
      </template>
    </a-table>
  </main>
</template>

<style scoped>
.manage-page{padding:4px}.page-title{display:flex;margin-bottom:24px;align-items:center;justify-content:space-between}.page-title span{color:#168c83;font-size:11px;font-weight:800;letter-spacing:.14em}.page-title h1{margin:4px 0;font-size:30px}.page-title p{margin:0;color:#768398}.search-card{margin-bottom:18px;border-radius:14px;box-shadow:0 8px 28px #2441680d}.search-card :deep(.ant-form){row-gap:14px}.manage-page :deep(.ant-table-wrapper){overflow:hidden;border-radius:14px;box-shadow:0 8px 28px #2441680d}.app-cell{display:flex;min-width:220px;align-items:center;gap:10px}.app-cell .ant-avatar{flex:none;background:#e7f4f2;color:#168c83}.app-cell div{min-width:0}.app-cell strong,.app-cell small{display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.app-cell small{max-width:185px;margin-top:3px;color:#919bad;font-size:11px}@media(max-width:600px){.page-title{align-items:flex-start;flex-direction:column;gap:12px}}
</style>
