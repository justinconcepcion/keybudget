import api from './axios'
import type {
  AccountResponse,
  ProviderStatusResponse,
  ConnectAccountRequest,
  SyncResultResponse,
  NetWorthResponse,
  NetWorthHistoryResponse,
} from '@/types'

export const integrationsApi = {
  getAccounts(): Promise<AccountResponse[]> {
    return api.get<AccountResponse[]>('/integrations/accounts').then((r) => r.data)
  },

  getProviders(): Promise<ProviderStatusResponse[]> {
    return api.get<ProviderStatusResponse[]>('/integrations/providers').then((r) => r.data)
  },

  connect(data: ConnectAccountRequest): Promise<AccountResponse[]> {
    return api.post<AccountResponse[]>('/integrations/connect', data).then((r) => r.data)
  },

  disconnect(credentialId: number): Promise<void> {
    return api.delete(`/integrations/providers/${credentialId}`).then(() => undefined)
  },

  sync(credentialId: number): Promise<SyncResultResponse> {
    return api
      .post<SyncResultResponse>(`/integrations/providers/${credentialId}/sync`)
      .then((r) => r.data)
  },

  getNetWorth(): Promise<NetWorthResponse> {
    return api.get<NetWorthResponse>('/integrations/net-worth').then((r) => r.data)
  },

  getNetWorthHistory(days = 30): Promise<NetWorthHistoryResponse> {
    return api
      .get<NetWorthHistoryResponse>('/integrations/net-worth/history', { params: { days } })
      .then((r) => r.data)
  },
}
