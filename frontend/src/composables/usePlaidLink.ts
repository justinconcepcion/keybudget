// Type declaration for the Plaid Link SDK loaded from CDN
declare global {
  interface Window {
    Plaid: {
      create(config: PlaidCreateConfig): PlaidHandler
    }
  }
}

interface PlaidCreateConfig {
  token: string
  onSuccess: (publicToken: string, metadata: PlaidSuccessMetadata) => void
  onExit: (err: PlaidExitError | null, metadata: PlaidExitMetadata) => void
  onLoad: () => void
}

interface PlaidHandler {
  open(): void
  destroy(): void
}

export interface PlaidSuccessMetadata {
  institution: { name: string; institution_id: string } | null
  accounts: Array<{ id: string; name: string; mask: string; type: string; subtype: string }>
  link_session_id: string
}

export interface PlaidExitError {
  error_type: string
  error_code: string
  error_message: string
  display_message: string | null
}

export interface PlaidExitMetadata {
  link_session_id: string
  status: string | null
}

import { ref, onUnmounted } from 'vue'

const PLAID_SCRIPT_URL = 'https://cdn.plaid.com/link/v2/stable/link-initialize.js'
const PLAID_SCRIPT_ID = 'plaid-link-script'

function loadPlaidScript(): Promise<void> {
  return new Promise((resolve, reject) => {
    if (document.getElementById(PLAID_SCRIPT_ID)) {
      resolve()
      return
    }
    const script = document.createElement('script')
    script.id = PLAID_SCRIPT_ID
    script.src = PLAID_SCRIPT_URL
    script.onload = () => resolve()
    script.onerror = () => reject(new Error('Failed to load Plaid Link SDK'))
    document.head.appendChild(script)
  })
}

export interface UsePlaidLinkOptions {
  linkToken: string
  onSuccess: (publicToken: string, metadata: PlaidSuccessMetadata) => void
  onExit?: (err: PlaidExitError | null, metadata: PlaidExitMetadata) => void
}

export function usePlaidLink(options: UsePlaidLinkOptions) {
  const ready = ref(false)
  let handler: PlaidHandler | null = null

  loadPlaidScript()
    .then(() => {
      handler = window.Plaid.create({
        token: options.linkToken,
        onSuccess: (publicToken, metadata) => {
          options.onSuccess(publicToken, metadata)
        },
        onExit: (err, metadata) => {
          if (options.onExit) {
            options.onExit(err, metadata)
          }
        },
        onLoad: () => {
          ready.value = true
        },
      })
    })
    .catch((err: unknown) => {
      console.error('[usePlaidLink] Script load error:', err)
    })

  function open() {
    handler?.open()
  }

  function destroy() {
    handler?.destroy()
    handler = null
  }

  onUnmounted(() => {
    destroy()
  })

  return { ready, open, destroy }
}
