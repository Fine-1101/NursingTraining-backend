<script setup>
defineProps({
  course: {
    type: Object,
    required: true,
  },
  compact: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['open-detail', 'start-learning'])

function progressPercent(course) {
  return Math.round(Number(course.progressPercent || 0))
}

function resolveCoverUrl(coverUrl) {
  if (!coverUrl) return ''
  if (/^(https?:)?\/\//.test(coverUrl) || coverUrl.startsWith('data:')) return coverUrl

  const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
  if (coverUrl.startsWith('/')) return `${apiBase}${coverUrl}`

  return `${apiBase}/${coverUrl}`
}
</script>

<template>
  <article
    v-if="!compact"
    class="course-card"
    tabindex="0"
    role="button"
    @click="emit('open-detail')"
    @keyup.enter="emit('open-detail')"
  >
    <div class="course-cover">
      <img v-if="course.coverUrl" :src="resolveCoverUrl(course.coverUrl)" :alt="course.title" />
      <span>{{ course.courseType === 'REQUIRED' ? '必修' : '推荐' }}</span>
      <svg v-if="!course.coverUrl" viewBox="0 0 80 80">
        <path d="M15 56h50v8H15v-8Zm8-36h34a8 8 0 0 1 8 8v24H15V28a8 8 0 0 1 8-8Zm14 8v8h-8v8h8v8h8v-8h8v-8h-8v-8h-8Z" />
      </svg>
    </div>
    <div class="course-body">
      <h3>{{ course.title }}</h3>
      <p>{{ course.instructorName || '未设置讲师' }} · {{ course.categoryName || '未分类' }}</p>
      <div class="course-meta">
        <span>{{ course.learningStatus === 'NOT_STARTED' ? '未开始' : '学习中' }}</span>
      </div>
      <div class="progress-line">
        <i :style="{ width: `${progressPercent(course)}%` }"></i>
      </div>
      <div class="course-action">
        <button
          type="button"
          :class="{ start: course.learningStatus === 'NOT_STARTED' }"
          @click.stop="emit('start-learning')"
        >
          {{ course.buttonText }}
        </button>
        <strong>{{ progressPercent(course) }}%</strong>
      </div>
    </div>
  </article>

  <article
    v-else
    class="continue-card"
    tabindex="0"
    role="button"
    @click="emit('open-detail')"
    @keyup.enter="emit('open-detail')"
  >
    <div class="mini-cover">
      <img v-if="course.coverUrl" :src="resolveCoverUrl(course.coverUrl)" :alt="course.title" />
      <svg v-else viewBox="0 0 64 64"><path d="M13 16h38v32H13V16Zm8 8v16l13-8-13-8Z" /></svg>
    </div>
    <div class="continue-info">
      <h3>{{ course.title }}</h3>
      <p>{{ course.instructorName || '未设置讲师' }} · {{ course.categoryName || '未分类' }}</p>
      <div class="course-meta">
        <span>{{ course.lastPointTitle || '继续上次学习' }}</span>
        <span>{{ progressPercent(course) }}%</span>
      </div>
      <div class="progress-line">
        <i :style="{ width: `${progressPercent(course)}%` }"></i>
      </div>
      <button type="button" @click.stop="emit('start-learning')">{{ course.buttonText || '继续学习' }}</button>
    </div>
  </article>
</template>
