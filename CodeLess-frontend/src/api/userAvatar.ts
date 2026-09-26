import request from '@/request'

/** 上传图片后，后端会立即保存当前登录用户的头像。 */
export const uploadMyAvatar = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request<API.BaseResponseString>('/user/avatar/upload', {
    method: 'POST',
    data: formData,
  })
}
