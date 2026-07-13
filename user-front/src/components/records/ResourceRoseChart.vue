<script setup>
import { computed } from 'vue'
import EChartPanel from './EChartPanel.vue'

const props = defineProps({
  items: {
    type: Array,
    default: () => [],
  },
})

const option = computed(() => ({
  tooltip: {
    trigger: 'item',
    formatter: '{b}: {c} ({d}%)',
  },
  legend: {
    orient: 'vertical',
    right: 8,
    top: 'middle',
  },
  color: ['#159947', '#45b86f', '#8bd7a4'],
  series: [
    {
      name: '课件分布',
      type: 'pie',
      radius: [18, 78],
      center: ['36%', '50%'],
      roseType: 'radius',
      itemStyle: {
        borderRadius: 4,
      },
      label: {
        formatter: '{d}%',
      },
      data: props.items.map((item) => ({
        name: item.resourceTypeName || item.resourceType,
        value: Number(item.count || 0),
      })),
    },
  ],
}))
</script>

<template>
  <EChartPanel :option="option" />
</template>
