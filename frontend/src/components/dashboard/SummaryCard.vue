<template>
  <div class="bg-white dark:bg-gray-800 rounded-2xl border border-gray-200 dark:border-gray-700 p-5">
    <div class="flex items-center justify-between mb-3">
      <p class="text-sm font-medium text-gray-500 dark:text-gray-400">
        {{ label }}
      </p>
      <div :class="['flex items-center justify-center w-9 h-9 rounded-xl', iconBg]">
        <!-- Balance -->
        <svg
          v-if="icon === 'balance'"
          class="w-5 h-5"
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
        <!-- Income -->
        <svg
          v-else-if="icon === 'income'"
          class="w-5 h-5"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          stroke-width="2"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            d="M7 11l5-5m0 0l5 5m-5-5v12"
          />
        </svg>
        <!-- Expense -->
        <svg
          v-else-if="icon === 'expense'"
          class="w-5 h-5"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          stroke-width="2"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            d="M17 13l-5 5m0 0l-5-5m5 5V6"
          />
        </svg>
        <!-- Savings -->
        <svg
          v-else-if="icon === 'savings'"
          class="w-5 h-5"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          stroke-width="2"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"
          />
        </svg>
        <!-- Net Worth -->
        <svg
          v-else-if="icon === 'net-worth'"
          class="w-5 h-5"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          stroke-width="2"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"
          />
        </svg>
      </div>
    </div>
    <p class="text-2xl font-bold text-gray-900 dark:text-white">
      {{ value }}
    </p>
    <p
      v-if="trend"
      class="mt-1 text-xs text-gray-400"
    >
      {{ trend }}
    </p>
  </div>
</template>

<script setup lang="ts">
  defineProps<{
    label: string
    value: string
    trend: string | null
    icon: 'balance' | 'income' | 'expense' | 'savings' | 'net-worth'
    iconBg: string
  }>()
</script>
