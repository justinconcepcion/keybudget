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
