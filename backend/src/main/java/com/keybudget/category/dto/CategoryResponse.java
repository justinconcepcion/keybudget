package com.keybudget.category.dto;

import com.keybudget.category.CategoryType;

/**
 * API response record for a single category.
 *
 * @param id        category primary key
 * @param name      display name
 * @param icon      icon identifier (may be null)
 * @param color     hex color string (may be null)
 * @param type      INCOME or EXPENSE
 * @param isDefault true when this is a system-default category (userId is null)
 */
public record CategoryResponse(
        Long id,
        String name,
        String icon,
        String color,
        CategoryType type,
        boolean isDefault
) {}
