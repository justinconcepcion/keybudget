import api from './axios'
import type { CategoryResponse, CreateCategoryRequest, UpdateCategoryRequest } from '@/types'

export const categoriesApi = {
  getAll(): Promise<CategoryResponse[]> {
    return api.get<CategoryResponse[]>('/categories').then((r) => r.data)
  },

  create(data: CreateCategoryRequest): Promise<CategoryResponse> {
    return api.post<CategoryResponse>('/categories', data).then((r) => r.data)
  },

  update(id: number, data: UpdateCategoryRequest): Promise<CategoryResponse> {
    return api.put<CategoryResponse>(`/categories/${id}`, data).then((r) => r.data)
  },

  delete(id: number): Promise<void> {
    return api.delete(`/categories/${id}`).then(() => undefined)
  },
}
