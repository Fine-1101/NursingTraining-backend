<script setup>
import { ref } from 'vue'
import AuthPage from './components/auth/AuthPage.vue'
import HomePage from './components/home/HomePage.vue'
import MyCoursesPage from './components/courses/MyCoursesPage.vue'
import CourseDetailPage from './components/courses/CourseDetailPage.vue'
import CourseLearningPage from './components/courses/CourseLearningPage.vue'
import LearningRecordsPage from './components/records/LearningRecordsPage.vue'
import DashboardLayout from './layouts/DashboardLayout.vue'
import { getStoredUser } from './api/request'
import { logout, register } from './api/auth'
import './styles/app.css'

const page = ref(getStoredUser().username ? 'dashboard' : 'auth')
const activeModule = ref('home')
const selectedCourseId = ref(null)
const selectedPointId = ref(null)
const authView = ref('login')
const selectedRole = ref('')
const registerDraft = ref({})
const registerLoading = ref(false)
const registerError = ref('')
const currentUser = ref(getStoredUser())

function showLogin() {
  authView.value = 'login'
}

function showRegister() {
  authView.value = 'registerInfo'
}

function goRoleSelect(form) {
  registerDraft.value = form
  registerError.value = ''
  authView.value = 'registerRole'
}

async function completeRegister() {
  registerError.value = ''

  if (!selectedRole.value) {
    selectedRole.value = 'student'
  }

  registerLoading.value = true

  try {
    const roleType = selectedRole.value === 'teacher' ? 2 : 1
    const session = await register({
      username: registerDraft.value.username,
      password: registerDraft.value.password,
      realName: registerDraft.value.realName,
      deptId: registerDraft.value.deptId,
      roleType,
    })

    currentUser.value = session.user || getStoredUser()
    page.value = 'dashboard'
  } catch (error) {
    registerError.value = error.message || '注册失败，请检查填写信息'
  } finally {
    registerLoading.value = false
  }
}

function handleLoginSuccess(user) {
  currentUser.value = user || getStoredUser()
  page.value = 'dashboard'
}

function openCourseDetail(courseId) {
  selectedCourseId.value = courseId
  activeModule.value = 'courseDetail'
}

function openCourseLearning(payload) {
  selectedCourseId.value = payload?.courseId || selectedCourseId.value
  selectedPointId.value = payload?.pointId || null
  activeModule.value = 'courseLearning'
}

function backToCourses() {
  activeModule.value = 'courses'
}

function backToCourseDetail() {
  activeModule.value = selectedCourseId.value ? 'courseDetail' : 'courses'
}

async function handleLogout() {
  await logout()
  currentUser.value = {}
  selectedCourseId.value = null
  selectedPointId.value = null
  activeModule.value = 'home'
  authView.value = 'login'
  page.value = 'auth'
}
</script>

<template>
  <main class="app-shell">
    <Transition name="page-fade" mode="out-in">
      <AuthPage
        v-if="page === 'auth'"
        :view="authView"
        :selected-role="selectedRole"
        :register-loading="registerLoading"
        :register-error="registerError"
        @login-success="handleLoginSuccess"
        @show-register="showRegister"
        @show-login="showLogin"
        @register-next="goRoleSelect"
        @select-role="selectedRole = $event"
        @register-complete="completeRegister"
      />

      <DashboardLayout
        v-else
        :user="currentUser"
        :active-module="activeModule"
        @change-module="activeModule = $event"
        @logout="handleLogout"
      >
        <HomePage
          v-if="activeModule === 'home'"
          @open-detail="openCourseDetail"
          @start-learning="openCourseLearning"
        />
        <MyCoursesPage
          v-else-if="activeModule === 'courses'"
          @open-detail="openCourseDetail"
          @start-learning="openCourseLearning"
        />
        <CourseDetailPage
          v-else-if="activeModule === 'courseDetail' && selectedCourseId"
          :course-id="selectedCourseId"
          @back="backToCourses"
          @start-learning="openCourseLearning"
        />
        <CourseLearningPage
          v-else-if="activeModule === 'courseLearning' && selectedCourseId"
          :course-id="selectedCourseId"
          :point-id="selectedPointId"
          @back="backToCourseDetail"
        />
        <LearningRecordsPage v-else-if="activeModule === 'records'" />
        <div v-else class="module-placeholder">
          <h1>{{ activeModule === 'records' ? '学习记录' : '个人中心' }}</h1>
          <p>模块待开发</p>
        </div>
      </DashboardLayout>
    </Transition>
  </main>
</template>
