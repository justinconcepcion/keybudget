<template>
  <div class="px-6 py-8 max-w-7xl mx-auto">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">Accounts</h1>
        <p class="mt-1 text-sm text-gray-500">Manage your connected financial providers.</p>
      </div>
      <button
        class="flex items-center gap-2 px-4 py-2 bg-primary-600 text-white text-sm font-medium rounded-lg hover:bg-primary-700 transition-colors"
        @click="showConnectModal = true"
      >
        <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="M12 4v16m8-8H4" />
        </svg>
        Connect Provider
      </button>
    </div>

    <div v-if="loading" class="flex items-center justify-center h-64 text-gray-400 text-sm">
      Loading…
    </div>

    <template v-else>
      <!-- Providers -->
      <div v-if="providers.length === 0" class="bg-white rounded-2xl border border-gray-200 flex items-center justify-center h-64 text-gray-400 text-sm">
        <div class="text-center">
          <p class="font-medium text-gray-500">No providers connected</p>
          <p class="text-xs mt-1">Connect Coinbase, Bitcoin, or other accounts to get started.</p>
        </div>
      </div>

      <div v-else class="space-y-6">
        <div
          v-for="provider in providers"
          :key="provider.credentialId"
          class="bg-white rounded-2xl border border-gray-200 overflow-hidden"
        >
          <!-- Provider header -->
          <div class="px-6 py-4 border-b border-gray-100 flex items-center justify-between">
            <div class="flex items-center gap-3">
              <div
                class="w-10 h-10 rounded-lg flex items-center justify-center text-xs font-bold text-white"
                :class="providerColor(provider.providerType)"
              >
                {{ providerIcon(provider.providerType) }}
              </div>
              <div>
                <p class="text-sm font-semibold text-gray-900">{{ providerLabel(provider.providerType) }}</p>
                <div class="flex items-center gap-2 text-xs text-gray-500">
                  <span
                    class="inline-flex items-center gap-1 px-1.5 py-0.5 rounded-full text-xs font-medium"
                    :class="statusClass(provider.status)"
                  >
                    {{ provider.status }}
                  </span>
                  <span v-if="provider.lastSyncedAt">
                    Synced {{ timeAgo(provider.lastSyncedAt) }}
                  </span>
                </div>
              </div>
            </div>
            <div class="flex items-center gap-2">
              <button
                class="px-3 py-1.5 text-xs font-medium text-primary-700 border border-primary-200 rounded-lg hover:bg-primary-50 transition-colors disabled:opacity-50"
                :disabled="syncing === provider.credentialId"
                @click="syncProvider(provider.credentialId)"
              >
                {{ syncing === provider.credentialId ? 'Syncing…' : 'Sync' }}
              </button>
              <button
                class="px-3 py-1.5 text-xs font-medium text-red-700 border border-red-200 rounded-lg hover:bg-red-50 transition-colors"
                @click="openDisconnectModal(provider)"
              >
                Disconnect
              </button>
            </div>
          </div>

          <!-- Provider error -->
          <div v-if="provider.errorMessage" class="px-6 py-2 bg-red-50 text-sm text-red-700">
            {{ provider.errorMessage }}
          </div>

          <!-- Accounts under this provider -->
          <div v-if="accountsByProvider(provider.credentialId).length > 0">
            <table class="w-full text-sm">
              <thead>
                <tr class="border-b border-gray-50">
                  <th class="px-6 py-2 text-left text-xs font-semibold text-gray-500 uppercase">Account</th>
                  <th class="px-6 py-2 text-left text-xs font-semibold text-gray-500 uppercase">Type</th>
                  <th class="px-6 py-2 text-right text-xs font-semibold text-gray-500 uppercase">Balance</th>
                  <th class="px-6 py-2 text-right text-xs font-semibold text-gray-500 uppercase">USD Value</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-50">
                <tr
                  v-for="acc in accountsByProvider(provider.credentialId)"
                  :key="acc.id"
                  class="hover:bg-gray-50 transition-colors"
                >
                  <td class="px-6 py-3 text-gray-800 font-medium">{{ acc.displayName }}</td>
                  <td class="px-6 py-3 text-gray-500">{{ accountTypeLabel(acc.accountType) }}</td>
                  <td class="px-6 py-3 text-right tabular-nums text-gray-600">
                    {{ acc.balance.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 8 }) }} {{ acc.currency }}
                  </td>
                  <td class="px-6 py-3 text-right font-semibold tabular-nums text-gray-900">
                    {{ formatMoney(acc.balanceUsd) }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="px-6 py-4 text-sm text-gray-400">No accounts discovered yet.</div>
        </div>
      </div>
    </template>

    <!-- Connect Provider modal -->
    <div
      v-if="showConnectModal"
      class="fixed inset-0 z-50 flex items-center justify-center bg-gray-900/50"
      @mousedown.self="showConnectModal = false"
    >
      <div class="bg-white rounded-2xl shadow-xl w-full max-w-md mx-4 p-6">
        <div class="flex items-center justify-between mb-5">
          <h2 class="text-lg font-semibold text-gray-900">Connect Provider</h2>
          <button class="text-gray-400 hover:text-gray-600" @click="showConnectModal = false">
            <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <form class="space-y-4" @submit.prevent="submitConnect">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Provider</label>
            <select
              v-model="connectForm.providerType"
              required
              class="w-full px-3 py-2 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            >
              <option value="" disabled>Select a provider</option>
              <option value="COINBASE">Coinbase</option>
              <option value="BITCOIN_WALLET">Bitcoin Wallet</option>
              <option value="M1_FINANCE">M1 Finance</option>
              <option value="MARCUS">Marcus by Goldman Sachs</option>
            </select>
          </div>

          <!-- Dynamic credential fields -->
          <template v-if="connectForm.providerType === 'COINBASE'">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">API Key</label>
              <input v-model="connectForm.credentials.apiKey" type="text" required
                class="w-full px-3 py-2 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent">
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">API Secret</label>
              <input v-model="connectForm.credentials.apiSecret" type="password" required
                class="w-full px-3 py-2 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent">
            </div>
          </template>
          <template v-else-if="connectForm.providerType === 'BITCOIN_WALLET'">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Bitcoin Address</label>
              <input v-model="connectForm.credentials.address" type="text" required placeholder="bc1..."
                class="w-full px-3 py-2 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent">
            </div>
          </template>
          <template v-else-if="connectForm.providerType">
            <div class="text-sm text-gray-500 bg-gray-50 rounded-lg p-3">
              Plaid integration coming soon. This provider will connect via Plaid Link.
            </div>
          </template>

          <p v-if="connectFormError" class="text-sm text-red-600">{{ connectFormError }}</p>

          <div class="flex gap-3 pt-1">
            <button type="button"
              class="flex-1 px-4 py-2 text-sm font-medium text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
              @click="showConnectModal = false">
              Cancel
            </button>
            <button type="submit" :disabled="submittingConnect || !connectForm.providerType"
              class="flex-1 px-4 py-2 text-sm font-medium text-white bg-primary-600 rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors">
              {{ submittingConnect ? 'Connecting…' : 'Connect' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Disconnect confirmation modal -->
    <div
      v-if="showDisconnectModal"
      class="fixed inset-0 z-50 flex items-center justify-center bg-gray-900/50"
      @mousedown.self="showDisconnectModal = false"
    >
      <div class="bg-white rounded-2xl shadow-xl w-full max-w-sm mx-4 p-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-2">Disconnect Provider</h2>
        <p class="text-sm text-gray-600 mb-5">
          Disconnect <span class="font-medium">{{ providerLabel(disconnectTarget?.providerType ?? 'COINBASE') }}</span>?
          Account data will be removed.
        </p>
        <p v-if="disconnectError" class="text-sm text-red-600 mb-4">{{ disconnectError }}</p>
        <div class="flex gap-3">
          <button type="button"
            class="flex-1 px-4 py-2 text-sm font-medium text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
            @click="showDisconnectModal = false">
            Cancel
          </button>
          <button type="button" :disabled="submittingDisconnect"
            class="flex-1 px-4 py-2 text-sm font-medium text-white bg-red-600 rounded-lg hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            @click="confirmDisconnect">
            {{ submittingDisconnect ? 'Disconnecting…' : 'Disconnect' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, reactive, onMounted } from 'vue'
  import { integrationsApi } from '@/api/integrations'
  import { formatMoney } from '@/utils/formatting'
  import type { AccountResponse, ProviderStatusResponse, ProviderType, AccountType } from '@/types'

  const loading = ref(false)
  const providers = ref<ProviderStatusResponse[]>([])
  const accounts = ref<AccountResponse[]>([])
  const syncing = ref<number | null>(null)

  function providerLabel(type: ProviderType): string {
    const map: Record<ProviderType, string> = {
      COINBASE: 'Coinbase',
      BITCOIN_WALLET: 'Bitcoin Wallet',
      M1_FINANCE: 'M1 Finance',
      MARCUS: 'Marcus by Goldman Sachs',
    }
    return map[type] ?? type
  }

  function providerIcon(type: ProviderType): string {
    const map: Record<ProviderType, string> = { COINBASE: 'CB', BITCOIN_WALLET: 'BTC', M1_FINANCE: 'M1', MARCUS: 'GS' }
    return map[type] ?? '?'
  }

  function providerColor(type: ProviderType): string {
    const map: Record<ProviderType, string> = { COINBASE: 'bg-blue-600', BITCOIN_WALLET: 'bg-orange-500', M1_FINANCE: 'bg-emerald-600', MARCUS: 'bg-indigo-600' }
    return map[type] ?? 'bg-gray-500'
  }

  function accountTypeLabel(type: AccountType): string {
    const map: Record<AccountType, string> = { CRYPTO_WALLET: 'Crypto', BROKERAGE: 'Brokerage', SAVINGS: 'Savings', CHECKING: 'Checking' }
    return map[type] ?? type
  }

  function statusClass(status: string): string {
    if (status === 'OK') return 'bg-emerald-100 text-emerald-700'
    if (status === 'ERROR') return 'bg-red-100 text-red-700'
    return 'bg-gray-100 text-gray-600'
  }

  function timeAgo(iso: string): string {
    const diff = Date.now() - new Date(iso).getTime()
    const mins = Math.floor(diff / 60000)
    if (mins < 1) return 'just now'
    if (mins < 60) return `${mins}m ago`
    const hrs = Math.floor(mins / 60)
    if (hrs < 24) return `${hrs}h ago`
    return `${Math.floor(hrs / 24)}d ago`
  }

  function accountsByProvider(credentialId: number): AccountResponse[] {
    return accounts.value.filter((a) => a.credentialId === credentialId && a.active)
  }

  async function syncProvider(credentialId: number) {
    syncing.value = credentialId
    try {
      await integrationsApi.sync(credentialId)
      await loadData()
    } catch {
      // Error shown via provider status
    } finally {
      syncing.value = null
    }
  }

  // ── Connect ────────────────────────────────────────────────────────────────

  const showConnectModal = ref(false)
  const submittingConnect = ref(false)
  const connectFormError = ref('')

  const connectForm = reactive({
    providerType: '' as ProviderType | '',
    credentials: {} as Record<string, string>,
  })

  async function submitConnect() {
    if (!connectForm.providerType) return
    connectFormError.value = ''
    submittingConnect.value = true
    try {
      await integrationsApi.connect({
        providerType: connectForm.providerType,
        credentials: connectForm.credentials,
      })
      showConnectModal.value = false
      connectForm.providerType = ''
      connectForm.credentials = {}
      await loadData()
    } catch {
      connectFormError.value = 'Failed to connect provider. Check your credentials.'
    } finally {
      submittingConnect.value = false
    }
  }

  // ── Disconnect ─────────────────────────────────────────────────────────────

  const showDisconnectModal = ref(false)
  const submittingDisconnect = ref(false)
  const disconnectError = ref('')
  const disconnectTarget = ref<ProviderStatusResponse | null>(null)

  function openDisconnectModal(provider: ProviderStatusResponse) {
    disconnectTarget.value = provider
    disconnectError.value = ''
    showDisconnectModal.value = true
  }

  async function confirmDisconnect() {
    if (!disconnectTarget.value) return
    submittingDisconnect.value = true
    disconnectError.value = ''
    try {
      await integrationsApi.disconnect(disconnectTarget.value.credentialId)
      showDisconnectModal.value = false
      await loadData()
    } catch {
      disconnectError.value = 'Failed to disconnect. Please try again.'
    } finally {
      submittingDisconnect.value = false
    }
  }

  async function loadData() {
    const [p, a] = await Promise.all([integrationsApi.getProviders(), integrationsApi.getAccounts()])
    providers.value = p
    accounts.value = a
  }

  onMounted(async () => {
    loading.value = true
    try {
      await loadData()
    } catch {
      // Empty state shown
    } finally {
      loading.value = false
    }
  })
</script>
