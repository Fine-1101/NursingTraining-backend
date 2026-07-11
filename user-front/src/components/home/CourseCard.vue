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

function progressPercent(course) {
  return Math.round(Number(course.progressPercent || 0))
}
</script>

<template>
  <article v-if="!compact" class="course-card">
    <div class="course-cover">
      <span>{{ course.courseType === 'REQUIRED' ? '必修' : '推荐' }}</span>
      <svg viewBox="0 0 80 80">
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
        <button type="button" :class="{ start: course.learningStatus === 'NOT_STARTED' }">{{ course.buttonText }}</button>
        <strong>{{ progressPercent(course) }}%</strong>
      </div>
    </div>
  </article>

  <article v-else class="continue-card">
    <div class="mini-cover">
      <svg viewBox="0 0 64 64"><path d="M13 16h38v32H13V16Zm8 8v16l13-8-13-8Z" /></svg>
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
      <button type="button">{{ course.buttonText || '继续学习' }}</button>
    </div>
  </article>
</template>
