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

export function getLearnerCourseDetail(courseId) {
  return request(`/api/learner/courses/${courseId}`)
}

export function getLearnerCourseStudy(courseId, pointId, activeType) {
  const query = new URLSearchParams()
  if (activeType) query.set('activeType', activeType)
  const suffix = query.toString() ? `?${query.toString()}` : ''
  return request(`/api/learner/courses/${courseId}/points/${pointId}/study${suffix}`)
}

export function saveLearnerVideoProgress(courseId, pointId, videoId, payload) {
  return request(`/api/learner/courses/${courseId}/points/${pointId}/videos/${videoId}/progress`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}
