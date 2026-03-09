<template>
  <div class="px-6 py-8 max-w-3xl mx-auto">
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-gray-900">
        Settings
      </h1>
      <p class="mt-1 text-sm text-gray-500">
        Manage your account preferences.
      </p>
    </div>

    <!-- Profile card -->
    <div class="bg-white rounded-2xl border border-gray-200 p-6 mb-4">
      <h2 class="text-base font-semibold text-gray-900 mb-4">
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
          <p class="font-semibold text-gray-900">
            {{ authStore.user?.name }}
          </p>
          <p class="text-sm text-gray-500">
            {{ authStore.user?.email }}
          </p>
          <p class="text-xs text-gray-400 mt-1">
            Signed in with Google
          </p>
        </div>
      </div>
    </div>

    <!-- Preferences placeholder -->
    <div class="bg-white rounded-2xl border border-gray-200 p-6 mb-4">
      <h2 class="text-base font-semibold text-gray-900 mb-4">
        Preferences
      </h2>
      <div class="space-y-4 text-sm text-gray-500">
        <div class="flex items-center justify-between py-2 border-b border-gray-100">
          <span class="text-gray-700">Currency</span>
          <span class="text-gray-400">USD — coming soon</span>
        </div>
        <div class="flex items-center justify-between py-2">
          <span class="text-gray-700">Notifications</span>
          <span class="text-gray-400">Coming soon</span>
        </div>
      </div>
    </div>

    <!-- Danger zone -->
    <div class="bg-white rounded-2xl border border-red-200 p-6">
      <h2 class="text-base font-semibold text-red-700 mb-4">
        Danger Zone
      </h2>
      <div class="flex items-center justify-between">
        <div>
          <p class="text-sm font-medium text-gray-900">
            Sign out
          </p>
          <p class="text-xs text-gray-500 mt-0.5">
            You'll need to sign in again to access your account.
          </p>
        </div>
        <button
          class="px-4 py-2 text-sm font-medium text-red-600 border border-red-300 rounded-lg hover:bg-red-50 transition-colors"
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

  const router = useRouter()
  const authStore = useAuthStore()

  const initials = computed(() => {
    const name = authStore.user?.name ?? ''
    return name
      .split(' ')
      .map((part) => part[0])
      .join('')
      .toUpperCase()
      .slice(0, 2)
  })

  async function handleLogout() {
    await authStore.logout()
    router.push('/login')
  }
</script>
