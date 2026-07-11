<script setup>
import { computed, onMounted, ref } from 'vue'
import { getLearnerHome } from '../../api/learnerHome'
import CourseCard from './CourseCard.vue'
import LearningCalendar from './LearningCalendar.vue'
import ProgressOverview from './ProgressOverview.vue'
import RecentRecords from './RecentRecords.vue'

const loading = ref(false)
const errorMessage = ref('')
const homeData = ref(null)

const stats = computed(() => homeData.value?.courseStats || {
  allCount: 0,
  recommendedCount: 0,
  completedCount: 0,
  learningCount: 0,
  notStartedCount: 0,
})

const statTabs = computed(() => [
  { label: '全部课程', value: stats.value.allCount, active: true },
  { label: '推荐课程', value: stats.value.recommendedCount },
  { label: '继续学习', value: stats.value.learningCount },
  { label: '已完成', value: stats.value.completedCount },
  { label: '未开始', value: stats.value.notStartedCount },
])

async function loadHome() {
  loading.value = true
  errorMessage.value = ''

  try {
    homeData.value = await getLearnerHome()
  } catch (error) {
    errorMessage.value = error.message || '首页数据加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadHome)
</script>

<template>
  <div class="home-content">
    <section class="stats-card">
      <button v-for="item in statTabs" :key="item.label" class="stat-tab" :class="{ active: item.active }" type="button">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </button>
    </section>

    <div class="home-grid">
      <div class="home-main">
        <section class="home-section">
          <div class="section-title">
            <h2>推荐课程</h2>
            <a href="/">查看全部</a>
          </div>
          <div v-if="homeData?.recommendedCourses?.length" class="course-grid">
            <CourseCard v-for="course in homeData.recommendedCourses" :key="course.courseId" :course="course" />
          </div>
          <p v-else class="empty-text">暂无推荐课程</p>
        </section>

        <section class="home-section">
          <div class="section-title">
            <h2>继续学习</h2>
            <a href="/">查看全部</a>
          </div>
          <div v-if="homeData?.continueCourses?.length" class="continue-grid">
            <CourseCard v-for="course in homeData.continueCourses" :key="course.courseId" :course="course" compact />
          </div>
          <p v-else class="empty-text">暂无继续学习课程</p>
        </section>
      </div>

      <aside class="home-aside">
        <LearningCalendar :calendar="homeData?.calendar" />
        <ProgressOverview :overview="homeData?.progressOverview" />
        <RecentRecords :records="homeData?.recentRecords || []" />
      </aside>
    </div>
  </div>
</template>
