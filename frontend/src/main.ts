import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { initApiDeps } from './api/axios'
import { useAuthStore } from './stores/auth'
import { useThemeStore } from './stores/theme'
import { usersApi } from './api/users'
import './assets/main.css'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

// Initialize theme (applies dark class to <html> before first paint)
useThemeStore()

// Wire lazy deps after pinia is installed (avoids circular import issue)
initApiDeps(
  () => useAuthStore(),
  () => router,
)

/**
 * Attempt a silent session restore using the HttpOnly refresh_token cookie.
 * If the cookie is missing or expired the user will be directed to /login
 * by the route guard. We don't block rendering — the guard handles protection.
 */
async function initSession() {
  const authStore = useAuthStore()
  try {
    await authStore.refreshTokens()
    authStore.user = await usersApi.getMe()
  } catch {
    // No valid session — route guard will redirect to /login if needed
    authStore.clear()
  }
}

initSession().finally(() => {
  app.mount('#app')
})
