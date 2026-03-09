import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'
import type { AuthTokens, UserProfile } from '@/types'

/**
 * Access token is kept in memory only — never written to localStorage or
 * sessionStorage to prevent XSS token theft.
 *
 * On a fresh page load, `initSession()` performs a silent refresh using the
 * HttpOnly refresh_token cookie so the user stays signed in without storing
 * anything in accessible storage.
 */
export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(null)
  const user = ref<UserProfile | null>(null)

  const isAuthenticated = computed(() => !!accessToken.value)

  function setAccessToken(token: string) {
    accessToken.value = token
  }

  /**
   * Silently refreshes the access token using the HttpOnly refresh_token cookie.
   * Called on app mount and by the Axios 401 interceptor.
   */
  async function refreshTokens(): Promise<AuthTokens> {
    const tokens = await authApi.refresh()
    accessToken.value = tokens.accessToken
    return tokens
  }

  function clear() {
    accessToken.value = null
    user.value = null
  }

  async function logout() {
    try {
      await authApi.logout()
    } catch (error) {
      // Best-effort: server-side logout failed, but local state is always cleared.
      console.error('[auth] Logout failed on server:', error)
    } finally {
      clear()
    }
  }

  return {
    accessToken,
    user,
    isAuthenticated,
    setAccessToken,
    refreshTokens,
    clear,
    logout,
  }
})
