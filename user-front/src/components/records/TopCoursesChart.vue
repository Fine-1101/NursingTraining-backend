<script setup>
import { computed } from 'vue'
import EChartPanel from './EChartPanel.vue'

const props = defineProps({
  courses: {
    type: Array,
    default: () => [],
  },
})

const option = computed(() => {
  const courses = [...props.courses].reverse()
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow',
      },
    },
    grid: {
      left: 116,
      right: 34,
      top: 24,
      bottom: 28,
    },
    xAxis: {
      type: 'value',
      axisLabel: {
        formatter: '{value}',
      },
      splitLine: {
        lineStyle: {
          color: '#eef2f6',
        },
      },
    },
    yAxis: {
      type: 'category',
      data: courses.map((course) => course.courseTitle),
      axisLabel: {
        width: 96,
        overflow: 'truncate',
      },
    },
    series: [
      {
        name: '学习时长',
        type: 'bar',
        barWidth: 12,
        data: courses.map((course) => Number(course.totalDurationHours || 0)),
        itemStyle: {
          color: '#159947',
          borderRadius: [0, 8, 8, 0],
        },
      },
    ],
  }
})
</script>

<template>
  <EChartPanel :option="option" />
</template>
