import axios, { type InternalAxiosRequestConfig } from 'axios'
import type { Router } from 'vue-router'
import type { useAuthStore } from '@/stores/auth'

type AuthStore = ReturnType<typeof useAuthStore>

// Lazily imported to avoid circular dependency at module init time
let getAuthStore: (() => AuthStore) | null = null
let getRouter: (() => Router) | null = null

export function initApiDeps(authStoreFn: () => AuthStore, routerFn: () => Router) {
  getAuthStore = authStoreFn
  getRouter = routerFn
}

const api = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
  withCredentials: true,
})

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getAuthStore?.().accessToken
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

type FailedRequest = {
  resolve: (token: string) => void
  reject: (err: unknown) => void
}

let isRefreshing = false
let failedQueue: FailedRequest[] = []

function processQueue(error: unknown, token: string | null = null) {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) {
      reject(error)
    } else if (token) {
      resolve(token)
    } else {
      reject(new Error('Token refresh failed: no token returned'))
    }
  })
  failedQueue = []
}

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config as InternalAxiosRequestConfig & { _retry?: boolean }

    if (error.response?.status !== 401 || original._retry) {
      return Promise.reject(error)
    }

    if (isRefreshing) {
      return new Promise<string>((resolve, reject) => {
        failedQueue.push({ resolve, reject })
      }).then((token) => {
        original.headers.Authorization = `Bearer ${token}`
        return api(original)
      })
    }

    original._retry = true
    isRefreshing = true

    try {
      const authStore = getAuthStore?.()
      if (!authStore) return Promise.reject(error)

      const tokens = await authStore.refreshTokens()
      processQueue(null, tokens.accessToken)
      original.headers.Authorization = `Bearer ${tokens.accessToken}`
      return api(original)
    } catch (refreshError) {
      processQueue(refreshError)
      getAuthStore?.().clear()
      getRouter?.().push('/login')
      return Promise.reject(refreshError)
    } finally {
      isRefreshing = false
    }
  },
)

export default api
