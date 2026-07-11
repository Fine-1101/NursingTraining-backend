<script setup>
import { computed } from 'vue'

const props = defineProps({
  overview: {
    type: Object,
    default: () => ({
      completedCount: 0,
      learningCount: 0,
      notStartedCount: 0,
      overallProgressPercent: 0,
    }),
  },
})

const ringStyle = computed(() => {
  const progress = Math.round(props.overview.overallProgressPercent || 0)
  return {
    background: `conic-gradient(#74bd32 0 ${progress}%, #ff9d23 ${progress}% ${Math.min(progress + 34, 100)}%, #d8dde2 0)`,
  }
})
</script>

<template>
  <section class="aside-card progress-card">
    <div class="section-title compact">
      <h2>学习进度</h2>
    </div>
    <div class="progress-overview">
      <div class="progress-ring" :style="ringStyle">
        <span>{{ Math.round(overview.overallProgressPercent || 0) }}%</span>
        <small>总体进度</small>
      </div>
      <div class="progress-legend">
        <p><i class="done"></i>已完成 <strong>{{ overview.completedCount }}门</strong></p>
        <p><i class="learning"></i>学习中 <strong>{{ overview.learningCount }}门</strong></p>
        <p><i class="todo"></i>未开始 <strong>{{ overview.notStartedCount }}门</strong></p>
      </div>
    </div>
  </section>
</template>
