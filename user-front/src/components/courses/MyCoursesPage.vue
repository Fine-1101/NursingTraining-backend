<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { getLearnerCourses, getLearnerCourseStats } from '../../api/learnerCourses'

const learningTabs = [
  { label: '全部', value: 'ALL', countKey: 'allCount' },
  { label: '未开始', value: 'NOT_STARTED', countKey: 'notStartedCount' },
  { label: '进行中', value: 'LEARNING', countKey: 'learningCount' },
  { label: '已完成', value: 'COMPLETED', countKey: 'completedCount' },
]

const courseTypeTabs = [
  { label: '全部性质', value: 'ALL', countKey: 'allCount' },
  { label: '必修课程', value: 'REQUIRED', countKey: 'requiredCount' },
  { label: '选修课程', value: 'OPTIONAL', countKey: 'optionalCount' },
]

const filters = reactive({
  learningStatus: 'ALL',
  courseType: 'ALL',
  keyword: '',
  page: 1,
  size: 10,
})

const stats = ref({
  allCount: 0,
  notStartedCount: 0,
  learningCount: 0,
  completedCount: 0,
  requiredCount: 0,
  optionalCount: 0,
})
const pageData = ref({
  records: [],
  total: 0,
  page: 1,
  size: 10,
  pages: 0,
})
const loading = ref(false)

const paginationText = computed(() => {
  if (!pageData.value.total) return '共 0 门课程'
  return `第 ${pageData.value.page} / ${pageData.value.pages || 1} 页，共 ${pageData.value.total} 门课程`
})

function progressPercent(course) {
  return Math.round(Number(course.progressPercent || 0))
}

function statusText(status) {
  const map = {
    NOT_STARTED: '未开始',
    LEARNING: '进行中',
    COMPLETED: '已完成',
  }
  return map[status] || status
}

function typeText(type) {
  return type === 'REQUIRED' ? '必修' : '选修'
}

async function loadStats() {
  stats.value = await getLearnerCourseStats()
}

async function loadCourses() {
  loading.value = true

  try {
    pageData.value = await getLearnerCourses({
      learningStatus: filters.learningStatus,
      courseType: filters.courseType,
      keyword: filters.keyword.trim(),
      page: filters.page,
      size: filters.size,
    })
  } finally {
    loading.value = false
  }
}

async function refreshAll() {
  await Promise.all([loadStats(), loadCourses()])
}

function changeLearningStatus(value) {
  filters.learningStatus = value
  filters.page = 1
  loadCourses()
}

function changeCourseType(value) {
  filters.courseType = value
  filters.page = 1
  loadCourses()
}

function searchCourses() {
  filters.page = 1
  loadCourses()
}

function changePage(nextPage) {
  if (nextPage < 1 || nextPage > (pageData.value.pages || 1)) return
  filters.page = nextPage
  loadCourses()
}

onMounted(refreshAll)
</script>

<template>
  <div class="my-courses-page">
    <section class="course-filter-card">
      <div class="filter-row">
        <label class="course-search">
          <input v-model="filters.keyword" placeholder="搜索课程名称或讲师姓名" @keyup.enter="searchCourses" />
          <button type="button" @click="searchCourses">
            <svg viewBox="0 0 24 24"><path d="m20 18.6-4.7-4.7A7 7 0 1 0 13.9 15.3l4.7 4.7 1.4-1.4ZM5 10a5 5 0 1 1 10 0 5 5 0 0 1-10 0Z" /></svg>
          </button>
        </label>
      </div>

      <div class="filter-line">
        <span class="filter-label">性质：</span>
        <button
          v-for="tab in courseTypeTabs"
          :key="tab.value"
          type="button"
          :class="{ active: filters.courseType === tab.value }"
          @click="changeCourseType(tab.value)"
        >
          {{ tab.label }} <strong>{{ stats[tab.countKey] }}</strong>
        </button>
      </div>

      <div class="filter-line">
        <span class="filter-label">进度：</span>
        <button
          v-for="tab in learningTabs"
          :key="tab.value"
          type="button"
          :class="{ active: filters.learningStatus === tab.value }"
          @click="changeLearningStatus(tab.value)"
        >
          {{ tab.label }} <strong>{{ stats[tab.countKey] }}</strong>
        </button>
      </div>
    </section>

    <section class="course-list-card">
      <div class="course-list-head">
        <h2>课程列表</h2>
        <span>{{ paginationText }}</span>
      </div>

      <div v-if="pageData.records.length" class="my-course-list">
        <article v-for="course in pageData.records" :key="course.courseId" class="my-course-item">
          <div class="my-course-cover">
            <img v-if="course.coverUrl" :src="course.coverUrl" :alt="course.title" />
            <svg v-else viewBox="0 0 80 80"><path d="M14 18h52v44H14V18Zm12 12v20l16-10-16-10Zm24 2h8v4h-8v-4Zm0 10h8v4h-8v-4Z" /></svg>
          </div>

          <div class="my-course-info">
            <div class="course-title-line">
              <h3>{{ course.title }}</h3>
              <span :class="['course-type-badge', course.courseType === 'REQUIRED' ? 'required' : 'optional']">
                {{ typeText(course.courseType) }}
              </span>
              <span class="course-status-badge">{{ statusText(course.learningStatus) }}</span>
            </div>
            <p>{{ course.summary || '暂无课程简介' }}</p>
            <div class="my-course-meta">
              <span>{{ course.instructorName || '未设置讲师' }}</span>
              <span>{{ course.categoryName || '未分类' }}</span>
              <span>{{ course.completedPointCount || 0 }}/{{ course.pointCount || 0 }} 课程点</span>
              <span v-if="course.lastLearnedAt">最近学习：{{ course.lastLearnedAt }}</span>
            </div>
            <div class="my-course-progress">
              <div class="progress-line">
                <i :style="{ width: `${progressPercent(course)}%` }"></i>
              </div>
              <strong>{{ progressPercent(course) }}%</strong>
            </div>
          </div>

          <button class="course-enter-button" type="button">{{ course.buttonText }}</button>
        </article>
      </div>

      <p v-else class="empty-text">暂无符合条件的课程</p>

      <div class="pagination-bar">
        <button type="button" :disabled="pageData.page <= 1 || loading" @click="changePage(pageData.page - 1)">上一页</button>
        <span>{{ paginationText }}</span>
        <button type="button" :disabled="pageData.page >= (pageData.pages || 1) || loading" @click="changePage(pageData.page + 1)">下一页</button>
      </div>
    </section>
  </div>
</template>
