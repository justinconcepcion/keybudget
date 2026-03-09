<template>
  <div class="px-6 py-8 max-w-7xl mx-auto">
    <div class="mb-8">
      <h1 class="text-2xl font-bold text-gray-900">
        Good {{ greeting }}, {{ firstName }}
      </h1>
      <p class="mt-1 text-sm text-gray-500">
        Here's your financial overview for {{ currentMonth }}.
      </p>
    </div>

    <!-- Summary cards -->
    <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4 mb-8">
      <SummaryCard
        v-for="card in summaryCards"
        :key="card.label"
        :label="card.label"
        :value="card.value"
        :trend="card.trend"
        :icon="card.icon"
        :icon-bg="card.iconBg"
      />
    </div>

    <!-- Lower grid -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- Spending by Category -->
      <div class="lg:col-span-2 bg-white rounded-2xl border border-gray-200 p-6">
        <h2 class="text-base font-semibold text-gray-900 mb-4">
          Spending by Category
        </h2>
        <div
          v-if="summaryLoading"
          class="flex items-center justify-center h-48 text-gray-400 text-sm"
        >
          Loading…
        </div>
        <div
          v-else-if="categoryTotals.length === 0"
          class="flex items-center justify-center h-48 text-gray-400 text-sm border-2 border-dashed border-gray-200 rounded-xl"
        >
          No spending data for this month
        </div>
        <div
          v-else
          class="space-y-3"
        >
          <div
            v-for="cat in categoryTotals"
            :key="cat.categoryId"
            class="space-y-1"
          >
            <div class="flex justify-between text-sm">
              <span class="text-gray-700 font-medium">{{ cat.categoryName }}</span>
              <span class="text-gray-600">{{ formatMoney(cat.total) }}</span>
            </div>
            <div class="w-full h-2 bg-gray-100 rounded-full overflow-hidden">
              <div
                class="h-full rounded-full bg-primary-500 transition-all"
                :style="{ width: `${categoryBarPct(cat.total)}%` }"
              />
            </div>
          </div>
        </div>
      </div>

      <!-- Budget Progress -->
      <div class="bg-white rounded-2xl border border-gray-200 p-6">
        <h2 class="text-base font-semibold text-gray-900 mb-4">
          Budget Progress
        </h2>
        <div
          v-if="budgetsLoading"
          class="text-sm text-gray-400"
        >
          Loading…
        </div>
        <div
          v-else-if="budgetsStore.budgets.length === 0"
          class="flex items-center justify-center h-32 text-sm text-gray-400 border-2 border-dashed border-gray-200 rounded-xl"
        >
          No budgets set
        </div>
        <div
          v-else
          class="space-y-4"
        >
          <div
            v-for="budget in budgetsStore.budgets"
            :key="budget.id"
            class="space-y-1"
          >
            <div class="flex justify-between text-sm">
              <div class="flex items-center gap-1.5">
                <span
                  class="inline-block w-2.5 h-2.5 rounded-full flex-shrink-0"
                  :style="{ backgroundColor: budget.categoryColor }"
                />
                <span class="text-gray-700">{{ budget.categoryName }}</span>
              </div>
              <span class="text-gray-500 tabular-nums">
                {{ formatMoney(budget.spentAmount) }} / {{ formatMoney(budget.limitAmount) }}
              </span>
            </div>
            <div class="w-full h-2 bg-gray-100 rounded-full overflow-hidden">
              <div
                class="h-full rounded-full transition-all"
                :class="budgetBarColor(budget)"
                :style="{ width: `${budgetPct(budget)}%` }"
              />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Recent Transactions -->
    <div class="mt-6 bg-white rounded-2xl border border-gray-200 p-6">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-base font-semibold text-gray-900">
          Recent Transactions
        </h2>
        <RouterLink
          to="/transactions"
          class="text-sm text-primary-600 hover:underline"
        >
          View all
        </RouterLink>
      </div>
      <div
        v-if="summaryLoading"
        class="flex items-center justify-center h-24 text-gray-400 text-sm"
      >
        Loading…
      </div>
      <div
        v-else-if="recentTransactions.length === 0"
        class="flex items-center justify-center h-24 text-gray-400 text-sm border-2 border-dashed border-gray-200 rounded-xl"
      >
        No transactions this month
      </div>
      <ul
        v-else
        class="divide-y divide-gray-100"
      >
        <li
          v-for="tx in recentTransactions"
          :key="tx.id"
          class="flex items-center justify-between py-3 first:pt-0 last:pb-0"
        >
          <div class="flex flex-col min-w-0">
            <span class="text-sm font-medium text-gray-800 truncate">
              {{ tx.description || tx.categoryName }}
            </span>
            <span class="text-xs text-gray-400 mt-0.5">
              {{ tx.categoryName }} · {{ formatDate(tx.date) }}
            </span>
          </div>
          <span
            class="ml-4 text-sm font-semibold tabular-nums flex-shrink-0"
            :class="tx.type === 'INCOME' ? 'text-emerald-600' : 'text-red-600'"
          >
            {{ tx.type === 'INCOME' ? '+' : '-' }}{{ formatMoney(tx.amount) }}
          </span>
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, computed, onMounted } from 'vue'
  import { RouterLink } from 'vue-router'
  import { useAuthStore } from '@/stores/auth'
  import { useTransactionsStore } from '@/stores/transactions'
  import { useBudgetsStore } from '@/stores/budgets'
  import SummaryCard from '@/components/dashboard/SummaryCard.vue'
  import type { BudgetResponse } from '@/types'

  const authStore = useAuthStore()
  const transactionsStore = useTransactionsStore()
  const budgetsStore = useBudgetsStore()

  const summaryLoading = ref(false)
  const budgetsLoading = ref(false)

  const firstName = computed(() => authStore.user?.name?.split(' ')[0] ?? 'there')

  const greeting = computed(() => {
    const h = new Date().getHours()
    if (h < 12) return 'morning'
    if (h < 17) return 'afternoon'
    return 'evening'
  })

  const currentMonthKey = computed(() => {
    const now = new Date()
    const y = now.getFullYear()
    const m = String(now.getMonth() + 1).padStart(2, '0')
    return `${y}-${m}`
  })

  const currentMonth = computed(() =>
    new Date().toLocaleString('default', { month: 'long', year: 'numeric' }),
  )

  function formatMoney(value: number): string {
    return `$${value.toFixed(2)}`
  }

  function formatDate(dateStr: string): string {
    return new Date(dateStr + 'T00:00:00').toLocaleDateString('default', {
      month: 'short',
      day: 'numeric',
    })
  }

  const summary = computed(() => transactionsStore.monthlySummary)

  const summaryCards = computed(() => {
    const income = summary.value?.totalIncome ?? 0
    const expenses = summary.value?.totalExpenses ?? 0
    const net = summary.value?.netSavings ?? 0
    const rate = income > 0 ? Math.round((net / income) * 100) : null

    return [
      {
        label: 'Net Savings',
        value: formatMoney(net),
        trend: null,
        icon: 'balance' as const,
        iconBg: 'bg-primary-100 text-primary-600',
      },
      {
        label: 'Monthly Income',
        value: formatMoney(income),
        trend: null,
        icon: 'income' as const,
        iconBg: 'bg-blue-100 text-blue-600',
      },
      {
        label: 'Monthly Expenses',
        value: formatMoney(expenses),
        trend: null,
        icon: 'expense' as const,
        iconBg: 'bg-red-100 text-red-600',
      },
      {
        label: 'Savings Rate',
        value: rate !== null ? `${rate}%` : '—',
        trend: null,
        icon: 'savings' as const,
        iconBg: 'bg-purple-100 text-purple-600',
      },
    ]
  })

  const categoryTotals = computed(() => summary.value?.byCategory ?? [])

  const maxCategoryTotal = computed(() => Math.max(...categoryTotals.value.map((c) => c.total), 1))

  function categoryBarPct(total: number): number {
    return Math.min(Math.round((total / maxCategoryTotal.value) * 100), 100)
  }

  function budgetPct(budget: BudgetResponse): number {
    if (budget.limitAmount === 0) return 0
    return Math.min(Math.round((budget.spentAmount / budget.limitAmount) * 100), 100)
  }

  function budgetBarColor(budget: BudgetResponse): string {
    const pct = budgetPct(budget)
    if (pct > 90) return 'bg-red-500'
    if (pct > 70) return 'bg-yellow-400'
    return 'bg-primary-500'
  }

  const recentTransactions = computed(() => transactionsStore.transactions.slice(0, 5))

  onMounted(async () => {
    const month = currentMonthKey.value
    const now = new Date()
    const start = `${month}-01`
    const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate()
    const end = `${month}-${String(lastDay).padStart(2, '0')}`

    summaryLoading.value = true
    budgetsLoading.value = true

    await Promise.all([
      transactionsStore.fetchMonthlySummary(month).finally(() => (summaryLoading.value = false)),
      transactionsStore.fetchTransactions({ start, end, page: 0, size: 20 }),
      budgetsStore.fetchBudgets(month).finally(() => (budgetsLoading.value = false)),
    ])
  })
</script>
