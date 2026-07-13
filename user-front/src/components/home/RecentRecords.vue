<script setup>
defineProps({
  records: {
    type: Array,
    default: () => [],
  },
})
</script>

<template>
  <section class="aside-card records-card">
    <div class="section-title compact">
      <h2>学习记录</h2>
      <a href="/">查看全部</a>
    </div>
    <div v-if="records.length" class="record-list">
      <article v-for="record in records" :key="`${record.recordType}-${record.occurredAt}`" class="record-item">
        <span></span>
        <div>
          <h3>{{ record.title }}</h3>
          <p>{{ record.occurredAt }}</p>
        </div>
      </article>
    </div>
    <p v-else class="empty-text">暂无学习记录</p>
  </section>
</template>
