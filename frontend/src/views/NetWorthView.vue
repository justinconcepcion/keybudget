<template>
  <div class="px-6 py-8 max-w-7xl mx-auto">
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white">
        Net Worth
      </h1>
      <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">
        Your total wealth across all connected accounts.
      </p>
    </div>

    <div
      v-if="loading"
      class="flex items-center justify-center h-64 text-gray-400 dark:text-gray-500 text-sm"
    >
      Loading…
    </div>

    <div
      v-else-if="loadError"
      class="bg-white dark:bg-gray-800 rounded-2xl border border-gray-200 dark:border-gray-700 flex flex-col items-center justify-center h-64 gap-3"
    >
      <p class="text-sm text-red-600">
        {{ loadError }}
      </p>
      <button
        class="px-4 py-2 text-sm font-medium text-primary-700 border border-primary-200 rounded-lg hover:bg-primary-50 transition-colors"
        @click="loadAll"
      >
        Retry
      </button>
    </div>

    <template v-else>
      <!-- Total net worth card -->
      <div class="bg-white dark:bg-gray-800 rounded-2xl border border-gray-200 dark:border-gray-700 p-6 mb-6">
        <p class="text-sm text-gray-500 dark:text-gray-400 mb-1">
          Total Net Worth
        </p>
        <p class="text-3xl font-bold text-gray-900 dark:text-white">
          {{ formatMoney(store.netWorth?.totalNetWorthUsd ?? 0) }}
        </p>
        <p
          v-if="store.netWorth?.asOf"
          class="text-xs text-gray-400 dark:text-gray-500 mt-2"
        >
          As of {{ new Date(store.netWorth.asOf).toLocaleString() }}
        </p>
      </div>

      <!-- Breakdown cards -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
        <!-- By Provider -->
        <div class="bg-white dark:bg-gray-800 rounded-2xl border border-gray-200 dark:border-gray-700 p-6">
          <h2 class="text-sm font-semibold text-gray-700 dark:text-gray-300 uppercase tracking-wide mb-4">
            By Provider
          </h2>
          <div
            v-if="(store.netWorth?.byProvider ?? []).length === 0"
            class="text-sm text-gray-400 dark:text-gray-500"
          >
            No connected providers.
          </div>
          <div
            v-else
            class="space-y-3"
          >
            <div
              v-for="p in store.netWorth!.byProvider"
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
                  <p class="text-sm font-medium text-gray-900 dark:text-white">
                    {{ providerLabel(p.providerType) }}
                  </p>
                  <p class="text-xs text-gray-500 dark:text-gray-400">
                    {{ p.accountCount }} account{{ p.accountCount !== 1 ? 's' : '' }}
                  </p>
                </div>
              </div>
              <p class="text-sm font-semibold text-gray-900 dark:text-white tabular-nums">
                {{ formatMoney(p.totalUsd) }}
              </p>
            </div>
          </div>
        </div>

        <!-- By Account Type -->
        <div class="bg-white dark:bg-gray-800 rounded-2xl border border-gray-200 dark:border-gray-700 p-6">
          <h2 class="text-sm font-semibold text-gray-700 dark:text-gray-300 uppercase tracking-wide mb-4">
            By Account Type
          </h2>
          <div
            v-if="(store.netWorth?.byAccountType ?? []).length === 0"
            class="text-sm text-gray-400 dark:text-gray-500"
          >
            No accounts yet.
          </div>
          <div
            v-else
            class="space-y-3"
          >
            <div
              v-for="a in store.netWorth!.byAccountType"
              :key="a.accountType"
              class="flex items-center justify-between"
            >
              <div>
                <p class="text-sm font-medium text-gray-900 dark:text-white">
                  {{ accountTypeLabel(a.accountType) }}
                </p>
                <p class="text-xs text-gray-500 dark:text-gray-400">
                  {{ a.accountCount }} account{{ a.accountCount !== 1 ? 's' : '' }}
                </p>
              </div>
              <p class="text-sm font-semibold text-gray-900 dark:text-white tabular-nums">
                {{ formatMoney(a.totalUsd) }}
              </p>
            </div>
          </div>
        </div>
      </div>

      <!-- History table -->
      <div class="bg-white dark:bg-gray-800 rounded-2xl border border-gray-200 dark:border-gray-700 p-6">
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-sm font-semibold text-gray-700 dark:text-gray-300 uppercase tracking-wide">
            History
          </h2>
          <select
            v-model.number="historyDays"
            class="text-sm border border-gray-300 dark:border-gray-600 rounded-lg px-3 py-1.5 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            @change="loadHistory"
          >
            <option :value="7">
              7 days
            </option>
            <option :value="30">
              30 days
            </option>
            <option :value="90">
              90 days
            </option>
            <option :value="365">
              1 year
            </option>
          </select>
        </div>

        <div
          v-if="historyLoading"
          class="text-sm text-gray-400 dark:text-gray-500 text-center py-8"
        >
          Loading history…
        </div>
        <div
          v-else-if="store.netWorthHistory.length === 0"
          class="text-sm text-gray-400 dark:text-gray-500 text-center py-8"
        >
          No history data yet. Connect accounts and sync to start tracking.
        </div>
        <div
          v-else
          class="overflow-x-auto"
        >
          <table class="w-full text-sm">
            <thead>
              <tr class="border-b border-gray-100 dark:border-gray-700">
                <th class="text-left px-4 py-2 text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase">
                  Date
                </th>
                <th class="text-right px-4 py-2 text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase">
                  Net Worth
                </th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-50 dark:divide-gray-700">
              <tr
                v-for="dp in store.netWorthHistory"
                :key="dp.date"
                class="hover:bg-gray-50 dark:hover:bg-gray-700"
              >
                <td class="px-4 py-2 text-gray-600 dark:text-gray-400">
                  {{ dp.date }}
                </td>
                <td class="px-4 py-2 text-right font-semibold text-gray-900 dark:text-white tabular-nums">
                  {{ formatMoney(dp.totalUsd) }}
                </td>
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
  import { useIntegrationsStore } from '@/stores/integrations'
  import { formatMoney } from '@/utils/formatting'
  import { providerLabel, providerIcon, providerColor, accountTypeLabel } from '@/utils/providers'

  const store = useIntegrationsStore()
  const loading = ref(false)
  const loadError = ref<string | null>(null)
  const historyDays = ref(30)
  const historyLoading = ref(false)

  async function loadHistory() {
    historyLoading.value = true
    try {
      await store.fetchNetWorthHistory(historyDays.value)
    } catch {
      store.netWorthHistory = []
    } finally {
      historyLoading.value = false
    }
  }

  async function loadAll() {
    loading.value = true
    loadError.value = null
    try {
      await Promise.all([store.fetchNetWorth(), loadHistory()])
    } catch {
      loadError.value = 'Failed to load net worth data. Please try again.'
    } finally {
      loading.value = false
    }
  }

  onMounted(loadAll)
</script>
