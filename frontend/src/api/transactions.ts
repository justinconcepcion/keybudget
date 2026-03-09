import api from './axios'
import type {
  TransactionPage,
  TransactionParams,
  TransactionResponse,
  CreateTransactionRequest,
  MonthlySummaryResponse,
} from '@/types'

export const transactionsApi = {
  getAll(params: TransactionParams): Promise<TransactionPage> {
    return api.get<TransactionPage>('/transactions', { params }).then((r) => r.data)
  },

  create(data: CreateTransactionRequest): Promise<TransactionResponse> {
    return api.post<TransactionResponse>('/transactions', data).then((r) => r.data)
  },

  getSummary(month: string): Promise<MonthlySummaryResponse> {
    return api
      .get<MonthlySummaryResponse>('/transactions/summary', { params: { month } })
      .then((r) => r.data)
  },
}
