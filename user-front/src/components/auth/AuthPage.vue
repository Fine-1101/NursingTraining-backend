<script setup>
import BrandLogo from '../BrandLogo.vue'
import LoginPanel from './LoginPanel.vue'
import RegisterPanel from './RegisterPanel.vue'
import RoleSelectPanel from './RoleSelectPanel.vue'

defineProps({
  view: {
    type: String,
    required: true,
  },
  selectedRole: {
    type: String,
    default: '',
  },
  registerLoading: {
    type: Boolean,
    default: false,
  },
  registerError: {
    type: String,
    default: '',
  },
})

const emit = defineEmits([
  'login-success',
  'show-register',
  'show-login',
  'register-next',
  'select-role',
  'register-complete',
])
</script>

<template>
  <section class="auth-page">
    <header class="brand-header">
      <BrandLogo />
      <div>
        <strong>护理培训系统</strong>
        <span>专业 · 规范 · 高效 · 成长</span>
      </div>
    </header>

    <div class="auth-content">
      <section class="login-layout">
        <aside class="intro-panel" aria-label="绿色背景留白"></aside>

        <section class="auth-card">
          <Transition name="card-swap" mode="out-in">
            <LoginPanel
              v-if="view === 'login'"
              key="login"
              @login-success="emit('login-success', $event)"
              @register="emit('show-register')"
            />
            <RegisterPanel
              v-else-if="view === 'registerInfo'"
              key="register-info"
              @next="emit('register-next', $event)"
              @login="emit('show-login')"
            />
            <RoleSelectPanel
              v-else
              key="register-role"
              :selected-role="selectedRole"
              :loading="registerLoading"
              :error-message="registerError"
              @select-role="emit('select-role', $event)"
              @back="emit('show-register')"
              @complete="emit('register-complete')"
              @login="emit('show-login')"
            />
          </Transition>
        </section>
      </section>
    </div>

    <footer class="help-footer">
      <span>
        <svg viewBox="0 0 24 24"><path d="M12 3a8 8 0 0 0-8 8v5a3 3 0 0 0 3 3h2v-8H6a6 6 0 0 1 12 0h-3v8h2a5 5 0 0 1-5 3h-2v-2h2a3 3 0 0 0 2.8-2H17a3 3 0 0 0 3-3v-4a8 8 0 0 0-8-8Z" /></svg>
        需要帮助?
      </span>
      <span>
        <svg viewBox="0 0 24 24"><path d="M7 2h4l2 5-3 2a12 12 0 0 0 5 5l2-3 5 2v4c0 2-2 4-4 4A16 16 0 0 1 3 6c0-2 2-4 4-4Z" /></svg>
        400-888-1234
      </span>
      <span>周一至周日 9:00-18:00</span>
    </footer>
  </section>
</template>
