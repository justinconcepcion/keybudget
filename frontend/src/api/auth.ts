import axios from 'axios'
import type { AuthTokens } from '@/types'

// Separate instance — does NOT go through the auth interceptor to avoid circular refresh loops
const raw = axios.create({ baseURL: '/api/v1', withCredentials: true })

export const authApi = {
  /**
   * Silently refresh the access token.
   * The refresh_token HttpOnly cookie is sent automatically by the browser
   * because it is scoped to the /api/v1/auth/refresh path.
   */
  refresh(): Promise<AuthTokens> {
    return raw.post<AuthTokens>('/auth/refresh', {}).then((r) => r.data)
  },

  logout(): Promise<void> {
    return raw.post('/auth/logout').then(() => undefined)
  },
}
