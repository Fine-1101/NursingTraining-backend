<script setup>
import { reactive, ref } from 'vue'
import BrandLogo from '../BrandLogo.vue'
import { login } from '../../api/auth'

const emit = defineEmits(['login-success', 'register'])

const form = reactive({
  username: '',
  password: '',
  remember: false,
})

const showPassword = ref(false)
const loading = ref(false)
const errorMessage = ref('')

async function submitLogin() {
  errorMessage.value = ''
  loading.value = true

  try {
    const session = await login(form.username, form.password)
    emit('login-success', session.user)
  } catch (error) {
    errorMessage.value = error.message || '登录失败，请检查账号和密码'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-card-content">
    <div class="card-title">
      <BrandLogo class="card-logo" />
      <h2>护理培训系统</h2>
      <p><span></span>学员登录<span></span></p>
    </div>

    <form class="login-form" @submit.prevent="submitLogin">
      <label class="input-row">
        <svg viewBox="0 0 24 24"><path d="M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm0 2c-4 0-7 2-7 4.5V20h14v-1.5C19 16 16 14 12 14Z" /></svg>
        <input v-model.trim="form.username" autocomplete="username" placeholder="用户名" required />
      </label>

      <label class="input-row">
        <svg viewBox="0 0 24 24"><path d="M17 9V7A5 5 0 0 0 7 7v2H5v12h14V9h-2Zm-8 0V7a3 3 0 0 1 6 0v2H9Z" /></svg>
        <input v-model="form.password" :type="showPassword ? 'text' : 'password'" autocomplete="current-password" placeholder="请输入密码" required />
        <button class="icon-button" type="button" title="显示或隐藏密码" @click="showPassword = !showPassword">
          <svg viewBox="0 0 24 24"><path d="M12 5c5.5 0 9 5.2 9 7s-3.5 7-9 7-9-5.2-9-7 3.5-7 9-7Zm0 2c-4 0-6.8 3.8-7 5 .2 1.2 3 5 7 5s6.8-3.8 7-5c-.2-1.2-3-5-7-5Zm0 2.5A2.5 2.5 0 1 1 12 14a2.5 2.5 0 0 1 0-5Z" /></svg>
        </button>
      </label>

      <div class="login-options">
        <label>
          <input v-model="form.remember" type="checkbox" />
          记住我
        </label>
        <a href="/">忘记密码?</a>
      </div>

      <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>
      <button class="primary-button" type="submit" :disabled="loading">{{ loading ? '登录中...' : '登录' }}</button>
      <button class="secondary-button" type="button" @click="emit('register')">注册账号</button>
      <p class="policy-text">登录即代表您已阅读并同意 <a href="/">《用户协议》</a> 与 <a href="/">《隐私政策》</a></p>
    </form>
  </div>
</template>
