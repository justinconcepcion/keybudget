import { useAuthStore } from '@/stores/auth'
import { usersApi } from '@/api/users'

/**
 * Ensures the current user's profile is loaded into the auth store.
 * Safe to call multiple times — skips the fetch if already populated.
 */
export async function useUser() {
  const authStore = useAuthStore()

  if (!authStore.user && authStore.isAuthenticated) {
    authStore.user = await usersApi.getMe()
  }

  return authStore.user
}
