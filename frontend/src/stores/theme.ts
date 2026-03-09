import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export const useThemeStore = defineStore('theme', () => {
  const dark = ref(localStorage.getItem('theme') === 'dark')

  function toggle() {
    dark.value = !dark.value
  }

  watch(
    dark,
    (isDark) => {
      document.documentElement.classList.toggle('dark', isDark)
      localStorage.setItem('theme', isDark ? 'dark' : 'light')
    },
    { immediate: true },
  )

  return { dark, toggle }
})
