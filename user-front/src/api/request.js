const API_BASE = import.meta.env.VITE_API_BASE_URL || ''

export function getAccessToken() {
  return localStorage.getItem('accessToken') || ''
}

export function setAuthSession(session) {
  localStorage.setItem('tokenType', session.tokenType || 'Bearer')
  localStorage.setItem('accessToken', session.accessToken)
  localStorage.setItem('expiresIn', String(session.expiresIn || ''))
  localStorage.setItem('userInfo', JSON.stringify(session.user || {}))
}

export function clearAuthSession() {
  localStorage.removeItem('tokenType')
  localStorage.removeItem('accessToken')
  localStorage.removeItem('expiresIn')
  localStorage.removeItem('userInfo')
}

export function getStoredUser() {
  try {
    return JSON.parse(localStorage.getItem('userInfo') || '{}')
  } catch {
    return {}
  }
}

export async function request(path, options = {}) {
  const token = getAccessToken()
  const headers = {
    ...(options.body ? { 'Content-Type': 'application/json' } : {}),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...options.headers,
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  })

  const result = await response.json().catch(() => null)

  if (!response.ok) {
    throw new Error(result?.message || `请求失败：${response.status}`)
  }

  if (result && typeof result.code === 'number' && result.code !== 0) {
    throw new Error(result.message || '业务请求失败')
  }

  return result?.data ?? result
}
