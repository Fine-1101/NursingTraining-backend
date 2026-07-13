<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { getLearnerCourseStudy, saveLearnerVideoProgress } from '../../api/learnerCourses'

const props = defineProps({
  courseId: {
    type: [Number, String],
    required: true,
  },
  pointId: {
    type: [Number, String, null],
    default: null,
  },
})

const emit = defineEmits(['back'])

const loading = ref(false)
const saving = ref(false)
const errorMessage = ref('')
const studyData = ref(null)
const currentPointId = ref(props.pointId)
const activeType = ref('')
const activeResourceIndex = ref(0)
const videoRef = ref(null)
const lastSavedSecond = ref(-1)
const autoSaveTimer = ref(null)

const tabs = computed(() => {
  const source = studyData.value?.tabs || {}
  const next = []
  if (source.videos?.length) next.push({ type: 'VIDEO', label: '视频', list: source.videos })
  if (source.articles?.length) next.push({ type: 'ARTICLE', label: '文章', list: source.articles })
  if (source.ppts?.length) next.push({ type: 'PPT', label: 'PPT', list: source.ppts })
  return next
})

const activeTab = computed(() => tabs.value.find((tab) => tab.type === activeType.value) || tabs.value[0])
const activeResource = computed(() => activeTab.value?.list?.[activeResourceIndex.value] || null)
const course = computed(() => studyData.value?.course || {})
const currentPoint = computed(() => studyData.value?.currentPoint || {})
const navigation = computed(() => studyData.value?.navigation || {})

function resolveAssetUrl(url) {
  if (!url) return ''
  if (/^(https?:)?\/\//.test(url) || url.startsWith('data:')) return url
  const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
  return url.startsWith('/') ? `${apiBase}${url}` : `${apiBase}/${url}`
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
    NOT_STARTED: '未学习',
    LEARNING: '学习中',
    COMPLETED: '已完成',
  }
  return map[status] || status || '未学习'
}

function formatDuration(seconds) {
  const total = Math.max(0, Math.floor(Number(seconds || 0)))
  const minutes = Math.floor(total / 60)
  const rest = total % 60
  return `${minutes}:${String(rest).padStart(2, '0')}`
}

function pickDefaultType(data) {
  if (data.currentPoint?.activeType) return data.currentPoint.activeType
  if (data.tabs?.videos?.length) return 'VIDEO'
  if (data.tabs?.articles?.length) return 'ARTICLE'
  if (data.tabs?.ppts?.length) return 'PPT'
  return ''
}

async function loadStudy(nextPointId = currentPointId.value, nextType = activeType.value) {
  if (!nextPointId) {
    studyData.value = null
    errorMessage.value = '课程内容暂未配置'
    return
  }

  loading.value = true
  errorMessage.value = ''
  stopAutoSave()

  try {
    const data = await getLearnerCourseStudy(props.courseId, nextPointId, nextType)
    studyData.value = data
    currentPointId.value = data.currentPoint?.pointId || nextPointId
    activeType.value = pickDefaultType(data)
    activeResourceIndex.value = 0
    lastSavedSecond.value = -1
    await nextTick()
    restoreVideoPosition()
    startAutoSave()
  } catch (error) {
    studyData.value = null
    errorMessage.value = error.message || '课程学习内容加载失败'
  } finally {
    loading.value = false
  }
}

function switchTab(type) {
  if (activeType.value === type) return
  saveCurrentVideoProgress('LEAVE')
  activeType.value = type
  activeResourceIndex.value = 0
  nextTick(restoreVideoPosition)
}

function switchResource(index) {
  if (activeResourceIndex.value === index) return
  saveCurrentVideoProgress('LEAVE')
  activeResourceIndex.value = index
  nextTick(restoreVideoPosition)
}

function restoreVideoPosition() {
  if (activeType.value !== 'VIDEO' || !videoRef.value || !activeResource.value) return
  const position = Number(activeResource.value.lastPositionSeconds || 0)
  if (position > 0) videoRef.value.currentTime = position
}

function startAutoSave() {
  stopAutoSave()
  autoSaveTimer.value = window.setInterval(() => {
    saveCurrentVideoProgress('AUTO')
  }, 15000)
}

function stopAutoSave() {
  if (!autoSaveTimer.value) return
  window.clearInterval(autoSaveTimer.value)
  autoSaveTimer.value = null
}

async function saveCurrentVideoProgress(eventType, ended = false) {
  if (activeType.value !== 'VIDEO' || !videoRef.value || !activeResource.value) return

  const video = videoRef.value
  const currentSeconds = Math.floor(video.currentTime || 0)
  const durationSeconds = Math.floor(video.duration || activeResource.value.durationSeconds || 0)
  if (!durationSeconds) return
  if (eventType === 'AUTO' && Math.abs(currentSeconds - lastSavedSecond.value) < 5) return

  lastSavedSecond.value = currentSeconds
  saving.value = true

  try {
    const result = await saveLearnerVideoProgress(
      props.courseId,
      currentPointId.value,
      activeResource.value.videoId,
      { currentSeconds, durationSeconds, eventType, ended },
    )

    Object.assign(activeResource.value, {
      learningStatus: result.learningStatus,
      progressPercent: result.progressPercent,
      lastPositionSeconds: result.lastPositionSeconds,
      maxPositionSeconds: result.maxPositionSeconds,
      completed: result.completed,
    })
  } catch (error) {
    errorMessage.value = error.message || '视频进度保存失败'
  } finally {
    saving.value = false
  }
}

function goPoint(pointId) {
  if (!pointId) return
  saveCurrentVideoProgress('LEAVE')
  loadStudy(pointId, activeType.value)
}

onMounted(() => loadStudy())

watch(() => props.pointId, (nextPointId) => {
  currentPointId.value = nextPointId
  loadStudy(nextPointId)
})

onBeforeUnmount(() => {
  saveCurrentVideoProgress('LEAVE')
  stopAutoSave()
})
</script>

<template>
  <div class="course-learning-page">
    <section class="study-hero">
      <div class="study-cover">
        <img v-if="course.coverUrl" :src="resolveAssetUrl(course.coverUrl)" :alt="displayText(course.title)" />
        <svg v-else viewBox="0 0 80 80"><path d="M14 18h52v44H14V18Zm12 12v20l16-10-16-10Zm24 2h8v4h-8v-4Zm0 10h8v4h-8v-4Z" /></svg>
      </div>
      <div class="study-heading">
        <h2>{{ displayText(course.title) || '课程学习' }}</h2>
        <p>
          当前学习：
          <strong>{{ displayText(currentPoint.chapterTitle) || '章节待加载' }}</strong>
          <span>{{ displayText(currentPoint.title) || '课程点待加载' }}</span>
        </p>
        <div class="study-meta">
          <span>{{ statusText(currentPoint.learningStatus) }}</span>
          <span>总进度 {{ Math.round(Number(course.progressPercent || 0)) }}%</span>
          <span>已完成 {{ course.completedPointCount || 0 }}/{{ course.pointCount || 0 }}</span>
        </div>
      </div>
    </section>

    <section class="study-content-card">
      <p v-if="loading" class="empty-text">课程学习内容加载中...</p>

      <div v-else-if="errorMessage" class="study-inline-error">
        <svg viewBox="0 0 96 96"><path d="M48 8a40 40 0 1 0 0 80 40 40 0 0 0 0-80Zm-4 20h8v26h-8V28Zm0 34h8v8h-8v-8Z" /></svg>
        <h3>学习内容加载失败</h3>
        <p>{{ errorMessage }}</p>
        <div>
          <button type="button" @click="loadStudy(currentPointId)">重试</button>
          <button type="button" class="secondary" @click="emit('back')">返回课程详情</button>
        </div>
      </div>

      <template v-else>
        <div v-if="tabs.length" class="study-tabs">
          <button
            v-for="tab in tabs"
            :key="tab.type"
            type="button"
            :class="{ active: activeType === tab.type }"
            @click="switchTab(tab.type)"
          >
            {{ tab.label }}
          </button>
        </div>

        <div v-if="activeTab?.list?.length > 1" class="resource-tabs">
          <button
            v-for="(resource, index) in activeTab.list"
            :key="resource.videoId || resource.articleId || resource.pptId"
            type="button"
            :class="{ active: activeResourceIndex === index }"
            @click="switchResource(index)"
          >
            {{ displayText(resource.title) }}
          </button>
        </div>

        <div v-if="activeResource" class="resource-viewer">
          <template v-if="activeType === 'VIDEO'">
            <div class="study-video-frame">
              <video
                ref="videoRef"
                class="study-video"
                :src="resolveAssetUrl(activeResource.playUrl)"
                :poster="resolveAssetUrl(activeResource.coverUrl)"
                controls
                :controlsList="activeResource.allowDownload ? '' : 'nodownload'"
                @play="saveCurrentVideoProgress('PLAY')"
                @pause="saveCurrentVideoProgress('PAUSE')"
                @ended="saveCurrentVideoProgress('ENDED', true)"
              ></video>
            </div>
            <div class="resource-summary">
              <h3>{{ displayText(activeResource.title) }}</h3>
              <p>{{ activeResource.description || '暂无视频简介' }}</p>
              <span>{{ formatDuration(activeResource.durationSeconds) }} · {{ statusText(activeResource.learningStatus) }} · {{ Math.round(Number(activeResource.progressPercent || 0)) }}%</span>
            </div>
          </template>

          <template v-else-if="activeType === 'ARTICLE'">
            <article class="article-viewer">
              <h3>{{ displayText(activeResource.title) }}</h3>
              <p v-if="activeResource.summary" class="article-summary">{{ activeResource.summary }}</p>
              <div class="article-html" v-html="activeResource.htmlContent"></div>
            </article>
          </template>

          <template v-else-if="activeType === 'PPT'">
            <div class="ppt-viewer">
              <iframe :src="resolveAssetUrl(activeResource.previewUrl)" :title="displayText(activeResource.title)"></iframe>
            </div>
            <div class="resource-summary">
              <h3>{{ displayText(activeResource.title) }}</h3>
              <p>{{ activeResource.description || '暂无 PPT 简介' }}</p>
              <span>{{ activeResource.pageCount || 0 }} 页 · {{ statusText(activeResource.learningStatus) }}</span>
            </div>
          </template>
        </div>

        <div v-else class="learning-player-placeholder">
          <svg viewBox="0 0 96 96"><path d="M18 16h60a8 8 0 0 1 8 8v42a8 8 0 0 1-8 8H18a8 8 0 0 1-8-8V24a8 8 0 0 1 8-8Zm22 17v24l20-12-20-12ZM30 82h36v-6H30v6Z" /></svg>
          <strong>暂无可学习内容</strong>
          <p>当前课程点还没有配置视频、文章或 PPT。</p>
        </div>

        <footer class="study-actions">
          <button v-if="navigation.previousPointId" type="button" class="study-nav-button" @click="goPoint(navigation.previousPointId)">
            <svg viewBox="0 0 24 24"><path d="m10 6-6 6 6 6 1.4-1.4L7.8 13H20v-2H7.8l3.6-3.6L10 6Z" /></svg>
            上一课
          </button>
          <span class="saving-text">{{ saving ? '进度保存中...' : '' }}</span>
          <button v-if="navigation.nextPointId" type="button" class="study-nav-button primary" @click="goPoint(navigation.nextPointId)">
            下一课
            <svg viewBox="0 0 24 24"><path d="m14 6-1.4 1.4 3.6 3.6H4v2h12.2l-3.6 3.6L14 18l6-6-6-6Z" /></svg>
          </button>
        </footer>
      </template>
    </section>
  </div>
</template>
