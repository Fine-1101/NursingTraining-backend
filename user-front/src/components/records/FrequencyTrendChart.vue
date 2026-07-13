<script setup>
import { computed } from 'vue'
import EChartPanel from './EChartPanel.vue'

const props = defineProps({
  trend: {
    type: Object,
    default: () => ({ points: [] }),
  },
})

const option = computed(() => {
  const points = props.trend?.points || []
  return {
    tooltip: {
      trigger: 'axis',
    },
    grid: {
      left: 36,
      right: 20,
      top: 36,
      bottom: 34,
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: points.map((point) => point.label),
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: {
        lineStyle: {
          color: '#eef2f6',
        },
      },
    },
    series: [
      {
        name: '学习次数',
        type: 'line',
        smooth: true,
        symbolSize: 8,
        data: points.map((point) => Number(point.count || 0)),
        lineStyle: {
          width: 3,
          color: '#159947',
        },
        itemStyle: {
          color: '#159947',
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(21, 153, 71, 0.22)' },
              { offset: 1, color: 'rgba(21, 153, 71, 0.02)' },
            ],
          },
        },
      },
    ],
  }
})
</script>

<template>
  <EChartPanel :option="option" />
</template>
