<template>
  <div class="px-6 py-8 max-w-7xl mx-auto">
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-gray-900">Net Worth</h1>
      <p class="mt-1 text-sm text-gray-500">Your total wealth across all connected accounts.</p>
    </div>

    <div v-if="loading" class="flex items-center justify-center h-64 text-gray-400 text-sm">
      Loading…
    </div>

    <template v-else>
      <!-- Total net worth card -->
      <div class="bg-white rounded-2xl border border-gray-200 p-6 mb-6">
        <p class="text-sm text-gray-500 mb-1">Total Net Worth</p>
        <p class="text-3xl font-bold text-gray-900">{{ formatMoney(netWorth?.totalNetWorthUsd ?? 0) }}</p>
        <p v-if="netWorth?.asOf" class="text-xs text-gray-400 mt-2">
          As of {{ new Date(netWorth.asOf).toLocaleString() }}
        </p>
      </div>

      <!-- Breakdown cards -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
        <!-- By Provider -->
        <div class="bg-white rounded-2xl border border-gray-200 p-6">
          <h2 class="text-sm font-semibold text-gray-700 uppercase tracking-wide mb-4">By Provider</h2>
          <div v-if="(netWorth?.byProvider ?? []).length === 0" class="text-sm text-gray-400">
            No connected providers.
          </div>
          <div v-else class="space-y-3">
            <div
              v-for="p in netWorth!.byProvider"
              :key="p.providerType"
              class="flex items-center justify-between"
            >
              <div class="flex items-center gap-3">
                <div
                  class="w-8 h-8 rounded-lg flex items-center justify-center text-xs font-bold text-white"
                  :class="providerColor(p.providerType)"
                >
                  {{ providerIcon(p.providerType) }}
                </div>
                <div>
                  <p class="text-sm font-medium text-gray-900">{{ providerLabel(p.providerType) }}</p>
                  <p class="text-xs text-gray-500">{{ p.accountCount }} account{{ p.accountCount !== 1 ? 's' : '' }}</p>
                </div>
              </div>
              <p class="text-sm font-semibold text-gray-900 tabular-nums">{{ formatMoney(p.totalUsd) }}</p>
            </div>
          </div>
        </div>

        <!-- By Account Type -->
        <div class="bg-white rounded-2xl border border-gray-200 p-6">
          <h2 class="text-sm font-semibold text-gray-700 uppercase tracking-wide mb-4">By Account Type</h2>
          <div v-if="(netWorth?.byAccountType ?? []).length === 0" class="text-sm text-gray-400">
            No accounts yet.
          </div>
          <div v-else class="space-y-3">
            <div
              v-for="a in netWorth!.byAccountType"
              :key="a.accountType"
              class="flex items-center justify-between"
            >
              <div>
                <p class="text-sm font-medium text-gray-900">{{ accountTypeLabel(a.accountType) }}</p>
                <p class="text-xs text-gray-500">{{ a.accountCount }} account{{ a.accountCount !== 1 ? 's' : '' }}</p>
              </div>
              <p class="text-sm font-semibold text-gray-900 tabular-nums">{{ formatMoney(a.totalUsd) }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- History chart (simple table for now) -->
      <div class="bg-white rounded-2xl border border-gray-200 p-6">
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-sm font-semibold text-gray-700 uppercase tracking-wide">History</h2>
          <select
            v-model.number="historyDays"
            class="text-sm border border-gray-300 rounded-lg px-3 py-1.5 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            @change="loadHistory"
          >
            <option :value="7">7 days</option>
            <option :value="30">30 days</option>
            <option :value="90">90 days</option>
            <option :value="365">1 year</option>
          </select>
        </div>

        <div v-if="historyLoading" class="text-sm text-gray-400 text-center py-8">Loading history…</div>
        <div v-else-if="history.length === 0" class="text-sm text-gray-400 text-center py-8">
          No history data yet. Connect accounts and sync to start tracking.
        </div>
        <div v-else class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead>
              <tr class="border-b border-gray-100">
                <th class="text-left px-4 py-2 text-xs font-semibold text-gray-500 uppercase">Date</th>
                <th class="text-right px-4 py-2 text-xs font-semibold text-gray-500 uppercase">Net Worth</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-50">
              <tr v-for="dp in history" :key="dp.date" class="hover:bg-gray-50">
                <td class="px-4 py-2 text-gray-600">{{ dp.date }}</td>
                <td class="px-4 py-2 text-right font-semibold text-gray-900 tabular-nums">{{ formatMoney(dp.totalUsd) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
  import { ref, onMounted } from 'vue'
  import { integrationsApi } from '@/api/integrations'
  import { formatMoney } from '@/utils/formatting'
  import type { NetWorthResponse, NetWorthDataPoint, ProviderType, AccountType } from '@/types'

  const loading = ref(false)
  const netWorth = ref<NetWorthResponse | null>(null)
  const history = ref<NetWorthDataPoint[]>([])
  const historyDays = ref(30)
  const historyLoading = ref(false)

  function providerLabel(type: ProviderType): string {
    const map: Record<ProviderType, string> = {
      COINBASE: 'Coinbase',
      BITCOIN_WALLET: 'Bitcoin',
      M1_FINANCE: 'M1 Finance',
      MARCUS: 'Marcus',
    }
    return map[type] ?? type
  }

  function providerIcon(type: ProviderType): string {
    const map: Record<ProviderType, string> = {
      COINBASE: 'CB',
      BITCOIN_WALLET: 'BTC',
      M1_FINANCE: 'M1',
      MARCUS: 'GS',
    }
    return map[type] ?? '?'
  }

  function providerColor(type: ProviderType): string {
    const map: Record<ProviderType, string> = {
      COINBASE: 'bg-blue-600',
      BITCOIN_WALLET: 'bg-orange-500',
      M1_FINANCE: 'bg-emerald-600',
      MARCUS: 'bg-indigo-600',
    }
    return map[type] ?? 'bg-gray-500'
  }

  function accountTypeLabel(type: AccountType): string {
    const map: Record<AccountType, string> = {
      CRYPTO_WALLET: 'Crypto',
      BROKERAGE: 'Brokerage',
      SAVINGS: 'Savings',
      CHECKING: 'Checking',
    }
    return map[type] ?? type
  }

  async function loadHistory() {
    historyLoading.value = true
    try {
      const resp = await integrationsApi.getNetWorthHistory(historyDays.value)
      history.value = resp.dataPoints
    } catch {
      history.value = []
    } finally {
      historyLoading.value = false
    }
  }

  onMounted(async () => {
    loading.value = true
    try {
      const [nw] = await Promise.all([integrationsApi.getNetWorth(), loadHistory()])
      netWorth.value = nw
    } catch {
      // Empty state shown
    } finally {
      loading.value = false
    }
  })
</script>
