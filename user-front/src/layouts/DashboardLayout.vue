<script setup>
import { computed } from 'vue'
import BrandLogo from '../components/BrandLogo.vue'

const props = defineProps({
  user: {
    type: Object,
    default: () => ({}),
  },
  activeModule: {
    type: String,
    default: 'home',
  },
})

const emit = defineEmits(['change-module', 'logout'])

const moduleTitles = {
  home: {
    title: '学习中心',
    subtitle: '系统化学习护理专业知识与技能，提升护理服务能力',
  },
  courses: {
    title: '我的课程',
    subtitle: '查看当前可学习课程，按学习状态和课程性质快速定位',
  },
  records: {
    title: '学习记录',
    subtitle: '查看近期学习轨迹和课程完成情况',
  },
  profile: {
    title: '个人中心',
    subtitle: '管理个人资料和账号信息',
  },
  courseDetail: {
    title: '课程详情',
    subtitle: '首页 > 我的课程 > 课程详情',
  },
  courseLearning: {
    title: '课程学习',
    subtitle: '首页 > 我的课程 > 课程详情 > 课程学习',
  },
}

const pageMeta = computed(() => moduleTitles[props.activeModule] || moduleTitles.home)
const showBreadcrumb = computed(() => ['courseDetail', 'courseLearning'].includes(props.activeModule))
</script>

<template>
  <section class="dashboard-page">
    <aside class="side-nav">
      <div class="side-brand">
        <BrandLogo class="small" />
        <strong>护理培训系统</strong>
      </div>

      <nav>
        <button :class="{ active: activeModule === 'home' }" type="button" @click="emit('change-module', 'home')">
          <svg viewBox="0 0 24 24"><path d="m3 11 9-8 9 8v10h-6v-6H9v6H3V11Z" /></svg>
          <span>首页</span>
        </button>
        <button :class="{ active: activeModule === 'courses' }" type="button" @click="emit('change-module', 'courses')">
          <svg viewBox="0 0 24 24"><path d="M6 3h11a3 3 0 0 1 3 3v15H7a3 3 0 0 1-3-3V5a2 2 0 0 1 2-2Zm1 14a1 1 0 0 0 0 2h11V6a1 1 0 0 0-1-1H7v12Zm2-9h6v2H9V8Z" /></svg>
          <span>我的课程</span>
        </button>
        <button :class="{ active: activeModule === 'records' }" type="button" @click="emit('change-module', 'records')">
          <svg viewBox="0 0 24 24"><path d="M7 2h2v3h6V2h2v3h3v16H4V5h3V2Zm11 8H6v9h12v-9Zm-9 2h3v3H9v-3Zm5 0h3v3h-3v-3Z" /></svg>
          <span>学习记录</span>
        </button>
        <button :class="{ active: activeModule === 'profile' }" type="button" @click="emit('change-module', 'profile')">
          <svg viewBox="0 0 24 24"><path d="M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm0 2c-4 0-7 2-7 4.5V21h14v-2.5C19 16 16 14 12 14Z" /></svg>
          <span>个人中心</span>
        </button>
      </nav>
    </aside>

    <section class="main-panel">
      <header class="top-bar">
        <div class="top-title">
          <h1 v-if="activeModule !== 'courseLearning'">{{ pageMeta.title }}</h1>
          <p v-if="!showBreadcrumb">{{ pageMeta.subtitle }}</p>
          <p v-else class="top-breadcrumb">
            <button type="button" @click="emit('change-module', 'home')">首页</button>
            <span>></span>
            <button type="button" @click="emit('change-module', 'courses')">我的课程</button>
            <span>></span>
            <button
              v-if="activeModule === 'courseLearning'"
              type="button"
              @click="emit('change-module', 'courseDetail')"
            >
              课程详情
            </button>
            <strong v-else>课程详情</strong>
            <template v-if="activeModule === 'courseLearning'">
              <span>></span>
              <strong>课程学习</strong>
            </template>
          </p>
        </div>

        <div class="top-actions">
          <label class="search-box">
            <input placeholder="搜索课程、资源、专题" />
            <svg viewBox="0 0 24 24"><path d="m20 18.6-4.7-4.7A7 7 0 1 0 13.9 15.3l4.7 4.7 1.4-1.4ZM5 10a5 5 0 1 1 10 0 5 5 0 0 1-10 0Z" /></svg>
          </label>
          <div class="user-box">
            <span class="avatar">{{ (user.nickname || user.username || '学').slice(0, 1) }}</span>
            <strong>{{ user.nickname || user.username || '学员' }}</strong>
            <small>{{ user.role || '学员' }}</small>
            <svg viewBox="0 0 24 24"><path d="m7 9 5 5 5-5H7Z" /></svg>
            <button class="logout-button" type="button" @click="emit('logout')">退出</button>
          </div>
        </div>
      </header>

      <slot />
    </section>
  </section>
</template>
