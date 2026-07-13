import { clearAuthSession, request, setAuthSession } from './request'

export async function login(username, password) {
  const session = await request('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  })

  setAuthSession(session)
  return session
}

export async function register(payload) {
  const session = await request('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(payload),
  })

  setAuthSession(session)
  return session
}

export async function logout() {
  try {
    await request('/api/auth/logout', {
      method: 'POST',
    })
  } finally {
    clearAuthSession()
  }
}
