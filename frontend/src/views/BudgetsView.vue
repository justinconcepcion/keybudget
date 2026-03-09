<template>
  <div class="px-6 py-8 max-w-7xl mx-auto">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">
          Budgets
        </h1>
        <p class="mt-1 text-sm text-gray-500">
          Set limits and stay on track.
        </p>
      </div>
      <div class="flex items-center gap-3">
        <input
          v-model="selectedMonth"
          type="month"
          class="text-sm border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
          @change="loadBudgets"
        >
        <button
          class="flex items-center gap-2 px-4 py-2 bg-primary-600 text-white text-sm font-medium rounded-lg hover:bg-primary-700 transition-colors"
          @click="openAddModal"
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
              d="M12 4v16m8-8H4"
            />
          </svg>
          New Budget
        </button>
      </div>
    </div>

    <!-- Loading -->
    <div
      v-if="loading"
      class="flex items-center justify-center py-20 text-gray-400 text-sm"
    >
      Loading…
    </div>

    <!-- Empty state -->
    <div
      v-else-if="budgetsStore.budgets.length === 0"
      class="bg-white rounded-2xl border border-gray-200"
    >
      <div class="flex flex-col items-center justify-center py-20 text-gray-400">
        <svg
          class="w-12 h-12 mb-4 text-gray-300"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="1.5"
            d="M11 3.055A9.001 9.001 0 1020.945 13H11V3.055z"
          />
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="1.5"
            d="M20.488 9H15V3.512A9.025 9.025 0 0120.488 9z"
          />
        </svg>
        <p class="font-medium text-gray-500">
          No budgets set
        </p>
        <p class="text-sm mt-1">
          Create your first budget to start tracking spending.
        </p>
        <button
          class="mt-4 px-4 py-2 bg-primary-600 text-white text-sm font-medium rounded-lg hover:bg-primary-700 transition-colors"
          @click="openAddModal"
        >
          Create your first budget
        </button>
      </div>
    </div>

    <!-- Budget cards grid -->
    <div
      v-else
      class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4"
    >
      <div
        v-for="budget in budgetsStore.budgets"
        :key="budget.id"
        class="bg-white rounded-2xl border border-gray-200 p-5"
      >
        <!-- Header -->
        <div class="flex items-center justify-between mb-4">
          <div class="flex items-center gap-2">
            <span
              class="inline-block w-3 h-3 rounded-full flex-shrink-0"
              :style="{ backgroundColor: budget.categoryColor }"
            />
            <span class="text-sm font-semibold text-gray-800">{{ budget.categoryName }}</span>
          </div>
          <div class="flex items-center gap-1">
            <button
              class="p-1.5 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-primary-50 transition-colors"
              title="Edit budget"
              @click="openEditModal(budget)"
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
                  d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"
                />
              </svg>
            </button>
            <button
              class="p-1.5 text-gray-400 hover:text-red-600 rounded-lg hover:bg-red-50 transition-colors"
              title="Delete budget"
              @click="confirmDelete(budget)"
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
                  d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
                />
              </svg>
            </button>
          </div>
        </div>

        <!-- Progress bar -->
        <div class="mb-3">
          <div class="w-full h-2.5 bg-gray-100 rounded-full overflow-hidden">
            <div
              class="h-full rounded-full transition-all"
              :class="budgetBarColor(budget)"
              :style="{ width: `${budgetPct(budget)}%` }"
            />
          </div>
        </div>

        <!-- Amounts -->
        <div class="flex justify-between text-sm">
          <div>
            <p class="text-xs text-gray-400">
              Spent
            </p>
            <p class="font-semibold text-gray-800 tabular-nums">
              {{ formatMoney(budget.spentAmount) }}
            </p>
          </div>
          <div class="text-right">
            <p class="text-xs text-gray-400">
              Limit
            </p>
            <p class="font-semibold text-gray-800 tabular-nums">
              {{ formatMoney(budget.limitAmount) }}
            </p>
          </div>
        </div>
        <div
          class="mt-2 text-xs"
          :class="budget.remainingAmount >= 0 ? 'text-emerald-600' : 'text-red-600'"
        >
          {{
            budget.remainingAmount >= 0
              ? `${formatMoney(budget.remainingAmount)} remaining`
              : `${formatMoney(Math.abs(budget.remainingAmount))} over budget`
          }}
        </div>
      </div>
    </div>

    <!-- Add Budget modal -->
    <div
      v-if="showAddModal"
      class="fixed inset-0 z-50 flex items-center justify-center bg-gray-900/50"
      @mousedown.self="showAddModal = false"
    >
      <div class="bg-white rounded-2xl shadow-xl w-full max-w-md mx-4 p-6">
        <div class="flex items-center justify-between mb-5">
          <h2 class="text-lg font-semibold text-gray-900">
            New Budget
          </h2>
          <button
            class="text-gray-400 hover:text-gray-600"
            @click="showAddModal = false"
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
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>

        <form
          class="space-y-4"
          @submit.prevent="submitAdd"
        >
          <!-- Category -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Category</label>
            <select
              v-model.number="addForm.categoryId"
              required
              class="w-full px-3 py-2 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            >
              <option
                value=""
                disabled
              >
                Select a category
              </option>
              <option
                v-for="cat in availableExpenseCategories"
                :key="cat.id"
                :value="cat.id"
              >
                {{ cat.name }}
              </option>
            </select>
            <p
              v-if="availableExpenseCategories.length === 0"
              class="mt-1 text-xs text-gray-400"
            >
              All expense categories already have a budget for this month.
            </p>
          </div>

          <!-- Month -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Month</label>
            <input
              v-model="addForm.monthYear"
              type="month"
              required
              class="w-full px-3 py-2 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            >
          </div>

          <!-- Limit -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Monthly Limit</label>
            <div class="relative">
              <span class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-sm">$</span>
              <input
                v-model.number="addForm.limitAmount"
                type="number"
                step="0.01"
                min="0.01"
                required
                placeholder="0.00"
                class="w-full pl-7 pr-3 py-2 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              >
            </div>
          </div>

          <p
            v-if="addFormError"
            class="text-sm text-red-600"
          >
            {{ addFormError }}
          </p>

          <div class="flex gap-3 pt-1">
            <button
              type="button"
              class="flex-1 px-4 py-2 text-sm font-medium text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
              @click="showAddModal = false"
            >
              Cancel
            </button>
            <button
              type="submit"
              :disabled="submittingAdd"
              class="flex-1 px-4 py-2 text-sm font-medium text-white bg-primary-600 rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {{ submittingAdd ? 'Saving…' : 'Create Budget' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Edit Budget modal -->
    <div
      v-if="showEditModal"
      class="fixed inset-0 z-50 flex items-center justify-center bg-gray-900/50"
      @mousedown.self="showEditModal = false"
    >
      <div class="bg-white rounded-2xl shadow-xl w-full max-w-md mx-4 p-6">
        <div class="flex items-center justify-between mb-5">
          <h2 class="text-lg font-semibold text-gray-900">
            Edit Budget — {{ editBudget?.categoryName }}
          </h2>
          <button
            class="text-gray-400 hover:text-gray-600"
            @click="showEditModal = false"
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
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>

        <form
          class="space-y-4"
          @submit.prevent="submitEdit"
        >
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Monthly Limit</label>
            <div class="relative">
              <span class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-sm">$</span>
              <input
                v-model.number="editForm.limitAmount"
                type="number"
                step="0.01"
                min="0.01"
                required
                placeholder="0.00"
                class="w-full pl-7 pr-3 py-2 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              >
            </div>
          </div>

          <p
            v-if="editFormError"
            class="text-sm text-red-600"
          >
            {{ editFormError }}
          </p>

          <div class="flex gap-3 pt-1">
            <button
              type="button"
              class="flex-1 px-4 py-2 text-sm font-medium text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
              @click="showEditModal = false"
            >
              Cancel
            </button>
            <button
              type="submit"
              :disabled="submittingEdit"
              class="flex-1 px-4 py-2 text-sm font-medium text-white bg-primary-600 rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {{ submittingEdit ? 'Saving…' : 'Update Budget' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Delete confirmation modal -->
    <div
      v-if="showDeleteModal"
      class="fixed inset-0 z-50 flex items-center justify-center bg-gray-900/50"
      @mousedown.self="showDeleteModal = false"
    >
      <div class="bg-white rounded-2xl shadow-xl w-full max-w-sm mx-4 p-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-2">
          Delete Budget
        </h2>
        <p class="text-sm text-gray-600 mb-6">
          Are you sure you want to delete the
          <span class="font-medium text-gray-800">{{ deleteBudget?.categoryName }}</span>
          budget? This action cannot be undone.
        </p>
        <div class="flex gap-3">
          <button
            class="flex-1 px-4 py-2 text-sm font-medium text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
            @click="showDeleteModal = false"
          >
            Cancel
          </button>
          <button
            :disabled="submittingDelete"
            class="flex-1 px-4 py-2 text-sm font-medium text-white bg-red-600 rounded-lg hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            @click="submitDelete"
          >
            {{ submittingDelete ? 'Deleting…' : 'Delete' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, computed, reactive, onMounted } from 'vue'
  import { useBudgetsStore } from '@/stores/budgets'
  import { useCategoriesStore } from '@/stores/categories'
  import type { BudgetResponse } from '@/types'
  import { formatMoney, budgetPct, budgetBarColor } from '@/utils/formatting'

  const budgetsStore = useBudgetsStore()
  const categoriesStore = useCategoriesStore()

  const loading = ref(false)

  // ── Month selector ────────────────────────────────────────────────────────

  function currentMonthKey(): string {
    const now = new Date()
    const y = now.getFullYear()
    const m = String(now.getMonth() + 1).padStart(2, '0')
    return `${y}-${m}`
  }

  const selectedMonth = ref(currentMonthKey())

  async function loadBudgets() {
    loading.value = true
    try {
      await budgetsStore.fetchBudgets(selectedMonth.value)
    } finally {
      loading.value = false
    }
  }


  // ── Add modal ─────────────────────────────────────────────────────────────

  const showAddModal = ref(false)
  const submittingAdd = ref(false)
  const addFormError = ref('')

  const addForm = reactive({
    categoryId: '' as number | '',
    monthYear: currentMonthKey(),
    limitAmount: null as number | null,
  })

  const budgetedCategoryIds = computed(() => new Set(budgetsStore.budgets.map((b) => b.categoryId)))

  const availableExpenseCategories = computed(() =>
    categoriesStore.expenseCategories.filter((c) => !budgetedCategoryIds.value.has(c.id)),
  )

  function openAddModal() {
    addForm.categoryId = ''
    addForm.monthYear = selectedMonth.value
    addForm.limitAmount = null
    addFormError.value = ''
    showAddModal.value = true
  }

  async function submitAdd() {
    if (!addForm.categoryId) {
      addFormError.value = 'Please select a category.'
      return
    }
    if (!addForm.limitAmount || addForm.limitAmount <= 0) {
      addFormError.value = 'Please enter a valid limit amount.'
      return
    }
    addFormError.value = ''
    submittingAdd.value = true
    try {
      await budgetsStore.createBudget({
        categoryId: addForm.categoryId as number,
        monthYear: addForm.monthYear,
        limitAmount: addForm.limitAmount,
      })
      showAddModal.value = false
    } catch {
      addFormError.value = 'Failed to create budget. Please try again.'
    } finally {
      submittingAdd.value = false
    }
  }

  // ── Edit modal ────────────────────────────────────────────────────────────

  const showEditModal = ref(false)
  const submittingEdit = ref(false)
  const editFormError = ref('')
  const editBudget = ref<BudgetResponse | null>(null)

  const editForm = reactive({
    limitAmount: null as number | null,
  })

  function openEditModal(budget: BudgetResponse) {
    editBudget.value = budget
    editForm.limitAmount = budget.limitAmount
    editFormError.value = ''
    showEditModal.value = true
  }

  async function submitEdit() {
    if (!editBudget.value) return
    if (!editForm.limitAmount || editForm.limitAmount <= 0) {
      editFormError.value = 'Please enter a valid limit amount.'
      return
    }
    editFormError.value = ''
    submittingEdit.value = true
    try {
      await budgetsStore.updateBudget(editBudget.value.id, {
        limitAmount: editForm.limitAmount,
      })
      showEditModal.value = false
    } catch {
      editFormError.value = 'Failed to update budget. Please try again.'
    } finally {
      submittingEdit.value = false
    }
  }

  // ── Delete modal ──────────────────────────────────────────────────────────

  const showDeleteModal = ref(false)
  const submittingDelete = ref(false)
  const deleteBudget = ref<BudgetResponse | null>(null)

  function confirmDelete(budget: BudgetResponse) {
    deleteBudget.value = budget
    showDeleteModal.value = true
  }

  async function submitDelete() {
    if (!deleteBudget.value) return
    submittingDelete.value = true
    try {
      await budgetsStore.deleteBudget(deleteBudget.value.id)
      showDeleteModal.value = false
    } catch {
      // Keep modal open so user can retry
    } finally {
      submittingDelete.value = false
    }
  }

  onMounted(async () => {
    await Promise.all([categoriesStore.fetchCategories(), loadBudgets()])
  })
</script>
