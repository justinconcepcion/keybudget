<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-50 px-4">
    <div class="text-center">
      <p class="text-7xl font-black text-primary-600 mb-4">
        404
      </p>
      <h1 class="text-2xl font-bold text-gray-900 mb-2">
        Page not found
      </h1>
      <p class="text-gray-500 mb-8">
        The page you're looking for doesn't exist.
      </p>
      <RouterLink
        to="/dashboard"
        class="inline-flex items-center gap-2 px-5 py-2.5 bg-primary-600 text-white text-sm font-medium rounded-lg hover:bg-primary-700 transition-colors"
      >
        <svg
          class="w-4 h-4"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          stroke-width="2"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            d="M10 19l-7-7m0 0l7-7m-7 7h18"
          />
        </svg>
        Back to Dashboard
      </RouterLink>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { RouterLink } from 'vue-router'
</script>
