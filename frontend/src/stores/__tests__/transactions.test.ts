import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useTransactionsStore } from '../transactions'

vi.mock('@/api/transactions', () => ({
  transactionsApi: {
    getAll: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
    getSummary: vi.fn(),
  },
}))

import { transactionsApi } from '@/api/transactions'

const TX = {
  id: 1,
  amount: 25.0,
  description: 'Lunch',
  date: '2026-03-09',
  type: 'EXPENSE' as const,
  categoryId: 1,
  categoryName: 'Food',
}

describe('useTransactionsStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('fetchTransactions populates list and pagination', async () => {
    vi.mocked(transactionsApi.getAll).mockResolvedValue({
      content: [TX],
      totalElements: 1,
      totalPages: 1,
      number: 0,
    })
    const store = useTransactionsStore()

    await store.fetchTransactions({})

    expect(store.transactions).toEqual([TX])
    expect(store.pagination.totalElements).toBe(1)
  })

  it('createTransaction calls API', async () => {
    vi.mocked(transactionsApi.create).mockResolvedValue(TX)
    const store = useTransactionsStore()

    const result = await store.createTransaction({
      amount: 25,
      date: '2026-03-09',
      type: 'EXPENSE',
      categoryId: 1,
    })

    expect(result).toEqual(TX)
  })

  it('updateTransaction calls API', async () => {
    const updated = { ...TX, amount: 30 }
    vi.mocked(transactionsApi.update).mockResolvedValue(updated)
    const store = useTransactionsStore()

    const result = await store.updateTransaction(1, {
      amount: 30,
      date: '2026-03-09',
      type: 'EXPENSE',
      categoryId: 1,
    })

    expect(result.amount).toBe(30)
  })

  it('deleteTransaction calls API', async () => {
    vi.mocked(transactionsApi.delete).mockResolvedValue(undefined)
    const store = useTransactionsStore()

    await store.deleteTransaction(1)

    expect(transactionsApi.delete).toHaveBeenCalledWith(1)
  })

  it('fetchMonthlySummary populates summary', async () => {
    const summary = {
      totalIncome: 5000,
      totalExpenses: 3000,
      netSavings: 2000,
      byCategory: [],
    }
    vi.mocked(transactionsApi.getSummary).mockResolvedValue(summary)
    const store = useTransactionsStore()

    await store.fetchMonthlySummary('2026-03')

    expect(store.monthlySummary).toEqual(summary)
  })
})
