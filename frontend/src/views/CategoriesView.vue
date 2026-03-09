<template>
  <div class="px-6 py-8 max-w-7xl mx-auto">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">
          Categories
        </h1>
        <p class="mt-1 text-sm text-gray-500">
          Organize your income and expenses.
        </p>
      </div>
      <button
        class="flex items-center gap-2 px-4 py-2 bg-primary-600 text-white text-sm font-medium rounded-lg hover:bg-primary-700 transition-colors"
        @click="showAddModal = true"
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
        Add Category
      </button>
    </div>

    <!-- Type tabs -->
    <div class="flex gap-2 mb-4">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        class="px-4 py-2 text-sm font-medium rounded-lg transition-colors"
        :class="activeTab === tab.value
          ? 'bg-primary-600 text-white'
          : 'bg-white text-gray-700 border border-gray-300 hover:bg-gray-50'"
        @click="activeTab = tab.value"
      >
        {{ tab.label }}
      </button>
    </div>

    <!-- Category grid -->
    <div
      v-if="loading"
      class="flex items-center justify-center h-64 text-gray-400 text-sm"
    >
      Loading…
    </div>
    <div
      v-else-if="filteredCategories.length === 0"
      class="bg-white rounded-2xl border border-gray-200 flex items-center justify-center h-64 text-gray-400 text-sm"
    >
      <div class="text-center">
        <p class="font-medium text-gray-500">
          No {{ activeTab.toLowerCase() }} categories
        </p>
        <p class="text-xs mt-1">
          Create one to get started.
        </p>
      </div>
    </div>
    <div
      v-else
      class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4"
    >
      <div
        v-for="cat in filteredCategories"
        :key="cat.id"
        class="bg-white rounded-2xl border border-gray-200 p-5 flex items-center justify-between hover:shadow-sm transition-shadow"
      >
        <div class="flex items-center gap-3">
          <div
            class="w-10 h-10 rounded-lg flex items-center justify-center text-lg"
            :style="{ backgroundColor: (cat.color || '#6B7280') + '20', color: cat.color || '#6B7280' }"
          >
            {{ cat.icon || '📁' }}
          </div>
          <div>
            <p class="text-sm font-medium text-gray-900">
              {{ cat.name }}
            </p>
            <p class="text-xs text-gray-500">
              {{ cat.type }}
            </p>
          </div>
        </div>
        <div
          v-if="!cat.isDefault"
          class="flex gap-1"
        >
          <button
            class="p-1.5 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-primary-50 transition-colors"
            title="Edit"
            @click="openEditModal(cat)"
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
            title="Delete"
            @click="openDeleteModal(cat)"
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
        <span
          v-else
          class="text-xs text-gray-400 italic"
        >Default</span>
      </div>
    </div>

    <!-- Add Category modal -->
    <div
      v-if="showAddModal"
      class="fixed inset-0 z-50 flex items-center justify-center bg-gray-900/50"
      @mousedown.self="showAddModal = false"
    >
      <div class="bg-white rounded-2xl shadow-xl w-full max-w-md mx-4 p-6">
        <div class="flex items-center justify-between mb-5">
          <h2 class="text-lg font-semibold text-gray-900">
            Add Category
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
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Type</label>
            <div class="flex rounded-lg border border-gray-300 overflow-hidden text-sm">
              <button
                type="button"
                class="flex-1 py-2 transition-colors"
                :class="addForm.type === 'EXPENSE' ? 'bg-red-500 text-white' : 'bg-white text-gray-700 hover:bg-gray-50'"
                @click="addForm.type = 'EXPENSE'"
              >
                Expense
              </button>
              <button
                type="button"
                class="flex-1 py-2 transition-colors"
                :class="addForm.type === 'INCOME' ? 'bg-emerald-500 text-white' : 'bg-white text-gray-700 hover:bg-gray-50'"
                @click="addForm.type = 'INCOME'"
              >
                Income
              </button>
            </div>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Name</label>
            <input
              v-model="addForm.name"
              type="text"
              required
              placeholder="e.g. Groceries"
              class="w-full px-3 py-2 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            >
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              Icon
              <span class="text-gray-400 font-normal">(emoji, optional)</span>
            </label>
            <input
              v-model="addForm.icon"
              type="text"
              placeholder="e.g. 🛒"
              maxlength="4"
              class="w-full px-3 py-2 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            >
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              Color
              <span class="text-gray-400 font-normal">(optional)</span>
            </label>
            <input
              v-model="addForm.color"
              type="color"
              class="w-12 h-9 border border-gray-300 rounded-lg cursor-pointer"
            >
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
              {{ submittingAdd ? 'Saving…' : 'Save' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Edit Category modal -->
    <div
      v-if="showEditModal"
      class="fixed inset-0 z-50 flex items-center justify-center bg-gray-900/50"
      @mousedown.self="showEditModal = false"
    >
      <div class="bg-white rounded-2xl shadow-xl w-full max-w-md mx-4 p-6">
        <div class="flex items-center justify-between mb-5">
          <h2 class="text-lg font-semibold text-gray-900">
            Edit Category
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
            <label class="block text-sm font-medium text-gray-700 mb-1">Type</label>
            <div class="flex rounded-lg border border-gray-300 overflow-hidden text-sm">
              <button
                type="button"
                class="flex-1 py-2 transition-colors"
                :class="editForm.type === 'EXPENSE' ? 'bg-red-500 text-white' : 'bg-white text-gray-700 hover:bg-gray-50'"
                @click="editForm.type = 'EXPENSE'"
              >
                Expense
              </button>
              <button
                type="button"
                class="flex-1 py-2 transition-colors"
                :class="editForm.type === 'INCOME' ? 'bg-emerald-500 text-white' : 'bg-white text-gray-700 hover:bg-gray-50'"
                @click="editForm.type = 'INCOME'"
              >
                Income
              </button>
            </div>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Name</label>
            <input
              v-model="editForm.name"
              type="text"
              required
              class="w-full px-3 py-2 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            >
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              Icon
              <span class="text-gray-400 font-normal">(emoji, optional)</span>
            </label>
            <input
              v-model="editForm.icon"
              type="text"
              maxlength="4"
              class="w-full px-3 py-2 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            >
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              Color
              <span class="text-gray-400 font-normal">(optional)</span>
            </label>
            <input
              v-model="editForm.color"
              type="color"
              class="w-12 h-9 border border-gray-300 rounded-lg cursor-pointer"
            >
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
              {{ submittingEdit ? 'Saving…' : 'Save Changes' }}
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
          Delete Category
        </h2>
        <p class="text-sm text-gray-600 mb-5">
          Are you sure you want to delete
          <span class="font-medium">"{{ deleteTarget?.name }}"</span>?
          Transactions using this category may be affected.
        </p>

        <p
          v-if="deleteError"
          class="text-sm text-red-600 mb-4"
        >
          {{ deleteError }}
        </p>

        <div class="flex gap-3">
          <button
            type="button"
            class="flex-1 px-4 py-2 text-sm font-medium text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
            @click="showDeleteModal = false"
          >
            Cancel
          </button>
          <button
            type="button"
            :disabled="submittingDelete"
            class="flex-1 px-4 py-2 text-sm font-medium text-white bg-red-600 rounded-lg hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            @click="confirmDelete"
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
  import { useCategoriesStore } from '@/stores/categories'
  import type { CategoryResponse } from '@/types'

  const categoriesStore = useCategoriesStore()
  const loading = ref(false)

  const tabs = [
    { label: 'Expense', value: 'EXPENSE' as const },
    { label: 'Income', value: 'INCOME' as const },
  ]
  const activeTab = ref<'INCOME' | 'EXPENSE'>('EXPENSE')

  const filteredCategories = computed(() =>
    categoriesStore.categories.filter((c) => c.type === activeTab.value),
  )

  // ── Add ────────────────────────────────────────────────────────────────────

  const showAddModal = ref(false)
  const submittingAdd = ref(false)
  const addFormError = ref('')

  const addForm = reactive({
    name: '',
    icon: '',
    color: '#4CAF50',
    type: 'EXPENSE' as 'INCOME' | 'EXPENSE',
  })

  async function submitAdd() {
    if (!addForm.name.trim()) {
      addFormError.value = 'Please enter a category name.'
      return
    }
    addFormError.value = ''
    submittingAdd.value = true
    try {
      await categoriesStore.createCategory({
        name: addForm.name.trim(),
        icon: addForm.icon || undefined,
        color: addForm.color || undefined,
        type: addForm.type,
      })
      showAddModal.value = false
      addForm.name = ''
      addForm.icon = ''
      addForm.color = '#4CAF50'
      addForm.type = 'EXPENSE'
    } catch {
      addFormError.value = 'Failed to create category. Please try again.'
    } finally {
      submittingAdd.value = false
    }
  }

  // ── Edit ───────────────────────────────────────────────────────────────────

  const showEditModal = ref(false)
  const submittingEdit = ref(false)
  const editFormError = ref('')
  const editTargetId = ref<number | null>(null)

  const editForm = reactive({
    name: '',
    icon: '',
    color: '#4CAF50',
    type: 'EXPENSE' as 'INCOME' | 'EXPENSE',
  })

  function openEditModal(cat: CategoryResponse) {
    editTargetId.value = cat.id
    editForm.name = cat.name
    editForm.icon = cat.icon || ''
    editForm.color = cat.color || '#4CAF50'
    editForm.type = cat.type
    editFormError.value = ''
    showEditModal.value = true
  }

  async function submitEdit() {
    if (!editForm.name.trim()) {
      editFormError.value = 'Please enter a category name.'
      return
    }
    editFormError.value = ''
    submittingEdit.value = true
    try {
      await categoriesStore.updateCategory(editTargetId.value!, {
        name: editForm.name.trim(),
        icon: editForm.icon || undefined,
        color: editForm.color || undefined,
        type: editForm.type,
      })
      showEditModal.value = false
    } catch {
      editFormError.value = 'Failed to update category. Please try again.'
    } finally {
      submittingEdit.value = false
    }
  }

  // ── Delete ─────────────────────────────────────────────────────────────────

  const showDeleteModal = ref(false)
  const submittingDelete = ref(false)
  const deleteError = ref('')
  const deleteTarget = ref<CategoryResponse | null>(null)

  function openDeleteModal(cat: CategoryResponse) {
    deleteTarget.value = cat
    deleteError.value = ''
    showDeleteModal.value = true
  }

  async function confirmDelete() {
    if (!deleteTarget.value) return
    submittingDelete.value = true
    deleteError.value = ''
    try {
      await categoriesStore.deleteCategory(deleteTarget.value.id)
      showDeleteModal.value = false
    } catch {
      deleteError.value = 'Failed to delete category. It may be in use by transactions.'
    } finally {
      submittingDelete.value = false
    }
  }

  onMounted(async () => {
    loading.value = true
    try {
      await categoriesStore.fetchCategories()
    } finally {
      loading.value = false
    }
  })
</script>
