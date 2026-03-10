<template>
  <div class="px-6 py-8 max-w-7xl mx-auto">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">
          Accounts
        </h1>
        <p class="mt-1 text-sm text-gray-500">
          Manage your connected financial providers.
        </p>
      </div>
      <button
        class="flex items-center gap-2 px-4 py-2 bg-primary-600 text-white text-sm font-medium rounded-lg hover:bg-primary-700 transition-colors"
        @click="showConnectModal = true"
      >
        <svg
          class="w-4 h-4"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          stroke-width="2"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            d="M12 4v16m8-8H4"
          />
        </svg>
        Connect Provider
      </button>
    </div>

    <div
      v-if="store.loading"
      class="flex items-center justify-center h-64 text-gray-400 text-sm"
    >
      Loading…
    </div>

    <div
      v-else-if="store.error"
      class="bg-white rounded-2xl border border-gray-200 flex flex-col items-center justify-center h-64 gap-3"
    >
      <p class="text-sm text-red-600">
        {{ store.error }}
      </p>
      <button
        class="px-4 py-2 text-sm font-medium text-primary-700 border border-primary-200 rounded-lg hover:bg-primary-50 transition-colors"
        @click="loadData"
      >
        Retry
      </button>
    </div>

    <template v-else>
      <!-- Providers -->
      <div
        v-if="store.providers.length === 0"
        class="bg-white rounded-2xl border border-gray-200 flex items-center justify-center h-64 text-gray-400 text-sm"
      >
        <div class="text-center">
          <p class="font-medium text-gray-500">
            No providers connected
          </p>
          <p class="text-xs mt-1">
            Connect Coinbase, Bitcoin, or other accounts to get started.
          </p>
        </div>
      </div>

      <div
        v-else
        class="space-y-6"
      >
        <div
          v-for="provider in store.providers"
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
                <p class="text-sm font-semibold text-gray-900">
                  {{ providerLabel(provider.providerType) }}
                </p>
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
                @click="handleSync(provider.credentialId)"
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
          <div
            v-if="provider.errorMessage"
            class="px-6 py-2 bg-red-50 text-sm text-red-700"
          >
            {{ provider.errorMessage }}
          </div>

          <!-- Accounts under this provider -->
          <div v-if="store.accountsByProvider(provider.credentialId).length > 0">
            <table class="w-full text-sm">
              <thead>
                <tr class="border-b border-gray-50">
                  <th class="px-6 py-2 text-left text-xs font-semibold text-gray-500 uppercase">
                    Account
                  </th>
                  <th class="px-6 py-2 text-left text-xs font-semibold text-gray-500 uppercase">
                    Type
                  </th>
                  <th class="px-6 py-2 text-right text-xs font-semibold text-gray-500 uppercase">
                    Balance
                  </th>
                  <th class="px-6 py-2 text-right text-xs font-semibold text-gray-500 uppercase">
                    USD Value
                  </th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-50">
                <tr
                  v-for="acc in store.accountsByProvider(provider.credentialId)"
                  :key="acc.id"
                  class="hover:bg-gray-50 transition-colors"
                >
                  <td class="px-6 py-3 text-gray-800 font-medium">
                    {{ acc.displayName }}
                  </td>
                  <td class="px-6 py-3 text-gray-500">
                    {{ accountTypeLabel(acc.accountType) }}
                  </td>
                  <td class="px-6 py-3 text-right tabular-nums text-gray-600">
                    {{
                      acc.balance.toLocaleString(undefined, {
                        minimumFractionDigits: 2,
                        maximumFractionDigits: 8,
                      })
                    }}
                    {{ acc.currency }}
                  </td>
                  <td class="px-6 py-3 text-right font-semibold tabular-nums text-gray-900">
                    {{ formatMoney(acc.balanceUsd) }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div
            v-else
            class="px-6 py-4 text-sm text-gray-400"
          >
            No accounts discovered yet.
          </div>
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
          <h2 class="text-lg font-semibold text-gray-900">
            Connect Provider
          </h2>
          <button
            class="text-gray-400 hover:text-gray-600"
            @click="showConnectModal = false"
          >
            <svg
              class="w-5 h-5"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              stroke-width="2"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>

        <form
          class="space-y-4"
          @submit.prevent="submitConnect"
        >
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Provider</label>
            <select
              v-model="connectForm.providerType"
              required
              class="w-full px-3 py-2 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            >
              <option
                value=""
                disabled
              >
                Select a provider
              </option>
              <option value="COINBASE">
                Coinbase
              </option>
              <option value="BITCOIN_WALLET">
                Bitcoin Wallet
              </option>
              <option value="M1_FINANCE">
                M1 Finance
              </option>
              <option value="MARCUS">
                Marcus by Goldman Sachs
              </option>
            </select>
          </div>

          <!-- Dynamic credential fields -->
          <template v-if="connectForm.providerType === 'COINBASE'">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">API Key</label>
              <input
                v-model="connectForm.credentials.apiKey"
                type="text"
                required
                class="w-full px-3 py-2 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              >
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">API Secret</label>
              <input
                v-model="connectForm.credentials.apiSecret"
                type="password"
                required
                class="w-full px-3 py-2 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              >
            </div>
          </template>
          <template v-else-if="connectForm.providerType === 'BITCOIN_WALLET'">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Bitcoin Address</label>
              <input
                v-model="connectForm.credentials.address"
                type="text"
                required
                placeholder="bc1..."
                class="w-full px-3 py-2 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              >
            </div>
          </template>
          <template
            v-else-if="
              connectForm.providerType === 'M1_FINANCE' || connectForm.providerType === 'MARCUS'
            "
          >
            <div class="rounded-lg border border-gray-100 bg-gray-50 px-4 py-3 text-sm text-gray-600">
              <p class="font-medium text-gray-700 mb-1">Secure Bank Login via Plaid</p>
              <p>
                Your credentials are entered directly with your bank through Plaid's secure
                interface — KeyBudget never sees them.
              </p>
            </div>
          </template>

          <p
            v-if="connectFormError"
            class="text-sm text-red-600"
          >
            {{ connectFormError }}
          </p>

          <!-- Buttons for credential-based providers -->
          <div
            v-if="!plaidProvider"
            class="flex gap-3 pt-1"
          >
            <button
              type="button"
              class="flex-1 px-4 py-2 text-sm font-medium text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
              @click="showConnectModal = false"
            >
              Cancel
            </button>
            <button
              type="submit"
              :disabled="submittingConnect || !connectForm.providerType"
              class="flex-1 px-4 py-2 text-sm font-medium text-white bg-primary-600 rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {{ submittingConnect ? 'Connecting…' : 'Connect' }}
            </button>
          </div>

          <!-- Buttons for Plaid-based providers -->
          <div
            v-else
            class="flex gap-3 pt-1"
          >
            <button
              type="button"
              class="flex-1 px-4 py-2 text-sm font-medium text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
              @click="showConnectModal = false"
            >
              Cancel
            </button>
            <button
              type="button"
              :disabled="plaidLoading"
              class="flex-1 px-4 py-2 text-sm font-medium text-white bg-primary-600 rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              @click="launchPlaidLink"
            >
              {{ plaidLoading ? 'Opening…' : 'Connect via Bank Login' }}
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
        <h2 class="text-lg font-semibold text-gray-900 mb-2">
          Disconnect Provider
        </h2>
        <p class="text-sm text-gray-600 mb-5">
          Disconnect
          <span class="font-medium">{{
            providerLabel(disconnectTarget?.providerType ?? 'COINBASE')
          }}</span>? Account data will be removed.
        </p>
        <p
          v-if="disconnectError"
          class="text-sm text-red-600 mb-4"
        >
          {{ disconnectError }}
        </p>
        <div class="flex gap-3">
          <button
            type="button"
            class="flex-1 px-4 py-2 text-sm font-medium text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
            @click="showDisconnectModal = false"
          >
            Cancel
          </button>
          <button
            type="button"
            :disabled="submittingDisconnect"
            class="flex-1 px-4 py-2 text-sm font-medium text-white bg-red-600 rounded-lg hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            @click="confirmDisconnect"
          >
            {{ submittingDisconnect ? 'Disconnecting…' : 'Disconnect' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, reactive, computed, watch, onMounted } from 'vue'
  import { useIntegrationsStore } from '@/stores/integrations'
  import { integrationsApi } from '@/api/integrations'
  import { usePlaidLink } from '@/composables/usePlaidLink'
  import { formatMoney } from '@/utils/formatting'
  import {
    providerLabel,
    providerIcon,
    providerColor,
    accountTypeLabel,
    statusClass,
    timeAgo,
  } from '@/utils/providers'
  import type { ProviderStatusResponse, ProviderType, PlaidProvider } from '@/types'

  const store = useIntegrationsStore()
  const syncing = ref<number | null>(null)

  async function handleSync(credentialId: number) {
    syncing.value = credentialId
    try {
      await store.syncProvider(credentialId)
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

  watch(
    () => connectForm.providerType,
    () => {
      connectForm.credentials = {}
      connectFormError.value = ''
    },
  )

  async function submitConnect() {
    if (!connectForm.providerType) return
    connectFormError.value = ''
    submittingConnect.value = true
    try {
      await store.connectProvider({
        providerType: connectForm.providerType,
        credentials: connectForm.credentials,
      })
      showConnectModal.value = false
      connectForm.providerType = ''
      connectForm.credentials = {}
    } catch (err: unknown) {
      const axiosErr = err as { response?: { status?: number; data?: { message?: string } } }
      const status = axiosErr?.response?.status
      const msg = axiosErr?.response?.data?.message
      if (status === 401) {
        connectFormError.value = 'Session expired. Please log in again.'
      } else if (status === 502) {
        connectFormError.value = msg || 'External provider error. Please try again later.'
      } else if (msg) {
        connectFormError.value = msg
      } else {
        connectFormError.value = 'Failed to connect provider. Check your credentials.'
      }
    } finally {
      submittingConnect.value = false
    }
  }

  // ── Plaid ──────────────────────────────────────────────────────────────────

  const plaidLoading = ref(false)
  const plaidLinkToken = ref<string | null>(null)
  const pendingPlaidProvider = ref<PlaidProvider | null>(null)

  const plaidProvider = computed<PlaidProvider | null>(() =>
    connectForm.providerType === 'M1_FINANCE' || connectForm.providerType === 'MARCUS'
      ? (connectForm.providerType as PlaidProvider)
      : null,
  )

  // Setup-level composable — safe for onUnmounted lifecycle
  const { open: openPlaidLink, ready: plaidReady } = usePlaidLink({
    linkToken: plaidLinkToken,
    onSuccess: async (publicToken) => {
      try {
        if (!pendingPlaidProvider.value) return
        await integrationsApi.exchangePlaidToken({
          publicToken,
          provider: pendingPlaidProvider.value,
        })
        await store.fetchAll()
        showConnectModal.value = false
        connectForm.providerType = ''
        connectForm.credentials = {}
      } catch (err: unknown) {
        const axiosErr = err as { response?: { data?: { message?: string } } }
        connectFormError.value =
          axiosErr?.response?.data?.message || 'Failed to connect account. Please try again.'
      } finally {
        plaidLoading.value = false
        plaidLinkToken.value = null
        pendingPlaidProvider.value = null
      }
    },
    onExit: () => {
      plaidLoading.value = false
      plaidLinkToken.value = null
      pendingPlaidProvider.value = null
    },
  })

  async function launchPlaidLink() {
    if (!plaidProvider.value) return
    plaidLoading.value = true
    connectFormError.value = ''
    try {
      const { linkToken } = await integrationsApi.createPlaidLinkToken(plaidProvider.value)
      pendingPlaidProvider.value = plaidProvider.value
      plaidLinkToken.value = linkToken
      // Wait for Plaid SDK to initialize, then open
      const unwatch = watch(plaidReady, (isReady) => {
        if (isReady) {
          unwatch()
          openPlaidLink()
        }
      }, { immediate: true })
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } }
      connectFormError.value =
        axiosErr?.response?.data?.message || 'Failed to start bank login. Please try again.'
      plaidLoading.value = false
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
      await store.disconnectProvider(disconnectTarget.value.credentialId)
      showDisconnectModal.value = false
    } catch {
      disconnectError.value = 'Failed to disconnect. Please try again.'
    } finally {
      submittingDisconnect.value = false
    }
  }

  async function loadData() {
    store.loading = true
    store.error = null
    try {
      await store.fetchAll()
    } catch {
      store.error = 'Failed to load accounts. Please try again.'
    } finally {
      store.loading = false
    }
  }

  onMounted(loadData)
</script>
