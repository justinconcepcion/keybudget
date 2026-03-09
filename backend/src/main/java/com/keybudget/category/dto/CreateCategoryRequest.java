package com.keybudget.category.dto;

import com.keybudget.category.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for creating a new user-defined category.
 *
 * @param name  display name — must not be blank
 * @param icon  optional icon identifier (e.g. "home")
 * @param color optional hex color string (e.g. "#4CAF50")
 * @param type  INCOME or EXPENSE — must not be null
 */
public record CreateCategoryRequest(
        @NotBlank String name,
        String icon,
        String color,
        @NotNull CategoryType type
) {}
