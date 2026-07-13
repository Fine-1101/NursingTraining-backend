<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { getLearningRecordOverview, getLearningRecords, getLearningTopCourses } from '../../api/learnerLearningRecords'
import FrequencyTrendChart from './FrequencyTrendChart.vue'
import LearningTimeline from './LearningTimeline.vue'
import ResourceRoseChart from './ResourceRoseChart.vue'
import TopCoursesChart from './TopCoursesChart.vue'

const ranges = [
  { label: '今日', value: 'TODAY' },
  { label: '近七日', value: 'LAST_7_DAYS' },
  { label: '近一月', value: 'LAST_30_DAYS' },
]

const activeRange = ref('TODAY')
const loading = ref(false)
const errorMessage = ref('')
const overview = ref({
  summaryCards: {},
  resourceDistribution: [],
  frequencyTrend: { points: [] },
  topCourses: [],
})
const listData = reactive({
  records: [],
  total: 0,
  page: 1,
  size: 10,
  pages: 0,
})
const topCoursePage = ref([])

const periodText = computed(() => ranges.find((item) => item.value === activeRange.value)?.label || '今日')

async function loadRecords() {
  loading.value = true
  errorMessage.value = ''

  try {
    const [overviewData, recordsData, topData] = await Promise.all([
      getLearningRecordOverview({ range: activeRange.value }),
      getLearningRecords({ range: activeRange.value, page: listData.page, size: listData.size }),
      getLearningTopCourses({ range: activeRange.value, page: 1, size: 8 }),
    ])

    overview.value = {
      summaryCards: overviewData.summaryCards || {},
      resourceDistribution: overviewData.resourceDistribution || [],
      frequencyTrend: overviewData.frequencyTrend || { points: [] },
      topCourses: overviewData.topCourses || [],
    }

    Object.assign(listData, {
      records: recordsData.records || [],
      total: recordsData.total || 0,
      page: recordsData.page || 1,
      size: recordsData.size || listData.size,
      pages: recordsData.pages || 0,
    })

    topCoursePage.value = topData.records || overview.value.topCourses || []
  } catch (error) {
    errorMessage.value = error.message || '学习记录加载失败'
    overview.value = {
      summaryCards: {},
      resourceDistribution: [],
      frequencyTrend: { points: [] },
      topCourses: [],
    }
    Object.assign(listData, {
      records: [],
      total: 0,
      page: 1,
      pages: 0,
    })
    topCoursePage.value = []
  } finally {
    loading.value = false
  }
}

function changeRange(range) {
  if (activeRange.value === range) return
  activeRange.value = range
  listData.page = 1
  loadRecords()
}

onMounted(loadRecords)
</script>

<template>
  <div class="learning-records-page">
    <div class="record-range-tabs">
      <button
        v-for="range in ranges"
        :key="range.value"
        type="button"
        :class="{ active: activeRange === range.value }"
        @click="changeRange(range.value)"
      >
        {{ range.label }}
      </button>
    </div>

    <section v-if="errorMessage" class="records-inline-error">
      <div>
        <strong>学习记录接口暂不可用</strong>
        <span>{{ errorMessage }}</span>
      </div>
      <button type="button" @click="loadRecords">重试</button>
    </section>

    <div class="records-layout">
      <div class="records-main">
        <section class="records-card timeline-card-panel">
          <div class="records-section-title">
            <h2>学习历史记录</h2>
            <span>{{ loading ? '加载中...' : `共 ${listData.total} 条` }}</span>
          </div>
          <p v-if="loading" class="empty-text">学习记录加载中...</p>
          <LearningTimeline v-else :records="listData.records" />
        </section>

        <div class="records-chart-grid">
          <section class="records-card">
            <div class="records-section-title">
              <h2>学习课件分布</h2>
              <span>{{ periodText }}</span>
            </div>
            <ResourceRoseChart :items="overview.resourceDistribution" />
          </section>

          <section class="records-card">
            <div class="records-section-title">
              <h2>学习频率趋势</h2>
              <span>平均 {{ overview.frequencyTrend?.averageCount || 0 }} 次</span>
            </div>
            <FrequencyTrendChart :trend="overview.frequencyTrend" />
          </section>
        </div>
      </div>

      <aside class="records-card top-course-panel">
        <div class="records-section-title">
          <h2>学习最多的课程</h2>
          <span>{{ periodText }}</span>
        </div>
        <TopCoursesChart :courses="topCoursePage" />
        <div v-if="topCoursePage.length" class="top-course-list">
          <article v-for="course in topCoursePage" :key="course.courseId">
            <strong>{{ course.rank }}</strong>
            <div>
              <h3>{{ course.courseTitle }}</h3>
              <i :style="{ width: `${course.barPercent || 0}%` }"></i>
            </div>
            <span>{{ course.totalDurationHours || 0 }}</span>
          </article>
        </div>
        <p v-else class="empty-text">暂无课程排行</p>
      </aside>
    </div>
  </div>
</template>
