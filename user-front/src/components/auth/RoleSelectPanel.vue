<script setup>
const props = defineProps({
  selectedRole: {
    type: String,
    default: '',
  },
  loading: {
    type: Boolean,
    default: false,
  },
  errorMessage: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['select-role', 'back', 'complete', 'login'])
</script>

<template>
  <div class="role-card-content">
    <div class="role-heading">
      <span class="round-icon">
        <svg viewBox="0 0 24 24"><path d="M5 4h14v16H5V4Zm2 2v12h10V6H7Zm3 2h4v2h-4V8Zm-1 7c.4-1.5 1.6-2.5 3-2.5s2.6 1 3 2.5H9Zm3-3a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3Z" /></svg>
      </span>
      <h1>选择身份</h1>
      <p>请选择您的身份，以便我们为您提供更精准的学习体验</p>
    </div>

    <div class="role-options">
      <button class="role-card" :class="{ selected: props.selectedRole === 'student' }" type="button" @click="emit('select-role', 'student')">
        <span class="role-icon">
          <svg viewBox="0 0 64 64"><path d="M32 8 56 20 32 32 8 20 32 8Zm-14 19 14 7 14-7v13c-4 4-10 6-14 6s-10-2-14-6V27Zm-6-2 4 2v12l-4 5V25Z" /></svg>
        </span>
        <strong>学生</strong>
        <span>我是护理专业学生<br />需要学习专业知识与技能</span>
      </button>
      <button class="role-card" :class="{ selected: props.selectedRole === 'teacher' }" type="button" @click="emit('select-role', 'teacher')">
        <span class="role-icon">
          <svg viewBox="0 0 64 64"><path d="M22 28a10 10 0 1 0 0-20 10 10 0 0 0 0 20Zm-16 24c1-11 7-18 16-18s15 7 16 18H6Zm32-40h20v28H44v-4h10V16H38v-4Zm-1 19 8-8 3 3-8 8 8 8-3 3-8-8v-6Z" /></svg>
        </span>
        <strong>讲师</strong>
        <span>我是护理专业讲师<br />需要教学管理与课程资源</span>
      </button>
    </div>

    <div class="role-actions">
      <button class="back-button" type="button" @click="emit('back')">
        <svg viewBox="0 0 24 24"><path d="m10 6 1.4 1.4L7.8 11H20v2H7.8l3.6 3.6L10 18l-6-6 6-6Z" /></svg>
        上一步
      </button>
      <button class="primary-button register-button" type="button" :disabled="loading" @click="emit('complete')">
        {{ loading ? '注册中' : '注册' }}
        <svg viewBox="0 0 24 24"><path d="m14 6-1.4 1.4L16.2 11H4v2h12.2l-3.6 3.6L14 18l6-6-6-6Z" /></svg>
      </button>
    </div>

    <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>
    <p class="signin-link bottom-link">已有账号，<button type="button" @click="emit('login')">去登录</button></p>
  </div>
</template>
