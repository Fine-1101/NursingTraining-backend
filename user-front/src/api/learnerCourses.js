import { request } from './request'

export function getLearnerCourses(params = {}) {
  const query = new URLSearchParams()

  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      query.set(key, value)
    }
  })

  const suffix = query.toString() ? `?${query.toString()}` : ''
  return request(`/api/learner/courses${suffix}`)
}

export function getLearnerCourseStats() {
  return request('/api/learner/courses/stats')
}
