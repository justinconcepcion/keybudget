<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-50">
    <div class="text-center">
      <div
        v-if="error"
        class="max-w-sm mx-auto px-4"
      >
        <div class="text-red-500 mb-4">
          <svg
            class="w-12 h-12 mx-auto"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
            />
          </svg>
        </div>
        <p class="text-gray-700 font-medium mb-2">
          Sign-in failed
        </p>
        <p class="text-sm text-gray-500 mb-4">
          {{ error }}
        </p>
        <RouterLink
          to="/login"
          class="text-sm text-primary-600 hover:underline"
        >
          Try again
        </RouterLink>
      </div>

      <div
        v-else
        class="flex flex-col items-center gap-3"
      >
        <svg
          class="animate-spin w-8 h-8 text-primary-600"
          fill="none"
          viewBox="0 0 24 24"
        >
          <circle
            class="opacity-25"
            cx="12"
            cy="12"
            r="10"
            stroke="currentColor"
            stroke-width="4"
          />
          <path
            class="opacity-75"
            fill="currentColor"
            d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
          />
        </svg>
        <p class="text-sm text-gray-500">
          Signing you in…
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { onMounted, ref } from 'vue'
  import { RouterLink, useRouter, useRoute } from 'vue-router'
  import { useAuthStore } from '@/stores/auth'
  import { usersApi } from '@/api/users'

  const router = useRouter()
  const route = useRoute()
  const authStore = useAuthStore()
  const error = ref<string | null>(null)

  onMounted(async () => {
    // Access token is passed via URL fragment (#token=...) — never sent to servers
    const hash = window.location.hash
    const match = hash.match(/[#&]token=([^&]+)/)

    if (!match) {
      error.value = 'No token received. Please try signing in again.'
      return
    }

    const accessToken = decodeURIComponent(match[1])
    authStore.setAccessToken(accessToken)

    try {
      authStore.user = await usersApi.getMe()
      const raw = route.query.redirect as string | undefined
      // Only allow relative paths to prevent open-redirect attacks
      const safeRedirect = raw && raw.startsWith('/') && !raw.startsWith('//') ? raw : '/dashboard'
      router.replace(safeRedirect)
    } catch {
      authStore.clear()
      error.value = 'Failed to load your profile. Please try again.'
    }
  })
</script>
