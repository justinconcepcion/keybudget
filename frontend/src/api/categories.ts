import api from './axios'
import type { CategoryResponse } from '@/types'

export const categoriesApi = {
  getAll(): Promise<CategoryResponse[]> {
    return api.get<CategoryResponse[]>('/categories').then((r) => r.data)
  },
}
