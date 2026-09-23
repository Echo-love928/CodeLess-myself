<script lang="ts" setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { userRegister } from '@/api/userController.ts'
import AuthFormShell from '@/components/AuthFormShell.vue'

const router = useRouter()
const submitting = ref(false)

const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

/**
 * 校验两次输入的密码是否一致
 */
const validateCheckPassword = async (_rule: unknown, value: string) => {
  if (!value) {
    return Promise.reject(new Error('请再次输入密码'))
  }
  if (value !== formState.userPassword) {
    return Promise.reject(new Error('两次输入的密码不一致'))
  }
  return Promise.resolve()
}

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: API.UserRegisterRequest) => {
  submitting.value = true
  try {
    const res = await userRegister(values)
    if (res.data.code === 0 && res.data.data) {
      message.success('注册成功，请登录')
      await router.replace('/user/login')
    } else {
      message.error('注册失败，' + (res.data.message ?? '请稍后重试'))
    }
  } catch {
    message.error('注册失败，请检查网络或稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <AuthFormShell title="CodeLess - 用户注册">
    <a-form :model="formState" name="basic" autocomplete="off" @finish="handleSubmit">
      <a-form-item
        name="userAccount"
        :rules="[
          { required: true, message: '请输入账号' },
          { min: 4, message: '账号不能小于 4 位' },
        ]"
      >
        <a-input v-model:value="formState.userAccount" placeholder="请输入账号" />
      </a-form-item>
      <a-form-item
        name="userPassword"
        :rules="[
          { required: true, message: '请输入密码' },
          { min: 8, message: '密码不能小于 8 位' },
        ]"
      >
        <a-input-password v-model:value="formState.userPassword" placeholder="请输入密码" />
      </a-form-item>
      <a-form-item
        name="checkPassword"
        :rules="[{ required: true, validator: validateCheckPassword, trigger: 'change' }]"
      >
        <a-input-password v-model:value="formState.checkPassword" placeholder="请再次输入密码" />
      </a-form-item>
      <div class="tips">
        已有账号？
        <RouterLink to="/user/login">去登录</RouterLink>
      </div>
      <a-form-item>
        <a-button type="primary" html-type="submit" :loading="submitting" style="width: 100%">
          注册
        </a-button>
      </a-form-item>
    </a-form>
  </AuthFormShell>
</template>
