import api from './axios'
import type { BudgetResponse, CreateBudgetRequest, UpdateBudgetRequest } from '@/types'

export const budgetsApi = {
  getAll(month: string): Promise<BudgetResponse[]> {
    return api.get<BudgetResponse[]>('/budgets', { params: { month } }).then((r) => r.data)
  },

  create(data: CreateBudgetRequest): Promise<BudgetResponse> {
    return api.post<BudgetResponse>('/budgets', data).then((r) => r.data)
  },

  update(id: number, data: UpdateBudgetRequest): Promise<BudgetResponse> {
    return api.put<BudgetResponse>(`/budgets/${id}`, data).then((r) => r.data)
  },

  delete(id: number): Promise<void> {
    return api.delete(`/budgets/${id}`).then(() => undefined)
  },
}
