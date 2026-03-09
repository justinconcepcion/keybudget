package com.keybudget.category;

import com.keybudget.category.dto.CategoryResponse;
import com.keybudget.category.dto.CreateCategoryRequest;
import com.keybudget.category.dto.UpdateCategoryRequest;
import com.keybudget.shared.ResourceNotFoundException;
import com.keybudget.transaction.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private CategoryServiceImpl categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryServiceImpl(categoryRepository, transactionRepository);
    }

    // -------------------------------------------------------------------------
    // seedDefaults
    // -------------------------------------------------------------------------

    @Test
    void seedDefaults_givenNoExistingDefaults_savesThirteenCategories() {
        when(categoryRepository.existsByUserIdIsNull()).thenReturn(false);

        categoryService.seedDefaults();

        verify(categoryRepository, times(13)).save(any(Category.class));
    }

    @Test
    void seedDefaults_givenDefaultsAlreadyExist_skipsSeeding() {
        when(categoryRepository.existsByUserIdIsNull()).thenReturn(true);

        categoryService.seedDefaults();

        verify(categoryRepository, never()).save(any(Category.class));
    }

    // -------------------------------------------------------------------------
    // getCategories
    // -------------------------------------------------------------------------

    @Test
    void getCategories_givenUserWithCustomAndDefaultCategories_returnsBoth() {
        Category custom = buildCategory(1L, "My Custom", null, CategoryType.EXPENSE);
        Category defaultCat = buildCategory(2L, "Housing", null, CategoryType.EXPENSE);
        when(categoryRepository.findByUserIdOrUserIdIsNull(42L)).thenReturn(List.of(custom, defaultCat));

        List<CategoryResponse> result = categoryService.getCategories(42L);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(CategoryResponse::id).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void getCategories_givenSystemDefaultCategory_isDefaultTrue() {
        Category defaultCat = buildCategory(1L, "Housing", null, CategoryType.EXPENSE);
        // userId is null for system defaults — already set in buildCategory
        when(categoryRepository.findByUserIdOrUserIdIsNull(1L)).thenReturn(List.of(defaultCat));

        List<CategoryResponse> result = categoryService.getCategories(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isDefault()).isTrue();
    }

    @Test
    void getCategories_givenUserCustomCategory_isDefaultFalse() {
        Category custom = buildCategory(1L, "My Budget", 99L, CategoryType.EXPENSE);
        when(categoryRepository.findByUserIdOrUserIdIsNull(99L)).thenReturn(List.of(custom));

        List<CategoryResponse> result = categoryService.getCategories(99L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isDefault()).isFalse();
    }

    @Test
    void getCategories_givenNoCategories_returnsEmptyList() {
        when(categoryRepository.findByUserIdOrUserIdIsNull(1L)).thenReturn(List.of());

        List<CategoryResponse> result = categoryService.getCategories(1L);

        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // createCategory
    // -------------------------------------------------------------------------

    @Test
    void createCategory_givenValidRequest_savesAndReturnsResponse() {
        CreateCategoryRequest req = new CreateCategoryRequest("Rent", "home", "#FF0000", CategoryType.EXPENSE);
        Category saved = buildCategory(10L, "Rent", 5L, CategoryType.EXPENSE);
        saved.setIcon("home");
        saved.setColor("#FF0000");
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);

        CategoryResponse result = categoryService.createCategory(5L, req);

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.name()).isEqualTo("Rent");
        assertThat(result.icon()).isEqualTo("home");
        assertThat(result.color()).isEqualTo("#FF0000");
        assertThat(result.type()).isEqualTo(CategoryType.EXPENSE);
        assertThat(result.isDefault()).isFalse();
    }

    @Test
    void createCategory_givenValidRequest_setsCorrectUserId() {
        CreateCategoryRequest req = new CreateCategoryRequest("Bonus", null, null, CategoryType.INCOME);
        Category saved = buildCategory(11L, "Bonus", 7L, CategoryType.INCOME);
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);

        categoryService.createCategory(7L, req);

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(7L);
    }

    // -------------------------------------------------------------------------
    // updateCategory
    // -------------------------------------------------------------------------

    @Test
    void updateCategory_givenValidRequest_updatesAndReturnsResponse() {
        Category existing = buildCategory(10L, "Old Name", 5L, CategoryType.EXPENSE);
        when(categoryRepository.findByIdAndUserId(10L, 5L)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any(Category.class))).thenReturn(existing);

        UpdateCategoryRequest req = new UpdateCategoryRequest("New Name", "star", "#000000", CategoryType.INCOME);
        CategoryResponse result = categoryService.updateCategory(5L, 10L, req);

        assertThat(result.name()).isEqualTo("New Name");
        assertThat(existing.getIcon()).isEqualTo("star");
    }

    @Test
    void updateCategory_givenNotFound_throwsResourceNotFoundException() {
        when(categoryRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        UpdateCategoryRequest req = new UpdateCategoryRequest("Name", null, null, CategoryType.EXPENSE);

        assertThatThrownBy(() -> categoryService.updateCategory(1L, 999L, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // deleteCategory
    // -------------------------------------------------------------------------

    @Test
    void deleteCategory_givenValidId_deletesCategory() {
        Category existing = buildCategory(10L, "Custom", 5L, CategoryType.EXPENSE);
        when(categoryRepository.findByIdAndUserId(10L, 5L)).thenReturn(Optional.of(existing));
        when(transactionRepository.existsByUserIdAndCategoryId(5L, 10L)).thenReturn(false);

        categoryService.deleteCategory(5L, 10L);

        verify(categoryRepository).delete(existing);
    }

    @Test
    void deleteCategory_givenNotFound_throwsResourceNotFoundException() {
        when(categoryRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategory(1L, 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteCategory_givenTransactionsExist_throwsIllegalArgument() {
        Category existing = buildCategory(10L, "Custom", 5L, CategoryType.EXPENSE);
        when(categoryRepository.findByIdAndUserId(10L, 5L)).thenReturn(Optional.of(existing));
        when(transactionRepository.existsByUserIdAndCategoryId(5L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.deleteCategory(5L, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("existing transactions");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Category buildCategory(Long id, String name, Long userId, CategoryType type) {
        Category c = new Category() {
            @Override
            public Long getId() { return id; }
        };
        c.setName(name);
        c.setUserId(userId);
        c.setType(type);
        return c;
    }
}
