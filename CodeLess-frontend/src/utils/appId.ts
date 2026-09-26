import { computed } from 'vue'
import { useRoute } from 'vue-router'

/** 雪花 ID 必须保持字符串，不能转为 JavaScript Number。 */
export const useAppId = () => {
  const route = useRoute()
  return computed(() => {
    const value = route.params.id
    return (Array.isArray(value) ? value[0] : value)?.trim() ?? ''
  })
}

export const isValidAppId = (appId: string): boolean => /^[1-9]\d*$/.test(appId)
