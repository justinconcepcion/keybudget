<template>
  <div class="px-6 py-8 max-w-7xl mx-auto">
    <div class="mb-8">
      <h1 class="text-2xl font-bold text-gray-900">
        Good {{ greeting }}, {{ firstName }} 👋
      </h1>
      <p class="mt-1 text-sm text-gray-500">Here's your financial overview for {{ currentMonth }}.</p>
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
      <!-- Spending breakdown placeholder -->
      <div class="lg:col-span-2 bg-white rounded-2xl border border-gray-200 p-6">
        <h2 class="text-base font-semibold text-gray-900 mb-4">Spending Breakdown</h2>
        <div class="flex items-center justify-center h-48 text-gray-400 text-sm border-2 border-dashed border-gray-200 rounded-xl">
          Chart coming soon
        </div>
      </div>

      <!-- Budget progress placeholder -->
      <div class="bg-white rounded-2xl border border-gray-200 p-6">
        <h2 class="text-base font-semibold text-gray-900 mb-4">Budget Progress</h2>
        <div class="space-y-4">
          <div v-for="budget in placeholderBudgets" :key="budget.name" class="space-y-1">
            <div class="flex justify-between text-sm">
              <span class="text-gray-700">{{ budget.name }}</span>
              <span class="text-gray-500">{{ budget.spent }} / {{ budget.total }}</span>
            </div>
            <div class="w-full h-2 bg-gray-100 rounded-full overflow-hidden">
              <div
                class="h-full rounded-full transition-all"
                :class="budget.pct > 90 ? 'bg-red-500' : budget.pct > 70 ? 'bg-yellow-400' : 'bg-primary-500'"
                :style="{ width: `${budget.pct}%` }"
              />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Recent transactions placeholder -->
    <div class="mt-6 bg-white rounded-2xl border border-gray-200 p-6">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-base font-semibold text-gray-900">Recent Transactions</h2>
        <RouterLink to="/transactions" class="text-sm text-primary-600 hover:underline">View all</RouterLink>
      </div>
      <div class="flex items-center justify-center h-24 text-gray-400 text-sm border-2 border-dashed border-gray-200 rounded-xl">
        Transactions coming soon
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed } from 'vue'
  import { RouterLink } from 'vue-router'
  import { useAuthStore } from '@/stores/auth'
  import SummaryCard from '@/components/dashboard/SummaryCard.vue'

  const authStore = useAuthStore()

  const firstName = computed(() => authStore.user?.name?.split(' ')[0] ?? 'there')

  const greeting = computed(() => {
    const h = new Date().getHours()
    if (h < 12) return 'morning'
    if (h < 17) return 'afternoon'
    return 'evening'
  })

  const currentMonth = computed(() =>
    new Date().toLocaleString('default', { month: 'long', year: 'numeric' }),
  )

  import type { SummaryCardProps } from '@/types'

  const summaryCards: SummaryCardProps[] = [
    {
      label: 'Total Balance',
      value: '$0.00',
      trend: null,
      icon: 'balance',
      iconBg: 'bg-primary-100 text-primary-600',
    },
    {
      label: 'Monthly Income',
      value: '$0.00',
      trend: null,
      icon: 'income',
      iconBg: 'bg-blue-100 text-blue-600',
    },
    {
      label: 'Monthly Expenses',
      value: '$0.00',
      trend: null,
      icon: 'expense',
      iconBg: 'bg-red-100 text-red-600',
    },
    {
      label: 'Savings Rate',
      value: '—',
      trend: null,
      icon: 'savings',
      iconBg: 'bg-purple-100 text-purple-600',
    },
  ]

  const placeholderBudgets = [
    { name: 'Groceries', spent: '$0', total: '$400', pct: 0 },
    { name: 'Dining Out', spent: '$0', total: '$200', pct: 0 },
    { name: 'Transport', spent: '$0', total: '$150', pct: 0 },
  ]
</script>
