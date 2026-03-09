<template>
  <div class="flex h-full">
    <!-- Desktop sidebar (always visible) -->
    <aside class="hidden lg:flex lg:flex-shrink-0">
      <AppSidebar />
    </aside>

    <!-- Mobile sidebar overlay -->
    <Transition name="sidebar">
      <div v-if="sidebarOpen" class="fixed inset-0 z-40 flex lg:hidden">
        <!-- Backdrop -->
        <div class="fixed inset-0 bg-gray-600/50" @click="sidebarOpen = false" />
        <!-- Panel -->
        <div class="relative flex w-64 flex-shrink-0 flex-col">
          <AppSidebar />
        </div>
      </div>
    </Transition>

    <!-- Main content -->
    <div class="flex flex-1 flex-col min-w-0 overflow-hidden">
      <AppNavbar @toggle-sidebar="sidebarOpen = !sidebarOpen" />

      <main class="flex-1 overflow-y-auto">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, watch } from 'vue'
  import { RouterView, useRoute } from 'vue-router'
  import AppSidebar from './AppSidebar.vue'
  import AppNavbar from './AppNavbar.vue'

  const sidebarOpen = ref(false)
  const route = useRoute()

  // Close mobile sidebar on navigation
  watch(
    () => route.path,
    () => {
      sidebarOpen.value = false
    },
  )
</script>

<style scoped>
  .sidebar-enter-active,
  .sidebar-leave-active {
    transition: opacity 0.2s ease;
  }
  .sidebar-enter-from,
  .sidebar-leave-to {
    opacity: 0;
  }
</style>
