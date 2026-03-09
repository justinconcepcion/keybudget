export interface AuthTokens {
  accessToken: string
  expiresIn: number
}

export interface UserProfile {
  id: number
  email: string
  name: string
  pictureUrl: string | null
}

export interface ApiError {
  message: string
  status: number
  timestamp?: string
}

export interface SummaryCardProps {
  label: string
  value: string
  trend: string | null
  icon: 'balance' | 'income' | 'expense' | 'savings'
  iconBg: string
}

// ── Categories ──────────────────────────────────────────────────────────────

export interface CategoryResponse {
  id: number
  name: string
  icon: string
  color: string
  type: 'INCOME' | 'EXPENSE'
  isDefault: boolean
}

export interface CreateCategoryRequest {
  name: string
  icon?: string
  color?: string
  type: 'INCOME' | 'EXPENSE'
}

export interface UpdateCategoryRequest {
  name: string
  icon?: string
  color?: string
  type: 'INCOME' | 'EXPENSE'
}

// ── Transactions ─────────────────────────────────────────────────────────────

export interface TransactionResponse {
  id: number
  amount: number
  description: string
  date: string
  type: 'INCOME' | 'EXPENSE'
  categoryId: number
  categoryName: string
}

export interface TransactionPage {
  content: TransactionResponse[]
  totalElements: number
  totalPages: number
  number: number
}

export interface TransactionParams {
  start?: string
  end?: string
  categoryId?: number
  type?: 'INCOME' | 'EXPENSE'
  page?: number
  size?: number
}

export interface CreateTransactionRequest {
  amount: number
  description?: string
  date: string
  type: 'INCOME' | 'EXPENSE'
  categoryId: number
}

export interface UpdateTransactionRequest {
  amount: number
  description?: string
  date: string
  type: 'INCOME' | 'EXPENSE'
  categoryId: number
}

export interface CategoryTotal {
  categoryId: number
  categoryName: string
  total: number
}

export interface MonthlySummaryResponse {
  totalIncome: number
  totalExpenses: number
  netSavings: number
  byCategory: CategoryTotal[]
}

// ── Budgets ──────────────────────────────────────────────────────────────────

export interface BudgetResponse {
  id: number
  categoryId: number
  categoryName: string
  categoryColor: string
  monthYear: string
  limitAmount: number
  spentAmount: number
  remainingAmount: number
}

export interface CreateBudgetRequest {
  categoryId: number
  monthYear: string
  limitAmount: number
}

export interface UpdateBudgetRequest {
  limitAmount: number
}

// ── Integrations ────────────────────────────────────────────────────────────

export interface AccountResponse {
  id: number
  credentialId: number
  providerType: ProviderType
  accountType: AccountType
  displayName: string
  currency: string
  balance: number
  balanceUsd: number
  asOf: string
  active: boolean
}

export type ProviderType = 'COINBASE' | 'BITCOIN_WALLET' | 'M1_FINANCE' | 'MARCUS'
export type AccountType = 'CRYPTO_WALLET' | 'BROKERAGE' | 'SAVINGS' | 'CHECKING'
export type SyncStatus = 'NEVER' | 'OK' | 'ERROR'

export interface ProviderStatusResponse {
  credentialId: number
  providerType: ProviderType
  status: SyncStatus
  lastSyncedAt: string | null
  errorMessage: string | null
  accountCount: number
}

export interface ConnectAccountRequest {
  providerType: ProviderType
  credentials: Record<string, string>
}

export interface SyncResultResponse {
  providerType: ProviderType
  syncedAt: string
  accountsUpdated: number
  status: SyncStatus
  errorMessage: string | null
}

export interface NetWorthResponse {
  totalNetWorthUsd: number
  byProvider: ProviderTotal[]
  byAccountType: AccountTypeTotal[]
  asOf: string
}

export interface ProviderTotal {
  providerType: ProviderType
  totalUsd: number
  accountCount: number
}

export interface AccountTypeTotal {
  accountType: AccountType
  totalUsd: number
  accountCount: number
}

export interface NetWorthHistoryResponse {
  dataPoints: NetWorthDataPoint[]
}

export interface NetWorthDataPoint {
  date: string
  totalUsd: number
}
