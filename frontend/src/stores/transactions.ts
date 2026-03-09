import { defineStore } from 'pinia'
import { ref } from 'vue'
import { transactionsApi } from '@/api/transactions'
import type {
  TransactionResponse,
  TransactionParams,
  CreateTransactionRequest,
  UpdateTransactionRequest,
  MonthlySummaryResponse,
} from '@/types'

interface Pagination {
  totalElements: number
  totalPages: number
  currentPage: number
}

export const useTransactionsStore = defineStore('transactions', () => {
  const transactions = ref<TransactionResponse[]>([])
  const pagination = ref<Pagination>({ totalElements: 0, totalPages: 0, currentPage: 0 })
  const monthlySummary = ref<MonthlySummaryResponse | null>(null)

  async function fetchTransactions(params: TransactionParams): Promise<void> {
    const page = await transactionsApi.getAll(params)
    transactions.value = page.content
    pagination.value = {
      totalElements: page.totalElements,
      totalPages: page.totalPages,
      currentPage: page.number,
    }
  }

  async function createTransaction(data: CreateTransactionRequest): Promise<TransactionResponse> {
    const created = await transactionsApi.create(data)
    return created
  }

  async function updateTransaction(
    id: number,
    data: UpdateTransactionRequest,
  ): Promise<TransactionResponse> {
    const updated = await transactionsApi.update(id, data)
    return updated
  }

  async function deleteTransaction(id: number): Promise<void> {
    await transactionsApi.delete(id)
  }

  async function fetchMonthlySummary(month: string): Promise<void> {
    monthlySummary.value = await transactionsApi.getSummary(month)
  }

  return {
    transactions,
    pagination,
    monthlySummary,
    fetchTransactions,
    createTransaction,
    updateTransaction,
    deleteTransaction,
    fetchMonthlySummary,
  }
})
