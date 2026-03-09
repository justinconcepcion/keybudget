package com.keybudget.category;

import com.keybudget.category.dto.CategoryResponse;
import com.keybudget.category.dto.CreateCategoryRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    private CategoryServiceImpl categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryServiceImpl(categoryRepository);
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
