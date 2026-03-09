import api from './axios'
import type {
  TransactionPage,
  TransactionParams,
  TransactionResponse,
  CreateTransactionRequest,
  UpdateTransactionRequest,
  MonthlySummaryResponse,
} from '@/types'

export const transactionsApi = {
  getAll(params: TransactionParams): Promise<TransactionPage> {
    return api.get<TransactionPage>('/transactions', { params }).then((r) => r.data)
  },

  create(data: CreateTransactionRequest): Promise<TransactionResponse> {
    return api.post<TransactionResponse>('/transactions', data).then((r) => r.data)
  },

  update(id: number, data: UpdateTransactionRequest): Promise<TransactionResponse> {
    return api.put<TransactionResponse>(`/transactions/${id}`, data).then((r) => r.data)
  },

  delete(id: number): Promise<void> {
    return api.delete(`/transactions/${id}`).then(() => undefined)
  },

  getSummary(month: string): Promise<MonthlySummaryResponse> {
    return api
      .get<MonthlySummaryResponse>('/transactions/summary', { params: { month } })
      .then((r) => r.data)
  },
}
