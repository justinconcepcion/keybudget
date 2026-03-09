import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { useUser } from '../useUser'

vi.mock('@/api/users', () => ({
  usersApi: {
    getMe: vi.fn(),
  },
}))

import { usersApi } from '@/api/users'

const USER = { id: 1, email: 'test@test.com', name: 'Test User', pictureUrl: null }

describe('useUser', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('fetches user when authenticated and not loaded', async () => {
    const store = useAuthStore()
    store.setAccessToken('token')
    vi.mocked(usersApi.getMe).mockResolvedValue(USER)

    const user = await useUser()

    expect(usersApi.getMe).toHaveBeenCalled()
    expect(user).toEqual(USER)
    expect(store.user).toEqual(USER)
  })

  it('skips fetch when user already loaded', async () => {
    const store = useAuthStore()
    store.setAccessToken('token')
    store.user = USER

    const user = await useUser()

    expect(usersApi.getMe).not.toHaveBeenCalled()
    expect(user).toEqual(USER)
  })

  it('returns null when not authenticated', async () => {
    const user = await useUser()

    expect(usersApi.getMe).not.toHaveBeenCalled()
    expect(user).toBeNull()
  })
})
