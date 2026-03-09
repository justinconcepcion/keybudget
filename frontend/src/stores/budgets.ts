import { defineStore } from 'pinia'
import { ref } from 'vue'
import { budgetsApi } from '@/api/budgets'
import type {
  BudgetAlertResponse,
  BudgetResponse,
  CreateBudgetRequest,
  UpdateBudgetRequest,
} from '@/types'

export const useBudgetsStore = defineStore('budgets', () => {
  const budgets = ref<BudgetResponse[]>([])
  const alerts = ref<BudgetAlertResponse[]>([])

  async function fetchBudgets(month: string): Promise<void> {
    budgets.value = await budgetsApi.getAll(month)
  }

  async function fetchAlerts(): Promise<void> {
    alerts.value = await budgetsApi.getAlerts()
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
    alerts,
    fetchBudgets,
    fetchAlerts,
    createBudget,
    updateBudget,
    deleteBudget,
  }
})
