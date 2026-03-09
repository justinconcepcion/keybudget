import api from './axios'
import type { UserProfile } from '@/types'

export const usersApi = {
  getMe(): Promise<UserProfile> {
    return api.get<UserProfile>('/users/me').then((r) => r.data)
  },
}
