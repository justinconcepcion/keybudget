import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { integrationsApi } from '@/api/integrations'
import type {
  AccountResponse,
  ProviderStatusResponse,
  ConnectAccountRequest,
  NetWorthResponse,
  NetWorthDataPoint,
} from '@/types'

export const useIntegrationsStore = defineStore('integrations', () => {
  const accounts = ref<AccountResponse[]>([])
  const providers = ref<ProviderStatusResponse[]>([])
  const netWorth = ref<NetWorthResponse | null>(null)
  const netWorthHistory = ref<NetWorthDataPoint[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  const hasConnectedProviders = computed(() => providers.value.length > 0)

  function accountsByProvider(credentialId: number): AccountResponse[] {
    return accounts.value.filter((a) => a.credentialId === credentialId && a.active)
  }

  async function fetchAccounts(): Promise<void> {
    accounts.value = await integrationsApi.getAccounts()
  }

  async function fetchProviders(): Promise<void> {
    providers.value = await integrationsApi.getProviders()
  }

  async function fetchAll(): Promise<void> {
    const [p, a] = await Promise.all([integrationsApi.getProviders(), integrationsApi.getAccounts()])
    providers.value = p
    accounts.value = a
  }

  async function connectProvider(req: ConnectAccountRequest): Promise<AccountResponse[]> {
    const result = await integrationsApi.connect(req)
    await fetchAll()
    return result
  }

  async function disconnectProvider(credentialId: number): Promise<void> {
    await integrationsApi.disconnect(credentialId)
    await fetchAll()
  }

  async function syncProvider(credentialId: number): Promise<void> {
    await integrationsApi.sync(credentialId)
    await fetchAll()
  }

  async function fetchNetWorth(): Promise<void> {
    netWorth.value = await integrationsApi.getNetWorth()
  }

  async function fetchNetWorthHistory(days = 30): Promise<void> {
    const resp = await integrationsApi.getNetWorthHistory(days)
    netWorthHistory.value = resp.dataPoints
  }

  return {
    accounts,
    providers,
    netWorth,
    netWorthHistory,
    loading,
    error,
    hasConnectedProviders,
    accountsByProvider,
    fetchAccounts,
    fetchProviders,
    fetchAll,
    connectProvider,
    disconnectProvider,
    syncProvider,
    fetchNetWorth,
    fetchNetWorthHistory,
  }
})
