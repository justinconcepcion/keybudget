import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { categoriesApi } from '@/api/categories'
import type { CategoryResponse } from '@/types'

export const useCategoriesStore = defineStore('categories', () => {
  const categories = ref<CategoryResponse[]>([])
  const loaded = ref(false)

  const expenseCategories = computed(() => categories.value.filter((c) => c.type === 'EXPENSE'))

  const incomeCategories = computed(() => categories.value.filter((c) => c.type === 'INCOME'))

  async function fetchCategories(): Promise<void> {
    if (loaded.value) return
    categories.value = await categoriesApi.getAll()
    loaded.value = true
  }

  return {
    categories,
    expenseCategories,
    incomeCategories,
    fetchCategories,
  }
})
