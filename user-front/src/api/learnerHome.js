import { request } from './request'

export function getLearnerHome() {
  return request('/api/learner/home')
}
