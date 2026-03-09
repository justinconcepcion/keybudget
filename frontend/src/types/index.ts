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
