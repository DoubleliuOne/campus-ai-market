import axios from 'axios'

const TOKEN_KEY = 'campus_market_token'
const USER_KEY = 'campus_market_user'

export function readStoredSession() {
  let user = null
  try {
    user = JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch {
    user = null
  }
  return {
    token: localStorage.getItem(TOKEN_KEY) || '',
    user,
  }
}

export function writeStoredSession(token, user) {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function clearStoredSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

const http = axios.create({
  baseURL: '/api',
  timeout: 60000,
})

http.interceptors.request.use((config) => {
  const { token } = readStoredSession()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body.code === 'number') {
      if (body.code !== 200) {
        const error = new Error(body.message || '请求失败')
        error.status = response.status || body.code
        return Promise.reject(error)
      }
      return body.data
    }
    return body
  },
  (error) => {
    let message = '网络连接失败，请稍后重试'
    let status = 0
    if (error.response) {
      status = error.response.status
      message = error.response.data?.message || `请求失败（${status}）`
    }
    const wrapped = new Error(message)
    wrapped.status = status

    if (status === 401 && !window.location.pathname.startsWith('/login')) {
      clearStoredSession()
      const redirect = encodeURIComponent(window.location.pathname + window.location.search)
      window.location.replace(`/login?redirect=${redirect}`)
    }
    return Promise.reject(wrapped)
  },
)

export default http
