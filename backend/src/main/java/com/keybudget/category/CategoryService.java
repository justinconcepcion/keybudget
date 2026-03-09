package com.keybudget.category;

import com.keybudget.category.dto.CategoryResponse;
import com.keybudget.category.dto.CreateCategoryRequest;

import java.util.List;

/** Business operations for categories. */
public interface CategoryService {

    /**
     * Returns all categories available to the given user: their own custom categories
     * plus all system-default categories.
     *
     * @param userId the authenticated user's id
     * @return combined list ordered by type then name
     */
    List<CategoryResponse> getCategories(Long userId);

    /**
     * Creates a new user-defined category.
     *
     * @param userId the authenticated user's id
     * @param req    the creation payload
     * @return the persisted category as a response DTO
     */
    CategoryResponse createCategory(Long userId, CreateCategoryRequest req);
}
