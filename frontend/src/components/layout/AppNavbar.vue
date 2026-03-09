<template>
  <header
    class="flex items-center justify-between px-4 h-14 border-b border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 lg:hidden"
  >
    <!-- Mobile hamburger -->
    <button
      class="p-2 rounded-lg text-gray-500 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
      @click="emit('toggle-sidebar')"
    >
      <svg
        class="w-5 h-5"
        fill="none"
        viewBox="0 0 24 24"
        stroke="currentColor"
        stroke-width="2"
      >
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          d="M4 6h16M4 12h16M4 18h16"
        />
      </svg>
    </button>

    <!-- Logo (mobile only) -->
    <div class="flex items-center gap-2">
      <div class="w-6 h-6 rounded-md bg-primary-600 flex items-center justify-center">
        <svg
          class="w-4 h-4 text-white"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          stroke-width="2"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
          />
        </svg>
      </div>
      <span class="text-sm font-bold text-gray-900 dark:text-white">KeyBudget</span>
    </div>

    <!-- Avatar -->
    <div class="w-8 h-8">
      <img
        v-if="authStore.user?.pictureUrl"
        :src="authStore.user.pictureUrl"
        :alt="authStore.user?.name ?? 'User avatar'"
        class="w-8 h-8 rounded-full"
      >
      <div
        v-else
        class="w-8 h-8 rounded-full bg-primary-100 text-primary-700 flex items-center justify-center text-xs font-semibold"
      >
        {{ initials }}
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
  import { computed } from 'vue'
  import { useAuthStore } from '@/stores/auth'

  const emit = defineEmits<{ 'toggle-sidebar': [] }>()

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
</script>
