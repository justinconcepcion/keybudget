import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useCategoriesStore } from '../categories'

vi.mock('@/api/categories', () => ({
  categoriesApi: {
    getAll: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
  },
}))

import { categoriesApi } from '@/api/categories'

const EXPENSE_CAT = { id: 1, name: 'Food', icon: '🍔', color: '#f00', type: 'EXPENSE' as const, isDefault: false }
const INCOME_CAT = { id: 2, name: 'Salary', icon: '💰', color: '#0f0', type: 'INCOME' as const, isDefault: false }

describe('useCategoriesStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('starts with empty categories', () => {
    const store = useCategoriesStore()
    expect(store.categories).toEqual([])
    expect(store.expenseCategories).toEqual([])
    expect(store.incomeCategories).toEqual([])
  })

  it('fetchCategories loads and filters categories', async () => {
    vi.mocked(categoriesApi.getAll).mockResolvedValue([EXPENSE_CAT, INCOME_CAT])
    const store = useCategoriesStore()

    await store.fetchCategories()

    expect(store.categories).toHaveLength(2)
    expect(store.expenseCategories).toEqual([EXPENSE_CAT])
    expect(store.incomeCategories).toEqual([INCOME_CAT])
  })

  it('createCategory appends to list', async () => {
    const store = useCategoriesStore()
    vi.mocked(categoriesApi.create).mockResolvedValue(EXPENSE_CAT)

    const result = await store.createCategory({ name: 'Food', type: 'EXPENSE' })

    expect(result).toEqual(EXPENSE_CAT)
    expect(store.categories).toContainEqual(EXPENSE_CAT)
  })

  it('updateCategory replaces in list', async () => {
    const store = useCategoriesStore()
    store.categories = [EXPENSE_CAT]
    const updated = { ...EXPENSE_CAT, name: 'Groceries' }
    vi.mocked(categoriesApi.update).mockResolvedValue(updated)

    await store.updateCategory(1, { name: 'Groceries', type: 'EXPENSE' })

    expect(store.categories[0].name).toBe('Groceries')
  })

  it('deleteCategory removes from list', async () => {
    const store = useCategoriesStore()
    store.categories = [EXPENSE_CAT, INCOME_CAT]
    vi.mocked(categoriesApi.delete).mockResolvedValue(undefined)

    await store.deleteCategory(1)

    expect(store.categories).toHaveLength(1)
    expect(store.categories[0].id).toBe(2)
  })
})
