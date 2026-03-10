import { ref, watch, onUnmounted, type Ref } from 'vue'

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

const PLAID_SCRIPT_URL = 'https://cdn.plaid.com/link/v2/stable/link-initialize.js'
const PLAID_SCRIPT_ID = 'plaid-link-script'

let scriptLoadPromise: Promise<void> | null = null

function loadPlaidScript(): Promise<void> {
  if (scriptLoadPromise) return scriptLoadPromise
  scriptLoadPromise = new Promise((resolve, reject) => {
    if (window.Plaid) {
      resolve()
      return
    }
    const existing = document.getElementById(PLAID_SCRIPT_ID)
    if (existing) {
      // Script tag exists but Plaid not ready yet — poll briefly
      const check = setInterval(() => {
        if (window.Plaid) {
          clearInterval(check)
          resolve()
        }
      }, 50)
      setTimeout(() => {
        clearInterval(check)
        reject(new Error('Plaid SDK load timeout'))
      }, 10000)
      return
    }
    const script = document.createElement('script')
    script.id = PLAID_SCRIPT_ID
    script.src = PLAID_SCRIPT_URL
    script.onload = () => resolve()
    script.onerror = () => reject(new Error('Failed to load Plaid Link SDK'))
    document.head.appendChild(script)
  })
  return scriptLoadPromise
}

export interface UsePlaidLinkOptions {
  linkToken: Ref<string | null>
  onSuccess: (publicToken: string, metadata: PlaidSuccessMetadata) => void
  onExit?: (err: PlaidExitError | null, metadata: PlaidExitMetadata) => void
}

/**
 * Composable for Plaid Link SDK. Must be called at component setup level.
 * Set linkToken ref to trigger handler creation; call open() to launch.
 */
export function usePlaidLink(options: UsePlaidLinkOptions) {
  const ready = ref(false)
  let handler: PlaidHandler | null = null

  function destroyHandler() {
    handler?.destroy()
    handler = null
    ready.value = false
  }

  watch(options.linkToken, async (token) => {
    destroyHandler()
    if (!token) return

    try {
      await loadPlaidScript()
      handler = window.Plaid.create({
        token,
        onSuccess: (publicToken, metadata) => {
          options.onSuccess(publicToken, metadata)
        },
        onExit: (err, metadata) => {
          options.onExit?.(err, metadata)
        },
        onLoad: () => {
          ready.value = true
        },
      })
    } catch (err) {
      console.error('[usePlaidLink] Failed to initialize:', err)
    }
  })

  function open() {
    handler?.open()
  }

  onUnmounted(() => {
    destroyHandler()
  })

  return { ready, open, destroy: destroyHandler }
}
