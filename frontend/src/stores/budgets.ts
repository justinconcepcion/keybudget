import { defineStore } from 'pinia'
import { ref } from 'vue'
import { budgetsApi } from '@/api/budgets'
import type { BudgetResponse, CreateBudgetRequest, UpdateBudgetRequest } from '@/types'

export const useBudgetsStore = defineStore('budgets', () => {
  const budgets = ref<BudgetResponse[]>([])

  async function fetchBudgets(month: string): Promise<void> {
    budgets.value = await budgetsApi.getAll(month)
  }

  async function createBudget(data: CreateBudgetRequest): Promise<BudgetResponse> {
    const created = await budgetsApi.create(data)
    budgets.value.push(created)
    return created
  }

  async function updateBudget(id: number, data: UpdateBudgetRequest): Promise<BudgetResponse> {
    const updated = await budgetsApi.update(id, data)
    const idx = budgets.value.findIndex((b) => b.id === id)
    if (idx !== -1) budgets.value[idx] = updated
    return updated
  }

  async function deleteBudget(id: number): Promise<void> {
    await budgetsApi.delete(id)
    budgets.value = budgets.value.filter((b) => b.id !== id)
  }

  return {
    budgets,
    fetchBudgets,
    createBudget,
    updateBudget,
    deleteBudget,
  }
})
