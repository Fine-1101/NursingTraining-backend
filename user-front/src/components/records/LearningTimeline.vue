<script setup>
defineProps({
  records: {
    type: Array,
    default: () => [],
  },
})

function progressText(record) {
  if (record.progressPercent === null || record.progressPercent === undefined) return ''
  return `${Math.round(Number(record.progressPercent || 0))}%`
}
</script>

<template>
  <div v-if="records.length" class="learning-timeline">
    <article v-for="record in records" :key="record.recordId" class="timeline-item">
      <time>{{ record.timeText }}</time>
      <span class="timeline-dot"></span>
      <div class="timeline-card">
        <div>
          <h3>{{ record.title || record.courseTitle }}</h3>
          <p>
            <span>{{ record.description || record.actionName }}</span>
            <i v-if="record.durationMinutes">|</i>
            <span v-if="record.durationMinutes">{{ record.durationMinutes }} 分钟</span>
          </p>
        </div>
        <strong v-if="progressText(record)">{{ progressText(record) }}</strong>
      </div>
    </article>
  </div>

  <p v-else class="empty-text">暂无学习历史记录</p>
</template>
