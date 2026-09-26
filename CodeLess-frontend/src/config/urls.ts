/**
 * 三种地址可独立部署：业务 API、生成预览、已部署网站。
 * Vite 环境变量在构建时注入，修改后需要重新启动开发服务或重新构建。
 */
const readBaseUrl = (value: string | undefined, variableName: string): string => {
  const baseUrl = value?.trim().replace(/\/+$/, '')
  if (!baseUrl) {
    throw new Error(`缺少 ${variableName}，请参考 .env.example 配置`)
  }
  try {
    const url = new URL(baseUrl)
    if (!['http:', 'https:'].includes(url.protocol) || url.search || url.hash) {
      throw new Error('地址格式不正确')
    }
  } catch {
    throw new Error(`${variableName} 必须是完整的 HTTP(S) 地址，且不能包含查询参数或片段`)
  }
  return baseUrl
}

export const API_BASE_URL = readBaseUrl(import.meta.env.VITE_API_BASE_URL, 'VITE_API_BASE_URL')
export const PREVIEW_BASE_URL = readBaseUrl(
  import.meta.env.VITE_PREVIEW_BASE_URL,
  'VITE_PREVIEW_BASE_URL',
)
export const DEPLOY_BASE_URL = readBaseUrl(
  import.meta.env.VITE_DEPLOY_BASE_URL,
  'VITE_DEPLOY_BASE_URL',
)

export const getDeployUrl = (deployKey: string): string =>
  `${DEPLOY_BASE_URL}/${encodeURIComponent(deployKey)}/`
