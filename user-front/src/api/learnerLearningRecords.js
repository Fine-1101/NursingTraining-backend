import { request } from './request'

function buildQuery(params = {}) {
  const query = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      query.set(key, value)
    }
  })
  const suffix = query.toString()
  return suffix ? `?${suffix}` : ''
}

export function getLearningRecordOverview(params = {}) {
  return request(`/api/learner/learning-records/overview${buildQuery(params)}`)
}

export function getLearningRecords(params = {}) {
  return request(`/api/learner/learning-records${buildQuery(params)}`)
}

export function getLearningTopCourses(params = {}) {
  return request(`/api/learner/learning-records/top-courses${buildQuery(params)}`)
}

export function getLearningResourceDistribution(params = {}) {
  return request(`/api/learner/learning-records/resource-distribution${buildQuery(params)}`)
}

export function getLearningFrequencyTrend(params = {}) {
  return request(`/api/learner/learning-records/frequency-trend${buildQuery(params)}`)
}
