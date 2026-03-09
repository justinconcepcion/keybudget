import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '../auth'

vi.mock('@/api/auth', () => ({
  authApi: {
    refresh: vi.fn(),
    logout: vi.fn(),
  },
}))

import { authApi } from '@/api/auth'

describe('useAuthStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('starts unauthenticated', () => {
    const store = useAuthStore()
    expect(store.isAuthenticated).toBe(false)
    expect(store.accessToken).toBeNull()
    expect(store.user).toBeNull()
  })

  it('setAccessToken sets token and authenticates', () => {
    const store = useAuthStore()
    store.setAccessToken('test-token')
    expect(store.accessToken).toBe('test-token')
    expect(store.isAuthenticated).toBe(true)
  })

  it('refreshTokens calls API and updates token', async () => {
    const store = useAuthStore()
    vi.mocked(authApi.refresh).mockResolvedValue({
      accessToken: 'refreshed-token',
      expiresIn: 900,
    })

    const result = await store.refreshTokens()

    expect(authApi.refresh).toHaveBeenCalled()
    expect(store.accessToken).toBe('refreshed-token')
    expect(result.accessToken).toBe('refreshed-token')
  })

  it('clear resets state', () => {
    const store = useAuthStore()
    store.setAccessToken('token')
    store.user = { id: 1, email: 'a@b.com', name: 'Test', pictureUrl: null }

    store.clear()

    expect(store.accessToken).toBeNull()
    expect(store.user).toBeNull()
    expect(store.isAuthenticated).toBe(false)
  })

  it('logout calls API and clears state', async () => {
    const store = useAuthStore()
    store.setAccessToken('token')
    vi.mocked(authApi.logout).mockResolvedValue(undefined)

    await store.logout()

    expect(authApi.logout).toHaveBeenCalled()
    expect(store.isAuthenticated).toBe(false)
  })

  it('logout clears state even if API call fails', async () => {
    const store = useAuthStore()
    store.setAccessToken('token')
    vi.mocked(authApi.logout).mockRejectedValue(new Error('network error'))

    await store.logout()

    expect(store.isAuthenticated).toBe(false)
  })
})
