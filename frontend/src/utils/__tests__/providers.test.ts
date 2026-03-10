import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import {
  providerLabel,
  providerIcon,
  providerColor,
  accountTypeLabel,
  statusClass,
  timeAgo,
} from '../providers'

describe('providerLabel', () => {
  it('returns label for known provider', () => {
    expect(providerLabel('COINBASE')).toBe('Coinbase')
    expect(providerLabel('BITCOIN_WALLET')).toBe('Bitcoin Wallet')
    expect(providerLabel('M1_FINANCE')).toBe('M1 Finance')
    expect(providerLabel('MARCUS')).toBe('Marcus by Goldman Sachs')
  })

  it('falls back to raw type for unknown provider', () => {
    expect(providerLabel('UNKNOWN' as any)).toBe('UNKNOWN')
  })
})

describe('providerIcon', () => {
  it('returns icon for known provider', () => {
    expect(providerIcon('COINBASE')).toBe('CB')
    expect(providerIcon('BITCOIN_WALLET')).toBe('BTC')
  })

  it('falls back to ? for unknown', () => {
    expect(providerIcon('UNKNOWN' as any)).toBe('?')
  })
})

describe('providerColor', () => {
  it('returns color for known provider', () => {
    expect(providerColor('COINBASE')).toBe('bg-blue-600')
  })

  it('falls back to gray for unknown', () => {
    expect(providerColor('UNKNOWN' as any)).toBe('bg-gray-500')
  })
})

describe('accountTypeLabel', () => {
  it('returns label for known account type', () => {
    expect(accountTypeLabel('CRYPTO_WALLET')).toBe('Crypto')
    expect(accountTypeLabel('SAVINGS')).toBe('Savings')
  })

  it('falls back to raw type for unknown', () => {
    expect(accountTypeLabel('UNKNOWN' as any)).toBe('UNKNOWN')
  })
})

describe('statusClass', () => {
  it('returns green for OK', () => {
    expect(statusClass('OK')).toBe(
      'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300',
    )
  })

  it('returns red for ERROR', () => {
    expect(statusClass('ERROR')).toBe(
      'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300',
    )
  })

  it('returns gray for other statuses', () => {
    expect(statusClass('NEVER')).toBe('bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300')
  })
})

describe('timeAgo', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-03-09T12:00:00Z'))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('returns just now for < 1 minute', () => {
    expect(timeAgo('2026-03-09T11:59:30Z')).toBe('just now')
  })

  it('returns minutes for < 1 hour', () => {
    expect(timeAgo('2026-03-09T11:30:00Z')).toBe('30m ago')
  })

  it('returns hours for < 1 day', () => {
    expect(timeAgo('2026-03-09T06:00:00Z')).toBe('6h ago')
  })

  it('returns days for >= 1 day', () => {
    expect(timeAgo('2026-03-07T12:00:00Z')).toBe('2d ago')
  })
})
