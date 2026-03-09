package com.keybudget.category.dto;

import com.keybudget.category.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateCategoryRequest(
        @NotBlank String name,
        String icon,
        String color,
        @NotNull CategoryType type
) {}
