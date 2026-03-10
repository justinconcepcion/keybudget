<template>
  <div class="px-6 py-8 max-w-3xl mx-auto">
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white">
        Settings
      </h1>
      <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">
        Manage your account preferences.
      </p>
    </div>

    <!-- Profile card -->
    <div class="bg-white dark:bg-gray-800 rounded-2xl border border-gray-200 dark:border-gray-700 p-6 mb-4">
      <h2 class="text-base font-semibold text-gray-900 dark:text-white mb-4">
        Profile
      </h2>
      <div class="flex items-center gap-4">
        <img
          v-if="authStore.user?.pictureUrl"
          :src="authStore.user.pictureUrl"
          :alt="authStore.user.name"
          class="w-16 h-16 rounded-full"
        >
        <div
          v-else
          class="w-16 h-16 rounded-full bg-primary-100 text-primary-700 flex items-center justify-center text-xl font-bold"
        >
          {{ initials }}
        </div>
        <div>
          <p class="font-semibold text-gray-900 dark:text-white">
            {{ authStore.user?.name }}
          </p>
          <p class="text-sm text-gray-500 dark:text-gray-400">
            {{ authStore.user?.email }}
          </p>
          <p class="text-xs text-gray-400 dark:text-gray-500 mt-1">
            Signed in with Google
          </p>
        </div>
      </div>
    </div>

    <!-- Preferences -->
    <div class="bg-white dark:bg-gray-800 rounded-2xl border border-gray-200 dark:border-gray-700 p-6 mb-4">
      <h2 class="text-base font-semibold text-gray-900 dark:text-white mb-4">
        Preferences
      </h2>
      <div class="space-y-4 text-sm text-gray-500 dark:text-gray-400">
        <div class="flex items-center justify-between py-2 border-b border-gray-100 dark:border-gray-700">
          <span class="text-gray-700 dark:text-gray-300">Theme</span>
          <button
            class="relative inline-flex h-6 w-11 items-center rounded-full transition-colors"
            :class="themeStore.dark ? 'bg-primary-600' : 'bg-gray-300'"
            @click="themeStore.toggle()"
          >
            <span
              class="inline-block h-4 w-4 transform rounded-full bg-white transition-transform"
              :class="themeStore.dark ? 'translate-x-6' : 'translate-x-1'"
            />
          </button>
        </div>
        <div class="flex items-center justify-between py-2 border-b border-gray-100 dark:border-gray-700">
          <span class="text-gray-700 dark:text-gray-300">Currency</span>
          <select
            :value="authStore.user?.preferredCurrency ?? 'USD'"
            class="text-sm border border-gray-300 dark:border-gray-600 rounded-lg px-3 py-1.5 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
            @change="handleCurrencyChange"
          >
            <option
              v-for="c in currencies"
              :key="c.code"
              :value="c.code"
            >
              {{ c.code }} — {{ c.label }}
            </option>
          </select>
        </div>
        <div class="flex items-center justify-between py-2">
          <span class="text-gray-700 dark:text-gray-300">Notifications</span>
          <span class="text-gray-400">Coming soon</span>
        </div>
      </div>
    </div>

    <!-- Danger zone -->
    <div class="bg-white dark:bg-gray-800 rounded-2xl border border-red-200 dark:border-red-900 p-6">
      <h2 class="text-base font-semibold text-red-700 dark:text-red-400 mb-4">
        Danger Zone
      </h2>
      <div class="flex items-center justify-between">
        <div>
          <p class="text-sm font-medium text-gray-900 dark:text-gray-100">
            Sign out
          </p>
          <p class="text-xs text-gray-500 dark:text-gray-400 mt-0.5">
            You'll need to sign in again to access your account.
          </p>
        </div>
        <button
          class="px-4 py-2 text-sm font-medium text-red-600 dark:text-red-400 border border-red-300 dark:border-red-700 rounded-lg hover:bg-red-50 dark:hover:bg-red-900/30 transition-colors"
          @click="handleLogout"
        >
          Sign out
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed } from 'vue'
  import { useRouter } from 'vue-router'
  import { useAuthStore } from '@/stores/auth'
  import { useThemeStore } from '@/stores/theme'
  import { usersApi } from '@/api/users'

  const router = useRouter()
  const authStore = useAuthStore()
  const themeStore = useThemeStore()

  const currencies = [
    { code: 'USD', label: 'US Dollar' },
    { code: 'EUR', label: 'Euro' },
    { code: 'GBP', label: 'British Pound' },
    { code: 'CAD', label: 'Canadian Dollar' },
    { code: 'AUD', label: 'Australian Dollar' },
    { code: 'JPY', label: 'Japanese Yen' },
    { code: 'CHF', label: 'Swiss Franc' },
    { code: 'PHP', label: 'Philippine Peso' },
  ]

  const initials = computed(() => {
    const name = authStore.user?.name ?? ''
    return name
      .split(' ')
      .map((part) => part[0])
      .join('')
      .toUpperCase()
      .slice(0, 2)
  })

  async function handleCurrencyChange(e: Event) {
    const select = e.target as HTMLSelectElement
    const previous = authStore.user?.preferredCurrency ?? 'USD'
    try {
      const updated = await usersApi.updateCurrency(select.value)
      authStore.user = updated
    } catch {
      select.value = previous
    }
  }

  async function handleLogout() {
    await authStore.logout()
    router.push('/login')
  }
</script>
