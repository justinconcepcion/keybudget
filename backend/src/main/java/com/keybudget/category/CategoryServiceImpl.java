package com.keybudget.category;

import com.keybudget.category.dto.CategoryResponse;
import com.keybudget.category.dto.CreateCategoryRequest;
import com.keybudget.category.dto.UpdateCategoryRequest;
import com.keybudget.shared.ResourceNotFoundException;
import com.keybudget.transaction.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Default implementation of {@link CategoryService}. */
@Slf4j
@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository,
                               TransactionRepository transactionRepository) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public void seedDefaults() {
        if (categoryRepository.existsByUserIdIsNull()) {
            log.debug("Default categories already seeded — skipping");
            return;
        }

        log.info("Seeding default categories");

        // EXPENSE defaults
        save("Housing",            "home",             "#4CAF50", CategoryType.EXPENSE);
        save("Food & Groceries",   "shopping-cart",    "#FF9800", CategoryType.EXPENSE);
        save("Transportation",     "car",              "#2196F3", CategoryType.EXPENSE);
        save("Entertainment",      "film",             "#9C27B0", CategoryType.EXPENSE);
        save("Healthcare",         "heart",            "#F44336", CategoryType.EXPENSE);
        save("Utilities",          "zap",              "#607D8B", CategoryType.EXPENSE);
        save("Shopping",           "shopping-bag",     "#E91E63", CategoryType.EXPENSE);
        save("Education",          "book",             "#3F51B5", CategoryType.EXPENSE);
        save("Personal Care",      "user",             "#00BCD4", CategoryType.EXPENSE);
        save("Other",              "more-horizontal",  "#795548", CategoryType.EXPENSE);

        // INCOME defaults
        save("Salary",      "briefcase",   "#4CAF50", CategoryType.INCOME);
        save("Freelance",   "laptop",      "#FF9800", CategoryType.INCOME);
        save("Investments", "trending-up", "#2196F3", CategoryType.INCOME);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories(Long userId) {
        return categoryRepository.findByUserIdOrUserIdIsNull(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public CategoryResponse createCategory(Long userId, CreateCategoryRequest req) {
        Category category = new Category();
        category.setUserId(userId);
        category.setName(req.name());
        category.setIcon(req.icon());
        category.setColor(req.color());
        category.setType(req.type());
        return toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long userId, Long categoryId, UpdateCategoryRequest req) {
        Category category = categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));

        category.setName(req.name());
        category.setIcon(req.icon());
        category.setColor(req.color());
        category.setType(req.type());
        return toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void deleteCategory(Long userId, Long categoryId) {
        Category category = categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));

        if (transactionRepository.existsByUserIdAndCategoryId(userId, categoryId)) {
            throw new IllegalArgumentException("Cannot delete category with existing transactions");
        }

        categoryRepository.delete(category);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void save(String name, String icon, String color, CategoryType type) {
        Category c = new Category();
        c.setName(name);
        c.setIcon(icon);
        c.setColor(color);
        c.setType(type);
        // userId remains null — system default
        categoryRepository.save(c);
    }

    private CategoryResponse toResponse(Category c) {
        return new CategoryResponse(
                c.getId(),
                c.getName(),
                c.getIcon(),
                c.getColor(),
                c.getType(),
                c.getUserId() == null
        );
    }
}
