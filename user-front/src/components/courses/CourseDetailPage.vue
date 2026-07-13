<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { getLearnerCourseDetail } from '../../api/learnerCourses'

const props = defineProps({
  courseId: {
    type: [Number, String],
    required: true,
  },
})

const emit = defineEmits(['back', 'start-learning'])

const loading = ref(false)
const errorMessage = ref('')
const course = ref(null)
const expandedChapterIds = ref(new Set())

const progressPercent = computed(() => Math.round(Number(course.value?.progressPercent || 0)))
const courseTags = computed(() => {
  const tags = course.value?.tags || course.value?.tagList || []
  if (Array.isArray(tags)) {
    return tags.map((tag) => displayText(tag)).filter(Boolean)
  }
  return []
})

function resolveCoverUrl(coverUrl) {
  if (!coverUrl) return ''
  if (/^(https?:)?\/\//.test(coverUrl) || coverUrl.startsWith('data:')) return coverUrl

  const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
  if (coverUrl.startsWith('/')) return `${apiBase}${coverUrl}`

  return `${apiBase}/${coverUrl}`
}

function typeText(type) {
  return type === 'REQUIRED' ? '必修课程' : '选修课程'
}

function displayText(value) {
  if (value === null || value === undefined) return ''
  if (typeof value === 'number') return String(value)
  if (typeof value === 'string') {
    const text = value.trim()
    if (text.startsWith('{') && text.endsWith('}')) {
      try {
        const parsed = JSON.parse(text)
        return parsed.title || parsed.name || parsed.label || text
      } catch {
        return text
      }
    }
    return value
  }
  return value.title || value.name || value.label || ''
}

function statusText(status) {
  const map = {
    NOT_STARTED: '未开始',
    LEARNING: '学习中',
    COMPLETED: '已完成',
  }
  return map[status] || status || '未知'
}

function pointStatusClass(status) {
  if (status === 'COMPLETED') return 'completed'
  if (status === 'LEARNING') return 'learning'
  return 'not-started'
}

function toggleChapter(chapterId) {
  const next = new Set(expandedChapterIds.value)
  if (next.has(chapterId)) {
    next.delete(chapterId)
  } else {
    next.add(chapterId)
  }
  expandedChapterIds.value = next
}

function isChapterExpanded(chapter) {
  return expandedChapterIds.value.has(chapter.chapterId)
}

function handleMainAction() {
  if (!course.value?.nextPointId) return
  emit('start-learning', {
    courseId: course.value.courseId,
    pointId: course.value.nextPointId,
  })
}

function handlePointClick(point) {
  emit('start-learning', {
    courseId: course.value?.courseId,
    pointId: point.pointId,
  })
}

async function loadCourseDetail() {
  loading.value = true
  errorMessage.value = ''

  try {
    const detail = await getLearnerCourseDetail(props.courseId)
    course.value = detail
    expandedChapterIds.value = new Set(
      (detail.chapters || []).map((chapter) => chapter.chapterId),
    )
  } catch (error) {
    errorMessage.value = error.message || '课程详情加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadCourseDetail)
watch(() => props.courseId, loadCourseDetail)
</script>

<template>
  <div class="course-detail-page">
    <nav class="detail-breadcrumb">
      <button type="button" @click="emit('back')">我的课程</button>
      <span>/</span>
      <strong>课程详情</strong>
    </nav>

    <p v-if="loading" class="empty-text">课程详情加载中...</p>
    <p v-else-if="errorMessage" class="home-error">{{ errorMessage }}</p>

    <div v-else-if="course" class="course-detail-layout">
      <aside class="detail-left">
        <section class="detail-card course-summary-card">
          <div class="detail-cover">
            <img v-if="course.coverUrl" :src="resolveCoverUrl(course.coverUrl)" :alt="displayText(course.title)" />
            <svg v-else viewBox="0 0 96 96"><path d="M16 20h64v56H16V20Zm14 14v28l24-14-24-14Zm34 4h8v6h-8v-6Zm0 14h8v6h-8v-6Z" /></svg>
          </div>

          <div class="detail-course-main">
            <h2>{{ displayText(course.title) }}</h2>
            <div class="detail-badges">
              <span>{{ course.categoryName || '未分类' }}</span>
              <span>{{ typeText(course.courseType) }}</span>
              <span>{{ statusText(course.learningStatus) }}</span>
            </div>
            <div class="detail-progress">
              <div class="progress-line">
                <i :style="{ width: `${progressPercent}%` }"></i>
              </div>
              <strong>{{ progressPercent }}%</strong>
            </div>
            <p>{{ course.progressSummary?.displayText || `已完成 ${course.completedPointCount || 0} / ${course.pointCount || 0}` }}</p>
            <button class="detail-main-button" type="button" :disabled="!course.nextPointId" @click="handleMainAction">
              {{ course.nextPointId ? course.buttonText : '课程内容暂未配置' }}
            </button>
          </div>
        </section>

        <section class="detail-card">
          <h3>简介</h3>
          <p class="detail-summary">{{ course.summary || '暂无课程简介' }}</p>
        </section>

        <section class="detail-card">
          <h3>讲师</h3>
          <div class="teacher-card">
            <span class="teacher-avatar">{{ displayText(course.instructorName || course.teacherName || '讲').slice(0, 1) }}</span>
            <div>
              <strong>{{ displayText(course.instructorName || course.teacherName) || '暂无讲师信息' }}</strong>
              <p>{{ displayText(course.instructorTitle || course.teacherTitle) || '暂无职称信息' }}</p>
            </div>
          </div>
        </section>

        <section class="detail-card">
          <h3>课程标签</h3>
          <div v-if="courseTags.length" class="course-tag-list">
            <span v-for="tag in courseTags" :key="tag">{{ tag }}</span>
          </div>
          <p v-else class="detail-summary">暂无课程标签</p>
        </section>
      </aside>

      <section class="detail-right">
        <div class="detail-directory-head">
          <div>
            <h2>课程目录</h2>
            <p>{{ course.chapterCount }} 个章节 · {{ course.pointCount }} 个课程点</p>
          </div>
          <span>{{ course.progressSummary?.displayText }}</span>
        </div>

        <article v-for="chapter in course.chapters" :key="chapter.chapterId" class="chapter-card">
          <button class="chapter-head" type="button" @click="toggleChapter(chapter.chapterId)">
            <span class="chapter-icon">
              <svg viewBox="0 0 24 24"><path d="M5 4h14v16H5V4Zm3 4v2h8V8H8Zm0 4v2h8v-2H8Z" /></svg>
            </span>
            <strong>{{ displayText(chapter.title) }}</strong>
            <em>{{ chapter.completedPointCount }}/{{ chapter.pointCount }}</em>
            <svg class="chapter-arrow" :class="{ expanded: isChapterExpanded(chapter) }" viewBox="0 0 24 24"><path d="m7 10 5 5 5-5H7Z" /></svg>
          </button>

          <div v-if="isChapterExpanded(chapter)" class="point-list">
            <button
              v-for="point in chapter.points"
              :key="point.pointId"
              class="point-row"
              type="button"
              @click="handlePointClick(point)"
            >
              <span :class="['point-play', pointStatusClass(point.learningStatus)]">
                <svg viewBox="0 0 24 24"><path d="M8 5v14l11-7-11-7Z" /></svg>
              </span>
              <span class="point-index">{{ chapter.sort }}-{{ point.sort }}</span>
              <span class="point-title">{{ displayText(point.title) }}</span>
              <span v-if="point.required" class="point-required">必修</span>
              <span class="point-status">{{ statusText(point.learningStatus) }}</span>
              <span class="point-resource">资源 {{ point.resourceCount }}</span>
            </button>
          </div>
        </article>
      </section>
    </div>
  </div>
</template>
