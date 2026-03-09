import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { categoriesApi } from '@/api/categories'
import type { CategoryResponse, CreateCategoryRequest, UpdateCategoryRequest } from '@/types'

export const useCategoriesStore = defineStore('categories', () => {
  const categories = ref<CategoryResponse[]>([])
  const loaded = ref(false)

  const expenseCategories = computed(() => categories.value.filter((c) => c.type === 'EXPENSE'))

  const incomeCategories = computed(() => categories.value.filter((c) => c.type === 'INCOME'))

  async function fetchCategories(): Promise<void> {
    categories.value = await categoriesApi.getAll()
    loaded.value = true
  }

  async function createCategory(data: CreateCategoryRequest): Promise<CategoryResponse> {
    const created = await categoriesApi.create(data)
    categories.value.push(created)
    return created
  }

  async function updateCategory(
    id: number,
    data: UpdateCategoryRequest,
  ): Promise<CategoryResponse> {
    const updated = await categoriesApi.update(id, data)
    const idx = categories.value.findIndex((c) => c.id === id)
    if (idx >= 0) categories.value[idx] = updated
    return updated
  }

  async function deleteCategory(id: number): Promise<void> {
    await categoriesApi.delete(id)
    categories.value = categories.value.filter((c) => c.id !== id)
  }

  return {
    categories,
    expenseCategories,
    incomeCategories,
    fetchCategories,
    createCategory,
    updateCategory,
    deleteCategory,
  }
})
