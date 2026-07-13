<script setup>
import { computed } from 'vue'

const props = defineProps({
  calendar: {
    type: Object,
    default: () => ({ year: '', month: '', days: [] }),
  },
})

function toDateKey(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function buildMonthDays(year, month, learningMarks) {
  const todayKey = toDateKey(new Date())
  const firstDay = new Date(year, month - 1, 1)
  const mondayIndex = (firstDay.getDay() + 6) % 7
  const startDate = new Date(year, month - 1, 1 - mondayIndex)
  const days = []

  for (let index = 0; index < 42; index += 1) {
    const date = new Date(startDate)
    date.setDate(startDate.getDate() + index)

    const dateKey = toDateKey(date)
    const mark = learningMarks.get(dateKey)

    days.push({
      date: dateKey,
      dayOfMonth: date.getDate(),
      currentMonth: date.getMonth() === month - 1,
      today: dateKey === todayKey || Boolean(mark?.today),
      hasLearning: Boolean(mark?.hasLearning),
    })
  }

  return days
}

const calendarModel = computed(() => {
  const now = new Date()
  const year = Number(props.calendar?.year) || now.getFullYear()
  const month = Number(props.calendar?.month) || now.getMonth() + 1
  const learningMarks = new Map((props.calendar?.days || []).map((day) => [day.date, day]))

  return {
    year,
    month,
    days: buildMonthDays(year, month, learningMarks),
  }
})

const weeks = computed(() => {
  const result = []
  const days = calendarModel.value.days
  for (let index = 0; index < days.length; index += 7) {
    result.push(days.slice(index, index + 7))
  }
  return result
})
</script>

<template>
  <section class="aside-card calendar-card">
    <div class="section-title compact">
      <h2>学习日历</h2>
      <span>{{ calendarModel.year }}年{{ calendarModel.month }}月</span>
    </div>
    <div class="calendar-weekdays">
      <span>一</span>
      <span>二</span>
      <span>三</span>
      <span>四</span>
      <span>五</span>
      <span>六</span>
      <span>日</span>
    </div>
    <div class="calendar-grid">
      <template v-for="(week, weekIndex) in weeks" :key="weekIndex">
        <span
          v-for="day in week"
          :key="day.date"
          class="calendar-day"
          :class="{ muted: !day.currentMonth, today: day.today, learned: day.hasLearning }"
        >
          {{ day.dayOfMonth }}
        </span>
      </template>
    </div>
  </section>
</template>
