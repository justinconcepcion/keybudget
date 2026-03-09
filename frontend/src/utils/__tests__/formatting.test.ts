import { describe, it, expect } from 'vitest'
import { formatMoney, formatDate, budgetPct, budgetBarColor } from '../formatting'
import type { BudgetResponse } from '@/types'

function makeBudget(limit: number, spent: number): BudgetResponse {
  return {
    id: 1,
    categoryId: 1,
    categoryName: 'Food',
    categoryColor: '#000',
    monthYear: '2026-03',
    limitAmount: limit,
    spentAmount: spent,
    remainingAmount: limit - spent,
  }
}

describe('formatMoney', () => {
  it('formats positive values', () => {
    expect(formatMoney(1234.5)).toBe('$1,234.50')
  })

  it('formats zero', () => {
    expect(formatMoney(0)).toBe('$0.00')
  })

  it('formats negative values', () => {
    expect(formatMoney(-42.1)).toBe('-$42.10')
  })

  it('rounds to two decimals', () => {
    expect(formatMoney(9.999)).toBe('$10.00')
  })

  it('formats with different currency', () => {
    expect(formatMoney(100, 'EUR')).toContain('100.00')
  })

  it('defaults to USD', () => {
    expect(formatMoney(50)).toBe('$50.00')
  })
})

describe('formatDate', () => {
  it('formats with year by default', () => {
    const result = formatDate('2026-03-09')
    expect(result).toContain('Mar')
    expect(result).toContain('9')
    expect(result).toContain('2026')
  })

  it('formats without year when includeYear is false', () => {
    const result = formatDate('2026-03-09', false)
    expect(result).toContain('Mar')
    expect(result).toContain('9')
    expect(result).not.toContain('2026')
  })
})

describe('budgetPct', () => {
  it('returns 0 when limit is 0', () => {
    expect(budgetPct(makeBudget(0, 50))).toBe(0)
  })

  it('calculates percentage correctly', () => {
    expect(budgetPct(makeBudget(100, 50))).toBe(50)
  })

  it('caps at 100', () => {
    expect(budgetPct(makeBudget(100, 200))).toBe(100)
  })

  it('rounds to nearest integer', () => {
    expect(budgetPct(makeBudget(3, 1))).toBe(33)
  })
})

describe('budgetBarColor', () => {
  it('returns green for low usage', () => {
    expect(budgetBarColor(makeBudget(100, 50))).toBe('bg-primary-500')
  })

  it('returns yellow for 71-90%', () => {
    expect(budgetBarColor(makeBudget(100, 80))).toBe('bg-yellow-400')
  })

  it('returns red for over 90%', () => {
    expect(budgetBarColor(makeBudget(100, 95))).toBe('bg-red-500')
  })
})
