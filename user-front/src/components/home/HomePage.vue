<script setup>
import { onMounted, ref } from 'vue'
import { getLearnerHome } from '../../api/learnerHome'
import CourseCard from './CourseCard.vue'
import LearningCalendar from './LearningCalendar.vue'
import ProgressOverview from './ProgressOverview.vue'
import RecentRecords from './RecentRecords.vue'

const emit = defineEmits(['open-detail', 'start-learning'])

const loading = ref(false)
const errorMessage = ref('')
const homeData = ref(null)

function openDetail(course) {
  emit('open-detail', course.courseId)
}

function startLearning(course) {
  emit('start-learning', {
    courseId: course.courseId,
    pointId: course.nextPointId || course.lastPointId || course.pointId || null,
  })
}

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
    <div class="home-grid">
      <div class="home-main">
        <section class="home-section">
          <div class="section-title">
            <h2>推荐课程</h2>
            <a href="/">查看全部</a>
          </div>
          <div v-if="homeData?.recommendedCourses?.length" class="course-grid">
            <CourseCard
              v-for="course in homeData.recommendedCourses"
              :key="course.courseId"
              :course="course"
              @open-detail="openDetail(course)"
              @start-learning="startLearning(course)"
            />
          </div>
          <p v-else class="empty-text">暂无推荐课程</p>
        </section>

        <section class="home-section">
          <div class="section-title">
            <h2>继续学习</h2>
            <a href="/">查看全部</a>
          </div>
          <div v-if="homeData?.continueCourses?.length" class="continue-grid">
            <CourseCard
              v-for="course in homeData.continueCourses"
              :key="course.courseId"
              :course="course"
              compact
              @open-detail="openDetail(course)"
              @start-learning="startLearning(course)"
            />
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
