import type { ProviderType, AccountType, SyncStatus } from '@/types'

const PROVIDER_LABELS: Record<ProviderType, string> = {
  COINBASE: 'Coinbase',
  BITCOIN_WALLET: 'Bitcoin Wallet',
  M1_FINANCE: 'M1 Finance',
  MARCUS: 'Marcus by Goldman Sachs',
}

const PROVIDER_ICONS: Record<ProviderType, string> = {
  COINBASE: 'CB',
  BITCOIN_WALLET: 'BTC',
  M1_FINANCE: 'M1',
  MARCUS: 'GS',
}

const PROVIDER_COLORS: Record<ProviderType, string> = {
  COINBASE: 'bg-blue-600',
  BITCOIN_WALLET: 'bg-orange-500',
  M1_FINANCE: 'bg-emerald-600',
  MARCUS: 'bg-indigo-600',
}

const ACCOUNT_TYPE_LABELS: Record<AccountType, string> = {
  CRYPTO_WALLET: 'Crypto',
  BROKERAGE: 'Brokerage',
  SAVINGS: 'Savings',
  CHECKING: 'Checking',
}

export function providerLabel(type: ProviderType): string {
  return PROVIDER_LABELS[type] ?? type
}

export function providerIcon(type: ProviderType): string {
  return PROVIDER_ICONS[type] ?? '?'
}

export function providerColor(type: ProviderType): string {
  return PROVIDER_COLORS[type] ?? 'bg-gray-500'
}

export function accountTypeLabel(type: AccountType): string {
  return ACCOUNT_TYPE_LABELS[type] ?? type
}

export function statusClass(status: SyncStatus | string): string {
  if (status === 'OK') return 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300'
  if (status === 'ERROR') return 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300'
  return 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300'
}

export function timeAgo(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return 'just now'
  if (mins < 60) return `${mins}m ago`
  const hrs = Math.floor(mins / 60)
  if (hrs < 24) return `${hrs}h ago`
  return `${Math.floor(hrs / 24)}d ago`
}
