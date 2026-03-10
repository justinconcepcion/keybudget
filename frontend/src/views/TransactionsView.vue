<template>
  <div class="px-6 py-8 max-w-7xl mx-auto">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white">
          Transactions
        </h1>
        <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">
          Track every dollar in and out.
        </p>
      </div>
      <div class="flex items-center gap-2">
        <button
          class="flex items-center gap-2 px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 text-sm font-medium rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
          @click="showImportModal = true"
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
              d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12"
            />
          </svg>
          Import CSV
        </button>
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
          Add Transaction
        </button>
      </div>
    </div>

    <!-- Import CSV Modal -->
    <div
      v-if="showImportModal"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/40"
      @click.self="showImportModal = false"
    >
      <div class="bg-white dark:bg-gray-800 rounded-2xl p-6 w-full max-w-md shadow-xl">
        <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">
          Import CSV
        </h3>
        <p class="text-sm text-gray-500 dark:text-gray-400 mb-4">
          Upload a CSV file with columns: Date, Description, Amount.
          Negative amounts are treated as expenses, positive as income.
        </p>
        <input
          ref="csvFileInput"
          type="file"
          accept=".csv"
          class="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-semibold file:bg-primary-50 file:text-primary-700 hover:file:bg-primary-100"
          @change="onCsvFileSelected"
        >
        <div
          v-if="importResult"
          class="mt-4 p-3 rounded-lg text-sm"
          :class="importResult.errors.length > 0 ? 'bg-yellow-50 dark:bg-yellow-950 text-yellow-800 dark:text-yellow-200' : 'bg-green-50 dark:bg-green-950 text-green-800 dark:text-green-200'"
        >
          <p class="font-medium">
            Imported {{ importResult.importedCount }} of {{ importResult.totalRows }} rows
          </p>
          <ul
            v-if="importResult.errors.length > 0"
            class="mt-2 list-disc list-inside text-xs"
          >
            <li
              v-for="(err, i) in importResult.errors.slice(0, 5)"
              :key="i"
            >
              {{ err }}
            </li>
            <li v-if="importResult.errors.length > 5">
              ...and {{ importResult.errors.length - 5 }} more
            </li>
          </ul>
        </div>
        <div
          v-if="importError"
          class="mt-4 p-3 bg-red-50 dark:bg-red-950 text-red-800 dark:text-red-200 rounded-lg text-sm"
        >
          {{ importError }}
        </div>
        <div class="flex justify-end gap-2 mt-6">
          <button
            class="px-4 py-2 text-sm text-gray-600 dark:text-gray-400 hover:text-gray-800"
            @click="showImportModal = false"
          >
            Close
          </button>
          <button
            :disabled="!csvFile || importingCsv"
            class="px-4 py-2 bg-primary-600 text-white text-sm font-medium rounded-lg hover:bg-primary-700 disabled:opacity-50 transition-colors"
            @click="submitCsvImport"
          >
            {{ importingCsv ? 'Importing...' : 'Import' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Filters -->
    <div class="bg-white dark:bg-gray-800 rounded-2xl border border-gray-200 dark:border-gray-700 p-4 mb-4 flex flex-wrap gap-3">
      <input
        v-model="selectedMonth"
        type="month"
        class="text-sm border border-gray-300 dark:border-gray-600 rounded-lg px-3 py-2 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
        @change="onFilterChange"
      >
      <select
        v-model="selectedCategoryId"
        class="text-sm border border-gray-300 dark:border-gray-600 rounded-lg px-3 py-2 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
        @change="onFilterChange"
      >
        <option value="">
          All categories
        </option>
        <option
          v-for="cat in categoriesStore.categories"
          :key="cat.id"
          :value="cat.id"
        >
          {{ cat.name }}
        </option>
      </select>
      <div class="flex rounded-lg border border-gray-300 dark:border-gray-600 overflow-hidden text-sm">
        <button
          v-for="opt in typeOptions"
          :key="opt.value"
          class="px-3 py-2 transition-colors"
          :class="
            selectedType === opt.value
              ? 'bg-primary-600 text-white'
              : 'bg-white dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600'
          "
          @click="selectType(opt.value)"
        >
          {{ opt.label }}
        </button>
      </div>
    </div>

    <!-- Transaction list -->
    <div class="bg-white dark:bg-gray-800 rounded-2xl border border-gray-200 dark:border-gray-700 overflow-hidden">
      <div
        v-if="loading"
        class="flex items-center justify-center h-64 text-gray-400 dark:text-gray-500 text-sm"
      >
        Loading…
      </div>
      <div
        v-else-if="transactionsStore.transactions.length === 0"
        class="flex items-center justify-center h-64 text-gray-400 dark:text-gray-500 text-sm"
      >
        <div class="text-center">
          <svg
            class="w-10 h-10 mx-auto mb-3 text-gray-300 dark:text-gray-600"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="1.5"
              d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"
            />
          </svg>
          <p class="font-medium text-gray-500 dark:text-gray-400">
            No transactions found
          </p>
          <p class="text-xs mt-1 dark:text-gray-500">
            Add your first transaction to get started.
          </p>
        </div>
      </div>
      <table
        v-else
        class="w-full text-sm"
      >
        <thead>
          <tr class="border-b border-gray-100 dark:border-gray-700 text-left">
            <th class="px-6 py-3 text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide">
              Date
            </th>
            <th class="px-6 py-3 text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide">
              Description
            </th>
            <th class="px-6 py-3 text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide">
              Category
            </th>
            <th
              class="px-6 py-3 text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide text-right"
            >
              Amount
            </th>
            <th class="px-6 py-3 text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide w-20">
              Actions
            </th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-50 dark:divide-gray-700">
          <tr
            v-for="tx in transactionsStore.transactions"
            :key="tx.id"
            class="hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
          >
            <td class="px-6 py-4 text-gray-500 dark:text-gray-400 tabular-nums whitespace-nowrap">
              {{ formatDate(tx.date) }}
            </td>
            <td class="px-6 py-4 text-gray-800 dark:text-gray-200 font-medium">
              {{ tx.description || '—' }}
            </td>
            <td class="px-6 py-4 text-gray-500 dark:text-gray-400">
              {{ tx.categoryName }}
            </td>
            <td
              class="px-6 py-4 text-right font-semibold tabular-nums whitespace-nowrap"
              :class="tx.type === 'INCOME' ? 'text-emerald-600' : 'text-red-600'"
            >
              {{ tx.type === 'INCOME' ? '+' : '-' }}{{ formatMoney(tx.amount) }}
            </td>
            <td class="px-6 py-4 whitespace-nowrap">
              <div class="flex gap-1">
                <button
                  class="p-1.5 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-primary-50 transition-colors"
                  title="Edit"
                  @click="openEditModal(tx)"
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
                  @click="openDeleteModal(tx)"
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
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div
      v-if="transactionsStore.pagination.totalPages > 1"
      class="flex items-center justify-between mt-4 text-sm text-gray-600 dark:text-gray-400"
    >
      <span>
        Page {{ transactionsStore.pagination.currentPage + 1 }} of
        {{ transactionsStore.pagination.totalPages }}
        ({{ transactionsStore.pagination.totalElements }} total)
      </span>
      <div class="flex gap-2">
        <button
          class="px-3 py-1.5 rounded-lg border border-gray-300 dark:border-gray-600 hover:bg-gray-50 dark:hover:bg-gray-700 dark:text-gray-300 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
          :disabled="transactionsStore.pagination.currentPage === 0"
          @click="changePage(transactionsStore.pagination.currentPage - 1)"
        >
          Previous
        </button>
        <button
          class="px-3 py-1.5 rounded-lg border border-gray-300 dark:border-gray-600 hover:bg-gray-50 dark:hover:bg-gray-700 dark:text-gray-300 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
          :disabled="
            transactionsStore.pagination.currentPage === transactionsStore.pagination.totalPages - 1
          "
          @click="changePage(transactionsStore.pagination.currentPage + 1)"
        >
          Next
        </button>
      </div>
    </div>

    <!-- Add Transaction modal -->
    <div
      v-if="showAddModal"
      class="fixed inset-0 z-50 flex items-center justify-center bg-gray-900/50"
      @mousedown.self="showAddModal = false"
    >
      <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-xl w-full max-w-md mx-4 p-6">
        <div class="flex items-center justify-between mb-5">
          <h2 class="text-lg font-semibold text-gray-900 dark:text-white">
            Add Transaction
          </h2>
          <button
            class="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
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
          @submit.prevent="submitTransaction"
        >
          <!-- Type toggle -->
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Type</label>
            <div class="flex rounded-lg border border-gray-300 dark:border-gray-600 overflow-hidden text-sm">
              <button
                type="button"
                class="flex-1 py-2 transition-colors"
                :class="
                  form.type === 'EXPENSE'
                    ? 'bg-red-500 text-white'
                    : 'bg-white dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600'
                "
                @click="form.type = 'EXPENSE'"
              >
                Expense
              </button>
              <button
                type="button"
                class="flex-1 py-2 transition-colors"
                :class="
                  form.type === 'INCOME'
                    ? 'bg-emerald-500 text-white'
                    : 'bg-white dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600'
                "
                @click="form.type = 'INCOME'"
              >
                Income
              </button>
            </div>
          </div>

          <!-- Amount -->
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Amount</label>
            <div class="relative">
              <span class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-sm">$</span>
              <input
                v-model.number="form.amount"
                type="number"
                step="0.01"
                min="0.01"
                required
                placeholder="0.00"
                class="w-full pl-7 pr-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              >
            </div>
          </div>

          <!-- Description -->
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Description
              <span class="text-gray-400 font-normal">(optional)</span>
            </label>
            <input
              v-model="form.description"
              type="text"
              placeholder="e.g. Grocery run"
              class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            >
          </div>

          <!-- Date -->
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Date</label>
            <input
              v-model="form.date"
              type="date"
              required
              class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            >
          </div>

          <!-- Category -->
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Category</label>
            <select
              v-model.number="form.categoryId"
              required
              class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            >
              <option
                value=""
                disabled
              >
                Select a category
              </option>
              <option
                v-for="cat in filteredFormCategories"
                :key="cat.id"
                :value="cat.id"
              >
                {{ cat.name }}
              </option>
            </select>
          </div>

          <!-- Error message -->
          <p
            v-if="formError"
            class="text-sm text-red-600"
          >
            {{ formError }}
          </p>

          <!-- Actions -->
          <div class="flex gap-3 pt-1">
            <button
              type="button"
              class="flex-1 px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
              @click="showAddModal = false"
            >
              Cancel
            </button>
            <button
              type="submit"
              :disabled="submitting"
              class="flex-1 px-4 py-2 text-sm font-medium text-white bg-primary-600 rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {{ submitting ? 'Saving…' : 'Save' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Edit Transaction modal -->
    <div
      v-if="showEditModal"
      class="fixed inset-0 z-50 flex items-center justify-center bg-gray-900/50"
      @mousedown.self="showEditModal = false"
    >
      <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-xl w-full max-w-md mx-4 p-6">
        <div class="flex items-center justify-between mb-5">
          <h2 class="text-lg font-semibold text-gray-900 dark:text-white">
            Edit Transaction
          </h2>
          <button
            class="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
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
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Type</label>
            <div class="flex rounded-lg border border-gray-300 dark:border-gray-600 overflow-hidden text-sm">
              <button
                type="button"
                class="flex-1 py-2 transition-colors"
                :class="editForm.type === 'EXPENSE' ? 'bg-red-500 text-white' : 'bg-white dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600'"
                @click="editForm.type = 'EXPENSE'"
              >
                Expense
              </button>
              <button
                type="button"
                class="flex-1 py-2 transition-colors"
                :class="editForm.type === 'INCOME' ? 'bg-emerald-500 text-white' : 'bg-white dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600'"
                @click="editForm.type = 'INCOME'"
              >
                Income
              </button>
            </div>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Amount</label>
            <div class="relative">
              <span class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-sm">$</span>
              <input
                v-model.number="editForm.amount"
                type="number"
                step="0.01"
                min="0.01"
                required
                class="w-full pl-7 pr-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              >
            </div>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Description
              <span class="text-gray-400 font-normal">(optional)</span>
            </label>
            <input
              v-model="editForm.description"
              type="text"
              class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            >
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Date</label>
            <input
              v-model="editForm.date"
              type="date"
              required
              class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            >
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Category</label>
            <select
              v-model.number="editForm.categoryId"
              required
              class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            >
              <option
                value=""
                disabled
              >
                Select a category
              </option>
              <option
                v-for="cat in filteredEditCategories"
                :key="cat.id"
                :value="cat.id"
              >
                {{ cat.name }}
              </option>
            </select>
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
              class="flex-1 px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
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
      <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-xl w-full max-w-sm mx-4 p-6">
        <h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-2">
          Delete Transaction
        </h2>
        <p class="text-sm text-gray-600 dark:text-gray-300 mb-5">
          Are you sure you want to delete this
          <span class="font-medium">{{ formatMoney(deleteTarget?.amount ?? 0) }}</span>
          transaction? This action cannot be undone.
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
            class="flex-1 px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
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
  import { ref, computed, reactive, watch, onMounted } from 'vue'
  import { useTransactionsStore } from '@/stores/transactions'
  import { useCategoriesStore } from '@/stores/categories'
  import { transactionsApi } from '@/api/transactions'
  import { formatMoney, formatDate } from '@/utils/formatting'
  import type { TransactionResponse, CsvImportResult } from '@/types'

  const transactionsStore = useTransactionsStore()
  const categoriesStore = useCategoriesStore()

  const loading = ref(false)
  const showAddModal = ref(false)
  const submitting = ref(false)
  const formError = ref('')

  // ── CSV Import ──────────────────────────────────────────────────────────────
  const showImportModal = ref(false)
  const csvFile = ref<File | null>(null)
  const csvFileInput = ref<HTMLInputElement | null>(null)
  const importingCsv = ref(false)
  const importResult = ref<CsvImportResult | null>(null)
  const importError = ref('')

  function onCsvFileSelected(e: Event) {
    const input = e.target as HTMLInputElement
    csvFile.value = input.files?.[0] ?? null
    importResult.value = null
    importError.value = ''
  }

  async function submitCsvImport() {
    if (!csvFile.value) return
    importingCsv.value = true
    importResult.value = null
    importError.value = ''
    try {
      importResult.value = await transactionsApi.importCsv(csvFile.value)
      if (importResult.value.importedCount > 0) {
        await loadTransactions(0)
      }
    } catch {
      importError.value = 'Failed to import CSV. Please check the file format.'
    } finally {
      importingCsv.value = false
    }
  }

  watch(showImportModal, (open) => {
    if (open) {
      csvFile.value = null
      importResult.value = null
      importError.value = ''
    }
  })

  // ── Filters ──────────────────────────────────────────────────────────────

  const typeOptions: { label: string; value: '' | 'INCOME' | 'EXPENSE' }[] = [
    { label: 'All', value: '' },
    { label: 'Income', value: 'INCOME' },
    { label: 'Expense', value: 'EXPENSE' },
  ]

  function todayMonthKey(): string {
    const now = new Date()
    const y = now.getFullYear()
    const m = String(now.getMonth() + 1).padStart(2, '0')
    return `${y}-${m}`
  }

  const selectedMonth = ref(todayMonthKey())
  const selectedCategoryId = ref<number | ''>('')
  const selectedType = ref<'INCOME' | 'EXPENSE' | ''>('')

  function monthDateRange(month: string): { start: string; end: string } {
    const [y, m] = month.split('-').map(Number)
    const lastDay = new Date(y, m, 0).getDate()
    return {
      start: `${month}-01`,
      end: `${month}-${String(lastDay).padStart(2, '0')}`,
    }
  }

  function selectType(value: 'INCOME' | 'EXPENSE' | '') {
    selectedType.value = value
    onFilterChange()
  }

  async function loadTransactions(page = 0) {
    loading.value = true
    const { start, end } = monthDateRange(selectedMonth.value)
    try {
      await transactionsStore.fetchTransactions({
        start,
        end,
        ...(selectedCategoryId.value !== ''
          ? { categoryId: selectedCategoryId.value as number }
          : {}),
        ...(selectedType.value !== '' ? { type: selectedType.value as 'INCOME' | 'EXPENSE' } : {}),
        page,
        size: 20,
      })
    } finally {
      loading.value = false
    }
  }

  function onFilterChange() {
    loadTransactions(0)
  }

  function changePage(page: number) {
    loadTransactions(page)
  }

  // ── Add Transaction form ──────────────────────────────────────────────────

  function todayDateStr(): string {
    const now = new Date()
    const y = now.getFullYear()
    const m = String(now.getMonth() + 1).padStart(2, '0')
    const d = String(now.getDate()).padStart(2, '0')
    return `${y}-${m}-${d}`
  }

  const form = reactive({
    type: 'EXPENSE' as 'INCOME' | 'EXPENSE',
    amount: null as number | null,
    description: '',
    date: todayDateStr(),
    categoryId: '' as number | '',
  })

  const filteredFormCategories = computed(() =>
    categoriesStore.categories.filter((c) => c.type === form.type),
  )

  watch(
    () => form.type,
    () => {
      form.categoryId = ''
    },
  )

  watch(showAddModal, (open) => {
    if (open) {
      form.type = 'EXPENSE'
      form.amount = null
      form.description = ''
      form.date = todayDateStr()
      form.categoryId = ''
      formError.value = ''
    }
  })

  async function submitTransaction() {
    if (!form.amount || form.amount <= 0) {
      formError.value = 'Please enter a valid amount.'
      return
    }
    if (!form.categoryId) {
      formError.value = 'Please select a category.'
      return
    }
    formError.value = ''
    submitting.value = true
    try {
      await transactionsStore.createTransaction({
        amount: form.amount,
        description: form.description || undefined,
        date: form.date,
        type: form.type,
        categoryId: form.categoryId as number,
      })
      showAddModal.value = false
      await loadTransactions(0)
    } catch {
      formError.value = 'Failed to save transaction. Please try again.'
    } finally {
      submitting.value = false
    }
  }

  // ── Edit Transaction ──────────────────────────────────────────────────────

  const showEditModal = ref(false)
  const submittingEdit = ref(false)
  const editFormError = ref('')
  const editTargetId = ref<number | null>(null)

  const editForm = reactive({
    type: 'EXPENSE' as 'INCOME' | 'EXPENSE',
    amount: null as number | null,
    description: '',
    date: '',
    categoryId: '' as number | '',
  })

  const filteredEditCategories = computed(() =>
    categoriesStore.categories.filter((c) => c.type === editForm.type),
  )

  watch(
    () => editForm.type,
    () => {
      editForm.categoryId = ''
    },
  )

  function openEditModal(tx: TransactionResponse) {
    editTargetId.value = tx.id
    editForm.type = tx.type
    editForm.amount = tx.amount
    editForm.description = tx.description || ''
    editForm.date = tx.date
    editForm.categoryId = tx.categoryId
    editFormError.value = ''
    showEditModal.value = true
  }

  async function submitEdit() {
    if (!editForm.amount || editForm.amount <= 0) {
      editFormError.value = 'Please enter a valid amount.'
      return
    }
    if (!editForm.categoryId) {
      editFormError.value = 'Please select a category.'
      return
    }
    editFormError.value = ''
    submittingEdit.value = true
    try {
      await transactionsStore.updateTransaction(editTargetId.value!, {
        amount: editForm.amount,
        description: editForm.description || undefined,
        date: editForm.date,
        type: editForm.type,
        categoryId: editForm.categoryId as number,
      })
      showEditModal.value = false
      await loadTransactions(transactionsStore.pagination.currentPage)
    } catch {
      editFormError.value = 'Failed to update transaction. Please try again.'
    } finally {
      submittingEdit.value = false
    }
  }

  // ── Delete Transaction ────────────────────────────────────────────────────

  const showDeleteModal = ref(false)
  const submittingDelete = ref(false)
  const deleteError = ref('')
  const deleteTarget = ref<TransactionResponse | null>(null)

  function openDeleteModal(tx: TransactionResponse) {
    deleteTarget.value = tx
    deleteError.value = ''
    showDeleteModal.value = true
  }

  async function confirmDelete() {
    if (!deleteTarget.value) return
    submittingDelete.value = true
    deleteError.value = ''
    try {
      await transactionsStore.deleteTransaction(deleteTarget.value.id)
      showDeleteModal.value = false
      await loadTransactions(transactionsStore.pagination.currentPage)
    } catch {
      deleteError.value = 'Failed to delete transaction. Please try again.'
    } finally {
      submittingDelete.value = false
    }
  }

  onMounted(async () => {
    await categoriesStore.fetchCategories()
    await loadTransactions(0)
  })
</script>
