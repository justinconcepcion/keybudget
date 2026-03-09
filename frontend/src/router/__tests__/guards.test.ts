import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import router from '../index'

describe('router auth guard', () => {
  beforeEach(async () => {
    setActivePinia(createPinia())
    // Reset router to a neutral starting point
    const store = useAuthStore()
    store.setAccessToken('reset-token')
    await router.push('/settings')
    store.clear()
  })

  it('redirects unauthenticated users to login', async () => {
    const store = useAuthStore()
    store.clear()

    await router.push('/dashboard')
    await router.isReady()

    expect(router.currentRoute.value.name).toBe('login')
  })

  it('allows authenticated users to access protected routes', async () => {
    const store = useAuthStore()
    store.setAccessToken('valid-token')

    await router.push('/dashboard')
    await router.isReady()

    expect(router.currentRoute.value.name).toBe('dashboard')
  })

  it('allows unauthenticated users to access public routes', async () => {
    const store = useAuthStore()
    store.clear()

    await router.push('/login')
    await router.isReady()

    expect(router.currentRoute.value.name).toBe('login')
  })

  it('redirects authenticated users away from login', async () => {
    const store = useAuthStore()
    store.setAccessToken('valid-token')

    await router.push('/login')
    await router.isReady()

    expect(router.currentRoute.value.name).toBe('dashboard')
  })

  it('preserves redirect query for unauthenticated users', async () => {
    const store = useAuthStore()
    store.clear()

    await router.push('/budgets')
    await router.isReady()

    expect(router.currentRoute.value.query.redirect).toBe('/budgets')
  })
})
