import type { BudgetResponse } from '@/types'

export function formatMoney(value: number): string {
  return `$${value.toFixed(2)}`
}

export function formatDate(dateStr: string, includeYear = true): string {
  const options: Intl.DateTimeFormatOptions = {
    month: 'short',
    day: 'numeric',
    ...(includeYear && { year: 'numeric' }),
  }
  return new Date(dateStr + 'T00:00:00').toLocaleDateString('default', options)
}

export function budgetPct(budget: BudgetResponse): number {
  if (budget.limitAmount === 0) return 0
  return Math.min(Math.round((budget.spentAmount / budget.limitAmount) * 100), 100)
}

export function budgetBarColor(budget: BudgetResponse): string {
  const pct = budgetPct(budget)
  if (pct > 90) return 'bg-red-500'
  if (pct > 70) return 'bg-yellow-400'
  return 'bg-primary-500'
}
