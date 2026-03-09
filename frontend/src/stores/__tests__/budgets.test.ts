import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useBudgetsStore } from '../budgets'

vi.mock('@/api/budgets', () => ({
  budgetsApi: {
    getAll: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
  },
}))

import { budgetsApi } from '@/api/budgets'

const BUDGET = {
  id: 1,
  categoryId: 1,
  categoryName: 'Food',
  categoryColor: '#f00',
  monthYear: '2026-03',
  limitAmount: 500,
  spentAmount: 200,
  remainingAmount: 300,
}

describe('useBudgetsStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('fetchBudgets loads budgets', async () => {
    vi.mocked(budgetsApi.getAll).mockResolvedValue([BUDGET])
    const store = useBudgetsStore()

    await store.fetchBudgets('2026-03')

    expect(store.budgets).toEqual([BUDGET])
    expect(budgetsApi.getAll).toHaveBeenCalledWith('2026-03')
  })

  it('createBudget appends to list', async () => {
    vi.mocked(budgetsApi.create).mockResolvedValue(BUDGET)
    const store = useBudgetsStore()

    const result = await store.createBudget({
      categoryId: 1,
      monthYear: '2026-03',
      limitAmount: 500,
    })

    expect(result).toEqual(BUDGET)
    expect(store.budgets).toContainEqual(BUDGET)
  })

  it('updateBudget replaces in list', async () => {
    const store = useBudgetsStore()
    store.budgets = [BUDGET]
    const updated = { ...BUDGET, limitAmount: 600 }
    vi.mocked(budgetsApi.update).mockResolvedValue(updated)

    await store.updateBudget(1, { limitAmount: 600 })

    expect(store.budgets[0].limitAmount).toBe(600)
  })

  it('deleteBudget removes from list', async () => {
    const store = useBudgetsStore()
    store.budgets = [BUDGET]
    vi.mocked(budgetsApi.delete).mockResolvedValue(undefined)

    await store.deleteBudget(1)

    expect(store.budgets).toHaveLength(0)
  })
})
