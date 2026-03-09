package com.keybudget.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCurrencyRequest(
        @NotBlank @Size(min = 3, max = 3) String currency
) {}
