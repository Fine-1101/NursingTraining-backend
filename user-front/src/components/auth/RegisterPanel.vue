<script setup>
import { reactive, ref } from 'vue'

const emit = defineEmits(['next', 'login'])

const form = reactive({
  username: '',
  realName: '',
  password: '',
  confirmPassword: '',
  deptId: '',
})

const showPassword = ref(false)
const showConfirmPassword = ref(false)
const errorMessage = ref('')

function submitNext() {
  errorMessage.value = ''

  if (form.password !== form.confirmPassword) {
    errorMessage.value = '两次输入的密码不一致'
    return
  }

  emit('next', {
    username: form.username,
    realName: form.realName,
    password: form.password,
    deptId: Number(form.deptId),
  })
}
</script>

<template>
  <div class="register-card-content">
    <div class="register-heading">
      <span class="round-icon">
        <svg viewBox="0 0 24 24"><path d="M7 3h10v2h3v16H4V5h3V3Zm2 2h6V3H9v2Zm0 5h6V8H9v2Zm0 4h8v-2H9v2Zm0 4h5v-2H9v2Z" /></svg>
      </span>
      <h1>注册账号</h1>
      <p>请填写基本信息，创建您的学习账号</p>
    </div>

    <form class="register-form" @submit.prevent="submitNext">
      <label>
        <span>用户名 <em>*</em></span>
        <input v-model.trim="form.username" placeholder="请输入用户名，6-20位字母、数字或下划线" required />
      </label>
      <label>
        <span>真实姓名 <em>*</em></span>
        <input v-model.trim="form.realName" placeholder="请输入您的真实姓名" required />
      </label>
      <label>
        <span>密码 <em>*</em></span>
        <div class="password-field">
          <input v-model="form.password" :type="showPassword ? 'text' : 'password'" placeholder="请输入密码，8-20位字母、数字和符号组合" required />
          <button class="icon-button" type="button" title="显示或隐藏密码" @click="showPassword = !showPassword">
            <svg viewBox="0 0 24 24"><path d="M12 5c5.5 0 9 5.2 9 7s-3.5 7-9 7-9-5.2-9-7 3.5-7 9-7Zm0 2c-4 0-6.8 3.8-7 5 .2 1.2 3 5 7 5s6.8-3.8 7-5c-.2-1.2-3-5-7-5Zm0 2.5A2.5 2.5 0 1 1 12 14a2.5 2.5 0 0 1 0-5Z" /></svg>
          </button>
        </div>
      </label>
      <label>
        <span>确认密码 <em>*</em></span>
        <div class="password-field">
          <input v-model="form.confirmPassword" :type="showConfirmPassword ? 'text' : 'password'" placeholder="请再次输入密码" required />
          <button class="icon-button" type="button" title="显示或隐藏密码" @click="showConfirmPassword = !showConfirmPassword">
            <svg viewBox="0 0 24 24"><path d="M12 5c5.5 0 9 5.2 9 7s-3.5 7-9 7-9-5.2-9-7 3.5-7 9-7Zm0 2c-4 0-6.8 3.8-7 5 .2 1.2 3 5 7 5s6.8-3.8 7-5c-.2-1.2-3-5-7-5Zm0 2.5A2.5 2.5 0 1 1 12 14a2.5 2.5 0 0 1 0-5Z" /></svg>
          </button>
        </div>
      </label>
      <label>
        <span>科室ID <em>*</em></span>
        <input v-model.number="form.deptId" type="number" min="1" placeholder="请输入科室ID" required />
      </label>

      <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>
      <button class="primary-button wide-button" type="submit">下一步</button>
      <p class="signin-link">已有账号，<button type="button" @click="emit('login')">去登录</button></p>
    </form>
  </div>
</template>
